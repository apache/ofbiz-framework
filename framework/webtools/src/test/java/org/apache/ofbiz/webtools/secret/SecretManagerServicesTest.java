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
package org.apache.ofbiz.webtools.secret;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for the rotation-sync logic in {@link SecretManagerServices}: the change-detection
 * step in {@code readCurrentStoredValue} and the {@code syncAllRotatedSystemProperties} loop
 * backing {@code autoSyncRotatedSecrets}. These mock {@link Delegator}/{@link DispatchContext}/
 * {@link LocalDispatcher} so they run without booting the full entity/service engine.
 */
@RunWith(MockitoJUnitRunner.class)
public class SecretManagerServicesTest {

    @Mock
    private Delegator delegator;
    @Mock
    private DispatchContext dctx;
    @Mock
    private LocalDispatcher dispatcher;

    private static GenericValue systemPropertyRow(String resourceId, String propertyId, String lookupKey, String value) {
        GenericValue gv = mock(GenericValue.class);
        when(gv.getString("systemResourceId")).thenReturn(resourceId);
        when(gv.getString("systemPropertyId")).thenReturn(propertyId);
        when(gv.getString("systemPropertyLookup")).thenReturn(lookupKey);
        when(gv.getString("systemPropertyValue")).thenReturn(value);
        return gv;
    }

    private void mockSystemPropertyFindList(List<GenericValue> rows) throws GenericEntityException {
        // EntityQuery.use(Delegator) routes through DelegatorProvider.getDelegator(), which an
        // un-stubbed mock would otherwise answer with null.
        when(delegator.getDelegator()).thenReturn(delegator);
        when(delegator.findList(eq("SystemProperty"), any(EntityCondition.class), any(), any(), any(),
                any(EntityFindOptions.class), anyBoolean())).thenReturn(rows);
    }

    @Test
    public void autoSyncNoOpsWhenDisabled() {
        // secret.rotation.autosync.enabled defaults to false in security.properties.
        // The permission check runs first; stub security to allow SECRET_MAINT so we reach
        // the autosync-disabled guard and confirm the early-exit success response.
        Security security = mock(Security.class);
        when(security.hasPermission(anyString(), (GenericValue) any())).thenReturn(true);
        when(dctx.getSecurity()).thenReturn(security);

        Map<String, Object> result = SecretManagerServices.autoSyncRotatedSecrets(dctx, UtilMisc.toMap());
        assertEquals("success", result.get("responseMessage"));
    }

    @Test
    public void readCurrentStoredValueReturnsNullWhenRowMissing() throws GenericEntityException, GeneralException {
        mockSystemPropertyFindList(List.of());
        String value = SecretManagerServices.readCurrentStoredValue(
                delegator, SecretManagerServices.TARGET_SYSTEM_PROPERTY, "myResource", "myProp", "myLookupKey");
        assertNull(value);
    }

    @Test
    public void readCurrentStoredValueReturnsPlainStoredValue() throws GenericEntityException, GeneralException {
        GenericValue row = systemPropertyRow("myResource", "myProp", "myLookupKey", "currentPlainValue");
        mockSystemPropertyFindList(List.of(row));
        String value = SecretManagerServices.readCurrentStoredValue(
                delegator, SecretManagerServices.TARGET_SYSTEM_PROPERTY, "myResource", "myProp", "myLookupKey");
        assertEquals("currentPlainValue", value);
    }

    @Test
    public void autoSyncCountsChangedUnchangedAndFailedRowsIndependently() throws GenericEntityException, GenericServiceException {
        GenericValue changedRow = systemPropertyRow("resA", "propA", "keyA", "old-value");
        GenericValue unchangedRow = systemPropertyRow("resB", "propB", "keyB", "same-value");
        GenericValue failingRow = systemPropertyRow("resC", "propC", "keyC", "whatever");
        mockSystemPropertyFindList(List.of(changedRow, unchangedRow, failingRow));
        when(dctx.getDelegator()).thenReturn(delegator);
        when(dctx.getDispatcher()).thenReturn(dispatcher);
        // Feature 2 adds a ROTATION_POLL SecretAuditLogger.log() call after the loop.
        // Return "test" so SecretAuditLogger skips the DB write in the test guard.
        when(delegator.getDelegatorBaseName()).thenReturn("test");

        when(dispatcher.runSync(eq("syncSecretFromProvider"), lookupKeyMatches("keyA"))).thenReturn(successWithChanged(true));
        when(dispatcher.runSync(eq("syncSecretFromProvider"), lookupKeyMatches("keyB"))).thenReturn(successWithChanged(false));
        when(dispatcher.runSync(eq("syncSecretFromProvider"), lookupKeyMatches("keyC")))
                .thenThrow(new GenericServiceException("vault unreachable"));

        Map<String, Object> result = SecretManagerServices.syncAllRotatedSystemProperties(dctx, null);

        assertEquals(1L, result.get("syncedCount"));
        assertEquals(1L, result.get("unchangedCount"));
        assertEquals(1L, result.get("failedCount"));
    }

    private static Map<String, Object> lookupKeyMatches(String lookupKey) {
        return argThat(map -> map != null && lookupKey.equals(map.get("lookupKey")));
    }

    private static Map<String, Object> successWithChanged(boolean changed) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("changed", changed);
        return result;
    }
}
