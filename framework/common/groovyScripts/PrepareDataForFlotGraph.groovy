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

import org.apache.ofbiz.base.util.StringUtil
chartData = context.chartData
chartType = context.chartType
labelFieldName = context.labelFieldName
dataFieldName = context.dataFieldName
if("Pie" == chartType){
    iter = chartData.iterator()
    first = true
    dataText = ""
    while(iter.hasNext()){
        entry = iter.next()
        if(!first){
            dataText = dataText + ","
        }
        first = false
        dataText = dataText + entry.get(labelFieldName) + "," + entry.get(dataFieldName)
    }
    context.dataText = dataText
}
else if("Bars" == chartType){
    iter = chartData.iterator()
    i = 1
    dataText = ""
    labels = ""
    while(iter.hasNext()){
        entry = iter.next()
        if(i!=1){
            dataText = dataText + ","
            labels = labels + ","
        }
        dataText = dataText + i + "," + entry.get(dataFieldName)
        labels = labels + entry.get(labelFieldName)
        i++
    }
    context.dataText = dataText
    context.labelsText = labels
}