/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.data.RdfStore");
dojo.provide("dojo.data.RhizomeStore");
dojo.require("dojo.lang.declare");
dojo.require("dojo.data.core.RemoteStore");
dojo.require("dojo.experimental");

/* summary:
 * RdfStore provides a dojo.data Store for querying and updating a server 
 * that supports the SPARQL Query Result JSON format.
 * (see http://www.w3.org/TR/rdf-sparql-json-res/)
 * 
 * It also maps RDF datatypes to Javascript objects.
 * 
 * RdfStore makes following assumptions about the Result JSON:
 * (1) The result always contains 3 bound variables named "s","p", and "o", 
 *     and each result binding is treated as an RDF statement.
 * (2) When saving changes to the store, the JSON "results" object will also 
 *     contain a "deleted" key whose value is a list of deleted RDF resources.
 *
 */

dojo.data.RdfDatatypeSerializer = function(/* JavaScript type */type, /* function */convertFunc, /* RDF datatype URI */uri) {
	/* summary:
	This class serializes a javascript object into a RDF datatype literal.
	*/
	this.type = type;
	this._converter = convertFunc;
	this.uri = uri;
	this.serialize = function(value) { 
		return this._converter.call(value, value); 
	};
}

dojo.declare("dojo.data.RdfStore", dojo.data.core.RemoteStore, {

	_datatypeMap: {
		//map datatype strings to constructor function
		literal: function(value) { 
			var literal = value.value;
			if (value["xml:lang"]) {
				literal.lang = value["xml:lang"];
			}
			return literal;
		},
		
		uri: function(value) { 
			return { id: value.value }; 
		},
		
		bnode: function(value) { 
			return { id: '_:' + value.value }; 
		},

		'http://www.w3.org/2001/XMLSchema#int': function(value) { 
			return parseInt(value.value); 
		},
		'http://www.w3.org/2001/XMLSchema#integer': function(value) { 
			return parseInt(value.value);
		},
		'http://www.w3.org/2001/XMLSchema#long': function(value) { 
			return parseInt(value.value);
		},
		'http://www.w3.org/2001/XMLSchema#float': function(value) { 
			return parseFloat(value.value);
		},
		'http://www.w3.org/2001/XMLSchema#double': function(value) { 
			return parseFloat(value.value);
		},
		'http://www.w3.org/2001/XMLSchema#boolean': function(value) { 
			return !value || value == "false" || value == "0" ? false : true; 
		}
		//todo: more datatypes: 
		//integer subtypes, string types, XMLiteral
		//,'http://www.w3.org/2001/XMLSchema#... : function(value) { return parseInt(value.value); }
	},

	_datatypeSerializers: [
		new dojo.data.RdfDatatypeSerializer(Number, Number.toString, 'http://www.w3.org/2001/XMLSchema#float'), 
		new dojo.data.RdfDatatypeSerializer(Boolean, Boolean.toString, 'http://www.w3.org/2001/XMLSchema#boolean')
	],
	
	_findDatatypeSerializer: function(value) {
		var length = this._datatypeSerializers.length;
		for (var i = 0; i < length; i++) {
			var datatype = this._datatypeSerializers[i];
			if (value instanceof datatype.type) {
				return datatype;
			}
		}
	},

	_toRDFValue: function(value) {
		//convert values to rdf json format
		//(from http://www.w3.org/TR/2006/NOTE-rdf-sparql-json-res-20061004/)

		var rdfvalue = {};
		if (value.id) {
			if (value.id.slice(0, 2) == '_:') {
				rdfvalue.type = 'bnode';
				rdfvalue.value = value.id.substring(2);
			} else {
				rdfvalue.type = 'uri';
				rdfvalue.value = value.id;
			}
		} else if (typeof value == "string" || value instanceof String) {
			rdfvalue.type = 'literal';
			rdfvalue.value = value;
			if (value.lang)
				rdfvalue["xml:lang"] = value.lang;
		} else {
			if (typeof value == "number")
				value = new Number(value);
			else if (typeof value == "boolean")
				value = new Boolean(value);
				
			var datatype = this._findDatatypeSerializer(value);
			if (datatype) {
				rdfvalue = {
					"type": "typed-literal",
					"datatype": datatype.uri,
					"value": value.toString()
					//todo: datatype.serialize(value) causes
					//Error: Function.prototype.toString called on incompatible number
				};
			} else {
				//treat it as a string 
				//todo: warn?
				rdfvalue = { 
					"type": "literal", 
				 	"value": value.toString() };
			}
		}
		return rdfvalue;
	},
	
	_setupSaveRequest: function(saveKeywordArgs, requestKw) { 
		 /*
		 This function prepares the save request by populating requestKw, 
		 an associative array that will be passed to dojo.io.bind.
		 */
		
		//see http://www.w3.org/TR/rdf-sparql-json-res/
		var rdfResult = { "head":  {'vars': ['s','p','o']}, 
						 "results": {'bindings': []} };

		var resources = [];
		for (var key in this._deleted) {
			resources.push(key);
		}
		rdfResult.results.deleted = resources;
		
		for (key in this._changed) {
			var subject = this._toRDFValue(this.getIdentity(key))
			
			var attributes = this._changed[key];
			for (var attr in attributes) {
				var predicate = {type:'uri', value: attr};
				
				var values = attributes[attr];
				if (!values.length)
					continue;
				var rdfvalues = [];
				for (var i = 0; i < values.length; i++) {
					var rdfvalue = this._toRDFValue(values[i]);
					rdfResult.results.bindings.push(
						{s: subject, p: predicate, o: rdfvalue});
				}
			}
		}
		
		var oldRegistry = dojo.json.jsonRegistry;
		dojo.json.jsonRegistry = this._jsonRegistry;
		var jsonString = dojo.json.serialize(rdfResult);
		dojo.json.jsonRegistry = oldRegistry;
		
		//dojo.debug('save json' , jsonString);
		
		requestKw.postContent = jsonString;
	},
	
	_resultToQueryMetadata: function(json) {
		return json.head;
	},
	
	_resultToQueryData: function(json) { 
		//assume s, p, o bindings
		var items = {};
		var stmts = json.results.bindings;
		for (var i = 0; i < stmts.length; i++) {
			var stmt = stmts[i];
			//assert stmt.s && stmt.p && stmt.o;
			var subject = stmt.s.value;
			if (stmt.s.type == 'bnode') {
				subject = '_:' + subject;
			}
			//else { assert stmt.s.type == 'uri';} 
			var attributes = data[subject];
			if (!attributes) {
				attributes = {};
				data[stmt.s] = attributes;
			}
			var attr = attributes[stmt.p.value];
			if (!attr) {
				attributes[stmt.p.value] = [stmt.o];
			} else {
				attr.push(stmt.o);
			}
		}
		return items;
	}
});

