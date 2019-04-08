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
 *	Progress bar for JSP tag
 */
ProgressBar = Class.create( );
ProgressBar.prototype = 
{
	/**
	 *	UI component html instance.
	 */
	__instance : null,
	
	/**
	 *  SOAP action handler  
	 */
	__handler : null,
	
	/**
	 *	Latency that will trigger the progress bar.
	 */
	__interval : 300,
	
	/**
	 *	Timer instance.
	 */
	__timer : null,
	
	/**
	 *	mask instance.
	 */
	__mask : null,
	
	/**
	 *	Closures
	 */
	__cb_bind_closure : null,
	
	/**
	 *	Event handler for click 'cancel' button
	 */
	__neh_click_closure : null,	
	
	/**
	 *	'Cancel' button container
	 */
	__cancel_button : 'cancelTaskButton',
	 
	/**
	 * The input control to save 'taskid'
	 */
	__task_id : 'taskid',
			 	
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *	@return, void
	 */
	initialize : function( id, maskId )
	{
		this.__instance = $( id );
		this.__mask = this.__create_mask( maskId );		
		this.__cb_bind_closure = this.__cb_bind.bindAsEventListener( this );
		this.__neh_click_closure = this.__neh_click.bindAsEventListener( this );
				
		this.__installEventHandlers( id );
	},

	/**
	 *	Set action handler
	 *
	 *	@return, void
	 */
	setHandler : function( handler )
	{
		this.__handler = handler;
	},

	/**
	 *	Binding data to the dialog UI. Data includes zoom scaling factor.
	 *
	 *	@return, void
	 */
	__cb_bind : function( )
	{
		if( !this.__handler ) return;
		
		if( this.__handler.__active )
		{
			this.__timer = window.setTimeout( this.__cb_bind_closure, this.__interval );
			this.__l_show( );
		}
		else
	  	{
			window.clearTimeout( this.__timer );
			this.__l_hide( );
	  	}
	},
	
	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, response id 
	 *	@return, void
	 */
	__installEventHandlers : function( id )
	{
		var oCancel = this.__loc_cancel_button( );
		if( oCancel )
			Event.observe( oCancel, 'click', this.__neh_click_closure, false );
	},

	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, response id 
	 *	@return, void
	 */
	__start : function( )
	{	
		// check taskid
		var taskid = birtUtility.getTaskId( );
		if( taskid.length > 0 )
		{
			// if taskid existed, show 'Cancel' button
			this.__l_show_cancel_button( );
			
			// enable 'cancel' button
			var oCancel = this.__loc_cancel_button( );
			if( oCancel )
				oCancel.disabled = false;
		}
		else
		{
			// hide 'Cancel' button
			this.__l_hide_cancel_button( );
		}
					
		this.__timer = window.setTimeout( this.__cb_bind_closure, this.__interval );
	},
	
	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, response id 
	 *	@return, void
	 */
	__stop : function( )
	{
		window.clearTimeout( this.__timer );
		this.__l_hide( );
		
		// clear taskid
		birtUtility.clearTaskId( );
	},
	
	/**
	 *	Create progress bar mask.
	 *	
	 *	@return, __mask
	 */
	__create_mask : function( maskId )
	{
		var oMask = $( maskId );
		if( !oMask ) return null;
		
		oMask.style.position = 'absolute';
		oMask.style.top = '0px';
		oMask.style.left = '0px';
		oMask.style.width = '50%';
		var height = BirtPosition.viewportHeight( );
		oMask.style.height = height + 'px';
		oMask.style.backgroundColor = '#dbe4ee';
		oMask.style.filter = 'alpha( opacity = 0.0 )';
		oMask.style.opacity = '.0';		
		oMask.scrolling = 'no';
		oMask.marginHeight = '0px';
		oMask.marginWidth = '0px';
		oMask.style.display = 'none';
		oMask.style.MozOpacity = 0;					
		
		return oMask;		
	},

	/**
	 *	Show progress bar.
	 */
	__l_show : function( )
	{
		Element.show( this.__mask, this.__instance );		
		BirtPosition.center( this.__instance );
	},
	
	/**
	 *	Hide progress bar.
	 */
	__l_hide : function( )
	{
		Element.hide( this.__instance, this.__mask );
	},

	/**
	 *  Returns 'cancel' button
	 * 	@return, INPUT
	 */
	__loc_cancel_button: function( )
	{
		var oIEC = this.__instance.getElementsByTagName( "input" );
		var oCancel;
		if( oIEC && oIEC.length > 0 )
		{
			for( var i = 0 ; i < oIEC.length; i++ )
			{
				if( oIEC[i].type == 'button' )
				{
					oCancel = oIEC[i];
					break;
				}
			}
		}
		
		return oCancel;
	},

	/**
	 *	Handle click "Cancel" button.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_click: function( event )
	{
		var oTaskId = document.getElementById( this.__task_id );
		if( oTaskId && window.confirm( "Do you want to cancel current task?" ) )
		{
			Event.element( event ).disabled = true;
			
			// cancel task
			birtSoapRequest.addOperation( Constants.documentId, Constants.Document, "CancelTask", null, { name : Constants.PARAM_TASKID, value : oTaskId.value } );
			birtSoapRequest.setURL( this.__handler.getAction( ) );
			
			if ( !birtSoapRequest.getURL( ) ) return;
			
			var myAjax = new Ajax.Request( birtSoapRequest.getURL( ), { method: 'post', postBody: birtSoapRequest.__xml_document,
			onSuccess: this.responseHandler, onFailure: this.invalidResponseHandler,
			requestHeaders: ['Content-type', 'text/xml; charset=utf-8', 'SOAPAction', '""', 'request-type', 'SOAP', 'Connection', 'keep-alive' ] } );

			birtSoapRequest.reset( );												
		}
	},

	/**
	 *	Callback function triggered when reponse is ready, status is 200.
	 *
	 *	@request, httpXmlRequest instance
	 *	@return, void
	 */
	responseHandler: function( request )
	{				
		progressBar.__stop( );
	},
	
	/**
	 *	Callback function triggered when reponse is ready status is not 200.
	 *	Process any http (non-200) errors. Note this is not exception from
	 *	server side.
	 *
	 *	@request, httpXmlRequest instance
	 *	@return, void
	 */
	invalidResponseHandler: function( request )
	{					
		progressBar.__stop( );
	},
	
	/**
	 *	Show "Cancel" button.
	 *
	 *	@return, void
	 */	
	__l_show_cancel_button: function( )
	{
		var container = document.getElementById( this.__cancel_button );
		if( container )
			container.style.display = 'block';
	},

	/**
	 *	Hide "Cancel" button.
	 *
	 *	@return, void
	 */		
	__l_hide_cancel_button: function( )
	{
		var container = document.getElementById( this.__cancel_button );
		if( container )
			container.style.display = 'none';
	}
}