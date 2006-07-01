<%--
 *  Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
 
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 *
 * @author David E. Jones (jonesde@ofbiz.org)
 * @version 1.0
--%><%@ page import="java.io.*, java.util.*, java.net.*, org.w3c.dom.*, org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.entity.condition.*, org.ofbiz.entity.util.*, org.ofbiz.base.util.*, org.ofbiz.entity.model.*" %><%@ taglib uri="ofbizTags" prefix="ofbiz" %><jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" /><jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" /><%
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
            writer.println("</entity-engine-xml>");
          }
          
      } else {%>
ERROR: No entityName list was found in the session, go back to the export page and try again.
    <%}%>
<%} else {%>
  ERROR: You do not have permission to use this page (ENTITY_MAINT needed)
<%}%>
