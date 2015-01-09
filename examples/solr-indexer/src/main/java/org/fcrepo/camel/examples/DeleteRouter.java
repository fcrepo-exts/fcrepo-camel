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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;

/**
 * A Camel Router for handling delete operations.
 *
 * @author Aaron Coburn
 */
public class DeleteRouter extends RouteBuilder {

    public void configure() throws Exception {

        /*
         * Handle items in the delete queue.
         */
        from("seda:delete")
            .log(String.format("Remove object: ${headers[%s]}", JmsHeaders.IDENTIFIER))
            .multicast()
                .to("direct:delete.triplestore", "direct:delete.solr");

        /*
         * Remove objects from the triplestore.
         */
        from("direct:delete.triplestore")
            .process(new SparqlDeleteProcessor())
            .to("http4:{{triplestore.baseUrl}}/update?throwExceptionOnFailure=false");

        /*
         * Remove objects from the solr index.
         */
        from("direct:delete.solr")
            .log(String.format("Deleting %s from Solr", JmsHeaders.IDENTIFIER))
            .setHeader(Exchange.CONTENT_TYPE)
                .constant("application/json")
            .setHeader(Exchange.HTTP_METHOD)
                .constant("POST")
            .transform()
                .simple(String.format("{\"delete\":{\"id\":\"${headers[%s]}\", \"commitWithin\": \"500\"}}", JmsHeaders.IDENTIFIER))
            .to("http4://{{solr.uri}}/update");
    }
}
