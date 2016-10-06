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

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createStatement;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.http.entity.ContentType.parse;
import static org.apache.jena.riot.RDFDataMgr.read;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.camel.processor.ProcessorUtils.getSubjectUri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.NoSuchHeaderException;
import org.apache.camel.Processor;

/**
 * Converts a Fedora Message into a format suitable for a LDN receiver.
 * See: http://www.w3.org/TR/ldn/
 *
 * @author acoburn
 */
public class LdnProcessor implements Processor {

    private static final String PROV = "http://www.w3.org/ns/prov#";

    private static final Property wasAssociatedWith = createProperty(PROV + "wasAssociatedWith");
    private static final Property wasAttributedTo = createProperty(PROV + "wasAttributedTo");
    private static final Property wasGeneratedBy = createProperty(PROV + "wasGeneratedBy");
    private static final Property used = createProperty(PROV + "used");

    /**
     * Process the Fedora message
     *
     * @param exchange the current camel message exchange
     */
    public void process(final Exchange exchange) throws IOException, NoSuchHeaderException {
        final Message in = exchange.getIn();
        final Model model = createDefaultModel();
        final Model newModel = createDefaultModel();
        final Resource resource = createResource(getSubjectUri(exchange));
        final Resource event = createResource("");
        final AtomicInteger counter = new AtomicInteger();
        final ByteArrayOutputStream serializedGraph = new ByteArrayOutputStream();

        read(model, in.getBody(InputStream.class),
                contentTypeToLang(parse(in.getHeader(CONTENT_TYPE, String.class)).getMimeType()));

        newModel.add(createStatement(event, used, resource));
        model.listObjectsOfProperty(resource, wasGeneratedBy).forEachRemaining(obj -> {
            if (obj.isResource()) {
                obj.asResource().listProperties().forEachRemaining(stmt -> {
                    newModel.add(createStatement(event, stmt.getPredicate(), stmt.getObject()));
                });
            }
        });
        model.listObjectsOfProperty(resource, wasAttributedTo).forEachRemaining(obj -> {
            final Resource agent = createResource("#agent" + Integer.toString(counter.getAndIncrement()));
            if (obj.isResource()) {
                obj.asResource().listProperties().forEachRemaining(stmt -> {
                    newModel.add(createStatement(agent, stmt.getPredicate(), stmt.getObject()));
                });
            }
            newModel.add(createStatement(event, wasAssociatedWith, agent));
        });

        newModel.write(serializedGraph, "JSON-LD");

        in.setBody(serializedGraph.toString("UTF-8"));
        in.setHeader(HTTP_METHOD, "POST");
        in.setHeader(CONTENT_TYPE, "application/ld+json");
    }
}
