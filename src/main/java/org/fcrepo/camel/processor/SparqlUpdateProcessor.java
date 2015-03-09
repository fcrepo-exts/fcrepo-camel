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

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;

import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.fcrepo.camel.FcrepoHeaders;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

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
        final ParsingProvider parser = new JenaParserProvider();
        final SerializingProvider serializer = new JenaSerializerProvider();
        final MGraph graph = new SimpleMGraph();
        final String contentType = in.getHeader(Exchange.CONTENT_TYPE, String.class);
        final String namedGraph = in.getHeader(FcrepoHeaders.FCREPO_NAMED_GRAPH, String.class);
        final ByteArrayOutputStream serializedGraph = new ByteArrayOutputStream();
        final String subject = ProcessorUtils.getSubjectUri(in);

        /* the text/rdf+nt mimetype will be removed in the next version of clerezza
         * at which point they will be using application/n-triples. Once that happens,
         * this ternary expression can be removed. */
        parser.parse(graph, in.getBody(InputStream.class),
                "application/n-triples".equals(contentType) ? "text/rdf+nt" : contentType, new UriRef(subject));
        serializer.serialize(serializedGraph, graph.getGraph(), "text/rdf+nt");

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
        final StringBuilder query = new StringBuilder("update=");
        query.append(ProcessorUtils.deleteWhere(subject, namedGraph));
        query.append(";\n");
        query.append(ProcessorUtils.deleteWhere(subject + "/fcr:export?format=jcr/xml", namedGraph));
        query.append(";\n");
        query.append(ProcessorUtils.insertData(serializedGraph.toString("UTF-8"), namedGraph));

        in.setBody(query.toString());
        in.setHeader(HTTP_METHOD, "POST");
        in.setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded");
    }
}
