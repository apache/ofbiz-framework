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
 *	BirtEventDispatcher
 *	...
 */
BirtEventDispatcher = Class.create( );

BirtEventDispatcher.prototype =
{
	__focusId : null,
	
	__event_map : { },
	
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *
	 *	@return, void
	 */
	initialize : function( )
	{
	},

	/**
	 *	Register birt event handler with global birt event dispatcher to
	 *	handle birt events (Not native events).
	 *
	 *	@eventType, pre-defined birt event type
	 *	@id, ui object id
	 *	@handler, birt event handler
	 *	@return, void
	 */
	registerEventHandler : function( eventType, id, handler )
	{
	    if ( !this.__event_map[eventType] )
	    {
			this.__event_map[eventType] = { };
	    }
	    
		this.__event_map[eventType][id] = handler;
	},
	
	/**
	 *	Register birt event handler with global birt event dispatcher to
	 *	handle birt events (Not native events).
	 *
	 *	@eventType, pre-defined birt event type
	 *	@id, ui object id
	 *	@handler, birt event handler
	 *	@return, void
	 */
	unregisterEventHandler : function( eventType, id )
	{	
		if( this.__event_map[eventType] && this.__event_map[eventType][id] )
		{   
			delete this.__event_map[eventType][id]; //deletes 'id' property
		}
	},
	
	/**
	Unregisters all handlers associated with a particular id
	@param id ui object id
	*/
	unregisterEventHandlerById : function( id )
	{	
		debug(birtEventDispatcher.toString());
		for( var eventType in this.__event_map)
		{
			if( this.__event_map[eventType][id])
			{
				delete this.__event_map[eventType][id]; //deletes 'id' property
			}
		}
		debug(birtEventDispatcher.toString());
	},
	
	/**
	 *	Register birt event handler with global birt event dispatcher to
	 *	handle birt events (Not native events).
	 *	If the event has already been registered under another handler,
	 *  replace that handler with new handler.
	 *
	 *	@eventType, pre-defined birt event type
	 *	@id, ui object id
	 *	@handler, birt event handler
	 *	@return, void
	 */
	replaceEventHandler : function( eventType, id, handler )
	{
	    if ( !this.__event_map[eventType] )
	    {
			this.__event_map[eventType] = { };
	    }
	    else
	    {
			for( var eventId in this.__event_map[eventType] )
			{
				delete this.__event_map[eventType][eventId]; //unregister previous event handler
			}
	    }
	    
		this.__event_map[eventType][id] = handler;
	},
	
	/**
	 *	Fire birt event. Objects that is or is the child of current focused object are waken.
	 *	Their registered event handlers are triggered.
	 *
	 *	@eventType, pr-defined birt events.
	 *	@object, event parameters.
	 *	@return, void
	 */
	fireEvent : function( eventType, object )
	{
		var handled = this.sendEvent( eventType, object, false );
		if ( !handled )
		{
			this.broadcastEvent( birtEvent.__E_WARN );
		}
	},
	
	/**
	 *	Fire birt event. Objects that is or is the child of current focused object are waken.
	 *	Their registered event handlers are triggered.
	 *
	 *	@eventType, pr-defined birt events.
	 *	@object, event parameters.
	 *	@return, void
	 */
	broadcastEvent : function( eventType, object )
	{
		this.sendEvent( eventType, object, true );
	},

	/**
	 *	Fire birt event. Objects that is or is the child of current focused object are waken.
	 *	Their registered event handlers are triggered.
	 *
	 *	@eventType, pr-defined birt events.
	 *	@object, event parameters.
	 *	@return, void
	 */
	sendEvent : function( eventType, object, isBroadcast )
	{
		var processed = false;
		var serverCall = false;
		var focus = this.__focusId ? this.__focusId : birtReportDocument.__instance.id;
		
		for ( var id in this.__event_map[ eventType ] )
		{
			if ( !isBroadcast && focus && focus != id )
			{
				// TODO: need more complex scheme.
				continue;
			}
			
			var handler = this.__event_map[ eventType ][ id ];
			if ( handler )
			{
				processed = true;
			}
			if ( handler( id, object ) )
			{
				serverCall = true;
			}
		}
		
		if( serverCall )
		{
			birtCommunicationManager.connect( );
		}
		
		return processed;
	},

	/**
	 *	@param id id of current DOM element in focus.
	 */
	setFocusId : function( id )
	{
		this.__focusId = id;
	},
	
	/**
	 *	@return id id of current DOM element in focus.
	 */
	getFocusId : function( )
	{
		return this.__focusId;
	},
	
	/**
	 *	Convenience method.
	 *
	 *	@param element current DOM element in focus, must have id
	 */
	setFocusElement : function( element )
	{
		this.__focusId = element.id || null;
	},
	
	/**
	 *	Convenience method.
	 *
	 *	@return element current DOM element in focus
	 */
	getFocusElement : function( )
	{
		return $( this.__focusId );
	},
	
	
	toString: function( )
	{
		var str = "";
		str += "----start-------BirtEventDispatcher.toString------------";
		str += "\n";
		str += ("    focusId = " + this.__focusId);		
		for(var i in this.__event_map)
		{
			str += "\n";
			str += ("    eventType = " + i);
			for(var k in this.__event_map[i])
			{
				str += "\n";
				str += ("        id = " + k);
			}
		}
		str += "\n";
		str += "------end-------BirtEventDispatcher.toString------------";
		return str;
	}
}

var birtEventDispatcher = new BirtEventDispatcher( );