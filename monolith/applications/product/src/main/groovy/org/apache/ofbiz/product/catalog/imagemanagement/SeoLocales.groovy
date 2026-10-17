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
package org.apache.ofbiz.product.catalog.imagemanagement

String serverName = request.getServerName()
String serverLocal = serverName.substring(serverName.lastIndexOf('.') + 1)

String nameLocal
String productTextData
contentAssocs.each { contentAssoc ->
    content = from('Content').where('contentId', contentAssoc.contentIdTo).queryOne()
    localeString = content.localeString

    switch (serverLocal) {
        case 'au':
            nameLocal = 'en_AU'
            break
        case 'ca':
            nameLocal = 'en_CA'
            break
        case 'de':
            nameLocal = 'de'
            break
        case 'ie':
            nameLocal = 'en_IE'
            break
        case 'fr':
            nameLocal = 'fr'
            break
        case 'es':
            nameLocal = 'es'
            break
        case 'it':
            nameLocal = 'it'
            break
        case 'uk':
            nameLocal = 'en_GB'
            break
        case 'sg':
            nameLocal = 'en_SG'
            break
        default:
            nameLocal = 'en_US'
            break
    }

    if (localeString == nameLocal) {
        electronicText = from('ElectronicText').where('dataResourceId', content.dataResourceId).queryOne()
        productTextData = electronicText.textData
    }
}

if (productTextData == null) {
    context.productTextData = product.productName
} else {
    context.productTextData = productTextData
}

