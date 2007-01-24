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
--%>

<%@ page import="java.util.*, java.io.*, java.net.*, org.ofbiz.base.util.collections.OrderedSet" %>
<%@ page import="org.w3c.dom.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>
<%@ page import="org.ofbiz.entity.model.*, org.ofbiz.entity.util.*, org.ofbiz.entity.transaction.*, org.ofbiz.entity.condition.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />
<jsp:useBean id="dispatcher" type="org.ofbiz.service.LocalDispatcher" scope="request" />
<%
  String outpath = request.getParameter("outpath");
  String filename = request.getParameter("filename");
  String maxRecStr = request.getParameter("maxrecords");
  String entitySyncId = request.getParameter("entitySyncId");
  String[] entityName = request.getParameterValues("entityName");
  String entityFrom = request.getParameter("entityFrom");
  String entityThru = request.getParameter("entityThru");

  // get the max records per file setting and convert to a int
  int maxRecordsPerFile = 0;
  if (UtilValidate.isNotEmpty(maxRecStr)) {
      try {
          maxRecordsPerFile = Integer.parseInt(maxRecStr);
      } catch (Exception e) {
      }
  }

  Set passedEntityNames = new TreeSet();
  if (entityName != null && entityName.length > 0) {
    for(int inc=0; inc<entityName.length; inc++) {
      passedEntityNames.add(entityName[inc]);
    }
  }
  
  String preConfiguredSetName = request.getParameter("preConfiguredSetName");
  if ("Product1".equals(preConfiguredSetName)) {
    passedEntityNames = new OrderedSet();
    passedEntityNames.add("DataResource");
    passedEntityNames.add("Facility");
    passedEntityNames.add("ProdCatalog");
    passedEntityNames.add("Product");
    passedEntityNames.add("ProductCategory");
    passedEntityNames.add("ProductFeatureCategory");
    passedEntityNames.add("ProductFeatureType");
    passedEntityNames.add("ProductPriceRule");
    passedEntityNames.add("ProductPromo");
  } else if ("Product2".equals(preConfiguredSetName)) {
    passedEntityNames = new OrderedSet();
    passedEntityNames.add("Content");
    passedEntityNames.add("ElectronicText");
    passedEntityNames.add("FacilityLocation");
    passedEntityNames.add("ProdCatalogCategory");
    passedEntityNames.add("ProdCatalogRole");
    passedEntityNames.add("ProductAssoc");
    passedEntityNames.add("ProductAttribute");
    passedEntityNames.add("ProductCategoryMember");
    passedEntityNames.add("ProductCategoryRollup");
    passedEntityNames.add("ProductFacility");
    passedEntityNames.add("ProductFeature");
    passedEntityNames.add("ProductFeatureCategoryAppl");
    passedEntityNames.add("ProductKeyword");
    passedEntityNames.add("ProductPrice");
    passedEntityNames.add("ProductPriceAction");
    passedEntityNames.add("ProductPriceCond");
    passedEntityNames.add("ProductPromoCode");
    passedEntityNames.add("ProductPromoCategory");
    passedEntityNames.add("ProductPromoProduct");
    passedEntityNames.add("ProductPromoRule");
  } else if ("Product3".equals(preConfiguredSetName)) {
    passedEntityNames = new OrderedSet();
    passedEntityNames.add("ProdCatalogInvFacility");
    passedEntityNames.add("ProductContent");
    passedEntityNames.add("ProductFacilityLocation");
    passedEntityNames.add("ProductFeatureAppl");
    passedEntityNames.add("ProductFeatureDataResource");
    passedEntityNames.add("ProductFeatureGroup");
    passedEntityNames.add("ProductPriceChange");
    passedEntityNames.add("ProductPromoAction");
    passedEntityNames.add("ProductPromoCodeEmail");
    passedEntityNames.add("ProductPromoCodeParty");
    passedEntityNames.add("ProductPromoCond");
  } else if ("Product4".equals(preConfiguredSetName)) {
    passedEntityNames = new OrderedSet();
    passedEntityNames.add("InventoryItem");
    passedEntityNames.add("ProductFeatureCatGrpAppl");
    passedEntityNames.add("ProductFeatureGroupAppl");
  } else if ("CatalogExport".equals(preConfiguredSetName)) {
    passedEntityNames = new OrderedSet();
    passedEntityNames.add("ProdCatalogCategoryType");
    passedEntityNames.add("ProdCatalog");
    passedEntityNames.add("ProductCategoryType");
    passedEntityNames.add("ProductCategory");
    passedEntityNames.add("ProductCategoryRollup");
    passedEntityNames.add("ProdCatalogCategory");
    passedEntityNames.add("ProductFeatureType");
    passedEntityNames.add("ProductFeatureCategory");

    passedEntityNames.add("DataResource");
    passedEntityNames.add("Content");
    passedEntityNames.add("ElectronicText");

    passedEntityNames.add("ProductType");
    passedEntityNames.add("Product");
    passedEntityNames.add("ProductAttribute");
    passedEntityNames.add("GoodIdentificationType");
    passedEntityNames.add("GoodIdentification");
    passedEntityNames.add("ProductPriceType");
    passedEntityNames.add("ProductPrice");

    passedEntityNames.add("ProductPriceRule");
    passedEntityNames.add("ProductPriceCond");
    passedEntityNames.add("ProductPriceAction");
    //passedEntityNames.add("ProductPriceChange");

    passedEntityNames.add("ProductPromo");
    passedEntityNames.add("ProductPromoCode");
    passedEntityNames.add("ProductPromoCategory");
    passedEntityNames.add("ProductPromoProduct");
    passedEntityNames.add("ProductPromoRule");
    passedEntityNames.add("ProductPromoAction");
    passedEntityNames.add("ProductPromoCodeEmail");
    passedEntityNames.add("ProductPromoCodeParty");
    passedEntityNames.add("ProductPromoCond");

    passedEntityNames.add("ProductCategoryMember");
    passedEntityNames.add("ProductAssoc");
    passedEntityNames.add("ProductContent");

    passedEntityNames.add("ProductFeature");
    passedEntityNames.add("ProductFeatureCategoryAppl");
    passedEntityNames.add("ProductFeatureAppl");
    passedEntityNames.add("ProductFeatureDataResource");
    passedEntityNames.add("ProductFeatureGroup");
    passedEntityNames.add("ProductFeatureCatGrpAppl");
    passedEntityNames.add("ProductFeatureGroupAppl");

    //passedEntityNames.add("ProductKeyword");
  }

  if (UtilValidate.isNotEmpty(entitySyncId)) {
      passedEntityNames = org.ofbiz.entityext.synchronization.EntitySyncContext.getEntitySyncModelNamesToUse(dispatcher, entitySyncId);
  }
  boolean checkAll = "true".equals(request.getParameter("checkAll"));
  boolean tobrowser = request.getParameter("tobrowser")!=null?true:false;
  
  EntityExpr entityFromCond = null;
  EntityExpr entityThruCond = null;
  EntityExpr entityDateCond = null;
  if (UtilValidate.isNotEmpty(entityFrom)) {
    entityFromCond = new EntityExpr("lastUpdatedTxStamp", EntityComparisonOperator.GREATER_THAN, entityFrom);
  }
  if (UtilValidate.isNotEmpty(entityThru)) {
    entityThruCond = new EntityExpr("lastUpdatedTxStamp", EntityComparisonOperator.LESS_THAN, entityThru);
  }
  if ((entityFromCond!=null) && (entityThruCond!=null)) {
    entityDateCond = new EntityExpr(entityFromCond, EntityJoinOperator.AND, entityThruCond);
  } else if(entityFromCond!=null) {
    entityDateCond = entityFromCond;
  } else if(entityThruCond!=null) {
    entityDateCond = entityThruCond;
  }
  
