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

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;

/**
 * Represents a client to interact with Fedora's HTTP API.
 *
 * @author Aaron Coburn
 * @since October 20, 2014
 */
public class FcrepoClient {

    private static final String DESCRIBED_BY = "describedby";

    private static final String CONTENT_TYPE = "Content-Type";

    private CloseableHttpClient httpclient;

    private Boolean throwExceptionOnFailure = true;

    private static final Logger LOGGER = getLogger(FcrepoClient.class);

    /**
     * Create a FcrepoClient with a set of authentication values.
     * @param username the username for the repository
     * @param password the password for the repository
     * @param host the authentication hostname (realm) for the repository
     * @param throwExceptionOnFailure whether to throw an exception on any non-2xx or 3xx HTTP responses
     */
    public FcrepoClient(final String username, final String password, final String host,
            final Boolean throwExceptionOnFailure) {

        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        AuthScope scope = null;

        this.throwExceptionOnFailure = throwExceptionOnFailure;

        if (isBlank(username) || isBlank(password)) {
            this.httpclient = HttpClients.createDefault();
        } else {
            LOGGER.debug("Accessing fcrepo with user credentials");

            if (isBlank(host)) {
                scope = new AuthScope(AuthScope.ANY);
            } else {
                scope = new AuthScope(new HttpHost(host));
            }
            credsProvider.setCredentials(
                    scope,
                    new UsernamePasswordCredentials(username, password));
            this.httpclient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .build();
        }
    }

