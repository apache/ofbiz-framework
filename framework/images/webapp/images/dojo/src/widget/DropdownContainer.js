/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.DropdownContainer");
dojo.require("dojo.widget.*");
dojo.require("dojo.widget.HtmlWidget");
dojo.require("dojo.widget.PopupContainer");
dojo.require("dojo.event.*");
dojo.require("dojo.html.layout");
dojo.require("dojo.html.display");
dojo.require("dojo.html.iframe");
dojo.require("dojo.html.util");

dojo.widget.defineWidget(
	"dojo.widget.DropdownContainer",
	dojo.widget.HtmlWidget,
	{
		// summary:
		//		provides an input box and a button for a dropdown.
		//		In subclass, the dropdown can be specified.

		// inputWidth: String: width of the input box
		inputWidth: "7em",

		// id: String: id of this widget
		id: "",

		// inputId: String: id of the input box
		inputId: "",

		// inputName: String: name of the input box
		inputName: "",

		// iconURL: dojo.uri.Uri: icon for the dropdown button
		iconURL: dojo.uri.dojoUri("src/widget/templates/images/combo_box_arrow.png"),

		// copyClass:
		//		should we use the class properties on the source node instead
		//		of our own styles?
		copyClasses: false,

		// iconAlt: dojo.uri.Uri: alt text for the dropdown button icon
		iconAlt: "",

		// containerToggle: String: toggle property of the dropdown
		containerToggle: "plain",

		// containerToggleDuration: Integer: toggle duration property of the dropdown
		containerToggleDuration: 150,

		templateString: '<span style="white-space:nowrap"><input type="hidden" name="" value="" dojoAttachPoint="valueNode" /><input name="" type="text" value="" style="vertical-align:middle;" dojoAttachPoint="inputNode" autocomplete="off" /> <img src="${this.iconURL}" alt="${this.iconAlt}" dojoAttachEvent="onclick:onIconClick" dojoAttachPoint="buttonNode" style="vertical-align:middle; cursor:pointer; cursor:hand" /></span>',
		templateCssPath: "",
		isContainer: true,

		attachTemplateNodes: function(){
			// summary: use attachTemplateNodes to specify containerNode, as fillInTemplate is too late for this
			dojo.widget.DropdownContainer.superclass.attachTemplateNodes.apply(this, arguments);
			this.popup = dojo.widget.createWidget("PopupContainer", {toggle: this.containerToggle, toggleDuration: this.containerToggleDuration});
			this.containerNode = this.popup.domNode;
		},

		fillInTemplate: function(args, frag){
			this.domNode.appendChild(this.popup.domNode);
			if(this.id) { this.domNode.id = this.id; }
			if(this.inputId){ this.inputNode.id = this.inputId; }
			if(this.inputName){ this.inputNode.name = this.inputName; }
			this.inputNode.style.width = this.inputWidth;
			this.inputNode.disabled = this.disabled;

			if(this.copyClasses){
				this.inputNode.style = "";
				this.inputNode.className = this.getFragNodeRef(frag).className;
			}


			dojo.event.connect(this.inputNode, "onchange", this, "onInputChange");
		},

		onIconClick: function(/*Event*/ evt){
			if(this.disabled) return;
			if(!this.popup.isShowingNow){
				this.popup.open(this.inputNode, this, this.buttonNode);
			}else{
				this.popup.close();
			}
		},

		hideContainer: function(){
			// summary: hide the dropdown
			if(this.popup.isShowingNow){
				this.popup.close();
			}
		},

		onInputChange: function(){
			// summary: signal for changes in the input box
		},
		
		enable: function() {
			// summary: enable this widget to accept user input
			this.inputNode.disabled = false;
			dojo.widget.DropdownContainer.superclass.enable.apply(this, arguments);
		},
		
		disable: function() {
			// summary: lock this widget so that the user can't change the value
			this.inputNode.disabled = true;
			dojo.widget.DropdownContainer.superclass.disable.apply(this, arguments);
		}
	}
);
