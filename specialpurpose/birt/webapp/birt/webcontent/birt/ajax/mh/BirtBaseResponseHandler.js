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
 *	BirtBaseResponseHandler
 *	...
 */
BirtBaseResponseHandler = function(){}; 

BirtBaseResponseHandler.prototype =
{
	
	associations: {}, //map of response types, handlers
	
	/**
	* Add handler for response target types
	* Holds one handlerObject per targetType with newer handlerObject overwriting existing handlerObject
	* @param targetType String name of target, length 5 
	* @param handlerObject 
	*/
	addAssociation: function(targetType, handlerObject)
	{
		this.associations[targetType] = handlerObject;			
	},
	

	/**
	 *	Process update response message. Trigger necessary UI updates.
	 *	See response schema for details.
	 *
	 *	@message, update response DOM.
	 *	@return, void
	 */
	__process: function( message )
	{
		this.__process( message );
	}
	
	////////////////////////////////////////////////////////////////////////////
	//	Local methods
	///////////////////////////////////////////////////////////////////////////
	

}