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
package org.ofbiz.birt.report.context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.birt.report.IBirtConstants;
import org.eclipse.birt.report.context.BirtContext;
import org.eclipse.birt.report.context.ViewerAttributeBean;
import org.ofbiz.base.util.UtilValidate;

public class OFBizBirtContext extends BirtContext {

    public final static String module = OFBizBirtContext.class.getName();

    public OFBizBirtContext(HttpServletRequest request,
            HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected void __init() {
        this.bean = (ViewerAttributeBean) request.getAttribute(IBirtConstants.ATTRIBUTE_BEAN);
        if (UtilValidate.isEmpty(bean)) {
            bean = new BirtViewerAttributeBean(request);
        }
        request.setAttribute(IBirtConstants.ATTRIBUTE_BEAN, bean);
    }
}
