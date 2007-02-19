/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.lang.func");
dojo.require("dojo.lang.common");

dojo.lang.hitch = function(/*Object*/thisObject, /*Function|String*/method){
	// summary: 
	//		Returns a function that will only ever execute in the a given scope
	//		(thisObject). This allows for easy use of object member functions
	//		in callbacks and other places in which the "this" keyword may
	//		otherwise not reference the expected scope. Note that the order of
	//		arguments may be reversed in a future version.
	// thisObject: the scope to run the method in
	// method:
	//		a function to be "bound" to thisObject or the name of the method in
	//		thisObject to be used as the basis for the binding
	// usage:
	//		dojo.lang.hitch(foo, "bar")(); // runs foo.bar() in the scope of foo
	//		dojo.lang.hitch(foo, myFunction); // returns a function that runs myFunction in the scope of foo

	// FIXME:
	//		should this be extended to "fixate" arguments in a manner similar
	//		to dojo.lang.curry, but without the default execution of curry()?
	var fcn = (dojo.lang.isString(method) ? thisObject[method] : method) || function(){};
	return function(){
		return fcn.apply(thisObject, arguments); // Function
	};
}

dojo.lang.anonCtr = 0;
dojo.lang.anon = {};

dojo.lang.nameAnonFunc = function(/*Function*/anonFuncPtr, /*Object*/thisObj, /*Boolean*/searchForNames){
	// summary:
	//		Creates a reference to anonFuncPtr in thisObj with a completely
	//		unique name. The new name is returned as a String.  If
	//		searchForNames is true, an effort will be made to locate an
	//		existing reference to anonFuncPtr in thisObj, and if one is found,
	//		the existing name will be returned instead. The default is for
	//		searchForNames to be false.
	var nso = (thisObj|| dojo.lang.anon);
	if( (searchForNames) ||
		((dj_global["djConfig"])&&(djConfig["slowAnonFuncLookups"] == true)) ){
		for(var x in nso){
			try{
				if(nso[x] === anonFuncPtr){
					return x;
				}
			}catch(e){} // window.external fails in IE embedded in Eclipse (Eclipse bug #151165)
		}
	}
	var ret = "__"+dojo.lang.anonCtr++;
	while(typeof nso[ret] != "undefined"){
		ret = "__"+dojo.lang.anonCtr++;
	}
	nso[ret] = anonFuncPtr;
	return ret; // String
}

dojo.lang.forward = function(funcName){
	// summary:
	// 		Returns a function that forwards a method call to
	// 		this.funcName(...).  Unlike dojo.lang.hitch(), the "this" scope is
	// 		not fixed on a single object. Ported from MochiKit.
	return function(){
		return this[funcName].apply(this, arguments);
	}; // Function
}

