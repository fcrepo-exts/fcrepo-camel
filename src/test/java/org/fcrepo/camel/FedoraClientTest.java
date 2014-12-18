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
import static org.fcrepo.camel.TestUtils.baseUrl;
import static org.fcrepo.camel.TestUtils.rdfXml;
import static org.fcrepo.camel.TestUtils.setField;
import static org.fcrepo.camel.TestUtils.sparqlUpdate;
import static org.fcrepo.camel.TestUtils.RDF_XML;
import static org.fcrepo.camel.TestUtils.SPARQL_UPDATE;
import static org.fcrepo.camel.TestUtils.TEXT_TURTLE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.apache.camel.component.http4.HttpOperationFailedException;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author acoburn
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraClientTest {

    private FedoraClient testClient;

    @Mock
    private CloseableHttpClient mockHttpclient;

    @Mock
    private CloseableHttpResponse mockResponse;

    @Mock
    private StatusLine mockStatus;

    @Before
    public void setUp() throws IOException {
        testClient = new FedoraClient(null, null, null, true);
        setField(testClient, "httpclient", mockHttpclient);
    }

    @Test
    public void testGet() throws IOException, HttpOperationFailedException {
        final int status = 200;
        final URI uri = create(baseUrl);
        final ByteArrayEntity entity = new ByteArrayEntity(rdfXml.getBytes());
        entity.setContentType(RDF_XML);

        doSetupMockRequest(RDF_XML, entity, status);

        final FedoraResponse response = testClient.get(uri, RDF_XML);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), RDF_XML);
        assertEquals(response.getLocation(), null);
        assertEquals(IOUtils.toString(response.getBody()), rdfXml);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testGetError() throws Exception {
        final int status = 400;
        final URI uri = create(baseUrl);
        final ByteArrayEntity entity = new ByteArrayEntity(rdfXml.getBytes());
        entity.setContentType(RDF_XML);

        doSetupMockRequest(RDF_XML, entity, status);
        testClient.get(uri, RDF_XML);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testGet100() throws Exception {
        final int status = 100;
        final URI uri = create(baseUrl);
        final ByteArrayEntity entity = new ByteArrayEntity(rdfXml.getBytes());
        entity.setContentType(RDF_XML);

        doSetupMockRequest(RDF_XML, entity, status);
        testClient.get(uri, RDF_XML);
    }

    @Test
    public void testGet300() throws Exception {
        final int status = 300;
        final URI uri = create(baseUrl);
        final String redirect = baseUrl + "/bar";
        final Header linkHeader = new BasicHeader("Link", "<" + redirect + ">; rel=\"describedby\"");
        final Header[] headers = new Header[] { linkHeader };
        final CloseableHttpResponse mockResponse = doSetupMockRequest(RDF_XML, null, status);

        when(mockResponse.getHeaders("Link")).thenReturn(headers);

        final FedoraResponse response = testClient.get(uri, RDF_XML);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), RDF_XML);
        assertEquals(response.getLocation(), create(redirect));
        assertEquals(response.getBody(), null);
    }

    @Test
    public void testGetNoAccept() throws Exception {
        final int status = 200;
        final URI uri = create(baseUrl);

        doSetupMockRequest(RDF_XML, null, status);

        final FedoraResponse response = testClient.get(uri, null);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), RDF_XML);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test
    public void testHead() throws IOException, HttpOperationFailedException {
        final int status = 200;
        final URI uri = create(baseUrl);

        doSetupMockRequest(TEXT_TURTLE, null, status);

        final FedoraResponse response = testClient.head(uri);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), TEXT_TURTLE);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testHeadError() throws IOException, HttpOperationFailedException {
        doSetupMockRequest(TEXT_TURTLE, null, 404);
        testClient.head(create(baseUrl));
    }

    @Test
    public void testPut() throws IOException, HttpOperationFailedException {
        final int status = 204;
        final URI uri = create(baseUrl);
        final InputStream body = new ByteArrayInputStream(rdfXml.getBytes());

        doSetupMockRequest(RDF_XML, null, status);

        final FedoraResponse response = testClient.put(uri, body, RDF_XML);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), RDF_XML);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test
    public void testPutNoBody() throws IOException, HttpOperationFailedException {
        final int status = 204;
        final URI uri = create(baseUrl);

        doSetupMockRequest(null, null, status);

        final FedoraResponse response = testClient.put(uri, null, null);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), null);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test
    public void testPutWithResponseBody() throws IOException, HttpOperationFailedException {
        final int status = 201;
        final URI uri = create(baseUrl);

        doSetupMockRequest(null, new ByteArrayEntity(uri.toString().getBytes()), status);

        final FedoraResponse response = testClient.put(uri, null, null);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), null);
        assertEquals(response.getLocation(), null);
        assertEquals(IOUtils.toString(response.getBody()), uri.toString());
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testPutError() throws IOException, HttpOperationFailedException {
        final int status = 500;
        final URI uri = create(baseUrl);
        final InputStream body = new ByteArrayInputStream(rdfXml.getBytes());

        doSetupMockRequest(RDF_XML, null, status);
        testClient.put(uri, body, RDF_XML);
    }

    @Test
    public void testDelete() throws IOException, HttpOperationFailedException {
        final int status = 204;
        final URI uri = create(baseUrl);

        doSetupMockRequest(SPARQL_UPDATE, null, status);

        final FedoraResponse response = testClient.delete(uri);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), SPARQL_UPDATE);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test
    public void testDeleteWithResponseBody() throws IOException, HttpOperationFailedException {
        final int status = 204;
        final URI uri = create(baseUrl);
        final String responseText = "tombstone found";

        doSetupMockRequest(null, new ByteArrayEntity(responseText.getBytes()), status);

        final FedoraResponse response = testClient.delete(uri);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), null);
        assertEquals(response.getLocation(), null);
        assertEquals(IOUtils.toString(response.getBody()), responseText);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testDeleteError() throws IOException, HttpOperationFailedException {
        final int status = 401;
        final URI uri = create(baseUrl);

        doSetupMockRequest(SPARQL_UPDATE, null, status);
        testClient.delete(uri);
    }

    @Test
    public void testPatch() throws IOException, HttpOperationFailedException {
        final int status = 204;
        final URI uri = create(baseUrl);
        final InputStream body = new ByteArrayInputStream(sparqlUpdate.getBytes());

        doSetupMockRequest(SPARQL_UPDATE, null, status);

        final FedoraResponse response = testClient.patch(uri, body);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), SPARQL_UPDATE);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testPatchNoContent() throws IOException, HttpOperationFailedException {
        final int status = 204;
        final URI uri = create(baseUrl);

        doSetupMockRequest(SPARQL_UPDATE, null, status);
        testClient.patch(uri, null);
    }

    @Test
    public void testPatchResponseBody() throws IOException, HttpOperationFailedException {
        final int status = 204;
        final URI uri = create(baseUrl);
        final String responseText = "Sparql-update response";
        final InputStream body = new ByteArrayInputStream(sparqlUpdate.getBytes());

        doSetupMockRequest(SPARQL_UPDATE, new ByteArrayEntity(responseText.getBytes()), status);

        final FedoraResponse response = testClient.patch(uri, body);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), SPARQL_UPDATE);
        assertEquals(IOUtils.toString(response.getBody()), responseText);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testPatchError() throws IOException, HttpOperationFailedException {
        final int status = 415;
        final URI uri = create(baseUrl);
        final InputStream body = new ByteArrayInputStream(sparqlUpdate.getBytes());

        doSetupMockRequest(SPARQL_UPDATE, null, status);
        testClient.patch(uri, body);
    }

    @Test
    public void testPost() throws IOException, HttpOperationFailedException {
        final int status = 204;
        final URI uri = create(baseUrl);
        final InputStream body = new ByteArrayInputStream(sparqlUpdate.getBytes());

        doSetupMockRequest(SPARQL_UPDATE, null, status);

        final FedoraResponse response = testClient.post(uri, body, SPARQL_UPDATE);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), SPARQL_UPDATE);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test
    public void testPostResponseBody() throws IOException, HttpOperationFailedException {
        final int status = 204;
        final URI uri = create(baseUrl);
        final String responseText = baseUrl + "/bar";
        final InputStream body = new ByteArrayInputStream(sparqlUpdate.getBytes());

        doSetupMockRequest(SPARQL_UPDATE, new ByteArrayEntity(responseText.getBytes()), status);

        final FedoraResponse response = testClient.post(uri, body, SPARQL_UPDATE);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), SPARQL_UPDATE);
        assertEquals(response.getLocation(), null);
        assertEquals(IOUtils.toString(response.getBody()), responseText);
    }

    @Test
    public void testPostNoBody() throws IOException, HttpOperationFailedException {
        final int status = 204;
        final URI uri = create(baseUrl);
        final String responseText = baseUrl + "/bar";

        doSetupMockRequest(null, new ByteArrayEntity(responseText.getBytes()), status);

        final FedoraResponse response = testClient.post(uri, null, null);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), null);
        assertEquals(response.getLocation(), null);
        assertEquals(IOUtils.toString(response.getBody()), responseText);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testPostError() throws IOException, HttpOperationFailedException {
        final int status = 415;
        final URI uri = create(baseUrl);
        final InputStream body = new ByteArrayInputStream(sparqlUpdate.getBytes());

        doSetupMockRequest(SPARQL_UPDATE, null, status);
        testClient.post(uri, body, SPARQL_UPDATE);
    }

    @Test
    public void testPostErrorContentTypeHeader() throws IOException, HttpOperationFailedException {
        final int status = 401;
        final URI uri = create(baseUrl);
        final String response = "Response error";
        final InputStream body = new ByteArrayInputStream(sparqlUpdate.getBytes());
        final Header contentTypeHeader = new BasicHeader("Content-Type", SPARQL_UPDATE);
        final Header[] responseHeaders = new Header[]{ contentTypeHeader };
        final ByteArrayEntity responseBody = new ByteArrayEntity(response.getBytes());
        final CloseableHttpResponse mockResponse = doSetupMockRequest(SPARQL_UPDATE, responseBody, status);

        when(mockResponse.getAllHeaders()).thenReturn(responseHeaders);

        try {
            testClient.post(uri, body, SPARQL_UPDATE);
        } catch (HttpOperationFailedException ex) {
            assertEquals(ex.getUri(), uri.toString());
            assertEquals(ex.getResponseBody(), response);
            assertEquals(ex.getRedirectLocation(), null);
            assertEquals(ex.getStatusCode(), status);
            for (Map.Entry<String, String> entry : ex.getResponseHeaders().entrySet()) {
                assertEquals(entry.getKey(), "Content-Type");
                assertEquals(entry.getValue(), SPARQL_UPDATE);
            }
        }
    }

    @Test
    public void testPostErrorLocationHeader() throws IOException, HttpOperationFailedException {
        final int status = 401;
        final URI uri = create(baseUrl);
        final String redirect = baseUrl + "/bar";
        final String response = "Response error";
        final InputStream body = new ByteArrayInputStream(sparqlUpdate.getBytes());
        final ByteArrayEntity responseBody = new ByteArrayEntity(response.getBytes());
        final Header locationHeader = new BasicHeader("Location", redirect);
        final Header[] responseHeaders = new Header[]{ locationHeader };
        final CloseableHttpResponse mockResponse = doSetupMockRequest(SPARQL_UPDATE, responseBody, status);

        when(mockResponse.getAllHeaders()).thenReturn(responseHeaders);
        when(mockResponse.getFirstHeader("location")).thenReturn(new BasicHeader("Location", redirect));

        try {
            testClient.post(uri, body, SPARQL_UPDATE);
        } catch (HttpOperationFailedException ex) {
            assertEquals(ex.getUri(), uri.toString());
            assertEquals(ex.getResponseBody(), response);
            assertEquals(ex.getRedirectLocation(), redirect);
            assertEquals(ex.getStatusCode(), status);
            for (Map.Entry<String, String> entry : ex.getResponseHeaders().entrySet()) {
                assertEquals(entry.getKey(), "Location");
                assertEquals(entry.getValue(), redirect);
            }
        }
    }

    @Test
    public void testPostErrorNoResponseHeaders() throws IOException, HttpOperationFailedException {
        final int status = 401;
        final URI uri = create(baseUrl);
        final String response = "Response error";
        final InputStream body = new ByteArrayInputStream(sparqlUpdate.getBytes());
        final ByteArrayEntity responseBody = new ByteArrayEntity(response.getBytes());
        final Header contentTypeHeader = new BasicHeader("Content-Type", SPARQL_UPDATE);
        final Header[] responseHeaders = new Header[]{ contentTypeHeader };

        final CloseableHttpResponse mockResponse = doSetupMockRequest(SPARQL_UPDATE, responseBody, status);

        when(mockResponse.getAllHeaders()).thenReturn(null);

        try {
            testClient.post(uri, body, SPARQL_UPDATE);
        } catch (HttpOperationFailedException ex) {
            assertEquals(ex.getUri(), uri.toString());
            assertEquals(ex.getResponseBody(), response);
            assertEquals(ex.getRedirectLocation(), null);
            assertEquals(ex.getStatusCode(), status);
            assertEquals(ex.getResponseHeaders(), null);
        }
    }

    private CloseableHttpResponse doSetupMockRequest(final String contentType, final ByteArrayEntity entity,
            final int status) throws IOException {
        final Header contentTypeHeader = new BasicHeader("Content-Type", contentType);
        final Header[] contentTypeHeaders = new Header[]{ contentTypeHeader };
        final Header[] linkHeaders = new Header[]{};
        final Header[] responseHeaders = new Header[]{ contentTypeHeader };

        when(mockHttpclient.execute(any(HttpUriRequest.class))).thenReturn(mockResponse);
        when(mockResponse.getFirstHeader("location")).thenReturn(null);
        when(mockResponse.getHeaders("Content-Type")).thenReturn(contentTypeHeaders);
        when(mockResponse.getHeaders("Link")).thenReturn(linkHeaders);
        when(mockResponse.getEntity()).thenReturn(entity);
        when(mockResponse.getStatusLine()).thenReturn(mockStatus);
        when(mockStatus.getStatusCode()).thenReturn(status);

        return mockResponse;
    }
}
