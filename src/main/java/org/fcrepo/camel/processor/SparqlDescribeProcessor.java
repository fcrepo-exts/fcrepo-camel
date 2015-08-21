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
package org.fcrepo.camel.processor;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

/**
 * Represents a Processor class that formulates a Sparql DESCRIBE query
 * that is ready to be POSTed to a Sparql endpoint.
 *
 * The processor expects the following headers:
 *      org.fcrepo.jms.identifier
 *      org.fcrepo.jms.baseURL
 * each of which can be overridden with the following:
 *      FCREPO_IDENTIFIER
 *      FCREPO_BASE_URL
 *
 * @author Aaron Coburn
 * @since November 6, 2014
 */
public class SparqlDescribeProcessor implements Processor {
    /**
     *  Define how this message should be processed
     *
     *  @param exchange the current camel message exchange
     */
    public void process(final Exchange exchange) throws IOException {

        final Message in = exchange.getIn();
        final String subject = ProcessorUtils.getSubjectUri(in);

        exchange.getIn().setBody("query=DESCRIBE <" + subject + ">");
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getIn().setHeader(Exchange.ACCEPT_CONTENT_TYPE, "application/rdf+xml");
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
    }
}
