/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.camel.examples.scala

import org.apache.camel.Exchange
import org.apache.camel.builder.xml.XPathBuilder
import org.apache.camel.scala.dsl.builder.RouteBuilder
import org.apache.activemq.camel.component.ActiveMQComponent

/**
 * A Camel Router using the Scala DSL
 *
 * @author Aaron Coburn
 * @since  Nov 8, 2014
 */
class SolrRouteBuilder extends RouteBuilder {

  val xpath = new XPathBuilder("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/indexing#Indexable']")
  xpath.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")

  // a route using Scala blocks
  "activemq:topic:fedora" ==> {
    to("fcrepo:{{fcrepo.baseUrl}}")
    filter(xpath) {
      to("fcrepo:{{fcrepo.baseUrl}}?transform={{fcrepo.defaultTransform}}")
      setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      to("http4:/{{solr.baseUrl}}/update")
    }
  }
}
