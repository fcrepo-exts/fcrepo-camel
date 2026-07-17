/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import org.springframework.transaction.support.SmartTransactionObject;

/**
 * @author Aaron Coburn
 * @since February 19, 2015
 */
class FcrepoTransactionObject implements SmartTransactionObject {

    private FcrepoSessionHolder holder;

    /**
     * Set the session id for this transaction. A {@code null} value clears the
     * associated session holder.
     *
     * @param sessionId the identifier for this session
     */
    public void setSessionId(final String sessionId) {
        this.holder = (sessionId == null) ? null : new FcrepoSessionHolder(sessionId);
    }

    /**
     * Get the session id for this transaction
     *
     * @return the identifier for this session
     */
    public String getSessionId() {
        return holder == null ? null : holder.getSessionId();
    }

    /**
     * Get the (potentially thread-shared) session holder backing this transaction.
     *
     * @return the session holder, or {@code null} if no session is associated
     */
    FcrepoSessionHolder getHolder() {
        return holder;
    }

    /**
     * Associate this transaction object with an existing session holder, allowing
     * a participating transaction to share the holder of the active transaction.
     *
     * @param holder the shared session holder
     */
    void setHolder(final FcrepoSessionHolder holder) {
        this.holder = holder;
    }

    @Override
    public boolean isRollbackOnly() {
        return holder != null && holder.isRollbackOnly();
    }

    @Override
    public void flush() {
        // no-op: fcrepo transactions have no intermediate flush semantics
    }
}
