/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

/**
 * @author acoburn
 */
public final class FcrepoConstants {

    public static final String FIXITY = "/fcr:fixity";

    public static final String TRANSACTION = "/fcr:tx";

    public static final String COMMIT = TRANSACTION + "/fcr:commit";

    public static final String ROLLBACK = TRANSACTION + "/fcr:rollback";

    // prevent instantiation
    private FcrepoConstants() {
    }
}
