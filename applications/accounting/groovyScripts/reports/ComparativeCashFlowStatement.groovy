/*
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
 */

import org.apache.ofbiz.base.util.UtilMisc

openingCashBalanceMap = [:]
openingCashBalanceList = []
openingCashBalanceList1.each { accountBalance ->
    openingCashBalanceMap.put(accountBalance.glAccountId, UtilMisc.toMap("glAccountId", accountBalance.glAccountId, "accountCode", accountBalance.accountCode, "accountName", accountBalance.accountName, "balance1", accountBalance.balance, "balance2", BigDecimal.ZERO))
}
openingCashBalanceList2.each { accountBalance ->
    Map openingCashAccount = (Map)openingCashBalanceMap.get(accountBalance.glAccountId)
    if (!openingCashAccount) {
        openingCashBalanceMap.put(accountBalance.glAccountId, UtilMisc.toMap("glAccountId", accountBalance.glAccountId, "accountCode", accountBalance.accountCode, "accountName", accountBalance.accountName, "balance2", accountBalance.balance, "balance1", BigDecimal.ZERO))
    } else {
        openingCashAccount.put("balance2", accountBalance.balance)
    }
}
openingCashBalanceList = UtilMisc.sortMaps(openingCashBalanceMap.values().asList(), UtilMisc.toList("accountCode"))
context.openingCashBalanceList = openingCashBalanceList

periodCashBalanceMap = [:]
periodCashBalanceList = []
periodCashBalanceList1.each { accountBalance ->
    periodCashBalanceMap.put(accountBalance.glAccountId, UtilMisc.toMap("glAccountId", accountBalance.glAccountId, "accountCode", accountBalance.accountCode, "accountName", accountBalance.accountName, "balance1", accountBalance.balance, "balance2", BigDecimal.ZERO, "D1", accountBalance.D, "C1", accountBalance.C, "D2", BigDecimal.ZERO, "C2", BigDecimal.ZERO))
}
periodCashBalanceList2.each { accountBalance ->
    Map periodCashAccount = (Map)periodCashBalanceMap.get(accountBalance.glAccountId)
    if (!periodCashAccount) {
        periodCashBalanceMap.put(accountBalance.glAccountId, UtilMisc.toMap("glAccountId", accountBalance.glAccountId, "accountCode", accountBalance.accountCode, "accountName", accountBalance.accountName, "balance2", accountBalance.balance, "balance1", BigDecimal.ZERO, "D2", accountBalance.D, "C2", accountBalance.C, "D1", BigDecimal.ZERO, "C1", BigDecimal.ZERO))
    } else {
        periodCashAccount.put("balance2", accountBalance.balance)
        periodCashAccount.put("D2", accountBalance.D)
        periodCashAccount.put("C2", accountBalance.C)
    }
}
periodCashBalanceList = UtilMisc.sortMaps(periodCashBalanceMap.values().asList(), UtilMisc.toList("accountCode"))

context.periodCashBalanceList = periodCashBalanceList
closingCashBalanceMap = [:]
closingCashBalanceList = []
closingCashBalanceList1.each { accountBalance ->
    closingCashBalanceMap.put(accountBalance.glAccountId, UtilMisc.toMap("glAccountId", accountBalance.glAccountId, "accountCode", accountBalance.accountCode, "accountName", accountBalance.accountName, "balance1", accountBalance.balance, "balance2", BigDecimal.ZERO))
}
closingCashBalanceList2.each { accountBalance ->
    Map closingCashAccount = (Map)closingCashBalanceMap.get(accountBalance.glAccountId)
    if (!closingCashAccount) {
        closingCashBalanceMap.put(accountBalance.glAccountId, UtilMisc.toMap("glAccountId", accountBalance.glAccountId, "accountCode", accountBalance.accountCode, "accountName", accountBalance.accountName, "balance2", accountBalance.balance, "balance1", BigDecimal.ZERO))
    } else {
        closingCashAccount.put("balance2", accountBalance.balance)
    }
}
closingCashBalanceList = UtilMisc.sortMaps(closingCashBalanceMap.values().asList(), UtilMisc.toList("accountCode"))
context.closingCashBalanceList = closingCashBalanceList

balanceTotalMap = [:]
cashFlowBalanceTotalList = []
cashFlowBalanceTotalList1.each { accountBalance ->
    balanceTotalMap.put(accountBalance.totalName, UtilMisc.toMap("totalName", accountBalance.totalName, "balance1", accountBalance.balance, "balance2", BigDecimal.ZERO))
}
cashFlowBalanceTotalList2.each { accountBalance ->
    Map cashFlowBalanceAccount = (Map)balanceTotalMap.get(accountBalance.totalName)
    if (!cashFlowBalanceAccount) {
        balanceTotalMap.put(accountBalance.totalName, UtilMisc.toMap("totalName", accountBalance.totalName, "balance2", accountBalance.balance, "balance1", BigDecimal.ZERO))
    } else {
        cashFlowBalanceAccount.put("balance2", accountBalance.balance)
    }
}
cashFlowBalanceTotalList = balanceTotalMap.values().asList()
context.cashFlowBalanceTotalList = cashFlowBalanceTotalList

