/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.data.core.RemoteStore");
dojo.require("dojo.data.core.Read");
dojo.require("dojo.data.core.Write");
dojo.require("dojo.data.core.Result");
dojo.require("dojo.experimental");
dojo.require("dojo.Deferred");
dojo.require("dojo.lang.declare");
dojo.require("dojo.json");
dojo.require("dojo.io.*");

/* summary:
 *   RemoteStore is an implemention the dojo.data.core.Read and Write APIs. 
 *   It is designed to serve as a base class for dojo.data stores which interact 
 *   with stateless web services that can querying and modifying record-oriented 
 *   data.  Its features include asynchronous and synchronous querying and saving; 
 *   caching of queries; transactions; and datatype mapping.
 */

/**************************************************************************
  Classes derived from RemoteStore should implement the following three 
  methods, which are each described in the documentation below:
    _setupQueryRequest(result, requestKw) 
    _resultToQueryData(responseData) 
    _setupSaveRequest(saveKeywordArgs, requestKw)
  
  Data Consistency Guarantees
  
  * if two references to the same item are obtained (e.g. from two different query results) any changes to one item will be reflected in the other item reference.
  * If an item has changed on the server and the item is retrieved via a new query, any previously obtained references to the item will (silently) reflect these new values.
  * However, any uncommitted changes will not be "overwritten".
  * If server queries are made while there are uncommitted changes, no attempt is made to evaluate whether the modifications would change the query result, e.g. add any uncommitted new items that match the query.
  * However, uncomitted deleted items are removed from the query result.
  * The transaction isolation level is equivalent to JDBC's "Read Committed":
    each store instance is treated as separate transaction; since there is no row or table locking so nonrepeatable and phantom reads are possible.
  
  Memory Usage
  
  Because Javascript doesn't support weak references or user-defined finalize methods, there is a tradeoff between data consistency and memory usage.
  In order to implement the above consistency guarantees (and to provide caching), RemoteStore remembers all the queries and items retrieved. 
  To reduce memory consumption, use the method forgetResults(query);
  
  Store assumptions
  
  RemoteStore makes some assumptions about the nature of the remote store, things may break if these aren't true:
  * that the items contained in a query response include all the attributes of the item (e.g. all the columns of a row).   
    (to fix: changes need to record add and removes and fix self._data[key] = [ attributeDict, refCount]; )
  * the query result may contain references to items that are not available to the client; use isItem() to test for the presence of the item.
  * that modification to an item's attributes won't change it's primary key.
  
**************************************************************************/

/* dojo.data API issues to resolve:
 * save should returns a Deferred, might want to add keyword argument with 'sync' 
 */

dojo.experimental("dojo.data.core.RemoteStore");

