/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel;

/**
 * @author acoburn
 */
public final class FcrepoHeaders {

    public static final String FCREPO_BASE_URL = "CamelFcrepoBaseUrl";

    public static final String FCREPO_IDENTIFIER = "CamelFcrepoIdentifier";

    public static final String FCREPO_TRANSFORM = "CamelFcrepoTransform";

    public static final String FCREPO_PREFER = "CamelFcrepoPrefer";

    public static final String FCREPO_NAMED_GRAPH = "CamelFcrepoNamedGraph";

    public static final String FCREPO_LOCATION = "CamelFcrepoLocation";

    public static final String FCREPO_TRANSACTION = "CamelFcrepoTransaction";

    private FcrepoHeaders() {
        // prevent instantiation
    }
}
