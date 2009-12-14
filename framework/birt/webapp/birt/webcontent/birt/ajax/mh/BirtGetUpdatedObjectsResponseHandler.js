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
 *	birtGetUpdatedObjectsResponseHandler
 *	...
 */
BirtGetUpdatedObjectsResponseHandler = function( ){ };

BirtGetUpdatedObjectsResponseHandler.prototype = Object.extend( new BirtBaseResponseHandler( ),
{
	/**
	 *	Process update response message. Trigger necessary UI updates.
	 *	See response schema for details.
	 *
	 *	@message, update response DOM.
	 *	@return, void
	 */
	__process: function( message )
	{
		if ( !message ) return;
		this.__processUpdateContent( message.getElementsByTagName( 'UpdateContent' ) );
		this.__processUpdateData( message.getElementsByTagName( 'UpdateData' ) );
	},
	
	/**
	 *	Process update response message. Trigger necessary UI updates.
	 *	See response schema for details.
	 *
	 *	@message, update response DOM.
	 *	@return, void
	 */
	__processUpdateContent: function( updates )
	{
		if ( !updates ) return;
		
		for ( var i = 0; i < updates.length; i++ )
		{
			try
			{
				var target = updates[i].getElementsByTagName( 'Target' )[0]; //assumes only 1 target per UpdateContent
				var targetData = target.firstChild.data				
				var targetType = targetData.substring( 0, 5 );
				var handler = this.associations[targetType];
				
				if ( !handler ) 
				{
					var error = { name : "CustomError",
								  message : ( "__processUpdateContent no handler registered for type: " + targetType ) };
					throw error;
				}
								
				var content = updates[i].getElementsByTagName( 'Content' )[0]; //assumes only 1 context per UpdateContent	
				if ( !content ) 
				{
					var error = { name : "CustomError",
								  message : ( " __processUpdateContent empty contents" ) };
					throw error;	
				}
				
				/**************************************************************
				 * Data is too large, seems firefox will wrap the content into
				 * several children nodes of text type. Need more verification.
				 **************************************************************/
				
				// firefox can use textContent to retrieve the complete node content,
				// check this property first to avoid string concatation which is very
				// slow for large node.  
				var contentData = content.textContent;
				
				if (!contentData)
				{
					// for non-fireforx browser, we still use string concatation 
					// to retrieve the complete content.
					
					contentData = "";
					
					if ( content.hasChildNodes )
					{
						for( var j = 0; j < content.childNodes.length; j++ )
						{
							if( content.childNodes[j].nodeType == 3 ) //Text type
							{
								contentData += content.childNodes[j].data;
							}
						}		
					}
				}
				
				if ( contentData )
				{
					handler.__cb_disposeEventHandlers( targetData );
					handler.__cb_render( targetData, contentData );
					var inits = updates[i].getElementsByTagName( 'InitializationId' );
					var bookmarks = updates[i].getElementsByTagName( 'Bookmark' );
					if ( bookmarks.length > 0 )
					{
						handler.__cb_installEventHandlers( targetData, inits, bookmarks[0].firstChild.data );
					}
					else
					{
						handler.__cb_installEventHandlers( targetData, inits );
					}
				}
			}
			catch( error )
			{
				debug( "ERROR in birtGetUpdatedObjectsResponseHandler" );
				
				for( var i in error )
				{
					debug( "ERROR " + i + " : " + error[i] );
				}
			}			
		}
	},

	/**
	 *	Process update response message. Trigger necessary UI updates.
	 *	See response schema for details.
	 *
	 *	@message, update response DOM.
	 *	@return, void
	 */
	__processUpdateData: function( updates )
	{
		if ( !updates ) return;
		
		for ( var i = 0; i < updates.length; i++ )
		{
			var targets = updates[i].getElementsByTagName( 'Target' );
			if ( !targets || targets.length <= 0 ) continue;
			
			var datas = updates[i].getElementsByTagName( 'Data' );
			if ( !datas || datas.length <= 0 ) continue;

			var handler = null;
			try
			{
				handler = eval( targets[0].firstChild.data );
			}
			catch ( e )
			{
			}
			if ( !handler || !handler.__cb_bind ) continue;
			
			handler.__cb_bind( datas[0] );
		}
	},
	
	/**
	 *	Helper function to handle "Init" type
	 *	@param init "Init" element
	 */
	initList: function( init )
	{
		var initData = init.firstChild.data;
		var targetType = init.firstChild.data.substring( 0, 5 );
		var handler = this.associations[targetType];	
		if ( !handler ) 
		{
			var error = {name:"CustomError", message: ("initList invalid Init")};
			throw error;					
		}
		handler.__cb_installEventHandlers( initData );
	}
} );

var birtGetUpdatedObjectsResponseHandler = new BirtGetUpdatedObjectsResponseHandler( );