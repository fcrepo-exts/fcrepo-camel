/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static java.util.Arrays.asList;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_ID;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.fcrepo.camel.processor.EventProcessor;
import org.junit.jupiter.api.Test;

/**
 * Exercises the EventProcessor with bodies that use the JSON-LD compact ({@code @id}/{@code @type})
 * keywords, with a single (non-array) actor, and with pre-parsed Map bodies. These paths are not
 * exercised by the resource-driven {@link EventProcessorTest}.
 *
 * @author Dan Field
 */
public class EventProcessorDirectTest {

    private final EventProcessor processor = new EventProcessor();

    private Exchange newExchange(final Object body) {
        final Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(body);
        return exchange;
    }

    @Test
    public void testJsonWithKeywordsAndSingleActor() throws Exception {
        final String json = "{" +
                "\"@id\": \"urn:uuid:event-1\"," +
                "\"type\": [\"Create\"]," +
                "\"published\": \"2020-01-01T00:00:00Z\"," +
                "\"actor\": {\"name\": \"Some Agent\", \"id\": \"info:fedora/local-user#someAdmin\"}," +
                "\"object\": {" +
                    "\"@id\": \"http://localhost:8080/rest/path\"," +
                    "\"@type\": [\"http://www.w3.org/ns/ldp#Container\"]" +
                "}" +
                "}";

        final Exchange exchange = newExchange(json);
        processor.process(exchange);

        assertEquals("urn:uuid:event-1", exchange.getIn().getHeader(FCREPO_EVENT_ID));
        assertEquals("2020-01-01T00:00:00Z", exchange.getIn().getHeader(FCREPO_DATE_TIME));
        assertEquals("http://localhost:8080/rest/path", exchange.getIn().getHeader(FCREPO_URI));
        assertEquals(asList("https://www.w3.org/ns/activitystreams#Create"),
                exchange.getIn().getHeader(FCREPO_EVENT_TYPE));
        assertEquals(asList("http://www.w3.org/ns/ldp#Container"),
                exchange.getIn().getHeader(FCREPO_RESOURCE_TYPE));
        // id is applied after name for a single actor object
        assertEquals(asList("info:fedora/local-user#someAdmin"), exchange.getIn().getHeader(FCREPO_AGENT));
    }

