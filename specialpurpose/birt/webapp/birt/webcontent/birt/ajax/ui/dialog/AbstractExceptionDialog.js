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
 *	Birt error dialog.
 */
AbstractExceptionDialog = function( ) { };

AbstractExceptionDialog.prototype = Object.extend( new AbstractBaseDialog( ),
{
	__faultCode : "",

	__setFaultContainersWidth: function( width )
	{		
		document.getElementById("faultStringContainer").style.width = width;
		document.getElementById("exceptionTraceContainer").style.width = width;
	},

	
	/**
	 * Formats the given stack trace for HTML output.
	 * @param data stack trace
	 * @return formatted HTML data
	 */
	__formatStackTrace : function( data )	
	{
		if ( !data )
		{
			return "";
		}
		return data.replace(/\r?\n/g,"<br/>").replace(/[\s]{1}at/g,"&nbsp;&nbsp;&nbsp;at");		
	},
	
	/**
	 *	Binding data to the dialog UI. Data includes zoom scaling factor.
	 *
	 *	@data, data DOM tree (schema TBD)
	 *	@return, void
	 */
	__bind: function( data )
	{
	 	if( !data )
	 	{
	 		return;
	 	}
		
	 	var oSpans = this.__instance.getElementsByTagName( 'SPAN' );
		
	 	// Prepare fault string (reason)
	 	var faultStrings = data.getElementsByTagName( 'faultstring' );
	 	if ( faultStrings[0] && faultStrings[0].firstChild )
	 	{
			oSpans[0].innerHTML = faultStrings[0].firstChild.data;
		}
		else
		{
			oSpans[0].innerHTML = "";
		}

	 	// Prepare fault detail (Stack traces)
	 	var faultDetail = data.getElementsByTagName( 'string' );
	 	if ( faultDetail && faultDetail.length > 0 )
	 	{
	 		var detailSpan = oSpans[1];
	 		for ( var detailIndex = 0; detailIndex < faultDetail.length; detailIndex++ )
	 		{
	 			if ( faultDetail[detailIndex].hasChildNodes() )
	 			{
	 				var detailNodes = faultDetail[detailIndex].childNodes;
	 				if ( detailIndex > 0 )
	 				{
	 					detailSpan.appendChild( document.createElement("hr") );
	 				}
	 				var detailElement = document.createElement("div");	 				
 					detailElement.style.whiteSpace = "nowrap";
 					if ( detailIndex > 0 )
 					{
 						detailElement.style.borderTopStyle = "solid";
 						detailElement.style.borderTopWidth = "1px";
 					}
	 				
	 				for ( var textIndex = 0; textIndex < detailNodes.length; textIndex++ )
	 				{
		 				var stackTrace = detailNodes[textIndex].data;
		 				stackTrace = this.__formatStackTrace( stackTrace )
		 				var stackTraceElement = document.createElement("span");
		 				stackTraceElement.innerHTML = stackTrace;
		 				detailElement.appendChild( stackTraceElement );		 				
		 				detailSpan.appendChild(detailElement);
	 				}
	 			}
	 		}
		}
		else
		{
			oSpans[1].innerHTML = "";
		}

		var faultCodeElement = data.getElementsByTagName( 'faultcode' );
	 	if ( faultCodeElement[0] && faultCodeElement[0].firstChild )
	 	{
			this.__faultCode = faultCodeElement[0].firstChild.data;
		}
		else
		{
			this.__faultCode = "";
		}
	
		if ( this.__faultCode == "DocumentProcessor.getPageNumber( )" )
		{
			birtEventDispatcher.broadcastEvent( 
				birtEvent.__E_GETPAGE_INIT, 
				{ name : Constants.PARAM_PAGE, value : 1 } 
				);
		}				
	
	}
} );