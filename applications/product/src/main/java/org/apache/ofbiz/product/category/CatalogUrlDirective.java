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
package org.apache.ofbiz.product.category;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.UtilGenerics;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.utility.DeepUnwrap;

/**
 * CatalogUrlDirective - Freemarker Template Directive for generating URLs suitable for use by the CatalogUrlServlet
 * 
 * Accepts the following arguments (see CatalogUrlServlet for their definition):
 * productId
 * currentCategoryId
 * previousCategoryId
 * 
 */
public class CatalogUrlDirective implements TemplateDirectiveModel {

    public final static String module = CatalogUrlDirective.class.getName();

    @Override
    public void execute(Environment env, Map args, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, TemplateModel> params = UtilGenerics.checkMap(args);
        String productId = (String) DeepUnwrap.unwrap(params.get("productId"));
        String currentCategoryId = (String) DeepUnwrap.unwrap(params.get("currentCategoryId"));
        String previousCategoryId = (String) DeepUnwrap.unwrap(params.get("previousCategoryId"));

        BeanModel req = (BeanModel) env.getVariable("request");

        if (req != null) {
            HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
            env.getOut().write(CatalogUrlServlet.makeCatalogUrl(request, productId, currentCategoryId, previousCategoryId));
        }
    }
}
