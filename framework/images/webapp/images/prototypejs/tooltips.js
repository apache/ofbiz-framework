// Tooltip Object
var Tooltip = Class.create();
Tooltip.prototype = {
	initialize: function(el, options) {
		this.el = $(el);
		this.initialized = false;
		this.setOptions(options);

		// Event handlers
		this.showEvent = this.show.bindAsEventListener(this);
		this.hideEvent = this.hide.bindAsEventListener(this);
		this.updateEvent = this.update.bindAsEventListener(this);
		Event.observe(this.el, "mouseover", this.showEvent );
		Event.observe(this.el, "mouseout", this.hideEvent );

		// Removing title from DOM element to avoid showing it
		this.content = this.el.title;
		this.el.title = "";

		// If descendant elements has 'alt' attribute defined, clear it
		this.el.descendants().each(function(el){
			if(Element.readAttribute(el, 'alt'))
				el.alt = "";
		});
	},
	setOptions: function(options) {
		this.options = {
			backgroundColor: '#999', // Default background color
			borderColor: '#666', // Default border color
			textColor: '', // Default text color (use CSS value)
			textShadowColor: '', // Default text shadow color (use CSS value)
			maxWidth: 250,	// Default tooltip width
			align: "left", // Default align
			delay: 250, // Default delay before tooltip appears in ms
			mouseFollow: true, // Tooltips follows the mouse moving
			opacity: .75, // Default tooltips opacity
			appearDuration: .25, // Default appear duration in sec
			hideDuration: .25 // Default disappear duration in sec
		};
		Object.extend(this.options, options || {});
	},
	show: function(e) {
		this.xCord = Event.pointerX(e);
		this.yCord = Event.pointerY(e);
		if(!this.initialized)
			this.timeout = window.setTimeout(this.appear.bind(this), this.options.delay);
	},
	hide: function(e) {
		if(this.initialized) {
			this.appearingFX.cancel();
			if(this.options.mouseFollow)
				Event.stopObserving(this.el, "mousemove", this.updateEvent);
			new Effect.Fade(this.tooltip, {duration: this.options.hideDuration, afterFinish: function() { Element.remove(this.tooltip) }.bind(this) });
		}
		this._clearTimeout(this.timeout);

		this.initialized = false;
	},
	update: function(e){
		this.xCord = Event.pointerX(e);
		this.yCord = Event.pointerY(e);
		this.setup();
	},
	appear: function() {
		// Building tooltip container
		this.tooltip = Builder.node("div", {className: "tooltip", style: "display: none;" }, [
			Builder.node("div", {className:"xtop"}, [
				Builder.node("div", {className:"xb1", style:"background-color:" + this.options.borderColor + ";"}),
				Builder.node("div", {className:"xb2", style: "background-color:" + this.options.backgroundColor + "; border-color:" + this.options.borderColor + ";"}),
				Builder.node("div", {className:"xb3", style: "background-color:" + this.options.backgroundColor + "; border-color:" + this.options.borderColor + ";"}),
				Builder.node("div", {className:"xb4", style: "background-color:" + this.options.backgroundColor + "; border-color:" + this.options.borderColor + ";"})
			]),
			Builder.node("div", {className: "xboxcontent", style: "background-color:" + this.options.backgroundColor +
				"; border-color:" + this.options.borderColor +
				((this.options.textColor != '') ? "; color:" + this.options.textColor : "") +
				((this.options.textShadowColor != '') ? "; text-shadow:2px 2px 0" + this.options.textShadowColor + ";" : "")}, this.content),
			Builder.node("div", {className:"xbottom"}, [
				Builder.node("div", {className:"xb4", style: "background-color:" + this.options.backgroundColor + "; border-color:" + this.options.borderColor + ";"}),
				Builder.node("div", {className:"xb3", style: "background-color:" + this.options.backgroundColor + "; border-color:" + this.options.borderColor + ";"}),
				Builder.node("div", {className:"xb2", style: "background-color:" + this.options.backgroundColor + "; border-color:" + this.options.borderColor + ";"}),
				Builder.node("div", {className:"xb1", style:"background-color:" + this.options.borderColor + ";"})
			]),
		]);
		document.body.insertBefore(this.tooltip, document.body.childNodes[0]);

		Element.extend(this.tooltip); // IE needs element to be manually extended
		this.options.width = this.tooltip.getWidth();
		this.tooltip.style.width = this.options.width + 'px'; // IE7 needs width to be defined

		this.setup();

		if(this.options.mouseFollow)
			Event.observe(this.el, "mousemove", this.updateEvent);

		this.initialized = true;
		this.appearingFX = new Effect.Appear(this.tooltip, {duration: this.options.appearDuration, to: this.options.opacity });
	},
	setup: function(){
		// If content width is more then allowed max width, set width to max
		if(this.options.width > this.options.maxWidth) {
			this.options.width = this.options.maxWidth;
			this.tooltip.style.width = this.options.width + 'px';
		}

		// Tooltip doesn't fit the current document dimensions
		if(this.xCord + this.options.width >= Element.getWidth(document.body)) {
			this.options.align = "right";
			this.xCord = this.xCord - this.options.width + 20;
		}

		this.tooltip.style.left = this.xCord - 7 + "px";
		this.tooltip.style.top = this.yCord + 12 + "px";
	},
	_clearTimeout: function(timer) {
		clearTimeout(timer);
		clearInterval(timer);
		return null;
	}
};