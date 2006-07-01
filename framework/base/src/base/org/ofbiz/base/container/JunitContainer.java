/*
 * $Id: JunitContainer.java 7551 2006-05-10 00:08:10Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.base.container;

import java.util.Enumeration;
import java.util.Iterator;

import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;


/**
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.1
 */
public class JunitContainer implements Container {

    public static final String module = JunitContainer.class.getName();
    protected TestResult results = null;
    protected String configFile = null;

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) {
        this.configFile = configFile;
    }

    public boolean start() throws ContainerException {
        ContainerConfig.Container jc = ContainerConfig.getContainer("junit-container", configFile);

        // get the tests to run
        Iterator ti = jc.properties.values().iterator();
        if (ti == null) {
            Debug.log("No tests to load", module);
            return true;
        }

        // load the tests into the suite
        TestSuite suite = new TestSuite();
        while (ti.hasNext()) {
            ContainerConfig.Container.Property prop = (ContainerConfig.Container.Property) ti.next();
            Class clz = null;
            try {
                clz = ObjectType.loadClass(prop.value);
                suite.addTestSuite(clz);
            } catch (Exception e) {
                Debug.logError(e, "Unable to load test suite class : " + prop.value, module);
            }
        }

        // holder for the results
        results = new TestResult();

        // run the tests
        suite.run(results);

        // dispay the results
        Debug.log("[JUNIT] Pass: " + results.wasSuccessful() + " | # Tests: " + results.runCount() + " | # Failed: " +
                results.failureCount() + " # Errors: " + results.errorCount(), module);
        if (Debug.infoOn()) {
            Debug.log("[JUNIT] ----------------------------- ERRORS ----------------------------- [JUNIT]", module);
            Enumeration err = results.errors();
            if (!err.hasMoreElements()) {
                Debug.log("None");
            } else {
                while (err.hasMoreElements()) {
                    Debug.log("--> " + err.nextElement(), module);
                }
            }
            Debug.log("[JUNIT] ------------------------------------------------------------------ [JUNIT]", module);
            Debug.log("[JUNIT] ---------------------------- FAILURES ---------------------------- [JUNIT]", module);
            Enumeration fail = results.failures();
            if (!fail.hasMoreElements()) {
                Debug.log("None");
            } else {
                while (fail.hasMoreElements()) {
                    Debug.log("--> " + fail.nextElement(), module);
                }
            }
            Debug.log("[JUNIT] ------------------------------------------------------------------ [JUNIT]", module);
        }

        return true;
    }

    public void stop() throws ContainerException {
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            throw new ContainerException(e);
        }
    }
}