dojo.lang.declare("dojo.data.core.RemoteStore", [dojo.data.core.Read, dojo.data.core.Write], {

	_datatypeMap: {
		//map datatype strings to constructor function
	},

	//set to customize json serialization
	_jsonRegistry: dojo.json.jsonRegistry,

	initializer: function(/* object */ kwArgs) {
		if (!kwArgs) {
			kwArgs = {};
		}
		this._serverQueryUrl = kwArgs.queryUrl || "";
		this._serverSaveUrl = kwArgs.saveUrl || "";
				
		this._deleted = {}; // deleted items {id: 1}	
		this._changed = {}; // {id: {attr: [new values]}} // [] if attribute is removed
		this._added = {};   // {id: 1} list of added items
		this._results = {}; // {query: [ id1, ]};	// todo: make MRUDict of queries
		/* data is a dictionary that conforms to this format: 
		  { id-string: { attribute-string: [ value1, value2 ] } }
		  where value is either an atomic JSON data type or 
		  { 'id': string } for references to items
		  or 
		  { 'type': 'name', 'value': 'value' } for user-defined datatypes
		*/ 
		this._data = {}; // {id: [values, refcount]} // todo: handle refcount
		this._numItems = 0;
	},
	
	_setupQueryRequest: function(/* dojo.data.core.Result */ result, /* object */ requestKw) { 
		/* summary:
		 *   Classes derived from RemoteStore should override this method to
		 *   provide their own implementations.
		 *   This function prepares the query request by populating requestKw, 
		 *   an associative array that will be passed to dojo.io.bind.
		 */
		result.query = result.query || "";
		requestKw.url = this._serverQueryUrl + encodeURIComponent(result.query);
		requestKw.method = 'get';
		requestKw.mimetype = "text/json";
	},

	_resultToQueryMetadata: function(/* varies */ serverResponseData) { 
		/* summary:
		 *   Classes derived from RemoteStore should override this method to
		 *   provide their own implementations.
		 *   Converts the server response data into the resultMetadata object
		 *   that will be returned to the caller.
		 * returns:
		 *   This simple default implementation just returns the entire raw
		 *   serverResponseData, allowing the caller complete access to the 
		 *   raw response data and metadata.
		 */
		return serverResponseData; 
	},
		
	_resultToQueryData: function(/* varies */ serverResponseData) {
		/* summary:
		 *   Classes derived from RemoteStore should override this method to
		 *   provide their own implementations.
		 *   Converts the server response data into the internal data structure 
		 *   used by RemoteStore.  
		 * returns:
		 *   The RemoteStore implementation requires _resultToQueryData() to 
		 *   return an object that looks like:
		 *   {item1-identifier-string: { 
		 *   	attribute1-string: [ value1, value2, ... ], 
		 *   	attribute2-string: [ value3, value4, ... ], 
		 *   	...
		 *   	},
		 *    item2-identifier-string: { 
		 *   	attribute1-string: [ value10, value11, ... ], 
		 *   	attribute2-string: [ value12, value13, ... ], 
		 *   	...
		 *   	}
		 *   }
		 *   where value is either an atomic JSON data type or 
		 *     {'id': string } for references to items
		 *   or 
		 *    {'type': 'name', 'value': 'value' } for user-defined datatypes
		 * data:
		 *   This simple default implementation assumes that the *serverResponseData* 
		 *   argument is an object that looks like:
		 *     { data:{ ... }, format:'format identifier', other metadata }
		 *   
		 */
		return serverResponseData.data;
	},

	_remoteToLocalValues: function(/* object */ attributes) {
		for (var key in attributes) {
			 var values = attributes[key];
			 for (var i = 0; i < values.length; i++) {
				var value = values[i];
				var type = value.datatype || value.type;
				if (type) {
					// todo: better error handling?
					var localValue = value.value;
					if (this._datatypeMap[type]) 
						localValue = this._datatypeMap[type](value);							
					values[i] = localValue;
				}
			}
		}
		return attributes; // object (attributes argument, modified in-place)
	},

	_queryToQueryKey: function(query) {
		/* summary:
		 *   Convert the query to a string that uniquely represents this query. 
		 *   (Used by the query cache.)
		 */
		if (typeof query == "string")
			return query;
		else
			return dojo.json.serialize(query);
	},

	_assertIsItem: function(/* item */ item) {
		if (!this.isItem(item)) { 
			throw new Error("dojo.data.RemoteStore: a function was passed an item argument that was not an item");
		}
	},
	
	get: function(/* item */ item, /* attribute || string */ attribute, /* value? */ defaultValue) {
		// summary: See dojo.data.core.Read.get()
		var valueArray = this.getValues(item, attribute);
		if (valueArray.length == 0) {
			return defaultValue;
		}
		return valueArray[0];  // value
	},

	getValues: function(/* item */ item, /* attribute || string */ attribute) {				
		// summary: See dojo.data.core.Read.getValues()
		var itemIdentity = this.getIdentity(item);
		this._assertIsItem(itemIdentity);
		var changes = this._changed[itemIdentity];
		if (changes) {
			var newvalues = changes[attribute]; 
			if (newvalues !== undefined) {
				return newvalues;  // Array
			}
			else {
				return []; // Array
			}
		}
		// return item.atts[attribute];
		return this._data[itemIdentity][0][attribute]; // Array
	},

	getAttributes: function(/* item */ item) {	
		// summary: See dojo.data.core.Read.getAttributes()
		var itemIdentity = this.getIdentity(item);
		if (!itemIdentity) 
			return undefined; //todo: raise exception

		var atts = [];
		//var attrDict = item.attrs;
		var attrDict = this._data[itemIdentity][0];
		for (var att in attrDict) {
			atts.push(att);
		}
		return atts; // Array
	},
	
	hasAttribute: function(/* item */ item, /* attribute || string */ attribute) {
		// summary: See dojo.data.core.Read.hasAttribute()
		var valueArray = this.getValues(item, attribute);
		return valueArray.length ? true : false; // Boolean
	},

	containsValue: function(/* item */ item, /* attribute || string */ attribute, /* value */ value) {
		// summary: See dojo.data.core.Read.containsValue()
		var valueArray = this.getValues(item, attribute);
		for (var i=0; i < valueArray.length; i++) {	
			if (valueArray[i] == value) {
				return true; // Boolean
			}
		}
		return false; // Boolean
	},
		
	isItem: function(/* anything */ something) {
		// summary: See dojo.data.core.Read.isItem()
		if (!something) { return false; }
		var itemIdentity = something;
		// var id = something.id ? something.id : something; 
		// if (!id) { return false; }
		if (this._deleted[itemIdentity]) { return false; } //todo: do this?
		if (this._data[itemIdentity]) { return true; } 
		if (this._added[itemIdentity]) { return true; }
		return false; // Boolean
	},

	find: function(/* object? || dojo.data.core.Result */ keywordArgs) {
		// summary: See dojo.data.core.Read.find()
		/* description:
		 *   In addition to the keywordArgs parameters described in the
		 *   dojo.data.core.Read.find() documentation, the keywordArgs for
		 *   the RemoteStore find() method may include a bindArgs parameter,
		 *   which the RemoteStore will pass to dojo.io.bind when it sends 
		 *   the query.  The bindArgs parameter should be a keyword argument 
		 *   object, as described in the dojo.io.bind documentation.
		 */
		var result = null;
		if (keywordArgs instanceof dojo.data.core.Result) {
			result = keywordArgs;
			result.store = this;
		} else {
			result = new dojo.data.core.Result(keywordArgs, this);
		}
		var query = result.query;
		
		//todo: use this._results to implement caching
		var self = this;
		var bindfunc = function(type, data, evt) {
			var scope = result.scope || dj_global;
			if(type == "load") {	
				//dojo.debug("loaded 1 " + dojo.json.serialize(data) );
				result.resultMetadata = self._resultToQueryMetadata(data);
				var dataDict = self._resultToQueryData(data); 
				//dojo.debug("loaded 2 " + dojo.json.serialize(dataDict) );
				if (result.onbegin) {
					result.onbegin.call(scope, result);
				}
				var count = 0;
				var resultData = []; 
				var newItemCount = 0;
				for (var key in dataDict) {
					if (result._aborted)  {
						break;
					}
					if (!self._deleted[key]) { //skip deleted items
						//todo if in _added, remove from _added
						var values = dataDict[key];										
						var attributeDict = self._remoteToLocalValues(values);
						var existingValue = self._data[key];
						var refCount = 1;
						if (existingValue) {
							refCount = ++existingValue[1]; //increment ref count
						} else {
							newItemCount++;
						}
						//note: if the item already exists, we replace the item with latest set of attributes
						//this assumes queries always return complete records
						self._data[key] = [ attributeDict, refCount]; 
						resultData.push(key);
						count++; 
						if (result.onnext) {
							result.onnext.call(scope, key, result);
						}
					}									
				}
				self._results[self._queryToQueryKey(query)] = resultData; 
				self._numItems += newItemCount;

				result.length = count;
				if (result.saveResult) {
					result.items = resultData;
				}
				if (!result._aborted && result.oncompleted) {
					result.oncompleted.call(scope, result);
				}
			} else if(type == "error" || type == 'timeout') {
				// here, "data" is our error object
				//todo: how to handle timeout?
				dojo.debug("find error: " + dojo.json.serialize(data));
				if (result.onerror) {
					result.onerror.call(scope, data);
				}
			}
		};

		var bindKw = keywordArgs.bindArgs || {};
		bindKw.sync = result.sync;
		bindKw.handle = bindfunc;

		this._setupQueryRequest(result, bindKw);
		var request = dojo.io.bind(bindKw);
		//todo: error if not bind success
		//dojo.debug( "bind success " + request.bindSuccess);
		result._abortFunc = request.abort;	 
		return result; 
	},

	getIdentity: function(item) {
		// summary: See dojo.data.core.Read.getIdentity()
		if (!this.isItem(item)) {
			return null;
		}
		return (item.id ? item.id : item); // Identity
	},

/*
	findByIdentity: function(id) {
		var item = this._latestData[id];
		var idQuery = "/" + "*[.='"+id+"']";
		//if (!item) item = this.find(idQuery, {async=0}); //todo: support bind(async=0)
		if (item)
			return new _Item(id, item, this); 
		return null;
	},
*/

/****
Write API
***/
	newItem: function(/* object? */ attributes, /* object? */ keywordArgs) {
		var itemIdentity = keywordArgs['identity'];
		if (this._deleted[itemIdentity]) {
			delete this._deleted[itemIdentity];
		} else {
			this._added[itemIdentity] = 1;
			//todo? this._numItems++; ?? but its not in this._data
		}
		if (attributes) {
			// FIXME:
			for (var attribute in attributes) {
				var valueOrArrayOfValues = attributes[attribute];
				if (dojo.lang.isArray(valueOrArrayOfValues)) {
					this.setValues(itemIdentity, attribute, valueOrArrayOfValues);
				} else {
					this.set(itemIdentity, attribute, valueOrArrayOfValues);
				}
			}
		}
		return { id: itemIdentity };
	},
		
	deleteItem: function(/* item */ item) {
		var identity = this.getIdentity(item);
		if (!identity) {
			return false;
		}
		
		if (this._added[identity]) {
			delete this._added[identity];
		} else {
			this._deleted[identity] = 1; 
			//todo? this._numItems--; ?? but its still in this._data
		}
			
		if (this._changed[identity]) {
			delete this._changed[identity];	
		}
		return true; 
	},
	
	setValues: function(/* item */ item, /* attribute || string */ attribute, /* array */ values) {
		var identity = this.getIdentity(item);
		if (!identity) {
			return undefined; //todo: raise exception
		}

		var changes = this._changed[identity];
		if (!changes) {
			changes = {}
			this._changed[identity] = changes;
		} 					
		changes[attribute] = values;
		return true; // boolean
	},

	set: function(/* item */ item, /* attribute || string */ attribute, /* almost anything */ value) {
		return this.setValues(item, attribute, [value]); 
	},

	unsetAttribute: function(/* item */ item, /* attribute || string */ attribute) {
		return this.setValues(item, attribute, []); 
	},

	_initChanges: function() {
		this._deleted = {}; 
		this._changed = {};
		this._added = {}; 
	},

	_setupSaveRequest: function(saveKeywordArgs, requestKw) { 
		/* summary:
		 *   This function prepares the save request by populating requestKw, 
		 *   an associative array that will be passed to dojo.io.bind.
		 */
		requestKw.url = this._serverSaveUrl;
		requestKw.method = 'post';
		requestKw.mimetype = "text/plain";
		var deleted = [];
		for (var key in this._deleted) {
			deleted.push(key);
		}
		//don't need _added in saveStruct, changed covers that info	 
		var saveStruct = {'changed': this._changed, 'deleted': deleted };
		var oldRegistry = dojo.json.jsonRegistry;
		dojo.json.jsonRegistry = this._jsonRegistry;
		var jsonString = dojo.json.serialize(saveStruct);
		dojo.json.jsonRegistry = oldRegistry;
		requestKw.postContent = jsonString;
	},

	save: function(/* object? */ keywordArgs) {
		/* summary:
		 *   Saves all the changes that have been made.
		 * keywordArgs:
		 *   The optional keywordArgs parameter may contain 'sync' to specify 
		 *   whether the save operation is asynchronous or not.  The default is 
		 *   asynchronous.  
		 * examples: 
		 *   store.save();
		 *   store.save({sync:true});
		 *   store.save({sync:false});
		 */
		keywordArgs = keywordArgs || {};
		var result = new dojo.Deferred();			 
		var self = this;

		var bindfunc = function(type, data, evt) {			
			if(type == "load"){ 
				if (result.fired == 1) {
					//it seems that mysteriously "load" sometime 
					//gets called after "error"
					//so check if an error has already occurred 
					//and stop if it has 
					return;
				}
				//update this._data upon save
				var key = null;
				for (key in self._added) {
					if (!self._data[key])
					self._data[key] = [{} , 1];
				}
				for (key in self._changed) {
					var existing = self._data[key];
					var changes = self._changed[key];
					if (existing) {
						existing[0] = changes;
					} else {
						self._data[key] = [changes, 1];
					}
				}
				for (key in self._deleted) {
					if (self._data[key]) {
						delete self._data[key];
					}
				}
				self._initChanges(); 
				result.callback(true); //todo: what result to pass?
			} else if(type == "error" || type == 'timeout'){
				result.errback(data); //todo: how to handle timeout
			}	
		};
				
		var bindKw = { sync: keywordArgs["sync"], handle: bindfunc };
		this._setupSaveRequest(keywordArgs, bindKw);
		var request = dojo.io.bind(bindKw);
		result.canceller = function(deferred) { request.abort(); };
				
		return result; 
	},
		 
	revert: function() {
		this._initChanges(); 
		return true;
	},

	isDirty: function(/*item?*/ item) {
		if (item) {
			// return true if this item is dirty
			var identity = item.id || item;
			return this._deleted[identity] || this._changed[identity];
		} else {
			// return true if any item is dirty
			var key = null;
			for (key in this._changed) {
				return true;
			}
			for (key in this._deleted) {
				return true;
			}
			for (key in this._added) {
				return true;
			}

			return false;
		}
	},

/**
additional public methods
*/
	createReference: function(idstring) {
		return { id : idstring };
	},

	getSize: function() { 
		return this._numItems; 
	},
		
	forgetResults: function(query) {
		var queryKey = this._queryToQueryKey(query);
		var results = this._results[queryKey];
		if (!results) return false;

		var removed = 0;
		for (var i = 0; i < results.length; i++) {
			var key = results[i];
			var existingValue = this._data[key];
			if (existingValue[1] <= 1) {
				delete this._data[key];
				removed++;
			}
			else
				existingValue[1] = --existingValue[1];
		}
		delete this._results[queryKey];
		this._numItems -= removed;
		return true;
	} 
});



