/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

/**
 * Holds the state of an active fcrepo transaction. A single holder is bound to
 * the current thread while a transaction is open so that a participating
 * (nested) transaction and the transaction that opened it share the same
 * session and rollback-only flag.
 *
 * @author Dan Field
 */
class FcrepoSessionHolder {

    private final String sessionId;

    private boolean rollbackOnly;

    /**
     * Create a session holder for the given transaction session id.
     *
     * @param sessionId the transaction URI used as the Atomic-ID value
     */
    FcrepoSessionHolder(final String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * @return the transaction URI used as the Atomic-ID value
     */
    String getSessionId() {
        return sessionId;
    }

    /**
     * @return whether the shared transaction has been marked rollback-only
     */
    boolean isRollbackOnly() {
        return rollbackOnly;
    }

    /**
     * Mark the shared transaction as rollback-only.
     *
     * @param rollbackOnly the rollback-only flag
     */
    void setRollbackOnly(final boolean rollbackOnly) {
        this.rollbackOnly = rollbackOnly;
    }
}
