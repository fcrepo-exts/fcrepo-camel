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
package org.fcrepo.camel.examples.osgi;

import javax.xml.transform.stream.StreamSource;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.Exchange;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.camel.RdfNamespaces;

/**
 * A router for traversing a node hierarchy, initiating'
 * re-indexing operations.
 *
 * @author Aaron Coburn
 */
public class IndexRouter extends RouteBuilder {

    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);
        ns.add("ldp", RdfNamespaces.LDP);

        final XPathBuilder children = new XPathBuilder("/rdf:RDF/rdf:Description/ldp:contains");
        children.namespaces(ns);

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .handled(true)
            .transform()
                .simple("Indexing Error: ${routeId}")
            .to("direct:error.log");

        /**
         *  Index objects, starting at the identified node
         */
        from("direct:index")
            .routeId("FcrepoIndexEndpoint")
            .log(String.format("Initial indexing path: ${headers[%s]}", Exchange.HTTP_URI))
            .setHeader(MessageHeaders.REST_PREFIX)
                .constant("{{rest.prefix}}")
            .setHeader(MessageHeaders.BASE_URL)
                .constant("http://{{fcrepo.baseUrl}}")
            .process(new IndexPathProcessor())
            .inOnly("seda:enqueue?waitForTaskToComplete=Never&blockWhenFull=true")
            .setHeader(Exchange.CONTENT_TYPE)
                .constant("text/plain")
            .transform()
                .simple(String.format("Indexing started at ${headers[%s]}", FcrepoHeaders.FCREPO_IDENTIFIER));

        /**
         *  Buffer index requests in an asynchronous queue before sending to ActiveMQ
         */
        from("seda:enqueue")
            .routeId("FcrepoEnqueue")
            .removeHeaders("CamelHttp*")
            .removeHeader("JMSCorrelationID")
            .inOnly("activemq:queue:index?disableTimeToLive=true");

        /**
         *  Process indexing requests from the index queue
         */
        from("activemq:queue:index")
            .routeId("FcrepoIndexer")
            .inOnly("seda:update?waitForTaskToComplete=Never&blockWhenFull=true")
            .removeHeaders("CamelHttp*")
            .setHeader(Exchange.HTTP_METHOD)
                .constant("GET")
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=PreferContainment&preferOmit=ServerManaged")
            .split(children)
                .transform()
                    .xpath("/ldp:contains/@rdf:resource", String.class, ns)
                .process(new NodePathProcessor())
                .inOnly("seda:enqueue?waitForTaskToComplete=Never&blockWhenFull=true");
    }
}
