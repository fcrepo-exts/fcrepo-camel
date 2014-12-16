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
import static org.apache.camel.component.http4.HttpMethods.DELETE;
import static org.apache.camel.component.http4.HttpMethods.GET;
import static org.apache.camel.component.http4.HttpMethods.HEAD;
import static org.apache.camel.component.http4.HttpMethods.PATCH;
import static org.apache.camel.component.http4.HttpMethods.POST;
import static org.fcrepo.camel.TestUtils.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author acoburn
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraProducerTest {

    private FedoraEndpoint testEndpoint;

    private FedoraProducer testProducer;

    private Exchange testExchange;

    @Mock
    private FedoraComponent mockComponent;

    @Mock
    private FedoraClient mockClient;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        mockComponent = mock(FedoraComponent.class);
        testEndpoint = new FedoraEndpoint("fcrepo:localhost:8080", "/rest", mockComponent);
        testEndpoint.setBaseUrl("localhost:8080/rest");
        mockClient = mock(FedoraClient.class);
        testExchange = new DefaultExchange(new DefaultCamelContext());
        testExchange.getIn().setBody(null);
    }

    public void init() throws IOException {
        testProducer = new FedoraProducer(testEndpoint);
        setField(testProducer, "client", mockClient);
    }

    @Test
    public void testGetProducer() throws Exception {
        init();
        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "application/rdf+xml";
        final String triples = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
              "<rdf:Description rdf:about=\"http://localhost:8080/rest/foo\">" +
                "<mixinTypes xmlns=\"http://fedora.info/definitions/v4/repository#\" " +
                    "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">fedora:resource</mixinTypes>" +
              "</rdf:Description>" +
            "</rdf:RDF>";
        final ByteArrayInputStream body = new ByteArrayInputStream(triples.getBytes());

        final FedoraResponse headResponse = new FedoraResponse(uri, 200, contentType, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, 200, contentType, null, body);

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(contentType))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), triples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), contentType);
    }

    @Test
    public void testGetAcceptHeaderProducer() throws Exception {
        init();
        final String accept = "application/n-triples";
        final URI uri = create("http://localhost:8080/rest/foo");
        final String triples = "<http://localhost:8080/rest/foo> " +
                               "<http://fedora.info/definitions/v4/repository#mixinTypes> " +
                               "\"fedora:resource\"";
        final ByteArrayInputStream body = new ByteArrayInputStream(triples.getBytes());

        final FedoraResponse headResponse = new FedoraResponse(uri, 200, "application/rdf+xml", null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, 200, accept, null, body);

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader("Accept", accept);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(accept))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), triples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), accept);
    }

    @Test
    public void testGetAcceptEndpointProducer() throws Exception {
        final String accept = "application/n-triples";
        testEndpoint.setAccept(accept);
        init();
        final URI uri = create("http://localhost:8080/rest/foo");
        final String triples = "<http://localhost:8080/rest/foo> " +
                               "<http://fedora.info/definitions/v4/repository#mixinTypes> " +
                               "\"fedora:resource\"";
        final ByteArrayInputStream body = new ByteArrayInputStream(triples.getBytes());

        final FedoraResponse headResponse = new FedoraResponse(uri, 200, "application/rdf+xml", null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, 200, accept, null, body);

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(accept))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), triples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), accept);
    }


    @Test
    public void testGetRootProducer() throws Exception {
        init();
        final URI uri = create("http://localhost:8080/rest");
        final String contentType = "application/rdf+xml";
        final String triples = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
              "<rdf:Description rdf:about=\"http://localhost:8080/rest\">" +
                "<mixinTypes xmlns=\"http://fedora.info/definitions/v4/repository#\" " +
                    "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">fedora:resource</mixinTypes>" +
              "</rdf:Description>" +
            "</rdf:RDF>";
        final ByteArrayInputStream body = new ByteArrayInputStream(triples.getBytes());

        final FedoraResponse headResponse = new FedoraResponse(uri, 200, contentType, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, 200, contentType, null, body);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(contentType))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), triples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), contentType);
    }



    @Test
    public void testGetBinaryProducer() throws Exception {
        testEndpoint.setMetadata(false);
        init();
        final URI uri = create("http://localhost:8080/rest/foo");
        final String contentType = "text/plain";
        final String content = "Foo";
        final ByteArrayInputStream body = new ByteArrayInputStream(content.getBytes());

        final FedoraResponse getResponse = new FedoraResponse(uri, 200, contentType, null, body);


        testExchange.getIn().setHeader("org.fcrepo.jms.identifier", "/foo");
        testExchange.getIn().setHeader(Exchange.ACCEPT_CONTENT_TYPE, contentType);
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, GET);

        when(mockClient.get(any(URI.class), eq(contentType))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), content);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), contentType);
    }

    @Test
    public void testHeadProducer() throws Exception {
        init();
        final URI uri = create("http://localhost:8080/rest/foo");
        final URI metadata = create("http://localhost:8080/rest/foo/bar");
        final String contentType = "application/rdf+xml";
        final int status = 200;
        final String triples = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
              "<rdf:Description rdf:about=\"http://localhost:8080/rest/foo\">" +
                "<mixinTypes xmlns=\"http://fedora.info/definitions/v4/repository#\" " +
                    "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">fedora:resource</mixinTypes>" +
              "</rdf:Description>" +
            "</rdf:RDF>";
        final ByteArrayInputStream body = new ByteArrayInputStream(triples.getBytes());

        final FedoraResponse headResponse = new FedoraResponse(uri, status, contentType, metadata, null);

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HEAD);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), contentType);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testDeleteProducer() throws Exception {
        init();
        final URI uri = create("http://localhost:8080/rest/foo");
        final int status = 204;

        final FedoraResponse deleteResponse = new FedoraResponse(uri, status, null, null, null);

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, DELETE);

        when(mockClient.delete(any(URI.class))).thenReturn(deleteResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testDeleteTombstoneProducer() throws Exception {
        testEndpoint.setTombstone(true);
        init();
        final URI uri = create("http://localhost:8080/rest/foo");
        final URI tombstone = create("http://localhost:8080/rest/foo/fcr:tombstone");
        final int status = 204;

        final FedoraResponse deleteResponse = new FedoraResponse(tombstone, status, null, null, null);

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, DELETE);

        when(mockClient.delete(tombstone)).thenReturn(deleteResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformGetProducer() throws Exception {
        testEndpoint.setTransform("default");
        init();

        final String url = "http://localhost:8080/rest/foo";
        final URI uri = create(url);
        final String contentType = "application/json";
        final String responseText = "[{\"title\": \"Test title\"}]";
        final int status = 200;

        final FedoraResponse headResponse = new FedoraResponse(uri, 200, contentType, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, status, contentType, null,
                new ByteArrayInputStream(responseText.getBytes()));

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, GET);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(url + "/fcr:transform/default"), contentType)).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), responseText);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), contentType);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformPostProducer() throws Exception {
        testEndpoint.setTransform("default");
        init();

        final String url = "http://localhost:8080/rest/foo";
        final URI uri = create(url);
        final String contentType = "application/rdf+ldpath";
        final String accept = "application/json";
        final String ldpathText = "@prefix dc : <http://purl.org/dc/elements/1.1/>\n" +
                            "title = dc:title :: xsd:string;";
        final ByteArrayInputStream body = new ByteArrayInputStream(ldpathText.getBytes());
        final String responseText = "[{\"title\": \"Test title\"}]";
        final int status = 200;

        final FedoraResponse headResponse = new FedoraResponse(uri, 200, accept, null, null);
        final FedoraResponse postResponse = new FedoraResponse(uri, status, accept, null,
                new ByteArrayInputStream(responseText.getBytes()));

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, POST);
        testExchange.getIn().setHeader(Exchange.CONTENT_TYPE, contentType);
        testExchange.getIn().setBody(body);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.post(create(url + "/fcr:transform"), body, contentType)).thenReturn(postResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), responseText);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), accept);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }


    @Test
    public void testTransformProducer() throws Exception {
        testEndpoint.setTransform("default");
        init();

        final String url = "http://localhost:8080/rest/foo";
        final URI uri = create(url);
        final String contentType = "application/json";
        final String responseText = "[{\"title\": \"Test title\"}]";
        final int status = 200;

        final FedoraResponse headResponse = new FedoraResponse(uri, 200, contentType, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, status, contentType, null,
                new ByteArrayInputStream(responseText.getBytes()));

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(url + "/fcr:transform/default"), contentType)).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), responseText);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), contentType);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);

    }

    @Test
    public void testPostProducer() throws Exception {
        init();
        final URI uri = create("http://localhost:8080/rest/foo");
        final String responseText = "http://localhost:8080/fcrepo4/rest/foo/e8/0b/ab/e80bab60";
        final String contentType = "text/plain";
        final int status = 201;

        final FedoraResponse postResponse = new FedoraResponse(uri, status, contentType, null,
                new ByteArrayInputStream(responseText.getBytes()));

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, POST);

        when(mockClient.post(any(URI.class), any(InputStream.class), any(String.class))).thenReturn(postResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), responseText);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), contentType);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPostContentTypeEndpointProducer() throws Exception {
        final String contentType = "application/sparql-update";
        testEndpoint.setContentType(contentType);
        init();
        final URI uri = create("http://localhost:8080/rest/foo");
        final String sparqlUpdate = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n" +
                "INSERT { <> dc:title \"test title\" }\n" +
                "WHERE { }";
        final int status = 204;

        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse postResponse = new FedoraResponse(uri, status, null, null, null);

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, POST);
        testExchange.getIn().setBody(new ByteArrayInputStream(sparqlUpdate.getBytes()));

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.post(any(URI.class), any(InputStream.class), eq(contentType))).thenReturn(postResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPatchProducer() throws Exception {
        init();
        final URI uri = create("http://localhost:8080/rest/foo");
        final URI metadata = create("http://localhost:8080/rest/foo/bar");
        final String contentType = "application/n-triples";
        final String sparqlUpdate = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n" +
                "INSERT { <> dc:title \"test title\" }\n" +
                "WHERE { }";
        final int status = 204;

        final FedoraResponse headResponse = new FedoraResponse(uri, 200, contentType, metadata, null);
        final FedoraResponse patchResponse = new FedoraResponse(uri, status, null, null, null);

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, PATCH);
        testExchange.getIn().setBody(new ByteArrayInputStream(sparqlUpdate.getBytes()));

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.patch(any(URI.class), any(InputStream.class))).thenReturn(patchResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPatchTransformEnabledProducer() throws Exception {
        testEndpoint.setTransform("default");
        init();
        final URI uri = create("http://localhost:8080/rest/foo");
        final URI metadata = create("http://localhost:8080/rest/foo/bar");
        final String contentType = "application/n-triples";
        final String sparqlUpdate = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n" +
                "INSERT { <> dc:title \"test title\" }\n" +
                "WHERE { }";
        final int status = 204;

        final FedoraResponse headResponse = new FedoraResponse(uri, 200, contentType, metadata, null);
        final FedoraResponse patchResponse = new FedoraResponse(uri, status, null, null, null);

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, PATCH);
        testExchange.getIn().setBody(new ByteArrayInputStream(sparqlUpdate.getBytes()));

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.patch(any(URI.class), any(InputStream.class))).thenReturn(patchResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }


    @Test
    public void testPutProducer() throws Exception {
        init();
        final String url = "http://localhost:8080/rest/foo";
        final URI uri = create(url);
        final String contentType = "text/plain";
        final int status = 201;

        final FedoraResponse patchResponse = new FedoraResponse(uri, status, contentType, null,
                new ByteArrayInputStream(url.getBytes()));

        testExchange.getIn().setHeader("FCREPO_IDENTIFIER", "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, "PUT");
        testExchange.getIn().setBody(null);

        when(mockClient.put(any(URI.class), any(InputStream.class), any(String.class))).thenReturn(patchResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), url);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), contentType);
    }



}
