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

import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.ArrayUtils.reverse;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getFcrepoEndpointUri;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getN3Document;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getTurtleDocument;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.junit.Test;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class SparqlUpdateProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testDescribe() throws IOException, InterruptedException {
        final String base = "http://localhost/rest";
        final String path = "/path/a/b/c/d";
        final String document = getN3Document();

        // Reverse the lines as the RDF is serialized in opposite order
        final String[] lines = document.split("\n");
        reverse(lines);

        // Assertions
        resultEndpoint.expectedBodiesReceived("DELETE WHERE { <" + base + path + "> ?p ?o }; " +
                                              "INSERT { " + join(lines, " ") + " } " +
                                              "WHERE { }");
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/sparql-update");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        // Test
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, base);
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        headers.put(Exchange.CONTENT_TYPE, "application/n-triples");
        template.sendBodyAndHeaders(document, headers);

        headers.clear();
        headers.put(JmsHeaders.BASE_URL, base);
        headers.put(JmsHeaders.IDENTIFIER, path);
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");
        template.sendBodyAndHeaders(getTurtleDocument(), headers);

        headers.clear();
        headers.put(JmsHeaders.BASE_URL, base);
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        headers.put(Exchange.CONTENT_TYPE, "application/n-triples");
        template.sendBodyAndHeaders(document, headers);

        headers.clear();
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, base);
        headers.put(JmsHeaders.IDENTIFIER, path);
        headers.put(Exchange.CONTENT_TYPE, "application/n-triples");
        template.sendBodyAndHeaders(document, headers);

        // Confirm that assertions passed
        resultEndpoint.expectedMessageCount(4);
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {

                final String fcrepo_uri = getFcrepoEndpointUri();

                from("direct:start")
                    .process(new SparqlUpdateProcessor())
                    // Normalize the whitespace to make it easier to compare
                    .process(new Processor() {
                        public void process(final Exchange exchange) throws Exception {
                           final String payload = exchange.getIn().getBody(String.class);
                           exchange.getIn().setBody(normalizeSpace(payload));
                       }
                    })
                    .to("mock:result");
            }
        };
    }
}
