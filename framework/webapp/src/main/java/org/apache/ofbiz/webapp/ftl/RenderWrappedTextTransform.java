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

import static org.apache.ofbiz.base.util.UtilGenerics.checkMap;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * RenderWrappedTextTransform - Freemarker Transform for URLs (links)
 */
public class RenderWrappedTextTransform implements  TemplateTransformModel {

    public static final String module = RenderWrappedTextTransform.class.getName();

    @Override
    public Writer getWriter(final Writer out, @SuppressWarnings("rawtypes") Map args) {
        final Environment env = Environment.getCurrentEnvironment();
        Map<String, Object> ctx = checkMap(FreeMarkerWorker.getWrappedObject("context", env), String.class, Object.class);
        final String wrappedFTL = FreeMarkerWorker.getArg(checkMap(args, String.class, Object.class), "wrappedFTL", ctx);

        return new Writer(out) {

            @Override
            public void write(char cbuf[], int off, int len) {
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                if (UtilValidate.isNotEmpty(wrappedFTL)) {
                        out.write(wrappedFTL);
                } else {
                    Debug.logInfo("wrappedFTL was empty. skipping write.", module);
                }
            }
        };
    }
}
