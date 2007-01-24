/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


var whizzywig_version = 'Whizzywig v50k'; // Office XML; x <code>; sync on clean; extend sels,buts[ ]; whizzy in table/div; monospace; cols ex; MIT license;x Enter on forms;http/mailto; no font for color; gentleClean true; IE anchor bug; designmode x-browser; x 404 text buts;
var buttonPath;  //path to toolbar button images;  unset or "textbuttons" means don't use images
var cssFile;     //url of CSS stylesheet to attach to edit area
var imageBrowse; //path to page for image browser
var linkBrowse;  //path to page for link browser
var idTa;        //id of the textarea to whizzywig (param to makeWhizzyWig)
var gentleClean = true;  //if true, cleanUp preserves spans, inline styles and classes
//OTHER GLOBALS
var oW, sel, rng, papa, trail ; //object of Whizzy,  current sel, range, parent, DOM path (IE)
var sels = '';
var buts = ''; 
var vals = new Array();
var opts = new Array();
var dobut = new Array();

function makeWhizzyWig(txtArea, controls){ // make a WhizzyWig from the textarea
 if ((navigator.userAgent.indexOf('Safari') != -1 ) || !document.getElementById || !document.designMode ) {//no designMode
  alert("Whizzywig "+t("editor not available for your browser"));
  return;
 }
 idTa = txtArea;
 var taContent = o(idTa).defaultValue ? o(idTa).defaultValue : o(idTa).innerHTML ? o(idTa).innerHTML: ''; //anything in the textarea?
 taWidth = o(idTa).style.width ? o(idTa).style.width : o(idTa).cols + "ex";  //grab the width and...
 taHeight = o(idTa).style.height ? o(idTa).style.height : o(idTa).rows + "em";  //...height from the textarea
 o(idTa).style.color = '#060';
 o(idTa).style.zIndex = '2';
 if (!o(idTa).rows) o(idTa).rows='15';//IE won't use % from style
 h(idTa);
 var frm=o(idTa).parentNode;
 while (frm.nodeName != 'FORM') frm=frm.parentNode;//if not form, keep trying
 if (frm.addEventListener) frm.addEventListener("submit", syncTextarea, false);
  else frm.attachEvent("onsubmit", syncTextarea);
 w('<style type="text/css">button {vertical-align:middle;padding:0;margin:1px 0} button img{vertical-align:middle;margin:-1px} select{vertical-align:middle;margin:1px}  .ctrl {background:ButtonFace; border:2px outset ButtonShadow; padding:5px;width:'+taWidth+'} #sourceTa{color:#060;font-family:mono;}</style>');
 sels += 'fontname fontsize formatblock';
 buts += ' bold italic underline | left center right | number bullet indent outdent | undo redo  | color hilite rule | link image table | clean html spellcheck ';
 var tbuts = ' tstart add_row_above add_row_below delete_row | add_column_before add_column_after delete_column | table_in_cell';
 var t_end = ''; //table controls end, if needed
 buts += tbuts;
 controls = controls.toLowerCase();
 if (!controls || controls == "all")  controls = sels +' newline '+ buts;
 else controls += tbuts;
 w('<div id="CONTROLS" class="ctrl" unselectable="on">');
 gizmos = controls.split(' ');
 for (var i = 0; i < gizmos.length; i++) {
  if (gizmos[i]){ //make buttons and selects for toolbar, in order requested
   if (gizmos[i] == 'tstart') {
    w('<div id="TABLE_CONTROLS" style="display:none" unselectable="on">');
    t_end = '</div>';
   }
   else if (gizmos[i] == '|') w('&nbsp;<big style="padding-bottom:2em">|</big>&nbsp;');
   else if (gizmos[i] == 'newline') w('<br>');
   else if (sels.indexOf(gizmos[i]) != -1) makeSelect(gizmos[i])
   else if (buts.indexOf(gizmos[i]) != -1) makeButton(gizmos[i]);
  }
 }
 w(t_end) //table controls end
 w(fGo('LINK'));
 if (linkBrowse) w('<input type="button" onclick=doWin("'+linkBrowse+'"); value="'+t("Browse")+'"> ');
 w(t('Link address (URL)')+': <input type="text" id="lf_url" size="60"><br><input type="button" value="http://" onclick="o(\'lf_url\').value=\'http://\'+o(\'lf_url\').value"> <input type="button" value="mailto:" onclick="o(\'lf_url\').value=\'mailto:\'+o(\'lf_url\').value"><input type="checkbox" id="lf_new">'+t("Open link in new window")+fNo(t("OK"),"insertLink()"));//LINK_FORM end
 w(fGo('IMAGE'));
 if (imageBrowse) w('<input type="button" onclick=doWin("'+imageBrowse+'"); value="'+t("Browse")+'"> ');
 w(t('Image address (URL)')+': <input type="text" id="if_url" size="50"> <label title='+t("to display if image unavailable")+'><br>'+t("Alternate text")+':<input id="if_alt" type="text" size="50"></label><br>'+t("Align")+':<select id="if_side"><option value="none">'+t("normal")+'</option><option value="left">'+t("left")+'</option><option value="right">'+t("right")+'</option></select> '+t("Border")+':<input type="text" id="if_border" size="20" value="none" title="'+t("number or CSS e.g. 3px maroon outset")+'"> '+t("Margin")+':<input type="text" id="if_margin" size="20" value="0" title="'+t("number or CSS e.g. 5px 1em")+'">'+fNo(t("Insert Image"),"insertImage()"));//IMAGE_FORM end
 w(fGo('TABLE')+t("Rows")+':<input type="text" id="tf_rows" size="2" value="3"> <select id="tf_head"><option value="0">'+t("No header row")+'</option><option value="1">'+t("Include header row")+'</option></select> '+t("Columns")+':<input type="text" id="tf_cols" size="2" value="3"> '+t("Border width")+':<input type="text" id="tf_border" size="2" value="1"> '+fNo(t("Insert Table"),"makeTable()")+'</div>');//TABLE_FORM end
 w(fGo('COLOR')+'<input type="hidden" id="cf_cmd"><div style="background:#000;padding:1px;height:22px;width:125px;float:left"><div id="cPrvw" style="background-color:red; height:100%; width:100%"></div></div> <input type=text id="cf_color" value="red" size=17 onpaste=vC(value) onblur=vC(value)> <input type="button" onmouseover=vC() onclick=sC() value="'+t("OK")+'">  <input type="button" onclick="hideDialogs();" value="'+t("Cancel")+'"><br> '+t("click below or enter a")+' <a href="http://www.unverse.net/colortable.htm" target="_blank">'+t("color name")+'</a><br clear=all> <table border=0 cellspacing=1 cellpadding=0 width=480 bgcolor="#000000">');
 var wC = new Array("00","33","66","99","CC","FF")  //color table
 for (i=0; i<wC.length; i++){
  w("<tr>");
  for (j=0; j<wC.length; j++){
   for (k=0; k<wC.length; k++){
    var clr = wC[i]+wC[j]+wC[k];
    w('<td style="background:#'+clr+';height:12px;width:12px" onmouseover=vC("#'+clr+'") onclick=sC("#'+clr+'")></td>');
   }
  }
  w('</tr>');
 }
 w('</table></div>'); //end color table,COLOR_FORM
 w('</div>'); //controls end
 w('<div class="ctrl" id="showWYSIWYG" style="display:none"><input type="button" onclick="showDesign();" value="'+t("Hide HTML")+'"></div>');
 
 w('<iframe style="width:'+taWidth+';height:'+taHeight+'" src="javascript:;" id="whizzyWig"></iframe><br>');

 w('<a href="http://www.unverse.net" style="color:buttonface" title="'+whizzywig_version+'">.</a> ');
 var startHTML = "<html>\n";
 startHTML += "<head>\n";
 if (cssFile) {
  startHTML += "<link media=\"all\" type=\"text/css\" href=\"" + cssFile + "\" rel=\"stylesheet\">\n";
 }
 startHTML += "</head>\n";
 startHTML += "<body>\n";
 startHTML += tidyD(taContent);
 startHTML += "</body>\n";
 startHTML += "</html>";
 oW = o("whizzyWig").contentWindow;
 try {
	 oW.document.designMode = "on";
 } catch (e) { //not set? try again
  setTimeout('oW.designMode = "on";', 100);
 }
 if (oW.addEventListener)oW.addEventListener("keypress", kb_handler, true); //keyboard shortcuts for Moz
 oW.document.open();
 oW.document.write(startHTML);
 oW.document.close();
 if (document.frames) oW.document.designMode = "On";
 oW.focus();
 whereAmI();
} //end makeWhizzyWig


