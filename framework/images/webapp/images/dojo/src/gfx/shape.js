/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.gfx.shape");

dojo.require("dojo.lang.declare");

dojo.require("dojo.gfx.common");

dojo.declare("dojo.gfx.Shape", null, {
	// summary: a Shape object, which knows how to apply 
	// graphical attributes and transformations

	initializer: function(){
	
		// rawNode: Node: underlying node
		this.rawNode = null;
		
		// shape: Object: an abstract shape object
		//	(see dojo.gfx.defaultPath,
		//	dojo.gfx.defaultPolyline,
		//	dojo.gfx.defaultRect,
		//	dojo.gfx.defaultEllipse,
		//	dojo.gfx.defaultCircle,
		//	dojo.gfx.defaultLine,
		//	or dojo.gfx.defaultImage)
		this.shape = null;
		
		// matrix: dojo.gfx.matrix.Matrix: a transformation matrix
		this.matrix = null;
		
		// fillStyle: Object: a fill object 
		//	(see dojo.gfx.defaultLinearGradient, 
		//	dojo.gfx.defaultRadialGradient, 
		//	dojo.gfx.defaultPattern, 
		//	or dojo.gfx.color.Color)
		this.fillStyle = null;
		
		// strokeStyle: Object: a stroke object 
		//	(see dojo.gfx.defaultStroke) 
		this.strokeStyle = null;
		
		// bbox: dojo.gfx.Rectangle: a bounding box of this shape
		//	(see dojo.gfx.defaultRect)
		this.bbox = null;
		
		// virtual group structure
		
		// parent: Object: a parent or null
		//	(see dojo.gfx.Surface,
		//	dojo.gfx.shape.VirtualGroup,
		//	or dojo.gfx.Group)
		this.parent = null;
		
		// parentMatrix: dojo.gfx.matrix.Matrix
		//	a transformation matrix inherited from the parent
		this.parentMatrix = null;
	},

	// trivial getters
	getNode: function(){
		// summary: returns the current DOM Node or null
		return this.rawNode; // Node
	},
	getShape: function(){
		// summary: returns the current shape object or null
		//	(see dojo.gfx.defaultPath,
		//	dojo.gfx.defaultPolyline,
		//	dojo.gfx.defaultRect,
		//	dojo.gfx.defaultEllipse,
		//	dojo.gfx.defaultCircle,
		//	dojo.gfx.defaultLine,
		//	or dojo.gfx.defaultImage)
		return this.shape; // Object
	},
	getTransform: function(){
		// summary: returns the current transformation matrix or null
		return this.matrix;	// dojo.gfx.matrix.Matrix
	},
	getFill: function(){
		// summary: returns the current fill object or null
		//	(see dojo.gfx.defaultLinearGradient, 
		//	dojo.gfx.defaultRadialGradient, 
		//	dojo.gfx.defaultPattern, 
		//	or dojo.gfx.color.Color)
		return this.fillStyle;	// Object
	},
	getStroke: function(){
		// summary: returns the current stroke object or null
		//	(see dojo.gfx.defaultStroke) 
		return this.strokeStyle;	// Object
	},
	getParent: function(){
		// summary: returns the parent or null
		//	(see dojo.gfx.Surface,
		//	dojo.gfx.shape.VirtualGroup,
		//	or dojo.gfx.Group)
		return this.parent;	// Object
	},
	getBoundingBox: function(){
		// summary: returns the bounding box or null
		//	(see dojo.gfx.defaultRect)
		return this.bbox;	// dojo.gfx.Rectangle
	},
	getEventSource: function(){
		// summary: returns a Node, which is used as 
		//	a source of events for this shape
		return this.rawNode;	// Node
	},
	
	// empty settings
	
	setShape: function(shape){
		// summary: sets a shape object
		//	(the default implementation simply ignores it)
		// shape: Object: a shape object
		//	(see dojo.gfx.defaultPath,
		//	dojo.gfx.defaultPolyline,
		//	dojo.gfx.defaultRect,
		//	dojo.gfx.defaultEllipse,
		//	dojo.gfx.defaultCircle,
		//	dojo.gfx.defaultLine,
		//	or dojo.gfx.defaultImage)
		return this;	// self
	},
	setFill: function(fill){
		// summary: sets a fill object
		//	(the default implementation simply ignores it)
		// fill: Object: a fill object
		//	(see dojo.gfx.defaultLinearGradient, 
		//	dojo.gfx.defaultRadialGradient, 
		//	dojo.gfx.defaultPattern, 
		//	or dojo.gfx.color.Color)
		return this;	// self
	},
	setStroke: function(stroke){
		// summary: sets a stroke object
		//	(the default implementation simply ignores it)
		// stroke: Object: a stroke object
		//	(see dojo.gfx.defaultStroke) 
		return this;	// self
	},
	
	// z-index
	
	moveToFront: function(){
		// summary: moves a shape to front of its parent's list of shapes
		//	(the default implementation does nothing)
		return this;	// self
	},
	moveToBack: function(){
		// summary: moves a shape to back of its parent's list of shapes
		//	(the default implementation does nothing)
		return this;
	},

	setTransform: function(matrix){
		// summary: sets a transformation matrix
		// matrix: dojo.gfx.matrix.Matrix: a matrix or a matrix-like object
		//	(see an argument of dojo.gfx.matrix.Matrix 
		//	constructor for a list of acceptable arguments)
		this.matrix = dojo.gfx.matrix.clone(matrix ? dojo.gfx.matrix.normalize(matrix) : dojo.gfx.identity, true);
		return this._applyTransform();	// self
	},
	
	// apply left & right transformation
	
	applyRightTransform: function(matrix){
		// summary: multiplies the existing matrix with an argument on right side
		//	(this.matrix * matrix)
		// matrix: dojo.gfx.matrix.Matrix: a matrix or a matrix-like object
		//	(see an argument of dojo.gfx.matrix.Matrix 
		//	constructor for a list of acceptable arguments)
		return matrix ? this.setTransform([this.matrix, matrix]) : this;	// self
	},
	applyLeftTransform: function(matrix){
		// summary: multiplies the existing matrix with an argument on left side
		//	(matrix * this.matrix)
		// matrix: dojo.gfx.matrix.Matrix: a matrix or a matrix-like object
		//	(see an argument of dojo.gfx.matrix.Matrix 
		//	constructor for a list of acceptable arguments)
		return matrix ? this.setTransform([matrix, this.matrix]) : this;	// self
	},

	applyTransform: function(matrix){
		// summary: a shortcut for dojo.gfx.Shape.applyRight
		// matrix: dojo.gfx.matrix.Matrix: a matrix or a matrix-like object
		//	(see an argument of dojo.gfx.matrix.Matrix 
		//	constructor for a list of acceptable arguments)
		return matrix ? this.setTransform([this.matrix, matrix]) : this;	// self
	},
	
	// virtual group methods
	
	remove: function(silently){
		// summary: removes the shape from its parent's list of shapes
		// silently: Boolean?: if true, do not redraw a picture yet
		if(this.parent){
			this.parent.remove(this, silently);
		}
		return this;	// self
	},
	_setParent: function(parent, matrix){
		// summary: sets a parent
		// parent: Object: a parent or null
		//	(see dojo.gfx.Surface,
		//	dojo.gfx.shape.VirtualGroup,
		//	or dojo.gfx.Group)
		// matrix: dojo.gfx.matrix.Matrix:
		//	a 2D matrix or a matrix-like object
		this.parent = parent;
		return this._updateParentMatrix(matrix);	// self
	},
	_updateParentMatrix: function(matrix){
		// summary: updates the parent matrix with new matrix
		// matrix: dojo.gfx.matrix.Matrix:
		//	a 2D matrix or a matrix-like object
		this.parentMatrix = matrix ? dojo.gfx.matrix.clone(matrix) : null;
		return this._applyTransform();	// self
	},
	_getRealMatrix: function(){
		// summary: returns the cumulative ("real") transformation matrix
		//	by combining the shape's matrix with its parent's matrix
		return this.parentMatrix ? new dojo.gfx.matrix.Matrix2D([this.parentMatrix, this.matrix]) : this.matrix;	// dojo.gfx.matrix.Matrix
	}
});

