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

productStoreId = parameters.productStoreId
storeThemeId = null
if (parameters.ebayStore) {
    ebayStore = parameters.get("ebayStore")
    storeThemeId = ebayStore.get("storeThemeId")
}
if (productStoreId != null) {
    flag = null
    storeBasicThemes = null
    resultsBasicThemes = runService('retrieveBasicThemeArray',["productStoreId":productStoreId, "userLogin": userLogin])
    if(resultsBasicThemes){
        storeBasicThemes = resultsBasicThemes.get("storeThemeList")
        //check what kind of theme?
        context.put("storeThemeOptList",storeBasicThemes)
        if (storeThemeId != null && storeBasicThemes != null) {
            storeBasicThemes.each { storeTheme ->
                if (storeThemeId == storeTheme.storeThemeId) {
                    flag = "Basic"
                }
            }
        }
    }
    storeAdvanceThemes = null
    storeAdvancedThemeColorOptList = null
    resultsAdvanceThemes = runService('retrieveAdvancedThemeArray',["productStoreId":productStoreId, "userLogin": userLogin])
    if (resultsAdvanceThemes) {
        storeAdvanceThemes = resultsAdvanceThemes.get("storeThemeList")
        storeAdvancedThemeColorOptList = resultsAdvanceThemes.get("storeAdvancedThemeColorOptList")
        context.put("storeAdvanceThemeOptList",storeAdvanceThemes)
        context.put("storeAdvancedThemeColorOptList",storeAdvancedThemeColorOptList)

        if (storeThemeId != null && storeAdvanceThemes != null) {
            storeAdvanceThemes.each { storeAdvanceTheme ->
                if(storeThemeId == storeAdvanceTheme.storeThemeId){
                    flag = "Advanced"
                }
            }
        }
    }
    resultsFontTheme = runService('retrieveStoreFontTheme',["productStoreId":productStoreId, "userLogin": userLogin])
    if (resultsFontTheme) {
        storeFontTheme = resultsFontTheme.get("advanceFontTheme")
        context.put("storeFontTheme",storeFontTheme)
    }
    context.put("themeType",flag)
}
