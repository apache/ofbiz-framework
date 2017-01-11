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
 *	BirtSoapRequest
 *	...
 */
BirtSoapRequest = Class.create( );

BirtSoapRequest.prototype =
{
	SOAP_NAMESPACE_URI : "http://schemas.xmlsoap.org/soap/envelope/",
	BIRT_NAMESPACE_URI : "http://schemas.eclipse.org/birt",

	__url : null,
	__message : null,
	__xml_document : null,
	__operation_content : null,

	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *
	 *	@return, void
	 */
	initialize : function( )
	{
		/* 
		   XML namespaces are supported by the following browsers :
		   - Firefox 2 and 3
		   - Safari 3
		   - Mozilla 1.7.13
		   Unsupported by:
		   - IE6 nor IE7
		*/ 
		this.hasXmlNamespaceSupport = ( typeof( document.createElementNS ) != "undefined" );
		
		this.reset();
	},
	
	/**
	 *	Init the request message. There's only one request message DOM instance at any time,
	 *	when client running.
	 *
	 *	@message, request message DOM
	 *	@return, void
	 */
	setMessage : function( message )
	{
		// TODO: need to use dom contrcture the request.
		this.__xml_document = message;
	},

	/**
	 *	Retrieve the request message.
	 *
	 *	@return, request message in DOM
	 */
	getMessage : function( message )
	{
		return this.__message;
	},

	/**
	 *	Retrieve the request message.
	 *
	 *	@return, request message in DOM
	 */
	setURL : function( url )
	{
		this.__url = url;
	},
	
	/**
	 *	Retrieve the request message.
	 *
	 *	@return, request message in DOM
	 */
	getURL : function( )
	{
		return this.__url;
	},

	/**
	 * Clears the message body
	 */
	reset : function( )
	{
		// Soap envelope
		this.__xml_document = this.createXMLDom( this.SOAP_NAMESPACE_URI, "soap:Envelope" );
		
 		var soapEnv = this.__xml_document.documentElement;

		// Soap body
		var soapBody;
		
		if ( this.hasXmlNamespaceSupport )
		{
		 	soapBody = this.__xml_document.createElementNS( this.SOAP_NAMESPACE_URI, "Body" );
		}
		else
		{
		 	soapBody = this.__xml_document.createElement( "soap:Body" );
		}
		 	
  		soapEnv.appendChild( soapBody );
  		
		// Get updated objects method
		var getUpdatedObjects = this.createBirtElement( "GetUpdatedObjects" );
		if ( !this.hasXmlNamespaceSupport )
		{
			// only the first element needs the xmlns attribute
			getUpdatedObjects.setAttribute( "xmlns", this.BIRT_NAMESPACE_URI );
		}
		
  		soapBody.appendChild( getUpdatedObjects );
		
		this.__operation_content = getUpdatedObjects;
		
		var str = this.prettyPrintXML( this.__xml_document.documentElement, false );
		debug( "RESET XML : new message body is\n" + str );
	},

	/**
	 * Utility method to create an XML element in the birt namespace.
	 *
	 * @param qualifiedName qualified name
	 */
	createBirtElement : function( qualifiedName )
	{
		if ( this.hasXmlNamespaceSupport )
		{
			return this.__xml_document.createElementNS( this.BIRT_NAMESPACE_URI, qualifiedName );
		}
		else
		{
			return this.__xml_document.createElement( qualifiedName );
		}			
	},

	/**
	 * Utility method creates an XML document.
	 *
	 * @param rootName the name of documentElement
	 * @return xmlDocument a new XML document
	 */
	createXMLDom : function( namespaceUri, qualifiedName )
	{
		if ( document.implementation && document.implementation.createDocument )
		{
			// DOM Level 2 Browsers
			var dom = document.implementation.createDocument( namespaceUri, qualifiedName, null );
			if ( !dom.documentElement )
			{
				dom.appendChild( dom.createElement( qualifiedName ) );
			}
			
			return dom;
		}
		else if ( window.ActiveXObject )
		{
			// Internet Explorer
			var createdDocument = new ActiveXObject( "Microsoft.XMLDOM" );
			var documentElement = createdDocument.createElement( qualifiedName );
			createdDocument.appendChild( documentElement );

			// check for a namespace prefix
			var namespacePrefix = null;
			var nameParts = qualifiedName.split(":");
			if ( nameParts.length > 0 )
			{
				namespacePrefix = nameParts[0];
				qualifiedName = nameParts[1];
			}
			
			var nsAttribute = "xmlns";			
			if ( namespacePrefix )
			{
				nsAttribute += ":" + namespacePrefix;
			}
			
			documentElement.setAttribute( nsAttribute, namespaceUri );
			return createdDocument;
		}
		else
		{
			throw "Unable to create new Document.";
		}
	},
	
	/**
	 * Adds Operation type to message.
	 *
	 * @param id id of UIComponent
	 * @param operator name of desired operation (from Constants.js)
	 * @param data xmlElement type 'Data' or null if not applicable
	 * @param (optional) argument[1] - argument[n] object  {name: "paramName1", value: "paramValue1"}
	 */
	addOperation : function( id, type, operator, data )
	{
		var optionalArgs = 4; //number of params
	 	var operation = this.createBirtElement( "Operation" );
	 	
  		// Target
  		var target = this.createBirtElement( "Target" );
  		operation.appendChild( target );
  		
  		// Target id
  		var reportId = this.createBirtElement( "Id" );
		target.appendChild( reportId );
  		var reportIdText = this.__xml_document.createTextNode( id || "Error-id" );
		reportId.appendChild( reportIdText );
		
  		// Target type
  		var reportType = this.createBirtElement( "Type" );
		target.appendChild( reportType );
  		var reportTypeText = this.__xml_document.createTextNode( type || "Error-id" );
		reportType.appendChild( reportTypeText );
		
		// Operator
		var operatorEl = this.createBirtElement( "Operator" );
		var targetText = this.__xml_document.createTextNode( operator || "Error-operator" );
		operatorEl.appendChild( targetText );
		operation.appendChild( operatorEl );
		
		// Oprands
		if( arguments.length > optionalArgs ) //there are optional parameters
		{	
	  		var operand, name, value;
	  		for( var i = optionalArgs; i < arguments.length; i++ )
	  		{
				if( arguments[i].length && arguments[i].length > 0 )
				{
					for( var j = 0; j < arguments[i].length; j++ )
					{
						if ( !arguments[i][j].name )
						{
							continue;
						}
						
						operand = this.createBirtElement( "Oprand" );
						name = this.createBirtElement( "Name" );
						nameText = this.__xml_document.createTextNode( arguments[i][j].name );
						name.appendChild( nameText );
						operand.appendChild( name );
						value = this.createBirtElement( "Value" );
						valueText = this.__xml_document.createTextNode( arguments[i][j].value );
						value.appendChild( valueText );
						operand.appendChild( value );
						operation.appendChild( operand );
					}
				}
				else
				{
					if ( arguments[i].name )
					{
						operand = this.createBirtElement( "Oprand" );
						name = this.createBirtElement( "Name" );
						nameText = this.__xml_document.createTextNode( arguments[i].name );
						name.appendChild( nameText );
						operand.appendChild( name );
						value = this.createBirtElement( "Value" );
						valueText = this.__xml_document.createTextNode( arguments[i].value );
						value.appendChild( valueText );
						operand.appendChild( value );
						operation.appendChild( operand );
					}
				}
			}
	  	}
	  	
	  	// Data
	  	if( data )
	  	{
	  		operation.appendChild( data.documentElement );
	  	}
	  	
  		this.__operation_content.appendChild( operation );
  		
		var str = this.prettyPrintXML( this.__xml_document.documentElement, false );
		debug( "ADDED OPERATION : new message body is\n" + str );
	},
		
	/**
	 * Creates string of xml, optionally prints to debug window.
	 *
	 * @param xmlRoot documentElement of xml Element
	 * @param str leave empty -- used in recursive call
	 * @param offset leave empty -- used in recursive call
	 * @return String with indented tree of elements and text contents
	 */
	prettyPrintXML : function( xmlRoot, isDebug, str, offset )
	{	
		if( !str )
		{
			str = "";
		}
		
		var myIndent = 6;
		if( offset == null )
		{
			myIndent = 2;
		}
		else
		{
			myIndent += offset;
		} 
		var spacer = "";
		for( var i = 0; i < myIndent;i++ )
		{
			spacer += " ";
		}		
		var topLevel = xmlRoot.childNodes;
		var size = topLevel.length;
		var temp;
		for( var i = 0; i < size; i++ )
		{	
			var curNode = topLevel[i];
			
			//either node or a leaf
			if( curNode.nodeType == "1" )
			{	
				str += 	spacer + "&lt" + curNode.nodeName + "&gt\n";			
				if( isDebug )
				{
					debug( spacer + "&lt" + curNode.nodeName + "&gt" );
				}
				str = this.prettyPrintXML( curNode, isDebug, str, myIndent );
				str += spacer + "&lt/" + curNode.nodeName + "&gt\n";
				if( isDebug )
				{
					debug( spacer + "&lt/" + curNode.nodeName + "&gt" );
				}
			}
			else
			{
				str += spacer + curNode.data + "\n";
				if( isDebug )
				{
					debug( spacer + curNode.data );
				}
			}
		}
		
		return str;
	}
}

var birtSoapRequest = new BirtSoapRequest( );