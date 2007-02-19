/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.Textbox");

dojo.require("dojo.widget.*");
dojo.require("dojo.widget.HtmlWidget");
dojo.require("dojo.widget.Manager");
dojo.require("dojo.widget.Parse");
dojo.require("dojo.xml.Parse");
dojo.require("dojo.lang.array");
dojo.require("dojo.lang.common");

dojo.require("dojo.i18n.common");
dojo.requireLocalization("dojo.widget", "validate", null, "fr,ja,zh-cn,ROOT");

dojo.widget.defineWidget(
	"dojo.widget.Textbox",
	dojo.widget.HtmlWidget,
	{
		// summary:
		//		A generic textbox field.
		//		Serves as a base class to derive more specialized functionality in subclasses.

		// className: String
		//		The textbox class attribute.
		className: "",

		//	name: String
		//		The textbox name attribute.
		name: "",

		// value: String
		//		The textbox value attribute.
		value: "",

		// type: String
		//		Basic input tag type declaration.
		type: "",

		//	trim: Boolean
		//		Removes leading and trailing whitespace if true.  Default is false.
		trim: false,

		//	uppercase: Boolean
		//		Converts all characters to uppercase if true.  Default is false.
		uppercase: false,

		//	lowercase: Boolean
		//		Converts all characters to lowercase if true.  Default is false.
		lowercase: false,

		//	ucFirst: Boolean
		//		Converts the first character of each word to uppercase if true.
		ucFirst: false,

		//	digit: Boolean
		//		Removes all characters that are not digits if true.  Default is false.
		digit: false,
		
		// htmlfloat: String
		//		"none", "left", or "right".  CSS float attribute applied to generated dom node.
		htmlfloat: "none",

		templatePath: dojo.uri.dojoUri("src/widget/templates/Textbox.html"),
	
		// textbox DomNode:
		//		our DOM node
		textbox: null,

		fillInTemplate: function() {
			// assign value programatically in case it has a quote in it
			this.textbox.value = this.value;
		},

		filter: function() {
			// summary: Apply various filters to textbox value
			if (this.trim) {
				this.textbox.value = this.textbox.value.replace(/(^\s*|\s*$)/g, "");
			} 
			if (this.uppercase) {
				this.textbox.value = this.textbox.value.toUpperCase();
			} 
			if (this.lowercase) {
				this.textbox.value = this.textbox.value.toLowerCase();
			} 
			if (this.ucFirst) {
				this.textbox.value = this.textbox.value.replace(/\b\w+\b/g, 
					function(word) { return word.substring(0,1).toUpperCase() + word.substring(1).toLowerCase(); });
			} 
			if (this.digit) {
				this.textbox.value = this.textbox.value.replace(/\D/g, "");
			} 
		},
	
		// event handlers, you can over-ride these in your own subclasses
		onfocus: function() {},
		onblur: function() { this.filter(); },
	
		// All functions below are called by create from dojo.widget.Widget
		mixInProperties: function(localProperties, frag) {
			dojo.widget.Textbox.superclass.mixInProperties.apply(this, arguments);
			if ( localProperties["class"] ) { 
				this.className = localProperties["class"];
			}
		}
	}
);
