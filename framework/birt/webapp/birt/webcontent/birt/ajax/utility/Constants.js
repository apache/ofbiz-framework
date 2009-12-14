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
 
var Constants = {

	/** id of document managed BirtReportDocument*/
	documentId: "Document",
	
	/** element is a UI component managed by BirtReportBase */
	reportBase: "ReportBase",
	
	/** element is a table managed by BirtReportTable */
	reportTable: "ReportTable",
	
	/** element is a chart managed by BirtReportChart */
	reportChart: "ReportChart",
	
	/** element is a document managed BirtReportDocument */
	isDocument: "isDocument",
	
	/** contains number of selected column if there is one */
	selColNum: "SelColNum",

	/** contains number of selected column if there is one */
	activeIds: "activeIds",
	activeIdTypes: "activeIdTypes",
	
	// Report object types.
	Document : "Document",
	Table : "Table",
	Chart : "Chart",
	Label : "Label",
	Table_T : "Table_T",	// template of table type
	Chart_T : "Chart_T",	// template of chart type
	Label_T : "Label_T",	// template of label type
	
	// URL parameters name
	PARAM_ACTION : '__action',
	PARAM_FORMAT : '__format',
	PARAM_ASATTACHMENT : '__asattachment',
	PARAM_OVERWRITE : '__overwrite',
	PARAM_PAGE : '__page',
	PARAM_PAGERANGE : '__pagerange',
	PARAM_PARAMETERPAGE : '__parameterpage',
	PARAM_BOOKMARK : '__bookmark',
	PARAM_INSTANCE_ID : '__instanceid',
	PARAM_SVG : '__svg',
	PARAM_TASKID : '__taskid',
	PARAM_ISTOC : '__istoc',
	PARAM_RESULTSETNAME : '__resultsetname',
	PARAM_SELECTEDCOLUMNNUMBER : '__selectedcolumnnumber',
	PARAM_SELECTEDCOLUMN : '__selectedcolumn',
	PARAM_EXPORT_ENCODING : '__exportencoding',
	PARAM_SEP : '__sep',
	PARAM_EXPORT_DATATYPE : '__exportdatatype',
	PARAM_LOCALENEUTRAL : '__localeneutral',
	PARAM_DATA_EXTRACT_FORMAT : '__extractformat',
	PARAM_DATA_EXTRACT_EXTENSION : '__extractextension',
	PARAM_ISNULL : '__isnull',
	PARAM_EMITTER_ID : '__emitterid',
	
	PARAM_PRINTER_NAME : '__printer',
	PARAM_PRINTER_COPIES : '__printer_copies',
	PARAM_PRINTER_COLLATE : '__printer_collate',
	PARAM_PRINTER_DUPLEX : '__printer_duplex',
	PARAM_PRINTER_MODE : '__printer_mode',
	PARAM_PRINTER_MEDIASIZE : '__printer_pagesize',
	
	PARAM_PAGE_OVERFLOW : '__pageoverflow',	
	PARAM_PAGEBREAKONLY : '__pagebreakonly',
	
	PARAM_SESSION_ID : '__sessionId',
	
	// Parameter Data Type
	TYPE_ANY : '0',
	TYPE_STRING : '1',
	TYPE_FLOAT : '2',
	TYPE_DECIMAL : '3',
	TYPE_DATE_TIME : '4',
	TYPE_BOOLEAN : '5',
	TYPE_INTEGER : '6',
	TYPE_DATE : '7',
	TYPE_TIME : '8',
	
	// Servlet patterns
	SERVLET_OUTPUT : 'output',
	SERVLET_EXTRACT : 'extract',
	SERVLET_FRAMESET : 'frameset',
	SERVLET_PARAMETER : 'parameter',
	SERVLET_PREVIEW : 'preview',
	
	// Output formats
	FORMAT_POSTSCRIPT : 'postscript',
	FORMAT_PDF : 'pdf',
	FORMAT_HTML : 'html',
	FORMAT_PPT : 'ppt',
	
	// Action names
	ACTION_PRINT : 'print',
	
	// Window names
	WINDOW_PRINT_PREVIEW : 'birtPrint',
	
	/** 
	event.returnvalue indicating that event has already been handled
	as a selection
	*/
	select: "select",
	
	/**
	event.returnvalue indicated that contextmenu has already been handled
	*/
	context: "context",
	
	error : { },
	
	type: {UpdateRequest: "UpdateRequest"},
	operation: "Operation",
	target: "Target",
	operator: {show: "Show", hide: "Hide", sort: "Sort"}
}