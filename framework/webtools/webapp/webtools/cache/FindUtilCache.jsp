<%--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
--%>

<%@ page import="java.util.*, java.net.*,
                 org.ofbiz.base.util.cache.UtilCache" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />

<%boolean hasUtilCacheEdit = security.hasPermission("UTIL_CACHE_EDIT", session);%>
<%Runtime rt = Runtime.getRuntime();%>

<h3 style="margin:0;">Cache Maintenance Page</h3>

<%if(security.hasPermission("UTIL_CACHE_VIEW", session)){%>

<div width="100%">
    <a href="<ofbiz:url>/FindUtilCache</ofbiz:url>" class="buttontext">Reload Cache List</a>
    <a href="<ofbiz:url>/FindUtilCacheClearAll</ofbiz:url>" class="buttontext">Clear All Caches</a>
    <a href="<ofbiz:url>/FindUtilCacheClearAllExpired</ofbiz:url>" class="buttontext">Clear Expired From All</a>
    <a href="<ofbiz:url>/ForceGarbageCollection</ofbiz:url>" class="buttontext">Run GC</a>
</div>
<div class="tabletext"><u><b>Memory:</b></u> [<b>TOTAL:</b> <%=rt.totalMemory()%>] [<b>FREE:</b> <%=rt.freeMemory()%>] [<b>USED:</b> <%=(rt.totalMemory() - rt.freeMemory())%>] [<b>MAX:</b> <%=rt.maxMemory()%>]</span></div>

<br/>
<table border="0" cellpadding="2" cellspacing="2">
<%
  String rowColor1 = "viewManyTR2";
  String rowColor2 = "viewManyTR1";
  String rowColor = "";
%>
  <tr class="viewOneTR1">
    <td>Cache&nbsp;Name</td>
    <td>size</td>
    <td>hits</td>
    <td>misses/NF/EXP/SR</td>
    <td>removes:H/M</td>
    <td>maxSize</td>
    <td>expireTime</td>
    <td>useSoftRef?</td>
    <td>useFileStore?</td>
    <td colspan="3">Administration</td>
  </tr>

  <%TreeSet names = new TreeSet(UtilCache.utilCacheTable.keySet());%>
  <%Iterator nameIter = names.iterator();%>
  <%if(nameIter!=null && nameIter.hasNext()){%>
    <%while(nameIter.hasNext()){%>
      <%String cacheName = (String)nameIter.next();%>
      <%UtilCache utilCache = (UtilCache)UtilCache.utilCacheTable.get(cacheName);%>
      <%rowColor=(rowColor==rowColor1?rowColor2:rowColor1);%>
      <tr class="<%=rowColor%>">
        <td><%=UtilFormatOut.checkNull(utilCache.getName())%></td>
        <td><%=UtilFormatOut.formatQuantity(utilCache.size())%></td>
        <td><%=UtilFormatOut.formatQuantity(utilCache.getHitCount())%></td>
        <td><%=UtilFormatOut.formatQuantity(utilCache.getMissCountTotal())%>/<%=UtilFormatOut.formatQuantity(utilCache.getMissCountNotFound())%>/<%=UtilFormatOut.formatQuantity(utilCache.getMissCountExpired())%>/<%=UtilFormatOut.formatQuantity(utilCache.getMissCountSoftRef())%></td>
        <td><%=UtilFormatOut.formatQuantity(utilCache.getRemoveHitCount())%>/<%=UtilFormatOut.formatQuantity(utilCache.getRemoveMissCount())%></td>
        <td><%=UtilFormatOut.formatQuantity(utilCache.getMaxSize())%></td>
        <td><%=UtilFormatOut.formatQuantity(utilCache.getExpireTime())%></td>
        <td><%=(new Boolean(utilCache.getUseSoftReference())).toString()%></td>
        <td><%=(new Boolean(utilCache.getUseFileSystemStore())).toString()%></td>
        
        <td align="center" valign=middle>
          <a href="<ofbiz:url>/FindUtilCacheElements?UTIL_CACHE_NAME=<%=UtilFormatOut.checkNull(utilCache.getName())%></ofbiz:url>" class="buttontext">Elements</a>
        </td>
        <td align="center" valign=middle>
          <%if(hasUtilCacheEdit){%>
            <a href="<ofbiz:url>/EditUtilCache?UTIL_CACHE_NAME=<%=UtilFormatOut.checkNull(utilCache.getName())%></ofbiz:url>" class="buttontext">Edit</a>
          <%}%>
        </td>
        <td align="center" valign=middle>
          <%if(hasUtilCacheEdit){%>
            <a href="<ofbiz:url>/FindUtilCacheClear?UTIL_CACHE_NAME=<%=UtilFormatOut.checkNull(utilCache.getName())%></ofbiz:url>" class="buttontext">Clear</a>
          <%}%>
        </td>
      </tr>
    <%}%>
  <%}else{%>
      <%rowColor=(rowColor==rowColor1?rowColor2:rowColor1);%><tr bgcolor="<%=rowColor%>">
        <td colspan="5">No UtilCache instances found.</td>
      </tr>
  <%}%>
</table>

<div>
    <a href="<ofbiz:url>/FindUtilCache</ofbiz:url>" class="buttontext">Reload Cache List</a>
    <a href="<ofbiz:url>/FindUtilCacheClearAll</ofbiz:url>" class="buttontext">Clear All Caches</a>
    <a href="<ofbiz:url>/FindUtilCacheClearAllExpired</ofbiz:url>" class="buttontext">Clear Expired From All</a>
</div>

<%}else{%>
  <h3>You do not have permission to view this page (UTIL_CACHE_VIEW needed).</h3>
<%}%>
