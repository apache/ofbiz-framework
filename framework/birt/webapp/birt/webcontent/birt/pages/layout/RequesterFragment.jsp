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
<%@ page import="org.eclipse.birt.report.resource.BirtResources,
				 org.eclipse.birt.report.context.BaseAttributeBean,
				 org.eclipse.birt.report.utility.ParameterAccessor,
 				 org.eclipse.birt.report.presentation.aggregation.IFragment" %>

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

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/REC-html40/strict.dtd">
<HTML>
	<HEAD>
		<TITLE>PARAMETER SELECTION PAGE</TITLE>
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
		
		<SCRIPT SRC="birt/ajax/utility/Debug.js" type="text/javascript"></script>
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
		<script src="birt/ajax/ui/app/BirtProgressBar.js" type="text/javascript"></script>

 		<script src="birt/ajax/ui/report/AbstractReportComponent.js" type="text/javascript"></script>
 		<script src="birt/ajax/ui/report/AbstractBaseReportDocument.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/report/BirtReportDocument.js" type="text/javascript"></script>

		<script src="birt/ajax/ui/dialog/AbstractBaseDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/AbstractParameterDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/BirtParameterDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/AbstractExceptionDialog.js" type="text/javascript"></script>
		<script src="birt/ajax/ui/dialog/BirtExceptionDialog.js" type="text/javascript"></script>
		
		<SCRIPT SRC="birt/ajax/utility/BirtPosition.js" type="text/javascript"></script>

		<SCRIPT SRC="birt/ajax/core/BirtCommunicationManager.js" type="text/javascript"></script>
		<SCRIPT SRC="birt/ajax/core/BirtSoapRequest.js" type="text/javascript"></script>
		<SCRIPT SRC="birt/ajax/core/BirtSoapResponse.js" type="text/javascript"></script>
				
	</HEAD>

	<BODY CLASS="BirtViewer_Body" onload="Javascript:init()" LEFTMARGIN='0px' STYLE='overflow:hidden'>
		<%
		if( attributeBean.isRtl() )
		{
		%>
		<DIV DIR="rtl">
		<%
		}
		%>
		<!-- Header section -->
		<TABLE ID='layout' CELLSPACING='0' CELLPADDING='0' STYLE='width:100%;height:100%'>
		<%
			if ( fragment != null )
			{
				fragment.callBack( request, response );
			}
		%>
		</TABLE>
		<%
		if( attributeBean.isRtl() )
		{
		%>
		</DIV>
		<%
		}
		%>
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
	
	<SCRIPT TYPE="text/vbscript">
		On Error Resume Next
		If useVBMethod = true Then
		    hasSVGSupport = IsObject(CreateObject("Adobe.SVGCtl"))
		End If
	</SCRIPT>

	<SCRIPT TYPE="text/javascript">		
		var Mask =  new Mask( false );
		var BrowserUtility = new BrowserUtility( );
		var DragDrop = new BirtDndManager( );

		var birtProgressBar = new BirtProgressBar( 'progressBar' );
		var birtReportDocument = new BirtReportDocument( "Document" );

		var parameterMode;
		if ( Constants.request.servletPath == Constants.SERVLET_PARAMETER )
		{
			parameterMode = Constants.SERVLET_PARAMETER;
		}
		else
		{
			parameterMode = Constants.SERVLET_PREVIEW;
		}
		
		var birtParameterDialog = new BirtParameterDialog( 'parameterDialog', parameterMode );
		var birtExceptionDialog = new BirtExceptionDialog( 'exceptionDialog' );
		
		function init( )
		{					
			soapURL = birtUtility.initSessionId( soapURL );
			birtParameterDialog.__cb_bind( );
		}
		
	</SCRIPT>
</HTML>

