/**
 * @class кнопка - отмена повтор действий
 *
 * @param  elRTE  rte   объект-редактор
 * @param  String name  название кнопки 
 * 
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 * @copyright: Studio 42, http://www.std42.ru
 **/
(function($) {
	elRTE.prototype.ui.prototype.buttons.undo = function(rte, name) {
		this.constructor.prototype.constructor.call(this, rte, name);
	
		this.command = function() {
			if (this.name == 'undo' && this.rte.history.canBack()) {
				this.rte.history.back();
				this.rte.ui.update();
			} else if (this.name == 'redo' && this.rte.history.canFwd()) {
				this.rte.history.fwd();
				this.rte.ui.update();
			}
		}
	
		this.update = function() {
			this.domElem.toggleClass('disabled', this.name == 'undo' ? !this.rte.history.canBack() : !this.rte.history.canFwd());
		}
	}

	elRTE.prototype.ui.prototype.buttons.redo = elRTE.prototype.ui.prototype.buttons.undo;

})(jQuery);