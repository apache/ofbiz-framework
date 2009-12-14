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
 *	Birt print report on the server dialog.
 */
BirtPrintReportServerDialog = Class.create( );

BirtPrintReportServerDialog.prototype = Object.extend( new AbstractBaseDialog( ),
{
	__neh_select_change_closure : null,
	__neh_printserver_click_closure : null,
	__neh_pageradio_click_closure : null,
		
	__enable : false,
	__printer : null,	
	
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
		this.__neh_select_change_closure = this.__neh_select_change.bindAsEventListener( this );
		this.__neh_printserver_click_closure = this.__neh_printserver_click.bindAsEventListener( this );
		this.__neh_pageradio_click_closure = this.__neh_pageradio_click.bindAsEventListener( this );
			
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
		
		var oInputs = this.__instance.getElementsByTagName( 'input' );
		Event.observe( oInputs[0], 'click', this.__neh_printserver_click_closure, false );	
		
		var oInputs = $( 'printServerPageSetting' ).getElementsByTagName( 'input' );
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
		var flag = false;
		if( this.__enable )
		{
			this.__collectPrinter( );
			flag = this.__printAction( );
			
			if ( flag )
			{
				birtConfirmationDialog.__cb_bind( );
			}			
		}

		// don't hide if there was an error
		if ( !( this.__enable && !flag ) )
		{
			this.__l_hide( );
		}
		
	},
	
	/**
	 * Collect printer information
	 * 
	 * @return, void
	 * 
	 */
	__collectPrinter : function( )
	{
		if( !this.__printer )
			return;
		
		var oCopies = $( 'printer_copies' );
		if( !oCopies.disabled )
			this.__printer.setCopies( oCopies.value );
		
		var oCollate = $( 'printer_collate' );
		if( !oCollate.disabled )
		{
			if( oCollate.checked )
				this.__printer.setCollate( true );
			else
				this.__printer.setCollate( false );
		}
		
		var oDuplex = $( 'printer_duplexSimplex' );
		if( !oDuplex.disabled && oDuplex.checked )
		{
			this.__printer.setDuplex( this.__printer.DUPLEX_SIMPLEX );	
		}		
		oDuplex = $( 'printer_duplexHorz' );
		if( !oDuplex.disabled && oDuplex.checked )
		{
			this.__printer.setDuplex( this.__printer.DUPLEX_HORIZONTAL );	
		}
		oDuplex = $( 'printer_duplexVert' );
		if( !oDuplex.disabled && oDuplex.checked )
		{
			this.__printer.setDuplex( this.__printer.DUPLEX_VERTICAL );	
		}		
		
		var oMode = $( 'printer_modeBW' );
		if( !oMode.disabled && oMode.checked )
		{
			this.__printer.setMode( this.__printer.MODE_MONOCHROME );	
		}
		oMode = $( 'printer_modeColor' );
		if( !oMode.disabled && oMode.checked )
		{
			this.__printer.setMode( this.__printer.MODE_COLOR );	
		}
		
		var oMediaSize = $( 'printer_mediasize' );
		if( !oMediaSize.disabled )
			this.__printer.setMediaSize( oMediaSize.value );
				
	},
	
	/**
	 * Handle print report action
	 * 
	 * @return, true or false
	 */
	__printAction : function( )
	{	
		if( !this.__printer )
			return false;
				
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

			// Replace "html" to selected output format
			var action = soapURL;
			var reg = new RegExp( "([&|?]{1}" + Constants.PARAM_FORMAT + "\s*)=([^&|^#]*)", "gi" );
			if( action.search( reg ) < 0 )
			{
				action = action + "&" + Constants.PARAM_FORMAT + "=" + Constants.FORMAT_POSTSCRIPT;
			}
			else
			{
				action = action.replace( reg, "$1=" + Constants.FORMAT_POSTSCRIPT );
			}
			
			// Delete page, pagerange and parameterpage settings in url if existed
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_PAGE );
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_PAGERANGE );
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_PARAMETERPAGE );
			action = birtUtility.deleteURLParameter( action, Constants.PARAM_EMITTER_ID );
			
			if( $( 'printServerPageCurrent' ).checked )
			{
				// Set page setting
				var currentPage = birtUtility.trim( $( 'pageNumber' ).innerHTML );
				action = action + "&" + Constants.PARAM_PAGE + "=" + currentPage;				
			}
			else if( $( 'printServerPageRange' ).checked )
			{
				// Set page range setting
				var pageRange = birtUtility.trim( $( 'printServerPageRange_input' ).value );
				if ( !birtUtility.checkPageRange( pageRange ) )
				{
					alert( Constants.error.invalidPageRange );
					return false;
				}
				action = action + "&" + Constants.PARAM_PAGERANGE + "=" + pageRange;
			}			

			// auto
			var pageOverflow = 0;
			//var pagebreakonly = "true";
			
			// fit to actual size
			if( $( 'printServerFitToActual' ).checked )
			{
				pageOverflow = 1;
			}
			//fit to whole page
			else if( $( 'printServerFitToWhole' ).checked )
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
			
			// Set action as print
			reg = new RegExp( "([&|?]{1}" + Constants.PARAM_ACTION + "\s*)=([^&|^#]*)", "gi" );
			if( action.search( reg ) < 0 )
			{
				action = action + "&" + Constants.PARAM_ACTION + "=" + Constants.ACTION_PRINT;
			}
			else
			{
				action = action.replace( reg, "$1=" + Constants.ACTION_PRINT );
			}			
			
			// Post printer settings
			var curPrinter = this.__findPrinter( this.__printer.getName( ) );
			if( curPrinter )
			{
				var param = document.createElement( "INPUT" );
				formObj.appendChild( param );
				param.TYPE = "HIDDEN";
				param.name = Constants.PARAM_PRINTER_NAME;
				param.value = this.__printer.getName( );
					
				if( curPrinter.isCopiesSupported( ) )
				{				
					param = document.createElement( "INPUT" );
					formObj.appendChild( param );
					param.TYPE = "HIDDEN";
					param.name = Constants.PARAM_PRINTER_COPIES;
					param.value = this.__printer.getCopies( );
				}
				
				if( curPrinter.isCollateSupported( ) )
				{				
					param = document.createElement( "INPUT" );
					formObj.appendChild( param );
					param.TYPE = "HIDDEN";
					param.name = Constants.PARAM_PRINTER_COLLATE;
					param.value = new String( this.__printer.isCollate( ) );
				}
				
				if( curPrinter.isDuplexSupported( ) )
				{				
					param = document.createElement( "INPUT" );
					formObj.appendChild( param );
					param.TYPE = "HIDDEN";
					param.name = Constants.PARAM_PRINTER_DUPLEX;
					param.value = this.__printer.getDuplex( );
				}

				if( curPrinter.isModeSupported( ) )
				{				
					param = document.createElement( "INPUT" );
					formObj.appendChild( param );
					param.TYPE = "HIDDEN";
					param.name = Constants.PARAM_PRINTER_MODE;
					param.value = this.__printer.getMode( );
				}	
				
				if( curPrinter.isMediaSupported( ) )
				{				
					param = document.createElement( "INPUT" );
					formObj.appendChild( param );
					param.TYPE = "HIDDEN";
					param.name = Constants.PARAM_PRINTER_MEDIASIZE;
					param.value = this.__printer.getMediaSize( );
				}
			}
						
			formObj.action = action;
			formObj.method = "post";
			formObj.target = "birt_confirmation_iframe";			
			formObj.submit( );
		}
		
		return true;		
	},
	
	/**
	 *	Native event handler for select control.
	 * 
	 * @param event
	 * @return, void
	 */
	__neh_select_change : function( event )
	{
		this.__updateInfo( );
	},
	
	/**
	 * Native event handler for click 'Print On Server' control.
	 * 
	 * @param event
	 * @return, void
	 */
	__neh_printserver_click : function( event )
	{
		var oCtl = Event.element( event );
		
		if( oCtl.checked )
		{
			this.__enable = true;
			$( 'printer' ).disabled = false;						
		}
		else
		{
			this.__enable = false;
			$( 'printer' ).disabled = true;			
		}
		
		// Update info
		this.__updateInfo( );
	},

	/**
	 * Check whether focus on input control
	 * 
	 * @param oSC
	 * @return, void
	 */
	__checkPageRadio : function( oSC )
	{
		if( !oSC || oSC.type != 'radio' )
			return;
			
		var oInput = $( 'printServerPageRange_input' );
		if( oSC.checked && oSC.id == 'printServerPageRange' )
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
	 *	Native event handler for page radio control.
	 */
	__neh_pageradio_click : function( event )
	{
		var oSC = Event.element( event );		
		this.__checkPageRadio( oSC );
	},
			
	/**
	 * Initilize dialog layout
	 * 
	 * @return, void
	 */
	__initLayout : function( )
	{
		var oSelect = $( 'printer' );
		if( !oSelect )
			return;
		
		oSelect.disabled = true;
		for( var i=0; i<printers.length; i++ )
		{		
			var oOption = document.createElement( "OPTION" );
			oOption.text = printers[i].name;
			oOption.value = printers[i].value.getName( );
			oSelect.options[oSelect.options.length] = oOption;
		}
		
		// Update info		
		this.__updateInfo( );		
	},
			
	/**
	 * Insert HTML in a control
	 * 
	 * @param id
	 * @param text
	 * @return, void
	 */
	__insertHTML : function( id,text )
	{
		var oControl = $( id );
		if( oControl )
		{
			oControl.innerHTML = text;
		} 	
	},
	
	/**
	 * Set disabled status for all controls in container 
	 * 
	 * @param id
	 * @param flag
	 * @return, void
	 */
	__setDisabled : function( id, flag )
	{
		var oContainer = $( id );
		if( !oContainer )
			return;
		
		var oSelects = oContainer.getElementsByTagName( "select" );
		if( oSelects )
		{
			for( var i=0; i<oSelects.length; i++ )
				oSelects[i].disabled = flag;
		}
		
		var oInputs = oContainer.getElementsByTagName( "input" );
		if( oInputs )
		{
			for( var i=0; i<oInputs.length; i++ )
				oInputs[i].disabled = flag;
		}		
	},
	
	/**
	 * Find certain printer object by name
	 * 
	 * @param name
	 * @return, void
	 */
	__findPrinter : function( name )
	{
		var curPrinter;
		for( var i=0; i<printers.length; i++ )
		{
			if( name == printers[i].name )
			{
				curPrinter = printers[i].value;
				break;
			}
		}
		
		return curPrinter;
	},
	
	/**
	 * Update generate information
	 * 
	 * @return, void
	 */
	__updateInfo : function( )
	{
		var printerName = $( 'printer' ).value;
		var curPrinter = this.__findPrinter( printerName );		
		if( !curPrinter )
		{
			this.__enable = false;			
			this.__setDisabled( 'printer_config',true );
			this.__setDisabled( 'printServerPageSetting',true );
			this.__setDisabled( 'printServerFitSetting',true );
			return;
		}
			
		this.__printer = curPrinter;	
		
		// Generate info	
		this.__insertHTML( 'printer_status', curPrinter.getStatus( ) );
		this.__insertHTML( 'printer_model', curPrinter.getModel( ) );
		this.__insertHTML( 'printer_info', curPrinter.getInfo( ) );
		
		// Print settings
		if( this.__enable )
		{			
			this.__setDisabled( 'printer_config',false );
			this.__setDisabled( 'printServerPageSetting',false );
			this.__setDisabled( 'printServerFitSetting',false );
			var oInputs = $( 'printServerPageSetting' ).getElementsByTagName( 'input' );
			for( var i=0; i<oInputs.length; i++ )
				this.__checkPageRadio( oInputs[i] );
		}
		else
		{			
			this.__setDisabled( 'printer_config',true );
			this.__setDisabled( 'printServerPageSetting',true );
			this.__setDisabled( 'printServerFitSetting',true );			
		}
		
		if( curPrinter.isCopiesSupported( ) )
		{			
			$( 'printer_copies' ).value = curPrinter.getCopies( );
		}
		else
		{
			$( 'printer_copies' ).disabled = true;
		}
		
		if( curPrinter.isCollateSupported( ) )
		{
			if( curPrinter.isCollate( ) )
				$( 'printer_collate' ).checked = true;
			else
				$( 'printer_collate' ).checked = false;
		}
		else
		{
			$( 'printer_collate' ).disabled = true;
			$( 'printer_collate' ).checked = false;
		}
		
		if( curPrinter.isDuplexSupported( ) )
		{
			var duplex = curPrinter.getDuplex( );
			switch( duplex )
			{
				case curPrinter.DUPLEX_SIMPLEX:
					$( 'printer_duplexSimplex' ).checked = true;
					break;
				case curPrinter.DUPLEX_HORIZONTAL:
					$( 'printer_duplexHorz' ).checked = true;
					break;					
				case curPrinter.DUPLEX_VERTICAL:
					$( 'printer_duplexVert' ).checked = true;
					break;
				default:
					$( 'printer_duplexSimplex' ).checked = true;	
			}			
		}
		else
		{
			$( 'printer_duplexSimplex' ).disabled = true;
			$( 'printer_duplexSimplex' ).checked = false;
			
			$( 'printer_duplexHorz' ).disabled = true;
			$( 'printer_duplexHorz' ).checked = false;
			
			$( 'printer_duplexVert' ).disabled = true;
			$( 'printer_duplexVert' ).checked = false;
		}
		
		if( curPrinter.isModeSupported( ) )
		{
			var mode = curPrinter.getMode( );
			switch( mode )
			{
				case curPrinter.MODE_MONOCHROME:
					$( 'printer_modeBW' ).checked = true;
					break;
				case curPrinter.MODE_COLOR:
					$( 'printer_modeColor' ).checked = true;
					break;
				default:
					$( 'printer_modeBW' ).checked = true;	
			}
		}
		else
		{
			$( 'printer_modeBW' ).disabled = true;
			$( 'printer_modeBW' ).checked = false;
			
			$( 'printer_modeColor' ).disabled = true;
			$( 'printer_modeColor' ).checked = false;
		}
		
		if( curPrinter.isMediaSupported( ) )
		{
			var mediaSize = curPrinter.getMediaSize( );
			var mediaSizeNames = curPrinter.getMediaSizeNames( );
			var oSize = $( 'printer_mediasize' );
			oSize.length = 0;
			for( var i=0; i<mediaSizeNames.length; i++ )
			{
				var oOption = document.createElement( "OPTION" );
				var oLabel = document.createElement( "LABEL" );
				oLabel.innerHTML = mediaSizeNames[i].name;
				oOption.text = oLabel.innerHTML;				
				oOption.value = mediaSizeNames[i].value;
				
				if( mediaSizeNames[i].value == mediaSize )
					oOption.selected = true;
				
				oSize.options[oSize.options.length] = oOption;
			}	
		}
		else
		{
			$( 'printer_mediasize' ).length = 0;
			$( 'printer_mediasize' ).disabled = true;
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