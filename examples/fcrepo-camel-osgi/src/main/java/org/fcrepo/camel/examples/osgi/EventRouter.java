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
package org.fcrepo.camel.examples.osgi;

import org.apache.camel.builder.RouteBuilder;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.RdfNamespaces;

/**
 * A content router for handling JMS events.
 *
 * @author Aaron Coburn
 */
public class EventRouter extends RouteBuilder {

    public void configure() throws Exception {

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .handled(true)
            .transform()
                .simple("Event Routing Error: ${routeId}")
            .to("direct:error.log");

        /**
         * route a message to the proper queue, based on whether
         * it is a DELETE or UPDATE operation.
         */
        from("activemq:{{jms.fcrepoEndpoint}}")
            .routeId("FcrepoEventRouter")
            .to("direct:event.log")
            .choice()
                .when(header(JmsHeaders.EVENT_TYPE).isEqualTo(RdfNamespaces.REPOSITORY + "NODE_REMOVED"))
                    .to("seda:delete?blockWhenFull=true")
                .otherwise()
                    .to("seda:update?blockWhenFull=true");
    }
}
