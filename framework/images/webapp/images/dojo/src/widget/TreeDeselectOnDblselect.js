/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/


dojo.provide("dojo.widget.TreeDeselectOnDblselect");

dojo.require("dojo.widget.HtmlWidget");
dojo.require("dojo.widget.TreeSelectorV3");

dojo.deprecated("Does anyone still need this extension? (TreeDeselectOnDblselect)");
// selector extension to emphasize node

dojo.widget.defineWidget(
	"dojo.widget.TreeDeselectOnDblselect",
	[dojo.widget.HtmlWidget],
{
	selector: "",
	
	initialize: function() {
		this.selector = dojo.widget.byId(this.selector);
		//dojo.debug("OK "+this.selector);
		dojo.event.topic.subscribe(this.selector.eventNames.dblselect, this, "onDblselect");		
	},

	onDblselect: function(message) {
		//dojo.debug("happen "+this.selector);
		//dojo.debug(message.node);
		this.selector.deselect(message.node);
	}
});
