<%--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
--%><%@ page import="java.io.*, java.util.*, java.net.*, org.w3c.dom.*, org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.entity.condition.*, org.ofbiz.entity.util.*, org.ofbiz.base.util.*, org.ofbiz.entity.model.*, org.ofbiz.entity.transaction.*" %><%@ taglib uri="ofbizTags" prefix="ofbiz" %><jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" /><jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" /><%
  if(security.hasPermission("ENTITY_MAINT", session)) {
      String[] entityName = (String[]) session.getAttribute("xmlrawdump_entitylist");
      session.removeAttribute("xmlrawdump_entitylist");
      EntityExpr entityDateCond = (EntityExpr) session.getAttribute("entityDateCond");
      session.removeAttribute("entityDateCond");
      if (entityName != null) {

          ModelReader reader = delegator.getModelReader();
          Collection ec = reader.getEntityNames();
          TreeSet entityNames = new TreeSet(ec);

          int numberOfEntities = 0;
          long numberWritten = 0;

          response.setContentType("text/xml; charset=UTF-8");
          //UtilXml.writeXmlDocument(, document);

          if(entityName != null && entityName.length > 0) {
            TreeSet passedEntityNames = new TreeSet();
            for(int inc=0; inc<entityName.length; inc++) {
              passedEntityNames.add(entityName[inc]);
            }

            numberOfEntities = passedEntityNames.size();
            
            PrintWriter writer = null;
            ServletContext context = pageContext.getServletContext();
            if (UtilJ2eeCompat.useOutputStreamNotWriter(context)) {
                writer = new PrintWriter(response.getOutputStream(), true);
            } else {
                writer = response.getWriter();
            }

            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<entity-engine-xml>");

            boolean beganTransaction = false;
            try {
                beganTransaction = TransactionUtil.begin();

          		Iterator i = passedEntityNames.iterator();
            	while(i.hasNext()) { 
                    String curEntityName = (String)i.next();
                    EntityListIterator values = delegator.findListIteratorByCondition(curEntityName, entityDateCond, null, null);

                    GenericValue value = null;
                    while ((value = (GenericValue) values.next()) != null) {
                        value.writeXmlText(writer, "");
                        numberWritten++;
                    }
                    values.close();
                }
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                String errMsg = "Failure in operation, rolling back transaction";
                String module = "xmldsrawdump.jsp";
                Debug.logError(e, errMsg, module);
                try {
                    // only rollback the transaction if we started one...
                    TransactionUtil.rollback(beganTransaction, errMsg, e);
                } catch (GenericEntityException e2) {
                    Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
                }
                // after rolling back, rethrow the exception
                throw e;
            } finally {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            }

            writer.println("</entity-engine-xml>");
          }
          
      } else {%>
ERROR: No entityName list was found in the session, go back to the export page and try again.
    <%}%>
<%} else {%>
  ERROR: You do not have permission to use this page (ENTITY_MAINT needed)
<%}%>
