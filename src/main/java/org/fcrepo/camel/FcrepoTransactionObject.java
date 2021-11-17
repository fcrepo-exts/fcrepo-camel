/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

/**
 * @author Aaron Coburn
 * @since February 19, 2015
 */
class FcrepoTransactionObject {

    private String sessionId;

    /**
     * Set the session id for this transaction
     *
     * @param sessionId the identifier for this session
     */
    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Get the session id for this transaction
     *
     * @return the identifier for this session
     */
    public String getSessionId() {
        return sessionId;
    }
}
