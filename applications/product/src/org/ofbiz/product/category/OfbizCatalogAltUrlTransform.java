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
package org.ofbiz.product.category;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.UtilValidate;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.NumberModel;
import freemarker.ext.beans.StringModel;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

public class OfbizCatalogAltUrlTransform implements TemplateTransformModel {
    public final static String module = OfbizCatalogUrlTransform.class.getName();

    @SuppressWarnings("unchecked")
    public String getStringArg(Map args, String key) {
        Object o = args.get(key);
        if (o instanceof SimpleScalar) {
            return ((SimpleScalar) o).getAsString();
        } else if (o instanceof StringModel) {
            return ((StringModel) o).getAsString();
        } else if (o instanceof SimpleNumber) {
            return ((SimpleNumber) o).getAsNumber().toString();
        } else if (o instanceof NumberModel) {
            return ((NumberModel) o).getAsNumber().toString();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Writer getWriter(final Writer out, final Map args)
            throws TemplateModelException, IOException {
        final StringBuilder buf = new StringBuilder();
        return new Writer(out) {
            
            public void write(char[] cbuf, int off, int len) throws IOException {
                buf.append(cbuf, off, len);
            }
            
            public void flush() throws IOException {
                out.flush();
            }
            
            public void close() throws IOException {
                try {
                Environment env = Environment.getCurrentEnvironment();
                BeanModel req = (BeanModel) env.getVariable("request");
                if (req != null) {
                    String previousCategoryId = getStringArg(args, "previousCategoryId");
                    String productCategoryId = getStringArg(args, "productCategoryId");
                    String productId = getStringArg(args, "productId");
                    
                    HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                    String url = "";
                    if (UtilValidate.isNotEmpty(productId)) {
                        url = CatalogUrlFilter.makeProductUrl(request, previousCategoryId, productCategoryId, productId);
                    } else {
                        String viewSize = getStringArg(args, "viewSize");
                        String viewIndex = getStringArg(args, "viewIndex");
                        String viewSort = getStringArg(args, "viewSort");
                        String searchString = getStringArg(args, "searchString");
                        url = CatalogUrlFilter.makeCategoryUrl(request, previousCategoryId, productCategoryId, productId, viewSize, viewIndex, viewSort, searchString);
                    }
                    out.write(url);
                }
                } catch (TemplateModelException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }
}
