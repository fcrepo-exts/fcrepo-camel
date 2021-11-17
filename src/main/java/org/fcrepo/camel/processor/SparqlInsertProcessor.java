/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.processor;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.http.entity.ContentType.parse;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.RDFDataMgr.read;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_NAMED_GRAPH;
import static org.fcrepo.camel.processor.ProcessorUtils.insertData;
import static java.net.URLEncoder.encode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

/**
 * Represents a processor for creating the sparql-update message to
 * be passed to an external triplestore.
 *
 * @author Aaron Coburn
 * @since Nov 8, 2014
 */
public class SparqlInsertProcessor implements Processor {
    /**
     * Define how the message is processed.
     *
     * @param exchange the current camel message exchange
     */
    public void process(final Exchange exchange) throws IOException {

        final Message in = exchange.getIn();
        final ByteArrayOutputStream serializedGraph = new ByteArrayOutputStream();
        final String namedGraph = in.getHeader(FCREPO_NAMED_GRAPH, "", String.class);
        final Model model = createDefaultModel();

        read(model, in.getBody(InputStream.class),
                contentTypeToLang(parse(in.getHeader(CONTENT_TYPE, String.class)).getMimeType()));

        model.write(serializedGraph, "N-TRIPLE");

        exchange.getIn().setBody("update=" +
                encode(insertData(serializedGraph.toString("UTF-8"), namedGraph), "UTF-8"));
        exchange.getIn().setHeader(HTTP_METHOD, "POST");
        exchange.getIn().setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
    }
}
