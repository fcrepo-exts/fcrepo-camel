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
package org.fcrepo.camel.processor;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_ID;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Converts a Fedora Message into camel-based headers.
 *
 * @author acoburn
 */
public class EventProcessor implements Processor {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Process the Fedora message
     *
     * @param exchange the current camel message exchange
     */
    public void process(final Exchange exchange) throws IOException {
        final Object body = exchange.getIn().getBody();
        final Map<String, List<String>> data = new HashMap<>();
        if (body != null) {
            // In the event that the message was already converted to a Map
            if (body instanceof Map) {
                data.putAll(getValuesFromMap((Map)body));
            } else if (body instanceof String) {
                data.putAll(getValuesFromJson(mapper.readTree((String)body)));
            } else if (body instanceof InputStream) {
                data.putAll(getValuesFromJson(mapper.readTree((InputStream)body)));
            }
        }

        final Set<String> singleValuedFields = new HashSet<String>();
        singleValuedFields.add(FCREPO_URI);
        singleValuedFields.add(FCREPO_DATE_TIME);
        singleValuedFields.add(FCREPO_EVENT_ID);

        data.entrySet().stream().filter(entry -> entry.getValue() != null)
                .filter(entry -> !entry.getValue().isEmpty()).forEach(entry -> {
                    if (singleValuedFields.contains(entry.getKey())) {
                        exchange.getIn().setHeader(entry.getKey(), entry.getValue().get(0));
                    } else {
                        exchange.getIn().setHeader(entry.getKey(), entry.getValue());
                    }
                });
    }

    private Map<String, List<String>> getValuesFromJson(final JsonNode body) {
        final Map<String, List<String>> data = new HashMap<>();
        getValues(body, "@id").ifPresent(id -> data.put(FCREPO_URI, id));
        getValues(body, "id").ifPresent(id -> data.putIfAbsent(FCREPO_URI, id));

        getValues(body, "@type").ifPresent(type -> data.put(FCREPO_RESOURCE_TYPE, type));
        getValues(body, "type").ifPresent(type -> data.putIfAbsent(FCREPO_RESOURCE_TYPE, type));

        if (body.has("wasGeneratedBy")) {
            final JsonNode generatedBy = body.get("wasGeneratedBy");
            getValues(generatedBy, "type").ifPresent(type -> data.put(FCREPO_EVENT_TYPE, type));
            getValues(generatedBy, "atTime").ifPresent(time -> data.put(FCREPO_DATE_TIME, time));
            getValues(generatedBy, "identifier").ifPresent(id -> data.put(FCREPO_EVENT_ID, id));
        }

        if (body.has("wasAttributedTo")) {
            final JsonNode attributedTo = body.get("wasAttributedTo");
            if (attributedTo.isArray()) {
                final List<String> agents = new ArrayList<>();
                for (final JsonNode agent : attributedTo) {
                    getString(agent, "name").ifPresent(agents::add);
                }
                data.put(FCREPO_AGENT, agents);
            } else {
                getString(attributedTo, "name").ifPresent(name -> data.put(FCREPO_AGENT, singletonList(name)));
            }
        }

        return data;
    }

    private static Optional<String> getString(final JsonNode node, final String fieldName) {
        if (node.has(fieldName)) {
            final JsonNode field = node.get(fieldName);
            if (field.isTextual()) {
                return of(field.asText());
            }
        }
        return empty();
    }

    private static Optional<List<String>> getValues(final JsonNode node, final String fieldName) {
        if (node.has(fieldName)) {
            final JsonNode field = node.get(fieldName);
            if (field.isArray()) {
                final List<String> elements = new ArrayList<>();
                field.elements().forEachRemaining(elem -> {
                    if (elem.isTextual()) {
                        elements.add(elem.asText());
                    }
                });
                return of(elements);
            } else if (field.isTextual()) {
                return of(singletonList(field.asText()));
            }
        }
        return empty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> getValuesFromMap(final Map body) {
        final Map<String, Object> values = (Map<String, Object>)body;
        final Map<String, List<String>> data = new HashMap<>();
        if (values.containsKey("@id")) {
            data.put(FCREPO_URI, singletonList((String)values.get("@id")));
        }
        if (values.containsKey("id")) {
            data.putIfAbsent(FCREPO_URI, singletonList((String)values.get("id")));
        }

        if (values.containsKey("@type")) {
            data.put(FCREPO_RESOURCE_TYPE, (List<String>)values.get("@type"));
        }
        if (values.containsKey("type")) {
            data.putIfAbsent(FCREPO_RESOURCE_TYPE, (List<String>)values.get("type"));
        }

        final Map<String, Object> wasGeneratedBy = (Map<String, Object>)values.get("wasGeneratedBy");

        if (wasGeneratedBy != null) {
            if (wasGeneratedBy.containsKey("type")) {
                data.put(FCREPO_EVENT_TYPE, (List<String>)wasGeneratedBy.get("type"));
            }
            data.put(FCREPO_EVENT_ID, singletonList((String)wasGeneratedBy.get("identifier")));
            data.put(FCREPO_DATE_TIME, singletonList((String)wasGeneratedBy.get("atTime")));
        }

        final List<Map<String, String>> wasAttributedTo = (List<Map<String, String>>)values.get("wasAttributedTo");
        if (wasAttributedTo != null) {
            data.put(FCREPO_AGENT,
                    wasAttributedTo.stream().map(agent -> agent.get("name")).collect(toList()));
        }

        return data;
    }
}
