/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author ajs6f
 */
@ExtendWith(MockitoExtension.class)
public class FcrepoEndpointTest {

    private final String FCREPO_URI = "fcrepo:foo/rest";

    private final String FCREPO_PATH = "foo/rest";

    @Mock
    private FcrepoComponent mockContext;

    @Mock
    private Processor mockProcessor;

    private final FcrepoConfiguration testConfig = new FcrepoConfiguration();

    @Test
    public void testNoConsumerCanBeCreated() throws RuntimeCamelException {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        assertThrows(RuntimeCamelException.class, () -> testEndpoint.createConsumer(mockProcessor));
    }

    @Test
    public void testCreateTransactionTemplate() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        assertNotNull(testEndpoint.createTransactionTemplate());
        testEndpoint.setTransactionManager(new FcrepoTransactionManager());
        assertNotNull(testEndpoint.createTransactionTemplate());
    }

    @Test
    public void testCreateProducer() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        final Producer testProducer = testEndpoint.createProducer();
        assertEquals(testEndpoint, testProducer.getEndpoint());
    }

    @Test
    public void testBaseUrl() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        assertEquals(testEndpoint.getBaseUrl(), FCREPO_PATH);
    }

    @Test
    public void testBaseUrlWithScheme1() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint("fcrepo:localhost:8080/rest", "localhost:8080/rest",
                mockContext, testConfig);
        assertEquals("http://localhost:8080/rest", testEndpoint.getBaseUrlWithScheme());
    }

    @Test
    public void testBaseUrlWithScheme2() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint("fcrepo:https://localhost/rest",
                "https://localhost/rest",
                mockContext, testConfig);
        assertEquals("https://localhost/rest", testEndpoint.getBaseUrlWithScheme());
    }

    @Test
    public void testBaseUrlWithScheme3() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint("fcrepo://localhost:443/rest",
                "localhost:443/rest",
                mockContext, testConfig);
        assertEquals("https://localhost:443/rest", testEndpoint.getBaseUrlWithScheme());
    }

    @Test
    public void testBaseUrlWithScheme4() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint("fcrepo:http://localhost:8080/rest",
                "http://localhost:8080/rest",
                mockContext, testConfig);
        assertEquals("http://localhost:8080/rest", testEndpoint.getBaseUrlWithScheme());
    }

    @Test
    public void testConfiguration() {
        final FcrepoConfiguration config = new FcrepoConfiguration();
        config.setFixity(true);

        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        assertEquals(false, testEndpoint.getFixity());
        testEndpoint.setConfiguration(config);
        assertEquals(true, testEndpoint.getFixity());
    }

    @Test
    public void testThrowExceptionOnFailure() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        assertEquals(true, testEndpoint.getThrowExceptionOnFailure());
        testEndpoint.setThrowExceptionOnFailure(false);
        assertEquals(false, testEndpoint.getThrowExceptionOnFailure());
    }

    @Test
    public void testFixity() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        assertEquals(false, testEndpoint.getFixity());
        testEndpoint.setFixity(true);
        assertEquals(true, testEndpoint.getFixity());
    }

    @Test
    public void testMetadata() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        assertEquals(true, testEndpoint.getMetadata());
        testEndpoint.setMetadata(false);
        assertEquals(false, testEndpoint.getMetadata());
    }

    @Test
    public void testAuthHost() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        final String authHost = "example.org";
        assertNull(testEndpoint.getAuthHost());
        testEndpoint.setAuthHost(authHost);
        assertEquals(authHost, testEndpoint.getAuthHost());
    }

    @Test
    public void testAuthUser() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        final String authUser = "fedoraAdmin";
        assertNull(testEndpoint.getAuthUsername());
        testEndpoint.setAuthUsername(authUser);
        assertEquals(authUser, testEndpoint.getAuthUsername());
    }

    @Test
    public void testAuthPassword() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        final String authPassword = "foo";
        assertNull(testEndpoint.getAuthPassword());
        testEndpoint.setAuthPassword(authPassword);
        assertEquals(authPassword, testEndpoint.getAuthPassword());
    }

    @Test
    public void testAccept() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        final String accept1 = "application/rdf+xml";
        final String accept2 = "text/turtle";
        final String accept3 = "application/ld+json";
        final String accept4 = "application/sparql-update";
        assertNull(testEndpoint.getAccept());
        testEndpoint.setAccept("application/rdf xml");
        assertEquals(accept1, testEndpoint.getAccept());
        testEndpoint.setAccept(accept2);
        assertEquals(accept2, testEndpoint.getAccept());
        testEndpoint.setAccept(accept3);
        assertEquals(accept3, testEndpoint.getAccept());
        testEndpoint.setAccept(accept4);
        assertEquals(accept4, testEndpoint.getAccept());
    }

    @Test
    public void testContentType() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        final String contentType1 = "application/rdf+xml";
        final String contentType2 = "text/turtle";
        final String contentType3 = "application/ld+json";
        final String contentType4 = "application/sparql-update";
        assertNull(testEndpoint.getContentType());
        testEndpoint.setContentType("application/rdf xml");
        assertEquals(contentType1, testEndpoint.getContentType());
        testEndpoint.setContentType(contentType2);
        assertEquals(contentType2, testEndpoint.getContentType());
        testEndpoint.setContentType(contentType3);
        assertEquals(contentType3, testEndpoint.getContentType());
        testEndpoint.setContentType(contentType4);
        assertEquals(contentType4, testEndpoint.getContentType());
    }

    @Test
    public void testPreferOmit() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        final String omit1 = "PreferContainment";
        final String omit2 = "http://www.w3.org/ns/ldp#PreferMembership";
        final String omit3 = "http://www.w3.org/ns/ldp#PreferMinimalContainer " +
                             "http://www.w3.org/ns/ldp#PreferContainment";
        assertNull(testEndpoint.getPreferOmit());
        testEndpoint.setPreferOmit(omit1);
        assertEquals(omit1, testEndpoint.getPreferOmit());
        testEndpoint.setPreferOmit(omit2);
        assertEquals(omit2, testEndpoint.getPreferOmit());
        testEndpoint.setPreferOmit(omit3);
        assertEquals(omit3, testEndpoint.getPreferOmit());
    }

    @Test
    public void testPreferInclude() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        final String include1 = "PreferContainment";
        final String include2 = "http://www.w3.org/ns/ldp#PreferMembership";
        final String include3 = "http://www.w3.org/ns/ldp#PreferMinimalContainer " +
                                "http://www.w3.org/ns/ldp#PreferContainment";
        assertNull(testEndpoint.getPreferInclude());
        testEndpoint.setPreferInclude(include1);
        assertEquals(include1, testEndpoint.getPreferInclude());
        testEndpoint.setPreferInclude(include2);
        assertEquals(include2, testEndpoint.getPreferInclude());
        testEndpoint.setPreferInclude(include3);
        assertEquals(include3, testEndpoint.getPreferInclude());
    }

    @Test
    public void testSingleton() {
        final FcrepoEndpoint testEndpoint = new FcrepoEndpoint(FCREPO_URI, FCREPO_PATH, mockContext, testConfig);
        assertTrue(testEndpoint.isSingleton());
    }

}
