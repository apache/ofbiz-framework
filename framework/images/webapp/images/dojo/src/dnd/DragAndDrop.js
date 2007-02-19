/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.require("dojo.lang.common");
dojo.require("dojo.lang.func");
dojo.require("dojo.lang.declare");
dojo.provide("dojo.dnd.DragAndDrop");

// summary:
//		Core "interfaces" for the participants in all DnD operations.
//		Subclasses implement all of the actions outlined by these APIs, with
//		most of the ones you probably care about being defined in
//		HtmlDragAndDrop.js, which will be automatically included should you
//		dojo.require("dojo.dnd.*");.
//
//		In addition to the various actor classes, a global manager will be
//		created/installed at dojo.dnd.dragManager. This manager object is of
//		type dojo.dnd.DragManager and will be replaced by environment-specific
//		managers.
//
// 		The 3 object types involved in any Drag and Drop operation are:
//			* DragSource
//				This is the item that can be selected for dragging. Drag
//				sources can have "types" to help mediate whether or not various
//				DropTargets will accept (or reject them). Most dragging actions
//				are handled by the DragObject which the DragSource generates
//				from its onDragStart method.
//			* DragObject
//				This, along with the manger, does most of the hard work of DnD.
//				Implementations may rely on DragObject instances to implement
//				"shadowing", "movement", or other kinds of DnD variations that
//				affect the visual representation of the drag operation.
//			* DropTarget
//				Represents some section of the screen that can accept drag
//				and drop events. DropTargets keep a list of accepted types
//				which is checked agains the types of the respective DragSource
//				objects that pass over it. DropTargets may implement behaviors
//				that respond to drop events to take application-level actions.

dojo.declare("dojo.dnd.DragSource", null, {
	// String: 
	//		what kind of drag source are we? Used to determine if we can be
	//		dropped on a given DropTarget
	type: "",

	onDragEnd: function(/*dojo.dnd.DragEvent*/evt){
		// summary:
		//		stub handler that is called when dragging finishes.
	},

	onDragStart: function(/*dojo.dnd.DragEvent*/evt){ // dojo.dnd.DragObject
		// summary:
		//		stub handler that is called when dragging starts. Subclasses
		//		should ensure that onDragStart *always* returns a
		//		dojo.dnd.DragObject instance.
	},

	onSelected: function(/*dojo.dnd.DragEvent*/evt){
		// summary:
		//		This function gets called when the DOM element was selected for
		//		dragging by the HtmlDragAndDropManager.
	},

	unregister: function(){
		// summary: remove this drag source from the manager
		dojo.dnd.dragManager.unregisterDragSource(this);
	},

	reregister: function(){
		// summary: add this drag source to the manager
		dojo.dnd.dragManager.registerDragSource(this);
	}
});

dojo.declare("dojo.dnd.DragObject", null, {
	// String: 
	//		what kind of drag object are we? Used to determine if we can be
	//		dropped on a given DropTarget
	type: "",
	
	register: function(){
		// summary: register this DragObject with the manager
		var dm = dojo.dnd.dragManager;
		if(dm["registerDragObject"]){ // side-effect prevention
			dm.registerDragObject(this);
		}
	},

	onDragStart: function(/*dojo.dnd.DragEvent*/evt){
		// summary:
		// 		over-ridden by subclasses. Gets called directly after being
		// 		created by the DragSource default action is to clone self as
		// 		icon
	},

	onDragMove: function(/*dojo.dnd.DragEvent*/evt){
		// summary: 
		//		Implemented by subclasses. Should change the UI for the drag
		//		icon i.e., "it moves itself"
	},

	onDragOver: function(/*dojo.dnd.DragEvent*/evt){
		// summary:
		//		stub handler that is called when the DragObject instance is
		//		"over" a DropTarget.
	},

	onDragOut: function(/*dojo.dnd.DragEvent*/evt){
		// summary:
		//		stub handler that is called when the DragObject instance leaves
		//		a DropTarget.
	},

	onDragEnd: function(/*dojo.dnd.DragEvent*/evt){
		// summary:
		//		stub handler that is called when dragging ends, either through
		//		dropping or cancelation.
	},

	// normal aliases
	onDragLeave: dojo.lang.forward("onDragOut"),
	onDragEnter: dojo.lang.forward("onDragOver"),

	// non-camel aliases
	ondragout: dojo.lang.forward("onDragOut"),
	ondragover: dojo.lang.forward("onDragOver")
});

