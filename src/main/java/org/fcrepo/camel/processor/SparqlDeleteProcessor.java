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
package org.fcrepo.camel.processor;

import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.CONTENT_TYPE;

import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;

import java.io.IOException;

/**
 * Represends a message processor that deletes objects from an
 * external triplestore.
 *
 * @author Aaron Coburn
 * @since Nov 8, 2014
 */
public class SparqlDeleteProcessor implements Processor {
    /**
     * Define how the message should be processed.
     */
    public void process(final Exchange exchange) throws IOException {

        final Message in = exchange.getIn();
        final String subject = ProcessorUtils.getSubjectUri(in);

        in.setBody("DELETE WHERE { <" + subject + "> ?p ?o };\n" +
                   "DELETE WHERE { <" + subject + "/fcr:export?format=jcr/xml> ?p ?o }");
        in.setHeader(HTTP_METHOD, "POST");
        in.setHeader(CONTENT_TYPE, "application/sparql-update");

   }
}
