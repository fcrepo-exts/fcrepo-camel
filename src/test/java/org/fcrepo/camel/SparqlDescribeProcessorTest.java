/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.NoSuchHeaderException;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.processor.SparqlDescribeProcessor;
import org.junit.Test;

import java.io.IOException;

import static java.net.URLEncoder.encode;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class SparqlDescribeProcessorTest extends CamelTestSupport {

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Test
    public void missingHeaders() throws IOException, InterruptedException {
        final Exchange in = new DefaultExchange(context());
        in.getIn().setHeader(FCREPO_IDENTIFIER, "/foo");
        final Exchange out = template.send(in);
        assertTrue(out.isFailed());
        assertTrue(out.getException() instanceof NoSuchHeaderException);
    }

    @Test
    public void testDescribe() throws IOException, InterruptedException {
        final String uri = "http://localhost/rest/path/a/b/c/d";

        // Assertions
        final var expectedBody = "query=" + encode("DESCRIBE <" + uri + ">", "UTF-8");
        resultEndpoint.expectedBodiesReceived(expectedBody, expectedBody);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, "POST");

        // Test
        template.sendBodyAndHeader(null, FCREPO_URI, uri);
        template.sendBodyAndHeader(null, FCREPO_BASE_URL, uri);

        // Confirm that assertions passed
        resultEndpoint.expectedMessageCount(2);
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                onException(IOException.class)
                    .handled(true);

                from("direct:start")
                    .process(new SparqlDescribeProcessor())
                    .to("mock:result");
            }
        };
    }
}
