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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static java.net.URI.create;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.camel.component.http4.HttpOperationFailedException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.junit.Before;
import org.mockito.Mock;

/**
 * @author acoburn
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraClientTest {
    
    private FedoraClient testClient;
    
    @Mock
    private HttpClient mockHttpclient;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        mockHttpclient = mock(HttpClient.class);
        testClient = new FedoraClient(mockHttpclient, true);
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

        doSetupMockRequest(new HttpGet(uri), accept, entity, status);

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

        doSetupMockRequest(new HttpGet(), accept, entity, status);
        testClient.get(uri, accept);
    }

    @Test
    public void testHead() throws IOException, HttpOperationFailedException {
        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "text/turtle";
        final int status = 200;

        doSetupMockRequest(new HttpHead(), contentType, null, status);

        final FedoraResponse response = testClient.head(uri);
        
        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }
    
    @Test (expected = HttpOperationFailedException.class)
    public void testHeadError() throws IOException, HttpOperationFailedException {
        doSetupMockRequest(new HttpHead(), "text/turtle", null, 404);
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

        doSetupMockRequest(new HttpPut(), contentType, null, status);

        final FedoraResponse response = testClient.put(uri, body, contentType);
        
        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
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

        doSetupMockRequest(new HttpPut(), contentType, null, status);
        testClient.put(uri, body, contentType);
    }

    @Test
    public void testDelete() throws IOException, HttpOperationFailedException {
        
        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 204;

        doSetupMockRequest(new HttpPost(), contentType, null, status);

        final FedoraResponse response = testClient.delete(uri);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
    }

    @Test (expected = HttpOperationFailedException.class)
    public void testDeleteError() throws IOException, HttpOperationFailedException {
        
        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/sparql-update";
        final int status = 401;

        doSetupMockRequest(new HttpDelete(), contentType, null, status);
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

        doSetupMockRequest(new HttpPost(), contentType, null, status);

        final FedoraResponse response = testClient.patch(uri, body);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), null);
        assertEquals(response.getBody(), null);
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

        doSetupMockRequest(new HttpPost(), contentType, null, status);
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

        doSetupMockRequest(new HttpPost(), contentType, null, status);

        final FedoraResponse response = testClient.post(uri, body, contentType);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
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

        doSetupMockRequest(new HttpPost(), contentType, null, status);
        testClient.post(uri, body, contentType);
    }

    private void doSetupMockRequest(final HttpUriRequest mockRequest, final String contentType,
            final ByteArrayEntity entity, final int status) throws IOException {
        final StatusLine mockStatus = mock(StatusLine.class);
        final Header contentTypeHeader = new BasicHeader("Content-Type", contentType);
        final Header[] contentTypeHeaders = new Header[]{ contentTypeHeader };
        final Header[] linkHeaders = new Header[]{};
        final HttpResponse mockResponse = mock(HttpResponse.class);

        when(mockHttpclient.execute(any(mockRequest.getClass()))).thenReturn(mockResponse);

        when(mockResponse.getFirstHeader("location")).thenReturn(null);
        when(mockResponse.getHeaders("Content-Type")).thenReturn(contentTypeHeaders);
        when(mockResponse.getHeaders("Link")).thenReturn(linkHeaders);
        when(mockResponse.getEntity()).thenReturn(entity);
        when(mockResponse.getStatusLine()).thenReturn(mockStatus);
        when(mockStatus.getStatusCode()).thenReturn(status);
    }
}
