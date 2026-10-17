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
package org.apache.ofbiz.common;

/**
 * Allow access to language files mapping from freemarker template
 */
public final class JsLanguageFilesMappingUtil {

    private JsLanguageFilesMappingUtil() { }

    public static String getFile(String libraryName, String localeString) {
        switch (libraryName) {
        case "datejs":
            return JsLanguageFilesMapping.DateJs.getFilePath(localeString);
        case "dateTime":
            return JsLanguageFilesMapping.DateTime.getFilePath(localeString);
        case "jquery":
            return JsLanguageFilesMapping.JQuery.getFilePath(localeString);
        case "select2":
            return JsLanguageFilesMapping.Select2.getFilePath(localeString);
        case "validation":
            return JsLanguageFilesMapping.Validation.getFilePath(localeString);
        default:
            return "";
        }
    }
}
