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

assetAccountBalanceMap = [:]
assetAccountBalanceList = []
assetAccountBalanceList1.each { accountBalance ->
    assetAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                            accountName: accountBalance.accountName, balance1: accountBalance.balance,
                                                            balance2: BigDecimal.ZERO])
}
assetAccountBalanceList2.each { accountBalance ->
    Map assetAccount = (Map)assetAccountBalanceMap.get(accountBalance.glAccountId)
    if (assetAccount) {
        assetAccount.put('balance2', accountBalance.balance)
    } else {
        assetAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                                accountName: accountBalance.accountName, balance2: accountBalance.balance,
                                                                balance1: BigDecimal.ZERO])
    }
}
assetAccountBalanceList = UtilMisc.sortMaps(assetAccountBalanceMap.values().asList(), UtilMisc.toList('accountCode'))
context.assetAccountBalanceList = assetAccountBalanceList

liabilityAccountBalanceMap = [:]
liabilityAccountBalanceList = []
liabilityAccountBalanceList1.each { accountBalance ->
    liabilityAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                                accountName: accountBalance.accountName, balance1: accountBalance.balance,
                                                                balance2: BigDecimal.ZERO])
}
liabilityAccountBalanceList2.each { accountBalance ->
    Map assetAccount = (Map)liabilityAccountBalanceMap.get(accountBalance.glAccountId)
    if (assetAccount) {
        assetAccount.put('balance2', accountBalance.balance)
    } else {
        liabilityAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                                    accountName: accountBalance.accountName, balance2: accountBalance.balance,
                                                                    balance1: BigDecimal.ZERO])
    }
}
liabilityAccountBalanceList = UtilMisc.sortMaps(liabilityAccountBalanceMap.values().asList(), UtilMisc.toList('accountCode'))
context.liabilityAccountBalanceList = liabilityAccountBalanceList

equityAccountBalanceMap = [:]
equityAccountBalanceList = []
equityAccountBalanceList1.each { accountBalance ->
    equityAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                             accountName: accountBalance.accountName, balance1: accountBalance.balance,
                                                             balance2: BigDecimal.ZERO])
}
equityAccountBalanceList2.each { accountBalance ->
    Map assetAccount = (Map)equityAccountBalanceMap.get(accountBalance.glAccountId)
    if (assetAccount) {
        assetAccount.put('balance2', accountBalance.balance)
    } else {
        equityAccountBalanceMap.put(accountBalance.glAccountId, [glAccountId: accountBalance.glAccountId, accountCode: accountBalance.accountCode,
                                                                 accountName: accountBalance.accountName, balance2: accountBalance.balance,
                                                                 balance1: BigDecimal.ZERO])
    }
}
equityAccountBalanceList = UtilMisc.sortMaps(equityAccountBalanceMap.values().asList(), UtilMisc.toList('accountCode'))
context.equityAccountBalanceList = equityAccountBalanceList

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
