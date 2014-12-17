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

import static org.fcrepo.camel.TestUtils.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static java.net.URI.create;

import java.io.IOException;
import java.net.URI;

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
public class FedoraClientAuthTest {

    private FedoraClient testClient;

    @Mock
    private CloseableHttpClient mockHttpclient;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
    }

    @Test
    public void testAuth1() throws IOException, HttpOperationFailedException {
        mockHttpclient = mock(CloseableHttpClient.class);
        testClient = new FedoraClient("user", "pass", null, true);
        setField(testClient, "httpclient", mockHttpclient);

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

    @Test
    public void testAuth2() throws IOException, HttpOperationFailedException {
        mockHttpclient = mock(CloseableHttpClient.class);
        testClient = new FedoraClient("user", "pass", "localhost", true);
        setField(testClient, "httpclient", mockHttpclient);

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

    @Test
    public void testAuth3() throws IOException, HttpOperationFailedException {
        mockHttpclient = mock(CloseableHttpClient.class);
        testClient = new FedoraClient("user", null, null, true);
        setField(testClient, "httpclient", mockHttpclient);

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

    private void doSetupMockRequest(final String contentType, final ByteArrayEntity entity, final int status)
            throws IOException {
        final StatusLine mockStatus = mock(StatusLine.class);
        final Header contentTypeHeader = new BasicHeader("Content-Type", contentType);
        final Header[] contentTypeHeaders = new Header[]{ contentTypeHeader };
        final Header[] linkHeaders = new Header[]{};
        final CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);

        when(mockHttpclient.execute(any(HttpUriRequest.class))).thenReturn(mockResponse);
        when(mockResponse.getFirstHeader("location")).thenReturn(null);
        when(mockResponse.getHeaders("Content-Type")).thenReturn(contentTypeHeaders);
        when(mockResponse.getHeaders("Link")).thenReturn(linkHeaders);
        when(mockResponse.getEntity()).thenReturn(entity);
        when(mockResponse.getStatusLine()).thenReturn(mockStatus);
        when(mockStatus.getStatusCode()).thenReturn(status);
    }
}
