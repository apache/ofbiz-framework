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
 *	BirtProgressBar
 *	...
 */
BirtProgressBar = Class.create( );
BirtProgressBar.prototype = Object.extend( new AbstractUIComponent( ),
{
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
	 * When click cancel button, indicate whether redirect a cancel page
	 */
	__redirect : false,
	
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *	@return, void
	 */
	initialize : function( id )
	{
		this.__initBase( id );
		this.__mask = this.__create_mask( );
		this.__cb_bind_closure = this.__cb_bind.bindAsEventListener( this );
		this.__neh_click_closure = this.__neh_click.bindAsEventListener( this );
		
		this.__installEventHandlers( id );
	},

	/**
	 *	Binding data to the dialog UI. Data includes zoom scaling factor.
	 *
	 *	@return, void
	 */
	__cb_bind : function( )
	{
		if( birtCommunicationManager.__active )
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
	 *	Install native/birt event handlers.
	 *
	 *	@id, response id 
	 *	@return, void
	 */
	__create_mask : function( )
	{
		var oMask = document.createElement( 'iframe' );
		// Workaround for IE https secure warning
		oMask.src = "birt/pages/common/blank.html";
		oMask.style.position = 'absolute';
		oMask.style.top = '0px';
		oMask.style.left = '0px';
		oMask.style.width = '100%';
		var height = BirtPosition.viewportHeight( );
		oMask.style.height = height + 'px';
		oMask.style.zIndex = '300';
		oMask.style.backgroundColor = '#dbe4ee';
		oMask.style.filter = 'alpha( opacity = 0.0 )';
		oMask.style.opacity = '.0';
		oMask.scrolling = 'no';
		oMask.marginHeight = '0px';
		oMask.marginWidth = '0px';
		oMask.style.display = 'none';
		document.body.appendChild( oMask );
		
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
		if( oTaskId && window.confirm( Constants.error.confirmCancelTask ) )
		{	
			if( this.__redirect )
			{
				this.cancel( oTaskId.value );	
			}
			else
			{
				birtEventDispatcher.broadcastEvent( birtEvent.__E_CANCEL_TASK, { name : Constants.PARAM_TASKID, value : oTaskId.value } );
				Event.element( event ).disabled = true;
			}			
		}	
	},
	
	/**
	 *	Try to cancel the process.
	 *
	 *	@return, void
	 */
	cancel: function( taskid )
	{
		if( !taskid )
		{
			var oTaskId = document.getElementById( this.__task_id );
			if( oTaskId )
				taskid = oTaskId.value;
		}
						 
		if( !taskid || taskid.length <= 0 )
			return;
						 
		var hiddenForm = document.createElement( 'form' );
		hiddenForm.method = 'post';
		hiddenForm.target = '_self';
		var url = soapURL;
		url = url.replace( /[\/][a-zA-Z]+[?]/, '/CancelTask.jsp?' );
		hiddenForm.action = url;
	
		var taskidInput = document.createElement( 'input' );
		taskidInput.type = 'hidden';
		taskidInput.name = Constants.PARAM_TASKID;
		taskidInput.value = taskid;
		hiddenForm.appendChild( taskidInput );
		
		var divObj = document.createElement( "DIV" );
		document.body.appendChild( divObj );
		divObj.style.display = "none";
		divObj.appendChild( hiddenForm );
		
		hiddenForm.submit( );		
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
	},
	
	/**
	 * Set redirect flag
	 * 
	 * @param, flag
	 * @return, void
	 */
	setRedirect : function( flag )
	{
		this.__redirect = flag;		
	}
} );