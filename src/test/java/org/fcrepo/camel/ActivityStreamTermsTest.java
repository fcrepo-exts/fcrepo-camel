/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static java.net.URI.create;
import static org.fcrepo.camel.processor.ActivityStreamTerms.ACTIVITY_STREAMS_BASEURI;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.fcrepo.camel.processor.ActivityStreamTerms;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ActivityStreamTerms}.
 *
 * @author Dan Field
 */
public class ActivityStreamTermsTest {

    @Test
    public void testAsUri() {
        assertEquals(create(ACTIVITY_STREAMS_BASEURI + "Create"), ActivityStreamTerms.Create.asUri());
        assertEquals(create(ACTIVITY_STREAMS_BASEURI + "Update"), ActivityStreamTerms.Update.asUri());
    }

    @Test
    public void testExpandKnownTerm() {
        assertEquals(ACTIVITY_STREAMS_BASEURI + "Delete", ActivityStreamTerms.expand("Delete"));
    }

    @Test
    public void testExpandUnknownTermReturnsInput() {
        assertEquals("http://example.org/NotATerm", ActivityStreamTerms.expand("http://example.org/NotATerm"));
    }
}
