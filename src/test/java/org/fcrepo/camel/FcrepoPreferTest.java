/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static java.net.URI.create;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class FcrepoPreferTest {

    private final String embed = "http://fedora.info/definitions/v4/repository#EmbedResources";

    private final String containment = "http://www.w3.org/ns/ldp#PreferContainment";

    @Test
    public void testPreferInclude() {
        final FcrepoPrefer prefer = new FcrepoPrefer("return=representation; " +
                "include=\"" + embed + "\"");
        assertTrue(prefer.isRepresentation());
        assertFalse(prefer.isMinimal());
        assertEquals(singletonList(create(embed)), prefer.getInclude());
        assertEquals(emptyList(), prefer.getOmit());
    }

    @Test
    public void testPreferIncludeMultiple() {
        final FcrepoPrefer prefer = new FcrepoPrefer("return=representation; " +
                "include=\"" + embed + " " + containment + "\"");
        assertTrue(prefer.isRepresentation());
        assertFalse(prefer.isMinimal());
        assertEquals(2, prefer.getInclude().size());
        assertTrue(prefer.getInclude().contains(create(embed)));
        assertTrue(prefer.getInclude().contains(create(containment)));
        assertEquals(emptyList(), prefer.getOmit());
    }

    @Test
    public void testPreferOmit() {
        final FcrepoPrefer prefer = new FcrepoPrefer("return=representation; " +
                "omit=\"" + embed + "\"");
        assertTrue(prefer.isRepresentation());
        assertFalse(prefer.isMinimal());
        assertEquals(1, prefer.getOmit().size());
        assertTrue(prefer.getOmit().contains(create(embed)));
        assertEquals(emptyList(), prefer.getInclude());
    }

    @Test
    public void testPreferOmitMultiple() {
        final FcrepoPrefer prefer = new FcrepoPrefer("return=representation; " +
                "omit=\"" + embed + " " + containment + "\"");
        assertTrue(prefer.isRepresentation());
        assertFalse(prefer.isMinimal());
        assertEquals(2, prefer.getOmit().size());
        assertTrue(prefer.getOmit().contains(create(embed)));
        assertTrue(prefer.getOmit().contains(create(containment)));
        assertEquals(emptyList(), prefer.getInclude());
    }

    @Test
    public void testPreferMinimal() {
        final FcrepoPrefer prefer = new FcrepoPrefer("return=minimal");
        assertTrue(prefer.isMinimal());
        assertFalse(prefer.isRepresentation());
        assertEquals(emptyList(), prefer.getInclude());
        assertEquals(emptyList(), prefer.getOmit());
    }

    @Test
    public void testPreferNoReturnType() {
        final FcrepoPrefer prefer = new FcrepoPrefer("include=\"" + embed + "\"");
        assertFalse(prefer.isMinimal());
        assertFalse(prefer.isRepresentation());
        assertEquals(singletonList(create(embed)), prefer.getInclude());
        assertEquals(emptyList(), prefer.getOmit());
    }

    @Test
    public void testPreferNull() {
        final FcrepoPrefer prefer = new FcrepoPrefer(null);
        assertFalse(prefer.isMinimal());
        assertFalse(prefer.isRepresentation());
    }

    @Test
    public void testPreferEmptyValue() {
        final FcrepoPrefer prefer = new FcrepoPrefer("return=representation; include=\"  \"");
        assertTrue(prefer.isRepresentation());
        assertEquals(emptyList(), prefer.getInclude());
    }
}
