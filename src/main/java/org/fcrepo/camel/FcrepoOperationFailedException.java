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
package org.fcrepo.camel;

import java.net.URI;

import org.apache.camel.CamelException;

/**
 * Represents a failure of the underlying HTTP client's interaction with fedora.
 * @author Aaron Coburn
 * @since January 8, 2015
 */
public class FcrepoOperationFailedException extends CamelException {

    private final URI url;
    private final int statusCode;
    private final String statusText;

    /**
     * Create an FcrepoOperationFailedException
     * @param url the requested url
     * @param statusCode the HTTP response code
     * @param statusText the response message
     */
    public FcrepoOperationFailedException(final URI url, final int statusCode, final String statusText) {
        super("HTTP operation failed invoking " + (url != null ? url.toString() : "[null]") +
                " with statusCode: " + statusCode + " and message: " + statusText);
        this.url = url;
        this.statusCode = statusCode;
        this.statusText = statusText;
    }

    /**
     * Return the requested url.
     */
    public URI getUrl() {
        return url;
    }

    /**
     * Get the status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get the status text.
     */
    public String getStatusText() {
        return statusText;
    }
}
