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

import static java.net.URI.create;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
