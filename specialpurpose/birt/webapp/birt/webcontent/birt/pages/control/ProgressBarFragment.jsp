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
				 org.eclipse.birt.report.resource.BirtResources" %>

<%-----------------------------------------------------------------------------
	Expected java beans
-----------------------------------------------------------------------------%>
<jsp:useBean id="fragment" type="org.eclipse.birt.report.presentation.aggregation.IFragment" scope="request" />
<jsp:useBean id="attributeBean" type="org.eclipse.birt.report.context.BaseAttributeBean" scope="request" />
<%-----------------------------------------------------------------------------
	Progress bar fragment
-----------------------------------------------------------------------------%>
<DIV ID="progressBar" STYLE="display:none;position:absolute;z-index:310">
	<TABLE WIDTH="250px" CLASS="birtviewer_progressbar" CELLSPACING="10px">
		<TR>
			<TD ALIGN="center">
				<B>
					<%= BirtResources.getMessage( "birt.viewer.progressbar.prompt" )%>
				</B>
			</TD>
		</TR>
		<TR>
			<TD ALIGN="center">
				<IMG SRC="<%= "birt/images/" + (attributeBean.isRtl()?"Loading_rtl":"Loading") + ".gif" %>" ALT="Progress Bar Image"/>
			</TD>
		</TR>
		<TR>
			<TD ALIGN="center">
				<DIV ID="cancelTaskButton" STYLE="display:none">
					<TABLE WIDTH="100%">
						<TR>
							<TD ALIGN="center">
								<INPUT TYPE="BUTTON" VALUE="<%= BirtResources.getHtmlMessage( "birt.viewer.dialog.cancel" )%>" 					   
									   TITLE="<%= BirtResources.getHtmlMessage( "birt.viewer.dialog.cancel" )%>"
									   CLASS="birtviewer_progressbar_button"/>
							</TD>
						</TR>
					</TABLE>
				</DIV>
			</TD>
		</TR>	
		<INPUT TYPE="HIDDEN" ID="taskid" VALUE=""/>
	</TABLE>
</DIV>