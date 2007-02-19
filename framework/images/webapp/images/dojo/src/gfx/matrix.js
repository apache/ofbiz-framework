/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.gfx.matrix");

dojo.require("dojo.lang.common");
dojo.require("dojo.math.*");

dojo.gfx.matrix.Matrix2D = function(arg){
	// summary: a 2D matrix object
	// description: Normalizes a 2D matrix-like object. If arrays is passed, 
	//		all objects of the array are normalized and multiplied sequentially.
	// arg: Object
	//		a 2D matrix-like object, or an array of such objects
	if(arg){
		if(arg instanceof Array){
			if(arg.length > 0){
				var m = dojo.gfx.matrix.normalize(arg[0]);
				// combine matrices
				for(var i = 1; i < arg.length; ++i){
					var l = m;
					var r = dojo.gfx.matrix.normalize(arg[i]);
					m = new dojo.gfx.matrix.Matrix2D();
					m.xx = l.xx * r.xx + l.xy * r.yx;
					m.xy = l.xx * r.xy + l.xy * r.yy;
					m.yx = l.yx * r.xx + l.yy * r.yx;
					m.yy = l.yx * r.xy + l.yy * r.yy;
					m.dx = l.xx * r.dx + l.xy * r.dy + l.dx;
					m.dy = l.yx * r.dx + l.yy * r.dy + l.dy;
				}
				dojo.mixin(this, m);
			}
		}else{
			dojo.mixin(this, arg);
		}
	}
};

// the default (identity) matrix, which is used to fill in missing values
dojo.extend(dojo.gfx.matrix.Matrix2D, {xx: 1, xy: 0, yx: 0, yy: 1, dx: 0, dy: 0});

