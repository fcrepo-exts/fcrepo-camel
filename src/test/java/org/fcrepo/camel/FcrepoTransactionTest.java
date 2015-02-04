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
import static org.junit.Assert.assertNull;
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
public class FcrepoTransactionTest {

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
    public void testCreateTransaction() throws Exception {

        final int status = 201;
        final String txId = "tx:83e34464-144e-43d9-af13-b3464a1fb9b5";
        final URI uri = create("http://localhost:8080/rest/fcr:tx");
        final FcrepoResponse postResponse = new FcrepoResponse(uri, status, null,
                URI.create("http://localhost:8080/rest/" + txId), null);
        final InputStream emptyBody = null;
        final String emptyContentType = null;

        testEndpoint.setTransaction("create");
        init();

        when(mockClient.post(eq(uri), eq(emptyBody), eq(emptyContentType))).thenReturn(postResponse);

        testProducer.process(testExchange);

        assertNull(testExchange.getIn().getBody(String.class));
        assertNull(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class));
        assertEquals(status, testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals(txId, testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_TRANSACTION, String.class));
        assertEquals("http://localhost:8080/rest/" + txId,
                testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_LOCATION, String.class));
    }

    @Test
    public void testCreateFalseTransaction() throws Exception {

        final int status = 201;
        final String txId = "tx:83e34464-144e-43d9-af13-b3464a1fb9b5";
        final URI uri = create("http://localhost:8080/rest/fcr:tx");
        final FcrepoResponse postResponse = new FcrepoResponse(uri, status, null, null, null);
        final InputStream emptyBody = null;
        final String emptyContentType = null;

        testEndpoint.setTransaction("create");
        init();

        when(mockClient.post(eq(uri), eq(emptyBody), eq(emptyContentType))).thenReturn(postResponse);

        testProducer.process(testExchange);

        assertNull(testExchange.getIn().getBody(String.class));
        assertNull(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class));
        assertNull(testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_TRANSACTION, String.class));
        assertNull(testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_LOCATION, String.class));
        assertEquals(status, testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }

    @Test
    public void testRefreshTransaction() throws Exception {

        final int status = 201;
        final String txId = "tx:83e34464-144e-43d9-af13-b3464a1fb9b5";
        final URI uri = create("http://localhost:8080/rest/" + txId + "/fcr:tx");
        final FcrepoResponse postResponse = new FcrepoResponse(uri, status, null,
                URI.create("http://localhost:8080/rest/" + txId), null);
        final InputStream emptyBody = null;
        final String emptyContentType = null;

        testEndpoint.setTransaction("refresh");
        init();

        when(mockClient.post(eq(uri), eq(emptyBody), eq(emptyContentType))).thenReturn(postResponse);

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_TRANSACTION, txId);

        testProducer.process(testExchange);

        assertNull(testExchange.getIn().getBody(String.class));
        assertNull(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class));
        assertEquals(status, testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals(txId, testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_TRANSACTION, String.class));
        assertEquals("http://localhost:8080/rest/" + txId,
                testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_LOCATION, String.class));
    }

    @Test
    public void testRollbackTransaction() throws Exception {

        final int status = 204;
        final String txId = "tx:83e34464-144e-43d9-af13-b3464a1fb9b5";
        final URI uri = create("http://localhost:8080/rest/" + txId + "/fcr:tx/fcr:rollback");
        final FcrepoResponse postResponse = new FcrepoResponse(uri, status, null, null, null);
        final InputStream emptyBody = null;
        final String emptyContentType = null;

        testEndpoint.setTransaction("rollback");
        init();

        when(mockClient.post(eq(uri), eq(emptyBody), eq(emptyContentType))).thenReturn(postResponse);

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_TRANSACTION, txId);

        testProducer.process(testExchange);

        assertNull(testExchange.getIn().getBody(String.class));
        assertNull(testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_TRANSACTION, String.class));
        assertNull(testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_LOCATION, String.class));
        assertEquals(status, testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }

    @Test
    public void testCommitTransaction() throws Exception {

        final int status = 204;
        final String txId = "tx:83e34464-144e-43d9-af13-b3464a1fb9b5";
        final URI uri = create("http://localhost:8080/rest/" + txId + "/fcr:tx/fcr:commit");
        final FcrepoResponse postResponse = new FcrepoResponse(uri, status, null, null, null);
        final InputStream emptyBody = null;
        final String emptyContentType = null;

        testEndpoint.setTransaction("commit");
        init();

        when(mockClient.post(eq(uri), eq(emptyBody), eq(emptyContentType))).thenReturn(postResponse);

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_TRANSACTION, txId);

        testProducer.process(testExchange);

        assertNull(testExchange.getIn().getBody(String.class));
        assertNull(testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_TRANSACTION, String.class));
        assertNull(testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_LOCATION, String.class));
        assertEquals(status, testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }


    @Test
    public void testGetProducer() throws Exception {
        final String txId = "tx:83e34464-144e-43d9-af13-b3464a1fb9b5";
        final URI uri = create("http://localhost:8080/rest/" + txId + "/foo");
        final int status = 200;
        final ByteArrayInputStream body = new ByteArrayInputStream(TestUtils.rdfXml.getBytes());
        final FcrepoResponse headResponse = new FcrepoResponse(uri, 200, null, null, null);
        final FcrepoResponse getResponse = new FcrepoResponse(uri, status, TestUtils.RDF_XML, null, body);

        init();

        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_TRANSACTION, txId);
        testExchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");

        when(mockClient.head(eq(uri))).thenReturn(headResponse);
        when(mockClient.get(eq(uri), eq(TestUtils.RDF_XML), any(String.class))).thenReturn(getResponse);

        testProducer.process(testExchange);

        assertEquals(testExchange.getIn().getBody(String.class), TestUtils.rdfXml);
        assertEquals(testExchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class), TestUtils.RDF_XML);
        assertEquals(testExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), status);
        assertEquals(testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_TRANSACTION, String.class), txId);
        assertNull(testExchange.getIn().getHeader(FcrepoHeaders.FCREPO_LOCATION, String.class));
    }
}
