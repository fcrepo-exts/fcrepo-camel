Fcrepo Component
================

The **fcrepo:** component provides access to an external
[Fedora4](http://fcrepo.org) Object
[API](https://wiki.duraspace.org/display/FEDORA4x/RESTful+HTTP+API+-+Containers)
for use with [Apache Camel](https://camel.apache.org).

[![Build Status](https://travis-ci.org/fcrepo4-exts/fcrepo-camel.png?branch=master)](https://travis-ci.org/fcrepo4-exts/fcrepo-camel)
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
| `transform` | `null` | **Deprecated** If set, this defines the transform used for the given object. This should be used in the context of GET or POST. For GET requests, the value should be the name of the transform (e.g. `default`). For POST requests, the value can simply be `true`. Using this causes the `Accept` header to be set as `application/json`. |
| `preferOmit` | `null` | If set, this populates the `Prefer:` HTTP header with omitted values. For single values, the standard [LDP values](http://www.w3.org/TR/ldp/#prefer-parameters) and the corresponding [Fcrepo extensions](https://wiki.duraspace.org/display/FEDORA4x/RESTful+HTTP+API+-+Containers#RESTfulHTTPAPI-Containers-GETRetrievethecontentoftheresource) can be provided in short form (without the namespace). |
| `preferInclude` | `null` | If set, this populates the `Prefer:` HTTP header with included values. For single values, the standard [LDP values](http://www.w3.org/TR/ldp/#prefer-parameters) and the corresponding [Fcrepo extensions](https://wiki.duraspace.org/display/FEDORA4x/RESTful+HTTP+API+-+Containers#RESTfulHTTPAPI-Containers-GETRetrievethecontentoftheresource) can be provided in short form (without the namespace). |
| `throwExceptionOnFailure` | `true` | Option to disable throwing the HttpOperationFailedException in case of failed responses from the remote server. This allows you to get all responses regardless of the HTTP status code. |

Examples
--------

A simple example for sending messages to an external Solr service:

    XPathBuilder xpath = new XPathBuilder("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/indexing#Indexable']");
    xpath.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

    from("activemq:topic:fedora").routeId("triplestore-router")
      .setHeader(FCREPO_IDENTIFIER).header("org.fcrepo.jms.identifier")
      .setHeader(FCREPO_BASE_URL).header("org.fcrepo.jms.baseUrl")
      .choice()
        .when(simple("${header[org.fcrepo.jms.eventType]} == \"http://fedora.info/definitions/v4/event#ResourceDeletion\"))
          .to("direct:remove")
        .when(simple("${header[org.fcrepo.jms.resourceType]} contains \"http://fedora.info/definitions/v4/indexing#Indexable\""))
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

    <camelContext xmlns="http://camel.apache.org/schema/blueprint">
      <route id="triplestore-router">
        <from uri="activemq:topic:fedora"/>
        <setHeader headerName="CamelFcrepoIdentifier">
          <simple>${header[org.fcrepo.jms.identifier]}</simple>
        </setHeader>
        <setHeader headerName="CamelFcrepoBaseUrl">
          <simple>${header[org.fcrepo.jms.baseUrl]}</simple>
        </setHeader>
        <choice>
          <when>
            <simple>${header[org.fcrepo.jms.eventType]} == 'http://fedora.info/definitions/v4/event#ResourceDeletion'</simple>
            <to uri="direct:remove"/>
          </when>
          <when>
            <simple>${header[org.fcrepo.jms.resourceType]} contains "http://fedora.info/definitions/v4/indexing#Indexable"</simple>
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
| `FcrepoHeaders.FCREPO_PREFER`  | `String` | This sets the `Prefer` header on a repository request. The full header value should be declared here, and it will override any value set directly on an endpoint. |
| `FcrepoHeaders.FCREPO_IDENTIFIER`    | `String` | The resource path, appended to the endpoint uri. |
| `FcrepoHeaders.FCREPO_BASE_URL`      | `String` | The base url used for accessing Fedora. |
| `FcrepoHeaders.FCREPO_TRANSFORM`     | `String` | **Deprecated** The named `fcr:transform` method to use. This value overrides any value set explicitly on the endpoint. |
| `FcrepoHeaders.FCREPO_NAMED_GRAPH`   | `String` | Sets a URI for a named graph when used with the `processor.Sparql*` classes. This may be useful when storing data in an external triplestore. |

The `fcrepo` component will also accept message headers produced directly by fedora, particularly the `org.fcrepo.jms.identifier` header. It will use that header only when `CamelFcrepoIdentifier` is not defined.

If these headers are used with the Spring DSL or with the Simple language, the header values can be used directly with the following values:

| Name    | Value |
| ------- | ----- |
| `FcrepoHeaders.FCREPO_BASE_URL` | `CamelFcrepoBaseUrl` |
| `FcrepoHeaders.FCREPO_IDENTIFIER` | `CamelFcrepoIdentifier` |
| `FcrepoHeaders.FCREPO_TRANSFORM` | `CamelFcrepoTransform` (**Deprecated**) |
| `FcrepoHeaders.FCREPO_PREFER` | `CamelFcrepoPrefer` |
| `FcrepoHeaders.FCREPO_NAMED_GRAPH` | `CamelFcrepoNamedGraph` |

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
`CamelFcrepoIdentifier` header is set, that value will be appended to the endpoint
URI. If the `CamelFcrepoIdentifier` is not set, the path will be populated by the
`org.fcrepo.jms.identifier` header and appended to the endpoint URI. If neither
header is set, only the endpoint URI will be used.

It is generally a good idea to set the endpoint URI to fedora's REST API
endpoint and then use the appropriate header to set the path of the intended
resource.

For example, each of these routes will request the resource at
`http://localhost:8080/rest/a/b/c/abcdef`:

    from("direct:start")
      .setHeader("CamelFcrepoIdentifier", "/a/b/c/abcdef")
      .to("fcrepo:localhost:8080/rest");

    // org.fcrepo.jms.identifier and CamelFcrepoIdentifier headers are undefined
    from("direct:start")
      .to("fcrepo:localhost:8080/rest/a/b/c/abcdef");

    // org.fcrepo.jms.identifier is set as '/a/b/c/abcdef'
    // and CamelFcrepoIdentifier is not defined
    from("direct:start")
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
The `eventType` values follow the [Fedora Event Type ontology](http://fedora.info/definitions/v4/event#):

  * `http://fedora.info/definitions/v4/event#ResourceCreation`
  * `http://fedora.info/definitions/v4/event#ResourceDeletion`
  * `http://fedora.info/definitions/v4/event#ResourceModification`
  * `http://fedora.info/definitions/v4/event#ResourceRelocation`

The `resourceType` values will include any `rdf:type` values for the resource in question.

Examples and more information
-----------------------------

There are several example projects in the `examples` directory of this distribution.

Furthermore, additional information about designing and deploying **fcrepo**-based message routes along
with configuration options for Fedora's ActiveMQ broker can be found on the
[fedora project wiki](https://wiki.duraspace.org/display/FEDORA4x/Setup+Camel+Message+Integrations).

Maintainers
-----------

Current maintainers:

* [Aaron Coburn](https://github.com/acoburn)
* [Daniel Lamb](https://github.com/dannylamb)
