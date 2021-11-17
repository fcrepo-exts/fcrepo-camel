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
import org.junit.Test;

import static org.apache.camel.Exchange.ACCEPT_CONTENT_TYPE;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getFcrepoEndpointUri;

/**
 * Test the retrieved content-type from a fcrepo endpoint
 * when the Accept type is put directly on the endpoint.
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class FcrepoContentTypeEndpointIT extends CamelTestSupport {

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
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
