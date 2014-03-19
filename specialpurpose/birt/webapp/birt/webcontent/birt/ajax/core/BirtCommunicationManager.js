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
 *	BirtCommunicationManager
 *	...
 */
BirtCommunicationManager = Class.create( );

BirtCommunicationManager.prototype =
{
	__active : false,
	
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *
	 *	@return, void
	 */
	initialize: function( )
	{
	},

	/**
	 *	Make xml http request.
	 *
	 *	return, void
	 */
	connect: function( )
	{
		var xmlDoc = birtSoapRequest.__xml_document;
		
		if( xmlDoc )
		{
			debug( birtSoapRequest.prettyPrintXML(xmlDoc), true);
			if ( BrowserUtility.isSafari || BrowserUtility.isFirefox3 )
			{
				// WORKAROUND: sending the XML DOM doesn't replace the
				// ampersands properly but the XMLSerializer does.
				xmlDoc = (new XMLSerializer()).serializeToString(xmlDoc);
			}
		}		
		
		if ( !birtSoapRequest.getURL( ) ) return;

		//activate delay message manager;
		this.__active = true;
		birtProgressBar.__start( );
		
		//workaround for Bugzilla Bug 144598. Add request header "Connection" as "keep-alive"
		var myAjax = new Ajax.Request( birtSoapRequest.getURL( ), { method: 'post', postBody: xmlDoc,
			onSuccess: this.responseHandler, onFailure: this.invalidResponseHandler,
			requestHeaders: ['Content-Type', 'text/xml; charset=UTF-8', 'SOAPAction', '""', 'request-type', 'SOAP', 'Connection', 'keep-alive' ] } );

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
		if ( isDebugging( ) )
		{
			debug(request.responseText, true);
			debug(birtSoapRequest.prettyPrintXML(request.responseXML.documentElement), true);
		}
		
		if ( request.responseXML && request.responseXML.documentElement )
		{
			birtSoapResponse.process( request.responseXML.documentElement );
		}
		
		birtCommunicationManager.postProcess( );
		//todo handle responseText
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
		debug("invalid response");
		
		if ( request.responseXML && request.responseXML.documentElement )
		{
			birtSoapResponse.process( request.responseXML.documentElement );
		}
		
		birtCommunicationManager.postProcess( );
	},

	/**
	 *	Post process after finish processing the response.
	 *
	 *	@return, void
	 */
	postProcess: function( )
	{
	    //deactivate delay message manager
		birtProgressBar.__stop( );
	    this.__active = false;
	}
}

var birtCommunicationManager = new BirtCommunicationManager( );