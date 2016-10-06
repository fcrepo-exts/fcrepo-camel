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
package org.fcrepo.camel;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.processor.LdnProcessor;
import org.junit.Test;

/**
 * @author acoburn
 */
public class LdnProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject(uri = "mock:agent")
    protected MockEndpoint agentEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testLdnProcessor() throws IOException, InterruptedException {
        final String base = "http://localhost:8080/fcrepo/rest";
        final String path = "/path/to/resource";
        final InputStream document = loadResourceAsStream("event.json");

        // Test
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_BASE_URL, base);
        headers.put(FCREPO_IDENTIFIER, path);
        headers.put(CONTENT_TYPE, "application/ld+json");
        template.sendBodyAndHeaders(document, headers);

        // Confirm that assertions passed
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, "POST");
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/ld+json");
        resultEndpoint.assertIsSatisfied();

        agentEndpoint.expectedMessageCount(2);
        agentEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                from("direct:start")
                    .process(new LdnProcessor())
                    .unmarshal().json(Jackson)
                    .split(simple("${body[@graph]}"))
                    .choice()
                    .when().simple("${body[wasAssociatedWith]} and ${body[@id]} == ''")
                        .to("mock:result")
                    .when().simple("${body[name]} and ${body[@id]} == '#agent0'")
                        .to("mock:agent")
                    .when().simple("${body[name]} and ${body[@id]} == '#agent1'")
                        .to("mock:agent");
            }
        };
    }
}
