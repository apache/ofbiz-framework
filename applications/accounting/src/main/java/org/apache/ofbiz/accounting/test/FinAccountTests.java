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

package org.apache.ofbiz.accounting.test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

/**
 * FinAccountTests
 */
public class FinAccountTests extends OFBizTestCase {

    public FinAccountTests(String name) {
        super(name);
    }

    /**
     * Test fin account operations.
     * @throws Exception the exception
     */
    public void testFinAccountOperations() throws Exception {
        GenericValue userLogin = getUserLogin("system");
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("finAccountId", "TESTACCOUNT1");
        ctx.put("finAccountName", "Test Financial Account");
        ctx.put("finAccountTypeId", "BANK_ACCOUNT");
        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = getDispatcher().runSync("createFinAccount", ctx);
        assertTrue("Service 'createFinAccount' result success", ServiceUtil.isSuccess(resp));
        ctx.clear();
        ctx.put("finAccountId", "TESTACCOUNT1");
        ctx.put("amount", new BigDecimal("100.00"));
        ctx.put("userLogin", userLogin);
        resp = getDispatcher().runSync("finAccountDeposit", ctx);
        assertTrue("Service 'finAccountDeposit' result success", ServiceUtil.isSuccess(resp));
        BigDecimal balance = (BigDecimal) resp.get("balance");
        assertEquals(balance.toPlainString(), "100.00");
        ctx.clear();
        ctx.put("finAccountId", "TESTACCOUNT1");
        ctx.put("amount", new BigDecimal("50.00"));
        ctx.put("userLogin", userLogin);
        resp = getDispatcher().runSync("finAccountWithdraw", ctx);
        assertTrue("Service 'finAccountWithdraw' result success", ServiceUtil.isSuccess(resp));
        BigDecimal previousBalance = (BigDecimal) resp.get("previousBalance");
        balance = ((BigDecimal) resp.get("balance"));
        assertEquals(balance.add(new BigDecimal("50.00")).toPlainString(), previousBalance.toPlainString());
    }
}
