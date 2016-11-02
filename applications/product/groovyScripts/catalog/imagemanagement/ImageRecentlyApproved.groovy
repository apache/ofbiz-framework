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

import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.util.*
import java.text.SimpleDateFormat
import java.util.List
import java.util.Set
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityFindOptions

def limit = 13 // set number of days
def sdf = new SimpleDateFormat("EEEE yyyy-MM-dd 00:00:00.000")
def sdf2 = new SimpleDateFormat("EEEE dd/MM/yyyy")
def sdfTime = new SimpleDateFormat("HH:mm")
def today = new Date()

for(i in 0..limit){
    def date1 = sdf.format(today-i)
    def date2 = sdf.format(today-i+1)
    def parseDate1 = sdf.parse(date1)
    def parseDate2 = sdf.parse(date2)
    def timeStampDate1 = UtilDateTime.toTimestamp(parseDate1)
    def timeStampDate2 = UtilDateTime.toTimestamp(parseDate2)
    // make condition for distinct productId
    def exprs = []
    exprs.add(EntityCondition.makeCondition("productContentTypeId",EntityOperator.EQUALS, "IMAGE"))
    exprs.add(EntityCondition.makeCondition("statusId",EntityOperator.EQUALS, "IM_APPROVED"))
    exprs.add(EntityCondition.makeCondition("purchaseFromDate", EntityOperator.GREATER_THAN_EQUAL_TO, timeStampDate1))
    exprs.add(EntityCondition.makeCondition("purchaseFromDate", EntityOperator.LESS_THAN, timeStampDate2))
    // query result
    def productContentAndInfoList = select("productId").from("ProductContentAndInfo").where(exprs).distinct().queryList()
    
    // finding time
    def timeList = from("ProductContentAndInfo").where(exprs).orderBy("productId").queryList()
    def groupByTimeList =  timeList.groupBy{it.productId}
    def tempTimeList = []
    groupByTimeList.each() {
        key,value -> tempTimeList.add(value.purchaseFromDate)
    }
    
    def time = []
    if(tempTimeList.size > 0){
        for(j in 0..tempTimeList.size-1){
            time.add(sdfTime.format(tempTimeList.get(j).get(0)))
        }
    }
    def showDate = sdf2.format(today-i)
    
    switch (i) {
        case 0: context.approved_0 = productContentAndInfoList; context.time_0 = time; context.date0 = showDate
            context.timeStampDate1_0 = timeStampDate1; context.timeStampDate2_0 = timeStampDate2; break
        case 1: context.approved_1 = productContentAndInfoList; context.time_1 = time; context.date1 = showDate
            context.timeStampDate1_1 = timeStampDate1; context.timeStampDate2_1 = timeStampDate2; break
        case 2: context.approved_2 = productContentAndInfoList; context.time_2 = time; context.date2 = showDate
            context.timeStampDate1_2 = timeStampDate1; context.timeStampDate2_2 = timeStampDate2; break
        case 3: context.approved_3 = productContentAndInfoList; context.time_3 = time; context.date3 = showDate
            context.timeStampDate1_3 = timeStampDate1; context.timeStampDate2_3 = timeStampDate2; break
        case 4: context.approved_4 = productContentAndInfoList; context.time_4 = time; context.date4 = showDate
            context.timeStampDate1_4 = timeStampDate1; context.timeStampDate2_4 = timeStampDate2; break
        case 5: context.approved_5 = productContentAndInfoList; context.time_5 = time; context.date5 = showDate
            context.timeStampDate1_5 = timeStampDate1; context.timeStampDate2_5 = timeStampDate2; break
        case 6: context.approved_6 = productContentAndInfoList; context.time_6 = time; context.date6 = showDate
            context.timeStampDate1_6 = timeStampDate1; context.timeStampDate2_6 = timeStampDate2; break
        case 7: context.approved_7 = productContentAndInfoList; context.time_7 = time; context.date7 = showDate
            context.timeStampDate1_7 = timeStampDate1; context.timeStampDate2_7 = timeStampDate2; break
        case 8: context.approved_8 = productContentAndInfoList; context.time_8 = time; context.date8 = showDate
            context.timeStampDate1_8 = timeStampDate1; context.timeStampDate2_8 = timeStampDate2; break
        case 9: context.approved_9 = productContentAndInfoList; context.time_9 = time; context.date9 = showDate
            context.timeStampDate1_9 = timeStampDate1; context.timeStampDate2_9 = timeStampDate2; break
        case 10: context.approved_10 = productContentAndInfoList; context.time_10 = time; context.date10 = showDate
            context.timeStampDate1_10 = timeStampDate1; context.timeStampDate2_10 = timeStampDate2; break
        case 11: context.approved_11 = productContentAndInfoList; context.time_11 = time; context.date11 = showDate
            context.timeStampDate1_11 = timeStampDate1; context.timeStampDate2_11 = timeStampDate2; break
        case 12: context.approved_12 = productContentAndInfoList; context.time_12 = time; context.date12 = showDate
            context.timeStampDate1_12 = timeStampDate1; context.timeStampDate2_12 = timeStampDate2; break
        case 13: context.approved_13 = productContentAndInfoList; context.time_13 = time; context.date13 = showDate
            context.timeStampDate1_13 = timeStampDate1; context.timeStampDate2_13 = timeStampDate2; break
        default: context.error = "error"
    }
}
