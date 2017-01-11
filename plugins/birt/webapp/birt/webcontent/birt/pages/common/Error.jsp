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
<%@ page import="org.eclipse.birt.core.exception.BirtException,
				 org.eclipse.birt.report.utility.ParameterAccessor,
				 org.eclipse.birt.report.resource.BirtResources,
				 java.io.PrintWriter" %>

<%-----------------------------------------------------------------------------
	Expected java beans
-----------------------------------------------------------------------------%>
<jsp:useBean id="error" type="java.lang.Exception" scope="request" />

<%-----------------------------------------------------------------------------
	Error content
-----------------------------------------------------------------------------%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<HTML>
	<HEAD>
		<TITLE>
				<%= BirtResources.getMessage( "birt.viewer.title.error" )%>
		</TITLE>
		<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=utf-8">
		<LINK REL="stylesheet" HREF="<%= request.getContextPath( ) + "/webcontent/birt/styles/style.css" %>" TYPE="text/css">
	</HEAD>
	<BODY>
		<TABLE CLASS="BirtViewer_Highlight_Label">
			<TR><TD NOWRAP>
				<%
					if ( error != null )
					{
						if ( error.getMessage( ) != null )
						{
							out.println( ParameterAccessor.htmlEncode( new String( error.getMessage( ).getBytes( "ISO-8859-1" ),"UTF-8" ) ) );
						}
						else
						{
							PrintWriter writer = new PrintWriter( out );									
							error.printStackTrace( writer );				
						}					
					}
				%>
			</TD></TR>
		</TABLE>
	</BODY>
</HTML>