dojo.declare("dojo.data.RhizomeStore", dojo.data.RdfStore, {
	/* summary:
	 *   RhizomeStore is a subclass of RdfStore that works with
	 *   the Rhizome semantic wiki (see http://www.liminalzone.org)
	 *   Rhizome understands the RemoteStore's "native" json format
	 *   so it doesn't need to convert it to the SPARQL Query Result format.
	 */

	initializer: function(kwArgs) {
		this._serverQueryUrl = kwArgs.baseUrl + 'search?view=json&searchType=RxPath&search=';
		this._serverSaveUrl = kwArgs.baseUrl + 'save-metadata';
	},

	_resultToQueryMetadata: function(json) {
		return json;
	},
	
	_resultToQueryData: function(json) { 
		//dojo.debug( 'resultjson ' + dojo.json.serialize(json) );
		return json;
	},
	
	_setupSaveRequest: function(saveKeywordArgs, requestKw) { 
		 /*
		 This function prepares the save request by populating requestKw, 
		 an associative array that will be passed to dojo.io.bind.
		 */
		requestKw.url = this._serverSaveUrl;
		requestKw.method = 'post';
		requestKw.mimetype = "text/plain";
		
		var resources = [];
		for (var key in this._deleted) {
			resources.push(key);
		}

		var changes = {};
		for (key in this._changed) {
			if (!this._added[key]) { //don't put new resources in this list
				resources.push(key);
			}
			
			var attributes = this._changed[key];
			var rdfattributes = {};
			for (var attr in attributes) {
				var values = attributes[attr];
				if (!values.length)
					continue;
				var rdfvalues = [];
				for (var i = 0; i < values.length; i++) {
					var rdfvalue = this._toRDFValue(values[i]);
					rdfvalues.push(rdfvalue);
				}
				rdfattributes[attr] = rdfvalues;
			}
			changes[key] = rdfattributes;
		}
		
		var oldRegistry = dojo.json.jsonRegistry;
		dojo.json.jsonRegistry = this._jsonRegistry;
		var jsonString = dojo.json.serialize(changes);
		dojo.json.jsonRegistry = oldRegistry;
		
		requestKw.content = {
			rdfFormat: 'json',
			resource: resources,
			metadata: jsonString
		};
	}
});
