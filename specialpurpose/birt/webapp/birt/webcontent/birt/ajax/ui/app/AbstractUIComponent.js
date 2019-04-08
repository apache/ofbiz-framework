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
 *	AbstractUIComponentBase.
 *		Base class for all UI components.
 */
AbstractUIComponent = function( ) { };

AbstractUIComponent.prototype =
{
	/**
	 *	UI component html instance.
	 */
	__instance : null,
	
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *
	 *	@return, void
	 */
	__initBase : function( id )
	{
		this.__instance = $( id );
	}
}