dojo.mixin(dojo.gfx.matrix, {
	// summary: class constants, and methods of dojo.gfx.matrix
	
	// matrix constants
	
	// identity: dojo.gfx.matrix.Matrix2D
	//		an identity matrix constant: identity * (x, y) == (x, y)
	identity: new dojo.gfx.matrix.Matrix2D(),
	
	// flipX: dojo.gfx.matrix.Matrix2D
	//		a matrix, which reflects points at x = 0 line: flipX * (x, y) == (-x, y)
	flipX:    new dojo.gfx.matrix.Matrix2D({xx: -1}),
	
	// flipY: dojo.gfx.matrix.Matrix2D
	//		a matrix, which reflects points at y = 0 line: flipY * (x, y) == (x, -y)
	flipY:    new dojo.gfx.matrix.Matrix2D({yy: -1}),
	
	// flipXY: dojo.gfx.matrix.Matrix2D
	//		a matrix, which reflects points at the origin of coordinates: flipXY * (x, y) == (-x, -y)
	flipXY:   new dojo.gfx.matrix.Matrix2D({xx: -1, yy: -1}),
	
	// matrix creators
	
	translate: function(a, b){
		// summary: forms a translation matrix
		// description: The resulting matrix is used to translate (move) points by specified offsets.
		// a: Number: an x coordinate value
		// b: Number: a y coordinate value
		if(arguments.length > 1){
			return new dojo.gfx.matrix.Matrix2D({dx: a, dy: b}); // dojo.gfx.matrix.Matrix2D
		}
		// branch
		// a: dojo.gfx.Point: a point-like object, which specifies offsets for both dimensions
		// b: null
		return new dojo.gfx.matrix.Matrix2D({dx: a.x, dy: a.y}); // dojo.gfx.matrix.Matrix2D
	},
	scale: function(a, b){
		// summary: forms a scaling matrix
		// description: The resulting matrix is used to scale (magnify) points by specified offsets.
		// a: Number: a scaling factor used for the x coordinate
		// b: Number: a scaling factor used for the y coordinate
		if(arguments.length > 1){
			return new dojo.gfx.matrix.Matrix2D({xx: a, yy: b}); // dojo.gfx.matrix.Matrix2D
		}
		if(typeof a == "number"){
			// branch
			// a: Number: a uniform scaling factor used for the both coordinates
			// b: null
			return new dojo.gfx.matrix.Matrix2D({xx: a, yy: a}); // dojo.gfx.matrix.Matrix2D
		}
		// branch
		// a: dojo.gfx.Point: a point-like object, which specifies scale factors for both dimensions
		// b: null
		return new dojo.gfx.matrix.Matrix2D({xx: a.x, yy: a.y}); // dojo.gfx.matrix.Matrix2D
	},
	rotate: function(angle){
		// summary: forms a rotating matrix
		// description: The resulting matrix is used to rotate points 
		//		around the origin of coordinates (0, 0) by specified angle.
		// angle: Number: an angle of rotation in radians (>0 for CCW)
		var c = Math.cos(angle);
		var s = Math.sin(angle);
		return new dojo.gfx.matrix.Matrix2D({xx: c, xy: s, yx: -s, yy: c}); // dojo.gfx.matrix.Matrix2D
	},
	rotateg: function(degree){
		// summary: forms a rotating matrix
		// description: The resulting matrix is used to rotate points
		//		around the origin of coordinates (0, 0) by specified degree.
		//		See dojo.gfx.matrix.rotate() for comparison.
		// degree: Number: an angle of rotation in degrees (>0 for CCW)
		return dojo.gfx.matrix.rotate(dojo.math.degToRad(degree)); // dojo.gfx.matrix.Matrix2D
	},
	skewX: function(angle) {
		// summary: forms an x skewing matrix
		// description: The resulting matrix is used to skew points in the x dimension
		//		around the origin of coordinates (0, 0) by specified angle.
		// angle: Number: an skewing angle in radians
		return new dojo.gfx.matrix.Matrix2D({xy: Math.tan(angle)}); // dojo.gfx.matrix.Matrix2D
	},
	skewXg: function(degree){
		// summary: forms an x skewing matrix
		// description: The resulting matrix is used to skew points in the x dimension
		//		around the origin of coordinates (0, 0) by specified degree.
		//		See dojo.gfx.matrix.skewX() for comparison.
		// degree: Number: an skewing angle in degrees
		return dojo.gfx.matrix.skewX(dojo.math.degToRad(degree)); // dojo.gfx.matrix.Matrix2D
	},
	skewY: function(angle){
		// summary: forms a y skewing matrix
		// description: The resulting matrix is used to skew points in the y dimension
		//		around the origin of coordinates (0, 0) by specified angle.
		// angle: Number: an skewing angle in radians
		return new dojo.gfx.matrix.Matrix2D({yx: -Math.tan(angle)}); // dojo.gfx.matrix.Matrix2D
	},
	skewYg: function(degree){
		// summary: forms a y skewing matrix
		// description: The resulting matrix is used to skew points in the y dimension
		//		around the origin of coordinates (0, 0) by specified degree.
		//		See dojo.gfx.matrix.skewY() for comparison.
		// degree: Number: an skewing angle in degrees
		return dojo.gfx.matrix.skewY(dojo.math.degToRad(degree)); // dojo.gfx.matrix.Matrix2D
	},
	
	// ensure matrix 2D conformance
	normalize: function(matrix){
		// summary: converts an object to a matrix, if necessary
		// description: Converts any 2D matrix-like object or an array of
		//		such objects to a valid dojo.gfx.matrix.Matrix2D object.
		// matrix: Object: an object, which is converted to a matrix, if necessary
		return (matrix instanceof dojo.gfx.matrix.Matrix2D) ? matrix : new dojo.gfx.matrix.Matrix2D(matrix); // dojo.gfx.matrix.Matrix2D
	},
	
	// common operations
	
	clone: function(matrix){
		// summary: creates a copy of a 2D matrix
		// matrix: dojo.gfx.matrix.Matrix2D: a 2D matrix-like object to be cloned
		var obj = new dojo.gfx.matrix.Matrix2D();
		for(var i in matrix){
			if(typeof(matrix[i]) == "number" && typeof(obj[i]) == "number" && obj[i] != matrix[i]) obj[i] = matrix[i];
		}
		return obj; // dojo.gfx.matrix.Matrix2D
	},
	invert: function(matrix){
		// summary: inverts a 2D matrix
		// matrix: dojo.gfx.matrix.Matrix2D: a 2D matrix-like object to be inverted
		var m = dojo.gfx.matrix.normalize(matrix);
		var D = m.xx * m.yy - m.xy * m.yx;
		var M = new dojo.gfx.matrix.Matrix2D({
			xx: m.yy/D, xy: -m.xy/D, 
			yx: -m.yx/D, yy: m.xx/D, 
			dx: (m.yx * m.dy - m.yy * m.dx) / D, 
			dy: (m.xy * m.dx - m.xx * m.dy) / D
		});
		return M; // dojo.gfx.matrix.Matrix2D
	},
	_multiplyPoint: function(m, x, y){
		// summary: applies a matrix to a point
		// matrix: dojo.gfx.matrix.Matrix2D: a 2D matrix object to be applied
		// x: Number: an x coordinate of a point
		// y: Number: a y coordinate of a point
		return {x: m.xx * x + m.xy * y + m.dx, y: m.yx * x + m.yy * y + m.dy}; // dojo.gfx.Point
	},
	multiplyPoint: function(matrix, /* Number||Point */ a, /* Number, optional */ b){
		// summary: applies a matrix to a point
		// matrix: dojo.gfx.matrix.Matrix2D: a 2D matrix object to be applied
		// a: Number: an x coordinate of a point
		// b: Number: a y coordinate of a point
		var m = dojo.gfx.matrix.normalize(matrix);
		if(typeof a == "number" && typeof b == "number"){
			return dojo.gfx.matrix._multiplyPoint(m, a, b); // dojo.gfx.Point
		}
		// branch
		// matrix: dojo.gfx.matrix.Matrix2D: a 2D matrix object to be applied
		// a: dojo.gfx.Point: a point
		// b: null
		return dojo.gfx.matrix._multiplyPoint(m, a.x, a.y); // dojo.gfx.Point
	},
	multiply: function(matrix){
		// summary: combines matrices by multiplying them sequentially in the given order
		// matrix: dojo.gfx.matrix.Matrix2D...: a 2D matrix-like object, 
		//		all subsequent arguments are matrix-like objects too
		var m = dojo.gfx.matrix.normalize(matrix);
		// combine matrices
		for(var i = 1; i < arguments.length; ++i){
			var l = m;
			var r = dojo.gfx.matrix.normalize(arguments[i]);
			m = new dojo.gfx.matrix.Matrix2D();
			m.xx = l.xx * r.xx + l.xy * r.yx;
			m.xy = l.xx * r.xy + l.xy * r.yy;
			m.yx = l.yx * r.xx + l.yy * r.yx;
			m.yy = l.yx * r.xy + l.yy * r.yy;
			m.dx = l.xx * r.dx + l.xy * r.dy + l.dx;
			m.dy = l.yx * r.dx + l.yy * r.dy + l.dy;
		}
		return m; // dojo.gfx.matrix.Matrix2D
	},
	
	// high level operations
	
	_sandwich: function(m, x, y){
		// summary: applies a matrix at a centrtal point
		// m: dojo.gfx.matrix.Matrix2D: a 2D matrix-like object, which is applied at a central point
		// x: Number: an x component of the central point
		// y: Number: a y component of the central point
		return dojo.gfx.matrix.multiply(dojo.gfx.matrix.translate(x, y), m, dojo.gfx.matrix.translate(-x, -y)); // dojo.gfx.matrix.Matrix2D
	},
	scaleAt: function(a, b, c, d){
		// summary: scales a picture using a specified point as a center of scaling
		// description: Compare with dojo.gfx.matrix.scale().
		// a: Number: a scaling factor used for the x coordinate
		// b: Number: a scaling factor used for the y coordinate
		// c: Number: an x component of a central point
		// d: Number: a y component of a central point
		
		// accepts several signatures:
		//	1) uniform scale factor, Point
		//	2) uniform scale factor, x, y
		//	3) x scale, y scale, Point
		//	4) x scale, y scale, x, y
		
		switch(arguments.length){
			case 4:
				// a and b are scale factor components, c and d are components of a point
				return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.scale(a, b), c, d); // dojo.gfx.matrix.Matrix2D
			case 3:
				if(typeof c == "number"){
					// branch
					// a: Number: a uniform scaling factor used for both coordinates
					// b: Number: an x component of a central point
					// c: Number: a y component of a central point
					// d: null
					return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.scale(a), b, c); // dojo.gfx.matrix.Matrix2D
				}
				// branch
				// a: Number: a scaling factor used for the x coordinate
				// b: Number: a scaling factor used for the y coordinate
				// c: dojo.gfx.Point: a central point
				// d: null
				return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.scale(a, b), c.x, c.y); // dojo.gfx.matrix.Matrix2D
		}
		// branch
		// a: Number: a uniform scaling factor used for both coordinates
		// b: dojo.gfx.Point: a central point
		// c: null
		// d: null
		return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.scale(a), b.x, b.y); // dojo.gfx.matrix.Matrix2D
	},
	rotateAt: function(angle, a, b){
		// summary: rotates a picture using a specified point as a center of rotation
		// description: Compare with dojo.gfx.matrix.rotate().
		// angle: Number: an angle of rotation in radians (>0 for CCW)
		// a: Number: an x component of a central point
		// b: Number: a y component of a central point
		
		// accepts several signatures:
		//	1) rotation angle in radians, Point
		//	2) rotation angle in radians, x, y
		
		if(arguments.length > 2){
			return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.rotate(angle), a, b); // dojo.gfx.matrix.Matrix2D
		}
		
		// branch
		// angle: Number: an angle of rotation in radians (>0 for CCW)
		// a: dojo.gfx.Point: a central point
		// b: null
		return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.rotate(angle), a.x, a.y); // dojo.gfx.matrix.Matrix2D
	},
	rotategAt: function(degree, a, b){
		// summary: rotates a picture using a specified point as a center of rotation
		// description: Compare with dojo.gfx.matrix.rotateg().
		// degree: Number: an angle of rotation in degrees (>0 for CCW)
		// a: Number: an x component of a central point
		// b: Number: a y component of a central point
		
		// accepts several signatures:
		//	1) rotation angle in degrees, Point
		//	2) rotation angle in degrees, x, y
		
		if(arguments.length > 2){
			return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.rotateg(degree), a, b); // dojo.gfx.matrix.Matrix2D
		}

		// branch
		// degree: Number: an angle of rotation in degrees (>0 for CCW)
		// a: dojo.gfx.Point: a central point
		// b: null
		return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.rotateg(degree), a.x, a.y); // dojo.gfx.matrix.Matrix2D
	},
	skewXAt: function(angle, a, b){
		// summary: skews a picture along the x axis using a specified point as a center of skewing
		// description: Compare with dojo.gfx.matrix.skewX().
		// angle: Number: an skewing angle in radians
		// a: Number: an x component of a central point
		// b: Number: a y component of a central point
		
		// accepts several signatures:
		//	1) skew angle in radians, Point
		//	2) skew angle in radians, x, y
		
		if(arguments.length > 2){
			return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.skewX(angle), a, b); // dojo.gfx.matrix.Matrix2D
		}

		// branch
		// angle: Number: an skewing angle in radians
		// a: dojo.gfx.Point: a central point
		// b: null
		return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.skewX(angle), a.x, a.y); // dojo.gfx.matrix.Matrix2D
	},
	skewXgAt: function(degree, a, b){
		// summary: skews a picture along the x axis using a specified point as a center of skewing
		// description: Compare with dojo.gfx.matrix.skewXg().
		// degree: Number: an skewing angle in degrees
		// a: Number: an x component of a central point
		// b: Number: a y component of a central point
		
		// accepts several signatures:
		//	1) skew angle in degrees, Point
		//	2) skew angle in degrees, x, y

		if(arguments.length > 2){
			return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.skewXg(degree), a, b); // dojo.gfx.matrix.Matrix2D
		}

		// branch
		// degree: Number: an skewing angle in degrees
		// a: dojo.gfx.Point: a central point
		// b: null
		return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.skewXg(degree), a.x, a.y); // dojo.gfx.matrix.Matrix2D
	},
	skewYAt: function(angle, a, b){
		// summary: skews a picture along the y axis using a specified point as a center of skewing
		// description: Compare with dojo.gfx.matrix.skewY().
		// angle: Number: an skewing angle in radians
		// a: Number: an x component of a central point
		// b: Number: a y component of a central point
		
		// accepts several signatures:
		//	1) skew angle in radians, Point
		//	2) skew angle in radians, x, y
		
		if(arguments.length > 2){
			return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.skewY(angle), a, b); // dojo.gfx.matrix.Matrix2D
		}

		// branch
		// angle: Number: an skewing angle in radians
		// a: dojo.gfx.Point: a central point
		// b: null
		return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.skewY(angle), a.x, a.y); // dojo.gfx.matrix.Matrix2D
	},
	skewYgAt: function(/* Number */ degree, /* Number||Point */ a, /* Number, optional */ b){
		// summary: skews a picture along the y axis using a specified point as a center of skewing
		// description: Compare with dojo.gfx.matrix.skewYg().
		// degree: Number: an skewing angle in degrees
		// a: Number: an x component of a central point
		// b: Number: a y component of a central point
		
		// accepts several signatures:
		//	1) skew angle in degrees, Point
		//	2) skew angle in degrees, x, y

		if(arguments.length > 2){
			return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.skewYg(degree), a, b); // dojo.gfx.matrix.Matrix2D
		}

		// branch
		// degree: Number: an skewing angle in degrees
		// a: dojo.gfx.Point: a central point
		// b: null
		return dojo.gfx.matrix._sandwich(dojo.gfx.matrix.skewYg(degree), a.x, a.y); // dojo.gfx.matrix.Matrix2D
	}
	
	// TODO: rect-to-rect mapping, scale-to-fit (isotropic and anisotropic versions)
	
});
