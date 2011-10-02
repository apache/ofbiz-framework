/**
 * @class button - insert smiley (open dialog window)
 *
 * @param  elRTE  rte   объект-редактор
 * @param  String name  название кнопки
 *
 * @author:    eSabbath
 *
 **/
(function($) { 
elRTE.prototype.ui.prototype.buttons.smiley = function(rte, name) {
	this.constructor.prototype.constructor.call(this, rte, name);
	var self = this;
	this.img = null;
	this.url = this.rte.filter.url+'smileys/';
	this.smileys = {
		'smile' : 'smile.png',
		'happy' : 'happy.png',
		'tongue' : 'tongue.png',
		'surprised' : 'surprised.png',
		'waii' : 'waii.png',
		'wink' : 'wink.png',
		'evilgrin' : 'evilgrin.png',
		'grin' : 'grin.png',
		
		
		'unhappy' : 'unhappy.png'
	};
	this.width = 120;

	this.command = function() {
		var self = this, url = this.url, d, opts, img;

		this.rte.browser.msie && this.rte.selection.saveIERange();

		opts = {
			dialog : {
				height  : 120,
				width   : this.width,
				title   : this.rte.i18n('Smiley'),
				buttons : {}
			}
		}
		d = new elDialogForm(opts);
		$.each(this.smileys, function(name, img) {
			d.append($('<img src="'+url+img+'" title="'+name+'" id="'+name+'" class="el-rte-smiley"/>').click(function() { self.set(this.id, d);	}));
		});
		d.open();
	}

	this.update = function() {
		this.domElem.removeClass('disabled');
		this.domElem.removeClass('active');
	}

	this.set = function(s, d) {
		this.rte.browser.msie && this.rte.selection.restoreIERange();
		if (this.smileys[s]) {
			this.img = $(this.rte.doc.createElement('img'));
			this.img.attr({
				src    : this.url + this.smileys[s],
				title  : s,
				alt    : s
			});
			this.rte.selection.insertNode(this.img.get(0));
			this.rte.ui.update();
		}
		d.close();
	}
}
})(jQuery);
