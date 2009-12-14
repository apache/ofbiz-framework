<%-----------------------------------------------------------------------------
	Copyright (c) 2004-2008 Actuate Corporation and others.
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
				 org.eclipse.birt.report.resource.ResourceConstants,
				 org.eclipse.birt.report.resource.BirtResources,
				 org.eclipse.birt.report.utility.ParameterAccessor" %>

<%-----------------------------------------------------------------------------
	Expected java beans
-----------------------------------------------------------------------------%>
<jsp:useBean id="fragment" type="org.eclipse.birt.report.presentation.aggregation.IFragment" scope="request" />
<jsp:useBean id="attributeBean" type="org.eclipse.birt.report.context.BaseAttributeBean" scope="request" />

<%
	// base href can be defined in config file for deployment.
	String baseHref = request.getScheme( ) + "://" + request.getServerName( ) + ":" + request.getServerPort( );
	if( !attributeBean.isDesigner( ) )
	{
		String baseURL = ParameterAccessor.getBaseURL( );
		if( baseURL != null )
			baseHref = baseURL;
	}
	baseHref += request.getContextPath( ) + fragment.getJSPRootPath( );
%>

<%-----------------------------------------------------------------------------
	Viewer root fragment
-----------------------------------------------------------------------------%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/REC-html40/strict.dtd">
<HTML>
	<HEAD>
		<TITLE><%= attributeBean.getReportTitle( ) %></TITLE>
		<BASE href="<%= baseHref %>" >
		
		<!-- Mimics Internet Explorer 7, it just works on IE8. -->
		<META HTTP-EQUIV="X-UA-Compatible" CONTENT="IE=EmulateIE7">
		<META HTTP-EQUIV="Content-Type" CONTENT="text/html; CHARSET=utf-8">
		<LINK REL="stylesheet" HREF="birt/styles/style.css" TYPE="text/css">
		<%
		if( attributeBean.isRtl() )
		{
		%>
		<LINK REL="stylesheet" HREF="birt/styles/dialogbase_rtl.css" MEDIA="screen" TYPE="text/css"/>
		<%
		}
		else
		{
		%>
		<LINK REL="stylesheet" HREF="birt/styles/dialogbase.css" MEDIA="screen" TYPE="text/css"/>	
		<%
		}
		%>
		<script type="text/javascript">			
			<%
			if( request.getAttribute("SoapURL") != null )
			{
			%>
			var soapURL = "<%= (String)request.getAttribute("SoapURL")%>";
			<%
			}
			else
			{
			%>
			var soapURL = document.location.href;
			<%
			}
			%>
			var rtl = <%= attributeBean.isRtl( ) %>;
		</script>
		
		<script src="birt/ajax/utility/Debug.js" type="text/javascript"></script>
		<script src="birt/ajax/lib/prototype.js" type="text/javascript"></script>
		
		<!-- Mask -->
		<script src="birt/ajax/core/Mask.js" type="text/javascript"></script>
		<script src="birt/ajax/utility/BrowserUtility.js" type="text/javascript"></script>
		
		<!-- Drag and Drop -->
		<script src="birt/ajax/core/BirtDndManager.js" type="text/javascript"></script>
		
		<script src="birt/ajax/utility/Constants.js" type="text/javascript"></script>
		<script src="birt/ajax/utility/BirtUtility.js" type="text/javascript"></script>
		
		<script src="birt/ajax/core/BirtEventDispatcher.js" type="text/javascript"></script>
		<script src="birt/ajax/core/BirtEvent.js" type="text/javascript"></script>
		
		<script src="birt/ajax/mh/BirtBaseResponseHandler.js" type="text/javascript"></script>
		<script src="birt/ajax/mh/BirtGetUpdatedObjectsResponseHandler.js" type="text/javascript"></script>

		<script src="birt/ajax/ui/app/AbstractUIComponent.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/app/AbstractBaseToolbar.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/app/BirtToolbar.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/app/BirtNavigationBar.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/app/AbstractBaseToc.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/app/BirtToc.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/app/BirtProgressBar.js" type="text/javascript"></script>

 		<script src="birt/ajax/ui/report/AbstractReportComponent.js" type="text/javascript"></script>
 		<script src="birt/ajax/ui/report/AbstractBaseReportDocument.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/report/BirtReportDocument.js" type="text/javascript"></script>

		<script src="birt/ajax/ui/dialog/AbstractBaseDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/BirtTabedDialogBase.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/AbstractParameterDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/BirtParameterDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/BirtSimpleExportDataDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/BirtExportReportDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/BirtPrintReportDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/BirtPrintReportServerDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/AbstractExceptionDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/BirtExceptionDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/BirtConfirmationDialog.js" type="text/javascript"></script>
		
		<script src="birt/ajax/utility/BirtPosition.js" type="text/javascript"></script>
		<script src="birt/ajax/utility/Printer.js" type="text/javascript"></script>

		<script src="birt/ajax/core/BirtCommunicationManager.js" type="text/javascript"></script>
		<script src="birt/ajax/core/BirtSoapRequest.js" type="text/javascript"></script>
		<script src="birt/ajax/core/BirtSoapResponse.js" type="text/javascript"></script>
		
	</HEAD>
	
	<BODY 
		CLASS="BirtViewer_Body"  
		ONLOAD="javascript:init( );" 
		SCROLL="no" 
		LEFTMARGIN='0px' 
		STYLE='overflow:hidden; direction: <%= attributeBean.isRtl()?"rtl":"ltr" %>'
		>
		<!-- Header section -->
		<TABLE ID='layout' CELLSPACING='0' CELLPADDING='0' STYLE='width:100%;height:100%'>
			<%
				if( attributeBean.isShowTitle( ) )
				{
			%>
			<TR CLASS='body_caption_top'>
				<TD COLSPAN='2'></TD>
			</TR>
			<TR CLASS='body_caption' VALIGN='bottom'>
				<TD COLSPAN='2'>
					<TABLE BORDER=0 CELLSPACING="0" CELLPADDING="1px" WIDTH="100%">
						<TR>
							<TD WIDTH="3px"/>
							<TD>
								<B><%= attributeBean.getReportTitle( ) %>
								</B>
							</TD>
							<TD ALIGN='right'>
							</TD>
							<TD WIDTH="3px"/>
						</TR>
					</TABLE>
				</TD>
			</TR>
			<%
				}
			%>
			
			<%
				if ( fragment != null )
				{
					fragment.callBack( request, response );
				}
			%>
		</TABLE>
	</BODY>

	<%@include file="../common/Locale.jsp" %>	
	<%@include file="../common/Attributes.jsp" %>	

	<script type="text/javascript">
	// <![CDATA[
		var hasSVGSupport = false;
		var useVBMethod = false;
		if ( navigator.mimeTypes != null && navigator.mimeTypes.length > 0 )
		{
		    if ( navigator.mimeTypes["image/svg+xml"] != null )
		    {
		        hasSVGSupport = true;
		    }
		}
		else
		{
		    useVBMethod = true;
		}
		
	// ]]>
	</script>
	
	<script type="text/vbscript">
		On Error Resume Next
		If useVBMethod = true Then
		    hasSVGSupport = IsObject(CreateObject("Adobe.SVGCtl"))
		End If
	</script>

	<script type="text/javascript">
		var Mask =  new Mask(false); //create mask using "div"
		var BrowserUtility = new BrowserUtility();
		DragDrop = new BirtDndManager();

		var birtToolbar = new BirtToolbar( 'toolbar' );
		var navigationBar = new BirtNavigationBar( 'navigationBar' );
		var birtToc = new BirtToc( 'display0' );
		var birtProgressBar = new BirtProgressBar( 'progressBar' );
		var birtReportDocument = new BirtReportDocument( "Document", birtToc );

		var birtParameterDialog = new BirtParameterDialog( 'parameterDialog', 'frameset' );
		var birtSimpleExportDataDialog = new BirtSimpleExportDataDialog( 'simpleExportDataDialog' );
		var birtExportReportDialog = new BirtExportReportDialog( 'exportReportDialog' );
		var birtPrintReportDialog = new BirtPrintReportDialog( 'printReportDialog' );
		var birtPrintReportServerDialog = new BirtPrintReportServerDialog( 'printReportServerDialog' );
		var birtExceptionDialog = new BirtExceptionDialog( 'exceptionDialog' );
		var birtConfirmationDialog = new BirtConfirmationDialog( 'confirmationDialog' );

		// register the base elements to the mask, so their input
		// will be disabled when a dialog is popped up.
		Mask.setBaseElements( new Array( birtToolbar.__instance, navigationBar.__instance, birtReportDocument.__instance) );
		
		function init()
		{
			soapURL = birtUtility.initSessionId( soapURL );
			
		<%
		if ( attributeBean.isShowParameterPage( ) )
		{
		%>
			birtParameterDialog.__cb_bind( );
		<%
		}
		else
		{
		%>
			soapURL = birtUtility.initDPI( soapURL );
			navigationBar.__init_page( );
		<%
		}
		%>
		}
		
		// When link to internal bookmark, use javascript to fire an Ajax request
		function catchBookmark( bookmark )
		{	
			birtEventDispatcher.broadcastEvent( birtEvent.__E_GETPAGE, { name : "__bookmark", value : bookmark } );		
		}
		
	</script>
</HTML>

