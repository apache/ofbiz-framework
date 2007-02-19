/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.io.BrowserIO");

dojo.require("dojo.io.common");
dojo.require("dojo.lang.array");
dojo.require("dojo.lang.func");
dojo.require("dojo.string.extras");
dojo.require("dojo.dom");
dojo.require("dojo.undo.browser");

if(!dj_undef("window")) {

dojo.io.checkChildrenForFile = function(/*DOMNode*/node){
	//summary: Checks any child nodes of node for an input type="file" element.
	var hasFile = false;
	var inputs = node.getElementsByTagName("input");
	dojo.lang.forEach(inputs, function(input){
		if(hasFile){ return; }
		if(input.getAttribute("type")=="file"){
			hasFile = true;
		}
	});
	return hasFile; //boolean
}

dojo.io.formHasFile = function(/*DOMNode*/formNode){
	//summary: Just calls dojo.io.checkChildrenForFile().
	return dojo.io.checkChildrenForFile(formNode); //boolean
}

dojo.io.updateNode = function(/*DOMNode*/node, /*String or Object*/urlOrArgs){
	//summary: Updates a DOMnode with the result of a dojo.io.bind() call.
	//node: DOMNode
	//urlOrArgs: String or Object
	//		Either a String that has an URL, or an object containing dojo.io.bind()
	//		arguments.
	node = dojo.byId(node);
	var args = urlOrArgs;
	if(dojo.lang.isString(urlOrArgs)){
		args = { url: urlOrArgs };
	}
	args.mimetype = "text/html";
	args.load = function(t, d, e){
		while(node.firstChild){
			dojo.dom.destroyNode(node.firstChild);
		}
		node.innerHTML = d;
	};
	dojo.io.bind(args);
}

dojo.io.formFilter = function(/*DOMNode*/node) {
	//summary: Returns true if the node is an input element that is enabled, has
	//a name, and whose type is one of the following values: ["file", "submit", "image", "reset", "button"]
	var type = (node.type||"").toLowerCase();
	return !node.disabled && node.name
		&& !dojo.lang.inArray(["file", "submit", "image", "reset", "button"], type); //boolean
}

// TODO: Move to htmlUtils
dojo.io.encodeForm = function(/*DOMNode*/formNode, /*String?*/encoding, /*Function?*/formFilter){
	//summary: Converts the names and values of form elements into an URL-encoded
	//string (name=value&name=value...).
	//formNode: DOMNode
	//encoding: String?
	//		The encoding to use for the values. Specify a string that starts with
	//		"utf" (for instance, "utf8"), to use encodeURIComponent() as the encoding
	//		function. Otherwise, dojo.string.encodeAscii will be used.
	//formFilter: Function?
	//	A function used to filter out form elements. The element node will be passed
	//	to the formFilter function, and a boolean result is expected (true indicating
	//	indicating that the element should have its name/value included in the output).
	//	If no formFilter is specified, then dojo.io.formFilter() will be used.
	if((!formNode)||(!formNode.tagName)||(!formNode.tagName.toLowerCase() == "form")){
		dojo.raise("Attempted to encode a non-form element.");
	}
	if(!formFilter) { formFilter = dojo.io.formFilter; }
	var enc = /utf/i.test(encoding||"") ? encodeURIComponent : dojo.string.encodeAscii;
	var values = [];

	for(var i = 0; i < formNode.elements.length; i++){
		var elm = formNode.elements[i];
		if(!elm || elm.tagName.toLowerCase() == "fieldset" || !formFilter(elm)) { continue; }
		var name = enc(elm.name);
		var type = elm.type.toLowerCase();

		if(type == "select-multiple"){
			for(var j = 0; j < elm.options.length; j++){
				if(elm.options[j].selected) {
					values.push(name + "=" + enc(elm.options[j].value));
				}
			}
		}else if(dojo.lang.inArray(["radio", "checkbox"], type)){
			if(elm.checked){
				values.push(name + "=" + enc(elm.value));
			}
		}else{
			values.push(name + "=" + enc(elm.value));
		}
	}

	// now collect input type="image", which doesn't show up in the elements array
	var inputs = formNode.getElementsByTagName("input");
	for(var i = 0; i < inputs.length; i++) {
		var input = inputs[i];
		if(input.type.toLowerCase() == "image" && input.form == formNode
			&& formFilter(input)) {
			var name = enc(input.name);
			values.push(name + "=" + enc(input.value));
			values.push(name + ".x=0");
			values.push(name + ".y=0");
		}
	}
	return values.join("&") + "&"; //String
}

dojo.io.FormBind = function(/*DOMNode or Object*/args) {
	//summary: constructor for a dojo.io.FormBind object. See the Dojo Book for
	//some information on usage: http://manual.dojotoolkit.org/WikiHome/DojoDotBook/Book23
	//args: DOMNode or Object
	//		args can either be the DOMNode for a form element, or an object containing
	//		dojo.io.bind() arguments, one of which should be formNode with the value of
	//		a form element DOMNode.
	this.bindArgs = {};

	if(args && args.formNode) {
		this.init(args);
	} else if(args) {
		this.init({formNode: args});
	}
}
dojo.lang.extend(dojo.io.FormBind, {
	form: null,

	bindArgs: null,

	clickedButton: null,

	init: function(/*DOMNode or Object*/args) {
		//summary: Internal function called by the dojo.io.FormBind() constructor
		//do not call this method directly.
		var form = dojo.byId(args.formNode);

		if(!form || !form.tagName || form.tagName.toLowerCase() != "form") {
			throw new Error("FormBind: Couldn't apply, invalid form");
		} else if(this.form == form) {
			return;
		} else if(this.form) {
			throw new Error("FormBind: Already applied to a form");
		}

		dojo.lang.mixin(this.bindArgs, args);
		this.form = form;

		this.connect(form, "onsubmit", "submit");

		for(var i = 0; i < form.elements.length; i++) {
			var node = form.elements[i];
			if(node && node.type && dojo.lang.inArray(["submit", "button"], node.type.toLowerCase())) {
				this.connect(node, "onclick", "click");
			}
		}

		var inputs = form.getElementsByTagName("input");
		for(var i = 0; i < inputs.length; i++) {
			var input = inputs[i];
			if(input.type.toLowerCase() == "image" && input.form == form) {
				this.connect(input, "onclick", "click");
			}
		}
	},

	onSubmit: function(/*DOMNode*/form) {
		//summary: Function used to verify that the form is OK to submit.
		//Override this function if you want specific form validation done.
		return true; //boolean
	},

	submit: function(/*Event*/e) {
		//summary: internal function that is connected as a listener to the
		//form's onsubmit event.
		e.preventDefault();
		if(this.onSubmit(this.form)) {
			dojo.io.bind(dojo.lang.mixin(this.bindArgs, {
				formFilter: dojo.lang.hitch(this, "formFilter")
			}));
		}
	},

	click: function(/*Event*/e) {
		//summary: internal method that is connected as a listener to the
		//form's elements whose click event can submit a form.
		var node = e.currentTarget;
		if(node.disabled) { return; }
		this.clickedButton = node;
	},

	formFilter: function(/*DOMNode*/node) {
		//summary: internal function used to know which form element values to include
		//		in the dojo.io.bind() request.
		var type = (node.type||"").toLowerCase();
		var accept = false;
		if(node.disabled || !node.name) {
			accept = false;
		} else if(dojo.lang.inArray(["submit", "button", "image"], type)) {
			if(!this.clickedButton) { this.clickedButton = node; }
			accept = node == this.clickedButton;
		} else {
			accept = !dojo.lang.inArray(["file", "submit", "reset", "button"], type);
		}
		return accept; //boolean
	},

	// in case you don't have dojo.event.* pulled in
	connect: function(/*Object*/srcObj, /*Function*/srcFcn, /*Function*/targetFcn) {
		//summary: internal function used to connect event listeners to form elements
		//that trigger events. Used in case dojo.event is not loaded.
		if(dojo.evalObjPath("dojo.event.connect")) {
			dojo.event.connect(srcObj, srcFcn, this, targetFcn);
		} else {
			var fcn = dojo.lang.hitch(this, targetFcn);
			srcObj[srcFcn] = function(e) {
				if(!e) { e = window.event; }
				if(!e.currentTarget) { e.currentTarget = e.srcElement; }
				if(!e.preventDefault) { e.preventDefault = function() { window.event.returnValue = false; } }
				fcn(e);
			}
		}
	}
});

dojo.io.XMLHTTPTransport = new function(){
	//summary: The object that implements the dojo.io.bind transport for XMLHttpRequest.
	var _this = this;

	var _cache = {}; // FIXME: make this public? do we even need to?
	this.useCache = false; // if this is true, we'll cache unless kwArgs.useCache = false
	this.preventCache = false; // if this is true, we'll always force GET requests to cache

	// FIXME: Should this even be a function? or do we just hard code it in the next 2 functions?
	function getCacheKey(url, query, method) {
		return url + "|" + query + "|" + method.toLowerCase();
	}

	function addToCache(url, query, method, http) {
		_cache[getCacheKey(url, query, method)] = http;
	}

	function getFromCache(url, query, method) {
		return _cache[getCacheKey(url, query, method)];
	}

	this.clearCache = function() {
		_cache = {};
	}

	// moved successful load stuff here
	function doLoad(kwArgs, http, url, query, useCache) {
		if(	((http.status>=200)&&(http.status<300))|| 	// allow any 2XX response code
			(http.status==304)|| 						// get it out of the cache
			(location.protocol=="file:" && (http.status==0 || http.status==undefined))||
			(location.protocol=="chrome:" && (http.status==0 || http.status==undefined))
		){
			var ret;
			if(kwArgs.method.toLowerCase() == "head"){
				var headers = http.getAllResponseHeaders();
				ret = {};
				ret.toString = function(){ return headers; }
				var values = headers.split(/[\r\n]+/g);
				for(var i = 0; i < values.length; i++) {
					var pair = values[i].match(/^([^:]+)\s*:\s*(.+)$/i);
					if(pair) {
						ret[pair[1]] = pair[2];
					}
				}
			}else if(kwArgs.mimetype == "text/javascript"){
				try{
					ret = dj_eval(http.responseText);
				}catch(e){
					dojo.debug(e);
					dojo.debug(http.responseText);
					ret = null;
				}
			}else if(kwArgs.mimetype == "text/json" || kwArgs.mimetype == "application/json"){
				try{
					ret = dj_eval("("+http.responseText+")");
				}catch(e){
					dojo.debug(e);
					dojo.debug(http.responseText);
					ret = false;
				}
			}else if((kwArgs.mimetype == "application/xml")||
						(kwArgs.mimetype == "text/xml")){
				ret = http.responseXML;
				if(!ret || typeof ret == "string" || !http.getResponseHeader("Content-Type")) {
					ret = dojo.dom.createDocumentFromText(http.responseText);
				}
			}else{
				ret = http.responseText;
			}

			if(useCache){ // only cache successful responses
				addToCache(url, query, kwArgs.method, http);
			}
			kwArgs[(typeof kwArgs.load == "function") ? "load" : "handle"]("load", ret, http, kwArgs);
		}else{
			var errObj = new dojo.io.Error("XMLHttpTransport Error: "+http.status+" "+http.statusText);
			kwArgs[(typeof kwArgs.error == "function") ? "error" : "handle"]("error", errObj, http, kwArgs);
		}
	}

	// set headers (note: Content-Type will get overriden if kwArgs.contentType is set)
	function setHeaders(http, kwArgs){
		if(kwArgs["headers"]) {
			for(var header in kwArgs["headers"]) {
				if(header.toLowerCase() == "content-type" && !kwArgs["contentType"]) {
					kwArgs["contentType"] = kwArgs["headers"][header];
				} else {
					http.setRequestHeader(header, kwArgs["headers"][header]);
				}
			}
		}
	}

	this.inFlight = [];
	this.inFlightTimer = null;

	this.startWatchingInFlight = function(){
		//summary: internal method used to trigger a timer to watch all inflight
		//XMLHttpRequests.
		if(!this.inFlightTimer){
			// setInterval broken in mozilla x86_64 in some circumstances, see
			// https://bugzilla.mozilla.org/show_bug.cgi?id=344439
			// using setTimeout instead
			this.inFlightTimer = setTimeout("dojo.io.XMLHTTPTransport.watchInFlight();", 10);
		}
	}

	this.watchInFlight = function(){
		//summary: internal method that checks each inflight XMLHttpRequest to see
		//if it has completed or if the timeout situation applies.
		var now = null;
		// make sure sync calls stay thread safe, if this callback is called during a sync call
		// and this results in another sync call before the first sync call ends the browser hangs
		if(!dojo.hostenv._blockAsync && !_this._blockAsync){
			for(var x=this.inFlight.length-1; x>=0; x--){
				try{
					var tif = this.inFlight[x];
					if(!tif || tif.http._aborted || !tif.http.readyState){
						this.inFlight.splice(x, 1); continue; 
					}
					if(4==tif.http.readyState){
						// remove it so we can clean refs
						this.inFlight.splice(x, 1);
						doLoad(tif.req, tif.http, tif.url, tif.query, tif.useCache);
					}else if (tif.startTime){
						//See if this is a timeout case.
						if(!now){
							now = (new Date()).getTime();
						}
						if(tif.startTime + (tif.req.timeoutSeconds * 1000) < now){
							//Stop the request.
							if(typeof tif.http.abort == "function"){
								tif.http.abort();
							}
		
							// remove it so we can clean refs
							this.inFlight.splice(x, 1);
							tif.req[(typeof tif.req.timeout == "function") ? "timeout" : "handle"]("timeout", null, tif.http, tif.req);
						}
					}
				}catch(e){
					try{
						var errObj = new dojo.io.Error("XMLHttpTransport.watchInFlight Error: " + e);
						tif.req[(typeof tif.req.error == "function") ? "error" : "handle"]("error", errObj, tif.http, tif.req);
					}catch(e2){
						dojo.debug("XMLHttpTransport error callback failed: " + e2);
					}
				}
			}
		}

		clearTimeout(this.inFlightTimer);
		if(this.inFlight.length == 0){
			this.inFlightTimer = null;
			return;
		}
		this.inFlightTimer = setTimeout("dojo.io.XMLHTTPTransport.watchInFlight();", 10);
	}

	var hasXmlHttp = dojo.hostenv.getXmlhttpObject() ? true : false;
	this.canHandle = function(/*dojo.io.Request*/kwArgs){
		//summary: Tells dojo.io.bind() if this is a good transport to
		//use for the particular type of request. This type of transport cannot
		//handle forms that have an input type="file" element.

		// FIXME: we need to determine when form values need to be
		// multi-part mime encoded and avoid using this transport for those
		// requests.
		return hasXmlHttp
			&& dojo.lang.inArray(["text/plain", "text/html", "application/xml", "text/xml", "text/javascript", "text/json", "application/json"], (kwArgs["mimetype"].toLowerCase()||""))
			&& !( kwArgs["formNode"] && dojo.io.formHasFile(kwArgs["formNode"]) ); //boolean
	}

	this.multipartBoundary = "45309FFF-BD65-4d50-99C9-36986896A96F";	// unique guid as a boundary value for multipart posts

	this.bind = function(/*dojo.io.Request*/kwArgs){
		//summary: function that sends the request to the server.

		//This function will attach an abort() function to the kwArgs dojo.io.Request object,
		//so if you need to abort the request, you can call that method on the request object.
		//The following are acceptable properties in kwArgs (in addition to the
		//normal dojo.io.Request object properties).
		//url: String: URL the server URL to use for the request.
		//method: String: the HTTP method to use (GET, POST, etc...).
		//mimetype: Specifies what format the result data should be given to the load/handle callback. Valid values are:
		//		text/javascript, text/json, application/json, application/xml, text/xml. Any other mimetype will give back a text
		//		string.
		//transport: String: specify "XMLHTTPTransport" to force the use of this XMLHttpRequest transport.
		//headers: Object: The object property names and values will be sent as HTTP request header
		//		names and values.
		//sendTransport: boolean: If true, then dojo.transport=xmlhttp will be added to the request.
		//encoding: String: The type of encoding to use when dealing with the content kwArgs property.
		//content: Object: The content object is converted into a name=value&name=value string, by
		//		using dojo.io.argsFromMap(). The encoding kwArgs property is passed to dojo.io.argsFromMap()
		//		for use in encoding the names and values. The resulting string is added to the request.
		//formNode: DOMNode: a form element node. This should not normally be used. Use new dojo.io.FormBind() instead.
		//		If formNode is used, then the names and values of the form elements will be converted
		//		to a name=value&name=value string and added to the request. The encoding kwArgs property is used
		//		to encode the names and values.
		//postContent: String: Raw name=value&name=value string to be included as part of the request.
		//back or backButton: Function: A function to be called if the back button is pressed. If this kwArgs property
		//		is used, then back button support via dojo.undo.browser will be used. See notes for dojo.undo.browser on usage.
		//		You need to set djConfig.preventBackButtonFix = false to enable back button support.
		//changeUrl: boolean or String: Used as part of back button support. See notes for dojo.undo.browser on usage.
		//user: String: The user name. Used in conjuction with password. Passed to XMLHttpRequest.open().
		//password: String: The user's password. Used in conjuction with user. Passed to XMLHttpRequest.open().
		//file: Object or Array of Objects: an object simulating a file to be uploaded. file objects should have the following properties:
		//		name or fileName: the name of the file
		//		contentType: the MIME content type for the file.
		//		content: the actual content of the file.
		//multipart: boolean: indicates whether this should be a multipart mime request. If kwArgs.file exists, then this
		//		property is set to true automatically.
		//sync: boolean: if true, then a synchronous XMLHttpRequest call is done,
		//		if false (the default), then an asynchronous call is used.
		//preventCache: boolean: If true, then a cache busting parameter is added to the request URL.
		//		default value is false.
		//useCache: boolean: If true, then XMLHttpTransport will keep an internal cache of the server
		//		response and use that response if a similar request is done again.
		//		A similar request is one that has the same URL, query string and HTTP method value.
		//		default is false.
		if(!kwArgs["url"]){
			// are we performing a history action?
			if( !kwArgs["formNode"]
				&& (kwArgs["backButton"] || kwArgs["back"] || kwArgs["changeUrl"] || kwArgs["watchForURL"])
				&& (!djConfig.preventBackButtonFix)) {
        dojo.deprecated("Using dojo.io.XMLHTTPTransport.bind() to add to browser history without doing an IO request",
        				"Use dojo.undo.browser.addToHistory() instead.", "0.4");
				dojo.undo.browser.addToHistory(kwArgs);
				return true;
			}
		}

		// build this first for cache purposes
		var url = kwArgs.url;
		var query = "";
		if(kwArgs["formNode"]){
			var ta = kwArgs.formNode.getAttribute("action");
			if((ta)&&(!kwArgs["url"])){ url = ta; }
			var tp = kwArgs.formNode.getAttribute("method");
			if((tp)&&(!kwArgs["method"])){ kwArgs.method = tp; }
			query += dojo.io.encodeForm(kwArgs.formNode, kwArgs.encoding, kwArgs["formFilter"]);
		}

		if(url.indexOf("#") > -1) {
			dojo.debug("Warning: dojo.io.bind: stripping hash values from url:", url);
			url = url.split("#")[0];
		}

		if(kwArgs["file"]){
			// force post for file transfer
			kwArgs.method = "post";
		}

		if(!kwArgs["method"]){
			kwArgs.method = "get";
		}

		// guess the multipart value
		if(kwArgs.method.toLowerCase() == "get"){
			// GET cannot use multipart
			kwArgs.multipart = false;
		}else{
			if(kwArgs["file"]){
				// enforce multipart when sending files
				kwArgs.multipart = true;
			}else if(!kwArgs["multipart"]){
				// default 
				kwArgs.multipart = false;
			}
		}

		if(kwArgs["backButton"] || kwArgs["back"] || kwArgs["changeUrl"]){
			dojo.undo.browser.addToHistory(kwArgs);
		}

		var content = kwArgs["content"] || {};

		if(kwArgs.sendTransport) {
			content["dojo.transport"] = "xmlhttp";
		}

		do { // break-block
			if(kwArgs.postContent){
				query = kwArgs.postContent;
				break;
			}

			if(content) {
				query += dojo.io.argsFromMap(content, kwArgs.encoding);
			}
			
			if(kwArgs.method.toLowerCase() == "get" || !kwArgs.multipart){
				break;
			}

			var	t = [];
			if(query.length){
				var q = query.split("&");
				for(var i = 0; i < q.length; ++i){
					if(q[i].length){
						var p = q[i].split("=");
						t.push(	"--" + this.multipartBoundary,
								"Content-Disposition: form-data; name=\"" + p[0] + "\"", 
								"",
								p[1]);
					}
				}
			}

			if(kwArgs.file){
				if(dojo.lang.isArray(kwArgs.file)){
					for(var i = 0; i < kwArgs.file.length; ++i){
						var o = kwArgs.file[i];
						t.push(	"--" + this.multipartBoundary,
								"Content-Disposition: form-data; name=\"" + o.name + "\"; filename=\"" + ("fileName" in o ? o.fileName : o.name) + "\"",
								"Content-Type: " + ("contentType" in o ? o.contentType : "application/octet-stream"),
								"",
								o.content);
					}
				}else{
					var o = kwArgs.file;
					t.push(	"--" + this.multipartBoundary,
							"Content-Disposition: form-data; name=\"" + o.name + "\"; filename=\"" + ("fileName" in o ? o.fileName : o.name) + "\"",
							"Content-Type: " + ("contentType" in o ? o.contentType : "application/octet-stream"),
							"",
							o.content);
				}
			}

			if(t.length){
				t.push("--"+this.multipartBoundary+"--", "");
				query = t.join("\r\n");
			}
		}while(false);

		// kwArgs.Connection = "close";

		var async = kwArgs["sync"] ? false : true;

		var preventCache = kwArgs["preventCache"] ||
			(this.preventCache == true && kwArgs["preventCache"] != false);
		var useCache = kwArgs["useCache"] == true ||
			(this.useCache == true && kwArgs["useCache"] != false );

		// preventCache is browser-level (add query string junk), useCache
		// is for the local cache. If we say preventCache, then don't attempt
		// to look in the cache, but if useCache is true, we still want to cache
		// the response
		if(!preventCache && useCache){
			var cachedHttp = getFromCache(url, query, kwArgs.method);
			if(cachedHttp){
				doLoad(kwArgs, cachedHttp, url, query, false);
				return;
			}
		}

		// much of this is from getText, but reproduced here because we need
		// more flexibility
		var http = dojo.hostenv.getXmlhttpObject(kwArgs);	
		var received = false;

		// build a handler function that calls back to the handler obj
		if(async){
			var startTime = 
			// FIXME: setting up this callback handler leaks on IE!!!
			this.inFlight.push({
				"req":		kwArgs,
				"http":		http,
				"url":	 	url,
				"query":	query,
				"useCache":	useCache,
				"startTime": kwArgs.timeoutSeconds ? (new Date()).getTime() : 0
			});
			this.startWatchingInFlight();
		}else{
			// block async callbacks until sync is in, needed in khtml, others?
			_this._blockAsync = true;
		}

		if(kwArgs.method.toLowerCase() == "post"){
			// FIXME: need to hack in more flexible Content-Type setting here!
			if (!kwArgs.user) {
				http.open("POST", url, async);
			}else{
        http.open("POST", url, async, kwArgs.user, kwArgs.password);
			}
			setHeaders(http, kwArgs);
			http.setRequestHeader("Content-Type", kwArgs.multipart ? ("multipart/form-data; boundary=" + this.multipartBoundary) : 
				(kwArgs.contentType || "application/x-www-form-urlencoded"));
			try{
				http.send(query);
			}catch(e){
				if(typeof http.abort == "function"){
					http.abort();
				}
				doLoad(kwArgs, {status: 404}, url, query, useCache);
			}
		}else{
			var tmpUrl = url;
			if(query != "") {
				tmpUrl += (tmpUrl.indexOf("?") > -1 ? "&" : "?") + query;
			}
			if(preventCache) {
				tmpUrl += (dojo.string.endsWithAny(tmpUrl, "?", "&")
					? "" : (tmpUrl.indexOf("?") > -1 ? "&" : "?")) + "dojo.preventCache=" + new Date().valueOf();
			}
			if (!kwArgs.user) {
				http.open(kwArgs.method.toUpperCase(), tmpUrl, async);
			}else{
				http.open(kwArgs.method.toUpperCase(), tmpUrl, async, kwArgs.user, kwArgs.password);
			}
			setHeaders(http, kwArgs);
			try {
				http.send(null);
			}catch(e)	{
				if(typeof http.abort == "function"){
					http.abort();
				}
				doLoad(kwArgs, {status: 404}, url, query, useCache);
			}
		}

		if( !async ) {
			doLoad(kwArgs, http, url, query, useCache);
			_this._blockAsync = false;
		}

		kwArgs.abort = function(){
			try{// khtml doesent reset readyState on abort, need this workaround
				http._aborted = true; 
			}catch(e){/*squelsh*/}
			return http.abort();
		}

		return;
	}
	dojo.io.transports.addTransport("XMLHTTPTransport");
}

}