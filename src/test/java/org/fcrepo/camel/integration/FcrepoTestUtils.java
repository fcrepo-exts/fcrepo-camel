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
package org.fcrepo.camel.integration;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;

/**
 * Utility functions for integration testing
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public final class FcrepoTestUtils {

    private static final int FCREPO_PORT = parseInt(getProperty(
                "fcrepo.dynamic.test.port", "8080"));

    /**
     * This is a utility class; the constructor is off-limits
     */
    private FcrepoTestUtils() {
    }

    /**
     * Retrieve the baseUrl for the fcrepo instance
     *
     * @return string containing base url
     */
    public static String getFcrepoBaseUrl() {
        if (FCREPO_PORT == 80) {
            return "http://localhost/fcrepo/rest";
        }
        return "http://localhost:" + FCREPO_PORT + "/fcrepo/rest";
    }

    /**
     * Retrieve the endpoint uri for fcrepo
     *
     * @return string containing endpoint uri
     */
    public static String getFcrepoEndpointUri() {
        if (FCREPO_PORT == 80) {
            return "fcrepo://localhost/fcrepo/rest";
        }
        return "fcrepo://localhost:" + FCREPO_PORT + "/fcrepo/rest";
    }

    /**
     * Retrieve the endpoint uri with an explicit scheme
     *
     * @return string containing endpoint uri
     */
    public static String getFcrepoEndpointUriWithScheme() {
        if (FCREPO_PORT == 80) {
            return "fcrepo:http://localhost/fcrepo/rest";
        }
        return "fcrepo:http://localhost:" + FCREPO_PORT + "/fcrepo/rest";
    }

    /**
     * Retrieve an RDF document serialized in TTL
     *
     * @return string containing RDF doc in TTL
     */
    public static String getTurtleDocument() {
        return "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n" +
                "<> dc:title \"some title & other\" .";
    }

    /**
     * Retrieve an N3 document
     *
     * @return string containing NS document
     */
    public static String getN3Document() {
        return "<http://localhost/fcrepo/rest/path/a/b/c> <http://purl.org/dc/elements/1.1/author> \"Author\" .\n" +
                "<http://localhost/fcrepo/rest/path/a/b/c> <http://purl.org/dc/elements/1.1/title> \"This & That\" .";
    }

    /**
     * Retrieve a simple text document
     *
     * @return string containing text document
     */
    public static String getTextDocument() {
        return "Simple plain text document";
    }

    /**
     * Retrieve a sparql-update document
     *
     * @return string containing sparql document
     */
    public static String getPatchDocument() {
        return "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n\n" +
                "INSERT { <> dc:title \"another title\" . } \nWHERE { }";
    }
}
