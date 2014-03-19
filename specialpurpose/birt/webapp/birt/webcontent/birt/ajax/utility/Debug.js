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
 * Show the debug window
 */
function showDebug(soapMessageDebug, regularDebug)
{
	if(soapMessageDebug == true)
	{
		var url = document.location.href;
		var index = url.indexOf( "wr" );
		url = url.substring( 0, index ) + "iportal/bizRD/test/DebugSoapMessage.jsp";
		var oldWindow = window.open("", "DebugSoapMessage");
		if(oldWindow)
		{
			oldWindow.close();
		} 
		window.top.debugWindow = window.open(url, "DebugSoapMessage", 
						"left=300,top=300,width=450,height=300,scrollbars=yes,"
	                  +"status=yes,resizable=yes");
		window.top.debugWindow.soapMsgWindow = true;
		window.top.debugWindow.opener = self;

	}
	else if(regularDebug == true)
	{
	  window.top.debugWindow =
	      window.open("",
	                  "Debug",
	                  "left=0,top=0,width=200,height=300,scrollbars=yes,"
	                  +"status=yes,resizable=yes");
	  window.top.debugWindow.opener = self;
	  // open the document for writing
	  window.top.debugWindow.document.open();
	  window.top.debugWindow.document.write(
	      "<HTML><HEAD><TITLE>Debug Window</TITLE></HEAD><BODY><PRE>\n");
	}
     
}

/*
 * Checks if debugging is currently enabled.
 */
function isDebugging( )
{
	try
	{
		return window.top.debugWindow && !window.top.debugWindow.closed;
	}
	catch(e)
	{
		return false; 
	}
}


/*
 * If the debug window exists, write debug messages to it.
 * If isSoapMessage, write output to special soap message window
 */
function debug( text, isSoapMessage )
{
	//debug( birtSoapRequest.prettyPrintXML(request.responseXML.documentElement), true);
	try
	{
		if ( isDebugging( ) )
		{
			if(window.top.debugWindow.soapMsgWindow)
			{
				var debugDiv;
				if(isSoapMessage)
				{
				 	debugDiv = window.top.debugWindow.document.getElementById("soapMsgDebug");
				 	var div = window.top.debugWindow.document.createElement("div");
					div.innerHTML = "<pre>" + text;
					//debugDiv.insertBefore(window.top.debugWindow.document.createTextNode("-------------END--------------"),debugDiv.firstChild);
					debugDiv.insertBefore(div,debugDiv.firstChild);
					div.style.display = "none";
					var btn = addDebugButton(text);
					debugDiv.insertBefore(btn, debugDiv.firstChild);
					//debugDiv.insertBefore(window.top.debugWindow.document.createTextNode("-------------START----------------"),debugDiv.firstChild);			
					debugDiv.insertBefore(window.top.debugWindow.document.createElement("br"),debugDiv.firstChild);
				 	
				 }
				 else
				 {
				 	debugDiv = window.top.debugWindow.document.getElementById("regularDebug").firstChild;
				 	var div = window.top.debugWindow.document.createElement("div");
					div.innerHTML = "<pre>" + text;				
					debugDiv.appendChild(div);				 	
				}
			}
				 
			else
			{
		    	window.top.debugWindow.document.write(text+"\n");	
		   	}
		}
	}
	catch(e){ }
}

g_debugButtonDivList = [];

