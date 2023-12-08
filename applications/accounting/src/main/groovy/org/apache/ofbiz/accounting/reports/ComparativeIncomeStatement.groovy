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
package org.apache.ofbiz.accounting.reports

import org.apache.ofbiz.base.util.UtilMisc

revenueAccountBalanceMap = [:]
revenueAccountBalanceList = []
revenueAccountBalanceList1.each { accountBalance ->
    revenueAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                              accountName: accountBalance.accountName, balance1: accountBalance.balance,
                                                              balance2: BigDecimal.ZERO])
}
revenueAccountBalanceList2.each { accountBalance ->
    Map assetAccount = (Map)revenueAccountBalanceMap.get(accountBalance.glAccountId)
    if (assetAccount) {
        assetAccount.put('balance2', accountBalance.balance)
    } else {
        revenueAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                                  accountName: accountBalance.accountName, balance2: accountBalance.balance,
                                                                  balance1: BigDecimal.ZERO])
    }
}
revenueAccountBalanceList = UtilMisc.sortMaps(revenueAccountBalanceMap.values().asList(), UtilMisc.toList('accountCode'))
context.revenueAccountBalanceList = revenueAccountBalanceList

expenseAccountBalanceMap = [:]
expenseAccountBalanceList = []
expenseAccountBalanceList1.each { accountBalance ->
    expenseAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                              accountName: accountBalance.accountName, balance1: accountBalance.balance,
                                                              balance2: BigDecimal.ZERO])
}
expenseAccountBalanceList2.each { accountBalance ->
    Map assetAccount = (Map)expenseAccountBalanceMap.get(accountBalance.glAccountId)
    if (assetAccount) {
        assetAccount.put('balance2', accountBalance.balance)
    } else {
        expenseAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                                  accountName: accountBalance.accountName, balance2: accountBalance.balance,
                                                                  balance1: BigDecimal.ZERO])
    }
}
expenseAccountBalanceList = UtilMisc.sortMaps(expenseAccountBalanceMap.values().asList(), UtilMisc.toList('accountCode'))
context.expenseAccountBalanceList = expenseAccountBalanceList

incomeAccountBalanceMap = [:]
incomeAccountBalanceList = []
incomeAccountBalanceList1.each { accountBalance ->
    incomeAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                             accountName: accountBalance.accountName, balance1: accountBalance.balance,
                                                             balance2: BigDecimal.ZERO])
}
incomeAccountBalanceList2.each { accountBalance ->
    Map assetAccount = (Map)incomeAccountBalanceMap.get(accountBalance.glAccountId)
    if (assetAccount) {
        assetAccount.put('balance2', accountBalance.balance)
    } else {
        incomeAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                                 accountName: accountBalance.accountName, balance2: accountBalance.balance,
                                                                 balance1: BigDecimal.ZERO])
    }
}
incomeAccountBalanceList = UtilMisc.sortMaps(incomeAccountBalanceMap.values().asList(), UtilMisc.toList('accountCode'))
context.incomeAccountBalanceList = incomeAccountBalanceList

balanceTotalMap = [:]
balanceTotalList = []
balanceTotalList1.each { accountBalance ->
    balanceTotalMap.put(accountBalance.totalName, [totalName: accountBalance.totalName, balance1: accountBalance.balance, balance2: BigDecimal.ZERO])
}
balanceTotalList2.each { accountBalance ->
    Map assetAccount = (Map)balanceTotalMap.get(accountBalance.totalName)
    if (assetAccount) {
        assetAccount.put('balance2', accountBalance.balance)
    } else {
        balanceTotalMap.put(accountBalance.totalName, [totalName: accountBalance.totalName, balance2: accountBalance.balance,
                                                       balance1: BigDecimal.ZERO])
    }
}
balanceTotalList = balanceTotalMap.values().asList()
context.balanceTotalList = balanceTotalList
