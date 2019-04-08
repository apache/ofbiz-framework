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
<%@ page import="org.eclipse.birt.report.context.ScalarParameterBean,
				 org.eclipse.birt.report.context.BaseAttributeBean,
				 org.eclipse.birt.report.IBirtConstants,
				 org.eclipse.birt.report.service.api.ParameterSelectionChoice,
				 org.eclipse.birt.report.utility.ParameterAccessor,
				 org.eclipse.birt.report.utility.DataUtil,
				 java.util.List" %>

<%-----------------------------------------------------------------------------
	Expected java beans
-----------------------------------------------------------------------------%>
<jsp:useBean id="attributeBean" type="org.eclipse.birt.report.context.BaseAttributeBean" scope="request" />

<%-----------------------------------------------------------------------------
	Combo box parameter control
-----------------------------------------------------------------------------%>
<%
	ScalarParameterBean parameterBean = ( ScalarParameterBean ) attributeBean.getParameterBean( );
	String encodedParameterName = ParameterAccessor.htmlEncode( parameterBean.getName( ) );
	String paramValue = parameterBean.getValue( );
	String displayText = parameterBean.getDisplayText( );
	String defaultValue = parameterBean.getDefaultValue( );
	String defaultDisplayText = parameterBean.getDefaultDisplayText( );
	boolean isDisplayTextInList = parameterBean.isDisplayTextInList( );
	boolean allowMultiValue = !parameterBean.allowNewValues( ) && parameterBean.getParameter( ).isMultiValue( );
	List values = parameterBean.getValueList( );	
%>
<TR>
	<TD NOWRAP>
		<IMG SRC="birt/images/parameter.gif" ALT="<%= parameterBean.getDisplayName( ) %>" TITLE="<%= parameterBean.getToolTip( ) %>"/>
	</TD>
	<TD NOWRAP>
		<FONT TITLE="<%= parameterBean.getToolTip( ) %>"><LABEL FOR="<%= encodedParameterName + "_selection"%>"><%= parameterBean.getDisplayName( ) %>:</LABEL></FONT>
		<%-- is required --%>
		<%
		if ( parameterBean.isRequired( ) )
		{
		%>
			<FONT COLOR="red"><LABEL FOR="<%= encodedParameterName + "_selection"%>">*</LABEL></FONT>
		<%
		}
		%>
	</TD>
</TR>
<TR>
	<TD NOWRAP></TD>
	<TD NOWRAP WIDTH="100%">
		<INPUT TYPE="HIDDEN" ID="control_type" VALUE="select">
		<INPUT TYPE="HIDDEN" ID="data_type" VALUE="<%="" + parameterBean.getParameter( ).getDataType( ) %>">
		<INPUT TYPE="HIDDEN"
			ID="<%= encodedParameterName + "_value" %>"
			NAME="<%= encodedParameterName %>"
			<%= paramValue != null ? " VALUE=\"" + ParameterAccessor.htmlEncode( paramValue ) + "\"": "" %>
		>

<%	
	boolean CHECKED = true;
		
	if ( parameterBean.allowNewValues( ) ) // TODO: Editable
	{
		CHECKED = parameterBean.isValueInList( ) 
				  || paramValue == null
				  || ( !parameterBean.isValueInList( ) && defaultValue != null && defaultValue.equals( paramValue ) );		
%>
		<LABEL FOR="<%= encodedParameterName + "_radio_selection" %>" CLASS="birtviewer_hidden_label">Select</LABEL>
		<INPUT TYPE="RADIO"
			birtParameterType="combobox"
			NAME="<%= encodedParameterName + "_radios" %>" 
			ID="<%= encodedParameterName + "_radio_selection" %>" 
			VALUE="<%= encodedParameterName %>"
			<%= CHECKED ? "CHECKED" : "" %> >
<%
	}
%>
		<SELECT ID="<%= encodedParameterName + "_selection"%>"
			TITLE="<%= parameterBean.getToolTip( ) %>"
			CLASS="birtviewer_parameter_dialog_Select"
			birtParameterType="combobox" 
			<%= !CHECKED ? "DISABLED='true'" : "" %> 
			<%=  allowMultiValue? "multiple='true'" : "" %>
			<%= ( !parameterBean.allowNewValues( ) && parameterBean.isRequired( ) ) ? "aria-required='true'" : "" %>
		    >
