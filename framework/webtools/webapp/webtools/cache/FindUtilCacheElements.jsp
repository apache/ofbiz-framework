<%
/**
 *  Title: UtilCache Maintenance Page
 *  Description: None
 *  Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
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
 *@author     David E. Jones
 *@created    May 28 2001
 *@version    1.0
 */
%> 

<%@ page import="java.util.*, java.net.*,
                 org.ofbiz.base.util.cache.UtilCache,
                 org.ofbiz.base.util.cache.CacheLine" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />

<%boolean hasUtilCacheEdit=security.hasPermission("UTIL_CACHE_EDIT", session);%>
<%String cacheName=request.getParameter("UTIL_CACHE_NAME");%>
<%long totalSize = 0;%>

<h3>Cache Element Maintenance Page</h3>

<%if (security.hasPermission("UTIL_CACHE_VIEW", session)) {%>
  <%if (cacheName!=null) {%>
   <%UtilCache utilCache = (UtilCache)UtilCache.utilCacheTable.get(cacheName);%>
   <%if (utilCache!=null) {%>
    <div class="tabletext"><b>Cache Name:</b>&nbsp;<%=cacheName%> (<%=(new Date()).toString()%>)</div>
    <div style="margin-top: 4px; margin-bottom: 4px;"><a href="<ofbiz:url>/FindUtilCache</ofbiz:url>" class="buttontext">Back to Cache Maintenance</a></div>
    <table border="0" cellpadding="2" cellspacing="2">
    <%
      String rowColor1 = "viewManyTR2";
      String rowColor2 = "viewManyTR1";
      String rowColor = "";
    %>
      <tr class="viewOneTR1">
        <td>Cache&nbsp;Element&nbsp;Key</td>
        <%-- <td>createTime</td> --%>
        <td>expireTime</td>
        <td>bytes</td>
        <td></td>
      </tr>

          <%Iterator iter = utilCache.cacheLineTable.keySet().iterator();%>
          <%if(iter!=null && iter.hasNext()){%>
            <%int keyNum = 0;%>
            <%while(iter.hasNext()){%>
              <%Object key = iter.next();%>
              <%CacheLine line = (CacheLine) utilCache.cacheLineTable.get(key);%>
              <%rowColor=(rowColor==rowColor1?rowColor2:rowColor1);%>
              <tr class="<%=rowColor%>">
                <td><%=key%></td>
                <td>
                  <%long expireTime = utilCache.getExpireTime();%>
                  <%if(line != null && line.loadTime > 0){%>
                    <%=(new Date(line.loadTime + expireTime)).toString()%>
                  <%}%>
                  &nbsp;
                </td>
                <td>
                  <%long lineSize = line.getSizeInBytes(); totalSize += lineSize;%>
                  <%=lineSize%>
                  &nbsp;
                </td>
                <td>
                  <%if (hasUtilCacheEdit) {%>
                    <a href="<ofbiz:url>/FindUtilCacheElementsRemoveElement?UTIL_CACHE_NAME=<%=cacheName%>&UTIL_CACHE_ELEMENT_NUMBER=<%=keyNum%></ofbiz:url>" class="buttontext">Remove</a>
                  <%}%>
                </td>
              </tr>
              <%keyNum++;%>
            <%}%>
          <%} else {%>
              <%rowColor=(rowColor==rowColor1?rowColor2:rowColor1);%><tr bgcolor="<%=rowColor%>">
                <td colspan="5">No UtilCache elements found.</td>
              </tr>
          <%}%>
    </table>
   <%}else{%>
    <H2>&nbsp;<%=cacheName%> Not Found</H2>
   <%}%>
  <%}else{%>
    <H2>&nbsp;No Cache Name Specified</H2>
  <%}%>
  <div class="tabletext">&nbsp;Size Total: <%=totalSize%> bytes</div>
  <div style="margin-top: 4px;"><a href="<ofbiz:url>/FindUtilCache</ofbiz:url>" class="buttontext">Back to Cache Maintenance</a></div>
<%}else{%>
  <h3>You do not have permission to view this page (UTIL_CACHE_VIEW needed).</h3>
<%}%>
