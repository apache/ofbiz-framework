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
 *	Birt print report dialog.
 */
BirtPrintReportDialog = Class.create( );

BirtPrintReportDialog.prototype = Object.extend( new AbstractBaseDialog( ),
{
	/**
	 * Print window instance.
	 */
	__printWindow : null,

	/**
	 * Timer instance control the popup print dialog.
	 */
	__timer : null,
		
	__printFormat : 'html',
	__neh_formatradio_click_closure : null,
	__neh_pageradio_click_closure : null,
	
	/**
	* PDF page fit setting
	*/
	FIT_TO_ACTUAL : '0',
	FIT_TO_WHOLE  : '1',
	
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *	@return, void
	 */
	initialize : function( id )
	{
		this.__initBase( id );
		this.__z_index = 200;
		
		this.__initLayout( );		
		
		// Binding
		this.__neh_formatradio_click_closure = this.__neh_formatradio_click.bindAsEventListener( this );
		this.__neh_pageradio_click_closure = this.__neh_pageradio_click.bindAsEventListener( this );
			
		this.__installEventHandlers( id );		
	},
	
	/**
	 * Initilize dialog layout
	 * 
	 * @return, void
	 */
	__initLayout : function( )
	{
	
	},
	
	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, toolbar id (optional since there is only one toolbar)
	 *	@return, void
	 */
	__installEventHandlers : function( id )
	{	
		// switch print format
		var oInputs = $( 'printFormatSetting' ).getElementsByTagName( 'input' );
		for( var i=0; i<oInputs.length; i++ )
		{
			if( oInputs[i].type == 'radio' )			
				Event.observe( oInputs[i], 'click', this.__neh_formatradio_click_closure,false );
		}

		// page setting
		var oInputs = $( 'printPageSetting' ).getElementsByTagName( 'input' );
		for( var i=0; i<oInputs.length; i++ )
		{
			if( oInputs[i].type == 'radio' )			
				Event.observe( oInputs[i], 'click', this.__neh_pageradio_click_closure,false );
		}					
	},

	/**
	 *	Handle clicking on ok.
	 *
	 *	@return, void
	 */
	__okPress : function( )
	{
		if ( this.__printAction( ) )
		{
			this.__l_hide( );
		}
	},
		
	/**
	 * Handle print report action
	 * 
	 * @return, true or false
	 */
	__printAction : function( )
	{
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

			// Replace format in URL with selected print format
			var action = soapURL;
			var reg = new RegExp( "([&|?]{1}" + Constants.PARAM_FORMAT + "\s*)=([^&|^#]*)", "gi" );
			if( action.search( reg ) < 0 )
			{
				action = action + "&" + Constants.PARAM_FORMAT + "=" + this.__printFormat;
			}
			else
			{
				action = action.replace( reg, "$1=" + this.__printFormat );
			}
			
			// Delete page, pagerange and parameterpage settings in url if existed
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_PAGE );
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_PAGERANGE );
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_PARAMETERPAGE );
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_EMITTER_ID );
			
			if( $( 'printPageCurrent' ).checked )
			{
				// Set page setting
				var currentPage = birtUtility.trim( $( 'pageNumber' ).innerHTML );
				action = action + "&" + Constants.PARAM_PAGE + "=" + currentPage;				
			}
			else if( $( 'printPageRange' ).checked )
			{
				// Set page range setting
				var pageRange = birtUtility.trim( $( 'printPageRange_input' ).value );
				if ( !birtUtility.checkPageRange( pageRange ) )
				{
					alert( Constants.error.invalidPageRange );
					return false;
				}
				action = action + "&" + Constants.PARAM_PAGERANGE + "=" + pageRange;
			}			

			var oSelect = this.__instance.getElementsByTagName( 'select' )[0];
			var pageOverflow = 0;
			//var pagebreakonly = "false";
			
			// 
			if( oSelect.selectedIndex >=0 )
				pageOverflow = oSelect.value;

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

			if ( Constants.viewingSessionId )
			{
				// append sub session in the POST part
				birtUtility.addHiddenFormField(formObj, Constants.PARAM_SESSION_ID, Constants.viewingSessionId);
				action = birtUtility.deleteURLParameter(action, Constants.PARAM_SESSION_ID);
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

			// Replace servlet pattern as output
			action = action.replace( /[\/][a-zA-Z]+[?]/, "/"+Constants.SERVLET_OUTPUT+"?" );

			var previewExists = false;
			// retrieve previous window instance
			var previousPrintWindow = window.open( '', Constants.WINDOW_PRINT_PREVIEW, 'location=no,scrollbars=yes,dependent=yes' );
			try
			{
				// if the window didn't exist, then window.open() has opened an empty window
				var previousBodyElement = previousPrintWindow.document.getElementsByTagName("body")[0];				
				if ( previousBodyElement && birtUtility.trim( previousBodyElement.innerHTML ).length > 0 )
				{
					previewExists = true;
				}
			}
			catch ( e )
			{
				// access denied is thrown if the previous preview window contains a PDF content
				previewExists = true;
			}

			if ( previewExists )
			{
				// workaround for Bugzilla Bug 227937
				window.setTimeout( function () { alert( Constants.error.printPreviewAlreadyOpen ) }, 0 );
				return false;
			}
			else
			{
				// use the created window as current window
				this.__printWindow = previousPrintWindow;
			}

			if ( !BrowserUtility.__isIE() )
			{
				// use onload event for the callback when page is loaded
				Event.observe( this.__printWindow, 'load', this.__cb_print.bindAsEventListener( this ), false );
			}
			
			formObj.action = action;
			formObj.method = "post";
			formObj.target = Constants.WINDOW_PRINT_PREVIEW;			
			formObj.submit( );
			
			// Launch the browser's print dialog (IE/Safari workaround)
			// Note: calling the print dialog for PDF in IE doesn't work (permission denied)
			if ( ( BrowserUtility.__isIE() && this.__printFormat != 'pdf' ) || BrowserUtility.__isSafari() )
			{
				this.__timer = window.setTimeout( this.__cb_waitPreviewLoaded.bindAsEventListener( this ), 1000 );
			}			
		}
		
		return true;		
	},

	/**
	 * Waits until the print preview is loaded (IE only)
	 */
	__cb_waitPreviewLoaded : function( )
	{
		window.clearTimeout( this.__timer );
	
		try
		{		
			if ( !this.__printWindow || this.__printWindow.closed )
			{
				return;
			}
		
			if ( !this.__printWindow.document || this.__printWindow.document.readyState != "complete" )
			{
				// wait a little longer
				this.__timer = window.setTimeout( this.__cb_waitPreviewLoaded.bindAsEventListener( this ), 1000 );
			}
			else
		  	{
				this.__cb_print();
		  	}
		}
		catch ( error )
		{
			// IE throws a permission denied exception if the user closes
			// the window too early. In this case ignore the exception.
		}
	},

	/**
	 * Control the browser's popup print dialog.
	 *
	 * Below are the implemented functions for the given browsers and output formats.
	 * Function              IE       Mozilla/Safari
	 * window.print()       HTML       HTML,PDF(delay)
	 *
	 */
	__cb_print : function( )
	{
		try
		{	
			if ( !this.__printWindow || this.__printWindow.closed )			
			{
				return;
			}

			var err = this.__printWindow.document.getElementById( "birt_errorPage" );
			if( err && err.innerHTML != '' )
			{
				return;
			}
			
			// Call the browser's print dialog (async)
			if ( this.__printFormat == 'pdf' ) // Mozilla only
			{
				// Mozilla needs some delay after loading PDF
				this.__printWindow.setTimeout( "window.print();", 1000 );
			}
			else
			{
				// defer call to let the window draw its content
				// (Firefox Bugzilla bug 213666)				
				this.__printWindow.setTimeout( "window.print();", 0 );
			}
		}
		catch ( error )
		{
			// IE throws a permission denied exception if the user closes
			// the window too early. In this case ignore the exception.
		}
	},
	
	/**
	 *	Native event handler for print format radio control.
	 */
	__neh_formatradio_click : function( event )
	{
		var oSC = Event.element( event );
		var oSelect = this.__instance.getElementsByTagName( 'select' )[0];
		if( oSC.checked && oSC.id == 'printAsPDF' )
		{
			this.__printFormat = 'pdf';
			oSelect.disabled = false;
			oSelect.focus();
		}
		else
		{
			this.__printFormat = 'html';
			oSelect.disabled = true;
		}
	},

	/**
	 *	Native event handler for page radio control.
	 */
	__neh_pageradio_click : function( event )
	{
		var oSC = Event.element( event );	
		var oInput = $( 'printPageRange_input' );
		if( oSC.checked && oSC.id == 'printPageRange' )
		{
			oInput.disabled = false;
			oInput.focus( );
		}
		else
		{
			oInput.disabled = true;
			oInput.value = "";
		}
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