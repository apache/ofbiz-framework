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
 * Class ReportComponentIdRegistry 
 */
ReportComponentIdRegistry =
{
	types:
	{
		DOCUMENT : null,
		TABLE : null,
		LABEL : null,
		CHART : null,
		TABLE_T : null,
		LABEL_T : null,
		CHART_T : null
	},
	
	ids: {},
	
	getDocumentType : function( )
	{
		return 'DOCUMENT';
	},
	
	getTableType : function( )
	{
		return 'TABLE';
	},
	
	getLabelType : function( )
	{
		return 'LABEL';
	},
	
	getChartType : function( )
	{
		return 'CHART';
	},
	
	getTemplateTableType : function( )
	{
		return 'TABLE_T';
	},
	
	getTemplateLabelType : function( )
	{
		return 'LABEL_T';
	},
	
	getTemplateChartType : function( )
	{
		return 'CHART_T';
	},
	
	getTypeForId : function( id )
	{
		if ( this.ids[id] )
		{
			return this.ids[id];
		}
		return null;
	},

	setHandlerObjectForType : function( object, type )
	{
		for ( var i in this.types )
		{
			if ( i == type )
			{
				this.types[i] = object;
				return;
			}
		}
	},
	
	getObjectForId : function( id )
	{
		var type = this.getTypeForId( id );
		if ( type )
		{
			return this.types[type];
		}
		return null;
	},
	
	getObjectForType : function( type )
	{
		if ( type )
		{
			if ( type.toUpperCase( ) == 'Group'.toUpperCase( )
					|| type.toUpperCase( ) == 'ColumnInfo'.toUpperCase( ) )
			{
				return this.types['TABLE'];
			}
			else
			{
				return this.types[type.toUpperCase( )];
			}
		}
		
		return null;
		
	},

	addId : function( id, type )
	{
		this.ids[id] = type;
	},
	
	removeId : function( id )
	{
		if ( this.ids[id] )
		{
			this.ids[id] = null;
			delete this.ids[id];
		}
	}	
};
