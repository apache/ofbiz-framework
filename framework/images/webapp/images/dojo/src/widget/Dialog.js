/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.Dialog");

dojo.require("dojo.widget.*");
dojo.require("dojo.widget.ContentPane");
dojo.require("dojo.event.*");
dojo.require("dojo.gfx.color");
dojo.require("dojo.html.layout");
dojo.require("dojo.html.display");
dojo.require("dojo.html.iframe");

dojo.declare(
	"dojo.widget.ModalDialogBase", 
	null,
	{
		// summary
		//	Mixin for widgets implementing a modal dialog

		isContainer: true,

		// focusElement: String
		//	provide a focusable element or element id if you need to
		//	work around FF's tendency to send focus into outer space on hide
		focusElement: "",

		// bgColor: String
		//	color of viewport when displaying a dialog
		bgColor: "black",
		
		// bgOpacity: Number
		//	opacity (0~1) of viewport color (see bgColor attribute)
		bgOpacity: 0.4,

		// followScroll: Boolean
		//	if true, readjusts the dialog (and dialog background) when the user moves the scrollbar
		followScroll: true,

		// closeOnBackgroundClick: Boolean
		//	clicking anywhere on the background will close the dialog
		closeOnBackgroundClick: false,

		trapTabs: function(/*Event*/ e){
			// summary
			//	callback on focus
			if(e.target == this.tabStartOuter) {
				if(this._fromTrap) {
					this.tabStart.focus();
					this._fromTrap = false;
				} else {
					this._fromTrap = true;
					this.tabEnd.focus();
				}
			} else if (e.target == this.tabStart) {
				if(this._fromTrap) {
					this._fromTrap = false;
				} else {
					this._fromTrap = true;
					this.tabEnd.focus();
				}
			} else if(e.target == this.tabEndOuter) {
				if(this._fromTrap) {
					this.tabEnd.focus();
					this._fromTrap = false;
				} else {
					this._fromTrap = true;
					this.tabStart.focus();
				}
			} else if(e.target == this.tabEnd) {
				if(this._fromTrap) {
					this._fromTrap = false;
				} else {
					this._fromTrap = true;
					this.tabStart.focus();
				}
			}
		},

		clearTrap: function(/*Event*/ e) {
			// summary
			//	callback on blur
			var _this = this;
			setTimeout(function() {
				_this._fromTrap = false;
			}, 100);
		},

		postCreate: function() {
			// summary
			//	if the target mixin class already defined postCreate,
			//	dojo.widget.ModalDialogBase.prototype.postCreate.call(this)
			//	should be called in its postCreate()
			with(this.domNode.style){
				position = "absolute";
				zIndex = 999;
				display = "none";
				overflow = "visible";
			}
			var b = dojo.body();
			b.appendChild(this.domNode);

			// make background (which sits behind the dialog but above the normal text)
			this.bg = document.createElement("div");
			this.bg.className = "dialogUnderlay";
			with(this.bg.style){
				position = "absolute";
				left = top = "0px";
				zIndex = 998;
				display = "none";
			}
			b.appendChild(this.bg);
			this.setBackgroundColor(this.bgColor);

			this.bgIframe = new dojo.html.BackgroundIframe();
            if(this.bgIframe.iframe){
				with(this.bgIframe.iframe.style){
					position = "absolute";
					left = top = "0px";
					zIndex = 90;
					display = "none";
				}
			}

			if(this.closeOnBackgroundClick){
				dojo.event.kwConnect({srcObj: this.bg, srcFunc: "onclick",
					adviceObj: this, adviceFunc: "onBackgroundClick", once: true});
			}
		},

		uninitialize: function(){
			this.bgIframe.remove();
			dojo.html.removeNode(this.bg, true);
		},

		setBackgroundColor: function(/*String*/ color) {
			// summary
			//	changes background color specified by "bgColor" parameter
			//	usage:
			//		setBackgroundColor("black");
			//		setBackgroundColor(0xff, 0xff, 0xff);
			if(arguments.length >= 3) {
				color = new dojo.gfx.color.Color(arguments[0], arguments[1], arguments[2]);
			} else {
				color = new dojo.gfx.color.Color(color);
			}
			this.bg.style.backgroundColor = color.toString();
			return this.bgColor = color;	// String: the color
		},

		setBackgroundOpacity: function(/*Number*/ op) {
			// summary
			//	changes background opacity set by "bgOpacity" parameter
			if(arguments.length == 0) { op = this.bgOpacity; }
			dojo.html.setOpacity(this.bg, op);
			try {
				this.bgOpacity = dojo.html.getOpacity(this.bg);
			} catch (e) {
				this.bgOpacity = op;
			}
			return this.bgOpacity;	// Number: the opacity
		},

		_sizeBackground: function() {
			if(this.bgOpacity > 0) {
				
				var viewport = dojo.html.getViewport();
				var h = viewport.height;
				var w = viewport.width;
				with(this.bg.style){
					width = w + "px";
					height = h + "px";
				}
				var scroll_offset = dojo.html.getScroll().offset;
				this.bg.style.top = scroll_offset.y + "px";
				this.bg.style.left = scroll_offset.x + "px";
				// process twice since the scroll bar may have been removed
				// by the previous resizing
				var viewport = dojo.html.getViewport();
				if (viewport.width != w) { this.bg.style.width = viewport.width + "px"; }
				if (viewport.height != h) { this.bg.style.height = viewport.height + "px"; }
			}
			this.bgIframe.size(this.bg);
		},

		_showBackground: function() {
			if(this.bgOpacity > 0) {
				this.bg.style.display = "block";
			}
			if(this.bgIframe.iframe){
				this.bgIframe.iframe.style.display = "block";
			}
		},

		placeModalDialog: function() {
			// summary: position modal dialog in center of screen

			var scroll_offset = dojo.html.getScroll().offset;
			var viewport_size = dojo.html.getViewport();
			
			// find the size of the dialog (dialog needs to be showing to get the size)
			var mb;
			if(this.isShowing()){
				mb = dojo.html.getMarginBox(this.domNode);
			}else{
				dojo.html.setVisibility(this.domNode, false);
				dojo.html.show(this.domNode);
				mb = dojo.html.getMarginBox(this.domNode);
				dojo.html.hide(this.domNode);
				dojo.html.setVisibility(this.domNode, true);
			}
			
			var x = scroll_offset.x + (viewport_size.width - mb.width)/2;
			var y = scroll_offset.y + (viewport_size.height - mb.height)/2;
			with(this.domNode.style){
				left = x + "px";
				top = y + "px";
			}
		},

		_onKey: function(/*Event*/ evt){
			if (evt.key){
				// see if the key is for the dialog
				var node = evt.target;
				while (node != null){
					if (node == this.domNode){
						return; // yes, so just let it go
					}
					node = node.parentNode;
				}
				// this key is for the disabled document window
				if (evt.key != evt.KEY_TAB){ // allow tabbing into the dialog for a11y
					dojo.event.browser.stopEvent(evt);
				// opera won't tab to a div
				}else if (!dojo.render.html.opera){
					try {
						this.tabStart.focus(); 
					} catch(e){}
				}
			}
		},

		showModalDialog: function() {
			// summary
			//	call this function in show() of subclass before calling superclass.show()
			if (this.followScroll && !this._scrollConnected){
				this._scrollConnected = true;
				dojo.event.connect(window, "onscroll", this, "_onScroll");
			}
			dojo.event.connect(document.documentElement, "onkey", this, "_onKey");

			this.placeModalDialog();
			this.setBackgroundOpacity();
			this._sizeBackground();
			this._showBackground();
			this._fromTrap = true; 

			// set timeout to allow the browser to render dialog 
			setTimeout(dojo.lang.hitch(this, function(){
				try{
					this.tabStart.focus();
				}catch(e){}
			}), 50);

		},

		hideModalDialog: function(){
			// summary
			//	call this function in hide() of subclass

			// workaround for FF focus going into outer space
			if (this.focusElement) {
				dojo.byId(this.focusElement).focus(); 
				dojo.byId(this.focusElement).blur();
			}

			this.bg.style.display = "none";
			this.bg.style.width = this.bg.style.height = "1px";
            if(this.bgIframe.iframe){
				this.bgIframe.iframe.style.display = "none";
			}

			dojo.event.disconnect(document.documentElement, "onkey", this, "_onKey");
			if (this._scrollConnected){
				this._scrollConnected = false;
				dojo.event.disconnect(window, "onscroll", this, "_onScroll");
			}
		},

		_onScroll: function(){
			var scroll_offset = dojo.html.getScroll().offset;
			this.bg.style.top = scroll_offset.y + "px";
			this.bg.style.left = scroll_offset.x + "px";
			this.placeModalDialog();
		},

		checkSize: function() {
			if(this.isShowing()){
				this._sizeBackground();
				this.placeModalDialog();
				this.onResized();
			}
		},
		
		onBackgroundClick: function(){
			// summary
			//		Callback on background click.
			//		Clicking anywhere on the background will close the dialog, but only
			//		if the dialog doesn't have an explicit close button, and only if
			//		the dialog doesn't have a blockDuration.
			if(this.lifetime - this.timeRemaining >= this.blockDuration){ return; }
			this.hide();
		}
	});

