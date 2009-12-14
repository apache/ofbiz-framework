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
				 org.eclipse.birt.report.context.BaseAttributeBean,
				 org.eclipse.birt.report.IBirtConstants,
				 org.eclipse.birt.report.utility.ParameterAccessor,
				 org.eclipse.birt.report.resource.BirtResources" %>

<%-----------------------------------------------------------------------------
	Expected java beans
-----------------------------------------------------------------------------%>
<jsp:useBean id="fragment" type="org.eclipse.birt.report.presentation.aggregation.IFragment" scope="request" />
<jsp:useBean id="attributeBean" type="org.eclipse.birt.report.context.BaseAttributeBean" scope="request" />

<%-----------------------------------------------------------------------------
	Dialog container fragment, shared by all standard dialogs.
-----------------------------------------------------------------------------%>
<div id="<%= fragment.getClientId( ) %>" class="dialogBorder" style="display:none;position:absolute;z-index:220">
	<iframe id="<%= fragment.getClientId( ) %>iframe"  name="<%= fragment.getClientId( ) %>iframe" style="z-index:-1; display: none; left:0px; top:0px;
					 background-color: #ff0000; opacity: .0; filter: alpha(opacity = 0); position: absolute;" frameBorder="0" scrolling="no" src="birt/pages/common/blank.html">
	</iframe>	
	<div id="<%= fragment.getClientId( ) %>dialogTitleBar" class="dialogTitleBar dTitleBar">
		<div class="dTitleTextContainer">
			<table style="width: 100%; height: 100%;">
				<tr>
					<td class="dialogTitleText dTitleText">
						<%= fragment.getTitle( ) %>
					</td>
				</tr>
			</table>
		</div>
		<div class="dialogCloseBtnContainer dCloseBtnContainer">
			<table style="width: 100%; height: 100%; border-collapse: collapse">
				<tr>
					<td>
						<label class="birtviewer_hidden_label" for="<%= fragment.getClientId( ) %>dialogCloseBtn">
							<%= 
								BirtResources.getMessage( "birt.viewer.dialog.close" )
							%>
						</label>						
						<div id="<%= fragment.getClientId( ) %>dialogCloseBtn" class="dialogCloseBtn dCloseBtn"/>
					</td>
				</tr>
			</table>
		</div>
	</div>
	<!-- overflow is set as workaround for Mozilla bug https://bugzilla.mozilla.org/show_bug.cgi?id=167801 -->		
	<div  class="dialogBackground" style="overflow: auto;"> 
		<div class="dBackground">
			<div class="dialogContentContainer" id="<%= fragment.getClientId( ) %>dialogContentContainer">
				<%
					if ( fragment != null )
					{
						fragment.callBack( request, response );
					}
				%>
			</div>
			<div class="dialogBtnBarContainer">
				<div>
					<div class="dBtnBarDividerTop">
					</div>
					<div class="dBtnBarDividerBottom">
					</div>
				</div>
				<div class="dialogBtnBar">
					<div id="<%= fragment.getClientId( ) %>dialogCustomButtonContainer" class="dialogBtnBarButtonContainer">
						<div id="<%= fragment.getClientId( ) %>okButton">
							<div id="<%= fragment.getClientId( ) %>okButtonLeft" class="dialogBtnBarButtonLeftBackgroundEnabled"></div>
							<div id="<%= fragment.getClientId( ) %>okButtonRight" class="dialogBtnBarButtonRightBackgroundEnabled"></div>
							<input type="button" value="<%= BirtResources.getHtmlMessage( "birt.viewer.dialog.ok" ) %>" 
								title="<%= BirtResources.getHtmlMessage( "birt.viewer.dialog.ok" ) %>"  
								class="dialogBtnBarButtonText dialogBtnBarButtonEnabled"/>
						</div>
						<div class="dialogBtnBarDivider"></div>
						<div id="<%= fragment.getClientId( ) %>cancelButton">
							<div class="dialogBtnBarButtonLeftBackgroundEnabled"></div>
							<div class="dialogBtnBarButtonRightBackgroundEnabled"></div>
							<input type="button" value="<%= BirtResources.getHtmlMessage( "birt.viewer.dialog.cancel" )%>" 
								title="<%= BirtResources.getHtmlMessage( "birt.viewer.dialog.cancel" )%>"  
								class="dialogBtnBarButtonText dialogBtnBarButtonEnabled"/>
						</div> 
					</div>							
				</div>
			</div>
		</div>
	</div>
</div>

