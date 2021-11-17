/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.camel.processor;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * ActivityStream vocabulary
 *
 * @author apb@jhu.edu
 */
public enum ActivityStreamTerms {

    Accept,
    Activity,
    IntransitiveActivity,
    Add,
    Announce,
    Application,
    Arrive,
    Article,
    Audio,
    Block,
    Collection,
    CollectionPage,
    Relationship,
    Create,
    Delete,
    Dislike,
    Document,
    Event,
    Follow,
    Flag,
    Group,
    Ignore,
    Image,
    Invite,
    Join,
    Leave,
    Like,
    Link,
    Mention,
    Note,
    Object,
    Offer,
    OrderedCollection,
    OrderedCollectionPage,
    Organization,
    Page,
    Person,
    Place,
    Profile,
    Question,
    Reject,
    Remove,
    Service,
    TentativeAccept,
    TentativeReject,
    Tombstone,
    Undo,
    Update,
    Video,
    View,
    Listen,
    Read,
    Move,
    Travel,
    IsFollowing,
    IsFollowedBy,
    IsContact,
    IsMember;

    /** ActivityStreams baseURI */
    public static final String ACTIVITY_STREAMS_BASEURI = "https://www.w3.org/ns/activitystreams#";

    private static Map<String, String> terms = asList(values()).stream()
            .map(Objects::toString)
            .collect(toMap(identity(), t -> ACTIVITY_STREAMS_BASEURI + t));

    /**
     * Return the URI of this term.
     *
     * @return The URI of this term.
     */
    public URI asUri() {
        return URI.create(ACTIVITY_STREAMS_BASEURI + this.toString());
    }

    /**
     * Expand a string in compact form to a full activityStream URI, if possible.
     *
     * @param term String which may represent an ActivityStream term (e.g. "Update"), or not.
     * @return Corresponding activityStream URI, or the original string if it does not map to an AS term.
     */
    public static String expand(final String term) {
        return ofNullable(terms.get(term)).orElse(term);
    }
}
