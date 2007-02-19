/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.CurrencyTextbox");

dojo.require("dojo.widget.IntegerTextbox");
dojo.require("dojo.validate.common");

dojo.widget.defineWidget(
	"dojo.widget.CurrencyTextbox",
	dojo.widget.IntegerTextbox,
	{
		// summary:
		//	  A subclass that extends IntegerTextbox.
		//		Over-rides isValid/isInRange to test if input denotes a monetary value .

		/*=====
		// fractional: Boolean
		//		The decimal places (e.g. for cents).  Can be true or false, optional if omitted.
		fractional: undefined,

		// symbol: String
		//		A currency symbol such as Yen "???", Pound "???", or the Euro "???". Default is "$".
		symbol: "$",

		// separator: String
		//		Default is "," instead of no separator as in IntegerTextbox.
		separator: ",",

		// min: Number
		//		Minimum signed value.  Default is -Infinity
		min: undefined,

		// max: Number
		//		Maximum signed value.  Default is +Infinity
		max: undefined,
		=====*/

		mixInProperties: function(localProperties, frag){
			// First initialize properties in super-class.
			dojo.widget.CurrencyTextbox.superclass.mixInProperties.apply(this, arguments);
	
			// Get properties from markup attributes, and assign to flags object.
			if(localProperties.fractional){
				this.flags.fractional = (localProperties.fractional == "true");
			}else if(localProperties.cents){
				dojo.deprecated("dojo.widget.IntegerTextbox", "use fractional attr instead of cents", "0.5");
				this.flags.fractional = (localProperties.cents == "true");
			}
			if(localProperties.symbol){
				this.flags.symbol = localProperties.symbol;
			}
			if(localProperties.min){ 
				this.flags.min = parseFloat(localProperties.min);
			}
			if(localProperties.max){ 
				this.flags.max = parseFloat(localProperties.max);
			}
		},

		isValid: function(){
			// summary: Over-ride for currency validation
			return dojo.validate.isCurrency(this.textbox.value, this.flags);
		},
		isInRange: function(){
			// summary: Over-ride for currency validation
			return dojo.validate.isInRange(this.textbox.value, this.flags);
		}
	}
);
