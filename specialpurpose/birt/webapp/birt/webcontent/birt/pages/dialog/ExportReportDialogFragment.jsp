<%-----------------------------------------------------------------------------
	Copyright (c) 2004 Actuate Corporation and others.
	All rights reserved. This program and the accompanying materials 
	are made available under the terms of the Eclipse Public License v1.0
	which accompanies this distribution, and is available at
	http://www.eclipse.org/legal/epl-v10.html
	
	Contributors:
		Actuate Corporation - Initial implementation.
-----------------------------------------------------------------------------%>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page session="false" buffer="none"%>
<%@ page import="org.eclipse.birt.report.presentation.aggregation.IFragment,
				 org.eclipse.birt.report.utility.ParameterAccessor,
				 org.eclipse.birt.report.resource.BirtResources"%>

<%-----------------------------------------------------------------------------
	Expected java beans
-----------------------------------------------------------------------------%>
<jsp:useBean id="fragment" type="org.eclipse.birt.report.presentation.aggregation.IFragment" scope="request" />

<%
	String[] supportedFormats = ParameterAccessor.supportedFormats;
%>
<%-----------------------------------------------------------------------------
	Export report dialog fragment
-----------------------------------------------------------------------------%>
<TABLE CELLSPACING="2" CELLPADDING="2" CLASS="birtviewer_dialog_body">
	<TR HEIGHT="5px"><TD></TD></TR>
	<TR>
		<TD>
		<%=BirtResources.getMessage( "birt.viewer.dialog.export.format" )%>
		<SELECT	ID="exportFormat" NAME="format" CLASS="birtviewer_exportreport_dialog_select">
			<%
				for ( int i = 0; i < supportedFormats.length; i++ )
				{
					if ( !ParameterAccessor.PARAM_FORMAT_HTML.equalsIgnoreCase( supportedFormats[i] ) )
					{
			%>
						<OPTION VALUE="<%= supportedFormats[i] %>"><%= ParameterAccessor.getOutputFormatLabel( supportedFormats[i] ) %></OPTION>
			<%
					}
				}
			%>
		</SELECT>
		</TD>
	</TR>
	<TR HEIGHT="5px"><TD></TD></TR>
	<TR>
		<TD>
			<DIV ID="exportPageSetting">
				<TABLE>
					<TR>
						<TD>
							<INPUT TYPE="radio" ID="exportPageAll" NAME="exportPages" CHECKED/><%=BirtResources.getHtmlMessage( "birt.viewer.dialog.page.all" )%>
						</TD>
						<TD STYLE="padding-left:5px">	
							<INPUT TYPE="radio" ID="exportPageCurrent" NAME="exportPages"/><%=BirtResources.getHtmlMessage( "birt.viewer.dialog.page.current" )%>
						</TD>	
						<TD STYLE="padding-left:5px">
							<INPUT TYPE="radio" ID="exportPageRange" NAME="exportPages"/><%=BirtResources.getHtmlMessage( "birt.viewer.dialog.page.range" )%>
							<INPUT TYPE="text" CLASS="birtviewer_exportreport_dialog_input" ID="exportPageRange_input" DISABLED="true"/>
						</TD>
					</TR>		
				</TABLE>
			</DIV>
		</TD>
	</TR>
	<TR>
		<TD>&nbsp;&nbsp;<%=BirtResources.getHtmlMessage( "birt.viewer.dialog.page.range.description" )%></TD>
	</TR>
	<TR HEIGHT="5px"><TD><HR/></TD></TR>
	<TR>
		<TD>
			<DIV ID="exportFitSetting">
				<TABLE>
					<TR>
						<TD>
							<INPUT TYPE="radio" ID="exportFitToAuto" NAME="exportFit" CHECKED/><%=BirtResources.getHtmlMessage( "birt.viewer.dialog.export.pdf.fittoauto" )%>
						</TD>
						<TD>
							<INPUT TYPE="radio" ID="exportFitToActual" NAME="exportFit"/><%=BirtResources.getHtmlMessage( "birt.viewer.dialog.export.pdf.fittoactual" )%>
						</TD>
						<TD STYLE="padding-left:5px">	
							<INPUT TYPE="radio" ID="exportFitToWhole" NAME="exportFit"/><%=BirtResources.getHtmlMessage( "birt.viewer.dialog.export.pdf.fittowhole" )%>
						</TD>
					</TR>
				</TABLE>			
			</DIV>			
		</TD>
	</TR>
	<TR HEIGHT="5px"><TD></TD></TR>
</TABLE>
