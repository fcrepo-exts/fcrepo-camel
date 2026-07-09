/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

/**
 * @author acoburn
 */
public final class FcrepoHeaders {

    public static final String FCREPO_BASE_URL = "CamelFcrepoBaseUrl";

    public static final String FCREPO_IDENTIFIER = "CamelFcrepoIdentifier";

    public static final String FCREPO_PREFER = "CamelFcrepoPrefer";

    public static final String FCREPO_NAMED_GRAPH = "CamelFcrepoNamedGraph";

    public static final String FCREPO_URI = "CamelFcrepoUri";

    public static final String FCREPO_EVENT_TYPE = "CamelFcrepoEventType";

    public static final String FCREPO_RESOURCE_TYPE = "CamelFcrepoResourceType";

    public static final String FCREPO_DATE_TIME = "CamelFcrepoDateTime";

    public static final String FCREPO_AGENT = "CamelFcrepoAgent";

    public static final String FCREPO_EVENT_ID = "CamelFcrepoEventId";

    /**
     * The requested response content type. Replaces the {@code ACCEPT_CONTENT_TYPE}
     * constant removed from {@code org.apache.camel.Exchange} in Camel 4, keeping the
     * original {@code "CamelAcceptContentType"} value so existing routes continue to work.
     */
    public static final String ACCEPT_CONTENT_TYPE = "CamelAcceptContentType";

    private FcrepoHeaders() {
        // prevent instantiation
    }
}
