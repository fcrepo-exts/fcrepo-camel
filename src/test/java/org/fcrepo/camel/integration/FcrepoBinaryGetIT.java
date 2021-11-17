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
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class FcrepoBinaryGetIT extends CamelTestSupport {

    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    private static final String PREMIS = "http://www.loc.gov/premis/rdf/v1#";

    @EndpointInject("mock:created")
    protected MockEndpoint createdEndpoint;

    @EndpointInject("mock:filter")
    protected MockEndpoint filteredEndpoint;

    @EndpointInject("mock:binary")
    protected MockEndpoint binaryEndpoint;

    @EndpointInject("mock:verifyGone")
    protected MockEndpoint goneEndpoint;

    @EndpointInject("mock:deleted")
    protected MockEndpoint deletedEndpoint;

    @EndpointInject("mock:fixity")
    protected MockEndpoint fixityEndpoint;

    @Produce("direct:filter")
    protected ProducerTemplate template;

    @Test
    public void testGetBinary() throws InterruptedException {
        // Assertions
        createdEndpoint.expectedMessageCount(2);
        createdEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 201);

        final var expectedMessage = FcrepoTestUtils.getTextDocument();
        binaryEndpoint.expectedBodiesReceived(expectedMessage);
        binaryEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        binaryEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        filteredEndpoint.expectedMessageCount(1);
        filteredEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/rdf+xml");
        filteredEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        deletedEndpoint.expectedBodiesReceived(null, null);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        goneEndpoint.expectedMessageCount(2);
        goneEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 410);

        fixityEndpoint.expectedMessageCount(3);
        fixityEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        final String binary = "/file";
        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String uri = template.requestBodyAndHeaders(
                "direct:create",
                FcrepoTestUtils.getTurtleDocument(),
                headers, String.class);

        headers.clear();
        headers.put(Exchange.HTTP_METHOD, "PUT");
        headers.put(Exchange.CONTENT_TYPE, "text/plain");
        headers.put(FCREPO_URI, uri + binary);

        template.sendBodyAndHeaders("direct:create", FcrepoTestUtils.getTextDocument(), headers);

        template.sendBodyAndHeader("direct:get", null, FCREPO_URI, uri);
        template.sendBodyAndHeader("direct:get", null, FCREPO_URI, uri + binary);

        template.sendBodyAndHeader("direct:getFixity", null, FCREPO_URI, uri + binary);

        template.sendBodyAndHeader("direct:delete", null, FCREPO_URI, uri + binary);
        template.sendBodyAndHeader("direct:delete", null, FCREPO_URI, uri);


        // Confirm that assertions passed
        createdEndpoint.assertIsSatisfied();
        filteredEndpoint.assertIsSatisfied();
        binaryEndpoint.assertIsSatisfied();
        goneEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();
        fixityEndpoint.assertIsSatisfied();

        // Additional assertions
        // skip first message, as we've already extracted the body
        assertEquals(uri + binary,
                createdEndpoint.getExchanges().get(1).getIn().getBody(String.class));
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {

                final String fcrepo_uri = FcrepoTestUtils.getFcrepoEndpointUri();

                final Namespaces ns = new Namespaces("rdf", RDF.uri);
                ns.add("premis", PREMIS);
                ns.add("fedora", REPOSITORY);

                from("direct:create")
                    .to(fcrepo_uri)
                    .to("mock:created");

                from("direct:get")
                    .to(fcrepo_uri)
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='" + REPOSITORY + "Binary']", ns)
                    .to("mock:filter")
                    .to(fcrepo_uri + "&metadata=false")
                    .to("mock:binary");

                from("direct:getFixity")
                        .to(fcrepo_uri + "&fixity=true")
                        .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                            "[@rdf:resource='" + PREMIS + "Fixity']", ns)
                        .to("mock:fixity")
                        .filter().xpath(
                        "/rdf:RDF/rdf:Description/premis:hasMessageDigest" +
                            "[@rdf:resource='urn:sha-512:5c3898de261ad170e7dcc83c6966c1788553b64a1f8dcd15d3aaffff0c" +
                            "10095e44eb8fe986bb6b23baebe4949e966814142cba4cbe8eba004a1f8dcb16056a5c']", ns)
                        .to("mock:fixity")
                        .filter().xpath(
                        "/rdf:RDF/rdf:Description/premis:hasEventOutcome" +
                            "[text()='SUCCESS']", ns)
                        .to("mock:fixity");

                from("direct:delete")
                    .setHeader(Exchange.HTTP_METHOD, constant("DELETE"))
                    .to(fcrepo_uri)
                    .to("mock:deleted")
                    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                    .to(fcrepo_uri + "&throwExceptionOnFailure=false")
                    .to("mock:verifyGone");
            }
        };
    }
}
