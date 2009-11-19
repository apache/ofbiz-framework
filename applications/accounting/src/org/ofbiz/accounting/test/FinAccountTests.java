/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.accounting.test;

import java.math.BigDecimal;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.testtools.OFBizTestCase;

/**
 * FinAccountTests
 */
public class FinAccountTests extends OFBizTestCase {

    protected GenericValue userLogin = null;

    public FinAccountTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testCreateFinAccount() throws Exception {
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("finAccountId", "TESTACCOUNT1");
        ctx.put("finAccountName", "Test Financial Account");
        ctx.put("finAccountTypeId", "BANK_ACCOUNT");
        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = dispatcher.runSync("createFinAccount", ctx);
        assertEquals("Service result success", ModelService.RESPOND_SUCCESS, resp.get(ModelService.RESPONSE_MESSAGE));
    }

    public void testDeposit() throws Exception {
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("finAccountId", "TESTACCOUNT1");
        ctx.put("amount", new BigDecimal("100.00"));
        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = dispatcher.runSync("finAccountDeposit", ctx);
        BigDecimal balance = (BigDecimal) resp.get("balance");
        assertEquals(balance.toPlainString(), "100.00");
    }

    public void testWithdraw() throws Exception {
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("finAccountId", "TESTACCOUNT1");
        ctx.put("amount", new BigDecimal("50.00"));
        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = dispatcher.runSync("finAccountWithdraw", ctx);
        BigDecimal previousBalance = (BigDecimal) resp.get("previousBalance");
        BigDecimal balance = ((BigDecimal) resp.get("balance"));
        assertEquals(balance.add(new BigDecimal("50.00")).toPlainString(), previousBalance.toPlainString());
    }
}
