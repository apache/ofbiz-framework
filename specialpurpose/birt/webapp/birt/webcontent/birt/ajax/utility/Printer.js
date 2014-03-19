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
 * Printer class.
 */
Printer = Class.create( );

Printer.prototype =
{
	__name : null,
	__status : null,
	__model : null,
	__info : null,
	
	__copiesSupported : null,
	__copies : null,
	
	__collateSupported : null,
	__collate : null,
	
	__modeSupported : null,
	__mode : null,
	
	__duplexSupported : null,
	__duplex : null,
	
	__mediaSupported : null,
	__mediaSize : null,
	__mediaSizeNames : null,
	
	/**
	 * Constants
	 */
	MODE_MONOCHROME : "0",
	MODE_COLOR : "1",
	
	DUPLEX_SIMPLEX : "0",
	DUPLEX_HORIZONTAL : "1",
	DUPLEX_VERTICAL : "2",
	
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *
	 *	@return, void
	 */
	initialize: function( )
	{
		this.__copiesSupported = false;		
		this.__collateSupported = false;
		this.__collate = false;
		this.__modeSupported = false;
		this.__duplexSupported = false;
		this.__mediaSupported = false;
		
		this.__mediaSizeNames = new Array( );
	},
		
	setName : function( name )
	{
		this.__name = name;
	},
	
	getName : function( )
	{
		return this.__name;
	},
	
	setStatus : function( status )
	{
		this.__status = status;
	},
	
	getStatus : function( )
	{
		return this.__status;
	},

	setModel : function( model )
	{
		this.__model = model;
	},
	
	getModel : function( )
	{
		return this.__model;
	},

	setInfo : function( info )
	{
		this.__info = info;
	},
	
	getInfo : function( )
	{
		return this.__info;
	},
	
	setCopiesSupported : function( copiesSupported )
	{
		this.__copiesSupported = copiesSupported;
	},
	
	isCopiesSupported : function( )
	{
		return this.__copiesSupported;
	},
	
	setCopies : function( copies )
	{
		this.__copies = copies;
	},
	
	getCopies : function( )
	{
		return this.__copies;
	},
	
	setCollateSupported : function( collateSupported )
	{
		this.__collateSupported = collateSupported;
	},
	
	isCollateSupported : function( )
	{
		return this.__collateSupported;
	},
	
	setCollate : function( collate )
	{
		this.__collate = collate;
	},
	
	isCollate : function( )
	{
		return this.__collate;
	},
	
	setModeSupported : function( modeSupported )
	{
		this.__modeSupported = modeSupported;
	},
	
	isModeSupported : function( )
	{
		return this.__modeSupported;
	},
	
	setMode : function( mode )
	{
		this.__mode = mode;
	},
	
	getMode : function( )
	{
		return this.__mode;
	},

	setDuplexSupported : function( duplexSupported )
	{
		this.__duplexSupported = duplexSupported;
	},
	
	isDuplexSupported : function( )
	{
		return this.__duplexSupported;
	},
	
	setDuplex : function( duplex )
	{
		this.__duplex = duplex;
	},
	
	getDuplex : function( )
	{
		return this.__duplex;
	},
	
	setMediaSupported : function( mediaSupported )
	{
		this.__mediaSupported = mediaSupported;
	},
	
	isMediaSupported : function( )
	{
		return this.__mediaSupported;
	},
	
	setMediaSize : function( mediaSize )
	{
		this.__mediaSize = mediaSize;
	},
	
	getMediaSize : function( )
	{
		return this.__mediaSize;
	},
	
	addMediaSizeName : function( mediaSize )
	{
		var index = this.__mediaSizeNames.length;
		if( !this.__mediaSizeNames[index] )		
			this.__mediaSizeNames[index] = { };
					
		this.__mediaSizeNames[index].name = mediaSize;
		this.__mediaSizeNames[index].value = mediaSize;
	},
	
	getMediaSizeNames : function( )
	{
		return this.__mediaSizeNames;
	}
}

var printers = new Array( );