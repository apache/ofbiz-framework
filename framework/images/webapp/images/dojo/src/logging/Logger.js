/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.logging.Logger");
dojo.provide("dojo.logging.LogFilter");
dojo.provide("dojo.logging.Record");
dojo.provide("dojo.log");
dojo.require("dojo.lang.common");
dojo.require("dojo.lang.declare");

/*		This is the dojo logging facility, which is imported from nWidgets
		(written by Alex Russell, CLA on file), which is patterned on the
		Python logging module, which in turn has been heavily influenced by
		log4j (execpt with some more pythonic choices, which we adopt as well).

		While the dojo logging facilities do provide a set of familiar
		interfaces, many of the details are changed to reflect the constraints
		of the browser environment. Mainly, file and syslog-style logging
		facilites are not provided, with HTTP POST and GET requests being the
		only ways of getting data from the browser back to a server. Minimal
		support for this (and XML serialization of logs) is provided, but may
		not be of practical use in a deployment environment.

		The Dojo logging classes are agnostic of any environment, and while
		default loggers are provided for browser-based interpreter
		environments, this file and the classes it define are explicitly
		designed to be portable to command-line interpreters and other
		ECMA-262v3 envrionments.

	the logger needs to accomidate:
		log "levels"
		type identifiers
		file?
		message
		tic/toc?

	The logger should ALWAYS record:
		time/date logged
		message
		type
		level
*/
// TODO: define DTD for XML-formatted log messages
// TODO: write XML Formatter class
// TODO: write HTTP Handler which uses POST to send log lines/sections


dojo.logging.Record = function(/*Integer*/logLevel, /*String||Array*/message){
	// summary:
	//		A simple data structure class that stores information for and about
	//		a logged event. Objects of this type are created automatically when
	//		an event is logged and are the internal format in which information
	//		about log events is kept.
	// logLevel:
	//		Integer mapped via the dojo.logging.log.levels object from a
	//		string. This mapping also corresponds to an instance of
	//		dojo.logging.Logger
	// message:
	//		The contents of the message represented by this log record.
	this.level = logLevel;
	this.message = "";
	this.msgArgs = [];
	this.time = new Date();
	
	if(dojo.lang.isArray(message)){
		if(message.length > 0 && dojo.lang.isString(message[0])){
			this.message=message.shift();
		}
		this.msgArgs = message;
	}else{
		this.message = message;
	}
	// FIXME: what other information can we receive/discover here?
}

dojo.logging.LogFilter = function(loggerChain){
	// summary:
	//		An empty parent (abstract) class which concrete filters should
	//		inherit from. Filters should have only a single method, filter(),
	//		which processes a record and returns true or false to denote
	//		whether or not it should be handled by the next step in a filter
	//		chain.
	this.passChain = loggerChain || "";
	this.filter = function(record){
		// FIXME: need to figure out a way to enforce the loggerChain
		// restriction
		return true; // pass all records
	}
}

dojo.logging.Logger = function(){
	this.cutOffLevel = 0;
	this.propagate = true;
	this.parent = null;
	// storage for dojo.logging.Record objects seen and accepted by this logger
	this.data = [];
	this.filters = [];
	this.handlers = [];
}