    @Test
    public void testJsonSingleActorNameOnly() throws Exception {
        final String json = "{" +
                "\"id\": \"urn:uuid:event-2\"," +
                "\"type\": [\"Update\"]," +
                "\"actor\": {\"name\": \"Only Name\"}," +
                "\"object\": {\"id\": \"http://localhost:8080/rest/other\"}" +
                "}";

        final Exchange exchange = newExchange(json);
        processor.process(exchange);

        assertEquals("urn:uuid:event-2", exchange.getIn().getHeader(FCREPO_EVENT_ID));
        assertEquals(asList("Only Name"), exchange.getIn().getHeader(FCREPO_AGENT));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMapBodyWithKeywords() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        object.put("type", asList("http://www.w3.org/ns/ldp#Container"));
        object.put("id", "http://localhost:8080/rest/mapped");

        final Map<String, String> actorWithName = new HashMap<>();
        actorWithName.put("name", "Agent Name");
        final Map<String, String> actorWithId = new HashMap<>();
        actorWithId.put("id", "info:fedora/local-user#mapAdmin");

        final Map<String, Object> body = new HashMap<>();
        body.put("@id", "urn:uuid:event-3");
        body.put("@type", asList("Create"));
        body.put("published", "2021-02-03T00:00:00Z");
        body.put("object", object);
        body.put("actor", asList(actorWithName, actorWithId));

        final Exchange exchange = newExchange(body);
        processor.process(exchange);

        assertEquals("urn:uuid:event-3", exchange.getIn().getHeader(FCREPO_EVENT_ID));
        assertEquals("2021-02-03T00:00:00Z", exchange.getIn().getHeader(FCREPO_DATE_TIME));
        assertEquals("http://localhost:8080/rest/mapped", exchange.getIn().getHeader(FCREPO_URI));
        assertEquals(asList("https://www.w3.org/ns/activitystreams#Create"),
                exchange.getIn().getHeader(FCREPO_EVENT_TYPE));
        final List<String> agents = exchange.getIn().getHeader(FCREPO_AGENT, List.class);
        assertEquals(asList("Agent Name", "info:fedora/local-user#mapAdmin"), agents);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMapBodyWithLowercaseKeys() throws Exception {
        final Map<String, Object> body = new HashMap<>();
        body.put("id", "urn:uuid:event-4");
        body.put("type", asList("Update"));

        final Exchange exchange = newExchange(body);
        processor.process(exchange);

        assertEquals("urn:uuid:event-4", exchange.getIn().getHeader(FCREPO_EVENT_ID));
        assertEquals(asList("https://www.w3.org/ns/activitystreams#Update"),
                exchange.getIn().getHeader(FCREPO_EVENT_TYPE));
    }

    @Test
    public void testNullBody() throws Exception {
        final Exchange exchange = newExchange(null);
        processor.process(exchange);

        assertNull(exchange.getIn().getHeader(FCREPO_EVENT_ID));
        assertNull(exchange.getIn().getHeader(FCREPO_URI));
    }

    @Test
    public void testUnsupportedBodyType() throws Exception {
        // a body that is neither a Map, a String, nor an InputStream is ignored
        final Exchange exchange = newExchange(Integer.valueOf(42));
        processor.process(exchange);

        assertNull(exchange.getIn().getHeader(FCREPO_EVENT_ID));
        assertNull(exchange.getIn().getHeader(FCREPO_URI));
    }

    @Test
    public void testJsonMinimalNoObjectNoActor() throws Exception {
        final Exchange exchange = newExchange("{\"@id\": \"urn:uuid:event-minimal\"}");
        processor.process(exchange);

        assertEquals("urn:uuid:event-minimal", exchange.getIn().getHeader(FCREPO_EVENT_ID));
        assertNull(exchange.getIn().getHeader(FCREPO_URI));
        assertNull(exchange.getIn().getHeader(FCREPO_AGENT));
    }

    @Test
    public void testJsonEmptyTypeArrayIsDropped() throws Exception {
        final Exchange exchange = newExchange("{\"@id\": \"urn:uuid:event-empty\", \"type\": []}");
        processor.process(exchange);

        assertEquals("urn:uuid:event-empty", exchange.getIn().getHeader(FCREPO_EVENT_ID));
        // an empty value list is filtered out and never set as a header
        assertNull(exchange.getIn().getHeader(FCREPO_EVENT_TYPE));
    }

    @Test
    public void testJsonWithNonTextualValues() throws Exception {
        // mixes non-textual entries that should be skipped: a numeric type element, a numeric
        // actor name (falls through to the id), and an object whose @id is itself an object.
        final String json = "{" +
                "\"@id\": \"urn:uuid:event-mixed\"," +
                "\"type\": [\"Create\", 5]," +
                "\"actor\": {\"name\": 42, \"id\": \"info:fedora/local-user#mixedAdmin\"}," +
                "\"object\": {\"@id\": {\"nested\": \"x\"}, \"@type\": [\"http://example.org/T\"]}" +
                "}";

        final Exchange exchange = newExchange(json);
        processor.process(exchange);

        assertEquals(asList("https://www.w3.org/ns/activitystreams#Create"),
                exchange.getIn().getHeader(FCREPO_EVENT_TYPE));
        assertEquals(asList("info:fedora/local-user#mixedAdmin"), exchange.getIn().getHeader(FCREPO_AGENT));
        assertEquals(asList("http://example.org/T"), exchange.getIn().getHeader(FCREPO_RESOURCE_TYPE));
        // the non-textual @id is skipped, so no resource URI is set
        assertNull(exchange.getIn().getHeader(FCREPO_URI));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMapBodyObjectWithoutType() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        object.put("id", "http://localhost:8080/rest/notype");

        final Map<String, Object> body = new HashMap<>();
        body.put("id", "urn:uuid:event-5");
        body.put("object", object);

        final Exchange exchange = newExchange(body);
        processor.process(exchange);

        assertEquals("urn:uuid:event-5", exchange.getIn().getHeader(FCREPO_EVENT_ID));
        assertEquals("http://localhost:8080/rest/notype", exchange.getIn().getHeader(FCREPO_URI));
        assertNull(exchange.getIn().getHeader(FCREPO_RESOURCE_TYPE));
    }
}
