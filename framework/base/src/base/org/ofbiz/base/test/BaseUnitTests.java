/*
 * $Id: BaseUnitTests.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 */
package org.ofbiz.base.test;

import junit.framework.TestCase;

import org.ofbiz.base.util.Debug;

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      May 4, 2004
 */
public class BaseUnitTests extends TestCase {

    public BaseUnitTests(String name) {
        super(name);
    }

    public void testDebug() {
        Debug.set(Debug.VERBOSE, true);
        assertTrue(Debug.verboseOn());

        Debug.set(Debug.VERBOSE, false);
        assertTrue(!Debug.verboseOn());

        Debug.set(Debug.INFO, true);
        assertTrue(Debug.infoOn());
    }
}
