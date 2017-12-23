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
package org.apache.ofbiz.order.test;

import java.util.Locale;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class FinAccountTest extends OFBizTestCase {
    public static final String module = FinAccountTest.class.getName();
    public FinAccountTest(String name) {
        super(name);
    }

    public void testCreateFinAccountBasic() throws Exception {
        String finAccountCode;
        GenericValue account;

        finAccountCode = FinAccountHelper.getNewFinAccountCode(20, delegator);
        Debug.logInfo("finAccountCode=%s%n", module, finAccountCode);
        assertNotNull(finAccountCode);

        account = FinAccountHelper.getFinAccountFromCode(finAccountCode, delegator);
        assertNull(account);

        delegator.createSetNextSeqId(delegator.makeValue("FinAccount", UtilMisc.toMap("finAccountCode", finAccountCode)));

        account = FinAccountHelper.getFinAccountFromCode(finAccountCode, delegator);
        assertNotNull(account);
        assertEquals(finAccountCode, account.get("finAccountCode"));
        account = FinAccountHelper.getFinAccountFromCode(finAccountCode.toUpperCase(Locale.getDefault()), delegator);
        assertNotNull(account);
        assertEquals(finAccountCode, account.get("finAccountCode"));
        account = FinAccountHelper.getFinAccountFromCode(finAccountCode.toLowerCase(Locale.getDefault()), delegator);
        assertNotNull(account);
        assertEquals(finAccountCode, account.get("finAccountCode"));

        delegator.createSetNextSeqId(delegator.makeValue("FinAccount", UtilMisc.toMap("finAccountCode", finAccountCode)));
        account = FinAccountHelper.getFinAccountFromCode(finAccountCode, delegator);
        assertNull(account);
    }
}
