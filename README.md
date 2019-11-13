Fcrepo Component
================

The **fcrepo:** component provides access to an external
[Fedora4](http://fcrepo.org) Object
[API](https://wiki.duraspace.org/display/FEDORA5x/RESTful+HTTP+API+-+Containers)
for use with [Apache Camel](https://camel.apache.org).

[![Build Status](https://travis-ci.com/fcrepo4-exts/fcrepo-camel.svg?branch=master)](https://travis-ci.com/fcrepo4-exts/fcrepo-camel)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.fcrepo.camel/fcrepo-camel/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.fcrepo.camel/fcrepo-camel/)

URI format
----------

    fcrepo:hostname[:port][/resourceUrl][?options]

By default this endpoint connects to fedora repositories on port 80.


FcrepoEndpoint options
-----------------------

| Name         |  Default Value | Description |
| ------------ | -------------- | ----------- |
| `contentType`       | `null`         | Set the `Content-Type` header |
| `accept` | `null` | Set the `Accept` header for content negotiation |
| `fixity` | `false` | Whether GET requests should check the fixity of non-RDF content |
| `metadata` | `true`  | Whether GET requests should retrieve RDF descriptions of non-RDF content  |
| `preferOmit` | `null` | If set, this populates the `Prefer:` HTTP header with omitted values. For single values, the standard [LDP values](http://www.w3.org/TR/ldp/#prefer-parameters) and the corresponding [Fcrepo extensions](https://wiki.duraspace.org/display/FEDORA5x/RESTful+HTTP+API+-+Containers#RESTfulHTTPAPI-Containers-GETRetrievethecontentoftheresource) can be provided in short form (without the namespace). |
| `preferInclude` | `null` | If set, this populates the `Prefer:` HTTP header with included values. For single values, the standard [LDP values](http://www.w3.org/TR/ldp/#prefer-parameters) and the corresponding [Fcrepo extensions](https://wiki.duraspace.org/display/FEDORA5x/RESTful+HTTP+API+-+Containers#RESTfulHTTPAPI-Containers-GETRetrievethecontentoftheresource) can be provided in short form (without the namespace). |
| `throwExceptionOnFailure` | `true` | Option to disable throwing the HttpOperationFailedException in case of failed responses from the remote server. This allows you to get all responses regardless of the HTTP status code. |

Examples
--------

A simple example for sending messages to an external Solr service:

    XPathBuilder xpath = new XPathBuilder("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/indexing#Indexable']");
    xpath.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

    from("activemq:topic:fedora").routeId("triplestore-router")
      .process(new EventProcessor())
      .choice()
        .when(header(FCREPO_EVENT_TYPE).contains("https://www.w3.org/ns/activitystreams#Delete"))
          .to("direct:remove")
        .when(header(FCREPO_RESOURCE_TYPE).contains("http://fedora.info/definitions/v4/indexing#Indexable"))
          .to("direct:update")
        .otherwise()
          .to("direct:remove");

    from("direct:update").routeId("triplestore-updater")
      .to("fcrepo:localhost:8080/fcrepo/rest?accept=application/n-triples")
      .process(new SparqlUpdateProcessor())
      .to("http4:triplestore-host:8080/dataset/update");

    from("direct:remove").routeId("triplestore-remover")
      .process(new SparqlDeleteProcessor())
      .to("http4:triplestore-host:8080/dataset/update");

Or, using the Spring DSL:

    <bean id="updateProcessor" class="org.fcrepo.camel.processor.SparqlUpdateProcessor"/>
    <bean id="deleteProcessor" class="org.fcrepo.camel.processor.SparqlDeleteProcessor"/>
    <bean id="eventProcessor" class="org.fcrepo.camel.processor.EventProcessor"/>

    <camelContext xmlns="http://camel.apache.org/schema/blueprint">
      <route id="triplestore-router">
        <from uri="activemq:topic:fedora"/>
        <process ref="eventProcessor"/>
        <choice>
          <when>
            <simple>${header[CamelFcrepoEventType].contains("https://www.w3.org/ns/activitystreams#Delete")}</simple>
            <to uri="direct:remove"/>
          </when>
          <when>
            <simple>${header[CamelFcrepoResourceType].contains("http://fedora.info/definitions/v4/indexing#Indexable")}</simple>
            <to uri="direct:update"/>
          </when>
          <otherwise>
            <to uri="direct:remove"/>
          </otherwise>
        </choice>
      </route>

      <route id="triplestore-updater">
        <from uri="direct:update"/>
        <to uri="fcrepo:localhost:8080/rest?accept=application/n-triples"/>
        <process ref="updateProcessor"/>
        <to uri="http4:triplestore-host:8080/dataset/update"/>
      </route>

      <route id="triplestore-remover">
        <from uri="direct:remove"/>
        <process ref="deleteProcessor"/>
        <to uri="http4:triplestore-host:8080/dataset/update"/>
      </route>
    </camelContext>

**Please Note**: as in this example, if you plan to handle `ResourceDeletion` events, you should expect any requests
back to fedora (via `fcrepo:`) to respond with a `410 Gone` error, so it is recommended that you route your
messages accordingly.

Setting basic authentication
----------------------------

| Name         | Default Value | Description |
| ------------ | ------------- | ----------- |
| `authUsername` | `null`          | Username for authentication |
| `authPassword` | `null`          | Password for authentication |
| `authHost`     | `null`          | The host name for authentication |

Configuring the fcrepo component
--------------------------------

In addition to configuring the `fcrepo` component with URI options on each request, it is also
sometimes convenient to set up component-wide configurations. This can be done via Spring
(or Blueprint), like so:

    <bean id="fcrepo" class="org.fcrepo.camel.FcrepoComponent">
      <property name="authUsername" value="${fcrepo.authUsername}"/>
      <property name="authPassword" value="${fcrepo.authPassword}"/>
      <property name="authHost" value="${fcrepo.authHost}"/>
      <property name="baseUrl" value="${fcrepo.baseUrl}"/>
    </bean>


Message headers
---------------

| Name     | Type   | Description |
| -------- | ------ | ----------- |
| `Exchange.HTTP_METHOD` | `String` | The HTTP method to use |
| `Exchange.CONTENT_TYPE` | `String` | The ContentType of the resource. This sets the `Content-Type` header, but this value can be overridden directly on the endpoint. |
| `Exchange.ACCEPT_CONTENT_TYPE` | `String` | This sets the `Accept` header, but this value can be overridden directly on the endpoint. |
| `FcrepoHeaders.FCREPO_AGENT` | `List` | A collection of agents that generated this event. |
| `FcrepoHeaders.FCREPO_BASE_URL`      | `String` | The base url used for accessing Fedora. **Note:** users are encouraged to use the `FCREPO_URI` header instead. |
| `FcrepoHeaders.FCREPO_DATE_TIME` | `String` | A datetime string formatted in ISO 8601 corresponding to the instant of the event. |
| `FcrepoHeaders.FCREPO_EVENT_ID` | `String` | A unique identifier for this event. |
| `FcrepoHeaders.FCREPO_EVENT_TYPE` | `List` | A set of URIs corresponding to the event type. |
| `FcrepoHeaders.FCREPO_IDENTIFIER`    | `String` | The resource path, appended to the endpoint uri. **Note:** users are encouraged to use the `FCREPO_URI` header instead. |
| `FcrepoHeaders.FCREPO_NAMED_GRAPH`   | `String` | Sets a URI for a named graph when used with the `processor.Sparql*` classes. This may be useful when storing data in an external triplestore. |
| `FcrepoHeaders.FCREPO_PREFER`  | `String` | This sets the `Prefer` header on a repository request. The full header value should be declared here, and it will override any value set directly on an endpoint. |
| `FcrepoHeaders.FCREPO_RESOURCE_TYPE` | `List` | A set of URIs corresponding to the resource type. |
| `FcrepoHeaders.FCREPO_URI`    | `String` | The full resource URI. Note: if this is defined, it takes precedence over any values set with `FCREPO_IDENTIFIER` and `FCREPO_BASE_URL`. |

If these headers are used with the Spring DSL or with the Simple language, the header values can be used directly with the following values:

| Name    | Value |
| ------- | ----- |
| `FcrepoHeaders.FCREPO_AGENT` | `CamelFcrepoAgent` |
| `FcrepoHeaders.FCREPO_BASE_URL` | `CamelFcrepoBaseUrl` |
| `FcrepoHeaders.FCREPO_DATE_TIME` | `CamelFcrepoDateTime` |
| `FcrepoHeaders.FCREPO_EVENT_ID` | `CamelFcrepoEventId` |
| `FcrepoHeaders.FCREPO_EVENT_TYPE` | `CamelFcrepoEventType` |
| `FcrepoHeaders.FCREPO_IDENTIFIER` | `CamelFcrepoIdentifier` |
| `FcrepoHeaders.FCREPO_NAMED_GRAPH` | `CamelFcrepoNamedGraph` |
| `FcrepoHeaders.FCREPO_PREFER` | `CamelFcrepoPrefer` |
| `FcrepoHeaders.FCREPO_RESOURCE_TYPE` | `CamelFcrepoResourceType` |
| `FcrepoHeaders.FCREPO_URI` | `CamelFcrepoUri` |

These headers can be removed as a group like this in the Java DSL: `removeHeaders("CamelFcrepo*")`

Message body
------------

Camel will store the HTTP response from the Fedora4 server on the
OUT body. All headers from the IN message will be copied to the OUT
message, so headers are preserved during routing. Additionally,
Camel will add the HTTP response headers to the OUT message headers.


Response code
-------------

Camel will handle the HTTP response code in the following ways:

* Response code in the range 100..299 is a success.
* Response code in the range 300..399 is a redirection and will throw a `FcrepoOperationFailedException` with the relevant information.
* Response code is 400+ is regarded as an external server error and will throw an `FcrepoOperationFailedException` with the relevant information.

Resource path
-------------

The path for `fcrepo` resources can be set in several different ways. If the
`CamelFcrepoUri` header is set, that will be used as the full path of the
Fedora resource. If that header is not set, the value of the `CamelFcrepoIdentifier`
header will be appended to either the endpoint URI or the value of the
`CamelFcrepoBaseUrl` header (the `CamelFcrepoBaseUrl` header takes precedence, if defined).

It is generally a good idea to set the endpoint URI to fedora's REST API
endpoint and then use the appropriate header to set the path of the intended
resource.

For example, each of these routes will request the resource at
`http://localhost:8080/rest/a/b/c/abcdef`:

    from("direct:start")
      .setHeader("CamelFcrepoIdentifier", "/a/b/c/abcdef")
      .to("fcrepo:localhost:8080/rest");

    // CamelFcrepoUri and CamelFcrepoIdentifier headers are undefined
    from("direct:start")
      .to("fcrepo:localhost:8080/rest/a/b/c/abcdef");

    // and CamelFcrepoIdentifier is not defined
    from("direct:start")
      .setHeader("CamelFcrepoUri", "http://localhost:8080/rest/a/b/c/abcdef")
      .to("fcrepo:localhost:8080/rest")


FcrepoOperationFailedException
------------------------------

This exception contains the following information:

* The requested URL
* The HTTP status code
* The HTTP status line (text of the status code)


How to set the HTTP method
--------------------------

The endpoint will always use the `GET` method unless explicitly set
in the `Exchange.HTTP_METHOD` header. Other methods, such as `PUT`,
`PATCH`, `POST`, and `DELETE` are available and will be passed through
to the Fedora server. Here is an example:

    from("direct:start")
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .to("fcrepo:localhost:8080/fcrepo4/rest")
        .to("mock:results");

And the equivalent Spring sample:

    <camelContext xmlns="http://activemq.apache.org/camel/schema/spring">
      <route>
        <from uri="direct:start"/>
        <setHeader headerName="Exchange.HTTP_METHOD">
            <constant>POST</constant>
        </setHeader>
        <to uri="fcrepo:localhost:8080/fcrepo4/rest"/>
        <to uri="mock:results"/>
      </route>
    </camelContext>


Getting the response code
-------------------------

You can get the HTTP response code from the `fcrepo` component by getting
the value from the Out message header with `Exchange.HTTP_RESPONSE_CODE`.


Transactions
------------

The `fcrepo-camel` component follows the [Transactional Client](http://camel.apache.org/transactional-client.html)
pattern when using transactions with a Fedora Repository. A route can begin using transactions by simply
identifying the route as `transacted()` like so:

    from("direct:foo")
      .transacted()
      .to("fcrepo:localhost:8080/rest")
      .process(new MyProcessor())
      .to("fcrepo:localhost:8080/rest")
      .process(new MyOtherProcessor())
      .to("fcrepo:localhost:8080/rest");

A single transaction can span multiple routes so long as the transaction is run within a single thread.
That is, if the `direct` endpoint is used, a transacted workflow may be divided among multiple routes
(do not use `seda` or `vm`).

In order to enable a transactional client, a `TransactionManager` must be added to the Spring configuration:
for this to work, the built-in `FcrepoTransactionManager` needs to know the `baseUrl` of the underlying
repository. Authentication information, if necessary, can also be added in the bean configuration.

    <bean id="fcrepoTxManager" class="org.fcrepo.camel.FcrepoTransactionManager">
      <property name="baseUrl" value="http://localhost:8080/rest"/>
    </bean>

    <bean id="fcrepo" class="org.fcrepo.camel.FcrepoComponent">
      <property name="transactionManager" ref="fcrepoTxManager"/>
    </bean>

Like with other transactional clients, if an error is encountered anywhere in the route, all transacted
operations will be rolled back.


Building the component
----------------------

The `fcrepo-camel` compnent can be built with Maven:

    mvn clean install


Fcrepo messaging
----------------

Fedora4 uses an internal [ActiveMQ](https://activemq.apache.org) message
broker to send messages about any updates to the repository content. By
default, all events are published to a `topic` called `fedora` on the
local broker. Each message contains an empty body and up to seven different
header values:

  * `org.fcrepo.jms.baseURL`
  * `org.fcrepo.jms.eventType`
  * `org.fcrepo.jms.identifier`
  * `org.fcrepo.jms.resourceType`
  * `org.fcrepo.jms.timestamp`
  * `org.fcrepo.jms.user`
  * `org.fcrepo.jms.userAgent`

Both `eventType` and `resourceType` are comma-delimited lists of values.
The `eventType` uses the following terms from the [ActivityStreams 2.0](https://www.w3.org/ns/activitystreams) Activities vocabulary:

  * `https://www.w3.org/ns/activitystreams#Create`
  * `https://www.w3.org/ns/activitystreams#Delete`
  * `https://www.w3.org/ns/activitystreams#Update`
  * `https://www.w3.org/ns/activitystreams#Move`
  * `https://www.w3.org/ns/activitystreams#Follow`

The `resourceType` values will include any `rdf:type` values for the resource in question.

The message body will be formatted as JSON-LD, and users are encouraged to rely on the data found there rather than in the JMS-specific headers.

Typically, an application will unmarshal the payload from a message broker like so:

    from("activemq:queue:fedora")
        .unmarshal().json(Jackson)

Additionally, the `EventProcessor` will populate Message headers with the data in this message. The recommended pattern to use is:

    from("activemq:queue:fedora")
        .unmarshal().json(Jackson)
        .process(new EventProcessor())

If you don't need further access to the message body, it is possible to omit the `unmarshal().json(Jackson)` step.

Examples and more information
-----------------------------

For projects that use `fcrepo-camel`, please refer to the [`fcrepo-camel-toolbox`](https://github.com/fcrepo4-exts/fcrepo-camel-toolbox) project.

Furthermore, additional information about designing and deploying **fcrepo**-based message routes along
with configuration options for Fedora's ActiveMQ broker can be found on the
[fedora project wiki](https://wiki.duraspace.org/display/FEDORA5x/Setup+Camel+Message+Integrations).

Maintainers
-----------

Current maintainers:

* [Daniel Lamb](https://github.com/dannylamb)
* [Bethany Seeger](https://github.com/bseeger)
