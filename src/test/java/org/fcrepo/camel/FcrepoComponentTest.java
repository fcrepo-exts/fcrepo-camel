/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class FcrepoComponentTest {

    private static final String TEST_ENDPOINT_URI = "fcrepo:foo";

    private static final Map<String, Object> EMPTY_MAP = emptyMap();

    @Mock
    private CamelContext mockContext;

    @Test
    public void testCreateEndpoint() {
        final FcrepoComponent testComponent = new FcrepoComponent(mockContext);
        final Endpoint testEndpoint = testComponent.createEndpoint(TEST_ENDPOINT_URI, "", EMPTY_MAP);
        assertEquals(mockContext, testEndpoint.getCamelContext());
        assertEquals(TEST_ENDPOINT_URI, testEndpoint.getEndpointUri());
    }

    @Test
    public void testCreateEndpointFromConfig() {
        final FcrepoConfiguration configuration = new FcrepoConfiguration();

        configuration.setMetadata(false);

        final FcrepoComponent testComponent = new FcrepoComponent(configuration);
        final Endpoint testEndpoint = testComponent.createEndpoint(TEST_ENDPOINT_URI, "", EMPTY_MAP);
        assertEquals(TEST_ENDPOINT_URI, testEndpoint.getEndpointUri());

        assertEquals(false, testComponent.getConfiguration().getMetadata());
    }

    @Test
    public void testPreConfiguredComponent() {
        final FcrepoConfiguration config = new FcrepoConfiguration();
        config.setAuthUsername("foo");
        config.setAuthPassword("bar");
        config.setAuthHost("baz");

        final FcrepoComponent testComponent = new FcrepoComponent();

        testComponent.setConfiguration(config);

        final Endpoint testEndpoint = testComponent.createEndpoint(TEST_ENDPOINT_URI, "", EMPTY_MAP);

        assertEquals(TEST_ENDPOINT_URI, testEndpoint.getEndpointUri());
        assertEquals("foo", testComponent.getConfiguration().getAuthUsername());
        assertEquals("bar", testComponent.getConfiguration().getAuthPassword());
        assertEquals("baz", testComponent.getConfiguration().getAuthHost());
    }

    @Test
    public void testPostConfiguredComponent() {
        final FcrepoComponent testComponent = new FcrepoComponent();
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        testComponent.setAuthUsername("foo");
        testComponent.setAuthPassword("bar");
        testComponent.setAuthHost("baz");
        testComponent.setTransactionManager(txMgr);

        final Endpoint testEndpoint = testComponent.createEndpoint(TEST_ENDPOINT_URI, "", EMPTY_MAP);

        assertEquals(TEST_ENDPOINT_URI, testEndpoint.getEndpointUri());
        assertEquals("foo", testComponent.getConfiguration().getAuthUsername());
        assertEquals("bar", testComponent.getConfiguration().getAuthPassword());
        assertEquals("baz", testComponent.getConfiguration().getAuthHost());
        assertNull(testComponent.getConfiguration().getTransactionManager());
        assertEquals(txMgr, testComponent.getTransactionManager());

        testComponent.getConfiguration().setTransactionManager(txMgr);

        assertEquals(txMgr, testComponent.getConfiguration().getTransactionManager());
    }

    @Test
    public void testCreateEndpointFromDefaultConstructor() {
        final FcrepoComponent testComponent = new FcrepoComponent();
        final Endpoint testEndpoint = testComponent.createEndpoint(TEST_ENDPOINT_URI, "", EMPTY_MAP);
        assertEquals(TEST_ENDPOINT_URI, testEndpoint.getEndpointUri());
    }

}
