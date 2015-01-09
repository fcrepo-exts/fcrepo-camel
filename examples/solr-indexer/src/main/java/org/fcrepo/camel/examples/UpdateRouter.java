/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.camel.examples;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.fcrepo.camel.FcrepoHeaders;
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

        /*
         * Process items in the update queue, processing only those with the indexing:indexable predicate.
         */
        from("seda:update")
            .removeHeaders("CamelHttp*")
            .to("fcrepo:{{fcrepo.baseUrl}}?authUsername={{fcrepo.authUsername}}&authPassword={{fcrepo.authPassword}}")
            .convertBodyTo(org.w3c.dom.Document.class)
            .filter(indexable)
                .to("direct:log")
                .setHeader(FcrepoHeaders.FCREPO_TRANSFORM)
                    .xpath("/rdf:RDF/rdf:Description/indexing:hasIndexingTransform/text()", String.class, ns)
                .multicast().to("direct:update.triplestore", "direct:update.solr");
                
        /*
         * Log what is happening.
         */
        from("direct:log")
            .choice()
                .when(header(FcrepoHeaders.FCREPO_IDENTIFIER))
                    .log(String.format("Updating ${headers[%s]}", FcrepoHeaders.FCREPO_IDENTIFIER))
                .when(header(JmsHeaders.IDENTIFIER))
                    .log(String.format("Updating ${headers[%s]}", JmsHeaders.IDENTIFIER))
                .otherwise()
                    .log("Updating root node");
                    
        /*
         * Update the triplestore.
         */
        from("direct:update.triplestore")
            .process(new SparqlUpdateProcessor())
            .to("http4:{{triplestore.baseUrl}}/update?throwExceptionOnFailure=false");

        /*
         * Update the solr index.
         */
        from("direct:update.solr")
            .removeHeaders("CamelHttp*")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"));
            //.to("fcrepo:{{fcrepo.baseUrl}}?transform={{fcrepo.defaultTransform}}&authUsername={{fcrepo.authUsername}}&authPassword={{fcrepo.authPassword}}")
            // solr http endpoint goes here
            //.log("SOLR: ${body}");
    }
}
