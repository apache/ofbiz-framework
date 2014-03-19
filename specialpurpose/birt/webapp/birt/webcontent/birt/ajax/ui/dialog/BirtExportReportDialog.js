/******************************************************************************
 *	Copyright (c) 2004 Actuate Corporation and others.
 *	All rights reserved. This program and the accompanying materials 
 *	are made available under the terms of the Eclipse Public License v1.0
 *	which accompanies this distribution, and is available at
 *		http://www.eclipse.org/legal/epl-v10.html
 *	
 *	Contributors:
 *		Actuate Corporation - Initial implementation.
 *****************************************************************************/
 
/**
 *	Birt export report dialog.
 */
BirtExportReportDialog = Class.create( );

BirtExportReportDialog.prototype = Object.extend( new AbstractBaseDialog( ),
{
	__neh_select_change_closure : null,
	__neh_radio_click_closure : null,
	
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *	@return, void
	 */
	initialize : function( id )
	{
		this.__initBase( id );
		this.__z_index = 200;
		
		this.__enableExtSection( );
		
		// Binding
		this.__neh_select_change_closure = this.__neh_select_change.bindAsEventListener( this );
		this.__neh_radio_click_closure = this.__neh_radio_click.bindAsEventListener( this );
			
		this.__installEventHandlers( id );
	},
	
	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, toolbar id (optional since there is only one toolbar)
	 *	@return, void
	 */
	__installEventHandlers : function( id )
	{
		var oSelects = this.__instance.getElementsByTagName( 'select' );
		Event.observe( oSelects[0], 'change', this.__neh_select_change_closure, false );
		
		var oInputs = $( 'exportPageSetting' ).getElementsByTagName( 'input' );
		for( var i=0; i<oInputs.length; i++ )
		{
			if( oInputs[i].type == 'radio' )			
				Event.observe( oInputs[i], 'click', this.__neh_radio_click_closure,false );
		}		
	},

	/**
	 *	Handle clicking on ok.
	 *
	 *	@return, void
	 */
	__okPress : function( )
	{
		var oSelect = $( 'exportFormat' );
		if( oSelect.value == '' )
			return;
		
		if ( this.__exportAction( ) )
		{
			this.__l_hide( );
		}
	},
	
	/**
	 * Handle export report action
	 * 
	 * @return, void
	 */
	__exportAction : function( )
	{
		var format = $( 'exportFormat' ).value.toLowerCase( );
		
		var docObj = document.getElementById( "Document" );
		if ( !docObj || birtUtility.trim( docObj.innerHTML ).length <= 0)
		{
			alert ( Constants.error.generateReportFirst );	
			return false;
		}	
		else
		{	
			var divObj = document.createElement( "DIV" );
			document.body.appendChild( divObj );
			divObj.style.display = "none";
		
			var formObj = document.createElement( "FORM" );
			divObj.appendChild( formObj );

			// Set selected output format
			var action = soapURL;
			var reg = new RegExp( "([&|?]{1}" + Constants.PARAM_FORMAT + "\s*)=([^&|^#]*)", "gi" );
			if( action.search( reg ) < 0 )
			{
				action = action + "&" + Constants.PARAM_FORMAT + "=" + format;
			}
			else
			{
				action = action.replace( reg, "$1=" + format );
			}

			// Delete page, pagerange and parameterpage settings in url if existed
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_PAGE );
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_PAGERANGE );
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_PARAMETERPAGE );
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_EMITTER_ID );
			
			if( $( 'exportPageCurrent' ).checked )
			{
				// Set page setting
				var currentPage = birtUtility.trim( $( 'pageNumber' ).innerHTML );
				action = action + "&" + Constants.PARAM_PAGE + "=" + currentPage;				
			}
			else if( $( 'exportPageRange' ).checked )
			{
				// Set page range setting
				var pageRange = birtUtility.trim( $( 'exportPageRange_input' ).value );
				if ( !birtUtility.checkPageRange( pageRange ) )
				{
					alert( Constants.error.invalidPageRange );
					return false;
				}
				action = action + "&" + Constants.PARAM_PAGERANGE + "=" + pageRange;
			}			
			
			// If output format is pdf/ppt/postscript, set some options
			if( this.__isPDFLayout( format ) )
			{
				// auto fit
				var pageOverflow = 0;
				//var pagebreakonly = "true";
				
				// actual size
				if( $( 'exportFitToActual' ).checked )
				{
					pageOverflow = 1;
				}
				else if( $( 'exportFitToWhole' ).checked )
				{
					pageOverflow = 2;
				}

				reg = new RegExp( "([&|?]{1}" + Constants.PARAM_PAGE_OVERFLOW + "\s*)=([^&|^#]*)", "gi" );
				if( action.search( reg ) < 0 )
				{
					action = action + "&" + Constants.PARAM_PAGE_OVERFLOW + "=" + pageOverflow;
				}
				else
				{
					action = action.replace( reg, "$1=" + pageOverflow );
				}
				
				/*
				reg = new RegExp( "([&|?]{1}" + Constants.PARAM_PAGEBREAKONLY + "\s*)=([^&|^#]*)", "gi" );
				if( action.search( reg ) < 0 )
				{
					action = action + "&" + Constants.PARAM_PAGEBREAKONLY + "=" + pagebreakonly;
				}
				else
				{
					action = action.replace( reg, "$1=" + pagebreakonly );
				}
				*/							
			}
			
			// Force "__asattachment" as true
			reg = new RegExp( "([&|?]{1}" + Constants.PARAM_ASATTACHMENT + "\s*)=([^&|^#]*)", "gi" );
			if( action.search( reg ) < 0 )
			{
				action = action + "&" + Constants.PARAM_ASATTACHMENT + "=true";
			}
			else
			{
				action = action.replace( reg, "$1=true" );
			}			
			
			// Force "__overwrite" as false
			reg = new RegExp( "([&|?]{1}" + Constants.PARAM_OVERWRITE + "\s*)=([^&|^#]*)", "gi" );
			if( action.search( reg ) < 0 )
			{
				action = action + "&" + Constants.PARAM_OVERWRITE + "=false";
			}
			else
			{
				action = action.replace( reg, "$1=false" );
			}
						
			formObj.action = action;
			formObj.method = "post";			
			formObj.submit( );
			
			return true;
		}		
	},
	
	/**
	 *	Native event handler for radio control.
	 */
	__neh_radio_click : function( event )
	{
		var oSC = Event.element( event );		
		if( oSC.type == 'radio' )
		{
			var oInput = $( 'exportPageRange_input' );
			if( oSC.id == 'exportPageRange' )
			{
				oInput.disabled = false;
				oInput.focus( );
			}
			else
			{
				oInput.disabled = true;
				oInput.value = "";
			}
		}		
	},
	
	/**
	 *	Native event handler for select control.
	 */
	__neh_select_change : function( event )
	{
		this.__enableExtSection( );		
	},
	
	/**
	 * Enable the extended setting controls according to current selected output format.
	 */
	__enableExtSection : function( )
	{		
		var format = $( 'exportFormat' ).value.toLowerCase( );
		if( this.__isPDFLayout( format ) )
		{
			this.__setDisabled( 'exportFitSetting', false );
		}
		else
		{
			this.__setDisabled( 'exportFitSetting', true );
		}
	},
	
	/**
	 * Set disabled flag for all the controls in the container
	 * 
	 * @param id, html container id. ( DIV/TABLE....)
	 * @param flag, true or false
	 * @return, void
	 */
	__setDisabled: function( id, flag )
	{
		var container = $( id );
		if( container )
		{
			var oInputs = container.getElementsByTagName( 'input' );
			for( var i=0; i<oInputs.length; i++ )
				oInputs[i].disabled = flag;
		}
	},
	
	/**
	 * Check whether this format uses the PDF layout
	 *
	 * @param format, the output format 
	 * @return true or false
	 */	 
	__isPDFLayout : function( format )
	{
		if( !format )
			return false;
		
		if( format == Constants.FORMAT_PDF 
		    || format == Constants.FORMAT_POSTSCRIPT
		    || format == Constants.FORMAT_PPT )
		{
			return true;
		}    
		
		return false;
	},
		 
	/**
	Called right before element is shown
	*/
	__preShow: function()
	{
		// disable the toolbar buttons
		birtUtility.setButtonsDisabled ( "toolbar", true );
		
		// disable the Navigation Bar buttons
		birtUtility.setButtonsDisabled ( "navigationBar", true );
	},
	
	/**
	Called before element is hidden
	*/
	__preHide: function()
	{
		// enable the toolbar buttons
		birtUtility.setButtonsDisabled ( "toolbar", false );
		
		// enable the Navigation Bar buttons
		birtUtility.setButtonsDisabled ( "navigationBar", false );		
	}	
} );