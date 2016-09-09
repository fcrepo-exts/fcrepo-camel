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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.fcrepo.camel.HttpMethods;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;

/**
 * A Camel Router for handling delete operations.
 *
 * @author Aaron Coburn
 */
public class DeleteRouter extends RouteBuilder {

    public void configure() throws Exception {

        final String solrFormat = "{\"delete\":{\"id\":\"${headers[%s]}\", \"commitWithin\": \"500\"}}";

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .handled(true)
            .transform()
                .simple("Delete Error: ${routeId}")
            .to("direct:error.log");

        /*
         * Handle items in the delete queue.
         */
        from("seda:delete")
            .routeId("FcrepoDelete")
            .multicast().to("direct:delete.log", "direct:delete.triplestore", "direct:delete.solr");

        /*
         * Remove objects from the triplestore.
         */
        from("direct:delete.triplestore")
            .routeId("FcrepoDeleteTriplestore")
            .process(new SparqlDeleteProcessor())
            .to("http4:{{triplestore.baseUrl}}/update?throwExceptionOnFailure=false");

        /*
         * Remove objects from the solr index.
         */
        from("direct:delete.solr")
            .routeId("FcrepoDeleteSolr")
            .setHeader(Exchange.CONTENT_TYPE)
                .constant("application/json")
            .setHeader(Exchange.HTTP_METHOD)
                .constant(HttpMethods.POST)
            .transform()
                .simple(String.format(solrFormat, JmsHeaders.IDENTIFIER))
            .to("http4://{{solr.baseUrl}}/update");
    }
}