function makeButton(button){  // assemble the button requested
 var ucBut = button.substring(0,1).toUpperCase();
 ucBut += button.substring(1);
 ucBut = t(ucBut.replace(/_/g,' '));
 if (!buttonPath || buttonPath == "textbuttons")
  var butHTML = '<button type=button onClick=makeSo("'+button+'")>'+ucBut+'</button>';
 else var butHTML = '<button type=button onClick=makeSo("'+button+'")><img src="'+buttonPath+button+'.gif"  alt="'+ucBut+'" title="'+ucBut+'" onError="this.parentNode.innerHTML=this.alt"></button>';
 w(butHTML);
}

function fGo(id){ return '<div id="'+id+'_FORM" style="display:none" onkeypress="if(event.keyCode==13) return false;"><hr> '; }//new form

function fNo(txt,go){ //form do it/cancel buttons
 return ' <input type="button" onclick="'+go+'" value="'+txt+'"> <input type="button" onclick="hideDialogs();" value='+t("Cancel")+'></div>';
}

function makeSelect(select){ // assemble the <select> requested
 if (select == 'formatblock') {
  var h = "Heading";
 var values = ["<p>", "<p>", "<h1>", "<h2>", "<h3>", "<h4>", "<h5>", "<h6>", "<address>",  "<pre>"];
 var options = [t("Choose style")+":", t("Paragraph"), t(h)+" 1 ", t(h)+" 2 ", t(h)+" 3 ", t(h)+" 4 ", t(h)+" 5 ", t(h)+" 6", t("Address"), t("Fixed width<pre>")];
 } else if (select == 'fontname') {
 var values = ["Verdana, Arial, Helvetica, sans-serif", "Arial, Helvetica, sans-serif", "Comic Sans MS, fantasy", "Courier New, Courier, monospace", "Georgia, serif", "Times New Roman, Times, serif", "Verdana, Arial, Helvetica, sans-serif"];
 var options = [t("Font")+":", "Arial", "Comic", "Courier New", "Georgia", "Times New Roman", "Verdana"];
 } else if (select == 'fontsize') {
  var values = ["3", "1", "2", "3", "4", "5", "6", "7"];
  var options = [t("Font size")+":", "1 "+t("Small"), "2", "3", "4", "5", "6", "7 "+t("Big")];
 } else { 
	 var values = vals[select];
	 var options = opts[select];
	}
 w('<select id="' + select + '" onchange="doSelect(this.id);">');
 for (var i = 0; i < values.length; i++) {
  w('<option value="' + values[i] + '">' + options[i] + '</option>');
 }
 w('</select>');
}

