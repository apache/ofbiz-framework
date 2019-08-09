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
package org.apache.ofbiz.webapp.ftl;

import java.io.Writer;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;

/**
 * OfbizAmountTransform - Freemarker Transform for content links
 * This class is keep for backware compatibilty and call directly
 * OfbizNumberTransform with good arguments :
 *    * amout translate to number
 *    * format force to UtilFormatOut.AMOUNT_FORMAT
 */
public class OfbizAmountTransform extends OfbizNumberTransform {

    public static final String module = OfbizAmountTransform.class.getName();

    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        Map<String, Object> arguments = UtilGenerics.cast(args);
        arguments.put("format", UtilFormatOut.AMOUNT_FORMAT);
        arguments.put("number", args.get("amount"));
        return super.getWriter(out, arguments);
    }
}
