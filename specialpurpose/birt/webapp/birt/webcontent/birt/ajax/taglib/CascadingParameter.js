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
 * Utility functions for BIRT JSP Cascading parameter tag.
 */

CascadingParameter = Class.create( );

CascadingParameter.prototype =
{
	__active : false,
	__requesterId : '',
	__parameter : null,
	__paramNames : null,
	__group : null,
	__data : null,
	__targetId : '',
	__action : null,
		 
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *	@return, void
	 */
	initialize : function( id, parameter, names, group )
	{
		this.__requesterId = id;
		this.__parameter = parameter;
		this.__paramNames = names;
		this.__group = group;
		this.__data = new Array( );
		
		this.__initialize( );
	},
		
	/**
	* Initialize cascading parameter
	*
	* @return, void
	*/
	__initialize : function( )
	{		
		var oDiv = $( this.__requesterId );
		if( !oDiv ) return;
		
		var forms = oDiv.getElementsByTagName( "FORM" );
		if( !forms || forms.length <= 0 ) return;
		this.__action = forms[0].action;	
	},
	
	/**
	*  Insert a option into select
	*  
	*  @param, list
	*  @param, index
	*  @param, text
	*  @param, value
	*  @return, void
	*/
	__insertOption : function( list, index, text, value )
	{
		var i = 0; 
		for ( i = list.options.length; i > index; i-- ) 
		{ 
			list.options[i] = new Option( list.options[i-1].text, list.options[i-1].value ); 
		} 

		list.options[index] = new Option( text, value );			
	},
	
	/**
	 * return action string
	 * 
	 * @return, action
	 */
	getAction : function( )
	{
		return this.__action;
	},
	
	/**
	 *	process to get cascading parameter
	 *  @return, void
	 */
	process : function( )
	{			
		if( !this.__group || !this.__paramNames ) return;				
				
		// If target select doesn't exist, return
		var targetName = this.__paramNames[ this.__paramNames.length -1 ];
		this.__targetId = this.__group.getParameterIdByName( targetName );		
		var target = $( this.__targetId );
		if( !target ) return;
		
		for( var i=0; i<this.__paramNames.length - 1; i++ )
		{
			this.__data[i] = {};			
			
			var id = this.__group.getParameterIdByName( this.__paramNames[i] );
			var radioSelectId = id + "_radio_select";
			if( !$( radioSelectId ) || $( radioSelectId ).checked )
			{
				var label = $( id ).text;
				if( label == Constants.nullValue )
				{
					this.__data[i].name = Constants.PARAM_ISNULL;
					this.__data[i].value = this.__paramNames[i];	
				}
				else
				{
					this.__data[i].name = this.__paramNames[i];
					this.__data[i].value = $( id ).value;
				}				
			}
			else
			{
				var inputTextId = id + "_input";
				if( $( inputTextId ) )
				{
					this.__data[i].name = this.__paramNames[i];
					this.__data[i].value = $( inputTextId ).value;
				}				
			} 			
		}
		
		// Set task id
		var taskid = birtUtility.setTaskId( );
		birtSoapRequest.addOperation( Constants.documentId, Constants.Document,'GetCascadingParameter',null,
									this.__data,{ name : Constants.PARAM_TASKID, value : taskid } );
		birtSoapRequest.setURL( this.__action );
			
		if ( !birtSoapRequest.getURL( ) ) return;
				
		this.__active = true;
		progressBar.__start( );
		
		// Set cascading parameter group
		soapResponseHelper.setParameterGroup( this.__group );
		
		//workaround for Bugzilla Bug 144598. Add request header "Connection" as "keep-alive"
		var myAjax = new Ajax.Request( birtSoapRequest.getURL( ), { method: 'post', postBody: birtSoapRequest.__xml_document,
			onSuccess: this.responseHandler, onFailure: this.invalidResponseHandler,
			requestHeaders: ['Content-type', 'text/xml; charset=utf-8', 'SOAPAction', '""', 'request-type', 'SOAP', 'Connection', 'keep-alive' ] } );

		birtSoapRequest.reset( );		
	},
	
	/**
	 *	Callback function triggered when reponse is ready, status is 200.
	 *
	 *	@request, httpXmlRequest instance
	 *	@return, void
	 */
	responseHandler: function( request )
	{		
		if ( request.responseXML && request.responseXML.documentElement )
		{
			soapResponseHelper.processCascadingParameter( request.responseXML.documentElement );
		}
		
		progressBar.__stop( );
	    this.__active = false;
	},
	
	/**
	 *	Callback function triggered when reponse is ready status is not 200.
	 *	Process any http (non-200) errors. Note this is not exception from
	 *	server side.
	 *
	 *	@request, httpXmlRequest instance
	 *	@return, void
	 */
	invalidResponseHandler: function( request )
	{			
		if ( request.responseXML && request.responseXML.documentElement )
		{
			soapResponseHelper.processCascadingParameter( request.responseXML.documentElement );
		}		
			
		progressBar.__stop( );
	    this.__active = false;
	},
			
	noComma : "" //just to avoid javascript syntax errors
}