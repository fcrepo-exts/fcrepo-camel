/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.converter.stream.CachedOutputStream;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The Fedora producer.
 *
 * @author Aaron Coburn
 * @since October 20, 2014
 */
public class FcrepoProducer extends DefaultProducer {

    public static final String DEFAULT_CONTENT_TYPE = "application/rdf+xml";

    private static final Logger LOGGER = getLogger(FcrepoProducer.class);

    private FcrepoEndpoint endpoint;

    private FcrepoClient client;

    private TransactionTemplate transactionTemplate;

    /**
     * Create a FcrepoProducer object
     *
     * @param endpoint the FcrepoEndpoint corresponding to the exchange.
     */
    public FcrepoProducer(final FcrepoEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
        this.transactionTemplate = endpoint.createTransactionTemplate();
        this.client = new FcrepoClient(
                endpoint.getAuthUsername(),
                endpoint.getAuthPassword(),
                endpoint.getAuthHost(),
                endpoint.getThrowExceptionOnFailure());

    }

    /**
     * Define how message exchanges are processed.
     *
     * @param exchange the InOut message exchange
     * @throws FcrepoOperationFailedException when the underlying HTTP request results in an error
     */
    @Override
    public void process(final Exchange exchange) throws FcrepoOperationFailedException {
        if (exchange.isTransacted()) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    final DefaultTransactionStatus st = (DefaultTransactionStatus)status;
                    final FcrepoTransactionObject tx = (FcrepoTransactionObject)st.getTransaction();
                    try {
                        doRequest(exchange, tx.getSessionId());
                    } catch (FcrepoOperationFailedException ex) {
                        throw new TransactionSystemException(
                            "Error executing fcrepo request in transaction: ", ex);
                    }
                }
            });
        } else {
            doRequest(exchange, null);
        }
    }

    private void doRequest(final Exchange exchange, final String transaction) throws FcrepoOperationFailedException {
        final Message in = exchange.getIn();
        final HttpMethods method = getMethod(exchange);
        final String url = getUrl(exchange, transaction);
        final String contentType = Optional.ofNullable(endpoint.getContentType())
                                           .filter(x -> x.length() > 0)
                                           .orElse(ExchangeHelper.getContentType(exchange));

        LOGGER.debug("Fcrepo Request [{}] with method [{}]", url, method);

        FcrepoResponse response;

        switch (method) {
        case PATCH:
            response = client.patch(getMetadataUri(url), in.getBody(InputStream.class));
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
            break;
        case PUT:
            response = client.put(URI.create(url), in.getBody(InputStream.class), contentType);
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
            break;
        case POST:
            response = client.post(URI.create(url), in.getBody(InputStream.class), contentType);
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
            break;
        case DELETE:
            response = client.delete(URI.create(url));
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
            break;
        case HEAD:
            response = client.head(URI.create(url));
            exchange.getIn().setBody(null);
            break;
        case GET:
        default:
            response = client.get(endpoint.getMetadata() ? getMetadataUri(url) : URI.create(url),
                    getAccept(exchange), getPrefer(exchange).orElse(null));
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
        }

        response.getContentType()
                .ifPresent(x -> exchange.getIn().setHeader(Exchange.CONTENT_TYPE, x));
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, response.getStatusCode());
    }



    /**
     * Retrieve the resource location from a HEAD request.
     */
    private URI getMetadataUri(final String url) throws FcrepoOperationFailedException {
        final FcrepoResponse headResponse = client.head(URI.create(url));
        return headResponse.getLocation().orElse(URI.create(url));
    }

    /**
     * Given an exchange, determine which HTTP method to use. Basically, use GET unless the value of the
     * Exchange.HTTP_METHOD header is defined. Unlike the http4: component, the request does not use POST if there is
     * a message body defined. This is so in order to avoid inadvertant changes to the repository.
     *
     * @param exchange the incoming message exchange
     */
    private HttpMethods getMethod(final Exchange exchange) {
        return Optional.ofNullable(exchange.getIn().getHeader(Exchange.HTTP_METHOD, HttpMethods.class))
                       .orElse(HttpMethods.GET);
    }

    /**
     * Given an exchange, extract the value for use with an Accept header. The order of preference is:
     * 1) whether a transform is being requested 2) an accept value is set on the endpoint 3) a value set on
     * the Exchange.ACCEPT_CONTENT_TYPE header 4) a value set on an "Accept" header 5) the endpoint
     * DEFAULT_CONTENT_TYPE (i.e. application/rdf+xml)
     *
     * @param exchange the incoming message exchange
     */
    private String getAccept(final Exchange exchange) {
        final Message in = exchange.getIn();
        final String fcrepoTransform = in.getHeader(FcrepoHeaders.FCREPO_TRANSFORM, String.class);
        final String acceptHeader = Optional.ofNullable(in.getHeader(Exchange.ACCEPT_CONTENT_TYPE, String.class))
                                            .orElse(in.getHeader("Accept", String.class));

        if (!isBlank(endpoint.getTransform()) || !isBlank(fcrepoTransform)) {
            return "application/json";
        } else if (!isBlank(endpoint.getAccept())) {
            return endpoint.getAccept();
        } else if (!isBlank(acceptHeader)) {
            return acceptHeader;
        } else {
            return DEFAULT_CONTENT_TYPE;
        }
    }

    /**
     *  Extract a transformation path from the exchange if the appropriate headers
     *  are set. This will format the URL to use the transform program defined
     *  in the CamelFcrepoTransform header or the transform uri option (in that
     *  order of precidence).
     *
     *  @param exchange the camel message exchange
     *  @return String
     */
    private Optional<String> getTransformPath(final Exchange exchange) {
        final Message in = exchange.getIn();
        final HttpMethods method = getMethod(exchange);
        final Optional<String> transformProgram = Optional.ofNullable(
                in.getHeader(FcrepoHeaders.FCREPO_TRANSFORM, String.class));

        if (!isBlank(endpoint.getTransform()) || transformProgram.isPresent()) {
            if (method == HttpMethods.POST) {
                return Optional.of(FcrepoConstants.TRANSFORM);
            } else if (method == HttpMethods.GET) {
                final Optional<String> maybe = transformProgram
                            .filter(x -> x.length() > 0)
                            .map(x -> FcrepoConstants.TRANSFORM + "/" + x);
                if (maybe.isPresent()) {
                    return maybe;
                } else {
                    return Optional.of(FcrepoConstants.TRANSFORM + "/" + endpoint.getTransform());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Given an exchange, extract the fully qualified URL for a fedora resource. By default, this will use the entire
     * path set on the endpoint. If either of the following headers are defined, they will be appended to that path in
     * this order of preference: 1) FCREPO_IDENTIFIER 2) org.fcrepo.jms.identifier
     *
     * @param exchange the incoming message exchange
     */
    private String getUrl(final Exchange exchange, final String transaction) {
        final StringBuilder url = new StringBuilder();

        url.append(endpoint.getBaseUrlWithScheme());

        url.append(Optional.ofNullable(transaction).map(x -> "/" + x).orElse(""));

        url.append(Optional.of(getIdentifierPath(exchange)).filter(x -> !isBlank(x)).orElse(""));

        getTransformPath(exchange).ifPresent(x -> url.append(x));

        if (getMethod(exchange) == HttpMethods.DELETE && endpoint.getTombstone()) {
            url.append(FcrepoConstants.TOMBSTONE);
        }

        return url.toString();
    }

    /**
     *  Given an exchange, extract the Fcrepo node path, if any.
     *
     *  @param exchange the incoming message exchange
     */
    private String getIdentifierPath(final Exchange exchange) {
        final Message in = exchange.getIn();

        return Optional.ofNullable(in.getHeader(FcrepoHeaders.FCREPO_IDENTIFIER, String.class))
            .orElse(in.getHeader(JmsHeaders.IDENTIFIER, "", String.class));
    }

    /**
     *  Given an exchange, extract the Prefer headers, if any.
     *
     *  @param exchange the incoming message exchange
     */
    private Optional<String> getPrefer(final Exchange exchange) {
        final Message in = exchange.getIn();

        if (getMethod(exchange) == HttpMethods.GET) {
            final Optional<String> maybe = Optional.ofNullable(
                    in.getHeader(FcrepoHeaders.FCREPO_PREFER, String.class));
            if (maybe.isPresent()) {
                return maybe;
            } else {
                return buildPreferHeader(endpoint.getPreferInclude(), endpoint.getPreferOmit());
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     *  Build the prefer header from include and/or omit endpoint values
     */
    private Optional<String> buildPreferHeader(final String include, final String omit) {
        if (isBlank(include) && isBlank(omit)) {
            return Optional.empty();
        } else {
            final StringBuilder prefer = new StringBuilder("return=representation;");

            if (!isBlank(include)) {
                prefer.append(" include=\"");
                prefer.append(
                    Optional.ofNullable(RdfNamespaces.PREFER_PROPERTIES.get(include))
                            .orElse(include));
                prefer.append("\";");
            }
            if (!isBlank(omit)) {
                prefer.append(" omit=\"");
                prefer.append(
                    Optional.ofNullable(RdfNamespaces.PREFER_PROPERTIES.get(omit))
                            .orElse(omit));
                prefer.append("\";");
            }
            return Optional.of(prefer.toString());
        }
    }

    private static Object extractResponseBodyAsStream(final InputStream is, final Exchange exchange) {
        // As httpclient is using a AutoCloseInputStream, it will be closed when the connection is closed
        // we need to cache the stream for it.
        if (is == null) {
            return null;
        }

        // convert the input stream to StreamCache if the stream cache is not disabled
        if (exchange.getProperty(Exchange.DISABLE_HTTP_STREAM_CACHE, Boolean.FALSE, Boolean.class)) {
            return is;
        } else {
            try (final CachedOutputStream cos = new CachedOutputStream(exchange)) {
                // This CachedOutputStream will not be closed when the exchange is onCompletion
                IOHelper.copyAndCloseInput(is, cos);
                // When the InputStream is closed, the CachedOutputStream will be closed
                return cos.newStreamCache();
            } catch (IOException ex) {
                LOGGER.debug("Error extracting body from http request", ex);
                return null;
            }
        }
    }
}
