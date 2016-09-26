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
package org.apache.ofbiz.entity.util;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EntitySaxReaderTests {
    @Test
    public void constructorWithDefaultTimeout() {
        Delegator delegator = mock(Delegator.class);
        EntitySaxReader esr = new EntitySaxReader(delegator); // create a reader with default tx timeout
        verify(delegator).cloneDelegator();
        verifyNoMoreInteractions(delegator);
        assertEquals(EntitySaxReader.DEFAULT_TX_TIMEOUT, esr.getTransactionTimeout());
    }

    @Test
    public void constructorWithTimeout() {
        Delegator delegator = mock(Delegator.class);
        EntitySaxReader esr = new EntitySaxReader(delegator, 14400); // create a reader with a non default tx timeout
        verify(delegator).cloneDelegator();
        verifyNoMoreInteractions(delegator);
        assertEquals(14400, esr.getTransactionTimeout());
    }

    @Test
    public void parse() throws Exception {
        Delegator delegator = mock(Delegator.class);
        Delegator clonedDelegator = mock(Delegator.class);
        GenericValue genericValue = mock(GenericValue.class);
        ModelEntity modelEntity = mock(ModelEntity.class);
        when(delegator.cloneDelegator()).thenReturn(clonedDelegator);
        when(clonedDelegator.makeValue("EntityName")).thenReturn(genericValue);
        when(genericValue.getModelEntity()).thenReturn(modelEntity);
        when(genericValue.containsPrimaryKey()).thenReturn(true);
        when(modelEntity.isField("fieldName")).thenReturn(true);

        EntitySaxReader esr = new EntitySaxReader(delegator);
        String input = "<entity-engine-xml><EntityName fieldName=\"field value\"/></entity-engine-xml>";
        long recordsProcessed = esr.parse(input);
        verify(clonedDelegator).makeValue("EntityName");
        assertEquals(1, recordsProcessed);
    }
}
