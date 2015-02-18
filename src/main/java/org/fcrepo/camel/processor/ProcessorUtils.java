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

import org.apache.camel.Message;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.lang3.StringUtils;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.FcrepoHeaders;

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
        String trimmed = path;
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * Extract the subject URI from the incoming message headers.
     * @param in the incoming Message
     * @return the subject URI
     * @throws IOException when no baseURL header is present
     */
    public static String getSubjectUri(final Message in) throws IOException {
        final StringBuilder base = new StringBuilder("");

        if (in.getHeader(FcrepoHeaders.FCREPO_BASE_URL) != null) {
            base.append(trimTrailingSlash(in.getHeader(FcrepoHeaders.FCREPO_BASE_URL, String.class)));
        } else if (in.getHeader(JmsHeaders.BASE_URL) != null) {
            base.append(trimTrailingSlash(in.getHeader(JmsHeaders.BASE_URL, String.class)));
        } else {
            throw new IOException("No baseURL header available!");
        }

        if (in.getHeader(FcrepoHeaders.FCREPO_IDENTIFIER) != null) {
           base.append(in.getHeader(FcrepoHeaders.FCREPO_IDENTIFIER, String.class));
        } else if (in.getHeader(JmsHeaders.IDENTIFIER) != null) {
           base.append(in.getHeader(JmsHeaders.IDENTIFIER, String.class));
        }
        return base.toString();
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

        if (!StringUtils.isBlank(namedGraph)) {
            stmt.append("GRAPH ");
            stmt.append(new UriRef(namedGraph));
            stmt.append(" { ");
        }

        stmt.append(new UriRef(subject));
        stmt.append(" ?p ?o ");

        if (!StringUtils.isBlank(namedGraph)) {
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

        if (!StringUtils.isBlank(namedGraph)) {
            query.append("GRAPH ");
            query.append(new UriRef(namedGraph));
            query.append(" { ");
        }

        query.append(serializedGraph);

        if (!StringUtils.isBlank(namedGraph)) {
            query.append("} ");
        }

        query.append("}");
        return query.toString();
    }
}

