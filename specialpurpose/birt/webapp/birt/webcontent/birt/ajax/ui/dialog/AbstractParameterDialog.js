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
AbstractParameterDialog = function( ) { };

AbstractParameterDialog.prototype = Object.extend( new AbstractBaseDialog( ),
{
    /**
     *__parameter to store "name" and "value" pairs 
     */
     __parameter : [],
         
    /**
     *__cascadingParameter to store "name" and "value" pairs
     */
     __cascadingParameter : [],

    /**
	 *	Event handler closures.
	 */
	 __neh_click_radio_closure : null,
	 __neh_change_select_closure : null,
	 
	/**
	 *	Initialize dialog base.
	 *	@return, void
	 */
	initializeBase : function( id )
	{
		this.__initBase( id, '500px' );
		this.__z_index = 200;
		
	    this.__neh_click_radio_closure = this.__neh_click_radio.bindAsEventListener( this );
	    this.__neh_change_select_closure = this.__neh_change_select.bindAsEventListener( this );

	    this.__local_installEventHandlers(id);
	},
	
	/**
	 *	Binding data to the dialog UI. Data includes zoom scaling factor.
	 *	@data, data DOM tree (schema TBD)
	 *	@return, void
	 */
	__bind : function( data )
	{
		this.__propogateCascadeParameter( data );
	},

	/**
	 *	Binding data to the dialog UI. Data includes zoom scaling factor.
	 *	@data, data DOM tree (schema TBD)
	 *	@return, void
	 */
	__propogateCascadeParameter : function( data )
	{
		if ( this.__operationCancelled )
		{
			return;
		}
		
		if( data )
		{
			var cascade_param = data.getElementsByTagName( 'CascadeParameter' )[0];//assume there is only one cascadeparameter
			var selectionLists = data.getElementsByTagName( 'SelectionList' );
			if ( !selectionLists )
			{
				return;
			}
			
			for ( var k = 0; k < selectionLists.length; k++ )
			{
				var param_name = selectionLists[k].getElementsByTagName( 'Name' )[0].firstChild.data;
				var selections = selectionLists[k].getElementsByTagName( 'Selections' );
				
				var append_selection = document.getElementById( param_name + "_selection" );
				append_selection.title = "";
				var len = append_selection.options.length;
								
				// Clear our selection list.
				for( var i = 0, index = 0; i < len; i++ )
				{
					/*
					if ( append_selection.options[index].value == "" )
					{
						index++;
						continue;
					}
					*/
					append_selection.remove( index );
				}
				
				// Add new options based on server response.
				for( var i = 0; i < selections.length; i++ )
				{
					if ( !selections[i].firstChild )
					{
						continue;
					}
	
					var oOption = document.createElement( "OPTION" );
					var oLabel = selections[i].getElementsByTagName( 'Label' )
					if ( oLabel && oLabel.length > 0 )
					{
						oLabel = oLabel[0].firstChild;
					}
					if( oLabel )
						oOption.text = oLabel.data;
					else
						oOption.text = "";

					var oValue = selections[i].getElementsByTagName( 'Value' );
					if ( oValue && oValue.length > 0 )
					{
						oValue = oValue[0].firstChild;
					}
					if( oValue )
						oOption.value = oValue.data;
					else
						oOption.value = "";
					append_selection.options[append_selection.options.length] = oOption;
				}
			}
		}
	},

	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, toolbar id (optional since there is only one toolbar)
	 *	@return, void
	 */
	__local_installEventHandlers : function( id )
	{
		//install UIComponent native handler
		var oTBC = document.getElementById("parameter_table").getElementsByTagName( 'TABLE' );
		for( var k = 0, counter = 0; k < oTBC.length; k++ )
		{
		    var temp = oTBC[k].getElementsByTagName( 'TABLE' );
		    if( !temp.length )
		    {
		        //install select event handler in cascade parameters
		        this.__install_cascade_parameter_event_handler( oTBC[k], counter++ );
		    }
		}
		
		var oTRC = document.getElementById( "parameter_table" ).getElementsByTagName( "TR" );
		for( var i = 0; i < oTRC.length; i++ )
		{
			var oInput = oTRC[i].getElementsByTagName( "input" );
			var oTable = oTRC[i].getElementsByTagName( "table" );
			if( oTable.length > 0 )
			{
				continue;
			}
			//find radio with textbox or select items to install event listener.
			var flag = false;
			for( var j = 0; j < oInput.length; j++ )
			{
				if( oInput[j].type == "radio" && !flag )
				{
					var tempRadio = oInput[j];
					flag = true;
					continue;
				}
	  
				if( oInput[j].type == "radio" && tempRadio != {} && oInput[j].id != tempRadio.id )
				{
					Event.observe( tempRadio, 'click', this.__neh_click_radio_closure, false );
					Event.observe( oInput[j], 'click', this.__neh_click_radio_closure, false );
				}
			}
		}
	},
	
	/**
	 *	Handle clicking on ok.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__okPress : function( )
	{
		if( birtParameterDialog.collect_parameter( ) )
		{
			birtEventDispatcher.broadcastEvent( birtEvent.__E_CHANGE_PARAMETER );
			this.__l_hide( );
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
					
		for( var i = 0; i < oInput.length; i++ )
		{
			if( oInput[i].id == temp.id )
			{
				//enable the next component
				oInput[i].checked = true;
				if( oInput[i+1] && ( oInput[i+1].type == "text" || oInput[i+1].type == "password" ) )
				{
					oInput[i+1].disabled = false;
					oInput[i+1].focus( );
				}
				else if( oSelect[0] )
				{
					oSelect[0].disabled = false;
					oSelect[0].focus( );
				}
			}
			else if( oInput[i].type == "radio" && oInput[i].id != temp.id )
			{
				//disable the next component and clear the radio
				oInput[i].checked = false;
				if( oInput[i+1] && ( oInput[i+1].type == "text" || oInput[i+1].type == "password" ) )
				{
					oInput[i+1].disabled = true;
				}
				else if( oSelect[0] )
				{
					oSelect[0].disabled = true;
				}
		    }
		}
	},
	
	/**
	 *	Handle change event when clicking on select.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_change_select : function( event )
	{
	    var matrix = new Array( );
	    var m = 0;
        for( var i = 0; i < this.__cascadingParameter.length; i++ )
        {
            for( var j = 0; j < this.__cascadingParameter[i].length; j++ )
            {
                if( this.__cascadingParameter[i][j].name == Event.element( event ).id.substr( 0, Event.element( event ).id.length - 10 ) )
                {
                    this.__cascadingParameter[i][j].value = Event.element( event ).options[Event.element( event ).selectedIndex].value;
                    for( var m = 0; m <= j; m++ )
                    {
					    if( !matrix[m] )
				        {
				            matrix[m] = {};
				        }
				        matrix[m].name = this.__cascadingParameter[i][m].name;
				        matrix[m].value = this.__cascadingParameter[i][m].value;
				    }                    
                    birtEventDispatcher.broadcastEvent( birtEvent.__E_CASCADING_PARAMETER, matrix );
                }
            }
        }
	}
} );