    /**
     * Make a HEAD response
     * @param url the URL of the resource to check
     * @return the repository response
     * @throws FcrepoOperationFailedException when the underlying HTTP request results in an error
     */
    public FcrepoResponse head(final URI url)
            throws FcrepoOperationFailedException {

        final HttpHead request = new HttpHead(url);
        final HttpResponse response = executeRequest(request);
        final int status = response.getStatusLine().getStatusCode();
        final String contentType = getContentTypeHeader(response);

        LOGGER.debug("Fcrepo HEAD request returned status [{}]", status);

        if ((status >= HttpStatus.SC_OK && status < HttpStatus.SC_BAD_REQUEST) || !this.throwExceptionOnFailure) {
            URI describedBy = null;
            final List<URI> links = getLinkHeaders(response, DESCRIBED_BY);
            if (links.size() == 1) {
                describedBy = links.get(0);
            }
            return new FcrepoResponse(url, status, contentType, describedBy, null);
        } else {
            throw new FcrepoOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase());
        }
    }

    /**
     * Make a PUT request
     * @param url the URL of the resource to PUT
     * @param body the contents of the resource to send
     * @param contentType the MIMEType of the resource
     * @return the repository response
     * @throws FcrepoOperationFailedException when the underlying HTTP request results in an error
     */
    public FcrepoResponse put(final URI url, final InputStream body, final String contentType)
            throws FcrepoOperationFailedException {

        final HttpPut request = new HttpPut(url);

        if (contentType != null) {
            request.addHeader(CONTENT_TYPE, contentType);
        }
        if (body != null) {
            request.setEntity(new InputStreamEntity(body));
        }

        LOGGER.debug("Fcrepo request headers: {}", request.getAllHeaders());

        final HttpResponse response = executeRequest(request);

        LOGGER.debug("Fcrepo PUT request returned status [{}]", response.getStatusLine().getStatusCode());

        return fcrepoGenericResponse(url, response, throwExceptionOnFailure);
    }

    /**
     * Make a PATCH request
     * Please note: the body should have an application/sparql-update content-type
     * @param url the URL of the resource to PATCH
     * @param body the body to be sent to the repository
     * @return the repository response
     * @throws FcrepoOperationFailedException when the underlying HTTP request results in an error
     */
    public FcrepoResponse patch(final URI url, final InputStream body)
            throws FcrepoOperationFailedException {

        final HttpPatch request = new HttpPatch(url);

        request.addHeader(CONTENT_TYPE, "application/sparql-update");
        request.setEntity(new InputStreamEntity(body));

        LOGGER.debug("Fcrepo request headers: {}", request.getAllHeaders());

        final HttpResponse response = executeRequest(request);

        LOGGER.debug("Fcrepo PATCH request returned status [{}]", response.getStatusLine().getStatusCode());

        return fcrepoGenericResponse(url, response, throwExceptionOnFailure);
    }

    /**
     * Make a POST request
     * @param url the URL of the resource to which to POST
     * @param body the content to be sent to the server
     * @param contentType the Content-Type of the body
     * @return the repository response
     * @throws FcrepoOperationFailedException when the underlying HTTP request results in an error
     */
    public FcrepoResponse post(final URI url, final InputStream body, final String contentType)
            throws FcrepoOperationFailedException {

        final HttpPost request = new HttpPost(url);

        if (contentType != null) {
            request.addHeader(CONTENT_TYPE, contentType);
        }
        if (body != null) {
            request.setEntity(new InputStreamEntity(body));
        }

        LOGGER.debug("Fcrepo request headers: {}", request.getAllHeaders());

        final HttpResponse response = executeRequest(request);

        LOGGER.debug("Fcrepo POST request returned status [{}]", response.getStatusLine().getStatusCode());

        return fcrepoGenericResponse(url, response, throwExceptionOnFailure);
    }

    /**
     * Make a DELETE request
     * @param url the URL of the resource to delete
     * @return the repository response
     * @throws FcrepoOperationFailedException when the underlying HTTP request results in an error
     */
    public FcrepoResponse delete(final URI url)
            throws FcrepoOperationFailedException {

        final HttpDelete request = new HttpDelete(url);

        final HttpResponse response = executeRequest(request);

        LOGGER.debug("Fcrepo DELETE request returned status [{}]", response.getStatusLine().getStatusCode());

        return fcrepoGenericResponse(url, response, throwExceptionOnFailure);
    }

    /**
     * Make a GET request
     * @param url the URL of the resource to fetch
     * @param accept the requested MIMEType of the resource to be retrieved
     * @param prefer the value for a prefer header sent in the request
     * @return the repository response
     * @throws FcrepoOperationFailedException when the underlying HTTP request results in an error
     */
    public FcrepoResponse get(final URI url, final String accept, final String prefer)
            throws FcrepoOperationFailedException {

        final HttpGet request = new HttpGet(url);

        if (accept != null) {
            request.setHeader("Accept", accept);
        }

        if (prefer != null) {
            request.setHeader("Prefer", prefer);
        }

        LOGGER.debug("Fcrepo request headers: {}", request.getAllHeaders());

        final HttpResponse response = executeRequest(request);
        final int status = response.getStatusLine().getStatusCode();
        final String contentType = getContentTypeHeader(response);

        LOGGER.debug("Fcrepo GET request returned status [{}]", status);

        if ((status >= HttpStatus.SC_OK && status < HttpStatus.SC_BAD_REQUEST) || !this.throwExceptionOnFailure) {
            URI describedBy = null;
            final List<URI> links = getLinkHeaders(response, DESCRIBED_BY);
            if (links.size() == 1) {
                describedBy = links.get(0);
            }
            return new FcrepoResponse(url, status, contentType, describedBy,
                    getEntityContent(response));
        } else {
            throw new FcrepoOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase());
        }
    }

    /**
     * Execute the HTTP request
     */
    private HttpResponse executeRequest(final HttpRequestBase request) throws FcrepoOperationFailedException {
        try {
            return httpclient.execute(request);
        } catch (IOException ex) {
            LOGGER.debug("HTTP Operation failed: ", ex);
            throw new FcrepoOperationFailedException(request.getURI(), -1, ex.getMessage());
        }
    }

    /**
     * Handle the general case with responses.
     */
    private FcrepoResponse fcrepoGenericResponse(final URI url, final HttpResponse response,
            final Boolean throwExceptionOnFailure) throws FcrepoOperationFailedException {
        final int status = response.getStatusLine().getStatusCode();
        final String contentTypeHeader = getContentTypeHeader(response);

        if ((status >= HttpStatus.SC_OK && status < HttpStatus.SC_BAD_REQUEST) || !throwExceptionOnFailure) {
            return new FcrepoResponse(url, status, contentTypeHeader, null, getEntityContent(response));
        } else {
            throw new FcrepoOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase());
        }
    }


    /**
     * Extract the response body as an input stream
     */
    private static InputStream getEntityContent(final HttpResponse response) {
        try {
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            } else {
                return entity.getContent();
            }
        } catch (IOException ex) {
            LOGGER.debug("Unable to extract HttpEntity response into an InputStream: ", ex);
            return null;
        }
    }

    /**
     * Extract the content-type header value
     */
    private static String getContentTypeHeader(final HttpResponse response) {
        final Header[] contentTypes = response.getHeaders(CONTENT_TYPE);
        if (contentTypes.length > 0) {
            return contentTypes[0].getValue();
        } else {
            return null;
        }
    }

    /**
     * Extract any Link headers
     */
    private static List<URI> getLinkHeaders(final HttpResponse response, final String relationship) {
        final List<URI> uris = new ArrayList<URI>();
        final Header[] links = response.getHeaders("Link");
        for (Header header: links) {
            final FcrepoLink link = new FcrepoLink(header.getValue());
            if (link.getRel().equals(relationship)) {
                uris.add(link.getUri());
            }
        }
        return uris;
    }
 }
