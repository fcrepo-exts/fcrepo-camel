/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel.integration;

import static org.osgi.framework.Bundle.ACTIVE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.util.PathUtils.getBaseDir;

import java.io.File;

import org.apache.camel.test.karaf.AbstractFeatureTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.slf4j.Logger;

/**
 * @author Aaron Coburn
 * @since February 8, 2016
 */
@RunWith(PaxExam.class)
public class KarafIT extends AbstractFeatureTest {

    private static Logger LOG = getLogger(KarafIT.class);

    @Configuration
    public Option[] config() {
        final ConfigurationManager cm = new ConfigurationManager();
        final String artifactName = cm.getProperty("project.artifactId") + "-" + cm.getProperty("project.version");
        final String fcrepoCamelBundle = "file:" + getBaseDir() + "/target/" + artifactName + ".jar";
        final String commonsCodecVersion = cm.getProperty("commons.codec.version");
        final String commonsCsvVersion = cm.getProperty("commons.csv.version");
        final String dexxVersion = cm.getProperty("dexx.version");
        final String httpclientVersion = cm.getProperty("httpclient.version");
        final String httpcoreVersion = cm.getProperty("httpcore.version");
        final String jsonldVersion = cm.getProperty("jsonld.version");
        final String thriftVersion = cm.getProperty("thrift.version");
        final String rmiRegistryPort = cm.getProperty("karaf.rmiRegistry.port");
        final String rmiServerPort = cm.getProperty("karaf.rmiServer.port");
        final String sshPort = cm.getProperty("karaf.ssh.port");
        return new Option[] {
            karafDistributionConfiguration()
                .frameworkUrl(maven().groupId("org.apache.karaf").artifactId("apache-karaf")
                        .versionAsInProject().type("zip"))
                .unpackDirectory(new File("target", "exam"))
                .useDeployFolder(false),
            logLevel(LogLevel.WARN),
            keepRuntimeFolder(),
            configureConsole().ignoreLocalConsole(),
            CoreOptions.systemProperty("fcrepo-camel-bundle").value(fcrepoCamelBundle),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
            features(maven().groupId("org.apache.karaf.features").artifactId("standard")
                        .versionAsInProject().classifier("features").type("xml"), "scr"),
            features(getCamelKarafFeatureUrl(), "camel-blueprint", "camel-spring", "camel-jackson"),
            mavenBundle().groupId("org.apache.camel").artifactId("camel-test-karaf").versionAsInProject(),
            mavenBundle().groupId("commons-codec").artifactId("commons-codec").version(commonsCodecVersion),
            mavenBundle().groupId("org.apache.commons").artifactId("commons-csv").version(commonsCsvVersion),
            mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient-osgi").versionAsInProject(),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore-osgi").versionAsInProject(),
            mavenBundle().groupId("org.apache.jena").artifactId("jena-osgi").versionAsInProject(),
            mavenBundle().groupId("com.github.jsonld-java").artifactId("jsonld-java").version(jsonldVersion),
            mavenBundle().groupId("org.apache.thrift").artifactId("libthrift").version(thriftVersion),
            mavenBundle().groupId("org.fcrepo.client").artifactId("fcrepo-java-client").versionAsInProject(),
            mavenBundle().groupId("com.github.andrewoma.dexx").artifactId("collection").version(dexxVersion),
            bundle(fcrepoCamelBundle).start()
       };
    }

    @Test
    public void testInstallation() throws Exception {
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-core")));
        assertNotNull(bundleContext);
        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("fcrepo-camel-bundle")).getState());
    }
}
