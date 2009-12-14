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
 *	BirtToc
 *	...
 */
BirtToc = Class.create( );

BirtToc.prototype = Object.extend( new AbstractBaseToc( ),
{
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *	@return, void
	 */
	initialize : function( id )
	{
		this.__initBase( id );
		
		this.__neh_click_closure = this.__neh_click.bindAsEventListener( this );
		this.__beh_toc_closure =  this.__beh_toc.bindAsEventListener( this );
		
		this.__beh_toc_image_closure = this.__beh_toc_image.bindAsEventListener( this );
		this.__neh_img_click_closure = this.__neh_img_click.bindAsEventListener( this );
		this.__neh_resize_closure = this.__neh_resize.bindAsEventListener( this );
		
		this.__neh_item_mouse_over = this.__neh_mousemove.bindAsEventListener( this );
		this.__neh_item_mouse_out  = this.__neh_mouseout.bindAsEventListener( this );
		this.__neh_item_click = this.__neh_text_click.bindAsEventListener( this );
		
		this.__cb_installEventHandlers( );
	}
} );