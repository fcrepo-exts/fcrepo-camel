/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static org.fcrepo.camel.processor.ProcessorUtils.tokenizePropertyPlaceholder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

/**
 * @author acoburn
 */
public class ProcessorUtilsTest extends CamelTestSupport {

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
         final Properties props = new Properties();
         props.put("test.prop1", "one,two,three");
         props.put("test.prop2", "    four   ,   five\n,six ");
         props.put("test.prop3", "seven");
         props.put("test.prop4", "eight,\n\t\tnine,\n   ten");
         props.put("test.prop5", "");
         props.put("test.prop6", "    ");
         return props;
    }

    @Test
    public void testPropertyTokenizerSimple() {
        final List<String> list1 = tokenizePropertyPlaceholder(context, "{{test.prop1}}", "\\s*,\\s*");
        assertEquals(3, list1.size());
        assertTrue(list1.contains("one"));
        assertTrue(list1.contains("two"));
        assertTrue(list1.contains("three"));
    }

    @Test
    public void testPropertyTokenizerWhitespace1() {
        final List<String> list2 = tokenizePropertyPlaceholder(context, "{{test.prop2}}", "\\s*,\\s*");
        assertEquals(3, list2.size());
        assertTrue(list2.contains("four"));
        assertTrue(list2.contains("five"));
        assertTrue(list2.contains("six"));
    }

    @Test
    public void testPropertyTokenizerSingleton() {
        final List<String> list3 = tokenizePropertyPlaceholder(context, "{{test.prop3}}", "\\s*,\\s*");
        assertEquals(1, list3.size());
        assertTrue(list3.contains("seven"));
    }

    @Test
    public void testPropertyTokenizerWhitespace2() {
        final List<String> list4 = tokenizePropertyPlaceholder(context, "{{test.prop4}}", "\\s*,\\s*");
        assertEquals(3, list4.size());
        assertTrue(list4.contains("eight"));
        assertTrue(list4.contains("nine"));
        assertTrue(list4.contains("ten"));
    }

    @Test
    public void testPropertyTokenizerEmpty1() {
        final List<String> list5 = tokenizePropertyPlaceholder(context, "{{test.prop5}}", "\\s*,\\s*");
        assertEquals(0, list5.size());
    }

    @Test
    public void testPropertyTokenizerEmpty2() {
        final List<String> list6 = tokenizePropertyPlaceholder(context, "{{test.prop6}}", "\\s*,\\s*");
        assertEquals(0, list6.size());
    }

    @Test
    public void testPropertyTokenizerNotDefined() {
        final List<String> list7 = tokenizePropertyPlaceholder(context, "{{test.prop7}}", "\\s*,\\s*");
        assertEquals(0, list7.size());
    }
}
