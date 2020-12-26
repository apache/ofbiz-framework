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

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.NumberModel;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.entity.Delegator;

/**
 * OfbizAmountTransform - Freemarker Transform for content links
 */
public class OfbizNumberTransform implements TemplateTransformModel {

    private static final String MODULE = OfbizNumberTransform.class.getName();
    private String format = null;

    private static String getArg(Map<String, Object> args, String key) {
        String result = "";
        Object o = args.get(key);
        if (o != null) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Arg Object : " + o.getClass().getName(), MODULE);
            }
            if (o instanceof TemplateScalarModel) {
                TemplateScalarModel s = (TemplateScalarModel) o;
                try {
                    result = s.getAsString();
                } catch (TemplateModelException e) {
                    Debug.logError(e, "Template Exception", MODULE);
                }
            } else {
                result = o.toString();
            }
        }
        return result;
    }

    private static Double getNumber(Map<String, Object> args, String key) {
        if (args.containsKey(key)) {
            Object o = args.get(key);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Number Object : " + o.getClass().getName(), MODULE);
            }

            // handle nulls better
            if (o == null) {
                o = 0.00;
            }

            if (o instanceof NumberModel) {
                NumberModel s = (NumberModel) o;
                return s.getAsNumber().doubleValue();
            }
            if (o instanceof SimpleNumber) {
                SimpleNumber s = (SimpleNumber) o;
                return s.getAsNumber().doubleValue();
            }
            if (o instanceof SimpleScalar) {
                SimpleScalar s = (SimpleScalar) o;
                return Double.valueOf(s.getAsString());
            }
            return Double.valueOf(o.toString());
        }
        return 0.00;
    }

    @Override
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final StringBuilder buf = new StringBuilder();

        Map<String, Object> arguments = UtilGenerics.cast(args);
        final Double number = OfbizNumberTransform.getNumber(arguments, "number");
        final String locale = OfbizNumberTransform.getArg(arguments, "locale");
        final String format = OfbizNumberTransform.getArg(arguments, "format");

        return new Writer(out) {
            @Override
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                try {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("params: " + number + " " + format + " " + locale, MODULE);
                    }
                    Locale localeObj = null;
                    Delegator delegator = null;
                    // Load the locale from the session
                    Environment env = Environment.getCurrentEnvironment();
                    BeanModel req = (BeanModel) env.getVariable("request");
                    if (req != null) {
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                        delegator = (Delegator) request.getAttribute("delegator");
                        if (locale.length() < 1) {
                            localeObj = UtilHttp.getLocale(request);
                        } else {
                            localeObj = env.getLocale();
                        }
                    } else {
                        localeObj = new Locale(locale);
                    }
                    out.write(UtilFormatOut.formatNumber(number, format, delegator, localeObj));
                } catch (TemplateModelException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }
}