function makeSo(command, option) {  //format selected text or line in the whizzy
 whereAmI();
 hideDialogs();
 if (!document.all) oW.document.execCommand('useCSS',false, true); //no spans for bold, italic
 if ("leftrightcenterjustify".indexOf(command) !=-1) command = "justify" + command;
 else if (command == "number") command = "insertorderedlist";
 else if (command == "bullet") command = "insertunorderedlist";
 else if (command == "rule") command = "inserthorizontalrule";
 switch (command) {
  case "color": o('cf_cmd').value="forecolor"; if (textSel()) s('COLOR_FORM'); break;
  case "hilite" : o('cf_cmd').value="backcolor"; if (textSel()) s('COLOR_FORM'); break;
  case "image" : s('IMAGE_FORM'); break;
  case "link" : if (textSel()) s('LINK_FORM'); break;
  case "html" : showHTML(); break;
  case "table" : doTable(); break;
  case "delete_row" : doRow('delete','0'); break;
  case "add_row_above" : doRow('add','0'); break;
  case "add_row_below" : doRow('add','1'); break;
  case "delete_column" : doCol('delete','0'); break;
  case "add_column_before" : doCol('add','0'); break;
  case "add_column_after" : doCol('add','1'); break;
  case "table_in_cell" : hideDialogs(); s('TABLE_FORM'); break;
  case "clean" : cleanUp(); break;
  case "spellcheck" : spellCheck(); break;
  default: oW.document.execCommand(command, false, option);
 }
 oW.focus;
}

