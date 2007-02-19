/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.TitlePane");
dojo.require("dojo.widget.*");
dojo.require("dojo.widget.ContentPane");
dojo.require("dojo.html.style");
dojo.require("dojo.lfx.*");

dojo.widget.defineWidget(
	"dojo.widget.TitlePane",
	dojo.widget.ContentPane,
{
	// summary
	//		A pane with a title on top, that can be opened or collapsed.
	
	// labelNodeClass: String
	//		CSS class name for <div> containing title of the pane.
	labelNodeClass: "",

	// containerNodeClass: String
	//		CSS class name for <div> containing content of the pane.
	containerNodeClass: "",

	// label: String
	//		Title of the pane
	label: "",
	
	// open: Boolean
	//		Whether pane is opened or closed.
	open: true,

	templatePath: dojo.uri.dojoUri("src/widget/templates/TitlePane.html"),

	postCreate: function() {
		if (this.label) {
			this.labelNode.appendChild(document.createTextNode(this.label));
		}

		if (this.labelNodeClass) {
			dojo.html.addClass(this.labelNode, this.labelNodeClass);
		}	

		if (this.containerNodeClass) {
			dojo.html.addClass(this.containerNode, this.containerNodeClass);
		}	

		if (!this.open) {
			dojo.html.hide(this.containerNode);
		}
		dojo.widget.TitlePane.superclass.postCreate.apply(this, arguments);
	},

	onLabelClick: function() {
		// summary: callback when label is clicked
		if (this.open) {
			dojo.lfx.wipeOut(this.containerNode, 250).play();
			this.open=false;
		} else {
			dojo.lfx.wipeIn(this.containerNode, 250).play();
			this.open=true;
		}
	},

	setLabel: function(/*String*/ label) {
		// summary: sets the text of the label
		this.labelNode.innerHTML=label;
	}
});
