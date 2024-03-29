/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.camel;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_ID;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.processor.EventProcessor;
import org.junit.Test;

/**
 * @author acoburn
 */
public class EventProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testEventProcessor() throws IOException, InterruptedException {

        final List<String> agents = new ArrayList<>();
        agents.add("info:fedora/local-user#bypassAdmin");
        agents.add("CLAW client/1.0");

        final List<String> eventTypes = new ArrayList<>();
        eventTypes.add("https://www.w3.org/ns/activitystreams#Create");

        final List<String> resourceTypes = new ArrayList<>();
        resourceTypes.add("http://www.w3.org/ns/ldp#Container");
        resourceTypes.add("http://fedora.info/definitions/v4/repository#Resource");
        resourceTypes.add("http://fedora.info/definitions/v4/repository#Container");
        resourceTypes.add("http://www.w3.org/ns/ldp#RDFSource");
        resourceTypes.add("http://www.w3.org/ns/ldp#BasicContainer");
        resourceTypes.add("http://www.w3.org/ns/prov#Entity");
        resourceTypes.add("http://example.org/CustomType");

        template.sendBody("direct:start", loadResourceAsStream("event.json"));
        template.sendBody("direct:string", loadResourceAsStream("event.json"));
        template.sendBody("direct:json", loadResourceAsStream("event.json"));

        // Confirm that assertions passed
        resultEndpoint.expectedMessageCount(3);
        resultEndpoint.expectedHeaderReceived(FCREPO_URI, "http://localhost:8080/fcrepo/rest/path/to/resource");
        resultEndpoint.expectedHeaderReceived(FCREPO_RESOURCE_TYPE, resourceTypes);
        resultEndpoint.expectedHeaderReceived(FCREPO_DATE_TIME, "2016-05-19T17:17:39-04:00Z");
        resultEndpoint.expectedHeaderReceived(FCREPO_EVENT_ID, "urn:uuid:3c834a8f-5638-4412-aa4b-35ea80416a18");
        resultEndpoint.expectedHeaderReceived(FCREPO_EVENT_TYPE, eventTypes);
        resultEndpoint.expectedHeaderReceived(FCREPO_AGENT, agents);

        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                from("direct:json")
                    .unmarshal().json(Jackson)
                    .process(new EventProcessor())
                    .to("mock:result");

                from("direct:string")
                    .convertBodyTo(String.class)
                    .process(new EventProcessor())
                    .to("mock:result");

                from("direct:start")
                    .process(new EventProcessor())
                    .to("mock:result");
            }
        };
    }
}