function doSelect(selectname) {  //select on toolbar used - do it
 whereAmI();
 var idx = o(selectname).selectedIndex;
 var selected = o(selectname).options[idx].value;
 if (" _formatblock_fontname_fontsize".indexOf('_'+selectname) > 0) {
  var cmd = selectname;
  oW.document.execCommand(cmd, false, selected);
 } else {
  insHTML(selected);//insHTML(selected.replace(/insHTML/,''));
 }  
 o(selectname).selectedIndex = 0;
 oW.focus();
}

function vC(colour) { // view Colour
 if (!colour) colour = o('cf_color').value;
 o('cPrvw').style.backgroundColor = colour;
 o('cf_color').value = colour;
}

function sC(color) {  //set Color for text or background
 hideDialogs();
 var cmd = o('cf_cmd').value;
 if  (!color) color = o('cf_color').value;
 if (document.selection) rng.select(); //else IE gets lost
 if (cmd == "backcolor")  insHTML('<span style="background-color:'+color+'">');
 else insHTML('<span style="color:'+color+'">');
 oW.focus();
}

function insertLink(URL) {
 hideDialogs();
 if (!URL) URL = o("lf_url").value;
 cmd = URL ? "createlink" : "unlink";
 if (document.selection) rng.select(); //else IE gets lost
 var tgt = "";
 if (o("lf_new").checked) tgt= 'target="_blank"';
 if (URL) insHTML('<a href="'+URL+'" '+tgt+'>');
 oW.focus();
}

function insertImage(URL, side, border, margin, alt) { // insert image as specified
 hideDialogs();
 if (!URL) URL = o("if_url").value;
 if (URL) {
 if (!alt) alt = o("if_alt").value ? o("if_alt").value: URL.replace(/.*\/(.+)\..*/,"$1");
 img = '<img alt="' + alt + '" src="' + URL +'" ';
 if (!side) side = o("if_side").value;
 if ((side == "left") || (side == "right")) img += 'align="' + side + '"';
 if (!border)  border = o("if_border").value;
 if (border.match(/^\d+$/)) border+='px solid';
 if (!margin) margin = o("if_margin").value;
 if (margin.match(/^\d+$/)) margin+='px';
 if (border || margin) img += 'style="border:' + border + ';margin:' + margin + ';"';
 img += '/>';
  insHTML(img);
 }
}

function doTable(){ //show table controls if in a table, else make table
 if (trail.indexOf('TABLE') > 0) s('TABLE_CONTROLS');
  else s('TABLE_FORM');
}

function doRow(toDo,below) { //insert or delete a table row
 var paNode = papa;
 while (paNode.tagName != "TR") paNode = paNode.parentNode;
 var tRow = paNode.rowIndex;
 var tCols = paNode.cells.length;
 while (paNode.tagName != "TABLE") paNode = paNode.parentNode;
 if (toDo == "delete") paNode.deleteRow(tRow);
 else {
  var newRow = paNode.insertRow(tRow+parseInt(below)); //1=below  0=above
   for (i = 0; i < tCols; i++){
    var newCell = newRow.insertCell(i);
    newCell.innerHTML = "#";
   }
 }
}

function doCol(toDo,after) { //insert or delete a column
 var paNode = papa;
 while (paNode.tagName != 'TD') paNode = paNode.parentNode;
 var tCol = paNode.cellIndex;
 while (paNode.tagName != "TABLE") paNode = paNode.parentNode;
 var tRows = paNode.rows.length;
 for (i = 0; i < tRows; i++){
  if (toDo == "delete") paNode.rows[i].deleteCell(tCol);
  else {
   var newCell = paNode.rows[i].insertCell(tCol+parseInt(after)); //if after = 0 then before
   newCell.innerHTML = "#";
  }
 }
}