<%
	if ( parameterBean.getSelectionList( ) != null )
	{
		if( !parameterBean.isRequired( ) || ( parameterBean.isCascade( ) && DataUtil.trimString( defaultValue ).length( )<=0 ) )
		{
			if( allowMultiValue && DataUtil.contain( values, "", true ) )
			{
%>
		<OPTION SELECTED></OPTION>
<%				
			}
			else
			{
%>
		<OPTION></OPTION>
<%
			}
		}
		
		if ( DataUtil.trimString( defaultValue ).length( ) > 0 && !parameterBean.isDefaultValueInList( ) ) // Add default value in Combo Box
		{
			boolean flag = false;
			if( allowMultiValue )
			{
				flag = DataUtil.contain( values, defaultValue, true );
			}
			else
			{
				flag = CHECKED && !parameterBean.isValueInList( );
				// if displayText is in request, use it
				if( flag && parameterBean.isDisplayTextInReq( ) )
				{
					defaultDisplayText = displayText;
				}				
			}
%>
			<OPTION VALUE="<%= ParameterAccessor.htmlEncode( defaultValue ) %>" 
			        TITLE="<%= ParameterAccessor.htmlEncode( defaultDisplayText ) %>"
				<%=  flag ? "SELECTED" : "" %> > <%= ParameterAccessor.htmlEncode( defaultDisplayText ) %></OPTION>
<%	
		}
		
		boolean isSelected = false;
		for ( int i = 0; i < parameterBean.getSelectionList( ).size( ); i++ )
		{
			ParameterSelectionChoice selectionItem = ( ParameterSelectionChoice )parameterBean.getSelectionList( ).get( i );						
			String label = selectionItem.getLabel( );
			String value = ( String ) selectionItem.getValue( );
			String outputValue = ParameterAccessor.htmlEncode(( value == null)?IBirtConstants.NULL_VALUE:value);
			String outputLabel = ParameterAccessor.htmlEncode(( label == null)?IBirtConstants.NULL_VALUE_DISPLAY:label);

			if( allowMultiValue )
			{
				if( DataUtil.contain( values, value, true ) )
				{
%>
			<OPTION VALUE="<%= outputValue %>"
			        TITLE="<%= outputLabel %>"
			        SELECTED><%= outputLabel %></OPTION>
<%
					
				}
				else
				{
%>
			<OPTION VALUE="<%= outputValue %>"
			        TITLE="<%= outputLabel %>"><%= outputLabel %></OPTION>
<%					
				}
			}
			else
			{
				if ( !isSelected && DataUtil.equals( paramValue, value ) 
					 && ( !isDisplayTextInList || ( isDisplayTextInList && DataUtil.equals(label, displayText ) )))
				{
					isSelected = true;				
%>
			<OPTION VALUE="<%= outputValue %>" 
			        TITLE="<%= outputLabel %>"
			        SELECTED><%= outputLabel %></OPTION>
<%
				}
				else
				{
%>
			<OPTION VALUE="<%= outputValue %>"
			        TITLE="<%= outputLabel %>"><%= outputLabel %></OPTION>
<%
				}
			}
		}
	}
%>
		</SELECT>
<%
	if ( parameterBean.allowNewValues( ) ) // TODO: editable.
	{
%>
		<BR>
		<LABEL FOR="<%= encodedParameterName + "_radio_input" %>" CLASS="birtviewer_hidden_label">Input</LABEL>
		<INPUT TYPE="RADIO"
			birtParameterType="combobox"
			NAME="<%= encodedParameterName + "_radios" %>" 
			ID="<%= encodedParameterName + "_radio_input"%>" 
			VALUE="<%= encodedParameterName %>"
			<%= !CHECKED ? "CHECKED" : "" %> >
			
		<LABEL FOR="<%= encodedParameterName + "_input" %>" CLASS="birtviewer_hidden_label">Input text</LABEL>
		<INPUT CLASS="BirtViewer_parameter_dialog_Input"
			birtParameterType="combobox"
			TYPE="<%= parameterBean.isValueConcealed( )? "PASSWORD" : "TEXT" %>"
			TITLE="<%= parameterBean.getToolTip( ) %>"
			<%= !CHECKED ? "NAME=\"" + encodedParameterName + "_default\"": "" %> 
			ID="<%= encodedParameterName + "_input"%>"
			<%= !CHECKED && displayText != null ? "VALUE=\"" + ParameterAccessor.htmlEncode( displayText ) + "\"": "" %> 
			<%= CHECKED ? "DISABLED='true'" : "" %>	>
		
		<INPUT TYPE="HIDDEN"
			ID="<%= encodedParameterName + "_displayText" %>"
			<%= !CHECKED && displayText != null ? "VALUE=\"" + ParameterAccessor.htmlEncode( displayText ) + "\"": "" %> 
			>		
	<%
	  }
	%>
		<INPUT TYPE="HIDDEN" ID="isRequired" 
			VALUE = "<%= parameterBean.isRequired( )? "true": "false" %>">
			
	<%
	if ( parameterBean.isCascade( ) )
	{
	%>
		<INPUT TYPE="HIDDEN" ID="<%=IBirtConstants.IS_CASCADE%>" VALUE="true"/>
	<%
	}
	%>
	</TD>
</TR>