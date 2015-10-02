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

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.camel.processor.ProcessorUtils.langFromMimeType;
import static java.net.URLEncoder.encode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.fcrepo.camel.FcrepoHeaders;

/**
 * Represents a processor for creating the sparql-update message to
 * be passed to an external triplestore.
 *
 * @author Aaron Coburn
 * @since Nov 8, 2014
 */
public class SparqlUpdateProcessor implements Processor {
    /**
     * Define how the message is processed.
     *
     * @param exchange the current camel message exchange
     */
    public void process(final Exchange exchange) throws IOException {

        final Message in = exchange.getIn();

        final ByteArrayOutputStream serializedGraph = new ByteArrayOutputStream();
        final String subject = ProcessorUtils.getSubjectUri(in);
        final String namedGraph = in.getHeader(FcrepoHeaders.FCREPO_NAMED_GRAPH, String.class);
        final Model model = createDefaultModel().read(in.getBody(InputStream.class), subject,
                langFromMimeType(in.getHeader(Exchange.CONTENT_TYPE, String.class)));

        model.write(serializedGraph, "N-TRIPLE");

        /*
         * Before inserting updated triples, the Sparql update command
         * below deletes all triples with the defined subject uri
         * (coming from the FCREPO_IDENTIFIER and FCREPO_BASE_URL headers).
         * It also deletes triples that have a subject corresponding to
         * that Fcrepo URI plus the "/fcr:export?format=jcr/xml" string
         * appended to it. This makes it possible to more completely
         * remove any triples for a given resource that were added
         * earlier. If fcrepo ever stops producing triples that are
         * appended with /fcr:export?format..., then that extra line
         * can be removed. It would also be possible to recursively delete
         * triples (by removing any triple whose subject is also an object
         * of the starting (or context) URI, but that approach tends to
         * delete too many triples from the triplestore. This command does
         * not delete blank nodes.
         */
        final StringBuilder query = new StringBuilder();
        query.append(ProcessorUtils.deleteWhere(subject, namedGraph));
        query.append(";\n");
        query.append(ProcessorUtils.deleteWhere(subject + "/fcr:export?format=jcr/xml", namedGraph));
        query.append(";\n");
        query.append(ProcessorUtils.insertData(serializedGraph.toString("UTF-8"), namedGraph));

        in.setBody("update=" + encode(query.toString(), "UTF-8"));
        in.setHeader(Exchange.HTTP_METHOD, "POST");
        in.setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
    }
}
