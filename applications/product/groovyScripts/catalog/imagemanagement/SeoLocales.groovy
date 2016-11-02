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
import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.model.*

import javax.servlet.*
import javax.servlet.http.*

Locale locale = UtilHttp.getLocale(request)
String serverName = request.getServerName()
String serverLocal = serverName.substring(serverName.lastIndexOf(".") + 1)

def nameLocal
def productTextData
contentAssocs.each { contentAssoc ->

content = from("Content").where("contentId", contentAssoc.contentIdTo).queryOne()
localeString = content.localeString

    if (serverLocal == "au") {
        nameLocal = "en_AU"
    } else if (serverLocal == "ca") {
        nameLocal = "en_CA"
    } else if (serverLocal == "de") {
        nameLocal = "de"
    } else if (serverLocal == "ie") {
        nameLocal = "en_IE"
    } else if (serverLocal == "fr") {
        nameLocal = "fr"
    } else if (serverLocal == "es") {
        nameLocal = "es"
    } else if (serverLocal == "it") {
        nameLocal = "it"
    } else if (serverLocal == "uk") {
        nameLocal = "en_GB"
    } else if (serverLocal == "sg") {
        nameLocal = "en_SG"
    } else {
        nameLocal = "en_US"
    }
    
    if (localeString == nameLocal) {
            electronicText = from("ElectronicText").where("dataResourceId", content.dataResourceId).queryOne()
            productTextData = electronicText.textData
    }

}

if (productTextData == null) {
    context.productTextData = product.productName
} else {
    context.productTextData = productTextData
}



