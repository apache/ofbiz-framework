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
<%@ page import="java.util.*" %>
<%@ page import="org.apache.ofbiz.base.util.*" %>
<%@ page import="org.apache.ofbiz.entity.*" %>
<%@ page import="org.apache.ofbiz.entity.util.*" %>
<%@ page import="org.apache.ofbiz.webapp.website.WebSiteWorker" %>
<jsp:useBean id="delegator" type="org.apache.ofbiz.entity.GenericDelegator" scope="request" />
<%
ServletContext context = pageContext.getServletContext();
String webSiteId = WebSiteWorker.getWebSiteId(request);
List<GenericValue> webAnalytics = delegator.findByAnd("WebAnalyticsConfig", UtilMisc.toMap("webSiteId", webSiteId), null, false);
%>
<html>
<head>
<title>Error 404</title>
<%if (webAnalytics != null) {%>
<script language="JavaScript" type="text/javascript">
<%for (GenericValue webAnalytic : webAnalytics) {%>
    <%=StringUtil.wrapString((String) webAnalytic.get("webAnalyticsCode"))%>
<%}%>
</script>
<%}%>
</head>
<body>
<p>
<b>404.</b>
<ins>That&#39;s an error.</ins>
</p>
<p>
The requested URL
<code><%=request.getAttribute("filterRequestUriError")%></code>
was not found on this server.
<ins>That&#39;s all we know.</ins>
</p>
</body>
</html>
