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

import org.ofbiz.accounting.util.UtilAccounting
import org.ofbiz.base.util.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
birtParameters = [:];
birtParameters.partyIdTo = request.getParameter("partyIdTo");
birtParameters.paymentId = request.getParameter("paymentId");
birtParameters.paymentTypeId = request.getParameter("paymentTypeId");
birtParameters.paymentId_op = request.getParameter("paymentId_op");
birtParameters.paymentRefNum_ic = request.getParameter("paymentRefNum_ic");
birtParameters.noConditionFind = request.getParameter("noConditionFind");
birtParameters.contentType = request.getParameter("contentType");
birtParameters.partyIdFrom = request.getParameter("partyIdFrom");
birtParameters.paymentRefNum_op = request.getParameter("paymentRefNum_op");
birtParameters.amount = request.getParameter("amount");
birtParameters.statusId = request.getParameter("statusId");
birtParameters.paymentGatewayResponseId = request.getParameter("paymentGatewayResponseId");
birtParameters.paymentId_ic = request.getParameter("paymentId_ic");
birtParameters.paymentRefNum = request.getParameter("paymentRefNum");
birtParameters.comments_ic = request.getParameter("comments_ic");
birtParameters.comments_op = request.getParameter("comments_op");
birtParameters.comments = request.getParameter("comments");
request.setAttribute("birtParameters", birtParameters);
return "success";
