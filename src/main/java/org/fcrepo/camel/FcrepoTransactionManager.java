/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static org.fcrepo.camel.FcrepoConstants.TRANSACTION;
import static org.fcrepo.client.FcrepoClient.client;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.net.URI;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A Transaction Manager for interacting with fedora-based transactions
 *
 * @author Aaron Coburn
 * @since Feb 16, 2015
 */
public class FcrepoTransactionManager extends AbstractPlatformTransactionManager {

    private FcrepoClient fcrepoClient;

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
                response = getClient().post(URI.create(baseUrl + TRANSACTION))
                    .body(is, contentType).perform();
            } catch (final FcrepoOperationFailedException ex) {
                LOGGER.debug("HTTP Operation failed: ", ex);
                throw new CannotCreateTransactionException("Could not create fcrepo transaction");
            }

            if (response != null && response.getLocation() != null) {
                // The Location header holds the full transaction URI, which is
                // used as the value of the Atomic-ID header on subsequent requests.
                tx.setSessionId(response.getLocation().toString());
            } else {
                throw new CannotCreateTransactionException("Invalid response while creating transaction");
            }
        }

        // Bind the transaction to the current thread so that in-transaction
        // operations (and any nested PROPAGATION_REQUIRED templates) participate
        // in this single fcrepo transaction rather than starting their own.
        TransactionSynchronizationManager.bindResource(this, tx.getHolder());
    }

    @Override
    protected void doCommit(final DefaultTransactionStatus status) {
        final FcrepoTransactionObject tx = (FcrepoTransactionObject)status.getTransaction();

        try {
            // Commit the transaction with a PUT to the transaction URI.
            getClient().put(URI.create(tx.getSessionId())).perform();
        } catch (final FcrepoOperationFailedException ex) {
            LOGGER.debug("Transaction commit failed: ", ex);
            throw new TransactionSystemException("Could not commit fcrepo transaction");
        }
    }

    @Override
    protected void doRollback(final DefaultTransactionStatus status) {
        final FcrepoTransactionObject tx = (FcrepoTransactionObject)status.getTransaction();

        try {
            // Roll back the transaction with a DELETE to the transaction URI.
            getClient().delete(URI.create(tx.getSessionId())).perform();
        } catch (final FcrepoOperationFailedException ex) {
            LOGGER.debug("Transaction rollback failed: ", ex);
            throw new TransactionSystemException("Could not rollback fcrepo transaction");
        }
    }

    @Override
    protected Object doGetTransaction() {
        // Reuse the session bound to the current thread, if any, so that nested
        // transaction templates participate in the active fcrepo transaction.
        final FcrepoTransactionObject tx = new FcrepoTransactionObject();
        final FcrepoSessionHolder holder =
            (FcrepoSessionHolder) TransactionSynchronizationManager.getResource(this);
        if (holder != null) {
            tx.setHolder(holder);
        }
        return tx;
    }

    @Override
    protected boolean isExistingTransaction(final Object transaction) {
        return ((FcrepoTransactionObject)transaction).getSessionId() != null;
    }

    @Override
    protected void doSetRollbackOnly(final DefaultTransactionStatus status) {
        // Mark the shared session holder rollback-only so that the transaction
        // that opened it rolls back when it completes.
        final FcrepoSessionHolder holder = ((FcrepoTransactionObject)status.getTransaction()).getHolder();
        if (holder != null) {
            holder.setRollbackOnly(true);
        }
    }

    @Override
    protected void doCleanupAfterCompletion(final Object transaction) {
        // Unbind the thread-bound session once the transaction has completed.
        if (TransactionSynchronizationManager.hasResource(this)) {
            TransactionSynchronizationManager.unbindResource(this);
        }
        ((FcrepoTransactionObject)transaction).setSessionId(null);
    }

    private FcrepoClient getClient() {
        if (fcrepoClient == null) {
            return client().credentials(authUsername, authPassword).authScope(authHost)
                .throwExceptionOnFailure().build();
        }
        return fcrepoClient;
    }
}
