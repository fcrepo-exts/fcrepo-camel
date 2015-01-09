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

import java.net.URL;

import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.fcrepo.camel.FcrepoHeaders;

/**
 * A processor that converts the value of the message body
 * into the identifying path for an fcrepo node.
 *
 * This assumes that the `baseUrl` value is in the
 * FCREPO_BASE_URL header.
 *
 * @author Aaron Coburn
 */
public class NodePathProcessor implements Processor {
    
    public void process(final Exchange exchange) throws Exception {

        final Message in = exchange.getIn();
        final URL base = new URL(in.getHeader(FcrepoHeaders.FCREPO_BASE_URL, String.class));
        final URL full = new URL(in.getBody(String.class));

        in.setHeader(FcrepoHeaders.FCREPO_IDENTIFIER,
            full.getPath().substring(base.getPath().length()));
        in.setHeader(FcrepoHeaders.FCREPO_BASE_URL,
            in.getHeader(AicHeaders.BASE_URL, String.class));

        in.removeHeaders("CamelHttp*");
        in.removeHeaders("CamelRestlet*");
        in.removeHeaders("org.restlet*");
    }

}
