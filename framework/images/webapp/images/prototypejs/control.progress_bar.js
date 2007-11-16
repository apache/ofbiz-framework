/**
 * @author Ryan Johnson <ryan@livepipe.net>
 * @copyright 2007 LivePipe LLC
 * @package Control.ProgressBar
 * @license MIT
 * @url http://livepipe.net/projects/control_progress_bar/
 * @version 1.0.1
 */

if(typeof(Control) == 'undefined')
	Control = {};
Control.ProgressBar = Class.create();
Object.extend(Control.ProgressBar.prototype,{
	container: false,
	containerWidth: 0,
	progressContainer: false,
	progress: 0,
	executer: false,
	active: false,
	poller: false,
	initialize: function(container,options){
		this.container = $(container);
		this.containerWidth = this.container.getDimensions().width - (parseInt(this.container.getStyle('border-right-width').replace(/px/,'')) + parseInt(this.container.getStyle('border-left-width').replace(/px/,'')));
		this.progressContainer = $(document.createElement('div'));
		this.progressContainer.setStyle({
			width: this.containerWidth + 'px',
			height: '100%',
			position: 'absolute',
			top: '0px',
			right: '0px'
		});
		this.container.appendChild(this.progressContainer);
		this.options = {
			afterChange: Prototype.emptyFunction,
			interval: 0.25,
			step: 1,
			classNames: {
				active: 'progress_bar_active',
				inactive: 'progress_bar_inactive'
			}
		};
		Object.extend(this.options,options || {});
		this.container.addClassName(this.options.classNames.inactive);
		this.active = false;
	},
	setProgress: function(value){
		this.progress = value;
		this.draw();
		if(this.progress >= 100)
			this.stop(false);
		this.notify('afterChange',this.progress,this.active);
	},
	poll: function(url,interval){
		this.active = true;
		this.poller = new PeriodicalExecuter(function(){
			new Ajax.Request(url,{
				onSuccess: function(request){
					this.setProgress(parseInt(request.responseText));
					if(!this.active)
						this.poller.stop();
				}.bind(this)
			});
		}.bind(this),interval || 3);
	},
	start: function(){
		this.active = true;
		this.container.removeClassName(this.options.classNames.inactive);
		this.container.addClassName(this.options.classNames.active);
		this.executer = new PeriodicalExecuter(this.step.bind(this,this.options.step),this.options.interval);
	},
	stop: function(reset){
		this.active = false;
		if(this.executer)
			this.executer.stop();
		this.container.removeClassName(this.options.classNames.active);
		this.container.addClassName(this.options.classNames.inactive);
		if(typeof(reset) == 'undefined' || reset == true)
			this.reset();
	},
	step: function(amount){
		this.active = true;
		this.setProgress(Math.min(100,this.progress + amount));
	},
	reset: function(){
		this.active = false;
		this.setProgress(0);
	},
	draw: function(){
		this.progressContainer.setStyle({
			width: (this.containerWidth - Math.floor((this.progress / 100) * this.containerWidth)) + 'px'
		});
	},
	notify: function(event_name){
		if(this.options[event_name])
			return [this.options[event_name].apply(this.options[event_name],$A(arguments).slice(1))];
	}
});
if(typeof(Object.Event) != 'undefined')
	Object.Event.extend(Control.ProgressBar);