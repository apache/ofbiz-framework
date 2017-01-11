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
 * Parameter definition.
 */

ParameterDefinition = Class.create( );

ParameterDefinition.prototype =
{
	__id : null,
	__name : null,
	__isRequired : null,
	__value : null,
	__displayText : null,
		
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *	@return, void
	 */
	initialize : function( id, name )
	{
		this.__id = id;
		this.__name = name;
	},

	/**
	 *	get parameter id
	 *  
	 *	@return, id
	 */
	getId : function( )
	{
		return this.__id;
	},

	/**
	 *	get parameter name
	 *  
	 *	@return, name
	 */
	getName : function( )
	{
		return this.__name;
	},

	/**
	 *	get parameter value
	 *  
	 *	@return, value
	 */
	getValue : function( )
	{
		return this.__value;
	},

	/**
	 *	get parameter display text
	 *  
	 *	@return, displayText
	 */
	getDisplayText : function( )
	{
		return this.__displayText;
	},
		
	/**
	 *	set value
	 *  
	 *  @param, value
	 *	@return, void
	 */
	setValue : function( value )
	{
		this.__value = value;
	},

	/**
	 *	set display Text
	 *
	 *  @param, displayText
	 *	@return, void
	 */
	setValue : function( displayText )
	{
		this.__displayText = displayText;
	},

	/**
	 *	set isRequired
	 *
	 *  @param, isRequired
	 *	@return, void
	 */
	setRequired : function( isRequired )
	{
		this.__isRequired = isRequired;
	},
					
	/**
	 *	get isRequired
	 *
	 *	@return, isRequired
	 */
	isRequired : function( )
	{
		return this.__isRequired;
	},
						
	noComma : "" //just to avoid javascript syntax errors
}