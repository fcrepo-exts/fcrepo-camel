/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel.integration;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import org.apache.camel.Produce;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.jena.fuseki.EmbeddedFusekiServer;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.RdfNamespaces;
import org.fcrepo.camel.processor.SparqlInsertProcessor;
import org.fcrepo.camel.processor.SparqlDescribeProcessor;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.junit.runner.RunWith;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.slf4j.Logger;

/**
 * Represents an integration test for interacting with an external triplestore.
 *
 * @author Aaron Coburn
 * @since Nov 8, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FcrepoSparqlIT extends CamelTestSupport {

    final private Logger logger = getLogger(FcrepoSparqlIT.class);

    private static final int FUSEKI_PORT = Integer.parseInt(System.getProperty(
            "test.fuseki.port", "3030"));

    private static EmbeddedFusekiServer server = null;

    @EndpointInject(uri = "mock:sparql.query")
    protected MockEndpoint sparqlQueryEndpoint;

    @EndpointInject(uri = "mock:sparql.update")
    protected MockEndpoint sparqlUpdateEndpoint;

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject(uri = "mock:deleted")
    protected MockEndpoint deletedEndpoint;

    @Produce(uri = "direct:describe.delete.insert")
    protected ProducerTemplate template;

    @Before
    public void setup() throws Exception {
        server = EmbeddedFusekiServer.mem(FUSEKI_PORT, "/test") ;
        logger.info("Starting EmbeddedFusekiServer on port {}", FUSEKI_PORT);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        logger.info("Stopping EmbeddedFusekiServer");
        server.stop();
    }

    @Test
    public void testUpdateSparql() throws Exception {
        // Assertions
        sparqlQueryEndpoint.expectedMessageCount(3);
        sparqlQueryEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        sparqlUpdateEndpoint.expectedMessageCount(1);
        sparqlUpdateEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        deletedEndpoint.expectedMessageCount(2);
        deletedEndpoint.expectedBodiesReceived(null, null);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        // Setup
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup", FcrepoTestUtils.getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(FcrepoTestUtils.getFcrepoBaseUrl(), "");
        final String base_url = "http://localhost:" + System.getProperty("test.port", "8080") + "/rest";

        // Test
        final Map<String, Object> testHeaders = new HashMap<String, Object>();
        testHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        testHeaders.put(FcrepoHeaders.FCREPO_BASE_URL, base_url);

        template.sendBodyAndHeaders("direct:update", null, testHeaders);
        template.sendBodyAndHeaders("direct:count.triples", null, testHeaders);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<String, Object>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that the assertions passed
        resultEndpoint.assertIsSatisfied();
        sparqlQueryEndpoint.assertIsSatisfied();
        sparqlUpdateEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();

    }

    @Test
    public void testInsertDeleteSparql() throws Exception {
        // Assertions
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        sparqlQueryEndpoint.expectedMessageCount(6);
        sparqlQueryEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        sparqlUpdateEndpoint.expectedMessageCount(4);
        sparqlUpdateEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        deletedEndpoint.expectedMessageCount(2);
        deletedEndpoint.expectedBodiesReceived(null, null);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        // Setup
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup", FcrepoTestUtils.getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(FcrepoTestUtils.getFcrepoBaseUrl(), "");
        final String base_url = "http://localhost:" + System.getProperty("test.port", "8080") + "/rest";

        // Test
        final Map<String, Object> testHeaders = new HashMap<String, Object>();
        testHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        testHeaders.put(FcrepoHeaders.FCREPO_BASE_URL, base_url);

        template.sendBodyAndHeaders("direct:insert", null, testHeaders);
        template.sendBodyAndHeaders("direct:delete", null, testHeaders);
        template.sendBodyAndHeaders("direct:count.triples", null, testHeaders);

        testHeaders.clear();
        testHeaders.put(JmsHeaders.IDENTIFIER, identifier);
        testHeaders.put(FcrepoHeaders.FCREPO_BASE_URL, base_url);
        template.sendBodyAndHeaders("direct:describe.delete.insert", null, testHeaders);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<String, Object>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that the assertions passed
        resultEndpoint.assertIsSatisfied();
        sparqlQueryEndpoint.assertIsSatisfied();
        sparqlUpdateEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();
    }

    @Test
    public void testInsertDeleteNamedGraphSparql() throws Exception {
        // Assertions
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        sparqlQueryEndpoint.expectedMessageCount(6);
        sparqlQueryEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        sparqlUpdateEndpoint.expectedMessageCount(4);
        sparqlUpdateEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 200);

        deletedEndpoint.expectedMessageCount(2);
        deletedEndpoint.expectedBodiesReceived(null, null);
        deletedEndpoint.expectedHeaderReceived(Exchange.HTTP_RESPONSE_CODE, 204);

        // Setup
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup", FcrepoTestUtils.getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(FcrepoTestUtils.getFcrepoBaseUrl(), "");
        final String base_url = "http://localhost:" + System.getProperty("test.port", "8080") + "/rest";

        // Test
        final Map<String, Object> testHeaders = new HashMap<String, Object>();
        testHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        testHeaders.put(FcrepoHeaders.FCREPO_NAMED_GRAPH, "http://example.org/foo");
        testHeaders.put(FcrepoHeaders.FCREPO_BASE_URL, base_url);

        template.sendBodyAndHeaders("direct:insert.named", null, testHeaders);
        template.sendBodyAndHeaders("direct:delete.named", null, testHeaders);
        template.sendBodyAndHeaders("direct:count.named.triples", null, testHeaders);

        testHeaders.clear();
        testHeaders.put(JmsHeaders.IDENTIFIER, identifier);
        testHeaders.put(FcrepoHeaders.FCREPO_BASE_URL, base_url);
        template.sendBodyAndHeaders("direct:describe.delete.insert.named", null, testHeaders);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<String, Object>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that the assertions passed
        resultEndpoint.assertIsSatisfied();
        sparqlQueryEndpoint.assertIsSatisfied();
        sparqlUpdateEndpoint.assertIsSatisfied();
        deletedEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws IOException {

                final String fcrepo_uri = FcrepoTestUtils.getFcrepoEndpointUri();
                final String fuseki_url = "localhost:" + System.getProperty("test.fuseki.port", "3030");
                final Processor sparqlInsert = new SparqlInsertProcessor();
                final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);
                ns.add("dc", "http://purl.org/dc/elements/1.1/");
                ns.add("sparql", "http://www.w3.org/2005/sparql-results#");

                final XPathBuilder titleXpath = new XPathBuilder(
                        "//sparql:literal[text() = 'some title']");
                titleXpath.namespaces(ns);
                final XPathBuilder resourceXpath = new XPathBuilder(
                        "//sparql:uri[text() = '" + RdfNamespaces.REPOSITORY + "Resource']");
                resourceXpath.namespaces(ns);
                final XPathBuilder containerXpath = new XPathBuilder(
                        "//sparql:uri[text() = '" + RdfNamespaces.REPOSITORY + "Container']");
                containerXpath.namespaces(ns);

                from("direct:setup")
                    .to(fcrepo_uri)
                    .to("mock:created");

                from("direct:check.TypeResource")
                    .removeHeaders("CamelHttp*")
                    .setHeader(Exchange.HTTP_QUERY, simple(
                            "query=SELECT * WHERE { " +
                                "<${headers.CamelFcrepoBaseUrl}${headers.CamelFcrepoIdentifier}> " +
                                "<" + RdfNamespaces.RDF + "type> ?o }"))
                    .to("http4:" + fuseki_url + "/test/query")
                    .filter(resourceXpath)
                        .to("mock:sparql.query");

                from("direct:check.named.TypeResource")
                    .removeHeaders("CamelHttp*")
                    .setHeader(Exchange.HTTP_QUERY, simple(
                            "query=SELECT * WHERE { " +
                                "GRAPH <${headers.CamelFcrepoNamedGraph}> { " +
                                "<${headers.CamelFcrepoBaseUrl}${headers.CamelFcrepoIdentifier}> " +
                                "<" + RdfNamespaces.RDF + "type> ?o } }"))
                    .to("http4:" + fuseki_url + "/test/query")
                    .filter(resourceXpath)
                        .to("mock:sparql.query");

                 from("direct:check.named.TypeContainer")
                    .removeHeaders("CamelHttp*")
                    .setHeader(Exchange.HTTP_QUERY, simple(
                            "query=SELECT * WHERE { " +
                               "GRAPH <${headers.CamelFcrepoNamedGraph}> { " +
                                "<${headers.CamelFcrepoBaseUrl}${headers.CamelFcrepoIdentifier}> " +
                                "<" + RdfNamespaces.RDF + "type> ?o } }"))
                    .to("http4:" + fuseki_url + "/test/query")
                    .filter(containerXpath)
                        .to("mock:sparql.query");

                 from("direct:check.TypeContainer")
                    .removeHeaders("CamelHttp*")
                    .setHeader(Exchange.HTTP_QUERY, simple(
                            "query=SELECT * WHERE { " +
                                "<${headers.CamelFcrepoBaseUrl}${headers.CamelFcrepoIdentifier}> " +
                                "<" + RdfNamespaces.RDF + "type> ?o }"))
                    .to("http4:" + fuseki_url + "/test/query")
                    .filter(containerXpath)
                        .to("mock:sparql.query");

                from("direct:check.named.DcTitle")
                    .removeHeaders("CamelHttp*")
                    .setHeader(Exchange.HTTP_QUERY, simple(
                            "query=SELECT * WHERE { " +
                                "GRAPH <${headers.CamelFcrepoNamedGraph}> { " +
                                "<${headers.CamelFcrepoBaseUrl}${headers.CamelFcrepoIdentifier}> " +
                                "<http://purl.org/dc/elements/1.1/title> ?o } }"))
                    .to("http4:" + fuseki_url + "/test/query")
                    .filter(titleXpath)
                        .to("mock:sparql.query");

                from("direct:check.DcTitle")
                    .removeHeaders("CamelHttp*")
                    .setHeader(Exchange.HTTP_QUERY, simple(
                            "query=SELECT * WHERE { " +
                                "<${headers.CamelFcrepoBaseUrl}${headers.CamelFcrepoIdentifier}> " +
                                "<http://purl.org/dc/elements/1.1/title> ?o }"))
                    .to("http4:" + fuseki_url + "/test/query")
                    .filter(titleXpath)
                        .to("mock:sparql.query");

                from("direct:count.triples")
                    .setHeader(Exchange.HTTP_QUERY, simple(
                            "query=SELECT ?s WHERE { ?s ?p ?o }"))
                    .to("http4:" + fuseki_url + "/test/query")
                    .setHeader("ItemCount").xpath("count(//sparql:result)", String.class, ns)
                    .filter().simple("${header.ItemCount} <= 1")
                        .to("mock:sparql.query");

                from("direct:count.named.triples")
                    .setHeader(Exchange.HTTP_QUERY, simple(
                            "query=SELECT ?s WHERE { GRAPH <${headers.CamelFcrepoNamedGraph}> { ?s ?p ?o } }"))
                    .to("http4:" + fuseki_url + "/test/query")
                    .setHeader("ItemCount").xpath("count(//sparql:result)", String.class, ns)
                    .filter().simple("${header.ItemCount} <= 1")
                        .to("mock:sparql.query");

                from("direct:update")
                    .to(fcrepo_uri)
                    .process(new SparqlUpdateProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .to("mock:sparql.update")
                    .to("direct:check.DcTitle")
                    .to("direct:check.TypeResource")
                    .to("direct:check.TypeContainer");

                from("direct:insert")
                    .to(fcrepo_uri)
                    .process(new SparqlInsertProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .to("mock:sparql.update")
                    .to("direct:check.DcTitle")
                    .to("direct:check.TypeResource")
                    .to("direct:check.TypeContainer");

                from("direct:insert.named")
                    .to(fcrepo_uri)
                    .process(new SparqlInsertProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .to("mock:sparql.update")
                    .to("direct:check.named.DcTitle")
                    .to("direct:check.named.TypeResource")
                    .to("direct:check.named.TypeContainer");


                from("direct:delete.named")
                    .process(new SparqlDescribeProcessor())
                    .to("http4:" + fuseki_url + "/test/query")
                    .to("mock:sparql.query")
                    .process(new SparqlDeleteProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .to("mock:sparql.update")
                    .to("direct:check.named.DcTitle")
                    .to("direct:check.named.TypeResource")
                    .to("direct:check.named.TypeContainer");

                from("direct:delete")
                    .process(new SparqlDescribeProcessor())
                    .to("http4:" + fuseki_url + "/test/query")
                    .to("mock:sparql.query")
                    .process(new SparqlDeleteProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .to("mock:sparql.update")
                    .to("direct:check.DcTitle")
                    .to("direct:check.TypeResource")
                    .to("direct:check.TypeContainer");

                from("direct:describe.delete.insert.named")
                    .process(new SparqlDescribeProcessor())
                    .to("http4:" + fuseki_url + "/test/query")
                    .to("mock:sparql.query")
                    .process(new SparqlDeleteProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .to("mock:sparql.update")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(fcrepo_uri + "?accept=application/n-triples")
                    .process(new SparqlInsertProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .to("mock:sparql.update")
                    .to("mock:result");

                from("direct:describe.delete.insert")
                    .process(new SparqlDescribeProcessor())
                    .to("http4:" + fuseki_url + "/test/query")
                    .to("mock:sparql.query")
                    .process(new SparqlDeleteProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .to("mock:sparql.update")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(fcrepo_uri + "?accept=application/n-triples")
                    .process(new SparqlInsertProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .to("mock:sparql.update")
                    .to("mock:result");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to("mock:deleted")
                    .to(fcrepo_uri + "?tombstone=true")
                    .to("mock:deleted");
            }
        };
    }
}
