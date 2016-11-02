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

import java.util.List
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.base.util.string.*
import org.apache.ofbiz.base.util.UtilMisc

locales = [] as LinkedList
availableLocales = UtilMisc.availableLocales()

// Debug.logInfo(parameters.localeString + "==" +  parameters.localeName)

if (availableLocales) {
    availableLocales.each { availableLocale ->
        locale = [:]
        locale.localeName = availableLocale.getDisplayName(availableLocale)
        locale.localeString = availableLocale.toString()
        if (UtilValidate.isNotEmpty(parameters.localeString)) {
            if (locale.localeString.toUpperCase().contains(parameters.localeString.toUpperCase())) {
                locales.add(locale)
            }
        }
        if (UtilValidate.isNotEmpty(parameters.localeName)) {
            if (locale.localeName.toUpperCase().contains(parameters.localeName.toUpperCase())) {
                locales.add(locale)
            }
        }
        if (UtilValidate.isEmpty(parameters.localeString) && UtilValidate.isEmpty(parameters.localeName)) {
            locales.add(locale)
        }
    }
}

context.locales = locales
