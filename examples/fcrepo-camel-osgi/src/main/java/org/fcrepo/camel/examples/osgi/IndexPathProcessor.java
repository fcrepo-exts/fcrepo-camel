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

import java.net.URL;

import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.fcrepo.camel.FcrepoHeaders;

/**
 * A processor that converts the REST uri into the
 * identifying path for an fcrepo node.
 *
 * This assumes that the `restPrefix` value is stored
 * in the org.fcrepo.camel.examples.osgi.restPrefix header.
 *
 * @author Aaron Coburn
 */
public class IndexPathProcessor implements Processor {

    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        final URL url = new URL(in.getHeader(Exchange.HTTP_URI, String.class));
        final String prefix = in.getHeader(MessageHeaders.REST_PREFIX, String.class);

        in.setHeader(FcrepoHeaders.FCREPO_IDENTIFIER,
                url.getPath().substring(prefix.length()));
        in.setHeader(FcrepoHeaders.FCREPO_BASE_URL,
                in.getHeader(MessageHeaders.BASE_URL, String.class));

        in.removeHeaders("CamelHttp*");
        in.removeHeaders("CamelRestlet*");
        in.removeHeaders("org.restlet*");
    }
}
