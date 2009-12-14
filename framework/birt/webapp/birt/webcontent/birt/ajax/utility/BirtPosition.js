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
 *	BirtPosition.
 *	We could extend Position from Ruby.
 */
var BirtPosition =
{
	viewportHeight: function( )
	{
		if (window.innerHeight!=window.undefined) return window.innerHeight;
		if (document.compatMode=='CSS1Compat') return document.documentElement.clientHeight;
		if (document.body) return document.body.clientHeight; 
		return window.undefined; 
	},
	
	viewportWidth: function( )
	{
		if (window.innerWidth!=window.undefined) return window.innerWidth; 
		if (document.compatMode=='CSS1Compat') return document.documentElement.clientWidth; 
		if (document.body) return document.body.clientWidth; 
		return window.undefined; 
	},
	
	center: function( )
	{
      	var element = $( arguments[0] );
      	if ( !element ) return;
      	
		var scTop = parseInt( document.documentElement.scrollTop, 10 );
		var scLeft = parseInt( document.documentElement.scrollLeft, 10 );

		element.style.top = ( scTop + ( ( this.viewportHeight( ) - element.offsetHeight ) / 2 ) ) + "px";
		element.style.left =  ( scLeft + ( ( this.viewportWidth( ) - element.offsetWidth ) / 2 ) ) + "px";
    },

	move: function( element, x, y )
	{
      	if ( !element ) return;
      	
		var scTop = parseInt( document.documentElement.scrollTop, 10 );
		var scLeft = parseInt( document.documentElement.scrollLeft, 10 );

		element.style.top = scTop + y + "px";
		element.style.left = scLeft + x + "px";
    }
}