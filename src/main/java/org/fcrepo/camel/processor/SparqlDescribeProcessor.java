/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.processor;

import static java.net.URLEncoder.encode;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.processor.ProcessorUtils.getSubjectUri;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.NoSuchHeaderException;
import org.apache.camel.Processor;

/**
 * Represents a Processor class that formulates a Sparql DESCRIBE query
 * that is ready to be POSTed to a Sparql endpoint.
 *
 * The processor expects the following headers:
 *      FCREPO_URI
 *      * or *
 *      FCREPO_BASE_URL
 *      FCREPO_IDENTIFIER
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
    public void process(final Exchange exchange) throws IOException, NoSuchHeaderException {
        final String subject = getSubjectUri(exchange);

        exchange.getIn().setBody("query=" + encode("DESCRIBE <" + subject + ">", "UTF-8"));
        exchange.getIn().setHeader(HTTP_METHOD, "POST");
        exchange.getIn().setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
    }
}
