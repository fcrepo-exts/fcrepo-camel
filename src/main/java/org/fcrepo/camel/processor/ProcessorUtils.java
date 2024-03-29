/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.processor;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.camel.support.ExchangeHelper.getMandatoryHeader;
import static org.apache.jena.util.URIref.encode;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.NoSuchHeaderException;

import org.slf4j.Logger;

/**
 * Utility functions for fcrepo processor classes
 * @author Aaron Coburn
 * @since November 14, 2014
 */

public final class ProcessorUtils {

    private static final Logger LOGGER  = getLogger(ProcessorUtils.class);

    /**
     * This is a utility class; the constructor is off-limits.
     */
    private ProcessorUtils() {
    }

    private static String trimTrailingSlash(final String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Extract the subject URI from the incoming exchange.
     * @param exchange the incoming Exchange
     * @return the subject URI
     * @throws NoSuchHeaderException when the CamelFcrepoBaseUrl header is not present
     */
    public static String getSubjectUri(final Exchange exchange) throws NoSuchHeaderException {
        final String uri = exchange.getIn().getHeader(FCREPO_URI, "", String.class);
        if (uri.isEmpty()) {
            final String base = getMandatoryHeader(exchange, FCREPO_BASE_URL, String.class);
            final String path = exchange.getIn().getHeader(FCREPO_IDENTIFIER, "", String.class);
            return trimTrailingSlash(base) + path;
        }
        return uri;
    }

    /**
     * Create a DELETE WHERE { ... } statement from the provided subject
     *
     * @param subject the subject of the triples to delete.
     * @param namedGraph an optional named graph
     * @return the delete statement
     */
    public static String deleteWhere(final String subject, final String namedGraph) {
        final StringBuilder stmt = new StringBuilder("DELETE WHERE { ");

        if (!namedGraph.isEmpty()) {
            stmt.append("GRAPH ");
            stmt.append("<" + encode(namedGraph) + ">");
            stmt.append(" { ");
        }

        stmt.append("<" + encode(subject) + ">");
        stmt.append(" ?p ?o ");

        if (!namedGraph.isEmpty()) {
            stmt.append("} ");
        }

        stmt.append("}");
        return stmt.toString();
    }

    /**
     *  Create an INSERT DATA { ... } update query with the provided ntriples
     *
     *  @param serializedGraph the triples to insert
     *  @param namedGraph an optional named graph
     *  @return the insert statement
     */
    public static String insertData(final String serializedGraph, final String namedGraph) {
        final StringBuilder query = new StringBuilder("INSERT DATA { ");

        if (!namedGraph.isEmpty()) {
            query.append("GRAPH <");
            query.append(encode(namedGraph));
            query.append("> { ");
        }

        query.append(serializedGraph);

        if (!namedGraph.isEmpty()) {
            query.append("} ");
        }

        query.append("}");
        return query.toString();
    }

    /**
     * Tokenize a property placeholder value
     *
     * @param context the camel context
     * @param property the name of the property placeholder
     * @param token the token used for splitting the value
     * @return a list of values
     */
    public static List<String> tokenizePropertyPlaceholder(final CamelContext context, final String property,
            final String token) {
        try {
            return stream(context.resolvePropertyPlaceholders(property).split(token)).map(String::trim)
                .filter(val -> !val.isEmpty()).collect(toList());
        } catch (final Exception ex) {
            LOGGER.debug("No property value found for {}", property);
            return emptyList();
        }
    }
}

