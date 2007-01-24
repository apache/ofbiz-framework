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

<%@ page import="java.util.*, java.net.*,
                 org.ofbiz.base.util.cache.UtilCache" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />

<div class="head1">Entity Engine Performance Tests</div>

<div class="tabletext">NOTE: These performance results may vary a great deal for different
databases, JDBC drivers, JTA implementations (transaction managers), connection pools, 
local vs. remote deployment configurations, and hardware (app server hardware, database 
server hardware, network connections).</div>

<br/>
<%if(security.hasPermission("ENTITY_MAINT", session)) {%>

<%double startTime, totalTime, callsPerSecond;%>
<%int calls;%>
<table width="100%" border="1" cellspacing="0" cellpadding="2">
  <tr>
    <td><div class="tabletext"><b>Operation</b></div></td>
    <td><div class="tabletext"><b>Entity</b></div></td>
    <td><div class="tabletext"><b>Calls</b></div></td>
    <td><div class="tabletext"><b>Seconds</b></div></td>
    <td><div class="tabletext"><b>Seconds/Call</b></div></td>
    <td><div class="tabletext"><b>Calls/Second</b></div></td>
  </tr>
  <%
    calls = 1000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { GenericValue dummy = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "GZ-1000")); }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">findByPrimaryKey</div></td>
    <td><div class="tabletext">Large:Product</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>
  <%
    calls = 10000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { GenericValue dummy = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", "GZ-1000")); }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">findByPrimaryKeyCache</div></td>
    <td><div class="tabletext">Large:Product</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>
  <%
    calls = 1000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { GenericValue dummy = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", "_NA_")); }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">findByPrimaryKey</div></td>
    <td><div class="tabletext">Small:Party</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>
  <%
    calls = 10000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { GenericValue dummy = delegator.findByPrimaryKeyCache("Party", UtilMisc.toMap("partyId", "_NA_")); }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">findByPrimaryKeyCache</div></td>
    <td><div class="tabletext">Small:Party</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>
  <%
  	List createTestList = new ArrayList();
    calls = 1000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { 
        GenericValue dummy = delegator.makeValue("Product", UtilMisc.toMap("autoCreateKeywords", "N", "description", "Initial Description", "internalName", "Auto-Test Name", "productId", "_~WRITE_TEST~_" + i)); 
        createTestList.add(dummy); 
        delegator.create(dummy);
    }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">create</div></td>
    <td><div class="tabletext">Large:Product</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>  
  <%
    calls = 1000; startTime = (double) System.currentTimeMillis();    
    for (int i=0; i < calls; i++) { 
        GenericValue dummy = (GenericValue) createTestList.get(i); 
        dummy.set("description", "This was a test from the performace JSP");
        dummy.store();
    }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">update</div></td>
    <td><div class="tabletext">Large:Product</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>    
  <%
    calls = 1000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { 
        GenericValue dummy = (GenericValue) createTestList.get(i); 
        dummy.remove();
    }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">remove</div></td>
    <td><div class="tabletext">Large:Product</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>      
  <%
    calls = 100000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { Map ptyMap = new HashMap(); ptyMap.put("partyId", "_NA_"); }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">new HashMap</div></td>
    <td><div class="tabletext">N/A</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>
  <%
    calls = 100000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { Map ptyMap = UtilMisc.toMap("partyId", "_NA_"); }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">UtilMisc.toMap</div></td>
    <td><div class="tabletext">N/A</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>
  <%
    UtilCache utilCache = new UtilCache("test-cache", 0,0, false);
    utilCache.put("testName", "testValue");
    calls = 1000000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { utilCache.get("testName"); }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">UtilCache.get(String) - basic settings</div></td>
    <td><div class="tabletext">N/A</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>
  <%
    GenericPK testPk = delegator.makePK("Party", UtilMisc.toMap("partyId", "_NA_"));
    utilCache.put(testPk, "testValue");
    calls = 1000000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { utilCache.get(testPk); }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">UtilCache.get(GenericPK) - basic settings</div></td>
    <td><div class="tabletext">N/A</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>
  <%
    calls = 1000000; startTime = (double) System.currentTimeMillis();
    for (int i=0; i < calls; i++) { utilCache.put(testPk, "testValue"); }
    totalTime = (double) System.currentTimeMillis() - startTime;
    callsPerSecond = (double) calls / (totalTime/1000);
  %>
  <tr>
    <td><div class="tabletext">UtilCache.put(GenericPK) - basic settings</div></td>
    <td><div class="tabletext">N/A</div></td>
    <td><div class="tabletext"><%=calls%></div></td>
    <td><div class="tabletext"><%=totalTime/1000%></div></td>
    <td><div class="tabletext"><%=1/callsPerSecond%></div></td>
    <td><div class="tabletext"><%=callsPerSecond%></div></td>
  </tr>
</table>

<%}else{%>

ERROR: You do not have permission to use this page (ENTITY_MAINT needed)

<%}%>
