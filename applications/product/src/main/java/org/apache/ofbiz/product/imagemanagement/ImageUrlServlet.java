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
package org.apache.ofbiz.product.imagemanagement;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * ControlServlet.java - Master servlet for the web application.
 */
@SuppressWarnings("serial")
public class ImageUrlServlet extends HttpServlet {

    public static final String module = ImageUrlServlet.class.getName();

    public ImageUrlServlet() {
        super();
    }

    /**
     * @see javax.servlet.http.HttpServlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Delegator delegator = (Delegator) getServletContext().getAttribute("delegator");

        String pathInfo = request.getPathInfo();
        List<String> pathElements = StringUtil.split(pathInfo, "/");
        
        List<String> tagElements = new LinkedList<String>();
        for (String pathElement : pathElements) {
            tagElements.addAll(StringUtil.split(pathElement, "-"));
        }
        
        String lastTagElement = tagElements.get(tagElements.size() - 1);
        String contentId = lastTagElement.substring(0, lastTagElement.lastIndexOf("."));
        String sizeTagElement = null;
        if(tagElements.size() > 2){
            sizeTagElement = tagElements.get(tagElements.size() - 2);
        }
        
        GenericValue content = null;
        try {
            GenericValue contentResult = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            if (contentResult == null) {
                content = EntityQuery.use(delegator).from("Content").where("contentId", sizeTagElement).queryOne();
            } else {
                content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
    
        if (content != null) {
            GenericValue dataResource = null;
            try {
                dataResource = content.getRelatedOne("DataResource", false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            String imageUrl = dataResource.getString("objectInfo");
            RequestDispatcher rd = request.getRequestDispatcher("/control/viewImage?drObjectInfo=" + imageUrl);
            rd.forward(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found with ID [" + contentId + "]");
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#destroy()
     */
    @Override
    public void destroy() {
        super.destroy();
    }

}
