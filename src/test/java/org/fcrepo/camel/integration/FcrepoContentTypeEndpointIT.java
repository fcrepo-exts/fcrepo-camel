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

import static org.apache.camel.Exchange.ACCEPT_CONTENT_TYPE;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getFcrepoEndpointUri;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the retrieved content-type from a fcrepo endpoint
 * when the Accept type is put directly on the endpoint.
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@Ignore
public class FcrepoContentTypeEndpointIT extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    public ProducerTemplate template;

    @Test
    public void testContentTypeTurtle() throws InterruptedException {
        resultEndpoint.expectedMessagesMatches(e -> e.getIn().getHeader("Content-Type", String.class)
                .contains("text/turtle"));
        resultEndpoint.expectedMessageCount(1);

        template.sendBody(null);

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeN3() throws InterruptedException {
        resultEndpoint.expectedMessagesMatches(e -> e.getIn().getHeader("Content-Type", String.class)
                .contains("text/turtle"));
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/n-triples");
        template.sendBodyAndHeader(null, ACCEPT_CONTENT_TYPE, "application/n-triples");

        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {

            @Override
            public void configure() {
                final String fcrepo_uri = getFcrepoEndpointUri();

                from("direct:start")
                        .to(fcrepo_uri + "&accept=text/turtle")
                        .to("mock:result");
            }
        };
    }
}
