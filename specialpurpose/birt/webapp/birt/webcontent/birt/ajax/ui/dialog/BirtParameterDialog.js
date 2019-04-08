/******************************************************************************
 *	Copyright (c) 2004 Actuate Corporation and others.
 *	All rights reserved. This program and the accompanying materials 
 *	are made available under the terms of the Eclipse Public License v1.0
 *	which accompanies this distribution, and is available at
 *		http://www.eclipse.org/legal/epl-v10.html
 *	
 *	Contributors:
 *		Actuate Corporation - Initial implementation.
 *****************************************************************************/
 
/**
 *	BirtParameterDialog
 *	...
 */
BirtParameterDialog = Class.create( );

BirtParameterDialog.prototype = Object.extend( new AbstractParameterDialog( ),
{
	/**
	 *	Parameter dialog working state. Whether embedded inside
	 *	designer dialog.
	 */
	__mode : Constants.SERVLET_FRAMESET,

	/**
	 *	Identify the parameter value is null.
	 */
	__isnull : '__isnull',

	/**
	 *	Identify the parameter value list is null.
	 */
	__isnulllist : '__isnulllist',

	/**
	 *	Prefix that identify the parameter is to set Display Text for "select" parameter
	 */
	__isdisplay : '__isdisplay__',
	
	/**
	 * identify the parameter value is a locale string
	 */
	__islocale : '__islocale',

	/**
	 * Prefix that identify the parameter value is a locale string
	 */	
	__prefix_islocale : '__islocale__',
	
    /**
	 *	Event handler closures.
	 */
	 __neh_change_cascade_text_closure : null,
	 __neh_mouseover_select_closure : null,
	 __neh_mouseout_select_closurre : null,

    /**
	 *	Check if parameter is required or not.
	 */
	__is_parameter_required : null,
	
	/**
	 *	Check if allow parameter blank or not.
	 */
	__is_parameter_allowblank : null,
	
	/*
	* Clear the sub cascading parameters
	*/
	__clearSubCascadingParameter : null,
	
	/**
	 * Mutex: counts the pending calls to __E_CASCADING_PARAMETER
	 *   
	 * >0 if a cascading value has just been changed.
	 * Used to defer the ok button clicks until
	 * a response has been received.
	 */
	__pendingCascadingCalls : 0,
	
	/**
	 * Function to call after the dialog data has been updated.
	 * Use to defer the click to ok whenever
	 * an onchange event is still pending.
	 */
	__onDataChanged : null,
	
	/**
	 * Cancels the next onchange event.
	 * Used for an IE select box behaviour workaround where pressing
	 * a key fires onchange events. It makes the select box
	 * behaviour similar to the one of FireFox. 
	 */
	__cancelOnChange : false,
	
	/**
	 * Cancels the show operation.
	 * This flag is	used when the user clicked the cancel button and
	 * wants to close the dialog box, and there are pending server requests.
	 * The bind() method of the base class will call the __l_show() method
	 * after receiving the response, which would popup the dialog again.
	 * This flag will prevent this to happen.
	 */
	__cancelShow : false,
	
	/**
	 * Stores the selected index from the focused select box. (IE only)
	 */
	__currentSelectedIndex : null,
	
	
	MIN_MULTILINES : 5,
	MAX_MULTLIINES : 10,
	
    /**
	 *	if previous is visible.
	 */	 
	 preVisible: null, 
	 
	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *	@return, void
	 */
	initialize : function( id, mode )
	{
		this.__mode = mode || Constants.request.servletPath;
		this.preVisible = false;

		this._hint = document.getElementById( "birt_hint" );
		
		// Change event for parameter text field
		this.__neh_change_cascade_text_closure = this.__neh_change_cascade_text.bindAsEventListener( this );
		this.__neh_change_select_closure = this.__neh_change_select.bindAsEventListener( this );
		this.__neh_change_cascade_select_closure = this.__neh_change_cascade_select.bindAsEventListener( this );
				
		if ( BrowserUtility.isIE6 )
		{
			// Mouse over event for Select field
			this.__neh_mouseover_select_closure = this.__neh_mouseover_select.bindAsEventListener( this );
			this.__neh_mouseout_select_closure = this.__neh_mouseout_select.bindAsEventListener( this );
		}
		
		if ( BrowserUtility.isIE )
		{
			// Focus events
			this.__neh_focus_select_closure = this.__neh_focus_select.bindAsEventListener( this );
			this.__neh_blur_select_closure = this.__neh_blur_select.bindAsEventListener( this );
		}
			    
	    this.initializeBase( id );
	    
		if ( this.__mode == Constants.SERVLET_PARAMETER )
		{
			// Hide dialog title bar if embedded in designer.
			this.__setTitleBarVisibile(false);
		}
	    
	    this.__local_installEventHandlers_extend( id );
	},

	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, toolbar id (optional since there is only one toolbar)
	 *	@return, void
	 */
	__local_installEventHandlers_extend : function( id )
	{
		// Observe "keydown" event
		this.keydown_closure = this.__neh_keydown.bindAsEventListener( this );
		Event.observe( $(id), 'keydown', this.keydown_closure, false );
		
		var oSC = document.getElementById( "parameter_table" ).getElementsByTagName( "select" );
		for( var i = 0; i < oSC.length; i++ )
		{
			var element = oSC[i];
			Event.observe( element, 'change', this.__neh_change_select_closure, false );
			
			if ( BrowserUtility.isIE6 )
			{
				Event.observe( element, 'mouseover', this.__neh_mouseover_select_closure, false );
				Event.observe( element, 'mouseout', this.__neh_mouseout_select_closure, false );
			}
			if ( BrowserUtility.isIE )
			{
				Event.observe( element, 'focus', this.__neh_focus_select_closure, false );
				Event.observe( element, 'blur', this.__neh_blur_select_closure, false );
			}

			// Set initial hint
			if ( element.selectedIndex >= 0 )
			{
				element.title = element.options[element.selectedIndex].text;
			}
			
			// Set size for multi-value parameter
			if( element.multiple )
			{				
				var len = oSC[i].options.length;				
				if( len < this.MIN_MULTILINES )
				{
					len = this.MIN_MULTILINES;
				}
				else if( len > this.MAX_MULTLIINES )
				{
					len = this.MAX_MULTLIINES;
				}
				
				element.size = len;
			}
		}
	},
	
	/**
	 *	Binding data to the dialog UI. Data includes zoom scaling factor.
	 *	@data, data DOM tree (schema TBD)
	 *	@return, void
	 */
	__bind : function( data )
	{
		if ( !data )
		{
			return;
		}
		
		var cascadeParamObj = data.getElementsByTagName( 'CascadeParameter' );
		var confirmObj = data.getElementsByTagName( 'Confirmation' );
		if ( cascadeParamObj.length > 0 )
		{
			this.__propogateCascadeParameter( data );
		}
		else if ( confirmObj.length > 0 )
		{
			this.__close( );
		}
		
		// call the internal onDataChanged event handler
		if ( this.__pendingCascadingCalls > 0 )
		{
			this.__pendingCascadingCalls--;
		}
		
		if ( this.__pendingCascadingCalls == 0 && this.__onDataChanged )
		{
			this.__pendingCascadingCalls = 0;
			var callback = this.__onDataChanged; 
			this.__onDataChanged = null;
			// add the handler in the browser's event queue
			window.setTimeout( callback, 0 );
		}
	},

	/**
	 *	Install the event handlers for cascade parameter.
	 *
	 *	@table_param, container table object.
	 *	@counter, index of possible cascade parameter.
	 *	@return, void
	 */
	__install_cascade_parameter_event_handler : function( table_param, counter )
	{
		var oSC = table_param.getElementsByTagName( "select" );
		var matrix = new Array( );
		var m = 0;
		
		var oTRC = table_param.getElementsByTagName( "TR" );
		for( var i = 0; i < oTRC.length; i++ )
		{
			var oSelect = oTRC[i].getElementsByTagName( "select" );
			var oInput = oTRC[i].getElementsByTagName( "input" );
			var oCascadeFlag = "";
			
			if ( oInput && oInput.length > 0 )
			{
				var oLastInput = oInput[oInput.length - 1];
				if ( oLastInput.id == "isCascade" )
					oCascadeFlag = oLastInput.value;
			}
						
			// find select items to install event listener
			if( oSelect.length > 0 && oCascadeFlag == "true" )
			{
				if ( i < oTRC.length - 1 )
				{
					Event.observe( oSelect[0], 'change', this.__neh_change_cascade_select_closure, false );
					
					// find text item to install event listener
					var oText;
					for( var j = 0; j < oInput.length; j++ )
					{
						if( oInput[j].type == "text" )
						{
							oText = oInput[j];
							break;
						}
					}
					if( oText )
					{
						Event.observe( oText, 'change', this.__neh_change_cascade_text_closure, false );
					}
				}
				
				if( !matrix[m] )
				{
					matrix[m] = {};
				}
				
				var name = oSelect[0].id.substr( 0, oSelect[0].id.length - 10 )
				var value = oSelect[0].value;
				if ( value == Constants.nullValue )
				{
					matrix[m].name = this.__isnull;
					matrix[m++].value = name;
				}
				else
				{
					matrix[m].name = name;
					matrix[m++].value = oSelect[0].value;
				}
			}
		}
		
		this.__cascadingParameter[counter] = matrix;
	},
	
	/**
	 *	Collect parameters, Support ComboBox/Listbox,Hidden,Radio,TextBox,Checkbox.
	 *
	 *	@return, void
	 */
	collect_parameter : function( )
	{
		// Clear parameter array
		this.__parameter = new Array( );		
				
		var k = 0;
		//oTRC[i] is <tr></tr> section
		var oTRC = document.getElementById( "parameter_table" ).getElementsByTagName( "TR" );
		for( var i = 0; i < oTRC.length; i++ )
		{
			if( !this.__parameter[k] )
			{
				this.__parameter[k] = { };
			}
			
			//input element collection
			var oIEC = oTRC[i].getElementsByTagName( "input" );
			//select element collection
			var oSEC = oTRC[i].getElementsByTagName( "select" );
			//avoid group parameter
			var oTable = oTRC[i].getElementsByTagName( "table" );
			if( oTable.length > 0 || ( oSEC.length == 0 && oIEC.length == 0 ) || ( oIEC.length == 1 && oIEC[0].type == 'submit' ) )
			{
				continue;
			}
			
			// control type
			var oType = oIEC[0].value;

			// deal with "hidden" parameter
			if( oType == 'hidden' )
			{
				var temp = oIEC[1];
				this.__parameter[k].name = temp.name;
				this.__parameter[k].value = temp.value;
				k++;
				
				// set display text
				if( !this.__parameter[k] )
				{
					this.__parameter[k] = { };
				}
				this.__parameter[k].name = this.__isdisplay + this.__parameter[k-1].name;
				this.__parameter[k].value = oIEC[2].value;
				k++;		
				
				continue;
			}
			
			// deal with "checkbox" parameter
			if( oType == 'checkbox' )
			{
				var temp = oIEC[2];
				this.__parameter[k].name = temp.value;
				temp.checked?this.__parameter[k].value = 'true':this.__parameter[k].value = 'false';  
				k++;
				continue;
			}
						
			// deal with "text" parameter
			if( oType == 'text' )
			{
				// data type of current parameter
				var dataType = oIEC[1].value;
				
				// allow null
				if( oIEC[2] && oIEC[2].type == 'radio' )
				{
					if( oIEC[2].checked )
					{
						var paramName = oIEC[3].name;
						var paramValue = oIEC[4].value;
						var displayText = oIEC[5].value;

						if( displayText != oIEC[3].value )
						{
							// change the text field value,regard as a locale string
							paramValue = oIEC[3].value;
							
							// set isLocale flag							
							this.__parameter[k].name = this.__islocale;
							this.__parameter[k].value = paramName;
							k++;	
						}
																		
						// check if required
						if( this.__is_parameter_required( oIEC ) 
							&& birtUtility.trim( paramValue ) == '' && this.visible )
						{
							oIEC[3].focus( );
							alert( birtUtility.formatMessage( Constants.error.parameterRequired, paramName ) );
							return false;
						}
						
						// check if allow blank
						if( !this.__is_parameter_allowblank( dataType )
							&& birtUtility.trim( paramValue ) == '' && this.visible )
						{
							oIEC[3].focus( );
							alert( birtUtility.formatMessage( Constants.error.parameterNotAllowBlank, paramName ) );
							return false;							
						}	
													
						// set parameter value
						if( !this.__parameter[k] )
						{
							this.__parameter[k] = { };
						}
						this.__parameter[k].name = paramName;
						this.__parameter[k].value = paramValue;
						k++;
						
						// set display text
						if( !this.__parameter[k] )
						{
							this.__parameter[k] = { };
						}
						this.__parameter[k].name = this.__isdisplay + this.__parameter[k-1].name;
						this.__parameter[k].value = oIEC[3].value;
						k++;						
					}
					else
					{
						// select null value
						this.__parameter[k].name = this.__isnull;
						this.__parameter[k].value = oIEC[2].value;
						k++;
					}										
				}
				// not allow null
				else
				{
					var paramName = oIEC[2].name;
					var fieldValue = oIEC[2].value;
					var paramValue = oIEC[3].value;
					var displayText = oIEC[4].value;

					// convert spaces to non-breakable spaces if data type is a number					
					if ( this.__is_parameter_number( dataType ) )
					{
						fieldValue = this.__convert_spaces_to_nbsp( fieldValue );
					}
					
					if( displayText != fieldValue )
					{
						// change the text field value,regard as a locale string
						paramValue = fieldValue;
						
						// set isLocale flag							
						this.__parameter[k].name = this.__islocale;
						this.__parameter[k].value = paramName;
						k++;	
					}
															
					// check if required
					if( this.__is_parameter_required( oIEC ) 
						&& birtUtility.trim( paramValue ) == '' && this.visible )
					{
						oIEC[2].focus( );
						alert( birtUtility.formatMessage( Constants.error.parameterRequired, paramName ) );
						return false;
					}

					// check if allow blank
					if( !this.__is_parameter_allowblank( dataType )
						&& birtUtility.trim( paramValue ) == '' && this.visible )
					{
						oIEC[2].focus( );
						alert( birtUtility.formatMessage( Constants.error.parameterNotAllowBlank, paramName ) );
						return false;							
					}	
												
					// set parameter value
					if( !this.__parameter[k] )
					{
						this.__parameter[k] = { };
					}
					this.__parameter[k].name = paramName;
					this.__parameter[k].value = paramValue;
					k++;
						
					// set display text
					if( !this.__parameter[k] )
					{
						this.__parameter[k] = { };
					}
					this.__parameter[k].name = this.__isdisplay + this.__parameter[k-1].name;
					this.__parameter[k].value = fieldValue;
					k++;					
				}
				
				continue;
			}
			
			// deal with "radio" parameter
			if( oType == 'radio' )
			{
				var dataType = oIEC[1].value;
				
				if( oIEC.length > 2 )
				{
					for( var j = 2; j < oIEC.length; j++ )
					{
						// deal with radio
						if( oIEC[j].type == 'radio' && oIEC[j].checked )
						{
							// null value
							if( oIEC[j].id == oIEC[j].name + "_null" )
							{
								this.__parameter[k].name = this.__isnull;
								this.__parameter[k].value = oIEC[j].name;
								k++;
							}
							else
							{
								// check if allow blank
								if( !this.__is_parameter_allowblank( dataType )
									&& birtUtility.trim( oIEC[j].value ) == '' && this.visible )
								{
									oIEC[j].focus( );
									alert( birtUtility.formatMessage( Constants.error.parameterNotAllowBlank, oIEC[j].name ) );
									return false;							
								}	
							
								// common radio value
								this.__parameter[k].name = oIEC[j].name;
								this.__parameter[k].value = oIEC[j].value;	
								k++;
								
								// set display text for the "radio" parameter
								var displayLabel = document.getElementById( oIEC[j].id + "_label" );
								if( displayLabel )
								{							
									if( !this.__parameter[k] )
									{
										this.__parameter[k] = { };
									}
									this.__parameter[k].name = this.__isdisplay + this.__parameter[k-1].name;
									this.__parameter[k].value = displayLabel.title;
									k++;			
								}
							}	
							
							break;								
						}	
					}
				}
								
				continue;		
			}
			
			// deal with "select" parameter
			if( oType == 'select' && oSEC.length == 1 )
			{
				var dataType = oIEC[1].value;
				var paramName = oIEC[2].name;
				
				var flag = true;
				if( oIEC[3] && oIEC[3].type == 'radio' && !oIEC[3].checked )
				{
					flag = false;
				}
				
				// need check selection
				if( flag )
				{
					if ( this.__is_parameter_required( oIEC ) && oSEC[0].selectedIndex < 0 && this.visible )
					{
						oSEC[0].focus( );
						alert( birtUtility.formatMessage( Constants.error.parameterNotSelected, paramName ) );
						return false;
					}
																									
					if( oSEC[0].multiple )
					{
						var options = oSEC[0].options;
						for( var l = 0; l < options.length; l++ )
						{
							if( !options[l].selected )
								continue;
							
							var tempText = options[l].text;
							var tempValue = options[l].value;
															
							// check if isRequired
							if( this.__is_parameter_required( oIEC ) 
								&& birtUtility.trim( tempValue ) == '' && this.visible )
							{
								oSEC[0].focus( );
								alert( birtUtility.formatMessage( Constants.error.parameterRequired, paramName ) );
								return false;									
							}
							
							if( tempValue == Constants.nullValue )
								continue;
								
							// check if allow blank
							if( !this.__is_parameter_allowblank( dataType )
								&& birtUtility.trim( tempValue ) == '' && this.visible )
							{
								oSEC[0].focus( );
								alert( birtUtility.formatMessage( Constants.error.parameterNotAllowBlank, paramName ) );
								return false;							
							}									
						}
					}
					else
					{
						var tempText = oSEC[0].options[oSEC[0].selectedIndex].text;
						var tempValue = oSEC[0].options[oSEC[0].selectedIndex].value;
											
						// check if isRequired
						if ( this.__is_parameter_required( oIEC )
							 && birtUtility.trim( tempValue ) == '' && this.visible )
						{
							oSEC[0].focus( );
							alert( birtUtility.formatMessage( Constants.error.parameterRequired, paramName ) );
							return false;
						}

						// Check if select 'Null Value' option for single parameter
						if( tempValue == Constants.nullValue )
						{
							this.__parameter[k].name = this.__isnull;
							this.__parameter[k].value = paramName;
							k++;	
							continue;
						}
							
						// check if allow blank
						if( !this.__is_parameter_allowblank( dataType )
							&& birtUtility.trim( tempValue ) == '' && this.visible )
						{
							oSEC[0].focus( );
							alert( birtUtility.formatMessage( Constants.error.parameterNotAllowBlank, paramName ) );
							return false;							
						}							
					}					
				}
				
				// allow new value
				if( oIEC[3] && oIEC[3].type == 'radio' )
				{					
					if( oIEC[3].checked )
					{
						// select value
						var tempText = oSEC[0].options[oSEC[0].selectedIndex].text;
						var tempValue = oSEC[0].options[oSEC[0].selectedIndex].value;
											
						// set value
						this.__parameter[k].name = paramName;
						this.__parameter[k].value = tempValue;
						k++;
						
						// set display text
						if( !this.__parameter[k] )
						{
							this.__parameter[k] = { };
						}
						this.__parameter[k].name = this.__isdisplay + this.__parameter[k-1].name;
						this.__parameter[k].value = tempText;
						k++;						
					}
					else
					{
						var inputValue = oIEC[5].value;
						var paramValue = oIEC[2].value;
						var displayText = oIEC[6].value;
						
						// if change the text field value or input text field isn't focus default,regard as a locale string 
						if( displayText != inputValue || oIEC[5].name.length <= 0 )
						{							
							paramValue = inputValue;
							
							// set isLocale flag							
							this.__parameter[k].name = this.__islocale;
							this.__parameter[k].value = paramName;
							k++;	
						}
						
						// check if isRequired
						if ( this.__is_parameter_required( oIEC ) 
							 && birtUtility.trim( paramValue ) == '' && this.visible )
						{
							oIEC[5].focus( );
							alert( birtUtility.formatMessage( Constants.error.parameterRequired, paramName ) );
							return false;
						}						

						// check if allow blank
						if( !this.__is_parameter_allowblank( dataType )
							&& birtUtility.trim( paramValue ) == '' && this.visible )
						{
							oIEC[5].focus( );
							alert( birtUtility.formatMessage( Constants.error.parameterNotAllowBlank, paramName ) );
							return false;							
						}	
						
						// set value
						if( !this.__parameter[k] )
						{
							this.__parameter[k] = { };
						}
						this.__parameter[k].name = paramName;
						this.__parameter[k].value = paramValue;
						k++;
											
						// set display text
						if( !this.__parameter[k] )
						{
							this.__parameter[k] = { };
						}
						this.__parameter[k].name = this.__isdisplay + this.__parameter[k-1].name;
						this.__parameter[k].value = inputValue;
						k++;						
					}
				}
				else
				{
					// don't allow new value
					
					// multi-value case
					if( oSEC[0].multiple )
					{
						// allow multi value
						var options = oSEC[0].options;
						// record the old length
						var ck = k;
						
						for( var l = 0; l < options.length; l++ )
						{
							if( !options[l].selected )
								continue;
							
							var tempText = options[l].text;
							var tempValue = options[l].value;
							
							// Check if select 'Null Value' option
							if( tempValue == Constants.nullValue )
							{
								if( !this.__parameter[k] )
								{
									this.__parameter[k] = { };
								}
							
								this.__parameter[k].name = this.__isnull;
								this.__parameter[k].value = paramName;
								k++;	
								continue;
							}		

							// set value
							if( !this.__parameter[k] )
							{
								this.__parameter[k] = { };
							}
							this.__parameter[k].name = paramName;
							this.__parameter[k].value = tempValue;
							k++;
						
							// set display text
							if( !this.__parameter[k] )
							{
								this.__parameter[k] = { };
							}
							this.__parameter[k].name = this.__isdisplay + this.__parameter[k-1].name;
							this.__parameter[k].value = tempText;
							k++;
						}
						
						// compare the length, if no any selection processed, should be an empty list.
						if ( ck == k )
						{
							if( !this.__parameter[k] )
							{
								this.__parameter[k] = { };
							}
						
							this.__parameter[k].name = this.__isnulllist;
							this.__parameter[k].value = paramName;
							k++;	
						}
					}
					else
					{
						// allow single value
						var tempText = oSEC[0].options[oSEC[0].selectedIndex].text;
						var tempValue = oSEC[0].options[oSEC[0].selectedIndex].value;
													
						// set value
						this.__parameter[k].name = paramName;
						this.__parameter[k].value = tempValue;
						k++;
						
						// set display text
						if( !this.__parameter[k] )
						{
							this.__parameter[k] = { };
						}
						this.__parameter[k].name = this.__isdisplay + this.__parameter[k-1].name;
						this.__parameter[k].value = tempText;
						k++;
					}					
				}
				
				continue;
			}			
		}
		
		return true;
	},

	/**
	 *	Check if current parameter is required or not.
	 *
	 *	@oInputs, Input control collection 
	 *	@return, true or false
	 */
	__is_parameter_required : function( oInputs )
	{
		if( !oInputs || oInputs.length <= 0 )
			return false;
		
		var flag = false;		
		for( var i = 0; i< oInputs.length; i++ )
		{
			// if find defined input control
			if( oInputs[i].id == 'isRequired' && oInputs[i].value == 'true' )
			{
				flag = true;
				break;		
			}
		}
		
		return flag;
	},

	/**
	 *	Check if current parameter allows blank value.
	 *  Currently, only any and string data type parameter can allow blank value.
	 *
	 *	@dataType, data type for parameter 
	 *	@return, true or false
	 */
	__is_parameter_allowblank : function( dataType )
	{
		if( !dataType )
			return false;
		
		if( dataType == Constants.TYPE_ANY )
			return true;
		
		if( dataType == Constants.TYPE_STRING )
			return true;

		return false;		
	},
	
	/**
	 *	Check if current parameter is a number.
	 *
	 *	@dataType data type for parameter 
	 *	@return true or false
	 */
	__is_parameter_number : function( dataType )
	{
		if( !dataType )
			return false;
		
		return ( dataType == Constants.TYPE_FLOAT				
				|| dataType == Constants.TYPE_DECIMAL
				|| dataType == Constants.TYPE_INTEGER );		
	},
	
	/**
	 * Converts the spaces to non-breakable spaces (unicode 0x00a0).
	 * This is mandatory for numbers which use a space separator,
	 * because the server-side parser expects it.
	 * @param value formatted string to process
	 * @return processed string
	 */
	__convert_spaces_to_nbsp : function( aValue )
	{
		var value = aValue;
		var startIndex = value.search(/\d/);
		if ( startIndex < 0 )
		{
			return value;
		}
		
		var endIndex = value.search(/\d\D*$/);
		if ( endIndex < 0 )
		{
			endIndex = value.length;
		}
		
		var prefix = value.substring(0, startIndex);
		var suffix = value.substr(endIndex + 1);
		var number = value.substring(startIndex, endIndex + 1).replace(/ /g,"\u00a0");
		
		
		return prefix + number + suffix;
	},	
	
	/**
	 *	Handle mouseover event on select.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_mouseover_select : function( event )
	{	
		var oSC = Event.element( event );
		var tempText;
		if( oSC.selectedIndex >=0 )
			tempText = oSC.options[oSC.selectedIndex].text;
		
		if( tempText && this._hint )
		{
			this._hint.innerHTML = tempText;
			this._hint.style.display = "block";
			this._hint.style.left = ( event.clientX - parseInt( this.__instance.style.left ) ) + "px";
			this._hint.style.top = ( 15 + event.clientY - parseInt( this.__instance.style.top ) ) + "px";
		}			
	},

	/**
	 *	Handle mouseout event on select.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_mouseout_select : function( event )
	{
		if( this._hint )
		{
			this._hint.style.display = "none"; 
		}			
	},

	/**
	 *	Handle focus event on select elements.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */

	__neh_focus_select : function( event )
	{
		var el = Event.element( event );
		if ( el )
		{
			this.__currentSelectedIndex = el.selectedIndex;
		}
		else
		{
			this.__currentSelectedIndex = -2;
		}
	},
	
	/**
	 *	Handle blur event on select elements.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */	
	__neh_blur_select : function( event )
	{	
		var el = Event.element( event );

		// prevents firing onchange twice, if the previous onchange has 
		// already updated the current selected index
		if ( el && el.selectedIndex != this.__currentSelectedIndex)
		{
			this.__neh_change_cascade_select( event );
		}
		this.__currentSelectedIndex = -2;
	},
	
	/**
	 *	Handle change event when clicking on select.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */	
	__neh_change_select : function( event )
	{
		var element = Event.element( event );
		if ( element.selectedIndex >= 0 )
		{
			element.title = element.options[element.selectedIndex].text;
		}
		else
		{
			element.title = "";
		}
	},

	
	/**
	 *	Handle change event when clicking on cascading select.
	 *
	 *	@event incoming browser native event
	 *	@return
	 */
	__neh_change_cascade_select : function( event )
	{
		if ( this.__cancelOnChange )
		{
			/**
			 * Cancel event because of keyboard selection.
			 * Event will be fired later when the element loses focus (like in Firefox)
			 */
			this.__cancelOnChange = false;
			return;
		}

		var element = Event.element( event );		
		this.__currentSelectedIndex = element.selectedIndex;
		this.__refresh_cascade_select(element);
	},
	
	/**
	 *	Refreshes the cascading elements following the given element.
	 */
	__refresh_cascade_select : function( element )
	{
	    var matrix = new Array( );
	    var m = 0;
        for( var i = 0; i < this.__cascadingParameter.length; i++ )
        {
            for( var j = 0; j < this.__cascadingParameter[i].length; j++ )
            {
            	var paramName = this.__cascadingParameter[i][j].name;
            	if( paramName == this.__isnull )
            		paramName = this.__cascadingParameter[i][j].value;
            		
                if( paramName == element.id.substr( 0, element.id.length - 10 ) )
                {
                	var tempText = element.options[element.selectedIndex].text;
					var tempValue = element.options[element.selectedIndex].value;
					 
            		// Null Value Parameter
					if ( tempValue == Constants.nullValue )
					{
            			this.__cascadingParameter[i][j].name = this.__isnull;
            			this.__cascadingParameter[i][j].value = paramName;
					}					
					else if( tempValue == '' )
                	{
                		if( tempText == "" )
                		{
                			var target = element;
							target = target.parentNode;
							var oInputs = target.getElementsByTagName( "input" );
							if( oInputs.length >0 && oInputs[1].value != Constants.TYPE_STRING )
							{
								// Only String parameter allows blank value
								alert( birtUtility.formatMessage( Constants.error.parameterNotAllowBlank, paramName ) );
								this.__clearSubCascadingParameter( this.__cascadingParameter[i], j );
								return;
							}
							else
							{
	                			// Blank Value
	                			this.__cascadingParameter[i][j].name = paramName;
	                	    	this.__cascadingParameter[i][j].value = tempValue;								
							}
                		}
                		else
                		{
                			// Blank Value
                			this.__cascadingParameter[i][j].name = paramName;
                	    	this.__cascadingParameter[i][j].value = tempValue;
                		}						
                	}
                	else
                	{
                		this.__cascadingParameter[i][j].name = paramName;
                	    this.__cascadingParameter[i][j].value = tempValue;
                	}
                	
                    for( var m = 0; m <= j; m++ )
                    {
					    if( !matrix[m] )
				        {
				            matrix[m] = {};
				        }
				        matrix[m].name = this.__cascadingParameter[i][m].name;
				        matrix[m].value = this.__cascadingParameter[i][m].value;
				    }
				    this.__pendingCascadingCalls++;
                    birtEventDispatcher.broadcastEvent( birtEvent.__E_CASCADING_PARAMETER, matrix );
                }
            }
        }
	},

	/**
	 * Clear the sub cascading parameter for the elements
	 * following the given element
	 * @param element element
	 */
	__clearSubCascadingParameterByName: function(parameterName)
	{
        for( var i = 0; i < this.__cascadingParameter.length; i++ )
        {
            for( var j = 0; j < this.__cascadingParameter[i].length; j++ )
            {
            	var paramName = this.__cascadingParameter[i][j].name;
                if( paramName == parameterName )
                {
    				this.__clearSubCascadingParameter( this.__cascadingParameter[i], j );                	
                }
            }
        }
	},
	
	/**
	 *	Clear the sub cascading parameter.
	 *
	 *  @cascadingParameterGroup
	 *  @index
	 *	@return, void
	 */	
	__clearSubCascadingParameter : function( cascadingParameterGroup, index )
	{
		for( var i = index + 1; i < cascadingParameterGroup.length; i++ )
		{
			var param_name = cascadingParameterGroup[i].name;
			if( param_name == this.__isnull )
				param_name = cascadingParameterGroup[i].value;
				
			var selection = document.getElementById( param_name + "_selection" );
			var len = selection.options.length;
			
			// Clear our selection list.
			for( var j = 0; j < len; j++ )
			{
				selection.remove( 0 );
			}
		}	
	},
	
	/**
	 *	Handle clicking on radio.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */	
	__neh_click_radio : function( event )
	{
		var temp = Event.element( event );
		var oInput = temp.parentNode.getElementsByTagName( "input" );
		var oSelect = temp.parentNode.getElementsByTagName( "select" );
		
		// check if current parameter is cascading parameter
		var oCascadeFlag = false;
		if ( oInput && oInput.length > 0 )
		{
			var oLastInput = oInput[oInput.length - 1];
			if ( oLastInput.id == "isCascade" )
				oCascadeFlag = ( oLastInput.value == "true" );
		}
		
		var oSelectElement = oSelect[0];
		
		for( var i = 0; i < oInput.length; i++ )
		{
			if( oInput[i].id == temp.id )
			{
				var element = oInput[i+1]; 
				//enable the next component
				oInput[i].checked = true;
				if( element && ( element.type == "text" || element.type == "password" ) )
				{
					element.disabled = false;
					element.focus( );
					if ( oCascadeFlag )
					{
						// refresh cascading elements (remove the "_input" suffix)
						this.__clearSubCascadingParameterByName(element.id.substr(0, element.id.length - 6));
					}
				}
				else if( oSelectElement )
				{
					oSelectElement.selectedIndex = 0;
					oSelectElement.disabled = false;
					oSelectElement.focus( );
					if ( oCascadeFlag )
					{
						// refresh cascading elements (remove the "_selection" suffix)
						this.__clearSubCascadingParameterByName(oSelectElement.id.substr(0, oSelectElement.id.length - 10));
					}
				}
			}
			// if i points to the element that must be disabled
			else if( oInput[i].type == "radio" && oInput[i].id != temp.id )			
			{				
				var element = oInput[i+1];
				//disable the next component and clear the radio
				oInput[i].checked = false;
				if( element && ( element.type == "text" || element.type == "password" ) )
				{
					element.disabled = true;
					// if cascading parameter, clear value 
					if ( oCascadeFlag )
					{
						element.value = "";
					}
				}
				else if( oSelectElement )
				{
					oSelectElement.disabled = true;
					// if cascading parameter, clear value
					if ( oCascadeFlag )
					{
						oSelectElement.selectedIndex = -1;
						oSelectElement.title = "";
					}
				}
		    }
		}
	},
	
	/**
	 * Check whether obj is the last select control
	 */
	__ifLastSelect : function( obj )
	{
		if( obj )
		{
			var oTABLE = obj.parentNode.parentNode.parentNode;
			if( oTABLE )
			{
				var oSelect = oTABLE.getElementsByTagName( "select" );
				if( oSelect && oSelect.length > 0 && oSelect[oSelect.length - 1].id == obj.id )
				{
					return true;	
				}
			}
		}
		return false;
	},

	/**
	 *	Handle changing on cascading parameter text field.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_change_cascade_text : function( event )
	{	
		var temp = Event.element( event );
		this.__refresh_cascade_text(temp);
	},
	
	/**
	 * Refresh cascade elements from a text box.
	 */
	__refresh_cascade_text : function( element )
	{		
		// trim the "_text" suffix from the parameter name
		var paramName = element.id.substr( 0, element.id.length - 6 );
	    var matrix = new Array( );
	    var m = 0;
        for( var i = 0; i < this.__cascadingParameter.length; i++ )
        {
            for( var j = 0; j < this.__cascadingParameter[i].length; j++ )
            {
                if( this.__cascadingParameter[i][j].name == paramName )
                {
                    this.__cascadingParameter[i][j].value = element.value;
                    for( var m = 0; m <= j; m++ )
                    {
					    if( !matrix[m] )
				        {
				            matrix[m] = {};
				        }
				        matrix[m].name = this.__prefix_islocale + this.__cascadingParameter[i][m].name;
				        matrix[m].value = this.__cascadingParameter[i][m].value;			
				    }
				    this.__pendingCascadingCalls++;
                    birtEventDispatcher.broadcastEvent( birtEvent.__E_CASCADING_PARAMETER, matrix );
                }
            }
        }	
	},
		
	/**
	 *	Handle press "Enter" key.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_keydown: function( event )
	{
		// If press 'Enter' key
		if( event.keyCode == 13 )
		{			
			var target = Event.element( event );
						
			// Focus on INPUT and SELECT controls
			if ( target.tagName == "INPUT" || target.tagName == "SELECT" )
			{			
				// (exclude 'button' type for non-Safari browsers)
				if ( target.type != "button" )
				{
					// blur the focus to force the onchange/onselect events
					target.blur();
					// defer okPress to let those events run first
					window.setTimeout( this.__okPress.bindAsEventListener(this), 0 );				
				}
				// Safari needs explicit click
				else if ( BrowserUtility.isSafari || BrowserUtility.isKHTML )
				{
					target.click();
					// prevent browser "beep"
					Event.stop( event );					
				}
			}

		}
		// in IE, when a key is pressed on a select box, cancel the onchange event 
		else if ( BrowserUtility.isIE && event.keyCode != 9 && Event.element( event ).tagName == "SELECT" )
		{
			this.__cancelOnChange = true;
		}
	},	
		
	/**
	 *	Handle clicking on okRun.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__okPress : function( )
	{
		// if a cascading parameter just changed
		if ( this.__pendingCascadingCalls > 0 )
		{
			debug("defer okPress call");
			// defer the call to the okPress function until the data has been updated
			this.__onDataChanged = this.__okPress.bindAsEventListener(this);
			return; 
		}		

		if( birtParameterDialog.collect_parameter( ) )
		{
			// workaround for Bugzilla Bug 146566. 
			// If change parameter and re-generate docuemnt file, close TOC panel.
			if ( this.__mode == Constants.SERVLET_FRAMESET )
			{
				var oToc = $( 'display0' );
				var oDoc = $( 'Document' );
				if( oToc && oDoc )
				{ 		
					oDoc.style.width = BirtPosition.viewportWidth( ) + "px";
					oToc.style.display="none";
					oToc.query = '0';
				}
			}
		
			var action = soapURL.toLowerCase( );
			
			if ( this.__mode == Constants.SERVLET_PARAMETER )
			{
				// check whether set __nocache setting in URL
				if ( this.__ifCache( action ) )
					birtEventDispatcher.broadcastEvent( birtEvent.__E_CACHE_PARAMETER );
				else
					this.__doSubmitWithPattern( );
			}
			else if ( this.__ifSubmit( this.__mode, action ) )
			{
				this.__doSubmit( );
			}
			else
			{
				if( this.__mode == Constants.SERVLET_FRAMESET )
				{
					var targetPage = "1";					
					var bookmark = birtUtility.getURLParameter(soapURL, "bookmark");
					// if a bookmark is defined, reset the target page
					// to force the server-side to use it
					if ( bookmark && bookmark.length > 0 )
					{
						targetPage = "";
					}
					
					var oPageNumber = $( 'pageNumber' );
					if ( oPageNumber )
					{
						oPageNumber.innerHTML = targetPage;						
					}
					
					birtEventDispatcher.broadcastEvent( birtEvent.__E_CHANGE_PARAMETER );
				}
				else
				{
					// if 'run' mode, fire GetPageAll event
					this.__init_page_all( );
				}
				
				this.__l_hide( );
			}
		}
	},

	/**
	 *	Override cancel button click.
	 */
	__neh_cancel : function( )
	{
		// if cascading parameter calls are pending
		if ( this.__pendingCascadingCalls > 0 )
		{
			// prevent the response to popup the dialog again
			this.__cancelShow = true;
			
			// reset the counter for the next time the dialog is needed
			this.__pendingCascadingCalls = 0;			
		}
		
		if ( this.__mode == Constants.SERVLET_PARAMETER )
		{
			this.__cancel();
		}
		else
		{
			this.__l_hide( );
		}
	},

	/**
	 *	Handle submit form with defined servlet pattern and current parameters.
	 *
	 *	@return, void
	 */
	__doSubmitWithPattern : function( )
	{
		var url = soapURL;
		
		// parse pattern
		var reg = new RegExp( "[&|?]{1}__pattern\s*=([^&|^#]*)", "gi" );
		var arr = url.match( reg );
		var pattern;
		if( arr && arr.length > 0 )		
			pattern = RegExp.$1;
		else
			pattern = "frameset";
						
		// parse target
		reg = new RegExp( "[&|?]{1}__target\s*=([^&|^#]*)", "gi" );
		arr = url.match( reg );			
		var target;
		if( arr && arr.length > 0 )
			target = RegExp.$1;
		
		reg = new RegExp( "[^/|^?]*[?]{1}", "gi" );
		if( url.search( reg ) > -1 )
			url = url.replace( reg, pattern + "?" );
		
		this.__doSubmit( url, target );
	},
	
	/**
	 *	Handle submit form with current parameters.
	 *
	 *  @param, url
	 *  @param, target
	 *	@return, void
	 */
	__doSubmit : function( url, target )
	{
		var action = url;
		if( !action )
			action = soapURL;
		
		var divObj = document.createElement( "DIV" );
		document.body.appendChild( divObj );
		divObj.style.display = "none";
		
		var formObj = document.createElement( "FORM" );
		divObj.appendChild( formObj );
		
		if ( this.__parameter != null )
		{
			for( var i = 0; i < this.__parameter.length; i++ )	
			{
				var param = document.createElement( "INPUT" );
				formObj.appendChild( param );
				param.TYPE = "HIDDEN";
				param.name = this.__parameter[i].name;
				param.value = this.__parameter[i].value;
				
				//replace the URL parameter			
				var reg = new RegExp( "&" + param.name + "[^&]*&*", "g" );
				action = action.replace( reg, "&" );
			}
		}
		
		if ( Constants.viewingSessionId )
		{
			// append sub session in the POST part
			birtUtility.addHiddenFormField(formObj, Constants.PARAM_SESSION_ID, Constants.viewingSessionId);
			action = birtUtility.deleteURLParameter(action, Constants.PARAM_SESSION_ID);
		}
		
		// replace __parameterpage setting
		var reg = new RegExp( "([&|?]{1})(__parameterpage\s*=[^&|^#]*)","gi" );
		if ( action.search( reg ) > -1 )
		{
			action = action.replace( reg, "$1" );
		}	
		
		// set target window
		if( target )
			formObj.target = target;
			
		formObj.action = action;
		formObj.method = "post";
		
		// if don't set target, hide the parameter dialog
		if( !target )		
			this.__l_hide( );
						
		formObj.submit( );		
	},

	/**
	 *	Caching parameters success, close window.
	 *
	 *	@return, void
	 */	
	__close : function( )
	{
		if ( BrowserUtility.__isIE( ) )
		{
			window.opener = null;
			window.close( );
		}
		else
		{
			window.status = "close";
		}
	},
	
	/**
	 *	Click 'Cancel', close window.
	 *
	 *	@return, void
	 */	
	__cancel : function( )
	{
		window.status = "cancel";
		document.title = "cancel";
	},

	/**
	Called right before element is shown
	*/
	__preShow: function()
	{
		// disable the toolbar buttons
		birtUtility.setButtonsDisabled ( "toolbar", true );
		
		// disable the Navigation Bar buttons
		birtUtility.setButtonsDisabled ( "navigationBar", true );
		
		// set preVisible
		this.preVisible = this.visible;
	},

	/**
	 * Override the dialog's show method. 
	 */
	__l_show: function()
	{
		if ( !this.__cancelShow )
		{
			// call to superclass method
			AbstractParameterDialog.prototype.__l_show.call( this );
		}
		this.__cancelShow = false;
	},

	/**
	Called after element is shown
	*/
	__postShow: function()
	{	
		// if previous is visible, return directly
		if( this.preVisible )	
			return;
				
		// focus on the first input text/password or select or button control
		this.__init_focus( );
	},
	
	/**
	 * Try to focus on the first control.
	 * Input text/password, select and button.
	 */
	__init_focus: function( )
	{
		var oFirstITC;
		var oFirstIBT;
		var oFirstST;
		
		var oITCs = this.__instance.getElementsByTagName( "input" );
		for( var i = 0; i < oITCs.length; i++ )
		{
			// get the first input text/password control
			if( oITCs[i].type == "text" 
			    || oITCs[i].type == "password"  )
			{
				if( !oITCs[i].disabled && !oFirstITC )
				{
					oFirstITC = oITCs[i];
				}
				continue;
			}
			
			// get the first input button control
			if( !oFirstIBT && oITCs[i].type == "button" && !oITCs[i].disabled )
			{
				oFirstIBT = oITCs[i];
			}
		}
		
		// get the first select control
		var oSTs = this.__instance.getElementsByTagName( "select" );
		for( var i = 0; i < oSTs.length; i++ )
		{
			if( !oSTs[i].disabled )
			{
				oFirstST = oSTs[i];
				break;
			}
		}
				
		if( oFirstITC && !oFirstST )
		{
			// if exist input text/password, no select control
			oFirstITC.focus( );
		}
		else if( !oFirstITC && oFirstST )
		{
			// if exist select control, no input text/password
			oFirstST.focus( );
		}
		else if( oFirstITC && oFirstST )
		{
			// exist select control and input text/password
			// compare the parent div offsetTop
			if( oFirstITC.parentNode && oFirstST.parentNode )
			{
				// Bugzilla 265615: need to use cumulative offset for special cases
				// where one element is inside a group container
				var offsetITC = Position.cumulativeOffset( oFirstITC );
				var offsetST = Position.cumulativeOffset( oFirstST );
				
				// compare y-offset first, then x-offset to determine the visual order
				if( ( offsetITC[1] > offsetST[1] ) || ( offsetITC[1] == offsetST[1] && offsetITC[0] > offsetST[0] ) )
				{
					oFirstST.focus( );				
				}
				else
				{
					oFirstITC.focus( );
				}
			}
			else
			{
				// default to focus on input control
				oFirstITC.focus( );
			}
		}
		else
		{
			// focus on button control
			oFirstIBT.focus( );
		}		
	},
		
	/**
	Called before element is hidden
	*/
	__preHide: function( )
	{
		// enable the toolbar buttons
		birtUtility.setButtonsDisabled ( "toolbar", false );
		
		// enable the Navigation Bar buttons
		birtUtility.setButtonsDisabled ( "navigationBar", false );		
	},
	
	/**
	 * Retrieve all pages
	 */
	__init_page_all: function( )
	{
		if( birtParameterDialog.collect_parameter( ) )
		{
			birtEventDispatcher.broadcastEvent( birtEvent.__E_GETPAGE_ALL );
		}
	},

	/**
	 * Check if cache parameter, default to true
	 * 
	 * @param url
	 * @return, true or false
	 */
	__ifCache: function( url )
	{		
		if( url )
			url = url.toLowerCase( );
		else
			url = "";
			
		// if don't set __nocache, default is true
		var reg = new RegExp( "[&|?]{1}__nocache[^&|^#]*", "gi" );
		if( url.search( reg ) < 0 )
			return true;
		else
			return false;			
					
		return true;
	},
		
	/**
	 * Check if submit request
	 * @param mode
	 * @param url
	 * @return, true or false
	 */
	__ifSubmit: function( mode, url )
	{
		// if use '/preview' pattern, submit anyway
		if( mode == 'preview' )
			return true;
		
		if( url )
			url = url.toLowerCase( );
		else
			url = "";
			
		// if use '/frameset' or '/run', check format.
		// if format is not HTML, submit request.	
		if( mode == 'run' || mode == 'frameset' )
		{
			var format = Constants.request.format;
			return !( format == "htm" || format == "html" );
		}
		
		return false;
	}
}
);
