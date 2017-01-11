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
 *	AbstractBaseToolbar.
 *	...
 */
AbstractBaseToolbar = function( ) { };

AbstractBaseToolbar.prototype = Object.extend( new AbstractUIComponent( ),
{
	/**
	 *	Closure for native click event.
	 */
	__neh_click_closure : null,
	
	/**
	 *	instance variable for preloading images
	 */
	toolbarImagesOff : null,

	/**
	 *	instance variable for preloading images
	 */
	toolbarImagesOn : null,
	
	/**
	 *
	 */
	toolbarImageState : null,
	
	/**
	 *	Loads image in off state and stores on and off src strings 
	 *	@param source
	 */
	preloadImages : function( source )
	{
		var image;
		var strFront;
		var strExtension;

		image = new Image( );
		strFront = source.substring( 0, source.lastIndexOf( '.' ) );
		strExtension = source.substring( source.lastIndexOf( '.' ), source.length );
		image.src = strFront + "Off" + strExtension;
		this.toolbarImagesOff.push( image.src );
		this.toolbarImagesOn.push( source );
	},
	
	/** 
	 *	Determines if an image is in disabled state by examining name for "Off."
	 *	@param name
	 */
	isEnabled : function( name )
	{
		var i = name.indexOf( 'Off.' );
		if( i > -1 )
		{
			return false;
		}
		return true;
	},
	
	/**
	 *	Changes state from enabled to disabled
	 *	@param button IMG
	 *	@param enable true to enable, false to disable
	 */
	toggleButton : function( button, enable )
	{
		if( enable )
		{
			this.toolbarImageState[button.name] = true;
			button.parentNode.className = "toolbarButton";
			Event.observe( button, 'click', this.__neh_click_closure, false );
		}
		else
		{
			this.toolbarImageState[button.name] = false;
			button.parentNode.className = "toolbarButtonOff";
			Event.stopObserving( button, 'click', this.__neh_click_closure, false );
		}
	}
}
);