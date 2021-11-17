/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.processor;

import static java.net.URLEncoder.encode;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_NAMED_GRAPH;
import static org.fcrepo.camel.processor.ProcessorUtils.deleteWhere;
import static org.fcrepo.camel.processor.ProcessorUtils.getSubjectUri;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.NoSuchHeaderException;
import org.apache.camel.Processor;

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
     *
     * @param exchange the current camel message exchange
     */
    public void process(final Exchange exchange) throws IOException, NoSuchHeaderException {

        final Message in = exchange.getIn();
        final String namedGraph = in.getHeader(FCREPO_NAMED_GRAPH, "", String.class);
        final String subject = getSubjectUri(exchange);

        in.setBody("update=" + encode(deleteWhere(subject, namedGraph), "UTF-8"));
        in.setHeader(HTTP_METHOD, "POST");
        in.setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
   }

}
