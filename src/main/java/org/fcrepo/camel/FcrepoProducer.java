/**
 * Copyright 2014 DuraSpace, Inc.
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

import java.net.URI;
import java.io.InputStream;
import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.http4.HttpOperationFailedException;
import org.apache.camel.converter.stream.CachedOutputStream;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;

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

    /**
     * Create a FcrepoProducer object
     *
     * @param endpoint the FcrepoEndpoint corresponding to the exchange.
     */
    public FcrepoProducer(final FcrepoEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
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
     * @throws HttpOperationFailedException
     */
    @Override
    public void process(final Exchange exchange) throws HttpOperationFailedException, IOException {
        final Message in = exchange.getIn();
        final HttpMethods method = getMethod(exchange);
        final String contentType = getContentType(exchange);
        final String accept = getAccept(exchange);
        final String url = getUrl(exchange);
        final String prefer = getPrefer(exchange);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Fcrepo Request [{}] with method [{}]", url, method);
        }

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
            response = client.get(endpoint.getMetadata() ? getMetadataUri(url) : URI.create(url), accept, prefer);
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
        }
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, response.getContentType());
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, response.getStatusCode());
    }

    /**
     * Retrieve the resource location from a HEAD request.
     */
    private URI getMetadataUri(final String url)
            throws HttpOperationFailedException, IOException {
        final FcrepoResponse headResponse = client.head(URI.create(url));
        if (headResponse.getLocation() != null) {
            return headResponse.getLocation();
        } else {
            return URI.create(url);
        }
    }

    /**
     * Given an exchange, determine which HTTP method to use. Basically, use GET unless the value of the
     * Exchange.HTTP_METHOD header is defined. Unlike the http4: component, the request does not use POST if there is
     * a message body defined. This is so in order to avoid inadvertant changes to the repository.
     *
     * @param exchange the incoming message exchange
     */
    private HttpMethods getMethod(final Exchange exchange) {
        final HttpMethods method = exchange.getIn().getHeader(Exchange.HTTP_METHOD, HttpMethods.class);
        if (method == null) {
            return HttpMethods.GET;
        } else {
            return method;
        }
    }

    /**
     * Given an exchange, extract the contentType value for use with a Content-Type header. The order of preference is
     * so: 1) a contentType value set on the endpoint 2) a contentType value set on the Exchange.CONTENT_TYPE header
     *
     * @param exchange the incoming message exchange
     */
    private String getContentType(final Exchange exchange) {
        final String contentTypeString = ExchangeHelper.getContentType(exchange);
        if (!isBlank(endpoint.getContentType())) {
            return endpoint.getContentType();
        } else if (!isBlank(contentTypeString)) {
            return contentTypeString;
        } else {
            return null;
        }
    }

    /**
     * Given an exchange, extract the accept value for use with an Accept header. The order of preference is:
     * 1) whether a transform is being requested 2) an accept value is set on the endpoint 3) a value set on
     * the Exchange.ACCEPT_CONTENT_TYPE header 4) a value set on an "Accept" header 5) the endpoint
     * DEFAULT_CONTENT_TYPE (i.e. application/rdf+xml)
     *
     * @param exchange the incoming message exchange
     */
    private String getAccept(final Exchange exchange) {
        final Message in = exchange.getIn();
        final String fcrepoTransform = in.getHeader(FcrepoHeaders.FCREPO_TRANSFORM, String.class);

        if (!isBlank(endpoint.getTransform()) || !isBlank(fcrepoTransform)) {
            return "application/json";
        } else if (!isBlank(endpoint.getAccept())) {
            return endpoint.getAccept();
        } else if (!isBlank(in.getHeader(Exchange.ACCEPT_CONTENT_TYPE, String.class))) {
            return in.getHeader(Exchange.ACCEPT_CONTENT_TYPE, String.class);
        } else if (!isBlank(in.getHeader("Accept", String.class))) {
            return in.getHeader("Accept", String.class);
        } else {
            return DEFAULT_CONTENT_TYPE;
        }
    }

    /**
     * Given an exchange, extract the fully qualified URL for a fedora resource. By default, this will use the entire
     * path set on the endpoint. If either of the following headers are defined, they will be appended to that path in
     * this order of preference: 1) FCREPO_IDENTIFIER 2) org.fcrepo.jms.identifier
     *
     * @param exchange the incoming message exchange
     */
    private String getUrl(final Exchange exchange) {
        final Message in = exchange.getIn();
        final HttpMethods method = getMethod(exchange);
        final URI baseUri = URI.create(endpoint.getBaseUrl());
        final String fcrepoTransform = in.getHeader(FcrepoHeaders.FCREPO_TRANSFORM, String.class);
        final StringBuilder url = new StringBuilder("http://" + baseUri);

        if (!isBlank(in.getHeader(FcrepoHeaders.FCREPO_IDENTIFIER, String.class))) {
            url.append(in.getHeader(FcrepoHeaders.FCREPO_IDENTIFIER, String.class));
        } else if (!isBlank(in.getHeader(JmsHeaders.IDENTIFIER, String.class))) {
            url.append(in.getHeader(JmsHeaders.IDENTIFIER, String.class));
        }

        if (!isBlank(endpoint.getTransform()) || !isBlank(fcrepoTransform)) {
            if (method == HttpMethods.POST) {
                url.append("/fcr:transform");
            } else if (method == HttpMethods.GET) {
                if (!isBlank(fcrepoTransform)) {
                    url.append("/fcr:transform/" + fcrepoTransform);
                } else {
                    url.append("/fcr:transform/" + endpoint.getTransform());
                }
            }
        } else if (method == HttpMethods.DELETE && endpoint.getTombstone()) {
            url.append("/fcr:tombstone");
        }

        return url.toString();
    }

    /**
     *  Given an exchange, extract the Prefer headers, if any.
     *
     *  @param exchange the incoming message exchange
     */
    private String getPrefer(final Exchange exchange) {
        final Message in = exchange.getIn();

        if (getMethod(exchange) == HttpMethods.GET) {
            if (!isBlank(in.getHeader(FcrepoHeaders.FCREPO_PREFER, String.class))) {
                return in.getHeader(FcrepoHeaders.FCREPO_PREFER, String.class);
            } else {
                return buildPreferHeader(endpoint.getPreferInclude(), endpoint.getPreferOmit());
            }
        } else {
            return null;
        }
    }

    /**
     *  Build the prefer header from include and/or omit endpoint values
     */
    private String buildPreferHeader(final String include, final String omit) {
        if (isBlank(include) && isBlank(omit)) {
            return null;
        } else {
            String prefer = "return=representation;";
            if (!isBlank(include)) {
                prefer += " include=\"" + addPreferNamespace(include) + "\";";
            }
            if (!isBlank(omit)) {
                prefer += " omit=\"" + addPreferNamespace(omit) + "\";";
            }
            return prefer;
        }
    }

    /**
     *  Add the appropriate namespace to the prefer header in case the
     *  short form was supplied.
     */
    private String addPreferNamespace(final String property) {
        final String prefer = RdfNamespaces.PREFER_PROPERTIES.get(property);
        if (!isBlank(prefer)) {
            return prefer;
        } else {
            return property;
        }
    }

    private static InputStream extractResponseBodyAsStream(final InputStream is, final Exchange exchange)
            throws IOException {
        // As httpclient is using a AutoCloseInputStream, it will be closed when the connection is closed
        // we need to cache the stream for it.
        if (is == null) {
            return null;
        } else {
            try (final CachedOutputStream cos = new CachedOutputStream(exchange, false)) {
                // This CachedOutputStream will not be closed when the exchange is onCompletion
                IOHelper.copy(is, cos);
                // When the InputStream is closed, the CachedOutputStream will be closed
                return cos.getWrappedInputStream();
            } finally {
                IOHelper.close(is, "Extracting response body", LOGGER);
            }
        }
    }
}
