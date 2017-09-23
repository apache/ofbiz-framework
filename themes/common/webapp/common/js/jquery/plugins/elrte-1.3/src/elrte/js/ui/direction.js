
(function($) {
	/**
	 * @class button - right to left direction (not work yet with text nodes in body)
	 *
	 * @param  elRTE  rte   объект-редактор
	 * @param  String name  название кнопки 
	 *
	 * @author:    Dmitry Levashov (dio) dio@std42.ru
	 * Copyright: Studio 42, http://www.std42.ru
	 **/
	elRTE.prototype.ui.prototype.buttons.rtl = function(rte, name)  {
		this.constructor.prototype.constructor.call(this, rte, name);
		var self = this;
	
		this.command = function() {
			var n = this.rte.selection.getNode(), self = this;
			if ($(n).attr('dir') == 'rtl' || $(n).parents('[dir="rtl"]').length || $(n).find('[dir="rtl"]').length) {
				$(n).removeAttr('dir');
				$(n).parents('[dir="rtl"]').removeAttr('dir');
				$(n).find('[dir="rtl"]').removeAttr('dir');
			} else {
				if (this.rte.dom.is(n, 'textNodes') && this.rte.dom.is(n, 'block')) {
					$(n).attr('dir', 'rtl');
				} else {
					$.each(this.rte.dom.parents(n, 'textNodes'), function(i, n) {
						if (self.rte.dom.is(n, 'block')) {
							$(n).attr('dir', 'rtl');
							return false;
						}
					});
				}
			}
			this.rte.ui.update();
		}

		this.update = function() {
			var n = this.rte.selection.getNode();
			this.domElem.removeClass('disabled');
			if ($(n).attr('dir') == 'rtl' || $(n).parents('[dir="rtl"]').length || $(n).find('[dir="rtl"]').length) {
				this.domElem.addClass('active');
			} else {
				this.domElem.removeClass('active');
			}
		}
	}
	
	/**
	 * @class button - left to right direction (not work yet with text nodes in body)
	 *
	 * @param  elRTE  rte   объект-редактор
	 * @param  String name  название кнопки 
	 *
	 * @author:    Dmitry Levashov (dio) dio@std42.ru
	 * Copyright: Studio 42, http://www.std42.ru
	 **/
	elRTE.prototype.ui.prototype.buttons.ltr = function(rte, name)  {
		this.constructor.prototype.constructor.call(this, rte, name);
		var self = this;
	
		this.command = function() {
			var n = this.rte.selection.getNode(), self = this;
			if ($(n).attr('dir') == 'ltr' || $(n).parents('[dir="ltr"]').length || $(n).find('[dir="ltr"]').length) {
				$(n).removeAttr('dir');
				$(n).parents('[dir="ltr"]').removeAttr('dir');
				$(n).find('[dir="ltr"]').removeAttr('dir');
			} else {
				if (this.rte.dom.is(n, 'textNodes') && this.rte.dom.is(n, 'block')) {
					$(n).attr('dir', 'ltr');
				} else {
					$.each(this.rte.dom.parents(n, 'textNodes'), function(i, n) {
						if (self.rte.dom.is(n, 'block')) {
							$(n).attr('dir', 'ltr');
							return false;
						}
					});
				}
			}
			this.rte.ui.update();
		}

		this.update = function() {
			var n = this.rte.selection.getNode();
			this.domElem.removeClass('disabled');
			if ($(n).attr('dir') == 'ltr' || $(n).parents('[dir="ltr"]').length || $(n).find('[dir="ltr"]').length) {
				this.domElem.addClass('active');
			} else {
				this.domElem.removeClass('active');
			}
		}
	}
	
})(jQuery);