dojo.extend(dojo.logging.Logger,{
	_argsToArr: function(args){
		var ret = [];
		for(var x=0; x<args.length; x++){
			ret.push(args[x]);
		}
		return ret;
	},

	setLevel: function(/*Integer*/lvl){
		// summary: 
		//		set the logging level for this logger.
		// lvl:
		//		the logging level to set the cutoff for, as derived from the
		//		dojo.logging.log.levels object. Any messages below the
		//		specified level are dropped on the floor
		this.cutOffLevel = parseInt(lvl);
	},

	isEnabledFor: function(/*Integer*/lvl){
		// summary:
		//		will a message at the specified level be emitted?
		return parseInt(lvl) >= this.cutOffLevel; // boolean
	},

	getEffectiveLevel: function(){
		// summary:
		//		gets the effective cutoff level, including that of any
		//		potential parent loggers in the chain.
		if((this.cutOffLevel==0)&&(this.parent)){
			return this.parent.getEffectiveLevel(); // Integer
		}
		return this.cutOffLevel; // Integer
	},

	addFilter: function(/*dojo.logging.LogFilter*/flt){
		// summary:
		//		registers a new LogFilter object. All records will be passed
		//		through this filter from now on.
		this.filters.push(flt);
		return this.filters.length-1; // Integer
	},

	removeFilterByIndex: function(/*Integer*/fltIndex){
		// summary:
		//		removes the filter at the specified index from the filter
		//		chain. Returns whether or not removal was successful.
		if(this.filters[fltIndex]){
			delete this.filters[fltIndex];
			return true; // boolean
		}
		return false; // boolean
	},

	removeFilter: function(/*dojo.logging.LogFilter*/fltRef){
		// summary:
		//		removes the passed LogFilter. Returns whether or not removal
		//		was successful.
		for(var x=0; x<this.filters.length; x++){
			if(this.filters[x]===fltRef){
				delete this.filters[x];
				return true;
			}
		}
		return false;
	},

	removeAllFilters: function(){
		// summary: clobbers all the registered filters.
		this.filters = []; // clobber all of them
	},

	filter: function(/*dojo.logging.Record*/rec){
		// summary:
		//		runs the passed Record through the chain of registered filters.
		//		Returns a boolean indicating whether or not the Record should
		//		be emitted.
		for(var x=0; x<this.filters.length; x++){
			if((this.filters[x]["filter"])&&
			   (!this.filters[x].filter(rec))||
			   (rec.level<this.cutOffLevel)){
				return false; // boolean
			}
		}
		return true; // boolean
	},

	addHandler: function(/*dojo.logging.LogHandler*/hdlr){
		// summary: adds as LogHandler to the chain
		this.handlers.push(hdlr);
		return this.handlers.length-1;
	},

	handle: function(/*dojo.logging.Record*/rec){
		// summary:
		//		if the Record survives filtering, pass it down to the
		//		registered handlers. Returns a boolean indicating whether or
		//		not the record was successfully handled. If the message is
		//		culled for some reason, returns false.
		if((!this.filter(rec))||(rec.level<this.cutOffLevel)){ return false; } // boolean
		for(var x=0; x<this.handlers.length; x++){
			if(this.handlers[x]["handle"]){
			   this.handlers[x].handle(rec);
			}
		}
		// FIXME: not sure what to do about records to be propagated that may have
		// been modified by the handlers or the filters at this logger. Should
		// parents always have pristine copies? or is passing the modified record
		// OK?
		// if((this.propagate)&&(this.parent)){ this.parent.handle(rec); }
		return true; // boolean
	},

	// the heart and soul of the logging system
	log: function(/*integer*/lvl, /*string*/msg){
		// summary:
		//		log a message at the specified log level
		if(	(this.propagate)&&(this.parent)&&
			(this.parent.rec.level>=this.cutOffLevel)){
			this.parent.log(lvl, msg);
			return false;
		}
		// FIXME: need to call logging providers here!
		this.handle(new dojo.logging.Record(lvl, msg));
		return true;
	},

	// logger helpers
	debug:function(/*string*/msg){
		// summary:
		//		log the msg and any other arguments at the "debug" logging
		//		level.
		return this.logType("DEBUG", this._argsToArr(arguments));
	},

	info: function(msg){
		// summary:
		//		log the msg and any other arguments at the "info" logging
		//		level.
		return this.logType("INFO", this._argsToArr(arguments));
	},

	warning: function(msg){
		// summary:
		//		log the msg and any other arguments at the "warning" logging
		//		level.
		return this.logType("WARNING", this._argsToArr(arguments));
	},

	error: function(msg){
		// summary:
		//		log the msg and any other arguments at the "error" logging
		//		level.
		return this.logType("ERROR", this._argsToArr(arguments));
	},

	critical: function(msg){
		// summary:
		//		log the msg and any other arguments at the "critical" logging
		//		level.
		return this.logType("CRITICAL", this._argsToArr(arguments));
	},

	exception: function(/*string*/msg, /*Error*/e, /*boolean*/squelch){
		// summary:
		//		logs the error and the message at the "exception" logging
		//		level. If squelch is true, also prevent bubbling of the
		//		exception.

		// FIXME: this needs to be modified to put the exception in the msg
		// if we're on Moz, we can get the following from the exception object:
		//		lineNumber
		//		message
		//		fileName
		//		stack
		//		name
		// on IE, we get:
		//		name
		//		message (from MDA?)
		//		number
		//		description (same as message!)
		if(e){
			var eparts = [e.name, (e.description||e.message)];
			if(e.fileName){
				eparts.push(e.fileName);
				eparts.push("line "+e.lineNumber);
				// eparts.push(e.stack);
			}
			msg += " "+eparts.join(" : ");
		}

		this.logType("ERROR", msg);
		if(!squelch){
			throw e;
		}
	},

	logType: function(/*string*/type, /*array*/args){
		// summary:
		//		a more "user friendly" version of the log() function. Takes the
		//		named log level instead of the corresponding integer.
		return this.log.apply(this, [dojo.logging.log.getLevel(type), 
			args]);
	},
	
	warn:function(){
		// summary: shorthand for warning()
		this.warning.apply(this,arguments);
	},
	err:function(){
		// summary: shorthand for error()
		this.error.apply(this,arguments);
	},
	crit:function(){
		// summary: shorthand for critical()
		this.critical.apply(this,arguments);
	}
});

