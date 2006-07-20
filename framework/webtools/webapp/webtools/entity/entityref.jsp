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

<%@ page import="org.ofbiz.base.util.*" %>

<HTML>
<HEAD>
<TITLE>Entity Reference Chart</TITLE>
</HEAD>
<%String controlPath=(String)request.getAttribute("_CONTROL_PATH_");%>
<%
	String list = controlPath + "/view/entityref_list";
	String main = controlPath + "/view/entityref_main";
	String search = (String) request.getParameter("search");
    String forstatic = (String) request.getParameter("forstatic");
	if (search != null) {
		list = list + "?search=" + search;
		main = main + "?search=" + search;
	} else if (forstatic != null) {
		list = list + "?forstatic=" + forstatic;
		main = main + "?forstatic=" + forstatic;
	}
%>
<FRAMESET cols="30%,70%">
<FRAME src="<%=response.encodeURL(list)%>" name="entityListFrame">
<FRAME src="<%=response.encodeURL(main)%>" name="entityFrame">
</FRAMESET>
<NOFRAMES>
<H2>Frame Alert</H2>
<P>This document is designed to be viewed using the frames feature. If you see this message, you are using a non-frame-capable web client.
<BR>
</NOFRAMES>
</HTML>
