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

import java.net.URI;
import java.io.InputStream;
import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
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

    public static final int DEFAULT_HTTPS_PORT = 443;

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
     * @throws FcrepoOperationFailedException when the underlying HTTP request results in an error
     */
    @Override
    public void process(final Exchange exchange) throws FcrepoOperationFailedException {
        final Message in = exchange.getIn();
        final HttpMethods method = getMethod(exchange);
        final String contentType = getContentType(exchange);
        final String accept = getAccept(exchange);
        final String url = getUrl(exchange);
        final String prefer = getPrefer(exchange);

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
            throws FcrepoOperationFailedException {
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
        final String acceptHeader = getAcceptHeader(exchange);

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
     * Given an exchange, extract the value of an incoming Accept header.
     *
     * @param exchange the incoming message exchange
     */
    private String getAcceptHeader(final Exchange exchange) {
        final Message in = exchange.getIn();
        if (!isBlank(in.getHeader(Exchange.ACCEPT_CONTENT_TYPE, String.class))) {
            return in.getHeader(Exchange.ACCEPT_CONTENT_TYPE, String.class);
        } else if (!isBlank(in.getHeader("Accept", String.class))) {
            return in.getHeader("Accept", String.class);
        } else {
            return null;
        }
    }

    /**
     * Get the repository baseUrl with a full scheme.
     * The base URL may be any of the following:
     * localhost:8080/rest
     * fedora.institution.org:8983/rest
     * http://localhost:8080/fcrepo/rest
     * https://fedora.institution.org/rest
     * fedora.insitution.org:443/rest
     *
     * This method ensures that the url (fragment) is properly prefixed
     * with either the http or https scheme, suitable for sending to the
     * httpclient.
     *
     * @return String
     */
    private String getBaseUrlWithScheme() {
        final String baseUrl = endpoint.getBaseUrl();
        final StringBuilder url = new StringBuilder();

        if (!baseUrl.startsWith("http:") && !baseUrl.startsWith("https:")) {
            if (URI.create("http://" + baseUrl).getPort() == DEFAULT_HTTPS_PORT) {
                url.append("https://");
            } else {
                url.append("http://");
            }
        }
        url.append(baseUrl);
        return url.toString();
    }

    /**
     * The resource path can be set either by the Camel header (CamelFcrepoIdentifier)
     * or by fedora's jms headers (org.fcrepo.jms.identifier). This method extracts
     * a path from the appropriate header (the camel header overrides the jms header).
     *
     * @param exchange The camel exchange
     * @return String
     */
    private String getPathFromHeaders(final Exchange exchange) {
        final Message in = exchange.getIn();

        if (!isBlank(in.getHeader(FcrepoHeaders.FCREPO_IDENTIFIER, String.class))) {
            return in.getHeader(FcrepoHeaders.FCREPO_IDENTIFIER, String.class);
        } else if (!isBlank(in.getHeader(JmsHeaders.IDENTIFIER, String.class))) {
            return in.getHeader(JmsHeaders.IDENTIFIER, String.class);
        } else {
            return "";
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
    private String getTransformPath(final Exchange exchange) {
        final Message in = exchange.getIn();
        final HttpMethods method = getMethod(exchange);
        final String transformProgram = in.getHeader(FcrepoHeaders.FCREPO_TRANSFORM, String.class);
        final String fcrTransform = "/fcr:transform";

        if (!isBlank(endpoint.getTransform()) || !isBlank(transformProgram)) {
            if (method == HttpMethods.POST) {
                return fcrTransform;
            } else if (method == HttpMethods.GET) {
                if (!isBlank(transformProgram)) {
                    return fcrTransform + "/" + transformProgram;
                } else {
                    return fcrTransform + "/" + endpoint.getTransform();
                }
            }
        }
        return "";
    }

    /**
     * Given an exchange, extract the fully qualified URL for a fedora resource. By default, this will use the entire
     * path set on the endpoint. If either of the following headers are defined, they will be appended to that path in
     * this order of preference: 1) FCREPO_IDENTIFIER 2) org.fcrepo.jms.identifier
     *
     * @param exchange the incoming message exchange
     */
    private String getUrl(final Exchange exchange) {
        final StringBuilder url = new StringBuilder();
        final String transformPath = getTransformPath(exchange);
        final HttpMethods method = getMethod(exchange);

        url.append(getBaseUrlWithScheme());
        url.append(getPathFromHeaders(exchange));

        if (!isBlank(transformPath)) {
            url.append(transformPath);
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
            final StringBuilder prefer = new StringBuilder("return=representation;");

            if (!isBlank(include)) {
                prefer.append(" include=\"" + addPreferNamespace(include) + "\";");
            }
            if (!isBlank(omit)) {
                prefer.append(" omit=\"" + addPreferNamespace(omit) + "\";");
            }
            return prefer.toString();
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

    private static InputStream extractResponseBodyAsStream(final InputStream is, final Exchange exchange) {
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
            } catch (IOException ex) {
                LOGGER.debug("Error extracting body from http request", ex);
                return null;
            } finally {
                IOHelper.close(is, "Extracting response body", LOGGER);
            }
        }
    }
}
