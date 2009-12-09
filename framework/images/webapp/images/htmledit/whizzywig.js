var whizzywig_version='Whizzywig v59'; //briefer whereami, xC(); paNode=>pa; hilite in FF; whizzybrowse;
//Copyright © 2005-2009 John Goodman - www.unverse.net  *date 091104
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 

var btn=[]; //object containing button strip information 
 btn._w=16; btn._h=16; btn._f="/images/htmledit/WhizzywigToolbar.png"; //set to path to toolbar image
var buttonPath;  //path to custom toolbar button images; "textbuttons" means don't use images; default tries btn[_f]
var buttonExt;   //extension (e.g. .png) for toolbar button images;  default '.gif'
var cssFile;     //url of CSS stylesheet to attach to edit area
var imageBrowse; //path to page for image browser
var linkBrowse;  //path to page for link browser
var idTa;        //id of the textarea (param to makeWhizzyWig)
//OTHER GLOBALS
var oW, sel, rng, papa, trail, ppw, wn=window; //Whizzy contentWindow, current sel, range, parent, DOM path, popwindow;
var sels='';
var buts=''; 
var vals=new Array();
var opts=new Array();
var dobut=new Array();
var whizzies=new Array();

function makeWhizzyWig(txtArea, controls){ // make a WhizzyWig from the textarea
 idTa=txtArea;
 whizzies[whizzies.length]=idTa;
 if (!document.designMode) {
  if (idTa.nodeName=="TEXTAREA") tagButs();
  alert("Whizzywig "+t("editor not available for your browser"));
  return;
 }
 var taContent=o(idTa).defaultValue ? o(idTa).defaultValue : o(idTa).innerHTML ? o(idTa).innerHTML: ''; //anything in the textarea?
 //this line is commented as it is not allowing bigger textarea than rows 
 //if (!o(idTa).rows < 5) o(idTa).rows='15';//IE won't use % from style
 taWidth=o(idTa).style.width ? o(idTa).style.width : o(idTa).cols + "ex";  //grab the width and...
 taHeight=o(idTa).style.height ? o(idTa).style.height : o(idTa).rows + "em";  //...height from the textarea
 if (o(idTa).nodeName=="TEXTAREA"){
 o(idTa).style.color='#060';
 o(idTa).style.zIndex='2';
 } else w('<input type="hidden" id="wzhid_'+idTa+'" name="'+idTa+'" />');
 h(idTa);
 var frm=o(idTa).parentNode;
 while (frm.nodeName != 'FORM') frm=frm.parentNode;//if not form, keep trying
 addEvt(frm,"submit",syncTextarea);
 w('<style type="text/css">button {vertical-align:middle;padding:0;margin:1px 0} button img{vertical-align:middle;margin:-1px} select{vertical-align:middle;margin:1px}  .wzCtrl {background:ButtonFace; border:2px outset ButtonShadow; padding:5px;} #sourceTa{color:#060;font-family:mono;} #whizzyWig {border-width:1px}</style>');
 var dsels='formatblock';
 if (!navigator.userAgent.match('AppleWebKit')){dsels+=' fontname fontsize'} //Font u/s on Chrome/Safari
 var dbuts=' bold italic underline | left center right justify | number bullet indent outdent | undo redo | color hilite rule | link image table | clean html spellcheck ';
 btns=dbuts.split(' ');
 for (var i=0, pos=0; i < btns.length; i++) {
  if(btns[i] && btns[i]!='|') {btn[btns[i]]=btn._w*pos++};
 }
 var tbuts=' tstart add_row_above add_row_below delete_row | add_column_before add_column_after delete_column | table_in_cell';
 var t_end=''; //table controls end, if needed
 controls=controls ? controls.toLowerCase() : "all";
 if (controls == "all") controls=dsels +' newline '+ buts + dbuts + tbuts;
 else controls += tbuts;
 w('<div style="width:'+taWidth+'" onmouseover="c(\''+idTa+'\')"><div id="CONTROLS'+idTa+'" class="wzCtrl" unselectable="on">');
 gizmos=controls.split(' ');
 for (var i=0; i < gizmos.length; i++) {
  if (gizmos[i]){ //make buttons and selects for toolbar, in order requested
   if (gizmos[i] == 'tstart') {
    w('<div id="TABLE_CONTROLS'+idTa+'" style="display:none" unselectable="on">');
    t_end='</div>';
   }
   else if (gizmos[i] == '|') w('&nbsp;<big style="padding-bottom:2em">|</big>&nbsp;');
   else if (gizmos[i] == 'newline') w('<br>');
   else if ((dsels+sels).indexOf(gizmos[i]) != -1) makeSelect(gizmos[i])
   else if ((dbuts+buts+tbuts).indexOf(gizmos[i]) != -1) makeButton(gizmos[i]);
  }
 }
 w(t_end) //table controls end
 w('<a href="http://www.unverse.net" style="color:buttonface" title="'+whizzywig_version+'">.</a> ');
 w(fGo('LINK'));
 if (linkBrowse) w('<input type="button" onclick=doWin("'+linkBrowse+'"); value="'+t("Browse")+'"> ');
 w(t('Link address (URL)')+': <input type="text" id="lf_url'+idTa+'" size="60"><br><input type="button" value="http://" onclick="o(\'lf_url'+idTa+'\').value=\'http://\'+o(\'lf_url'+idTa+'\').value"> <input type="button" value="mailto:" onclick="o(\'lf_url'+idTa+'\').value=\'mailto:\'+o(\'lf_url'+idTa+'\').value"><input type="checkbox" id="lf_new'+idTa+'">'+t("Open link in new window")+fNo(t("OK"),"insertLink()"));//LINK_FORM end
 w(fGo('IMAGE'));
 if (imageBrowse) w('<input type="button" onclick=doWin("'+imageBrowse+'"); value="'+t("Browse")+'"> ');
 w(t('Image address (URL)')+': <input type="text" id="if_url'+idTa+'" size="50"> <label title='+t("to display if image unavailable")+'><br>'+t("Alternate text")+':<input id="if_alt'+idTa+'" type="text" size="50"></label><br>'+t("Align")+':<select id="if_side'+idTa+'"><option value="none">_&hearts;_ '+t("normal")+'</option><option value="left">&hearts;= &nbsp;'+t("left")+'</option><option value="right">=&hearts; &nbsp;'+t("right")+'</option></select> '+t("Border")+':<input type="text" id="if_border'+idTa+'" size="20" value="0" title="'+t("number or CSS e.g. 3px maroon outset")+'"> '+t("Margin")+':<input type="text" id="if_margin'+idTa+'" size="20" value="0" title="'+t("number or CSS e.g. 5px 1em")+'">'+fNo(t("Insert Image"),"insertImage()"));//IMAGE_FORM end
 w(fGo('TABLE')+t("Rows")+':<input type="text" id="tf_rows'+idTa+'" size="2" value="3"> <select id="tf_head'+idTa+'"><option value="0">'+t("No header row")+'</option><option value="1">'+t("Include header row")+'</option></select> '+t("Columns")+':<input type="text" id="tf_cols'+idTa+'" size="2" value="3"> '+t("Border width")+':<input type="text" id="tf_border'+idTa+'" size="2" value="1"> '+fNo(t("Insert Table"),"makeTable()"));//TABLE_FORM end
 w(fGo('COLOR')+'<input type="hidden" id="cf_cmd'+idTa+'"><div style="background:#000;padding:1px;height:22px;width:125px;float:left"><div id="cPrvw'+idTa+'" style="background-color:red; height:100%; width:100%"></div></div> <input type=text id="cf_color'+idTa+'" value="red" size=17 onpaste=vC(value) onblur=vC(value)> <input type="button" onmouseover=vC() onclick=sC() value="'+t("OK")+'">  <input type="button" onclick="hideDialogs();" value="'+t("Cancel")+'"><br> '+t("click below or enter a")+' <a href="http://www.unverse.net/colortable.htm" target="_blank">'+t("color name")+'</a><br clear=all> <table border=0 cellspacing=1 cellpadding=0 width=480 bgcolor="#000000">'+"\n");
 var wC=new Array("00","33","66","99","CC","FF")  //color table
 for (i=0; i<wC.length; i++){
  w("<tr>");
  for (j=0; j<wC.length; j++){
   for (k=0; k<wC.length; k++){
    var clr=wC[i]+wC[j]+wC[k];
    w(' <td style="background:#'+clr+';height:12px;width:12px" onmouseover=vC("#'+clr+'") onclick=sC("#'+clr+'")></td>'+"\n");
   }
  }
  w('</tr>');
 }
 w("</table></div>\n"); //end color table,COLOR_FORM
 w("</div>\n"); //controls end
 w('<div class="wzCtrl" id="showWYSIWYG'+idTa+'" style="display:none"><input type="button" onclick="showDesign();" value="'+t("Hide HTML")+'">');
 tagButs();
 w('</div>'+"\n");
 
 w('<iframe style="border:1px inset ButtonShadow;width:100%;height:'+taHeight+'" src="javascript:;" id="whizzy'+idTa+'"></iframe></div>'+"\n");

 var startHTML="<html>\n<head>\n";
 if (cssFile) startHTML += '<link media="all" type="text/css" href="'+cssFile+'" rel="stylesheet">\n';
 startHTML += '</head>\n<body id="'+idTa+'" style="background-image:none">\n'+tidyD(taContent)+'</body>\n</html>';
 oW=o("whizzy"+idTa).contentWindow;

 var d=oW.document;
 try{d.designMode="on";} catch(e){ setTimeout('oW.designMode="on";', 100);}
 d.open(); d.write(startHTML); d.close();
 if (oW.addEventListener) oW.addEventListener("keypress", kb_handler, true); //keyboard shortcuts for Moz
 else {d.body.attachEvent("onpaste",function(){setTimeout('cleanUp()',10);});}
 addEvt(d,"mouseup", whereAmI); addEvt(d,"keyup", whereAmI); addEvt(d,"dblclick", doDbl);
 idTa=null;
} //end makeWhizzyWig
function addEvt(o,e,f){ if(wn.addEventListener) o.addEventListener(e, f, false); else o.attachEvent("on"+e,f);}
function doDbl(){if (papa.nodeName == 'IMG') doImage(); else if (papa.nodeName == 'A') doLink();}
function makeButton(button){  // assemble the button requested
 var butHTML, ucBut=button.substring(0,1).toUpperCase();
 ucBut += button.substring(1);
 ucBut=t(ucBut.replace(/_/g,' '));
 if (!document.frames && (button=="spellcheck")) return; //Not allowed from Firefox
 if (o(idTa).nodeName!="TEXTAREA" && button=="html") return; 
 if (!buttonExt) buttonExt='.gif';
 if (buttonPath == "textbuttons")
   butHTML='<button type=button onClick=makeSo("'+button+'")>'+ucBut+"</button>\n";
 else butHTML='<button  title="'+ucBut+'" type=button onClick=makeSo("'+button+'")>'+(btn[button]!=undefined?'<div style="width:'+btn._w+'px;height:'+btn._h+'px;background-image:url('+btn._f+');background-position:-'+btn[button]+'px 0px"></div>':'<img src="'+buttonPath+button+buttonExt+'" alt="'+ucBut+'" onError="this.parentNode.innerHTML=this.alt">')+'</button>\n';
 w(butHTML);
}
function fGo(id){ return '<div id="'+id+'_FORM'+idTa+'" unselectable="on" style="display:none" onkeypress="if(event.keyCode==13) {return false;}"><hr>'+"\n"; }//new form
function fNo(txt,go){ //form do it/cancel buttons
 return ' <input type="button" onclick="'+go+'" value="'+txt+'"> <input type="button" onclick="hideDialogs();" value='+t("Cancel")+"></div>\n";
}
function makeSelect(select){ // assemble the <select> requested
 if (select == 'formatblock') {
  var h="Heading";
 var values=["<p>", "<p>", "<h1>", "<h2>", "<h3>", "<h4>", "<h5>", "<h6>", "<address>",  "<pre>"];
 var options=[t("Choose style")+":", t("Paragraph"), t(h)+" 1 ", t(h)+" 2 ", t(h)+" 3 ", t(h)+" 4 ", t(h)+" 5 ", t(h)+" 6", t("Address"), t("Fixed width<pre>")];
 } else if (select == 'fontname') {
var values=["Arial, Helvetica, sans-serif", "Arial, Helvetica, sans-serif","'Arial Black', Helvetica, sans-serif", "'Comic Sans MS' fantasy", "Courier New, Courier, monospace", "Georgia, serif", "Impact,sans-serif","'Times New Roman', Times, serif", "'Trebuchet MS',sans-serif", "Verdana, Arial, Helvetica, sans-serif"];
 var options=[t("Font")+":", "Arial","Arial Black", "Comic", "Courier", "Georgia", "Impact","Times New Roman", "Trebuchet","Verdana"];
 } else if (select == 'fontsize') {
  var values=["3", "1", "2", "3", "4", "5", "6", "7"];
  var options=[t("Font size")+":", "1 "+t("Small"), "2", "3", "4", "5", "6", "7 "+t("Big")];
 } else { 
  var values=vals[select];
  var options=opts[select];
 }
 w('<select id="'+select+idTa+'" onchange="doSelect(this.id);">'+"\n");
 for (var i=0; i < values.length; i++) {
  w(' <option value="' + values[i] + '">' + options[i] + "</option>\n");
 }
 w("</select>\n");
}
function tagButs() {
 w('<input type="button" onclick=\'doTag("<h1>")\' value="H1" title="<H1>"><input type="button" onclick=\'doTag("<h2>")\' value="H2" title="<H2>"><input type="button" onclick=\'doTag("<h3>")\' value="H3" title="<H3>"><input type="button" onclick=\'doTag("<h4>")\' value="H4" title="<H4>"><input type="button" onclick=\'doTag("<p>")\' value="P" title="<P>"><input type="button" onclick=\'doTag("<strong>")\' value="S" title="<STRONG>" style="font-weight:bold"><input type="button" onclick=\'doTag("<em>")\' value="E" title="<EM>" style="font-style:italic;"><input type="button" onclick=\'doTag("<li>")\' value="&bull;&mdash;" title="<LI>"><input type="button" onclick=\'doTag("<a>")\' value="@" title="<A HREF= >"><input type="button" onclick=\'doTag("<img>")\' value="[&hearts;]" title="<IMG SRC= >"><input type="button" onclick=\'doTag("<br />")\' value="&larr;" title="<BR />">');
}
function xC(c,o){return oW.document.execCommand(c,false,o);}
function makeSo(cm,op) {  //format selected text or line in the whizzy
 hideDialogs();
 oW.focus();
 if(dobut[cm]) {insHTML(dobut[cm]); return;}
 if (/Firefox/.test(navigator.userAgent)) {xC("styleWithCSS",cm=="hilite")} //no spans for bold, italic, ok hilite
 if (cm == "justify") cm = "full";
 if ("leftrightcenterfull".indexOf(cm) !=-1) cm="justify" + cm;
 else if (cm == "number") cm="insertorderedlist";
 else if (cm == "bullet") cm="insertunorderedlist";
 else if (cm == "rule") cm="inserthorizontalrule";
 switch (cm) {
  case "color": o('cf_cmd'+idTa).value="forecolor"; if (textSel()) s('COLOR_FORM'+idTa); break;
  case "hilite" : o('cf_cmd'+idTa).value=cm; if (textSel()) s('COLOR_FORM'+idTa); break;
  case "image" : doImage(); break;
  case "link" : doLink(); break;
  case "html" : showHTML(); break;
  case "table" : doTable(); break;
  case "delete_row" : doRow('delete','0'); break;
  case "add_row_above" : doRow('add','0'); break;
  case "add_row_below" : doRow('add','1'); break;
  case "delete_column" : doCol('delete','0'); break;
  case "add_column_before" : doCol('add','0'); break;
  case "add_column_after" : doCol('add','1'); break;
  case "table_in_cell" : hideDialogs(); s('TABLE_FORM'+idTa); break;
  case "clean" : cleanUp(); break;
  case "spellcheck" : spellCheck(); break;
  default: xC(cm,op);
 }
 oW.focus();
}
function doSelect(selectname) {  //select on toolbar used - do it
 var idx=o(selectname).selectedIndex;
 var selected=o(selectname).options[idx].value;
 o(selectname).selectedIndex=0;
 selectname=selectname.replace(idTa,"");
 if (" _formatblock_fontname_fontsize".indexOf('_'+selectname) > 0) {
  var cmd=selectname;
  oW.focus();
  xC(cmd,selected);
 } else {
  insHTML(selected);
 }  
 oW.focus();
}
function vC(colour) { // view Color
 if (!colour) colour=o('cf_color'+idTa).value;
 o('cPrvw'+idTa).style.backgroundColor=colour;
 o('cf_color'+idTa).value=colour;
}
function sC(color) {  //set Color
 hideDialogs();
 var cmd=o('cf_cmd'+idTa).value;
 if  (!color) color=o('cf_color'+idTa).value;
 if (rng) rng.select();
 if (cmd=="hilite") try {xC("hilitecolor",color);} catch(e){xC("backcolor",color);}
 else {xC(cmd,color);}
 oW.focus();
}
function doLink(){
 if (textSel()) {
  if (papa.nodeName == 'A') o("lf_url"+idTa).value=papa.href;
  s('LINK_FORM'+idTa);
 }
}
function insertLink(url) {
 if (rng) rng.select();
 URL=url ? url : o("lf_url"+idTa).value; 
 if (URL.replace(/ /g,"") == "") xC('Unlink',null);
 else if (o("lf_new"+idTa).checked) insHTML('<a href="'+URL+'" target="_blank">');
 else xC('CreateLink',URL);
 hideDialogs();
}
function doImage(){
 if (papa && papa.nodeName == 'IMG'){
  o("if_url"+idTa).value=papa.src;
  o("if_alt"+idTa).value=papa.alt;
  o("if_side"+idTa).selectedIndex=(papa.align=="left")?1:(papa.align=="right")?2:0; 
  o("if_border"+idTa).value=papa.style.border?papa.style.border:papa.border>0?papa.border:0;
  o("if_margin"+idTa).value=papa.style.margin?papa.style.margin:papa.hspace>0?papa.hspace:0;
 }
 s('IMAGE_FORM'+idTa);
}
function insertImage(URL, side, border, margin, alt) { // insert image as specified
 hideDialogs();
 if (!URL) URL=o("if_url"+idTa).value;
 if (URL) {
  if (!alt) alt=o("if_alt"+idTa).value ? o("if_alt"+idTa).value: URL.replace(/.*\/(.+)\..*/,"$1");
  img='<img alt="' + alt + '" src="' + URL +'" ';
  if (!side) side=o("if_side"+idTa).value;
  if ((side == "left") || (side == "right")) img += 'align="' + side + '"';
  if (!border)  border=o("if_border"+idTa).value;
  if (border.match(/^\d+$/)) border+='px solid';
  if (!margin) margin=o("if_margin"+idTa).value;
  if (margin.match(/^\d+$/)) margin+='px';
  if (border || margin) img += ' style="border:' + border + ';margin:' + margin + ';"';
  img += '/>';
  insHTML(img);
 }
}
function doTable(){ //show table controls if in a table, else make table
 if (trail && trail.indexOf('TABLE') > 0) s('TABLE_CONTROLS'+idTa);
  else s('TABLE_FORM'+idTa);
}
function doRow(toDo,below) { //insert or delete a table row
 var pa=papa;
 while (pa.tagName != "TR") pa=pa.parentNode;
 var tRow=pa.rowIndex;
 var tCols=pa.cells.length;
 while (pa.tagName != "TABLE") pa=pa.parentNode;
 if (toDo == "delete") pa.deleteRow(tRow);
 else {
  var newRow=pa.insertRow(tRow+parseInt(below)); //1=below  0=above
   for (i=0; i < tCols; i++){
    var newCell=newRow.insertCell(i);
    newCell.innerHTML="#";
   }
 }
}
function doCol(toDo,after) { //insert or delete a column
 var pa=papa;
 while (pa.tagName != 'TD') pa=pa.parentNode;
 var tCol=pa.cellIndex;
 while (pa.tagName != "TABLE") pa=pa.parentNode;
 var tRows=pa.rows.length;
 for (i=0; i < tRows; i++){
  if (toDo == "delete") pa.rows[i].deleteCell(tCol);
  else {
   var newCell=pa.rows[i].insertCell(tCol+parseInt(after)); //if after=0 then before
   newCell.innerHTML="#";
  }
 }
}
function makeTable() { //insert a table
 hideDialogs();
 var rows=o('tf_rows'+idTa).value, cols=o('tf_cols'+idTa).value, border=o('tf_border'+idTa).value, head=o('tf_head'+idTa).value;
 if ((rows > 0) && (cols > 0)) {
  var table='<table border="' + border + '">';
  for (var i=1; i <= rows; i++) {
   table=table + "<tr>";
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
 ppw=wn.open(URL,'popWhizz','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width=640,height=480,top=100');
 ppw.focus();
}
function spellCheck() {  //check spelling with ieSpell if available
 try {
  var axo=new ActiveXObject("ieSpell.ieSpellExtension");
  axo.CheckAllLinkedDocuments(document);
 } catch(e) {
  if(e.number==-2146827859) {
  if (confirm("ieSpell is not installed on your computer. \n Click [OK] to go to download page."))
    wn.open("http://www.iespell.com/download.php","DownLoad");
  } else {
   alert("Error Loading ieSpell: Exception " + e.number);
  }
 }
}
function hideDialogs() {
 h('LINK_FORM'+idTa); h('IMAGE_FORM'+idTa); h('COLOR_FORM'+idTa); h('TABLE_FORM'+idTa); h('TABLE_CONTROLS'+idTa);
}
function showDesign() {
 oW.document.body.innerHTML=tidyD(o(idTa).value);
 h(idTa); h('showWYSIWYG'+idTa); s('CONTROLS'+idTa); s('whizzy'+idTa);
 if(o("whizzy"+idTa).contentDocument) o("whizzy"+idTa).contentDocument.designMode="on"; //FF loses it on hide
 oW.focus();
}
function showHTML() { 
 o(idTa).value=tidyH(oW.document);
 h('CONTROLS'+idTa); h('whizzy'+idTa); s(idTa); s('showWYSIWYG'+idTa);
 o(idTa).focus();
}

function syncTextarea() { //tidy up before we go-go
 for (var i=0;i<whizzies.length;i++){
  var t=whizzies[i];
  var d=o("whizzy"+t).contentWindow.document;
  if (o(t).style.display == 'block') d.body.innerHTML=o(t).value;
  var ret=(o(t).nodeName!="TEXTAREA") ? o('wzhid_'+o(t).id) : o(t);
  ret.value=tidyH(d);
 }
}
function cleanUp(){ xC("removeformat",null); tidyH(oW.document);}
function tidyD(h){ //FF designmode likes <B>,<I>...
 h=h.replace(/<(\/?)strong([^>]*)>/gi, "<$1B$2>"); 
 h=h.replace(/<(\/?)em>/gi, "<$1I>");
 return h;
}
function tidyH(d){ //attempt valid xhtml
 function lc(str){return str.toLowerCase()}
 function qa(str){return str.replace(/(\s+\w+=)([^"][^>\s]*)/gi,'$1"$2"');}
 function sa(str){return str.replace(/("|;)\s*[A-Z-]+\s*:/g,lc);}
 var sz=new Array('medium','xx-small','x-small','small','medium','large','x-large','xx-large');
 fs=d.getElementsByTagName("FONT");
 for (var i=0;i<fs.length;i++){
  if (fs[i].face) {fs[i].style.fontFamily = fs[i].face; fs[i].removeAttribute('face');}
  if (fs[i].size) {fs[i].style.fontSize = sz[fs[i].size]; fs[i].removeAttribute('size');} 
  if (fs[i].color) {fs[i].style.color = fs[i].color; fs[i].removeAttribute('color');}
 }
 ih=d.body.innerHTML;
 ih=ih.replace(/(<\/?)FONT([^>]*)/gi,"$1span$2") 
 .replace(/(<\/?)[Bb](\s+[^>]*)?>/g, "$1strong$2>")
 .replace(/(<\/?)[Ii](\s+[^>]*)?>/g, "$1em$2>")
 .replace(/<\/?(COL|XML|ST1|SHAPE|V:|O:|F:|F |PATH|LOCK|IMAGEDATA|STROKE|FORMULAS)[^>]*>/gi, "")
 .replace(/\bCLASS="?(MSOw*|Apple-style-span)"?/gi,"")
 .replace(/<[^>]+=[^>]+>/g,qa) //quote all atts
 .replace(/[–]/g,'-') //long –
 .replace(/[‘’]/g, "'") //single smartquotes ‘’ 
 .replace(/[“�?]/g, '"') //double smartquotes “�?
 .replace(/<(TABLE|TD|TH|COL)(.*)(WIDTH|HEIGHT)=["'0-9A-Z]*/gi, "<$1$2") //no fixed size tables (%OK) [^A-Za-z>]
 .replace(/<([^>]+)>\s*<\/\1>/gi, "") //empty tag
 .replace(/>(<\/?[C-Z])/gi,">\n$1") //newline adjacent tags
 .replace(/(<BR ?\/?>)([^\r\n])/gi,"$1\n$2") //newline on BR
 .replace(/([^\n])<(P|DIV|TABLE|FORM)/gi,"$1\n\n<$2") //add white space
 .replace(/\n<(LI|OPTION|TR|INPUT)/gi,"\n <$1") //indent..
 .replace(/\n<(TD|TH)/gi,"\n  <$1") //..more
 .replace(location.href+'#','#') //IE anchor bug
 .replace(/<(IMG|INPUT|BR|HR|LINK|META)([^>]*)>/gi,"<$1$2 />")  //self-close tags
 .replace(/(<\/?[A-Z]*)/g,lc) //lowercase tags...
 .replace(/style="[^"]*"/gi,sa) //and style atts
 return ih; 
}
function kb_handler(e) { // keyboard controls for Moz
 var cmd=false, prm=false;
 if (e && (e.ctrlKey && e.keyCode == e.DOM_VK_V)||(e.shiftKey && e.keyCode == e.DOM_VK_INSERT))
  {setTimeout('cleanUp()',10);}
 else if (e && e.keyCode==13 && !e.shiftKey &&papa.nodeName=="BODY") {cmd="formatblock";prm="<p>";}
 else if (e && e.ctrlKey) {
  var k=String.fromCharCode(e.charCode).toLowerCase();
  var cmd=(k=='b')?'bold':(k=='i')?'italic':(k=='l')?'link':(k=='m')?'image':false;
 }
 if (cmd) {
  makeSo(cmd, prm);
  e.preventDefault();  // stop the event bubble
  e.stopPropagation();
 }
}
function doTag(html) { // insert HTML into text area
 var url;
 if (!html) html=prompt("Enter some HTML or text to insert:", "");
 o(idTa).focus();
 if (html == '<a>') {
  url=prompt("Link address:","http://"); 
  html='<a href="'+url+'">';
 }
 if (html == '<img>') {
  url=prompt("Address of image:","http://"); 
  var alt=prompt("Description of image");
  html ='<img src="'+url+'" alt="'+alt+'">';
 }
 var close='';
 if (html.indexOf('<') == 0 && html.indexOf('br') != 1 && html.indexOf('img') != 1)
  close=html.replace(/<([a-z0-6]+).*/,"<\/$1>");
 if (html != '<strong>' && html != '<em>') close += '\n';
 if (document.selection) {
  sel=document.selection.createRange();
  sel.text=html+sel.text+close;
 } else {
   before=o(idTa).value.slice(0,o(idTa).selectionStart);
   sel=o(idTa).value.slice(o(idTa).selectionStart,o(idTa).selectionEnd);
   after=o(idTa).value.slice(o(idTa).selectionEnd);
   o(idTa).value =before+html+sel+close+after;
 }
 o(idTa).focus(); 
}
function insHTML(html) { //insert HTML at current selection
 if (!html) html=prompt(t("Enter some HTML or text to insert:"), "");
 if (html.indexOf('js:') == 0) {
  try{eval(html.replace(/^js:/,''))} catch(e){}; return;
 }
 try {if (sel.type && sel.type!="Text") sel=""; xC("inserthtml",html+sel);}
 catch (e) {if (document.selection) {
  if (papa && papa.nodeName == 'IMG') {papa.outerHTML=html;}
  else if(rng) {rng.select(); rng.pasteHTML(html+rng.htmlText);}
 } }
 whereAmI();
}
function whereAmI(e){
 if (!e) var e=wn.event;
 var mu=e&&e.type=='mouseup';
 if (wn.getSelection){
  sel=oW.getSelection(),sn=sel.anchorNode;
  papa=mu?e.target:(sn.nodeName == '#text')?sn.parentNode:sn;
 } else { 
  sel=oW.document.selection;
  rng=sel.createRange();
  papa=mu?e.srcElement:(sel.type == "Control")?rng.item(0):rng.parentElement();
 }
 var pa=papa;
 trail=papa.nodeName; 
 while (!pa.nodeName.match(/^(HTML|BODY)/) && pa.className!="wzCtrl") {
  pa=pa.parentNode;
  trail=pa.nodeName + '>' + trail;
 }
 if (pa.className=="wzCtrl") trail=sel=rng=null;
 var id=pa.nodeName=="HTML" ? pa.getElementsByTagName("BODY")[0].id : pa.id.replace("CONTROL","");
 c(id); 
 wn.status=id+":"+trail;
 if (trail.indexOf('TABLE') > 0) s('TABLE_CONTROLS'+idTa); else h('TABLE_CONTROLS'+idTa);
}
function c(id) {//set current whizzy
 if (id=="" || whizzies.join().indexOf(id)=='-1') return;
 if (id!=idTa){
  idTa=id;
  try {oW=o("whizzy"+id).contentWindow;} catch(e){alert('set current: '+id);}
  if (oW) {if(oW.focus)oW.focus();wn.status=oW.document.body.id; }
 }
} 
function textSel() { if (sel  && sel != "" && sel.type != "None") return true;  else {alert(t("Select some text first")); return false;}}
function s(id) {o(id).style.display='block';} //show element
function h(id) {o(id).style.display='none';} //hide element
function o(id) { return document.getElementById(id); } //get element by ID
function w(str) { return document.write(str); } //document write
function t(key) {return (wn.language && language[key]) ? language[key] :  key;} //translation