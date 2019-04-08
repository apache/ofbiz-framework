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
				 org.eclipse.birt.report.resource.BirtResources"%>

<%-----------------------------------------------------------------------------
	Expected java beans
-----------------------------------------------------------------------------%>
<jsp:useBean id="fragment" type="org.eclipse.birt.report.presentation.aggregation.IFragment" scope="request" />

<%-----------------------------------------------------------------------------
	Print report dialog fragment
-----------------------------------------------------------------------------%>
<TABLE CELLSPACING="2" CELLPADDING="2" CLASS="birtviewer_dialog_body">
	<TR HEIGHT="5px"><TD></TD></TR>
	<TR>
		<TD>
			<DIV ID="printFormatSetting">
				<DIV><%=BirtResources.getMessage( "birt.viewer.dialog.print.format" )%></DIV>
				<br/>
				<DIV>
				<INPUT TYPE="radio" ID="printAsHTML" name="printFormat" CHECKED/><%=BirtResources.getMessage( "birt.viewer.dialog.print.format.html" )%>
				</DIV>
				<DIV>
				<INPUT TYPE="radio" ID="printAsPDF" name="printFormat"/><%=BirtResources.getMessage( "birt.viewer.dialog.print.format.pdf" )%>
				&nbsp;&nbsp;
				<SELECT	ID="printFitSetting" CLASS="birtviewer_printreport_dialog_select" DISABLED="true">
					<option value="0" selected><%=BirtResources.getMessage( "birt.viewer.dialog.export.pdf.fittoauto" )%></option>
					<option value="1"><%=BirtResources.getMessage( "birt.viewer.dialog.export.pdf.fittoactual" )%></option>
					<option value="2"><%=BirtResources.getMessage( "birt.viewer.dialog.export.pdf.fittowhole" )%></option>
				</SELECT>
				</DIV>
			</DIV>
		</TD>
	</TR>
	<TR HEIGHT="5px"><TD><HR/></TD></TR>
	<TR>
		<TD>
			<DIV ID="printPageSetting">
				<TABLE>
					<TR>
						<TD>
							<INPUT TYPE="radio" ID="printPageAll" NAME="printPages" CHECKED/><%=BirtResources.getMessage( "birt.viewer.dialog.page.all" )%>
						</TD>
						<TD STYLE="padding-left:5px">	
							<INPUT TYPE="radio" ID="printPageCurrent" NAME="printPages"/><%=BirtResources.getMessage( "birt.viewer.dialog.page.current" )%>
						</TD>	
						<TD STYLE="padding-left:5px">
							<INPUT TYPE="radio" ID="printPageRange" NAME="printPages"/><%=BirtResources.getMessage( "birt.viewer.dialog.page.range" )%>
							<INPUT TYPE="text" CLASS="birtviewer_printreport_dialog_input" ID="printPageRange_input" DISABLED="true"/>
						</TD>
					</TR>		
				</TABLE>
			</DIV>
		</TD>
	</TR>
	<TR>
		<TD>&nbsp;&nbsp;<%=BirtResources.getMessage( "birt.viewer.dialog.page.range.description" )%></TD>
	</TR>
	<TR HEIGHT="5px"><TD></TD></TR>
</TABLE>
