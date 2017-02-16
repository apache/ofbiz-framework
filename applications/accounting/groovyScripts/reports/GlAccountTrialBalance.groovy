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
import com.ibm.icu.util.Calendar
import org.apache.ofbiz.accounting.util.UtilAccounting
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilNumber
import org.apache.ofbiz.base.util.UtilValidate

import java.sql.Timestamp

if (parameters.get('ApplicationDecorator|organizationPartyId')) {
    onlyIncludePeriodTypeIdList = []
    onlyIncludePeriodTypeIdList.add("FISCAL_YEAR")
    customTimePeriodResults = runService('findCustomTimePeriods', [findDate : UtilDateTime.nowTimestamp(), organizationPartyId : parameters.get('ApplicationDecorator|organizationPartyId'), onlyIncludePeriodTypeIdList : onlyIncludePeriodTypeIdList, userLogin : userLogin])
    customTimePeriodList = customTimePeriodResults.customTimePeriodList
    if (UtilValidate.isNotEmpty(customTimePeriodList)) {
        context.timePeriod = customTimePeriodList.first().customTimePeriodId
    }
    decimals = UtilNumber.getBigDecimalScale("ledger.decimals")
    rounding = UtilNumber.getBigDecimalRoundingMode("ledger.rounding")
    context.currentOrganization = from("PartyNameView").where("partyId", parameters.get('ApplicationDecorator|organizationPartyId')).queryOne()
    if (parameters.glAccountId) {
        glAccount = from("GlAccount").where("glAccountId", parameters.glAccountId).queryOne()
        isDebitAccount = UtilAccounting.isDebitAccount(glAccount)
        context.isDebitAccount = isDebitAccount
        context.glAccount = glAccount
    }

    currentTimePeriod = null
    BigDecimal balanceOfTheAcctgForYear = BigDecimal.ZERO

    if (parameters.timePeriod) {
        currentTimePeriod = from("CustomTimePeriod").where("customTimePeriodId", parameters.timePeriod).queryOne()
        previousTimePeriodResult = runService('getPreviousTimePeriod', [customTimePeriodId : parameters.timePeriod, userLogin : userLogin])
        previousTimePeriod = previousTimePeriodResult.previousTimePeriod
        if (UtilValidate.isNotEmpty(previousTimePeriod)) {
            glAccountHistory = from("GlAccountHistory").where("customTimePeriodId", previousTimePeriod.customTimePeriodId, "glAccountId", parameters.glAccountId, "organizationPartyId", parameters.get('ApplicationDecorator|organizationPartyId')).queryOne()
            if (glAccountHistory && glAccountHistory.endingBalance != null) {
                context.openingBalance = glAccountHistory.endingBalance
                balanceOfTheAcctgForYear = glAccountHistory.endingBalance
            } else {
                context.openingBalance = BigDecimal.ZERO
            }
        }
    }

    if (currentTimePeriod) {
        context.currentTimePeriod = currentTimePeriod
        customTimePeriodStartDate = UtilDateTime.getMonthStart(UtilDateTime.toTimestamp(currentTimePeriod.fromDate), timeZone, locale)
        customTimePeriodEndDate = UtilDateTime.getMonthEnd(UtilDateTime.toTimestamp(currentTimePeriod.fromDate), timeZone, locale)

        Calendar calendarTimePeriodStartDate = UtilDateTime.toCalendar(customTimePeriodStartDate)
        glAcctgTrialBalanceList = []
        BigDecimal totalOfYearToDateDebit = BigDecimal.ZERO
        BigDecimal totalOfYearToDateCredit = BigDecimal.ZERO
        isPosted = parameters.isPosted

        while (customTimePeriodEndDate <= currentTimePeriod.thruDate) {
            if ("ALL".equals(isPosted)) {
                isPosted = ""
            }
            acctgTransEntriesAndTransTotal = runService('getAcctgTransEntriesAndTransTotal', 
                    [customTimePeriodStartDate : customTimePeriodStartDate, customTimePeriodEndDate : customTimePeriodEndDate, organizationPartyId : parameters.get('ApplicationDecorator|organizationPartyId'), glAccountId : parameters.glAccountId, isPosted : isPosted, userLogin : userLogin])
            totalOfYearToDateDebit = totalOfYearToDateDebit + acctgTransEntriesAndTransTotal.debitTotal
            acctgTransEntriesAndTransTotal.totalOfYearToDateDebit = totalOfYearToDateDebit.setScale(decimals, rounding)
            totalOfYearToDateCredit = totalOfYearToDateCredit + acctgTransEntriesAndTransTotal.creditTotal
            acctgTransEntriesAndTransTotal.totalOfYearToDateCredit = totalOfYearToDateCredit.setScale(decimals, rounding)

            if (isDebitAccount) {
                acctgTransEntriesAndTransTotal.balance = acctgTransEntriesAndTransTotal.debitCreditDifference
            } else {
                acctgTransEntriesAndTransTotal.balance = -1 * acctgTransEntriesAndTransTotal.debitCreditDifference
            }
            balanceOfTheAcctgForYear = balanceOfTheAcctgForYear + acctgTransEntriesAndTransTotal.balance
            acctgTransEntriesAndTransTotal.balanceOfTheAcctgForYear = balanceOfTheAcctgForYear.setScale(decimals, rounding)

            glAcctgTrialBalanceList.add(acctgTransEntriesAndTransTotal)

            calendarTimePeriodStartDate.add(Calendar.MONTH, 1)
            Timestamp retStampStartDate = new Timestamp(calendarTimePeriodStartDate.getTimeInMillis())
            retStampStartDate.setNanos(0)
            customTimePeriodStartDate = retStampStartDate
            customTimePeriodEndDate = UtilDateTime.getMonthEnd(UtilDateTime.toTimestamp(retStampStartDate), timeZone, locale)
        }
        context.glAcctgTrialBalanceList = glAcctgTrialBalanceList
    }
}
