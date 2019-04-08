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

 import org.apache.ofbiz.base.util.UtilDateTime
 
 birtParameters = [:]
 /*
 birtParameters.facilityId = request.getParameter("facilityId")
 birtParameters.productId = request.getParameter("productId")
 birtParameters.productTypeId = request.getParameter("productTypeId")
 birtParameters.searchInProductCategoryId = request.getParameter("searchInProductCategoryId")
 birtParameters.contentType = request.getParameter("contentType")
 birtParameters.productSupplierId = request.getParameter("productSupplierId")
 birtParameters.statusId = request.getParameter("statusId")
 birtParameters.productsSoldThruTimestamp = request.getParameter("productsSoldThruTimestamp")
 birtParameters.VIEW_SIZE = request.getParameter("VIEW_SIZE")
 birtParameters.monthsInPastLimit = request.getParameter("monthsInPastLimit")
 birtParameters.fromDateSellThrough = request.getParameter("fromDateSellThrough")
 birtParameters.thruDateSellThrough = request.getParameter("thruDateSellThrough")
 */

 int lastIntMonth = Integer.parseInt(request.getParameter("lastIntMonth"))
 if (lastIntMonth == 0 ){
     fromOrderDate = null
 }else{
     fromDateTime = UtilDateTime.getDayStart(UtilDateTime.toTimestamp(UtilDateTime.nowTimestamp()), (lastIntMonth*(-30)))
     fromOrderDate = UtilDateTime.toDateString(fromDateTime,"MMMM dd, yyyy")
 }

 birtParameters.facilityId = request.getParameter("facilityId")
 birtParameters.orderDateDateValue_fld0_op = fromOrderDate.toString()
 request.setAttribute("birtParameters", birtParameters)
 return "success"
