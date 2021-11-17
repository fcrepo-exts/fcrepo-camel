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
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.UUID.randomUUID;

/**
 * Test adding an RDF resource with PUT
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@Ignore
public class FcrepoPutIT extends CamelTestSupport {

    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    @EndpointInject("mock:created")
    protected MockEndpoint createdEndpoint;

    @EndpointInject("mock:deleted")
    protected MockEndpoint deletedEndpoint;

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Test
    public void testPut() throws InterruptedException {

        final String path1 = "/" + randomUUID();
        final String path2 = "/" + randomUUID();

        // Assertions
        resultEndpoint.expectedMessageCount(2);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/rdf+xml");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        createdEndpoint.expectedMessageCount(2);
        createdEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 201);

        deletedEndpoint.expectedMessageCount(2);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        // Setup
        final Map<String, Object> setupHeaders = new HashMap<>();
        setupHeaders.put(Exchange.HTTP_METHOD, "PUT");
        setupHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, path1);
        setupHeaders.put(Exchange.CONTENT_TYPE, "text/turtle");
        template.sendBodyAndHeaders("direct:setup", FcrepoTestUtils.getTurtleDocument(), setupHeaders);

        setupHeaders.clear();
        setupHeaders.put(Exchange.HTTP_METHOD, "PUT");
        setupHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, path2);
        template.sendBodyAndHeaders("direct:setup2", FcrepoTestUtils.getTurtleDocument(), setupHeaders);

        // Test
        template.sendBodyAndHeader(null, FcrepoHeaders.FCREPO_IDENTIFIER, path1);
        template.sendBodyAndHeader(null, FcrepoHeaders.FCREPO_IDENTIFIER, path2);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, path1);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        teardownHeaders.clear();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, path2);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that assertions passed
        resultEndpoint.assertIsSatisfied();
        createdEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                final String fcrepo_uri = FcrepoTestUtils.getFcrepoEndpointUri();

                final Namespaces ns = new Namespaces("rdf", RDF.uri);

                from("direct:setup")
                    .to(fcrepo_uri)
                    .to("mock:created");

                from("direct:setup2")
                    .to(fcrepo_uri + "&contentType=text/turtle")
                    .to("mock:created");

                from("direct:start")
                    .to(fcrepo_uri)
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='" + REPOSITORY + "Resource']", ns)
                    .to("mock:result");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to("mock:deleted");
            }
        };
    }
}