dojo.declare("dojo.gfx.shape.VirtualGroup", dojo.gfx.Shape, {
	// summary: a virtual group of shapes, which can be used 
	//	as a foundation for renderer-specific groups, or as a way 
	//	to logically group shapes (e.g, to propagate matricies)

	initializer: function() {
	
		// children: Array: a list of children
		this.children = [];
	},
	
	// group management
	
	add: function(shape){
		// summary: adds a shape to the list
		// shape: dojo.gfx.Shape: a shape
		var oldParent = shape.getParent();
		if(oldParent){
			oldParent.remove(shape, true);
		}
		this.children.push(shape);
		return shape._setParent(this, this._getRealMatrix());	// self
	},
	remove: function(shape, silently){
		// summary: removes a shape from the list
		// silently: Boolean?: if true, do not redraw a picture yet
		for(var i = 0; i < this.children.length; ++i){
			if(this.children[i] == shape){
				if(silently){
					// skip for now
				}else{
					shape._setParent(null, null);
				}
				this.children.splice(i, 1);
				break;
			}
		}
		return this;	// self
	},
	
	// apply transformation
	
	_applyTransform: function(){
		// summary: applies a transformation matrix to a group
		var matrix = this._getRealMatrix();
		for(var i = 0; i < this.children.length; ++i){
			this.children[i]._updateParentMatrix(matrix);
		}
		return this;	// self
	}
});

