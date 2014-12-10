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

import static java.net.URI.create;
import static org.apache.camel.Exchange.ACCEPT_CONTENT_TYPE;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.component.http4.HttpMethods.DELETE;
import static org.apache.camel.component.http4.HttpMethods.GET;
import static org.apache.camel.component.http4.HttpMethods.POST;
import static org.fcrepo.camel.FedoraEndpoint.DEFAULT_CONTENT_TYPE;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_TRANSFORM;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
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
public class FedoraProducer extends DefaultProducer {
    private static final Logger LOGGER = getLogger(FedoraProducer.class);

    private FedoraEndpoint endpoint;

    private FedoraClient client;

    /**
     * Create a FedoraProducer object
     *
     * @param endpoint the FedoraEndpoint corresponding to the exchange.
     */
    public FedoraProducer(final FedoraEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
        this.client = new FedoraClient(
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

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Fcrepo Request [{}] with method [{}]", url, method);
        }

        FedoraResponse response;

        switch (method) {
        case PATCH:
            response = client.patch(getMetadataUri(url), in.getBody(InputStream.class));
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
            break;
        case PUT:
            response = client.put(create(url), in.getBody(InputStream.class), contentType);
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
            break;
        case POST:
            response = client.post(create(url), in.getBody(InputStream.class), contentType);
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
            break;
        case DELETE:
            response = client.delete(create(url));
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
            break;
        case HEAD:
            response = client.head(create(url));
            exchange.getIn().setBody(null);
            break;
        case GET:
        default:
            response = client.get(endpoint.getMetadata() ? getMetadataUri(url) : create(url), accept);
            exchange.getIn().setBody(extractResponseBodyAsStream(response.getBody(), exchange));
        }
        exchange.getIn().setHeader(CONTENT_TYPE, response.getContentType());
        exchange.getIn().setHeader(HTTP_RESPONSE_CODE, response.getStatusCode());
    }

    /**
     *
     */
    protected URI getMetadataUri(final String url)
            throws HttpOperationFailedException, IOException {
        final FedoraResponse headResponse = client.head(create(url));
        if (headResponse.getLocation() != null) {
            return headResponse.getLocation();
        } else {
            return create(url);
        }
    }

    /**
     * Given an exchange, determine which HTTP method to use. Basically, use GET unless the value of the
     * Exchange.HTTP_METHOD header is defined. Unlike the http4: component, the request does not use POST if there is
     * a message body defined. This is so in order to avoid inadvertant changes to the repository.
     *
     * @param exchange the incoming message exchange
     */
    protected HttpMethods getMethod(final Exchange exchange) {
        final HttpMethods method = exchange.getIn().getHeader(HTTP_METHOD, HttpMethods.class);
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
    protected String getContentType(final Exchange exchange) {
        final String contentTypeString = ExchangeHelper.getContentType(exchange);
        if (endpoint.getContentType() != null) {
            return endpoint.getContentType();
        } else if (contentTypeString != null) {
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
    protected String getAccept(final Exchange exchange) {
        final Message in = exchange.getIn();
        final String fcrepoTransform = in.getHeader(FCREPO_TRANSFORM, String.class);

        if (endpoint.getTransform() != null || (fcrepoTransform != null && !fcrepoTransform.isEmpty()) ) {
            return "application/json";
        } else if (endpoint.getAccept() != null) {
            return endpoint.getAccept();
        } else if (in.getHeader(ACCEPT_CONTENT_TYPE, String.class) != null) {
            return in.getHeader(ACCEPT_CONTENT_TYPE, String.class);
        } else if (in.getHeader("Accept", String.class) != null) {
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
    protected String getUrl(final Exchange exchange) {
        final Message in = exchange.getIn();
        final HttpMethods method = exchange.getIn().getHeader(HTTP_METHOD, HttpMethods.class);
        final URI baseUri = create(endpoint.getBaseUrl());
        final String fcrepoTransform = in.getHeader(FCREPO_TRANSFORM, String.class);
        final StringBuilder url = new StringBuilder("http://" + baseUri);
        if (in.getHeader(FCREPO_IDENTIFIER) != null) {
            url.append(in.getHeader(FCREPO_IDENTIFIER, String.class));
        } else if (in.getHeader(IDENTIFIER_HEADER_NAME) != null) {
            url.append(in.getHeader(IDENTIFIER_HEADER_NAME, String.class));
        }
        if (endpoint.getTransform() != null || (fcrepoTransform != null && !fcrepoTransform.isEmpty())) {
            if (method == POST) {
                url.append("/fcr:transform");
            } else if (method == null || method == GET) {
                if (fcrepoTransform != null && !fcrepoTransform.isEmpty()) {
                    url.append("/fcr:transform/" + fcrepoTransform);
                } else {
                    url.append("/fcr:transform/" + endpoint.getTransform());
                }
            }
        } else if (method == DELETE && endpoint.getTombstone()) {
            url.append("/fcr:tombstone");
        }
        return url.toString();
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
