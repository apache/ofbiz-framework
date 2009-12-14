/******************************************************************************
 *	Copyright (c) 2004-2008 Actuate Corporation and others.
 *	All rights reserved. This program and the accompanying materials 
 *	are made available under the terms of the Eclipse Public License v1.0
 *	which accompanies this distribution, and is available at
 *		http://www.eclipse.org/legal/epl-v10.html
 *	
 *	Contributors:
 *		Actuate Corporation - Initial implementation.
 *****************************************************************************/
 
/**
 *	AbstractBaseDocument
 *	...
 */
AbstractBaseReportDocument = function( ) { };

AbstractBaseReportDocument.prototype = Object.extend( new AbstractReportComponent( ),
{
	__instance : null,
	__has_svg_support : false,
	__tocElement : null,
	
	/**
	 *	Event handler closures.
	 */
	__neh_resize_closure : null,
	__neh_select_closure : null,
	__beh_toc_closure : null,
	__beh_getPage_closure : null,
	__beh_changeParameter_closure : null,
	__rtl : null,
		
	__cb_bind : function( data )
	{
		// set rtl only the first time
		if ( this.__rtl == null )
		{
			this.__rtl = false;
			var oRtlElement = data.getElementsByTagName( 'rtl' );
			if ( oRtlElement && oRtlElement[0] && oRtlElement[0].firstChild )
			{
				this.__rtl = ( "true" == oRtlElement[0].firstChild.data );
			}
		}
		
		var documentViewElement = $("documentView");
		documentViewElement.style.direction = this.__rtl?"rtl":"ltr";
		var docObj = document.getElementById( "Document" );
		if ( docObj && BrowserUtility.isMozilla && !BrowserUtility.isFirefox3 )
		{
			docObj.scrollLeft = this.__rtl?(docObj.offsetWidth + "px"):"0px";
		}
	},
	
	/**
	 *	Local version of __cb_installEventHandlers.
	 */
	__local_installEventHandlers : function( id, children, bookmark )
	{
		// jump to bookmark.
		if ( bookmark )
		{
			var obj = $( bookmark );
			if ( obj && obj.scrollIntoView )
			{
				obj.scrollIntoView( true );
			}
		}
	},
	
	/**
	 *	Unregister any birt event handlers.
	 *
	 *	@id, object id
	 *	@return, void
	 */
	__local_disposeEventHandlers : function( id )
	{
	},

	/**
	 *	Handle native event 'click'.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_resize : function( event )
	{
		if ( !this.__updateContainerSize_closure )
		{
			this.__updateContainerSize_closure = this.__updateContainerSize.bind( this );
		}
		
		if ( BrowserUtility.isIE6 || BrowserUtility.isIE7 )
		{
			// delay resizing operation to the event queue
			// else IE might override our changes with its calculated ones
			setTimeout( this.__updateContainerSize_closure, 0);
		}
		else
		{
			this.__updateContainerSize();
		}
	},
	
	/**
	 * Updates the container size according to the window size.
	 */
	__updateContainerSize : function()
	{
		var tocWidth = 0;
		if ( this.__tocElement && this.__tocElement.__instance )
		{
			tocWidth = this.__tocElement.getWidth();
		}
		
		var width = BirtPosition.viewportWidth( ) - tocWidth - (BrowserUtility.isFirefox?6:4);

		// in IE, the width contains the border, unlike in other browsers
		if ( BrowserUtility.isIE6 || BrowserUtility.isIE7 || 
				BrowserUtility.isOpera || BrowserUtility.isKHTML || BrowserUtility.isSafari )
		{
			var containerLeft = 0;
			// if viewer in rtl mode
			if ( Constants.request.rtl )
			{
				// if report in rtl mode
				if ( birtReportDocument && birtReportDocument.__rtl )
				{
					if ( BrowserUtility.isKHTML || BrowserUtility.isSafari )
					{
						containerLeft = 0;
					}
					else
					{
						containerLeft = -tocWidth;
					}
				}
				else
				{
					containerLeft = tocWidth;
				}
				
				this.__instance.style.left = containerLeft + "px";
			}
			else
			{
				this.__instance.style.left = "0px";
			}
		}
		if( width > 0 )
			this.__instance.style.width = width + "px";
			
		var height = BirtPosition.viewportHeight( ) - this.__instance.offsetTop - 2;
		if( height > 0 )
			this.__instance.style.height = height + "px";		
		
		this.__instance.style.left = containerLeft + "px";

		if (BrowserUtility.isIE) {
			var reportContainer = this.__instance.firstChild;

			if (reportContainer != null) {
				var scrollBarWidth = BrowserUtility._getScrollBarWidth(reportContainer, width, height);
				var containerWidth = "100%";

				if (height < reportContainer.offsetHeight && width > scrollBarWidth) {
					containerWidth = (width - scrollBarWidth) + "px";
				}
				reportContainer.style.overflowX = "visible";
				reportContainer.style.overflowY = "visible";
				reportContainer.style.position = "relative";
				reportContainer.style.width = containerWidth;
			}
		}
	},
	
	/**
	 *	Birt event handler for "getpage" event.
	 *
	 *	@id, document id (optional since there's only one document instance)
	 *	@return, true indicating server call
	 */
	__beh_parameter : function( id )
	{
		birtParameterDialog.__cb_bind( );
	},

	/**
	 *	Birt event handler for "change parameter" event.
	 *
	 *	@id, document id (optional since there's only one document instance)
	 *	@return, true indicating server call
	 */
	__beh_changeParameter : function( id )
	{
		// set task id
		var taskid = birtUtility.setTaskId( );
		
		if ( birtParameterDialog.__parameter > 0 )
		{
	        birtParameterDialog.__parameter.length = birtParameterDialog.__parameter.length - 1;
		}
		
		// Get current page number
		var pageNum = birtUtility.getPageNumber( );
		
		if( pageNum > 0 )
		{		
	        birtSoapRequest.addOperation( Constants.documentId, Constants.Document,
	        							  "ChangeParameter", null, birtParameterDialog.__parameter,
										  { name : Constants.PARAM_SVG, value : this.__has_svg_support? "true" : "false" },
										  { name : Constants.PARAM_PAGE, value : pageNum },
										  { name : Constants.PARAM_TASKID, value : taskid } );
		}
		else
		{
	        birtSoapRequest.addOperation( Constants.documentId, Constants.Document,
	        							  "ChangeParameter", null, birtParameterDialog.__parameter,
										  { name : Constants.PARAM_SVG, value : this.__has_svg_support? "true" : "false" },
										  { name : Constants.PARAM_TASKID, value : taskid } );			
		}
		birtSoapRequest.setURL( soapURL );
		birtProgressBar.setRedirect( true );
		birtEventDispatcher.setFocusId( null );	// Clear out current focusid.
		return true;
	},
	
	/**
	 *	Handle change cascade parameter.
	 */
	__beh_cascadingParameter : function( id, object )
	{
		// set task id
		var taskid = birtUtility.setTaskId( );
		
	    birtSoapRequest.addOperation( Constants.documentId, Constants.Document,
	    							 "GetCascadingParameter", null, object,
	    							 { name : Constants.PARAM_TASKID, value : taskid } );
		birtSoapRequest.setURL( soapURL );
		birtEventDispatcher.setFocusId( null );	// Clear out current focusid.
		return true;
	},

	/**
	 *	Handle native event 'click'.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__beh_toc : function( id )
	{
		// resize document window
		this.__neh_resize();
	},

	/**
	 *	Birt event handler for "getpage" event.
	 *
	 *	@param id, document id (optional since there's only one document instance)
	 *  @param object, pass some settings, for example: page,bookmark...
	 *	@return, true indicating server call
	 */
	__beh_getPage : function( id, object )
	{
		// set task id
		var taskid = birtUtility.setTaskId( );
		
		var url = soapURL;
		// if set bookmark, delete the bookmark parameter in URL
		if( object && object.name && object.name == Constants.PARAM_BOOKMARK )
		{
			url = birtUtility.deleteURLParameter( url, Constants.PARAM_BOOKMARK );
			url = birtUtility.deleteURLParameter( url, Constants.PARAM_ISTOC );
		}
		
		birtSoapRequest.setURL( url );
		if ( object )
		{
			birtSoapRequest.addOperation( Constants.documentId, Constants.Document,
										  "GetPage", null,
										  object,
										  { name : Constants.PARAM_SVG, value : this.__has_svg_support? "true" : "false" },
										  { name : Constants.PARAM_TASKID, value : taskid } );
		}
		else
		{
			birtSoapRequest.addOperation( Constants.documentId, Constants.Document,
										  "GetPage", null,
										  { name : Constants.PARAM_SVG, value : this.__has_svg_support? "true" : "false" },
										  { name : Constants.PARAM_TASKID, value : taskid } );
		}

		birtEventDispatcher.setFocusId( null );	// Clear out current focusid.
		birtProgressBar.setRedirect( true );
		return true;
	},

	/**
	 *	Birt event handler for "getpage" event with collected parameters.
	 *
	 *	@param id, document id (optional since there's only one document instance)
	 *  @param object, pass some settings, for example: page...
	 *	@return, true indicating server call
	 */
	__beh_getPageInit : function( id, object )
	{
		// set task id
		var taskid = birtUtility.setTaskId( );
		
		// Get current page number
		var pageNum = birtUtility.getPageNumber( );
		
		birtSoapRequest.setURL( soapURL );
		if ( object )
		{
			if( pageNum > 0 )
			{
				birtSoapRequest.addOperation( Constants.documentId, Constants.Document,
											  "GetPage", null, birtParameterDialog.__parameter,
											  object,
											  { name : Constants.PARAM_SVG, value : this.__has_svg_support? "true" : "false" },
											  { name : Constants.PARAM_PAGE, value : pageNum },
											  { name : Constants.PARAM_TASKID, value : taskid } );
			}
			else
			{
				birtSoapRequest.addOperation( Constants.documentId, Constants.Document,
											  "GetPage", null, birtParameterDialog.__parameter,
											  object,
											  { name : Constants.PARAM_SVG, value : this.__has_svg_support? "true" : "false" },
											  { name : Constants.PARAM_TASKID, value : taskid } );				
			}
		}
		else
		{
			if( pageNum > 0 )
			{
				birtSoapRequest.addOperation( Constants.documentId, Constants.Document,
											  "GetPage", null, birtParameterDialog.__parameter,
											  { name : Constants.PARAM_SVG, value : this.__has_svg_support? "true" : "false" },
											  { name : Constants.PARAM_PAGE, value : pageNum },
											  { name : Constants.PARAM_TASKID, value : taskid } );				
			}
			else
			{
				birtSoapRequest.addOperation( Constants.documentId, Constants.Document,
											  "GetPage", null, birtParameterDialog.__parameter,
											  { name : Constants.PARAM_SVG, value : this.__has_svg_support? "true" : "false" },
											  { name : Constants.PARAM_TASKID, value : taskid } );
			}
		}

		birtEventDispatcher.setFocusId( null );	// Clear out current focusid.
		birtProgressBar.setRedirect( true );
		return true;
	},
	
	/**
	 *	Birt event handler for "print" event.
	 *
	 *	@id, document id (optional since there's only one document instance)
	 *	@return, true indicating server call
	 */
	__beh_export : function( id )
	{
		birtSoapRequest.setURL( soapURL);
		birtSoapRequest.addOperation( "Document", Constants.Document, "QueryExport", null );
		return true;
	}
});
