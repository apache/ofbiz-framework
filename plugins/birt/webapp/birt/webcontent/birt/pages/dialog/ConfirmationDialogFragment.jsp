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
<%@ page import="org.eclipse.birt.report.presentation.aggregation.IFragment,
				 org.eclipse.birt.report.resource.ResourceConstants,
				 org.eclipse.birt.report.resource.BirtResources"  %>

<%-----------------------------------------------------------------------------
	Expected java beans
-----------------------------------------------------------------------------%>
<jsp:useBean id="fragment" type="org.eclipse.birt.report.presentation.aggregation.IFragment" scope="request" />
<jsp:useBean id="attributeBean" type="org.eclipse.birt.report.context.BaseAttributeBean" scope="request" />

<%-----------------------------------------------------------------------------
	Confirmatin dialog fragment
-----------------------------------------------------------------------------%>
<TABLE CELLSPACING="2" CELLPADDING="2" CLASS="birtviewer_dialog_body">
	<TR>
		<TD VALIGN="bottom" ALIGN="center">
			<TABLE CELLSPACING="2" CELLPADDING="2">
				<TR>					
					<TD>
						<iframe name="birt_confirmation_iframe" 
							class="birtviewer_confirmation_dialog_iframe" 
							frameBorder="0" src="<%= "birt/pages/common/processing.jsp?__rtl=" + Boolean.toString( attributeBean.isRtl() )  %>">
						</iframe>			
					</TD>					
				</TR>
			</TABLE>
		</TD>
	</TR>
</TABLE>