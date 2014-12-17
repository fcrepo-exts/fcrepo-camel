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
import static org.fcrepo.camel.TestUtils.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        mockHttpclient = mock(CloseableHttpClient.class);
        testClient = new FedoraClient(null, null, null, true);
        setField(testClient, "httpclient", mockHttpclient);
    }

    @Test
    public void testGet() throws IOException, HttpOperationFailedException {
        final URI uri = create("http://localhost:8080/rest/foo");
        final String accept = "application/rdf+xml";
        final String triples = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
              "<rdf:Description rdf:about=\"http://localhost:8080/rest/foo\">" +
                "<mixinTypes xmlns=\"http://fedora.info/definitions/v4/repository#\" " +
                    "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">fedora:resource</mixinTypes>" +
              "</rdf:Description>" +
            "</rdf:RDF>";
        final ByteArrayEntity entity = new ByteArrayEntity(triples.getBytes());
        entity.setContentType(accept);

        final int status = 200;

        doSetupMockRequest(accept, entity, status);

        final FedoraResponse response = testClient.get(uri, accept);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), accept);
        assertEquals(response.getLocation(), null);
        assertEquals(IOUtils.toString(response.getBody()), triples);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testGetError() throws Exception {
        final URI uri = create("http://localhost:8080/rest/foo");
        final String accept = "application/rdf+xml";
        final String triples = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
              "<rdf:Description rdf:about=\"http://localhost:8080/rest/foo\">" +
                "<mixinTypes xmlns=\"http://fedora.info/definitions/v4/repository#\" " +
                    "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">fedora:resource</mixinTypes>" +
              "</rdf:Description>" +
            "</rdf:RDF>";
        final ByteArrayEntity entity = new ByteArrayEntity(triples.getBytes());
        entity.setContentType(accept);

        final int status = 400;

        doSetupMockRequest(accept, entity, status);
        testClient.get(uri, accept);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testGet100() throws Exception {
        final URI uri = create("http://localhost:8080/rest/foo");
        final String accept = "application/rdf+xml";
        final String triples = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
              "<rdf:Description rdf:about=\"http://localhost:8080/rest/foo\">" +
                "<mixinTypes xmlns=\"http://fedora.info/definitions/v4/repository#\" " +
                    "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">fedora:resource</mixinTypes>" +
              "</rdf:Description>" +
            "</rdf:RDF>";
        final ByteArrayEntity entity = new ByteArrayEntity(triples.getBytes());
        entity.setContentType(accept);

        final int status = 100;

        doSetupMockRequest(accept, entity, status);
        testClient.get(uri, accept);
    }

    @Test
    public void testGet300() throws Exception {
        final URI uri = create("http://localhost:8080/rest/foo");
        final String accept = "application/rdf+xml";

        final String redirect = "http://localhost:8080/rest/foo/bar";
        final int status = 300;
        final Header linkHeader = new BasicHeader("Link", "<" + redirect + ">; rel=\"describedby\"");
        final Header[] headers = new Header[] { linkHeader };

        final CloseableHttpResponse mockResponse = doSetupMockRequest(accept, null, status);

        when(mockResponse.getHeaders("Link")).thenReturn(headers);

        final FedoraResponse response = testClient.get(uri, accept);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), accept);
        assertEquals(response.getLocation(), create(redirect));
        assertEquals(response.getBody(), null);
    }

    @Test
    public void testGetNoAccept() throws Exception {
        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/rdf+xml";
        final int status = 200;

        doSetupMockRequest(contentType, null, status);

        final FedoraResponse response = testClient.get(uri, null);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }



    @Test
    public void testHead() throws IOException, HttpOperationFailedException {
        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "text/turtle";
        final int status = 200;

        doSetupMockRequest(contentType, null, status);

        final FedoraResponse response = testClient.head(uri);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testHeadError() throws IOException, HttpOperationFailedException {
        doSetupMockRequest("text/turtle", null, 404);
        testClient.head(create("http://localhost:8080/rest/foo"));
    }

    @Test
    public void testPut() throws IOException, HttpOperationFailedException {
        final URI uri = create("http://localhost:8080/rest/foo");
        final int status = 204;
        final String contentType = "application/rdf+xml";
        final String triples = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
              "<rdf:Description rdf:about=\"http://localhost:8080/rest/foo\">" +
                "<mixinTypes xmlns=\"http://fedora.info/definitions/v4/repository#\" " +
                    "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">fedora:resource</mixinTypes>" +
              "</rdf:Description>" +
            "</rdf:RDF>";
        final InputStream body = new ByteArrayInputStream(triples.getBytes());

        doSetupMockRequest(contentType, null, status);

        final FedoraResponse response = testClient.put(uri, body, contentType);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test
    public void testPutNoBody() throws IOException, HttpOperationFailedException {
        final URI uri = create("http://localhost:8080/rest/foo");
        final int status = 204;

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
        final URI uri = create("http://localhost:8080/rest/foo");
        final int status = 201;

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
        final URI uri = create("http://localhost:8080/rest/foo");
        final int status = 500;
        final String contentType = "application/rdf+xml";
        final String triples = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
              "<rdf:Description rdf:about=\"http://localhost:8080/rest/foo\">" +
                "<mixinTypes xmlns=\"http://fedora.info/definitions/v4/repository#\" " +
                    "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">fedora:resource</mixinTypes>" +
              "</rdf:Description>" +
            "</rdf:RDF>";
        final InputStream body = new ByteArrayInputStream(triples.getBytes());

        doSetupMockRequest(contentType, null, status);
        testClient.put(uri, body, contentType);
    }

    @Test
    public void testDelete() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 204;

        doSetupMockRequest(contentType, null, status);

        final FedoraResponse response = testClient.delete(uri);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test
    public void testDeleteWithResponseBody() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final String responseText = "tombstone found";
        final int status = 204;

        doSetupMockRequest(contentType, new ByteArrayEntity(responseText.getBytes()), status);

        final FedoraResponse response = testClient.delete(uri);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(IOUtils.toString(response.getBody()), responseText);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testDeleteError() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 401;

        doSetupMockRequest(contentType, null, status);
        testClient.delete(uri);
    }

    @Test
    public void testPatch() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 204;
        final String sparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT {\n" +
              "<> dc:title \"Foo\" .\n" +
            "} WHERE {}";
        final InputStream body = new ByteArrayInputStream(sparql.getBytes());

        doSetupMockRequest(contentType, null, status);

        final FedoraResponse response = testClient.patch(uri, body);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testPatchNoContent() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 204;

        doSetupMockRequest(contentType, null, status);
        testClient.patch(uri, null);
    }

    @Test
    public void testPatchResponseBody() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 204;
        final String responseText = "Sparql-update response";
        final String sparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT {\n" +
              "<> dc:title \"Foo\" .\n" +
            "} WHERE {}";
        final InputStream body = new ByteArrayInputStream(sparql.getBytes());


        doSetupMockRequest(contentType, new ByteArrayEntity(responseText.getBytes()), status);
        final FedoraResponse response = testClient.patch(uri, body);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(IOUtils.toString(response.getBody()), responseText);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testPatchError() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 415;
        final String sparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT {\n" +
              "<> dc:title \"Foo\" .\n" +
            "} WHERE {}";
        final InputStream body = new ByteArrayInputStream(sparql.getBytes());

        doSetupMockRequest(contentType, null, status);
        testClient.patch(uri, body);
    }


    @Test
    public void testPost() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 204;
        final String sparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT {\n" +
              "<> dc:title \"Foo\" .\n" +
            "} WHERE {}";
        final InputStream body = new ByteArrayInputStream(sparql.getBytes());

        doSetupMockRequest(contentType, null, status);

        final FedoraResponse response = testClient.post(uri, body, contentType);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test
    public void testPostResponseBody() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 204;
        final String sparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT {\n" +
              "<> dc:title \"Foo\" .\n" +
            "} WHERE {}";
        final String responseText = "http://localhost:8080/rest/foo/bar";
        final InputStream body = new ByteArrayInputStream(sparql.getBytes());

        doSetupMockRequest(contentType, new ByteArrayEntity(responseText.getBytes()), status);

        final FedoraResponse response = testClient.post(uri, body, contentType);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(IOUtils.toString(response.getBody()), responseText);
    }

    @Test
    public void testPostNoBody() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final int status = 204;
        final String responseText = "http://localhost:8080/rest/foo/bar";

        doSetupMockRequest(null, null, status);

        final FedoraResponse response = testClient.post(uri, null, null);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), null);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }


    @Test (expected = HttpOperationFailedException.class)
    public void testPostError() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 415;
        final String sparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT {\n" +
              "<> dc:title \"Foo\" .\n" +
            "} WHERE {}";
        final InputStream body = new ByteArrayInputStream(sparql.getBytes());

        doSetupMockRequest(contentType, null, status);
        testClient.post(uri, body, contentType);
    }

    @Test
    public void testPostErrorContentTypeHeader() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 401;
        final String sparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT {\n" +
              "<> dc:title \"Foo\" .\n" +
            "} WHERE {}";
        final String response = "Response error";
        final InputStream body = new ByteArrayInputStream(sparql.getBytes());
        final Header contentTypeHeader = new BasicHeader("Content-Type", contentType);
        final Header[] responseHeaders = new Header[]{ contentTypeHeader };
        final ByteArrayEntity responseBody = new ByteArrayEntity(response.getBytes());
        final CloseableHttpResponse mockResponse = doSetupMockRequest(contentType, responseBody, status);

        when(mockResponse.getAllHeaders()).thenReturn(responseHeaders);

        try {
            testClient.post(uri, body, contentType);
        } catch (HttpOperationFailedException ex) {
            assertEquals(ex.getUri(), uri.toString());
            assertEquals(ex.getResponseBody(), response);
            assertEquals(ex.getRedirectLocation(), null);
            assertEquals(ex.getStatusCode(), status);
            for (Map.Entry<String, String> entry : ex.getResponseHeaders().entrySet()) {
                assertEquals(entry.getKey(), "Content-Type");
                assertEquals(entry.getValue(), contentType);
            }
        }
    }

    @Test
    public void testPostErrorLocationHeader() throws IOException, HttpOperationFailedException {

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 401;
        final String redirect = "http://localhost:8080/rest/foo/bar";
        final String sparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT {\n" +
              "<> dc:title \"Foo\" .\n" +
            "} WHERE {}";
        final String response = "Response error";
        final InputStream body = new ByteArrayInputStream(sparql.getBytes());
        final ByteArrayEntity responseBody = new ByteArrayEntity(response.getBytes());
        final Header locationHeader = new BasicHeader("Location", redirect);
        final Header[] responseHeaders = new Header[]{ locationHeader };
        final CloseableHttpResponse mockResponse = doSetupMockRequest(contentType, responseBody, status);

        when(mockResponse.getAllHeaders()).thenReturn(responseHeaders);
        when(mockResponse.getFirstHeader("location")).thenReturn(new BasicHeader("Location", redirect));

        try {
            testClient.post(uri, body, contentType);
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

        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 401;
        final String sparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT {\n" +
              "<> dc:title \"Foo\" .\n" +
            "} WHERE {}";
        final String response = "Response error";
        final InputStream body = new ByteArrayInputStream(sparql.getBytes());
        final ByteArrayEntity responseBody = new ByteArrayEntity(response.getBytes());
        final Header contentTypeHeader = new BasicHeader("Content-Type", contentType);
        final Header[] responseHeaders = new Header[]{ contentTypeHeader };

        final CloseableHttpResponse mockResponse = doSetupMockRequest(contentType, responseBody, status);

        when(mockResponse.getAllHeaders()).thenReturn(null);
        try {
            testClient.post(uri, body, contentType);
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
        final StatusLine mockStatus = mock(StatusLine.class);
        final Header contentTypeHeader = new BasicHeader("Content-Type", contentType);
        final Header[] contentTypeHeaders = new Header[]{ contentTypeHeader };
        final Header[] linkHeaders = new Header[]{};
        final Header[] responseHeaders = new Header[]{ contentTypeHeader };
        final CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);

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
