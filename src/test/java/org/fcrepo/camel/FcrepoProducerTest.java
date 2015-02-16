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

import static java.net.URI.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.converter.stream.InputStreamCache;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultUnitOfWork;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author acoburn
 */
@RunWith(MockitoJUnitRunner.class)
public class FcrepoProducerTest {

    private FcrepoEndpoint testEndpoint;

    private FcrepoProducer testProducer;

    private Exchange testExchange;

    @Mock
    private FcrepoClient mockClient;

    @Before
    public void setUp() throws IOException {
        final FcrepoComponent mockComponent = mock(FcrepoComponent.class);

        final FcrepoConfiguration testConfig = new FcrepoConfiguration();
        testEndpoint = new FcrepoEndpoint("fcrepo:localhost:8080", "/rest", mockComponent, testConfig);
        testEndpoint.setBaseUrl("localhost:8080/rest");
        testExchange = new DefaultExchange(new DefaultCamelContext());
        testExchange.getIn().setBody(null);
    }

    public void init() throws IOException {
        testProducer = new FcrepoProducer(testEndpoint);
        TestUtils.setField(testProducer, "client", mockClient);
    }

    @Test
    public void testGetProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 200;
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfXml.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.RDF_XML, null, body);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(TestUtils.RDF_XML), any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfXml);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.RDF_XML);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testGetAcceptHeaderProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfTriples.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, 200, TestUtils.N_TRIPLES, null, body);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader("Accept", TestUtils.N_TRIPLES);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(TestUtils.N_TRIPLES), any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfTriples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.N_TRIPLES);
    }

    @Test
    public void testGetPreferHeaderProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfTriples.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, 200, TestUtils.N_TRIPLES, null, body);
        final String prefer = "return=representation; omit=\"http://www.w3.org/ns/ldp#PreferContainment\";";

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_PREFER, prefer);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), any(String.class), eq(prefer))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfTriples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.N_TRIPLES);
    }

    @Test
    public void testGetPreferIncludeLongEndpointProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfTriples.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, 200, TestUtils.N_TRIPLES, null, body);
        final String prefer = "return=representation; " +
                    "include=\"http://fedora.info/definitions/v4/repository#ServerManaged\";";

        testEndpoint.setPreferInclude("http://fedora.info/definitions/v4/repository#ServerManaged");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), any(String.class), eq(prefer))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfTriples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.N_TRIPLES);
    }

    @Test
    public void testGetPreferIncludeShortEndpointProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfTriples.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, 200, TestUtils.N_TRIPLES, null, body);
        final String prefer = "return=representation; include=\"http://www.w3.org/ns/ldp#PreferMembership\";";

        testEndpoint.setPreferInclude("PreferMembership");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), any(String.class), eq(prefer))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfTriples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.N_TRIPLES);
    }

    @Test
    public void testGetPreferOmitLongEndpointProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfTriples.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, 200, TestUtils.N_TRIPLES, null, body);
        final String prefer = "return=representation; " +
                    "omit=\"http://fedora.info/definitions/v4/repository#EmbedResources\";";

        testEndpoint.setPreferOmit("http://fedora.info/definitions/v4/repository#EmbedResources");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), any(String.class), eq(prefer))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfTriples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.N_TRIPLES);
    }

    @Test
    public void testGetPreferOmitShortEndpointProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfTriples.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, 200, TestUtils.N_TRIPLES, null, body);
        final String prefer = "return=representation; omit=\"http://www.w3.org/ns/ldp#PreferContainment\";";

        testEndpoint.setPreferOmit("PreferContainment");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), any(String.class), eq(prefer))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfTriples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.N_TRIPLES);
    }

    @Test
    public void testGetAcceptEndpointProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfTriples.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, 200, TestUtils.N_TRIPLES, null, body);

        testEndpoint.setAccept(TestUtils.N_TRIPLES);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(TestUtils.N_TRIPLES), any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfTriples);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.N_TRIPLES);
    }

    @Test
    public void testGetRootProducer() throws Exception {
        final URI uri = create("http://localhost:8080/rest");
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfXml.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, 200, TestUtils.RDF_XML, null, body);

        init();

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(TestUtils.RDF_XML), any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfXml);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.RDF_XML);
    }

    @Test
    public void testGetBinaryProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final String content = "Foo";
        final ByteArrayInputStream body = new ByteArrayInputStream(content.getBytes());
        final FcrepoResponse getResponse = new FcrepoResponse(uri, 200, TestUtils.TEXT_PLAIN, null, body);

        testEndpoint.setMetadata(false);

        init();

        testExchange.getIn().setHeader(JmsHeaders.IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.ACCEPT_CONTENT_TYPE, TestUtils.TEXT_PLAIN);
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.GET);

        when(mockClient.get(any(URI.class), eq(TestUtils.TEXT_PLAIN), any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), content);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.TEXT_PLAIN);
    }

    @Test
    public void testHeadProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final URI metadata = create(TestUtils.baseUrl + "/bar");
        final int status = 200;
        final FcrepoResponse headResponse = new FcrepoResponse(uri, status, TestUtils.N_TRIPLES, metadata, null);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.HEAD);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.N_TRIPLES);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testDeleteProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 204;
        final FcrepoResponse deleteResponse = new FcrepoResponse(uri, status, null, null, null);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.DELETE);

        when(mockClient.delete(any(URI.class))).thenReturn(deleteResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testDeleteTombstoneProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl + "/fcr:tombstone");
        final int status = 204;
        final FcrepoResponse deleteResponse = new FcrepoResponse(uri, status, null, null, null);

        testEndpoint.setTombstone(true);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.DELETE);

        when(mockClient.delete(uri)).thenReturn(deleteResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformGetProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 200;
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.JSON, null,
                new ByteArrayInputStream(TestUtils.serializedJson.getBytes()));

        testEndpoint.setTransform("default");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.GET);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(TestUtils.baseUrl + "/fcr:transform/default"), TestUtils.JSON, null))
            .thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), TestUtils.JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformPostProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final String ldpathText = "@prefix dc : <http://purl.org/dc/elements/1.1/>\n" +
                            "title = dc:title :: xsd:string;";
        final ByteArrayInputStream body = new ByteArrayInputStream(ldpathText.getBytes());
        final int status = 200;
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse postResponse = new FcrepoResponse(uri, status, TestUtils.JSON, null,
                new ByteArrayInputStream(TestUtils.serializedJson.getBytes()));

        testEndpoint.setTransform("default");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
        testExchange.getIn().setHeader(Exchange.CONTENT_TYPE, TestUtils.RDF_LDPATH);
        testExchange.getIn().setBody(body);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.post(create(TestUtils.baseUrl + "/fcr:transform"), body, TestUtils.RDF_LDPATH))
            .thenReturn(postResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), TestUtils.JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformHeaderProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 200;
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.JSON, null,
                new ByteArrayInputStream(TestUtils.serializedJson.getBytes()));

        testEndpoint.setTransform("true");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_TRANSFORM, "default");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(TestUtils.baseUrl + "/fcr:transform/default"), TestUtils.JSON, null))
            .thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), TestUtils.JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformHeaderOnlyProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 200;
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.JSON, null,
                new ByteArrayInputStream(TestUtils.serializedJson.getBytes()));

        testEndpoint.setTransform("");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_TRANSFORM, "default");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(TestUtils.baseUrl + "/fcr:transform/default"), TestUtils.JSON, null))
            .thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), TestUtils.JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransformProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 200;
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.JSON, null,
                new ByteArrayInputStream(TestUtils.serializedJson.getBytes()));

        testEndpoint.setTransform("default");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(TestUtils.baseUrl + "/fcr:transform/default"), TestUtils.JSON, null))
            .thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), TestUtils.JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPostProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final String responseText = TestUtils.baseUrl + "/e8/0b/ab/e80bab60";
        final int status = 201;
        final FcrepoResponse postResponse = new FcrepoResponse(uri, status, TestUtils.TEXT_PLAIN, null,
                new ByteArrayInputStream(responseText.getBytes()));

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);

        when(mockClient.post(any(URI.class), any(InputStream.class), any(String.class))).thenReturn(postResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), responseText);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), TestUtils.TEXT_PLAIN);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPostContentTypeEndpointProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 204;
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse postResponse = new FcrepoResponse(uri, status, null, null, null);

        testEndpoint.setContentType(TestUtils.SPARQL_UPDATE);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
        testExchange.getIn().setBody(new ByteArrayInputStream(TestUtils.sparqlUpdate.getBytes()));

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.post(any(URI.class), any(InputStream.class), eq(TestUtils.SPARQL_UPDATE)))
            .thenReturn(postResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPatchProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final URI metadata = create(TestUtils.baseUrl + "/bar");
        final int status = 204;
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, metadata, null);
        final FcrepoResponse patchResponse = new FcrepoResponse(uri, status, null, null, null);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.PATCH);
        testExchange.getIn().setBody(new ByteArrayInputStream(TestUtils.sparqlUpdate.getBytes()));

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.patch(any(URI.class), any(InputStream.class))).thenReturn(patchResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPatchTransformEnabledProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final URI metadata = create(TestUtils.baseUrl + "/bar");
        final int status = 204;
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, TestUtils.RDF_XML, metadata, null);
        final FcrepoResponse patchResponse = new FcrepoResponse(uri, status, null, null, null);

        testEndpoint.setTransform("default");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.PATCH);
        testExchange.getIn().setBody(new ByteArrayInputStream(TestUtils.sparqlUpdate.getBytes()));

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.patch(any(URI.class), any(InputStream.class))).thenReturn(patchResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(), null);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testPutProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 201;
        final FcrepoResponse patchResponse = new FcrepoResponse(uri, status, TestUtils.TEXT_PLAIN, null,
                new ByteArrayInputStream(TestUtils.baseUrl.getBytes()));

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.PUT);
        testExchange.getIn().setBody(null);

        when(mockClient.put(any(URI.class), any(InputStream.class), any(String.class))).thenReturn(patchResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.baseUrl);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), TestUtils.TEXT_PLAIN);
    }

    @Test
    public void testPreferProperties() throws Exception {
        testProducer = new FcrepoProducer(testEndpoint);

        assertEquals(6, RdfNamespaces.PREFER_PROPERTIES.size());
        final String[] fcrepoPrefer = new String[] { "ServerManaged", "EmbedResources", "InboundReferences" };
        for (final String s : fcrepoPrefer) {
            assertEquals(RdfNamespaces.REPOSITORY + s, RdfNamespaces.PREFER_PROPERTIES.get(s));
        }

        final String[] ldpPrefer = new String[] { "PreferContainment", "PreferMembership",
            "PreferMinimalContainer" };
        for (final String s : ldpPrefer) {
            assertEquals(RdfNamespaces.LDP + s, RdfNamespaces.PREFER_PROPERTIES.get(s));
        }
    }

    @Test
    public void testGetProducerWithScheme() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 200;
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfXml.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.RDF_XML, null, body);

        // set the baseUrl with an explicit http:// scheme
        testEndpoint.setBaseUrl("http://localhost:8080/rest");
        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(eq(create(TestUtils.baseUrl)), eq(TestUtils.RDF_XML),
                    any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfXml);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.RDF_XML);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testGetSecureProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrlSecure);
        final int status = 200;
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfXml.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.RDF_XML, null, body);

        // set the baseUrl with no scheme but with a secure port
        testEndpoint.setBaseUrl("localhost:443/rest");
        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/secure");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(eq(create(TestUtils.baseUrlSecure)), eq(TestUtils.RDF_XML),
                    any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfXml);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.RDF_XML);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testGetSecureProducerWithScheme() throws Exception {
        final URI uri = create(TestUtils.baseUrlSecureWithoutPort);
        final int status = 200;
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfXml.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.RDF_XML, null, body);

        // set the baseUrl with explicit scheme but no port
        testEndpoint.setBaseUrl("https://localhost/rest");
        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/secure");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(eq(create(TestUtils.baseUrlSecureWithoutPort)), eq(TestUtils.RDF_XML),
                    any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfXml);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.RDF_XML);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testSecureTransformProducer() throws Exception {
        final URI uri = create(TestUtils.baseUrlSecure);
        final int status = 200;
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.JSON, null,
                new ByteArrayInputStream(TestUtils.serializedJson.getBytes()));

        // set the baseUrl with no scheme but explicit secure port
        testEndpoint.setBaseUrl("localhost:443/rest");
        testEndpoint.setTransform("default");

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/secure");

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(create(TestUtils.baseUrlSecure + "/fcr:transform/default"), TestUtils.JSON, null))
            .thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.serializedJson);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE), TestUtils.JSON);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testTransactedGetProducer() throws Exception {
        final String baseUrl = "http://localhost:8080/rest";
        final String path = "/transact";
        final String path2 = "/transact2";
        final String tx = "tx:12345";
        final URI uri = create(baseUrl + "/" + tx + path);
        final URI uri2 = create(baseUrl + "/" + tx + path2);
        final URI commitUri = URI.create(baseUrl + "/" + tx + "/fcr:tx/fcr:commit");
        final URI beginUri = URI.create(baseUrl + "/fcr:tx");
        final int status = 200;
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfXml.getBytes());
        final ByteArrayInputStream body2 = new ByteArrayInputStream(TestUtils.rdfTriples.getBytes());
        final DefaultUnitOfWork uow = new DefaultUnitOfWork(testExchange);
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);

        testEndpoint.setTransactionManager(txMgr);

        final FcrepoClient otherMockClient = mock(FcrepoClient.class);

        init();
        TestUtils.setField(txMgr, "client", otherMockClient);

        uow.beginTransactedBy((Object)tx);

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        testExchange.setUnitOfWork(uow);

        when(otherMockClient.post(eq(beginUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(beginUri, 201, null, URI.create(baseUrl + "/" + tx), null));
        when(otherMockClient.post(eq(commitUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(commitUri, 201, null, null, null));

        when(mockClient.head(any(URI.class))).thenReturn(new FcrepoResponse(uri, 200, null, null, null));
        when(mockClient.get(eq(uri), eq(TestUtils.RDF_XML), any(String.class))).thenReturn(
            new FcrepoResponse(uri, status, TestUtils.RDF_XML, null, body));
        when(mockClient.get(eq(uri2), eq(TestUtils.N_TRIPLES), any(String.class))).thenReturn(
            new FcrepoResponse(uri2, status, TestUtils.N_TRIPLES, null, body2));

        testProducer.process(testExchange);

        assertEquals(status, testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals(TestUtils.RDF_XML, testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class));
        assertEquals(TestUtils.rdfXml, testExchange.getIn().getBody(String.class));

        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
        testExchange.getIn().setHeader(Exchange.ACCEPT_CONTENT_TYPE, TestUtils.N_TRIPLES);
        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, path2);
        testExchange.setUnitOfWork(uow);
        testProducer.process(testExchange);
        assertEquals(status, testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals(TestUtils.N_TRIPLES, testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class));
        assertEquals(TestUtils.rdfTriples, testExchange.getIn().getBody(String.class));

    }

    @Test (expected = RuntimeException.class)
    public void testTransactedProducerWithError() throws Exception {
        final String baseUrl = "http://localhost:8080/rest";
        final String path = "/transact";
        final String path2 = "/transact2";
        final String tx = "tx:12345";
        final URI uri = create(baseUrl + "/" + tx + path);
        final URI uri2 = create(baseUrl + "/" + tx + path2);
        final URI commitUri = URI.create(baseUrl + "/" + tx + "/fcr:tx/fcr:commit");
        final URI beginUri = URI.create(baseUrl + "/fcr:tx");
        final int status = 200;
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfXml.getBytes());
        final ByteArrayInputStream body2 = new ByteArrayInputStream(TestUtils.rdfTriples.getBytes());
        final DefaultUnitOfWork uow = new DefaultUnitOfWork(testExchange);
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);

        testEndpoint.setTransactionManager(txMgr);

        final FcrepoClient otherMockClient = mock(FcrepoClient.class);

        init();
        TestUtils.setField(txMgr, "client", otherMockClient);

        uow.beginTransactedBy((Object)tx);

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        testExchange.setUnitOfWork(uow);

        when(otherMockClient.post(eq(beginUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(beginUri, 201, null, URI.create(baseUrl + "/" + tx), null));
        when(otherMockClient.post(eq(commitUri), any(InputStream.class), anyString())).thenReturn(
                new FcrepoResponse(commitUri, 201, null, null, null));

        when(mockClient.head(any(URI.class))).thenReturn(new FcrepoResponse(uri, 200, null, null, null));
        when(mockClient.get(eq(uri), eq(TestUtils.RDF_XML), any(String.class))).thenReturn(
            new FcrepoResponse(uri, status, TestUtils.RDF_XML, null, body));
        when(mockClient.get(eq(uri2), eq(TestUtils.N_TRIPLES), any(String.class))).thenThrow(
            new FcrepoOperationFailedException(uri2, 400, "Bad Request"));

        testProducer.process(testExchange);

        assertEquals(status, testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals(TestUtils.RDF_XML, testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class));
        assertEquals(TestUtils.rdfXml, testExchange.getIn().getBody(String.class));

        testExchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
        testExchange.getIn().setHeader(Exchange.ACCEPT_CONTENT_TYPE, TestUtils.N_TRIPLES);
        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, path2);
        testExchange.setUnitOfWork(uow);
        testProducer.process(testExchange);
    }

    @Test
    public void testNoStreamCaching() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 200;
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfXml.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.RDF_XML, null, body);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.setProperty(Exchange.DISABLE_HTTP_STREAM_CACHE, true);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(TestUtils.RDF_XML), any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfXml);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.RDF_XML);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }

    @Test
    public void testStreamCaching() throws Exception {
        final URI uri = create(TestUtils.baseUrl);
        final int status = 200;
        final String rdfConcat = StringUtils.repeat(TestUtils.rdfXml, 10000);
        final ByteArrayInputStream body = new ByteArrayInputStream(rdfConcat.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.RDF_XML, null, body);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        testExchange.setProperty(Exchange.DISABLE_HTTP_STREAM_CACHE, false);

        testExchange.getContext().getStreamCachingStrategy().setSpoolThreshold(1024);
        testExchange.getContext().getStreamCachingStrategy().setBufferSize(256);
        testExchange.getContext().setStreamCaching(true);

        when(mockClient.head(any(URI.class))).thenReturn(headResponse);
        when(mockClient.get(any(URI.class), eq(TestUtils.RDF_XML), any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(true, testExchange.getContext().isStreamCaching());
        assertNotNull(testExchange.getIn().getBody(InputStreamCache.class));
        assertEquals(rdfConcat.length(), testExchange.getIn().getBody(InputStreamCache.class).length());
        assertEquals(rdfConcat.length(), testExchange.getIn().getBody(InputStreamCache.class).length());
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.RDF_XML);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
    }
}
