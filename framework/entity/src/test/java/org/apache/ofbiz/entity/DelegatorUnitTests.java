/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.ofbiz.base.util.Debug;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DelegatorUnitTests {
    private boolean logErrorOn;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void initialize() {
        System.setProperty("ofbiz.home", System.getProperty("user.dir"));
        System.setProperty("derby.system.home", "./runtime/data/derby");
        logErrorOn = Debug.isOn(Debug.ERROR); // save the current setting (to be restored after the tests)
        Debug.set(Debug.ERROR, false); // disable error logging
    }

    @After
    public void restore() {
        Debug.set(Debug.ERROR, logErrorOn); // restore the error log setting
    }

    @Test
    public void delegatorCreationUsingConstructorFailsIfConfigurationIsMissing() throws GenericEntityException {
        expectedException.expect(GenericEntityException.class);
        expectedException.expectMessage("No configuration found for delegator");
        new GenericDelegator("delegatorNameWithNoConfiguration");
    }

    @Test
    public void delegatorCreationUsingConstructor() throws GenericEntityException {
        Delegator delegator = new GenericDelegator("default");
        assertNotNull(delegator);
        assertEquals(delegator.getOriginalDelegatorName(), "default");
        assertEquals(delegator.getDelegatorBaseName(), "default");
        assertEquals(delegator.getDelegatorName(), "default");
    }

    @Test
    public void delegatorCreationUsingFactoryGetInstance() {
        DelegatorFactory df = new DelegatorFactoryImpl();
        assertNotNull(df);
        Delegator delegator = df.getInstance("default");
        assertNotNull(delegator);
        assertTrue(delegator instanceof GenericDelegator);
        assertEquals(delegator.getOriginalDelegatorName(), "default");
        assertEquals(delegator.getDelegatorBaseName(), "default");
        assertEquals(delegator.getDelegatorName(), "default");
        Delegator delegatorWithSameName = df.getInstance("default");
        assertNotSame(delegator, delegatorWithSameName);
    }

    @Test
    public void delegatorCreationUsingFactoryGetDelegator() {
        Delegator delegator = DelegatorFactory.getDelegator("default");
        assertNotNull(delegator);
        assertTrue(delegator instanceof GenericDelegator);
        assertEquals(delegator.getOriginalDelegatorName(), "default");
        assertEquals(delegator.getDelegatorBaseName(), "default");
        assertEquals(delegator.getDelegatorName(), "default");
        Delegator delegatorWithSameName = DelegatorFactory.getDelegator("default");
        assertSame(delegator, delegatorWithSameName);
        Delegator delegatorWithNullName = DelegatorFactory.getDelegator(null);
        assertSame(delegator, delegatorWithNullName);
    }

    @Test
    public void delegatorCreationUsingFactoryReturnsNullIfConfigurationIsMissing() throws GenericEntityException {
        // TODO: the framework code should throw the exception instead of returning a null reference
        DelegatorFactory df = new DelegatorFactoryImpl();
        Delegator delegator = df.getInstance("delegatorNameWithNoConfiguration");
        assertNull(delegator);
    }

}
