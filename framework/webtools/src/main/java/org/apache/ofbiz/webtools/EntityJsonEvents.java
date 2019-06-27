/*
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
 */
package org.apache.ofbiz.webtools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.security.Security;

public class EntityJsonEvents {

    public static final String module = EntityJsonEvents.class.getName();
    public static final String err_resource = "WebtoolsErrorUiLabels";

    public static String downloadJsonData(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        ServletContext application = session.getServletContext();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Security security = (Security) request.getAttribute("security");
        boolean isFirst = true;
        if (security.hasPermission("ENTITY_MAINT", session)) {
            TreeSet passedEntityNames = (TreeSet) session.getAttribute("jsonrawdump_entitylist");
            session.removeAttribute("jsonrawdump_entitylist");
            EntityExpr entityDateCond = (EntityExpr) session.getAttribute("entityDateCond");
            session.removeAttribute("entityDateCond");
            try {
                if (passedEntityNames != null) {

                    ModelReader reader = delegator.getModelReader();
                    Collection ec = reader.getEntityNames();
                    TreeSet entityNames = new TreeSet(ec);

                    long numberWritten = 0;
                    byte[] outputByte = new byte[4096];

                    response.setContentType("text/plain;charset=UTF-8");
                    response.setHeader("Content-Disposition", "attachment; filename=DownloadEntityData.json");

                    if (passedEntityNames.size() > 0) {
                        StringBuilder textBuilder = new StringBuilder();
                        textBuilder.append("[");

                        boolean beganTransaction = false;
                        try {
                            beganTransaction = TransactionUtil.begin();

                            Iterator i = passedEntityNames.iterator();
                            while (i.hasNext()) {
                                String curEntityName = (String) i.next();

                                ModelEntity me = reader.getModelEntity(curEntityName);
                                EntityListIterator values = null;
                                if (me.getNoAutoStamp() == true) {
                                    values = delegator.find(curEntityName, null, null, null, null, null);
                                } else {
                                    values = delegator.find(curEntityName, entityDateCond, null, null, null, null);
                                }

                                GenericValue value = null;
                                if(!isFirst) {
                                    textBuilder.append(',');
                                }
                                textBuilder.append('{');
                                textBuilder.append("\"");
                                textBuilder.append(curEntityName);
                                textBuilder.append("\"");
                                textBuilder.append(":");
                                textBuilder.append("\n\t");
                                textBuilder.append("[");
                                int numberOfValues = 0;
                                while ((value = (GenericValue) values.next()) != null) {
                                    EntityJsonHelper.writeJsonText(textBuilder, value);
                                    numberWritten++;
                                    numberOfValues++;
                                    if (numberOfValues < values.getResultsSizeAfterPartialList()) {
                                        textBuilder.append(",");
                                    }
                                    textBuilder.append("\n\t");
                                }
                                textBuilder.append("]");
                                textBuilder.append("\n");
                                textBuilder.append("}");
                                values.close();
                                isFirst = false;
                            }
                            //TransactionUtil.commit(beganTransaction);
                        } catch (GenericEntityException e) {
                            String errMsg = "Failure in operation, rolling back transaction";
                            Debug.logError(e, errMsg, module);
                            try {
                                // only rollback the transaction if we started one...
                                TransactionUtil.rollback(beganTransaction, errMsg, e);
                            } catch (GenericEntityException e2) {
                                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
                            }
                            // after rolling back, rethrow the exception
                            //throw e;
                            return "error";
                        } finally {
                            // only commit the transaction if we started one... this will throw an exception if it fails
                            if (beganTransaction) {
                                try {
                                    TransactionUtil.commit(beganTransaction);
                                } catch (GenericEntityException e2) {
                                    Debug.logError(e2, "Could not commit transaction: " + e2.toString(), module);
                                    request.setAttribute("_ERROR_MESSAGE_", "Could not commit transaction: " + e2.toString());
                                    return "error";
                                }
                            }
                        }

                        textBuilder.append("]");
                        String text = textBuilder.toString();
                        PrintWriter writer = response.getWriter();
                        writer.write(text);
                        writer.flush();
                        writer.close();
                    }
                } else {
                    String errMsg = "No entityName list was found in the session, go back to the export page and try again.";
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }
            } catch (GeneralException e) {
                String errMsg = "Error downloading json data: " + e.toString();
                Debug.logError(e, errMsg, module);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            } catch (IOException e) {
                String errMsg = "Error downloading json data : " + e.toString();
                Debug.logError(e, errMsg, module);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } else {
            String errMsg = "You do not have permission to use this page (ENTITY_MAINT needed)";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }
}
