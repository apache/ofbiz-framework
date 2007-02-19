/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.data.CsvStore");
dojo.require("dojo.data.core.RemoteStore");
dojo.require("dojo.lang.assert");

dojo.declare("dojo.data.CsvStore", dojo.data.core.RemoteStore, {
	/* summary:
	 *   The CsvStore subclasses dojo.data.core.RemoteStore to implement
	 *   the dojo.data.core.Read API.  
	 */
	
	/* examples:
	 *   var csvStore = new dojo.data.CsvStore({queryUrl:"movies.csv");
	 *   var csvStore = new dojo.data.CsvStore({url:"http://example.com/movies.csv");
	 */
	_setupQueryRequest: function(/* dojo.data.core.Result */ result, /* object */ requestKw) { 
		// summary: See dojo.data.core.RemoteStore._setupQueryRequest()
		var serverQueryUrl = this._serverQueryUrl ? this._serverQueryUrl : "";
		var queryUrl = result.query ? result.query : "";
		requestKw.url = serverQueryUrl + queryUrl;
		requestKw.method = 'get';
	},
	
	_resultToQueryData: function(/* varies */ serverResponseData) {
		// summary: See dojo.data.core.RemoteStore._resultToQueryData()
		var csvFileContentString = serverResponseData;
		var arrayOfArrays = this._getArrayOfArraysFromCsvFileContents(csvFileContentString);
		var arrayOfObjects = this._getArrayOfObjectsFromArrayOfArrays(arrayOfArrays);
        var remoteStoreData = this._getRemoteStoreDataFromArrayOfObjects(arrayOfObjects);
		return remoteStoreData;
	},
	
	_setupSaveRequest: function(/* object */ saveKeywordArgs, /* object */ requestKw) {
		// summary: See dojo.data.core.RemoteStore._setupSaveRequest()
		// description: NOT IMPLEMENTED -- CsvStore is a read-only store
	},
	
	// -------------------------------------------------------------------
	// Private methods
	_getArrayOfArraysFromCsvFileContents: function(/* string */ csvFileContents) {
		/* summary:
		 *   Parses a string of CSV records into a nested array structure.
		 * description:
		 *   Given a string containing CSV records, this method parses
		 *   the string and returns a data structure containing the parsed
		 *   content.  The data structure we return is an array of length
		 *   R, where R is the number of rows (lines) in the CSV data.  The 
		 *   return array contains one sub-array for each CSV line, and each 
		 *   sub-array contains C string values, where C is the number of 
		 *   columns in the CSV data.
		 */
		 
		/* example:
		 *   For example, given this CSV string as input:
		 *     "Title, Year, Producer \n Alien, 1979, Ridley Scott \n Blade Runner, 1982, Ridley Scott"
		 *   We will return this data structure:
		 *     [["Title", "Year", "Producer"]
		 *      ["Alien", "1979", "Ridley Scott"],  
		 *      ["Blade Runner", "1982", "Ridley Scott"]]
		 */
		dojo.lang.assertType(csvFileContents, String);
		
		var lineEndingCharacters = new RegExp("\r\n|\n|\r");
		var leadingWhiteSpaceCharacters = new RegExp("^\\s+",'g');
		var trailingWhiteSpaceCharacters = new RegExp("\\s+$",'g');
		var doubleQuotes = new RegExp('""','g');
		var arrayOfOutputRecords = [];
		
		var arrayOfInputLines = csvFileContents.split(lineEndingCharacters);
		for (var i in arrayOfInputLines) {
			var singleLine = arrayOfInputLines[i];
			if (singleLine.length > 0) {
				var listOfFields = singleLine.split(',');
				var j = 0;
				while (j < listOfFields.length) {
					var space_field_space = listOfFields[j];
					var field_space = space_field_space.replace(leadingWhiteSpaceCharacters, ''); // trim leading whitespace
					var field = field_space.replace(trailingWhiteSpaceCharacters, ''); // trim trailing whitespace
					var firstChar = field.charAt(0);
					var lastChar = field.charAt(field.length - 1);
					var secondToLastChar = field.charAt(field.length - 2);
					var thirdToLastChar = field.charAt(field.length - 3);
					if ((firstChar == '"') && 
							((lastChar != '"') || 
							 ((lastChar == '"') && (secondToLastChar == '"') && (thirdToLastChar != '"')) )) {
						if (j+1 === listOfFields.length) {
							// alert("The last field in record " + i + " is corrupted:\n" + field);
							return null;
						}
						var nextField = listOfFields[j+1];
						listOfFields[j] = field_space + ',' + nextField;
						listOfFields.splice(j+1, 1); // delete element [j+1] from the list
					} else {
						if ((firstChar == '"') && (lastChar == '"')) {
							field = field.slice(1, (field.length - 1)); // trim the " characters off the ends
							field = field.replace(doubleQuotes, '"');   // replace "" with "
						}
						listOfFields[j] = field;
						j += 1;
					}
				}
				arrayOfOutputRecords.push(listOfFields);
			}
		}
		return arrayOfOutputRecords; // Array
	},

	_getArrayOfObjectsFromArrayOfArrays: function(/* array[] */ arrayOfArrays) {
		/* summary:
		 *   Converts a nested array structure into an array of keyword objects.
		 */
		 
		/* example:
		 *   For example, given this as input:
		 *     [["Title", "Year", "Producer"]
		 *      ["Alien", "1979", "Ridley Scott"],  
		 *      ["Blade Runner", "1982", "Ridley Scott"]]
		 *   We will return this as output:
		 *     [{"Title":"Alien", "Year":"1979", "Producer":"Ridley Scott"},
		 *      {"Title":"Blade Runner", "Year":"1982", "Producer":"Ridley Scott"}]
		 */
		dojo.lang.assertType(arrayOfArrays, Array);
		var arrayOfItems = [];
		if (arrayOfArrays.length > 1) {
			var arrayOfKeys = arrayOfArrays[0];
			for (var i = 1; i < arrayOfArrays.length; ++i) {
				var row = arrayOfArrays[i];
				var item = {};
				for (var j in row) {
					var value = row[j];
					var key = arrayOfKeys[j];
					item[key] = value;
				}
				arrayOfItems.push(item);
			}
		}
		return arrayOfItems; // Array
	},
	
	_getRemoteStoreDataFromArrayOfObjects: function(/* object[] */ arrayOfObjects) {
		/* summary:
		 *   Converts an array of keyword objects in the internal record data 
		 *    structure used by RemoteStore.
		 */

		/* example:
		 *   For example, given this as input:
		 *     [{"Title":"Alien", "Year":"1979", "Producer":"Ridley Scott"},
		 *      {"Title":"Blade Runner", "Year":"1982", "Producer":"Ridley Scott"}]
		 *   We will return this as output:
		 *     { "1": {"Title":["Alien"], "Year":["1979"], "Producer":["Ridley Scott"]},
		 *       "2": {"Title":["Blade Runner"], "Year":["1982"], "Producer":["Ridley Scott"]}
		 *     }
		 */
		dojo.lang.assertType(arrayOfObjects, Array);
		var output = {};
		for (var i = 0; i < arrayOfObjects.length; ++i) {
			var object = arrayOfObjects[i];
			for (var key in object) {
				var value = object[key]; // {"Title":"Alien"} --> "Alien"
				object[key] = [value];   // {"Title":["Alien"]}
			}
			output[i] = object;
		}
		return output; // Object
	},

	// CsvStore implements the dojo.data.core.Read API, but does not yet  
	// implements the dojo.data.core.Write API.  CsvStore extends RemoteStore,
	// and RemoteStore does implement the Write API, so we need to explicitly
	// mark those Write API methods as being unimplemented.
	newItem: function(/* object? */ attributes, /* object? */ keywordArgs) {
		dojo.unimplemented('dojo.data.CsvStore.newItem');
	},
	deleteItem: function(/* item */ item) {
		dojo.unimplemented('dojo.data.CsvStore.deleteItem');
	},
	setValues: function(/* item */ item, /* attribute || string */ attribute, /* array */ values) {
		dojo.unimplemented('dojo.data.CsvStore.setValues');
	},
	set: function(/* item */ item, /* attribute || string */ attribute, /* almost anything */ value) {
		dojo.unimplemented('dojo.data.CsvStore.set');
	},
	unsetAttribute: function(/* item */ item, /* attribute || string */ attribute) {
		dojo.unimplemented('dojo.data.CsvStore.unsetAttribute');
	},
	save: function(/* object? */ keywordArgs) {
		dojo.unimplemented('dojo.data.CsvStore.save');
	},
	revert: function() {
		dojo.unimplemented('dojo.data.CsvStore.revert');
	},
	isDirty: function(/*item?*/ item) {
		dojo.unimplemented('dojo.data.CsvStore.isDirty');
	}

});

