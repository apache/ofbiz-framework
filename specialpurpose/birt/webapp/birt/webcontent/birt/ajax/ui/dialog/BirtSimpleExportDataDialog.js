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
 *	Birt export data dialog.
 */
BirtSimpleExportDataDialog = Class.create( );

BirtSimpleExportDataDialog.prototype = Object.extend( new AbstractBaseDialog( ),
{
	__neh_select_change_closure : null,
	__neh_switchResultSet_closure : null,
		
	availableResultSets : [],
	selectedColumns : [],

	/**
	 *	Initialization routine required by "ProtoType" lib.
	 *	@return, void
	 */
	initialize : function( id )
	{
		this.__initBase( id );
		this.__z_index = 200;
		
		// Closures
		this.__neh_switchResultSet_closure = this.__neh_switchResultSet.bindAsEventListener( this );
		this.__neh_click_exchange_closure = this.__neh_click_exchange.bindAsEventListener( this );
		this.__neh_dblclick_src_closure = this.__neh_dblclick_src.bindAsEventListener( this );
		this.__neh_dblclick_dest_closure = this.__neh_dblclick_dest.bindAsEventListener( this );
		this.__neh_click_src_closure = this.__neh_click_src.bindAsEventListener( this );
		this.__neh_click_dest_closure = this.__neh_click_dest.bindAsEventListener( this );
		
		this.__installEventHandlers( id );
	},
	
	/**
	 *	Install native/birt event handlers.
	 *
	 *	@id, toolbar id (optional since there is only one toolbar)
	 *	@return, void
	 */
	__installEventHandlers : function( id )
	{
		var oSelects = this.__instance.getElementsByTagName( 'select' );
		Event.observe( oSelects[0], 'change', this.__neh_switchResultSet_closure, false );
		
		// Initialize exchange buttons
		var oInputs = this.__instance.getElementsByTagName( 'input' );
		for ( var i = 0; i < oInputs.length ; i++ )
		{
			Event.observe( oInputs[i], 'click', this.__neh_click_exchange_closure, false );
		}
		
		// Initialize exchange selects
		var oSelects = this.__instance.getElementsByTagName( 'select' );
		if( oSelects.length > 2 )
		{
			Event.observe( oSelects[1], 'dblclick', this.__neh_dblclick_src_closure, false );
			Event.observe( oSelects[2], 'dblclick', this.__neh_dblclick_dest_closure, false );
			Event.observe( oSelects[1], 'change', this.__neh_click_src_closure, false );
			Event.observe( oSelects[2], 'change', this.__neh_click_dest_closure, false );
		}
	},
	
	/**
	 *	Native event handler for selection item movement.
	 */
	__neh_click_exchange : function( event )
	{	
		var oSC = Event.element( event );
		if( oSC.id == 'exportDataEncoding_other' )
		{
			this.__updateButtons( );
			$( 'exportDataOtherEncoding_input' ).focus( );
		}
		else
		{	
			var oInputs = this.__instance.getElementsByTagName( 'input' );
			var oSelects = this.__instance.getElementsByTagName( 'select' );
			
			switch ( Event.element( event ).name )
			{
				case 'Addall':
				{
					if ( oSelects[1].options.length  > 0 )
					{
						this.moveAllItems( oSelects[1], oSelects[2] );
					}
					break;
				}
				case 'Add':
				{
					if ( oSelects[1].options.length  > 0 )
					{
						this.moveSingleItem( oSelects[1], oSelects[2] );
					}
					break;
				}
				case 'Remove':
				{
					if ( oSelects[2].options.length  > 0 )
					{
						this.moveSingleItem( oSelects[2], oSelects[1] );
					}
					break;
				}
				case 'Removeall':
				{
					if ( oSelects[2].options.length  > 0 )
					{
						this.moveAllItems( oSelects[2], oSelects[1] );
					}
					break;
				}
				case 'Up':
				{
					birtUtility.moveSelectedItemsUp( oSelects[2] );
					break;
				}
				case 'Down':
				{
					birtUtility.moveSelectedItemsDown( oSelects[2] );
					break;
				}
			}
			
			this.__updateButtons( );
		}				
	},

	/**
	 *	Native event handler for double click source select element.
	 */
	__neh_dblclick_src : function( event )
	{
		var oSelects = this.__instance.getElementsByTagName( 'select' );
		
		if ( oSelects[1].options.length  > 0 )
		{
			this.moveSingleItem( oSelects[1], oSelects[2] );
		}
		
		this.__updateButtons( );
	},

	/**
	 *	Native event handler for double click dest select element.
	 */
	__neh_dblclick_dest : function( event )
	{
		var oSelects = this.__instance.getElementsByTagName( 'select' );
		
		if ( oSelects[2].options.length  > 0 )
		{
			this.moveSingleItem( oSelects[2], oSelects[1] );
		}
		
		this.__updateButtons( );
	},
	
	/**
	 *	Native event handler for click source select element.
	 */
	__neh_click_src : function( event )
	{
		this.__updateButtons( );
	},

	/**
	 *	Native event handler for click dest select element.
	 */
	__neh_click_dest : function( event )
	{
		this.__updateButtons( );
	},
		
	/**
	 *	Update button status.
	 */
	__updateButtons : function( )
	{
		var oSelects = this.__instance.getElementsByTagName( 'select' );
		var canExport = oSelects[0].options.length > 0;
		var canAdd = oSelects[1].options.length > 0;
		var canRemove = oSelects[2].options.length  > 0;
		var srcSelectedIndex = oSelects[1].selectedIndex;
		var destSelectedIndex = oSelects[2].selectedIndex;

		var oInputs = this.__instance.getElementsByTagName( 'input' );
		
		if( !rtl )
		{
			oInputs[0].src = canAdd ? "birt/images/AddAll.gif" : "birt/images/AddAll_disabled.gif";
		}
		else
		{
			oInputs[0].src = canAdd ? "birt/images/AddAll_rtl.gif" : "birt/images/AddAll_disabled_rtl.gif";
		}
		oInputs[0].style.cursor = canAdd ? "pointer" : "default";
		
		if( !rtl )
		{
			oInputs[1].src = canAdd && srcSelectedIndex >= 0 ? "birt/images/Add.gif" : "birt/images/Add_disabled.gif";
		}
		else
		{
			oInputs[1].src = canAdd && srcSelectedIndex >= 0 ? "birt/images/Add_rtl.gif" : "birt/images/Add_disabled_rtl.gif";
		}	
		oInputs[1].style.cursor = canAdd ? "pointer" : "default";
		
		if( !rtl )
		{ 
			oInputs[2].src = canRemove && destSelectedIndex >= 0 ? "birt/images/Remove.gif" : "birt/images/Remove_disabled.gif";
		}
		else
		{
			oInputs[2].src = canRemove && destSelectedIndex >= 0 ? "birt/images/Remove_rtl.gif" : "birt/images/Remove_disabled_rtl.gif";
		}	
		oInputs[2].style.cursor = canRemove ? "pointer" : "default";

		if( !rtl )
		{
			oInputs[3].src = canRemove ? "birt/images/RemoveAll.gif" : "birt/images/RemoveAll_disabled.gif";
		}
		else
		{
			oInputs[3].src = canRemove ? "birt/images/RemoveAll_rtl.gif" : "birt/images/RemoveAll_disabled_rtl.gif";
		}
		oInputs[3].style.cursor = canRemove ? "pointer" : "default";

		oInputs[4].src = canRemove && destSelectedIndex >= 0 ? "birt/images/Up.gif" : "birt/images/Up_disabled.gif";
		oInputs[4].style.cursor = canRemove ? "pointer" : "default";

		oInputs[5].src = canRemove && destSelectedIndex >= 0 ? "birt/images/Down.gif" : "birt/images/Down_disabled.gif";
		oInputs[5].style.cursor = canRemove ? "pointer" : "default";
		
		if( canExport )
		{
			this.__setDisabled( 'exportDataEncodingSetting', false );
			$( 'exportDataCSVSeparator' ).disabled = false;
			
			var oEnc = $( 'exportDataEncoding_other' );
			var oEncInput = $( 'exportDataOtherEncoding_input' );
			if( oEnc && oEnc.checked )
			{				
				oEncInput.disabled = false;
			}
			else
			{
				oEncInput.disabled = true;
			}
		}
		else
		{
			this.__setDisabled( 'exportDataEncodingSetting', true );
			$( 'exportDataCSVSeparator' ).disabled = true;
		}
	},

	/**
	 * Set disabled status for all controls in container 
	 * 
	 * @param id
	 * @param flag
	 * @return, void
	 */
	__setDisabled : function( id, flag )
	{
		var oContainer = $( id );
		if( !oContainer )
			return;
		
		var oSelects = oContainer.getElementsByTagName( "select" );
		if( oSelects )
		{
			for( var i=0; i<oSelects.length; i++ )
				oSelects[i].disabled = flag;
		}
		
		var oInputs = oContainer.getElementsByTagName( "input" );
		if( oInputs )
		{
			for( var i=0; i<oInputs.length; i++ )
				oInputs[i].disabled = flag;
		}		
	},
		
	/**
	 *	Move single selection item.
	 */
	moveSingleItem : function( sel_source, sel_dest )
	{
		if ( sel_source.selectedIndex == -1 )
		{
			return;
		}
		
		for ( var i=0; i<sel_source.options.length; i++ )
		{
			if ( sel_source.options[i].selected )
			{
				var selectedItem = sel_source.options[i];
				sel_dest.options[sel_dest.options.length] = new Option( selectedItem.text, selectedItem.value );
				sel_source.remove( i );
				i = i - 1;
			}							
		}
		
		sel_source.selectedIndex = 0;
	},
	
	/**
	 *	Move all selection items.
	 */
	moveAllItems : function( sel_source, sel_dest )
	{
   		for ( var i = 0; i < sel_source.length; i++ )
   		{
     		var SelectedText = sel_source.options[i].text;
     		var SelectedValue = sel_source.options[i].value;
	   		var newOption = new Option( SelectedText );
			newOption.value = SelectedValue;
     		sel_dest.options[sel_dest.options.length] = newOption;
   		}
   		
   		sel_dest.selectedIndex = 0;
   		sel_source.length = 0;
	},	

	/**
	 *	Binding data to the dialog UI. Data includes zoom scaling factor.
	 *
	 *	@data, data DOM tree (schema TBD)
	 *	@return, void
	 */
	 __bind : function( data )
	 {
	 	if ( !data )
	 	{
	 		return;
	 	}
	 	
	 	var oSelects = this.__instance.getElementsByTagName( 'select' );
		oSelects[0].options.length = 0;
		oSelects[1].options.length = 0;
		oSelects[2].options.length = 0;
		
		this.availableResultSets = [];
	 	
	 	var resultSets = data.getElementsByTagName( 'ResultSet' );
	 	for ( var k = 0; k < resultSets.length; k++ )
	 	{
	 		var resultSet = resultSets[k];
	 		
		 	var queryNames = resultSet.getElementsByTagName( 'QueryName' );
			oSelects[0].options[oSelects[0].options.length] = new Option(queryNames[0].firstChild.data);
				 	
			this.availableResultSets[k] = {};
			
		 	var columns = resultSet.getElementsByTagName( 'Column' );
		 	for( var i = 0; i < columns.length; i++ )
		 	{
		 		var column = columns[i];
		 		
		 		var columnName = column.getElementsByTagName( 'Name' );
		 		var label = column.getElementsByTagName( 'Label' );
				this.availableResultSets[k][label[0].firstChild.data] = columnName[0].firstChild.data;
		 	}
		}
		
		this.__neh_switchResultSet( );
	 },
	 
	 /**
	  *	switch result set.
	  */
	 __neh_switchResultSet : function( )
	 {
	 	var oSelects = this.__instance.getElementsByTagName( 'select' );
		oSelects[1].options.length = 0;
		oSelects[2].options.length = 0;	
	 	
	 	var columns = this.availableResultSets[oSelects[0].selectedIndex];
	 	for( var label in columns )
	 	{
	 		var colName = columns[label];
			var option = new Option( label );
			option.value = colName;
			oSelects[1].options[oSelects[1].options.length] = option;
	 	}
	 	
		this.__updateButtons( );
	 },

	/**
	 *	Handle clicking on ok.
	 *
	 *	@return, void
	 */
	__okPress : function( )
	{
		var oSelects = this.__instance.getElementsByTagName( 'select' );
		this.__l_hide( );
		if ( oSelects[2].options.length > 0 )
		{
			for( var i = 0; i < oSelects[2].options.length; i++ )
			{
				this.selectedColumns[i] = oSelects[2].options[i].value;
			}
			
			this.__constructForm( );
		}
	},
	
	/**
	 *	Construct extract data form. Post it to server.
	 */
	__constructForm : function( )
	{
		var dialogContent = $( 'simpleExportDialogBody' );
		var hiddenDiv = document.createElement( 'div' );
		hiddenDiv.style.display = 'none';

		var hiddenForm = document.createElement( 'form' );
		hiddenForm.method = 'post';
		hiddenForm.target = '_self';
		var url = soapURL;
		url = url.replace( /[\/][a-zA-Z]+[?]/, '/' + Constants.SERVLET_EXTRACT + '?' );
		
		// delete some URL parameters
		url = birtUtility.deleteURLParameter( url, Constants.PARAM_BOOKMARK );
		url = birtUtility.deleteURLParameter( url, Constants.PARAM_INSTANCE_ID );
		hiddenForm.action = url;
		
		// Pass over current element's iid.
		var queryNameInput = document.createElement( 'input' );
		queryNameInput.type = 'hidden';
		queryNameInput.name = Constants.PARAM_RESULTSETNAME;
		var oSelects = this.__instance.getElementsByTagName( 'select' );
		queryNameInput.value = oSelects[0].options[oSelects[0].selectedIndex].text;
		hiddenForm.appendChild( queryNameInput );

		// Total # of selected columns.
		if ( this.selectedColumns.length > 0 )
		{
			var hiddenSelectedColumnNumber = document.createElement( 'input' );
			hiddenSelectedColumnNumber.type = 'hidden';
			hiddenSelectedColumnNumber.name = Constants.PARAM_SELECTEDCOLUMNNUMBER;
			hiddenSelectedColumnNumber.value = this.selectedColumns.length;
			hiddenForm.appendChild( hiddenSelectedColumnNumber );

			// data of selected columns.
			for( var i = 0; i < this.selectedColumns.length; i++ )
			{
				var hiddenSelectedColumns = document.createElement( 'input' );
				hiddenSelectedColumns.type = 'hidden';
				hiddenSelectedColumns.name = Constants.PARAM_SELECTEDCOLUMN + i;
				hiddenSelectedColumns.value = this.selectedColumns[i];
				hiddenForm.appendChild( hiddenSelectedColumns );
			}
		}
		
		this.selectedColumns = [];
		
		// CSV separator
		var oExtension = $( 'exportDataExtension' );
		if( oExtension && oExtension.value != '' )
		{
			var hiddenExtension = document.createElement( 'input' );
			hiddenExtension.type = 'hidden';
			hiddenExtension.name = Constants.PARAM_DATA_EXTRACT_EXTENSION;
			hiddenExtension.value = oExtension.value;
			hiddenForm.appendChild( hiddenExtension );			
		}
		
		// Pass the export data encoding		
		var oUTF8 = $( 'exportDataEncoding_UTF8' );
		var hiddenEnc = document.createElement( 'input' );
		hiddenEnc.type = 'hidden';
		hiddenEnc.name = Constants.PARAM_EXPORT_ENCODING;
		if( oUTF8 && oUTF8.checked )
		{
			hiddenEnc.value = oUTF8.value;
		}
		else
		{
			hiddenEnc.value = $('exportDataOtherEncoding_input').value;
		}
		hiddenForm.appendChild( hiddenEnc );
		
		// CSV separator
		var oSep = $( 'exportDataCSVSeparator' );
		if( oSep && oSep.value != '' )
		{
			var hiddenSep = document.createElement( 'input' );
			hiddenSep.type = 'hidden';
			hiddenSep.name = Constants.PARAM_SEP;
			hiddenSep.value = oSep.value;
			hiddenForm.appendChild( hiddenSep );			
		}

		var hiddenAsAttachment = document.createElement( 'input' );
		hiddenAsAttachment.type = 'hidden';
		hiddenAsAttachment.name = Constants.PARAM_ASATTACHMENT;
		hiddenAsAttachment.value = "true";
		hiddenForm.appendChild( hiddenAsAttachment );			
		
		// Whether exports column's data type
		var oType = $( 'exportColumnDataType' );
		var hiddenType = document.createElement( 'input' );
		hiddenType.type = 'hidden';
		hiddenType.name = Constants.PARAM_EXPORT_DATATYPE;
		if( oType && oType.checked )
			hiddenType.value = "true";
		else
			hiddenType.value = "false";
		hiddenForm.appendChild( hiddenType );
		
		// Whether exports column as locale neutral
		var oLocaleNeutral = $( 'exportColumnLocaleNeutral' );
		var hiddenLocaleNeutral = document.createElement( 'input' );
		hiddenLocaleNeutral.type = 'hidden';
		hiddenLocaleNeutral.name = Constants.PARAM_LOCALENEUTRAL;
		if( oLocaleNeutral && oLocaleNeutral.checked )
			hiddenLocaleNeutral.value = "true";
		else
			hiddenLocaleNeutral.value = "false";
		hiddenForm.appendChild( hiddenLocaleNeutral );		
		
		var tmpSubmit = document.createElement( 'input' );
		tmpSubmit.type = 'submit';
		tmpSubmit.value = 'TmpSubmit';
		hiddenForm.appendChild( tmpSubmit );
		
		hiddenDiv.appendChild( hiddenForm );
		dialogContent.appendChild( hiddenDiv );
		tmpSubmit.click( );
		dialogContent.removeChild( hiddenDiv );
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
	},
	
	/**
	Called before element is hidden
	*/
	__preHide: function()
	{
		// enable the toolbar buttons
		birtUtility.setButtonsDisabled ( "toolbar", false );
		
		// enable the Navigation Bar buttons
		birtUtility.setButtonsDisabled ( "navigationBar", false );		
	}	
} );