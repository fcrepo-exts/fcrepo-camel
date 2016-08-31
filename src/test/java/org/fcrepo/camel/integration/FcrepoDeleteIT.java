/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.camel.RdfNamespaces;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test deleting an RDF resource
 * @author Aaron Coburn
 * @since Dec 26, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FcrepoDeleteIT extends CamelTestSupport {

    @EndpointInject(uri = "mock:created")
    protected MockEndpoint createdEndpoint;

    @EndpointInject(uri = "mock:filter")
    protected MockEndpoint filteredEndpoint;

    @EndpointInject(uri = "mock:container")
    protected MockEndpoint containerEndpoint;

    @EndpointInject(uri = "mock:verifyGone")
    protected MockEndpoint goneEndpoint;

    @EndpointInject(uri = "mock:deleted")
    protected MockEndpoint deletedEndpoint;

    @EndpointInject(uri = "mock:verifyNotFound")
    protected MockEndpoint notFoundEndpoint;

    @Produce(uri = "direct:filter")
    protected ProducerTemplate template;

    @Test
    public void testDeleteContainer() throws InterruptedException {
        // Assertions
        createdEndpoint.expectedMessageCount(1);
        createdEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 201);

        containerEndpoint.expectedMessageCount(1);
        containerEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        filteredEndpoint.expectedMessageCount(1);
        filteredEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/rdf+xml");
        filteredEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        deletedEndpoint.expectedMessageCount(2);
        deletedEndpoint.expectedBodiesReceived(null, null);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        goneEndpoint.expectedMessageCount(1);
        goneEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 410);

        notFoundEndpoint.expectedMessageCount(1);
        notFoundEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 404);

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:create",
                FcrepoTestUtils.getTurtleDocument(),
                headers, String.class);

        // Strip off the baseUrl to get the resource path
        final String identifier = fullPath.replaceAll(FcrepoTestUtils.getFcrepoBaseUrl(), "");

        template.sendBodyAndHeader("direct:get", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier);

        template.sendBodyAndHeader("direct:delete", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier);

        // Confirm that assertions passed
        createdEndpoint.assertIsSatisfied();
        filteredEndpoint.assertIsSatisfied();
        containerEndpoint.assertIsSatisfied();
        goneEndpoint.assertIsSatisfied();
        notFoundEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();

        // Check deleted container
        goneEndpoint.getExchanges().forEach(exchange -> {
            assertTrue(exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class).contains("application/rdf+xml"));
            assertTrue(exchange.getIn().getBody(String.class).contains("Discovered tombstone"));
        });

        notFoundEndpoint.getExchanges().forEach(exchange -> {
            assertTrue(exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class).contains("text/html"));
            assertTrue(exchange.getIn().getBody(String.class).contains("Not Found"));
        });
     }

    @Test
    public void testDeleteNestedContainer() throws InterruptedException {
        // Assertions
        createdEndpoint.expectedMessageCount(2);
        createdEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 201);

        containerEndpoint.expectedMessageCount(2);
        containerEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        filteredEndpoint.expectedMessageCount(2);
        filteredEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/rdf+xml");
        filteredEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        deletedEndpoint.expectedMessageCount(2);
        deletedEndpoint.expectedBodiesReceived(null, null);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        goneEndpoint.expectedMessageCount(1);
        goneEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 410);

        notFoundEndpoint.expectedMessageCount(2);
        notFoundEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 404);

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:create",
                FcrepoTestUtils.getTurtleDocument(),
                headers, String.class);

        // Strip off the baseUrl to get the resource path
        final String identifier = fullPath.replaceAll(FcrepoTestUtils.getFcrepoBaseUrl(), "");
        final String child = identifier + "/child";

        headers.clear();
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, child);
        headers.put(Exchange.HTTP_METHOD, "PUT");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        template.sendBodyAndHeaders("direct:create", FcrepoTestUtils.getTurtleDocument(), headers);

        template.sendBodyAndHeader("direct:get", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeader("direct:get", null, FcrepoHeaders.FCREPO_IDENTIFIER, child);

        template.sendBodyAndHeader("direct:delete", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier);

        template.sendBodyAndHeader("direct:getPostDelete", null, FcrepoHeaders.FCREPO_IDENTIFIER, child);

        // Confirm that assertions passed
        createdEndpoint.assertIsSatisfied();
        filteredEndpoint.assertIsSatisfied();
        containerEndpoint.assertIsSatisfied();
        goneEndpoint.assertIsSatisfied();
        notFoundEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();

        // Check deleted container
        goneEndpoint.getExchanges().forEach(exchange -> {
            assertTrue(exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class).contains("application/rdf+xml"));
            assertTrue(exchange.getIn().getBody(String.class).contains("Discovered tombstone"));
        });

        notFoundEndpoint.getExchanges().forEach(exchange -> {
            assertTrue(exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class).contains("text/html"));
            assertTrue(exchange.getIn().getBody(String.class).contains("Not Found"));
        });
     }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {

                final String fcrepo_uri = FcrepoTestUtils.getFcrepoEndpointUri();

                final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);

                from("direct:create")
                    .to(fcrepo_uri)
                    .to("mock:created");

                from("direct:get")
                    .to(fcrepo_uri)
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='" + RdfNamespaces.REPOSITORY + "Container']", ns)
                    .to("mock:filter")
                    .to(fcrepo_uri)
                    .to("mock:container");

                from("direct:delete")
                    .setHeader(Exchange.HTTP_METHOD, constant("DELETE"))
                    .to(fcrepo_uri)
                    .to("mock:deleted")
                    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                    .to(fcrepo_uri + "?throwExceptionOnFailure=false")
                    .to("mock:verifyGone")
                    .setHeader(Exchange.HTTP_METHOD, constant("DELETE"))
                    .to(fcrepo_uri + "?tombstone=true")
                    .to("mock:deleted")
                    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                    .to(fcrepo_uri + "?throwExceptionOnFailure=false")
                    .to("mock:verifyNotFound");

                from("direct:getPostDelete")
                    .to(fcrepo_uri + "?throwExceptionOnFailure=false")
                    .to("mock:verifyNotFound");

            }
        };
    }
}
