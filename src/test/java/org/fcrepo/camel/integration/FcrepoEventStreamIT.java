/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.integration;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.processor.EventProcessor;
import org.junit.Test;

import java.io.InputStream;

import static java.util.UUID.randomUUID;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.component.activemq.ActiveMQComponent.activeMQComponent;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_ID;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.integration.FcrepoTestUtils.AUTH_QUERY_PARAMS;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getTurtleDocument;

/**
 * Test handling a Fedora Event
 * @author Aaron Coburn
 * @since September 14, 2016
 */
public class FcrepoEventStreamIT extends CamelTestSupport {

    @EndpointInject("mock:results")
    protected MockEndpoint resultsEndpoint;

    @Produce("direct:create")
    protected ProducerTemplate template;

    private final String container = randomUUID().toString();

    @Test
    public void testGetMessage() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String baseContainer = "http://localhost:" + webPort + "/fcrepo/rest/" + container;
        final String fcrepoResource = "http://fedora.info/definitions/v4/repository#Resource";

        resetMocks();

        resultsEndpoint.expectedMessageCount(6);
        resultsEndpoint.allMessages().header(FCREPO_URI).startsWith(baseContainer);
        resultsEndpoint.allMessages().header(FCREPO_RESOURCE_TYPE).contains(fcrepoResource);
        resultsEndpoint.allMessages().header(FCREPO_EVENT_TYPE).isNotNull();
        resultsEndpoint.allMessages().header(FCREPO_AGENT).regex(".+?fedoraAdmin.+?");
        resultsEndpoint.allMessages().header(FCREPO_EVENT_ID).startsWith("urn:uuid:");
        resultsEndpoint.allMessages().header(FCREPO_DATE_TIME).isNotNull();

        template.sendBody("direct:setup", null);
        template.sendBodyAndHeader("direct:create", getTurtleDocument(), CONTENT_TYPE, "text/turtle");
        template.sendBodyAndHeader("direct:create", getTurtleDocument(), CONTENT_TYPE, "text/turtle");

        resultsEndpoint.assertIsSatisfied();
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
                    .convertBodyTo(String.class)
                    .multicast().to("direct:process", "direct:stream");

                from("direct:stream")
                    .convertBodyTo(InputStream.class)
                    .to("direct:process");

                from("direct:process")
                    .process(new EventProcessor())
                    .filter()
                    .simple("${header.CamelFcrepoUri} starts with 'http://localhost:" +
                            webPort + "/fcrepo/rest/" + container + "'")
                    .to("mock:results");

                from("direct:setup")
                    .setHeader(HTTP_METHOD).constant("PUT")
                    .to("http:localhost:" + webPort + "/fcrepo/rest/" + container + AUTH_QUERY_PARAMS);

                from("direct:create")
                    .setHeader(HTTP_METHOD).constant("POST")
                    .to("http:localhost:" + webPort + "/fcrepo/rest/" + container + AUTH_QUERY_PARAMS);
            }
        };
    }
}
