/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel.examples.osgi;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.camel.HttpMethods;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.RdfNamespaces;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;

/**
 * A Camel Router for handling update operations.
 *
 * @author Aaron Coburn
 */
public class UpdateRouter extends RouteBuilder {

    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);
        ns.add("indexing", RdfNamespaces.INDEXING);

        final XPathBuilder indexable = new XPathBuilder(
                String.format("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='%s']", RdfNamespaces.INDEXING + "indexable"));
        indexable.namespaces(ns);

        /**
         * a general error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .handled(true)
            .transform()
                .simple("Update Error: ${routeId}")
            .to("direct:error.log");

        /**
         * Process items in the update queue, processing only those with the indexing:indexable predicate.
         */
        from("seda:update")
            .routeId("FcrepoUpdate")
            .removeHeaders("CamelHttp*")
            .to("fcrepo:{{fcrepo.baseUrl}}")
            .convertBodyTo(org.w3c.dom.Document.class)
            .filter(indexable)
                .to("direct:log")
                .setHeader(FcrepoHeaders.FCREPO_TRANSFORM)
                    .xpath("/rdf:RDF/rdf:Description/indexing:hasIndexingTransform/text()", String.class, ns)
                .multicast().to("direct:update.log", "direct:update.triplestore", "direct:update.solr");

        /**
         * Update the triplestore.
         */
        from("direct:update.triplestore")
            .routeId("FcrepoUpdateTriplestore")
            .process(new SparqlUpdateProcessor())
            .to("http4:{{triplestore.baseUrl}}/update?throwExceptionOnFailure=false");

        /**
         * Update the solr index.
         */
        from("direct:update.solr")
            .routeId("FcrepoUpdateSolr")
            .removeHeaders("CamelHttp*")
            .setHeader(Exchange.HTTP_METHOD)
                .constant(HttpMethods.GET)
            .to("fcrepo:{{fcrepo.baseUrl}}?transform={{fcrepo.defaultTransform}}")
            .setHeader(Exchange.HTTP_METHOD)
                .constant(HttpMethods.POST)
            .setHeader(Exchange.CONTENT_TYPE)
                .constant("application/json")
            .to("http4://{{solr.baseUrl}}/update");
    }
}
