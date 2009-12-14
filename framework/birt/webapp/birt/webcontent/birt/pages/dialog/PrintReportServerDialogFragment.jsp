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
				 org.eclipse.birt.report.IBirtConstants,
				 java.util.ArrayList,
				 java.util.Map,
				 org.eclipse.birt.report.utility.Printer,
				 org.eclipse.birt.report.utility.DataUtil,
				 org.eclipse.birt.report.utility.PrintUtility,
				 org.eclipse.birt.report.utility.ParameterAccessor,
				 org.eclipse.birt.report.resource.BirtResources"%>

<%-----------------------------------------------------------------------------
	Expected java beans
-----------------------------------------------------------------------------%>
<jsp:useBean id="fragment" type="org.eclipse.birt.report.presentation.aggregation.IFragment" scope="request" />

<SCRIPT LANGUAGE="javascript">var index = 0;</SCRIPT>
<%
	boolean enable = ParameterAccessor.isSupportedPrintOnServer;
	if( enable )
	{
		String[] supportedFormats = ParameterAccessor.supportedFormats;
		for( int i=0; i<supportedFormats.length; i++ )
		{
			if( IBirtConstants.POSTSCRIPT_RENDER_FORMAT.equalsIgnoreCase( supportedFormats[i] ) )
			{
				enable = true;
				break;
			}
		}
	}
	
	if( enable )
	{
		ArrayList printers = (ArrayList)PrintUtility.findPrinters();
		for( int i=0; i<printers.size( ); i++ )
		{
			Printer bean = (Printer)printers.get( i );
			String name = PrintUtility.handleSlash( bean.getName( ) );
			String status = null; 
				
			if ( bean.getStatus() == Printer.STATUS_ACCEPTING_JOBS )
			{
				status = BirtResources.getMessage( "birt.viewer.dialog.printserver.status.acceptingjobs" ); // TODO: localized key
			}
			else 
			{
				status = BirtResources.getMessage( "birt.viewer.dialog.printserver.status.notacceptingjobs" ); // TODO: localized key
			}
			status = DataUtil.trimString( status );
			
			String model = DataUtil.trimString( bean.getModel( ) );
			String info = DataUtil.trimString( bean.getInfo( ) );
			String copies = "" + bean.getCopies( );
			String mode = "" + bean.getMode( );
			String duplex = "" + bean.getDuplex( );
			String mediaSize = DataUtil.trimString( bean.getMediaSize( ) );
			Map map = bean.getMediaSizeNames( );
			Object[] mediaSizeNames = map.keySet( ).toArray( );
%>
			<SCRIPT LANGUAGE="javascript">
				var printer = new Printer( );
				printer.setName( "<%= name %>" );
				printer.setStatus( "<%= status %>" );
				printer.setModel( "<%= model %>" );
				printer.setInfo( "<%= info %>" );
				
				// Copies attribute
				<%
				if( bean.isCopiesSupported() )
				{
				%>
				printer.setCopiesSupported( true );
				printer.setCopies( "<%= copies %>" );
				<%
				}
				else
				{
				%>	
				printer.setCopiesSupported( false );
				<%
				}
				%>
				
				// Collate attribute
				<%
				if( bean.isCollateSupported() )
				{
				%>
				printer.setCollateSupported( true );
					<%
					if( bean.isCollate( ) )
					{
					%>
				printer.setCollate( true );
					<%
					}
					else
					{
					%>
				printer.setCollate( false );	
				<%
					}
				}
				else
				{
				%>	
				printer.setCopiesSupported( false );
				<%
				}
				%>
				
				// Mode attribute
				<%
				if( bean.isModeSupported( ) )
				{
				%>
				printer.setModeSupported( true );
				printer.setMode( "<%= mode %>" );
				<%
				}
				else
				{
				%>	
				printer.setModeSupported( false );
				<%
				}
				%>				

				// Duplex attribute
				<%
				if( bean.isDuplexSupported( ) )
				{
				%>
				printer.setDuplexSupported( true );
				printer.setDuplex( "<%= duplex %>" );
				<%
				}
				else
				{
				%>	
				printer.setDuplexSupported( false );
				<%
				}
				%>	
				
				// Media attribute
				<%
				if( bean.isMediaSupported( ) )
				{
				%>
				printer.setMediaSupported( true );
				printer.setMediaSize( "<%= mediaSize %>" );
					<%
					for( int j=0; j<mediaSizeNames.length; j++ )
					{
						String mediaSizeName = DataUtil.trimString( (String)mediaSizeNames[j] );
						mediaSizeName = ParameterAccessor.htmlEncode( mediaSizeName );
					%>
				printer.addMediaSizeName( "<%= mediaSizeName %>" );						
				<%
					}
				}
				else
				{
				%>	
				printer.setMediaSupported( false );
				<%
				}
				%>	
				
				if( !printers[index] )								
					printers[index] = {};
					
				printers[index].name = printer.getName( );
				printers[index].value = printer;
				
				index++;
				
			</SCRIPT>
<%		
		}
	}	
%>
<%-----------------------------------------------------------------------------
	Print report on the server dialog fragment
