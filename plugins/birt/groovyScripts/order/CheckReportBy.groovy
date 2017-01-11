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

import org.apache.ofbiz.base.util.UtilValidate

reportBy = parameters.reportBy
exportType = parameters.exportType

if (UtilValidate.isEmpty(parameters.fromDate)) {
    request.setAttribute("_ERROR_MESSAGE_", "Please select From Date.")
    return "error"
}

if (exportType == "pdf") {
    if (reportBy == "day") {
        return "dayPDF"
    } else if (reportBy == "week") {
        return "weekPDF"
    } else if (reportBy == "month") {
        return "monthPDF"
    } else {
        request.setAttribute("_ERROR_MESSAGE_", "Please select Report By.")
        return "error"
    }
}

if (exportType == "excel") {
    if (reportBy == "day") {
        return "dayExcel"
    } else if (reportBy == "week") {
        return "weekExcel"
    } else if (reportBy == "month") {
        return "monthExcel"
    } else {
        request.setAttribute("_ERROR_MESSAGE_", "Please select Report By.")
        return "error"
    }
}

if (exportType == "html") {
    if (reportBy == "day") {
        return "dayHTML"
    } else if (reportBy == "week") {
        return "weekHTML"
    } else if (reportBy == "month") {
        return "monthHTML"
    } else {
        request.setAttribute("_ERROR_MESSAGE_", "Please select Report By.")
        return "error"
    }
}