function addDebugButton(text)
{
	var numTargets = text.match(/Target&gt/g);
	var txt = window.top.debugWindow.document.createTextNode((numTargets ? "XML" : " Text"));
	var msgType = text.match(/(GetUpdatedObjectsResponse|GetUpdatedObjects)/);
	if(msgType)
	{
		if(msgType[0] == "GetUpdatedObjectsResponse")
		{
			var msgTypeTxt = window.top.debugWindow.document.createTextNode("--Response--");
		}
		else if(msgType[0] == "GetUpdatedObjects")
		{
			var msgTypeTxt = window.top.debugWindow.document.createTextNode("--Request--");
		}
	}
	else
	{
		var msgTypeTxt = window.top.debugWindow.document.createTextNode("no message type");
	}		
	var buttonDiv = window.top.debugWindow.document.createElement("div");	
	buttonDiv.appendChild(msgTypeTxt);
	buttonDiv.appendChild(txt);
		
	if(numTargets && msgTypeTxt.nodeValue == "testing--Response--")
	{
		var updateData = text.match(/UpdateData&gt[\w\s\.()"&]*&lt\/UpdateData/g);
		if(updateData)
		{
			for(var i = 0; i < updateData.length; i++)
			{
				var dataType = updateData[i].match(/&ltData&gt\s*&lt[\w]*&gt/g);
				dataType = dataType[0];
				dataType = dataType.match(/&gt\s*&lt[\w]*&gt/g);
				dataType = dataType[0];
				dataType = dataType.replace(/\s*/, "");	
				dataType = dataType.replace(/\n*/, "");
				var targets = updateData[i].match(/Target&gt\s*[A-Za-z_]*\s*&lt\//g);
				if(targets)
				{
					for(var j = 0; j < targets.length; j++)
					{
						var targTxt = targets[j].match(/\n\s*[A-Za-z_]*\n/);
						if(targTxt)
						{	
							var targetDiv = window.top.debugWindow.document.createElement("div");
							targTxt = targTxt[0];
							targTxt = targTxt.replace(/\s*/, "");	
							targTxt = targTxt.replace(/\n*/, "");
							targetDiv.appendChild(window.top.debugWindow.document.createTextNode("Target: " + targTxt + " DataType: " + dataType));
							buttonDiv.appendChild(targetDiv);
						}
					}
				}
			}
		}
	}
	if(numTargets && msgTypeTxt.nodeValue == "--Response--")
	{
		var targets = text.match(/Target&gt\s*[A-Za-z_]*\s*&lt\//g);
		if(targets)
		{
			for(var i = 0; i < targets.length; i++)
			{
				var targTxt = targets[i].match(/\n\s*[A-Za-z_]*\n/);
				if(targTxt)
				{	
					var targetDiv = window.top.debugWindow.document.createElement("div");
					targTxt = targTxt[0];
					targTxt = targTxt.replace(/\s*/, "");	
					targTxt = targTxt.replace(/\n*/, "");
					targetDiv.appendChild(window.top.debugWindow.document.createTextNode("Target: " + targTxt));
					buttonDiv.appendChild(targetDiv);
				}
			}
		}
	
	}
	else if(msgTypeTxt.nodeValue == "--Request--")
	{
		var targets = text.match(/Operator&gt\s*[A-Za-z_]*\s*&lt\//g);
		if(targets)
		{
			for(var i = 0; i < targets.length; i++)
			{
				var targTxt = targets[i].match(/\n\s*[A-Za-z_]*\n/);
				if(targTxt)
				{	
					var targetDiv = window.top.debugWindow.document.createElement("div");
					targTxt = targTxt[0];
					targTxt = targTxt.replace(/\s*/, "");	
					targTxt = targTxt.replace(/\n*/, "");
					targetDiv.appendChild(window.top.debugWindow.document.createTextNode(targTxt));
					buttonDiv.appendChild(targetDiv);
				}
			}
		}
	}
	buttonDiv.style.backgroundColor = "#ccffcc";
	buttonDiv.onmousedown = pushDebugButton;
	g_debugButtonDivList.push(buttonDiv);
	buttonDiv.listIndex = g_debugButtonDivList.length -1;
	return buttonDiv;
}

function pushDebugButton(e)
{
	if(!e) var e = window.top.debugWindow.event;
	var targ = Event.element(e);
	while(!targ.listIndex && targ.listIndex != 0)
	{
		targ = targ.parentNode;
	}
	var btn = g_debugButtonDivList[targ.listIndex];
	if(btn.nextSibling.style.display == "block")
	{
		btn.style.backgroundColor = "#ccffcc";
		btn.nextSibling.style.display = "none";
	}
	else
	{
		btn.style.backgroundColor = "#ff0000";
		btn.nextSibling.style.display = "block";
	}
	
}

/**
 * If the debug window exists, then close it
 */
function hideDebug()
{
	if (window.top.debugWindow && !window.top.debugWindow.closed)
	{
		window.top.debugWindow.close();
		window.top.debugWindow = null;
	}
}