%>
<%if (tobrowser) {
    session.setAttribute("xmlrawdump_entitylist", entityName);
    session.setAttribute("entityDateCond", entityDateCond);
%>   
    <div class="head1">XML Export from DataSource(s)</div>
    <div class="tabletext">This page can be used to export data from the database. The exported documents will have a root tag of "&lt;entity-engine-xml&gt;".</div>
    <hr/>
    <%if(security.hasPermission("ENTITY_MAINT", session)) {%>
        <a href="<ofbiz:url>/xmldsrawdump</ofbiz:url>" class="buttontext" target="_blank">Click Here to Get Data (or save to file)</a>
    <%} else {%>
      <div class="tabletext">You do not have permission to use this page (ENTITY_MAINT needed)</div>
    <%}%>
<%} else {%>
<%
  EntityFindOptions efo = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
  ModelReader reader = delegator.getModelReader();
  Collection ec = reader.getEntityNames();
  TreeSet entityNames = new TreeSet(ec);

  int numberOfEntities = passedEntityNames.size();
  long numberWritten = 0;
  
  // single file
  if(filename != null && filename.length() > 0 && numberOfEntities > 0) {
    PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8")));
    writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    writer.println("<entity-engine-xml>");

    Iterator i = passedEntityNames.iterator();
    while(i.hasNext()) { 
        boolean beganTransaction = TransactionUtil.begin(3600);
        try {
            String curEntityName = (String)i.next();
            EntityListIterator values = delegator.findListIteratorByCondition(curEntityName, entityDateCond, null, null, UtilMisc.toList("-createdTxStamp"), efo);

            GenericValue value = null;
            long curNumberWritten = 0;
            while ((value = (GenericValue) values.next()) != null) {
                value.writeXmlText(writer, "");
                numberWritten++;
                curNumberWritten++;
                if (curNumberWritten % 500 == 0 || curNumberWritten == 1) {
                    Debug.log("Records written [" + curEntityName + "]: " + curNumberWritten + " Total: " + numberWritten);
                }
            }
            values.close();
            Debug.log("Wrote [" + curNumberWritten + "] from entity : " + curEntityName);
            TransactionUtil.commit(beganTransaction);
        } catch (Exception e) {
            String errMsg = "Error reading data for XML export:";
            Debug.logError(e, errMsg, "JSP");
            TransactionUtil.rollback(beganTransaction, errMsg, e);
        }
    }
    writer.println("</entity-engine-xml>");
    writer.close();
    Debug.log("Total records written from all entities: " + numberWritten);
  }

  // multiple files in a directory
  Collection results = new ArrayList();
  int fileNumber = 1;

  if (outpath != null){
      File outdir = new File(outpath);
      if(!outdir.exists()){
          outdir.mkdir();
      }
      if(outdir.isDirectory() && outdir.canWrite()) {
        Iterator i= passedEntityNames.iterator();

        while(i.hasNext()) {
            numberWritten = 0;
            String curEntityName = (String)i.next();
            String fileName = preConfiguredSetName != null ? UtilFormatOut.formatPaddedNumber((long) fileNumber, 3) + "_" : "";
            fileName = fileName + curEntityName;

            EntityListIterator values = null;
            boolean beganTransaction = false;
            try{
                beganTransaction = TransactionUtil.begin(3600);
                
                ModelEntity me = delegator.getModelEntity(curEntityName);
                if (me instanceof ModelViewEntity) {
                    results.add("["+fileNumber +"] [vvv] " + curEntityName + " skipping view entity");
                    continue;
                }
                values = delegator.findListIteratorByCondition(curEntityName, entityDateCond, null, null, me.getPkFieldNames(), efo);
                boolean isFirst = true;
                PrintWriter writer = null;
                int fileSplitNumber = 1;
                GenericValue value = null;
                while ((value = (GenericValue) values.next()) != null) {
                    //Don't bother writing the file if there's nothing
                    //to put into it
                    if (isFirst) {
                        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outdir, fileName +".xml")), "UTF-8")));
                        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                        writer.println("<entity-engine-xml>");
                        isFirst = false;
                    }
                    value.writeXmlText(writer, "");
                    numberWritten++;

                    // split into small files
                    if ((maxRecordsPerFile > 0) && (numberWritten % maxRecordsPerFile == 0)) {
                        fileSplitNumber++;
                        // close the file
                        writer.println("</entity-engine-xml>");
                        writer.close();

                        // create a new file
                        String splitNumStr = UtilFormatOut.formatPaddedNumber((long) fileSplitNumber, 3);
                        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outdir, fileName + "_" + splitNumStr +".xml")), "UTF-8")));
                        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                        writer.println("<entity-engine-xml>");
                    }

                    if (numberWritten % 500 == 0 || numberWritten == 1) {
                       Debug.log("Records written [" + curEntityName + "]: " + numberWritten);
                    }

                }
                if (writer != null) {
                    writer.println("</entity-engine-xml>");
                    writer.close();
                    String thisResult = "["+fileNumber +"] [" + numberWritten + "] " + curEntityName + " wrote " + numberWritten + " records";
                    Debug.log(thisResult);
                    results.add(thisResult);
                } else {
                    String thisResult = "["+fileNumber +"] [---] " + curEntityName + " has no records, not writing file";
                    Debug.log(thisResult);
                    results.add(thisResult);
                }
                values.close();
            } catch (Exception ex) {
                if (values != null) {
                    values.close();
                }
                String thisResult = "["+fileNumber +"] [xxx] Error when writing " + curEntityName + ": " + ex;
                Debug.log(thisResult);
                results.add(thisResult);
                TransactionUtil.rollback(beganTransaction, thisResult, ex);
            } finally {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            }
            fileNumber++;
        }
    }
  }

