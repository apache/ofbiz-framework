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
 *	BirtTabedDialogBase
 *	...
 */
BirtTabedDialogBase = Class.create( );

BirtTabedDialogBase.prototype = Object.extend( new AbstractBaseDialog( ),
{
	/**
	 *	Current tabs
	 */
	__tabs_caption : { },
	
	/**
	 *	Current sections
	 */
	__tabs_content : { },
	
	/**
	 *	Current tabs
	 */
	__current_tab : null,
	
	__neh_switchTab_closure : null,
	__neh_hoverTab_closure : null,
	
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *
	 *	@return, void
	 */
	initialize : function( id )
	{
		this.__initBase(id);
		this.__z_index = 300;
		
		// TODO: need a better way to identify the dialog layout.
		var oTabs = $( 'tabs' );
		var oDivs = $( 'aaacontent' );
		if ( oTabs && oDivs )
		{
			var oCells = oTabs.getElementsByTagName( 'td' );
			var oTables = oDivs.getElementsByTagName( 'div' );
			
			for ( var i = 0; i < oCells.length; i++ )
			{
				this.__tabs_caption[ oCells[i].firstChild.data ] = oCells[i];
				this.__tabs_content[ oCells[i].firstChild.data ] = oTables[i];
			}
			
			this.__current_tab = oCells[0].firstChild.data;
		}

		// Closures
		this.__neh_switchTab_closure = this.__neh_switchTab.bindAsEventListener( this );
		this.__neh_hoverTab_closure = this.__neh_hoverTab.bindAsEventListener( this );
		
		this.__initialize( id );
	},

	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, toolbar id (optional since there is only one toolbar)
	 *	@return, void
	 */
	__tabBase_installEventHandlers : function( id )
	{
		// Tabs
		if ( this.__tabs_caption )
		{
			for ( var i in this.__tabs_caption )
			{
				Event.observe( this.__tabs_caption[ i ], 'click', this.__neh_switchTab_closure, false );
				Event.observe( this.__tabs_caption[ i ], 'mouseover', this.__neh_hoverTab_closure, false );
				Event.observe( this.__tabs_caption[ i ], 'mouseout', this.__neh_hoverTab_closure, false );
			}
		}
		
		this.__installEventHandlers( id );
	},
	
	/**
	 *	Handle native event 'click'.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_hoverTab : function( event )
	{
		var oCell = Event.element( event );
		if ( this.__current_tab != oCell.firstChild.data )
		{
			this.__tabs_caption[oCell.firstChild.data].style.backgroundColor = ( event.type == 'mouseover' )? "#4682b4" : "#dbe4ee";
			this.__tabs_caption[oCell.firstChild.data].style.color = ( event.type == 'mouseover' )? "white" : "black";
		}
	},

	/**
	 *	Handle native event 'click'.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_switchTab : function( event )
	{
		var oCell = Event.element( event );
		if ( this.__current_tab != oCell.firstChild.data )
		{
			this.__tabs_caption[this.__current_tab].style.borderStyle = "outset";
			this.__tabs_caption[this.__current_tab].style.cursor = "hand";
			
			this.__tabs_caption[oCell.firstChild.data].style.borderStyle = "inset";
			this.__tabs_caption[oCell.firstChild.data].style.cursor = "default";
			this.__tabs_caption[oCell.firstChild.data].style.backgroundColor = "#dbe4ee";
			this.__tabs_caption[oCell.firstChild.data].style.color = "black";
			
			this.__tabs_content[this.__current_tab].style.display = "none";
			this.__tabs_content[oCell.firstChild.data].style.display = "block";
	
			this.__current_tab = oCell.firstChild.data;
		}
	}
});