function makeTable() { //insert a table
 hideDialogs();
 var rows = o('tf_rows').value;
 var cols = o('tf_cols').value;
 var border = o('tf_border').value;
 var head = o('tf_head').value;
 if ((rows > 0) && (cols > 0)) {
  var table = '<table border="' + border + '">';
  for (var i=1; i <= rows; i++) {
   table = table + "<tr>";
   for (var j=1; j <= cols; j++) {
    if (i==1) {
     if (head=="1") table += "<th>Title"+j+"</th>"; //Title1 Title2 etc.
     else table += "<td>"+j+"</td>";
    }
    else if (j==1) table += "<td>"+i+"</td>";
   else table += "<td>#</td>";
   }
   table += "</tr>";
  }
  table += " </table>";
  insHTML(table);
 }
}

function doWin(URL) {  //popup  for browse function
 window.open(URL,'popWhizz','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width=640,height=480,top=100');
}

function spellCheck() {  //check spelling with plugin if available
 if (document.all) {
 try {
  var tmpis = new ActiveXObject("ieSpell.ieSpellExtension");
  tmpis.CheckAllLinkedDocuments(document);
 }
 catch(exception) {
  if(exception.number==-2146827859) {
  if (confirm("ieSpell is not installed on your computer. \n Click [OK] to go to download page."))
    window.open("http://www.iespell.com/download.php","DownLoad");
  } else {
   alert("Error Loading ieSpell: Exception " + exception.number);
  }
 }
 } else {
   if (confirm("Click [OK] for instructions to download and install SpellBound on your computer. \n If you already have it, click [Cancel] then right click in the edit area and select 'Check Spelling'."))
     window.open("http://spellbound.sourceforge.net/install","DownLoad");
 }
}

