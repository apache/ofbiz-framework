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
 *	AbstractReportComponent.
 *		Base class for all UI components.
 */
AbstractReportComponent = function( ) { };

AbstractReportComponent.prototype =
{
	/**
	 *	UI component html instance.
	 */
	__instance : null,
	
	/**
	 *	Re-render ui object with new content.
	 *
	 *	@id, ui object id
	 *	@content, new html UI content
	 *	@return, void
	 */
	__cb_render : function( id, content )
	{
		var oDiv = $( id );
		
		while( oDiv.childNodes.length > 0)
		{
			oDiv.removeChild(oDiv.firstChild);
		}
				
		// workaround for IE. If content starts with script, 
		// append a hidden line to avoid ignore these scripts.
		// Delete script attribute "defer" to avoid exec javascript twice
		if( BrowserUtility.__isIE( ) )
		{
			content = '<span style="display: none">&nbsp;</span>' + content;
			content = content.replace(/<script(.*)defer([^\s|^>]*)([^>]*)>/gi,'<script$1$3>');
		}
				
		var container = document.createElement( "div" );
		
		container.innerHTML = content;
		
		// FIXME: Mozilla 1.7.12 produces an exception on the following line
		// because the "content" variable contains some unrecognized parts
		// see Bugzilla Bug 199998
		try
		{
			oDiv.appendChild( container );
		}
		catch ( error )
		{
			// ignore exception
		}		
		
		var scripts = container.getElementsByTagName( "script" );
		for( var i = 0; i < scripts.length; i++ )
		{
		    if( scripts[i].src )
		    {		
		    	// workaround for IE, need append these scripts in head    			    					
				if( BrowserUtility.__isIE( ) )
				{
				   	var scriptObj = document.createElement( "script" );
					scriptObj.setAttribute( "type", "text/javascript" );
					scriptObj.setAttribute( "src", scripts[i].src );
				
					var head = document.getElementsByTagName( "head" )[0];	
					if( head )
						head.appendChild( scriptObj );
				}	
		    }
		    else if ( scripts[i].innerHTML )
		    {	    	
			    //  Internet Explorer has a funky execScript method that makes this easy
			    if ( window.execScript )
			        window.execScript( scripts[i].innerHTML );
		    }
		}

		if ( BrowserUtility.__isSafari() || BrowserUtility.__isKHTML() )
		{
			// add the styles explicitly into the head
			var styles = container.getElementsByTagName("style");
			for ( var i = 0; i < styles.length; i++ )
			{
				var style = styles[i];
				var styleContent = style.innerHTML;
				if ( styleContent )
				{
					birtUtility.addStyleSheet( styleContent );
				}
			}
		}
		
		// workaround for bug 165750, overflow-x and overflow-y only used in IE
		if( BrowserUtility.__isIE( ) )
		{
			container.style.overflowX = "visible";
			container.style.overflowY = "visible";
		}
		
		this.__postRender(id);
	},
	
	/**
	 *	Called after the component content is rendered.	 
	 *
	 *	@id, ui object id
	 *	@return, void
	 */	
	__postRender : function( id )
	{
		//implementation is left to extending class
	},
		
	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, table object id
	 *	@return, void
	 */
	__cb_installEventHandlers : function( id, children, bookmark, type )
	{
		if ( this.__local_installEventHandlers )
		{
			this.__local_installEventHandlers( id, children, bookmark, type );
		}
		
		var container = $( id );

		container[ Constants.reportBase ] = ( type != 'Group' && type != 'ColumnInfo');
		container[ Constants.activeIds ] = [ ]; // Need to remember active children
		container[ Constants.activeIdTypes ] = [ ]; // Need to remember active children types
		
		if ( !children )
		{
			return;
		}

		// Also need to take care the active children.
		for( var i = 0; i < children.length; i++ )
		{
			var oElementIds = children[i].getElementsByTagName( 'Id' );
			var oElementTypes = children[i].getElementsByTagName( 'Type' );

			var birtObj = ReportComponentIdRegistry.getObjectForType( oElementTypes[0].firstChild.data );
			
			if ( !birtObj || !birtObj.__cb_installEventHandlers )
			{
				continue;
			}
			
			container[ Constants.activeIds ].push( oElementIds[0].firstChild.data );
			container[ Constants.activeIdTypes ].push( oElementTypes[0].firstChild.data );

			birtObj.__cb_installEventHandlers( oElementIds[0].firstChild.data, null, null, oElementTypes[0].firstChild.data );
		}
	},
	
	/**
	 *	Unregister any birt event handlers.
	 *	Remove local event listeners
	 *
	 *	@id, object id
	 *	@return, void
	 */
	__cb_disposeEventHandlers : function( id, type )
	{
		if ( this.__local_disposeEventHandlers )
		{
			this.__local_disposeEventHandlers( id, type );
		}

		var container = $( id );
		
		var id = null;
		while( container[ Constants.activeIds ].length > 0 )
		{
			var id = container[ Constants.activeIds ].shift( )
			var type = container[ Constants.activeIdTypes ].shift( );
			var birtObj = ReportComponentIdRegistry.getObjectForType( type );
			if ( !birtObj || !birtObj.__cb_disposeEventHandlers )
			{
				continue;
			}
			birtObj.__cb_disposeEventHandlers( id, type );
		}
	}
}