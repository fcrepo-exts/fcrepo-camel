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
package org.fcrepo.camel.integration;

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
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.RdfNamespaces;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test adding a resource at a specific path
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FcrepoPathIT extends CamelTestSupport {

    @EndpointInject(uri = "mock:deleted")
    protected MockEndpoint deletedEndpoint;

    @EndpointInject(uri = "mock:created")
    protected MockEndpoint createdEndpoint;

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:check")
    protected ProducerTemplate template;

    @Test
    public void testPath() throws InterruptedException {
        final String path = "/test/a/b/c/d";

        // Assertions
        resultEndpoint.expectedMessageCount(3);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/rdf+xml");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        createdEndpoint.expectedMessageCount(1);
        createdEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 201);

        deletedEndpoint.expectedMessageCount(2);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        // Setup
        final Map<String, Object> setupHeaders = new HashMap<>();
        setupHeaders.put(Exchange.HTTP_METHOD, "PUT");
        setupHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        setupHeaders.put(Exchange.CONTENT_TYPE, "text/turtle");
        template.sendBodyAndHeaders("direct:setup", FcrepoTestUtils.getTurtleDocument(), setupHeaders);

        // Test
        template.sendBodyAndHeader(null, JmsHeaders.IDENTIFIER, path);
        template.sendBodyAndHeader(null, FcrepoHeaders.FCREPO_IDENTIFIER, path);
        template.sendBody("direct:checkPath", null);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that assertions passed
        resultEndpoint.assertIsSatisfied();
        createdEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {

                final String fcrepo_uri = FcrepoTestUtils.getFcrepoEndpointUri();

                final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);

                from("direct:setup")
                    .to(fcrepo_uri)
                    .to("mock:created");

                from("direct:check")
                    .to(fcrepo_uri)
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='" + RdfNamespaces.REPOSITORY + "Resource']", ns)
                    .to("mock:result");

                from("direct:checkPath")
                    .to(fcrepo_uri + "/test/a/b/c/d")
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='" + RdfNamespaces.REPOSITORY + "Resource']", ns)
                    .to("mock:result");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to("mock:deleted")
                    .to(fcrepo_uri + "?tombstone=true")
                    .to("mock:deleted");
            }
        };
    }
}
