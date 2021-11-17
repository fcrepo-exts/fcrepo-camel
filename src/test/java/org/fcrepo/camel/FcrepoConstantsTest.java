/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author acoburn
 */
@RunWith(MockitoJUnitRunner.class)
public class FcrepoConstantsTest {

    @Test
    public void testConstants() {
        assertEquals(FcrepoProducer.DEFAULT_CONTENT_TYPE, "application/rdf+xml");
        assertEquals(FcrepoConstants.TRANSACTION, "/fcr:tx");
        assertEquals(FcrepoConstants.ROLLBACK, "/fcr:tx/fcr:rollback");
        assertEquals(FcrepoConstants.COMMIT, "/fcr:tx/fcr:commit");
        assertEquals(FcrepoConstants.FIXITY, "/fcr:fixity");
    }

    @Test
    public void testFcrepoHeaders() {
        assertEquals(FcrepoHeaders.FCREPO_BASE_URL, "CamelFcrepoBaseUrl");
        assertEquals(FcrepoHeaders.FCREPO_IDENTIFIER, "CamelFcrepoIdentifier");
        assertEquals(FcrepoHeaders.FCREPO_PREFER, "CamelFcrepoPrefer");
        assertEquals(FcrepoHeaders.FCREPO_NAMED_GRAPH, "CamelFcrepoNamedGraph");
        assertEquals(FcrepoHeaders.FCREPO_URI, "CamelFcrepoUri");
    }
}
