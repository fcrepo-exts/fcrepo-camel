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
package org.fcrepo.camel.integration;

/**
 * Utility functions for integration testing
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public final class FedoraTestUtils {

    private FedoraTestUtils() {
    }

    public static String getFcrepoBaseUri() {
        return "http://localhost:8080/rest";
    }

    public static String getFcrepoEndpointUri() {
        return "fcrepo:localhost:8080/rest";
    }

    public static String getTurtleDocument() {
        return "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n" +
                "<> dc:title \"some title\" .";
    }

    public static String getTextDocument() {
        return "Simple plain text document";
    }

    public static String getPatchDocument() {
        return "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n\n" +
                "INSERT { <> dc:title \"another title\" . } \nWHERE { }";
    }
}