function cleanUp(){  //clean up crud inserted by Micro$oft Orifice
 oW.document.execCommand("removeformat",false,null);
 whereAmI();
 var h = oW.document.body.innerHTML;
 if (!gentleClean) {
	h = h.replace(/<\/?(SPAN|DEL|INS|DIR)[^>]*>/gi, "");
  h = h.replace(/\b(CLASS|STYLE)=\"?[^\"]*\"?/gi, "");
 }
 h = h.replace(/<\/?(FONT|SHAPE|V:|O:|F:|F |PATH|LOCK|IMAGEDATA|STROKE|FORMULAS)[^>]*>/gi, "");
 h = h.replace(/\bCLASS=\"?MSO\w*\"?/gi, "");
 // h = h.replace(//g,'-'); //long -
 h = h.replace(/[]/g, "'"); //single smartquotes  
 h = h.replace(/[]/g, '"'); //double smartquotes 
 h = h.replace(/align="?justify"?/gi, ""); //justify sends some browsers mad
 h = h.replace(/<(TABLE|TD)(.*)WIDTH[^A-Za-z>]*/gi, "<$1$2"); //no fixed width tables
 h = h.replace(/<([^>]+)>\s*<\/\1>/gi, ""); //empty tag
 oW.document.body.innerHTML = h;
 syncTextarea();
}

function hideDialogs() {
 h('LINK_FORM');
 h('IMAGE_FORM');
 h('COLOR_FORM');
 h('TABLE_FORM');
 h('TABLE_CONTROLS');
}

function showDesign() {
 oW.document.body.innerHTML = tidyD(o(idTa).value);
 h(idTa);
 h('showWYSIWYG');
 s('CONTROLS');
 s('whizzyWig');
 if(o("whizzyWig").contentDocument) o("whizzyWig").contentDocument.designMode = "on"; //FF loses it on hide
 oW.focus();
}

function showHTML() { 
 var t = (window.get_xhtml) ? get_xhtml(oW.document.body) : oW.document.body.innerHTML;
 o(idTa).value = tidyH(t);
 h('CONTROLS');
 h('whizzyWig');
 s(idTa);
 s('showWYSIWYG');
 o(idTa).focus();
}

function syncTextarea() { //tidy up before we go-go
 var b = oW.document.body;
 if (o(idTa).style.display == 'block') b.innerHTML = o(idTa).value;
 b.innerHTML = tidyH(b.innerHTML);
 o(idTa).value = (window.get_xhtml) ? get_xhtml(b) : b.innerHTML;
}

function tidyD(h){ //FF designmode likes <B>,<I>...
 h = h.replace(/<(\/?)strong([^>]*)>/gi, "<$1B$2>"); 
 h = h.replace(/<(\/?)em([^>]*)>/gi, "<$1I$2>");
 return h;
}

function tidyH(h){ //...but <B>,<I> deprecated
 h = h.replace(/<([^>]+)>\s*<\/\1>/gi, ""); //empty tag
 h = h.replace(/(<\/?)[Bb]>/g, "$1strong>");
 h = h.replace(/(<\/?)[Ii]>/g, "$1em>");
 h = h.replace(/(<\/?)[Bb](\s+[^>]*)>/g, "$1strong$2>");
 h = h.replace(/(<\/?)[Ii](\s+[^>]*)>/g, "$1em$2>");
 var me = 'href="'+location.href+'#'; //IE anchor bug
 h = h.replace(me,'href="#');
 return h;
}

function kb_handler(evt) { // keyboard controls for Mozilla
 var w = evt.target.id;
 if (evt.ctrlKey) {
  var key = String.fromCharCode(evt.charCode).toLowerCase();
  var cmd = '';
  switch (key) {
   case 'b': cmd = "bold"; break;
   case 'i': cmd = "italic"; break;
   case 'u': cmd = "underline"; break;
   case 'l': cmd = "link"; break;
   case 'm': cmd = "image"; break;
  };
  if (cmd) {
   makeSo(cmd, true);
   evt.preventDefault();  // stop the event bubble
   evt.stopPropagation();
  }
 }
}

function insHTML(html) { // insert arbitrary HTML at current selection
if (html.indexOf('js:') == 0) {
  eval(html.replace(/^js:/,''));
  return;
}
whereAmI();
 if (!html) html = prompt("Enter some HTML to insert:", "");
 if (!html) return;
 if (document.selection) {
  rng.select(); //else IE gets lost
  html = html + rng.htmlText;
  try { oW.document.selection.createRange().pasteHTML(html); } //
  catch (e) { }// catch error if range is bad for IE
 } else { //Moz
  if (sel) html = html + sel;
  var fragment = oW.document.createDocumentFragment();
  var div = oW.document.createElement("div");
  div.innerHTML = html;
  while (div.firstChild) {
   fragment.appendChild(div.firstChild);
  }
  sel.removeAllRanges();
  rng.deleteContents();
  var node = rng.startContainer;
  var pos = rng.startOffset;
  switch (node.nodeType) {
   case 3: if (fragment.nodeType == 3) {
    node.insertData(pos, fragment.data);
    rng.setEnd(node, pos + fragment.length);
    rng.setStart(node, pos + fragment.length);
   } else {
    node = node.splitText(pos);
    node.parentNode.insertBefore(fragment, node);
    rng.setEnd(node, pos + fragment.length);
    rng.setStart(node, pos + fragment.length);
   }
   break;
   case 1: node = node.childNodes[pos];
    node.parentNode.insertBefore(fragment, node);
    rng.setEnd(node, pos + fragment.length);
    rng.setStart(node, pos + fragment.length);
   break;
  }
  sel.addRange(rng);
 }
 oW.focus();
}

function whereAmI() {//get current selected range if available 
 oW.focus();
 if (document.all) { //IE
  sel = oW.document.selection;
  if (sel != null) {
   rng = sel.createRange();
   switch (sel.type) {
    case "Text":case "None":
     papa = rng.parentElement(); break;
    case "Control":
     papa = rng.item(0); break;
    default:
     papa = frames['whizzyWig'].document.body;
   }
   var paNode = papa;
   trail = papa.tagName + '>' +sel.type;
   while (paNode.tagName != 'BODY') {
    paNode = paNode.parentNode;
    trail = paNode.tagName + '>' + trail;
   }
   window.status = trail;
  }
 } else { //Moz
  sel = oW.getSelection();
  if (sel != null) rng = sel.getRangeAt(sel.rangeCount - 1).cloneRange();
 }
}

function textSel() {
	if (sel != "") return true; 
	else {alert(t("Select some text first")); return false;}
}
function s(id) {o(id).style.display = 'block';} //show element
function h(id) {o(id).style.display = 'none';} //hide element
function o(id) { return document.getElementById(id); } //get element by ID
function w(str) { return document.write(str); } //document write
function t(key) {return (window.language && language[key]) ? language[key] :  key;} //translation
