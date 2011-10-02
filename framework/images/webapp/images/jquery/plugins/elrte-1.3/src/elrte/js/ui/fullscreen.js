/**
 * @class button - switch to fullscreen mode and back
 *
 * @param  elRTE  rte   объект-редактор
 * @param  String name  название кнопки 
 *
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 * @copyright: Studio 42, http://www.std42.ru
 **/
(function($) {
elRTE.prototype.ui.prototype.buttons.fullscreen = function(rte, name) {
	var self     = this;
	this.constructor.prototype.constructor.call(this, rte, name);
	this.active  = true;
	this.editor = rte.editor;
	this.wz = rte.workzone;
	this.height  = 0;
	this.delta = 0;
	this._class = 'el-fullscreen';
	
	setTimeout(function() {
		self.height  = self.wz.height();
		self.delta   = self.editor.outerHeight()-self.height;
	}, 50);
	
	
	/**
	 * Update editor height on window resize in fullscreen view
	 *
	 **/
	function resize() {
		self.wz.height($(window).height()-self.delta);
		self.rte.updateHeight();
	}
	
	this.command = function() {
		var w = $(window),
			e = this.editor,
			p = e.parents().filter(function(i, n) { return  !/^(html|body)$/i.test(n.nodeName) && $(n).css('position') == 'relative'; }),
			wz = this.wz,
			c = this._class,
			f = e.hasClass(c),
			rte = this.rte,
			s = this.rte.selection,
			m = $.browser.mozilla,
			b, h;

		function save() {
			if (m) {
				b = s.getBookmark();
			}
		}
		
		function restore() {
			if (m) {
				self.wz.children().toggle();
				self.rte.source.focus();
				self.wz.children().toggle();
				s.moveToBookmark(b);
			}
		}

		save();
		p.css('position', f ? 'relative' : 'static');	
		
		if (f) {
			e.removeClass(c);
			wz.height(this.height);
			w.unbind('resize', resize);
			this.domElem.removeClass('active');
		} else {
			e.addClass(c).removeAttr('style');
			wz.height(w.height() - this.delta).css('width', '100%');
			w.bind('resize', resize);
			this.domElem.addClass('active');
		}
		rte.updateHeight();	
		rte.resizable(f);
		restore();
		
	}
	
	this.update = function() {
		this.domElem.removeClass('disabled');
	}
}
})(jQuery);
