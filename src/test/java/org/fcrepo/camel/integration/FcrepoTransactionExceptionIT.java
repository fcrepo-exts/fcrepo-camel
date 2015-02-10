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
package org.fcrepo.camel.integration;

import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.RdfNamespaces;
import org.fcrepo.camel.FcrepoOperationFailedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test adding a new resource with POST
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FcrepoTransactionExceptionIT extends CamelTestSupport {

    @EndpointInject(uri = "mock:started")
    protected MockEndpoint startedEndpoint;

    @EndpointInject(uri = "mock:rollback")
    protected MockEndpoint rollbackEndpoint;

    @EndpointInject(uri = "mock:committed")
    protected MockEndpoint committedEndpoint;

    @EndpointInject(uri = "mock:created")
    protected MockEndpoint createdEndpoint;

    @EndpointInject(uri = "mock:verified")
    protected MockEndpoint verifiedEndpoint;

    @EndpointInject(uri = "mock:deleted")
    protected MockEndpoint deletedEndpoint;

    @EndpointInject(uri = "mock:refreshed")
    protected MockEndpoint refreshedEndpoint;

    @EndpointInject(uri = "mock:missing")
    protected MockEndpoint missingEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testGetTransactionWithException() throws InterruptedException {
        // Assertions
        missingEndpoint.expectedMessageCount(1);
        missingEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 404);

        deletedEndpoint.expectedMessageCount(2);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        refreshedEndpoint.expectedMessageCount(1);
        refreshedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        committedEndpoint.expectedMessageCount(1);
        committedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        verifiedEndpoint.expectedMessageCount(2);

        final Exchange exchange = template.send("direct:start", new DefaultExchange(new DefaultCamelContext()));
        final String transaction = exchange.getIn().getHeader(FcrepoHeaders.FCREPO_TRANSACTION, String.class);

        assertNotNull(transaction);

        // Start the transaction
        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(FcrepoHeaders.FCREPO_TRANSACTION, transaction);
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        // Create the object
        final String fullPath = template.requestBodyAndHeaders(
                "direct:create", FcrepoTestUtils.getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(FcrepoTestUtils.getFcrepoBaseUrl() + "/" + transaction, "");

        // Test the object (unsuccessful)
        final Exchange exchangeIn = new DefaultExchange(new DefaultCamelContext());
        exchangeIn.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        final Exchange exchangeOut = template.send("direct:verify", exchangeIn);
        final FcrepoOperationFailedException ex = (FcrepoOperationFailedException)exchangeOut.getException();
        assertNotNull(ex);
        assertEquals(404, ex.getStatusCode());
        assertEquals(URI.create(FcrepoTestUtils.getFcrepoBaseUrl() + identifier), ex.getUrl());
        assertEquals("Not Found", ex.getStatusText());

        // Test the object (successful)
        headers.clear();
        headers.put(JmsHeaders.IDENTIFIER, identifier);
        headers.put(FcrepoHeaders.FCREPO_TRANSACTION, transaction);
        template.sendBodyAndHeaders("direct:verify", null, headers);

        // Refresh
        headers.clear();
        headers.put(FcrepoHeaders.FCREPO_TRANSACTION, transaction);
        template.sendBodyAndHeaders("direct:refresh", null, headers);

        // Commit
        headers.clear();
        headers.put(FcrepoHeaders.FCREPO_TRANSACTION, transaction);
        template.sendBodyAndHeaders("direct:commit", null, headers);

        // Test the object (successful)
        headers.clear();
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders("direct:verify", null, headers);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm assertions
        verifiedEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();
        createdEndpoint.assertIsSatisfied();
        committedEndpoint.assertIsSatisfied();
        refreshedEndpoint.assertIsSatisfied();
    }

    @Test
    public void testPostTransactionWithException() throws InterruptedException {
        // Assertions
        rollbackEndpoint.expectedMessageCount(1);

        // Start the transaction
        final Exchange exchange = template.send("direct:start", new DefaultExchange(new DefaultCamelContext()));
        final String transaction = exchange.getIn().getHeader(FcrepoHeaders.FCREPO_TRANSACTION, String.class);

        assertNotNull(transaction);

        // Create the object (unsuccessful)
        final Exchange exchangeIn = new DefaultExchange(new DefaultCamelContext());
        exchangeIn.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchangeIn.getIn().setHeader(Exchange.CONTENT_TYPE, "text/turtle");
        exchangeIn.getIn().setHeader(FcrepoHeaders.FCREPO_TRANSACTION, transaction + "foo");
        exchangeIn.getIn().setBody(FcrepoTestUtils.getTurtleDocument());

        final Exchange exchangeOut = template.send("direct:create", exchangeIn);
        final FcrepoOperationFailedException ex = (FcrepoOperationFailedException)exchangeOut.getException();
        assertNotNull(ex);
        assertEquals(410, ex.getStatusCode());
        assertEquals(URI.create(FcrepoTestUtils.getFcrepoBaseUrl() + "/" + transaction + "foo"), ex.getUrl());
        assertEquals("Gone", ex.getStatusText());

        // Rollback
        template.sendBodyAndHeader("direct:rollback", null, FcrepoHeaders.FCREPO_TRANSACTION, transaction);

        // Confirm assertions
        rollbackEndpoint.assertIsSatisfied();
    }

    @Test
    public void testPostTransactionWithNoException() throws InterruptedException {
        // Assertions
        rollbackEndpoint.expectedMessageCount(1);

        createdEndpoint.expectedMessageCount(1);
        createdEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 410);

        // Start the transaction
        final Exchange exchange = template.send("direct:start", new DefaultExchange(new DefaultCamelContext()));
        final String transaction = exchange.getIn().getHeader(FcrepoHeaders.FCREPO_TRANSACTION, String.class);

        assertNotNull(transaction);

        // Create the object (unsuccessful)
        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(FcrepoHeaders.FCREPO_TRANSACTION, transaction + "foo");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        template.sendBodyAndHeaders("direct:createHandled", FcrepoTestUtils.getTurtleDocument(), headers);

        // Rollback
        template.sendBodyAndHeader("direct:rollback", null, FcrepoHeaders.FCREPO_TRANSACTION, transaction);

        // Confirm assertions
        rollbackEndpoint.assertIsSatisfied();
    }


    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                final String fcrepo_uri = FcrepoTestUtils.getFcrepoEndpointUri();

                final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);

                final XPathBuilder titleXpath = new XPathBuilder("/rdf:RDF/rdf:Description/dc:title/text()");
                titleXpath.namespaces(ns);
                titleXpath.namespace("dc", "http://purl.org/dc/elements/1.1/");

                from("direct:start")
                    .to(fcrepo_uri + "?transaction=create")
                    .to("mock:started");

                from("direct:refresh")
                    .to(fcrepo_uri + "?transaction=refresh")
                    .to("mock:refreshed");

                from("direct:createHandled")
                    .to(fcrepo_uri + "?throwExceptionOnFailure=false")
                    .to("mock:created");

                from("direct:create")
                    .to(fcrepo_uri)
                    .to("mock:created");

                from("direct:rollback")
                    .to(fcrepo_uri + "?transaction=rollback")
                    .to("mock:rollback");

                from("direct:verify")
                    .to(fcrepo_uri)
                    .convertBodyTo(org.w3c.dom.Document.class)
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='" + RdfNamespaces.REPOSITORY + "Resource']", ns)
                    .to("mock:verified");

                from("direct:commit")
                    .to(fcrepo_uri + "?transaction=commit")
                    .to("mock:committed");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to("mock:deleted")
                    .to(fcrepo_uri + "?tombstone=true")
                    .to("mock:deleted");
            }
        };
    }
}
