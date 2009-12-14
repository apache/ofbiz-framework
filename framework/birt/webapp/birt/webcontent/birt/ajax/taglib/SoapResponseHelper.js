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
 *	Helper to handle soap response
 */
SoapResponseHelper = Class.create( );

SoapResponseHelper.prototype =
{
	__parameterGroup : null,
	
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *
	 *	@return, void
	 */
	initialize: function( )
	{
	},
	
	/**
	*  Set current parameter group object 
	*  
	*  @param, group cascading parameter group
	*  @return, void
	*/
	setParameterGroup : function( group )
	{
		this.__parameterGroup = group;
	},
	
	/**
	 *	Process the soap response, dispatcher message content to designated
	 *	message handlers.
	 *
	 *	@message, incoming soap message DOM
	 *	@return, void
	 */
	processCascadingParameter : function( message )
	{
		if( !message || !this.__parameterGroup ) return;
		
		//alert( birtSoapRequest.prettyPrintXML( message ) );
		var soapBody = message.getElementsByTagName( 'soapenv:Body' )[ 0 ];
		if ( !soapBody )
		{
			soapBody = message.getElementsByTagName( 'Body' )[ 0 ];
		}
		
		if ( soapBody )
		{
			for ( var i = 0; i < soapBody.childNodes.length; i++ )
			{
				if ( soapBody.childNodes[i].nodeType == 1 ) // Need to use NodeType definition.
				{
					if ( soapBody.childNodes[i].tagName == 'soapenv:Fault' )
					{
						// error message
						this.__handleErrorMessage( soapBody.childNodes[i] );
					}
					else
					{
						var datas = soapBody.childNodes[i].getElementsByTagName( 'Data' );
						if( !datas || datas.length<=0 ) return;
						
						var data = datas[0];//assume there is only one data
						var cascade_param = data.getElementsByTagName( 'CascadeParameter' )[0];//assume there is only one cascadeparameter
						var selectionLists = data.getElementsByTagName( 'SelectionList' );
						if ( !selectionLists )
						{
							return;
						}
						
						for ( var k = 0; k < selectionLists.length; k++ )
						{
							var paramName = selectionLists[k].getElementsByTagName( 'Name' )[0].firstChild.data;
							var selections = selectionLists[k].getElementsByTagName( 'Selections' );
							
							var paramId = this.__parameterGroup.getParameterIdByName( paramName );
							var append_selection = document.getElementById( paramId );
							var len = append_selection.options.length;
														
							// Clear our selection list.
							for( var i = 0, index = 0; i < len; i++ )
							{
								append_selection.remove( index );
							}
							
							// Add new options based on server response.
							for( var i = 0; i < selections.length; i++ )
							{
								if ( !selections[i].firstChild )
								{
									continue;
								}
				
								var oOption = document.createElement( "OPTION" );
								var oLabel = selections[i].getElementsByTagName( 'Label' )[0].firstChild;
								if( oLabel )
									oOption.text = oLabel.data;
								else
									oOption.text = "";
			
								var oValue = selections[i].getElementsByTagName( 'Value' )[0].firstChild;
								if( oValue )
									oOption.value = oValue.data;
								else
									oOption.value = "";
								append_selection.options[append_selection.options.length] = oOption;
							}
						}						
					}				
				}
			}
		}
		
		// reset parameter group
		this.__parameterGroup = null;
	},
	
	/**
	* handle error message
	*
	* @param, data incoming soap error message DOM
	* @return, void
	*/
	__handleErrorMessage : function( data )
	{
		if( !data ) return;

		// Prepare fault string (reason)
	 	var faultStrings = data.getElementsByTagName( 'faultstring' );
	 	if ( faultStrings[0] && faultStrings[0].firstChild )
	 	{
			alert( faultStrings[0].firstChild.data );
		}
		else
		{
			alert( Constants.error.unknownError );
		}	
	},
	
	noComma : "" //just to avoid javascript syntax errors
}

var soapResponseHelper = new SoapResponseHelper( );