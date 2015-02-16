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
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.camel.FcrepoOperationFailedException;
import org.fcrepo.camel.FcrepoTransactionManager;
import org.fcrepo.camel.RdfNamespaces;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Test adding a new resource with POST
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FcrepoTransactionIT extends CamelTestSupport {

    private TransactionTemplate txTemplate;

    private FcrepoTransactionManager txMgr;

    @EndpointInject(uri = "mock:created")
    protected MockEndpoint createdEndpoint;

    @EndpointInject(uri = "mock:transactedput")
    protected MockEndpoint midtransactionEndpoint;

    @EndpointInject(uri = "mock:notfound")
    protected MockEndpoint notfoundEndpoint;

    @EndpointInject(uri = "mock:verified")
    protected MockEndpoint verifiedEndpoint;

    @EndpointInject(uri = "mock:transacted")
    protected MockEndpoint transactedEndpoint;

    @EndpointInject(uri = "mock:rollback")
    protected MockEndpoint rollbackEndpoint;

    @EndpointInject(uri = "mock:deleted")
    protected MockEndpoint deletedEndpoint;

    @EndpointInject(uri = "mock:missing")
    protected MockEndpoint missingEndpoint;

    @Produce(uri = "direct:create")
    protected ProducerTemplate template;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        txTemplate = new TransactionTemplate(txMgr);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        txTemplate.afterPropertiesSet();
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        final JndiRegistry reg = super.createRegistry();

        txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(FcrepoTestUtils.getFcrepoBaseUrl());
        reg.bind("txManager", txMgr);

        final SpringTransactionPolicy txPolicy = new SpringTransactionPolicy();
        txPolicy.setTransactionManager(txMgr);
        txPolicy.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        reg.bind("required", txPolicy);

        return reg;
    }

    @Test
    public void testTransaction() throws InterruptedException {
        // Assertions
        deletedEndpoint.expectedMessageCount(8);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        transactedEndpoint.expectedMessageCount(1);

        verifiedEndpoint.expectedMessageCount(3);

        midtransactionEndpoint.expectedMessageCount(3);
        midtransactionEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 201);

        notfoundEndpoint.expectedMessageCount(6);
        notfoundEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 404);

        // Start the transaction
        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        // Create the object
        final String fullPath = template.requestBodyAndHeaders(
                "direct:create", FcrepoTestUtils.getTurtleDocument(), headers, String.class);

        assertNotNull(fullPath);

        final String identifier = fullPath.replaceAll(FcrepoTestUtils.getFcrepoBaseUrl(), "");

        // Test the creation of several objects
        template.sendBodyAndHeader("direct:transact", null, "TestIdentifierBase", identifier);

        // Test the object
        template.sendBodyAndHeader("direct:verify", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/one");
        template.sendBodyAndHeader("direct:verify", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/two");
        template.sendBodyAndHeader("direct:verify", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/three");

        // Teardown
        template.sendBodyAndHeader("direct:teardown", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/one");
        template.sendBodyAndHeader("direct:teardown", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/two");
        template.sendBodyAndHeader("direct:teardown", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/three");
        template.sendBodyAndHeader("direct:teardown", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier);

        // Confirm assertions
        verifiedEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();
        transactedEndpoint.assertIsSatisfied();
        notfoundEndpoint.assertIsSatisfied();
        midtransactionEndpoint.assertIsSatisfied();
    }

    @Test
    public void testTransactionWithRollback() throws InterruptedException {
        // Assertions
        deletedEndpoint.expectedMessageCount(2);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        transactedEndpoint.expectedMessageCount(0);

        verifiedEndpoint.expectedMessageCount(0);

        midtransactionEndpoint.expectedMessageCount(2);
        midtransactionEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 201);

        notfoundEndpoint.expectedMessageCount(3);
        notfoundEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 404);

        // Start the transaction
        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        // Create the object
        final String fullPath = template.requestBodyAndHeaders(
                "direct:create", FcrepoTestUtils.getTurtleDocument(), headers, String.class);

        assertNotNull(fullPath);

        final String identifier = fullPath.replaceAll(FcrepoTestUtils.getFcrepoBaseUrl(), "");

        // Test the creation of several objects
        template.sendBodyAndHeader("direct:transactWithError", null, "TestIdentifierBase", identifier);

        // Test the object
        template.sendBodyAndHeader("direct:verifyMissing", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/one");
        template.sendBodyAndHeader("direct:verifyMissing", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/two");

        // Teardown
        template.sendBodyAndHeader("direct:teardown", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier);

        // Confirm assertions
        verifiedEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();
        transactedEndpoint.assertIsSatisfied();
        notfoundEndpoint.assertIsSatisfied();
        midtransactionEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                final String fcrepo_uri = FcrepoTestUtils.getFcrepoEndpointUri();
                final String http4_uri = fcrepo_uri.replaceAll("fcrepo:", "http4:");

                final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);

                final XPathBuilder titleXpath = new XPathBuilder("/rdf:RDF/rdf:Description/dc:title/text()");
                titleXpath.namespaces(ns);
                titleXpath.namespace("dc", "http://purl.org/dc/elements/1.1/");

                onException(FcrepoOperationFailedException.class)
                    .handled(true)
                    .to("mock:missing");

                from("direct:create")
                    .to(fcrepo_uri)
                    .to("mock:created");

                from("direct:transactWithError")
                    .transacted("required")
                    .setHeader(FcrepoHeaders.FCREPO_IDENTIFIER).simple("${headers.TestIdentifierBase}/one")
                    .setHeader(Exchange.HTTP_METHOD).constant("PUT")
                    .to(fcrepo_uri)
                    .to("mock:transactedput")

                    .setHeader(Exchange.HTTP_PATH).simple("/rest${headers.TestIdentifierBase}/one")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(http4_uri + "?throwExceptionOnFailure=false")
                    .to("mock:notfound")

                    .setHeader(FcrepoHeaders.FCREPO_IDENTIFIER).simple("${headers.TestIdentifierBase}/two")
                    .setHeader(Exchange.HTTP_METHOD).constant("PUT")
                    .to(fcrepo_uri)
                    .to("mock:transactedput")

                    .setHeader(Exchange.HTTP_PATH).simple("/rest${headers.TestIdentifierBase}/one")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(http4_uri + "?throwExceptionOnFailure=false")
                    .to("mock:notfound")
                    .setHeader(Exchange.HTTP_PATH).simple("/rest${headers.TestIdentifierBase}/two")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(http4_uri + "?throwExceptionOnFailure=false")
                    .to("mock:notfound")

                    // this should throw an error
                    .setHeader(FcrepoHeaders.FCREPO_IDENTIFIER).simple("${headers.TestIdentifierBase}/foo/")
                    .setHeader(Exchange.HTTP_METHOD).constant("POST")
                    .to(fcrepo_uri)
                    .to("mock:transactedput")

                    // this should never be reached
                    .to("mock:transacted");

                from("direct:transact")
                    .transacted("required")
                    .setHeader(FcrepoHeaders.FCREPO_IDENTIFIER).simple("${headers.TestIdentifierBase}/one")
                    .setHeader(Exchange.HTTP_METHOD).constant("PUT")
                    .to(fcrepo_uri)
                    .to("mock:transactedput")

                    .setHeader(Exchange.HTTP_PATH).simple("/rest${headers.TestIdentifierBase}/one")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(http4_uri + "?throwExceptionOnFailure=false")
                    .to("mock:notfound")

                    .setHeader(FcrepoHeaders.FCREPO_IDENTIFIER).simple("${headers.TestIdentifierBase}/two")
                    .setHeader(Exchange.HTTP_METHOD).constant("PUT")
                    .to(fcrepo_uri)
                    .to("mock:transactedput")

                    .setHeader(Exchange.HTTP_PATH).simple("/rest${headers.TestIdentifierBase}/one")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(http4_uri + "?throwExceptionOnFailure=false")
                    .to("mock:notfound")
                    .setHeader(Exchange.HTTP_PATH).simple("/rest${headers.TestIdentifierBase}/two")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(http4_uri + "?throwExceptionOnFailure=false")
                    .to("mock:notfound")

                    .setHeader(FcrepoHeaders.FCREPO_IDENTIFIER).simple("${headers.TestIdentifierBase}/three")
                    .setHeader(Exchange.HTTP_METHOD).constant("PUT")
                    .to(fcrepo_uri)
                    .to("mock:transactedput")

                    .setHeader(Exchange.HTTP_PATH).simple("/rest${headers.TestIdentifierBase}/one")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(http4_uri + "?throwExceptionOnFailure=false")
                    .to("mock:notfound")
                    .setHeader(Exchange.HTTP_PATH).simple("/rest${headers.TestIdentifierBase}/two")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(http4_uri + "?throwExceptionOnFailure=false")
                    .to("mock:notfound")

                    .setHeader(Exchange.HTTP_PATH).simple("/rest${headers.TestIdentifierBase}/three")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(http4_uri + "?throwExceptionOnFailure=false")
                    .to("mock:notfound")

                    .to("mock:transacted");

                from("direct:verify")
                    .to(fcrepo_uri)
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='" + RdfNamespaces.REPOSITORY + "Resource']", ns)
                    .to("mock:verified");

                from("direct:verifyMissing")
                    .to(fcrepo_uri + "?throwExceptionOnFailure=false")
                    .filter(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo("404"))
                    .to("mock:notfound");

                from("direct:teardown")
                    .setHeader(Exchange.HTTP_METHOD).constant("DELETE")
                    .to(fcrepo_uri)
                    .to("mock:deleted")
                    .to(fcrepo_uri + "?tombstone=true")
                    .to("mock:deleted");
            }
        };
    }
}
