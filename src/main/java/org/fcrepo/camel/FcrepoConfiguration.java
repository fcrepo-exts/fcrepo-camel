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
package org.fcrepo.camel;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * An FcrepoConfiguration class.
 *
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
    private String authUsername = null;

    @UriParam
    private String authPassword = null;

    @UriParam
    private String authHost = null;

    @UriParam
    private Boolean fixity = false;

    @UriParam
    private Boolean metadata = true;

    @UriParam
    private Boolean throwExceptionOnFailure = true;

    @UriParam
    private String preferInclude = null;

    @UriParam
    private String preferOmit = null;

    @UriParam
    private PlatformTransactionManager transactionManager = null;

    /**
     * Create a new FcrepoConfiguration object
     */
    public FcrepoConfiguration() {
        super();
    }

    /**
     * Copy an FcrepoConfiguration object.
     *
     * @return a copy of the component-wide configuration
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
     * 
     * @param url the baseUrl string
     */
    public void setBaseUrl(final String url) {
        this.baseUrl = url;
    }

    /**
     * baseUrl getter
     *
     * @return the fedora base url
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * accept setter
     *
     * @param type the mime-type for Accept headers
     */
    public void setAccept(final String type) {
        this.accept = type.replaceAll(" ", "+");
    }

    /**
     * accept getter
     *
     * @return the mime-type for Accept headers
     */
    public String getAccept() {
        return accept;
    }

    /**
     * contentType setter
     *
     * @param type the mime-type used with Content-Type headers
     */
    public void setContentType(final String type) {
        this.contentType = type.replaceAll(" ", "+");
    }

    /**
     * contentType getter
     *
     * @return the mime-type used with Content-Type headers
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * authUsername setter
     * 
     * @param username used for repository authentication
     */
    public void setAuthUsername(final String username) {
        this.authUsername = username;
    }

    /**
     * authUsername getter
     *
     * @return the username used for repository authentication
     */
    public String getAuthUsername() {
        return authUsername;
    }

    /**
     * authPassword setter
     * 
     * @param password used for repository authentication
     */
    public void setAuthPassword(final String password) {
        this.authPassword = password;
    }

    /**
     * authPassword getter
     *
     * @return the password used for repository authentication
     */
    public String getAuthPassword() {
        return authPassword;
    }

    /**
     * authHost setter
     * 
     * @param host used for authentication
     */
    public void setAuthHost(final String host) {
        this.authHost = host;
    }

    /**
     * authHost getter
     *
     * @return the host realm used for repository authentication
     */
    public String getAuthHost() {
        return authHost;
    }

    /**
     * metadata setter
     * 
     * @param metadata whether to retrieve rdf metadata for non-rdf nodes
     */
    public void setMetadata(final Boolean metadata) {
        this.metadata = metadata;
    }

    /**
     * metadata getter
     *
     * @return whether to retrieve the rdf metadata for non-rdf nodes
     */
    public Boolean getMetadata() {
        return metadata;
    }

    /**
     * throwExceptionOnFailure setter
     *
     * @param throwOnFailure whether HTTP response errors throw exceptions
     */
    public void setThrowExceptionOnFailure(final Boolean throwOnFailure) {
        this.throwExceptionOnFailure = throwOnFailure;
    }

    /**
     * throwExceptionOnFailure getter
     *
     * @return whether HTTP response errors throw exceptions
     */
    public Boolean getThrowExceptionOnFailure() {
        return throwExceptionOnFailure;
    }

    /**
     * preferInclude setter
     *
     * @param include the URI(s) that populate the include section in a Prefer header
     */
    public void setPreferInclude(final String include) {
        this.preferInclude = include;
    }

    /**
     * preferInclude getter
     * 
     * @return the URI(s) that populate the include section in a Prefer header
     */
    public String getPreferInclude() {
        return preferInclude;
    }

    /**
     * preferOmit setter
     *
     * @param omit the URI(s) that populate the omit section in a Prefer header
     */
    public void setPreferOmit(final String omit) {
        this.preferOmit = omit;
    }

    /**
     * preferOmit getter
     *
     * @return the URI(s) that populate the omit section in a Prefer header
     */
    public String getPreferOmit() {
        return preferOmit;
    }


    /**
     * transactionManager setter
     *
     * @param transactionManager the transaction manager for handling transactions
     */
    public void setTransactionManager(final PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * transactionManger getter
     *
     * @return the transaction manager for handling transactions
     */
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * fixity setter
     *
     * @param fixity whether to run a fixity check on the fcrepo resource
     */
    public void setFixity(final Boolean fixity) {
        this.fixity = fixity;
    }

    /**
     * fixity getter
     *
     * @return whether to access the /fcr:fixity endpoint for a resource
     */
    public Boolean getFixity() {
        return fixity;
    }


}
