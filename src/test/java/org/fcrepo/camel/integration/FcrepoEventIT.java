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

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getTurtleDocument;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

/**
 * Test handling a Fedora Event
 * @author Aaron Coburn
 * @since September 14, 2016
 */
public class FcrepoEventIT extends CamelTestSupport {

    @EndpointInject(uri = "mock:type")
    protected MockEndpoint typeEndpoint;

    @EndpointInject(uri = "mock:id")
    protected MockEndpoint idEndpoint;

    @EndpointInject(uri = "mock:isPartOf")
    protected MockEndpoint isPartOfEndpoint;

    @EndpointInject(uri = "mock:wasAttributedTo")
    protected MockEndpoint wasAttributedToEndpoint;

    @EndpointInject(uri = "mock:wasGeneratedBy")
    protected MockEndpoint wasGeneratedByEndpoint;

    @Produce(uri = "direct:create")
    protected ProducerTemplate template;

    @Test
    public void testGetMessage() throws Exception {
        resetMocks();

        isPartOfEndpoint.expectedMessageCount(5);
        idEndpoint.expectedMessageCount(5);
        typeEndpoint.expectedMessageCount(5);
        wasGeneratedByEndpoint.expectedMessageCount(3);
        wasAttributedToEndpoint.expectedMessageCount(5);

        template.sendBody("direct:setup", null);
        template.sendBodyAndHeader("direct:create", getTurtleDocument(), CONTENT_TYPE, "text/turtle");
        template.sendBodyAndHeader("direct:create", getTurtleDocument(), CONTENT_TYPE, "text/turtle");

        idEndpoint.assertIsSatisfied();
        typeEndpoint.assertIsSatisfied();
        wasGeneratedByEndpoint.assertIsSatisfied();
        wasAttributedToEndpoint.assertIsSatisfied();
        isPartOfEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");

        context().addComponent("activemq", activeMQComponent("tcp://localhost:" + jmsPort));

        return new RouteBuilder() {
            @Override
            public void configure() {
                from("activemq:queue:fedora")
                    .unmarshal().json(Jackson)
                    .filter()
                    .simple("${body[id]} starts with 'http://localhost:" + webPort + "/fcrepo/rest/event'")
                    .multicast()
                    .to("direct:type", "direct:id", "direct:isPartOf", "direct:wasGeneratedBy",
                            "direct:wasAttributedTo");

                from("direct:type")
                    .filter()
                    .simple("'http://fedora.info/definitions/v4/repository#Resource' in ${body[type]}")
                    .to("mock:type");

                from("direct:id")
                    .filter()
                    .simple("${body[id]} starts with 'http://localhost:" + webPort + "/fcrepo/rest'")
                    .to("mock:id");

                from("direct:isPartOf")
                    .filter()
                    .simple("'http://localhost:" + webPort + "/fcrepo/rest' == ${body[isPartOf]}")
                    .to("mock:isPartOf");

                from("direct:wasAttributedTo")
                    .split(simple("${body[wasAttributedTo]}"))
                    .filter()
                    .simple("'bypassAdmin' == ${body[name]}")
                    .to("mock:wasAttributedTo");

                from("direct:wasGeneratedBy")
                    .filter()
                    .simple(
                        "'http://fedora.info/definitions/v4/event#ResourceCreation' in ${body[wasGeneratedBy][type]}")
                    .to("mock:wasGeneratedBy");

                from("direct:setup")
                    .setHeader(HTTP_METHOD).constant("PUT")
                    .to("http4:localhost:" + webPort + "/fcrepo/rest/event");

                from("direct:create")
                    .setHeader(HTTP_METHOD).constant("POST")
                    .to("http4:localhost:" + webPort + "/fcrepo/rest/event");
            }
        };
    }
}
