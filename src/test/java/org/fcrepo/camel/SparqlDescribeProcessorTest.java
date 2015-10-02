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

import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.ACCEPT_CONTENT_TYPE;
import static java.net.URLEncoder.encode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.processor.SparqlDescribeProcessor;
import org.junit.Test;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class SparqlDescribeProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void missingHeaders() throws IOException, InterruptedException {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, "/foo");
        template.sendBodyAndHeaders(null, headers);
        resultEndpoint.expectedMessageCount(0);
        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testDescribe() throws IOException, InterruptedException {
        final String base = "http://localhost/rest";
        final String path = "/path/a/b/c/d";

        // Assertions
        resultEndpoint.expectedBodiesReceived("query=" + encode("DESCRIBE <" + base + path + ">", "UTF-8"));
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, "POST");
        resultEndpoint.expectedHeaderReceived(ACCEPT_CONTENT_TYPE, "application/rdf+xml");

        // Test
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, base);
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        template.sendBodyAndHeaders(null, headers);

        headers.clear();
        headers.put(JmsHeaders.BASE_URL, base);
        headers.put(JmsHeaders.IDENTIFIER, path);
        template.sendBodyAndHeaders(null, headers);

        headers.clear();
        headers.put(JmsHeaders.BASE_URL, base);
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        template.sendBodyAndHeaders(null, headers);

        headers.clear();
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, base);
        headers.put(JmsHeaders.IDENTIFIER, path);
        template.sendBodyAndHeaders(null, headers);

        headers.clear();
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, base + path);
        template.sendBodyAndHeaders(null, headers);

        // Confirm that assertions passed
        resultEndpoint.expectedMessageCount(5);
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                onException(IOException.class)
                    .handled(true);

                from("direct:start")
                    .process(new SparqlDescribeProcessor())
                    .to("mock:result");
            }
        };
    }
}