%>
    <div class="head1">XML Export from DataSource(s)</div>
    <div class="tabletext">This page can be used to export data from the database. The exported documents will have a root tag of "&lt;entity-engine-xml&gt;".</div>
    <hr/>
    <%if(security.hasPermission("ENTITY_MAINT", session)) {%>
      <div class="head2">Results:</div>
    
      <%if(filename != null && filename.length() > 0 && numberOfEntities > 0) {%>
        <div class="tabletext">Wrote XML for all data in <%=numberOfEntities%> entities.</div>
        <div class="tabletext">Wrote <%=numberWritten%> records to XML file <%=filename%></div>
      <%} else if (outpath != null && numberOfEntities > 0) {%>
        <%Iterator re = results.iterator();%>
        <%while (re.hasNext()){%>
            <div class="tabletext"><%=(String)re.next()%> </div>
        <%}%>
      <%} else {%>
        <div class="tabletext">No filename specified or no entity names specified, doing nothing.</div>
      <%}%>
    
      <hr/>
    
      <div class="head2">Export:</div>
      <form method="post" action="<ofbiz:url>/xmldsdump</ofbiz:url>" name="entityExport">
        <div class="tabletext">Output Directory&nbsp;: <input type="text" class="inputBox" size="60" name="outpath" value="<%=UtilFormatOut.checkNull(outpath)%>"/></div>
        <div class="tabletext">Max Records Per File&nbsp;: <input type="text" class="inputBox" size="10" name="maxrecords"/></div>
        <div class="tabletext">Single Filename&nbsp;&nbsp;: <input type="text" class="inputBox" size="60" name="filename" value="<%=UtilFormatOut.checkNull(filename)%>"/></div>
        
        <div class="tabletext">Records Updated Since&nbsp;&nbsp;:
          <input type="text" class="inputBox" size="25" name="entityFrom" />
          <a href="javascript:call_cal(document.entityExport.entityFrom, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
        </div>
        <div class="tabletext">Records Updated Before&nbsp;&nbsp;:
          <input type="text" class="inputBox" size="25" name="entityThru" />
          <a href="javascript:call_cal(document.entityExport.entityThru, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
        </div>

        <div class="tabletext">OR Out to Browser: <input type="checkbox" name="tobrowser" <%=tobrowser?"checked":""%>></div>
        <br/>
        <div class="tabletext">Entity Names:</div>
        <input type="submit" value="Export"/>
        <a href="<ofbiz:url>/xmldsdump?checkAll=true</ofbiz:url>" class="buttontext">Check All</a>
        <a href="<ofbiz:url>/xmldsdump</ofbiz:url>" class="buttontext">Un-Check All</a>
        <div class="tabletext">Entity Sync Dump:
        <input name="entitySyncId" class="inputBox" size="30" value="<%=UtilFormatOut.checkNull(entitySyncId)%>"/>
        </div>
        Pre-configured set:
        <select name="preConfiguredSetName">
            <option value="">None</option>
            <option value="CatalogExport">Catalog Export</option>
            <option value="Product1">Product Part 1</option>
            <option value="Product2">Product Part 2</option>
            <option value="Product3">Product Part 3</option>
            <option value="Product4">Product Part 4</option>
        </select>
        <br/>

        <table>
          <tr>
            <%Iterator iter = entityNames.iterator();%>
            <%int entCount = 0;%>
            <%while(iter.hasNext()) {%>
              <%String curEntityName = (String)iter.next();%>
              <%if(entCount % 3 == 0) {%></TR><TR><%}%>
              <%entCount++;%>
              <%-- don't check view entities... --%>
              <%boolean check = checkAll;%>
              <%if (check) {%>
                <%ModelEntity curModelEntity = delegator.getModelEntity(curEntityName);%>
                <%if (curModelEntity instanceof ModelViewEntity) check = false;%>
              <%}%>
              <td><div class="tabletext"><input type="checkbox" name="entityName" value="<%=curEntityName%>" <%=check?"checked":""%>/><%=curEntityName%></div></td>
            <%}%>
          </tr>
        </table>
    
        <input type="submit" value="Export">
        <A href="<ofbiz:url>/xmldsdump?checkAll=true</ofbiz:url>" class="buttontext">Check All</A>
        <A href="<ofbiz:url>/xmldsdump</ofbiz:url>" class="buttontext">Un-Check All</A>
      </form>
    <%} else {%>
      <div class="tabletext">You do not have permission to use this page (ENTITY_MAINT needed)</div>
    <%}%>
<%}%>
