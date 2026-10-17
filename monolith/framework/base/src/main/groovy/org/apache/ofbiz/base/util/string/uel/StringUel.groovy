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

import org.apache.ofbiz.base.util.string.IUelMappingLibrary
import org.apache.ofbiz.base.util.string.UelFunctions
import org.apache.ofbiz.base.util.string.UelMapping

/**
 * Class for importing the String Uel
 */
class StringUel implements IUelMappingLibrary {

    @Override
    List<UelMapping> getUelMappingList() {
        return [
                new UelMapping('str:endsWith', UelFunctions.getMethod('endsWith', String, String),
                        'Returns true if this string ends with the specified suffix.'),
                new UelMapping('str:indexOf', UelFunctions.getMethod('indexOf', String, String),
                        'Returns the index within this string of the first occurrence of the specified substring .'),
                new UelMapping('str:lastIndexOf', UelFunctions.getMethod('lastIndexOf', String, String),
                        'Returns the index within this string of the last occurrence of the specified character.'),
                new UelMapping('str:length', UelFunctions.getMethod('length', String),
                        'Returns the length of this string.'),
                new UelMapping('str:replace', UelFunctions.getMethod('replace', String, String, String),
                        'Replaces each substring of this string that matches the literal target sequence with the' +
                                ' specified literal replacement sequence.'),
                new UelMapping('str:replaceAll', UelFunctions.getMethod('replaceAll', String, String, String),
                        'Replaces each substring of this string that matches the given regular expression with the given replacement.'),
                new UelMapping('str:replaceFirst', UelFunctions.getMethod('replaceFirst', String, String, String),
                        'Replaces the first substring of this string that matches the given regular expression with the given replacement.'),
                new UelMapping('str:startsWith', UelFunctions.getMethod('startsWith', String, String),
                        'Returns true if this string starts with the specified prefix.'),
                new UelMapping('str:endstring', UelFunctions.getMethod('endString', String, int),
                        'Returns a new string that is a substring of this string. The substring begins with the' +
                                ' character at the specified index and extends to the end of this string.'),
                new UelMapping('str:substring', UelFunctions.getMethod('subString', String, int, int),
                        'Returns a new string that is a substring of this string. The substring begins at the ' +
                                'specified beginIndex and extends to the character at index endIndex - 1. Thus ' +
                                'the length of the substring is endIndex-beginIndex.'),
                new UelMapping('str:toString', UelFunctions.getMethod('toString', Object),
                        'Converts all of the characters in this String to lower case using the rules of the default locale.'),
                new UelMapping('str:toLowerCase', UelFunctions.getMethod('toLowerCase', String),
                        'Converts Object to a String - bypassing localization.'),
                new UelMapping('str:toUpperCase', UelFunctions.getMethod('toUpperCase', String),
                        'Converts all of the characters in this String to upper case using the rules of the default locale.'),
                new UelMapping('str:trim', UelFunctions.getMethod('trim', String),
                        'Returns a copy of the string, with leading and trailing whitespace omitted.')
        ]
    }

}