dojo.declare("dojo.gfx.shape.Rect", dojo.gfx.Shape, {
	// summary: a generic rectangle
	
	initializer: function(rawNode) {
		// summary: creates a rectangle
		// rawNode: Node: a DOM Node
		this.shape = dojo.lang.shallowCopy(dojo.gfx.defaultRect, true);
		this.attach(rawNode);
	},
	
	getBoundingBox: function(){
		// summary: returns the bounding box (its shape in this case)
		return this.shape;	// dojo.gfx.Rectangle
	}
});

dojo.declare("dojo.gfx.shape.Ellipse", dojo.gfx.Shape, {
	// summary: a generic ellipse
	
	initializer: function(rawNode) {
		// summary: creates an ellipse
		// rawNode: Node: a DOM Node
		this.shape = dojo.lang.shallowCopy(dojo.gfx.defaultEllipse, true);
		this.attach(rawNode);
	},
	getBoundingBox: function(){
		// summary: returns the bounding box
		if(!this.bbox){
			var shape = this.shape;
			this.bbox = {x: shape.cx - shape.rx, y: shape.cy - shape.ry, 
				width: 2 * shape.rx, height: 2 * shape.ry};
		}
		return this.bbox;	// dojo.gfx.Rectangle
	}
});

dojo.declare("dojo.gfx.shape.Circle", dojo.gfx.Shape, {
	// summary: a generic circle
	//	(this is a helper object, which is defined for convinience)

	initializer: function(rawNode) {
		// summary: creates a circle
		// rawNode: Node: a DOM Node
		this.shape = dojo.lang.shallowCopy(dojo.gfx.defaultCircle, true);
		this.attach(rawNode);
	},
	getBoundingBox: function(){
		// summary: returns the bounding box
		if(!this.bbox){
			var shape = this.shape;
			this.bbox = {x: shape.cx - shape.r, y: shape.cy - shape.r, 
				width: 2 * shape.r, height: 2 * shape.r};
		}
		return this.bbox;	// dojo.gfx.Rectangle
	}
});

dojo.declare("dojo.gfx.shape.Line", dojo.gfx.Shape, {
	// summary: a generic line
	//	(this is a helper object, which is defined for convinience)

	initializer: function(rawNode) {
		// summary: creates a line
		// rawNode: Node: a DOM Node
		this.shape = dojo.lang.shallowCopy(dojo.gfx.defaultLine, true);
		this.attach(rawNode);
	},
	getBoundingBox: function(){
		// summary: returns the bounding box
		if(!this.bbox){
			var shape = this.shape;
			this.bbox = {
				x:		Math.min(shape.x1, shape.x2),
				y:		Math.min(shape.y1, shape.y2),
				width:	Math.abs(shape.x2 - shape.x1),
				height:	Math.abs(shape.y2 - shape.y1)
			};
		}
		return this.bbox;	// dojo.gfx.Rectangle
	}
});

dojo.declare("dojo.gfx.shape.Polyline", dojo.gfx.Shape, {
	// summary: a generic polyline/polygon
	//	(this is a helper object, which is defined for convinience)

	initializer: function(rawNode) {
		// summary: creates a line
		// rawNode: Node: a DOM Node
		this.shape = dojo.lang.shallowCopy(dojo.gfx.defaultPolyline, true);
		this.attach(rawNode);
	},
	getBoundingBox: function(){
		// summary: returns the bounding box
		if(!this.bbox && this.shape.points.length){
			var p = this.shape.points;
			var l = p.length;
			var t = p[0];
			var bbox = {l: t.x, t: t.y, r: t.x, b: t.y};
			for(var i = 1; i < l; ++i){
				t = p[i];
				if(bbox.l > t.x) bbox.l = t.x;
				if(bbox.r < t.x) bbox.r = t.x;
				if(bbox.t > t.y) bbox.t = t.y;
				if(bbox.b < t.y) bbox.b = t.y;
			}
			this.bbox = {
				x:		bbox.l, 
				y:		bbox.t, 
				width:	bbox.r - bbox.l, 
				height:	bbox.b - bbox.t
			};
		}
		return this.bbox;	// dojo.gfx.Rectangle
	}
});

dojo.declare("dojo.gfx.shape.Image", dojo.gfx.Shape, {
	// summary: a generic image
	//	(this is a helper object, which is defined for convinience)

	initializer: function(rawNode) {
		// summary: creates an image
		// rawNode: Node: a DOM Node
		this.shape = dojo.lang.shallowCopy(dojo.gfx.defaultImage, true);
		this.attach(rawNode);
	},
	getBoundingBox: function(){
		// summary: returns the bounding box
		if(!this.bbox){
			var shape = this.shape;
			this.bbox = {
				x:		0,
				y:		0,
				width:	shape.width,
				height:	shape.height
			};
		}
		return this.bbox;	// dojo.gfx.Rectangle
	}
});
