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
package org.ofbiz.accounting;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.webapp.event.EventHandlerException;

public class GlEvents {

public static final String module = GlEvents.class.getName();
public static String createReconcileAccount(HttpServletRequest request,HttpServletResponse response) {
    LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
    final GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
    GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
    Map ctx = UtilHttp.getParameterMap(request);
    String acctgTransId;
    String acctgTransEntrySeqId;
    String glAccountId = null;
    String organizationPartyId = null;
    BigDecimal reconciledBalance = BigDecimal.ZERO;
    boolean isSelected;
    String debitCreditFlag;
    // The number of multi form rows is retrieved
    int rowCount = UtilHttp.getMultiFormRowCount(ctx);
    for (int i = 0; i < rowCount; i++) {  //for calculating amount per glAccountId
        BigDecimal amount = BigDecimal.ZERO;
        String suffix = UtilHttp.MULTI_ROW_DELIMITER + i;
        isSelected = (ctx.containsKey("_rowSubmit" + suffix) && "Y".equalsIgnoreCase((String)ctx.get("_rowSubmit" + suffix)));
        if (!isSelected) {
            continue;
        }
        acctgTransId = (String) ctx.get("acctgTransId" + suffix);
        acctgTransEntrySeqId = (String) ctx.get("acctgTransEntrySeqId" + suffix);
        organizationPartyId = (String) ctx.get("organizationPartyId" + suffix);
        glAccountId = (String) ctx.get("glAccountId" + suffix);
        GenericValue acctgTransEntry;
        try {
            List acctgTransEntries = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", acctgTransId, "acctgTransEntrySeqId", acctgTransEntrySeqId));
            if (acctgTransEntries.size() > 0) {
                Iterator acctgTransEntryItr = acctgTransEntries.iterator();
                while (acctgTransEntryItr.hasNext()) {  //calculate amount for each AcctgTransEntry according to glAccountId based on debit and credit
                    acctgTransEntry = (GenericValue) acctgTransEntryItr.next();
                    debitCreditFlag = (String) acctgTransEntry.getString("debitCreditFlag");
                    if ("D".equalsIgnoreCase(debitCreditFlag)) {
                        amount = amount.add(acctgTransEntry.getBigDecimal("amount")); //for debit
                    } else {
                          amount = amount.subtract(acctgTransEntry.getBigDecimal("amount")); //for credit
                    }
                }
            }
            reconciledBalance = reconciledBalance.add(amount);  //total balance per glAccountId
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return "error";
        }

    }
    Map fieldMap = UtilMisc.toMap("glReconciliationName", "Reconciliation at date " + UtilDateTime.nowTimestamp(), "glAccountId", glAccountId, "organizationPartyId", organizationPartyId, "reconciledDate", UtilDateTime.nowTimestamp(), "reconciledBalance", reconciledBalance, "userLogin", userLogin);
    Map glReconResult = null;
    try {
        glReconResult = dispatcher.runSync("createGlReconciliation", fieldMap); //create GlReconciliation for the glAccountId
        if (ServiceUtil.isError(glReconResult)) {
            return "error";
         }
    } catch (GenericServiceException e) {
        Debug.logError(e, module);
        return "error";
    }
    String glReconciliationId = (String) glReconResult.get("glReconciliationId");
    String reconciledAmount;
    for (int i = 0; i < rowCount; i++) {
        String suffix = UtilHttp.MULTI_ROW_DELIMITER + i;
        isSelected = (ctx.containsKey("_rowSubmit" + suffix) && "Y".equalsIgnoreCase((String)ctx.get("_rowSubmit" + suffix)));
        if (!isSelected) {
            continue;
        }
        acctgTransId = (String) ctx.get("acctgTransId" + suffix);
        acctgTransEntrySeqId = (String) ctx.get("acctgTransEntrySeqId" + suffix);
        GenericValue acctgTransEntry;
        try {
            List acctgTransEntries = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", acctgTransId, "acctgTransEntrySeqId", acctgTransEntrySeqId));
            if (acctgTransEntries.size() > 0) {
                Iterator acctgTransEntryItr = acctgTransEntries.iterator();
                while (acctgTransEntryItr.hasNext()) {
                    acctgTransEntry = (GenericValue) acctgTransEntryItr.next();
                    reconciledAmount = acctgTransEntry.getString("amount");
                    acctgTransId = acctgTransEntry.getString("acctgTransId");
                    acctgTransEntrySeqId = acctgTransEntry.getString("acctgTransEntrySeqId");
                    Map glReconEntryMap = UtilMisc.toMap("glReconciliationId", glReconciliationId, "acctgTransId", acctgTransId, "acctgTransEntrySeqId", acctgTransEntrySeqId, "reconciledAmount", reconciledAmount, "userLogin", userLogin);
                    Map glReconEntryResult = null;
                    try {
                        glReconEntryResult = dispatcher.runSync("createGlReconciliationEntry", glReconEntryMap);
                        if (ServiceUtil.isError(glReconEntryResult)) {
                            return "error";
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return "error";
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return "error";
        }
    }
    ctx.put("glReconciliationId", glReconciliationId);
    request.setAttribute("glReconciliationId", glReconciliationId);
    return "success";
  }
}