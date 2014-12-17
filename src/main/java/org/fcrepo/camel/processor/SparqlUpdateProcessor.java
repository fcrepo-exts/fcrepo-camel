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

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;

import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;

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
     */
    public void process(final Exchange exchange) throws IOException {

        final Message in = exchange.getIn();
        final ParsingProvider parser = new JenaParserProvider();
        final SerializingProvider serializer = new JenaSerializerProvider();
        final MGraph graph = new SimpleMGraph();
        final String contentType = in.getHeader(Exchange.CONTENT_TYPE, String.class);
        final ByteArrayOutputStream serializedGraph = new ByteArrayOutputStream();
        final String subject = ProcessorUtils.getSubjectUri(in);

        parser.parse(graph, in.getBody(InputStream.class),
                "application/n-triples".equals(contentType) ? "text/rdf+nt" : contentType, null);
        serializer.serialize(serializedGraph, graph.getGraph(), "text/rdf+nt");

        exchange.getIn().setBody("DELETE WHERE { <" + subject + "> ?p ?o };\n" +
                                 "INSERT { " + serializedGraph.toString("UTF-8") + " }\n" +
                                 "WHERE { }");
        exchange.getIn().setHeader(HTTP_METHOD, "POST");
        exchange.getIn().setHeader(CONTENT_TYPE, "application/sparql-update");
    }
}
