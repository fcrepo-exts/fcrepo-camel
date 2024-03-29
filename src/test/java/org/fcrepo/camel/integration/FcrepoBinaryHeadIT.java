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
import org.apache.camel.support.builder.Namespaces;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.camel.FcrepoHeaders;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class FcrepoBinaryHeadIT extends CamelTestSupport {

    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    @EndpointInject("mock:created")
    protected MockEndpoint createdEndpoint;

    @EndpointInject("mock:filter")
    protected MockEndpoint filteredEndpoint;

    @EndpointInject("mock:binary")
    protected MockEndpoint binaryEndpoint;

    @EndpointInject("mock:verify")
    protected MockEndpoint goneEndpoint;

    @EndpointInject("mock:deleted")
    protected MockEndpoint deletedEndpoint;

    @Produce("direct:filter")
    protected ProducerTemplate template;

    @Test
    public void testHeadBinary() throws InterruptedException {
        // Assertions
        createdEndpoint.expectedMessageCount(2);
        createdEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 201);

        binaryEndpoint.allMessages().body().equals(null);
        binaryEndpoint.expectedMessageCount(1);
        binaryEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        binaryEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        filteredEndpoint.expectedMessageCount(1);
        filteredEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/rdf+xml");
        filteredEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        deletedEndpoint.expectedMessageCount(2);
        deletedEndpoint.allMessages().body().equals(null);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        goneEndpoint.expectedMessageCount(2);
        goneEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 410);

        final String binary = "/file";
        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:create",
                FcrepoTestUtils.getTurtleDocument(),
                headers, String.class);

        // Strip off the baseUrl to get the resource path
        final String identifier = fullPath.replaceAll(FcrepoTestUtils.getFcrepoBaseUrl(), "");

        headers.clear();
        headers.put(Exchange.HTTP_METHOD, "PUT");
        headers.put(Exchange.CONTENT_TYPE, "text/plain");
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier + binary);

        template.sendBodyAndHeaders("direct:create", FcrepoTestUtils.getTextDocument(), headers);


        template.sendBodyAndHeader("direct:head", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeader("direct:head", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + binary);

        template.sendBodyAndHeader("direct:delete", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier + binary);
        template.sendBodyAndHeader("direct:delete", null, FcrepoHeaders.FCREPO_IDENTIFIER, identifier);

        // Confirm that assertions passed
        createdEndpoint.assertIsSatisfied();
        filteredEndpoint.assertIsSatisfied();
        binaryEndpoint.assertIsSatisfied();
        goneEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();

        // skip first message, as we've already extracted the body
        assertEquals(FcrepoTestUtils.getFcrepoBaseUrl() + identifier + binary,
                createdEndpoint.getExchanges().get(1).getIn().getBody(String.class));
     }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {

                final String fcrepo_uri = FcrepoTestUtils.getFcrepoEndpointUri();

                final Namespaces ns = new Namespaces("rdf", RDF.uri);

                from("direct:create")
                    .to(fcrepo_uri)
                    .to("mock:created");

                from("direct:head")
                    .to(fcrepo_uri)
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='" + REPOSITORY + "Binary']", ns)
                    .to("mock:filter")
                    .setHeader(Exchange.HTTP_METHOD, constant("HEAD"))
                    .to(fcrepo_uri + "&metadata=false")
                    .to("mock:binary");

                 from("direct:delete")
                    .setHeader(Exchange.HTTP_METHOD, constant("DELETE"))
                    .to(fcrepo_uri)
                    .to("mock:deleted")
                    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                    .to(fcrepo_uri + "&throwExceptionOnFailure=false")
                    .to("mock:verify");
            }
        };
    }
}
