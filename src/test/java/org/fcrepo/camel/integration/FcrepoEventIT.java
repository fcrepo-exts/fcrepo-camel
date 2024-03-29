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

import static java.util.UUID.randomUUID;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.component.activemq.ActiveMQComponent.activeMQComponent;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
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
public class FcrepoEventIT extends CamelTestSupport {

    @EndpointInject("mock:type")
    protected MockEndpoint typeEndpoint;

    @EndpointInject("mock:id")
    protected MockEndpoint idEndpoint;

    @EndpointInject("mock:agent")
    protected MockEndpoint agentEndpoint;

    @EndpointInject("mock:wasGeneratedBy")
    protected MockEndpoint wasGeneratedByEndpoint;

    @Produce("direct:create")
    protected ProducerTemplate template;

    private final String container = randomUUID().toString();

    @Test
    public void testGetMessage() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String baseContainer = "http://localhost:" + webPort + "/fcrepo/rest/" + container;
        final String fcrepoResource = "http://fedora.info/definitions/v4/repository#Resource";

        resetMocks();

        idEndpoint.expectedMessageCount(3);
        idEndpoint.allMessages().header(FCREPO_URI).startsWith(baseContainer);
        idEndpoint.allMessages().header(FCREPO_RESOURCE_TYPE).contains(fcrepoResource);
        idEndpoint.allMessages().header(FCREPO_EVENT_TYPE).isNotNull();
        idEndpoint.allMessages().header(FCREPO_AGENT).regex(".+?fedoraAdmin.+?");
        idEndpoint.allMessages().header(FCREPO_EVENT_ID).startsWith("urn:uuid:");
        idEndpoint.allMessages().header(FCREPO_DATE_TIME).isNotNull();

        typeEndpoint.expectedMessageCount(3);
        typeEndpoint.allMessages().header(FCREPO_URI).startsWith(baseContainer);
        typeEndpoint.allMessages().header(FCREPO_RESOURCE_TYPE).contains(fcrepoResource);
        typeEndpoint.allMessages().header(FCREPO_EVENT_TYPE).isNotNull();
        typeEndpoint.allMessages().header(FCREPO_AGENT).regex(".+?fedoraAdmin.+?");
        typeEndpoint.allMessages().header(FCREPO_EVENT_ID).startsWith("urn:uuid:");
        typeEndpoint.allMessages().header(FCREPO_DATE_TIME).isNotNull();

        wasGeneratedByEndpoint.expectedMessageCount(3);
        wasGeneratedByEndpoint.allMessages().header(FCREPO_URI).startsWith(baseContainer);
        wasGeneratedByEndpoint.allMessages().header(FCREPO_RESOURCE_TYPE).contains(fcrepoResource);
        wasGeneratedByEndpoint.allMessages().header(FCREPO_EVENT_TYPE).isNotNull();
        wasGeneratedByEndpoint.allMessages().header(FCREPO_AGENT).regex(".+?fedoraAdmin.+?");
        wasGeneratedByEndpoint.allMessages().header(FCREPO_EVENT_ID).startsWith("urn:uuid:");
        wasGeneratedByEndpoint.allMessages().header(FCREPO_DATE_TIME).isNotNull();

        agentEndpoint.expectedMessageCount(3);
        agentEndpoint.allMessages().header(FCREPO_URI).startsWith(baseContainer);
        agentEndpoint.allMessages().header(FCREPO_RESOURCE_TYPE).contains(fcrepoResource);
        agentEndpoint.allMessages().header(FCREPO_EVENT_TYPE).isNotNull();
        agentEndpoint.allMessages().header(FCREPO_AGENT).regex(".+?fedoraAdmin.+?");
        agentEndpoint.allMessages().header(FCREPO_EVENT_ID).startsWith("urn:uuid:");
        agentEndpoint.allMessages().header(FCREPO_DATE_TIME).isNotNull();

        template.sendBody("direct:setup", null);
        template.sendBodyAndHeader("direct:create", getTurtleDocument(), CONTENT_TYPE, "text/turtle");
        template.sendBodyAndHeader("direct:create", getTurtleDocument(), CONTENT_TYPE, "text/turtle");

        idEndpoint.assertIsSatisfied();
        typeEndpoint.assertIsSatisfied();
        wasGeneratedByEndpoint.assertIsSatisfied();
        agentEndpoint.assertIsSatisfied();
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
                    .process(new EventProcessor())
                    .filter(header(FCREPO_URI).startsWith("http://localhost:" + webPort + "/fcrepo/rest/" + container))
                    .multicast()
                    .to("direct:type", "direct:id", "direct:wasGeneratedBy",
                            "direct:agent");

                from("direct:type")
                    .filter(header(FCREPO_RESOURCE_TYPE)
                            .contains("http://fedora.info/definitions/v4/repository#Resource"))
                    .to("mock:type");

                from("direct:id")
                    .filter(header(FCREPO_EVENT_ID).startsWith("urn:uuid:"))
                    .to("mock:id");

                from("direct:agent")
                    .filter(header(FCREPO_AGENT).regex(".+?fedoraAdmin.+?"))
                    .to("mock:agent");

                from("direct:wasGeneratedBy")
                    .filter(header(FCREPO_EVENT_TYPE).regex(".+?Creat.+?"))
                    .to("mock:wasGeneratedBy");

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
