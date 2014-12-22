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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FcrepoFileIT extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject(uri = "mock:file")
    protected MockEndpoint fileEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testFile() throws InterruptedException {
        // Assertions
        fileEndpoint.expectedBodiesReceived(FcrepoTestUtils.getTextDocument());
        fileEndpoint.expectedMessageCount(1);
        fileEndpoint.expectedHeaderReceived("Content-Type", "text/plain");

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/rdf+xml");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup",
                FcrepoTestUtils.getTurtleDocument(),
                headers, String.class);

        // Strip off the baseUri to get the resource path
        final String identifier = fullPath.replaceAll(FcrepoTestUtils.getFcrepoBaseUrl(), "");

        final Map<String, Object> fileHeaders = new HashMap<>();
        fileHeaders.put(Exchange.HTTP_METHOD, "PUT");
        fileHeaders.put(Exchange.CONTENT_TYPE, "text/plain");
        fileHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/file");
        template.sendBodyAndHeaders("direct:setup", FcrepoTestUtils.getTextDocument(), fileHeaders);

        template.sendBodyAndHeader(null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/file");
        template.sendBodyAndHeader("direct:file", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/file");


        final Map<String, Object> teardownHeaders = new HashMap<>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier + "/file");
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);
        teardownHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that assertions passed
        resultEndpoint.assertIsSatisfied();
        fileEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {

                final String fcrepo_uri = FcrepoTestUtils.getFcrepoEndpointUri();

                final Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

                from("direct:setup")
                    .to(fcrepo_uri);

                from("direct:start")
                    .to(fcrepo_uri)
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='http://fedora.info/definitions/v4/repository#Binary']", ns)
                    .to("mock:result");

                from("direct:file")
                    .to(fcrepo_uri + "?metadata=false")
                    .to("mock:file");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
