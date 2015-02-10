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
package org.fcrepo.camel;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import org.apache.camel.RuntimeCamelException;

/**
 * An FcrepoConfiguration class.
 * @author Aaron Coburn
 * @since Jan 20, 2015
 */
@UriParams
public class FcrepoConfiguration implements Cloneable {

    private String baseUrl = "";

    @UriParam
    private String contentType = null;

    @UriParam
    private String accept = null;

    @UriParam
    private String transform = null;

    @UriParam
    private String authUsername = null;

    @UriParam
    private String authPassword = null;

    @UriParam
    private String authHost = null;

    @UriParam
    private Boolean tombstone = false;

    @UriParam
    private Boolean metadata = true;

    @UriParam
    private Boolean throwExceptionOnFailure = true;

    @UriParam
    private String preferInclude = null;

    @UriParam
    private String preferOmit = null;

    @UriParam
    private String transaction = null;

    /**
     * Create a new FcrepoConfiguration object
     */
    public FcrepoConfiguration() {
        super();
    }

    /**
     * Copy an FcrepoConfiguration object.
     */
    @Override
    public FcrepoConfiguration clone() {
        try {
            return (FcrepoConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeCamelException(e);
        }
    }

   /**
     * baseUrl setter
     * @param url the baseUrl string
     */
    public void setBaseUrl(final String url) {
        this.baseUrl = url;
    }

    /**
     * baseUrl getter
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * accept setter
     * @param type the content-type for Accept headers
     */
    public void setAccept(final String type) {
        this.accept = type.replaceAll(" ", "+");
    }

    /**
     * accept getter
     */
    public String getAccept() {
        return accept;
    }

    /**
     * contentType setter
     * @param type the content-type for Content-Type headers
     */
    public void setContentType(final String type) {
        this.contentType = type.replaceAll(" ", "+");
    }

    /**
     * contentType getter
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * authUsername setter
     * @param username used for authentication
     */
    public void setAuthUsername(final String username) {
        this.authUsername = username;
    }

    /**
     * authUsername getter
     */
    public String getAuthUsername() {
        return authUsername;
    }

    /**
     * authPassword setter
     * @param password used for authentication
     */
    public void setAuthPassword(final String password) {
        this.authPassword = password;
    }

    /**
     * authPassword getter
     */
    public String getAuthPassword() {
        return authPassword;
    }

    /**
     * authHost setter
     * @param host used for authentication
     */
    public void setAuthHost(final String host) {
        this.authHost = host;
    }

    /**
     * authHost getter
     */
    public String getAuthHost() {
        return authHost;
    }

    /**
     * metadata setter
     * @param metadata whether to retrieve rdf metadata for non-rdf nodes
     */
    public void setMetadata(final Boolean metadata) {
        this.metadata = metadata;
    }

    /**
     * metadata getter
     */
    public Boolean getMetadata() {
        return metadata;
    }

    /**
     * throwExceptionOnFailure setter
     * @param throwOnFailure whether non-2xx HTTP response codes throw exceptions
     */
    public void setThrowExceptionOnFailure(final Boolean throwOnFailure) {
        this.throwExceptionOnFailure = throwOnFailure;
    }

    /**
     * throwExceptionOnFailure getter
     */
    public Boolean getThrowExceptionOnFailure() {
        return throwExceptionOnFailure;
    }

    /**
     * transform setter
     * @param transform define an LD-Path transform program for converting RDF to JSON
     */
    public void setTransform(final String transform) {
        this.transform = transform;
    }

    /**
     * transform getter
     */
    public String getTransform() {
        return transform;
    }

    /**
     * tombstone setter
     * @param tombstone whether to access the /fcr:tombstone endpoint for a resource
     */
    public void setTombstone(final Boolean tombstone) {
        this.tombstone = tombstone;
    }

    /**
     * tombstone getter
     */
    public Boolean getTombstone() {
        return tombstone;
    }

    /**
     * preferInclude setter
     */
    public void setPreferInclude(final String include) {
        this.preferInclude = include;
    }

    /**
     * preferOmit getter
     */
    public String getPreferInclude() {
        return preferInclude;
    }

    /**
     * preferOmit setter
     */
    public void setPreferOmit(final String omit) {
        this.preferOmit = omit;
    }

    /**
     * preferOmit getter
     */
    public String getPreferOmit() {
        return preferOmit;
    }

    /**
     * transaction getter
     */
    public String getTransaction() {
        return transaction;
    }

    /**
     * transaction setter
     */
    public void setTransaction(final String transaction) {
        this.transaction = transaction;
    }
}