dojo.lang.curry = function(thisObj, func /* args ... */){
	// summary:
	//		similar to the curry() method found in many functional programming
	//		environments, this function returns an "argument accumulator"
	//		function, bound to a particular scope, and "primed" with a variable
	//		number of arguments. The curry method is unique in that it returns
	//		a function that may return other "partial" function which can be
	//		called repeatedly. New functions are returned until the arity of
	//		the original function is reached, at which point the underlying
	//		function (func) is called in the scope thisObj with all of the
	//		accumulated arguments (plus any extras) in positional order.
	// examples:
	//		assuming a function defined like this:
	//			var foo = {
	//				bar: function(arg1, arg2, arg3){
	//					dojo.debug.apply(dojo, arguments);
	//				}
	//			};
	//		
	//		dojo.lang.curry() can be used most simply in this way:
	//		
	//			tmp = dojo.lang.curry(foo, foo.bar, "arg one", "thinger");
	//			tmp("blah", "this is superfluous");
	//			// debugs: "arg one thinger blah this is superfluous"
	//			tmp("blah");
	//			// debugs: "arg one thinger blah"
	//			tmp();
	//			// returns a function exactly like tmp that expects one argument
	//
	//		other intermittent functions could be created until the 3
	//		positional arguments are filled:
	//
	//			tmp = dojo.lang.curry(foo, foo.bar, "arg one");
	//			tmp2 = tmp("arg two");
	//			tmp2("blah blah");
	//			// debugs: "arg one arg two blah blah"
	//			tmp2("oy");
	//			// debugs: "arg one arg two oy"
	//
	//		curry() can also be used to call the function if enough arguments
	//		are passed in the initial invocation:
	//
	//			dojo.lang.curry(foo, foo.bar, "one", "two", "three", "four");
	//			// debugs: "one two three four"
	//			dojo.lang.curry(foo, foo.bar, "one", "two", "three");
	//			// debugs: "one two three"


	// FIXME: the order of func and thisObj should be changed!!!
	var outerArgs = [];
	thisObj = thisObj||dj_global;
	if(dojo.lang.isString(func)){
		func = thisObj[func];
	}
	for(var x=2; x<arguments.length; x++){
		outerArgs.push(arguments[x]);
	}
	// since the event system replaces the original function with a new
	// join-point runner with an arity of 0, we check to see if it's left us
	// any clues about the original arity in lieu of the function's actual
	// length property
	var ecount = (func["__preJoinArity"]||func.length) - outerArgs.length;
	// borrowed from svend tofte
	function gather(nextArgs, innerArgs, expected){
		var texpected = expected;
		var totalArgs = innerArgs.slice(0); // copy
		for(var x=0; x<nextArgs.length; x++){
			totalArgs.push(nextArgs[x]);
		}
		// check the list of provided nextArgs to see if it, plus the
		// number of innerArgs already supplied, meets the total
		// expected.
		expected = expected-nextArgs.length;
		if(expected<=0){
			var res = func.apply(thisObj, totalArgs);
			expected = texpected;
			return res;
		}else{
			return function(){
				return gather(arguments,// check to see if we've been run
										// with enough args
							totalArgs,	// a copy
							expected);	// how many more do we need to run?;
			};
		}
	}
	return gather([], outerArgs, ecount);
}

dojo.lang.curryArguments = function(/*Object*/thisObj, /*Function*/func, /*Array*/args, /*Integer, optional*/offset){
	// summary:
	//		similar to dojo.lang.curry(), except that a list of arguments to
	//		start the curry with may be provided as an array instead of as
	//		positional arguments. An offset may be specified from the 0 index
	//		to skip some elements in args.
	var targs = [];
	var x = offset||0;
	for(x=offset; x<args.length; x++){
		targs.push(args[x]); // ensure that it's an arr
	}
	return dojo.lang.curry.apply(dojo.lang, [thisObj, func].concat(targs));
}

dojo.lang.tryThese = function(/*...*/){
	// summary:
	//		executes each function argument in turn, returning the return value
	//		from the first one which does not throw an exception in execution.
	//		Any number of functions may be passed.
	for(var x=0; x<arguments.length; x++){
		try{
			if(typeof arguments[x] == "function"){
				var ret = (arguments[x]());
				if(ret){
					return ret;
				}
			}
		}catch(e){
			dojo.debug(e);
		}
	}
}

dojo.lang.delayThese = function(/*Array*/farr, /*Function, optional*/cb, /*Integer*/delay, /*Function, optional*/onend){
	// summary:
	//		executes a series of functions contained in farr, but spaces out
	//		calls to each function by the millisecond delay provided. If cb is
	//		provided, it will be called directly after each item in farr is
	//		called and if onend is passed, it will be called when all items
	//		have completed executing.

	/**
	 * alternate: (array funcArray, function callback, function onend)
	 * alternate: (array funcArray, function callback)
	 * alternate: (array funcArray)
	 */
	if(!farr.length){ 
		if(typeof onend == "function"){
			onend();
		}
		return;
	}
	if((typeof delay == "undefined")&&(typeof cb == "number")){
		delay = cb;
		cb = function(){};
	}else if(!cb){
		cb = function(){};
		if(!delay){ delay = 0; }
	}
	setTimeout(function(){
		(farr.shift())();
		cb();
		dojo.lang.delayThese(farr, cb, delay, onend);
	}, delay);
}
