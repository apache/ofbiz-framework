/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
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
