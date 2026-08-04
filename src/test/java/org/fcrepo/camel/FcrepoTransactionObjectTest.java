/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Dan Field
 */
public class FcrepoTransactionObjectTest {

    @Test
    public void testSessionId() {
        final String txUri = "http://localhost:8080/rest/fcr:tx/1234567890";
        final FcrepoTransactionObject tx = new FcrepoTransactionObject();

        assertNull(tx.getSessionId());
        assertNull(tx.getHolder());
        assertFalse(tx.isRollbackOnly());

        tx.setSessionId(txUri);
        assertEquals(txUri, tx.getSessionId());
        assertEquals(txUri, tx.getHolder().getSessionId());
        assertFalse(tx.isRollbackOnly());

        // no-op, but exercises the SmartTransactionObject contract
        tx.flush();

        tx.setSessionId(null);
        assertNull(tx.getSessionId());
        assertNull(tx.getHolder());
        assertFalse(tx.isRollbackOnly());
    }

    @Test
    public void testRollbackOnly() {
        final FcrepoSessionHolder holder = new FcrepoSessionHolder("http://localhost:8080/rest/fcr:tx/abc");
        final FcrepoTransactionObject tx = new FcrepoTransactionObject();

        // Sharing an existing holder is how a participating transaction observes
        // the rollback-only flag set on the transaction that opened it.
        tx.setHolder(holder);
        assertSame(holder, tx.getHolder());
        assertEquals("http://localhost:8080/rest/fcr:tx/abc", tx.getSessionId());
        assertFalse(tx.isRollbackOnly());

        holder.setRollbackOnly(true);
        assertTrue(tx.isRollbackOnly());
    }
}
