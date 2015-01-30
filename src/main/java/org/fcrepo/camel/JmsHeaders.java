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
public final class JmsHeaders {

    public static final String PREFIX = "org.fcrepo.jms.";

    public static final String IDENTIFIER = PREFIX + "identifier";

    public static final String EVENT_TYPE = PREFIX + "eventType";

    public static final String PROPERTIES = PREFIX + "properties";

    public static final String TIMESTAMP = PREFIX + "timestamp";

    public static final String BASE_URL = PREFIX + "baseURL";

    private JmsHeaders() {
        // prevent instantiation
    }

}

