/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static com.hp.hpl.jena.util.URIref.encode;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;

import java.util.Optional;

import org.apache.camel.Message;

/**
 * Utility functions for fcrepo processor classes
 * @author Aaron Coburn
 * @since November 14, 2014
 */

public final class ProcessorUtils {

    /**
     * This is a utility class; the constructor is off-limits.
     */
    private ProcessorUtils() {
    }

    private static String trimTrailingSlash(final String path) {
        if (path.endsWith("/")) {
            return trimTrailingSlash(path.substring(0, path.length() - 1));
        }
        return path;
    }

    /**
     * Extract the subject URI from the incoming message headers.
     * @param in the incoming Message
     * @return the subject URI
     */
    public static Optional<String> getSubjectUri(final Message in) {
        final String base = in.getHeader(FCREPO_BASE_URL, String.class);
        final String path = in.getHeader(FCREPO_IDENTIFIER, "", String.class);
        if (base == null) {
            return empty();
        }
        return of(trimTrailingSlash(base) + path);
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
}

