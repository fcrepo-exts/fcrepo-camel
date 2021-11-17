/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.integration;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import static org.fcrepo.camel.FcrepoProducer.DEFAULT_CONTENT_TYPE;

/**
 * Test the retrieved content-type from a fcrepo endpoint
 * when the Accept type is put in a message header.
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class FcrepoContentTypeHeaderIT extends CamelTestSupport {

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Test
    public void testContentTypeJson() throws InterruptedException {
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/ld+json");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/ld+json");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "application/ld+json");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeRdfXml() throws InterruptedException {
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/rdf+xml");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/rdf+xml");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "application/rdf+xml");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeNTriples() throws InterruptedException  {
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/n-triples");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/n-triples");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "application/n-triples");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeTurtle() throws InterruptedException {
        resultEndpoint.expectedMessagesMatches(e -> e.getIn().getHeader("Content-Type", String.class)
                .contains("text/turtle"));
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "text/turtle");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "text/turtle");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeN3() throws InterruptedException {
        resultEndpoint.expectedMessagesMatches(e -> e.getIn().getHeader("Content-Type", String.class)
                .contains("text/rdf+n3"));
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "text/rdf+n3");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "text/rdf+n3");

        resultEndpoint.assertIsSatisfied();
    }


    @Test
    public void testContentTypeDefault() throws InterruptedException {
        resultEndpoint.expectedHeaderReceived("Content-Type", DEFAULT_CONTENT_TYPE);
        resultEndpoint.expectedMessageCount(1);

        template.sendBody(null);

        resultEndpoint.assertIsSatisfied();
    }


    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                final String fcrepo_uri = FcrepoTestUtils.getFcrepoEndpointUri();

                from("direct:start")
                    .to(fcrepo_uri)
                    .to("mock:result");
            }
        };
    }
}
