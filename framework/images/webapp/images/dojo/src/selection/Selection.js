/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.selection.Selection");
dojo.require("dojo.lang.array");
dojo.require("dojo.lang.func");
dojo.require("dojo.lang.common");
dojo.require("dojo.math");

dojo.declare("dojo.selection.Selection", null,
	{
		initializer: function(items, isCollection){
			this.items = [];
			this.selection = [];
			this._pivotItems = [];
			this.clearItems();

			if(items) {
				if(isCollection) {
					this.setItemsCollection(items);
				} else {
					this.setItems(items);
				}
			}
		},

		// Array: items to select from, order matters for growable selections
		items: null,

		// Array: items selected, aren't stored in order (see sorted())
		selection: null, 
		lastSelected: null, // last item selected

		// Boolean: if true, grow selection will start from 0th item when nothing is selected
		allowImplicit: true, 

		// Integer: number of *selected* items
		length: 0, 

		// Boolean:
		//		if true, the selection is treated as an in-order and can grow
		//		by ranges, not just by single item
		isGrowable: true,

		_pivotItems: null, // stack of pivot items
		_pivotItem: null, // item we grow selections from, top of stack

		// event handlers
		onSelect: function(item){
			// summary: slot to be connect()'d to
		},
		onDeselect: function(item){
			// summary: slot to be connect()'d to
		},
		onSelectChange: function(item, selected){
			// summary: slot to be connect()'d to
		},

		_find: function(item, inSelection) {
			if(inSelection) {
				return dojo.lang.find(this.selection, item);
			} else {
				return dojo.lang.find(this.items, item);
			}
		},

		isSelectable: function(/*Object*/item){
			// summary:
			//		user-customizable and should be over-ridden, will filter
			//		items through this
			return true; // boolean
		},

		setItems: function(/* ... */){
			// summary:
			//		adds all passed arguments to the items array, removing any
			//		previously selected items.
			this.clearItems();
			this.addItems.call(this, arguments);
		},

 
		setItemsCollection: function(/*Object*/collection){
			// summary:
			//		like setItems, but use in case you have an active
			//		collection array-like object (i.e. getElementsByTagName
			//		collection) that manages its own order and item list
			this.items = collection;
		},

		addItems: function(/* ... */){
			// summary:
			//		adds all passed arguments to the items array
			var args = dojo.lang.unnest(arguments);
			for(var i = 0; i < args.length; i++){
				this.items.push(args[i]);
			}
		},

		addItemsAt: function(/*Object*/item, /*Object*/before /* ... */){
			// summary:
			//		add items to the array after the the passed "before" item.
			if(this.items.length == 0){ // work for empy case
				return this.addItems(dojo.lang.toArray(arguments, 2));
			}

			if(!this.isItem(item)){
				item = this.items[item];
			}
			if(!item){ throw new Error("addItemsAt: item doesn't exist"); }
			var idx = this._find(item);
			if(idx > 0 && before){ idx--; }
			for(var i = 2; i < arguments.length; i++){
				if(!this.isItem(arguments[i])){
					this.items.splice(idx++, 0, arguments[i]);
				}
			}
		},

		removeItem: function(/*Object*/item){
			// summary: remove item
			var idx = this._find(item);
			if(idx > -1) {
				this.items.splice(idx, 1);
			}
			// remove from selection
			// FIXME: do we call deselect? I don't think so because this isn't how
			// you usually want to deselect an item. For example, if you deleted an
			// item, you don't really want to deselect it -- you want it gone. -DS
			idx = this._find(item, true);
			if(idx > -1) {
				this.selection.splice(idx, 1);
			}
		},

		clearItems: function(){
			// summary: remove and uselect all items
			this.items = [];
			this.deselectAll();
		},

		isItem: function(/*Object*/item){
			// summary: do we already "know" about the passed item?
			return this._find(item) > -1; // boolean
		},

		isSelected: function(/*Object*/item){
			// summary:
			//		do we know about the item and is it selected by this
			//		selection?
			return this._find(item, true) > -1; // boolean
		},

		/**
		 * allows you to filter item in or out of the selection
		 * depending on the current selection and action to be taken
		**/
		selectFilter: function(item, selection, add, grow) {
			return true;
		},

		update: function(/*Object*/item, /*Boolean*/add, /*Boolean*/grow, noToggle) {
			// summary: manages selections, most selecting should be done here
			// item: item which may be added/grown to/only selected/deselected
			// add: behaves like ctrl in windows selection world
			// grow: behaves like shift
			// noToggle: if true, don't toggle selection on item
			if(!this.isItem(item)){ return false; } // boolean

			if(this.isGrowable && grow){
				if( (!this.isSelected(item)) && 
					this.selectFilter(item, this.selection, false, true) ){
					this.grow(item);
					this.lastSelected = item;
				}
			}else if(add){
				if(this.selectFilter(item, this.selection, true, false)){
					if(noToggle){
						if(this.select(item)){
							this.lastSelected = item;
						}
					}else if(this.toggleSelected(item)){
						this.lastSelected = item;
					}
				}
			}else{
				this.deselectAll();
				this.select(item);
			}

			this.length = this.selection.length;
			return true; // Boolean
		},

		grow: function(/*Object*/toItem, /*Object*/fromItem){
			// summary:
			//		Grow a selection. Any items in (fromItem, lastSelected]
			//		that aren't part of (fromItem, toItem] will be deselected
			// toItem: which item to grow selection to
			// fromItem: which item to start the growth from (it won't be selected)
			if(!this.isGrowable){ return; }

			if(arguments.length == 1){
				fromItem = this._pivotItem;
				if(!fromItem && this.allowImplicit){
					fromItem = this.items[0];
				}
			}
			if(!toItem || !fromItem){ return false; }

			var fromIdx = this._find(fromItem);

			// get items to deselect (fromItem, lastSelected]
			var toDeselect = {};
			var lastIdx = -1;
			if(this.lastSelected){
				lastIdx = this._find(this.lastSelected);
				var step = fromIdx < lastIdx ? -1 : 1;
				var range = dojo.math.range(lastIdx, fromIdx, step);
				for(var i = 0; i < range.length; i++){
					toDeselect[range[i]] = true;
				}
			}

			// add selection (fromItem, toItem]
			var toIdx = this._find(toItem);
			var step = fromIdx < toIdx ? -1 : 1;
			var shrink = lastIdx >= 0 && step == 1 ? lastIdx < toIdx : lastIdx > toIdx;
			var range = dojo.math.range(toIdx, fromIdx, step);
			if(range.length){
				for(var i = range.length-1; i >= 0; i--){
					var item = this.items[range[i]];
					if(this.selectFilter(item, this.selection, false, true)){
						if(this.select(item, true) || shrink){
							this.lastSelected = item;
						}
						if(range[i] in toDeselect){
							delete toDeselect[range[i]];
						}
					}
				}
			}else{
				this.lastSelected = fromItem;
			}

			// now deselect...
			for(var i in toDeselect){
				if(this.items[i] == this.lastSelected){
					//dojo.debug("oops!");
				}
				this.deselect(this.items[i]);
			}

			// make sure everything is all kosher after selections+deselections
			this._updatePivot();
		},

		growUp: function(){
			// summary: Grow selection upwards one item from lastSelected
			if(!this.isGrowable){ return; }

			var idx = this._find(this.lastSelected) - 1;
			while(idx >= 0){
				if(this.selectFilter(this.items[idx], this.selection, false, true)){
					this.grow(this.items[idx]);
					break;
				}
				idx--;
			}
		},

		growDown: function(){
			// summary: Grow selection downwards one item from lastSelected
			if(!this.isGrowable){ return; }

			var idx = this._find(this.lastSelected);
			if(idx < 0 && this.allowImplicit){
				this.select(this.items[0]);
				idx = 0;
			}
			idx++;
			while(idx > 0 && idx < this.items.length){
				if(this.selectFilter(this.items[idx], this.selection, false, true)){
					this.grow(this.items[idx]);
					break;
				}
				idx++;
			}
		},

		toggleSelected: function(/*Object*/item, /*Boolean*/noPivot){
			// summary:
			//		like it says on the tin. If noPivot is true, no selection
			//		pivot is added (or removed) from the selection. Returns 1
			//		if the item is selected, -1 if it is deselected, and 0 if
			//		the item is not under management.
			if(this.isItem(item)){
				if(this.select(item, noPivot)){ return 1; }
				if(this.deselect(item)){ return -1; }
			}
			return 0;
		},

		select: function(/*Object*/item, /*Boolean*/noPivot){
			// summary:
			//		like it says on the tin. If noPivot is true, no selection
			//		pivot is added  from the selection.
			if(this.isItem(item) && !this.isSelected(item)
				&& this.isSelectable(item)){
				this.selection.push(item);
				this.lastSelected = item;
				this.onSelect(item);
				this.onSelectChange(item, true);
				if(!noPivot){
					this._addPivot(item);
				}
				this.length = this.selection.length;
				return true;
			}
			return false;
		},

		deselect: function(item){
			// summary: deselects the item if it's selected.
			var idx = this._find(item, true);
			if(idx > -1){
				this.selection.splice(idx, 1);
				this.onDeselect(item);
				this.onSelectChange(item, false);
				if(item == this.lastSelected){
					this.lastSelected = null;
				}
				this._removePivot(item);
				this.length = this.selection.length;
				return true;
			}
			return false;
		},

		selectAll: function(){
			// summary: selects all known items
			for(var i = 0; i < this.items.length; i++){
				this.select(this.items[i]);
			}
		},

		deselectAll: function(){
			// summary: deselects all currently selected items
			while(this.selection && this.selection.length){
				this.deselect(this.selection[0]);
			}
		},

		selectNext: function(){
			// summary:
			//		clobbers the existing selection (if any) and selects the
			//		next item "below" the previous "bottom" selection. Returns
			//		whether or not selection was successful.
			var idx = this._find(this.lastSelected);
			while(idx > -1 && ++idx < this.items.length){
				if(this.isSelectable(this.items[idx])){
					this.deselectAll();
					this.select(this.items[idx]);
					return true;
				}
			}
			return false;
		},

		selectPrevious: function(){
			// summary:
			//		clobbers the existing selection (if any) and selects the
			//		item "above" the previous "top" selection. Returns whether
			//		or not selection was successful.
			var idx = this._find(this.lastSelected);
			while(idx-- > 0){
				if(this.isSelectable(this.items[idx])){
					this.deselectAll();
					this.select(this.items[idx]);
					return true;
				}
			}
			return false;
		},

		selectFirst: function(){
			// summary:
			//		select first selectable item. Returns whether or not an
			//		item was selected.
			this.deselectAll();
			var idx = 0;
			while(this.items[idx] && !this.select(this.items[idx])){
				idx++;
			}
			return this.items[idx] ? true : false;
		},

		selectLast: function(){
			// summary: select last selectable item
			this.deselectAll();
			var idx = this.items.length-1;
			while(this.items[idx] && !this.select(this.items[idx])) {
				idx--;
			}
			return this.items[idx] ? true : false;
		},

		_addPivot: function(item, andClear){
			this._pivotItem = item;
			if(andClear){
				this._pivotItems = [item];
			}else{
				this._pivotItems.push(item);
			}
		},

		_removePivot: function(item){
			var i = dojo.lang.find(this._pivotItems, item);
			if(i > -1){
				this._pivotItems.splice(i, 1);
				this._pivotItem = this._pivotItems[this._pivotItems.length-1];
			}

			this._updatePivot();
		},

		_updatePivot: function(){
			if(this._pivotItems.length == 0){
				if(this.lastSelected){
					this._addPivot(this.lastSelected);
				}
			}
		},

		sorted: function(){
			// summary: returns an array of items in sort order
			return dojo.lang.toArray(this.selection).sort(
				dojo.lang.hitch(this, function(a, b){
					var A = this._find(a), B = this._find(b);
					if(A > B){
						return 1;
					}else if(A < B){
						return -1;
					}else{
						return 0;
					}
				})
			);
		},

		updateSelected: function(){
			// summary: 
			//		remove any items from the selection that are no longer in
			//		this.items
			for(var i = 0; i < this.selection.length; i++) {
				if(this._find(this.selection[i]) < 0) {
					var removed = this.selection.splice(i, 1);

					this._removePivot(removed[0]);
				}
			}
			this.length = this.selection.length;
		}
	}
);