// the Handler class
dojo.logging.LogHandler = function(level){
	this.cutOffLevel = (level) ? level : 0;
	this.formatter = null; // FIXME: default formatter?
	this.data = [];
	this.filters = [];
}
dojo.lang.extend(dojo.logging.LogHandler,{
	
	setFormatter:function(formatter){
		dojo.unimplemented("setFormatter");
	},
	
	flush:function(){
		// summary:
		//		Unimplemented. Should be implemented by subclasses to handle
		//		finishing a transaction or otherwise comitting pending log
		//		messages to whatevery underlying transport or storage system is
		//		available.
	},
	close:function(){
		// summary:
		//		Unimplemented. Should be implemented by subclasses to handle
		//		shutting down the logger, including a call to flush()
	},
	handleError:function(){
		// summary:
		//		Unimplemented. Should be implemented by subclasses.
		dojo.deprecated("dojo.logging.LogHandler.handleError", "use handle()", "0.6");
	},
	
	handle:function(/*dojo.logging.Record*/record){
		// summary:
		//		Emits the record object passed in should the record meet the
		//		current logging level cuttof, as specified in cutOffLevel.
		if((this.filter(record))&&(record.level>=this.cutOffLevel)){
			this.emit(record);
		}
	},
	
	emit:function(/*dojo.logging.Record*/record){
		// summary:
		//		Unimplemented. Should be implemented by subclasses to handle
		//		an individual record. Subclasses may batch records and send
		//		them to their "substrate" only when flush() is called, but this
		//		is generally not a good idea as losing logging messages may
		//		make debugging significantly more difficult. Tuning the volume
		//		of logging messages written to storage should be accomplished
		//		with log levels instead.
		dojo.unimplemented("emit");
	}
});

// set aliases since we don't want to inherit from dojo.logging.Logger
void(function(){ // begin globals protection closure
	var names = [
		"setLevel", "addFilter", "removeFilterByIndex", "removeFilter",
		"removeAllFilters", "filter"
	];
	var tgt = dojo.logging.LogHandler.prototype;
	var src = dojo.logging.Logger.prototype;
	for(var x=0; x<names.length; x++){
		tgt[names[x]] = src[names[x]];
	}
})(); // end globals protection closure

dojo.logging.log = new dojo.logging.Logger();

// an associative array of logger objects. This object inherits from
// a list of level names with their associated numeric levels
dojo.logging.log.levels = [ {"name": "DEBUG", "level": 1},
						   {"name": "INFO", "level": 2},
						   {"name": "WARNING", "level": 3},
						   {"name": "ERROR", "level": 4},
						   {"name": "CRITICAL", "level": 5} ];

dojo.logging.log.loggers = {};

dojo.logging.log.getLogger = function(/*string*/name){
	// summary:
	//		returns a named dojo.logging.Logger instance. If one is not already
	//		available with that name in the global map, one is created and
	//		returne.
	if(!this.loggers[name]){
		this.loggers[name] = new dojo.logging.Logger();
		this.loggers[name].parent = this;
	}
	return this.loggers[name]; // dojo.logging.Logger
}

dojo.logging.log.getLevelName = function(/*integer*/lvl){
	// summary: turns integer logging level into a human-friendly name
	for(var x=0; x<this.levels.length; x++){
		if(this.levels[x].level == lvl){
			return this.levels[x].name; // string
		}
	}
	return null;
}

dojo.logging.log.getLevel = function(/*string*/name){
	// summary: name->integer conversion for log levels
	for(var x=0; x<this.levels.length; x++){
		if(this.levels[x].name.toUpperCase() == name.toUpperCase()){
			return this.levels[x].level; // integer
		}
	}
	return null;
}

// a default handler class, it simply saves all of the handle()'d records in
// memory. Useful for attaching to with dojo.event.connect()

dojo.declare("dojo.logging.MemoryLogHandler", 
	dojo.logging.LogHandler,
	{
		initializer: function(level, recordsToKeep, postType, postInterval){
			// mixin style inheritance
			dojo.logging.LogHandler.call(this, level);
			// default is unlimited
			this.numRecords = (typeof djConfig['loggingNumRecords'] != 'undefined') ? djConfig['loggingNumRecords'] : ((recordsToKeep) ? recordsToKeep : -1);
			// 0=count, 1=time, -1=don't post TODO: move this to a better location for prefs
			this.postType = (typeof djConfig['loggingPostType'] != 'undefined') ? djConfig['loggingPostType'] : ( postType || -1);
			// milliseconds for time, interger for number of records, -1 for non-posting,
			this.postInterval = (typeof djConfig['loggingPostInterval'] != 'undefined') ? djConfig['loggingPostInterval'] : ( postType || -1);
		},
		emit: function(record){
			if(!djConfig.isDebug){ return; }
			var logStr = String(dojo.log.getLevelName(record.level)+": "
						+record.time.toLocaleTimeString())+": "+record.message;
			if(!dj_undef("println", dojo.hostenv)){
				dojo.hostenv.println(logStr, record.msgArgs);
			}
			
			this.data.push(record);
			if(this.numRecords != -1){
				while(this.data.length>this.numRecords){
					this.data.shift();
				}
			}
		}
	}
);

dojo.logging.logQueueHandler = new dojo.logging.MemoryLogHandler(0,50,0,10000);

dojo.logging.log.addHandler(dojo.logging.logQueueHandler);
dojo.log = dojo.logging.log;
