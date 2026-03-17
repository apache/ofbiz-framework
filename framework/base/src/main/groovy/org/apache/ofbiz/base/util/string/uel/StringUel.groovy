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
package org.apache.ofbiz.base.util.string.uel


import org.apache.ofbiz.base.util.string.UelFunctions
import org.apache.ofbiz.base.util.string.IUelMappingLibrary
import org.apache.ofbiz.base.util.string.UelMapping

import java.lang.reflect.Method

/**
 * Class for importing the String Uel
 */
class StringUel implements IUelMappingLibrary {

    @Override
    List<UelMapping> getUelMappingList() {
        return [
                new UelMapping('str:endsWith', UelFunctions.getMethod('endsWith', String, String)),
                new UelMapping('str:indexOf', UelFunctions.getMethod('indexOf', String, String)),
                new UelMapping('str:lastIndexOf', UelFunctions.getMethod('lastIndexOf', String, String)),
                new UelMapping('str:length', UelFunctions.getMethod('length', String)),
                new UelMapping('str:replace', UelFunctions.getMethod('replace', String, String, String)),
                new UelMapping('str:replaceAll', UelFunctions.getMethod('replaceAll', String, String, String)),
                new UelMapping('str:replaceFirst', UelFunctions.getMethod('replaceFirst', String, String, String)),
                new UelMapping('str:startsWith', UelFunctions.getMethod('startsWith', String, String)),
                new UelMapping('str:endstring', UelFunctions.getMethod('endString', String, int)),
                new UelMapping('str:substring', UelFunctions.getMethod('subString', String, int, int)),
                new UelMapping('str:toString', UelFunctions.getMethod('toString', Object)),
                new UelMapping('str:toLowerCase', UelFunctions.getMethod('toLowerCase', String)),
                new UelMapping('str:toUpperCase', UelFunctions.getMethod('toUpperCase', String)),
                new UelMapping('str:trim', UelFunctions.getMethod('trim', String)),
        ]
    }

}
