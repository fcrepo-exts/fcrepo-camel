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

import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getFcrepoEndpointUri;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getN3Document;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getTurtleDocument;
import static java.net.URLEncoder.encode;

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
import org.fcrepo.camel.processor.SparqlInsertProcessor;
import org.junit.Test;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class SparqlInsertProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testInsert() throws IOException, InterruptedException {
        final String base = "http://localhost/rest";
        final String path = "/path/a/b/c";
        final String document = getN3Document();

        // Assertions
        resultEndpoint.allMessages().body().startsWith("update=" + encode("INSERT DATA { ", "UTF-8"));
        resultEndpoint.allMessages().body().endsWith(encode("}", "UTF-8"));
        for (final String s : document.split("\n")) {
            resultEndpoint.expectedBodyReceived().body().contains(encode(s, "UTF-8"));
        }
        resultEndpoint.expectedBodyReceived().body().contains(
                encode("<" + base + path + "> dc:title \"some title & other\" .", "UTF-8"));
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
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
        headers.put(Exchange.CONTENT_TYPE, "application/n-triples");
        template.sendBodyAndHeaders(document, headers);

        headers.clear();
        headers.put(JmsHeaders.BASE_URL, base);
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");
        template.sendBodyAndHeaders(getTurtleDocument(), headers);

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
                    .process(new SparqlInsertProcessor())
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
