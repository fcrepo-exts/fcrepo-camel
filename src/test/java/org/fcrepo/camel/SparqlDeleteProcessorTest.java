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
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.junit.Test;

import java.io.IOException;

import static java.net.URLEncoder.encode;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class SparqlDeleteProcessorTest extends CamelTestSupport {

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
    public void testDelete() throws IOException, InterruptedException {
        final String base = "http://localhost/rest/";
        final String uri = "path/book3";
        final String incomingDoc =
            "<rdf:RDF" +
            "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"" +
            "    xmlns:vcard=\"http://www.w3.org/2001/vcard-rdf/3.0#\"" +
            "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
            "    xmlns=\"" + uri + "\">" +
            "  <rdf:Description rdf:about=\"" + base + "\">" +
            "    <dc:title>Silas Marner</dc:title>" +
            "  </rdf:Description>" +
            "  <rdf:Description rdf:about=\"" + uri + "\">" +
            "    <dc:title>Middlemarch</dc:title>" +
            "    <dc:relation rdf:resource=\"" + uri + "/appendix\"/>" +
            "    <dc:relation rdf:resource=\"" + uri + "#appendix2\"/>" +
            "    <dc:relation rdf:resource=\"http://some-other-uri/appendix3\"/>" +
            "    <dc:relation rdf:resource=\"" + uri + "\"/>" +
            "    <dc:creator rdf:parseType=\"Resource\">" +
            "      <vcard:FN>George Elliot</vcard:FN>" +
            "      <vcard:N rdf:parseType=\"Resource\">" +
            "        <vcard:Family>Elliot</vcard:Family>" +
            "        <vcard:Given>George</vcard:Given>" +
            "      </vcard:N>" +
            "    </dc:creator>" +
            "  </rdf:Description>" +
            "</rdf:RDF>";

        // Assertions
        final var expectedBody = "update=" +
                encode("DELETE WHERE { <" + uri + "> ?p ?o }", "UTF-8");
        resultEndpoint.expectedBodiesReceived(expectedBody, expectedBody);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE,
                "application/x-www-form-urlencoded; charset=utf-8");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        // Test
        template.sendBodyAndHeader(incomingDoc, FCREPO_URI, uri);
        template.sendBodyAndHeader(incomingDoc, FCREPO_BASE_URL, uri);

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
                    .process(new SparqlDeleteProcessor())
                    .to("mock:result");
            }
        };
    }
}
