/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

/**
 * A class representing the value of an HTTP Prefer header
 *
 * @author Aaron Coburn
 */
public class FcrepoPrefer {

    private static final Logger LOGGER = getLogger(FcrepoPrefer.class);

    private static final String LINK_DELIM = "\\s*;\\s*";

    private static final String OMIT = "omit";

    private static final String INCLUDE = "include";

    private String returnType = null;

    private List<URI> omit;

    private List<URI> include;

    /**
     * Create a representation of a Prefer header.
     *
     * @param prefer the value for a Prefer header
     */
    public FcrepoPrefer(final String prefer) {
        parse(prefer);
    }

    /**
     * Whether a minimal representation has been requested
     * @return whether this is a minimal request
     */
    public boolean isMinimal() {
        return returnType != null && returnType.equals("minimal");
    }

    /**
     * Whether a non-minimal representation has been requested
     * @return whether this is a non-minimal request
     */
    public boolean isRepresentation() {
        return returnType != null && returnType.equals("representation");
    }

    /**
     * Retrieve the omit portion of a Prefer header
     *
     * @return the omit portion of a Prefer header
     */
    public List<URI> getOmit() {
        return omit;
    }

    /**
     * Retrieve the include portion of the prefer header
     *
     * @return the include portion of a Prefer header
     */
    public List<URI> getInclude() {
        return include;
    }

    /**
     * Parse the value of a prefer header
     */
    private void parse(final String prefer) {
        if (prefer != null) {
            final Map<String, String> data = new HashMap<>();

            for (final String section : prefer.split(LINK_DELIM)) {
                final String[] parts = section.split("=");
                if (parts.length == 2) {
                    final String value;
                    if (parts[1].startsWith("\"") && parts[1].endsWith("\"")) {
                        value = parts[1].substring(1, parts[1].length() - 1);
                    } else {
                        value = parts[1];
                    }
                    data.put(parts[0], value);
                }
            }
            this.returnType = data.get("return");
            this.include = getUris(data.get(INCLUDE));
            this.omit = getUris(data.get(OMIT));
        } else {
            LOGGER.warn("Could not parse a null Prefer value");
        }
    }

    private List<URI> getUris(final String uris) {
        if (uris != null) {
            return stream(uris.split("\\s+")).filter(uri -> uri.length() > 0).map(URI::create).collect(toList());
        }
        return emptyList();
    }
}
