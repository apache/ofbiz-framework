/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.HtmlWidget");
dojo.require("dojo.widget.DomWidget");
dojo.require("dojo.html.util");
dojo.require("dojo.html.display");
dojo.require("dojo.html.layout");
dojo.require("dojo.lang.extras");
dojo.require("dojo.lang.func");
dojo.require("dojo.lfx.toggle");

dojo.declare("dojo.widget.HtmlWidget", dojo.widget.DomWidget, {								 
	// summary
	//	Base class for all browser based widgets, or at least "html" widgets.
	//	The meaning of "html" has become unclear; in practice, all widgets derive from this class.
	
	// templateCssPath: String
	//	Path to CSS file for this widget
	templateCssPath: null,
	
	// templatePath: String
	//	Path to template (HTML file) for this widget
	templatePath: null,

	// lang: String
	//	Language to display this widget in (like en-us).
	//	Defaults to brower's specified preferred language (typically the language of the OS)
	lang: "",

	// toggle: String
	//	Controls animation effect for when show() and hide() (or toggle()) are called.
	//	Possible values: "plain", "wipe", "fade", "explode"
	toggle: "plain",

	// toggleDuration: Integer
	//	Number of milliseconds for toggle animation effect to complete
	toggleDuration: 150,

	initialize: function(args, frag){
		// summary: called after the widget is rendered; most subclasses won't override or call this function
	},

	postMixInProperties: function(args, frag){
		if(this.lang === ""){this.lang = null;}
		// now that we know the setting for toggle, get toggle object
		// (default to plain toggler if user specified toggler not present)
		this.toggleObj =
			dojo.lfx.toggle[this.toggle.toLowerCase()] || dojo.lfx.toggle.plain;
	},

	createNodesFromText: function(txt, wrap){
		return dojo.html.createNodesFromText(txt, wrap);
	},

	destroyRendering: function(finalize){
		try{
			if(this.bgIframe){
				this.bgIframe.remove();
				delete this.bgIframe;
			}
			if(!finalize && this.domNode){
				dojo.event.browser.clean(this.domNode);
			}
			dojo.widget.HtmlWidget.superclass.destroyRendering.call(this);
		}catch(e){ /* squelch! */ }
	},

	/////////////////////////////////////////////////////////
	// Displaying/hiding the widget
	/////////////////////////////////////////////////////////
	isShowing: function(){
		// summary
		//	Tests whether widget is set to show-mode or hide-mode (see show() and 
		//	hide() methods)
		//
		//	This function is poorly named.  Even if widget is in show-mode,
		//	if it's inside a container that's hidden
		//	(either a container widget, or just a domnode with display:none),
		//	then it won't be displayed
		return dojo.html.isShowing(this.domNode);	// Boolean
	},

	toggleShowing: function(){
		// summary: show or hide the widget, to switch it's state
		if(this.isShowing()){
			this.hide();
		}else{
			this.show();
		}
	},

	show: function(){
		// summary: show the widget
		if(this.isShowing()){ return; }
		this.animationInProgress=true;
		this.toggleObj.show(this.domNode, this.toggleDuration, null,
			dojo.lang.hitch(this, this.onShow), this.explodeSrc);
	},

	onShow: function(){
		// summary: called after the show() animation has completed
		this.animationInProgress=false;
		this.checkSize();
	},

	hide: function(){
		// summary: hide the widget (ending up with display:none)
		if(!this.isShowing()){ return; }
		this.animationInProgress = true;
		this.toggleObj.hide(this.domNode, this.toggleDuration, null,
			dojo.lang.hitch(this, this.onHide), this.explodeSrc);
	},

	onHide: function(){
		// summary: called after the hide() animation has completed
		this.animationInProgress=false;
	},

	//////////////////////////////////////////////////////////////////////////////
	// Sizing related methods
	//  If the parent changes size then for each child it should call either
	//   - resizeTo(): size the child explicitly
	//   - or checkSize(): notify the child the the parent has changed size
	//////////////////////////////////////////////////////////////////////////////

	_isResized: function(w, h){
		// summary
		//	Test if my size has changed.
		//	If width & height are specified then that's my new size; otherwise,
		//	query outerWidth/outerHeight of my domNode

		// If I'm not being displayed then disregard (show() must
		// check if the size has changed)
		if(!this.isShowing()){ return false; }

		// If my parent has been resized and I have style="height: 100%"
		// or something similar then my size has changed too.
		var wh = dojo.html.getMarginBox(this.domNode);
		var width=w||wh.width;
		var height=h||wh.height;
		if(this.width == width && this.height == height){ return false; }

		this.width=width;
		this.height=height;
		return true;
	},

	checkSize: function(){
		// summary
		//	Called when my parent has changed size, but my parent won't call resizeTo().
		//	This is useful if my size is height:100% or something similar.
		//	Also called whenever I am shown, because the first time I am shown I may need
		//	to do size calculations.
		if(!this._isResized()){ return; }
		this.onResized();
	},

	resizeTo: function(w, h){
		// summary: explicitly set this widget's size (in pixels).
		dojo.html.setMarginBox(this.domNode, { width: w, height: h });
		
		// can't do sizing if widget is hidden because referencing node.offsetWidth/node.offsetHeight returns 0.
		// do sizing on show() instead.
		if(this.isShowing()){
			this.onResized();
		}
	},

	resizeSoon: function(){
		// summary
		//	schedule onResized() to be called soon, after browser has had
		//	a little more time to calculate the sizes
		if(this.isShowing()){
			dojo.lang.setTimeout(this, this.onResized, 0);
		}
	},

	onResized: function(){
		// summary
		//	Called when my size has changed.
		//	Must notify children if their size has (possibly) changed.
		dojo.lang.forEach(this.children, function(child){ if(child.checkSize){child.checkSize();} });
	}
});
