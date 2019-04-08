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

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.NumberModel;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 * OfbizCurrencyTransform - Freemarker Transform for content links
 */
public class OfbizCurrencyTransform implements TemplateTransformModel {

    public static final String module = OfbizCurrencyTransform.class.getName();

    private static String getArg(Map args, String key) {
        String  result = "";
        Object o = args.get(key);
        if (o != null) {
            if (Debug.verboseOn()) Debug.logVerbose("Arg Object : " + o.getClass().getName(), module);
            if (o instanceof TemplateScalarModel) {
                TemplateScalarModel s = (TemplateScalarModel) o;
                try {
                    result = s.getAsString();
                } catch (TemplateModelException e) {
                    Debug.logError(e, "Template Exception", module);
                }
            } else {
              result = o.toString();
            }
        }
        return result;
    }

    private static BigDecimal getAmount(Map args, String key) {
        if (args.containsKey(key)) {
            Object o = args.get(key);

            // handle nulls better
            if (o == null) {
                o = 0.00;
            }
            if (Debug.verboseOn()) Debug.logVerbose("Amount Object : " + o.getClass().getName(), module);

            if (o instanceof SimpleScalar) {
                SimpleScalar s = (SimpleScalar) o;
                return new BigDecimal(s.getAsString());
            }
            return new BigDecimal(o.toString());
        }
        return BigDecimal.ZERO;
    }

    private static Integer getInteger(Map args, String key) {
        if (args.containsKey(key)) {
            Object o = args.get(key);
            if (Debug.verboseOn()) Debug.logVerbose("Amount Object : " + o.getClass().getName(), module);

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

    public Writer getWriter(final Writer out, Map args) {
        final StringBuilder buf = new StringBuilder();

        final BigDecimal amount = OfbizCurrencyTransform.getAmount(args, "amount");
        final String isoCode = OfbizCurrencyTransform.getArg(args, "isoCode");
        final String locale = OfbizCurrencyTransform.getArg(args, "locale");

        // check the rounding -- DEFAULT is 10 to not round for display, only use this when necessary
        // rounding should be handled by the code, however some times the numbers are coming from
        // someplace else (i.e. an integration)
        Integer roundingNumber = getInteger(args, "rounding");
        String scaleEnabled = "N";
        Environment env = Environment.getCurrentEnvironment();
        BeanModel req = null;
        try {
            req = (BeanModel) env.getVariable("request");
        } catch (TemplateModelException e) {
            Debug.logError(e.getMessage(), module);
        }
        if (req != null) {
            HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            // Get rounding from SystemProperty
            if (UtilValidate.isNotEmpty(delegator)) {
                scaleEnabled = EntityUtilProperties.getPropertyValue("general", "currency.scale.enabled", "N", delegator);
                if (UtilValidate.isEmpty(roundingNumber)) {
                    String roundingString = EntityUtilProperties.getPropertyValue("general", "currency.rounding.default", "10", delegator);
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
                    if (Debug.verboseOn()) Debug.logVerbose("parms: " + amount + " " + isoCode + " " + locale, module);
                    if (locale.length() < 1) {
                        // Load the locale from the session
                        Environment env = Environment.getCurrentEnvironment();
                        BeanModel req = (BeanModel) env.getVariable("request");
                        if (req != null) {
                            HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                            out.write(UtilFormatOut.formatCurrency(amount, isoCode, UtilHttp.getLocale(request), rounding)); // we set the max to 10 digits as an hack to not round numbers in the ui
                        } else {
                            out.write(UtilFormatOut.formatCurrency(amount, isoCode, env.getLocale(), rounding)); // we set the max to 10 digits as an hack to not round numbers in the ui
                        }
                    } else {
                        out.write(UtilFormatOut.formatCurrency(amount, isoCode, new Locale(locale), rounding)); // we set the max to 10 digits as an hack to not round numbers in the ui
                    }
                } catch (TemplateModelException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }
}
