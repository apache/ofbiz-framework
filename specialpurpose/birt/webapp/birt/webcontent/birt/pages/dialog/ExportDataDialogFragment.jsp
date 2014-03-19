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

<%-----------------------------------------------------------------------------
	Export data dialog fragment
-----------------------------------------------------------------------------%>
<DIV ID="dialog_content">
	<TABLE CELLSPACING="0" CELLPADDING="0" STYLE="width:100%">
		<TR>
			<TD>
				<TABLE ID="tabs" CELLSPACING="0" CELLPADDING="2">
					<TR HEIGHT="20px">
						<TD CLASS="birtviewer_dialog_tab_selected" NOWRAP>
							<%= 
								BirtResources.getMessage( "birt.viewer.dialog.exportdata.tab.field" )
							%>
						</TD>
						<TD CLASS="birtviewer_dialog_tab_normal">
							<%= BirtResources.getMessage( "birt.viewer.dialog.exportdata.tab.filter" )%>
						</TD>
					</TR>
				</TABLE>
			</TD>
		</TR>
		<TR>
			<TD>
				<DIV ID="aaacontent">
					<DIV >
						<TABLE CELLSPACING="2" CELLPADDING="2" CLASS="birtviewer_dialog_body">
							<TR HEIGHT="5px"><TD></TD></TR>
							<TR>
								<TD>
									<%= BirtResources.getMessage( "birt.viewer.dialog.exportdata.availablecolumn" )%>
								</TD>
								<TD></TD>
								<TD>
									<%= BirtResources.getMessage( "birt.viewer.dialog.exportdata.selectedcolumn" )%>
								</TD>
							</TR>
							<TR>
								<TD VALIGN="top">
									<TABLE>
										<TR><TD>
											<SELECT ID="availableColumnSelect" SIZE="10" CLASS="birtviewer_exportdata_dialog_select">
											</SELECT>
										</TD></TR>
									</TABLE>
								</TD>
								<TD VALIGN="top">
									<TABLE HEIGHT="100%">
										<TR><TD>&nbsp;</TD></TR>
										<TR><TD>
											<TABLE VALIGN="top">
												<TR><TD>
													<INPUT TYPE="button" VALUE=">>" CLASS="birtviewer_exportdata_dialog_button">
												</TD></TR>
												<TR><TD>
													<INPUT TYPE="button" VALUE=">" CLASS="birtviewer_exportdata_dialog_button">
												</TD></TR>
												<TR><TD>
													<INPUT TYPE="button" VALUE="<" CLASS="birtviewer_exportdata_dialog_button">
												</TD></TR>
												<TR><TD>
													<INPUT TYPE="button" VALUE="<<" CLASS="birtviewer_exportdata_dialog_button">
												</TD></TR>
											</TABLE>
										</TD></TR>
									</TABLE>
								</TD>
								<TD>
									<TABLE>
										<TR><TD>
											<SELECT ID="selectedColumnSelect" SIZE="10" CLASS="birtviewer_exportdata_dialog_select">
											</SELECT>
										</TD></TR>
									</TABLE>
								</TD>
							</TR>
							<TR HEIGHT="5px"><TD></TD></TR>
							<TR>
								<TD COLSPAN="3" STYLE="font-size:7pt">
									<%= BirtResources.getMessage( "birt.viewer.dialog.exportdata.format" )%>
								</TD>
							</TR>
							<TR HEIGHT="5px"><TD></TD></TR>
						</TABLE>
					</DIV>
					<DIV STYLE="display:none">
						<IMG NAME="add" SRC="birt/images/AddFilter.gif" TITLE="add" CLASS="birtviewer_clickable">
						<TABLE ID="ExportCriteriaTable" CELLSPACING="2" CELLPADDING="2" CLASS="birtviewer_dialog_body">
							<TBODY ID="ExportCriteriaTBODY">
							</TBODY> 
						</TABLE>
					</DIV>
				</DIV>
			</TD>
		</TR>
	</TABLE>
</DIV>