dojo.declare("dojo.dnd.DropTarget", null, {

	acceptsType: function(/*String*/type){
		// summary: 
		//		determines whether or not this DropTarget will accept the given
		//		type. The default behavior is to consult this.acceptedTypes and
		//		if "*" is a member, to always accept the type.
		if(!dojo.lang.inArray(this.acceptedTypes, "*")){ // wildcard
			if(!dojo.lang.inArray(this.acceptedTypes, type)) { return false; } // Boolean
		}
		return true; // Boolean
	},

	accepts: function(/*Array*/dragObjects){
		// summary: 
		//		determines if we'll accept all members of the passed array of
		//		dojo.dnd.DragObject instances
		if(!dojo.lang.inArray(this.acceptedTypes, "*")){ // wildcard
			for (var i = 0; i < dragObjects.length; i++) {
				if (!dojo.lang.inArray(this.acceptedTypes,
					dragObjects[i].type)) { return false; } // Boolean
			}
		}
		return true; // Boolean
	},

	unregister: function(){
		// summary: remove from the drag manager
		dojo.dnd.dragManager.unregisterDropTarget(this);
	},

	onDragOver: function(/*dojo.dnd.DragEvent*/evt){
		// summary:
		//		stub handler that is called when DragObject instances are
		//		"over" this DropTarget.
	},

	onDragOut: function(/*dojo.dnd.DragEvent*/evt){
		// summary:
		//		stub handler that is called when DragObject instances are
		//		"leave" this DropTarget.
	},

	onDragMove: function(/*dojo.dnd.DragEvent*/evt){
		// summary:
		//		stub handler that is called when DragObject instances are
		//		moved across this DropTarget. May fire many times in the course
		//		of the drag operation but will end after onDragOut
	},

	onDropStart: function(/*dojo.dnd.DragEvent*/evt){ // Boolean
		// summary:
		//		stub handler that is called when DragObject instances are
		//		dropped on this target. If true is returned from onDropStart,
		//		dropping proceeds, otherwise it's cancled.
	},

	onDrop: function(/*dojo.dnd.DragEvent*/evt){
		// summary: we're getting dropped on!
	},

	onDropEnd: function(){
		// summary: dropping is over
	}
}, function(){
	this.acceptedTypes = [];
});

// NOTE: this interface is defined here for the convenience of the DragManager
// implementor. It is expected that in most cases it will be satisfied by
// extending a native event (DOM event in HTML and SVG).
dojo.dnd.DragEvent = function(){
	this.dragSource = null;
	this.dragObject = null;
	this.target = null;
	this.eventStatus = "success";
	//
	// can be one of:
	//	[	"dropSuccess", "dropFailure", "dragMove",
	//		"dragStart", "dragEnter", "dragLeave"]
	//
}
/*
 *	The DragManager handles listening for low-level events and dispatching
 *	them to higher-level primitives like drag sources and drop targets. In
 *	order to do this, it must keep a list of the items.
 */
dojo.declare("dojo.dnd.DragManager", null, {
	// Array: an array of currently selected DragSource objects
	selectedSources: [],
	// Array: all DragObjects we know about
	dragObjects: [],
	// Array: all DragSources we know about
	dragSources: [],
	registerDragSource: function(/*dojo.dnd.DragSource*/ source){
		// summary: called by DragSource class constructor
	},
	// Array: all DropTargets we know about
	dropTargets: [],
	registerDropTarget: function(/*dojo.dnd.DropTarget*/ target){
		// summary: called by DropTarget class constructor
	},
	// dojo.dnd.DropTarget:
	//		what was the last DropTarget instance we left in the drag phase?
	lastDragTarget: null,
	// dojo.dnd.DropTarget:
	//		the DropTarget the mouse is currently over
	currentDragTarget: null,
	onKeyDown: function(){
		// summary: generic handler called by low-level events
	},
	onMouseOut: function(){
		// summary: generic handler called by low-level events
	},
	onMouseMove: function(){
		// summary: generic handler called by low-level events
	},
	onMouseUp: function(){
		// summary: generic handler called by low-level events
	}
});

// NOTE: despite the existance of the DragManager class, there will be a
// singleton drag manager provided by the renderer-specific D&D support code.
// It is therefore sane for us to assign instance variables to the DragManager
// prototype

// The renderer-specific file will define the following object:
// dojo.dnd.dragManager = null;
