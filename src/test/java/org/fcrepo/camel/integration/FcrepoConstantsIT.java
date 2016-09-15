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
package org.fcrepo.camel.integration;

import static org.junit.Assert.assertEquals;

import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.RdfNamespaces;
import org.fcrepo.jms.DefaultMessageFactory;
import org.fcrepo.kernel.api.RdfLexicon;
import org.junit.Test;

/**
 * @author acoburn
 */
public class FcrepoConstantsIT {

    @Test
    public void testJmsHeaders() {
        assertEquals(JmsHeaders.BASE_URL, DefaultMessageFactory.BASE_URL_HEADER_NAME);
        assertEquals(JmsHeaders.IDENTIFIER, DefaultMessageFactory.IDENTIFIER_HEADER_NAME);
        assertEquals(JmsHeaders.EVENT_TYPE, DefaultMessageFactory.EVENT_TYPE_HEADER_NAME);
        assertEquals(JmsHeaders.EVENT_ID, DefaultMessageFactory.EVENT_ID_HEADER_NAME);
        assertEquals(JmsHeaders.TIMESTAMP, DefaultMessageFactory.TIMESTAMP_HEADER_NAME);
        assertEquals(JmsHeaders.USER, DefaultMessageFactory.USER_HEADER_NAME);
        assertEquals(JmsHeaders.USER_AGENT, DefaultMessageFactory.USER_AGENT_HEADER_NAME);
        assertEquals(JmsHeaders.PREFIX, DefaultMessageFactory.JMS_NAMESPACE);
    }

    @Test
    public void testRdfLexicon() {
        assertEquals(RdfNamespaces.REPOSITORY, RdfLexicon.REPOSITORY_NAMESPACE);
        assertEquals(RdfNamespaces.INDEXING, RdfLexicon.INDEXING_NAMESPACE);
        assertEquals(RdfNamespaces.RDF, RdfLexicon.RDF_NAMESPACE);
        assertEquals(RdfNamespaces.LDP, RdfLexicon.LDP_NAMESPACE);
        assertEquals(RdfNamespaces.PREMIS, RdfLexicon.PREMIS_NAMESPACE);
        assertEquals(RdfNamespaces.REPOSITORY + "ServerManaged", RdfLexicon.SERVER_MANAGED.toString());
        assertEquals(RdfNamespaces.REPOSITORY + "EmbedResources", RdfLexicon.EMBED_CONTAINS.toString());
        assertEquals(RdfNamespaces.REPOSITORY + "InboundReferences", RdfLexicon.INBOUND_REFERENCES.toString());
    }
}
