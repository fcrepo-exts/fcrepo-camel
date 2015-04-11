/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
    public void testJmsHeaders() {
        assertEquals(JmsHeaders.BASE_URL, "org.fcrepo.jms.baseURL");
        assertEquals(JmsHeaders.IDENTIFIER, "org.fcrepo.jms.identifier");
        assertEquals(JmsHeaders.EVENT_TYPE, "org.fcrepo.jms.eventType");
        assertEquals(JmsHeaders.PROPERTIES, "org.fcrepo.jms.properties");
        assertEquals(JmsHeaders.TIMESTAMP, "org.fcrepo.jms.timestamp");
        assertEquals(JmsHeaders.USER, "org.fcrepo.jms.user");
        assertEquals(JmsHeaders.USER_AGENT, "org.fcrepo.jms.userAgent");
        assertEquals(JmsHeaders.PREFIX, "org.fcrepo.jms.");
    }

    @Test
    public void testConstants() {
        assertEquals(FcrepoProducer.DEFAULT_CONTENT_TYPE, "application/rdf+xml");
        assertEquals(FcrepoConstants.TRANSACTION, "/fcr:tx");
        assertEquals(FcrepoConstants.ROLLBACK, "/fcr:tx/fcr:rollback");
        assertEquals(FcrepoConstants.COMMIT, "/fcr:tx/fcr:commit");
        assertEquals(FcrepoConstants.TOMBSTONE, "/fcr:tombstone");
        assertEquals(FcrepoConstants.TRANSFORM, "/fcr:transform");
    }

    @Test
    public void testFcrepoHeaders() {
        assertEquals(FcrepoHeaders.FCREPO_BASE_URL, "CamelFcrepoBaseUrl");
        assertEquals(FcrepoHeaders.FCREPO_IDENTIFIER, "CamelFcrepoIdentifier");
        assertEquals(FcrepoHeaders.FCREPO_TRANSFORM, "CamelFcrepoTransform");
        assertEquals(FcrepoHeaders.FCREPO_PREFER, "CamelFcrepoPrefer");
        assertEquals(FcrepoHeaders.FCREPO_NAMED_GRAPH, "CamelFcrepoNamedGraph");
    }

    @Test
    public void testRdfLexicon() {
        assertEquals(RdfNamespaces.REPOSITORY, "http://fedora.info/definitions/v4/repository#");
        assertEquals(RdfNamespaces.INDEXING, "http://fedora.info/definitions/v4/indexing#");
        assertEquals(RdfNamespaces.RDF, "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        assertEquals(RdfNamespaces.LDP, "http://www.w3.org/ns/ldp#");
    }

}
