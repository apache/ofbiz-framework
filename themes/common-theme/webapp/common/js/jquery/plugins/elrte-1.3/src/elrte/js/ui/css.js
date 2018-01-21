(function($) {
	
	elRTE.prototype.ui.prototype.buttons.css = function(rte, name) {
		var self = this;
		this.constructor.prototype.constructor.call(this, rte, name);
		this.cssStyle  = $('<input type="text" size="42" name="style" />');
		this.cssClass  = $('<input type="text" size="42" name="class" />');
		this.elementID = $('<input type="text" size="42" name="id" />');
		
		this.command = function() {
			var n = this.node(), opts;
			this.rte.selection.saveIERange();
			if (n) {
				var opts = {
					
					submit : function(e, d) { e.stopPropagation(); e.preventDefault(); d.close(); self.set();  },
					dialog : {
						title : this.rte.i18n('Style'),
						width : 450,
						resizable : true,
						modal : true
					}
				}
				this.cssStyle.val($(n).attr('style'));
				this.cssClass.val($(n).attr('class'));
				this.elementID.val($(n).attr('id'));
				var d = new elDialogForm(opts);
				d.append([this.rte.i18n('Css style'), this.cssStyle],  null, true)
				d.append([this.rte.i18n('Css class'), this.cssClass],  null, true)
				d.append([this.rte.i18n('ID'),        this.elementID], null, true)
				d.open();
				setTimeout(function() { self.cssStyle.focus() }, 20)
			}
		}
		
		this.set = function() {
			var n = this.node();
			this.rte.selection.restoreIERange();
			if (n) {
				$(n).attr('style', this.cssStyle.val());
				$(n).attr('class', this.cssClass.val());
				$(n).attr('id',    this.elementID.val());
				this.rte.ui.update();
			}
		}
		
		this.node = function() {
			var n = this.rte.selection.getNode();
			if (n.nodeType == 3) {
				n = n.parentNode;
			}
			return n.nodeType == 1 && n.nodeName != 'BODY' ? n : null;
		}
		
		this.update = function() {
			this.domElem.toggleClass('disabled', this.node()?false:true);
		}
		
	}
	
})(jQuery);
