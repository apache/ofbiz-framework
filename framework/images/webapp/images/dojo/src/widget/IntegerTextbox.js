/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.IntegerTextbox");

dojo.require("dojo.widget.ValidationTextbox");
dojo.require("dojo.validate.common");

dojo.widget.defineWidget(
	"dojo.widget.IntegerTextbox",
	dojo.widget.ValidationTextbox,
	{
		// summary:
		//		A subclass of ValidationTextbox.
		//		Over-rides isValid/isInRange to test for integer input.
		// signed: String
		//		The leading plus-or-minus sign. Can be true or false, default is either.
		/*=====
		signed: "either",
		// separator: "",
		//		The character used as the thousands separator.  Default is no separator.
		separator: "",
		// min: Number
		//		Minimum signed value.  Default is -Infinity
		min: undefined,
		// max: Number
		//		Maximum signed value.  Default is +Infinity
		max: undefined,
		=====*/
		mixInProperties: function(localProperties, frag){
			// First initialize properties in super-class.
			dojo.widget.IntegerTextbox.superclass.mixInProperties.apply(this, arguments);
	
			// Get properties from markup attributes, and assign to flags object.
			if((localProperties.signed == "true")||
				(localProperties.signed == "always")){
				this.flags.signed = true;
			}else if((localProperties.signed == "false")||
					(localProperties.signed == "never")){
				this.flags.signed = false;
				this.flags.min = 0;
			}else{
				this.flags.signed = [ true, false ]; // optional
			}
			if(localProperties.separator){ 
				this.flags.separator = localProperties.separator;
			}
			if(localProperties.min){ 
				this.flags.min = parseInt(localProperties.min);
			}
			if(localProperties.max){ 
				this.flags.max = parseInt(localProperties.max);
			}
		},

		isValid: function(){
			// summary: Over-ride for integer validation
			return dojo.validate.isInteger(this.textbox.value, this.flags);
		},
		isInRange: function(){
			// summary: Over-ride for integer validation
			return dojo.validate.isInRange(this.textbox.value, this.flags);
		}
	}
);
