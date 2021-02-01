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

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.NumberModel;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;

/**
 * OfbizCurrencyTransform - Freemarker Transform for content links
 */
public class OfbizCurrencyTransform implements TemplateTransformModel {

    private static final String MODULE = OfbizCurrencyTransform.class.getName();

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

    private static BigDecimal getAmount(Map<String, Object> args, String key) {
        if (args.containsKey(key)) {
            Object o = args.get(key);

            // handle nulls better
            if (o == null) {
                o = 0.00;
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Amount Object : " + o.getClass().getName(), MODULE);
            }

            if (o instanceof SimpleScalar) {
                SimpleScalar s = (SimpleScalar) o;
                return new BigDecimal(s.getAsString());
            }
            return new BigDecimal(o.toString());
        }
        return BigDecimal.ZERO;
    }

    private static Integer getInteger(Map<String, Object> args, String key) {
        if (args.containsKey(key)) {
            Object o = args.get(key);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Amount Object : " + o.getClass().getName(), MODULE);
            }

            // handle nulls better
            if (o == null) {
                return null;
            }

            if (o instanceof NumberModel) {
                NumberModel s = (NumberModel) o;
                return s.getAsNumber().intValue();
            }
            if (o instanceof SimpleNumber) {
                SimpleNumber s = (SimpleNumber) o;
                return s.getAsNumber().intValue();
            }
            if (o instanceof SimpleScalar) {
                SimpleScalar s = (SimpleScalar) o;
                return Integer.valueOf(s.getAsString());
            }
            return Integer.valueOf(o.toString());
        }
        return null;
    }

    @Override
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final StringBuilder buf = new StringBuilder();

        Map<String, Object> arguments = UtilGenerics.cast(args);
        final BigDecimal amount = OfbizCurrencyTransform.getAmount(arguments, "amount");
        final String isoCode = OfbizCurrencyTransform.getArg(arguments, "isoCode");
        final String locale = OfbizCurrencyTransform.getArg(arguments, "locale");

        // check the rounding -- DEFAULT is 10 to not round for display, only use this when necessary
        // rounding should be handled by the code, however some times the numbers are coming from
        // someplace else (i.e. an integration)
        Integer roundingNumber = getInteger(arguments, "rounding");
        String scaleEnabled = "N";
        Environment env = Environment.getCurrentEnvironment();
        BeanModel req = null;
        try {
            req = (BeanModel) env.getVariable("request");
        } catch (TemplateModelException e) {
            Debug.logError(e.getMessage(), MODULE);
        }
        if (req != null) {
            HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            // Get rounding from SystemProperty
            if (UtilValidate.isNotEmpty(delegator)) {
                scaleEnabled = EntityUtilProperties.getPropertyValue("number", "currency.scale.enabled", "N", delegator);
                if (UtilValidate.isEmpty(roundingNumber)) {
                    String roundingString = EntityUtilProperties.getPropertyValue("number", "currency.rounding.default", "10", delegator);
                    if (UtilValidate.isInteger(roundingString)) roundingNumber = Integer.parseInt(roundingString);
                }
            }
        }
        if (roundingNumber == null) roundingNumber = 10;
        if ("Y".equals(scaleEnabled)) {
            if (amount.stripTrailingZeros().scale() <= 0) {
                roundingNumber = 0;
            }
        }
        final int rounding = roundingNumber;

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
                        Debug.logVerbose("parms: " + amount + " " + isoCode + " " + locale, MODULE);
                    }
                    if (locale.length() < 1) {
                        // Load the locale from the session
                        Environment env = Environment.getCurrentEnvironment();
                        BeanModel req = (BeanModel) env.getVariable("request");
                        if (req != null) {
                            HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                            out.write(UtilFormatOut.formatCurrency(amount, isoCode, UtilHttp.getLocale(request), rounding));
                            // we set the max to 10 digits as an hack to not round numbers in the ui
                        } else {
                            out.write(UtilFormatOut.formatCurrency(amount, isoCode, env.getLocale(), rounding));
                            // we set the max to 10 digits as an hack to not round numbers in the ui
                        }
                    } else {
                        out.write(UtilFormatOut.formatCurrency(amount, isoCode, new Locale(locale), rounding));
                        // we set the max to 10 digits as an hack to not round numbers in the ui
                    }
                } catch (TemplateModelException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }
}