dojo.widget.defineWidget(
	"dojo.widget.Dialog",
	[dojo.widget.ContentPane, dojo.widget.ModalDialogBase],
	{
		// summary
		//	Pops up a modal dialog window, blocking access to the screen and also graying out the screen
		//	Dialog is extended from ContentPane so it supports all the same parameters (href, etc.)

		templatePath: dojo.uri.dojoUri("src/widget/templates/Dialog.html"),

		// blockDuration: Integer
		//	number of seconds for which the user cannot dismiss the dialog
		blockDuration: 0,
		
		// lifetime: Integer
		//	if set, this controls the number of seconds the dialog will be displayed before automatically disappearing
		lifetime: 0,

		// closeNode: String
		//	Id of button or other dom node to click to close this dialog
		closeNode: "",

		postMixInProperties: function(){
			dojo.widget.Dialog.superclass.postMixInProperties.apply(this, arguments);
			if(this.closeNode){
				this.setCloseControl(this.closeNode);
			}
		},

		postCreate: function(){
			dojo.widget.Dialog.superclass.postCreate.apply(this, arguments);
			dojo.widget.ModalDialogBase.prototype.postCreate.apply(this, arguments);
		},

		show: function() {
			if(this.lifetime){
				this.timeRemaining = this.lifetime;
				if(this.timerNode){
					this.timerNode.innerHTML = Math.ceil(this.timeRemaining/1000);
				}
				if(this.blockDuration && this.closeNode){
					if(this.lifetime > this.blockDuration){
						this.closeNode.style.visibility = "hidden";
					}else{
						this.closeNode.style.display = "none";
					}
				}
				if (this.timer) {
					clearInterval(this.timer);
				}
				this.timer = setInterval(dojo.lang.hitch(this, "_onTick"), 100);
			}

			this.showModalDialog();
			dojo.widget.Dialog.superclass.show.call(this);
		},

		onLoad: function(){
			// when href is specified we need to reposition
			// the dialog after the data is loaded
			this.placeModalDialog();
			dojo.widget.Dialog.superclass.onLoad.call(this);
		},
		
		fillInTemplate: function(){
			// dojo.event.connect(this.domNode, "onclick", this, "killEvent");
		},

		hide: function(){
			this.hideModalDialog();
			dojo.widget.Dialog.superclass.hide.call(this);

			if(this.timer){
				clearInterval(this.timer);
			}
		},
		
		setTimerNode: function(node){
			// summary
			//	specify into which node to write the remaining # of seconds
			// TODO: make this a parameter too
			this.timerNode = node;
		},

		setCloseControl: function(/*String|DomNode*/ node) {
			// summary
			//	Specify which node is the close button for this dialog.
			//	If no close node is specified then clicking anywhere on the screen will close the dialog.
			this.closeNode = dojo.byId(node);
			dojo.event.connect(this.closeNode, "onclick", this, "hide");
		},

		setShowControl: function(/*String|DomNode*/ node) {
			// summary
			//	when specified node is clicked, show this dialog
			// TODO: make this a parameter too
			node = dojo.byId(node);
			dojo.event.connect(node, "onclick", this, "show");
		},

		_onTick: function(){
			// summary
			//	callback every second that the timer clicks
			if(this.timer){
				this.timeRemaining -= 100;
				if(this.lifetime - this.timeRemaining >= this.blockDuration){
					// TODO: this block of code is executing over and over again, rather than just once
					if(this.closeNode){
						this.closeNode.style.visibility = "visible";
					}
				}
				if(!this.timeRemaining){
					clearInterval(this.timer);
					this.hide();
				}else if(this.timerNode){
					this.timerNode.innerHTML = Math.ceil(this.timeRemaining/1000);
				}
			}
		}
	}
);
