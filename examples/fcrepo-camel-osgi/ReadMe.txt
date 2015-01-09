Fcrepo Camel OSGi Project
=========================

This is a sample project that listens to fedora's JMS message stream,
passing updates to an external triplestore and solr index. The component
also exposes a REST endpoint for re-indexing the entire collection.

To build this project use

    mvn install

Once built, this can be deployed in an OSGi container, such as
[Karaf](https://karaf.apache.org). If using Karaf, add the fcrepo-camel
jarfile to $KARAF_HOME/deploy. Then, add this component to the same deploy
directory.

Properties can be defined and updated in an org.fcrepo.camel.examples.osgi.cfg
file.

For more help see the Apache Camel documentation

    http://camel.apache.org/

