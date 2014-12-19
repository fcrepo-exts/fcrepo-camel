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
import static org.apache.camel.component.http4.HttpMethods.PUT;
import static org.fcrepo.camel.TestUtils.baseUrl;
import static org.fcrepo.camel.TestUtils.rdfTriples;
import static org.fcrepo.camel.TestUtils.rdfXml;
import static org.fcrepo.camel.TestUtils.serializedJson;
import static org.fcrepo.camel.TestUtils.setField;
import static org.fcrepo.camel.TestUtils.sparqlUpdate;
import static org.fcrepo.camel.TestUtils.JSON;
import static org.fcrepo.camel.TestUtils.N_TRIPLES;
import static org.fcrepo.camel.TestUtils.RDF_LDPATH;
import static org.fcrepo.camel.TestUtils.RDF_XML;
import static org.fcrepo.camel.TestUtils.SPARQL_UPDATE;
import static org.fcrepo.camel.TestUtils.TEXT_PLAIN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private FedoraClient mockClient;

    @Before
    public void setUp() throws IOException {
        final FedoraComponent mockComponent = mock(FedoraComponent.class);

        testEndpoint = new FedoraEndpoint("fcrepo:localhost:8080", "/rest", mockComponent);
        testEndpoint.setBaseUrl("localhost:8080/rest");
        testExchange = new DefaultExchange(new DefaultCamelContext());
        testExchange.getIn().setBody(null);
    }

    public void init() throws IOException {
        testProducer = new FedoraProducer(testEndpoint);
        setField(testProducer, "client", mockClient);
    }

    @Test
    public void testGetProducer() throws Exception {
        final URI uri = create(baseUrl);
        final int status = 200;
        final ByteArrayInputStream body = new ByteArrayInputStream(rdfXml.getBytes());
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, status, RDF_XML, null, body);

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(RDF_XML))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), rdfXml);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), RDF_XML);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testGetAcceptHeaderProducer() throws Exception {
        final URI uri = create(baseUrl);
        final ByteArrayInputStream body = new ByteArrayInputStream(rdfTriples.getBytes());
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, 200, N_TRIPLES, null, body);

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader("Accept", N_TRIPLES);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(N_TRIPLES))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), rdfTriples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), N_TRIPLES);
    }

    @Test
    public void testGetAcceptEndpointProducer() throws Exception {
        final URI uri = create(baseUrl);
        final ByteArrayInputStream body = new ByteArrayInputStream(rdfTriples.getBytes());
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, 200, N_TRIPLES, null, body);

        testEndpoint.setAccept(N_TRIPLES);

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(N_TRIPLES))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), rdfTriples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), N_TRIPLES);
    }

    @Test
    public void testGetRootProducer() throws Exception {
        final URI uri = create("http://localhost:8080/rest");
        final ByteArrayInputStream body = new ByteArrayInputStream(rdfXml.getBytes());
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, 200, RDF_XML, null, body);

        init();

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(RDF_XML))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), rdfXml);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), RDF_XML);
    }

    @Test
    public void testGetBinaryProducer() throws Exception {
        final URI uri = create(baseUrl);
        final String content = "Foo";
        final ByteArrayInputStream body = new ByteArrayInputStream(content.getBytes());
        final FedoraResponse getResponse = new FedoraResponse(uri, 200, TEXT_PLAIN, null, body);

        testEndpoint.setMetadata(false);

        init();

        testExchange.getIn().setHeader("org.fcrepo.jms.identifier", "/foo");
        testExchange.getIn().setHeader(Exchange.ACCEPT_CONTENT_TYPE, TEXT_PLAIN);
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, GET);

        when(mockClient.get(any(URI.class), eq(TEXT_PLAIN))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), content);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TEXT_PLAIN);
    }

    @Test
    public void testHeadProducer() throws Exception {
        final URI uri = create(baseUrl);
        final URI metadata = create(baseUrl + "/bar");
        final int status = 200;
        final FedoraResponse headResponse = new FedoraResponse(uri, status, N_TRIPLES, metadata, null);

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HEAD);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), N_TRIPLES);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testDeleteProducer() throws Exception {
        final URI uri = create(baseUrl);
        final int status = 204;
        final FedoraResponse deleteResponse = new FedoraResponse(uri, status, null, null, null);

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, DELETE);

        when(mockClient.delete(any(URI.class))).thenReturn(deleteResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testDeleteTombstoneProducer() throws Exception {
        final URI uri = create(baseUrl + "/fcr:tombstone");
        final int status = 204;
        final FedoraResponse deleteResponse = new FedoraResponse(uri, status, null, null, null);

        testEndpoint.setTombstone(true);

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, DELETE);

        when(mockClient.delete(uri)).thenReturn(deleteResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformGetProducer() throws Exception {
        final URI uri = create(baseUrl);
        final int status = 200;
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, status, JSON, null,
                new ByteArrayInputStream(serializedJson.getBytes()));

        testEndpoint.setTransform("default");

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, GET);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(baseUrl + "/fcr:transform/default"), JSON)).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformPostProducer() throws Exception {
        final URI uri = create(baseUrl);
        final String ldpathText = "@prefix dc : <http://purl.org/dc/elements/1.1/>\n" +
                            "title = dc:title :: xsd:string;";
        final ByteArrayInputStream body = new ByteArrayInputStream(ldpathText.getBytes());
        final int status = 200;
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse postResponse = new FedoraResponse(uri, status, JSON, null,
                new ByteArrayInputStream(serializedJson.getBytes()));

        testEndpoint.setTransform("default");

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, POST);
        testExchange.getIn().setHeader(Exchange.CONTENT_TYPE, RDF_LDPATH);
        testExchange.getIn().setBody(body);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.post(create(baseUrl + "/fcr:transform"), body, RDF_LDPATH)).thenReturn(postResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformHeaderProducer() throws Exception {
        final URI uri = create(baseUrl);
        final int status = 200;
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, status, JSON, null,
                new ByteArrayInputStream(serializedJson.getBytes()));

        testEndpoint.setTransform("true");

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_TRANSFORM, "default");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(baseUrl + "/fcr:transform/default"), JSON)).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformHeaderOnlyProducer() throws Exception {
        final URI uri = create(baseUrl);
        final int status = 200;
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, status, JSON, null,
                new ByteArrayInputStream(serializedJson.getBytes()));

        testEndpoint.setTransform("");

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_TRANSFORM, "default");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(baseUrl + "/fcr:transform/default"), JSON)).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }



    @Test
    public void testTransformProducer() throws Exception {
        final URI uri = create(baseUrl);
        final int status = 200;
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse getResponse = new FedoraResponse(uri, status, JSON, null,
                new ByteArrayInputStream(serializedJson.getBytes()));

        testEndpoint.setTransform("default");

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(baseUrl + "/fcr:transform/default"), JSON)).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPostProducer() throws Exception {
        final URI uri = create(baseUrl);
        final String responseText = baseUrl + "/e8/0b/ab/e80bab60";
        final int status = 201;
        final FedoraResponse postResponse = new FedoraResponse(uri, status, TEXT_PLAIN, null,
                new ByteArrayInputStream(responseText.getBytes()));

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, POST);

        when(mockClient.post(any(URI.class), any(InputStream.class), any(String.class))).thenReturn(postResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), responseText);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), TEXT_PLAIN);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPostContentTypeEndpointProducer() throws Exception {
        final URI uri = create(baseUrl);
        final int status = 204;
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, null, null);
        final FedoraResponse postResponse = new FedoraResponse(uri, status, null, null, null);

        testEndpoint.setContentType(SPARQL_UPDATE);

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, POST);
        testExchange.getIn().setBody(new ByteArrayInputStream(sparqlUpdate.getBytes()));

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.post(any(URI.class), any(InputStream.class), eq(SPARQL_UPDATE))).thenReturn(postResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPatchProducer() throws Exception {
        final URI uri = create(baseUrl);
        final URI metadata = create(baseUrl + "/bar");
        final int status = 204;
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, null, metadata, null);
        final FedoraResponse patchResponse = new FedoraResponse(uri, status, null, null, null);

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
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
        final URI uri = create(baseUrl);
        final URI metadata = create(baseUrl + "/bar");
        final int status = 204;
        final FedoraResponse headResponse = new FedoraResponse(uri, 200, RDF_XML, metadata, null);
        final FedoraResponse patchResponse = new FedoraResponse(uri, status, null, null, null);

        testEndpoint.setTransform("default");

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
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
        final URI uri = create(baseUrl);
        final int status = 201;
        final FedoraResponse patchResponse = new FedoraResponse(uri, status, TEXT_PLAIN, null,
                new ByteArrayInputStream(baseUrl.getBytes()));

        init();

        testExchange.getIn().setHeader(FedoraEndpoint.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, PUT);
        testExchange.getIn().setBody(null);

        when(mockClient.put(any(URI.class), any(InputStream.class), any(String.class))).thenReturn(patchResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), baseUrl);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), TEXT_PLAIN);
    }
}
