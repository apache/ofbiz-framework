<%-----------------------------------------------------------------------------
	Copyright (c) 2004 Actuate Corporation and others.
	All rights reserved. This program and the accompanying materials 
	are made available under the terms of the Eclipse Public License v1.0
	which accompanies this distribution, and is available at
	http://www.eclipse.org/legal/epl-v10.html
	
	Contributors:
		Actuate Corporation - Initial implementation.
-----------------------------------------------------------------------------%>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ page session="false" buffer="none" %>
<%@ page import="org.eclipse.birt.report.presentation.aggregation.IFragment" %>

<%-----------------------------------------------------------------------------
	Expected java beans
-----------------------------------------------------------------------------%>
<jsp:useBean id="fragment" type="org.eclipse.birt.report.presentation.aggregation.IFragment" scope="request" />
 
<%-----------------------------------------------------------------------------
	Report content fragment
-----------------------------------------------------------------------------%>
<TD ID='content' STYLE='width:100%;vertical-align:top'>
	<TABLE CELLSPACING="0" CELLPADDING="0" STYLE="height:100%; width:100%">
	<%
		if ( fragment != null )
		{
			fragment.callBack( request, response );
		}
	%>
	</TABLE>
</TD>