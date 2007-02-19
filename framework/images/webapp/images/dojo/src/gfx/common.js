/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.gfx.common");

dojo.require("dojo.gfx.color");
dojo.require("dojo.lang.declare");
dojo.require("dojo.lang.extras");
dojo.require("dojo.dom");

dojo.lang.mixin(dojo.gfx, {
	// summary: defines constants, prototypes, and utility functions
	
	// default shapes, which is used to fill in missing parameters
	defaultPath:     {type: "path",     path: ""},
	defaultPolyline: {type: "polyline", points: []},
	defaultRect:     {type: "rect",     x: 0, y: 0, width: 100, height: 100, r: 0},
	defaultEllipse:  {type: "ellipse",  cx: 0, cy: 0, rx: 200, ry: 100},
	defaultCircle:   {type: "circle",   cx: 0, cy: 0, r: 100},
	defaultLine:     {type: "line",     x1: 0, y1: 0, x2: 100, y2: 100},
	defaultImage:    {type: "image",    width: 0, height: 0, src: ""},

	// default geometric attributes (a stroke, and fills)
	defaultStroke: {color: "black", width: 1, cap: "butt", join: 4},
	defaultLinearGradient: {type: "linear", x1: 0, y1: 0, x2: 100, y2: 100, 
		colors: [{offset: 0, color: "black"}, {offset: 1, color: "white"}]},
	defaultRadialGradient: {type: "radial", cx: 0, cy: 0, r: 100, 
		colors: [{offset: 0, color: "black"}, {offset: 1, color: "white"}]},
	defaultPattern: {type: "pattern", x: 0, y: 0, width: 0, height: 0, src: ""},

	normalizeColor: function(/*Color*/ color){
		// summary: converts any legal color representation to normalized dojo.gfx.color.Color object
		return (color instanceof dojo.gfx.color.Color) ? color : new dojo.gfx.color.Color(color); // dojo.gfx.color.Color
	},
	normalizeParameters: function(existed, update){
		// summary: updates an existing object with properties from an "update" object
		// existed: Object: the "target" object to be updated
		// update:  Object: the "update" object, whose properties will be used to update the existed object
		if(update){
			var empty = {};
			for(var x in existed){
				if(x in update && !(x in empty)){
					existed[x] = update[x];
				}
			}
		}
		return existed;	// Object
	},
	makeParameters: function(defaults, update){
		// summary: copies the original object, and all copied properties from the "update" object
		// defaults: Object: the object to be cloned before updating
		// update:   Object: the object, which properties are to be cloned during updating
		if(!update) return dojo.lang.shallowCopy(defaults, true);
		var result = {};
		for(var i in defaults){
			if(!(i in result)){
				result[i] = dojo.lang.shallowCopy((i in update) ? update[i] : defaults[i], true);
			}
		}
		return result; // Object
	},
	formatNumber: function(x, addSpace){
		// summary: converts a number to a string using a fixed notation
		// x:			Number:		number to be converted
		// addSpace:	Boolean?:	if it is true, add a space before a positive number
		var val = x.toString();
		if(val.indexOf("e") >= 0){
			val = x.toFixed(4);
		}else{
			var point = val.indexOf(".");
			if(point >= 0 && val.length - point > 5){
				val = x.toFixed(4);
			}
		}
		if(x < 0){
			return val; // String
		}
		return addSpace ? " " + val : val; // String
	},
	
	// a constant used to split a SVG/VML path into primitive components
	pathRegExp: /([A-Za-z]+)|(\d+(\.\d+)?)|(\.\d+)|(-\d+(\.\d+)?)|(-\.\d+)/g
});

dojo.declare("dojo.gfx.Surface", null, {
	// summary: a surface object to be used for drawings
	
	initializer: function(){
		// summary: a constructor
		
		// underlying node
		this.rawNode = null;
	},
	getEventSource: function(){
		// summary: returns a node, which can be used to attach event listeners
		
		return this.rawNode; // Node
	}
});

dojo.declare("dojo.gfx.Point", null, {
	// summary: a hypothetical 2D point to be used for drawings - {x, y}
	// description: This object is defined for documentation purposes.
	//	You should use a naked object instead: {x: 1, y: 2}.
});

dojo.declare("dojo.gfx.Rectangle", null, {
	// summary: a hypothetical rectangle - {x, y, width, height}
	// description: This object is defined for documentation purposes.
	//	You should use a naked object instead: {x: 1, y: 2, width: 100, height: 200}.
});
