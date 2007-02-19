/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.undo.Manager");
dojo.require("dojo.lang.common");

dojo.undo.Manager = function(/*dojo.undo.Manager Object */parent) {
	//summary: Constructor for a dojo.undo.Manager object.
	this.clear();
	this._parent = parent;
};
dojo.extend(dojo.undo.Manager, {
	_parent: null,
	_undoStack: null,
	_redoStack: null,
	_currentManager: null,

	canUndo: false,
	canRedo: false,

	isUndoing: false,
	isRedoing: false,

	onUndo: function(/*Object*/manager, /*Object*/item) {
		//summary: An event that fires when undo is called.
		//It allows you to hook in and update your code (UI?) as necessary.

		//manager: Object: the dojo.undo.Manager instance.
		//item: Object: The object stored by the undo stack. It has the following properties:
		//		undo: Function: the undo function for this item in the stack.
		//		redo: Function: the redo function for this item in the stack. May be null.
		//		description: String: description of this item. May be null.
	},
	onRedo: function(/*Object*/manager, /*Object*/item) {
		//summary: An event that fires when redo is called.
		//It allows you to hook in and update your code (UI?) as necessary.

		//manager: Object: the dojo.undo.Manager instance.
		//item: Object: The object stored by the redo stack. It has the following properties:
		//		undo: Function: the undo function for this item in the stack.
		//		redo: Function: the redo function for this item in the stack. May be null.
		//		description: String: description of this item. May be null.
	},

	onUndoAny: function(/*Object*/manager, /*Object*/item) {
		//summary: An event that fires when *any* undo action is done, 
		//which means you'll have one for every item
		//in a transaction. This is usually only useful for debugging.
		//See notes for onUndo for info on the function parameters.
	},
	
	onRedoAny: function(/*Object*/manager, /*Object*/item) {
		//summary: An event that fires when *any* redo action is done, 
		//which means you'll have one for every item
		//in a transaction. This is usually only useful for debugging.
		//See notes for onRedo for info on the function parameters.
	},

	_updateStatus: function() {
		//summary: Private method used to set some internal state.
		this.canUndo = this._undoStack.length > 0;
		this.canRedo = this._redoStack.length > 0;
	},

	clear: function() {
		//summary: Clears this instance of dojo.undo.Manager.
		this._undoStack = [];
		this._redoStack = [];
		this._currentManager = this;

		this.isUndoing = false;
		this.isRedoing = false;

		this._updateStatus();
	},

	undo: function() {
		//summary: Call this method to go one place back in the undo
		//stack. Returns true if the manager successfully completed
		//the undo step.
		if(!this.canUndo) { return false; /*boolean*/}

		this.endAllTransactions();

		this.isUndoing = true;
		var top = this._undoStack.pop();
		if(top instanceof dojo.undo.Manager){
			top.undoAll();
		}else{
			top.undo();
		}
		if(top.redo){
			this._redoStack.push(top);
		}
		this.isUndoing = false;

		this._updateStatus();
		this.onUndo(this, top);
		if(!(top instanceof dojo.undo.Manager)) {
			this.getTop().onUndoAny(this, top);
		}
		return true; //boolean
	},

	redo: function() {
		//summary: Call this method to go one place forward in the redo
		//stack. Returns true if the manager successfully completed
		//the redo step.
		if(!this.canRedo){ return false; /*boolean*/}

		this.isRedoing = true;
		var top = this._redoStack.pop();
		if(top instanceof dojo.undo.Manager) {
			top.redoAll();
		}else{
			top.redo();
		}
		this._undoStack.push(top);
		this.isRedoing = false;

		this._updateStatus();
		this.onRedo(this, top);
		if(!(top instanceof dojo.undo.Manager)){
			this.getTop().onRedoAny(this, top);
		}
		return true; //boolean
	},

	undoAll: function() {
		//summary: Call undo as many times as it takes to get all the
		//way through the undo stack.
		while(this._undoStack.length > 0) {
			this.undo();
		}
	},

	redoAll: function() {
		//summary: Call redo as many times as it takes to get all the
		//way through the redo stack.
		while(this._redoStack.length > 0) {
			this.redo();
		}
	},

	push: function(/*Function*/undo, /*Function?*/redo, /*String?*/description) {
		//summary: add something to the undo manager.
		if(!undo) { return; }

		if(this._currentManager == this) {
			this._undoStack.push({
				undo: undo,
				redo: redo,
				description: description
			});
		} else {
			this._currentManager.push.apply(this._currentManager, arguments);
		}
		// adding a new undo-able item clears out the redo stack
		this._redoStack = [];
		this._updateStatus();
	},

	concat: function(/*Object*/manager) {
		//summary: Adds all undo and redo stack items to another dojo.undo.Manager
		//instance.
		if ( !manager ) { return; }

		if (this._currentManager == this ) {
			for(var x=0; x < manager._undoStack.length; x++) {
				this._undoStack.push(manager._undoStack[x]);
			}
			// adding a new undo-able item clears out the redo stack
			if (manager._undoStack.length > 0) {
				this._redoStack = [];
			}
			this._updateStatus();
		} else {
			this._currentManager.concat.apply(this._currentManager, arguments);
		}
	},

	beginTransaction: function(/*String?*/description) {
		//summary: All undo/redo items added via
		//push() after this call is made but before endTransaction() is called are
		//treated as one item in the undo and redo stacks. When undo() or redo() is
		//called then undo/redo is called on all of the items in the transaction.
		//Transactions can be nested.
		if(this._currentManager == this) {
			var mgr = new dojo.undo.Manager(this);
			mgr.description = description ? description : "";
			this._undoStack.push(mgr);
			this._currentManager = mgr;
			return mgr;
		} else {
			//for nested transactions need to make sure the top level _currentManager is set
			this._currentManager = this._currentManager.beginTransaction.apply(this._currentManager, arguments);
		}
	},

	endTransaction: function(flatten /* optional */) {
		//summary: Ends a transaction started by beginTransaction(). See beginTransaction()
		//for details.
		
		//flatten: boolean: If true, adds the current transaction to the parent's
		//undo stack.
	
		if(this._currentManager == this) {
			if(this._parent) {
				this._parent._currentManager = this._parent;
				// don't leave empty transactions hangin' around
				if(this._undoStack.length == 0 || flatten) {
					var idx = dojo.lang.find(this._parent._undoStack, this);
					if (idx >= 0) {
						this._parent._undoStack.splice(idx, 1);
						//add the current transaction to parents undo stack
						if (flatten) {
							for(var x=0; x < this._undoStack.length; x++){
								this._parent._undoStack.splice(idx++, 0, this._undoStack[x]);
							}
							this._updateStatus();
						}
					}
				}
				return this._parent;
			}
		} else {
			//for nested transactions need to make sure the top level _currentManager is set
			this._currentManager = this._currentManager.endTransaction.apply(this._currentManager, arguments);
		}
	},

	endAllTransactions: function() {
		//summary: Ends all nested transactions.
		while(this._currentManager != this) {
			this.endTransaction();
		}
	},

	getTop: function() {
		//summary: Finds the top parent of an undo manager.
		if(this._parent) {
			return this._parent.getTop();
		} else {
			return this;
		}
	}
});
