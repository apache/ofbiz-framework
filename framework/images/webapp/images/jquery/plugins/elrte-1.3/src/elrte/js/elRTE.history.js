(function($) {
elRTE.prototype.history = function(rte) {
	this.rte = rte;
	this._prev = []
	this._next = [];

	
	this.add = function() {
		if (this.rte.options.historyLength>0 && this._prev.length>= this.rte.options.historyLength) {
			this._prev.slice(this.rte.options.historyLength);
		}
		var b = this.rte.selection.getBookmark();
		this._prev.push([$(this.rte.doc.body).html(), b]);
		this.rte.selection.moveToBookmark(b);
		// this._prev.push($(this.rte.doc.body).html());
		this._next = [];
	}
	
	this.back = function() {
		
		if (this._prev.length) {
			var b = this.rte.selection.getBookmark(), 
				data = this._prev.pop();
			this._next.push([$(this.rte.doc.body).html(), b]);
			
			$(this.rte.doc.body).html(data[0]);
			this.rte.selection.moveToBookmark(data[1]);
		}
	}

	this.fwd = function() {
		if (this._next.length) {
			var b = this.rte.selection.getBookmark(), 
				data = this._next.pop();
			this._prev.push([$(this.rte.doc.body).html(), b]);
			
			$(this.rte.doc.body).html(data[0]);
			this.rte.selection.moveToBookmark(data[1]);
		}
	}
	
	this.canBack = function() {
		return this._prev.length;
	}
	
	this.canFwd = function() {
		return this._next.length;
	}

}
})(jQuery);