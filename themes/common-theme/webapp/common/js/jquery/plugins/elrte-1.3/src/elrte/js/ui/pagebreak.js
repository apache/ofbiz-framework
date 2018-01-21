(function($) {
	elRTE.prototype.ui.prototype.buttons.pagebreak = function(rte, name) {
		this.constructor.prototype.constructor.call(this, rte, name);
		
		// prevent resize
		$(this.rte.doc.body).bind('mousedown', function(e) {
			if ($(e.target).hasClass('elrte-pagebreak')) {
				e.preventDefault();
			}
		})
		
		this.command = function() {
			this.rte.selection.insertHtml('<img src="'+this.rte.filter.url+'pixel.gif" class="elrte-protected elrte-pagebreak"/>', false);
		}
		
		this.update = function() {
			this.domElem.removeClass('disabled');
		}
	}
	
})(jQuery);