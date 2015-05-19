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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.net.URI;

import org.slf4j.Logger;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * A Transaction Manager for interacting with fedora-based transactions
 *
 * @author Aaron Coburn
 * @since Feb 16, 2015
 */
public class FcrepoTransactionManager extends AbstractPlatformTransactionManager {

    private FcrepoClient client;

    private String baseUrl;

    private String authUsername;

    private String authPassword;

    private String authHost;

    private static final Logger LOGGER = getLogger(FcrepoTransactionManager.class);

    /**
     * Create a FcrepoTransactionManager
     */
    public FcrepoTransactionManager() {
        super();
        setNestedTransactionAllowed(false);
    }

    /**
     * Set the baseUrl for the transaction manager.
     *
     * @param baseUrl the fcrepo base url
     */
    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Get the base url for the transaction manager.
     *
     * @return the fcrepo base url
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Set the authUsername for the transaction manager.
     *
     * @param authUsername the username for authentication
     */
    public void setAuthUsername(final String authUsername) {
        this.authUsername = authUsername;
    }

    /**
     * Get the authUsername for the transaction manager.
     *
     * @return the username for authentication
     */
    public String getAuthUsername() {
        return authUsername;
    }

    /**
     * Set the authPassword for the transaction manager.
     *
     * @param authPassword the password used for authentication
     */
    public void setAuthPassword(final String authPassword) {
        this.authPassword = authPassword;
    }

    /**
     * Get the authPassword for the transaction manager.
     *
     * @return the password used for authentication
     */
    public String getAuthPassword() {
        return authPassword;
    }

    /**
     * Set the authHost for the transaction manager.
     *
     * @param authHost the host realm used for authentication
     */
    public void setAuthHost(final String authHost) {
        this.authHost = authHost;
    }

    /**
     * Get the authHost for the transaction manager.
     *
     * @return the host realm used for authentication
     */
    public String getAuthHost() {
        return authHost;
    }

    @Override
    protected void doBegin(final Object transaction, final TransactionDefinition definition) {
        final FcrepoResponse response;
        final InputStream is = null;
        final String contentType = null;
        final FcrepoTransactionObject tx = (FcrepoTransactionObject)transaction;

        if (tx.getSessionId() == null) {
            try {
                response = getClient().post(URI.create(baseUrl + FcrepoConstants.TRANSACTION), is, contentType);
            } catch (FcrepoOperationFailedException ex) {
                LOGGER.debug("HTTP Operation failed: ", ex);
                throw new CannotCreateTransactionException("Could not create fcrepo transaction");
            }

            if (response != null) {
                response.getLocation()
                    .map(x -> x.toString())
                    .map(x -> x.substring(baseUrl.length() + 1))
                    .ifPresent(x -> tx.setSessionId(x));
            }

            if (tx.getSessionId() == null) {
                throw new CannotCreateTransactionException("Invalid response while creating transaction");
            }
        }
    }

    @Override
    protected void doCommit(final DefaultTransactionStatus status) {
        final FcrepoTransactionObject tx = (FcrepoTransactionObject)status.getTransaction();
        final InputStream is = null;
        final String contentType = null;

        try {
            getClient().post(URI.create(baseUrl + "/" + tx.getSessionId() + FcrepoConstants.COMMIT), is, contentType);
        } catch (FcrepoOperationFailedException ex) {
            LOGGER.debug("Transaction commit failed: ", ex);
            throw new TransactionSystemException("Could not commit fcrepo transaction");
        } finally {
            tx.setSessionId(null);
        }
    }

    @Override
    protected void doRollback(final DefaultTransactionStatus status) {
        final FcrepoTransactionObject tx = (FcrepoTransactionObject)status.getTransaction();

        try {
            getClient().post(URI.create(baseUrl + "/" + tx.getSessionId() + FcrepoConstants.ROLLBACK), null, null);
        } catch (FcrepoOperationFailedException ex) {
            LOGGER.debug("Transaction rollback failed: ", ex);
            throw new TransactionSystemException("Could not rollback fcrepo transaction");
        } finally {
            tx.setSessionId(null);
        }
    }

    @Override
    protected Object doGetTransaction() {
        return new FcrepoTransactionObject();
    }

    private FcrepoClient getClient() {
        final Boolean throwExceptionOnFailure = true;

        if (client == null) {
            client = new FcrepoClient(
                    authUsername,
                    authPassword,
                    authHost,
                    throwExceptionOnFailure);
        }
        return client;
    }
}
