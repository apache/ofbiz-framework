/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

/*
	This is a compiled version of Dojo, built for deployment and not for
	development. To get an editable version, please visit:

		http://dojotoolkit.org

	for documentation and information on getting the source.
*/

if(typeof dojo=="undefined"){
var dj_global=this;
var dj_currentContext=this;
function dj_undef(_1,_2){
return (typeof (_2||dj_currentContext)[_1]=="undefined");
}
if(dj_undef("djConfig",this)){
var djConfig={};
}
if(dj_undef("dojo",this)){
var dojo={};
}
dojo.global=function(){
return dj_currentContext;
};
dojo.locale=djConfig.locale;
dojo.version={major:0,minor:4,patch:1,flag:"",revision:Number("$Rev$".match(/[0-9]+/)[0]),toString:function(){
with(dojo.version){
return major+"."+minor+"."+patch+flag+" ("+revision+")";
}
}};
dojo.evalProp=function(_3,_4,_5){
if((!_4)||(!_3)){
return undefined;
}
if(!dj_undef(_3,_4)){
return _4[_3];
}
return (_5?(_4[_3]={}):undefined);
};
dojo.parseObjPath=function(_6,_7,_8){
var _9=(_7||dojo.global());
var _a=_6.split(".");
var _b=_a.pop();
for(var i=0,l=_a.length;i<l&&_9;i++){
_9=dojo.evalProp(_a[i],_9,_8);
}
return {obj:_9,prop:_b};
};
dojo.evalObjPath=function(_e,_f){
if(typeof _e!="string"){
return dojo.global();
}
if(_e.indexOf(".")==-1){
return dojo.evalProp(_e,dojo.global(),_f);
}
var ref=dojo.parseObjPath(_e,dojo.global(),_f);
if(ref){
return dojo.evalProp(ref.prop,ref.obj,_f);
}
return null;
};
dojo.errorToString=function(_11){
if(!dj_undef("message",_11)){
return _11.message;
}else{
if(!dj_undef("description",_11)){
return _11.description;
}else{
return _11;
}
}
};
dojo.raise=function(_12,_13){
if(_13){
_12=_12+": "+dojo.errorToString(_13);
}else{
_12=dojo.errorToString(_12);
}
try{
if(djConfig.isDebug){
dojo.hostenv.println("FATAL exception raised: "+_12);
}
}
catch(e){
}
throw _13||Error(_12);
};
dojo.debug=function(){
};
dojo.debugShallow=function(obj){
};
dojo.profile={start:function(){
},end:function(){
},stop:function(){
},dump:function(){
}};
function dj_eval(_15){
return dj_global.eval?dj_global.eval(_15):eval(_15);
}
dojo.unimplemented=function(_16,_17){
var _18="'"+_16+"' not implemented";
if(_17!=null){
_18+=" "+_17;
}
dojo.raise(_18);
};
dojo.deprecated=function(_19,_1a,_1b){
var _1c="DEPRECATED: "+_19;
if(_1a){
_1c+=" "+_1a;
}
if(_1b){
_1c+=" -- will be removed in version: "+_1b;
}
dojo.debug(_1c);
};
dojo.render=(function(){
function vscaffold(_1d,_1e){
var tmp={capable:false,support:{builtin:false,plugin:false},prefixes:_1d};
for(var i=0;i<_1e.length;i++){
tmp[_1e[i]]=false;
}
return tmp;
}
return {name:"",ver:dojo.version,os:{win:false,linux:false,osx:false},html:vscaffold(["html"],["ie","opera","khtml","safari","moz"]),svg:vscaffold(["svg"],["corel","adobe","batik"]),vml:vscaffold(["vml"],["ie"]),swf:vscaffold(["Swf","Flash","Mm"],["mm"]),swt:vscaffold(["Swt"],["ibm"])};
})();
dojo.hostenv=(function(){
var _21={isDebug:false,allowQueryConfig:false,baseScriptUri:"",baseRelativePath:"",libraryScriptUri:"",iePreventClobber:false,ieClobberMinimal:true,preventBackButtonFix:true,delayMozLoadingFix:false,searchIds:[],parseWidgets:true};
if(typeof djConfig=="undefined"){
djConfig=_21;
}else{
for(var _22 in _21){
if(typeof djConfig[_22]=="undefined"){
djConfig[_22]=_21[_22];
}
}
}
return {name_:"(unset)",version_:"(unset)",getName:function(){
return this.name_;
},getVersion:function(){
return this.version_;
},getText:function(uri){
dojo.unimplemented("getText","uri="+uri);
}};
})();
dojo.hostenv.getBaseScriptUri=function(){
if(djConfig.baseScriptUri.length){
return djConfig.baseScriptUri;
}
var uri=new String(djConfig.libraryScriptUri||djConfig.baseRelativePath);
if(!uri){
dojo.raise("Nothing returned by getLibraryScriptUri(): "+uri);
}
var _25=uri.lastIndexOf("/");
djConfig.baseScriptUri=djConfig.baseRelativePath;
return djConfig.baseScriptUri;
};
(function(){
var _26={pkgFileName:"__package__",loading_modules_:{},loaded_modules_:{},addedToLoadingCount:[],removedFromLoadingCount:[],inFlightCount:0,modulePrefixes_:{dojo:{name:"dojo",value:"src"}},setModulePrefix:function(_27,_28){
this.modulePrefixes_[_27]={name:_27,value:_28};
},moduleHasPrefix:function(_29){
var mp=this.modulePrefixes_;
return Boolean(mp[_29]&&mp[_29].value);
},getModulePrefix:function(_2b){
if(this.moduleHasPrefix(_2b)){
return this.modulePrefixes_[_2b].value;
}
return _2b;
},getTextStack:[],loadUriStack:[],loadedUris:[],post_load_:false,modulesLoadedListeners:[],unloadListeners:[],loadNotifying:false};
for(var _2c in _26){
dojo.hostenv[_2c]=_26[_2c];
}
})();
dojo.hostenv.loadPath=function(_2d,_2e,cb){
var uri;
if(_2d.charAt(0)=="/"||_2d.match(/^\w+:/)){
uri=_2d;
}else{
uri=this.getBaseScriptUri()+_2d;
}
if(djConfig.cacheBust&&dojo.render.html.capable){
uri+="?"+String(djConfig.cacheBust).replace(/\W+/g,"");
}
try{
return !_2e?this.loadUri(uri,cb):this.loadUriAndCheck(uri,_2e,cb);
}
catch(e){
dojo.debug(e);
return false;
}
};
dojo.hostenv.loadUri=function(uri,cb){
if(this.loadedUris[uri]){
return true;
}
var _33=this.getText(uri,null,true);
if(!_33){
return false;
}
this.loadedUris[uri]=true;
if(cb){
_33="("+_33+")";
}
var _34=dj_eval(_33);
if(cb){
cb(_34);
}
return true;
};
dojo.hostenv.loadUriAndCheck=function(uri,_36,cb){
var ok=true;
try{
ok=this.loadUri(uri,cb);
}
catch(e){
dojo.debug("failed loading ",uri," with error: ",e);
}
return Boolean(ok&&this.findModule(_36,false));
};
dojo.loaded=function(){
};
dojo.unloaded=function(){
};
dojo.hostenv.loaded=function(){
this.loadNotifying=true;
this.post_load_=true;
var mll=this.modulesLoadedListeners;
for(var x=0;x<mll.length;x++){
mll[x]();
}
this.modulesLoadedListeners=[];
this.loadNotifying=false;
dojo.loaded();
};
dojo.hostenv.unloaded=function(){
var mll=this.unloadListeners;
while(mll.length){
(mll.pop())();
}
dojo.unloaded();
};
dojo.addOnLoad=function(obj,_3d){
var dh=dojo.hostenv;
if(arguments.length==1){
dh.modulesLoadedListeners.push(obj);
}else{
if(arguments.length>1){
dh.modulesLoadedListeners.push(function(){
obj[_3d]();
});
}
}
if(dh.post_load_&&dh.inFlightCount==0&&!dh.loadNotifying){
dh.callLoaded();
}
};
dojo.addOnUnload=function(obj,_40){
var dh=dojo.hostenv;
if(arguments.length==1){
dh.unloadListeners.push(obj);
}else{
if(arguments.length>1){
dh.unloadListeners.push(function(){
obj[_40]();
});
}
}
};
dojo.hostenv.modulesLoaded=function(){
if(this.post_load_){
return;
}
if(this.loadUriStack.length==0&&this.getTextStack.length==0){
if(this.inFlightCount>0){
dojo.debug("files still in flight!");
return;
}
dojo.hostenv.callLoaded();
}
};
dojo.hostenv.callLoaded=function(){
if(typeof setTimeout=="object"){
setTimeout("dojo.hostenv.loaded();",0);
}else{
dojo.hostenv.loaded();
}
};
dojo.hostenv.getModuleSymbols=function(_42){
var _43=_42.split(".");
for(var i=_43.length;i>0;i--){
var _45=_43.slice(0,i).join(".");
if((i==1)&&!this.moduleHasPrefix(_45)){
_43[0]="../"+_43[0];
}else{
var _46=this.getModulePrefix(_45);
if(_46!=_45){
_43.splice(0,i,_46);
break;
}
}
}
return _43;
};
dojo.hostenv._global_omit_module_check=false;
dojo.hostenv.loadModule=function(_47,_48,_49){
if(!_47){
return;
}
_49=this._global_omit_module_check||_49;
var _4a=this.findModule(_47,false);
if(_4a){
return _4a;
}
if(dj_undef(_47,this.loading_modules_)){
this.addedToLoadingCount.push(_47);
}
this.loading_modules_[_47]=1;
var _4b=_47.replace(/\./g,"/")+".js";
var _4c=_47.split(".");
var _4d=this.getModuleSymbols(_47);
var _4e=((_4d[0].charAt(0)!="/")&&!_4d[0].match(/^\w+:/));
var _4f=_4d[_4d.length-1];
var ok;
if(_4f=="*"){
_47=_4c.slice(0,-1).join(".");
while(_4d.length){
_4d.pop();
_4d.push(this.pkgFileName);
_4b=_4d.join("/")+".js";
if(_4e&&_4b.charAt(0)=="/"){
_4b=_4b.slice(1);
}
ok=this.loadPath(_4b,!_49?_47:null);
if(ok){
break;
}
_4d.pop();
}
}else{
_4b=_4d.join("/")+".js";
_47=_4c.join(".");
var _51=!_49?_47:null;
ok=this.loadPath(_4b,_51);
if(!ok&&!_48){
_4d.pop();
while(_4d.length){
_4b=_4d.join("/")+".js";
ok=this.loadPath(_4b,_51);
if(ok){
break;
}
_4d.pop();
_4b=_4d.join("/")+"/"+this.pkgFileName+".js";
if(_4e&&_4b.charAt(0)=="/"){
_4b=_4b.slice(1);
}
ok=this.loadPath(_4b,_51);
if(ok){
break;
}
}
}
if(!ok&&!_49){
dojo.raise("Could not load '"+_47+"'; last tried '"+_4b+"'");
}
}
if(!_49&&!this["isXDomain"]){
_4a=this.findModule(_47,false);
if(!_4a){
dojo.raise("symbol '"+_47+"' is not defined after loading '"+_4b+"'");
}
}
return _4a;
};
dojo.hostenv.startPackage=function(_52){
var _53=String(_52);
var _54=_53;
var _55=_52.split(/\./);
if(_55[_55.length-1]=="*"){
_55.pop();
_54=_55.join(".");
}
var _56=dojo.evalObjPath(_54,true);
this.loaded_modules_[_53]=_56;
this.loaded_modules_[_54]=_56;
return _56;
};
dojo.hostenv.findModule=function(_57,_58){
var lmn=String(_57);
if(this.loaded_modules_[lmn]){
return this.loaded_modules_[lmn];
}
if(_58){
dojo.raise("no loaded module named '"+_57+"'");
}
return null;
};
dojo.kwCompoundRequire=function(_5a){
var _5b=_5a["common"]||[];
var _5c=_5a[dojo.hostenv.name_]?_5b.concat(_5a[dojo.hostenv.name_]||[]):_5b.concat(_5a["default"]||[]);
for(var x=0;x<_5c.length;x++){
var _5e=_5c[x];
if(_5e.constructor==Array){
dojo.hostenv.loadModule.apply(dojo.hostenv,_5e);
}else{
dojo.hostenv.loadModule(_5e);
}
}
};
dojo.require=function(_5f){
dojo.hostenv.loadModule.apply(dojo.hostenv,arguments);
};
dojo.requireIf=function(_60,_61){
var _62=arguments[0];
if((_62===true)||(_62=="common")||(_62&&dojo.render[_62].capable)){
var _63=[];
for(var i=1;i<arguments.length;i++){
_63.push(arguments[i]);
}
dojo.require.apply(dojo,_63);
}
};
dojo.requireAfterIf=dojo.requireIf;
dojo.provide=function(_65){
return dojo.hostenv.startPackage.apply(dojo.hostenv,arguments);
};
dojo.registerModulePath=function(_66,_67){
return dojo.hostenv.setModulePrefix(_66,_67);
};
dojo.setModulePrefix=function(_68,_69){
dojo.deprecated("dojo.setModulePrefix(\""+_68+"\", \""+_69+"\")","replaced by dojo.registerModulePath","0.5");
return dojo.registerModulePath(_68,_69);
};
dojo.exists=function(obj,_6b){
var p=_6b.split(".");
for(var i=0;i<p.length;i++){
if(!obj[p[i]]){
return false;
}
obj=obj[p[i]];
}
return true;
};
dojo.hostenv.normalizeLocale=function(_6e){
var _6f=_6e?_6e.toLowerCase():dojo.locale;
if(_6f=="root"){
_6f="ROOT";
}
return _6f;
};
dojo.hostenv.searchLocalePath=function(_70,_71,_72){
_70=dojo.hostenv.normalizeLocale(_70);
var _73=_70.split("-");
var _74=[];
for(var i=_73.length;i>0;i--){
_74.push(_73.slice(0,i).join("-"));
}
_74.push(false);
if(_71){
_74.reverse();
}
for(var j=_74.length-1;j>=0;j--){
var loc=_74[j]||"ROOT";
var _78=_72(loc);
if(_78){
break;
}
}
};
dojo.hostenv.localesGenerated;
dojo.hostenv.registerNlsPrefix=function(){
dojo.registerModulePath("nls","nls");
};
dojo.hostenv.preloadLocalizations=function(){
if(dojo.hostenv.localesGenerated){
dojo.hostenv.registerNlsPrefix();
function preload(_79){
_79=dojo.hostenv.normalizeLocale(_79);
dojo.hostenv.searchLocalePath(_79,true,function(loc){
for(var i=0;i<dojo.hostenv.localesGenerated.length;i++){
if(dojo.hostenv.localesGenerated[i]==loc){
dojo["require"]("nls.dojo_"+loc);
return true;
}
}
return false;
});
}
preload();
var _7c=djConfig.extraLocale||[];
for(var i=0;i<_7c.length;i++){
preload(_7c[i]);
}
}
dojo.hostenv.preloadLocalizations=function(){
};
};
dojo.requireLocalization=function(_7e,_7f,_80,_81){
dojo.hostenv.preloadLocalizations();
var _82=dojo.hostenv.normalizeLocale(_80);
var _83=[_7e,"nls",_7f].join(".");
var _84="";
if(_81){
var _85=_81.split(",");
for(var i=0;i<_85.length;i++){
if(_82.indexOf(_85[i])==0){
if(_85[i].length>_84.length){
_84=_85[i];
}
}
}
if(!_84){
_84="ROOT";
}
}
var _87=_81?_84:_82;
var _88=dojo.hostenv.findModule(_83);
var _89=null;
if(_88){
if(djConfig.localizationComplete&&_88._built){
return;
}
var _8a=_87.replace("-","_");
var _8b=_83+"."+_8a;
_89=dojo.hostenv.findModule(_8b);
}
if(!_89){
_88=dojo.hostenv.startPackage(_83);
var _8c=dojo.hostenv.getModuleSymbols(_7e);
var _8d=_8c.concat("nls").join("/");
var _8e;
dojo.hostenv.searchLocalePath(_87,_81,function(loc){
var _90=loc.replace("-","_");
var _91=_83+"."+_90;
var _92=false;
if(!dojo.hostenv.findModule(_91)){
dojo.hostenv.startPackage(_91);
var _93=[_8d];
if(loc!="ROOT"){
_93.push(loc);
}
_93.push(_7f);
var _94=_93.join("/")+".js";
_92=dojo.hostenv.loadPath(_94,null,function(_95){
var _96=function(){
};
_96.prototype=_8e;
_88[_90]=new _96();
for(var j in _95){
_88[_90][j]=_95[j];
}
});
}else{
_92=true;
}
if(_92&&_88[_90]){
_8e=_88[_90];
}else{
_88[_90]=_8e;
}
if(_81){
return true;
}
});
}
if(_81&&_82!=_84){
_88[_82.replace("-","_")]=_88[_84.replace("-","_")];
}
};
(function(){
var _98=djConfig.extraLocale;
if(_98){
if(!_98 instanceof Array){
_98=[_98];
}
var req=dojo.requireLocalization;
dojo.requireLocalization=function(m,b,_9c,_9d){
req(m,b,_9c,_9d);
if(_9c){
return;
}
for(var i=0;i<_98.length;i++){
req(m,b,_98[i],_9d);
}
};
}
})();
}
if(typeof window!="undefined"){
(function(){
if(djConfig.allowQueryConfig){
var _9f=document.location.toString();
var _a0=_9f.split("?",2);
if(_a0.length>1){
var _a1=_a0[1];
var _a2=_a1.split("&");
for(var x in _a2){
var sp=_a2[x].split("=");
if((sp[0].length>9)&&(sp[0].substr(0,9)=="djConfig.")){
var opt=sp[0].substr(9);
try{
djConfig[opt]=eval(sp[1]);
}
catch(e){
djConfig[opt]=sp[1];
}
}
}
}
}
if(((djConfig["baseScriptUri"]=="")||(djConfig["baseRelativePath"]==""))&&(document&&document.getElementsByTagName)){
var _a6=document.getElementsByTagName("script");
var _a7=/(__package__|dojo|bootstrap1)\.js([\?\.]|$)/i;
for(var i=0;i<_a6.length;i++){
var src=_a6[i].getAttribute("src");
if(!src){
continue;
}
var m=src.match(_a7);
if(m){
var _ab=src.substring(0,m.index);
if(src.indexOf("bootstrap1")>-1){
_ab+="../";
}
if(!this["djConfig"]){
djConfig={};
}
if(djConfig["baseScriptUri"]==""){
djConfig["baseScriptUri"]=_ab;
}
if(djConfig["baseRelativePath"]==""){
djConfig["baseRelativePath"]=_ab;
}
break;
}
}
}
var dr=dojo.render;
var drh=dojo.render.html;
var drs=dojo.render.svg;
var dua=(drh.UA=navigator.userAgent);
var dav=(drh.AV=navigator.appVersion);
var t=true;
var f=false;
drh.capable=t;
drh.support.builtin=t;
dr.ver=parseFloat(drh.AV);
dr.os.mac=dav.indexOf("Macintosh")>=0;
dr.os.win=dav.indexOf("Windows")>=0;
dr.os.linux=dav.indexOf("X11")>=0;
drh.opera=dua.indexOf("Opera")>=0;
drh.khtml=(dav.indexOf("Konqueror")>=0)||(dav.indexOf("Safari")>=0);
drh.safari=dav.indexOf("Safari")>=0;
var _b3=dua.indexOf("Gecko");
drh.mozilla=drh.moz=(_b3>=0)&&(!drh.khtml);
if(drh.mozilla){
drh.geckoVersion=dua.substring(_b3+6,_b3+14);
}
drh.ie=(document.all)&&(!drh.opera);
drh.ie50=drh.ie&&dav.indexOf("MSIE 5.0")>=0;
drh.ie55=drh.ie&&dav.indexOf("MSIE 5.5")>=0;
drh.ie60=drh.ie&&dav.indexOf("MSIE 6.0")>=0;
drh.ie70=drh.ie&&dav.indexOf("MSIE 7.0")>=0;
var cm=document["compatMode"];
drh.quirks=(cm=="BackCompat")||(cm=="QuirksMode")||drh.ie55||drh.ie50;
dojo.locale=dojo.locale||(drh.ie?navigator.userLanguage:navigator.language).toLowerCase();
dr.vml.capable=drh.ie;
drs.capable=f;
drs.support.plugin=f;
drs.support.builtin=f;
var _b5=window["document"];
var tdi=_b5["implementation"];
if((tdi)&&(tdi["hasFeature"])&&(tdi.hasFeature("org.w3c.dom.svg","1.0"))){
drs.capable=t;
drs.support.builtin=t;
drs.support.plugin=f;
}
if(drh.safari){
var tmp=dua.split("AppleWebKit/")[1];
var ver=parseFloat(tmp.split(" ")[0]);
if(ver>=420){
drs.capable=t;
drs.support.builtin=t;
drs.support.plugin=f;
}
}else{
}
})();
dojo.hostenv.startPackage("dojo.hostenv");
dojo.render.name=dojo.hostenv.name_="browser";
dojo.hostenv.searchIds=[];
dojo.hostenv._XMLHTTP_PROGIDS=["Msxml2.XMLHTTP","Microsoft.XMLHTTP","Msxml2.XMLHTTP.4.0"];
dojo.hostenv.getXmlhttpObject=function(){
var _b9=null;
var _ba=null;
try{
_b9=new XMLHttpRequest();
}
catch(e){
}
if(!_b9){
for(var i=0;i<3;++i){
var _bc=dojo.hostenv._XMLHTTP_PROGIDS[i];
try{
_b9=new ActiveXObject(_bc);
}
catch(e){
_ba=e;
}
if(_b9){
dojo.hostenv._XMLHTTP_PROGIDS=[_bc];
break;
}
}
}
if(!_b9){
return dojo.raise("XMLHTTP not available",_ba);
}
return _b9;
};
dojo.hostenv._blockAsync=false;
dojo.hostenv.getText=function(uri,_be,_bf){
if(!_be){
this._blockAsync=true;
}
var _c0=this.getXmlhttpObject();
function isDocumentOk(_c1){
var _c2=_c1["status"];
return Boolean((!_c2)||((200<=_c2)&&(300>_c2))||(_c2==304));
}
if(_be){
var _c3=this,_c4=null,gbl=dojo.global();
var xhr=dojo.evalObjPath("dojo.io.XMLHTTPTransport");
_c0.onreadystatechange=function(){
if(_c4){
gbl.clearTimeout(_c4);
_c4=null;
}
if(_c3._blockAsync||(xhr&&xhr._blockAsync)){
_c4=gbl.setTimeout(function(){
_c0.onreadystatechange.apply(this);
},10);
}else{
if(4==_c0.readyState){
if(isDocumentOk(_c0)){
_be(_c0.responseText);
}
}
}
};
}
_c0.open("GET",uri,_be?true:false);
try{
_c0.send(null);
if(_be){
return null;
}
if(!isDocumentOk(_c0)){
var err=Error("Unable to load "+uri+" status:"+_c0.status);
err.status=_c0.status;
err.responseText=_c0.responseText;
throw err;
}
}
catch(e){
this._blockAsync=false;
if((_bf)&&(!_be)){
return null;
}else{
throw e;
}
}
this._blockAsync=false;
return _c0.responseText;
};
dojo.hostenv.defaultDebugContainerId="dojoDebug";
dojo.hostenv._println_buffer=[];
dojo.hostenv._println_safe=false;
dojo.hostenv.println=function(_c8){
if(!dojo.hostenv._println_safe){
dojo.hostenv._println_buffer.push(_c8);
}else{
try{
var _c9=document.getElementById(djConfig.debugContainerId?djConfig.debugContainerId:dojo.hostenv.defaultDebugContainerId);
if(!_c9){
_c9=dojo.body();
}
var div=document.createElement("div");
div.appendChild(document.createTextNode(_c8));
_c9.appendChild(div);
}
catch(e){
try{
document.write("<div>"+_c8+"</div>");
}
catch(e2){
window.status=_c8;
}
}
}
};
dojo.addOnLoad(function(){
dojo.hostenv._println_safe=true;
while(dojo.hostenv._println_buffer.length>0){
dojo.hostenv.println(dojo.hostenv._println_buffer.shift());
}
});
function dj_addNodeEvtHdlr(_cb,_cc,fp){
var _ce=_cb["on"+_cc]||function(){
};
_cb["on"+_cc]=function(){
fp.apply(_cb,arguments);
_ce.apply(_cb,arguments);
};
return true;
}
function dj_load_init(e){
var _d0=(e&&e.type)?e.type.toLowerCase():"load";
if(arguments.callee.initialized||(_d0!="domcontentloaded"&&_d0!="load")){
return;
}
arguments.callee.initialized=true;
if(typeof (_timer)!="undefined"){
clearInterval(_timer);
delete _timer;
}
var _d1=function(){
if(dojo.render.html.ie){
dojo.hostenv.makeWidgets();
}
};
if(dojo.hostenv.inFlightCount==0){
_d1();
dojo.hostenv.modulesLoaded();
}else{
dojo.hostenv.modulesLoadedListeners.unshift(_d1);
}
}
if(document.addEventListener){
if(dojo.render.html.opera||(dojo.render.html.moz&&!djConfig.delayMozLoadingFix)){
document.addEventListener("DOMContentLoaded",dj_load_init,null);
}
window.addEventListener("load",dj_load_init,null);
}
if(dojo.render.html.ie&&dojo.render.os.win){
document.attachEvent("onreadystatechange",function(e){
if(document.readyState=="complete"){
dj_load_init();
}
});
}
if(/(WebKit|khtml)/i.test(navigator.userAgent)){
var _timer=setInterval(function(){
if(/loaded|complete/.test(document.readyState)){
dj_load_init();
}
},10);
}
if(dojo.render.html.ie){
dj_addNodeEvtHdlr(window,"beforeunload",function(){
dojo.hostenv._unloading=true;
window.setTimeout(function(){
dojo.hostenv._unloading=false;
},0);
});
}
dj_addNodeEvtHdlr(window,"unload",function(){
dojo.hostenv.unloaded();
if((!dojo.render.html.ie)||(dojo.render.html.ie&&dojo.hostenv._unloading)){
dojo.hostenv.unloaded();
}
});
dojo.hostenv.makeWidgets=function(){
var _d3=[];
if(djConfig.searchIds&&djConfig.searchIds.length>0){
_d3=_d3.concat(djConfig.searchIds);
}
if(dojo.hostenv.searchIds&&dojo.hostenv.searchIds.length>0){
_d3=_d3.concat(dojo.hostenv.searchIds);
}
if((djConfig.parseWidgets)||(_d3.length>0)){
if(dojo.evalObjPath("dojo.widget.Parse")){
var _d4=new dojo.xml.Parse();
if(_d3.length>0){
for(var x=0;x<_d3.length;x++){
var _d6=document.getElementById(_d3[x]);
if(!_d6){
continue;
}
var _d7=_d4.parseElement(_d6,null,true);
dojo.widget.getParser().createComponents(_d7);
}
}else{
if(djConfig.parseWidgets){
var _d7=_d4.parseElement(dojo.body(),null,true);
dojo.widget.getParser().createComponents(_d7);
}
}
}
}
};
dojo.addOnLoad(function(){
if(!dojo.render.html.ie){
dojo.hostenv.makeWidgets();
}
});
try{
if(dojo.render.html.ie){
document.namespaces.add("v","urn:schemas-microsoft-com:vml");
document.createStyleSheet().addRule("v\\:*","behavior:url(#default#VML)");
}
}
catch(e){
}
dojo.hostenv.writeIncludes=function(){
};
if(!dj_undef("document",this)){
dj_currentDocument=this.document;
}
dojo.doc=function(){
return dj_currentDocument;
};
dojo.body=function(){
return dojo.doc().body||dojo.doc().getElementsByTagName("body")[0];
};
dojo.byId=function(id,doc){
if((id)&&((typeof id=="string")||(id instanceof String))){
if(!doc){
doc=dj_currentDocument;
}
var ele=doc.getElementById(id);
if(ele&&(ele.id!=id)&&doc.all){
ele=null;
eles=doc.all[id];
if(eles){
if(eles.length){
for(var i=0;i<eles.length;i++){
if(eles[i].id==id){
ele=eles[i];
break;
}
}
}else{
ele=eles;
}
}
}
return ele;
}
return id;
};
dojo.setContext=function(_dc,_dd){
dj_currentContext=_dc;
dj_currentDocument=_dd;
};
dojo._fireCallback=function(_de,_df,_e0){
if((_df)&&((typeof _de=="string")||(_de instanceof String))){
_de=_df[_de];
}
return (_df?_de.apply(_df,_e0||[]):_de());
};
dojo.withGlobal=function(_e1,_e2,_e3,_e4){
var _e5;
var _e6=dj_currentContext;
var _e7=dj_currentDocument;
try{
dojo.setContext(_e1,_e1.document);
_e5=dojo._fireCallback(_e2,_e3,_e4);
}
finally{
dojo.setContext(_e6,_e7);
}
return _e5;
};
dojo.withDoc=function(_e8,_e9,_ea,_eb){
var _ec;
var _ed=dj_currentDocument;
try{
dj_currentDocument=_e8;
_ec=dojo._fireCallback(_e9,_ea,_eb);
}
finally{
dj_currentDocument=_ed;
}
return _ec;
};
}
(function(){
if(typeof dj_usingBootstrap!="undefined"){
return;
}
var _ee=false;
var _ef=false;
var _f0=false;
if((typeof this["load"]=="function")&&((typeof this["Packages"]=="function")||(typeof this["Packages"]=="object"))){
_ee=true;
}else{
if(typeof this["load"]=="function"){
_ef=true;
}else{
if(window.widget){
_f0=true;
}
}
}
var _f1=[];
if((this["djConfig"])&&((djConfig["isDebug"])||(djConfig["debugAtAllCosts"]))){
_f1.push("debug.js");
}
if((this["djConfig"])&&(djConfig["debugAtAllCosts"])&&(!_ee)&&(!_f0)){
_f1.push("browser_debug.js");
}
var _f2=djConfig["baseScriptUri"];
if((this["djConfig"])&&(djConfig["baseLoaderUri"])){
_f2=djConfig["baseLoaderUri"];
}
for(var x=0;x<_f1.length;x++){
var _f4=_f2+"src/"+_f1[x];
if(_ee||_ef){
load(_f4);
}else{
try{
document.write("<scr"+"ipt type='text/javascript' src='"+_f4+"'></scr"+"ipt>");
}
catch(e){
var _f5=document.createElement("script");
_f5.src=_f4;
document.getElementsByTagName("head")[0].appendChild(_f5);
}
}
}
})();
dojo.provide("dojo.string.common");
dojo.string.trim=function(str,wh){
if(!str.replace){
return str;
}
if(!str.length){
return str;
}
var re=(wh>0)?(/^\s+/):(wh<0)?(/\s+$/):(/^\s+|\s+$/g);
return str.replace(re,"");
};
dojo.string.trimStart=function(str){
return dojo.string.trim(str,1);
};
dojo.string.trimEnd=function(str){
return dojo.string.trim(str,-1);
};
dojo.string.repeat=function(str,_fc,_fd){
var out="";
for(var i=0;i<_fc;i++){
out+=str;
if(_fd&&i<_fc-1){
out+=_fd;
}
}
return out;
};
dojo.string.pad=function(str,len,c,dir){
var out=String(str);
if(!c){
c="0";
}
if(!dir){
dir=1;
}
while(out.length<len){
if(dir>0){
out=c+out;
}else{
out+=c;
}
}
return out;
};
dojo.string.padLeft=function(str,len,c){
return dojo.string.pad(str,len,c,1);
};
dojo.string.padRight=function(str,len,c){
return dojo.string.pad(str,len,c,-1);
};
dojo.provide("dojo.string");
dojo.provide("dojo.lang.common");
dojo.lang.inherits=function(_10b,_10c){
if(!dojo.lang.isFunction(_10c)){
dojo.raise("dojo.inherits: superclass argument ["+_10c+"] must be a function (subclass: ["+_10b+"']");
}
_10b.prototype=new _10c();
_10b.prototype.constructor=_10b;
_10b.superclass=_10c.prototype;
_10b["super"]=_10c.prototype;
};
dojo.lang._mixin=function(obj,_10e){
var tobj={};
for(var x in _10e){
if((typeof tobj[x]=="undefined")||(tobj[x]!=_10e[x])){
obj[x]=_10e[x];
}
}
if(dojo.render.html.ie&&(typeof (_10e["toString"])=="function")&&(_10e["toString"]!=obj["toString"])&&(_10e["toString"]!=tobj["toString"])){
obj.toString=_10e.toString;
}
return obj;
};
dojo.lang.mixin=function(obj,_112){
for(var i=1,l=arguments.length;i<l;i++){
dojo.lang._mixin(obj,arguments[i]);
}
return obj;
};
dojo.lang.extend=function(_115,_116){
for(var i=1,l=arguments.length;i<l;i++){
dojo.lang._mixin(_115.prototype,arguments[i]);
}
return _115;
};
dojo.inherits=dojo.lang.inherits;
dojo.mixin=dojo.lang.mixin;
dojo.extend=dojo.lang.extend;
dojo.lang.find=function(_119,_11a,_11b,_11c){
if(!dojo.lang.isArrayLike(_119)&&dojo.lang.isArrayLike(_11a)){
dojo.deprecated("dojo.lang.find(value, array)","use dojo.lang.find(array, value) instead","0.5");
var temp=_119;
_119=_11a;
_11a=temp;
}
var _11e=dojo.lang.isString(_119);
if(_11e){
_119=_119.split("");
}
if(_11c){
var step=-1;
var i=_119.length-1;
var end=-1;
}else{
var step=1;
var i=0;
var end=_119.length;
}
if(_11b){
while(i!=end){
if(_119[i]===_11a){
return i;
}
i+=step;
}
}else{
while(i!=end){
if(_119[i]==_11a){
return i;
}
i+=step;
}
}
return -1;
};
dojo.lang.indexOf=dojo.lang.find;
dojo.lang.findLast=function(_122,_123,_124){
return dojo.lang.find(_122,_123,_124,true);
};
dojo.lang.lastIndexOf=dojo.lang.findLast;
dojo.lang.inArray=function(_125,_126){
return dojo.lang.find(_125,_126)>-1;
};
dojo.lang.isObject=function(it){
if(typeof it=="undefined"){
return false;
}
return (typeof it=="object"||it===null||dojo.lang.isArray(it)||dojo.lang.isFunction(it));
};
dojo.lang.isArray=function(it){
return (it&&it instanceof Array||typeof it=="array");
};
dojo.lang.isArrayLike=function(it){
if((!it)||(dojo.lang.isUndefined(it))){
return false;
}
if(dojo.lang.isString(it)){
return false;
}
if(dojo.lang.isFunction(it)){
return false;
}
if(dojo.lang.isArray(it)){
return true;
}
if((it.tagName)&&(it.tagName.toLowerCase()=="form")){
return false;
}
if(dojo.lang.isNumber(it.length)&&isFinite(it.length)){
return true;
}
return false;
};
dojo.lang.isFunction=function(it){
return (it instanceof Function||typeof it=="function");
};
(function(){
if((dojo.render.html.capable)&&(dojo.render.html["safari"])){
dojo.lang.isFunction=function(it){
if((typeof (it)=="function")&&(it=="[object NodeList]")){
return false;
}
return (it instanceof Function||typeof it=="function");
};
}
})();
dojo.lang.isString=function(it){
return (typeof it=="string"||it instanceof String);
};
dojo.lang.isAlien=function(it){
if(!it){
return false;
}
return !dojo.lang.isFunction(it)&&/\{\s*\[native code\]\s*\}/.test(String(it));
};
dojo.lang.isBoolean=function(it){
return (it instanceof Boolean||typeof it=="boolean");
};
dojo.lang.isNumber=function(it){
return (it instanceof Number||typeof it=="number");
};
dojo.lang.isUndefined=function(it){
return ((typeof (it)=="undefined")&&(it==undefined));
};
dojo.provide("dojo.lang.extras");
dojo.lang.setTimeout=function(func,_132){
var _133=window,_134=2;
if(!dojo.lang.isFunction(func)){
_133=func;
func=_132;
_132=arguments[2];
_134++;
}
if(dojo.lang.isString(func)){
func=_133[func];
}
var args=[];
for(var i=_134;i<arguments.length;i++){
args.push(arguments[i]);
}
return dojo.global().setTimeout(function(){
func.apply(_133,args);
},_132);
};
dojo.lang.clearTimeout=function(_137){
dojo.global().clearTimeout(_137);
};
dojo.lang.getNameInObj=function(ns,item){
if(!ns){
ns=dj_global;
}
for(var x in ns){
if(ns[x]===item){
return new String(x);
}
}
return null;
};
dojo.lang.shallowCopy=function(obj,deep){
var i,ret;
if(obj===null){
return null;
}
if(dojo.lang.isObject(obj)){
ret=new obj.constructor();
for(i in obj){
if(dojo.lang.isUndefined(ret[i])){
ret[i]=deep?dojo.lang.shallowCopy(obj[i],deep):obj[i];
}
}
}else{
if(dojo.lang.isArray(obj)){
ret=[];
for(i=0;i<obj.length;i++){
ret[i]=deep?dojo.lang.shallowCopy(obj[i],deep):obj[i];
}
}else{
ret=obj;
}
}
return ret;
};
dojo.lang.firstValued=function(){
for(var i=0;i<arguments.length;i++){
if(typeof arguments[i]!="undefined"){
return arguments[i];
}
}
return undefined;
};
dojo.lang.getObjPathValue=function(_140,_141,_142){
with(dojo.parseObjPath(_140,_141,_142)){
return dojo.evalProp(prop,obj,_142);
}
};
dojo.lang.setObjPathValue=function(_143,_144,_145,_146){
dojo.deprecated("dojo.lang.setObjPathValue","use dojo.parseObjPath and the '=' operator","0.6");
if(arguments.length<4){
_146=true;
}
with(dojo.parseObjPath(_143,_145,_146)){
if(obj&&(_146||(prop in obj))){
obj[prop]=_144;
}
}
};
dojo.provide("dojo.io.common");
dojo.io.transports=[];
dojo.io.hdlrFuncNames=["load","error","timeout"];
dojo.io.Request=function(url,_148,_149,_14a){
if((arguments.length==1)&&(arguments[0].constructor==Object)){
this.fromKwArgs(arguments[0]);
}else{
this.url=url;
if(_148){
this.mimetype=_148;
}
if(_149){
this.transport=_149;
}
if(arguments.length>=4){
this.changeUrl=_14a;
}
}
};
dojo.lang.extend(dojo.io.Request,{url:"",mimetype:"text/plain",method:"GET",content:undefined,transport:undefined,changeUrl:undefined,formNode:undefined,sync:false,bindSuccess:false,useCache:false,preventCache:false,load:function(type,data,_14d,_14e){
},error:function(type,_150,_151,_152){
},timeout:function(type,_154,_155,_156){
},handle:function(type,data,_159,_15a){
},timeoutSeconds:0,abort:function(){
},fromKwArgs:function(_15b){
if(_15b["url"]){
_15b.url=_15b.url.toString();
}
if(_15b["formNode"]){
_15b.formNode=dojo.byId(_15b.formNode);
}
if(!_15b["method"]&&_15b["formNode"]&&_15b["formNode"].method){
_15b.method=_15b["formNode"].method;
}
if(!_15b["handle"]&&_15b["handler"]){
_15b.handle=_15b.handler;
}
if(!_15b["load"]&&_15b["loaded"]){
_15b.load=_15b.loaded;
}
if(!_15b["changeUrl"]&&_15b["changeURL"]){
_15b.changeUrl=_15b.changeURL;
}
_15b.encoding=dojo.lang.firstValued(_15b["encoding"],djConfig["bindEncoding"],"");
_15b.sendTransport=dojo.lang.firstValued(_15b["sendTransport"],djConfig["ioSendTransport"],false);
var _15c=dojo.lang.isFunction;
for(var x=0;x<dojo.io.hdlrFuncNames.length;x++){
var fn=dojo.io.hdlrFuncNames[x];
if(_15b[fn]&&_15c(_15b[fn])){
continue;
}
if(_15b["handle"]&&_15c(_15b["handle"])){
_15b[fn]=_15b.handle;
}
}
dojo.lang.mixin(this,_15b);
}});
dojo.io.Error=function(msg,type,num){
this.message=msg;
this.type=type||"unknown";
this.number=num||0;
};
dojo.io.transports.addTransport=function(name){
this.push(name);
this[name]=dojo.io[name];
};
dojo.io.bind=function(_163){
if(!(_163 instanceof dojo.io.Request)){
try{
_163=new dojo.io.Request(_163);
}
catch(e){
dojo.debug(e);
}
}
var _164="";
if(_163["transport"]){
_164=_163["transport"];
if(!this[_164]){
dojo.io.sendBindError(_163,"No dojo.io.bind() transport with name '"+_163["transport"]+"'.");
return _163;
}
if(!this[_164].canHandle(_163)){
dojo.io.sendBindError(_163,"dojo.io.bind() transport with name '"+_163["transport"]+"' cannot handle this type of request.");
return _163;
}
}else{
for(var x=0;x<dojo.io.transports.length;x++){
var tmp=dojo.io.transports[x];
if((this[tmp])&&(this[tmp].canHandle(_163))){
_164=tmp;
break;
}
}
if(_164==""){
dojo.io.sendBindError(_163,"None of the loaded transports for dojo.io.bind()"+" can handle the request.");
return _163;
}
}
this[_164].bind(_163);
_163.bindSuccess=true;
return _163;
};
dojo.io.sendBindError=function(_167,_168){
if((typeof _167.error=="function"||typeof _167.handle=="function")&&(typeof setTimeout=="function"||typeof setTimeout=="object")){
var _169=new dojo.io.Error(_168);
setTimeout(function(){
_167[(typeof _167.error=="function")?"error":"handle"]("error",_169,null,_167);
},50);
}else{
dojo.raise(_168);
}
};
dojo.io.queueBind=function(_16a){
if(!(_16a instanceof dojo.io.Request)){
try{
_16a=new dojo.io.Request(_16a);
}
catch(e){
dojo.debug(e);
}
}
var _16b=_16a.load;
_16a.load=function(){
dojo.io._queueBindInFlight=false;
var ret=_16b.apply(this,arguments);
dojo.io._dispatchNextQueueBind();
return ret;
};
var _16d=_16a.error;
_16a.error=function(){
dojo.io._queueBindInFlight=false;
var ret=_16d.apply(this,arguments);
dojo.io._dispatchNextQueueBind();
return ret;
};
dojo.io._bindQueue.push(_16a);
dojo.io._dispatchNextQueueBind();
return _16a;
};
dojo.io._dispatchNextQueueBind=function(){
if(!dojo.io._queueBindInFlight){
dojo.io._queueBindInFlight=true;
if(dojo.io._bindQueue.length>0){
dojo.io.bind(dojo.io._bindQueue.shift());
}else{
dojo.io._queueBindInFlight=false;
}
}
};
dojo.io._bindQueue=[];
dojo.io._queueBindInFlight=false;
dojo.io.argsFromMap=function(map,_170,last){
var enc=/utf/i.test(_170||"")?encodeURIComponent:dojo.string.encodeAscii;
var _173=[];
var _174=new Object();
for(var name in map){
var _176=function(elt){
var val=enc(name)+"="+enc(elt);
_173[(last==name)?"push":"unshift"](val);
};
if(!_174[name]){
var _179=map[name];
if(dojo.lang.isArray(_179)){
dojo.lang.forEach(_179,_176);
}else{
_176(_179);
}
}
}
return _173.join("&");
};
dojo.io.setIFrameSrc=function(_17a,src,_17c){
try{
var r=dojo.render.html;
if(!_17c){
if(r.safari){
_17a.location=src;
}else{
frames[_17a.name].location=src;
}
}else{
var idoc;
if(r.ie){
idoc=_17a.contentWindow.document;
}else{
if(r.safari){
idoc=_17a.document;
}else{
idoc=_17a.contentWindow;
}
}
if(!idoc){
_17a.location=src;
return;
}else{
idoc.location.replace(src);
}
}
}
catch(e){
dojo.debug(e);
dojo.debug("setIFrameSrc: "+e);
}
};
dojo.provide("dojo.lang.array");
dojo.lang.mixin(dojo.lang,{has:function(obj,name){
try{
return typeof obj[name]!="undefined";
}
catch(e){
return false;
}
},isEmpty:function(obj){
if(dojo.lang.isObject(obj)){
var tmp={};
var _183=0;
for(var x in obj){
if(obj[x]&&(!tmp[x])){
_183++;
break;
}
}
return _183==0;
}else{
if(dojo.lang.isArrayLike(obj)||dojo.lang.isString(obj)){
return obj.length==0;
}
}
},map:function(arr,obj,_187){
var _188=dojo.lang.isString(arr);
if(_188){
arr=arr.split("");
}
if(dojo.lang.isFunction(obj)&&(!_187)){
_187=obj;
obj=dj_global;
}else{
if(dojo.lang.isFunction(obj)&&_187){
var _189=obj;
obj=_187;
_187=_189;
}
}
if(Array.map){
var _18a=Array.map(arr,_187,obj);
}else{
var _18a=[];
for(var i=0;i<arr.length;++i){
_18a.push(_187.call(obj,arr[i]));
}
}
if(_188){
return _18a.join("");
}else{
return _18a;
}
},reduce:function(arr,_18d,obj,_18f){
var _190=_18d;
if(arguments.length==1){
dojo.debug("dojo.lang.reduce called with too few arguments!");
return false;
}else{
if(arguments.length==2){
_18f=_18d;
_190=arr.shift();
}else{
if(arguments.lenght==3){
if(dojo.lang.isFunction(obj)){
_18f=obj;
obj=null;
}
}else{
if(dojo.lang.isFunction(obj)){
var tmp=_18f;
_18f=obj;
obj=tmp;
}
}
}
}
var ob=obj?obj:dj_global;
dojo.lang.map(arr,function(val){
_190=_18f.call(ob,_190,val);
});
return _190;
},forEach:function(_194,_195,_196){
if(dojo.lang.isString(_194)){
_194=_194.split("");
}
if(Array.forEach){
Array.forEach(_194,_195,_196);
}else{
if(!_196){
_196=dj_global;
}
for(var i=0,l=_194.length;i<l;i++){
_195.call(_196,_194[i],i,_194);
}
}
},_everyOrSome:function(_199,arr,_19b,_19c){
if(dojo.lang.isString(arr)){
arr=arr.split("");
}
if(Array.every){
return Array[_199?"every":"some"](arr,_19b,_19c);
}else{
if(!_19c){
_19c=dj_global;
}
for(var i=0,l=arr.length;i<l;i++){
var _19f=_19b.call(_19c,arr[i],i,arr);
if(_199&&!_19f){
return false;
}else{
if((!_199)&&(_19f)){
return true;
}
}
}
return Boolean(_199);
}
},every:function(arr,_1a1,_1a2){
return this._everyOrSome(true,arr,_1a1,_1a2);
},some:function(arr,_1a4,_1a5){
return this._everyOrSome(false,arr,_1a4,_1a5);
},filter:function(arr,_1a7,_1a8){
var _1a9=dojo.lang.isString(arr);
if(_1a9){
arr=arr.split("");
}
var _1aa;
if(Array.filter){
_1aa=Array.filter(arr,_1a7,_1a8);
}else{
if(!_1a8){
if(arguments.length>=3){
dojo.raise("thisObject doesn't exist!");
}
_1a8=dj_global;
}
_1aa=[];
for(var i=0;i<arr.length;i++){
if(_1a7.call(_1a8,arr[i],i,arr)){
_1aa.push(arr[i]);
}
}
}
if(_1a9){
return _1aa.join("");
}else{
return _1aa;
}
},unnest:function(){
var out=[];
for(var i=0;i<arguments.length;i++){
if(dojo.lang.isArrayLike(arguments[i])){
var add=dojo.lang.unnest.apply(this,arguments[i]);
out=out.concat(add);
}else{
out.push(arguments[i]);
}
}
return out;
},toArray:function(_1af,_1b0){
var _1b1=[];
for(var i=_1b0||0;i<_1af.length;i++){
_1b1.push(_1af[i]);
}
return _1b1;
}});
dojo.provide("dojo.lang.func");
dojo.lang.hitch=function(_1b3,_1b4){
var fcn=(dojo.lang.isString(_1b4)?_1b3[_1b4]:_1b4)||function(){
};
return function(){
return fcn.apply(_1b3,arguments);
};
};
dojo.lang.anonCtr=0;
dojo.lang.anon={};
dojo.lang.nameAnonFunc=function(_1b6,_1b7,_1b8){
var nso=(_1b7||dojo.lang.anon);
if((_1b8)||((dj_global["djConfig"])&&(djConfig["slowAnonFuncLookups"]==true))){
for(var x in nso){
try{
if(nso[x]===_1b6){
return x;
}
}
catch(e){
}
}
}
var ret="__"+dojo.lang.anonCtr++;
while(typeof nso[ret]!="undefined"){
ret="__"+dojo.lang.anonCtr++;
}
nso[ret]=_1b6;
return ret;
};
dojo.lang.forward=function(_1bc){
return function(){
return this[_1bc].apply(this,arguments);
};
};
dojo.lang.curry=function(_1bd,func){
var _1bf=[];
_1bd=_1bd||dj_global;
if(dojo.lang.isString(func)){
func=_1bd[func];
}
for(var x=2;x<arguments.length;x++){
_1bf.push(arguments[x]);
}
var _1c1=(func["__preJoinArity"]||func.length)-_1bf.length;
function gather(_1c2,_1c3,_1c4){
var _1c5=_1c4;
var _1c6=_1c3.slice(0);
for(var x=0;x<_1c2.length;x++){
_1c6.push(_1c2[x]);
}
_1c4=_1c4-_1c2.length;
if(_1c4<=0){
var res=func.apply(_1bd,_1c6);
_1c4=_1c5;
return res;
}else{
return function(){
return gather(arguments,_1c6,_1c4);
};
}
}
return gather([],_1bf,_1c1);
};
dojo.lang.curryArguments=function(_1c9,func,args,_1cc){
var _1cd=[];
var x=_1cc||0;
for(x=_1cc;x<args.length;x++){
_1cd.push(args[x]);
}
return dojo.lang.curry.apply(dojo.lang,[_1c9,func].concat(_1cd));
};
dojo.lang.tryThese=function(){
for(var x=0;x<arguments.length;x++){
try{
if(typeof arguments[x]=="function"){
var ret=(arguments[x]());
if(ret){
return ret;
}
}
}
catch(e){
dojo.debug(e);
}
}
};
dojo.lang.delayThese=function(farr,cb,_1d3,_1d4){
if(!farr.length){
if(typeof _1d4=="function"){
_1d4();
}
return;
}
if((typeof _1d3=="undefined")&&(typeof cb=="number")){
_1d3=cb;
cb=function(){
};
}else{
if(!cb){
cb=function(){
};
if(!_1d3){
_1d3=0;
}
}
}
setTimeout(function(){
(farr.shift())();
cb();
dojo.lang.delayThese(farr,cb,_1d3,_1d4);
},_1d3);
};
dojo.provide("dojo.string.extras");
dojo.string.substituteParams=function(_1d5,hash){
var map=(typeof hash=="object")?hash:dojo.lang.toArray(arguments,1);
return _1d5.replace(/\%\{(\w+)\}/g,function(_1d8,key){
if(typeof (map[key])!="undefined"&&map[key]!=null){
return map[key];
}
dojo.raise("Substitution not found: "+key);
});
};
dojo.string.capitalize=function(str){
if(!dojo.lang.isString(str)){
return "";
}
if(arguments.length==0){
str=this;
}
var _1db=str.split(" ");
for(var i=0;i<_1db.length;i++){
_1db[i]=_1db[i].charAt(0).toUpperCase()+_1db[i].substring(1);
}
return _1db.join(" ");
};
dojo.string.isBlank=function(str){
if(!dojo.lang.isString(str)){
return true;
}
return (dojo.string.trim(str).length==0);
};
dojo.string.encodeAscii=function(str){
if(!dojo.lang.isString(str)){
return str;
}
var ret="";
var _1e0=escape(str);
var _1e1,re=/%u([0-9A-F]{4})/i;
while((_1e1=_1e0.match(re))){
var num=Number("0x"+_1e1[1]);
var _1e4=escape("&#"+num+";");
ret+=_1e0.substring(0,_1e1.index)+_1e4;
_1e0=_1e0.substring(_1e1.index+_1e1[0].length);
}
ret+=_1e0.replace(/\+/g,"%2B");
return ret;
};
dojo.string.escape=function(type,str){
var args=dojo.lang.toArray(arguments,1);
switch(type.toLowerCase()){
case "xml":
case "html":
case "xhtml":
return dojo.string.escapeXml.apply(this,args);
case "sql":
return dojo.string.escapeSql.apply(this,args);
case "regexp":
case "regex":
return dojo.string.escapeRegExp.apply(this,args);
case "javascript":
case "jscript":
case "js":
return dojo.string.escapeJavaScript.apply(this,args);
case "ascii":
return dojo.string.encodeAscii.apply(this,args);
default:
return str;
}
};
dojo.string.escapeXml=function(str,_1e9){
str=str.replace(/&/gm,"&amp;").replace(/</gm,"&lt;").replace(/>/gm,"&gt;").replace(/"/gm,"&quot;");
if(!_1e9){
str=str.replace(/'/gm,"&#39;");
}
return str;
};
dojo.string.escapeSql=function(str){
return str.replace(/'/gm,"''");
};
dojo.string.escapeRegExp=function(str){
return str.replace(/\\/gm,"\\\\").replace(/([\f\b\n\t\r[\^$|?*+(){}])/gm,"\\$1");
};
dojo.string.escapeJavaScript=function(str){
return str.replace(/(["'\f\b\n\t\r])/gm,"\\$1");
};
dojo.string.escapeString=function(str){
return ("\""+str.replace(/(["\\])/g,"\\$1")+"\"").replace(/[\f]/g,"\\f").replace(/[\b]/g,"\\b").replace(/[\n]/g,"\\n").replace(/[\t]/g,"\\t").replace(/[\r]/g,"\\r");
};
dojo.string.summary=function(str,len){
if(!len||str.length<=len){
return str;
}
return str.substring(0,len).replace(/\.+$/,"")+"...";
};
dojo.string.endsWith=function(str,end,_1f2){
if(_1f2){
str=str.toLowerCase();
end=end.toLowerCase();
}
if((str.length-end.length)<0){
return false;
}
return str.lastIndexOf(end)==str.length-end.length;
};
dojo.string.endsWithAny=function(str){
for(var i=1;i<arguments.length;i++){
if(dojo.string.endsWith(str,arguments[i])){
return true;
}
}
return false;
};
dojo.string.startsWith=function(str,_1f6,_1f7){
if(_1f7){
str=str.toLowerCase();
_1f6=_1f6.toLowerCase();
}
return str.indexOf(_1f6)==0;
};
dojo.string.startsWithAny=function(str){
for(var i=1;i<arguments.length;i++){
if(dojo.string.startsWith(str,arguments[i])){
return true;
}
}
return false;
};
dojo.string.has=function(str){
for(var i=1;i<arguments.length;i++){
if(str.indexOf(arguments[i])>-1){
return true;
}
}
return false;
};
dojo.string.normalizeNewlines=function(text,_1fd){
if(_1fd=="\n"){
text=text.replace(/\r\n/g,"\n");
text=text.replace(/\r/g,"\n");
}else{
if(_1fd=="\r"){
text=text.replace(/\r\n/g,"\r");
text=text.replace(/\n/g,"\r");
}else{
text=text.replace(/([^\r])\n/g,"$1\r\n").replace(/\r([^\n])/g,"\r\n$1");
}
}
return text;
};
dojo.string.splitEscaped=function(str,_1ff){
var _200=[];
for(var i=0,_202=0;i<str.length;i++){
if(str.charAt(i)=="\\"){
i++;
continue;
}
if(str.charAt(i)==_1ff){
_200.push(str.substring(_202,i));
_202=i+1;
}
}
_200.push(str.substr(_202));
return _200;
};
dojo.provide("dojo.dom");
dojo.dom.ELEMENT_NODE=1;
dojo.dom.ATTRIBUTE_NODE=2;
dojo.dom.TEXT_NODE=3;
dojo.dom.CDATA_SECTION_NODE=4;
dojo.dom.ENTITY_REFERENCE_NODE=5;
dojo.dom.ENTITY_NODE=6;
dojo.dom.PROCESSING_INSTRUCTION_NODE=7;
dojo.dom.COMMENT_NODE=8;
dojo.dom.DOCUMENT_NODE=9;
dojo.dom.DOCUMENT_TYPE_NODE=10;
dojo.dom.DOCUMENT_FRAGMENT_NODE=11;
dojo.dom.NOTATION_NODE=12;
dojo.dom.dojoml="http://www.dojotoolkit.org/2004/dojoml";
dojo.dom.xmlns={svg:"http://www.w3.org/2000/svg",smil:"http://www.w3.org/2001/SMIL20/",mml:"http://www.w3.org/1998/Math/MathML",cml:"http://www.xml-cml.org",xlink:"http://www.w3.org/1999/xlink",xhtml:"http://www.w3.org/1999/xhtml",xul:"http://www.mozilla.org/keymaster/gatekeeper/there.is.only.xul",xbl:"http://www.mozilla.org/xbl",fo:"http://www.w3.org/1999/XSL/Format",xsl:"http://www.w3.org/1999/XSL/Transform",xslt:"http://www.w3.org/1999/XSL/Transform",xi:"http://www.w3.org/2001/XInclude",xforms:"http://www.w3.org/2002/01/xforms",saxon:"http://icl.com/saxon",xalan:"http://xml.apache.org/xslt",xsd:"http://www.w3.org/2001/XMLSchema",dt:"http://www.w3.org/2001/XMLSchema-datatypes",xsi:"http://www.w3.org/2001/XMLSchema-instance",rdf:"http://www.w3.org/1999/02/22-rdf-syntax-ns#",rdfs:"http://www.w3.org/2000/01/rdf-schema#",dc:"http://purl.org/dc/elements/1.1/",dcq:"http://purl.org/dc/qualifiers/1.0","soap-env":"http://schemas.xmlsoap.org/soap/envelope/",wsdl:"http://schemas.xmlsoap.org/wsdl/",AdobeExtensions:"http://ns.adobe.com/AdobeSVGViewerExtensions/3.0/"};
dojo.dom.isNode=function(wh){
if(typeof Element=="function"){
try{
return wh instanceof Element;
}
catch(e){
}
}else{
return wh&&!isNaN(wh.nodeType);
}
};
dojo.dom.getUniqueId=function(){
var _204=dojo.doc();
do{
var id="dj_unique_"+(++arguments.callee._idIncrement);
}while(_204.getElementById(id));
return id;
};
dojo.dom.getUniqueId._idIncrement=0;
dojo.dom.firstElement=dojo.dom.getFirstChildElement=function(_206,_207){
var node=_206.firstChild;
while(node&&node.nodeType!=dojo.dom.ELEMENT_NODE){
node=node.nextSibling;
}
if(_207&&node&&node.tagName&&node.tagName.toLowerCase()!=_207.toLowerCase()){
node=dojo.dom.nextElement(node,_207);
}
return node;
};
dojo.dom.lastElement=dojo.dom.getLastChildElement=function(_209,_20a){
var node=_209.lastChild;
while(node&&node.nodeType!=dojo.dom.ELEMENT_NODE){
node=node.previousSibling;
}
if(_20a&&node&&node.tagName&&node.tagName.toLowerCase()!=_20a.toLowerCase()){
node=dojo.dom.prevElement(node,_20a);
}
return node;
};
dojo.dom.nextElement=dojo.dom.getNextSiblingElement=function(node,_20d){
if(!node){
return null;
}
do{
node=node.nextSibling;
}while(node&&node.nodeType!=dojo.dom.ELEMENT_NODE);
if(node&&_20d&&_20d.toLowerCase()!=node.tagName.toLowerCase()){
return dojo.dom.nextElement(node,_20d);
}
return node;
};
dojo.dom.prevElement=dojo.dom.getPreviousSiblingElement=function(node,_20f){
if(!node){
return null;
}
if(_20f){
_20f=_20f.toLowerCase();
}
do{
node=node.previousSibling;
}while(node&&node.nodeType!=dojo.dom.ELEMENT_NODE);
if(node&&_20f&&_20f.toLowerCase()!=node.tagName.toLowerCase()){
return dojo.dom.prevElement(node,_20f);
}
return node;
};
dojo.dom.moveChildren=function(_210,_211,trim){
var _213=0;
if(trim){
while(_210.hasChildNodes()&&_210.firstChild.nodeType==dojo.dom.TEXT_NODE){
_210.removeChild(_210.firstChild);
}
while(_210.hasChildNodes()&&_210.lastChild.nodeType==dojo.dom.TEXT_NODE){
_210.removeChild(_210.lastChild);
}
}
while(_210.hasChildNodes()){
_211.appendChild(_210.firstChild);
_213++;
}
return _213;
};
dojo.dom.copyChildren=function(_214,_215,trim){
var _217=_214.cloneNode(true);
return this.moveChildren(_217,_215,trim);
};
dojo.dom.replaceChildren=function(node,_219){
var _21a=[];
if(dojo.render.html.ie){
for(var i=0;i<node.childNodes.length;i++){
_21a.push(node.childNodes[i]);
}
}
dojo.dom.removeChildren(node);
node.appendChild(_219);
for(var i=0;i<_21a.length;i++){
dojo.dom.destroyNode(_21a[i]);
}
};
dojo.dom.removeChildren=function(node){
var _21d=node.childNodes.length;
while(node.hasChildNodes()){
dojo.dom.removeNode(node.firstChild);
}
return _21d;
};
dojo.dom.replaceNode=function(node,_21f){
return node.parentNode.replaceChild(_21f,node);
};
dojo.dom.destroyNode=function(node){
if(node.parentNode){
node=dojo.dom.removeNode(node);
}
if(node.nodeType!=3){
if(dojo.evalObjPath("dojo.event.browser.clean",false)){
dojo.event.browser.clean(node);
}
if(dojo.render.html.ie){
node.outerHTML="";
}
}
};
dojo.dom.removeNode=function(node){
if(node&&node.parentNode){
return node.parentNode.removeChild(node);
}
};
dojo.dom.getAncestors=function(node,_223,_224){
var _225=[];
var _226=(_223&&(_223 instanceof Function||typeof _223=="function"));
while(node){
if(!_226||_223(node)){
_225.push(node);
}
if(_224&&_225.length>0){
return _225[0];
}
node=node.parentNode;
}
if(_224){
return null;
}
return _225;
};
dojo.dom.getAncestorsByTag=function(node,tag,_229){
tag=tag.toLowerCase();
return dojo.dom.getAncestors(node,function(el){
return ((el.tagName)&&(el.tagName.toLowerCase()==tag));
},_229);
};
dojo.dom.getFirstAncestorByTag=function(node,tag){
return dojo.dom.getAncestorsByTag(node,tag,true);
};
dojo.dom.isDescendantOf=function(node,_22e,_22f){
if(_22f&&node){
node=node.parentNode;
}
while(node){
if(node==_22e){
return true;
}
node=node.parentNode;
}
return false;
};
dojo.dom.innerXML=function(node){
if(node.innerXML){
return node.innerXML;
}else{
if(node.xml){
return node.xml;
}else{
if(typeof XMLSerializer!="undefined"){
return (new XMLSerializer()).serializeToString(node);
}
}
}
};
dojo.dom.createDocument=function(){
var doc=null;
var _232=dojo.doc();
if(!dj_undef("ActiveXObject")){
var _233=["MSXML2","Microsoft","MSXML","MSXML3"];
for(var i=0;i<_233.length;i++){
try{
doc=new ActiveXObject(_233[i]+".XMLDOM");
}
catch(e){
}
if(doc){
break;
}
}
}else{
if((_232.implementation)&&(_232.implementation.createDocument)){
doc=_232.implementation.createDocument("","",null);
}
}
return doc;
};
dojo.dom.createDocumentFromText=function(str,_236){
if(!_236){
_236="text/xml";
}
if(!dj_undef("DOMParser")){
var _237=new DOMParser();
return _237.parseFromString(str,_236);
}else{
if(!dj_undef("ActiveXObject")){
var _238=dojo.dom.createDocument();
if(_238){
_238.async=false;
_238.loadXML(str);
return _238;
}else{
dojo.debug("toXml didn't work?");
}
}else{
var _239=dojo.doc();
if(_239.createElement){
var tmp=_239.createElement("xml");
tmp.innerHTML=str;
if(_239.implementation&&_239.implementation.createDocument){
var _23b=_239.implementation.createDocument("foo","",null);
for(var i=0;i<tmp.childNodes.length;i++){
_23b.importNode(tmp.childNodes.item(i),true);
}
return _23b;
}
return ((tmp.document)&&(tmp.document.firstChild?tmp.document.firstChild:tmp));
}
}
}
return null;
};
dojo.dom.prependChild=function(node,_23e){
if(_23e.firstChild){
_23e.insertBefore(node,_23e.firstChild);
}else{
_23e.appendChild(node);
}
return true;
};
dojo.dom.insertBefore=function(node,ref,_241){
if((_241!=true)&&(node===ref||node.nextSibling===ref)){
return false;
}
var _242=ref.parentNode;
_242.insertBefore(node,ref);
return true;
};
dojo.dom.insertAfter=function(node,ref,_245){
var pn=ref.parentNode;
if(ref==pn.lastChild){
if((_245!=true)&&(node===ref)){
return false;
}
pn.appendChild(node);
}else{
return this.insertBefore(node,ref.nextSibling,_245);
}
return true;
};
dojo.dom.insertAtPosition=function(node,ref,_249){
if((!node)||(!ref)||(!_249)){
return false;
}
switch(_249.toLowerCase()){
case "before":
return dojo.dom.insertBefore(node,ref);
case "after":
return dojo.dom.insertAfter(node,ref);
case "first":
if(ref.firstChild){
return dojo.dom.insertBefore(node,ref.firstChild);
}else{
ref.appendChild(node);
return true;
}
break;
default:
ref.appendChild(node);
return true;
}
};
dojo.dom.insertAtIndex=function(node,_24b,_24c){
var _24d=_24b.childNodes;
if(!_24d.length||_24d.length==_24c){
_24b.appendChild(node);
return true;
}
if(_24c==0){
return dojo.dom.prependChild(node,_24b);
}
return dojo.dom.insertAfter(node,_24d[_24c-1]);
};
dojo.dom.textContent=function(node,text){
if(arguments.length>1){
var _250=dojo.doc();
dojo.dom.replaceChildren(node,_250.createTextNode(text));
return text;
}else{
if(node.textContent!=undefined){
return node.textContent;
}
var _251="";
if(node==null){
return _251;
}
for(var i=0;i<node.childNodes.length;i++){
switch(node.childNodes[i].nodeType){
case 1:
case 5:
_251+=dojo.dom.textContent(node.childNodes[i]);
break;
case 3:
case 2:
case 4:
_251+=node.childNodes[i].nodeValue;
break;
default:
break;
}
}
return _251;
}
};
dojo.dom.hasParent=function(node){
return Boolean(node&&node.parentNode&&dojo.dom.isNode(node.parentNode));
};
dojo.dom.isTag=function(node){
if(node&&node.tagName){
for(var i=1;i<arguments.length;i++){
if(node.tagName==String(arguments[i])){
return String(arguments[i]);
}
}
}
return "";
};
dojo.dom.setAttributeNS=function(elem,_257,_258,_259){
if(elem==null||((elem==undefined)&&(typeof elem=="undefined"))){
dojo.raise("No element given to dojo.dom.setAttributeNS");
}
if(!((elem.setAttributeNS==undefined)&&(typeof elem.setAttributeNS=="undefined"))){
elem.setAttributeNS(_257,_258,_259);
}else{
var _25a=elem.ownerDocument;
var _25b=_25a.createNode(2,_258,_257);
_25b.nodeValue=_259;
elem.setAttributeNode(_25b);
}
};
dojo.provide("dojo.undo.browser");
try{
if((!djConfig["preventBackButtonFix"])&&(!dojo.hostenv.post_load_)){
document.write("<iframe style='border: 0px; width: 1px; height: 1px; position: absolute; bottom: 0px; right: 0px; visibility: visible;' name='djhistory' id='djhistory' src='"+(dojo.hostenv.getBaseScriptUri()+"iframe_history.html")+"'></iframe>");
}
}
catch(e){
}
if(dojo.render.html.opera){
dojo.debug("Opera is not supported with dojo.undo.browser, so back/forward detection will not work.");
}
dojo.undo.browser={initialHref:(!dj_undef("window"))?window.location.href:"",initialHash:(!dj_undef("window"))?window.location.hash:"",moveForward:false,historyStack:[],forwardStack:[],historyIframe:null,bookmarkAnchor:null,locationTimer:null,setInitialState:function(args){
this.initialState=this._createState(this.initialHref,args,this.initialHash);
},addToHistory:function(args){
this.forwardStack=[];
var hash=null;
var url=null;
if(!this.historyIframe){
this.historyIframe=window.frames["djhistory"];
}
if(!this.bookmarkAnchor){
this.bookmarkAnchor=document.createElement("a");
dojo.body().appendChild(this.bookmarkAnchor);
this.bookmarkAnchor.style.display="none";
}
if(args["changeUrl"]){
hash="#"+((args["changeUrl"]!==true)?args["changeUrl"]:(new Date()).getTime());
if(this.historyStack.length==0&&this.initialState.urlHash==hash){
this.initialState=this._createState(url,args,hash);
return;
}else{
if(this.historyStack.length>0&&this.historyStack[this.historyStack.length-1].urlHash==hash){
this.historyStack[this.historyStack.length-1]=this._createState(url,args,hash);
return;
}
}
this.changingUrl=true;
setTimeout("window.location.href = '"+hash+"'; dojo.undo.browser.changingUrl = false;",1);
this.bookmarkAnchor.href=hash;
if(dojo.render.html.ie){
url=this._loadIframeHistory();
var _260=args["back"]||args["backButton"]||args["handle"];
var tcb=function(_262){
if(window.location.hash!=""){
setTimeout("window.location.href = '"+hash+"';",1);
}
_260.apply(this,[_262]);
};
if(args["back"]){
args.back=tcb;
}else{
if(args["backButton"]){
args.backButton=tcb;
}else{
if(args["handle"]){
args.handle=tcb;
}
}
}
var _263=args["forward"]||args["forwardButton"]||args["handle"];
var tfw=function(_265){
if(window.location.hash!=""){
window.location.href=hash;
}
if(_263){
_263.apply(this,[_265]);
}
};
if(args["forward"]){
args.forward=tfw;
}else{
if(args["forwardButton"]){
args.forwardButton=tfw;
}else{
if(args["handle"]){
args.handle=tfw;
}
}
}
}else{
if(dojo.render.html.moz){
if(!this.locationTimer){
this.locationTimer=setInterval("dojo.undo.browser.checkLocation();",200);
}
}
}
}else{
url=this._loadIframeHistory();
}
this.historyStack.push(this._createState(url,args,hash));
},checkLocation:function(){
if(!this.changingUrl){
var hsl=this.historyStack.length;
if((window.location.hash==this.initialHash||window.location.href==this.initialHref)&&(hsl==1)){
this.handleBackButton();
return;
}
if(this.forwardStack.length>0){
if(this.forwardStack[this.forwardStack.length-1].urlHash==window.location.hash){
this.handleForwardButton();
return;
}
}
if((hsl>=2)&&(this.historyStack[hsl-2])){
if(this.historyStack[hsl-2].urlHash==window.location.hash){
this.handleBackButton();
return;
}
}
}
},iframeLoaded:function(evt,_268){
if(!dojo.render.html.opera){
var _269=this._getUrlQuery(_268.href);
if(_269==null){
if(this.historyStack.length==1){
this.handleBackButton();
}
return;
}
if(this.moveForward){
this.moveForward=false;
return;
}
if(this.historyStack.length>=2&&_269==this._getUrlQuery(this.historyStack[this.historyStack.length-2].url)){
this.handleBackButton();
}else{
if(this.forwardStack.length>0&&_269==this._getUrlQuery(this.forwardStack[this.forwardStack.length-1].url)){
this.handleForwardButton();
}
}
}
},handleBackButton:function(){
var _26a=this.historyStack.pop();
if(!_26a){
return;
}
var last=this.historyStack[this.historyStack.length-1];
if(!last&&this.historyStack.length==0){
last=this.initialState;
}
if(last){
if(last.kwArgs["back"]){
last.kwArgs["back"]();
}else{
if(last.kwArgs["backButton"]){
last.kwArgs["backButton"]();
}else{
if(last.kwArgs["handle"]){
last.kwArgs.handle("back");
}
}
}
}
this.forwardStack.push(_26a);
},handleForwardButton:function(){
var last=this.forwardStack.pop();
if(!last){
return;
}
if(last.kwArgs["forward"]){
last.kwArgs.forward();
}else{
if(last.kwArgs["forwardButton"]){
last.kwArgs.forwardButton();
}else{
if(last.kwArgs["handle"]){
last.kwArgs.handle("forward");
}
}
}
this.historyStack.push(last);
},_createState:function(url,args,hash){
return {"url":url,"kwArgs":args,"urlHash":hash};
},_getUrlQuery:function(url){
var _271=url.split("?");
if(_271.length<2){
return null;
}else{
return _271[1];
}
},_loadIframeHistory:function(){
var url=dojo.hostenv.getBaseScriptUri()+"iframe_history.html?"+(new Date()).getTime();
this.moveForward=true;
dojo.io.setIFrameSrc(this.historyIframe,url,false);
return url;
}};
dojo.provide("dojo.io.BrowserIO");
if(!dj_undef("window")){
dojo.io.checkChildrenForFile=function(node){
var _274=false;
var _275=node.getElementsByTagName("input");
dojo.lang.forEach(_275,function(_276){
if(_274){
return;
}
if(_276.getAttribute("type")=="file"){
_274=true;
}
});
return _274;
};
dojo.io.formHasFile=function(_277){
return dojo.io.checkChildrenForFile(_277);
};
dojo.io.updateNode=function(node,_279){
node=dojo.byId(node);
var args=_279;
if(dojo.lang.isString(_279)){
args={url:_279};
}
args.mimetype="text/html";
args.load=function(t,d,e){
while(node.firstChild){
dojo.dom.destroyNode(node.firstChild);
}
node.innerHTML=d;
};
dojo.io.bind(args);
};
dojo.io.formFilter=function(node){
var type=(node.type||"").toLowerCase();
return !node.disabled&&node.name&&!dojo.lang.inArray(["file","submit","image","reset","button"],type);
};
dojo.io.encodeForm=function(_280,_281,_282){
if((!_280)||(!_280.tagName)||(!_280.tagName.toLowerCase()=="form")){
dojo.raise("Attempted to encode a non-form element.");
}
if(!_282){
_282=dojo.io.formFilter;
}
var enc=/utf/i.test(_281||"")?encodeURIComponent:dojo.string.encodeAscii;
var _284=[];
for(var i=0;i<_280.elements.length;i++){
var elm=_280.elements[i];
if(!elm||elm.tagName.toLowerCase()=="fieldset"||!_282(elm)){
continue;
}
var name=enc(elm.name);
var type=elm.type.toLowerCase();
if(type=="select-multiple"){
for(var j=0;j<elm.options.length;j++){
if(elm.options[j].selected){
_284.push(name+"="+enc(elm.options[j].value));
}
}
}else{
if(dojo.lang.inArray(["radio","checkbox"],type)){
if(elm.checked){
_284.push(name+"="+enc(elm.value));
}
}else{
_284.push(name+"="+enc(elm.value));
}
}
}
var _28a=_280.getElementsByTagName("input");
for(var i=0;i<_28a.length;i++){
var _28b=_28a[i];
if(_28b.type.toLowerCase()=="image"&&_28b.form==_280&&_282(_28b)){
var name=enc(_28b.name);
_284.push(name+"="+enc(_28b.value));
_284.push(name+".x=0");
_284.push(name+".y=0");
}
}
return _284.join("&")+"&";
};
dojo.io.FormBind=function(args){
this.bindArgs={};
if(args&&args.formNode){
this.init(args);
}else{
if(args){
this.init({formNode:args});
}
}
};
dojo.lang.extend(dojo.io.FormBind,{form:null,bindArgs:null,clickedButton:null,init:function(args){
var form=dojo.byId(args.formNode);
if(!form||!form.tagName||form.tagName.toLowerCase()!="form"){
throw new Error("FormBind: Couldn't apply, invalid form");
}else{
if(this.form==form){
return;
}else{
if(this.form){
throw new Error("FormBind: Already applied to a form");
}
}
}
dojo.lang.mixin(this.bindArgs,args);
this.form=form;
this.connect(form,"onsubmit","submit");
for(var i=0;i<form.elements.length;i++){
var node=form.elements[i];
if(node&&node.type&&dojo.lang.inArray(["submit","button"],node.type.toLowerCase())){
this.connect(node,"onclick","click");
}
}
var _291=form.getElementsByTagName("input");
for(var i=0;i<_291.length;i++){
var _292=_291[i];
if(_292.type.toLowerCase()=="image"&&_292.form==form){
this.connect(_292,"onclick","click");
}
}
},onSubmit:function(form){
return true;
},submit:function(e){
e.preventDefault();
if(this.onSubmit(this.form)){
dojo.io.bind(dojo.lang.mixin(this.bindArgs,{formFilter:dojo.lang.hitch(this,"formFilter")}));
}
},click:function(e){
var node=e.currentTarget;
if(node.disabled){
return;
}
this.clickedButton=node;
},formFilter:function(node){
var type=(node.type||"").toLowerCase();
var _299=false;
if(node.disabled||!node.name){
_299=false;
}else{
if(dojo.lang.inArray(["submit","button","image"],type)){
if(!this.clickedButton){
this.clickedButton=node;
}
_299=node==this.clickedButton;
}else{
_299=!dojo.lang.inArray(["file","submit","reset","button"],type);
}
}
return _299;
},connect:function(_29a,_29b,_29c){
if(dojo.evalObjPath("dojo.event.connect")){
dojo.event.connect(_29a,_29b,this,_29c);
}else{
var fcn=dojo.lang.hitch(this,_29c);
_29a[_29b]=function(e){
if(!e){
e=window.event;
}
if(!e.currentTarget){
e.currentTarget=e.srcElement;
}
if(!e.preventDefault){
e.preventDefault=function(){
window.event.returnValue=false;
};
}
fcn(e);
};
}
}});
dojo.io.XMLHTTPTransport=new function(){
var _29f=this;
var _2a0={};
this.useCache=false;
this.preventCache=false;
function getCacheKey(url,_2a2,_2a3){
return url+"|"+_2a2+"|"+_2a3.toLowerCase();
}
function addToCache(url,_2a5,_2a6,http){
_2a0[getCacheKey(url,_2a5,_2a6)]=http;
}
function getFromCache(url,_2a9,_2aa){
return _2a0[getCacheKey(url,_2a9,_2aa)];
}
this.clearCache=function(){
_2a0={};
};
function doLoad(_2ab,http,url,_2ae,_2af){
if(((http.status>=200)&&(http.status<300))||(http.status==304)||(location.protocol=="file:"&&(http.status==0||http.status==undefined))||(location.protocol=="chrome:"&&(http.status==0||http.status==undefined))){
var ret;
if(_2ab.method.toLowerCase()=="head"){
var _2b1=http.getAllResponseHeaders();
ret={};
ret.toString=function(){
return _2b1;
};
var _2b2=_2b1.split(/[\r\n]+/g);
for(var i=0;i<_2b2.length;i++){
var pair=_2b2[i].match(/^([^:]+)\s*:\s*(.+)$/i);
if(pair){
ret[pair[1]]=pair[2];
}
}
}else{
if(_2ab.mimetype=="text/javascript"){
try{
ret=dj_eval(http.responseText);
}
catch(e){
dojo.debug(e);
dojo.debug(http.responseText);
ret=null;
}
}else{
if(_2ab.mimetype=="text/json"||_2ab.mimetype=="application/json"){
try{
ret=dj_eval("("+http.responseText+")");
}
catch(e){
dojo.debug(e);
dojo.debug(http.responseText);
ret=false;
}
}else{
if((_2ab.mimetype=="application/xml")||(_2ab.mimetype=="text/xml")){
ret=http.responseXML;
if(!ret||typeof ret=="string"||!http.getResponseHeader("Content-Type")){
ret=dojo.dom.createDocumentFromText(http.responseText);
}
}else{
ret=http.responseText;
}
}
}
}
if(_2af){
addToCache(url,_2ae,_2ab.method,http);
}
_2ab[(typeof _2ab.load=="function")?"load":"handle"]("load",ret,http,_2ab);
}else{
var _2b5=new dojo.io.Error("XMLHttpTransport Error: "+http.status+" "+http.statusText);
_2ab[(typeof _2ab.error=="function")?"error":"handle"]("error",_2b5,http,_2ab);
}
}
function setHeaders(http,_2b7){
if(_2b7["headers"]){
for(var _2b8 in _2b7["headers"]){
if(_2b8.toLowerCase()=="content-type"&&!_2b7["contentType"]){
_2b7["contentType"]=_2b7["headers"][_2b8];
}else{
http.setRequestHeader(_2b8,_2b7["headers"][_2b8]);
}
}
}
}
this.inFlight=[];
this.inFlightTimer=null;
this.startWatchingInFlight=function(){
if(!this.inFlightTimer){
this.inFlightTimer=setTimeout("dojo.io.XMLHTTPTransport.watchInFlight();",10);
}
};
this.watchInFlight=function(){
var now=null;
if(!dojo.hostenv._blockAsync&&!_29f._blockAsync){
for(var x=this.inFlight.length-1;x>=0;x--){
try{
var tif=this.inFlight[x];
if(!tif||tif.http._aborted||!tif.http.readyState){
this.inFlight.splice(x,1);
continue;
}
if(4==tif.http.readyState){
this.inFlight.splice(x,1);
doLoad(tif.req,tif.http,tif.url,tif.query,tif.useCache);
}else{
if(tif.startTime){
if(!now){
now=(new Date()).getTime();
}
if(tif.startTime+(tif.req.timeoutSeconds*1000)<now){
if(typeof tif.http.abort=="function"){
tif.http.abort();
}
this.inFlight.splice(x,1);
tif.req[(typeof tif.req.timeout=="function")?"timeout":"handle"]("timeout",null,tif.http,tif.req);
}
}
}
}
catch(e){
try{
var _2bc=new dojo.io.Error("XMLHttpTransport.watchInFlight Error: "+e);
tif.req[(typeof tif.req.error=="function")?"error":"handle"]("error",_2bc,tif.http,tif.req);
}
catch(e2){
dojo.debug("XMLHttpTransport error callback failed: "+e2);
}
}
}
}
clearTimeout(this.inFlightTimer);
if(this.inFlight.length==0){
this.inFlightTimer=null;
return;
}
this.inFlightTimer=setTimeout("dojo.io.XMLHTTPTransport.watchInFlight();",10);
};
var _2bd=dojo.hostenv.getXmlhttpObject()?true:false;
this.canHandle=function(_2be){
return _2bd&&dojo.lang.inArray(["text/plain","text/html","application/xml","text/xml","text/javascript","text/json","application/json"],(_2be["mimetype"].toLowerCase()||""))&&!(_2be["formNode"]&&dojo.io.formHasFile(_2be["formNode"]));
};
this.multipartBoundary="45309FFF-BD65-4d50-99C9-36986896A96F";
this.bind=function(_2bf){
if(!_2bf["url"]){
if(!_2bf["formNode"]&&(_2bf["backButton"]||_2bf["back"]||_2bf["changeUrl"]||_2bf["watchForURL"])&&(!djConfig.preventBackButtonFix)){
dojo.deprecated("Using dojo.io.XMLHTTPTransport.bind() to add to browser history without doing an IO request","Use dojo.undo.browser.addToHistory() instead.","0.4");
dojo.undo.browser.addToHistory(_2bf);
return true;
}
}
var url=_2bf.url;
var _2c1="";
if(_2bf["formNode"]){
var ta=_2bf.formNode.getAttribute("action");
if((ta)&&(!_2bf["url"])){
url=ta;
}
var tp=_2bf.formNode.getAttribute("method");
if((tp)&&(!_2bf["method"])){
_2bf.method=tp;
}
_2c1+=dojo.io.encodeForm(_2bf.formNode,_2bf.encoding,_2bf["formFilter"]);
}
if(url.indexOf("#")>-1){
dojo.debug("Warning: dojo.io.bind: stripping hash values from url:",url);
url=url.split("#")[0];
}
if(_2bf["file"]){
_2bf.method="post";
}
if(!_2bf["method"]){
_2bf.method="get";
}
if(_2bf.method.toLowerCase()=="get"){
_2bf.multipart=false;
}else{
if(_2bf["file"]){
_2bf.multipart=true;
}else{
if(!_2bf["multipart"]){
_2bf.multipart=false;
}
}
}
if(_2bf["backButton"]||_2bf["back"]||_2bf["changeUrl"]){
dojo.undo.browser.addToHistory(_2bf);
}
var _2c4=_2bf["content"]||{};
if(_2bf.sendTransport){
_2c4["dojo.transport"]="xmlhttp";
}
do{
if(_2bf.postContent){
_2c1=_2bf.postContent;
break;
}
if(_2c4){
_2c1+=dojo.io.argsFromMap(_2c4,_2bf.encoding);
}
if(_2bf.method.toLowerCase()=="get"||!_2bf.multipart){
break;
}
var t=[];
if(_2c1.length){
var q=_2c1.split("&");
for(var i=0;i<q.length;++i){
if(q[i].length){
var p=q[i].split("=");
t.push("--"+this.multipartBoundary,"Content-Disposition: form-data; name=\""+p[0]+"\"","",p[1]);
}
}
}
if(_2bf.file){
if(dojo.lang.isArray(_2bf.file)){
for(var i=0;i<_2bf.file.length;++i){
var o=_2bf.file[i];
t.push("--"+this.multipartBoundary,"Content-Disposition: form-data; name=\""+o.name+"\"; filename=\""+("fileName" in o?o.fileName:o.name)+"\"","Content-Type: "+("contentType" in o?o.contentType:"application/octet-stream"),"",o.content);
}
}else{
var o=_2bf.file;
t.push("--"+this.multipartBoundary,"Content-Disposition: form-data; name=\""+o.name+"\"; filename=\""+("fileName" in o?o.fileName:o.name)+"\"","Content-Type: "+("contentType" in o?o.contentType:"application/octet-stream"),"",o.content);
}
}
if(t.length){
t.push("--"+this.multipartBoundary+"--","");
_2c1=t.join("\r\n");
}
}while(false);
var _2ca=_2bf["sync"]?false:true;
var _2cb=_2bf["preventCache"]||(this.preventCache==true&&_2bf["preventCache"]!=false);
var _2cc=_2bf["useCache"]==true||(this.useCache==true&&_2bf["useCache"]!=false);
if(!_2cb&&_2cc){
var _2cd=getFromCache(url,_2c1,_2bf.method);
if(_2cd){
doLoad(_2bf,_2cd,url,_2c1,false);
return;
}
}
var http=dojo.hostenv.getXmlhttpObject(_2bf);
var _2cf=false;
if(_2ca){
var _2d0=this.inFlight.push({"req":_2bf,"http":http,"url":url,"query":_2c1,"useCache":_2cc,"startTime":_2bf.timeoutSeconds?(new Date()).getTime():0});
this.startWatchingInFlight();
}else{
_29f._blockAsync=true;
}
if(_2bf.method.toLowerCase()=="post"){
if(!_2bf.user){
http.open("POST",url,_2ca);
}else{
http.open("POST",url,_2ca,_2bf.user,_2bf.password);
}
setHeaders(http,_2bf);
http.setRequestHeader("Content-Type",_2bf.multipart?("multipart/form-data; boundary="+this.multipartBoundary):(_2bf.contentType||"application/x-www-form-urlencoded"));
try{
http.send(_2c1);
}
catch(e){
if(typeof http.abort=="function"){
http.abort();
}
doLoad(_2bf,{status:404},url,_2c1,_2cc);
}
}else{
var _2d1=url;
if(_2c1!=""){
_2d1+=(_2d1.indexOf("?")>-1?"&":"?")+_2c1;
}
if(_2cb){
_2d1+=(dojo.string.endsWithAny(_2d1,"?","&")?"":(_2d1.indexOf("?")>-1?"&":"?"))+"dojo.preventCache="+new Date().valueOf();
}
if(!_2bf.user){
http.open(_2bf.method.toUpperCase(),_2d1,_2ca);
}else{
http.open(_2bf.method.toUpperCase(),_2d1,_2ca,_2bf.user,_2bf.password);
}
setHeaders(http,_2bf);
try{
http.send(null);
}
catch(e){
if(typeof http.abort=="function"){
http.abort();
}
doLoad(_2bf,{status:404},url,_2c1,_2cc);
}
}
if(!_2ca){
doLoad(_2bf,http,url,_2c1,_2cc);
_29f._blockAsync=false;
}
_2bf.abort=function(){
try{
http._aborted=true;
}
catch(e){
}
return http.abort();
};
return;
};
dojo.io.transports.addTransport("XMLHTTPTransport");
};
}
dojo.provide("dojo.io.cookie");
dojo.io.cookie.setCookie=function(name,_2d3,days,path,_2d6,_2d7){
var _2d8=-1;
if((typeof days=="number")&&(days>=0)){
var d=new Date();
d.setTime(d.getTime()+(days*24*60*60*1000));
_2d8=d.toGMTString();
}
_2d3=escape(_2d3);
document.cookie=name+"="+_2d3+";"+(_2d8!=-1?" expires="+_2d8+";":"")+(path?"path="+path:"")+(_2d6?"; domain="+_2d6:"")+(_2d7?"; secure":"");
};
dojo.io.cookie.set=dojo.io.cookie.setCookie;
dojo.io.cookie.getCookie=function(name){
var idx=document.cookie.lastIndexOf(name+"=");
if(idx==-1){
return null;
}
var _2dc=document.cookie.substring(idx+name.length+1);
var end=_2dc.indexOf(";");
if(end==-1){
end=_2dc.length;
}
_2dc=_2dc.substring(0,end);
_2dc=unescape(_2dc);
return _2dc;
};
dojo.io.cookie.get=dojo.io.cookie.getCookie;
dojo.io.cookie.deleteCookie=function(name){
dojo.io.cookie.setCookie(name,"-",0);
};
dojo.io.cookie.setObjectCookie=function(name,obj,days,path,_2e3,_2e4,_2e5){
if(arguments.length==5){
_2e5=_2e3;
_2e3=null;
_2e4=null;
}
var _2e6=[],_2e7,_2e8="";
if(!_2e5){
_2e7=dojo.io.cookie.getObjectCookie(name);
}
if(days>=0){
if(!_2e7){
_2e7={};
}
for(var prop in obj){
if(obj[prop]==null){
delete _2e7[prop];
}else{
if((typeof obj[prop]=="string")||(typeof obj[prop]=="number")){
_2e7[prop]=obj[prop];
}
}
}
prop=null;
for(var prop in _2e7){
_2e6.push(escape(prop)+"="+escape(_2e7[prop]));
}
_2e8=_2e6.join("&");
}
dojo.io.cookie.setCookie(name,_2e8,days,path,_2e3,_2e4);
};
dojo.io.cookie.getObjectCookie=function(name){
var _2eb=null,_2ec=dojo.io.cookie.getCookie(name);
if(_2ec){
_2eb={};
var _2ed=_2ec.split("&");
for(var i=0;i<_2ed.length;i++){
var pair=_2ed[i].split("=");
var _2f0=pair[1];
if(isNaN(_2f0)){
_2f0=unescape(pair[1]);
}
_2eb[unescape(pair[0])]=_2f0;
}
}
return _2eb;
};
dojo.io.cookie.isSupported=function(){
if(typeof navigator.cookieEnabled!="boolean"){
dojo.io.cookie.setCookie("__TestingYourBrowserForCookieSupport__","CookiesAllowed",90,null);
var _2f1=dojo.io.cookie.getCookie("__TestingYourBrowserForCookieSupport__");
navigator.cookieEnabled=(_2f1=="CookiesAllowed");
if(navigator.cookieEnabled){
this.deleteCookie("__TestingYourBrowserForCookieSupport__");
}
}
return navigator.cookieEnabled;
};
if(!dojo.io.cookies){
dojo.io.cookies=dojo.io.cookie;
}
dojo.provide("dojo.io.*");
dojo.provide("dojo.event.common");
dojo.event=new function(){
this._canTimeout=dojo.lang.isFunction(dj_global["setTimeout"])||dojo.lang.isAlien(dj_global["setTimeout"]);
function interpolateArgs(args,_2f3){
var dl=dojo.lang;
var ao={srcObj:dj_global,srcFunc:null,adviceObj:dj_global,adviceFunc:null,aroundObj:null,aroundFunc:null,adviceType:(args.length>2)?args[0]:"after",precedence:"last",once:false,delay:null,rate:0,adviceMsg:false};
switch(args.length){
case 0:
return;
case 1:
return;
case 2:
ao.srcFunc=args[0];
ao.adviceFunc=args[1];
break;
case 3:
if((dl.isObject(args[0]))&&(dl.isString(args[1]))&&(dl.isString(args[2]))){
ao.adviceType="after";
ao.srcObj=args[0];
ao.srcFunc=args[1];
ao.adviceFunc=args[2];
}else{
if((dl.isString(args[1]))&&(dl.isString(args[2]))){
ao.srcFunc=args[1];
ao.adviceFunc=args[2];
}else{
if((dl.isObject(args[0]))&&(dl.isString(args[1]))&&(dl.isFunction(args[2]))){
ao.adviceType="after";
ao.srcObj=args[0];
ao.srcFunc=args[1];
var _2f6=dl.nameAnonFunc(args[2],ao.adviceObj,_2f3);
ao.adviceFunc=_2f6;
}else{
if((dl.isFunction(args[0]))&&(dl.isObject(args[1]))&&(dl.isString(args[2]))){
ao.adviceType="after";
ao.srcObj=dj_global;
var _2f6=dl.nameAnonFunc(args[0],ao.srcObj,_2f3);
ao.srcFunc=_2f6;
ao.adviceObj=args[1];
ao.adviceFunc=args[2];
}
}
}
}
break;
case 4:
if((dl.isObject(args[0]))&&(dl.isObject(args[2]))){
ao.adviceType="after";
ao.srcObj=args[0];
ao.srcFunc=args[1];
ao.adviceObj=args[2];
ao.adviceFunc=args[3];
}else{
if((dl.isString(args[0]))&&(dl.isString(args[1]))&&(dl.isObject(args[2]))){
ao.adviceType=args[0];
ao.srcObj=dj_global;
ao.srcFunc=args[1];
ao.adviceObj=args[2];
ao.adviceFunc=args[3];
}else{
if((dl.isString(args[0]))&&(dl.isFunction(args[1]))&&(dl.isObject(args[2]))){
ao.adviceType=args[0];
ao.srcObj=dj_global;
var _2f6=dl.nameAnonFunc(args[1],dj_global,_2f3);
ao.srcFunc=_2f6;
ao.adviceObj=args[2];
ao.adviceFunc=args[3];
}else{
if((dl.isString(args[0]))&&(dl.isObject(args[1]))&&(dl.isString(args[2]))&&(dl.isFunction(args[3]))){
ao.srcObj=args[1];
ao.srcFunc=args[2];
var _2f6=dl.nameAnonFunc(args[3],dj_global,_2f3);
ao.adviceObj=dj_global;
ao.adviceFunc=_2f6;
}else{
if(dl.isObject(args[1])){
ao.srcObj=args[1];
ao.srcFunc=args[2];
ao.adviceObj=dj_global;
ao.adviceFunc=args[3];
}else{
if(dl.isObject(args[2])){
ao.srcObj=dj_global;
ao.srcFunc=args[1];
ao.adviceObj=args[2];
ao.adviceFunc=args[3];
}else{
ao.srcObj=ao.adviceObj=ao.aroundObj=dj_global;
ao.srcFunc=args[1];
ao.adviceFunc=args[2];
ao.aroundFunc=args[3];
}
}
}
}
}
}
break;
case 6:
ao.srcObj=args[1];
ao.srcFunc=args[2];
ao.adviceObj=args[3];
ao.adviceFunc=args[4];
ao.aroundFunc=args[5];
ao.aroundObj=dj_global;
break;
default:
ao.srcObj=args[1];
ao.srcFunc=args[2];
ao.adviceObj=args[3];
ao.adviceFunc=args[4];
ao.aroundObj=args[5];
ao.aroundFunc=args[6];
ao.once=args[7];
ao.delay=args[8];
ao.rate=args[9];
ao.adviceMsg=args[10];
break;
}
if(dl.isFunction(ao.aroundFunc)){
var _2f6=dl.nameAnonFunc(ao.aroundFunc,ao.aroundObj,_2f3);
ao.aroundFunc=_2f6;
}
if(dl.isFunction(ao.srcFunc)){
ao.srcFunc=dl.getNameInObj(ao.srcObj,ao.srcFunc);
}
if(dl.isFunction(ao.adviceFunc)){
ao.adviceFunc=dl.getNameInObj(ao.adviceObj,ao.adviceFunc);
}
if((ao.aroundObj)&&(dl.isFunction(ao.aroundFunc))){
ao.aroundFunc=dl.getNameInObj(ao.aroundObj,ao.aroundFunc);
}
if(!ao.srcObj){
dojo.raise("bad srcObj for srcFunc: "+ao.srcFunc);
}
if(!ao.adviceObj){
dojo.raise("bad adviceObj for adviceFunc: "+ao.adviceFunc);
}
if(!ao.adviceFunc){
dojo.debug("bad adviceFunc for srcFunc: "+ao.srcFunc);
dojo.debugShallow(ao);
}
return ao;
}
this.connect=function(){
if(arguments.length==1){
var ao=arguments[0];
}else{
var ao=interpolateArgs(arguments,true);
}
if(dojo.lang.isString(ao.srcFunc)&&(ao.srcFunc.toLowerCase()=="onkey")){
if(dojo.render.html.ie){
ao.srcFunc="onkeydown";
this.connect(ao);
}
ao.srcFunc="onkeypress";
}
if(dojo.lang.isArray(ao.srcObj)&&ao.srcObj!=""){
var _2f8={};
for(var x in ao){
_2f8[x]=ao[x];
}
var mjps=[];
dojo.lang.forEach(ao.srcObj,function(src){
if((dojo.render.html.capable)&&(dojo.lang.isString(src))){
src=dojo.byId(src);
}
_2f8.srcObj=src;
mjps.push(dojo.event.connect.call(dojo.event,_2f8));
});
return mjps;
}
var mjp=dojo.event.MethodJoinPoint.getForMethod(ao.srcObj,ao.srcFunc);
if(ao.adviceFunc){
var mjp2=dojo.event.MethodJoinPoint.getForMethod(ao.adviceObj,ao.adviceFunc);
}
mjp.kwAddAdvice(ao);
return mjp;
};
this.log=function(a1,a2){
var _300;
if((arguments.length==1)&&(typeof a1=="object")){
_300=a1;
}else{
_300={srcObj:a1,srcFunc:a2};
}
_300.adviceFunc=function(){
var _301=[];
for(var x=0;x<arguments.length;x++){
_301.push(arguments[x]);
}
dojo.debug("("+_300.srcObj+")."+_300.srcFunc,":",_301.join(", "));
};
this.kwConnect(_300);
};
this.connectBefore=function(){
var args=["before"];
for(var i=0;i<arguments.length;i++){
args.push(arguments[i]);
}
return this.connect.apply(this,args);
};
this.connectAround=function(){
var args=["around"];
for(var i=0;i<arguments.length;i++){
args.push(arguments[i]);
}
return this.connect.apply(this,args);
};
this.connectOnce=function(){
var ao=interpolateArgs(arguments,true);
ao.once=true;
return this.connect(ao);
};
this._kwConnectImpl=function(_308,_309){
var fn=(_309)?"disconnect":"connect";
if(typeof _308["srcFunc"]=="function"){
_308.srcObj=_308["srcObj"]||dj_global;
var _30b=dojo.lang.nameAnonFunc(_308.srcFunc,_308.srcObj,true);
_308.srcFunc=_30b;
}
if(typeof _308["adviceFunc"]=="function"){
_308.adviceObj=_308["adviceObj"]||dj_global;
var _30b=dojo.lang.nameAnonFunc(_308.adviceFunc,_308.adviceObj,true);
_308.adviceFunc=_30b;
}
_308.srcObj=_308["srcObj"]||dj_global;
_308.adviceObj=_308["adviceObj"]||_308["targetObj"]||dj_global;
_308.adviceFunc=_308["adviceFunc"]||_308["targetFunc"];
return dojo.event[fn](_308);
};
this.kwConnect=function(_30c){
return this._kwConnectImpl(_30c,false);
};
this.disconnect=function(){
if(arguments.length==1){
var ao=arguments[0];
}else{
var ao=interpolateArgs(arguments,true);
}
if(!ao.adviceFunc){
return;
}
if(dojo.lang.isString(ao.srcFunc)&&(ao.srcFunc.toLowerCase()=="onkey")){
if(dojo.render.html.ie){
ao.srcFunc="onkeydown";
this.disconnect(ao);
}
ao.srcFunc="onkeypress";
}
if(!ao.srcObj[ao.srcFunc]){
return null;
}
var mjp=dojo.event.MethodJoinPoint.getForMethod(ao.srcObj,ao.srcFunc,true);
mjp.removeAdvice(ao.adviceObj,ao.adviceFunc,ao.adviceType,ao.once);
return mjp;
};
this.kwDisconnect=function(_30f){
return this._kwConnectImpl(_30f,true);
};
};
dojo.event.MethodInvocation=function(_310,obj,args){
this.jp_=_310;
this.object=obj;
this.args=[];
for(var x=0;x<args.length;x++){
this.args[x]=args[x];
}
this.around_index=-1;
};
dojo.event.MethodInvocation.prototype.proceed=function(){
this.around_index++;
if(this.around_index>=this.jp_.around.length){
return this.jp_.object[this.jp_.methodname].apply(this.jp_.object,this.args);
}else{
var ti=this.jp_.around[this.around_index];
var mobj=ti[0]||dj_global;
var meth=ti[1];
return mobj[meth].call(mobj,this);
}
};
dojo.event.MethodJoinPoint=function(obj,_318){
this.object=obj||dj_global;
this.methodname=_318;
this.methodfunc=this.object[_318];
this.squelch=false;
};
dojo.event.MethodJoinPoint.getForMethod=function(obj,_31a){
if(!obj){
obj=dj_global;
}
if(!obj[_31a]){
obj[_31a]=function(){
};
if(!obj[_31a]){
dojo.raise("Cannot set do-nothing method on that object "+_31a);
}
}else{
if((!dojo.lang.isFunction(obj[_31a]))&&(!dojo.lang.isAlien(obj[_31a]))){
return null;
}
}
var _31b=_31a+"$joinpoint";
var _31c=_31a+"$joinpoint$method";
var _31d=obj[_31b];
if(!_31d){
var _31e=false;
if(dojo.event["browser"]){
if((obj["attachEvent"])||(obj["nodeType"])||(obj["addEventListener"])){
_31e=true;
dojo.event.browser.addClobberNodeAttrs(obj,[_31b,_31c,_31a]);
}
}
var _31f=obj[_31a].length;
obj[_31c]=obj[_31a];
_31d=obj[_31b]=new dojo.event.MethodJoinPoint(obj,_31c);
obj[_31a]=function(){
var args=[];
if((_31e)&&(!arguments.length)){
var evt=null;
try{
if(obj.ownerDocument){
evt=obj.ownerDocument.parentWindow.event;
}else{
if(obj.documentElement){
evt=obj.documentElement.ownerDocument.parentWindow.event;
}else{
if(obj.event){
evt=obj.event;
}else{
evt=window.event;
}
}
}
}
catch(e){
evt=window.event;
}
if(evt){
args.push(dojo.event.browser.fixEvent(evt,this));
}
}else{
for(var x=0;x<arguments.length;x++){
if((x==0)&&(_31e)&&(dojo.event.browser.isEvent(arguments[x]))){
args.push(dojo.event.browser.fixEvent(arguments[x],this));
}else{
args.push(arguments[x]);
}
}
}
return _31d.run.apply(_31d,args);
};
obj[_31a].__preJoinArity=_31f;
}
return _31d;
};
dojo.lang.extend(dojo.event.MethodJoinPoint,{unintercept:function(){
this.object[this.methodname]=this.methodfunc;
this.before=[];
this.after=[];
this.around=[];
},disconnect:dojo.lang.forward("unintercept"),run:function(){
var obj=this.object||dj_global;
var args=arguments;
var _325=[];
for(var x=0;x<args.length;x++){
_325[x]=args[x];
}
var _327=function(marr){
if(!marr){
dojo.debug("Null argument to unrollAdvice()");
return;
}
var _329=marr[0]||dj_global;
var _32a=marr[1];
if(!_329[_32a]){
dojo.raise("function \""+_32a+"\" does not exist on \""+_329+"\"");
}
var _32b=marr[2]||dj_global;
var _32c=marr[3];
var msg=marr[6];
var _32e;
var to={args:[],jp_:this,object:obj,proceed:function(){
return _329[_32a].apply(_329,to.args);
}};
to.args=_325;
var _330=parseInt(marr[4]);
var _331=((!isNaN(_330))&&(marr[4]!==null)&&(typeof marr[4]!="undefined"));
if(marr[5]){
var rate=parseInt(marr[5]);
var cur=new Date();
var _334=false;
if((marr["last"])&&((cur-marr.last)<=rate)){
if(dojo.event._canTimeout){
if(marr["delayTimer"]){
clearTimeout(marr.delayTimer);
}
var tod=parseInt(rate*2);
var mcpy=dojo.lang.shallowCopy(marr);
marr.delayTimer=setTimeout(function(){
mcpy[5]=0;
_327(mcpy);
},tod);
}
return;
}else{
marr.last=cur;
}
}
if(_32c){
_32b[_32c].call(_32b,to);
}else{
if((_331)&&((dojo.render.html)||(dojo.render.svg))){
dj_global["setTimeout"](function(){
if(msg){
_329[_32a].call(_329,to);
}else{
_329[_32a].apply(_329,args);
}
},_330);
}else{
if(msg){
_329[_32a].call(_329,to);
}else{
_329[_32a].apply(_329,args);
}
}
}
};
var _337=function(){
if(this.squelch){
try{
return _327.apply(this,arguments);
}
catch(e){
dojo.debug(e);
}
}else{
return _327.apply(this,arguments);
}
};
if((this["before"])&&(this.before.length>0)){
dojo.lang.forEach(this.before.concat(new Array()),_337);
}
var _338;
try{
if((this["around"])&&(this.around.length>0)){
var mi=new dojo.event.MethodInvocation(this,obj,args);
_338=mi.proceed();
}else{
if(this.methodfunc){
_338=this.object[this.methodname].apply(this.object,args);
}
}
}
catch(e){
if(!this.squelch){
dojo.debug(e,"when calling",this.methodname,"on",this.object,"with arguments",args);
dojo.raise(e);
}
}
if((this["after"])&&(this.after.length>0)){
dojo.lang.forEach(this.after.concat(new Array()),_337);
}
return (this.methodfunc)?_338:null;
},getArr:function(kind){
var type="after";
if((typeof kind=="string")&&(kind.indexOf("before")!=-1)){
type="before";
}else{
if(kind=="around"){
type="around";
}
}
if(!this[type]){
this[type]=[];
}
return this[type];
},kwAddAdvice:function(args){
this.addAdvice(args["adviceObj"],args["adviceFunc"],args["aroundObj"],args["aroundFunc"],args["adviceType"],args["precedence"],args["once"],args["delay"],args["rate"],args["adviceMsg"]);
},addAdvice:function(_33d,_33e,_33f,_340,_341,_342,once,_344,rate,_346){
var arr=this.getArr(_341);
if(!arr){
dojo.raise("bad this: "+this);
}
var ao=[_33d,_33e,_33f,_340,_344,rate,_346];
if(once){
if(this.hasAdvice(_33d,_33e,_341,arr)>=0){
return;
}
}
if(_342=="first"){
arr.unshift(ao);
}else{
arr.push(ao);
}
},hasAdvice:function(_349,_34a,_34b,arr){
if(!arr){
arr=this.getArr(_34b);
}
var ind=-1;
for(var x=0;x<arr.length;x++){
var aao=(typeof _34a=="object")?(new String(_34a)).toString():_34a;
var a1o=(typeof arr[x][1]=="object")?(new String(arr[x][1])).toString():arr[x][1];
if((arr[x][0]==_349)&&(a1o==aao)){
ind=x;
}
}
return ind;
},removeAdvice:function(_351,_352,_353,once){
var arr=this.getArr(_353);
var ind=this.hasAdvice(_351,_352,_353,arr);
if(ind==-1){
return false;
}
while(ind!=-1){
arr.splice(ind,1);
if(once){
break;
}
ind=this.hasAdvice(_351,_352,_353,arr);
}
return true;
}});
dojo.provide("dojo.event.topic");
dojo.event.topic=new function(){
this.topics={};
this.getTopic=function(_357){
if(!this.topics[_357]){
this.topics[_357]=new this.TopicImpl(_357);
}
return this.topics[_357];
};
this.registerPublisher=function(_358,obj,_35a){
var _358=this.getTopic(_358);
_358.registerPublisher(obj,_35a);
};
this.subscribe=function(_35b,obj,_35d){
var _35b=this.getTopic(_35b);
_35b.subscribe(obj,_35d);
};
this.unsubscribe=function(_35e,obj,_360){
var _35e=this.getTopic(_35e);
_35e.unsubscribe(obj,_360);
};
this.destroy=function(_361){
this.getTopic(_361).destroy();
delete this.topics[_361];
};
this.publishApply=function(_362,args){
var _362=this.getTopic(_362);
_362.sendMessage.apply(_362,args);
};
this.publish=function(_364,_365){
var _364=this.getTopic(_364);
var args=[];
for(var x=1;x<arguments.length;x++){
args.push(arguments[x]);
}
_364.sendMessage.apply(_364,args);
};
};
dojo.event.topic.TopicImpl=function(_368){
this.topicName=_368;
this.subscribe=function(_369,_36a){
var tf=_36a||_369;
var to=(!_36a)?dj_global:_369;
return dojo.event.kwConnect({srcObj:this,srcFunc:"sendMessage",adviceObj:to,adviceFunc:tf});
};
this.unsubscribe=function(_36d,_36e){
var tf=(!_36e)?_36d:_36e;
var to=(!_36e)?null:_36d;
return dojo.event.kwDisconnect({srcObj:this,srcFunc:"sendMessage",adviceObj:to,adviceFunc:tf});
};
this._getJoinPoint=function(){
return dojo.event.MethodJoinPoint.getForMethod(this,"sendMessage");
};
this.setSquelch=function(_371){
this._getJoinPoint().squelch=_371;
};
this.destroy=function(){
this._getJoinPoint().disconnect();
};
this.registerPublisher=function(_372,_373){
dojo.event.connect(_372,_373,this,"sendMessage");
};
this.sendMessage=function(_374){
};
};
dojo.provide("dojo.event.browser");
dojo._ie_clobber=new function(){
this.clobberNodes=[];
function nukeProp(node,prop){
try{
node[prop]=null;
}
catch(e){
}
try{
delete node[prop];
}
catch(e){
}
try{
node.removeAttribute(prop);
}
catch(e){
}
}
this.clobber=function(_377){
var na;
var tna;
if(_377){
tna=_377.all||_377.getElementsByTagName("*");
na=[_377];
for(var x=0;x<tna.length;x++){
if(tna[x]["__doClobber__"]){
na.push(tna[x]);
}
}
}else{
try{
window.onload=null;
}
catch(e){
}
na=(this.clobberNodes.length)?this.clobberNodes:document.all;
}
tna=null;
var _37b={};
for(var i=na.length-1;i>=0;i=i-1){
var el=na[i];
try{
if(el&&el["__clobberAttrs__"]){
for(var j=0;j<el.__clobberAttrs__.length;j++){
nukeProp(el,el.__clobberAttrs__[j]);
}
nukeProp(el,"__clobberAttrs__");
nukeProp(el,"__doClobber__");
}
}
catch(e){
}
}
na=null;
};
};
if(dojo.render.html.ie){
dojo.addOnUnload(function(){
dojo._ie_clobber.clobber();
try{
if((dojo["widget"])&&(dojo.widget["manager"])){
dojo.widget.manager.destroyAll();
}
}
catch(e){
}
if(dojo.widget){
for(var name in dojo.widget._templateCache){
if(dojo.widget._templateCache[name].node){
dojo.dom.destroyNode(dojo.widget._templateCache[name].node);
dojo.widget._templateCache[name].node=null;
delete dojo.widget._templateCache[name].node;
}
}
}
try{
window.onload=null;
}
catch(e){
}
try{
window.onunload=null;
}
catch(e){
}
dojo._ie_clobber.clobberNodes=[];
});
}
dojo.event.browser=new function(){
var _380=0;
this.normalizedEventName=function(_381){
switch(_381){
case "CheckboxStateChange":
case "DOMAttrModified":
case "DOMMenuItemActive":
case "DOMMenuItemInactive":
case "DOMMouseScroll":
case "DOMNodeInserted":
case "DOMNodeRemoved":
case "RadioStateChange":
return _381;
break;
default:
return _381.toLowerCase();
break;
}
};
this.clean=function(node){
if(dojo.render.html.ie){
dojo._ie_clobber.clobber(node);
}
};
this.addClobberNode=function(node){
if(!dojo.render.html.ie){
return;
}
if(!node["__doClobber__"]){
node.__doClobber__=true;
dojo._ie_clobber.clobberNodes.push(node);
node.__clobberAttrs__=[];
}
};
this.addClobberNodeAttrs=function(node,_385){
if(!dojo.render.html.ie){
return;
}
this.addClobberNode(node);
for(var x=0;x<_385.length;x++){
node.__clobberAttrs__.push(_385[x]);
}
};
this.removeListener=function(node,_388,fp,_38a){
if(!_38a){
var _38a=false;
}
_388=dojo.event.browser.normalizedEventName(_388);
if((_388=="onkey")||(_388=="key")){
if(dojo.render.html.ie){
this.removeListener(node,"onkeydown",fp,_38a);
}
_388="onkeypress";
}
if(_388.substr(0,2)=="on"){
_388=_388.substr(2);
}
if(node.removeEventListener){
node.removeEventListener(_388,fp,_38a);
}
};
this.addListener=function(node,_38c,fp,_38e,_38f){
if(!node){
return;
}
if(!_38e){
var _38e=false;
}
_38c=dojo.event.browser.normalizedEventName(_38c);
if((_38c=="onkey")||(_38c=="key")){
if(dojo.render.html.ie){
this.addListener(node,"onkeydown",fp,_38e,_38f);
}
_38c="onkeypress";
}
if(_38c.substr(0,2)!="on"){
_38c="on"+_38c;
}
if(!_38f){
var _390=function(evt){
if(!evt){
evt=window.event;
}
var ret=fp(dojo.event.browser.fixEvent(evt,this));
if(_38e){
dojo.event.browser.stopEvent(evt);
}
return ret;
};
}else{
_390=fp;
}
if(node.addEventListener){
node.addEventListener(_38c.substr(2),_390,_38e);
return _390;
}else{
if(typeof node[_38c]=="function"){
var _393=node[_38c];
node[_38c]=function(e){
_393(e);
return _390(e);
};
}else{
node[_38c]=_390;
}
if(dojo.render.html.ie){
this.addClobberNodeAttrs(node,[_38c]);
}
return _390;
}
};
this.isEvent=function(obj){
return (typeof obj!="undefined")&&(obj)&&(typeof Event!="undefined")&&(obj.eventPhase);
};
this.currentEvent=null;
this.callListener=function(_396,_397){
if(typeof _396!="function"){
dojo.raise("listener not a function: "+_396);
}
dojo.event.browser.currentEvent.currentTarget=_397;
return _396.call(_397,dojo.event.browser.currentEvent);
};
this._stopPropagation=function(){
dojo.event.browser.currentEvent.cancelBubble=true;
};
this._preventDefault=function(){
dojo.event.browser.currentEvent.returnValue=false;
};
this.keys={KEY_BACKSPACE:8,KEY_TAB:9,KEY_CLEAR:12,KEY_ENTER:13,KEY_SHIFT:16,KEY_CTRL:17,KEY_ALT:18,KEY_PAUSE:19,KEY_CAPS_LOCK:20,KEY_ESCAPE:27,KEY_SPACE:32,KEY_PAGE_UP:33,KEY_PAGE_DOWN:34,KEY_END:35,KEY_HOME:36,KEY_LEFT_ARROW:37,KEY_UP_ARROW:38,KEY_RIGHT_ARROW:39,KEY_DOWN_ARROW:40,KEY_INSERT:45,KEY_DELETE:46,KEY_HELP:47,KEY_LEFT_WINDOW:91,KEY_RIGHT_WINDOW:92,KEY_SELECT:93,KEY_NUMPAD_0:96,KEY_NUMPAD_1:97,KEY_NUMPAD_2:98,KEY_NUMPAD_3:99,KEY_NUMPAD_4:100,KEY_NUMPAD_5:101,KEY_NUMPAD_6:102,KEY_NUMPAD_7:103,KEY_NUMPAD_8:104,KEY_NUMPAD_9:105,KEY_NUMPAD_MULTIPLY:106,KEY_NUMPAD_PLUS:107,KEY_NUMPAD_ENTER:108,KEY_NUMPAD_MINUS:109,KEY_NUMPAD_PERIOD:110,KEY_NUMPAD_DIVIDE:111,KEY_F1:112,KEY_F2:113,KEY_F3:114,KEY_F4:115,KEY_F5:116,KEY_F6:117,KEY_F7:118,KEY_F8:119,KEY_F9:120,KEY_F10:121,KEY_F11:122,KEY_F12:123,KEY_F13:124,KEY_F14:125,KEY_F15:126,KEY_NUM_LOCK:144,KEY_SCROLL_LOCK:145};
this.revKeys=[];
for(var key in this.keys){
this.revKeys[this.keys[key]]=key;
}
this.fixEvent=function(evt,_39a){
if(!evt){
if(window["event"]){
evt=window.event;
}
}
if((evt["type"])&&(evt["type"].indexOf("key")==0)){
evt.keys=this.revKeys;
for(var key in this.keys){
evt[key]=this.keys[key];
}
if(evt["type"]=="keydown"&&dojo.render.html.ie){
switch(evt.keyCode){
case evt.KEY_SHIFT:
case evt.KEY_CTRL:
case evt.KEY_ALT:
case evt.KEY_CAPS_LOCK:
case evt.KEY_LEFT_WINDOW:
case evt.KEY_RIGHT_WINDOW:
case evt.KEY_SELECT:
case evt.KEY_NUM_LOCK:
case evt.KEY_SCROLL_LOCK:
case evt.KEY_NUMPAD_0:
case evt.KEY_NUMPAD_1:
case evt.KEY_NUMPAD_2:
case evt.KEY_NUMPAD_3:
case evt.KEY_NUMPAD_4:
case evt.KEY_NUMPAD_5:
case evt.KEY_NUMPAD_6:
case evt.KEY_NUMPAD_7:
case evt.KEY_NUMPAD_8:
case evt.KEY_NUMPAD_9:
case evt.KEY_NUMPAD_PERIOD:
break;
case evt.KEY_NUMPAD_MULTIPLY:
case evt.KEY_NUMPAD_PLUS:
case evt.KEY_NUMPAD_ENTER:
case evt.KEY_NUMPAD_MINUS:
case evt.KEY_NUMPAD_DIVIDE:
break;
case evt.KEY_PAUSE:
case evt.KEY_TAB:
case evt.KEY_BACKSPACE:
case evt.KEY_ENTER:
case evt.KEY_ESCAPE:
case evt.KEY_PAGE_UP:
case evt.KEY_PAGE_DOWN:
case evt.KEY_END:
case evt.KEY_HOME:
case evt.KEY_LEFT_ARROW:
case evt.KEY_UP_ARROW:
case evt.KEY_RIGHT_ARROW:
case evt.KEY_DOWN_ARROW:
case evt.KEY_INSERT:
case evt.KEY_DELETE:
case evt.KEY_F1:
case evt.KEY_F2:
case evt.KEY_F3:
case evt.KEY_F4:
case evt.KEY_F5:
case evt.KEY_F6:
case evt.KEY_F7:
case evt.KEY_F8:
case evt.KEY_F9:
case evt.KEY_F10:
case evt.KEY_F11:
case evt.KEY_F12:
case evt.KEY_F12:
case evt.KEY_F13:
case evt.KEY_F14:
case evt.KEY_F15:
case evt.KEY_CLEAR:
case evt.KEY_HELP:
evt.key=evt.keyCode;
break;
default:
if(evt.ctrlKey||evt.altKey){
var _39c=evt.keyCode;
if(_39c>=65&&_39c<=90&&evt.shiftKey==false){
_39c+=32;
}
if(_39c>=1&&_39c<=26&&evt.ctrlKey){
_39c+=96;
}
evt.key=String.fromCharCode(_39c);
}
}
}else{
if(evt["type"]=="keypress"){
if(dojo.render.html.opera){
if(evt.which==0){
evt.key=evt.keyCode;
}else{
if(evt.which>0){
switch(evt.which){
case evt.KEY_SHIFT:
case evt.KEY_CTRL:
case evt.KEY_ALT:
case evt.KEY_CAPS_LOCK:
case evt.KEY_NUM_LOCK:
case evt.KEY_SCROLL_LOCK:
break;
case evt.KEY_PAUSE:
case evt.KEY_TAB:
case evt.KEY_BACKSPACE:
case evt.KEY_ENTER:
case evt.KEY_ESCAPE:
evt.key=evt.which;
break;
default:
var _39c=evt.which;
if((evt.ctrlKey||evt.altKey||evt.metaKey)&&(evt.which>=65&&evt.which<=90&&evt.shiftKey==false)){
_39c+=32;
}
evt.key=String.fromCharCode(_39c);
}
}
}
}else{
if(dojo.render.html.ie){
if(!evt.ctrlKey&&!evt.altKey&&evt.keyCode>=evt.KEY_SPACE){
evt.key=String.fromCharCode(evt.keyCode);
}
}else{
if(dojo.render.html.safari){
switch(evt.keyCode){
case 25:
evt.key=evt.KEY_TAB;
evt.shift=true;
break;
case 63232:
evt.key=evt.KEY_UP_ARROW;
break;
case 63233:
evt.key=evt.KEY_DOWN_ARROW;
break;
case 63234:
evt.key=evt.KEY_LEFT_ARROW;
break;
case 63235:
evt.key=evt.KEY_RIGHT_ARROW;
break;
case 63236:
evt.key=evt.KEY_F1;
break;
case 63237:
evt.key=evt.KEY_F2;
break;
case 63238:
evt.key=evt.KEY_F3;
break;
case 63239:
evt.key=evt.KEY_F4;
break;
case 63240:
evt.key=evt.KEY_F5;
break;
case 63241:
evt.key=evt.KEY_F6;
break;
case 63242:
evt.key=evt.KEY_F7;
break;
case 63243:
evt.key=evt.KEY_F8;
break;
case 63244:
evt.key=evt.KEY_F9;
break;
case 63245:
evt.key=evt.KEY_F10;
break;
case 63246:
evt.key=evt.KEY_F11;
break;
case 63247:
evt.key=evt.KEY_F12;
break;
case 63250:
evt.key=evt.KEY_PAUSE;
break;
case 63272:
evt.key=evt.KEY_DELETE;
break;
case 63273:
evt.key=evt.KEY_HOME;
break;
case 63275:
evt.key=evt.KEY_END;
break;
case 63276:
evt.key=evt.KEY_PAGE_UP;
break;
case 63277:
evt.key=evt.KEY_PAGE_DOWN;
break;
case 63302:
evt.key=evt.KEY_INSERT;
break;
case 63248:
case 63249:
case 63289:
break;
default:
evt.key=evt.charCode>=evt.KEY_SPACE?String.fromCharCode(evt.charCode):evt.keyCode;
}
}else{
evt.key=evt.charCode>0?String.fromCharCode(evt.charCode):evt.keyCode;
}
}
}
}
}
}
if(dojo.render.html.ie){
if(!evt.target){
evt.target=evt.srcElement;
}
if(!evt.currentTarget){
evt.currentTarget=(_39a?_39a:evt.srcElement);
}
if(!evt.layerX){
evt.layerX=evt.offsetX;
}
if(!evt.layerY){
evt.layerY=evt.offsetY;
}
var doc=(evt.srcElement&&evt.srcElement.ownerDocument)?evt.srcElement.ownerDocument:document;
var _39e=((dojo.render.html.ie55)||(doc["compatMode"]=="BackCompat"))?doc.body:doc.documentElement;
if(!evt.pageX){
evt.pageX=evt.clientX+(_39e.scrollLeft||0);
}
if(!evt.pageY){
evt.pageY=evt.clientY+(_39e.scrollTop||0);
}
if(evt.type=="mouseover"){
evt.relatedTarget=evt.fromElement;
}
if(evt.type=="mouseout"){
evt.relatedTarget=evt.toElement;
}
this.currentEvent=evt;
evt.callListener=this.callListener;
evt.stopPropagation=this._stopPropagation;
evt.preventDefault=this._preventDefault;
}
return evt;
};
this.stopEvent=function(evt){
if(window.event){
evt.cancelBubble=true;
evt.returnValue=false;
}else{
evt.preventDefault();
evt.stopPropagation();
}
};
};
dojo.provide("dojo.event.*");
dojo.provide("dojo.gfx.color");
dojo.gfx.color.Color=function(r,g,b,a){
if(dojo.lang.isArray(r)){
this.r=r[0];
this.g=r[1];
this.b=r[2];
this.a=r[3]||1;
}else{
if(dojo.lang.isString(r)){
var rgb=dojo.gfx.color.extractRGB(r);
this.r=rgb[0];
this.g=rgb[1];
this.b=rgb[2];
this.a=g||1;
}else{
if(r instanceof dojo.gfx.color.Color){
this.r=r.r;
this.b=r.b;
this.g=r.g;
this.a=r.a;
}else{
this.r=r;
this.g=g;
this.b=b;
this.a=a;
}
}
}
};
dojo.gfx.color.Color.fromArray=function(arr){
return new dojo.gfx.color.Color(arr[0],arr[1],arr[2],arr[3]);
};
dojo.extend(dojo.gfx.color.Color,{toRgb:function(_3a6){
if(_3a6){
return this.toRgba();
}else{
return [this.r,this.g,this.b];
}
},toRgba:function(){
return [this.r,this.g,this.b,this.a];
},toHex:function(){
return dojo.gfx.color.rgb2hex(this.toRgb());
},toCss:function(){
return "rgb("+this.toRgb().join()+")";
},toString:function(){
return this.toHex();
},blend:function(_3a7,_3a8){
var rgb=null;
if(dojo.lang.isArray(_3a7)){
rgb=_3a7;
}else{
if(_3a7 instanceof dojo.gfx.color.Color){
rgb=_3a7.toRgb();
}else{
rgb=new dojo.gfx.color.Color(_3a7).toRgb();
}
}
return dojo.gfx.color.blend(this.toRgb(),rgb,_3a8);
}});
dojo.gfx.color.named={white:[255,255,255],black:[0,0,0],red:[255,0,0],green:[0,255,0],lime:[0,255,0],blue:[0,0,255],navy:[0,0,128],gray:[128,128,128],silver:[192,192,192]};
dojo.gfx.color.blend=function(a,b,_3ac){
if(typeof a=="string"){
return dojo.gfx.color.blendHex(a,b,_3ac);
}
if(!_3ac){
_3ac=0;
}
_3ac=Math.min(Math.max(-1,_3ac),1);
_3ac=((_3ac+1)/2);
var c=[];
for(var x=0;x<3;x++){
c[x]=parseInt(b[x]+((a[x]-b[x])*_3ac));
}
return c;
};
dojo.gfx.color.blendHex=function(a,b,_3b1){
return dojo.gfx.color.rgb2hex(dojo.gfx.color.blend(dojo.gfx.color.hex2rgb(a),dojo.gfx.color.hex2rgb(b),_3b1));
};
dojo.gfx.color.extractRGB=function(_3b2){
var hex="0123456789abcdef";
_3b2=_3b2.toLowerCase();
if(_3b2.indexOf("rgb")==0){
var _3b4=_3b2.match(/rgba*\((\d+), *(\d+), *(\d+)/i);
var ret=_3b4.splice(1,3);
return ret;
}else{
var _3b6=dojo.gfx.color.hex2rgb(_3b2);
if(_3b6){
return _3b6;
}else{
return dojo.gfx.color.named[_3b2]||[255,255,255];
}
}
};
dojo.gfx.color.hex2rgb=function(hex){
var _3b8="0123456789ABCDEF";
var rgb=new Array(3);
if(hex.indexOf("#")==0){
hex=hex.substring(1);
}
hex=hex.toUpperCase();
if(hex.replace(new RegExp("["+_3b8+"]","g"),"")!=""){
return null;
}
if(hex.length==3){
rgb[0]=hex.charAt(0)+hex.charAt(0);
rgb[1]=hex.charAt(1)+hex.charAt(1);
rgb[2]=hex.charAt(2)+hex.charAt(2);
}else{
rgb[0]=hex.substring(0,2);
rgb[1]=hex.substring(2,4);
rgb[2]=hex.substring(4);
}
for(var i=0;i<rgb.length;i++){
rgb[i]=_3b8.indexOf(rgb[i].charAt(0))*16+_3b8.indexOf(rgb[i].charAt(1));
}
return rgb;
};
dojo.gfx.color.rgb2hex=function(r,g,b){
if(dojo.lang.isArray(r)){
g=r[1]||0;
b=r[2]||0;
r=r[0]||0;
}
var ret=dojo.lang.map([r,g,b],function(x){
x=new Number(x);
var s=x.toString(16);
while(s.length<2){
s="0"+s;
}
return s;
});
ret.unshift("#");
return ret.join("");
};
dojo.provide("dojo.lfx.Animation");
dojo.lfx.Line=function(_3c1,end){
this.start=_3c1;
this.end=end;
if(dojo.lang.isArray(_3c1)){
var diff=[];
dojo.lang.forEach(this.start,function(s,i){
diff[i]=this.end[i]-s;
},this);
this.getValue=function(n){
var res=[];
dojo.lang.forEach(this.start,function(s,i){
res[i]=(diff[i]*n)+s;
},this);
return res;
};
}else{
var diff=end-_3c1;
this.getValue=function(n){
return (diff*n)+this.start;
};
}
};
dojo.lfx.easeDefault=function(n){
if(dojo.render.html.khtml){
return (parseFloat("0.5")+((Math.sin((n+parseFloat("1.5"))*Math.PI))/2));
}else{
return (0.5+((Math.sin((n+1.5)*Math.PI))/2));
}
};
dojo.lfx.easeIn=function(n){
return Math.pow(n,3);
};
dojo.lfx.easeOut=function(n){
return (1-Math.pow(1-n,3));
};
dojo.lfx.easeInOut=function(n){
return ((3*Math.pow(n,2))-(2*Math.pow(n,3)));
};
dojo.lfx.IAnimation=function(){
};
dojo.lang.extend(dojo.lfx.IAnimation,{curve:null,duration:1000,easing:null,repeatCount:0,rate:25,handler:null,beforeBegin:null,onBegin:null,onAnimate:null,onEnd:null,onPlay:null,onPause:null,onStop:null,play:null,pause:null,stop:null,connect:function(evt,_3d0,_3d1){
if(!_3d1){
_3d1=_3d0;
_3d0=this;
}
_3d1=dojo.lang.hitch(_3d0,_3d1);
var _3d2=this[evt]||function(){
};
this[evt]=function(){
var ret=_3d2.apply(this,arguments);
_3d1.apply(this,arguments);
return ret;
};
return this;
},fire:function(evt,args){
if(this[evt]){
this[evt].apply(this,(args||[]));
}
return this;
},repeat:function(_3d6){
this.repeatCount=_3d6;
return this;
},_active:false,_paused:false});
dojo.lfx.Animation=function(_3d7,_3d8,_3d9,_3da,_3db,rate){
dojo.lfx.IAnimation.call(this);
if(dojo.lang.isNumber(_3d7)||(!_3d7&&_3d8.getValue)){
rate=_3db;
_3db=_3da;
_3da=_3d9;
_3d9=_3d8;
_3d8=_3d7;
_3d7=null;
}else{
if(_3d7.getValue||dojo.lang.isArray(_3d7)){
rate=_3da;
_3db=_3d9;
_3da=_3d8;
_3d9=_3d7;
_3d8=null;
_3d7=null;
}
}
if(dojo.lang.isArray(_3d9)){
this.curve=new dojo.lfx.Line(_3d9[0],_3d9[1]);
}else{
this.curve=_3d9;
}
if(_3d8!=null&&_3d8>0){
this.duration=_3d8;
}
if(_3db){
this.repeatCount=_3db;
}
if(rate){
this.rate=rate;
}
if(_3d7){
dojo.lang.forEach(["handler","beforeBegin","onBegin","onEnd","onPlay","onStop","onAnimate"],function(item){
if(_3d7[item]){
this.connect(item,_3d7[item]);
}
},this);
}
if(_3da&&dojo.lang.isFunction(_3da)){
this.easing=_3da;
}
};
dojo.inherits(dojo.lfx.Animation,dojo.lfx.IAnimation);
dojo.lang.extend(dojo.lfx.Animation,{_startTime:null,_endTime:null,_timer:null,_percent:0,_startRepeatCount:0,play:function(_3de,_3df){
if(_3df){
clearTimeout(this._timer);
this._active=false;
this._paused=false;
this._percent=0;
}else{
if(this._active&&!this._paused){
return this;
}
}
this.fire("handler",["beforeBegin"]);
this.fire("beforeBegin");
if(_3de>0){
setTimeout(dojo.lang.hitch(this,function(){
this.play(null,_3df);
}),_3de);
return this;
}
this._startTime=new Date().valueOf();
if(this._paused){
this._startTime-=(this.duration*this._percent/100);
}
this._endTime=this._startTime+this.duration;
this._active=true;
this._paused=false;
var step=this._percent/100;
var _3e1=this.curve.getValue(step);
if(this._percent==0){
if(!this._startRepeatCount){
this._startRepeatCount=this.repeatCount;
}
this.fire("handler",["begin",_3e1]);
this.fire("onBegin",[_3e1]);
}
this.fire("handler",["play",_3e1]);
this.fire("onPlay",[_3e1]);
this._cycle();
return this;
},pause:function(){
clearTimeout(this._timer);
if(!this._active){
return this;
}
this._paused=true;
var _3e2=this.curve.getValue(this._percent/100);
this.fire("handler",["pause",_3e2]);
this.fire("onPause",[_3e2]);
return this;
},gotoPercent:function(pct,_3e4){
clearTimeout(this._timer);
this._active=true;
this._paused=true;
this._percent=pct;
if(_3e4){
this.play();
}
return this;
},stop:function(_3e5){
clearTimeout(this._timer);
var step=this._percent/100;
if(_3e5){
step=1;
}
var _3e7=this.curve.getValue(step);
this.fire("handler",["stop",_3e7]);
this.fire("onStop",[_3e7]);
this._active=false;
this._paused=false;
return this;
},status:function(){
if(this._active){
return this._paused?"paused":"playing";
}else{
return "stopped";
}
return this;
},_cycle:function(){
clearTimeout(this._timer);
if(this._active){
var curr=new Date().valueOf();
var step=(curr-this._startTime)/(this._endTime-this._startTime);
if(step>=1){
step=1;
this._percent=100;
}else{
this._percent=step*100;
}
if((this.easing)&&(dojo.lang.isFunction(this.easing))){
step=this.easing(step);
}
var _3ea=this.curve.getValue(step);
this.fire("handler",["animate",_3ea]);
this.fire("onAnimate",[_3ea]);
if(step<1){
this._timer=setTimeout(dojo.lang.hitch(this,"_cycle"),this.rate);
}else{
this._active=false;
this.fire("handler",["end"]);
this.fire("onEnd");
if(this.repeatCount>0){
this.repeatCount--;
this.play(null,true);
}else{
if(this.repeatCount==-1){
this.play(null,true);
}else{
if(this._startRepeatCount){
this.repeatCount=this._startRepeatCount;
this._startRepeatCount=0;
}
}
}
}
}
return this;
}});
dojo.lfx.Combine=function(_3eb){
dojo.lfx.IAnimation.call(this);
this._anims=[];
this._animsEnded=0;
var _3ec=arguments;
if(_3ec.length==1&&(dojo.lang.isArray(_3ec[0])||dojo.lang.isArrayLike(_3ec[0]))){
_3ec=_3ec[0];
}
dojo.lang.forEach(_3ec,function(anim){
this._anims.push(anim);
anim.connect("onEnd",dojo.lang.hitch(this,"_onAnimsEnded"));
},this);
};
dojo.inherits(dojo.lfx.Combine,dojo.lfx.IAnimation);
dojo.lang.extend(dojo.lfx.Combine,{_animsEnded:0,play:function(_3ee,_3ef){
if(!this._anims.length){
return this;
}
this.fire("beforeBegin");
if(_3ee>0){
setTimeout(dojo.lang.hitch(this,function(){
this.play(null,_3ef);
}),_3ee);
return this;
}
if(_3ef||this._anims[0].percent==0){
this.fire("onBegin");
}
this.fire("onPlay");
this._animsCall("play",null,_3ef);
return this;
},pause:function(){
this.fire("onPause");
this._animsCall("pause");
return this;
},stop:function(_3f0){
this.fire("onStop");
this._animsCall("stop",_3f0);
return this;
},_onAnimsEnded:function(){
this._animsEnded++;
if(this._animsEnded>=this._anims.length){
this.fire("onEnd");
}
return this;
},_animsCall:function(_3f1){
var args=[];
if(arguments.length>1){
for(var i=1;i<arguments.length;i++){
args.push(arguments[i]);
}
}
var _3f4=this;
dojo.lang.forEach(this._anims,function(anim){
anim[_3f1](args);
},_3f4);
return this;
}});
dojo.lfx.Chain=function(_3f6){
dojo.lfx.IAnimation.call(this);
this._anims=[];
this._currAnim=-1;
var _3f7=arguments;
if(_3f7.length==1&&(dojo.lang.isArray(_3f7[0])||dojo.lang.isArrayLike(_3f7[0]))){
_3f7=_3f7[0];
}
var _3f8=this;
dojo.lang.forEach(_3f7,function(anim,i,_3fb){
this._anims.push(anim);
if(i<_3fb.length-1){
anim.connect("onEnd",dojo.lang.hitch(this,"_playNext"));
}else{
anim.connect("onEnd",dojo.lang.hitch(this,function(){
this.fire("onEnd");
}));
}
},this);
};
dojo.inherits(dojo.lfx.Chain,dojo.lfx.IAnimation);
dojo.lang.extend(dojo.lfx.Chain,{_currAnim:-1,play:function(_3fc,_3fd){
if(!this._anims.length){
return this;
}
if(_3fd||!this._anims[this._currAnim]){
this._currAnim=0;
}
var _3fe=this._anims[this._currAnim];
this.fire("beforeBegin");
if(_3fc>0){
setTimeout(dojo.lang.hitch(this,function(){
this.play(null,_3fd);
}),_3fc);
return this;
}
if(_3fe){
if(this._currAnim==0){
this.fire("handler",["begin",this._currAnim]);
this.fire("onBegin",[this._currAnim]);
}
this.fire("onPlay",[this._currAnim]);
_3fe.play(null,_3fd);
}
return this;
},pause:function(){
if(this._anims[this._currAnim]){
this._anims[this._currAnim].pause();
this.fire("onPause",[this._currAnim]);
}
return this;
},playPause:function(){
if(this._anims.length==0){
return this;
}
if(this._currAnim==-1){
this._currAnim=0;
}
var _3ff=this._anims[this._currAnim];
if(_3ff){
if(!_3ff._active||_3ff._paused){
this.play();
}else{
this.pause();
}
}
return this;
},stop:function(){
var _400=this._anims[this._currAnim];
if(_400){
_400.stop();
this.fire("onStop",[this._currAnim]);
}
return _400;
},_playNext:function(){
if(this._currAnim==-1||this._anims.length==0){
return this;
}
this._currAnim++;
if(this._anims[this._currAnim]){
this._anims[this._currAnim].play(null,true);
}
return this;
}});
dojo.lfx.combine=function(_401){
var _402=arguments;
if(dojo.lang.isArray(arguments[0])){
_402=arguments[0];
}
if(_402.length==1){
return _402[0];
}
return new dojo.lfx.Combine(_402);
};
dojo.lfx.chain=function(_403){
var _404=arguments;
if(dojo.lang.isArray(arguments[0])){
_404=arguments[0];
}
if(_404.length==1){
return _404[0];
}
return new dojo.lfx.Chain(_404);
};
dojo.provide("dojo.html.common");
dojo.lang.mixin(dojo.html,dojo.dom);
dojo.html.body=function(){
dojo.deprecated("dojo.html.body() moved to dojo.body()","0.5");
return dojo.body();
};
dojo.html.getEventTarget=function(evt){
if(!evt){
evt=dojo.global().event||{};
}
var t=(evt.srcElement?evt.srcElement:(evt.target?evt.target:null));
while((t)&&(t.nodeType!=1)){
t=t.parentNode;
}
return t;
};
dojo.html.getViewport=function(){
var _407=dojo.global();
var _408=dojo.doc();
var w=0;
var h=0;
if(dojo.render.html.mozilla){
w=_408.documentElement.clientWidth;
h=_407.innerHeight;
}else{
if(!dojo.render.html.opera&&_407.innerWidth){
w=_407.innerWidth;
h=_407.innerHeight;
}else{
if(!dojo.render.html.opera&&dojo.exists(_408,"documentElement.clientWidth")){
var w2=_408.documentElement.clientWidth;
if(!w||w2&&w2<w){
w=w2;
}
h=_408.documentElement.clientHeight;
}else{
if(dojo.body().clientWidth){
w=dojo.body().clientWidth;
h=dojo.body().clientHeight;
}
}
}
}
return {width:w,height:h};
};
dojo.html.getScroll=function(){
var _40c=dojo.global();
var _40d=dojo.doc();
var top=_40c.pageYOffset||_40d.documentElement.scrollTop||dojo.body().scrollTop||0;
var left=_40c.pageXOffset||_40d.documentElement.scrollLeft||dojo.body().scrollLeft||0;
return {top:top,left:left,offset:{x:left,y:top}};
};
dojo.html.getParentByType=function(node,type){
var _412=dojo.doc();
var _413=dojo.byId(node);
type=type.toLowerCase();
while((_413)&&(_413.nodeName.toLowerCase()!=type)){
if(_413==(_412["body"]||_412["documentElement"])){
return null;
}
_413=_413.parentNode;
}
return _413;
};
dojo.html.getAttribute=function(node,attr){
node=dojo.byId(node);
if((!node)||(!node.getAttribute)){
return null;
}
var ta=typeof attr=="string"?attr:new String(attr);
var v=node.getAttribute(ta.toUpperCase());
if((v)&&(typeof v=="string")&&(v!="")){
return v;
}
if(v&&v.value){
return v.value;
}
if((node.getAttributeNode)&&(node.getAttributeNode(ta))){
return (node.getAttributeNode(ta)).value;
}else{
if(node.getAttribute(ta)){
return node.getAttribute(ta);
}else{
if(node.getAttribute(ta.toLowerCase())){
return node.getAttribute(ta.toLowerCase());
}
}
}
return null;
};
dojo.html.hasAttribute=function(node,attr){
return dojo.html.getAttribute(dojo.byId(node),attr)?true:false;
};
dojo.html.getCursorPosition=function(e){
e=e||dojo.global().event;
var _41b={x:0,y:0};
if(e.pageX||e.pageY){
_41b.x=e.pageX;
_41b.y=e.pageY;
}else{
var de=dojo.doc().documentElement;
var db=dojo.body();
_41b.x=e.clientX+((de||db)["scrollLeft"])-((de||db)["clientLeft"]);
_41b.y=e.clientY+((de||db)["scrollTop"])-((de||db)["clientTop"]);
}
return _41b;
};
dojo.html.isTag=function(node){
node=dojo.byId(node);
if(node&&node.tagName){
for(var i=1;i<arguments.length;i++){
if(node.tagName.toLowerCase()==String(arguments[i]).toLowerCase()){
return String(arguments[i]).toLowerCase();
}
}
}
return "";
};
if(dojo.render.html.ie&&!dojo.render.html.ie70){
if(window.location.href.substr(0,6).toLowerCase()!="https:"){
(function(){
var _420=dojo.doc().createElement("script");
_420.src="javascript:'dojo.html.createExternalElement=function(doc, tag){ return doc.createElement(tag); }'";
dojo.doc().getElementsByTagName("head")[0].appendChild(_420);
})();
}
}else{
dojo.html.createExternalElement=function(doc,tag){
return doc.createElement(tag);
};
}
dojo.html._callDeprecated=function(_423,_424,args,_426,_427){
dojo.deprecated("dojo.html."+_423,"replaced by dojo.html."+_424+"("+(_426?"node, {"+_426+": "+_426+"}":"")+")"+(_427?"."+_427:""),"0.5");
var _428=[];
if(_426){
var _429={};
_429[_426]=args[1];
_428.push(args[0]);
_428.push(_429);
}else{
_428=args;
}
var ret=dojo.html[_424].apply(dojo.html,args);
if(_427){
return ret[_427];
}else{
return ret;
}
};
dojo.html.getViewportWidth=function(){
return dojo.html._callDeprecated("getViewportWidth","getViewport",arguments,null,"width");
};
dojo.html.getViewportHeight=function(){
return dojo.html._callDeprecated("getViewportHeight","getViewport",arguments,null,"height");
};
dojo.html.getViewportSize=function(){
return dojo.html._callDeprecated("getViewportSize","getViewport",arguments);
};
dojo.html.getScrollTop=function(){
return dojo.html._callDeprecated("getScrollTop","getScroll",arguments,null,"top");
};
dojo.html.getScrollLeft=function(){
return dojo.html._callDeprecated("getScrollLeft","getScroll",arguments,null,"left");
};
dojo.html.getScrollOffset=function(){
return dojo.html._callDeprecated("getScrollOffset","getScroll",arguments,null,"offset");
};
dojo.provide("dojo.uri.Uri");
dojo.uri=new function(){
this.dojoUri=function(uri){
return new dojo.uri.Uri(dojo.hostenv.getBaseScriptUri(),uri);
};
this.moduleUri=function(_42c,uri){
var loc=dojo.hostenv.getModuleSymbols(_42c).join("/");
if(!loc){
return null;
}
if(loc.lastIndexOf("/")!=loc.length-1){
loc+="/";
}
return new dojo.uri.Uri(dojo.hostenv.getBaseScriptUri()+loc,uri);
};
this.Uri=function(){
var uri=arguments[0];
for(var i=1;i<arguments.length;i++){
if(!arguments[i]){
continue;
}
var _431=new dojo.uri.Uri(arguments[i].toString());
var _432=new dojo.uri.Uri(uri.toString());
if((_431.path=="")&&(_431.scheme==null)&&(_431.authority==null)&&(_431.query==null)){
if(_431.fragment!=null){
_432.fragment=_431.fragment;
}
_431=_432;
}else{
if(_431.scheme==null){
_431.scheme=_432.scheme;
if(_431.authority==null){
_431.authority=_432.authority;
if(_431.path.charAt(0)!="/"){
var path=_432.path.substring(0,_432.path.lastIndexOf("/")+1)+_431.path;
var segs=path.split("/");
for(var j=0;j<segs.length;j++){
if(segs[j]=="."){
if(j==segs.length-1){
segs[j]="";
}else{
segs.splice(j,1);
j--;
}
}else{
if(j>0&&!(j==1&&segs[0]=="")&&segs[j]==".."&&segs[j-1]!=".."){
if(j==segs.length-1){
segs.splice(j,1);
segs[j-1]="";
}else{
segs.splice(j-1,2);
j-=2;
}
}
}
}
_431.path=segs.join("/");
}
}
}
}
uri="";
if(_431.scheme!=null){
uri+=_431.scheme+":";
}
if(_431.authority!=null){
uri+="//"+_431.authority;
}
uri+=_431.path;
if(_431.query!=null){
uri+="?"+_431.query;
}
if(_431.fragment!=null){
uri+="#"+_431.fragment;
}
}
this.uri=uri.toString();
var _436="^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?$";
var r=this.uri.match(new RegExp(_436));
this.scheme=r[2]||(r[1]?"":null);
this.authority=r[4]||(r[3]?"":null);
this.path=r[5];
this.query=r[7]||(r[6]?"":null);
this.fragment=r[9]||(r[8]?"":null);
if(this.authority!=null){
_436="^((([^:]+:)?([^@]+))@)?([^:]*)(:([0-9]+))?$";
r=this.authority.match(new RegExp(_436));
this.user=r[3]||null;
this.password=r[4]||null;
this.host=r[5];
this.port=r[7]||null;
}
this.toString=function(){
return this.uri;
};
};
};
dojo.provide("dojo.html.style");
dojo.html.getClass=function(node){
node=dojo.byId(node);
if(!node){
return "";
}
var cs="";
if(node.className){
cs=node.className;
}else{
if(dojo.html.hasAttribute(node,"class")){
cs=dojo.html.getAttribute(node,"class");
}
}
return cs.replace(/^\s+|\s+$/g,"");
};
dojo.html.getClasses=function(node){
var c=dojo.html.getClass(node);
return (c=="")?[]:c.split(/\s+/g);
};
dojo.html.hasClass=function(node,_43d){
return (new RegExp("(^|\\s+)"+_43d+"(\\s+|$)")).test(dojo.html.getClass(node));
};
dojo.html.prependClass=function(node,_43f){
_43f+=" "+dojo.html.getClass(node);
return dojo.html.setClass(node,_43f);
};
dojo.html.addClass=function(node,_441){
if(dojo.html.hasClass(node,_441)){
return false;
}
_441=(dojo.html.getClass(node)+" "+_441).replace(/^\s+|\s+$/g,"");
return dojo.html.setClass(node,_441);
};
dojo.html.setClass=function(node,_443){
node=dojo.byId(node);
var cs=new String(_443);
try{
if(typeof node.className=="string"){
node.className=cs;
}else{
if(node.setAttribute){
node.setAttribute("class",_443);
node.className=cs;
}else{
return false;
}
}
}
catch(e){
dojo.debug("dojo.html.setClass() failed",e);
}
return true;
};
dojo.html.removeClass=function(node,_446,_447){
try{
if(!_447){
var _448=dojo.html.getClass(node).replace(new RegExp("(^|\\s+)"+_446+"(\\s+|$)"),"$1$2");
}else{
var _448=dojo.html.getClass(node).replace(_446,"");
}
dojo.html.setClass(node,_448);
}
catch(e){
dojo.debug("dojo.html.removeClass() failed",e);
}
return true;
};
dojo.html.replaceClass=function(node,_44a,_44b){
dojo.html.removeClass(node,_44b);
dojo.html.addClass(node,_44a);
};
dojo.html.classMatchType={ContainsAll:0,ContainsAny:1,IsOnly:2};
dojo.html.getElementsByClass=function(_44c,_44d,_44e,_44f,_450){
_450=false;
var _451=dojo.doc();
_44d=dojo.byId(_44d)||_451;
var _452=_44c.split(/\s+/g);
var _453=[];
if(_44f!=1&&_44f!=2){
_44f=0;
}
var _454=new RegExp("(\\s|^)(("+_452.join(")|(")+"))(\\s|$)");
var _455=_452.join(" ").length;
var _456=[];
if(!_450&&_451.evaluate){
var _457=".//"+(_44e||"*")+"[contains(";
if(_44f!=dojo.html.classMatchType.ContainsAny){
_457+="concat(' ',@class,' '), ' "+_452.join(" ') and contains(concat(' ',@class,' '), ' ")+" ')";
if(_44f==2){
_457+=" and string-length(@class)="+_455+"]";
}else{
_457+="]";
}
}else{
_457+="concat(' ',@class,' '), ' "+_452.join(" ') or contains(concat(' ',@class,' '), ' ")+" ')]";
}
var _458=_451.evaluate(_457,_44d,null,XPathResult.ANY_TYPE,null);
var _459=_458.iterateNext();
while(_459){
try{
_456.push(_459);
_459=_458.iterateNext();
}
catch(e){
break;
}
}
return _456;
}else{
if(!_44e){
_44e="*";
}
_456=_44d.getElementsByTagName(_44e);
var node,i=0;
outer:
while(node=_456[i++]){
var _45c=dojo.html.getClasses(node);
if(_45c.length==0){
continue outer;
}
var _45d=0;
for(var j=0;j<_45c.length;j++){
if(_454.test(_45c[j])){
if(_44f==dojo.html.classMatchType.ContainsAny){
_453.push(node);
continue outer;
}else{
_45d++;
}
}else{
if(_44f==dojo.html.classMatchType.IsOnly){
continue outer;
}
}
}
if(_45d==_452.length){
if((_44f==dojo.html.classMatchType.IsOnly)&&(_45d==_45c.length)){
_453.push(node);
}else{
if(_44f==dojo.html.classMatchType.ContainsAll){
_453.push(node);
}
}
}
}
return _453;
}
};
dojo.html.getElementsByClassName=dojo.html.getElementsByClass;
dojo.html.toCamelCase=function(_45f){
var arr=_45f.split("-"),cc=arr[0];
for(var i=1;i<arr.length;i++){
cc+=arr[i].charAt(0).toUpperCase()+arr[i].substring(1);
}
return cc;
};
dojo.html.toSelectorCase=function(_463){
return _463.replace(/([A-Z])/g,"-$1").toLowerCase();
};
dojo.html.getComputedStyle=function(node,_465,_466){
node=dojo.byId(node);
var _465=dojo.html.toSelectorCase(_465);
var _467=dojo.html.toCamelCase(_465);
if(!node||!node.style){
return _466;
}else{
if(document.defaultView&&dojo.html.isDescendantOf(node,node.ownerDocument)){
try{
var cs=document.defaultView.getComputedStyle(node,"");
if(cs){
return cs.getPropertyValue(_465);
}
}
catch(e){
if(node.style.getPropertyValue){
return node.style.getPropertyValue(_465);
}else{
return _466;
}
}
}else{
if(node.currentStyle){
return node.currentStyle[_467];
}
}
}
if(node.style.getPropertyValue){
return node.style.getPropertyValue(_465);
}else{
return _466;
}
};
dojo.html.getStyleProperty=function(node,_46a){
node=dojo.byId(node);
return (node&&node.style?node.style[dojo.html.toCamelCase(_46a)]:undefined);
};
dojo.html.getStyle=function(node,_46c){
var _46d=dojo.html.getStyleProperty(node,_46c);
return (_46d?_46d:dojo.html.getComputedStyle(node,_46c));
};
dojo.html.setStyle=function(node,_46f,_470){
node=dojo.byId(node);
if(node&&node.style){
var _471=dojo.html.toCamelCase(_46f);
node.style[_471]=_470;
}
};
dojo.html.setStyleText=function(_472,text){
try{
_472.style.cssText=text;
}
catch(e){
_472.setAttribute("style",text);
}
};
dojo.html.copyStyle=function(_474,_475){
if(!_475.style.cssText){
_474.setAttribute("style",_475.getAttribute("style"));
}else{
_474.style.cssText=_475.style.cssText;
}
dojo.html.addClass(_474,dojo.html.getClass(_475));
};
dojo.html.getUnitValue=function(node,_477,_478){
var s=dojo.html.getComputedStyle(node,_477);
if((!s)||((s=="auto")&&(_478))){
return {value:0,units:"px"};
}
var _47a=s.match(/(\-?[\d.]+)([a-z%]*)/i);
if(!_47a){
return dojo.html.getUnitValue.bad;
}
return {value:Number(_47a[1]),units:_47a[2].toLowerCase()};
};
dojo.html.getUnitValue.bad={value:NaN,units:""};
dojo.html.getPixelValue=function(node,_47c,_47d){
var _47e=dojo.html.getUnitValue(node,_47c,_47d);
if(isNaN(_47e.value)){
return 0;
}
if((_47e.value)&&(_47e.units!="px")){
return NaN;
}
return _47e.value;
};
dojo.html.setPositivePixelValue=function(node,_480,_481){
if(isNaN(_481)){
return false;
}
node.style[_480]=Math.max(0,_481)+"px";
return true;
};
dojo.html.styleSheet=null;
dojo.html.insertCssRule=function(_482,_483,_484){
if(!dojo.html.styleSheet){
if(document.createStyleSheet){
dojo.html.styleSheet=document.createStyleSheet();
}else{
if(document.styleSheets[0]){
dojo.html.styleSheet=document.styleSheets[0];
}else{
return null;
}
}
}
if(arguments.length<3){
if(dojo.html.styleSheet.cssRules){
_484=dojo.html.styleSheet.cssRules.length;
}else{
if(dojo.html.styleSheet.rules){
_484=dojo.html.styleSheet.rules.length;
}else{
return null;
}
}
}
if(dojo.html.styleSheet.insertRule){
var rule=_482+" { "+_483+" }";
return dojo.html.styleSheet.insertRule(rule,_484);
}else{
if(dojo.html.styleSheet.addRule){
return dojo.html.styleSheet.addRule(_482,_483,_484);
}else{
return null;
}
}
};
dojo.html.removeCssRule=function(_486){
if(!dojo.html.styleSheet){
dojo.debug("no stylesheet defined for removing rules");
return false;
}
if(dojo.render.html.ie){
if(!_486){
_486=dojo.html.styleSheet.rules.length;
dojo.html.styleSheet.removeRule(_486);
}
}else{
if(document.styleSheets[0]){
if(!_486){
_486=dojo.html.styleSheet.cssRules.length;
}
dojo.html.styleSheet.deleteRule(_486);
}
}
return true;
};
dojo.html._insertedCssFiles=[];
dojo.html.insertCssFile=function(URI,doc,_489,_48a){
if(!URI){
return;
}
if(!doc){
doc=document;
}
var _48b=dojo.hostenv.getText(URI,false,_48a);
if(_48b===null){
return;
}
_48b=dojo.html.fixPathsInCssText(_48b,URI);
if(_489){
var idx=-1,node,ent=dojo.html._insertedCssFiles;
for(var i=0;i<ent.length;i++){
if((ent[i].doc==doc)&&(ent[i].cssText==_48b)){
idx=i;
node=ent[i].nodeRef;
break;
}
}
if(node){
var _490=doc.getElementsByTagName("style");
for(var i=0;i<_490.length;i++){
if(_490[i]==node){
return;
}
}
dojo.html._insertedCssFiles.shift(idx,1);
}
}
var _491=dojo.html.insertCssText(_48b,doc);
dojo.html._insertedCssFiles.push({"doc":doc,"cssText":_48b,"nodeRef":_491});
if(_491&&djConfig.isDebug){
_491.setAttribute("dbgHref",URI);
}
return _491;
};
dojo.html.insertCssText=function(_492,doc,URI){
if(!_492){
return;
}
if(!doc){
doc=document;
}
if(URI){
_492=dojo.html.fixPathsInCssText(_492,URI);
}
var _495=doc.createElement("style");
_495.setAttribute("type","text/css");
var head=doc.getElementsByTagName("head")[0];
if(!head){
dojo.debug("No head tag in document, aborting styles");
return;
}else{
head.appendChild(_495);
}
if(_495.styleSheet){
var _497=function(){
try{
_495.styleSheet.cssText=_492;
}
catch(e){
dojo.debug(e);
}
};
if(_495.styleSheet.disabled){
setTimeout(_497,10);
}else{
_497();
}
}else{
var _498=doc.createTextNode(_492);
_495.appendChild(_498);
}
return _495;
};
dojo.html.fixPathsInCssText=function(_499,URI){
if(!_499||!URI){
return;
}
var _49b,str="",url="",_49e="[\\t\\s\\w\\(\\)\\/\\.\\\\'\"-:#=&?~]+";
var _49f=new RegExp("url\\(\\s*("+_49e+")\\s*\\)");
var _4a0=/(file|https?|ftps?):\/\//;
regexTrim=new RegExp("^[\\s]*(['\"]?)("+_49e+")\\1[\\s]*?$");
if(dojo.render.html.ie55||dojo.render.html.ie60){
var _4a1=new RegExp("AlphaImageLoader\\((.*)src=['\"]("+_49e+")['\"]");
while(_49b=_4a1.exec(_499)){
url=_49b[2].replace(regexTrim,"$2");
if(!_4a0.exec(url)){
url=(new dojo.uri.Uri(URI,url).toString());
}
str+=_499.substring(0,_49b.index)+"AlphaImageLoader("+_49b[1]+"src='"+url+"'";
_499=_499.substr(_49b.index+_49b[0].length);
}
_499=str+_499;
str="";
}
while(_49b=_49f.exec(_499)){
url=_49b[1].replace(regexTrim,"$2");
if(!_4a0.exec(url)){
url=(new dojo.uri.Uri(URI,url).toString());
}
str+=_499.substring(0,_49b.index)+"url("+url+")";
_499=_499.substr(_49b.index+_49b[0].length);
}
return str+_499;
};
dojo.html.setActiveStyleSheet=function(_4a2){
var i=0,a,els=dojo.doc().getElementsByTagName("link");
while(a=els[i++]){
if(a.getAttribute("rel").indexOf("style")!=-1&&a.getAttribute("title")){
a.disabled=true;
if(a.getAttribute("title")==_4a2){
a.disabled=false;
}
}
}
};
dojo.html.getActiveStyleSheet=function(){
var i=0,a,els=dojo.doc().getElementsByTagName("link");
while(a=els[i++]){
if(a.getAttribute("rel").indexOf("style")!=-1&&a.getAttribute("title")&&!a.disabled){
return a.getAttribute("title");
}
}
return null;
};
dojo.html.getPreferredStyleSheet=function(){
var i=0,a,els=dojo.doc().getElementsByTagName("link");
while(a=els[i++]){
if(a.getAttribute("rel").indexOf("style")!=-1&&a.getAttribute("rel").indexOf("alt")==-1&&a.getAttribute("title")){
return a.getAttribute("title");
}
}
return null;
};
dojo.html.applyBrowserClass=function(node){
var drh=dojo.render.html;
var _4ae={dj_ie:drh.ie,dj_ie55:drh.ie55,dj_ie6:drh.ie60,dj_ie7:drh.ie70,dj_iequirks:drh.ie&&drh.quirks,dj_opera:drh.opera,dj_opera8:drh.opera&&(Math.floor(dojo.render.version)==8),dj_opera9:drh.opera&&(Math.floor(dojo.render.version)==9),dj_khtml:drh.khtml,dj_safari:drh.safari,dj_gecko:drh.mozilla};
for(var p in _4ae){
if(_4ae[p]){
dojo.html.addClass(node,p);
}
}
};
dojo.provide("dojo.html.display");
dojo.html._toggle=function(node,_4b1,_4b2){
node=dojo.byId(node);
_4b2(node,!_4b1(node));
return _4b1(node);
};
dojo.html.show=function(node){
node=dojo.byId(node);
if(dojo.html.getStyleProperty(node,"display")=="none"){
dojo.html.setStyle(node,"display",(node.dojoDisplayCache||""));
node.dojoDisplayCache=undefined;
}
};
dojo.html.hide=function(node){
node=dojo.byId(node);
if(typeof node["dojoDisplayCache"]=="undefined"){
var d=dojo.html.getStyleProperty(node,"display");
if(d!="none"){
node.dojoDisplayCache=d;
}
}
dojo.html.setStyle(node,"display","none");
};
dojo.html.setShowing=function(node,_4b7){
dojo.html[(_4b7?"show":"hide")](node);
};
dojo.html.isShowing=function(node){
return (dojo.html.getStyleProperty(node,"display")!="none");
};
dojo.html.toggleShowing=function(node){
return dojo.html._toggle(node,dojo.html.isShowing,dojo.html.setShowing);
};
dojo.html.displayMap={tr:"",td:"",th:"",img:"inline",span:"inline",input:"inline",button:"inline"};
dojo.html.suggestDisplayByTagName=function(node){
node=dojo.byId(node);
if(node&&node.tagName){
var tag=node.tagName.toLowerCase();
return (tag in dojo.html.displayMap?dojo.html.displayMap[tag]:"block");
}
};
dojo.html.setDisplay=function(node,_4bd){
dojo.html.setStyle(node,"display",((_4bd instanceof String||typeof _4bd=="string")?_4bd:(_4bd?dojo.html.suggestDisplayByTagName(node):"none")));
};
dojo.html.isDisplayed=function(node){
return (dojo.html.getComputedStyle(node,"display")!="none");
};
dojo.html.toggleDisplay=function(node){
return dojo.html._toggle(node,dojo.html.isDisplayed,dojo.html.setDisplay);
};
dojo.html.setVisibility=function(node,_4c1){
dojo.html.setStyle(node,"visibility",((_4c1 instanceof String||typeof _4c1=="string")?_4c1:(_4c1?"visible":"hidden")));
};
dojo.html.isVisible=function(node){
return (dojo.html.getComputedStyle(node,"visibility")!="hidden");
};
dojo.html.toggleVisibility=function(node){
return dojo.html._toggle(node,dojo.html.isVisible,dojo.html.setVisibility);
};
dojo.html.setOpacity=function(node,_4c5,_4c6){
node=dojo.byId(node);
var h=dojo.render.html;
if(!_4c6){
if(_4c5>=1){
if(h.ie){
dojo.html.clearOpacity(node);
return;
}else{
_4c5=0.999999;
}
}else{
if(_4c5<0){
_4c5=0;
}
}
}
if(h.ie){
if(node.nodeName.toLowerCase()=="tr"){
var tds=node.getElementsByTagName("td");
for(var x=0;x<tds.length;x++){
tds[x].style.filter="Alpha(Opacity="+_4c5*100+")";
}
}
node.style.filter="Alpha(Opacity="+_4c5*100+")";
}else{
if(h.moz){
node.style.opacity=_4c5;
node.style.MozOpacity=_4c5;
}else{
if(h.safari){
node.style.opacity=_4c5;
node.style.KhtmlOpacity=_4c5;
}else{
node.style.opacity=_4c5;
}
}
}
};
dojo.html.clearOpacity=function(node){
node=dojo.byId(node);
var ns=node.style;
var h=dojo.render.html;
if(h.ie){
try{
if(node.filters&&node.filters.alpha){
ns.filter="";
}
}
catch(e){
}
}else{
if(h.moz){
ns.opacity=1;
ns.MozOpacity=1;
}else{
if(h.safari){
ns.opacity=1;
ns.KhtmlOpacity=1;
}else{
ns.opacity=1;
}
}
}
};
dojo.html.getOpacity=function(node){
node=dojo.byId(node);
var h=dojo.render.html;
if(h.ie){
var opac=(node.filters&&node.filters.alpha&&typeof node.filters.alpha.opacity=="number"?node.filters.alpha.opacity:100)/100;
}else{
var opac=node.style.opacity||node.style.MozOpacity||node.style.KhtmlOpacity||1;
}
return opac>=0.999999?1:Number(opac);
};
dojo.provide("dojo.html.color");
dojo.html.getBackgroundColor=function(node){
node=dojo.byId(node);
var _4d1;
do{
_4d1=dojo.html.getStyle(node,"background-color");
if(_4d1.toLowerCase()=="rgba(0, 0, 0, 0)"){
_4d1="transparent";
}
if(node==document.getElementsByTagName("body")[0]){
node=null;
break;
}
node=node.parentNode;
}while(node&&dojo.lang.inArray(["transparent",""],_4d1));
if(_4d1=="transparent"){
_4d1=[255,255,255,0];
}else{
_4d1=dojo.gfx.color.extractRGB(_4d1);
}
return _4d1;
};
dojo.provide("dojo.html.layout");
dojo.html.sumAncestorProperties=function(node,prop){
node=dojo.byId(node);
if(!node){
return 0;
}
var _4d4=0;
while(node){
if(dojo.html.getComputedStyle(node,"position")=="fixed"){
return 0;
}
var val=node[prop];
if(val){
_4d4+=val-0;
if(node==dojo.body()){
break;
}
}
node=node.parentNode;
}
return _4d4;
};
dojo.html.setStyleAttributes=function(node,_4d7){
node=dojo.byId(node);
var _4d8=_4d7.replace(/(;)?\s*$/,"").split(";");
for(var i=0;i<_4d8.length;i++){
var _4da=_4d8[i].split(":");
var name=_4da[0].replace(/\s*$/,"").replace(/^\s*/,"").toLowerCase();
var _4dc=_4da[1].replace(/\s*$/,"").replace(/^\s*/,"");
switch(name){
case "opacity":
dojo.html.setOpacity(node,_4dc);
break;
case "content-height":
dojo.html.setContentBox(node,{height:_4dc});
break;
case "content-width":
dojo.html.setContentBox(node,{width:_4dc});
break;
case "outer-height":
dojo.html.setMarginBox(node,{height:_4dc});
break;
case "outer-width":
dojo.html.setMarginBox(node,{width:_4dc});
break;
default:
node.style[dojo.html.toCamelCase(name)]=_4dc;
}
}
};
dojo.html.boxSizing={MARGIN_BOX:"margin-box",BORDER_BOX:"border-box",PADDING_BOX:"padding-box",CONTENT_BOX:"content-box"};
dojo.html.getAbsolutePosition=dojo.html.abs=function(node,_4de,_4df){
node=dojo.byId(node,node.ownerDocument);
var ret={x:0,y:0};
var bs=dojo.html.boxSizing;
if(!_4df){
_4df=bs.CONTENT_BOX;
}
var _4e2=2;
var _4e3;
switch(_4df){
case bs.MARGIN_BOX:
_4e3=3;
break;
case bs.BORDER_BOX:
_4e3=2;
break;
case bs.PADDING_BOX:
default:
_4e3=1;
break;
case bs.CONTENT_BOX:
_4e3=0;
break;
}
var h=dojo.render.html;
var db=document["body"]||document["documentElement"];
if(h.ie){
with(node.getBoundingClientRect()){
ret.x=left-2;
ret.y=top-2;
}
}else{
if(document.getBoxObjectFor){
_4e2=1;
try{
var bo=document.getBoxObjectFor(node);
ret.x=bo.x-dojo.html.sumAncestorProperties(node,"scrollLeft");
ret.y=bo.y-dojo.html.sumAncestorProperties(node,"scrollTop");
}
catch(e){
}
}else{
if(node["offsetParent"]){
var _4e7;
if((h.safari)&&(node.style.getPropertyValue("position")=="absolute")&&(node.parentNode==db)){
_4e7=db;
}else{
_4e7=db.parentNode;
}
if(node.parentNode!=db){
var nd=node;
if(dojo.render.html.opera){
nd=db;
}
ret.x-=dojo.html.sumAncestorProperties(nd,"scrollLeft");
ret.y-=dojo.html.sumAncestorProperties(nd,"scrollTop");
}
var _4e9=node;
do{
var n=_4e9["offsetLeft"];
if(!h.opera||n>0){
ret.x+=isNaN(n)?0:n;
}
var m=_4e9["offsetTop"];
ret.y+=isNaN(m)?0:m;
_4e9=_4e9.offsetParent;
}while((_4e9!=_4e7)&&(_4e9!=null));
}else{
if(node["x"]&&node["y"]){
ret.x+=isNaN(node.x)?0:node.x;
ret.y+=isNaN(node.y)?0:node.y;
}
}
}
}
if(_4de){
var _4ec=dojo.html.getScroll();
ret.y+=_4ec.top;
ret.x+=_4ec.left;
}
var _4ed=[dojo.html.getPaddingExtent,dojo.html.getBorderExtent,dojo.html.getMarginExtent];
if(_4e2>_4e3){
for(var i=_4e3;i<_4e2;++i){
ret.y+=_4ed[i](node,"top");
ret.x+=_4ed[i](node,"left");
}
}else{
if(_4e2<_4e3){
for(var i=_4e3;i>_4e2;--i){
ret.y-=_4ed[i-1](node,"top");
ret.x-=_4ed[i-1](node,"left");
}
}
}
ret.top=ret.y;
ret.left=ret.x;
return ret;
};
dojo.html.isPositionAbsolute=function(node){
return (dojo.html.getComputedStyle(node,"position")=="absolute");
};
dojo.html._sumPixelValues=function(node,_4f1,_4f2){
var _4f3=0;
for(var x=0;x<_4f1.length;x++){
_4f3+=dojo.html.getPixelValue(node,_4f1[x],_4f2);
}
return _4f3;
};
dojo.html.getMargin=function(node){
return {width:dojo.html._sumPixelValues(node,["margin-left","margin-right"],(dojo.html.getComputedStyle(node,"position")=="absolute")),height:dojo.html._sumPixelValues(node,["margin-top","margin-bottom"],(dojo.html.getComputedStyle(node,"position")=="absolute"))};
};
dojo.html.getBorder=function(node){
return {width:dojo.html.getBorderExtent(node,"left")+dojo.html.getBorderExtent(node,"right"),height:dojo.html.getBorderExtent(node,"top")+dojo.html.getBorderExtent(node,"bottom")};
};
dojo.html.getBorderExtent=function(node,side){
return (dojo.html.getStyle(node,"border-"+side+"-style")=="none"?0:dojo.html.getPixelValue(node,"border-"+side+"-width"));
};
dojo.html.getMarginExtent=function(node,side){
return dojo.html._sumPixelValues(node,["margin-"+side],dojo.html.isPositionAbsolute(node));
};
dojo.html.getPaddingExtent=function(node,side){
return dojo.html._sumPixelValues(node,["padding-"+side],true);
};
dojo.html.getPadding=function(node){
return {width:dojo.html._sumPixelValues(node,["padding-left","padding-right"],true),height:dojo.html._sumPixelValues(node,["padding-top","padding-bottom"],true)};
};
dojo.html.getPadBorder=function(node){
var pad=dojo.html.getPadding(node);
var _500=dojo.html.getBorder(node);
return {width:pad.width+_500.width,height:pad.height+_500.height};
};
dojo.html.getBoxSizing=function(node){
var h=dojo.render.html;
var bs=dojo.html.boxSizing;
if(((h.ie)||(h.opera))&&node.nodeName!="IMG"){
var cm=document["compatMode"];
if((cm=="BackCompat")||(cm=="QuirksMode")){
return bs.BORDER_BOX;
}else{
return bs.CONTENT_BOX;
}
}else{
if(arguments.length==0){
node=document.documentElement;
}
var _505=dojo.html.getStyle(node,"-moz-box-sizing");
if(!_505){
_505=dojo.html.getStyle(node,"box-sizing");
}
return (_505?_505:bs.CONTENT_BOX);
}
};
dojo.html.isBorderBox=function(node){
return (dojo.html.getBoxSizing(node)==dojo.html.boxSizing.BORDER_BOX);
};
dojo.html.getBorderBox=function(node){
node=dojo.byId(node);
return {width:node.offsetWidth,height:node.offsetHeight};
};
dojo.html.getPaddingBox=function(node){
var box=dojo.html.getBorderBox(node);
var _50a=dojo.html.getBorder(node);
return {width:box.width-_50a.width,height:box.height-_50a.height};
};
dojo.html.getContentBox=function(node){
node=dojo.byId(node);
var _50c=dojo.html.getPadBorder(node);
return {width:node.offsetWidth-_50c.width,height:node.offsetHeight-_50c.height};
};
dojo.html.setContentBox=function(node,args){
node=dojo.byId(node);
var _50f=0;
var _510=0;
var isbb=dojo.html.isBorderBox(node);
var _512=(isbb?dojo.html.getPadBorder(node):{width:0,height:0});
var ret={};
if(typeof args.width!="undefined"){
_50f=args.width+_512.width;
ret.width=dojo.html.setPositivePixelValue(node,"width",_50f);
}
if(typeof args.height!="undefined"){
_510=args.height+_512.height;
ret.height=dojo.html.setPositivePixelValue(node,"height",_510);
}
return ret;
};
dojo.html.getMarginBox=function(node){
var _515=dojo.html.getBorderBox(node);
var _516=dojo.html.getMargin(node);
return {width:_515.width+_516.width,height:_515.height+_516.height};
};
dojo.html.setMarginBox=function(node,args){
node=dojo.byId(node);
var _519=0;
var _51a=0;
var isbb=dojo.html.isBorderBox(node);
var _51c=(!isbb?dojo.html.getPadBorder(node):{width:0,height:0});
var _51d=dojo.html.getMargin(node);
var ret={};
if(typeof args.width!="undefined"){
_519=args.width-_51c.width;
_519-=_51d.width;
ret.width=dojo.html.setPositivePixelValue(node,"width",_519);
}
if(typeof args.height!="undefined"){
_51a=args.height-_51c.height;
_51a-=_51d.height;
ret.height=dojo.html.setPositivePixelValue(node,"height",_51a);
}
return ret;
};
dojo.html.getElementBox=function(node,type){
var bs=dojo.html.boxSizing;
switch(type){
case bs.MARGIN_BOX:
return dojo.html.getMarginBox(node);
case bs.BORDER_BOX:
return dojo.html.getBorderBox(node);
case bs.PADDING_BOX:
return dojo.html.getPaddingBox(node);
case bs.CONTENT_BOX:
default:
return dojo.html.getContentBox(node);
}
};
dojo.html.toCoordinateObject=dojo.html.toCoordinateArray=function(_522,_523,_524){
if(_522 instanceof Array||typeof _522=="array"){
dojo.deprecated("dojo.html.toCoordinateArray","use dojo.html.toCoordinateObject({left: , top: , width: , height: }) instead","0.5");
while(_522.length<4){
_522.push(0);
}
while(_522.length>4){
_522.pop();
}
var ret={left:_522[0],top:_522[1],width:_522[2],height:_522[3]};
}else{
if(!_522.nodeType&&!(_522 instanceof String||typeof _522=="string")&&("width" in _522||"height" in _522||"left" in _522||"x" in _522||"top" in _522||"y" in _522)){
var ret={left:_522.left||_522.x||0,top:_522.top||_522.y||0,width:_522.width||0,height:_522.height||0};
}else{
var node=dojo.byId(_522);
var pos=dojo.html.abs(node,_523,_524);
var _528=dojo.html.getMarginBox(node);
var ret={left:pos.left,top:pos.top,width:_528.width,height:_528.height};
}
}
ret.x=ret.left;
ret.y=ret.top;
return ret;
};
dojo.html.setMarginBoxWidth=dojo.html.setOuterWidth=function(node,_52a){
return dojo.html._callDeprecated("setMarginBoxWidth","setMarginBox",arguments,"width");
};
dojo.html.setMarginBoxHeight=dojo.html.setOuterHeight=function(){
return dojo.html._callDeprecated("setMarginBoxHeight","setMarginBox",arguments,"height");
};
dojo.html.getMarginBoxWidth=dojo.html.getOuterWidth=function(){
return dojo.html._callDeprecated("getMarginBoxWidth","getMarginBox",arguments,null,"width");
};
dojo.html.getMarginBoxHeight=dojo.html.getOuterHeight=function(){
return dojo.html._callDeprecated("getMarginBoxHeight","getMarginBox",arguments,null,"height");
};
dojo.html.getTotalOffset=function(node,type,_52d){
return dojo.html._callDeprecated("getTotalOffset","getAbsolutePosition",arguments,null,type);
};
dojo.html.getAbsoluteX=function(node,_52f){
return dojo.html._callDeprecated("getAbsoluteX","getAbsolutePosition",arguments,null,"x");
};
dojo.html.getAbsoluteY=function(node,_531){
return dojo.html._callDeprecated("getAbsoluteY","getAbsolutePosition",arguments,null,"y");
};
dojo.html.totalOffsetLeft=function(node,_533){
return dojo.html._callDeprecated("totalOffsetLeft","getAbsolutePosition",arguments,null,"left");
};
dojo.html.totalOffsetTop=function(node,_535){
return dojo.html._callDeprecated("totalOffsetTop","getAbsolutePosition",arguments,null,"top");
};
dojo.html.getMarginWidth=function(node){
return dojo.html._callDeprecated("getMarginWidth","getMargin",arguments,null,"width");
};
dojo.html.getMarginHeight=function(node){
return dojo.html._callDeprecated("getMarginHeight","getMargin",arguments,null,"height");
};
dojo.html.getBorderWidth=function(node){
return dojo.html._callDeprecated("getBorderWidth","getBorder",arguments,null,"width");
};
dojo.html.getBorderHeight=function(node){
return dojo.html._callDeprecated("getBorderHeight","getBorder",arguments,null,"height");
};
dojo.html.getPaddingWidth=function(node){
return dojo.html._callDeprecated("getPaddingWidth","getPadding",arguments,null,"width");
};
dojo.html.getPaddingHeight=function(node){
return dojo.html._callDeprecated("getPaddingHeight","getPadding",arguments,null,"height");
};
dojo.html.getPadBorderWidth=function(node){
return dojo.html._callDeprecated("getPadBorderWidth","getPadBorder",arguments,null,"width");
};
dojo.html.getPadBorderHeight=function(node){
return dojo.html._callDeprecated("getPadBorderHeight","getPadBorder",arguments,null,"height");
};
dojo.html.getBorderBoxWidth=dojo.html.getInnerWidth=function(){
return dojo.html._callDeprecated("getBorderBoxWidth","getBorderBox",arguments,null,"width");
};
dojo.html.getBorderBoxHeight=dojo.html.getInnerHeight=function(){
return dojo.html._callDeprecated("getBorderBoxHeight","getBorderBox",arguments,null,"height");
};
dojo.html.getContentBoxWidth=dojo.html.getContentWidth=function(){
return dojo.html._callDeprecated("getContentBoxWidth","getContentBox",arguments,null,"width");
};
dojo.html.getContentBoxHeight=dojo.html.getContentHeight=function(){
return dojo.html._callDeprecated("getContentBoxHeight","getContentBox",arguments,null,"height");
};
dojo.html.setContentBoxWidth=dojo.html.setContentWidth=function(node,_53f){
return dojo.html._callDeprecated("setContentBoxWidth","setContentBox",arguments,"width");
};
dojo.html.setContentBoxHeight=dojo.html.setContentHeight=function(node,_541){
return dojo.html._callDeprecated("setContentBoxHeight","setContentBox",arguments,"height");
};
dojo.provide("dojo.lfx.html");
dojo.lfx.html._byId=function(_542){
if(!_542){
return [];
}
if(dojo.lang.isArrayLike(_542)){
if(!_542.alreadyChecked){
var n=[];
dojo.lang.forEach(_542,function(node){
n.push(dojo.byId(node));
});
n.alreadyChecked=true;
return n;
}else{
return _542;
}
}else{
var n=[];
n.push(dojo.byId(_542));
n.alreadyChecked=true;
return n;
}
};
dojo.lfx.html.propertyAnimation=function(_545,_546,_547,_548,_549){
_545=dojo.lfx.html._byId(_545);
var _54a={"propertyMap":_546,"nodes":_545,"duration":_547,"easing":_548||dojo.lfx.easeDefault};
var _54b=function(args){
if(args.nodes.length==1){
var pm=args.propertyMap;
if(!dojo.lang.isArray(args.propertyMap)){
var parr=[];
for(var _54f in pm){
pm[_54f].property=_54f;
parr.push(pm[_54f]);
}
pm=args.propertyMap=parr;
}
dojo.lang.forEach(pm,function(prop){
if(dj_undef("start",prop)){
if(prop.property!="opacity"){
prop.start=parseInt(dojo.html.getComputedStyle(args.nodes[0],prop.property));
}else{
prop.start=dojo.html.getOpacity(args.nodes[0]);
}
}
});
}
};
var _551=function(_552){
var _553=[];
dojo.lang.forEach(_552,function(c){
_553.push(Math.round(c));
});
return _553;
};
var _555=function(n,_557){
n=dojo.byId(n);
if(!n||!n.style){
return;
}
for(var s in _557){
try{
if(s=="opacity"){
dojo.html.setOpacity(n,_557[s]);
}else{
n.style[s]=_557[s];
}
}
catch(e){
dojo.debug(e);
}
}
};
var _559=function(_55a){
this._properties=_55a;
this.diffs=new Array(_55a.length);
dojo.lang.forEach(_55a,function(prop,i){
if(dojo.lang.isFunction(prop.start)){
prop.start=prop.start(prop,i);
}
if(dojo.lang.isFunction(prop.end)){
prop.end=prop.end(prop,i);
}
if(dojo.lang.isArray(prop.start)){
this.diffs[i]=null;
}else{
if(prop.start instanceof dojo.gfx.color.Color){
prop.startRgb=prop.start.toRgb();
prop.endRgb=prop.end.toRgb();
}else{
this.diffs[i]=prop.end-prop.start;
}
}
},this);
this.getValue=function(n){
var ret={};
dojo.lang.forEach(this._properties,function(prop,i){
var _561=null;
if(dojo.lang.isArray(prop.start)){
}else{
if(prop.start instanceof dojo.gfx.color.Color){
_561=(prop.units||"rgb")+"(";
for(var j=0;j<prop.startRgb.length;j++){
_561+=Math.round(((prop.endRgb[j]-prop.startRgb[j])*n)+prop.startRgb[j])+(j<prop.startRgb.length-1?",":"");
}
_561+=")";
}else{
_561=((this.diffs[i])*n)+prop.start+(prop.property!="opacity"?prop.units||"px":"");
}
}
ret[dojo.html.toCamelCase(prop.property)]=_561;
},this);
return ret;
};
};
var anim=new dojo.lfx.Animation({beforeBegin:function(){
_54b(_54a);
anim.curve=new _559(_54a.propertyMap);
},onAnimate:function(_564){
dojo.lang.forEach(_54a.nodes,function(node){
_555(node,_564);
});
}},_54a.duration,null,_54a.easing);
if(_549){
for(var x in _549){
if(dojo.lang.isFunction(_549[x])){
anim.connect(x,anim,_549[x]);
}
}
}
return anim;
};
dojo.lfx.html._makeFadeable=function(_567){
var _568=function(node){
if(dojo.render.html.ie){
if((node.style.zoom.length==0)&&(dojo.html.getStyle(node,"zoom")=="normal")){
node.style.zoom="1";
}
if((node.style.width.length==0)&&(dojo.html.getStyle(node,"width")=="auto")){
node.style.width="auto";
}
}
};
if(dojo.lang.isArrayLike(_567)){
dojo.lang.forEach(_567,_568);
}else{
_568(_567);
}
};
dojo.lfx.html.fade=function(_56a,_56b,_56c,_56d,_56e){
_56a=dojo.lfx.html._byId(_56a);
var _56f={property:"opacity"};
if(!dj_undef("start",_56b)){
_56f.start=_56b.start;
}else{
_56f.start=function(){
return dojo.html.getOpacity(_56a[0]);
};
}
if(!dj_undef("end",_56b)){
_56f.end=_56b.end;
}else{
dojo.raise("dojo.lfx.html.fade needs an end value");
}
var anim=dojo.lfx.propertyAnimation(_56a,[_56f],_56c,_56d);
anim.connect("beforeBegin",function(){
dojo.lfx.html._makeFadeable(_56a);
});
if(_56e){
anim.connect("onEnd",function(){
_56e(_56a,anim);
});
}
return anim;
};
dojo.lfx.html.fadeIn=function(_571,_572,_573,_574){
return dojo.lfx.html.fade(_571,{end:1},_572,_573,_574);
};
dojo.lfx.html.fadeOut=function(_575,_576,_577,_578){
return dojo.lfx.html.fade(_575,{end:0},_576,_577,_578);
};
dojo.lfx.html.fadeShow=function(_579,_57a,_57b,_57c){
_579=dojo.lfx.html._byId(_579);
dojo.lang.forEach(_579,function(node){
dojo.html.setOpacity(node,0);
});
var anim=dojo.lfx.html.fadeIn(_579,_57a,_57b,_57c);
anim.connect("beforeBegin",function(){
if(dojo.lang.isArrayLike(_579)){
dojo.lang.forEach(_579,dojo.html.show);
}else{
dojo.html.show(_579);
}
});
return anim;
};
dojo.lfx.html.fadeHide=function(_57f,_580,_581,_582){
var anim=dojo.lfx.html.fadeOut(_57f,_580,_581,function(){
if(dojo.lang.isArrayLike(_57f)){
dojo.lang.forEach(_57f,dojo.html.hide);
}else{
dojo.html.hide(_57f);
}
if(_582){
_582(_57f,anim);
}
});
return anim;
};
dojo.lfx.html.wipeIn=function(_584,_585,_586,_587){
_584=dojo.lfx.html._byId(_584);
var _588=[];
dojo.lang.forEach(_584,function(node){
var _58a={};
var _58b,_58c,_58d;
with(node.style){
_58b=top;
_58c=left;
_58d=position;
top="-9999px";
left="-9999px";
position="absolute";
display="";
}
var _58e=dojo.html.getBorderBox(node).height;
with(node.style){
top=_58b;
left=_58c;
position=_58d;
display="none";
}
var anim=dojo.lfx.propertyAnimation(node,{"height":{start:1,end:function(){
return _58e;
}}},_585,_586);
anim.connect("beforeBegin",function(){
_58a.overflow=node.style.overflow;
_58a.height=node.style.height;
with(node.style){
overflow="hidden";
_58e="1px";
}
dojo.html.show(node);
});
anim.connect("onEnd",function(){
with(node.style){
overflow=_58a.overflow;
_58e=_58a.height;
}
if(_587){
_587(node,anim);
}
});
_588.push(anim);
});
return dojo.lfx.combine(_588);
};
dojo.lfx.html.wipeOut=function(_590,_591,_592,_593){
_590=dojo.lfx.html._byId(_590);
var _594=[];
dojo.lang.forEach(_590,function(node){
var _596={};
var anim=dojo.lfx.propertyAnimation(node,{"height":{start:function(){
return dojo.html.getContentBox(node).height;
},end:1}},_591,_592,{"beforeBegin":function(){
_596.overflow=node.style.overflow;
_596.height=node.style.height;
with(node.style){
overflow="hidden";
}
dojo.html.show(node);
},"onEnd":function(){
dojo.html.hide(node);
with(node.style){
overflow=_596.overflow;
height=_596.height;
}
if(_593){
_593(node,anim);
}
}});
_594.push(anim);
});
return dojo.lfx.combine(_594);
};
dojo.lfx.html.slideTo=function(_598,_599,_59a,_59b,_59c){
_598=dojo.lfx.html._byId(_598);
var _59d=[];
var _59e=dojo.html.getComputedStyle;
if(dojo.lang.isArray(_599)){
dojo.deprecated("dojo.lfx.html.slideTo(node, array)","use dojo.lfx.html.slideTo(node, {top: value, left: value});","0.5");
_599={top:_599[0],left:_599[1]};
}
dojo.lang.forEach(_598,function(node){
var top=null;
var left=null;
var init=(function(){
var _5a3=node;
return function(){
var pos=_59e(_5a3,"position");
top=(pos=="absolute"?node.offsetTop:parseInt(_59e(node,"top"))||0);
left=(pos=="absolute"?node.offsetLeft:parseInt(_59e(node,"left"))||0);
if(!dojo.lang.inArray(["absolute","relative"],pos)){
var ret=dojo.html.abs(_5a3,true);
dojo.html.setStyleAttributes(_5a3,"position:absolute;top:"+ret.y+"px;left:"+ret.x+"px;");
top=ret.y;
left=ret.x;
}
};
})();
init();
var anim=dojo.lfx.propertyAnimation(node,{"top":{start:top,end:(_599.top||0)},"left":{start:left,end:(_599.left||0)}},_59a,_59b,{"beforeBegin":init});
if(_59c){
anim.connect("onEnd",function(){
_59c(_598,anim);
});
}
_59d.push(anim);
});
return dojo.lfx.combine(_59d);
};
dojo.lfx.html.slideBy=function(_5a7,_5a8,_5a9,_5aa,_5ab){
_5a7=dojo.lfx.html._byId(_5a7);
var _5ac=[];
var _5ad=dojo.html.getComputedStyle;
if(dojo.lang.isArray(_5a8)){
dojo.deprecated("dojo.lfx.html.slideBy(node, array)","use dojo.lfx.html.slideBy(node, {top: value, left: value});","0.5");
_5a8={top:_5a8[0],left:_5a8[1]};
}
dojo.lang.forEach(_5a7,function(node){
var top=null;
var left=null;
var init=(function(){
var _5b2=node;
return function(){
var pos=_5ad(_5b2,"position");
top=(pos=="absolute"?node.offsetTop:parseInt(_5ad(node,"top"))||0);
left=(pos=="absolute"?node.offsetLeft:parseInt(_5ad(node,"left"))||0);
if(!dojo.lang.inArray(["absolute","relative"],pos)){
var ret=dojo.html.abs(_5b2,true);
dojo.html.setStyleAttributes(_5b2,"position:absolute;top:"+ret.y+"px;left:"+ret.x+"px;");
top=ret.y;
left=ret.x;
}
};
})();
init();
var anim=dojo.lfx.propertyAnimation(node,{"top":{start:top,end:top+(_5a8.top||0)},"left":{start:left,end:left+(_5a8.left||0)}},_5a9,_5aa).connect("beforeBegin",init);
if(_5ab){
anim.connect("onEnd",function(){
_5ab(_5a7,anim);
});
}
_5ac.push(anim);
});
return dojo.lfx.combine(_5ac);
};
dojo.lfx.html.explode=function(_5b6,_5b7,_5b8,_5b9,_5ba){
var h=dojo.html;
_5b6=dojo.byId(_5b6);
_5b7=dojo.byId(_5b7);
var _5bc=h.toCoordinateObject(_5b6,true);
var _5bd=document.createElement("div");
h.copyStyle(_5bd,_5b7);
if(_5b7.explodeClassName){
_5bd.className=_5b7.explodeClassName;
}
with(_5bd.style){
position="absolute";
display="none";
var _5be=h.getStyle(_5b6,"background-color");
backgroundColor=_5be?_5be.toLowerCase():"transparent";
backgroundColor=(backgroundColor=="transparent")?"rgb(221, 221, 221)":backgroundColor;
}
dojo.body().appendChild(_5bd);
with(_5b7.style){
visibility="hidden";
display="block";
}
var _5bf=h.toCoordinateObject(_5b7,true);
with(_5b7.style){
display="none";
visibility="visible";
}
var _5c0={opacity:{start:0.5,end:1}};
dojo.lang.forEach(["height","width","top","left"],function(type){
_5c0[type]={start:_5bc[type],end:_5bf[type]};
});
var anim=new dojo.lfx.propertyAnimation(_5bd,_5c0,_5b8,_5b9,{"beforeBegin":function(){
h.setDisplay(_5bd,"block");
},"onEnd":function(){
h.setDisplay(_5b7,"block");
_5bd.parentNode.removeChild(_5bd);
}});
if(_5ba){
anim.connect("onEnd",function(){
_5ba(_5b7,anim);
});
}
return anim;
};
dojo.lfx.html.implode=function(_5c3,end,_5c5,_5c6,_5c7){
var h=dojo.html;
_5c3=dojo.byId(_5c3);
end=dojo.byId(end);
var _5c9=dojo.html.toCoordinateObject(_5c3,true);
var _5ca=dojo.html.toCoordinateObject(end,true);
var _5cb=document.createElement("div");
dojo.html.copyStyle(_5cb,_5c3);
if(_5c3.explodeClassName){
_5cb.className=_5c3.explodeClassName;
}
dojo.html.setOpacity(_5cb,0.3);
with(_5cb.style){
position="absolute";
display="none";
backgroundColor=h.getStyle(_5c3,"background-color").toLowerCase();
}
dojo.body().appendChild(_5cb);
var _5cc={opacity:{start:1,end:0.5}};
dojo.lang.forEach(["height","width","top","left"],function(type){
_5cc[type]={start:_5c9[type],end:_5ca[type]};
});
var anim=new dojo.lfx.propertyAnimation(_5cb,_5cc,_5c5,_5c6,{"beforeBegin":function(){
dojo.html.hide(_5c3);
dojo.html.show(_5cb);
},"onEnd":function(){
_5cb.parentNode.removeChild(_5cb);
}});
if(_5c7){
anim.connect("onEnd",function(){
_5c7(_5c3,anim);
});
}
return anim;
};
dojo.lfx.html.highlight=function(_5cf,_5d0,_5d1,_5d2,_5d3){
_5cf=dojo.lfx.html._byId(_5cf);
var _5d4=[];
dojo.lang.forEach(_5cf,function(node){
var _5d6=dojo.html.getBackgroundColor(node);
var bg=dojo.html.getStyle(node,"background-color").toLowerCase();
var _5d8=dojo.html.getStyle(node,"background-image");
var _5d9=(bg=="transparent"||bg=="rgba(0, 0, 0, 0)");
while(_5d6.length>3){
_5d6.pop();
}
var rgb=new dojo.gfx.color.Color(_5d0);
var _5db=new dojo.gfx.color.Color(_5d6);
var anim=dojo.lfx.propertyAnimation(node,{"background-color":{start:rgb,end:_5db}},_5d1,_5d2,{"beforeBegin":function(){
if(_5d8){
node.style.backgroundImage="none";
}
node.style.backgroundColor="rgb("+rgb.toRgb().join(",")+")";
},"onEnd":function(){
if(_5d8){
node.style.backgroundImage=_5d8;
}
if(_5d9){
node.style.backgroundColor="transparent";
}
if(_5d3){
_5d3(node,anim);
}
}});
_5d4.push(anim);
});
return dojo.lfx.combine(_5d4);
};
dojo.lfx.html.unhighlight=function(_5dd,_5de,_5df,_5e0,_5e1){
_5dd=dojo.lfx.html._byId(_5dd);
var _5e2=[];
dojo.lang.forEach(_5dd,function(node){
var _5e4=new dojo.gfx.color.Color(dojo.html.getBackgroundColor(node));
var rgb=new dojo.gfx.color.Color(_5de);
var _5e6=dojo.html.getStyle(node,"background-image");
var anim=dojo.lfx.propertyAnimation(node,{"background-color":{start:_5e4,end:rgb}},_5df,_5e0,{"beforeBegin":function(){
if(_5e6){
node.style.backgroundImage="none";
}
node.style.backgroundColor="rgb("+_5e4.toRgb().join(",")+")";
},"onEnd":function(){
if(_5e1){
_5e1(node,anim);
}
}});
_5e2.push(anim);
});
return dojo.lfx.combine(_5e2);
};
dojo.lang.mixin(dojo.lfx,dojo.lfx.html);
dojo.provide("dojo.lfx.*");

