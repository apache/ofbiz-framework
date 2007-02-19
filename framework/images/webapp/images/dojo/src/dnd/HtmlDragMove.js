/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.dnd.HtmlDragMove");
dojo.require("dojo.dnd.*");

dojo.declare("dojo.dnd.HtmlDragMoveSource", dojo.dnd.HtmlDragSource, {
	onDragStart: function(){
		var dragObj =  new dojo.dnd.HtmlDragMoveObject(this.dragObject, this.type);
		if (this.constrainToContainer) {
			dragObj.constrainTo(this.constrainingContainer);
		}
		return dragObj;
	},
	/*
	 * see dojo.dnd.HtmlDragSource.onSelected
	 */
	onSelected: function() {
		for (var i=0; i<this.dragObjects.length; i++) {
			dojo.dnd.dragManager.selectedSources.push(new dojo.dnd.HtmlDragMoveSource(this.dragObjects[i]));
		}
	}
});

dojo.declare("dojo.dnd.HtmlDragMoveObject", dojo.dnd.HtmlDragObject, {
	onDragStart: function(e){
		dojo.html.clearSelection();

		this.dragClone = this.domNode;

		// Record drag start position, where "position" is simply the top/left style values for
		// the node (the meaning of top/left is dependent on whether node is position:absolute or
		// position:relative, and also on the container).
		// Not sure if we should support moving nodes that aren't position:absolute,
		// but supporting it for now
		if(dojo.html.getComputedStyle(this.domNode, 'position') != 'absolute'){
			this.domNode.style.position = "relative";
		}
		var left = parseInt(dojo.html.getComputedStyle(this.domNode, 'left'));
		var top = parseInt(dojo.html.getComputedStyle(this.domNode, 'top'));
		this.dragStartPosition = {
			x: isNaN(left) ? 0 : left,
			y: isNaN(top) ? 0 : top
		};

		this.scrollOffset = dojo.html.getScroll().offset;

		// used to convert mouse position into top/left value for node
		this.dragOffset = {y: this.dragStartPosition.y - e.pageY,
			x: this.dragStartPosition.x - e.pageX};

		// since the DragObject's position is relative to the containing block, for our purposes
		// the containing block's position is just (0,0)
		this.containingBlockPosition = {x:0, y:0};

		if (this.constrainToContainer) {
			this.constraints = this.getConstraints();
		}

		// shortly the browser will fire an onClick() event,
		// but since this was really a drag, just squelch it
		dojo.event.connect(this.domNode, "onclick", this, "_squelchOnClick");
	},

	onDragEnd: function(e){
	},

	setAbsolutePosition: function(x, y){
		// summary: Set the top & left style attributes of the drag node (TODO: function is poorly named)
		if(!this.disableY) { this.domNode.style.top = y + "px"; }
		if(!this.disableX) { this.domNode.style.left = x + "px"; }
	},

	_squelchOnClick: function(e){
		// summary
		//	this function is called to squelch this onClick() event because
		//	it's the result of a drag (ie, it's not a real click)

		dojo.event.browser.stopEvent(e);
		dojo.event.disconnect(this.domNode, "onclick", this, "_squelchOnClick");
	}
});
