/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.processor.ProcessorUtils.getSubjectUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.camel.Exchange;
import org.apache.camel.NoSuchHeaderException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link org.fcrepo.camel.processor.ProcessorUtils#getSubjectUri}.
 *
 * @author Dan Field
 */
public class ProcessorUtilsSubjectTest {

    private Exchange newExchange() {
        return new DefaultExchange(new DefaultCamelContext());
    }

    @Test
    public void testSubjectFromUriHeader() throws NoSuchHeaderException {
        final Exchange exchange = newExchange();
        exchange.getIn().setHeader(FCREPO_URI, "http://localhost:8080/rest/foo");
        assertEquals("http://localhost:8080/rest/foo", getSubjectUri(exchange));
    }

    @Test
    public void testSubjectFromBaseAndIdentifier() throws NoSuchHeaderException {
        final Exchange exchange = newExchange();
        exchange.getIn().setHeader(FCREPO_BASE_URL, "http://localhost:8080/rest");
        exchange.getIn().setHeader(FCREPO_IDENTIFIER, "/foo");
        assertEquals("http://localhost:8080/rest/foo", getSubjectUri(exchange));
    }

    @Test
    public void testSubjectTrimsTrailingSlashOnBase() throws NoSuchHeaderException {
        final Exchange exchange = newExchange();
        exchange.getIn().setHeader(FCREPO_BASE_URL, "http://localhost:8080/rest/");
        exchange.getIn().setHeader(FCREPO_IDENTIFIER, "/foo");
        assertEquals("http://localhost:8080/rest/foo", getSubjectUri(exchange));
    }

    @Test
    public void testSubjectMissingBaseUrlThrows() {
        final Exchange exchange = newExchange();
        assertThrows(NoSuchHeaderException.class, () -> getSubjectUri(exchange));
    }
}