-----------------------------------------------------------------------------%>
<TABLE CELLSPACING="2" CELLPADDING="2" CLASS="birtviewer_dialog_body">
	<TR HEIGHT="5px"><TD></TD></TR>
	<TR>
		<TD>
			<INPUT TYPE="checkbox" ID="print_onserver" <%if( !enable ) { %>DISABLED="true"<%}%>/>
			<%=BirtResources.getMessage( "birt.viewer.dialog.printserver.onserver" )%>
		</TD>
	</TR>
	<TR HEIGHT="5px"><TD></TD></TR>
	<TR>
		<TD>
			<TABLE WIDTH="100%" ID="printer_general">
				<TR>
					<TD WIDTH="80px"><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.printer" )%></TD>
					<TD>						
						<SELECT ID="printer" CLASS="birtviewer_printreportserver_dialog_select"></SELECT>
					</TD>
				</TR>
				<TR>
					<TD><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.status" )%></TD>
					<TD><LABEL ID="printer_status"></LABEL></TD>
				</TR>
				<TR>
					<TD><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.model" )%></TD>
					<TD><LABEL ID="printer_model"></LABEL></TD>
				</TR>
				<TR>
					<TD><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.description" )%></TD>
					<TD><LABEL ID="printer_description"></LABEL></TD>
				</TR>
			</TABLE>
		</TD>
	</TR>			
	<TR HEIGHT="5px"><TD><HR/></TD></TR>
	<TR>
		<TD><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings" )%></TD>
	</TR>	
	<TR>
		<TD>
			<TABLE WIDTH="100%" ID="printer_config">
				<TR>
					<TD WIDTH="100px">
						<%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.copies" )%>
					</TD>
					<TD>
						<INPUT TYPE="text" CLASS="birtviewer_printreportserver_dialog_input_short" ID="printer_copies"/>
						&nbsp;&nbsp;<%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.collate" )%>&nbsp;&nbsp;<INPUT TYPE="checkbox" ID="printer_collate"/>						
					</TD>
				</TR>	
				<TR>
					<TD>
						<%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.duplex" )%>
					</TD>
					<TD>						
						<INPUT TYPE="radio" ID="printer_duplexSimplex" NAME="printerDuplex"/><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.duplex.simplex" )%>
						&nbsp;&nbsp;<INPUT TYPE="radio" ID="printer_duplexHorz" NAME="printerDuplex"/><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.duplex.horizontal" )%>
						&nbsp;&nbsp;<INPUT TYPE="radio" ID="printer_duplexVert" NAME="printerDuplex"/><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.duplex.vertical" )%>			
					</TD>
				</TR>
				<TR>
					<TD>
						<%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.mode" )%>
					</TD>
					<TD>						
						<INPUT TYPE="radio" ID="printer_modeBW" NAME="printerMode"/><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.mode.bw" )%>
						&nbsp;&nbsp;<INPUT TYPE="radio" ID="printer_modeColor" NAME="printerMode"/><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.mode.color" )%>			
					</TD>
				</TR>
				<TR>
					<TD>
						<%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.pagesize" )%>
					</TD>				
					<TD>						
						<SELECT ID="printer_mediasize" CLASS="birtviewer_printreportserver_dialog_select"></SELECT>
					</TD>
				</TR>					
			</TABLE>
		</TD>
	</TR>	
	<TR HEIGHT="5px"><TD><HR/></TD></TR>
	<TR>
		<TD> 
			<DIV ID="printServerPageSetting">
				<TABLE>
					<TR>
						<TD><%=BirtResources.getMessage( "birt.viewer.dialog.printserver.settings.print" )%></TD>
						<TD STYLE="padding-left:5px">
							<INPUT TYPE="radio" ID="printServerPageAll" NAME="printServerPages" CHECKED/><%=BirtResources.getMessage( "birt.viewer.dialog.page.all" )%>
						</TD>
						<TD STYLE="padding-left:5px">	
							<INPUT TYPE="radio" ID="printServerPageCurrent" NAME="printServerPages"/><%=BirtResources.getMessage( "birt.viewer.dialog.page.current" )%>
						</TD>
						<TD STYLE="padding-left:5px">	
							<INPUT TYPE="radio" ID="printServerPageRange" NAME="printServerPages"/><%=BirtResources.getMessage( "birt.viewer.dialog.page.range" )%>
							<INPUT TYPE="text" CLASS="birtviewer_printreportserver_dialog_input" ID="printServerPageRange_input"/>
						</TD>
					</TR>
				</TABLE>			
			</DIV>						
		</TD>
	</TR>	
	<TR>
		<TD>&nbsp;&nbsp;<%=BirtResources.getMessage( "birt.viewer.dialog.page.range.description" )%></TD>
	</TR>	
	<TR HEIGHT="5px"><TD><HR/></TD></TR>
	<TR>
		<TD>
			<DIV ID="printServerFitSetting">
				<TABLE>
					<TR>
						<TD>
							<INPUT TYPE="radio" ID="printServerFitToAuto" NAME="printServerFit" CHECKED/><%=BirtResources.getHtmlMessage( "birt.viewer.dialog.export.pdf.fittoauto" )%>
						</TD>
						<TD>
							<INPUT TYPE="radio" ID="printServerFitToActual" NAME="printServerFit"/><%=BirtResources.getMessage( "birt.viewer.dialog.export.pdf.fittoactual" )%>
						</TD>
						<TD STYLE="padding-left:5px">							
							<INPUT TYPE="radio" ID="printServerFitToWhole" NAME="printServerFit"/><%=BirtResources.getMessage( "birt.viewer.dialog.export.pdf.fittowhole" )%>
						</TD>
					<TR>
				</TABLE>			
			</DIV>			
		</TD>
	</TR>
	<TR HEIGHT="5px"><TD></TD></TR>	
</TABLE>
