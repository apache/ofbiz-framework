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
 *	BirtToolbar
 *	...
 */
BirtToolbar = Class.create( );

BirtToolbar.prototype = Object.extend( new AbstractBaseToolbar( ),
{
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *	@return, void
	 */
	initialize : function( id )
	{
		this.__initBase( id );
		this.__neh_click_closure = this.__neh_click.bindAsEventListener( this );
		this.__cb_installEventHandlers( );
	},

	/**
	 *	Binding data to the toolbar UI. Data includes zoom scaling factor.
	 *
	 *	@data, data DOM tree (schema TBD)
	 *	@return, void
	 */
	__cb_bind : function( data )
	{
	},

	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, toolbar id (optional since there is only one toolbar)
	 *	@return, void
	 */
	__cb_installEventHandlers : function( )
	{
		var oImgs = this.__instance.getElementsByTagName( 'INPUT' );
		if ( oImgs )
		{
			for ( var i = 0; i < oImgs.length; i++ )
			{
				if ( oImgs[i].type == 'image' )
				{
					Event.observe( oImgs[i], 'click', this.__neh_click_closure, false );
				}
			}
		}
	},
	
	/**
	 *	Handle native event 'click'.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_click : function( event )
	{
		var oBtn = Event.element( event );
		
		if ( oBtn )
		{
			switch ( oBtn.name )
			{
				case 'print':
				{
					birtEventDispatcher.broadcastEvent( birtEvent.__E_PRINT );
					break;
				}				
				case 'printServer':
				{
					birtEventDispatcher.broadcastEvent( birtEvent.__E_PRINT_SERVER );
					break;
				}
				case 'exportReport':
				{
					birtEventDispatcher.broadcastEvent( birtEvent.__E_EXPORT_REPORT );
					break;
				}
				case 'export':
				{
					birtEventDispatcher.fireEvent( birtEvent.__E_QUERY_EXPORT );
					break;
				}
				case 'toc':
				{
					birtEventDispatcher.broadcastEvent( birtEvent.__E_TOC );
					break;
				}
				case 'parameter':
				{
					birtEventDispatcher.broadcastEvent( birtEvent.__E_PARAMETER );
					break;
				}
				default:
				{
					alert( oBtn.name );
					break;
				}	
			}
		}
	}
}
);