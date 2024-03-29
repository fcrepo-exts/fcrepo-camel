/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_NAMED_GRAPH;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getFcrepoEndpointUri;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getN3Document;
import static org.fcrepo.camel.integration.FcrepoTestUtils.getTurtleDocument;
import static java.net.URLEncoder.encode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.junit.Test;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class SparqlUpdateProcessorTest extends CamelTestSupport {

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Test
    public void testNamedGraph() throws IOException, InterruptedException {
        final String uri = "http://localhost/rest/path/a/b/c";
        final String graph = "foo";
        final String document = getN3Document();
        // Reverse the lines as the RDF may be serialized in opposite order

        final String responsePrefix =
              "DELETE WHERE { GRAPH <" + graph + "> { <" + uri + "> ?p ?o } };\n" +
              "INSERT DATA { GRAPH <" + graph + "> { ";
        final String responseSuffix = "\n} }";

        // Assertions
        resultEndpoint.allMessages().body().startsWith("update=" + encode(responsePrefix, "UTF-8"));
        resultEndpoint.allMessages().body().endsWith(encode(responseSuffix, "UTF-8"));
        for (final String s : document.split("\n")) {
            resultEndpoint.expectedBodyReceived().body().contains(encode(s, "UTF-8"));
        }
        resultEndpoint.expectedBodyReceived().body().contains(
                encode("<" + uri + "> dc:title \"some title\" .", "UTF-8"));
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, "POST");

        // Test
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, uri);
        headers.put(FCREPO_NAMED_GRAPH, graph);
        headers.put(CONTENT_TYPE, "application/n-triples");
        template.sendBodyAndHeaders(document, headers);

        // Confirm that assertions passed
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testUpdate() throws IOException, InterruptedException {
        final String uri = "http://localhost/rest/path/a/b/c";
        final String document = getTurtleDocument();
        // Reverse the lines as the RDF may be serialized in opposite order

        final String responsePrefix =
                  "DELETE WHERE { <" + uri + "> ?p ?o };\n" +
                  "INSERT DATA { ";
        final String responseSuffix = "\n}";

        // Assertions
        resultEndpoint.allMessages().body().startsWith("update=" + encode(responsePrefix, "UTF-8"));
        resultEndpoint.allMessages().body().endsWith(encode(responseSuffix, "UTF-8"));
        for (final String s : document.split("\n")) {
            resultEndpoint.expectedBodyReceived().body().contains(encode(s, "UTF-8"));
        }
        resultEndpoint.expectedBodyReceived().body().contains(
                encode("<" + uri + "> dc:title \"some title\" .", "UTF-8"));
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, "POST");

        // Test
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, uri);
        headers.put(CONTENT_TYPE, "text/turtle");
        template.sendBodyAndHeaders(document, headers);

        // Confirm that assertions passed
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {

                final String fcrepo_uri = getFcrepoEndpointUri();

                from("direct:start")
                    .process(new SparqlUpdateProcessor())
                    // Normalize the whitespace to make it easier to compare
                    .process(new Processor() {
                        public void process(final Exchange exchange) throws Exception {
                           final String payload = exchange.getIn().getBody(String.class);
                           exchange.getIn().setBody(normalizeSpace(payload));
                       }
                    })
                    .to("mock:result");
            }
        };
    }
}
