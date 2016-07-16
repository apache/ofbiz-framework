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
package org.apache.ofbiz.base.conversion;

/** Converter loader interface. Applications implement this
 * interface to load their Java object converters.
 *
 */
public interface ConverterLoader {
    /** Create and register converters with the Java object type
     * conversion framework. If the converter extends one of the
     * converter abstract classes, then the converter will register
     * itself when an instance is created. Otherwise, call
     * {@link org.apache.ofbiz.base.conversion.Converters#registerConverter(Converter)}
     * with the <code>Converter</code> instance.
     *
     */
    public void loadConverters();
}
