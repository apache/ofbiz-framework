<%@page contentType="text/html"%>
<!--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 * 
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
-->

<%@ page import="java.util.*" %>
<%@ page import="org.ofbiz.base.util.*" %>
<%@ page import="org.ofbiz.entity.*, org.ofbiz.entity.util.*, org.ofbiz.entity.datasource.*" %>
<%@ page import="org.ofbiz.entity.model.*, org.ofbiz.entity.jdbc.*" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />
<%String controlPath=(String)request.getAttribute("_CONTROL_PATH_");%>

<%
if(security.hasPermission("ENTITY_MAINT", session)) {
  boolean addMissing = "true".equals(request.getParameter("addMissing"));
  boolean checkFkIdx = "true".equals(request.getParameter("checkFkIdx"));
  boolean checkFks = "true".equals(request.getParameter("checkFks"));
  boolean checkPks = "true".equals(request.getParameter("checkPks"));
  boolean repair = "true".equals(request.getParameter("repair"));
  String option = request.getParameter("option");
  String groupName = request.getParameter("groupName");
  
  Iterator miter = null;
  if(groupName != null && groupName.length() > 0) {
    String helperName = delegator.getGroupHelperName(groupName);

    List messages = new LinkedList();
    //GenericHelper helper = GenericHelperFactory.getHelper(helperName);
    DatabaseUtil dbUtil = new DatabaseUtil(helperName);
    Map modelEntities = delegator.getModelEntityMapByGroup(groupName);
    Set modelEntityNames = new TreeSet(modelEntities.keySet());

    if ("checkupdatetables".equals(option)) {
        List fieldsToRepair = null;
        if (repair) {
            fieldsToRepair = new ArrayList();
        }
        dbUtil.checkDb(modelEntities, fieldsToRepair, messages, checkPks, checkFks, checkFkIdx, addMissing);
        if (fieldsToRepair != null && fieldsToRepair.size() > 0) {
            dbUtil.repairColumnSizeChanges(modelEntities, fieldsToRepair, messages);
        }
    } else if ("removetables".equals(option)) {
        Iterator modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
      	    String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.deleteTable(modelEntity, messages);
        }
    } else if ("removepks".equals(option)) {
        Iterator modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
            String modelEntityName = (String) modelEntityNameIter.next();
            ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.deletePrimaryKey(modelEntity, messages);
        }
    } else if ("createpks".equals(option)) {
        Iterator modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
            String modelEntityName = (String) modelEntityNameIter.next();
            ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.createPrimaryKey(modelEntity, messages);
        }
    } else if ("createfkidxs".equals(option)) {
        Iterator modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
      	    String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.createForeignKeyIndices(modelEntity, messages);
        }
    } else if ("removefkidxs".equals(option)) {
        Iterator modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
      	    String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.deleteForeignKeyIndices(modelEntity, messages);
        }
    } else if ("createfks".equals(option)) {
        Iterator modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
      	    String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.createForeignKeys(modelEntity, modelEntities, messages);
        }
    } else if ("removefks".equals(option)) {
        Iterator modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
      	    String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.deleteForeignKeys(modelEntity, modelEntities, messages);
        }
    } else if ("createidx".equals(option)) {
        Iterator modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
            String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.createDeclaredIndices(modelEntity, messages);
        }
    } else if ("removeidx".equals(option)) {
        Iterator modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
            String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.deleteDeclaredIndices(modelEntity, messages);
        }
    } else if ("updateCharsetCollate".equals(option)) {
        Iterator modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
            String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.updateCharacterSetAndCollation(modelEntity, messages);
        }
    }
    miter = messages.iterator();
  }
%>

<h3>Check/Update Database</h3>

<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>">
  <input type="hidden" name="option" value="checkupdatetables"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  &nbsp;<input type="checkbox" name="checkPks" value="true" checked="checked"/>&nbsp;pks
  &nbsp;<input type="checkbox" name="checkFks" value="true"/>&nbsp;fks
  &nbsp;<input type="checkbox" name="checkFkIdx" value="true"/>&nbsp;fk-idx
  &nbsp;<input type="checkbox" name="addMissing" value="true"/>&nbsp;Add Missing
  &nbsp;<input type="checkbox" name="repair" value="true"/>&nbsp;Repair Column Sizes
  <input type="submit" value="Check/Update"/>
</form>

<p>NOTE: Use the following at your own risk; make sure you know what you are doing before running these...</p>


<script language="JavaScript" type="text/javascript">
 <!--
     function enableTablesRemove() {
         document.forms["TablesRemoveForm"].elements["TablesRemoveButton"].disabled=false;
     }
 //-->
</script>

<h3>Remove All Tables</h3>
<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>" name="TablesRemoveForm">
  <input type="hidden" name="option" value="removetables"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  <input type="submit" value="Remove" name="TablesRemoveButton" disabled/>
  <input type="button" value="Enable" onClick="enableTablesRemove();"/>
</form>

<h3>Create/Remove All Primary Keys</h3>
<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>">
  <input type="hidden" name="option" value="createpks"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  <input type="submit" value="Create"/>
</form>
<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>">
  <input type="hidden" name="option" value="removepks"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  <input type="submit" value="Remove"/>
</form>

<h3>Create/Remove All Declared Indices</h3>
<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>">
  <input type="hidden" name="option" value="createidx"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  <input type="submit" value="Create"/>
</form>
<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>">
  <input type="hidden" name="option" value="removeidx"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  <input type="submit" value="Remove"/>
</form>

<h3>Create/Remove All Foreign Key Indices</h3>
<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>">
  <input type="hidden" name="option" value="createfkidxs"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  <input type="submit" value="Create"/>
</form>
<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>">
  <input type="hidden" name="option" value="removefkidxs"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  <input type="submit" value="Remove"/>
</form>

<h3>Create/Remove All Foreign Keys</h3>
<p>NOTE: Foreign keys may also be created in the Check/Update database operation if the check-fks-on-start and other options on the datasource element are setup to do so.</p>
<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>">
  <input type="hidden" name="option" value="createfks"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  <input type="submit" value="Create"/>
</form>
<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>">
  <input type="hidden" name="option" value="removefks"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  <input type="submit" value="Remove"/>
</form>

<h3>Update character-set and collate (based on settings on datasource in entityengine.xml)</h3>
<form method="post" action="<%=response.encodeURL(controlPath + "/view/checkdb")%>">
  <input type="hidden" name="option" value="updateCharsetCollate"/>
  Group Name: <input type="text" class="inputBox" name="groupName" value="<%=groupName!=null?groupName:"org.ofbiz"%>" size="40"/>
  <input type="submit" value="Update"/>
</form>

<hr>
<ul>
<%while (miter != null && miter.hasNext()) {%>
  <%String message = (String) miter.next();%>
  <li><%=message%></li>
<%}%>
</ul>
<%} else {%>
<h3>Entity Editor</h3>
<div>ERROR: You do not have permission to use this page (ENTITY_MAINT needed)</div>
<%}%>
