/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import java.lang.reflect.Field;

/**
 * @author acoburn
 */
class TestUtils {

    private TestUtils() { }

    public static final String baseUrl = "http://localhost:8080/rest/foo";

    public static final String baseUrlSecure = "https://localhost:443/rest/secure";

    public static final String baseUrlSecureWithoutPort = "https://localhost/rest/secure";

    public static final String rdfXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
              "<rdf:Description rdf:about=\"http://localhost:8080/rest/foo\">" +
                "<mixinTypes xmlns=\"http://fedora.info/definitions/v4/repository#\" " +
                    "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">fedora:resource</mixinTypes>" +
              "</rdf:Description>" +
            "</rdf:RDF>";

    public static final String fixityTriples =
            "<http://localhost:8080/rest/binary> " +
                "<http://www.loc.gov/premis/rdf/v1#hasFixity> " +
                "<http://localhost:8080/rest/binary#fixity/1434565188010> .\n" +
            "<http://localhost:8080/rest/binary#fixity/1434565188010> " +
                "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " +
                "<http://www.loc.gov/premis/rdf/v1#Fixity> .\n" +
            "<http://localhost:8080/rest/binary#fixity/1434565188010> " +
                "<http://fedora.info/definitions/v4/repository#status> " +
                "\"SUCCESS\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "<http://localhost:8080/rest/binary#fixity/1434565188010> " +
                "<http://www.loc.gov/premis/rdf/v1#hasMessageDigest> " +
                "<urn:sha1:a8069665cdee87b7963676cfcc759ef0f0c9a0e9> .\n" +
            "<http://localhost:8080/rest/binary#fixity/1434565188010> " +
                "<http://www.loc.gov/premis/rdf/v1#hasSize> " +
                "\"1636261\"^^<http://www.w3.org/2001/XMLSchema#long> .\n";

    public static final String serializedJson = "[{\"title\": \"Test title\"}]";

    public static final String rdfTriples = "<http://localhost:8080/rest/foo> " +
                   "<http://fedora.info/definitions/v4/repository#mixinTypes> " +
                   "\"fedora:resource\"";

    public static final String sparqlUpdate = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT { <> dc:title \"Foo\" . } WHERE {}";

    public static final String SPARQL_UPDATE = "application/sparql-update";

    public static final String RDF_XML = "application/rdf+xml";

    public static final String TEXT_TURTLE = "text/turtle";

    public static final String N_TRIPLES = "application/n-triples";

    public static final String TEXT_PLAIN = "text/plain";

    public static final String RDF_LDPATH = "application/rdf+ldpath";

    public static final String JSON = "application/json";

    public static void setField(final Object parent, final String name,
        final Object obj) {
        /* check the parent class too if the field could not be found */
        try {
            final Field f = findField(parent.getClass(), name);
            f.setAccessible(true);
            f.set(parent, obj);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static Field findField(final Class<?> clazz, final String name)
            throws NoSuchFieldException {
        for (final Field f : clazz.getDeclaredFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        if (clazz.getSuperclass() == null) {
            throw new NoSuchFieldException("Field " + name +
                                                   " could not be found");
        }
        return findField(clazz.getSuperclass(), name);
    }
}

