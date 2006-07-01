/*
 * $Id: OfbizAmountTransform.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilHttp;

/**
 * OfbizAmountTransform - Freemarker Transform for content links
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:tiz@sastau.it">Jacopo Cappellato</a>
 * @version    $Rev$
 */
public class OfbizAmountTransform implements TemplateTransformModel {

    public static final String module = OfbizAmountTransform.class.getName();
    public static final String SPELLED_OUT_FORMAT = "spelled-out";
    
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

    private static Double getAmount(Map args, String key) {
        if (args.containsKey(key)) {
            Object o = args.get(key);
            if (Debug.verboseOn()) Debug.logVerbose("Amount Object : " + o.getClass().getName(), module);

            // handle nulls better
            if (o == null) {
                o = new Double(0.00);
            }

            if (o instanceof NumberModel) {
                NumberModel s = (NumberModel) o;
                return new Double( s.getAsNumber().doubleValue() );
            }
            if (o instanceof SimpleNumber) {
                SimpleNumber s = (SimpleNumber) o;
                return new Double( s.getAsNumber().doubleValue() );
            }
            if (o instanceof SimpleScalar) {
                SimpleScalar s = (SimpleScalar) o;
                return new Double( s.getAsString() );
            }
            return new Double( o.toString() );
        }
        return new Double(0.00);
    }
    
    public Writer getWriter(final Writer out, Map args) {
        final StringBuffer buf = new StringBuffer();

        final Double amount = OfbizAmountTransform.getAmount(args, "amount");
        final String locale = OfbizAmountTransform.getArg(args, "locale");
        final String format = OfbizAmountTransform.getArg(args, "format");

        return new Writer(out) {
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException { 
                try {
                    if (Debug.verboseOn()) Debug.logVerbose("parms: " + amount + " " + format + " " + locale, module);
                    Locale localeObj = null;
                    if (locale.length() < 1) {
                        // Load the locale from the session
                        Environment env = Environment.getCurrentEnvironment();
                        BeanModel req = (BeanModel) env.getVariable("request");
                        if (req != null) {
                            HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                            localeObj = UtilHttp.getLocale(request);
                        } else {
                            localeObj = env.getLocale();
                        }
                    } else {
                        localeObj = new Locale(locale);
                    }
                    if (format.equals(OfbizAmountTransform.SPELLED_OUT_FORMAT)) {
                        out.write(UtilFormatOut.formatSpelledOutAmount(amount.doubleValue(), localeObj));
                    } else {
                        out.write(UtilFormatOut.formatAmount(amount.doubleValue(), localeObj));
                    }
                } catch (TemplateModelException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }
}
