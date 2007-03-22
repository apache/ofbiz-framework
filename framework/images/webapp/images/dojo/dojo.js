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
var _f7=dojo.doc();
do{
var id="dj_unique_"+(++arguments.callee._idIncrement);
}while(_f7.getElementById(id));
return id;
};
dojo.dom.getUniqueId._idIncrement=0;
dojo.dom.firstElement=dojo.dom.getFirstChildElement=function(_f9,_fa){
var _fb=_f9.firstChild;
while(_fb&&_fb.nodeType!=dojo.dom.ELEMENT_NODE){
_fb=_fb.nextSibling;
}
if(_fa&&_fb&&_fb.tagName&&_fb.tagName.toLowerCase()!=_fa.toLowerCase()){
_fb=dojo.dom.nextElement(_fb,_fa);
}
return _fb;
};
dojo.dom.lastElement=dojo.dom.getLastChildElement=function(_fc,_fd){
var _fe=_fc.lastChild;
while(_fe&&_fe.nodeType!=dojo.dom.ELEMENT_NODE){
_fe=_fe.previousSibling;
}
if(_fd&&_fe&&_fe.tagName&&_fe.tagName.toLowerCase()!=_fd.toLowerCase()){
_fe=dojo.dom.prevElement(_fe,_fd);
}
return _fe;
};
dojo.dom.nextElement=dojo.dom.getNextSiblingElement=function(_ff,_100){
if(!_ff){
return null;
}
do{
_ff=_ff.nextSibling;
}while(_ff&&_ff.nodeType!=dojo.dom.ELEMENT_NODE);
if(_ff&&_100&&_100.toLowerCase()!=_ff.tagName.toLowerCase()){
return dojo.dom.nextElement(_ff,_100);
}
return _ff;
};
dojo.dom.prevElement=dojo.dom.getPreviousSiblingElement=function(node,_102){
if(!node){
return null;
}
if(_102){
_102=_102.toLowerCase();
}
do{
node=node.previousSibling;
}while(node&&node.nodeType!=dojo.dom.ELEMENT_NODE);
if(node&&_102&&_102.toLowerCase()!=node.tagName.toLowerCase()){
return dojo.dom.prevElement(node,_102);
}
return node;
};
dojo.dom.moveChildren=function(_103,_104,trim){
var _106=0;
if(trim){
while(_103.hasChildNodes()&&_103.firstChild.nodeType==dojo.dom.TEXT_NODE){
_103.removeChild(_103.firstChild);
}
while(_103.hasChildNodes()&&_103.lastChild.nodeType==dojo.dom.TEXT_NODE){
_103.removeChild(_103.lastChild);
}
}
while(_103.hasChildNodes()){
_104.appendChild(_103.firstChild);
_106++;
}
return _106;
};
dojo.dom.copyChildren=function(_107,_108,trim){
var _10a=_107.cloneNode(true);
return this.moveChildren(_10a,_108,trim);
};
dojo.dom.replaceChildren=function(node,_10c){
var _10d=[];
if(dojo.render.html.ie){
for(var i=0;i<node.childNodes.length;i++){
_10d.push(node.childNodes[i]);
}
}
dojo.dom.removeChildren(node);
node.appendChild(_10c);
for(var i=0;i<_10d.length;i++){
dojo.dom.destroyNode(_10d[i]);
}
};
dojo.dom.removeChildren=function(node){
var _110=node.childNodes.length;
while(node.hasChildNodes()){
dojo.dom.removeNode(node.firstChild);
}
return _110;
};
dojo.dom.replaceNode=function(node,_112){
return node.parentNode.replaceChild(_112,node);
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
dojo.dom.getAncestors=function(node,_116,_117){
var _118=[];
var _119=(_116&&(_116 instanceof Function||typeof _116=="function"));
while(node){
if(!_119||_116(node)){
_118.push(node);
}
if(_117&&_118.length>0){
return _118[0];
}
node=node.parentNode;
}
if(_117){
return null;
}
return _118;
};
dojo.dom.getAncestorsByTag=function(node,tag,_11c){
tag=tag.toLowerCase();
return dojo.dom.getAncestors(node,function(el){
return ((el.tagName)&&(el.tagName.toLowerCase()==tag));
},_11c);
};
dojo.dom.getFirstAncestorByTag=function(node,tag){
return dojo.dom.getAncestorsByTag(node,tag,true);
};
dojo.dom.isDescendantOf=function(node,_121,_122){
if(_122&&node){
node=node.parentNode;
}
while(node){
if(node==_121){
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
var _125=dojo.doc();
if(!dj_undef("ActiveXObject")){
var _126=["MSXML2","Microsoft","MSXML","MSXML3"];
for(var i=0;i<_126.length;i++){
try{
doc=new ActiveXObject(_126[i]+".XMLDOM");
}
catch(e){
}
if(doc){
break;
}
}
}else{
if((_125.implementation)&&(_125.implementation.createDocument)){
doc=_125.implementation.createDocument("","",null);
}
}
return doc;
};
dojo.dom.createDocumentFromText=function(str,_129){
if(!_129){
_129="text/xml";
}
if(!dj_undef("DOMParser")){
var _12a=new DOMParser();
return _12a.parseFromString(str,_129);
}else{
if(!dj_undef("ActiveXObject")){
var _12b=dojo.dom.createDocument();
if(_12b){
_12b.async=false;
_12b.loadXML(str);
return _12b;
}else{
dojo.debug("toXml didn't work?");
}
}else{
var _12c=dojo.doc();
if(_12c.createElement){
var tmp=_12c.createElement("xml");
tmp.innerHTML=str;
if(_12c.implementation&&_12c.implementation.createDocument){
var _12e=_12c.implementation.createDocument("foo","",null);
for(var i=0;i<tmp.childNodes.length;i++){
_12e.importNode(tmp.childNodes.item(i),true);
}
return _12e;
}
return ((tmp.document)&&(tmp.document.firstChild?tmp.document.firstChild:tmp));
}
}
}
return null;
};
dojo.dom.prependChild=function(node,_131){
if(_131.firstChild){
_131.insertBefore(node,_131.firstChild);
}else{
_131.appendChild(node);
}
return true;
};
dojo.dom.insertBefore=function(node,ref,_134){
if((_134!=true)&&(node===ref||node.nextSibling===ref)){
return false;
}
var _135=ref.parentNode;
_135.insertBefore(node,ref);
return true;
};
dojo.dom.insertAfter=function(node,ref,_138){
var pn=ref.parentNode;
if(ref==pn.lastChild){
if((_138!=true)&&(node===ref)){
return false;
}
pn.appendChild(node);
}else{
return this.insertBefore(node,ref.nextSibling,_138);
}
return true;
};
dojo.dom.insertAtPosition=function(node,ref,_13c){
if((!node)||(!ref)||(!_13c)){
return false;
}
switch(_13c.toLowerCase()){
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
dojo.dom.insertAtIndex=function(node,_13e,_13f){
var _140=_13e.childNodes;
if(!_140.length||_140.length==_13f){
_13e.appendChild(node);
return true;
}
if(_13f==0){
return dojo.dom.prependChild(node,_13e);
}
return dojo.dom.insertAfter(node,_140[_13f-1]);
};
dojo.dom.textContent=function(node,text){
if(arguments.length>1){
var _143=dojo.doc();
dojo.dom.replaceChildren(node,_143.createTextNode(text));
return text;
}else{
if(node.textContent!=undefined){
return node.textContent;
}
var _144="";
if(node==null){
return _144;
}
for(var i=0;i<node.childNodes.length;i++){
switch(node.childNodes[i].nodeType){
case 1:
case 5:
_144+=dojo.dom.textContent(node.childNodes[i]);
break;
case 3:
case 2:
case 4:
_144+=node.childNodes[i].nodeValue;
break;
default:
break;
}
}
return _144;
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
dojo.dom.setAttributeNS=function(elem,_14a,_14b,_14c){
if(elem==null||((elem==undefined)&&(typeof elem=="undefined"))){
dojo.raise("No element given to dojo.dom.setAttributeNS");
}
if(!((elem.setAttributeNS==undefined)&&(typeof elem.setAttributeNS=="undefined"))){
elem.setAttributeNS(_14a,_14b,_14c);
}else{
var _14d=elem.ownerDocument;
var _14e=_14d.createNode(2,_14b,_14a);
_14e.nodeValue=_14c;
elem.setAttributeNode(_14e);
}
};
dojo.provide("dojo.xml.Parse");
dojo.xml.Parse=function(){
var isIE=((dojo.render.html.capable)&&(dojo.render.html.ie));
function getTagName(node){
try{
return node.tagName.toLowerCase();
}
catch(e){
return "";
}
}
function getDojoTagName(node){
var _152=getTagName(node);
if(!_152){
return "";
}
if((dojo.widget)&&(dojo.widget.tags[_152])){
return _152;
}
var p=_152.indexOf(":");
if(p>=0){
return _152;
}
if(_152.substr(0,5)=="dojo:"){
return _152;
}
if(dojo.render.html.capable&&dojo.render.html.ie&&node.scopeName!="HTML"){
return node.scopeName.toLowerCase()+":"+_152;
}
if(_152.substr(0,4)=="dojo"){
return "dojo:"+_152.substring(4);
}
var djt=node.getAttribute("dojoType")||node.getAttribute("dojotype");
if(djt){
if(djt.indexOf(":")<0){
djt="dojo:"+djt;
}
return djt.toLowerCase();
}
djt=node.getAttributeNS&&node.getAttributeNS(dojo.dom.dojoml,"type");
if(djt){
return "dojo:"+djt.toLowerCase();
}
try{
djt=node.getAttribute("dojo:type");
}
catch(e){
}
if(djt){
return "dojo:"+djt.toLowerCase();
}
if((dj_global["djConfig"])&&(!djConfig["ignoreClassNames"])){
var _155=node.className||node.getAttribute("class");
if((_155)&&(_155.indexOf)&&(_155.indexOf("dojo-")!=-1)){
var _156=_155.split(" ");
for(var x=0,c=_156.length;x<c;x++){
if(_156[x].slice(0,5)=="dojo-"){
return "dojo:"+_156[x].substr(5).toLowerCase();
}
}
}
}
return "";
}
this.parseElement=function(node,_15a,_15b,_15c){
var _15d=getTagName(node);
if(isIE&&_15d.indexOf("/")==0){
return null;
}
try{
var attr=node.getAttribute("parseWidgets");
if(attr&&attr.toLowerCase()=="false"){
return {};
}
}
catch(e){
}
var _15f=true;
if(_15b){
var _160=getDojoTagName(node);
_15d=_160||_15d;
_15f=Boolean(_160);
}
var _161={};
_161[_15d]=[];
var pos=_15d.indexOf(":");
if(pos>0){
var ns=_15d.substring(0,pos);
_161["ns"]=ns;
if((dojo.ns)&&(!dojo.ns.allow(ns))){
_15f=false;
}
}
if(_15f){
var _164=this.parseAttributes(node);
for(var attr in _164){
if((!_161[_15d][attr])||(typeof _161[_15d][attr]!="array")){
_161[_15d][attr]=[];
}
_161[_15d][attr].push(_164[attr]);
}
_161[_15d].nodeRef=node;
_161.tagName=_15d;
_161.index=_15c||0;
}
var _165=0;
for(var i=0;i<node.childNodes.length;i++){
var tcn=node.childNodes.item(i);
switch(tcn.nodeType){
case dojo.dom.ELEMENT_NODE:
var ctn=getDojoTagName(tcn)||getTagName(tcn);
if(!_161[ctn]){
_161[ctn]=[];
}
_161[ctn].push(this.parseElement(tcn,true,_15b,_165));
if((tcn.childNodes.length==1)&&(tcn.childNodes.item(0).nodeType==dojo.dom.TEXT_NODE)){
_161[ctn][_161[ctn].length-1].value=tcn.childNodes.item(0).nodeValue;
}
_165++;
break;
case dojo.dom.TEXT_NODE:
if(node.childNodes.length==1){
_161[_15d].push({value:node.childNodes.item(0).nodeValue});
}
break;
default:
break;
}
}
return _161;
};
this.parseAttributes=function(node){
var _16a={};
var atts=node.attributes;
var _16c,i=0;
while((_16c=atts[i++])){
if(isIE){
if(!_16c){
continue;
}
if((typeof _16c=="object")&&(typeof _16c.nodeValue=="undefined")||(_16c.nodeValue==null)||(_16c.nodeValue=="")){
continue;
}
}
var nn=_16c.nodeName.split(":");
nn=(nn.length==2)?nn[1]:_16c.nodeName;
_16a[nn]={value:_16c.nodeValue};
}
return _16a;
};
};
dojo.provide("dojo.lang.common");
dojo.lang.inherits=function(_16f,_170){
if(!dojo.lang.isFunction(_170)){
dojo.raise("dojo.inherits: superclass argument ["+_170+"] must be a function (subclass: ["+_16f+"']");
}
_16f.prototype=new _170();
_16f.prototype.constructor=_16f;
_16f.superclass=_170.prototype;
_16f["super"]=_170.prototype;
};
dojo.lang._mixin=function(obj,_172){
var tobj={};
for(var x in _172){
if((typeof tobj[x]=="undefined")||(tobj[x]!=_172[x])){
obj[x]=_172[x];
}
}
if(dojo.render.html.ie&&(typeof (_172["toString"])=="function")&&(_172["toString"]!=obj["toString"])&&(_172["toString"]!=tobj["toString"])){
obj.toString=_172.toString;
}
return obj;
};
dojo.lang.mixin=function(obj,_176){
for(var i=1,l=arguments.length;i<l;i++){
dojo.lang._mixin(obj,arguments[i]);
}
return obj;
};
dojo.lang.extend=function(_179,_17a){
for(var i=1,l=arguments.length;i<l;i++){
dojo.lang._mixin(_179.prototype,arguments[i]);
}
return _179;
};
dojo.inherits=dojo.lang.inherits;
dojo.mixin=dojo.lang.mixin;
dojo.extend=dojo.lang.extend;
dojo.lang.find=function(_17d,_17e,_17f,_180){
if(!dojo.lang.isArrayLike(_17d)&&dojo.lang.isArrayLike(_17e)){
dojo.deprecated("dojo.lang.find(value, array)","use dojo.lang.find(array, value) instead","0.5");
var temp=_17d;
_17d=_17e;
_17e=temp;
}
var _182=dojo.lang.isString(_17d);
if(_182){
_17d=_17d.split("");
}
if(_180){
var step=-1;
var i=_17d.length-1;
var end=-1;
}else{
var step=1;
var i=0;
var end=_17d.length;
}
if(_17f){
while(i!=end){
if(_17d[i]===_17e){
return i;
}
i+=step;
}
}else{
while(i!=end){
if(_17d[i]==_17e){
return i;
}
i+=step;
}
}
return -1;
};
dojo.lang.indexOf=dojo.lang.find;
dojo.lang.findLast=function(_186,_187,_188){
return dojo.lang.find(_186,_187,_188,true);
};
dojo.lang.lastIndexOf=dojo.lang.findLast;
dojo.lang.inArray=function(_189,_18a){
return dojo.lang.find(_189,_18a)>-1;
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
dojo.provide("dojo.lang.func");
dojo.lang.hitch=function(_195,_196){
var fcn=(dojo.lang.isString(_196)?_195[_196]:_196)||function(){
};
return function(){
return fcn.apply(_195,arguments);
};
};
dojo.lang.anonCtr=0;
dojo.lang.anon={};
dojo.lang.nameAnonFunc=function(_198,_199,_19a){
var nso=(_199||dojo.lang.anon);
if((_19a)||((dj_global["djConfig"])&&(djConfig["slowAnonFuncLookups"]==true))){
for(var x in nso){
try{
if(nso[x]===_198){
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
nso[ret]=_198;
return ret;
};
dojo.lang.forward=function(_19e){
return function(){
return this[_19e].apply(this,arguments);
};
};
dojo.lang.curry=function(_19f,func){
var _1a1=[];
_19f=_19f||dj_global;
if(dojo.lang.isString(func)){
func=_19f[func];
}
for(var x=2;x<arguments.length;x++){
_1a1.push(arguments[x]);
}
var _1a3=(func["__preJoinArity"]||func.length)-_1a1.length;
function gather(_1a4,_1a5,_1a6){
var _1a7=_1a6;
var _1a8=_1a5.slice(0);
for(var x=0;x<_1a4.length;x++){
_1a8.push(_1a4[x]);
}
_1a6=_1a6-_1a4.length;
if(_1a6<=0){
var res=func.apply(_19f,_1a8);
_1a6=_1a7;
return res;
}else{
return function(){
return gather(arguments,_1a8,_1a6);
};
}
}
return gather([],_1a1,_1a3);
};
dojo.lang.curryArguments=function(_1ab,func,args,_1ae){
var _1af=[];
var x=_1ae||0;
for(x=_1ae;x<args.length;x++){
_1af.push(args[x]);
}
return dojo.lang.curry.apply(dojo.lang,[_1ab,func].concat(_1af));
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
dojo.lang.delayThese=function(farr,cb,_1b5,_1b6){
if(!farr.length){
if(typeof _1b6=="function"){
_1b6();
}
return;
}
if((typeof _1b5=="undefined")&&(typeof cb=="number")){
_1b5=cb;
cb=function(){
};
}else{
if(!cb){
cb=function(){
};
if(!_1b5){
_1b5=0;
}
}
}
setTimeout(function(){
(farr.shift())();
cb();
dojo.lang.delayThese(farr,cb,_1b5,_1b6);
},_1b5);
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
var _1bb=0;
for(var x in obj){
if(obj[x]&&(!tmp[x])){
_1bb++;
break;
}
}
return _1bb==0;
}else{
if(dojo.lang.isArrayLike(obj)||dojo.lang.isString(obj)){
return obj.length==0;
}
}
},map:function(arr,obj,_1bf){
var _1c0=dojo.lang.isString(arr);
if(_1c0){
arr=arr.split("");
}
if(dojo.lang.isFunction(obj)&&(!_1bf)){
_1bf=obj;
obj=dj_global;
}else{
if(dojo.lang.isFunction(obj)&&_1bf){
var _1c1=obj;
obj=_1bf;
_1bf=_1c1;
}
}
if(Array.map){
var _1c2=Array.map(arr,_1bf,obj);
}else{
var _1c2=[];
for(var i=0;i<arr.length;++i){
_1c2.push(_1bf.call(obj,arr[i]));
}
}
if(_1c0){
return _1c2.join("");
}else{
return _1c2;
}
},reduce:function(arr,_1c5,obj,_1c7){
var _1c8=_1c5;
if(arguments.length==1){
dojo.debug("dojo.lang.reduce called with too few arguments!");
return false;
}else{
if(arguments.length==2){
_1c7=_1c5;
_1c8=arr.shift();
}else{
if(arguments.lenght==3){
if(dojo.lang.isFunction(obj)){
_1c7=obj;
obj=null;
}
}else{
if(dojo.lang.isFunction(obj)){
var tmp=_1c7;
_1c7=obj;
obj=tmp;
}
}
}
}
var ob=obj?obj:dj_global;
dojo.lang.map(arr,function(val){
_1c8=_1c7.call(ob,_1c8,val);
});
return _1c8;
},forEach:function(_1cc,_1cd,_1ce){
if(dojo.lang.isString(_1cc)){
_1cc=_1cc.split("");
}
if(Array.forEach){
Array.forEach(_1cc,_1cd,_1ce);
}else{
if(!_1ce){
_1ce=dj_global;
}
for(var i=0,l=_1cc.length;i<l;i++){
_1cd.call(_1ce,_1cc[i],i,_1cc);
}
}
},_everyOrSome:function(_1d1,arr,_1d3,_1d4){
if(dojo.lang.isString(arr)){
arr=arr.split("");
}
if(Array.every){
return Array[_1d1?"every":"some"](arr,_1d3,_1d4);
}else{
if(!_1d4){
_1d4=dj_global;
}
for(var i=0,l=arr.length;i<l;i++){
var _1d7=_1d3.call(_1d4,arr[i],i,arr);
if(_1d1&&!_1d7){
return false;
}else{
if((!_1d1)&&(_1d7)){
return true;
}
}
}
return Boolean(_1d1);
}
},every:function(arr,_1d9,_1da){
return this._everyOrSome(true,arr,_1d9,_1da);
},some:function(arr,_1dc,_1dd){
return this._everyOrSome(false,arr,_1dc,_1dd);
},filter:function(arr,_1df,_1e0){
var _1e1=dojo.lang.isString(arr);
if(_1e1){
arr=arr.split("");
}
var _1e2;
if(Array.filter){
_1e2=Array.filter(arr,_1df,_1e0);
}else{
if(!_1e0){
if(arguments.length>=3){
dojo.raise("thisObject doesn't exist!");
}
_1e0=dj_global;
}
_1e2=[];
for(var i=0;i<arr.length;i++){
if(_1df.call(_1e0,arr[i],i,arr)){
_1e2.push(arr[i]);
}
}
}
if(_1e1){
return _1e2.join("");
}else{
return _1e2;
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
},toArray:function(_1e7,_1e8){
var _1e9=[];
for(var i=_1e8||0;i<_1e7.length;i++){
_1e9.push(_1e7[i]);
}
return _1e9;
}});
dojo.provide("dojo.lang.extras");
dojo.lang.setTimeout=function(func,_1ec){
var _1ed=window,_1ee=2;
if(!dojo.lang.isFunction(func)){
_1ed=func;
func=_1ec;
_1ec=arguments[2];
_1ee++;
}
if(dojo.lang.isString(func)){
func=_1ed[func];
}
var args=[];
for(var i=_1ee;i<arguments.length;i++){
args.push(arguments[i]);
}
return dojo.global().setTimeout(function(){
func.apply(_1ed,args);
},_1ec);
};
dojo.lang.clearTimeout=function(_1f1){
dojo.global().clearTimeout(_1f1);
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
dojo.lang.getObjPathValue=function(_1fa,_1fb,_1fc){
with(dojo.parseObjPath(_1fa,_1fb,_1fc)){
return dojo.evalProp(prop,obj,_1fc);
}
};
dojo.lang.setObjPathValue=function(_1fd,_1fe,_1ff,_200){
dojo.deprecated("dojo.lang.setObjPathValue","use dojo.parseObjPath and the '=' operator","0.6");
if(arguments.length<4){
_200=true;
}
with(dojo.parseObjPath(_1fd,_1ff,_200)){
if(obj&&(_200||(prop in obj))){
obj[prop]=_1fe;
}
}
};
dojo.provide("dojo.lang.declare");
dojo.lang.declare=function(_201,_202,init,_204){
if((dojo.lang.isFunction(_204))||((!_204)&&(!dojo.lang.isFunction(init)))){
var temp=_204;
_204=init;
init=temp;
}
var _206=[];
if(dojo.lang.isArray(_202)){
_206=_202;
_202=_206.shift();
}
if(!init){
init=dojo.evalObjPath(_201,false);
if((init)&&(!dojo.lang.isFunction(init))){
init=null;
}
}
var ctor=dojo.lang.declare._makeConstructor();
var scp=(_202?_202.prototype:null);
if(scp){
scp.prototyping=true;
ctor.prototype=new _202();
scp.prototyping=false;
}
ctor.superclass=scp;
ctor.mixins=_206;
for(var i=0,l=_206.length;i<l;i++){
dojo.lang.extend(ctor,_206[i].prototype);
}
ctor.prototype.initializer=null;
ctor.prototype.declaredClass=_201;
if(dojo.lang.isArray(_204)){
dojo.lang.extend.apply(dojo.lang,[ctor].concat(_204));
}else{
dojo.lang.extend(ctor,(_204)||{});
}
dojo.lang.extend(ctor,dojo.lang.declare._common);
ctor.prototype.constructor=ctor;
ctor.prototype.initializer=(ctor.prototype.initializer)||(init)||(function(){
});
var _20b=dojo.parseObjPath(_201,null,true);
_20b.obj[_20b.prop]=ctor;
return ctor;
};
dojo.lang.declare._makeConstructor=function(){
return function(){
var self=this._getPropContext();
var s=self.constructor.superclass;
if((s)&&(s.constructor)){
if(s.constructor==arguments.callee){
this._inherited("constructor",arguments);
}else{
this._contextMethod(s,"constructor",arguments);
}
}
var ms=(self.constructor.mixins)||([]);
for(var i=0,m;(m=ms[i]);i++){
(((m.prototype)&&(m.prototype.initializer))||(m)).apply(this,arguments);
}
if((!this.prototyping)&&(self.initializer)){
self.initializer.apply(this,arguments);
}
};
};
dojo.lang.declare._common={_getPropContext:function(){
return (this.___proto||this);
},_contextMethod:function(_211,_212,args){
var _214,_215=this.___proto;
this.___proto=_211;
try{
_214=_211[_212].apply(this,(args||[]));
}
catch(e){
throw e;
}
finally{
this.___proto=_215;
}
return _214;
},_inherited:function(prop,args){
var p=this._getPropContext();
do{
if((!p.constructor)||(!p.constructor.superclass)){
return;
}
p=p.constructor.superclass;
}while(!(prop in p));
return (dojo.lang.isFunction(p[prop])?this._contextMethod(p,prop,args):p[prop]);
},inherited:function(prop,args){
dojo.deprecated("'inherited' method is dangerous, do not up-call! 'inherited' is slated for removal in 0.5; name your super class (or use superclass property) instead.","0.5");
this._inherited(prop,args);
}};
dojo.declare=dojo.lang.declare;
dojo.provide("dojo.ns");
dojo.ns={namespaces:{},failed:{},loading:{},loaded:{},register:function(name,_21c,_21d,_21e){
if(!_21e||!this.namespaces[name]){
this.namespaces[name]=new dojo.ns.Ns(name,_21c,_21d);
}
},allow:function(name){
if(this.failed[name]){
return false;
}
if((djConfig.excludeNamespace)&&(dojo.lang.inArray(djConfig.excludeNamespace,name))){
return false;
}
return ((name==this.dojo)||(!djConfig.includeNamespace)||(dojo.lang.inArray(djConfig.includeNamespace,name)));
},get:function(name){
return this.namespaces[name];
},require:function(name){
var ns=this.namespaces[name];
if((ns)&&(this.loaded[name])){
return ns;
}
if(!this.allow(name)){
return false;
}
if(this.loading[name]){
dojo.debug("dojo.namespace.require: re-entrant request to load namespace \""+name+"\" must fail.");
return false;
}
var req=dojo.require;
this.loading[name]=true;
try{
if(name=="dojo"){
req("dojo.namespaces.dojo");
}else{
if(!dojo.hostenv.moduleHasPrefix(name)){
dojo.registerModulePath(name,"../"+name);
}
req([name,"manifest"].join("."),false,true);
}
if(!this.namespaces[name]){
this.failed[name]=true;
}
}
finally{
this.loading[name]=false;
}
return this.namespaces[name];
}};
dojo.ns.Ns=function(name,_225,_226){
this.name=name;
this.module=_225;
this.resolver=_226;
this._loaded=[];
this._failed=[];
};
dojo.ns.Ns.prototype.resolve=function(name,_228,_229){
if(!this.resolver||djConfig["skipAutoRequire"]){
return false;
}
var _22a=this.resolver(name,_228);
if((_22a)&&(!this._loaded[_22a])&&(!this._failed[_22a])){
var req=dojo.require;
req(_22a,false,true);
if(dojo.hostenv.findModule(_22a,false)){
this._loaded[_22a]=true;
}else{
if(!_229){
dojo.raise("dojo.ns.Ns.resolve: module '"+_22a+"' not found after loading via namespace '"+this.name+"'");
}
this._failed[_22a]=true;
}
}
return Boolean(this._loaded[_22a]);
};
dojo.registerNamespace=function(name,_22d,_22e){
dojo.ns.register.apply(dojo.ns,arguments);
};
dojo.registerNamespaceResolver=function(name,_230){
var n=dojo.ns.namespaces[name];
if(n){
n.resolver=_230;
}
};
dojo.registerNamespaceManifest=function(_232,path,name,_235,_236){
dojo.registerModulePath(name,path);
dojo.registerNamespace(name,_235,_236);
};
dojo.registerNamespace("dojo","dojo.widget");
dojo.provide("dojo.event.common");
dojo.event=new function(){
this._canTimeout=dojo.lang.isFunction(dj_global["setTimeout"])||dojo.lang.isAlien(dj_global["setTimeout"]);
function interpolateArgs(args,_238){
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
var _23b=dl.nameAnonFunc(args[2],ao.adviceObj,_238);
ao.adviceFunc=_23b;
}else{
if((dl.isFunction(args[0]))&&(dl.isObject(args[1]))&&(dl.isString(args[2]))){
ao.adviceType="after";
ao.srcObj=dj_global;
var _23b=dl.nameAnonFunc(args[0],ao.srcObj,_238);
ao.srcFunc=_23b;
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
var _23b=dl.nameAnonFunc(args[1],dj_global,_238);
ao.srcFunc=_23b;
ao.adviceObj=args[2];
ao.adviceFunc=args[3];
}else{
if((dl.isString(args[0]))&&(dl.isObject(args[1]))&&(dl.isString(args[2]))&&(dl.isFunction(args[3]))){
ao.srcObj=args[1];
ao.srcFunc=args[2];
var _23b=dl.nameAnonFunc(args[3],dj_global,_238);
ao.adviceObj=dj_global;
ao.adviceFunc=_23b;
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
var _23b=dl.nameAnonFunc(ao.aroundFunc,ao.aroundObj,_238);
ao.aroundFunc=_23b;
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
var _23d={};
for(var x in ao){
_23d[x]=ao[x];
}
var mjps=[];
dojo.lang.forEach(ao.srcObj,function(src){
if((dojo.render.html.capable)&&(dojo.lang.isString(src))){
src=dojo.byId(src);
}
_23d.srcObj=src;
mjps.push(dojo.event.connect.call(dojo.event,_23d));
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
var _245;
if((arguments.length==1)&&(typeof a1=="object")){
_245=a1;
}else{
_245={srcObj:a1,srcFunc:a2};
}
_245.adviceFunc=function(){
var _246=[];
for(var x=0;x<arguments.length;x++){
_246.push(arguments[x]);
}
dojo.debug("("+_245.srcObj+")."+_245.srcFunc,":",_246.join(", "));
};
this.kwConnect(_245);
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
this._kwConnectImpl=function(_24d,_24e){
var fn=(_24e)?"disconnect":"connect";
if(typeof _24d["srcFunc"]=="function"){
_24d.srcObj=_24d["srcObj"]||dj_global;
var _250=dojo.lang.nameAnonFunc(_24d.srcFunc,_24d.srcObj,true);
_24d.srcFunc=_250;
}
if(typeof _24d["adviceFunc"]=="function"){
_24d.adviceObj=_24d["adviceObj"]||dj_global;
var _250=dojo.lang.nameAnonFunc(_24d.adviceFunc,_24d.adviceObj,true);
_24d.adviceFunc=_250;
}
_24d.srcObj=_24d["srcObj"]||dj_global;
_24d.adviceObj=_24d["adviceObj"]||_24d["targetObj"]||dj_global;
_24d.adviceFunc=_24d["adviceFunc"]||_24d["targetFunc"];
return dojo.event[fn](_24d);
};
this.kwConnect=function(_251){
return this._kwConnectImpl(_251,false);
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
this.kwDisconnect=function(_254){
return this._kwConnectImpl(_254,true);
};
};
dojo.event.MethodInvocation=function(_255,obj,args){
this.jp_=_255;
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
dojo.event.MethodJoinPoint=function(obj,_25d){
this.object=obj||dj_global;
this.methodname=_25d;
this.methodfunc=this.object[_25d];
this.squelch=false;
};
dojo.event.MethodJoinPoint.getForMethod=function(obj,_25f){
if(!obj){
obj=dj_global;
}
if(!obj[_25f]){
obj[_25f]=function(){
};
if(!obj[_25f]){
dojo.raise("Cannot set do-nothing method on that object "+_25f);
}
}else{
if((!dojo.lang.isFunction(obj[_25f]))&&(!dojo.lang.isAlien(obj[_25f]))){
return null;
}
}
var _260=_25f+"$joinpoint";
var _261=_25f+"$joinpoint$method";
var _262=obj[_260];
if(!_262){
var _263=false;
if(dojo.event["browser"]){
if((obj["attachEvent"])||(obj["nodeType"])||(obj["addEventListener"])){
_263=true;
dojo.event.browser.addClobberNodeAttrs(obj,[_260,_261,_25f]);
}
}
var _264=obj[_25f].length;
obj[_261]=obj[_25f];
_262=obj[_260]=new dojo.event.MethodJoinPoint(obj,_261);
obj[_25f]=function(){
var args=[];
if((_263)&&(!arguments.length)){
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
if((x==0)&&(_263)&&(dojo.event.browser.isEvent(arguments[x]))){
args.push(dojo.event.browser.fixEvent(arguments[x],this));
}else{
args.push(arguments[x]);
}
}
}
return _262.run.apply(_262,args);
};
obj[_25f].__preJoinArity=_264;
}
return _262;
};
dojo.lang.extend(dojo.event.MethodJoinPoint,{unintercept:function(){
this.object[this.methodname]=this.methodfunc;
this.before=[];
this.after=[];
this.around=[];
},disconnect:dojo.lang.forward("unintercept"),run:function(){
var obj=this.object||dj_global;
var args=arguments;
var _26a=[];
for(var x=0;x<args.length;x++){
_26a[x]=args[x];
}
var _26c=function(marr){
if(!marr){
dojo.debug("Null argument to unrollAdvice()");
return;
}
var _26e=marr[0]||dj_global;
var _26f=marr[1];
if(!_26e[_26f]){
dojo.raise("function \""+_26f+"\" does not exist on \""+_26e+"\"");
}
var _270=marr[2]||dj_global;
var _271=marr[3];
var msg=marr[6];
var _273;
var to={args:[],jp_:this,object:obj,proceed:function(){
return _26e[_26f].apply(_26e,to.args);
}};
to.args=_26a;
var _275=parseInt(marr[4]);
var _276=((!isNaN(_275))&&(marr[4]!==null)&&(typeof marr[4]!="undefined"));
if(marr[5]){
var rate=parseInt(marr[5]);
var cur=new Date();
var _279=false;
if((marr["last"])&&((cur-marr.last)<=rate)){
if(dojo.event._canTimeout){
if(marr["delayTimer"]){
clearTimeout(marr.delayTimer);
}
var tod=parseInt(rate*2);
var mcpy=dojo.lang.shallowCopy(marr);
marr.delayTimer=setTimeout(function(){
mcpy[5]=0;
_26c(mcpy);
},tod);
}
return;
}else{
marr.last=cur;
}
}
if(_271){
_270[_271].call(_270,to);
}else{
if((_276)&&((dojo.render.html)||(dojo.render.svg))){
dj_global["setTimeout"](function(){
if(msg){
_26e[_26f].call(_26e,to);
}else{
_26e[_26f].apply(_26e,args);
}
},_275);
}else{
if(msg){
_26e[_26f].call(_26e,to);
}else{
_26e[_26f].apply(_26e,args);
}
}
}
};
var _27c=function(){
if(this.squelch){
try{
return _26c.apply(this,arguments);
}
catch(e){
dojo.debug(e);
}
}else{
return _26c.apply(this,arguments);
}
};
if((this["before"])&&(this.before.length>0)){
dojo.lang.forEach(this.before.concat(new Array()),_27c);
}
var _27d;
try{
if((this["around"])&&(this.around.length>0)){
var mi=new dojo.event.MethodInvocation(this,obj,args);
_27d=mi.proceed();
}else{
if(this.methodfunc){
_27d=this.object[this.methodname].apply(this.object,args);
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
dojo.lang.forEach(this.after.concat(new Array()),_27c);
}
return (this.methodfunc)?_27d:null;
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
},addAdvice:function(_282,_283,_284,_285,_286,_287,once,_289,rate,_28b){
var arr=this.getArr(_286);
if(!arr){
dojo.raise("bad this: "+this);
}
var ao=[_282,_283,_284,_285,_289,rate,_28b];
if(once){
if(this.hasAdvice(_282,_283,_286,arr)>=0){
return;
}
}
if(_287=="first"){
arr.unshift(ao);
}else{
arr.push(ao);
}
},hasAdvice:function(_28e,_28f,_290,arr){
if(!arr){
arr=this.getArr(_290);
}
var ind=-1;
for(var x=0;x<arr.length;x++){
var aao=(typeof _28f=="object")?(new String(_28f)).toString():_28f;
var a1o=(typeof arr[x][1]=="object")?(new String(arr[x][1])).toString():arr[x][1];
if((arr[x][0]==_28e)&&(a1o==aao)){
ind=x;
}
}
return ind;
},removeAdvice:function(_296,_297,_298,once){
var arr=this.getArr(_298);
var ind=this.hasAdvice(_296,_297,_298,arr);
if(ind==-1){
return false;
}
while(ind!=-1){
arr.splice(ind,1);
if(once){
break;
}
ind=this.hasAdvice(_296,_297,_298,arr);
}
return true;
}});
dojo.provide("dojo.event.topic");
dojo.event.topic=new function(){
this.topics={};
this.getTopic=function(_29c){
if(!this.topics[_29c]){
this.topics[_29c]=new this.TopicImpl(_29c);
}
return this.topics[_29c];
};
this.registerPublisher=function(_29d,obj,_29f){
var _29d=this.getTopic(_29d);
_29d.registerPublisher(obj,_29f);
};
this.subscribe=function(_2a0,obj,_2a2){
var _2a0=this.getTopic(_2a0);
_2a0.subscribe(obj,_2a2);
};
this.unsubscribe=function(_2a3,obj,_2a5){
var _2a3=this.getTopic(_2a3);
_2a3.unsubscribe(obj,_2a5);
};
this.destroy=function(_2a6){
this.getTopic(_2a6).destroy();
delete this.topics[_2a6];
};
this.publishApply=function(_2a7,args){
var _2a7=this.getTopic(_2a7);
_2a7.sendMessage.apply(_2a7,args);
};
this.publish=function(_2a9,_2aa){
var _2a9=this.getTopic(_2a9);
var args=[];
for(var x=1;x<arguments.length;x++){
args.push(arguments[x]);
}
_2a9.sendMessage.apply(_2a9,args);
};
};
dojo.event.topic.TopicImpl=function(_2ad){
this.topicName=_2ad;
this.subscribe=function(_2ae,_2af){
var tf=_2af||_2ae;
var to=(!_2af)?dj_global:_2ae;
return dojo.event.kwConnect({srcObj:this,srcFunc:"sendMessage",adviceObj:to,adviceFunc:tf});
};
this.unsubscribe=function(_2b2,_2b3){
var tf=(!_2b3)?_2b2:_2b3;
var to=(!_2b3)?null:_2b2;
return dojo.event.kwDisconnect({srcObj:this,srcFunc:"sendMessage",adviceObj:to,adviceFunc:tf});
};
this._getJoinPoint=function(){
return dojo.event.MethodJoinPoint.getForMethod(this,"sendMessage");
};
this.setSquelch=function(_2b6){
this._getJoinPoint().squelch=_2b6;
};
this.destroy=function(){
this._getJoinPoint().disconnect();
};
this.registerPublisher=function(_2b7,_2b8){
dojo.event.connect(_2b7,_2b8,this,"sendMessage");
};
this.sendMessage=function(_2b9){
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
this.clobber=function(_2bc){
var na;
var tna;
if(_2bc){
tna=_2bc.all||_2bc.getElementsByTagName("*");
na=[_2bc];
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
var _2c0={};
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
var _2c5=0;
this.normalizedEventName=function(_2c6){
switch(_2c6){
case "CheckboxStateChange":
case "DOMAttrModified":
case "DOMMenuItemActive":
case "DOMMenuItemInactive":
case "DOMMouseScroll":
case "DOMNodeInserted":
case "DOMNodeRemoved":
case "RadioStateChange":
return _2c6;
break;
default:
return _2c6.toLowerCase();
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
this.addClobberNodeAttrs=function(node,_2ca){
if(!dojo.render.html.ie){
return;
}
this.addClobberNode(node);
for(var x=0;x<_2ca.length;x++){
node.__clobberAttrs__.push(_2ca[x]);
}
};
this.removeListener=function(node,_2cd,fp,_2cf){
if(!_2cf){
var _2cf=false;
}
_2cd=dojo.event.browser.normalizedEventName(_2cd);
if((_2cd=="onkey")||(_2cd=="key")){
if(dojo.render.html.ie){
this.removeListener(node,"onkeydown",fp,_2cf);
}
_2cd="onkeypress";
}
if(_2cd.substr(0,2)=="on"){
_2cd=_2cd.substr(2);
}
if(node.removeEventListener){
node.removeEventListener(_2cd,fp,_2cf);
}
};
this.addListener=function(node,_2d1,fp,_2d3,_2d4){
if(!node){
return;
}
if(!_2d3){
var _2d3=false;
}
_2d1=dojo.event.browser.normalizedEventName(_2d1);
if((_2d1=="onkey")||(_2d1=="key")){
if(dojo.render.html.ie){
this.addListener(node,"onkeydown",fp,_2d3,_2d4);
}
_2d1="onkeypress";
}
if(_2d1.substr(0,2)!="on"){
_2d1="on"+_2d1;
}
if(!_2d4){
var _2d5=function(evt){
if(!evt){
evt=window.event;
}
var ret=fp(dojo.event.browser.fixEvent(evt,this));
if(_2d3){
dojo.event.browser.stopEvent(evt);
}
return ret;
};
}else{
_2d5=fp;
}
if(node.addEventListener){
node.addEventListener(_2d1.substr(2),_2d5,_2d3);
return _2d5;
}else{
if(typeof node[_2d1]=="function"){
var _2d8=node[_2d1];
node[_2d1]=function(e){
_2d8(e);
return _2d5(e);
};
}else{
node[_2d1]=_2d5;
}
if(dojo.render.html.ie){
this.addClobberNodeAttrs(node,[_2d1]);
}
return _2d5;
}
};
this.isEvent=function(obj){
return (typeof obj!="undefined")&&(obj)&&(typeof Event!="undefined")&&(obj.eventPhase);
};
this.currentEvent=null;
this.callListener=function(_2db,_2dc){
if(typeof _2db!="function"){
dojo.raise("listener not a function: "+_2db);
}
dojo.event.browser.currentEvent.currentTarget=_2dc;
return _2db.call(_2dc,dojo.event.browser.currentEvent);
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
this.fixEvent=function(evt,_2df){
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
var _2e1=evt.keyCode;
if(_2e1>=65&&_2e1<=90&&evt.shiftKey==false){
_2e1+=32;
}
if(_2e1>=1&&_2e1<=26&&evt.ctrlKey){
_2e1+=96;
}
evt.key=String.fromCharCode(_2e1);
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
var _2e1=evt.which;
if((evt.ctrlKey||evt.altKey||evt.metaKey)&&(evt.which>=65&&evt.which<=90&&evt.shiftKey==false)){
_2e1+=32;
}
evt.key=String.fromCharCode(_2e1);
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
evt.currentTarget=(_2df?_2df:evt.srcElement);
}
if(!evt.layerX){
evt.layerX=evt.offsetX;
}
if(!evt.layerY){
evt.layerY=evt.offsetY;
}
var doc=(evt.srcElement&&evt.srcElement.ownerDocument)?evt.srcElement.ownerDocument:document;
var _2e3=((dojo.render.html.ie55)||(doc["compatMode"]=="BackCompat"))?doc.body:doc.documentElement;
if(!evt.pageX){
evt.pageX=evt.clientX+(_2e3.scrollLeft||0);
}
if(!evt.pageY){
evt.pageY=evt.clientY+(_2e3.scrollTop||0);
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
dojo.provide("dojo.widget.Manager");
dojo.widget.manager=new function(){
this.widgets=[];
this.widgetIds=[];
this.topWidgets={};
var _2e5={};
var _2e6=[];
this.getUniqueId=function(_2e7){
var _2e8;
do{
_2e8=_2e7+"_"+(_2e5[_2e7]!=undefined?++_2e5[_2e7]:_2e5[_2e7]=0);
}while(this.getWidgetById(_2e8));
return _2e8;
};
this.add=function(_2e9){
this.widgets.push(_2e9);
if(!_2e9.extraArgs["id"]){
_2e9.extraArgs["id"]=_2e9.extraArgs["ID"];
}
if(_2e9.widgetId==""){
if(_2e9["id"]){
_2e9.widgetId=_2e9["id"];
}else{
if(_2e9.extraArgs["id"]){
_2e9.widgetId=_2e9.extraArgs["id"];
}else{
_2e9.widgetId=this.getUniqueId(_2e9.ns+"_"+_2e9.widgetType);
}
}
}
if(this.widgetIds[_2e9.widgetId]){
dojo.debug("widget ID collision on ID: "+_2e9.widgetId);
}
this.widgetIds[_2e9.widgetId]=_2e9;
};
this.destroyAll=function(){
for(var x=this.widgets.length-1;x>=0;x--){
try{
this.widgets[x].destroy(true);
delete this.widgets[x];
}
catch(e){
}
}
};
this.remove=function(_2eb){
if(dojo.lang.isNumber(_2eb)){
var tw=this.widgets[_2eb].widgetId;
delete this.widgetIds[tw];
this.widgets.splice(_2eb,1);
}else{
this.removeById(_2eb);
}
};
this.removeById=function(id){
if(!dojo.lang.isString(id)){
id=id["widgetId"];
if(!id){
dojo.debug("invalid widget or id passed to removeById");
return;
}
}
for(var i=0;i<this.widgets.length;i++){
if(this.widgets[i].widgetId==id){
this.remove(i);
break;
}
}
};
this.getWidgetById=function(id){
if(dojo.lang.isString(id)){
return this.widgetIds[id];
}
return id;
};
this.getWidgetsByType=function(type){
var lt=type.toLowerCase();
var _2f2=(type.indexOf(":")<0?function(x){
return x.widgetType.toLowerCase();
}:function(x){
return x.getNamespacedType();
});
var ret=[];
dojo.lang.forEach(this.widgets,function(x){
if(_2f2(x)==lt){
ret.push(x);
}
});
return ret;
};
this.getWidgetsByFilter=function(_2f7,_2f8){
var ret=[];
dojo.lang.every(this.widgets,function(x){
if(_2f7(x)){
ret.push(x);
if(_2f8){
return false;
}
}
return true;
});
return (_2f8?ret[0]:ret);
};
this.getAllWidgets=function(){
return this.widgets.concat();
};
this.getWidgetByNode=function(node){
var w=this.getAllWidgets();
node=dojo.byId(node);
for(var i=0;i<w.length;i++){
if(w[i].domNode==node){
return w[i];
}
}
return null;
};
this.byId=this.getWidgetById;
this.byType=this.getWidgetsByType;
this.byFilter=this.getWidgetsByFilter;
this.byNode=this.getWidgetByNode;
var _2fe={};
var _2ff=["dojo.widget"];
for(var i=0;i<_2ff.length;i++){
_2ff[_2ff[i]]=true;
}
this.registerWidgetPackage=function(_301){
if(!_2ff[_301]){
_2ff[_301]=true;
_2ff.push(_301);
}
};
this.getWidgetPackageList=function(){
return dojo.lang.map(_2ff,function(elt){
return (elt!==true?elt:undefined);
});
};
this.getImplementation=function(_303,_304,_305,ns){
var impl=this.getImplementationName(_303,ns);
if(impl){
var ret=_304?new impl(_304):new impl();
return ret;
}
};
function buildPrefixCache(){
for(var _309 in dojo.render){
if(dojo.render[_309]["capable"]===true){
var _30a=dojo.render[_309].prefixes;
for(var i=0;i<_30a.length;i++){
_2e6.push(_30a[i].toLowerCase());
}
}
}
}
var _30c=function(_30d,_30e){
if(!_30e){
return null;
}
for(var i=0,l=_2e6.length,_311;i<=l;i++){
_311=(i<l?_30e[_2e6[i]]:_30e);
if(!_311){
continue;
}
for(var name in _311){
if(name.toLowerCase()==_30d){
return _311[name];
}
}
}
return null;
};
var _313=function(_314,_315){
var _316=dojo.evalObjPath(_315,false);
return (_316?_30c(_314,_316):null);
};
this.getImplementationName=function(_317,ns){
var _319=_317.toLowerCase();
ns=ns||"dojo";
var imps=_2fe[ns]||(_2fe[ns]={});
var impl=imps[_319];
if(impl){
return impl;
}
if(!_2e6.length){
buildPrefixCache();
}
var _31c=dojo.ns.get(ns);
if(!_31c){
dojo.ns.register(ns,ns+".widget");
_31c=dojo.ns.get(ns);
}
if(_31c){
_31c.resolve(_317);
}
impl=_313(_319,_31c.module);
if(impl){
return (imps[_319]=impl);
}
_31c=dojo.ns.require(ns);
if((_31c)&&(_31c.resolver)){
_31c.resolve(_317);
impl=_313(_319,_31c.module);
if(impl){
return (imps[_319]=impl);
}
}
dojo.deprecated("dojo.widget.Manager.getImplementationName","Could not locate widget implementation for \""+_317+"\" in \""+_31c.module+"\" registered to namespace \""+_31c.name+"\". "+"Developers must specify correct namespaces for all non-Dojo widgets","0.5");
for(var i=0;i<_2ff.length;i++){
impl=_313(_319,_2ff[i]);
if(impl){
return (imps[_319]=impl);
}
}
throw new Error("Could not locate widget implementation for \""+_317+"\" in \""+_31c.module+"\" registered to namespace \""+_31c.name+"\"");
};
this.resizing=false;
this.onWindowResized=function(){
if(this.resizing){
return;
}
try{
this.resizing=true;
for(var id in this.topWidgets){
var _31f=this.topWidgets[id];
if(_31f.checkSize){
_31f.checkSize();
}
}
}
catch(e){
}
finally{
this.resizing=false;
}
};
if(typeof window!="undefined"){
dojo.addOnLoad(this,"onWindowResized");
dojo.event.connect(window,"onresize",this,"onWindowResized");
}
};
(function(){
var dw=dojo.widget;
var dwm=dw.manager;
var h=dojo.lang.curry(dojo.lang,"hitch",dwm);
var g=function(_324,_325){
dw[(_325||_324)]=h(_324);
};
g("add","addWidget");
g("destroyAll","destroyAllWidgets");
g("remove","removeWidget");
g("removeById","removeWidgetById");
g("getWidgetById");
g("getWidgetById","byId");
g("getWidgetsByType");
g("getWidgetsByFilter");
g("getWidgetsByType","byType");
g("getWidgetsByFilter","byFilter");
g("getWidgetByNode","byNode");
dw.all=function(n){
var _327=dwm.getAllWidgets.apply(dwm,arguments);
if(arguments.length>0){
return _327[n];
}
return _327;
};
g("registerWidgetPackage");
g("getImplementation","getWidgetImplementation");
g("getImplementationName","getWidgetImplementationName");
dw.widgets=dwm.widgets;
dw.widgetIds=dwm.widgetIds;
dw.root=dwm.root;
})();
dojo.provide("dojo.uri.Uri");
dojo.uri=new function(){
this.dojoUri=function(uri){
return new dojo.uri.Uri(dojo.hostenv.getBaseScriptUri(),uri);
};
this.moduleUri=function(_329,uri){
var loc=dojo.hostenv.getModuleSymbols(_329).join("/");
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
var _32e=new dojo.uri.Uri(arguments[i].toString());
var _32f=new dojo.uri.Uri(uri.toString());
if((_32e.path=="")&&(_32e.scheme==null)&&(_32e.authority==null)&&(_32e.query==null)){
if(_32e.fragment!=null){
_32f.fragment=_32e.fragment;
}
_32e=_32f;
}else{
if(_32e.scheme==null){
_32e.scheme=_32f.scheme;
if(_32e.authority==null){
_32e.authority=_32f.authority;
if(_32e.path.charAt(0)!="/"){
var path=_32f.path.substring(0,_32f.path.lastIndexOf("/")+1)+_32e.path;
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
_32e.path=segs.join("/");
}
}
}
}
uri="";
if(_32e.scheme!=null){
uri+=_32e.scheme+":";
}
if(_32e.authority!=null){
uri+="//"+_32e.authority;
}
uri+=_32e.path;
if(_32e.query!=null){
uri+="?"+_32e.query;
}
if(_32e.fragment!=null){
uri+="#"+_32e.fragment;
}
}
this.uri=uri.toString();
var _333="^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?$";
var r=this.uri.match(new RegExp(_333));
this.scheme=r[2]||(r[1]?"":null);
this.authority=r[4]||(r[3]?"":null);
this.path=r[5];
this.query=r[7]||(r[6]?"":null);
this.fragment=r[9]||(r[8]?"":null);
if(this.authority!=null){
_333="^((([^:]+:)?([^@]+))@)?([^:]*)(:([0-9]+))?$";
r=this.authority.match(new RegExp(_333));
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
dojo.provide("dojo.uri.*");
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
var _337=dojo.global();
var _338=dojo.doc();
var w=0;
var h=0;
if(dojo.render.html.mozilla){
w=_338.documentElement.clientWidth;
h=_337.innerHeight;
}else{
if(!dojo.render.html.opera&&_337.innerWidth){
w=_337.innerWidth;
h=_337.innerHeight;
}else{
if(!dojo.render.html.opera&&dojo.exists(_338,"documentElement.clientWidth")){
var w2=_338.documentElement.clientWidth;
if(!w||w2&&w2<w){
w=w2;
}
h=_338.documentElement.clientHeight;
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
var _33c=dojo.global();
var _33d=dojo.doc();
var top=_33c.pageYOffset||_33d.documentElement.scrollTop||dojo.body().scrollTop||0;
var left=_33c.pageXOffset||_33d.documentElement.scrollLeft||dojo.body().scrollLeft||0;
return {top:top,left:left,offset:{x:left,y:top}};
};
dojo.html.getParentByType=function(node,type){
var _342=dojo.doc();
var _343=dojo.byId(node);
type=type.toLowerCase();
while((_343)&&(_343.nodeName.toLowerCase()!=type)){
if(_343==(_342["body"]||_342["documentElement"])){
return null;
}
_343=_343.parentNode;
}
return _343;
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
var _34b={x:0,y:0};
if(e.pageX||e.pageY){
_34b.x=e.pageX;
_34b.y=e.pageY;
}else{
var de=dojo.doc().documentElement;
var db=dojo.body();
_34b.x=e.clientX+((de||db)["scrollLeft"])-((de||db)["clientLeft"]);
_34b.y=e.clientY+((de||db)["scrollTop"])-((de||db)["clientTop"]);
}
return _34b;
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
var _350=dojo.doc().createElement("script");
_350.src="javascript:'dojo.html.createExternalElement=function(doc, tag){ return doc.createElement(tag); }'";
dojo.doc().getElementsByTagName("head")[0].appendChild(_350);
})();
}
}else{
dojo.html.createExternalElement=function(doc,tag){
return doc.createElement(tag);
};
}
dojo.html._callDeprecated=function(_353,_354,args,_356,_357){
dojo.deprecated("dojo.html."+_353,"replaced by dojo.html."+_354+"("+(_356?"node, {"+_356+": "+_356+"}":"")+")"+(_357?"."+_357:""),"0.5");
var _358=[];
if(_356){
var _359={};
_359[_356]=args[1];
_358.push(args[0]);
_358.push(_359);
}else{
_358=args;
}
var ret=dojo.html[_354].apply(dojo.html,args);
if(_357){
return ret[_357];
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
dojo.provide("dojo.a11y");
dojo.a11y={imgPath:dojo.uri.dojoUri("src/widget/templates/images"),doAccessibleCheck:true,accessible:null,checkAccessible:function(){
if(this.accessible===null){
this.accessible=false;
if(this.doAccessibleCheck==true){
this.accessible=this.testAccessible();
}
}
return this.accessible;
},testAccessible:function(){
this.accessible=false;
if(dojo.render.html.ie||dojo.render.html.mozilla){
var div=document.createElement("div");
div.style.backgroundImage="url(\""+this.imgPath+"/tab_close.gif\")";
dojo.body().appendChild(div);
var _35c=null;
if(window.getComputedStyle){
var _35d=getComputedStyle(div,"");
_35c=_35d.getPropertyValue("background-image");
}else{
_35c=div.currentStyle.backgroundImage;
}
var _35e=false;
if(_35c!=null&&(_35c=="none"||_35c=="url(invalid-url:)")){
this.accessible=true;
}
dojo.body().removeChild(div);
}
return this.accessible;
},setCheckAccessible:function(_35f){
this.doAccessibleCheck=_35f;
},setAccessibleMode:function(){
if(this.accessible===null){
if(this.checkAccessible()){
dojo.render.html.prefixes.unshift("a11y");
}
}
return this.accessible;
}};
dojo.provide("dojo.widget.Widget");
dojo.declare("dojo.widget.Widget",null,function(){
this.children=[];
this.extraArgs={};
},{parent:null,isTopLevel:false,disabled:false,isContainer:false,widgetId:"",widgetType:"Widget",ns:"dojo",getNamespacedType:function(){
return (this.ns?this.ns+":"+this.widgetType:this.widgetType).toLowerCase();
},toString:function(){
return "[Widget "+this.getNamespacedType()+", "+(this.widgetId||"NO ID")+"]";
},repr:function(){
return this.toString();
},enable:function(){
this.disabled=false;
},disable:function(){
this.disabled=true;
},onResized:function(){
this.notifyChildrenOfResize();
},notifyChildrenOfResize:function(){
for(var i=0;i<this.children.length;i++){
var _361=this.children[i];
if(_361.onResized){
_361.onResized();
}
}
},create:function(args,_363,_364,ns){
if(ns){
this.ns=ns;
}
this.satisfyPropertySets(args,_363,_364);
this.mixInProperties(args,_363,_364);
this.postMixInProperties(args,_363,_364);
dojo.widget.manager.add(this);
this.buildRendering(args,_363,_364);
this.initialize(args,_363,_364);
this.postInitialize(args,_363,_364);
this.postCreate(args,_363,_364);
return this;
},destroy:function(_366){
if(this.parent){
this.parent.removeChild(this);
}
this.destroyChildren();
this.uninitialize();
this.destroyRendering(_366);
dojo.widget.manager.removeById(this.widgetId);
},destroyChildren:function(){
var _367;
var i=0;
while(this.children.length>i){
_367=this.children[i];
if(_367 instanceof dojo.widget.Widget){
this.removeChild(_367);
_367.destroy();
continue;
}
i++;
}
},getChildrenOfType:function(type,_36a){
var ret=[];
var _36c=dojo.lang.isFunction(type);
if(!_36c){
type=type.toLowerCase();
}
for(var x=0;x<this.children.length;x++){
if(_36c){
if(this.children[x] instanceof type){
ret.push(this.children[x]);
}
}else{
if(this.children[x].widgetType.toLowerCase()==type){
ret.push(this.children[x]);
}
}
if(_36a){
ret=ret.concat(this.children[x].getChildrenOfType(type,_36a));
}
}
return ret;
},getDescendants:function(){
var _36e=[];
var _36f=[this];
var elem;
while((elem=_36f.pop())){
_36e.push(elem);
if(elem.children){
dojo.lang.forEach(elem.children,function(elem){
_36f.push(elem);
});
}
}
return _36e;
},isFirstChild:function(){
return this===this.parent.children[0];
},isLastChild:function(){
return this===this.parent.children[this.parent.children.length-1];
},satisfyPropertySets:function(args){
return args;
},mixInProperties:function(args,frag){
if((args["fastMixIn"])||(frag["fastMixIn"])){
for(var x in args){
this[x]=args[x];
}
return;
}
var _376;
var _377=dojo.widget.lcArgsCache[this.widgetType];
if(_377==null){
_377={};
for(var y in this){
_377[((new String(y)).toLowerCase())]=y;
}
dojo.widget.lcArgsCache[this.widgetType]=_377;
}
var _379={};
for(var x in args){
if(!this[x]){
var y=_377[(new String(x)).toLowerCase()];
if(y){
args[y]=args[x];
x=y;
}
}
if(_379[x]){
continue;
}
_379[x]=true;
if((typeof this[x])!=(typeof _376)){
if(typeof args[x]!="string"){
this[x]=args[x];
}else{
if(dojo.lang.isString(this[x])){
this[x]=args[x];
}else{
if(dojo.lang.isNumber(this[x])){
this[x]=new Number(args[x]);
}else{
if(dojo.lang.isBoolean(this[x])){
this[x]=(args[x].toLowerCase()=="false")?false:true;
}else{
if(dojo.lang.isFunction(this[x])){
if(args[x].search(/[^\w\.]+/i)==-1){
this[x]=dojo.evalObjPath(args[x],false);
}else{
var tn=dojo.lang.nameAnonFunc(new Function(args[x]),this);
dojo.event.kwConnect({srcObj:this,srcFunc:x,adviceObj:this,adviceFunc:tn});
}
}else{
if(dojo.lang.isArray(this[x])){
this[x]=args[x].split(";");
}else{
if(this[x] instanceof Date){
this[x]=new Date(Number(args[x]));
}else{
if(typeof this[x]=="object"){
if(this[x] instanceof dojo.uri.Uri){
this[x]=dojo.uri.dojoUri(args[x]);
}else{
var _37b=args[x].split(";");
for(var y=0;y<_37b.length;y++){
var si=_37b[y].indexOf(":");
if((si!=-1)&&(_37b[y].length>si)){
this[x][_37b[y].substr(0,si).replace(/^\s+|\s+$/g,"")]=_37b[y].substr(si+1);
}
}
}
}else{
this[x]=args[x];
}
}
}
}
}
}
}
}
}else{
this.extraArgs[x.toLowerCase()]=args[x];
}
}
},postMixInProperties:function(args,frag,_37f){
},initialize:function(args,frag,_382){
return false;
},postInitialize:function(args,frag,_385){
return false;
},postCreate:function(args,frag,_388){
return false;
},uninitialize:function(){
return false;
},buildRendering:function(args,frag,_38b){
dojo.unimplemented("dojo.widget.Widget.buildRendering, on "+this.toString()+", ");
return false;
},destroyRendering:function(){
dojo.unimplemented("dojo.widget.Widget.destroyRendering");
return false;
},addedTo:function(_38c){
},addChild:function(_38d){
dojo.unimplemented("dojo.widget.Widget.addChild");
return false;
},removeChild:function(_38e){
for(var x=0;x<this.children.length;x++){
if(this.children[x]===_38e){
this.children.splice(x,1);
_38e.parent=null;
break;
}
}
return _38e;
},getPreviousSibling:function(){
var idx=this.getParentIndex();
if(idx<=0){
return null;
}
return this.parent.children[idx-1];
},getSiblings:function(){
return this.parent.children;
},getParentIndex:function(){
return dojo.lang.indexOf(this.parent.children,this,true);
},getNextSibling:function(){
var idx=this.getParentIndex();
if(idx==this.parent.children.length-1){
return null;
}
if(idx<0){
return null;
}
return this.parent.children[idx+1];
}});
dojo.widget.lcArgsCache={};
dojo.widget.tags={};
dojo.widget.tags.addParseTreeHandler=function(type){
dojo.deprecated("addParseTreeHandler",". ParseTreeHandlers are now reserved for components. Any unfiltered DojoML tag without a ParseTreeHandler is assumed to be a widget","0.5");
};
dojo.widget.tags["dojo:propertyset"]=function(_393,_394,_395){
var _396=_394.parseProperties(_393["dojo:propertyset"]);
};
dojo.widget.tags["dojo:connect"]=function(_397,_398,_399){
var _39a=_398.parseProperties(_397["dojo:connect"]);
};
dojo.widget.buildWidgetFromParseTree=function(type,frag,_39d,_39e,_39f,_3a0){
dojo.a11y.setAccessibleMode();
var _3a1=type.split(":");
_3a1=(_3a1.length==2)?_3a1[1]:type;
var _3a2=_3a0||_39d.parseProperties(frag[frag["ns"]+":"+_3a1]);
var _3a3=dojo.widget.manager.getImplementation(_3a1,null,null,frag["ns"]);
if(!_3a3){
throw new Error("cannot find \""+type+"\" widget");
}else{
if(!_3a3.create){
throw new Error("\""+type+"\" widget object has no \"create\" method and does not appear to implement *Widget");
}
}
_3a2["dojoinsertionindex"]=_39f;
var ret=_3a3.create(_3a2,frag,_39e,frag["ns"]);
return ret;
};
dojo.widget.defineWidget=function(_3a5,_3a6,_3a7,init,_3a9){
if(dojo.lang.isString(arguments[3])){
dojo.widget._defineWidget(arguments[0],arguments[3],arguments[1],arguments[4],arguments[2]);
}else{
var args=[arguments[0]],p=3;
if(dojo.lang.isString(arguments[1])){
args.push(arguments[1],arguments[2]);
}else{
args.push("",arguments[1]);
p=2;
}
if(dojo.lang.isFunction(arguments[p])){
args.push(arguments[p],arguments[p+1]);
}else{
args.push(null,arguments[p]);
}
dojo.widget._defineWidget.apply(this,args);
}
};
dojo.widget.defineWidget.renderers="html|svg|vml";
dojo.widget._defineWidget=function(_3ac,_3ad,_3ae,init,_3b0){
var _3b1=_3ac.split(".");
var type=_3b1.pop();
var regx="\\.("+(_3ad?_3ad+"|":"")+dojo.widget.defineWidget.renderers+")\\.";
var r=_3ac.search(new RegExp(regx));
_3b1=(r<0?_3b1.join("."):_3ac.substr(0,r));
dojo.widget.manager.registerWidgetPackage(_3b1);
var pos=_3b1.indexOf(".");
var _3b6=(pos>-1)?_3b1.substring(0,pos):_3b1;
_3b0=(_3b0)||{};
_3b0.widgetType=type;
if((!init)&&(_3b0["classConstructor"])){
init=_3b0.classConstructor;
delete _3b0.classConstructor;
}
dojo.declare(_3ac,_3ae,init,_3b0);
};
dojo.provide("dojo.widget.Parse");
dojo.widget.Parse=function(_3b7){
this.propertySetsList=[];
this.fragment=_3b7;
this.createComponents=function(frag,_3b9){
var _3ba=[];
var _3bb=false;
try{
if(frag&&frag.tagName&&(frag!=frag.nodeRef)){
var _3bc=dojo.widget.tags;
var tna=String(frag.tagName).split(";");
for(var x=0;x<tna.length;x++){
var ltn=tna[x].replace(/^\s+|\s+$/g,"").toLowerCase();
frag.tagName=ltn;
var ret;
if(_3bc[ltn]){
_3bb=true;
ret=_3bc[ltn](frag,this,_3b9,frag.index);
_3ba.push(ret);
}else{
if(ltn.indexOf(":")==-1){
ltn="dojo:"+ltn;
}
ret=dojo.widget.buildWidgetFromParseTree(ltn,frag,this,_3b9,frag.index);
if(ret){
_3bb=true;
_3ba.push(ret);
}
}
}
}
}
catch(e){
dojo.debug("dojo.widget.Parse: error:",e);
}
if(!_3bb){
_3ba=_3ba.concat(this.createSubComponents(frag,_3b9));
}
return _3ba;
};
this.createSubComponents=function(_3c1,_3c2){
var frag,_3c4=[];
for(var item in _3c1){
frag=_3c1[item];
if(frag&&typeof frag=="object"&&(frag!=_3c1.nodeRef)&&(frag!=_3c1.tagName)&&(!dojo.dom.isNode(frag))){
_3c4=_3c4.concat(this.createComponents(frag,_3c2));
}
}
return _3c4;
};
this.parsePropertySets=function(_3c6){
return [];
};
this.parseProperties=function(_3c7){
var _3c8={};
for(var item in _3c7){
if((_3c7[item]==_3c7.tagName)||(_3c7[item]==_3c7.nodeRef)){
}else{
var frag=_3c7[item];
if(frag.tagName&&dojo.widget.tags[frag.tagName.toLowerCase()]){
}else{
if(frag[0]&&frag[0].value!=""&&frag[0].value!=null){
try{
if(item.toLowerCase()=="dataprovider"){
var _3cb=this;
this.getDataProvider(_3cb,frag[0].value);
_3c8.dataProvider=this.dataProvider;
}
_3c8[item]=frag[0].value;
var _3cc=this.parseProperties(frag);
for(var _3cd in _3cc){
_3c8[_3cd]=_3cc[_3cd];
}
}
catch(e){
dojo.debug(e);
}
}
}
switch(item.toLowerCase()){
case "checked":
case "disabled":
if(typeof _3c8[item]!="boolean"){
_3c8[item]=true;
}
break;
}
}
}
return _3c8;
};
this.getDataProvider=function(_3ce,_3cf){
dojo.io.bind({url:_3cf,load:function(type,_3d1){
if(type=="load"){
_3ce.dataProvider=_3d1;
}
},mimetype:"text/javascript",sync:true});
};
this.getPropertySetById=function(_3d2){
for(var x=0;x<this.propertySetsList.length;x++){
if(_3d2==this.propertySetsList[x]["id"][0].value){
return this.propertySetsList[x];
}
}
return "";
};
this.getPropertySetsByType=function(_3d4){
var _3d5=[];
for(var x=0;x<this.propertySetsList.length;x++){
var cpl=this.propertySetsList[x];
var cpcc=cpl.componentClass||cpl.componentType||null;
var _3d9=this.propertySetsList[x]["id"][0].value;
if(cpcc&&(_3d9==cpcc[0].value)){
_3d5.push(cpl);
}
}
return _3d5;
};
this.getPropertySets=function(_3da){
var ppl="dojo:propertyproviderlist";
var _3dc=[];
var _3dd=_3da.tagName;
if(_3da[ppl]){
var _3de=_3da[ppl].value.split(" ");
for(var _3df in _3de){
if((_3df.indexOf("..")==-1)&&(_3df.indexOf("://")==-1)){
var _3e0=this.getPropertySetById(_3df);
if(_3e0!=""){
_3dc.push(_3e0);
}
}else{
}
}
}
return this.getPropertySetsByType(_3dd).concat(_3dc);
};
this.createComponentFromScript=function(_3e1,_3e2,_3e3,ns){
_3e3.fastMixIn=true;
var ltn=(ns||"dojo")+":"+_3e2.toLowerCase();
if(dojo.widget.tags[ltn]){
return [dojo.widget.tags[ltn](_3e3,this,null,null,_3e3)];
}
return [dojo.widget.buildWidgetFromParseTree(ltn,_3e3,this,null,null,_3e3)];
};
};
dojo.widget._parser_collection={"dojo":new dojo.widget.Parse()};
dojo.widget.getParser=function(name){
if(!name){
name="dojo";
}
if(!this._parser_collection[name]){
this._parser_collection[name]=new dojo.widget.Parse();
}
return this._parser_collection[name];
};
dojo.widget.createWidget=function(name,_3e8,_3e9,_3ea){
var _3eb=false;
var _3ec=(typeof name=="string");
if(_3ec){
var pos=name.indexOf(":");
var ns=(pos>-1)?name.substring(0,pos):"dojo";
if(pos>-1){
name=name.substring(pos+1);
}
var _3ef=name.toLowerCase();
var _3f0=ns+":"+_3ef;
_3eb=(dojo.byId(name)&&!dojo.widget.tags[_3f0]);
}
if((arguments.length==1)&&(_3eb||!_3ec)){
var xp=new dojo.xml.Parse();
var tn=_3eb?dojo.byId(name):name;
return dojo.widget.getParser().createComponents(xp.parseElement(tn,null,true))[0];
}
function fromScript(_3f3,name,_3f5,ns){
_3f5[_3f0]={dojotype:[{value:_3ef}],nodeRef:_3f3,fastMixIn:true};
_3f5.ns=ns;
return dojo.widget.getParser().createComponentFromScript(_3f3,name,_3f5,ns);
}
_3e8=_3e8||{};
var _3f7=false;
var tn=null;
var h=dojo.render.html.capable;
if(h){
tn=document.createElement("span");
}
if(!_3e9){
_3f7=true;
_3e9=tn;
if(h){
dojo.body().appendChild(_3e9);
}
}else{
if(_3ea){
dojo.dom.insertAtPosition(tn,_3e9,_3ea);
}else{
tn=_3e9;
}
}
var _3f9=fromScript(tn,name.toLowerCase(),_3e8,ns);
if((!_3f9)||(!_3f9[0])||(typeof _3f9[0].widgetType=="undefined")){
throw new Error("createWidget: Creation of \""+name+"\" widget failed.");
}
try{
if(_3f7&&_3f9[0].domNode.parentNode){
_3f9[0].domNode.parentNode.removeChild(_3f9[0].domNode);
}
}
catch(e){
dojo.debug(e);
}
return _3f9[0];
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
dojo.html.hasClass=function(node,_3ff){
return (new RegExp("(^|\\s+)"+_3ff+"(\\s+|$)")).test(dojo.html.getClass(node));
};
dojo.html.prependClass=function(node,_401){
_401+=" "+dojo.html.getClass(node);
return dojo.html.setClass(node,_401);
};
dojo.html.addClass=function(node,_403){
if(dojo.html.hasClass(node,_403)){
return false;
}
_403=(dojo.html.getClass(node)+" "+_403).replace(/^\s+|\s+$/g,"");
return dojo.html.setClass(node,_403);
};
dojo.html.setClass=function(node,_405){
node=dojo.byId(node);
var cs=new String(_405);
try{
if(typeof node.className=="string"){
node.className=cs;
}else{
if(node.setAttribute){
node.setAttribute("class",_405);
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
dojo.html.removeClass=function(node,_408,_409){
try{
if(!_409){
var _40a=dojo.html.getClass(node).replace(new RegExp("(^|\\s+)"+_408+"(\\s+|$)"),"$1$2");
}else{
var _40a=dojo.html.getClass(node).replace(_408,"");
}
dojo.html.setClass(node,_40a);
}
catch(e){
dojo.debug("dojo.html.removeClass() failed",e);
}
return true;
};
dojo.html.replaceClass=function(node,_40c,_40d){
dojo.html.removeClass(node,_40d);
dojo.html.addClass(node,_40c);
};
dojo.html.classMatchType={ContainsAll:0,ContainsAny:1,IsOnly:2};
dojo.html.getElementsByClass=function(_40e,_40f,_410,_411,_412){
_412=false;
var _413=dojo.doc();
_40f=dojo.byId(_40f)||_413;
var _414=_40e.split(/\s+/g);
var _415=[];
if(_411!=1&&_411!=2){
_411=0;
}
var _416=new RegExp("(\\s|^)(("+_414.join(")|(")+"))(\\s|$)");
var _417=_414.join(" ").length;
var _418=[];
if(!_412&&_413.evaluate){
var _419=".//"+(_410||"*")+"[contains(";
if(_411!=dojo.html.classMatchType.ContainsAny){
_419+="concat(' ',@class,' '), ' "+_414.join(" ') and contains(concat(' ',@class,' '), ' ")+" ')";
if(_411==2){
_419+=" and string-length(@class)="+_417+"]";
}else{
_419+="]";
}
}else{
_419+="concat(' ',@class,' '), ' "+_414.join(" ') or contains(concat(' ',@class,' '), ' ")+" ')]";
}
var _41a=_413.evaluate(_419,_40f,null,XPathResult.ANY_TYPE,null);
var _41b=_41a.iterateNext();
while(_41b){
try{
_418.push(_41b);
_41b=_41a.iterateNext();
}
catch(e){
break;
}
}
return _418;
}else{
if(!_410){
_410="*";
}
_418=_40f.getElementsByTagName(_410);
var node,i=0;
outer:
while(node=_418[i++]){
var _41e=dojo.html.getClasses(node);
if(_41e.length==0){
continue outer;
}
var _41f=0;
for(var j=0;j<_41e.length;j++){
if(_416.test(_41e[j])){
if(_411==dojo.html.classMatchType.ContainsAny){
_415.push(node);
continue outer;
}else{
_41f++;
}
}else{
if(_411==dojo.html.classMatchType.IsOnly){
continue outer;
}
}
}
if(_41f==_414.length){
if((_411==dojo.html.classMatchType.IsOnly)&&(_41f==_41e.length)){
_415.push(node);
}else{
if(_411==dojo.html.classMatchType.ContainsAll){
_415.push(node);
}
}
}
}
return _415;
}
};
dojo.html.getElementsByClassName=dojo.html.getElementsByClass;
dojo.html.toCamelCase=function(_421){
var arr=_421.split("-"),cc=arr[0];
for(var i=1;i<arr.length;i++){
cc+=arr[i].charAt(0).toUpperCase()+arr[i].substring(1);
}
return cc;
};
dojo.html.toSelectorCase=function(_425){
return _425.replace(/([A-Z])/g,"-$1").toLowerCase();
};
dojo.html.getComputedStyle=function(node,_427,_428){
node=dojo.byId(node);
var _427=dojo.html.toSelectorCase(_427);
var _429=dojo.html.toCamelCase(_427);
if(!node||!node.style){
return _428;
}else{
if(document.defaultView&&dojo.html.isDescendantOf(node,node.ownerDocument)){
try{
var cs=document.defaultView.getComputedStyle(node,"");
if(cs){
return cs.getPropertyValue(_427);
}
}
catch(e){
if(node.style.getPropertyValue){
return node.style.getPropertyValue(_427);
}else{
return _428;
}
}
}else{
if(node.currentStyle){
return node.currentStyle[_429];
}
}
}
if(node.style.getPropertyValue){
return node.style.getPropertyValue(_427);
}else{
return _428;
}
};
dojo.html.getStyleProperty=function(node,_42c){
node=dojo.byId(node);
return (node&&node.style?node.style[dojo.html.toCamelCase(_42c)]:undefined);
};
dojo.html.getStyle=function(node,_42e){
var _42f=dojo.html.getStyleProperty(node,_42e);
return (_42f?_42f:dojo.html.getComputedStyle(node,_42e));
};
dojo.html.setStyle=function(node,_431,_432){
node=dojo.byId(node);
if(node&&node.style){
var _433=dojo.html.toCamelCase(_431);
node.style[_433]=_432;
}
};
dojo.html.setStyleText=function(_434,text){
try{
_434.style.cssText=text;
}
catch(e){
_434.setAttribute("style",text);
}
};
dojo.html.copyStyle=function(_436,_437){
if(!_437.style.cssText){
_436.setAttribute("style",_437.getAttribute("style"));
}else{
_436.style.cssText=_437.style.cssText;
}
dojo.html.addClass(_436,dojo.html.getClass(_437));
};
dojo.html.getUnitValue=function(node,_439,_43a){
var s=dojo.html.getComputedStyle(node,_439);
if((!s)||((s=="auto")&&(_43a))){
return {value:0,units:"px"};
}
var _43c=s.match(/(\-?[\d.]+)([a-z%]*)/i);
if(!_43c){
return dojo.html.getUnitValue.bad;
}
return {value:Number(_43c[1]),units:_43c[2].toLowerCase()};
};
dojo.html.getUnitValue.bad={value:NaN,units:""};
dojo.html.getPixelValue=function(node,_43e,_43f){
var _440=dojo.html.getUnitValue(node,_43e,_43f);
if(isNaN(_440.value)){
return 0;
}
if((_440.value)&&(_440.units!="px")){
return NaN;
}
return _440.value;
};
dojo.html.setPositivePixelValue=function(node,_442,_443){
if(isNaN(_443)){
return false;
}
node.style[_442]=Math.max(0,_443)+"px";
return true;
};
dojo.html.styleSheet=null;
dojo.html.insertCssRule=function(_444,_445,_446){
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
_446=dojo.html.styleSheet.cssRules.length;
}else{
if(dojo.html.styleSheet.rules){
_446=dojo.html.styleSheet.rules.length;
}else{
return null;
}
}
}
if(dojo.html.styleSheet.insertRule){
var rule=_444+" { "+_445+" }";
return dojo.html.styleSheet.insertRule(rule,_446);
}else{
if(dojo.html.styleSheet.addRule){
return dojo.html.styleSheet.addRule(_444,_445,_446);
}else{
return null;
}
}
};
dojo.html.removeCssRule=function(_448){
if(!dojo.html.styleSheet){
dojo.debug("no stylesheet defined for removing rules");
return false;
}
if(dojo.render.html.ie){
if(!_448){
_448=dojo.html.styleSheet.rules.length;
dojo.html.styleSheet.removeRule(_448);
}
}else{
if(document.styleSheets[0]){
if(!_448){
_448=dojo.html.styleSheet.cssRules.length;
}
dojo.html.styleSheet.deleteRule(_448);
}
}
return true;
};
dojo.html._insertedCssFiles=[];
dojo.html.insertCssFile=function(URI,doc,_44b,_44c){
if(!URI){
return;
}
if(!doc){
doc=document;
}
var _44d=dojo.hostenv.getText(URI,false,_44c);
if(_44d===null){
return;
}
_44d=dojo.html.fixPathsInCssText(_44d,URI);
if(_44b){
var idx=-1,node,ent=dojo.html._insertedCssFiles;
for(var i=0;i<ent.length;i++){
if((ent[i].doc==doc)&&(ent[i].cssText==_44d)){
idx=i;
node=ent[i].nodeRef;
break;
}
}
if(node){
var _452=doc.getElementsByTagName("style");
for(var i=0;i<_452.length;i++){
if(_452[i]==node){
return;
}
}
dojo.html._insertedCssFiles.shift(idx,1);
}
}
var _453=dojo.html.insertCssText(_44d,doc);
dojo.html._insertedCssFiles.push({"doc":doc,"cssText":_44d,"nodeRef":_453});
if(_453&&djConfig.isDebug){
_453.setAttribute("dbgHref",URI);
}
return _453;
};
dojo.html.insertCssText=function(_454,doc,URI){
if(!_454){
return;
}
if(!doc){
doc=document;
}
if(URI){
_454=dojo.html.fixPathsInCssText(_454,URI);
}
var _457=doc.createElement("style");
_457.setAttribute("type","text/css");
var head=doc.getElementsByTagName("head")[0];
if(!head){
dojo.debug("No head tag in document, aborting styles");
return;
}else{
head.appendChild(_457);
}
if(_457.styleSheet){
var _459=function(){
try{
_457.styleSheet.cssText=_454;
}
catch(e){
dojo.debug(e);
}
};
if(_457.styleSheet.disabled){
setTimeout(_459,10);
}else{
_459();
}
}else{
var _45a=doc.createTextNode(_454);
_457.appendChild(_45a);
}
return _457;
};
dojo.html.fixPathsInCssText=function(_45b,URI){
if(!_45b||!URI){
return;
}
var _45d,str="",url="",_460="[\\t\\s\\w\\(\\)\\/\\.\\\\'\"-:#=&?~]+";
var _461=new RegExp("url\\(\\s*("+_460+")\\s*\\)");
var _462=/(file|https?|ftps?):\/\//;
regexTrim=new RegExp("^[\\s]*(['\"]?)("+_460+")\\1[\\s]*?$");
if(dojo.render.html.ie55||dojo.render.html.ie60){
var _463=new RegExp("AlphaImageLoader\\((.*)src=['\"]("+_460+")['\"]");
while(_45d=_463.exec(_45b)){
url=_45d[2].replace(regexTrim,"$2");
if(!_462.exec(url)){
url=(new dojo.uri.Uri(URI,url).toString());
}
str+=_45b.substring(0,_45d.index)+"AlphaImageLoader("+_45d[1]+"src='"+url+"'";
_45b=_45b.substr(_45d.index+_45d[0].length);
}
_45b=str+_45b;
str="";
}
while(_45d=_461.exec(_45b)){
url=_45d[1].replace(regexTrim,"$2");
if(!_462.exec(url)){
url=(new dojo.uri.Uri(URI,url).toString());
}
str+=_45b.substring(0,_45d.index)+"url("+url+")";
_45b=_45b.substr(_45d.index+_45d[0].length);
}
return str+_45b;
};
dojo.html.setActiveStyleSheet=function(_464){
var i=0,a,els=dojo.doc().getElementsByTagName("link");
while(a=els[i++]){
if(a.getAttribute("rel").indexOf("style")!=-1&&a.getAttribute("title")){
a.disabled=true;
if(a.getAttribute("title")==_464){
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
var _470={dj_ie:drh.ie,dj_ie55:drh.ie55,dj_ie6:drh.ie60,dj_ie7:drh.ie70,dj_iequirks:drh.ie&&drh.quirks,dj_opera:drh.opera,dj_opera8:drh.opera&&(Math.floor(dojo.render.version)==8),dj_opera9:drh.opera&&(Math.floor(dojo.render.version)==9),dj_khtml:drh.khtml,dj_safari:drh.safari,dj_gecko:drh.mozilla};
for(var p in _470){
if(_470[p]){
dojo.html.addClass(node,p);
}
}
};
dojo.provide("dojo.widget.DomWidget");
dojo.widget._cssFiles={};
dojo.widget._cssStrings={};
dojo.widget._templateCache={};
dojo.widget.defaultStrings={dojoRoot:dojo.hostenv.getBaseScriptUri(),baseScriptUri:dojo.hostenv.getBaseScriptUri()};
dojo.widget.fillFromTemplateCache=function(obj,_473,_474,_475){
var _476=_473||obj.templatePath;
var _477=dojo.widget._templateCache;
if(!_476&&!obj["widgetType"]){
do{
var _478="__dummyTemplate__"+dojo.widget._templateCache.dummyCount++;
}while(_477[_478]);
obj.widgetType=_478;
}
var wt=_476?_476.toString():obj.widgetType;
var ts=_477[wt];
if(!ts){
_477[wt]={"string":null,"node":null};
if(_475){
ts={};
}else{
ts=_477[wt];
}
}
if((!obj.templateString)&&(!_475)){
obj.templateString=_474||ts["string"];
}
if((!obj.templateNode)&&(!_475)){
obj.templateNode=ts["node"];
}
if((!obj.templateNode)&&(!obj.templateString)&&(_476)){
var _47b=dojo.hostenv.getText(_476);
if(_47b){
_47b=_47b.replace(/^\s*<\?xml(\s)+version=[\'\"](\d)*.(\d)*[\'\"](\s)*\?>/im,"");
var _47c=_47b.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);
if(_47c){
_47b=_47c[1];
}
}else{
_47b="";
}
obj.templateString=_47b;
if(!_475){
_477[wt]["string"]=_47b;
}
}
if((!ts["string"])&&(!_475)){
ts.string=obj.templateString;
}
};
dojo.widget._templateCache.dummyCount=0;
dojo.widget.attachProperties=["dojoAttachPoint","id"];
dojo.widget.eventAttachProperty="dojoAttachEvent";
dojo.widget.onBuildProperty="dojoOnBuild";
dojo.widget.waiNames=["waiRole","waiState"];
dojo.widget.wai={waiRole:{name:"waiRole","namespace":"http://www.w3.org/TR/xhtml2",alias:"x2",prefix:"wairole:"},waiState:{name:"waiState","namespace":"http://www.w3.org/2005/07/aaa",alias:"aaa",prefix:""},setAttr:function(node,ns,attr,_480){
if(dojo.render.html.ie){
node.setAttribute(this[ns].alias+":"+attr,this[ns].prefix+_480);
}else{
node.setAttributeNS(this[ns]["namespace"],attr,this[ns].prefix+_480);
}
},getAttr:function(node,ns,attr){
if(dojo.render.html.ie){
return node.getAttribute(this[ns].alias+":"+attr);
}else{
return node.getAttributeNS(this[ns]["namespace"],attr);
}
},removeAttr:function(node,ns,attr){
var _487=true;
if(dojo.render.html.ie){
_487=node.removeAttribute(this[ns].alias+":"+attr);
}else{
node.removeAttributeNS(this[ns]["namespace"],attr);
}
return _487;
}};
dojo.widget.attachTemplateNodes=function(_488,_489,_48a){
var _48b=dojo.dom.ELEMENT_NODE;
function trim(str){
return str.replace(/^\s+|\s+$/g,"");
}
if(!_488){
_488=_489.domNode;
}
if(_488.nodeType!=_48b){
return;
}
var _48d=_488.all||_488.getElementsByTagName("*");
var _48e=_489;
for(var x=-1;x<_48d.length;x++){
var _490=(x==-1)?_488:_48d[x];
var _491=[];
if(!_489.widgetsInTemplate||!_490.getAttribute("dojoType")){
for(var y=0;y<this.attachProperties.length;y++){
var _493=_490.getAttribute(this.attachProperties[y]);
if(_493){
_491=_493.split(";");
for(var z=0;z<_491.length;z++){
if(dojo.lang.isArray(_489[_491[z]])){
_489[_491[z]].push(_490);
}else{
_489[_491[z]]=_490;
}
}
break;
}
}
var _495=_490.getAttribute(this.eventAttachProperty);
if(_495){
var evts=_495.split(";");
for(var y=0;y<evts.length;y++){
if((!evts[y])||(!evts[y].length)){
continue;
}
var _497=null;
var tevt=trim(evts[y]);
if(evts[y].indexOf(":")>=0){
var _499=tevt.split(":");
tevt=trim(_499[0]);
_497=trim(_499[1]);
}
if(!_497){
_497=tevt;
}
var tf=function(){
var ntf=new String(_497);
return function(evt){
if(_48e[ntf]){
_48e[ntf](dojo.event.browser.fixEvent(evt,this));
}
};
}();
dojo.event.browser.addListener(_490,tevt,tf,false,true);
}
}
for(var y=0;y<_48a.length;y++){
var _49d=_490.getAttribute(_48a[y]);
if((_49d)&&(_49d.length)){
var _497=null;
var _49e=_48a[y].substr(4);
_497=trim(_49d);
var _49f=[_497];
if(_497.indexOf(";")>=0){
_49f=dojo.lang.map(_497.split(";"),trim);
}
for(var z=0;z<_49f.length;z++){
if(!_49f[z].length){
continue;
}
var tf=function(){
var ntf=new String(_49f[z]);
return function(evt){
if(_48e[ntf]){
_48e[ntf](dojo.event.browser.fixEvent(evt,this));
}
};
}();
dojo.event.browser.addListener(_490,_49e,tf,false,true);
}
}
}
}
var _4a2=_490.getAttribute(this.templateProperty);
if(_4a2){
_489[_4a2]=_490;
}
dojo.lang.forEach(dojo.widget.waiNames,function(name){
var wai=dojo.widget.wai[name];
var val=_490.getAttribute(wai.name);
if(val){
if(val.indexOf("-")==-1){
dojo.widget.wai.setAttr(_490,wai.name,"role",val);
}else{
var _4a6=val.split("-");
dojo.widget.wai.setAttr(_490,wai.name,_4a6[0],_4a6[1]);
}
}
},this);
var _4a7=_490.getAttribute(this.onBuildProperty);
if(_4a7){
eval("var node = baseNode; var widget = targetObj; "+_4a7);
}
}
};
dojo.widget.getDojoEventsFromStr=function(str){
var re=/(dojoOn([a-z]+)(\s?))=/gi;
var evts=str?str.match(re)||[]:[];
var ret=[];
var lem={};
for(var x=0;x<evts.length;x++){
if(evts[x].length<1){
continue;
}
var cm=evts[x].replace(/\s/,"");
cm=(cm.slice(0,cm.length-1));
if(!lem[cm]){
lem[cm]=true;
ret.push(cm);
}
}
return ret;
};
dojo.declare("dojo.widget.DomWidget",dojo.widget.Widget,function(){
if((arguments.length>0)&&(typeof arguments[0]=="object")){
this.create(arguments[0]);
}
},{templateNode:null,templateString:null,templateCssString:null,preventClobber:false,domNode:null,containerNode:null,widgetsInTemplate:false,addChild:function(_4af,_4b0,pos,ref,_4b3){
if(!this.isContainer){
dojo.debug("dojo.widget.DomWidget.addChild() attempted on non-container widget");
return null;
}else{
if(_4b3==undefined){
_4b3=this.children.length;
}
this.addWidgetAsDirectChild(_4af,_4b0,pos,ref,_4b3);
this.registerChild(_4af,_4b3);
}
return _4af;
},addWidgetAsDirectChild:function(_4b4,_4b5,pos,ref,_4b8){
if((!this.containerNode)&&(!_4b5)){
this.containerNode=this.domNode;
}
var cn=(_4b5)?_4b5:this.containerNode;
if(!pos){
pos="after";
}
if(!ref){
if(!cn){
cn=dojo.body();
}
ref=cn.lastChild;
}
if(!_4b8){
_4b8=0;
}
_4b4.domNode.setAttribute("dojoinsertionindex",_4b8);
if(!ref){
cn.appendChild(_4b4.domNode);
}else{
if(pos=="insertAtIndex"){
dojo.dom.insertAtIndex(_4b4.domNode,ref.parentNode,_4b8);
}else{
if((pos=="after")&&(ref===cn.lastChild)){
cn.appendChild(_4b4.domNode);
}else{
dojo.dom.insertAtPosition(_4b4.domNode,cn,pos);
}
}
}
},registerChild:function(_4ba,_4bb){
_4ba.dojoInsertionIndex=_4bb;
var idx=-1;
for(var i=0;i<this.children.length;i++){
if(this.children[i].dojoInsertionIndex<=_4bb){
idx=i;
}
}
this.children.splice(idx+1,0,_4ba);
_4ba.parent=this;
_4ba.addedTo(this,idx+1);
delete dojo.widget.manager.topWidgets[_4ba.widgetId];
},removeChild:function(_4be){
dojo.dom.removeNode(_4be.domNode);
return dojo.widget.DomWidget.superclass.removeChild.call(this,_4be);
},getFragNodeRef:function(frag){
if(!frag){
return null;
}
if(!frag[this.getNamespacedType()]){
dojo.raise("Error: no frag for widget type "+this.getNamespacedType()+", id "+this.widgetId+" (maybe a widget has set it's type incorrectly)");
}
return frag[this.getNamespacedType()]["nodeRef"];
},postInitialize:function(args,frag,_4c2){
var _4c3=this.getFragNodeRef(frag);
if(_4c2&&(_4c2.snarfChildDomOutput||!_4c3)){
_4c2.addWidgetAsDirectChild(this,"","insertAtIndex","",args["dojoinsertionindex"],_4c3);
}else{
if(_4c3){
if(this.domNode&&(this.domNode!==_4c3)){
this._sourceNodeRef=dojo.dom.replaceNode(_4c3,this.domNode);
}
}
}
if(_4c2){
_4c2.registerChild(this,args.dojoinsertionindex);
}else{
dojo.widget.manager.topWidgets[this.widgetId]=this;
}
if(this.widgetsInTemplate){
var _4c4=new dojo.xml.Parse();
var _4c5;
var _4c6=this.domNode.getElementsByTagName("*");
for(var i=0;i<_4c6.length;i++){
if(_4c6[i].getAttribute("dojoAttachPoint")=="subContainerWidget"){
_4c5=_4c6[i];
}
if(_4c6[i].getAttribute("dojoType")){
_4c6[i].setAttribute("isSubWidget",true);
}
}
if(this.isContainer&&!this.containerNode){
if(_4c5){
var src=this.getFragNodeRef(frag);
if(src){
dojo.dom.moveChildren(src,_4c5);
frag["dojoDontFollow"]=true;
}
}else{
dojo.debug("No subContainerWidget node can be found in template file for widget "+this);
}
}
var _4c9=_4c4.parseElement(this.domNode,null,true);
dojo.widget.getParser().createSubComponents(_4c9,this);
var _4ca=[];
var _4cb=[this];
var w;
while((w=_4cb.pop())){
for(var i=0;i<w.children.length;i++){
var _4cd=w.children[i];
if(_4cd._processedSubWidgets||!_4cd.extraArgs["issubwidget"]){
continue;
}
_4ca.push(_4cd);
if(_4cd.isContainer){
_4cb.push(_4cd);
}
}
}
for(var i=0;i<_4ca.length;i++){
var _4ce=_4ca[i];
if(_4ce._processedSubWidgets){
dojo.debug("This should not happen: widget._processedSubWidgets is already true!");
return;
}
_4ce._processedSubWidgets=true;
if(_4ce.extraArgs["dojoattachevent"]){
var evts=_4ce.extraArgs["dojoattachevent"].split(";");
for(var j=0;j<evts.length;j++){
var _4d1=null;
var tevt=dojo.string.trim(evts[j]);
if(tevt.indexOf(":")>=0){
var _4d3=tevt.split(":");
tevt=dojo.string.trim(_4d3[0]);
_4d1=dojo.string.trim(_4d3[1]);
}
if(!_4d1){
_4d1=tevt;
}
if(dojo.lang.isFunction(_4ce[tevt])){
dojo.event.kwConnect({srcObj:_4ce,srcFunc:tevt,targetObj:this,targetFunc:_4d1});
}else{
alert(tevt+" is not a function in widget "+_4ce);
}
}
}
if(_4ce.extraArgs["dojoattachpoint"]){
this[_4ce.extraArgs["dojoattachpoint"]]=_4ce;
}
}
}
if(this.isContainer&&!frag["dojoDontFollow"]){
dojo.widget.getParser().createSubComponents(frag,this);
}
},buildRendering:function(args,frag){
var ts=dojo.widget._templateCache[this.widgetType];
if(args["templatecsspath"]){
args["templateCssPath"]=args["templatecsspath"];
}
var _4d7=args["templateCssPath"]||this.templateCssPath;
if(_4d7&&!dojo.widget._cssFiles[_4d7.toString()]){
if((!this.templateCssString)&&(_4d7)){
this.templateCssString=dojo.hostenv.getText(_4d7);
this.templateCssPath=null;
}
dojo.widget._cssFiles[_4d7.toString()]=true;
}
if((this["templateCssString"])&&(!dojo.widget._cssStrings[this.templateCssString])){
dojo.html.insertCssText(this.templateCssString,null,_4d7);
dojo.widget._cssStrings[this.templateCssString]=true;
}
if((!this.preventClobber)&&((this.templatePath)||(this.templateNode)||((this["templateString"])&&(this.templateString.length))||((typeof ts!="undefined")&&((ts["string"])||(ts["node"]))))){
this.buildFromTemplate(args,frag);
}else{
this.domNode=this.getFragNodeRef(frag);
}
this.fillInTemplate(args,frag);
},buildFromTemplate:function(args,frag){
var _4da=false;
if(args["templatepath"]){
args["templatePath"]=args["templatepath"];
}
dojo.widget.fillFromTemplateCache(this,args["templatePath"],null,_4da);
var ts=dojo.widget._templateCache[this.templatePath?this.templatePath.toString():this.widgetType];
if((ts)&&(!_4da)){
if(!this.templateString.length){
this.templateString=ts["string"];
}
if(!this.templateNode){
this.templateNode=ts["node"];
}
}
var _4dc=false;
var node=null;
var tstr=this.templateString;
if((!this.templateNode)&&(this.templateString)){
_4dc=this.templateString.match(/\$\{([^\}]+)\}/g);
if(_4dc){
var hash=this.strings||{};
for(var key in dojo.widget.defaultStrings){
if(dojo.lang.isUndefined(hash[key])){
hash[key]=dojo.widget.defaultStrings[key];
}
}
for(var i=0;i<_4dc.length;i++){
var key=_4dc[i];
key=key.substring(2,key.length-1);
var kval=(key.substring(0,5)=="this.")?dojo.lang.getObjPathValue(key.substring(5),this):hash[key];
var _4e3;
if((kval)||(dojo.lang.isString(kval))){
_4e3=new String((dojo.lang.isFunction(kval))?kval.call(this,key,this.templateString):kval);
while(_4e3.indexOf("\"")>-1){
_4e3=_4e3.replace("\"","&quot;");
}
tstr=tstr.replace(_4dc[i],_4e3);
}
}
}else{
this.templateNode=this.createNodesFromText(this.templateString,true)[0];
if(!_4da){
ts.node=this.templateNode;
}
}
}
if((!this.templateNode)&&(!_4dc)){
dojo.debug("DomWidget.buildFromTemplate: could not create template");
return false;
}else{
if(!_4dc){
node=this.templateNode.cloneNode(true);
if(!node){
return false;
}
}else{
node=this.createNodesFromText(tstr,true)[0];
}
}
this.domNode=node;
this.attachTemplateNodes();
if(this.isContainer&&this.containerNode){
var src=this.getFragNodeRef(frag);
if(src){
dojo.dom.moveChildren(src,this.containerNode);
}
}
},attachTemplateNodes:function(_4e5,_4e6){
if(!_4e5){
_4e5=this.domNode;
}
if(!_4e6){
_4e6=this;
}
return dojo.widget.attachTemplateNodes(_4e5,_4e6,dojo.widget.getDojoEventsFromStr(this.templateString));
},fillInTemplate:function(){
},destroyRendering:function(){
try{
dojo.dom.destroyNode(this.domNode);
delete this.domNode;
}
catch(e){
}
if(this._sourceNodeRef){
try{
dojo.dom.destroyNode(this._sourceNodeRef);
}
catch(e){
}
}
},createNodesFromText:function(){
dojo.unimplemented("dojo.widget.DomWidget.createNodesFromText");
}});
dojo.provide("dojo.html.display");
dojo.html._toggle=function(node,_4e8,_4e9){
node=dojo.byId(node);
_4e9(node,!_4e8(node));
return _4e8(node);
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
dojo.html.setShowing=function(node,_4ee){
dojo.html[(_4ee?"show":"hide")](node);
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
dojo.html.setDisplay=function(node,_4f4){
dojo.html.setStyle(node,"display",((_4f4 instanceof String||typeof _4f4=="string")?_4f4:(_4f4?dojo.html.suggestDisplayByTagName(node):"none")));
};
dojo.html.isDisplayed=function(node){
return (dojo.html.getComputedStyle(node,"display")!="none");
};
dojo.html.toggleDisplay=function(node){
return dojo.html._toggle(node,dojo.html.isDisplayed,dojo.html.setDisplay);
};
dojo.html.setVisibility=function(node,_4f8){
dojo.html.setStyle(node,"visibility",((_4f8 instanceof String||typeof _4f8=="string")?_4f8:(_4f8?"visible":"hidden")));
};
dojo.html.isVisible=function(node){
return (dojo.html.getComputedStyle(node,"visibility")!="hidden");
};
dojo.html.toggleVisibility=function(node){
return dojo.html._toggle(node,dojo.html.isVisible,dojo.html.setVisibility);
};
dojo.html.setOpacity=function(node,_4fc,_4fd){
node=dojo.byId(node);
var h=dojo.render.html;
if(!_4fd){
if(_4fc>=1){
if(h.ie){
dojo.html.clearOpacity(node);
return;
}else{
_4fc=0.999999;
}
}else{
if(_4fc<0){
_4fc=0;
}
}
}
if(h.ie){
if(node.nodeName.toLowerCase()=="tr"){
var tds=node.getElementsByTagName("td");
for(var x=0;x<tds.length;x++){
tds[x].style.filter="Alpha(Opacity="+_4fc*100+")";
}
}
node.style.filter="Alpha(Opacity="+_4fc*100+")";
}else{
if(h.moz){
node.style.opacity=_4fc;
node.style.MozOpacity=_4fc;
}else{
if(h.safari){
node.style.opacity=_4fc;
node.style.KhtmlOpacity=_4fc;
}else{
node.style.opacity=_4fc;
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
dojo.provide("dojo.html.layout");
dojo.html.sumAncestorProperties=function(node,prop){
node=dojo.byId(node);
if(!node){
return 0;
}
var _509=0;
while(node){
if(dojo.html.getComputedStyle(node,"position")=="fixed"){
return 0;
}
var val=node[prop];
if(val){
_509+=val-0;
if(node==dojo.body()){
break;
}
}
node=node.parentNode;
}
return _509;
};
dojo.html.setStyleAttributes=function(node,_50c){
node=dojo.byId(node);
var _50d=_50c.replace(/(;)?\s*$/,"").split(";");
for(var i=0;i<_50d.length;i++){
var _50f=_50d[i].split(":");
var name=_50f[0].replace(/\s*$/,"").replace(/^\s*/,"").toLowerCase();
var _511=_50f[1].replace(/\s*$/,"").replace(/^\s*/,"");
switch(name){
case "opacity":
dojo.html.setOpacity(node,_511);
break;
case "content-height":
dojo.html.setContentBox(node,{height:_511});
break;
case "content-width":
dojo.html.setContentBox(node,{width:_511});
break;
case "outer-height":
dojo.html.setMarginBox(node,{height:_511});
break;
case "outer-width":
dojo.html.setMarginBox(node,{width:_511});
break;
default:
node.style[dojo.html.toCamelCase(name)]=_511;
}
}
};
dojo.html.boxSizing={MARGIN_BOX:"margin-box",BORDER_BOX:"border-box",PADDING_BOX:"padding-box",CONTENT_BOX:"content-box"};
dojo.html.getAbsolutePosition=dojo.html.abs=function(node,_513,_514){
node=dojo.byId(node,node.ownerDocument);
var ret={x:0,y:0};
var bs=dojo.html.boxSizing;
if(!_514){
_514=bs.CONTENT_BOX;
}
var _517=2;
var _518;
switch(_514){
case bs.MARGIN_BOX:
_518=3;
break;
case bs.BORDER_BOX:
_518=2;
break;
case bs.PADDING_BOX:
default:
_518=1;
break;
case bs.CONTENT_BOX:
_518=0;
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
_517=1;
try{
var bo=document.getBoxObjectFor(node);
ret.x=bo.x-dojo.html.sumAncestorProperties(node,"scrollLeft");
ret.y=bo.y-dojo.html.sumAncestorProperties(node,"scrollTop");
}
catch(e){
}
}else{
if(node["offsetParent"]){
var _51c;
if((h.safari)&&(node.style.getPropertyValue("position")=="absolute")&&(node.parentNode==db)){
_51c=db;
}else{
_51c=db.parentNode;
}
if(node.parentNode!=db){
var nd=node;
if(dojo.render.html.opera){
nd=db;
}
ret.x-=dojo.html.sumAncestorProperties(nd,"scrollLeft");
ret.y-=dojo.html.sumAncestorProperties(nd,"scrollTop");
}
var _51e=node;
do{
var n=_51e["offsetLeft"];
if(!h.opera||n>0){
ret.x+=isNaN(n)?0:n;
}
var m=_51e["offsetTop"];
ret.y+=isNaN(m)?0:m;
_51e=_51e.offsetParent;
}while((_51e!=_51c)&&(_51e!=null));
}else{
if(node["x"]&&node["y"]){
ret.x+=isNaN(node.x)?0:node.x;
ret.y+=isNaN(node.y)?0:node.y;
}
}
}
}
if(_513){
var _521=dojo.html.getScroll();
ret.y+=_521.top;
ret.x+=_521.left;
}
var _522=[dojo.html.getPaddingExtent,dojo.html.getBorderExtent,dojo.html.getMarginExtent];
if(_517>_518){
for(var i=_518;i<_517;++i){
ret.y+=_522[i](node,"top");
ret.x+=_522[i](node,"left");
}
}else{
if(_517<_518){
for(var i=_518;i>_517;--i){
ret.y-=_522[i-1](node,"top");
ret.x-=_522[i-1](node,"left");
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
dojo.html._sumPixelValues=function(node,_526,_527){
var _528=0;
for(var x=0;x<_526.length;x++){
_528+=dojo.html.getPixelValue(node,_526[x],_527);
}
return _528;
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
var _535=dojo.html.getBorder(node);
return {width:pad.width+_535.width,height:pad.height+_535.height};
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
var _53a=dojo.html.getStyle(node,"-moz-box-sizing");
if(!_53a){
_53a=dojo.html.getStyle(node,"box-sizing");
}
return (_53a?_53a:bs.CONTENT_BOX);
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
var _53f=dojo.html.getBorder(node);
return {width:box.width-_53f.width,height:box.height-_53f.height};
};
dojo.html.getContentBox=function(node){
node=dojo.byId(node);
var _541=dojo.html.getPadBorder(node);
return {width:node.offsetWidth-_541.width,height:node.offsetHeight-_541.height};
};
dojo.html.setContentBox=function(node,args){
node=dojo.byId(node);
var _544=0;
var _545=0;
var isbb=dojo.html.isBorderBox(node);
var _547=(isbb?dojo.html.getPadBorder(node):{width:0,height:0});
var ret={};
if(typeof args.width!="undefined"){
_544=args.width+_547.width;
ret.width=dojo.html.setPositivePixelValue(node,"width",_544);
}
if(typeof args.height!="undefined"){
_545=args.height+_547.height;
ret.height=dojo.html.setPositivePixelValue(node,"height",_545);
}
return ret;
};
dojo.html.getMarginBox=function(node){
var _54a=dojo.html.getBorderBox(node);
var _54b=dojo.html.getMargin(node);
return {width:_54a.width+_54b.width,height:_54a.height+_54b.height};
};
dojo.html.setMarginBox=function(node,args){
node=dojo.byId(node);
var _54e=0;
var _54f=0;
var isbb=dojo.html.isBorderBox(node);
var _551=(!isbb?dojo.html.getPadBorder(node):{width:0,height:0});
var _552=dojo.html.getMargin(node);
var ret={};
if(typeof args.width!="undefined"){
_54e=args.width-_551.width;
_54e-=_552.width;
ret.width=dojo.html.setPositivePixelValue(node,"width",_54e);
}
if(typeof args.height!="undefined"){
_54f=args.height-_551.height;
_54f-=_552.height;
ret.height=dojo.html.setPositivePixelValue(node,"height",_54f);
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
dojo.html.toCoordinateObject=dojo.html.toCoordinateArray=function(_557,_558,_559){
if(_557 instanceof Array||typeof _557=="array"){
dojo.deprecated("dojo.html.toCoordinateArray","use dojo.html.toCoordinateObject({left: , top: , width: , height: }) instead","0.5");
while(_557.length<4){
_557.push(0);
}
while(_557.length>4){
_557.pop();
}
var ret={left:_557[0],top:_557[1],width:_557[2],height:_557[3]};
}else{
if(!_557.nodeType&&!(_557 instanceof String||typeof _557=="string")&&("width" in _557||"height" in _557||"left" in _557||"x" in _557||"top" in _557||"y" in _557)){
var ret={left:_557.left||_557.x||0,top:_557.top||_557.y||0,width:_557.width||0,height:_557.height||0};
}else{
var node=dojo.byId(_557);
var pos=dojo.html.abs(node,_558,_559);
var _55d=dojo.html.getMarginBox(node);
var ret={left:pos.left,top:pos.top,width:_55d.width,height:_55d.height};
}
}
ret.x=ret.left;
ret.y=ret.top;
return ret;
};
dojo.html.setMarginBoxWidth=dojo.html.setOuterWidth=function(node,_55f){
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
dojo.html.getTotalOffset=function(node,type,_562){
return dojo.html._callDeprecated("getTotalOffset","getAbsolutePosition",arguments,null,type);
};
dojo.html.getAbsoluteX=function(node,_564){
return dojo.html._callDeprecated("getAbsoluteX","getAbsolutePosition",arguments,null,"x");
};
dojo.html.getAbsoluteY=function(node,_566){
return dojo.html._callDeprecated("getAbsoluteY","getAbsolutePosition",arguments,null,"y");
};
dojo.html.totalOffsetLeft=function(node,_568){
return dojo.html._callDeprecated("totalOffsetLeft","getAbsolutePosition",arguments,null,"left");
};
dojo.html.totalOffsetTop=function(node,_56a){
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
dojo.html.setContentBoxWidth=dojo.html.setContentWidth=function(node,_574){
return dojo.html._callDeprecated("setContentBoxWidth","setContentBox",arguments,"width");
};
dojo.html.setContentBoxHeight=dojo.html.setContentHeight=function(node,_576){
return dojo.html._callDeprecated("setContentBoxHeight","setContentBox",arguments,"height");
};
dojo.provide("dojo.html.util");
dojo.html.getElementWindow=function(_577){
return dojo.html.getDocumentWindow(_577.ownerDocument);
};
dojo.html.getDocumentWindow=function(doc){
if(dojo.render.html.safari&&!doc._parentWindow){
var fix=function(win){
win.document._parentWindow=win;
for(var i=0;i<win.frames.length;i++){
fix(win.frames[i]);
}
};
fix(window.top);
}
if(dojo.render.html.ie&&window!==document.parentWindow&&!doc._parentWindow){
doc.parentWindow.execScript("document._parentWindow = window;","Javascript");
var win=doc._parentWindow;
doc._parentWindow=null;
return win;
}
return doc._parentWindow||doc.parentWindow||doc.defaultView;
};
dojo.html.gravity=function(node,e){
node=dojo.byId(node);
var _57f=dojo.html.getCursorPosition(e);
with(dojo.html){
var _580=getAbsolutePosition(node,true);
var bb=getBorderBox(node);
var _582=_580.x+(bb.width/2);
var _583=_580.y+(bb.height/2);
}
with(dojo.html.gravity){
return ((_57f.x<_582?WEST:EAST)|(_57f.y<_583?NORTH:SOUTH));
}
};
dojo.html.gravity.NORTH=1;
dojo.html.gravity.SOUTH=1<<1;
dojo.html.gravity.EAST=1<<2;
dojo.html.gravity.WEST=1<<3;
dojo.html.overElement=function(_584,e){
_584=dojo.byId(_584);
var _586=dojo.html.getCursorPosition(e);
var bb=dojo.html.getBorderBox(_584);
var _588=dojo.html.getAbsolutePosition(_584,true,dojo.html.boxSizing.BORDER_BOX);
var top=_588.y;
var _58a=top+bb.height;
var left=_588.x;
var _58c=left+bb.width;
return (_586.x>=left&&_586.x<=_58c&&_586.y>=top&&_586.y<=_58a);
};
dojo.html.renderedTextContent=function(node){
node=dojo.byId(node);
var _58e="";
if(node==null){
return _58e;
}
for(var i=0;i<node.childNodes.length;i++){
switch(node.childNodes[i].nodeType){
case 1:
case 5:
var _590="unknown";
try{
_590=dojo.html.getStyle(node.childNodes[i],"display");
}
catch(E){
}
switch(_590){
case "block":
case "list-item":
case "run-in":
case "table":
case "table-row-group":
case "table-header-group":
case "table-footer-group":
case "table-row":
case "table-column-group":
case "table-column":
case "table-cell":
case "table-caption":
_58e+="\n";
_58e+=dojo.html.renderedTextContent(node.childNodes[i]);
_58e+="\n";
break;
case "none":
break;
default:
if(node.childNodes[i].tagName&&node.childNodes[i].tagName.toLowerCase()=="br"){
_58e+="\n";
}else{
_58e+=dojo.html.renderedTextContent(node.childNodes[i]);
}
break;
}
break;
case 3:
case 2:
case 4:
var text=node.childNodes[i].nodeValue;
var _592="unknown";
try{
_592=dojo.html.getStyle(node,"text-transform");
}
catch(E){
}
switch(_592){
case "capitalize":
var _593=text.split(" ");
for(var i=0;i<_593.length;i++){
_593[i]=_593[i].charAt(0).toUpperCase()+_593[i].substring(1);
}
text=_593.join(" ");
break;
case "uppercase":
text=text.toUpperCase();
break;
case "lowercase":
text=text.toLowerCase();
break;
default:
break;
}
switch(_592){
case "nowrap":
break;
case "pre-wrap":
break;
case "pre-line":
break;
case "pre":
break;
default:
text=text.replace(/\s+/," ");
if(/\s$/.test(_58e)){
text.replace(/^\s/,"");
}
break;
}
_58e+=text;
break;
default:
break;
}
}
return _58e;
};
dojo.html.createNodesFromText=function(txt,trim){
if(trim){
txt=txt.replace(/^\s+|\s+$/g,"");
}
var tn=dojo.doc().createElement("div");
tn.style.visibility="hidden";
dojo.body().appendChild(tn);
var _597="none";
if((/^<t[dh][\s\r\n>]/i).test(txt.replace(/^\s+/))){
txt="<table><tbody><tr>"+txt+"</tr></tbody></table>";
_597="cell";
}else{
if((/^<tr[\s\r\n>]/i).test(txt.replace(/^\s+/))){
txt="<table><tbody>"+txt+"</tbody></table>";
_597="row";
}else{
if((/^<(thead|tbody|tfoot)[\s\r\n>]/i).test(txt.replace(/^\s+/))){
txt="<table>"+txt+"</table>";
_597="section";
}
}
}
tn.innerHTML=txt;
if(tn["normalize"]){
tn.normalize();
}
var _598=null;
switch(_597){
case "cell":
_598=tn.getElementsByTagName("tr")[0];
break;
case "row":
_598=tn.getElementsByTagName("tbody")[0];
break;
case "section":
_598=tn.getElementsByTagName("table")[0];
break;
default:
_598=tn;
break;
}
var _599=[];
for(var x=0;x<_598.childNodes.length;x++){
_599.push(_598.childNodes[x].cloneNode(true));
}
tn.style.display="none";
dojo.html.destroyNode(tn);
return _599;
};
dojo.html.placeOnScreen=function(node,_59c,_59d,_59e,_59f,_5a0,_5a1){
if(_59c instanceof Array||typeof _59c=="array"){
_5a1=_5a0;
_5a0=_59f;
_59f=_59e;
_59e=_59d;
_59d=_59c[1];
_59c=_59c[0];
}
if(_5a0 instanceof String||typeof _5a0=="string"){
_5a0=_5a0.split(",");
}
if(!isNaN(_59e)){
_59e=[Number(_59e),Number(_59e)];
}else{
if(!(_59e instanceof Array||typeof _59e=="array")){
_59e=[0,0];
}
}
var _5a2=dojo.html.getScroll().offset;
var view=dojo.html.getViewport();
node=dojo.byId(node);
var _5a4=node.style.display;
node.style.display="";
var bb=dojo.html.getBorderBox(node);
var w=bb.width;
var h=bb.height;
node.style.display=_5a4;
if(!(_5a0 instanceof Array||typeof _5a0=="array")){
_5a0=["TL"];
}
var _5a8,_5a9,_5aa=Infinity,_5ab;
for(var _5ac=0;_5ac<_5a0.length;++_5ac){
var _5ad=_5a0[_5ac];
var _5ae=true;
var tryX=_59c-(_5ad.charAt(1)=="L"?0:w)+_59e[0]*(_5ad.charAt(1)=="L"?1:-1);
var tryY=_59d-(_5ad.charAt(0)=="T"?0:h)+_59e[1]*(_5ad.charAt(0)=="T"?1:-1);
if(_59f){
tryX-=_5a2.x;
tryY-=_5a2.y;
}
if(tryX<0){
tryX=0;
_5ae=false;
}
if(tryY<0){
tryY=0;
_5ae=false;
}
var x=tryX+w;
if(x>view.width){
x=view.width-w;
_5ae=false;
}else{
x=tryX;
}
x=Math.max(_59e[0],x)+_5a2.x;
var y=tryY+h;
if(y>view.height){
y=view.height-h;
_5ae=false;
}else{
y=tryY;
}
y=Math.max(_59e[1],y)+_5a2.y;
if(_5ae){
_5a8=x;
_5a9=y;
_5aa=0;
_5ab=_5ad;
break;
}else{
var dist=Math.pow(x-tryX-_5a2.x,2)+Math.pow(y-tryY-_5a2.y,2);
if(_5aa>dist){
_5aa=dist;
_5a8=x;
_5a9=y;
_5ab=_5ad;
}
}
}
if(!_5a1){
node.style.left=_5a8+"px";
node.style.top=_5a9+"px";
}
return {left:_5a8,top:_5a9,x:_5a8,y:_5a9,dist:_5aa,corner:_5ab};
};
dojo.html.placeOnScreenPoint=function(node,_5b5,_5b6,_5b7,_5b8){
dojo.deprecated("dojo.html.placeOnScreenPoint","use dojo.html.placeOnScreen() instead","0.5");
return dojo.html.placeOnScreen(node,_5b5,_5b6,_5b7,_5b8,["TL","TR","BL","BR"]);
};
dojo.html.placeOnScreenAroundElement=function(node,_5ba,_5bb,_5bc,_5bd,_5be){
var best,_5c0=Infinity;
_5ba=dojo.byId(_5ba);
var _5c1=_5ba.style.display;
_5ba.style.display="";
var mb=dojo.html.getElementBox(_5ba,_5bc);
var _5c3=mb.width;
var _5c4=mb.height;
var _5c5=dojo.html.getAbsolutePosition(_5ba,true,_5bc);
_5ba.style.display=_5c1;
for(var _5c6 in _5bd){
var pos,_5c8,_5c9;
var _5ca=_5bd[_5c6];
_5c8=_5c5.x+(_5c6.charAt(1)=="L"?0:_5c3);
_5c9=_5c5.y+(_5c6.charAt(0)=="T"?0:_5c4);
pos=dojo.html.placeOnScreen(node,_5c8,_5c9,_5bb,true,_5ca,true);
if(pos.dist==0){
best=pos;
break;
}else{
if(_5c0>pos.dist){
_5c0=pos.dist;
best=pos;
}
}
}
if(!_5be){
node.style.left=best.left+"px";
node.style.top=best.top+"px";
}
return best;
};
dojo.html.scrollIntoView=function(node){
if(!node){
return;
}
if(dojo.render.html.ie){
if(dojo.html.getBorderBox(node.parentNode).height<=node.parentNode.scrollHeight){
node.scrollIntoView(false);
}
}else{
if(dojo.render.html.mozilla){
node.scrollIntoView(false);
}else{
var _5cc=node.parentNode;
var _5cd=_5cc.scrollTop+dojo.html.getBorderBox(_5cc).height;
var _5ce=node.offsetTop+dojo.html.getMarginBox(node).height;
if(_5cd<_5ce){
_5cc.scrollTop+=(_5ce-_5cd);
}else{
if(_5cc.scrollTop>node.offsetTop){
_5cc.scrollTop-=(_5cc.scrollTop-node.offsetTop);
}
}
}
}
};
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
dojo.extend(dojo.gfx.color.Color,{toRgb:function(_5d5){
if(_5d5){
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
},blend:function(_5d6,_5d7){
var rgb=null;
if(dojo.lang.isArray(_5d6)){
rgb=_5d6;
}else{
if(_5d6 instanceof dojo.gfx.color.Color){
rgb=_5d6.toRgb();
}else{
rgb=new dojo.gfx.color.Color(_5d6).toRgb();
}
}
return dojo.gfx.color.blend(this.toRgb(),rgb,_5d7);
}});
dojo.gfx.color.named={white:[255,255,255],black:[0,0,0],red:[255,0,0],green:[0,255,0],lime:[0,255,0],blue:[0,0,255],navy:[0,0,128],gray:[128,128,128],silver:[192,192,192]};
dojo.gfx.color.blend=function(a,b,_5db){
if(typeof a=="string"){
return dojo.gfx.color.blendHex(a,b,_5db);
}
if(!_5db){
_5db=0;
}
_5db=Math.min(Math.max(-1,_5db),1);
_5db=((_5db+1)/2);
var c=[];
for(var x=0;x<3;x++){
c[x]=parseInt(b[x]+((a[x]-b[x])*_5db));
}
return c;
};
dojo.gfx.color.blendHex=function(a,b,_5e0){
return dojo.gfx.color.rgb2hex(dojo.gfx.color.blend(dojo.gfx.color.hex2rgb(a),dojo.gfx.color.hex2rgb(b),_5e0));
};
dojo.gfx.color.extractRGB=function(_5e1){
var hex="0123456789abcdef";
_5e1=_5e1.toLowerCase();
if(_5e1.indexOf("rgb")==0){
var _5e3=_5e1.match(/rgba*\((\d+), *(\d+), *(\d+)/i);
var ret=_5e3.splice(1,3);
return ret;
}else{
var _5e5=dojo.gfx.color.hex2rgb(_5e1);
if(_5e5){
return _5e5;
}else{
return dojo.gfx.color.named[_5e1]||[255,255,255];
}
}
};
dojo.gfx.color.hex2rgb=function(hex){
var _5e7="0123456789ABCDEF";
var rgb=new Array(3);
if(hex.indexOf("#")==0){
hex=hex.substring(1);
}
hex=hex.toUpperCase();
if(hex.replace(new RegExp("["+_5e7+"]","g"),"")!=""){
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
rgb[i]=_5e7.indexOf(rgb[i].charAt(0))*16+_5e7.indexOf(rgb[i].charAt(1));
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
dojo.lfx.Line=function(_5f0,end){
this.start=_5f0;
this.end=end;
if(dojo.lang.isArray(_5f0)){
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
var diff=end-_5f0;
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
dojo.lang.extend(dojo.lfx.IAnimation,{curve:null,duration:1000,easing:null,repeatCount:0,rate:25,handler:null,beforeBegin:null,onBegin:null,onAnimate:null,onEnd:null,onPlay:null,onPause:null,onStop:null,play:null,pause:null,stop:null,connect:function(evt,_5ff,_600){
if(!_600){
_600=_5ff;
_5ff=this;
}
_600=dojo.lang.hitch(_5ff,_600);
var _601=this[evt]||function(){
};
this[evt]=function(){
var ret=_601.apply(this,arguments);
_600.apply(this,arguments);
return ret;
};
return this;
},fire:function(evt,args){
if(this[evt]){
this[evt].apply(this,(args||[]));
}
return this;
},repeat:function(_605){
this.repeatCount=_605;
return this;
},_active:false,_paused:false});
dojo.lfx.Animation=function(_606,_607,_608,_609,_60a,rate){
dojo.lfx.IAnimation.call(this);
if(dojo.lang.isNumber(_606)||(!_606&&_607.getValue)){
rate=_60a;
_60a=_609;
_609=_608;
_608=_607;
_607=_606;
_606=null;
}else{
if(_606.getValue||dojo.lang.isArray(_606)){
rate=_609;
_60a=_608;
_609=_607;
_608=_606;
_607=null;
_606=null;
}
}
if(dojo.lang.isArray(_608)){
this.curve=new dojo.lfx.Line(_608[0],_608[1]);
}else{
this.curve=_608;
}
if(_607!=null&&_607>0){
this.duration=_607;
}
if(_60a){
this.repeatCount=_60a;
}
if(rate){
this.rate=rate;
}
if(_606){
dojo.lang.forEach(["handler","beforeBegin","onBegin","onEnd","onPlay","onStop","onAnimate"],function(item){
if(_606[item]){
this.connect(item,_606[item]);
}
},this);
}
if(_609&&dojo.lang.isFunction(_609)){
this.easing=_609;
}
};
dojo.inherits(dojo.lfx.Animation,dojo.lfx.IAnimation);
dojo.lang.extend(dojo.lfx.Animation,{_startTime:null,_endTime:null,_timer:null,_percent:0,_startRepeatCount:0,play:function(_60d,_60e){
if(_60e){
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
if(_60d>0){
setTimeout(dojo.lang.hitch(this,function(){
this.play(null,_60e);
}),_60d);
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
var _610=this.curve.getValue(step);
if(this._percent==0){
if(!this._startRepeatCount){
this._startRepeatCount=this.repeatCount;
}
this.fire("handler",["begin",_610]);
this.fire("onBegin",[_610]);
}
this.fire("handler",["play",_610]);
this.fire("onPlay",[_610]);
this._cycle();
return this;
},pause:function(){
clearTimeout(this._timer);
if(!this._active){
return this;
}
this._paused=true;
var _611=this.curve.getValue(this._percent/100);
this.fire("handler",["pause",_611]);
this.fire("onPause",[_611]);
return this;
},gotoPercent:function(pct,_613){
clearTimeout(this._timer);
this._active=true;
this._paused=true;
this._percent=pct;
if(_613){
this.play();
}
return this;
},stop:function(_614){
clearTimeout(this._timer);
var step=this._percent/100;
if(_614){
step=1;
}
var _616=this.curve.getValue(step);
this.fire("handler",["stop",_616]);
this.fire("onStop",[_616]);
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
var _619=this.curve.getValue(step);
this.fire("handler",["animate",_619]);
this.fire("onAnimate",[_619]);
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
dojo.lfx.Combine=function(_61a){
dojo.lfx.IAnimation.call(this);
this._anims=[];
this._animsEnded=0;
var _61b=arguments;
if(_61b.length==1&&(dojo.lang.isArray(_61b[0])||dojo.lang.isArrayLike(_61b[0]))){
_61b=_61b[0];
}
dojo.lang.forEach(_61b,function(anim){
this._anims.push(anim);
anim.connect("onEnd",dojo.lang.hitch(this,"_onAnimsEnded"));
},this);
};
dojo.inherits(dojo.lfx.Combine,dojo.lfx.IAnimation);
dojo.lang.extend(dojo.lfx.Combine,{_animsEnded:0,play:function(_61d,_61e){
if(!this._anims.length){
return this;
}
this.fire("beforeBegin");
if(_61d>0){
setTimeout(dojo.lang.hitch(this,function(){
this.play(null,_61e);
}),_61d);
return this;
}
if(_61e||this._anims[0].percent==0){
this.fire("onBegin");
}
this.fire("onPlay");
this._animsCall("play",null,_61e);
return this;
},pause:function(){
this.fire("onPause");
this._animsCall("pause");
return this;
},stop:function(_61f){
this.fire("onStop");
this._animsCall("stop",_61f);
return this;
},_onAnimsEnded:function(){
this._animsEnded++;
if(this._animsEnded>=this._anims.length){
this.fire("onEnd");
}
return this;
},_animsCall:function(_620){
var args=[];
if(arguments.length>1){
for(var i=1;i<arguments.length;i++){
args.push(arguments[i]);
}
}
var _623=this;
dojo.lang.forEach(this._anims,function(anim){
anim[_620](args);
},_623);
return this;
}});
dojo.lfx.Chain=function(_625){
dojo.lfx.IAnimation.call(this);
this._anims=[];
this._currAnim=-1;
var _626=arguments;
if(_626.length==1&&(dojo.lang.isArray(_626[0])||dojo.lang.isArrayLike(_626[0]))){
_626=_626[0];
}
var _627=this;
dojo.lang.forEach(_626,function(anim,i,_62a){
this._anims.push(anim);
if(i<_62a.length-1){
anim.connect("onEnd",dojo.lang.hitch(this,"_playNext"));
}else{
anim.connect("onEnd",dojo.lang.hitch(this,function(){
this.fire("onEnd");
}));
}
},this);
};
dojo.inherits(dojo.lfx.Chain,dojo.lfx.IAnimation);
dojo.lang.extend(dojo.lfx.Chain,{_currAnim:-1,play:function(_62b,_62c){
if(!this._anims.length){
return this;
}
if(_62c||!this._anims[this._currAnim]){
this._currAnim=0;
}
var _62d=this._anims[this._currAnim];
this.fire("beforeBegin");
if(_62b>0){
setTimeout(dojo.lang.hitch(this,function(){
this.play(null,_62c);
}),_62b);
return this;
}
if(_62d){
if(this._currAnim==0){
this.fire("handler",["begin",this._currAnim]);
this.fire("onBegin",[this._currAnim]);
}
this.fire("onPlay",[this._currAnim]);
_62d.play(null,_62c);
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
var _62e=this._anims[this._currAnim];
if(_62e){
if(!_62e._active||_62e._paused){
this.play();
}else{
this.pause();
}
}
return this;
},stop:function(){
var _62f=this._anims[this._currAnim];
if(_62f){
_62f.stop();
this.fire("onStop",[this._currAnim]);
}
return _62f;
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
dojo.lfx.combine=function(_630){
var _631=arguments;
if(dojo.lang.isArray(arguments[0])){
_631=arguments[0];
}
if(_631.length==1){
return _631[0];
}
return new dojo.lfx.Combine(_631);
};
dojo.lfx.chain=function(_632){
var _633=arguments;
if(dojo.lang.isArray(arguments[0])){
_633=arguments[0];
}
if(_633.length==1){
return _633[0];
}
return new dojo.lfx.Chain(_633);
};
dojo.provide("dojo.html.color");
dojo.html.getBackgroundColor=function(node){
node=dojo.byId(node);
var _635;
do{
_635=dojo.html.getStyle(node,"background-color");
if(_635.toLowerCase()=="rgba(0, 0, 0, 0)"){
_635="transparent";
}
if(node==document.getElementsByTagName("body")[0]){
node=null;
break;
}
node=node.parentNode;
}while(node&&dojo.lang.inArray(["transparent",""],_635));
if(_635=="transparent"){
_635=[255,255,255,0];
}else{
_635=dojo.gfx.color.extractRGB(_635);
}
return _635;
};
dojo.provide("dojo.lfx.html");
dojo.lfx.html._byId=function(_636){
if(!_636){
return [];
}
if(dojo.lang.isArrayLike(_636)){
if(!_636.alreadyChecked){
var n=[];
dojo.lang.forEach(_636,function(node){
n.push(dojo.byId(node));
});
n.alreadyChecked=true;
return n;
}else{
return _636;
}
}else{
var n=[];
n.push(dojo.byId(_636));
n.alreadyChecked=true;
return n;
}
};
dojo.lfx.html.propertyAnimation=function(_639,_63a,_63b,_63c,_63d){
_639=dojo.lfx.html._byId(_639);
var _63e={"propertyMap":_63a,"nodes":_639,"duration":_63b,"easing":_63c||dojo.lfx.easeDefault};
var _63f=function(args){
if(args.nodes.length==1){
var pm=args.propertyMap;
if(!dojo.lang.isArray(args.propertyMap)){
var parr=[];
for(var _643 in pm){
pm[_643].property=_643;
parr.push(pm[_643]);
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
var _645=function(_646){
var _647=[];
dojo.lang.forEach(_646,function(c){
_647.push(Math.round(c));
});
return _647;
};
var _649=function(n,_64b){
n=dojo.byId(n);
if(!n||!n.style){
return;
}
for(var s in _64b){
try{
if(s=="opacity"){
dojo.html.setOpacity(n,_64b[s]);
}else{
n.style[s]=_64b[s];
}
}
catch(e){
dojo.debug(e);
}
}
};
var _64d=function(_64e){
this._properties=_64e;
this.diffs=new Array(_64e.length);
dojo.lang.forEach(_64e,function(prop,i){
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
var _655=null;
if(dojo.lang.isArray(prop.start)){
}else{
if(prop.start instanceof dojo.gfx.color.Color){
_655=(prop.units||"rgb")+"(";
for(var j=0;j<prop.startRgb.length;j++){
_655+=Math.round(((prop.endRgb[j]-prop.startRgb[j])*n)+prop.startRgb[j])+(j<prop.startRgb.length-1?",":"");
}
_655+=")";
}else{
_655=((this.diffs[i])*n)+prop.start+(prop.property!="opacity"?prop.units||"px":"");
}
}
ret[dojo.html.toCamelCase(prop.property)]=_655;
},this);
return ret;
};
};
var anim=new dojo.lfx.Animation({beforeBegin:function(){
_63f(_63e);
anim.curve=new _64d(_63e.propertyMap);
},onAnimate:function(_658){
dojo.lang.forEach(_63e.nodes,function(node){
_649(node,_658);
});
}},_63e.duration,null,_63e.easing);
if(_63d){
for(var x in _63d){
if(dojo.lang.isFunction(_63d[x])){
anim.connect(x,anim,_63d[x]);
}
}
}
return anim;
};
dojo.lfx.html._makeFadeable=function(_65b){
var _65c=function(node){
if(dojo.render.html.ie){
if((node.style.zoom.length==0)&&(dojo.html.getStyle(node,"zoom")=="normal")){
node.style.zoom="1";
}
if((node.style.width.length==0)&&(dojo.html.getStyle(node,"width")=="auto")){
node.style.width="auto";
}
}
};
if(dojo.lang.isArrayLike(_65b)){
dojo.lang.forEach(_65b,_65c);
}else{
_65c(_65b);
}
};
dojo.lfx.html.fade=function(_65e,_65f,_660,_661,_662){
_65e=dojo.lfx.html._byId(_65e);
var _663={property:"opacity"};
if(!dj_undef("start",_65f)){
_663.start=_65f.start;
}else{
_663.start=function(){
return dojo.html.getOpacity(_65e[0]);
};
}
if(!dj_undef("end",_65f)){
_663.end=_65f.end;
}else{
dojo.raise("dojo.lfx.html.fade needs an end value");
}
var anim=dojo.lfx.propertyAnimation(_65e,[_663],_660,_661);
anim.connect("beforeBegin",function(){
dojo.lfx.html._makeFadeable(_65e);
});
if(_662){
anim.connect("onEnd",function(){
_662(_65e,anim);
});
}
return anim;
};
dojo.lfx.html.fadeIn=function(_665,_666,_667,_668){
return dojo.lfx.html.fade(_665,{end:1},_666,_667,_668);
};
dojo.lfx.html.fadeOut=function(_669,_66a,_66b,_66c){
return dojo.lfx.html.fade(_669,{end:0},_66a,_66b,_66c);
};
dojo.lfx.html.fadeShow=function(_66d,_66e,_66f,_670){
_66d=dojo.lfx.html._byId(_66d);
dojo.lang.forEach(_66d,function(node){
dojo.html.setOpacity(node,0);
});
var anim=dojo.lfx.html.fadeIn(_66d,_66e,_66f,_670);
anim.connect("beforeBegin",function(){
if(dojo.lang.isArrayLike(_66d)){
dojo.lang.forEach(_66d,dojo.html.show);
}else{
dojo.html.show(_66d);
}
});
return anim;
};
dojo.lfx.html.fadeHide=function(_673,_674,_675,_676){
var anim=dojo.lfx.html.fadeOut(_673,_674,_675,function(){
if(dojo.lang.isArrayLike(_673)){
dojo.lang.forEach(_673,dojo.html.hide);
}else{
dojo.html.hide(_673);
}
if(_676){
_676(_673,anim);
}
});
return anim;
};
dojo.lfx.html.wipeIn=function(_678,_679,_67a,_67b){
_678=dojo.lfx.html._byId(_678);
var _67c=[];
dojo.lang.forEach(_678,function(node){
var _67e={};
var _67f,_680,_681;
with(node.style){
_67f=top;
_680=left;
_681=position;
top="-9999px";
left="-9999px";
position="absolute";
display="";
}
var _682=dojo.html.getBorderBox(node).height;
with(node.style){
top=_67f;
left=_680;
position=_681;
display="none";
}
var anim=dojo.lfx.propertyAnimation(node,{"height":{start:1,end:function(){
return _682;
}}},_679,_67a);
anim.connect("beforeBegin",function(){
_67e.overflow=node.style.overflow;
_67e.height=node.style.height;
with(node.style){
overflow="hidden";
_682="1px";
}
dojo.html.show(node);
});
anim.connect("onEnd",function(){
with(node.style){
overflow=_67e.overflow;
_682=_67e.height;
}
if(_67b){
_67b(node,anim);
}
});
_67c.push(anim);
});
return dojo.lfx.combine(_67c);
};
dojo.lfx.html.wipeOut=function(_684,_685,_686,_687){
_684=dojo.lfx.html._byId(_684);
var _688=[];
dojo.lang.forEach(_684,function(node){
var _68a={};
var anim=dojo.lfx.propertyAnimation(node,{"height":{start:function(){
return dojo.html.getContentBox(node).height;
},end:1}},_685,_686,{"beforeBegin":function(){
_68a.overflow=node.style.overflow;
_68a.height=node.style.height;
with(node.style){
overflow="hidden";
}
dojo.html.show(node);
},"onEnd":function(){
dojo.html.hide(node);
with(node.style){
overflow=_68a.overflow;
height=_68a.height;
}
if(_687){
_687(node,anim);
}
}});
_688.push(anim);
});
return dojo.lfx.combine(_688);
};
dojo.lfx.html.slideTo=function(_68c,_68d,_68e,_68f,_690){
_68c=dojo.lfx.html._byId(_68c);
var _691=[];
var _692=dojo.html.getComputedStyle;
if(dojo.lang.isArray(_68d)){
dojo.deprecated("dojo.lfx.html.slideTo(node, array)","use dojo.lfx.html.slideTo(node, {top: value, left: value});","0.5");
_68d={top:_68d[0],left:_68d[1]};
}
dojo.lang.forEach(_68c,function(node){
var top=null;
var left=null;
var init=(function(){
var _697=node;
return function(){
var pos=_692(_697,"position");
top=(pos=="absolute"?node.offsetTop:parseInt(_692(node,"top"))||0);
left=(pos=="absolute"?node.offsetLeft:parseInt(_692(node,"left"))||0);
if(!dojo.lang.inArray(["absolute","relative"],pos)){
var ret=dojo.html.abs(_697,true);
dojo.html.setStyleAttributes(_697,"position:absolute;top:"+ret.y+"px;left:"+ret.x+"px;");
top=ret.y;
left=ret.x;
}
};
})();
init();
var anim=dojo.lfx.propertyAnimation(node,{"top":{start:top,end:(_68d.top||0)},"left":{start:left,end:(_68d.left||0)}},_68e,_68f,{"beforeBegin":init});
if(_690){
anim.connect("onEnd",function(){
_690(_68c,anim);
});
}
_691.push(anim);
});
return dojo.lfx.combine(_691);
};
dojo.lfx.html.slideBy=function(_69b,_69c,_69d,_69e,_69f){
_69b=dojo.lfx.html._byId(_69b);
var _6a0=[];
var _6a1=dojo.html.getComputedStyle;
if(dojo.lang.isArray(_69c)){
dojo.deprecated("dojo.lfx.html.slideBy(node, array)","use dojo.lfx.html.slideBy(node, {top: value, left: value});","0.5");
_69c={top:_69c[0],left:_69c[1]};
}
dojo.lang.forEach(_69b,function(node){
var top=null;
var left=null;
var init=(function(){
var _6a6=node;
return function(){
var pos=_6a1(_6a6,"position");
top=(pos=="absolute"?node.offsetTop:parseInt(_6a1(node,"top"))||0);
left=(pos=="absolute"?node.offsetLeft:parseInt(_6a1(node,"left"))||0);
if(!dojo.lang.inArray(["absolute","relative"],pos)){
var ret=dojo.html.abs(_6a6,true);
dojo.html.setStyleAttributes(_6a6,"position:absolute;top:"+ret.y+"px;left:"+ret.x+"px;");
top=ret.y;
left=ret.x;
}
};
})();
init();
var anim=dojo.lfx.propertyAnimation(node,{"top":{start:top,end:top+(_69c.top||0)},"left":{start:left,end:left+(_69c.left||0)}},_69d,_69e).connect("beforeBegin",init);
if(_69f){
anim.connect("onEnd",function(){
_69f(_69b,anim);
});
}
_6a0.push(anim);
});
return dojo.lfx.combine(_6a0);
};
dojo.lfx.html.explode=function(_6aa,_6ab,_6ac,_6ad,_6ae){
var h=dojo.html;
_6aa=dojo.byId(_6aa);
_6ab=dojo.byId(_6ab);
var _6b0=h.toCoordinateObject(_6aa,true);
var _6b1=document.createElement("div");
h.copyStyle(_6b1,_6ab);
if(_6ab.explodeClassName){
_6b1.className=_6ab.explodeClassName;
}
with(_6b1.style){
position="absolute";
display="none";
var _6b2=h.getStyle(_6aa,"background-color");
backgroundColor=_6b2?_6b2.toLowerCase():"transparent";
backgroundColor=(backgroundColor=="transparent")?"rgb(221, 221, 221)":backgroundColor;
}
dojo.body().appendChild(_6b1);
with(_6ab.style){
visibility="hidden";
display="block";
}
var _6b3=h.toCoordinateObject(_6ab,true);
with(_6ab.style){
display="none";
visibility="visible";
}
var _6b4={opacity:{start:0.5,end:1}};
dojo.lang.forEach(["height","width","top","left"],function(type){
_6b4[type]={start:_6b0[type],end:_6b3[type]};
});
var anim=new dojo.lfx.propertyAnimation(_6b1,_6b4,_6ac,_6ad,{"beforeBegin":function(){
h.setDisplay(_6b1,"block");
},"onEnd":function(){
h.setDisplay(_6ab,"block");
_6b1.parentNode.removeChild(_6b1);
}});
if(_6ae){
anim.connect("onEnd",function(){
_6ae(_6ab,anim);
});
}
return anim;
};
dojo.lfx.html.implode=function(_6b7,end,_6b9,_6ba,_6bb){
var h=dojo.html;
_6b7=dojo.byId(_6b7);
end=dojo.byId(end);
var _6bd=dojo.html.toCoordinateObject(_6b7,true);
var _6be=dojo.html.toCoordinateObject(end,true);
var _6bf=document.createElement("div");
dojo.html.copyStyle(_6bf,_6b7);
if(_6b7.explodeClassName){
_6bf.className=_6b7.explodeClassName;
}
dojo.html.setOpacity(_6bf,0.3);
with(_6bf.style){
position="absolute";
display="none";
backgroundColor=h.getStyle(_6b7,"background-color").toLowerCase();
}
dojo.body().appendChild(_6bf);
var _6c0={opacity:{start:1,end:0.5}};
dojo.lang.forEach(["height","width","top","left"],function(type){
_6c0[type]={start:_6bd[type],end:_6be[type]};
});
var anim=new dojo.lfx.propertyAnimation(_6bf,_6c0,_6b9,_6ba,{"beforeBegin":function(){
dojo.html.hide(_6b7);
dojo.html.show(_6bf);
},"onEnd":function(){
_6bf.parentNode.removeChild(_6bf);
}});
if(_6bb){
anim.connect("onEnd",function(){
_6bb(_6b7,anim);
});
}
return anim;
};
dojo.lfx.html.highlight=function(_6c3,_6c4,_6c5,_6c6,_6c7){
_6c3=dojo.lfx.html._byId(_6c3);
var _6c8=[];
dojo.lang.forEach(_6c3,function(node){
var _6ca=dojo.html.getBackgroundColor(node);
var bg=dojo.html.getStyle(node,"background-color").toLowerCase();
var _6cc=dojo.html.getStyle(node,"background-image");
var _6cd=(bg=="transparent"||bg=="rgba(0, 0, 0, 0)");
while(_6ca.length>3){
_6ca.pop();
}
var rgb=new dojo.gfx.color.Color(_6c4);
var _6cf=new dojo.gfx.color.Color(_6ca);
var anim=dojo.lfx.propertyAnimation(node,{"background-color":{start:rgb,end:_6cf}},_6c5,_6c6,{"beforeBegin":function(){
if(_6cc){
node.style.backgroundImage="none";
}
node.style.backgroundColor="rgb("+rgb.toRgb().join(",")+")";
},"onEnd":function(){
if(_6cc){
node.style.backgroundImage=_6cc;
}
if(_6cd){
node.style.backgroundColor="transparent";
}
if(_6c7){
_6c7(node,anim);
}
}});
_6c8.push(anim);
});
return dojo.lfx.combine(_6c8);
};
dojo.lfx.html.unhighlight=function(_6d1,_6d2,_6d3,_6d4,_6d5){
_6d1=dojo.lfx.html._byId(_6d1);
var _6d6=[];
dojo.lang.forEach(_6d1,function(node){
var _6d8=new dojo.gfx.color.Color(dojo.html.getBackgroundColor(node));
var rgb=new dojo.gfx.color.Color(_6d2);
var _6da=dojo.html.getStyle(node,"background-image");
var anim=dojo.lfx.propertyAnimation(node,{"background-color":{start:_6d8,end:rgb}},_6d3,_6d4,{"beforeBegin":function(){
if(_6da){
node.style.backgroundImage="none";
}
node.style.backgroundColor="rgb("+_6d8.toRgb().join(",")+")";
},"onEnd":function(){
if(_6d5){
_6d5(node,anim);
}
}});
_6d6.push(anim);
});
return dojo.lfx.combine(_6d6);
};
dojo.lang.mixin(dojo.lfx,dojo.lfx.html);
dojo.provide("dojo.lfx.*");
dojo.provide("dojo.lfx.toggle");
dojo.lfx.toggle.plain={show:function(node,_6dd,_6de,_6df){
dojo.html.show(node);
if(dojo.lang.isFunction(_6df)){
_6df();
}
},hide:function(node,_6e1,_6e2,_6e3){
dojo.html.hide(node);
if(dojo.lang.isFunction(_6e3)){
_6e3();
}
}};
dojo.lfx.toggle.fade={show:function(node,_6e5,_6e6,_6e7){
dojo.lfx.fadeShow(node,_6e5,_6e6,_6e7).play();
},hide:function(node,_6e9,_6ea,_6eb){
dojo.lfx.fadeHide(node,_6e9,_6ea,_6eb).play();
}};
dojo.lfx.toggle.wipe={show:function(node,_6ed,_6ee,_6ef){
dojo.lfx.wipeIn(node,_6ed,_6ee,_6ef).play();
},hide:function(node,_6f1,_6f2,_6f3){
dojo.lfx.wipeOut(node,_6f1,_6f2,_6f3).play();
}};
dojo.lfx.toggle.explode={show:function(node,_6f5,_6f6,_6f7,_6f8){
dojo.lfx.explode(_6f8||{x:0,y:0,width:0,height:0},node,_6f5,_6f6,_6f7).play();
},hide:function(node,_6fa,_6fb,_6fc,_6fd){
dojo.lfx.implode(node,_6fd||{x:0,y:0,width:0,height:0},_6fa,_6fb,_6fc).play();
}};
dojo.provide("dojo.widget.HtmlWidget");
dojo.declare("dojo.widget.HtmlWidget",dojo.widget.DomWidget,{templateCssPath:null,templatePath:null,lang:"",toggle:"plain",toggleDuration:150,initialize:function(args,frag){
},postMixInProperties:function(args,frag){
if(this.lang===""){
this.lang=null;
}
this.toggleObj=dojo.lfx.toggle[this.toggle.toLowerCase()]||dojo.lfx.toggle.plain;
},createNodesFromText:function(txt,wrap){
return dojo.html.createNodesFromText(txt,wrap);
},destroyRendering:function(_704){
try{
if(this.bgIframe){
this.bgIframe.remove();
delete this.bgIframe;
}
if(!_704&&this.domNode){
dojo.event.browser.clean(this.domNode);
}
dojo.widget.HtmlWidget.superclass.destroyRendering.call(this);
}
catch(e){
}
},isShowing:function(){
return dojo.html.isShowing(this.domNode);
},toggleShowing:function(){
if(this.isShowing()){
this.hide();
}else{
this.show();
}
},show:function(){
if(this.isShowing()){
return;
}
this.animationInProgress=true;
this.toggleObj.show(this.domNode,this.toggleDuration,null,dojo.lang.hitch(this,this.onShow),this.explodeSrc);
},onShow:function(){
this.animationInProgress=false;
this.checkSize();
},hide:function(){
if(!this.isShowing()){
return;
}
this.animationInProgress=true;
this.toggleObj.hide(this.domNode,this.toggleDuration,null,dojo.lang.hitch(this,this.onHide),this.explodeSrc);
},onHide:function(){
this.animationInProgress=false;
},_isResized:function(w,h){
if(!this.isShowing()){
return false;
}
var wh=dojo.html.getMarginBox(this.domNode);
var _708=w||wh.width;
var _709=h||wh.height;
if(this.width==_708&&this.height==_709){
return false;
}
this.width=_708;
this.height=_709;
return true;
},checkSize:function(){
if(!this._isResized()){
return;
}
this.onResized();
},resizeTo:function(w,h){
dojo.html.setMarginBox(this.domNode,{width:w,height:h});
if(this.isShowing()){
this.onResized();
}
},resizeSoon:function(){
if(this.isShowing()){
dojo.lang.setTimeout(this,this.onResized,0);
}
},onResized:function(){
dojo.lang.forEach(this.children,function(_70c){
if(_70c.checkSize){
_70c.checkSize();
}
});
}});
dojo.provide("dojo.widget.*");
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
dojo.string.repeat=function(str,_713,_714){
var out="";
for(var i=0;i<_713;i++){
out+=str;
if(_714&&i<_713-1){
out+=_714;
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
dojo.provide("dojo.io.common");
dojo.io.transports=[];
dojo.io.hdlrFuncNames=["load","error","timeout"];
dojo.io.Request=function(url,_723,_724,_725){
if((arguments.length==1)&&(arguments[0].constructor==Object)){
this.fromKwArgs(arguments[0]);
}else{
this.url=url;
if(_723){
this.mimetype=_723;
}
if(_724){
this.transport=_724;
}
if(arguments.length>=4){
this.changeUrl=_725;
}
}
};
dojo.lang.extend(dojo.io.Request,{url:"",mimetype:"text/plain",method:"GET",content:undefined,transport:undefined,changeUrl:undefined,formNode:undefined,sync:false,bindSuccess:false,useCache:false,preventCache:false,load:function(type,data,_728,_729){
},error:function(type,_72b,_72c,_72d){
},timeout:function(type,_72f,_730,_731){
},handle:function(type,data,_734,_735){
},timeoutSeconds:0,abort:function(){
},fromKwArgs:function(_736){
if(_736["url"]){
_736.url=_736.url.toString();
}
if(_736["formNode"]){
_736.formNode=dojo.byId(_736.formNode);
}
if(!_736["method"]&&_736["formNode"]&&_736["formNode"].method){
_736.method=_736["formNode"].method;
}
if(!_736["handle"]&&_736["handler"]){
_736.handle=_736.handler;
}
if(!_736["load"]&&_736["loaded"]){
_736.load=_736.loaded;
}
if(!_736["changeUrl"]&&_736["changeURL"]){
_736.changeUrl=_736.changeURL;
}
_736.encoding=dojo.lang.firstValued(_736["encoding"],djConfig["bindEncoding"],"");
_736.sendTransport=dojo.lang.firstValued(_736["sendTransport"],djConfig["ioSendTransport"],false);
var _737=dojo.lang.isFunction;
for(var x=0;x<dojo.io.hdlrFuncNames.length;x++){
var fn=dojo.io.hdlrFuncNames[x];
if(_736[fn]&&_737(_736[fn])){
continue;
}
if(_736["handle"]&&_737(_736["handle"])){
_736[fn]=_736.handle;
}
}
dojo.lang.mixin(this,_736);
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
dojo.io.bind=function(_73e){
if(!(_73e instanceof dojo.io.Request)){
try{
_73e=new dojo.io.Request(_73e);
}
catch(e){
dojo.debug(e);
}
}
var _73f="";
if(_73e["transport"]){
_73f=_73e["transport"];
if(!this[_73f]){
dojo.io.sendBindError(_73e,"No dojo.io.bind() transport with name '"+_73e["transport"]+"'.");
return _73e;
}
if(!this[_73f].canHandle(_73e)){
dojo.io.sendBindError(_73e,"dojo.io.bind() transport with name '"+_73e["transport"]+"' cannot handle this type of request.");
return _73e;
}
}else{
for(var x=0;x<dojo.io.transports.length;x++){
var tmp=dojo.io.transports[x];
if((this[tmp])&&(this[tmp].canHandle(_73e))){
_73f=tmp;
break;
}
}
if(_73f==""){
dojo.io.sendBindError(_73e,"None of the loaded transports for dojo.io.bind()"+" can handle the request.");
return _73e;
}
}
this[_73f].bind(_73e);
_73e.bindSuccess=true;
return _73e;
};
dojo.io.sendBindError=function(_742,_743){
if((typeof _742.error=="function"||typeof _742.handle=="function")&&(typeof setTimeout=="function"||typeof setTimeout=="object")){
var _744=new dojo.io.Error(_743);
setTimeout(function(){
_742[(typeof _742.error=="function")?"error":"handle"]("error",_744,null,_742);
},50);
}else{
dojo.raise(_743);
}
};
dojo.io.queueBind=function(_745){
if(!(_745 instanceof dojo.io.Request)){
try{
_745=new dojo.io.Request(_745);
}
catch(e){
dojo.debug(e);
}
}
var _746=_745.load;
_745.load=function(){
dojo.io._queueBindInFlight=false;
var ret=_746.apply(this,arguments);
dojo.io._dispatchNextQueueBind();
return ret;
};
var _748=_745.error;
_745.error=function(){
dojo.io._queueBindInFlight=false;
var ret=_748.apply(this,arguments);
dojo.io._dispatchNextQueueBind();
return ret;
};
dojo.io._bindQueue.push(_745);
dojo.io._dispatchNextQueueBind();
return _745;
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
dojo.io.argsFromMap=function(map,_74b,last){
var enc=/utf/i.test(_74b||"")?encodeURIComponent:dojo.string.encodeAscii;
var _74e=[];
var _74f=new Object();
for(var name in map){
var _751=function(elt){
var val=enc(name)+"="+enc(elt);
_74e[(last==name)?"push":"unshift"](val);
};
if(!_74f[name]){
var _754=map[name];
if(dojo.lang.isArray(_754)){
dojo.lang.forEach(_754,_751);
}else{
_751(_754);
}
}
}
return _74e.join("&");
};
dojo.io.setIFrameSrc=function(_755,src,_757){
try{
var r=dojo.render.html;
if(!_757){
if(r.safari){
_755.location=src;
}else{
frames[_755.name].location=src;
}
}else{
var idoc;
if(r.ie){
idoc=_755.contentWindow.document;
}else{
if(r.safari){
idoc=_755.document;
}else{
idoc=_755.contentWindow;
}
}
if(!idoc){
_755.location=src;
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
dojo.provide("dojo.string.extras");
dojo.string.substituteParams=function(_75a,hash){
var map=(typeof hash=="object")?hash:dojo.lang.toArray(arguments,1);
return _75a.replace(/\%\{(\w+)\}/g,function(_75d,key){
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
var _760=str.split(" ");
for(var i=0;i<_760.length;i++){
_760[i]=_760[i].charAt(0).toUpperCase()+_760[i].substring(1);
}
return _760.join(" ");
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
var _765=escape(str);
var _766,re=/%u([0-9A-F]{4})/i;
while((_766=_765.match(re))){
var num=Number("0x"+_766[1]);
var _769=escape("&#"+num+";");
ret+=_765.substring(0,_766.index)+_769;
_765=_765.substring(_766.index+_766[0].length);
}
ret+=_765.replace(/\+/g,"%2B");
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
dojo.string.escapeXml=function(str,_76e){
str=str.replace(/&/gm,"&amp;").replace(/</gm,"&lt;").replace(/>/gm,"&gt;").replace(/"/gm,"&quot;");
if(!_76e){
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
dojo.string.endsWith=function(str,end,_777){
if(_777){
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
dojo.string.startsWith=function(str,_77b,_77c){
if(_77c){
str=str.toLowerCase();
_77b=_77b.toLowerCase();
}
return str.indexOf(_77b)==0;
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
dojo.string.normalizeNewlines=function(text,_782){
if(_782=="\n"){
text=text.replace(/\r\n/g,"\n");
text=text.replace(/\r/g,"\n");
}else{
if(_782=="\r"){
text=text.replace(/\r\n/g,"\r");
text=text.replace(/\n/g,"\r");
}else{
text=text.replace(/([^\r])\n/g,"$1\r\n").replace(/\r([^\n])/g,"\r\n$1");
}
}
return text;
};
dojo.string.splitEscaped=function(str,_784){
var _785=[];
for(var i=0,_787=0;i<str.length;i++){
if(str.charAt(i)=="\\"){
i++;
continue;
}
if(str.charAt(i)==_784){
_785.push(str.substring(_787,i));
_787=i+1;
}
}
_785.push(str.substr(_787));
return _785;
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
var _78c=args["back"]||args["backButton"]||args["handle"];
var tcb=function(_78e){
if(window.location.hash!=""){
setTimeout("window.location.href = '"+hash+"';",1);
}
_78c.apply(this,[_78e]);
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
var _78f=args["forward"]||args["forwardButton"]||args["handle"];
var tfw=function(_791){
if(window.location.hash!=""){
window.location.href=hash;
}
if(_78f){
_78f.apply(this,[_791]);
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
},iframeLoaded:function(evt,_794){
if(!dojo.render.html.opera){
var _795=this._getUrlQuery(_794.href);
if(_795==null){
if(this.historyStack.length==1){
this.handleBackButton();
}
return;
}
if(this.moveForward){
this.moveForward=false;
return;
}
if(this.historyStack.length>=2&&_795==this._getUrlQuery(this.historyStack[this.historyStack.length-2].url)){
this.handleBackButton();
}else{
if(this.forwardStack.length>0&&_795==this._getUrlQuery(this.forwardStack[this.forwardStack.length-1].url)){
this.handleForwardButton();
}
}
}
},handleBackButton:function(){
var _796=this.historyStack.pop();
if(!_796){
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
this.forwardStack.push(_796);
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
var _79d=url.split("?");
if(_79d.length<2){
return null;
}else{
return _79d[1];
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
var _7a0=false;
var _7a1=node.getElementsByTagName("input");
dojo.lang.forEach(_7a1,function(_7a2){
if(_7a0){
return;
}
if(_7a2.getAttribute("type")=="file"){
_7a0=true;
}
});
return _7a0;
};
dojo.io.formHasFile=function(_7a3){
return dojo.io.checkChildrenForFile(_7a3);
};
dojo.io.updateNode=function(node,_7a5){
node=dojo.byId(node);
var args=_7a5;
if(dojo.lang.isString(_7a5)){
args={url:_7a5};
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
dojo.io.encodeForm=function(_7ac,_7ad,_7ae){
if((!_7ac)||(!_7ac.tagName)||(!_7ac.tagName.toLowerCase()=="form")){
dojo.raise("Attempted to encode a non-form element.");
}
if(!_7ae){
_7ae=dojo.io.formFilter;
}
var enc=/utf/i.test(_7ad||"")?encodeURIComponent:dojo.string.encodeAscii;
var _7b0=[];
for(var i=0;i<_7ac.elements.length;i++){
var elm=_7ac.elements[i];
if(!elm||elm.tagName.toLowerCase()=="fieldset"||!_7ae(elm)){
continue;
}
var name=enc(elm.name);
var type=elm.type.toLowerCase();
if(type=="select-multiple"){
for(var j=0;j<elm.options.length;j++){
if(elm.options[j].selected){
_7b0.push(name+"="+enc(elm.options[j].value));
}
}
}else{
if(dojo.lang.inArray(["radio","checkbox"],type)){
if(elm.checked){
_7b0.push(name+"="+enc(elm.value));
}
}else{
_7b0.push(name+"="+enc(elm.value));
}
}
}
var _7b6=_7ac.getElementsByTagName("input");
for(var i=0;i<_7b6.length;i++){
var _7b7=_7b6[i];
if(_7b7.type.toLowerCase()=="image"&&_7b7.form==_7ac&&_7ae(_7b7)){
var name=enc(_7b7.name);
_7b0.push(name+"="+enc(_7b7.value));
_7b0.push(name+".x=0");
_7b0.push(name+".y=0");
}
}
return _7b0.join("&")+"&";
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
var _7bd=form.getElementsByTagName("input");
for(var i=0;i<_7bd.length;i++){
var _7be=_7bd[i];
if(_7be.type.toLowerCase()=="image"&&_7be.form==form){
this.connect(_7be,"onclick","click");
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
var _7c5=false;
if(node.disabled||!node.name){
_7c5=false;
}else{
if(dojo.lang.inArray(["submit","button","image"],type)){
if(!this.clickedButton){
this.clickedButton=node;
}
_7c5=node==this.clickedButton;
}else{
_7c5=!dojo.lang.inArray(["file","submit","reset","button"],type);
}
}
return _7c5;
},connect:function(_7c6,_7c7,_7c8){
if(dojo.evalObjPath("dojo.event.connect")){
dojo.event.connect(_7c6,_7c7,this,_7c8);
}else{
var fcn=dojo.lang.hitch(this,_7c8);
_7c6[_7c7]=function(e){
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
var _7cb=this;
var _7cc={};
this.useCache=false;
this.preventCache=false;
function getCacheKey(url,_7ce,_7cf){
return url+"|"+_7ce+"|"+_7cf.toLowerCase();
}
function addToCache(url,_7d1,_7d2,http){
_7cc[getCacheKey(url,_7d1,_7d2)]=http;
}
function getFromCache(url,_7d5,_7d6){
return _7cc[getCacheKey(url,_7d5,_7d6)];
}
this.clearCache=function(){
_7cc={};
};
function doLoad(_7d7,http,url,_7da,_7db){
if(((http.status>=200)&&(http.status<300))||(http.status==304)||(location.protocol=="file:"&&(http.status==0||http.status==undefined))||(location.protocol=="chrome:"&&(http.status==0||http.status==undefined))){
var ret;
if(_7d7.method.toLowerCase()=="head"){
var _7dd=http.getAllResponseHeaders();
ret={};
ret.toString=function(){
return _7dd;
};
var _7de=_7dd.split(/[\r\n]+/g);
for(var i=0;i<_7de.length;i++){
var pair=_7de[i].match(/^([^:]+)\s*:\s*(.+)$/i);
if(pair){
ret[pair[1]]=pair[2];
}
}
}else{
if(_7d7.mimetype=="text/javascript"){
try{
ret=dj_eval(http.responseText);
}
catch(e){
dojo.debug(e);
dojo.debug(http.responseText);
ret=null;
}
}else{
if(_7d7.mimetype=="text/json"||_7d7.mimetype=="application/json"){
try{
ret=dj_eval("("+http.responseText+")");
}
catch(e){
dojo.debug(e);
dojo.debug(http.responseText);
ret=false;
}
}else{
if((_7d7.mimetype=="application/xml")||(_7d7.mimetype=="text/xml")){
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
if(_7db){
addToCache(url,_7da,_7d7.method,http);
}
_7d7[(typeof _7d7.load=="function")?"load":"handle"]("load",ret,http,_7d7);
}else{
var _7e1=new dojo.io.Error("XMLHttpTransport Error: "+http.status+" "+http.statusText);
_7d7[(typeof _7d7.error=="function")?"error":"handle"]("error",_7e1,http,_7d7);
}
}
function setHeaders(http,_7e3){
if(_7e3["headers"]){
for(var _7e4 in _7e3["headers"]){
if(_7e4.toLowerCase()=="content-type"&&!_7e3["contentType"]){
_7e3["contentType"]=_7e3["headers"][_7e4];
}else{
http.setRequestHeader(_7e4,_7e3["headers"][_7e4]);
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
if(!dojo.hostenv._blockAsync&&!_7cb._blockAsync){
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
var _7e8=new dojo.io.Error("XMLHttpTransport.watchInFlight Error: "+e);
tif.req[(typeof tif.req.error=="function")?"error":"handle"]("error",_7e8,tif.http,tif.req);
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
var _7e9=dojo.hostenv.getXmlhttpObject()?true:false;
this.canHandle=function(_7ea){
return _7e9&&dojo.lang.inArray(["text/plain","text/html","application/xml","text/xml","text/javascript","text/json","application/json"],(_7ea["mimetype"].toLowerCase()||""))&&!(_7ea["formNode"]&&dojo.io.formHasFile(_7ea["formNode"]));
};
this.multipartBoundary="45309FFF-BD65-4d50-99C9-36986896A96F";
this.bind=function(_7eb){
if(!_7eb["url"]){
if(!_7eb["formNode"]&&(_7eb["backButton"]||_7eb["back"]||_7eb["changeUrl"]||_7eb["watchForURL"])&&(!djConfig.preventBackButtonFix)){
dojo.deprecated("Using dojo.io.XMLHTTPTransport.bind() to add to browser history without doing an IO request","Use dojo.undo.browser.addToHistory() instead.","0.4");
dojo.undo.browser.addToHistory(_7eb);
return true;
}
}
var url=_7eb.url;
var _7ed="";
if(_7eb["formNode"]){
var ta=_7eb.formNode.getAttribute("action");
if((ta)&&(!_7eb["url"])){
url=ta;
}
var tp=_7eb.formNode.getAttribute("method");
if((tp)&&(!_7eb["method"])){
_7eb.method=tp;
}
_7ed+=dojo.io.encodeForm(_7eb.formNode,_7eb.encoding,_7eb["formFilter"]);
}
if(url.indexOf("#")>-1){
dojo.debug("Warning: dojo.io.bind: stripping hash values from url:",url);
url=url.split("#")[0];
}
if(_7eb["file"]){
_7eb.method="post";
}
if(!_7eb["method"]){
_7eb.method="get";
}
if(_7eb.method.toLowerCase()=="get"){
_7eb.multipart=false;
}else{
if(_7eb["file"]){
_7eb.multipart=true;
}else{
if(!_7eb["multipart"]){
_7eb.multipart=false;
}
}
}
if(_7eb["backButton"]||_7eb["back"]||_7eb["changeUrl"]){
dojo.undo.browser.addToHistory(_7eb);
}
var _7f0=_7eb["content"]||{};
if(_7eb.sendTransport){
_7f0["dojo.transport"]="xmlhttp";
}
do{
if(_7eb.postContent){
_7ed=_7eb.postContent;
break;
}
if(_7f0){
_7ed+=dojo.io.argsFromMap(_7f0,_7eb.encoding);
}
if(_7eb.method.toLowerCase()=="get"||!_7eb.multipart){
break;
}
var t=[];
if(_7ed.length){
var q=_7ed.split("&");
for(var i=0;i<q.length;++i){
if(q[i].length){
var p=q[i].split("=");
t.push("--"+this.multipartBoundary,"Content-Disposition: form-data; name=\""+p[0]+"\"","",p[1]);
}
}
}
if(_7eb.file){
if(dojo.lang.isArray(_7eb.file)){
for(var i=0;i<_7eb.file.length;++i){
var o=_7eb.file[i];
t.push("--"+this.multipartBoundary,"Content-Disposition: form-data; name=\""+o.name+"\"; filename=\""+("fileName" in o?o.fileName:o.name)+"\"","Content-Type: "+("contentType" in o?o.contentType:"application/octet-stream"),"",o.content);
}
}else{
var o=_7eb.file;
t.push("--"+this.multipartBoundary,"Content-Disposition: form-data; name=\""+o.name+"\"; filename=\""+("fileName" in o?o.fileName:o.name)+"\"","Content-Type: "+("contentType" in o?o.contentType:"application/octet-stream"),"",o.content);
}
}
if(t.length){
t.push("--"+this.multipartBoundary+"--","");
_7ed=t.join("\r\n");
}
}while(false);
var _7f6=_7eb["sync"]?false:true;
var _7f7=_7eb["preventCache"]||(this.preventCache==true&&_7eb["preventCache"]!=false);
var _7f8=_7eb["useCache"]==true||(this.useCache==true&&_7eb["useCache"]!=false);
if(!_7f7&&_7f8){
var _7f9=getFromCache(url,_7ed,_7eb.method);
if(_7f9){
doLoad(_7eb,_7f9,url,_7ed,false);
return;
}
}
var http=dojo.hostenv.getXmlhttpObject(_7eb);
var _7fb=false;
if(_7f6){
var _7fc=this.inFlight.push({"req":_7eb,"http":http,"url":url,"query":_7ed,"useCache":_7f8,"startTime":_7eb.timeoutSeconds?(new Date()).getTime():0});
this.startWatchingInFlight();
}else{
_7cb._blockAsync=true;
}
if(_7eb.method.toLowerCase()=="post"){
if(!_7eb.user){
http.open("POST",url,_7f6);
}else{
http.open("POST",url,_7f6,_7eb.user,_7eb.password);
}
setHeaders(http,_7eb);
http.setRequestHeader("Content-Type",_7eb.multipart?("multipart/form-data; boundary="+this.multipartBoundary):(_7eb.contentType||"application/x-www-form-urlencoded"));
try{
http.send(_7ed);
}
catch(e){
if(typeof http.abort=="function"){
http.abort();
}
doLoad(_7eb,{status:404},url,_7ed,_7f8);
}
}else{
var _7fd=url;
if(_7ed!=""){
_7fd+=(_7fd.indexOf("?")>-1?"&":"?")+_7ed;
}
if(_7f7){
_7fd+=(dojo.string.endsWithAny(_7fd,"?","&")?"":(_7fd.indexOf("?")>-1?"&":"?"))+"dojo.preventCache="+new Date().valueOf();
}
if(!_7eb.user){
http.open(_7eb.method.toUpperCase(),_7fd,_7f6);
}else{
http.open(_7eb.method.toUpperCase(),_7fd,_7f6,_7eb.user,_7eb.password);
}
setHeaders(http,_7eb);
try{
http.send(null);
}
catch(e){
if(typeof http.abort=="function"){
http.abort();
}
doLoad(_7eb,{status:404},url,_7ed,_7f8);
}
}
if(!_7f6){
doLoad(_7eb,http,url,_7ed,_7f8);
_7cb._blockAsync=false;
}
_7eb.abort=function(){
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
dojo.io.cookie.setCookie=function(name,_7ff,days,path,_802,_803){
var _804=-1;
if((typeof days=="number")&&(days>=0)){
var d=new Date();
d.setTime(d.getTime()+(days*24*60*60*1000));
_804=d.toGMTString();
}
_7ff=escape(_7ff);
document.cookie=name+"="+_7ff+";"+(_804!=-1?" expires="+_804+";":"")+(path?"path="+path:"")+(_802?"; domain="+_802:"")+(_803?"; secure":"");
};
dojo.io.cookie.set=dojo.io.cookie.setCookie;
dojo.io.cookie.getCookie=function(name){
var idx=document.cookie.lastIndexOf(name+"=");
if(idx==-1){
return null;
}
var _808=document.cookie.substring(idx+name.length+1);
var end=_808.indexOf(";");
if(end==-1){
end=_808.length;
}
_808=_808.substring(0,end);
_808=unescape(_808);
return _808;
};
dojo.io.cookie.get=dojo.io.cookie.getCookie;
dojo.io.cookie.deleteCookie=function(name){
dojo.io.cookie.setCookie(name,"-",0);
};
dojo.io.cookie.setObjectCookie=function(name,obj,days,path,_80f,_810,_811){
if(arguments.length==5){
_811=_80f;
_80f=null;
_810=null;
}
var _812=[],_813,_814="";
if(!_811){
_813=dojo.io.cookie.getObjectCookie(name);
}
if(days>=0){
if(!_813){
_813={};
}
for(var prop in obj){
if(obj[prop]==null){
delete _813[prop];
}else{
if((typeof obj[prop]=="string")||(typeof obj[prop]=="number")){
_813[prop]=obj[prop];
}
}
}
prop=null;
for(var prop in _813){
_812.push(escape(prop)+"="+escape(_813[prop]));
}
_814=_812.join("&");
}
dojo.io.cookie.setCookie(name,_814,days,path,_80f,_810);
};
dojo.io.cookie.getObjectCookie=function(name){
var _817=null,_818=dojo.io.cookie.getCookie(name);
if(_818){
_817={};
var _819=_818.split("&");
for(var i=0;i<_819.length;i++){
var pair=_819[i].split("=");
var _81c=pair[1];
if(isNaN(_81c)){
_81c=unescape(pair[1]);
}
_817[unescape(pair[0])]=_81c;
}
}
return _817;
};
dojo.io.cookie.isSupported=function(){
if(typeof navigator.cookieEnabled!="boolean"){
dojo.io.cookie.setCookie("__TestingYourBrowserForCookieSupport__","CookiesAllowed",90,null);
var _81d=dojo.io.cookie.getCookie("__TestingYourBrowserForCookieSupport__");
navigator.cookieEnabled=(_81d=="CookiesAllowed");
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
dojo.provide("dojo.widget.Toolbar");
dojo.widget.defineWidget("dojo.widget.ToolbarContainer",dojo.widget.HtmlWidget,{isContainer:true,templateString:"<div class=\"toolbarContainer\" dojoAttachPoint=\"containerNode\"></div>",templateCssPath:dojo.uri.dojoUri("src/widget/templates/Toolbar.css"),getItem:function(name){
if(name instanceof dojo.widget.ToolbarItem){
return name;
}
for(var i=0;i<this.children.length;i++){
var _820=this.children[i];
if(_820 instanceof dojo.widget.Toolbar){
var item=_820.getItem(name);
if(item){
return item;
}
}
}
return null;
},getItems:function(){
var _822=[];
for(var i=0;i<this.children.length;i++){
var _824=this.children[i];
if(_824 instanceof dojo.widget.Toolbar){
_822=_822.concat(_824.getItems());
}
}
return _822;
},enable:function(){
for(var i=0;i<this.children.length;i++){
var _826=this.children[i];
if(_826 instanceof dojo.widget.Toolbar){
_826.enable.apply(_826,arguments);
}
}
},disable:function(){
for(var i=0;i<this.children.length;i++){
var _828=this.children[i];
if(_828 instanceof dojo.widget.Toolbar){
_828.disable.apply(_828,arguments);
}
}
},select:function(name){
for(var i=0;i<this.children.length;i++){
var _82b=this.children[i];
if(_82b instanceof dojo.widget.Toolbar){
_82b.select(arguments);
}
}
},deselect:function(name){
for(var i=0;i<this.children.length;i++){
var _82e=this.children[i];
if(_82e instanceof dojo.widget.Toolbar){
_82e.deselect(arguments);
}
}
},getItemsState:function(){
var _82f={};
for(var i=0;i<this.children.length;i++){
var _831=this.children[i];
if(_831 instanceof dojo.widget.Toolbar){
dojo.lang.mixin(_82f,_831.getItemsState());
}
}
return _82f;
},getItemsActiveState:function(){
var _832={};
for(var i=0;i<this.children.length;i++){
var _834=this.children[i];
if(_834 instanceof dojo.widget.Toolbar){
dojo.lang.mixin(_832,_834.getItemsActiveState());
}
}
return _832;
},getItemsSelectedState:function(){
var _835={};
for(var i=0;i<this.children.length;i++){
var _837=this.children[i];
if(_837 instanceof dojo.widget.Toolbar){
dojo.lang.mixin(_835,_837.getItemsSelectedState());
}
}
return _835;
}});
dojo.widget.defineWidget("dojo.widget.Toolbar",dojo.widget.HtmlWidget,{isContainer:true,templateString:"<div class=\"toolbar\" dojoAttachPoint=\"containerNode\" unselectable=\"on\" dojoOnMouseover=\"_onmouseover\" dojoOnMouseout=\"_onmouseout\" dojoOnClick=\"_onclick\" dojoOnMousedown=\"_onmousedown\" dojoOnMouseup=\"_onmouseup\"></div>",_getItem:function(node){
var _839=new Date();
var _83a=null;
while(node&&node!=this.domNode){
if(dojo.html.hasClass(node,"toolbarItem")){
var _83b=dojo.widget.manager.getWidgetsByFilter(function(w){
return w.domNode==node;
});
if(_83b.length==1){
_83a=_83b[0];
break;
}else{
if(_83b.length>1){
dojo.raise("Toolbar._getItem: More than one widget matches the node");
}
}
}
node=node.parentNode;
}
return _83a;
},_onmouseover:function(e){
var _83e=this._getItem(e.target);
if(_83e&&_83e._onmouseover){
_83e._onmouseover(e);
}
},_onmouseout:function(e){
var _840=this._getItem(e.target);
if(_840&&_840._onmouseout){
_840._onmouseout(e);
}
},_onclick:function(e){
var _842=this._getItem(e.target);
if(_842&&_842._onclick){
_842._onclick(e);
}
},_onmousedown:function(e){
var _844=this._getItem(e.target);
if(_844&&_844._onmousedown){
_844._onmousedown(e);
}
},_onmouseup:function(e){
var _846=this._getItem(e.target);
if(_846&&_846._onmouseup){
_846._onmouseup(e);
}
},addChild:function(item,pos,_849){
var _84a=dojo.widget.ToolbarItem.make(item,null,_849);
var ret=dojo.widget.Toolbar.superclass.addChild.call(this,_84a,null,pos,null);
return ret;
},push:function(){
for(var i=0;i<arguments.length;i++){
this.addChild(arguments[i]);
}
},getItem:function(name){
if(name instanceof dojo.widget.ToolbarItem){
return name;
}
for(var i=0;i<this.children.length;i++){
var _84f=this.children[i];
if(_84f instanceof dojo.widget.ToolbarItem&&_84f._name==name){
return _84f;
}
}
return null;
},getItems:function(){
var _850=[];
for(var i=0;i<this.children.length;i++){
var _852=this.children[i];
if(_852 instanceof dojo.widget.ToolbarItem){
_850.push(_852);
}
}
return _850;
},getItemsState:function(){
var _853={};
for(var i=0;i<this.children.length;i++){
var _855=this.children[i];
if(_855 instanceof dojo.widget.ToolbarItem){
_853[_855._name]={selected:_855._selected,enabled:!_855.disabled};
}
}
return _853;
},getItemsActiveState:function(){
var _856=this.getItemsState();
for(var item in _856){
_856[item]=_856[item].enabled;
}
return _856;
},getItemsSelectedState:function(){
var _858=this.getItemsState();
for(var item in _858){
_858[item]=_858[item].selected;
}
return _858;
},enable:function(){
var _85a=arguments.length?arguments:this.children;
for(var i=0;i<_85a.length;i++){
var _85c=this.getItem(_85a[i]);
if(_85c instanceof dojo.widget.ToolbarItem){
_85c.enable(false,true);
}
}
},disable:function(){
var _85d=arguments.length?arguments:this.children;
for(var i=0;i<_85d.length;i++){
var _85f=this.getItem(_85d[i]);
if(_85f instanceof dojo.widget.ToolbarItem){
_85f.disable();
}
}
},select:function(){
for(var i=0;i<arguments.length;i++){
var name=arguments[i];
var item=this.getItem(name);
if(item){
item.select();
}
}
},deselect:function(){
for(var i=0;i<arguments.length;i++){
var name=arguments[i];
var item=this.getItem(name);
if(item){
item.disable();
}
}
},setValue:function(){
for(var i=0;i<arguments.length;i+=2){
var name=arguments[i],_868=arguments[i+1];
var item=this.getItem(name);
if(item){
if(item instanceof dojo.widget.ToolbarItem){
item.setValue(_868);
}
}
}
}});
dojo.widget.defineWidget("dojo.widget.ToolbarItem",dojo.widget.HtmlWidget,{templateString:"<span unselectable=\"on\" class=\"toolbarItem\"></span>",_name:null,getName:function(){
return this._name;
},setName:function(_86a){
return (this._name=_86a);
},getValue:function(){
return this.getName();
},setValue:function(_86b){
return this.setName(_86b);
},_selected:false,isSelected:function(){
return this._selected;
},setSelected:function(is,_86d,_86e){
if(!this._toggleItem&&!_86d){
return;
}
is=Boolean(is);
if(_86d||!this.disabled&&this._selected!=is){
this._selected=is;
this.update();
if(!_86e){
this._fireEvent(is?"onSelect":"onDeselect");
this._fireEvent("onChangeSelect");
}
}
},select:function(_86f,_870){
return this.setSelected(true,_86f,_870);
},deselect:function(_871,_872){
return this.setSelected(false,_871,_872);
},_toggleItem:false,isToggleItem:function(){
return this._toggleItem;
},setToggleItem:function(_873){
this._toggleItem=Boolean(_873);
},toggleSelected:function(_874){
return this.setSelected(!this._selected,_874);
},isEnabled:function(){
return !this.disabled;
},setEnabled:function(is,_876,_877){
is=Boolean(is);
if(_876||this.disabled==is){
this.disabled=!is;
this.update();
if(!_877){
this._fireEvent(this.disabled?"onDisable":"onEnable");
this._fireEvent("onChangeEnabled");
}
}
return !this.disabled;
},enable:function(_878,_879){
return this.setEnabled(true,_878,_879);
},disable:function(_87a,_87b){
return this.setEnabled(false,_87a,_87b);
},toggleEnabled:function(_87c,_87d){
return this.setEnabled(this.disabled,_87c,_87d);
},_icon:null,getIcon:function(){
return this._icon;
},setIcon:function(_87e){
var icon=dojo.widget.Icon.make(_87e);
if(this._icon){
this._icon.setIcon(icon);
}else{
this._icon=icon;
}
var _880=this._icon.getNode();
if(_880.parentNode!=this.domNode){
if(this.domNode.hasChildNodes()){
this.domNode.insertBefore(_880,this.domNode.firstChild);
}else{
this.domNode.appendChild(_880);
}
}
return this._icon;
},_label:"",getLabel:function(){
return this._label;
},setLabel:function(_881){
var ret=(this._label=_881);
if(!this.labelNode){
this.labelNode=document.createElement("span");
this.domNode.appendChild(this.labelNode);
}
this.labelNode.innerHTML="";
this.labelNode.appendChild(document.createTextNode(this._label));
this.update();
return ret;
},update:function(){
if(this.disabled){
this._selected=false;
dojo.html.addClass(this.domNode,"disabled");
dojo.html.removeClass(this.domNode,"down");
dojo.html.removeClass(this.domNode,"hover");
}else{
dojo.html.removeClass(this.domNode,"disabled");
if(this._selected){
dojo.html.addClass(this.domNode,"selected");
}else{
dojo.html.removeClass(this.domNode,"selected");
}
}
this._updateIcon();
},_updateIcon:function(){
if(this._icon){
if(this.disabled){
this._icon.disable();
}else{
if(this._cssHover){
this._icon.hover();
}else{
if(this._selected){
this._icon.select();
}else{
this._icon.enable();
}
}
}
}
},_fireEvent:function(evt){
if(typeof this[evt]=="function"){
var args=[this];
for(var i=1;i<arguments.length;i++){
args.push(arguments[i]);
}
this[evt].apply(this,args);
}
},_onmouseover:function(e){
if(this.disabled){
return;
}
dojo.html.addClass(this.domNode,"hover");
this._fireEvent("onMouseOver");
},_onmouseout:function(e){
dojo.html.removeClass(this.domNode,"hover");
dojo.html.removeClass(this.domNode,"down");
if(!this._selected){
dojo.html.removeClass(this.domNode,"selected");
}
this._fireEvent("onMouseOut");
},_onclick:function(e){
if(!this.disabled&&!this._toggleItem){
this._fireEvent("onClick");
}
},_onmousedown:function(e){
if(e.preventDefault){
e.preventDefault();
}
if(this.disabled){
return;
}
dojo.html.addClass(this.domNode,"down");
if(this._toggleItem){
if(this.parent.preventDeselect&&this._selected){
return;
}
this.toggleSelected();
}
this._fireEvent("onMouseDown");
},_onmouseup:function(e){
dojo.html.removeClass(this.domNode,"down");
this._fireEvent("onMouseUp");
},onClick:function(){
},onMouseOver:function(){
},onMouseOut:function(){
},onMouseDown:function(){
},onMouseUp:function(){
},fillInTemplate:function(args,frag){
if(args.name){
this._name=args.name;
}
if(args.selected){
this.select();
}
if(args.disabled){
this.disable();
}
if(args.label){
this.setLabel(args.label);
}
if(args.icon){
this.setIcon(args.icon);
}
if(args.toggleitem||args.toggleItem){
this.setToggleItem(true);
}
}});
dojo.widget.ToolbarItem.make=function(wh,_88e,_88f){
var item=null;
if(wh instanceof Array){
item=dojo.widget.createWidget("ToolbarButtonGroup",_88f);
item.setName(wh[0]);
for(var i=1;i<wh.length;i++){
item.addChild(wh[i]);
}
}else{
if(wh instanceof dojo.widget.ToolbarItem){
item=wh;
}else{
if(wh instanceof dojo.uri.Uri){
item=dojo.widget.createWidget("ToolbarButton",dojo.lang.mixin(_88f||{},{icon:new dojo.widget.Icon(wh.toString())}));
}else{
if(_88e){
item=dojo.widget.createWidget(wh,_88f);
}else{
if(typeof wh=="string"||wh instanceof String){
switch(wh.charAt(0)){
case "|":
case "-":
case "/":
item=dojo.widget.createWidget("ToolbarSeparator",_88f);
break;
case " ":
if(wh.length==1){
item=dojo.widget.createWidget("ToolbarSpace",_88f);
}else{
item=dojo.widget.createWidget("ToolbarFlexibleSpace",_88f);
}
break;
default:
if(/\.(gif|jpg|jpeg|png)$/i.test(wh)){
item=dojo.widget.createWidget("ToolbarButton",dojo.lang.mixin(_88f||{},{icon:new dojo.widget.Icon(wh.toString())}));
}else{
item=dojo.widget.createWidget("ToolbarButton",dojo.lang.mixin(_88f||{},{label:wh.toString()}));
}
}
}else{
if(wh&&wh.tagName&&/^img$/i.test(wh.tagName)){
item=dojo.widget.createWidget("ToolbarButton",dojo.lang.mixin(_88f||{},{icon:wh}));
}else{
item=dojo.widget.createWidget("ToolbarButton",dojo.lang.mixin(_88f||{},{label:wh.toString()}));
}
}
}
}
}
}
return item;
};
dojo.widget.defineWidget("dojo.widget.ToolbarButtonGroup",dojo.widget.ToolbarItem,{isContainer:true,templateString:"<span unselectable=\"on\" class=\"toolbarButtonGroup\" dojoAttachPoint=\"containerNode\"></span>",defaultButton:"",postCreate:function(){
for(var i=0;i<this.children.length;i++){
this._injectChild(this.children[i]);
}
},addChild:function(item,pos,_895){
var _896=dojo.widget.ToolbarItem.make(item,null,dojo.lang.mixin(_895||{},{toggleItem:true}));
var ret=dojo.widget.ToolbarButtonGroup.superclass.addChild.call(this,_896,null,pos,null);
this._injectChild(_896);
return ret;
},_injectChild:function(_898){
dojo.event.connect(_898,"onSelect",this,"onChildSelected");
dojo.event.connect(_898,"onDeselect",this,"onChildDeSelected");
if(_898._name==this.defaultButton||(typeof this.defaultButton=="number"&&this.children.length-1==this.defaultButton)){
_898.select(false,true);
}
},getItem:function(name){
if(name instanceof dojo.widget.ToolbarItem){
return name;
}
for(var i=0;i<this.children.length;i++){
var _89b=this.children[i];
if(_89b instanceof dojo.widget.ToolbarItem&&_89b._name==name){
return _89b;
}
}
return null;
},getItems:function(){
var _89c=[];
for(var i=0;i<this.children.length;i++){
var _89e=this.children[i];
if(_89e instanceof dojo.widget.ToolbarItem){
_89c.push(_89e);
}
}
return _89c;
},onChildSelected:function(e){
this.select(e._name);
},onChildDeSelected:function(e){
this._fireEvent("onChangeSelect",this._value);
},enable:function(_8a1,_8a2){
for(var i=0;i<this.children.length;i++){
var _8a4=this.children[i];
if(_8a4 instanceof dojo.widget.ToolbarItem){
_8a4.enable(_8a1,_8a2);
if(_8a4._name==this._value){
_8a4.select(_8a1,_8a2);
}
}
}
},disable:function(_8a5,_8a6){
for(var i=0;i<this.children.length;i++){
var _8a8=this.children[i];
if(_8a8 instanceof dojo.widget.ToolbarItem){
_8a8.disable(_8a5,_8a6);
}
}
},_value:"",getValue:function(){
return this._value;
},select:function(name,_8aa,_8ab){
for(var i=0;i<this.children.length;i++){
var _8ad=this.children[i];
if(_8ad instanceof dojo.widget.ToolbarItem){
if(_8ad._name==name){
_8ad.select(_8aa,_8ab);
this._value=name;
}else{
_8ad.deselect(true,true);
}
}
}
if(!_8ab){
this._fireEvent("onSelect",this._value);
this._fireEvent("onChangeSelect",this._value);
}
},setValue:this.select,preventDeselect:false});
dojo.widget.defineWidget("dojo.widget.ToolbarButton",dojo.widget.ToolbarItem,{fillInTemplate:function(args,frag){
dojo.widget.ToolbarButton.superclass.fillInTemplate.call(this,args,frag);
dojo.html.addClass(this.domNode,"toolbarButton");
if(this._icon){
this.setIcon(this._icon);
}
if(this._label){
this.setLabel(this._label);
}
if(!this._name){
if(this._label){
this.setName(this._label);
}else{
if(this._icon){
var src=this._icon.getSrc("enabled").match(/[\/^]([^\.\/]+)\.(gif|jpg|jpeg|png)$/i);
if(src){
this.setName(src[1]);
}
}else{
this._name=this._widgetId;
}
}
}
}});
dojo.widget.defineWidget("dojo.widget.ToolbarDialog",dojo.widget.ToolbarButton,{fillInTemplate:function(args,frag){
dojo.widget.ToolbarDialog.superclass.fillInTemplate.call(this,args,frag);
dojo.event.connect(this,"onSelect",this,"showDialog");
dojo.event.connect(this,"onDeselect",this,"hideDialog");
},showDialog:function(e){
dojo.lang.setTimeout(dojo.event.connect,1,document,"onmousedown",this,"deselect");
},hideDialog:function(e){
dojo.event.disconnect(document,"onmousedown",this,"deselect");
}});
dojo.widget.defineWidget("dojo.widget.ToolbarMenu",dojo.widget.ToolbarDialog,{});
dojo.widget.ToolbarMenuItem=function(){
};
dojo.widget.defineWidget("dojo.widget.ToolbarSeparator",dojo.widget.ToolbarItem,{templateString:"<span unselectable=\"on\" class=\"toolbarItem toolbarSeparator\"></span>",defaultIconPath:new dojo.uri.dojoUri("src/widget/templates/buttons/sep.gif"),fillInTemplate:function(args,frag,skip){
dojo.widget.ToolbarSeparator.superclass.fillInTemplate.call(this,args,frag);
this._name=this.widgetId;
if(!skip){
if(!this._icon){
this.setIcon(this.defaultIconPath);
}
this.domNode.appendChild(this._icon.getNode());
}
},_onmouseover:null,_onmouseout:null,_onclick:null,_onmousedown:null,_onmouseup:null});
dojo.widget.defineWidget("dojo.widget.ToolbarSpace",dojo.widget.ToolbarSeparator,{fillInTemplate:function(args,frag,skip){
dojo.widget.ToolbarSpace.superclass.fillInTemplate.call(this,args,frag,true);
if(!skip){
dojo.html.addClass(this.domNode,"toolbarSpace");
}
}});
dojo.widget.defineWidget("dojo.widget.ToolbarSelect",dojo.widget.ToolbarItem,{templateString:"<span class=\"toolbarItem toolbarSelect\" unselectable=\"on\"><select dojoAttachPoint=\"selectBox\" dojoOnChange=\"changed\"></select></span>",fillInTemplate:function(args,frag){
dojo.widget.ToolbarSelect.superclass.fillInTemplate.call(this,args,frag,true);
var keys=args.values;
var i=0;
for(var val in keys){
var opt=document.createElement("option");
opt.setAttribute("value",keys[val]);
opt.innerHTML=val;
this.selectBox.appendChild(opt);
}
},changed:function(e){
this._fireEvent("onSetValue",this.selectBox.value);
},setEnabled:function(is,_8c3,_8c4){
var ret=dojo.widget.ToolbarSelect.superclass.setEnabled.call(this,is,_8c3,_8c4);
this.selectBox.disabled=this.disabled;
return ret;
},_onmouseover:null,_onmouseout:null,_onclick:null,_onmousedown:null,_onmouseup:null});
dojo.widget.Icon=function(_8c6,_8c7,_8c8,_8c9){
if(!arguments.length){
throw new Error("Icon must have at least an enabled state");
}
var _8ca=["enabled","disabled","hovered","selected"];
var _8cb="enabled";
var _8cc=document.createElement("img");
this.getState=function(){
return _8cb;
};
this.setState=function(_8cd){
if(dojo.lang.inArray(_8ca,_8cd)){
if(this[_8cd]){
_8cb=_8cd;
var img=this[_8cb];
if((dojo.render.html.ie55||dojo.render.html.ie60)&&img.src&&img.src.match(/[.]png$/i)){
_8cc.width=img.width||img.offsetWidth;
_8cc.height=img.height||img.offsetHeight;
_8cc.setAttribute("src",dojo.uri.dojoUri("src/widget/templates/images/blank.gif").uri);
_8cc.style.filter="progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"+img.src+"',sizingMethod='image')";
}else{
_8cc.setAttribute("src",img.src);
}
}
}else{
throw new Error("Invalid state set on Icon (state: "+_8cd+")");
}
};
this.setSrc=function(_8cf,_8d0){
if(/^img$/i.test(_8d0.tagName)){
this[_8cf]=_8d0;
}else{
if(typeof _8d0=="string"||_8d0 instanceof String||_8d0 instanceof dojo.uri.Uri){
this[_8cf]=new Image();
this[_8cf].src=_8d0.toString();
}
}
return this[_8cf];
};
this.setIcon=function(icon){
for(var i=0;i<_8ca.length;i++){
if(icon[_8ca[i]]){
this.setSrc(_8ca[i],icon[_8ca[i]]);
}
}
this.update();
};
this.enable=function(){
this.setState("enabled");
};
this.disable=function(){
this.setState("disabled");
};
this.hover=function(){
this.setState("hovered");
};
this.select=function(){
this.setState("selected");
};
this.getSize=function(){
return {width:_8cc.width||_8cc.offsetWidth,height:_8cc.height||_8cc.offsetHeight};
};
this.setSize=function(w,h){
_8cc.width=w;
_8cc.height=h;
return {width:w,height:h};
};
this.getNode=function(){
return _8cc;
};
this.getSrc=function(_8d5){
if(_8d5){
return this[_8d5].src;
}
return _8cc.src||"";
};
this.update=function(){
this.setState(_8cb);
};
for(var i=0;i<_8ca.length;i++){
var arg=arguments[i];
var _8d8=_8ca[i];
this[_8d8]=null;
if(!arg){
continue;
}
this.setSrc(_8d8,arg);
}
this.enable();
};
dojo.widget.Icon.make=function(a,b,c,d){
for(var i=0;i<arguments.length;i++){
if(arguments[i] instanceof dojo.widget.Icon){
return arguments[i];
}
}
return new dojo.widget.Icon(a,b,c,d);
};
dojo.widget.defineWidget("dojo.widget.ToolbarColorDialog",dojo.widget.ToolbarDialog,{palette:"7x10",fillInTemplate:function(args,frag){
dojo.widget.ToolbarColorDialog.superclass.fillInTemplate.call(this,args,frag);
this.dialog=dojo.widget.createWidget("ColorPalette",{palette:this.palette});
this.dialog.domNode.style.position="absolute";
dojo.event.connect(this.dialog,"onColorSelect",this,"_setValue");
},_setValue:function(_8e0){
this._value=_8e0;
this._fireEvent("onSetValue",_8e0);
},showDialog:function(e){
dojo.widget.ToolbarColorDialog.superclass.showDialog.call(this,e);
var abs=dojo.html.getAbsolutePosition(this.domNode,true);
var y=abs.y+dojo.html.getBorderBox(this.domNode).height;
this.dialog.showAt(abs.x,y);
},hideDialog:function(e){
dojo.widget.ToolbarColorDialog.superclass.hideDialog.call(this,e);
this.dialog.hide();
}});
dojo.provide("dojo.html.*");
dojo.provide("dojo.html.selection");
dojo.html.selectionType={NONE:0,TEXT:1,CONTROL:2};
dojo.html.clearSelection=function(){
var _8e5=dojo.global();
var _8e6=dojo.doc();
try{
if(_8e5["getSelection"]){
if(dojo.render.html.safari){
_8e5.getSelection().collapse();
}else{
_8e5.getSelection().removeAllRanges();
}
}else{
if(_8e6.selection){
if(_8e6.selection.empty){
_8e6.selection.empty();
}else{
if(_8e6.selection.clear){
_8e6.selection.clear();
}
}
}
}
return true;
}
catch(e){
dojo.debug(e);
return false;
}
};
dojo.html.disableSelection=function(_8e7){
_8e7=dojo.byId(_8e7)||dojo.body();
var h=dojo.render.html;
if(h.mozilla){
_8e7.style.MozUserSelect="none";
}else{
if(h.safari){
_8e7.style.KhtmlUserSelect="none";
}else{
if(h.ie){
_8e7.unselectable="on";
}else{
return false;
}
}
}
return true;
};
dojo.html.enableSelection=function(_8e9){
_8e9=dojo.byId(_8e9)||dojo.body();
var h=dojo.render.html;
if(h.mozilla){
_8e9.style.MozUserSelect="";
}else{
if(h.safari){
_8e9.style.KhtmlUserSelect="";
}else{
if(h.ie){
_8e9.unselectable="off";
}else{
return false;
}
}
}
return true;
};
dojo.html.selectElement=function(_8eb){
dojo.deprecated("dojo.html.selectElement","replaced by dojo.html.selection.selectElementChildren",0.5);
};
dojo.html.selectInputText=function(_8ec){
var _8ed=dojo.global();
var _8ee=dojo.doc();
_8ec=dojo.byId(_8ec);
if(_8ee["selection"]&&dojo.body()["createTextRange"]){
var _8ef=_8ec.createTextRange();
_8ef.moveStart("character",0);
_8ef.moveEnd("character",_8ec.value.length);
_8ef.select();
}else{
if(_8ed["getSelection"]){
var _8f0=_8ed.getSelection();
_8ec.setSelectionRange(0,_8ec.value.length);
}
}
_8ec.focus();
};
dojo.html.isSelectionCollapsed=function(){
dojo.deprecated("dojo.html.isSelectionCollapsed","replaced by dojo.html.selection.isCollapsed",0.5);
return dojo.html.selection.isCollapsed();
};
dojo.lang.mixin(dojo.html.selection,{getType:function(){
if(dojo.doc()["selection"]){
return dojo.html.selectionType[dojo.doc().selection.type.toUpperCase()];
}else{
var _8f1=dojo.html.selectionType.TEXT;
var oSel;
try{
oSel=dojo.global().getSelection();
}
catch(e){
}
if(oSel&&oSel.rangeCount==1){
var _8f3=oSel.getRangeAt(0);
if(_8f3.startContainer==_8f3.endContainer&&(_8f3.endOffset-_8f3.startOffset)==1&&_8f3.startContainer.nodeType!=dojo.dom.TEXT_NODE){
_8f1=dojo.html.selectionType.CONTROL;
}
}
return _8f1;
}
},isCollapsed:function(){
var _8f4=dojo.global();
var _8f5=dojo.doc();
if(_8f5["selection"]){
return _8f5.selection.createRange().text=="";
}else{
if(_8f4["getSelection"]){
var _8f6=_8f4.getSelection();
if(dojo.lang.isString(_8f6)){
return _8f6=="";
}else{
return _8f6.isCollapsed||_8f6.toString()=="";
}
}
}
},getSelectedElement:function(){
if(dojo.html.selection.getType()==dojo.html.selectionType.CONTROL){
if(dojo.doc()["selection"]){
var _8f7=dojo.doc().selection.createRange();
if(_8f7&&_8f7.item){
return dojo.doc().selection.createRange().item(0);
}
}else{
var _8f8=dojo.global().getSelection();
return _8f8.anchorNode.childNodes[_8f8.anchorOffset];
}
}
},getParentElement:function(){
if(dojo.html.selection.getType()==dojo.html.selectionType.CONTROL){
var p=dojo.html.selection.getSelectedElement();
if(p){
return p.parentNode;
}
}else{
if(dojo.doc()["selection"]){
return dojo.doc().selection.createRange().parentElement();
}else{
var _8fa=dojo.global().getSelection();
if(_8fa){
var node=_8fa.anchorNode;
while(node&&node.nodeType!=dojo.dom.ELEMENT_NODE){
node=node.parentNode;
}
return node;
}
}
}
},getSelectedText:function(){
if(dojo.doc()["selection"]){
if(dojo.html.selection.getType()==dojo.html.selectionType.CONTROL){
return null;
}
return dojo.doc().selection.createRange().text;
}else{
var _8fc=dojo.global().getSelection();
if(_8fc){
return _8fc.toString();
}
}
},getSelectedHtml:function(){
if(dojo.doc()["selection"]){
if(dojo.html.selection.getType()==dojo.html.selectionType.CONTROL){
return null;
}
return dojo.doc().selection.createRange().htmlText;
}else{
var _8fd=dojo.global().getSelection();
if(_8fd&&_8fd.rangeCount){
var frag=_8fd.getRangeAt(0).cloneContents();
var div=document.createElement("div");
div.appendChild(frag);
return div.innerHTML;
}
return null;
}
},hasAncestorElement:function(_900){
return (dojo.html.selection.getAncestorElement.apply(this,arguments)!=null);
},getAncestorElement:function(_901){
var node=dojo.html.selection.getSelectedElement()||dojo.html.selection.getParentElement();
while(node){
if(dojo.html.selection.isTag(node,arguments).length>0){
return node;
}
node=node.parentNode;
}
return null;
},isTag:function(node,tags){
if(node&&node.tagName){
for(var i=0;i<tags.length;i++){
if(node.tagName.toLowerCase()==String(tags[i]).toLowerCase()){
return String(tags[i]).toLowerCase();
}
}
}
return "";
},selectElement:function(_906){
var _907=dojo.global();
var _908=dojo.doc();
_906=dojo.byId(_906);
if(_908.selection&&dojo.body().createTextRange){
try{
var _909=dojo.body().createControlRange();
_909.addElement(_906);
_909.select();
}
catch(e){
dojo.html.selection.selectElementChildren(_906);
}
}else{
if(_907["getSelection"]){
var _90a=_907.getSelection();
if(_90a["removeAllRanges"]){
var _909=_908.createRange();
_909.selectNode(_906);
_90a.removeAllRanges();
_90a.addRange(_909);
}
}
}
},selectElementChildren:function(_90b){
var _90c=dojo.global();
var _90d=dojo.doc();
_90b=dojo.byId(_90b);
if(_90d.selection&&dojo.body().createTextRange){
var _90e=dojo.body().createTextRange();
_90e.moveToElementText(_90b);
_90e.select();
}else{
if(_90c["getSelection"]){
var _90f=_90c.getSelection();
if(_90f["setBaseAndExtent"]){
_90f.setBaseAndExtent(_90b,0,_90b,_90b.innerText.length-1);
}else{
if(_90f["selectAllChildren"]){
_90f.selectAllChildren(_90b);
}
}
}
}
},getBookmark:function(){
var _910;
var _911=dojo.doc();
if(_911["selection"]){
var _912=_911.selection.createRange();
_910=_912.getBookmark();
}else{
var _913;
try{
_913=dojo.global().getSelection();
}
catch(e){
}
if(_913){
var _912=_913.getRangeAt(0);
_910=_912.cloneRange();
}else{
dojo.debug("No idea how to store the current selection for this browser!");
}
}
return _910;
},moveToBookmark:function(_914){
var _915=dojo.doc();
if(_915["selection"]){
var _916=_915.selection.createRange();
_916.moveToBookmark(_914);
_916.select();
}else{
var _917;
try{
_917=dojo.global().getSelection();
}
catch(e){
}
if(_917&&_917["removeAllRanges"]){
_917.removeAllRanges();
_917.addRange(_914);
}else{
dojo.debug("No idea how to restore selection for this browser!");
}
}
},collapse:function(_918){
if(dojo.global()["getSelection"]){
var _919=dojo.global().getSelection();
if(_919.removeAllRanges){
if(_918){
_919.collapseToStart();
}else{
_919.collapseToEnd();
}
}else{
dojo.global().getSelection().collapse(_918);
}
}else{
if(dojo.doc().selection){
var _91a=dojo.doc().selection.createRange();
_91a.collapse(_918);
_91a.select();
}
}
},remove:function(){
if(dojo.doc().selection){
var _91b=dojo.doc().selection;
if(_91b.type.toUpperCase()!="NONE"){
_91b.clear();
}
return _91b;
}else{
var _91b=dojo.global().getSelection();
for(var i=0;i<_91b.rangeCount;i++){
_91b.getRangeAt(i).deleteContents();
}
return _91b;
}
}});
dojo.provide("dojo.Deferred");
dojo.Deferred=function(_91d){
this.chain=[];
this.id=this._nextId();
this.fired=-1;
this.paused=0;
this.results=[null,null];
this.canceller=_91d;
this.silentlyCancelled=false;
};
dojo.lang.extend(dojo.Deferred,{getFunctionFromArgs:function(){
var a=arguments;
if((a[0])&&(!a[1])){
if(dojo.lang.isFunction(a[0])){
return a[0];
}else{
if(dojo.lang.isString(a[0])){
return dj_global[a[0]];
}
}
}else{
if((a[0])&&(a[1])){
return dojo.lang.hitch(a[0],a[1]);
}
}
return null;
},makeCalled:function(){
var _91f=new dojo.Deferred();
_91f.callback();
return _91f;
},repr:function(){
var _920;
if(this.fired==-1){
_920="unfired";
}else{
if(this.fired==0){
_920="success";
}else{
_920="error";
}
}
return "Deferred("+this.id+", "+_920+")";
},toString:dojo.lang.forward("repr"),_nextId:(function(){
var n=1;
return function(){
return n++;
};
})(),cancel:function(){
if(this.fired==-1){
if(this.canceller){
this.canceller(this);
}else{
this.silentlyCancelled=true;
}
if(this.fired==-1){
this.errback(new Error(this.repr()));
}
}else{
if((this.fired==0)&&(this.results[0] instanceof dojo.Deferred)){
this.results[0].cancel();
}
}
},_pause:function(){
this.paused++;
},_unpause:function(){
this.paused--;
if((this.paused==0)&&(this.fired>=0)){
this._fire();
}
},_continue:function(res){
this._resback(res);
this._unpause();
},_resback:function(res){
this.fired=((res instanceof Error)?1:0);
this.results[this.fired]=res;
this._fire();
},_check:function(){
if(this.fired!=-1){
if(!this.silentlyCancelled){
dojo.raise("already called!");
}
this.silentlyCancelled=false;
return;
}
},callback:function(res){
this._check();
this._resback(res);
},errback:function(res){
this._check();
if(!(res instanceof Error)){
res=new Error(res);
}
this._resback(res);
},addBoth:function(cb,cbfn){
var _928=this.getFunctionFromArgs(cb,cbfn);
if(arguments.length>2){
_928=dojo.lang.curryArguments(null,_928,arguments,2);
}
return this.addCallbacks(_928,_928);
},addCallback:function(cb,cbfn){
var _92b=this.getFunctionFromArgs(cb,cbfn);
if(arguments.length>2){
_92b=dojo.lang.curryArguments(null,_92b,arguments,2);
}
return this.addCallbacks(_92b,null);
},addErrback:function(cb,cbfn){
var _92e=this.getFunctionFromArgs(cb,cbfn);
if(arguments.length>2){
_92e=dojo.lang.curryArguments(null,_92e,arguments,2);
}
return this.addCallbacks(null,_92e);
return this.addCallbacks(null,cbfn);
},addCallbacks:function(cb,eb){
this.chain.push([cb,eb]);
if(this.fired>=0){
this._fire();
}
return this;
},_fire:function(){
var _931=this.chain;
var _932=this.fired;
var res=this.results[_932];
var self=this;
var cb=null;
while(_931.length>0&&this.paused==0){
var pair=_931.shift();
var f=pair[_932];
if(f==null){
continue;
}
try{
res=f(res);
_932=((res instanceof Error)?1:0);
if(res instanceof dojo.Deferred){
cb=function(res){
self._continue(res);
};
this._pause();
}
}
catch(err){
_932=1;
res=err;
}
}
this.fired=_932;
this.results[_932]=res;
if((cb)&&(this.paused)){
res.addBoth(cb);
}
}});
dojo.provide("dojo.widget.RichText");
if(dojo.hostenv.post_load_){
(function(){
var _939=dojo.doc().createElement("textarea");
_939.id="dojo.widget.RichText.savedContent";
_939.style="display:none;position:absolute;top:-100px;left:-100px;height:3px;width:3px;overflow:hidden;";
dojo.body().appendChild(_939);
})();
}else{
try{
dojo.doc().write("<textarea id=\"dojo.widget.RichText.savedContent\" "+"style=\"display:none;position:absolute;top:-100px;left:-100px;height:3px;width:3px;overflow:hidden;\"></textarea>");
}
catch(e){
}
}
dojo.widget.defineWidget("dojo.widget.RichText",dojo.widget.HtmlWidget,function(){
this.contentPreFilters=[];
this.contentPostFilters=[];
this.contentDomPreFilters=[];
this.contentDomPostFilters=[];
this.editingAreaStyleSheets=[];
if(dojo.render.html.moz){
this.contentPreFilters.push(this._fixContentForMoz);
}
this._keyHandlers={};
if(dojo.Deferred){
this.onLoadDeferred=new dojo.Deferred();
}
},{inheritWidth:false,focusOnLoad:false,saveName:"",styleSheets:"",_content:"",height:"",minHeight:"1em",isClosed:true,isLoaded:false,useActiveX:false,relativeImageUrls:false,_SEPARATOR:"@@**%%__RICHTEXTBOUNDRY__%%**@@",onLoadDeferred:null,fillInTemplate:function(){
dojo.event.topic.publish("dojo.widget.RichText::init",this);
this.open();
dojo.event.connect(this,"onKeyPressed",this,"afterKeyPress");
dojo.event.connect(this,"onKeyPress",this,"keyPress");
dojo.event.connect(this,"onKeyDown",this,"keyDown");
dojo.event.connect(this,"onKeyUp",this,"keyUp");
this.setupDefaultShortcuts();
},setupDefaultShortcuts:function(){
var ctrl=this.KEY_CTRL;
var exec=function(cmd,arg){
return arguments.length==1?function(){
this.execCommand(cmd);
}:function(){
this.execCommand(cmd,arg);
};
};
this.addKeyHandler("b",ctrl,exec("bold"));
this.addKeyHandler("i",ctrl,exec("italic"));
this.addKeyHandler("u",ctrl,exec("underline"));
this.addKeyHandler("a",ctrl,exec("selectall"));
this.addKeyHandler("s",ctrl,function(){
this.save(true);
});
this.addKeyHandler("1",ctrl,exec("formatblock","h1"));
this.addKeyHandler("2",ctrl,exec("formatblock","h2"));
this.addKeyHandler("3",ctrl,exec("formatblock","h3"));
this.addKeyHandler("4",ctrl,exec("formatblock","h4"));
this.addKeyHandler("\\",ctrl,exec("insertunorderedlist"));
if(!dojo.render.html.ie){
this.addKeyHandler("Z",ctrl,exec("redo"));
}
},events:["onBlur","onFocus","onKeyPress","onKeyDown","onKeyUp","onClick"],open:function(_93e){
if(this.onLoadDeferred.fired>=0){
this.onLoadDeferred=new dojo.Deferred();
}
var h=dojo.render.html;
if(!this.isClosed){
this.close();
}
dojo.event.topic.publish("dojo.widget.RichText::open",this);
this._content="";
if((arguments.length==1)&&(_93e["nodeName"])){
this.domNode=_93e;
}
if((this.domNode["nodeName"])&&(this.domNode.nodeName.toLowerCase()=="textarea")){
this.textarea=this.domNode;
var html=dojo.string.trim(this.textarea.value);
this.domNode=dojo.doc().createElement("div");
dojo.html.copyStyle(this.domNode,this.textarea);
var _941=dojo.lang.hitch(this,function(){
with(this.textarea.style){
display="block";
position="absolute";
left=top="-1000px";
if(h.ie){
this.__overflow=overflow;
overflow="hidden";
}
}
});
if(h.ie){
setTimeout(_941,10);
}else{
_941();
}
if(!h.safari){
dojo.html.insertBefore(this.domNode,this.textarea);
}
if(this.textarea.form){
dojo.event.connect("before",this.textarea.form,"onsubmit",dojo.lang.hitch(this,function(){
this.textarea.value=this.getEditorContent();
}));
}
var _942=this;
dojo.event.connect(this,"postCreate",function(){
dojo.html.insertAfter(_942.textarea,_942.domNode);
});
}else{
var html=this._preFilterContent(dojo.string.trim(this.domNode.innerHTML));
}
if(html==""){
html="&nbsp;";
}
var _943=dojo.html.getContentBox(this.domNode);
this._oldHeight=_943.height;
this._oldWidth=_943.width;
this._firstChildContributingMargin=this._getContributingMargin(this.domNode,"top");
this._lastChildContributingMargin=this._getContributingMargin(this.domNode,"bottom");
this.savedContent=this.domNode.innerHTML;
this.domNode.innerHTML="";
this.editingArea=dojo.doc().createElement("div");
this.domNode.appendChild(this.editingArea);
if((this.domNode["nodeName"])&&(this.domNode.nodeName=="LI")){
this.domNode.innerHTML=" <br>";
}
if(this.saveName!=""){
var _944=dojo.doc().getElementById("dojo.widget.RichText.savedContent");
if(_944.value!=""){
var _945=_944.value.split(this._SEPARATOR);
for(var i=0;i<_945.length;i++){
var data=_945[i].split(":");
if(data[0]==this.saveName){
html=data[1];
_945.splice(i,1);
break;
}
}
}
dojo.event.connect("before",window,"onunload",this,"_saveContent");
}
if(h.ie70&&this.useActiveX){
dojo.debug("activeX in ie70 is not currently supported, useActiveX is ignored for now.");
this.useActiveX=false;
}
if(this.useActiveX&&h.ie){
var self=this;
setTimeout(function(){
self._drawObject(html);
},0);
}else{
if(h.ie||this._safariIsLeopard()||h.opera){
this.iframe=dojo.doc().createElement("iframe");
this.iframe.src="javascript:void(0)";
this.editorObject=this.iframe;
with(this.iframe.style){
border="0";
width="100%";
}
this.iframe.frameBorder=0;
this.editingArea.appendChild(this.iframe);
this.window=this.iframe.contentWindow;
this.document=this.window.document;
this.document.open();
this.document.write("<html><head><style>body{margin:0;padding:0;border:0;overflow:hidden;}</style></head><body><div></div></body></html>");
this.document.close();
this.editNode=this.document.body.firstChild;
this.editNode.contentEditable=true;
with(this.iframe.style){
if(h.ie70){
if(this.height){
height=this.height;
}
if(this.minHeight){
minHeight=this.minHeight;
}
}else{
height=this.height?this.height:this.minHeight;
}
}
var _949=["p","pre","address","h1","h2","h3","h4","h5","h6","ol","div","ul"];
var _94a="";
for(var i in _949){
if(_949[i].charAt(1)!="l"){
_94a+="<"+_949[i]+"><span>content</span></"+_949[i]+">";
}else{
_94a+="<"+_949[i]+"><li>content</li></"+_949[i]+">";
}
}
with(this.editNode.style){
position="absolute";
left="-2000px";
top="-2000px";
}
this.editNode.innerHTML=_94a;
var node=this.editNode.firstChild;
while(node){
dojo.withGlobal(this.window,"selectElement",dojo.html.selection,[node.firstChild]);
var _94c=node.tagName.toLowerCase();
this._local2NativeFormatNames[_94c]=this.queryCommandValue("formatblock");
this._native2LocalFormatNames[this._local2NativeFormatNames[_94c]]=_94c;
node=node.nextSibling;
}
with(this.editNode.style){
position="";
left="";
top="";
}
this.editNode.innerHTML=html;
if(this.height){
this.document.body.style.overflowY="scroll";
}
dojo.lang.forEach(this.events,function(e){
dojo.event.connect(this.editNode,e.toLowerCase(),this,e);
},this);
this.onLoad();
}else{
this._drawIframe(html);
this.editorObject=this.iframe;
}
}
if(this.domNode.nodeName=="LI"){
this.domNode.lastChild.style.marginTop="-1.2em";
}
dojo.html.addClass(this.domNode,"RichTextEditable");
this.isClosed=false;
},_hasCollapseableMargin:function(_94e,side){
if(dojo.html.getPixelValue(_94e,"border-"+side+"-width",false)){
return false;
}else{
if(dojo.html.getPixelValue(_94e,"padding-"+side,false)){
return false;
}else{
return true;
}
}
},_getContributingMargin:function(_950,_951){
if(_951=="top"){
var _952="previousSibling";
var _953="nextSibling";
var _954="firstChild";
var _955="margin-top";
var _956="margin-bottom";
}else{
var _952="nextSibling";
var _953="previousSibling";
var _954="lastChild";
var _955="margin-bottom";
var _956="margin-top";
}
var _957=dojo.html.getPixelValue(_950,_955,false);
function isSignificantNode(_958){
return !(_958.nodeType==3&&dojo.string.isBlank(_958.data))&&dojo.html.getStyle(_958,"display")!="none"&&!dojo.html.isPositionAbsolute(_958);
}
var _959=0;
var _95a=_950[_954];
while(_95a){
while((!isSignificantNode(_95a))&&_95a[_953]){
_95a=_95a[_953];
}
_959=Math.max(_959,dojo.html.getPixelValue(_95a,_955,false));
if(!this._hasCollapseableMargin(_95a,_951)){
break;
}
_95a=_95a[_954];
}
if(!this._hasCollapseableMargin(_950,_951)){
return parseInt(_959);
}
var _95b=0;
var _95c=_950[_952];
while(_95c){
if(isSignificantNode(_95c)){
_95b=dojo.html.getPixelValue(_95c,_956,false);
break;
}
_95c=_95c[_952];
}
if(!_95c){
_95b=dojo.html.getPixelValue(_950.parentNode,_955,false);
}
if(_959>_957){
return parseInt(Math.max((_959-_957)-_95b,0));
}else{
return 0;
}
},_drawIframe:function(html){
var _95e=Boolean(dojo.render.html.moz&&(typeof window.XML=="undefined"));
if(!this.iframe){
var _95f=(new dojo.uri.Uri(dojo.doc().location)).host;
this.iframe=dojo.doc().createElement("iframe");
with(this.iframe){
style.border="none";
style.lineHeight="0";
style.verticalAlign="bottom";
scrolling=this.height?"auto":"no";
}
}
this.iframe.src=dojo.uri.dojoUri("src/widget/templates/richtextframe.html")+((dojo.doc().domain!=_95f)?("#"+dojo.doc().domain):"");
this.iframe.width=this.inheritWidth?this._oldWidth:"100%";
if(this.height){
this.iframe.style.height=this.height;
}else{
var _960=this._oldHeight;
if(this._hasCollapseableMargin(this.domNode,"top")){
_960+=this._firstChildContributingMargin;
}
if(this._hasCollapseableMargin(this.domNode,"bottom")){
_960+=this._lastChildContributingMargin;
}
this.iframe.height=_960;
}
var _961=dojo.doc().createElement("div");
_961.innerHTML=html;
this.editingArea.appendChild(_961);
if(this.relativeImageUrls){
var imgs=_961.getElementsByTagName("img");
for(var i=0;i<imgs.length;i++){
imgs[i].src=(new dojo.uri.Uri(dojo.global().location,imgs[i].src)).toString();
}
html=_961.innerHTML;
}
var _964=dojo.html.firstElement(_961);
var _965=dojo.html.lastElement(_961);
if(_964){
_964.style.marginTop=this._firstChildContributingMargin+"px";
}
if(_965){
_965.style.marginBottom=this._lastChildContributingMargin+"px";
}
this.editingArea.appendChild(this.iframe);
if(dojo.render.html.safari){
this.iframe.src=this.iframe.src;
}
var _966=false;
var _967=dojo.lang.hitch(this,function(){
if(!_966){
_966=true;
}else{
return;
}
if(!this.editNode){
if(this.iframe.contentWindow){
this.window=this.iframe.contentWindow;
this.document=this.iframe.contentWindow.document;
}else{
if(this.iframe.contentDocument){
this.window=this.iframe.contentDocument.window;
this.document=this.iframe.contentDocument;
}
}
var _968=(function(_969){
return function(_96a){
return dojo.html.getStyle(_969,_96a);
};
})(this.domNode);
var font=_968("font-weight")+" "+_968("font-size")+" "+_968("font-family");
var _96c="1.0";
var _96d=dojo.html.getUnitValue(this.domNode,"line-height");
if(_96d.value&&_96d.units==""){
_96c=_96d.value;
}
dojo.html.insertCssText("body,html{background:transparent;padding:0;margin:0;}"+"body{top:0;left:0;right:0;"+(((this.height)||(dojo.render.html.opera))?"":"position:fixed;")+"font:"+font+";"+"min-height:"+this.minHeight+";"+"line-height:"+_96c+"}"+"p{margin: 1em 0 !important;}"+"body > *:first-child{padding-top:0 !important;margin-top:"+this._firstChildContributingMargin+"px !important;}"+"body > *:last-child{padding-bottom:0 !important;margin-bottom:"+this._lastChildContributingMargin+"px !important;}"+"li > ul:-moz-first-node, li > ol:-moz-first-node{padding-top:1.2em;}\n"+"li{min-height:1.2em;}"+"",this.document);
dojo.html.removeNode(_961);
this.document.body.innerHTML=html;
if(_95e||dojo.render.html.safari){
this.document.designMode="on";
}
this.onLoad();
}else{
dojo.html.removeNode(_961);
this.editNode.innerHTML=html;
this.onDisplayChanged();
}
});
if(this.editNode){
_967();
}else{
if(dojo.render.html.moz){
this.iframe.onload=function(){
setTimeout(_967,250);
};
}else{
this.iframe.onload=_967;
}
}
},_applyEditingAreaStyleSheets:function(){
var _96e=[];
if(this.styleSheets){
_96e=this.styleSheets.split(";");
this.styleSheets="";
}
_96e=_96e.concat(this.editingAreaStyleSheets);
this.editingAreaStyleSheets=[];
if(_96e.length>0){
for(var i=0;i<_96e.length;i++){
var url=_96e[i];
if(url){
this.addStyleSheet(dojo.uri.dojoUri(url));
}
}
}
},addStyleSheet:function(uri){
var url=uri.toString();
if(dojo.lang.find(this.editingAreaStyleSheets,url)>-1){
dojo.debug("dojo.widget.RichText.addStyleSheet: Style sheet "+url+" is already applied to the editing area!");
return;
}
if(url.charAt(0)=="."||(url.charAt(0)!="/"&&!uri.host)){
url=(new dojo.uri.Uri(dojo.global().location,url)).toString();
}
this.editingAreaStyleSheets.push(url);
if(this.document.createStyleSheet){
this.document.createStyleSheet(url);
}else{
var head=this.document.getElementsByTagName("head")[0];
var _974=this.document.createElement("link");
with(_974){
rel="stylesheet";
type="text/css";
href=url;
}
head.appendChild(_974);
}
},removeStyleSheet:function(uri){
var url=uri.toString();
if(url.charAt(0)=="."||(url.charAt(0)!="/"&&!uri.host)){
url=(new dojo.uri.Uri(dojo.global().location,url)).toString();
}
var _977=dojo.lang.find(this.editingAreaStyleSheets,url);
if(_977==-1){
dojo.debug("dojo.widget.RichText.removeStyleSheet: Style sheet "+url+" is not applied to the editing area so it can not be removed!");
return;
}
delete this.editingAreaStyleSheets[_977];
var _978=this.document.getElementsByTagName("link");
for(var i=0;i<_978.length;i++){
if(_978[i].href==url){
if(dojo.render.html.ie){
_978[i].href="";
}
dojo.html.removeNode(_978[i]);
break;
}
}
},_drawObject:function(html){
this.object=dojo.html.createExternalElement(dojo.doc(),"object");
with(this.object){
classid="clsid:2D360201-FFF5-11D1-8D03-00A0C959BC0A";
width=this.inheritWidth?this._oldWidth:"100%";
style.height=this.height?this.height:(this._oldHeight+"px");
Scrollbars=this.height?true:false;
Appearance=this._activeX.appearance.flat;
}
this.editorObject=this.object;
this.editingArea.appendChild(this.object);
this.object.attachEvent("DocumentComplete",dojo.lang.hitch(this,"onLoad"));
dojo.lang.forEach(this.events,function(e){
this.object.attachEvent(e.toLowerCase(),dojo.lang.hitch(this,e));
},this);
this.object.DocumentHTML="<!doctype HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">"+"<html><title></title>"+"<style type=\"text/css\">"+"    body,html { padding: 0; margin: 0; }"+(this.height?"":"    body,  { overflow: hidden; }")+"</style>"+"<body><div>"+html+"<div></body></html>";
this._cacheLocalBlockFormatNames();
},_local2NativeFormatNames:{},_native2LocalFormatNames:{},_cacheLocalBlockFormatNames:function(){
if(!this._native2LocalFormatNames["p"]){
var obj=this.object;
var _97d=false;
if(!obj){
try{
obj=dojo.html.createExternalElement(dojo.doc(),"object");
obj.classid="clsid:2D360201-FFF5-11D1-8D03-00A0C959BC0A";
dojo.body().appendChild(obj);
obj.DocumentHTML="<html><head></head><body></body></html>";
}
catch(e){
_97d=true;
}
}
try{
var _97e=new ActiveXObject("DEGetBlockFmtNamesParam.DEGetBlockFmtNamesParam");
obj.ExecCommand(this._activeX.command["getblockformatnames"],0,_97e);
var _97f=new VBArray(_97e.Names);
var _980=_97f.toArray();
var _981=["p","pre","address","h1","h2","h3","h4","h5","h6","ol","ul","","","","","div"];
for(var i=0;i<_981.length;++i){
if(_981[i].length>0){
this._local2NativeFormatNames[_980[i]]=_981[i];
this._native2LocalFormatNames[_981[i]]=_980[i];
}
}
}
catch(e){
_97d=true;
}
if(obj&&!this.object){
dojo.body().removeChild(obj);
}
}
return !_97d;
},_isResized:function(){
return false;
},onLoad:function(e){
this.isLoaded=true;
if(this.object){
this.document=this.object.DOM;
this.window=this.document.parentWindow;
this.editNode=this.document.body.firstChild;
this.editingArea.style.height=this.height?this.height:this.minHeight;
if(!this.height){
this.connect(this,"onDisplayChanged","_updateHeight");
}
this.window._frameElement=this.object;
}else{
if(this.iframe&&!dojo.render.html.ie){
this.editNode=this.document.body;
if(!this.height){
this.connect(this,"onDisplayChanged","_updateHeight");
}
try{
this.document.execCommand("useCSS",false,true);
this.document.execCommand("styleWithCSS",false,false);
}
catch(e2){
}
if(dojo.render.html.safari){
this.connect(this.editNode,"onblur","onBlur");
this.connect(this.editNode,"onfocus","onFocus");
this.connect(this.editNode,"onclick","onFocus");
this.interval=setInterval(dojo.lang.hitch(this,"onDisplayChanged"),750);
}else{
if(dojo.render.html.mozilla||dojo.render.html.opera){
var doc=this.document;
var _985=dojo.event.browser.addListener;
var self=this;
dojo.lang.forEach(this.events,function(e){
var l=_985(self.document,e.substr(2).toLowerCase(),dojo.lang.hitch(self,e));
if(e=="onBlur"){
var _989={unBlur:function(e){
dojo.event.browser.removeListener(doc,"blur",l);
}};
dojo.event.connect("before",self,"close",_989,"unBlur");
}
});
}
}
}else{
if(dojo.render.html.ie){
if(!this.height){
this.connect(this,"onDisplayChanged","_updateHeight");
}
this.editNode.style.zoom=1;
}
}
}
this._applyEditingAreaStyleSheets();
if(this.focusOnLoad){
this.focus();
}
this.onDisplayChanged(e);
if(this.onLoadDeferred){
this.onLoadDeferred.callback(true);
}
},onKeyDown:function(e){
if((!e)&&(this.object)){
e=dojo.event.browser.fixEvent(this.window.event);
}
if((dojo.render.html.ie)&&(e.keyCode==e.KEY_TAB)){
e.preventDefault();
e.stopPropagation();
this.execCommand((e.shiftKey?"outdent":"indent"));
}else{
if(dojo.render.html.ie){
if((65<=e.keyCode)&&(e.keyCode<=90)){
e.charCode=e.keyCode;
this.onKeyPress(e);
}
}
}
},onKeyUp:function(e){
return;
},KEY_CTRL:1,onKeyPress:function(e){
if((!e)&&(this.object)){
e=dojo.event.browser.fixEvent(this.window.event);
}
var _98e=e.ctrlKey?this.KEY_CTRL:0;
if(this._keyHandlers[e.key]){
var _98f=this._keyHandlers[e.key],i=0,_991;
while(_991=_98f[i++]){
if(_98e==_991.modifiers){
e.preventDefault();
_991.handler.call(this);
break;
}
}
}
dojo.lang.setTimeout(this,this.onKeyPressed,1,e);
},addKeyHandler:function(key,_993,_994){
if(!(this._keyHandlers[key] instanceof Array)){
this._keyHandlers[key]=[];
}
this._keyHandlers[key].push({modifiers:_993||0,handler:_994});
},onKeyPressed:function(e){
this.onDisplayChanged();
},onClick:function(e){
this.onDisplayChanged(e);
},onBlur:function(e){
},_initialFocus:true,onFocus:function(e){
if((dojo.render.html.mozilla)&&(this._initialFocus)){
this._initialFocus=false;
if(dojo.string.trim(this.editNode.innerHTML)=="&nbsp;"){
this.placeCursorAtStart();
}
}
},blur:function(){
if(this.iframe){
this.window.blur();
}else{
if(this.object){
this.document.body.blur();
}else{
if(this.editNode){
this.editNode.blur();
}
}
}
},focus:function(){
if(this.iframe&&!dojo.render.html.ie){
this.window.focus();
}else{
if(this.object){
this.document.focus();
}else{
if(this.editNode&&this.editNode.focus){
this.editNode.focus();
}else{
dojo.debug("Have no idea how to focus into the editor!");
}
}
}
},onDisplayChanged:function(e){
},_activeX:{command:{bold:5000,italic:5023,underline:5048,justifycenter:5024,justifyleft:5025,justifyright:5026,cut:5003,copy:5002,paste:5032,"delete":5004,undo:5049,redo:5033,removeformat:5034,selectall:5035,unlink:5050,indent:5018,outdent:5031,insertorderedlist:5030,insertunorderedlist:5051,inserttable:5022,insertcell:5019,insertcol:5020,insertrow:5021,deletecells:5005,deletecols:5006,deleterows:5007,mergecells:5029,splitcell:5047,setblockformat:5043,getblockformat:5011,getblockformatnames:5012,setfontname:5044,getfontname:5013,setfontsize:5045,getfontsize:5014,setbackcolor:5042,getbackcolor:5010,setforecolor:5046,getforecolor:5015,findtext:5008,font:5009,hyperlink:5016,image:5017,lockelement:5027,makeabsolute:5028,sendbackward:5036,bringforward:5037,sendbelowtext:5038,bringabovetext:5039,sendtoback:5040,bringtofront:5041,properties:5052},ui:{"default":0,prompt:1,noprompt:2},status:{notsupported:0,disabled:1,enabled:3,latched:7,ninched:11},appearance:{flat:0,inset:1},state:{unchecked:0,checked:1,gray:2}},_normalizeCommand:function(cmd){
var drh=dojo.render.html;
var _99c=cmd.toLowerCase();
if(_99c=="formatblock"){
if(drh.safari){
_99c="heading";
}
}else{
if(this.object){
switch(_99c){
case "createlink":
_99c="hyperlink";
break;
case "insertimage":
_99c="image";
break;
}
}else{
if(_99c=="hilitecolor"&&!drh.mozilla){
_99c="backcolor";
}
}
}
return _99c;
},_safariIsLeopard:function(){
var _99d=false;
if(dojo.render.html.safari){
var tmp=dojo.render.html.UA.split("AppleWebKit/")[1];
var ver=parseFloat(tmp.split(" ")[0]);
if(ver>=420){
_99d=true;
}
}
return _99d;
},queryCommandAvailable:function(_9a0){
var ie=1;
var _9a2=1<<1;
var _9a3=1<<2;
var _9a4=1<<3;
var _9a5=1<<4;
var _9a6=this._safariIsLeopard();
function isSupportedBy(_9a7){
return {ie:Boolean(_9a7&ie),mozilla:Boolean(_9a7&_9a2),safari:Boolean(_9a7&_9a3),safari420:Boolean(_9a7&_9a5),opera:Boolean(_9a7&_9a4)};
}
var _9a8=null;
switch(_9a0.toLowerCase()){
case "bold":
case "italic":
case "underline":
case "subscript":
case "superscript":
case "fontname":
case "fontsize":
case "forecolor":
case "hilitecolor":
case "justifycenter":
case "justifyfull":
case "justifyleft":
case "justifyright":
case "delete":
case "selectall":
_9a8=isSupportedBy(_9a2|ie|_9a3|_9a4);
break;
case "createlink":
case "unlink":
case "removeformat":
case "inserthorizontalrule":
case "insertimage":
case "insertorderedlist":
case "insertunorderedlist":
case "indent":
case "outdent":
case "formatblock":
case "inserthtml":
case "undo":
case "redo":
case "strikethrough":
_9a8=isSupportedBy(_9a2|ie|_9a4|_9a5);
break;
case "blockdirltr":
case "blockdirrtl":
case "dirltr":
case "dirrtl":
case "inlinedirltr":
case "inlinedirrtl":
_9a8=isSupportedBy(ie);
break;
case "cut":
case "copy":
case "paste":
_9a8=isSupportedBy(ie|_9a2|_9a5);
break;
case "inserttable":
_9a8=isSupportedBy(_9a2|(this.object?ie:0));
break;
case "insertcell":
case "insertcol":
case "insertrow":
case "deletecells":
case "deletecols":
case "deleterows":
case "mergecells":
case "splitcell":
_9a8=isSupportedBy(this.object?ie:0);
break;
default:
return false;
}
return (dojo.render.html.ie&&_9a8.ie)||(dojo.render.html.mozilla&&_9a8.mozilla)||(dojo.render.html.safari&&_9a8.safari)||(_9a6&&_9a8.safari420)||(dojo.render.html.opera&&_9a8.opera);
},execCommand:function(_9a9,_9aa){
var _9ab;
this.focus();
_9a9=this._normalizeCommand(_9a9);
if(_9aa!=undefined){
if(_9a9=="heading"){
throw new Error("unimplemented");
}else{
if(_9a9=="formatblock"){
if(this.object){
_9aa=this._native2LocalFormatNames[_9aa];
}else{
if(dojo.render.html.ie){
_9aa="<"+_9aa+">";
}
}
}
}
}
if(this.object){
switch(_9a9){
case "hilitecolor":
_9a9="setbackcolor";
break;
case "forecolor":
case "backcolor":
case "fontsize":
case "fontname":
_9a9="set"+_9a9;
break;
case "formatblock":
_9a9="setblockformat";
}
if(_9a9=="strikethrough"){
_9a9="inserthtml";
var _9ac=this.document.selection.createRange();
if(!_9ac.htmlText){
return;
}
_9aa=_9ac.htmlText.strike();
}else{
if(_9a9=="inserthorizontalrule"){
_9a9="inserthtml";
_9aa="<hr>";
}
}
if(_9a9=="inserthtml"){
var _9ac=this.document.selection.createRange();
if(this.document.selection.type.toUpperCase()=="CONTROL"){
for(var i=0;i<_9ac.length;i++){
_9ac.item(i).outerHTML=_9aa;
}
}else{
_9ac.pasteHTML(_9aa);
_9ac.select();
}
_9ab=true;
}else{
if(arguments.length==1){
_9ab=this.object.ExecCommand(this._activeX.command[_9a9],this._activeX.ui.noprompt);
}else{
_9ab=this.object.ExecCommand(this._activeX.command[_9a9],this._activeX.ui.noprompt,_9aa);
}
}
}else{
if(_9a9=="inserthtml"){
if(dojo.render.html.ie){
var _9ae=this.document.selection.createRange();
_9ae.pasteHTML(_9aa);
_9ae.select();
return true;
}else{
return this.document.execCommand(_9a9,false,_9aa);
}
}else{
if((_9a9=="unlink")&&(this.queryCommandEnabled("unlink"))&&(dojo.render.html.mozilla)){
var _9af=this.window.getSelection();
var _9b0=_9af.getRangeAt(0);
var _9b1=_9b0.startContainer;
var _9b2=_9b0.startOffset;
var _9b3=_9b0.endContainer;
var _9b4=_9b0.endOffset;
var a=dojo.withGlobal(this.window,"getAncestorElement",dojo.html.selection,["a"]);
dojo.withGlobal(this.window,"selectElement",dojo.html.selection,[a]);
_9ab=this.document.execCommand("unlink",false,null);
var _9b0=this.document.createRange();
_9b0.setStart(_9b1,_9b2);
_9b0.setEnd(_9b3,_9b4);
_9af.removeAllRanges();
_9af.addRange(_9b0);
return _9ab;
}else{
if((_9a9=="hilitecolor")&&(dojo.render.html.mozilla)){
this.document.execCommand("useCSS",false,false);
_9ab=this.document.execCommand(_9a9,false,_9aa);
this.document.execCommand("useCSS",false,true);
}else{
if((dojo.render.html.ie)&&((_9a9=="backcolor")||(_9a9=="forecolor"))){
_9aa=arguments.length>1?_9aa:null;
_9ab=this.document.execCommand(_9a9,false,_9aa);
}else{
_9aa=arguments.length>1?_9aa:null;
if(_9aa||_9a9!="createlink"){
_9ab=this.document.execCommand(_9a9,false,_9aa);
}
}
}
}
}
}
this.onDisplayChanged();
return _9ab;
},queryCommandEnabled:function(_9b6){
_9b6=this._normalizeCommand(_9b6);
if(this.object){
switch(_9b6){
case "hilitecolor":
_9b6="setbackcolor";
break;
case "forecolor":
case "backcolor":
case "fontsize":
case "fontname":
_9b6="set"+_9b6;
break;
case "formatblock":
_9b6="setblockformat";
break;
case "strikethrough":
_9b6="bold";
break;
case "inserthorizontalrule":
return true;
}
if(typeof this._activeX.command[_9b6]=="undefined"){
return false;
}
var _9b7=this.object.QueryStatus(this._activeX.command[_9b6]);
return ((_9b7!=this._activeX.status.notsupported)&&(_9b7!=this._activeX.status.disabled));
}else{
if(dojo.render.html.mozilla){
if(_9b6=="unlink"){
return dojo.withGlobal(this.window,"hasAncestorElement",dojo.html.selection,["a"]);
}else{
if(_9b6=="inserttable"){
return true;
}
}
}
var elem=(dojo.render.html.ie)?this.document.selection.createRange():this.document;
return elem.queryCommandEnabled(_9b6);
}
},queryCommandState:function(_9b9){
_9b9=this._normalizeCommand(_9b9);
if(this.object){
if(_9b9=="forecolor"){
_9b9="setforecolor";
}else{
if(_9b9=="backcolor"){
_9b9="setbackcolor";
}else{
if(_9b9=="strikethrough"){
return dojo.withGlobal(this.window,"hasAncestorElement",dojo.html.selection,["strike"]);
}else{
if(_9b9=="inserthorizontalrule"){
return false;
}
}
}
}
if(typeof this._activeX.command[_9b9]=="undefined"){
return null;
}
var _9ba=this.object.QueryStatus(this._activeX.command[_9b9]);
return ((_9ba==this._activeX.status.latched)||(_9ba==this._activeX.status.ninched));
}else{
return this.document.queryCommandState(_9b9);
}
},queryCommandValue:function(_9bb){
_9bb=this._normalizeCommand(_9bb);
if(this.object){
switch(_9bb){
case "forecolor":
case "backcolor":
case "fontsize":
case "fontname":
_9bb="get"+_9bb;
return this.object.execCommand(this._activeX.command[_9bb],this._activeX.ui.noprompt);
case "formatblock":
var _9bc=this.object.execCommand(this._activeX.command["getblockformat"],this._activeX.ui.noprompt);
if(_9bc){
return this._local2NativeFormatNames[_9bc];
}
}
}else{
if(dojo.render.html.ie&&_9bb=="formatblock"){
return this._local2NativeFormatNames[this.document.queryCommandValue(_9bb)]||this.document.queryCommandValue(_9bb);
}
return this.document.queryCommandValue(_9bb);
}
},placeCursorAtStart:function(){
this.focus();
if(dojo.render.html.moz&&this.editNode.firstChild&&this.editNode.firstChild.nodeType!=dojo.dom.TEXT_NODE){
dojo.withGlobal(this.window,"selectElementChildren",dojo.html.selection,[this.editNode.firstChild]);
}else{
dojo.withGlobal(this.window,"selectElementChildren",dojo.html.selection,[this.editNode]);
}
dojo.withGlobal(this.window,"collapse",dojo.html.selection,[true]);
},placeCursorAtEnd:function(){
this.focus();
if(dojo.render.html.moz&&this.editNode.lastChild&&this.editNode.lastChild.nodeType!=dojo.dom.TEXT_NODE){
dojo.withGlobal(this.window,"selectElementChildren",dojo.html.selection,[this.editNode.lastChild]);
}else{
dojo.withGlobal(this.window,"selectElementChildren",dojo.html.selection,[this.editNode]);
}
dojo.withGlobal(this.window,"collapse",dojo.html.selection,[false]);
},replaceEditorContent:function(html){
html=this._preFilterContent(html);
if(this.isClosed){
this.domNode.innerHTML=html;
}else{
if(this.window&&this.window.getSelection&&!dojo.render.html.moz){
this.editNode.innerHTML=html;
}else{
if((this.window&&this.window.getSelection)||(this.document&&this.document.selection)){
this.execCommand("selectall");
if(dojo.render.html.moz&&!html){
html="&nbsp;";
}
this.execCommand("inserthtml",html);
}
}
}
},_preFilterContent:function(html){
var ec=html;
dojo.lang.forEach(this.contentPreFilters,function(ef){
ec=ef(ec);
});
if(this.contentDomPreFilters.length>0){
var dom=dojo.doc().createElement("div");
dom.style.display="none";
dojo.body().appendChild(dom);
dom.innerHTML=ec;
dojo.lang.forEach(this.contentDomPreFilters,function(ef){
dom=ef(dom);
});
ec=dom.innerHTML;
dojo.body().removeChild(dom);
}
return ec;
},_postFilterContent:function(html){
var ec=html;
if(this.contentDomPostFilters.length>0){
var dom=this.document.createElement("div");
dom.innerHTML=ec;
dojo.lang.forEach(this.contentDomPostFilters,function(ef){
dom=ef(dom);
});
ec=dom.innerHTML;
}
dojo.lang.forEach(this.contentPostFilters,function(ef){
ec=ef(ec);
});
return ec;
},_lastHeight:0,_updateHeight:function(){
if(!this.isLoaded){
return;
}
if(this.height){
return;
}
var _9c8=dojo.html.getBorderBox(this.editNode).height;
if(!_9c8){
_9c8=dojo.html.getBorderBox(this.document.body).height;
}
if(_9c8==0){
dojo.debug("Can not figure out the height of the editing area!");
return;
}
this._lastHeight=_9c8;
this.editorObject.style.height=this._lastHeight+"px";
this.window.scrollTo(0,0);
},_saveContent:function(e){
var _9ca=dojo.doc().getElementById("dojo.widget.RichText.savedContent");
_9ca.value+=this._SEPARATOR+this.saveName+":"+this.getEditorContent();
},getEditorContent:function(){
var ec="";
try{
ec=(this._content.length>0)?this._content:this.editNode.innerHTML;
if(dojo.string.trim(ec)=="&nbsp;"){
ec="";
}
}
catch(e){
}
if(dojo.render.html.ie&&!this.object){
var re=new RegExp("(?:<p>&nbsp;</p>[\n\r]*)+$","i");
ec=ec.replace(re,"");
}
ec=this._postFilterContent(ec);
if(this.relativeImageUrls){
var _9cd=dojo.global().location.protocol+"//"+dojo.global().location.host;
var _9ce=dojo.global().location.pathname;
if(_9ce.match(/\/$/)){
}else{
var _9cf=_9ce.split("/");
if(_9cf.length){
_9cf.pop();
}
_9ce=_9cf.join("/")+"/";
}
var _9d0=new RegExp("(<img[^>]* src=[\"'])("+_9cd+"("+_9ce+")?)","ig");
ec=ec.replace(_9d0,"$1");
}
return ec;
},close:function(save,_9d2){
if(this.isClosed){
return false;
}
if(arguments.length==0){
save=true;
}
this._content=this._postFilterContent(this.editNode.innerHTML);
var _9d3=(this.savedContent!=this._content);
if(this.interval){
clearInterval(this.interval);
}
if(dojo.render.html.ie&&!this.object){
dojo.event.browser.clean(this.editNode);
}
if(this.iframe){
delete this.iframe;
}
if(this.textarea){
with(this.textarea.style){
position="";
left=top="";
if(dojo.render.html.ie){
overflow=this.__overflow;
this.__overflow=null;
}
}
if(save){
this.textarea.value=this._content;
}else{
this.textarea.value=this.savedContent;
}
dojo.html.removeNode(this.domNode);
this.domNode=this.textarea;
}else{
if(save){
if(dojo.render.html.moz){
var nc=dojo.doc().createElement("span");
this.domNode.appendChild(nc);
nc.innerHTML=this.editNode.innerHTML;
}else{
this.domNode.innerHTML=this._content;
}
}else{
this.domNode.innerHTML=this.savedContent;
}
}
dojo.html.removeClass(this.domNode,"RichTextEditable");
this.isClosed=true;
this.isLoaded=false;
delete this.editNode;
if(this.window._frameElement){
this.window._frameElement=null;
}
this.window=null;
this.document=null;
this.object=null;
this.editingArea=null;
this.editorObject=null;
return _9d3;
},destroyRendering:function(){
},destroy:function(){
this.destroyRendering();
if(!this.isClosed){
this.close(false);
}
dojo.widget.RichText.superclass.destroy.call(this);
},connect:function(_9d5,_9d6,_9d7){
dojo.event.connect(_9d5,_9d6,this,_9d7);
},disconnect:function(_9d8,_9d9,_9da){
dojo.event.disconnect(_9d8,_9d9,this,_9da);
},disconnectAllWithRoot:function(_9db){
dojo.deprecated("disconnectAllWithRoot","is deprecated. No need to disconnect manually","0.5");
},_fixContentForMoz:function(html){
html=html.replace(/<strong([ \>])/gi,"<b$1");
html=html.replace(/<\/strong>/gi,"</b>");
html=html.replace(/<em([ \>])/gi,"<i$1");
html=html.replace(/<\/em>/gi,"</i>");
return html;
}});
dojo.provide("dojo.widget.ColorPalette");
dojo.widget.defineWidget("dojo.widget.ColorPalette",dojo.widget.HtmlWidget,{palette:"7x10",_palettes:{"7x10":[["fff","fcc","fc9","ff9","ffc","9f9","9ff","cff","ccf","fcf"],["ccc","f66","f96","ff6","ff3","6f9","3ff","6ff","99f","f9f"],["c0c0c0","f00","f90","fc6","ff0","3f3","6cc","3cf","66c","c6c"],["999","c00","f60","fc3","fc0","3c0","0cc","36f","63f","c3c"],["666","900","c60","c93","990","090","399","33f","60c","939"],["333","600","930","963","660","060","366","009","339","636"],["000","300","630","633","330","030","033","006","309","303"]],"3x4":[["ffffff","00ff00","008000","0000ff"],["c0c0c0","ffff00","ff00ff","000080"],["808080","ff0000","800080","000000"]]},buildRendering:function(){
this.domNode=document.createElement("table");
dojo.html.disableSelection(this.domNode);
dojo.event.connect(this.domNode,"onmousedown",function(e){
e.preventDefault();
});
with(this.domNode){
cellPadding="0";
cellSpacing="1";
border="1";
style.backgroundColor="white";
}
var _9de=this._palettes[this.palette];
for(var i=0;i<_9de.length;i++){
var tr=this.domNode.insertRow(-1);
for(var j=0;j<_9de[i].length;j++){
if(_9de[i][j].length==3){
_9de[i][j]=_9de[i][j].replace(/(.)(.)(.)/,"$1$1$2$2$3$3");
}
var td=tr.insertCell(-1);
with(td.style){
backgroundColor="#"+_9de[i][j];
border="1px solid gray";
width=height="15px";
fontSize="1px";
}
td.color="#"+_9de[i][j];
td.onmouseover=function(e){
this.style.borderColor="white";
};
td.onmouseout=function(e){
this.style.borderColor="gray";
};
dojo.event.connect(td,"onmousedown",this,"onClick");
td.innerHTML="&nbsp;";
}
}
},onClick:function(e){
this.onColorSelect(e.currentTarget.color);
e.currentTarget.style.borderColor="gray";
},onColorSelect:function(_9e6){
}});
dojo.provide("dojo.widget.Editor");
dojo.deprecated("dojo.widget.Editor","is replaced by dojo.widget.Editor2","0.5");
dojo.widget.tags.addParseTreeHandler("dojo:Editor");
dojo.widget.Editor=function(){
dojo.widget.HtmlWidget.call(this);
this.contentFilters=[];
this._toolbars=[];
};
dojo.inherits(dojo.widget.Editor,dojo.widget.HtmlWidget);
dojo.widget.Editor.itemGroups={textGroup:["bold","italic","underline","strikethrough"],blockGroup:["formatBlock","fontName","fontSize"],justifyGroup:["justifyleft","justifycenter","justifyright"],commandGroup:["save","cancel"],colorGroup:["forecolor","hilitecolor"],listGroup:["insertorderedlist","insertunorderedlist"],indentGroup:["outdent","indent"],linkGroup:["createlink","insertimage","inserthorizontalrule"]};
dojo.widget.Editor.formatBlockValues={"Normal":"p","Main heading":"h2","Sub heading":"h3","Sub sub heading":"h4","Preformatted":"pre"};
dojo.widget.Editor.fontNameValues={"Arial":"Arial, Helvetica, sans-serif","Verdana":"Verdana, sans-serif","Times New Roman":"Times New Roman, serif","Courier":"Courier New, monospace"};
dojo.widget.Editor.fontSizeValues={"1 (8 pt)":"1","2 (10 pt)":"2","3 (12 pt)":"3","4 (14 pt)":"4","5 (18 pt)":"5","6 (24 pt)":"6","7 (36 pt)":"7"};
dojo.widget.Editor.defaultItems=["commandGroup","|","blockGroup","|","textGroup","|","colorGroup","|","justifyGroup","|","listGroup","indentGroup","|","linkGroup"];
dojo.widget.Editor.supportedCommands=["save","cancel","|","-","/"," "];
dojo.lang.extend(dojo.widget.Editor,{widgetType:"Editor",saveUrl:"",saveMethod:"post",saveArgName:"editorContent",closeOnSave:false,items:dojo.widget.Editor.defaultItems,formatBlockItems:dojo.lang.shallowCopy(dojo.widget.Editor.formatBlockValues),fontNameItems:dojo.lang.shallowCopy(dojo.widget.Editor.fontNameValues),fontSizeItems:dojo.lang.shallowCopy(dojo.widget.Editor.fontSizeValues),getItemProperties:function(name){
var _9e8={};
switch(name.toLowerCase()){
case "bold":
case "italic":
case "underline":
case "strikethrough":
_9e8.toggleItem=true;
break;
case "justifygroup":
_9e8.defaultButton="justifyleft";
_9e8.preventDeselect=true;
_9e8.buttonGroup=true;
break;
case "listgroup":
_9e8.buttonGroup=true;
break;
case "save":
case "cancel":
_9e8.label=dojo.string.capitalize(name);
break;
case "forecolor":
case "hilitecolor":
_9e8.name=name;
_9e8.toggleItem=true;
_9e8.icon=this.getCommandImage(name);
break;
case "formatblock":
_9e8.name="formatBlock";
_9e8.values=this.formatBlockItems;
break;
case "fontname":
_9e8.name="fontName";
_9e8.values=this.fontNameItems;
case "fontsize":
_9e8.name="fontSize";
_9e8.values=this.fontSizeItems;
}
return _9e8;
},validateItems:true,focusOnLoad:true,minHeight:"1em",_richText:null,_richTextType:"RichText",_toolbarContainer:null,_toolbarContainerType:"ToolbarContainer",_toolbars:[],_toolbarType:"Toolbar",_toolbarItemType:"ToolbarItem",buildRendering:function(args,frag){
var node=frag["dojo:"+this.widgetType.toLowerCase()]["nodeRef"];
var trt=dojo.widget.createWidget(this._richTextType,{focusOnLoad:this.focusOnLoad,minHeight:this.minHeight},node);
var _9ed=this;
setTimeout(function(){
_9ed.setRichText(trt);
_9ed.initToolbar();
_9ed.fillInTemplate(args,frag);
},0);
},setRichText:function(_9ee){
if(this._richText&&this._richText==_9ee){
dojo.debug("Already set the richText to this richText!");
return;
}
if(this._richText&&!this._richText.isClosed){
dojo.debug("You are switching richTexts yet you haven't closed the current one. Losing reference!");
}
this._richText=_9ee;
dojo.event.connect(this._richText,"close",this,"onClose");
dojo.event.connect(this._richText,"onLoad",this,"onLoad");
dojo.event.connect(this._richText,"onDisplayChanged",this,"updateToolbar");
if(this._toolbarContainer){
this._toolbarContainer.enable();
this.updateToolbar(true);
}
},initToolbar:function(){
if(this._toolbarContainer){
return;
}
this._toolbarContainer=dojo.widget.createWidget(this._toolbarContainerType);
var tb=this.addToolbar();
var last=true;
for(var i=0;i<this.items.length;i++){
if(this.items[i]=="\n"){
tb=this.addToolbar();
}else{
if((this.items[i]=="|")&&(!last)){
last=true;
}else{
last=this.addItem(this.items[i],tb);
}
}
}
this.insertToolbar(this._toolbarContainer.domNode,this._richText.domNode);
},insertToolbar:function(_9f2,_9f3){
dojo.html.insertBefore(_9f2,_9f3);
},addToolbar:function(_9f4){
this.initToolbar();
if(!(_9f4 instanceof dojo.widget.Toolbar)){
_9f4=dojo.widget.createWidget(this._toolbarType);
}
this._toolbarContainer.addChild(_9f4);
this._toolbars.push(_9f4);
return _9f4;
},addItem:function(item,tb,_9f7){
if(!tb){
tb=this._toolbars[0];
}
var cmd=((item)&&(!dojo.lang.isUndefined(item["getValue"])))?cmd=item["getValue"]():item;
var _9f9=dojo.widget.Editor.itemGroups;
if(item instanceof dojo.widget.ToolbarItem){
tb.addChild(item);
}else{
if(_9f9[cmd]){
var _9fa=_9f9[cmd];
var _9fb=true;
if(cmd=="justifyGroup"||cmd=="listGroup"){
var _9fc=[cmd];
for(var i=0;i<_9fa.length;i++){
if(_9f7||this.isSupportedCommand(_9fa[i])){
_9fc.push(this.getCommandImage(_9fa[i]));
}else{
_9fb=false;
}
}
if(_9fc.length){
var btn=tb.addChild(_9fc,null,this.getItemProperties(cmd));
dojo.event.connect(btn,"onClick",this,"_action");
dojo.event.connect(btn,"onChangeSelect",this,"_action");
}
return _9fb;
}else{
for(var i=0;i<_9fa.length;i++){
if(!this.addItem(_9fa[i],tb)){
_9fb=false;
}
}
return _9fb;
}
}else{
if((!_9f7)&&(!this.isSupportedCommand(cmd))){
return false;
}
if(_9f7||this.isSupportedCommand(cmd)){
cmd=cmd.toLowerCase();
if(cmd=="formatblock"){
var _9ff=dojo.widget.createWidget("ToolbarSelect",{name:"formatBlock",values:this.formatBlockItems});
tb.addChild(_9ff);
var _a00=this;
dojo.event.connect(_9ff,"onSetValue",function(item,_a02){
_a00.onAction("formatBlock",_a02);
});
}else{
if(cmd=="fontname"){
var _9ff=dojo.widget.createWidget("ToolbarSelect",{name:"fontName",values:this.fontNameItems});
tb.addChild(_9ff);
dojo.event.connect(_9ff,"onSetValue",dojo.lang.hitch(this,function(item,_a04){
this.onAction("fontName",_a04);
}));
}else{
if(cmd=="fontsize"){
var _9ff=dojo.widget.createWidget("ToolbarSelect",{name:"fontSize",values:this.fontSizeItems});
tb.addChild(_9ff);
dojo.event.connect(_9ff,"onSetValue",dojo.lang.hitch(this,function(item,_a06){
this.onAction("fontSize",_a06);
}));
}else{
if(dojo.lang.inArray(cmd,["forecolor","hilitecolor"])){
var btn=tb.addChild(dojo.widget.createWidget("ToolbarColorDialog",this.getItemProperties(cmd)));
dojo.event.connect(btn,"onSetValue",this,"_setValue");
}else{
var btn=tb.addChild(this.getCommandImage(cmd),null,this.getItemProperties(cmd));
if(cmd=="save"){
dojo.event.connect(btn,"onClick",this,"_save");
}else{
if(cmd=="cancel"){
dojo.event.connect(btn,"onClick",this,"_close");
}else{
dojo.event.connect(btn,"onClick",this,"_action");
dojo.event.connect(btn,"onChangeSelect",this,"_action");
}
}
}
}
}
}
}
}
}
return true;
},enableToolbar:function(){
if(this._toolbarContainer){
this._toolbarContainer.domNode.style.display="";
this._toolbarContainer.enable();
}
},disableToolbar:function(hide){
if(hide){
if(this._toolbarContainer){
this._toolbarContainer.domNode.style.display="none";
}
}else{
if(this._toolbarContainer){
this._toolbarContainer.disable();
}
}
},_updateToolbarLastRan:null,_updateToolbarTimer:null,_updateToolbarFrequency:500,updateToolbar:function(_a08){
if(!this._toolbarContainer){
return;
}
var diff=new Date()-this._updateToolbarLastRan;
if(!_a08&&this._updateToolbarLastRan&&(diff<this._updateToolbarFrequency)){
clearTimeout(this._updateToolbarTimer);
var _a0a=this;
this._updateToolbarTimer=setTimeout(function(){
_a0a.updateToolbar();
},this._updateToolbarFrequency/2);
return;
}else{
this._updateToolbarLastRan=new Date();
}
var _a0b=this._toolbarContainer.getItems();
for(var i=0;i<_a0b.length;i++){
var item=_a0b[i];
if(item instanceof dojo.widget.ToolbarSeparator){
continue;
}
var cmd=item._name;
if(cmd=="save"||cmd=="cancel"){
continue;
}else{
if(cmd=="justifyGroup"){
try{
if(!this._richText.queryCommandEnabled("justifyleft")){
item.disable(false,true);
}else{
item.enable(false,true);
var _a0f=item.getItems();
for(var j=0;j<_a0f.length;j++){
var name=_a0f[j]._name;
var _a12=this._richText.queryCommandValue(name);
if(typeof _a12=="boolean"&&_a12){
_a12=name;
break;
}else{
if(typeof _a12=="string"){
_a12="justify"+_a12;
}else{
_a12=null;
}
}
}
if(!_a12){
_a12="justifyleft";
}
item.setValue(_a12,false,true);
}
}
catch(err){
}
}else{
if(cmd=="listGroup"){
var _a13=item.getItems();
for(var j=0;j<_a13.length;j++){
this.updateItem(_a13[j]);
}
}else{
this.updateItem(item);
}
}
}
}
},updateItem:function(item){
try{
var cmd=item._name;
var _a16=this._richText.queryCommandEnabled(cmd);
item.setEnabled(_a16,false,true);
var _a17=this._richText.queryCommandState(cmd);
if(_a17&&cmd=="underline"){
_a17=!this._richText.queryCommandEnabled("unlink");
}
item.setSelected(_a17,false,true);
return true;
}
catch(err){
return false;
}
},supportedCommands:dojo.widget.Editor.supportedCommands.concat(),isSupportedCommand:function(cmd){
var yes=dojo.lang.inArray(cmd,this.supportedCommands);
if(!yes){
try{
var _a1a=this._richText||dojo.widget.HtmlRichText.prototype;
yes=_a1a.queryCommandAvailable(cmd);
}
catch(E){
}
}
return yes;
},getCommandImage:function(cmd){
if(cmd=="|"){
return cmd;
}else{
return dojo.uri.dojoUri("src/widget/templates/buttons/"+cmd+".gif");
}
},_action:function(e){
this._fire("onAction",e.getValue());
},_setValue:function(a,b){
this._fire("onAction",a.getValue(),b);
},_save:function(e){
if(!this._richText.isClosed){
if(this.saveUrl.length){
var _a20={};
_a20[this.saveArgName]=this.getHtml();
dojo.io.bind({method:this.saveMethod,url:this.saveUrl,content:_a20});
}else{
dojo.debug("please set a saveUrl for the editor");
}
if(this.closeOnSave){
this._richText.close(e.getName().toLowerCase()=="save");
}
}
},_close:function(e){
if(!this._richText.isClosed){
this._richText.close(e.getName().toLowerCase()=="save");
}
},onAction:function(cmd,_a23){
switch(cmd){
case "createlink":
if(!(_a23=prompt("Please enter the URL of the link:","http://"))){
return;
}
break;
case "insertimage":
if(!(_a23=prompt("Please enter the URL of the image:","http://"))){
return;
}
break;
}
this._richText.execCommand(cmd,_a23);
},fillInTemplate:function(args,frag){
},_fire:function(_a26){
if(dojo.lang.isFunction(this[_a26])){
var args=[];
if(arguments.length==1){
args.push(this);
}else{
for(var i=1;i<arguments.length;i++){
args.push(arguments[i]);
}
}
this[_a26].apply(this,args);
}
},getHtml:function(){
this._richText.contentFilters=this._richText.contentFilters.concat(this.contentFilters);
return this._richText.getEditorContent();
},getEditorContent:function(){
return this.getHtml();
},onClose:function(save,hide){
this.disableToolbar(hide);
if(save){
this._fire("onSave");
}else{
this._fire("onCancel");
}
},onLoad:function(){
},onSave:function(){
},onCancel:function(){
}});
dojo.provide("dojo.lang.type");
dojo.lang.whatAmI=function(_a2b){
dojo.deprecated("dojo.lang.whatAmI","use dojo.lang.getType instead","0.5");
return dojo.lang.getType(_a2b);
};
dojo.lang.whatAmI.custom={};
dojo.lang.getType=function(_a2c){
try{
if(dojo.lang.isArray(_a2c)){
return "array";
}
if(dojo.lang.isFunction(_a2c)){
return "function";
}
if(dojo.lang.isString(_a2c)){
return "string";
}
if(dojo.lang.isNumber(_a2c)){
return "number";
}
if(dojo.lang.isBoolean(_a2c)){
return "boolean";
}
if(dojo.lang.isAlien(_a2c)){
return "alien";
}
if(dojo.lang.isUndefined(_a2c)){
return "undefined";
}
for(var name in dojo.lang.whatAmI.custom){
if(dojo.lang.whatAmI.custom[name](_a2c)){
return name;
}
}
if(dojo.lang.isObject(_a2c)){
return "object";
}
}
catch(e){
}
return "unknown";
};
dojo.lang.isNumeric=function(_a2e){
return (!isNaN(_a2e)&&isFinite(_a2e)&&(_a2e!=null)&&!dojo.lang.isBoolean(_a2e)&&!dojo.lang.isArray(_a2e)&&!/^\s*$/.test(_a2e));
};
dojo.lang.isBuiltIn=function(_a2f){
return (dojo.lang.isArray(_a2f)||dojo.lang.isFunction(_a2f)||dojo.lang.isString(_a2f)||dojo.lang.isNumber(_a2f)||dojo.lang.isBoolean(_a2f)||(_a2f==null)||(_a2f instanceof Error)||(typeof _a2f=="error"));
};
dojo.lang.isPureObject=function(_a30){
return ((_a30!=null)&&dojo.lang.isObject(_a30)&&_a30.constructor==Object);
};
dojo.lang.isOfType=function(_a31,type,_a33){
var _a34=false;
if(_a33){
_a34=_a33["optional"];
}
if(_a34&&((_a31===null)||dojo.lang.isUndefined(_a31))){
return true;
}
if(dojo.lang.isArray(type)){
var _a35=type;
for(var i in _a35){
var _a37=_a35[i];
if(dojo.lang.isOfType(_a31,_a37)){
return true;
}
}
return false;
}else{
if(dojo.lang.isString(type)){
type=type.toLowerCase();
}
switch(type){
case Array:
case "array":
return dojo.lang.isArray(_a31);
case Function:
case "function":
return dojo.lang.isFunction(_a31);
case String:
case "string":
return dojo.lang.isString(_a31);
case Number:
case "number":
return dojo.lang.isNumber(_a31);
case "numeric":
return dojo.lang.isNumeric(_a31);
case Boolean:
case "boolean":
return dojo.lang.isBoolean(_a31);
case Object:
case "object":
return dojo.lang.isObject(_a31);
case "pureobject":
return dojo.lang.isPureObject(_a31);
case "builtin":
return dojo.lang.isBuiltIn(_a31);
case "alien":
return dojo.lang.isAlien(_a31);
case "undefined":
return dojo.lang.isUndefined(_a31);
case null:
case "null":
return (_a31===null);
case "optional":
dojo.deprecated("dojo.lang.isOfType(value, [type, \"optional\"])","use dojo.lang.isOfType(value, type, {optional: true} ) instead","0.5");
return ((_a31===null)||dojo.lang.isUndefined(_a31));
default:
if(dojo.lang.isFunction(type)){
return (_a31 instanceof type);
}else{
dojo.raise("dojo.lang.isOfType() was passed an invalid type");
}
}
}
dojo.raise("If we get here, it means a bug was introduced above.");
};
dojo.lang.getObject=function(str){
var _a39=str.split("."),i=0,obj=dj_global;
do{
obj=obj[_a39[i++]];
}while(i<_a39.length&&obj);
return (obj!=dj_global)?obj:null;
};
dojo.lang.doesObjectExist=function(str){
var _a3d=str.split("."),i=0,obj=dj_global;
do{
obj=obj[_a3d[i++]];
}while(i<_a3d.length&&obj);
return (obj&&obj!=dj_global);
};
dojo.provide("dojo.lang.assert");
dojo.lang.assert=function(_a40,_a41){
if(!_a40){
var _a42="An assert statement failed.\n"+"The method dojo.lang.assert() was called with a 'false' value.\n";
if(_a41){
_a42+="Here's the assert message:\n"+_a41+"\n";
}
throw new Error(_a42);
}
};
dojo.lang.assertType=function(_a43,type,_a45){
if(dojo.lang.isString(_a45)){
dojo.deprecated("dojo.lang.assertType(value, type, \"message\")","use dojo.lang.assertType(value, type) instead","0.5");
}
if(!dojo.lang.isOfType(_a43,type,_a45)){
if(!dojo.lang.assertType._errorMessage){
dojo.lang.assertType._errorMessage="Type mismatch: dojo.lang.assertType() failed.";
}
dojo.lang.assert(false,dojo.lang.assertType._errorMessage);
}
};
dojo.lang.assertValidKeywords=function(_a46,_a47,_a48){
var key;
if(!_a48){
if(!dojo.lang.assertValidKeywords._errorMessage){
dojo.lang.assertValidKeywords._errorMessage="In dojo.lang.assertValidKeywords(), found invalid keyword:";
}
_a48=dojo.lang.assertValidKeywords._errorMessage;
}
if(dojo.lang.isArray(_a47)){
for(key in _a46){
if(!dojo.lang.inArray(_a47,key)){
dojo.lang.assert(false,_a48+" "+key);
}
}
}else{
for(key in _a46){
if(!(key in _a47)){
dojo.lang.assert(false,_a48+" "+key);
}
}
}
};
dojo.provide("dojo.AdapterRegistry");
dojo.AdapterRegistry=function(_a4a){
this.pairs=[];
this.returnWrappers=_a4a||false;
};
dojo.lang.extend(dojo.AdapterRegistry,{register:function(name,_a4c,wrap,_a4e,_a4f){
var type=(_a4f)?"unshift":"push";
this.pairs[type]([name,_a4c,wrap,_a4e]);
},match:function(){
for(var i=0;i<this.pairs.length;i++){
var pair=this.pairs[i];
if(pair[1].apply(this,arguments)){
if((pair[3])||(this.returnWrappers)){
return pair[2];
}else{
return pair[2].apply(this,arguments);
}
}
}
throw new Error("No match found");
},unregister:function(name){
for(var i=0;i<this.pairs.length;i++){
var pair=this.pairs[i];
if(pair[0]==name){
this.pairs.splice(i,1);
return true;
}
}
return false;
}});
dojo.provide("dojo.lang.repr");
dojo.lang.reprRegistry=new dojo.AdapterRegistry();
dojo.lang.registerRepr=function(name,_a57,wrap,_a59){
dojo.lang.reprRegistry.register(name,_a57,wrap,_a59);
};
dojo.lang.repr=function(obj){
if(typeof (obj)=="undefined"){
return "undefined";
}else{
if(obj===null){
return "null";
}
}
try{
if(typeof (obj["__repr__"])=="function"){
return obj["__repr__"]();
}else{
if((typeof (obj["repr"])=="function")&&(obj.repr!=arguments.callee)){
return obj["repr"]();
}
}
return dojo.lang.reprRegistry.match(obj);
}
catch(e){
if(typeof (obj.NAME)=="string"&&(obj.toString==Function.prototype.toString||obj.toString==Object.prototype.toString)){
return obj.NAME;
}
}
if(typeof (obj)=="function"){
obj=(obj+"").replace(/^\s+/,"");
var idx=obj.indexOf("{");
if(idx!=-1){
obj=obj.substr(0,idx)+"{...}";
}
}
return obj+"";
};
dojo.lang.reprArrayLike=function(arr){
try{
var na=dojo.lang.map(arr,dojo.lang.repr);
return "["+na.join(", ")+"]";
}
catch(e){
}
};
(function(){
var m=dojo.lang;
m.registerRepr("arrayLike",m.isArrayLike,m.reprArrayLike);
m.registerRepr("string",m.isString,m.reprString);
m.registerRepr("numbers",m.isNumber,m.reprNumber);
m.registerRepr("boolean",m.isBoolean,m.reprNumber);
})();
dojo.provide("dojo.lang.*");
dojo.provide("dojo.html.iframe");
dojo.html.iframeContentWindow=function(_a5f){
var win=dojo.html.getDocumentWindow(dojo.html.iframeContentDocument(_a5f))||dojo.html.iframeContentDocument(_a5f).__parent__||(_a5f.name&&document.frames[_a5f.name])||null;
return win;
};
dojo.html.iframeContentDocument=function(_a61){
var doc=_a61.contentDocument||((_a61.contentWindow)&&(_a61.contentWindow.document))||((_a61.name)&&(document.frames[_a61.name])&&(document.frames[_a61.name].document))||null;
return doc;
};
dojo.html.BackgroundIframe=function(node){
if(dojo.render.html.ie55||dojo.render.html.ie60){
var html="<iframe src='javascript:false'"+" style='position: absolute; left: 0px; top: 0px; width: 100%; height: 100%;"+"z-index: -1; filter:Alpha(Opacity=\"0\");' "+">";
this.iframe=dojo.doc().createElement(html);
this.iframe.tabIndex=-1;
if(node){
node.appendChild(this.iframe);
this.domNode=node;
}else{
dojo.body().appendChild(this.iframe);
this.iframe.style.display="none";
}
}
};
dojo.lang.extend(dojo.html.BackgroundIframe,{iframe:null,onResized:function(){
if(this.iframe&&this.domNode&&this.domNode.parentNode){
var _a65=dojo.html.getMarginBox(this.domNode);
if(_a65.width==0||_a65.height==0){
dojo.lang.setTimeout(this,this.onResized,100);
return;
}
this.iframe.style.width=_a65.width+"px";
this.iframe.style.height=_a65.height+"px";
}
},size:function(node){
if(!this.iframe){
return;
}
var _a67=dojo.html.toCoordinateObject(node,true,dojo.html.boxSizing.BORDER_BOX);
with(this.iframe.style){
width=_a67.width+"px";
height=_a67.height+"px";
left=_a67.left+"px";
top=_a67.top+"px";
}
},setZIndex:function(node){
if(!this.iframe){
return;
}
if(dojo.dom.isNode(node)){
this.iframe.style.zIndex=dojo.html.getStyle(node,"z-index")-1;
}else{
if(!isNaN(node)){
this.iframe.style.zIndex=node;
}
}
},show:function(){
if(this.iframe){
this.iframe.style.display="block";
}
},hide:function(){
if(this.iframe){
this.iframe.style.display="none";
}
},remove:function(){
if(this.iframe){
dojo.html.removeNode(this.iframe,true);
delete this.iframe;
this.iframe=null;
}
}});
dojo.provide("dojo.widget.PopupContainer");
dojo.declare("dojo.widget.PopupContainerBase",null,function(){
this.queueOnAnimationFinish=[];
},{isContainer:true,templateString:"<div dojoAttachPoint=\"containerNode\" style=\"display:none;position:absolute;\" class=\"dojoPopupContainer\" ></div>",isShowingNow:false,currentSubpopup:null,beginZIndex:1000,parentPopup:null,parent:null,popupIndex:0,aroundBox:dojo.html.boxSizing.BORDER_BOX,openedForWindow:null,processKey:function(evt){
return false;
},applyPopupBasicStyle:function(){
with(this.domNode.style){
display="none";
position="absolute";
}
},aboutToShow:function(){
},open:function(x,y,_a6c,_a6d,_a6e,_a6f){
if(this.isShowingNow){
return;
}
if(this.animationInProgress){
this.queueOnAnimationFinish.push(this.open,arguments);
return;
}
this.aboutToShow();
var _a70=false,node,_a72;
if(typeof x=="object"){
node=x;
_a72=_a6d;
_a6d=_a6c;
_a6c=y;
_a70=true;
}
this.parent=_a6c;
dojo.body().appendChild(this.domNode);
_a6d=_a6d||_a6c["domNode"]||[];
var _a73=null;
this.isTopLevel=true;
while(_a6c){
if(_a6c!==this&&(_a6c.setOpenedSubpopup!=undefined&&_a6c.applyPopupBasicStyle!=undefined)){
_a73=_a6c;
this.isTopLevel=false;
_a73.setOpenedSubpopup(this);
break;
}
_a6c=_a6c.parent;
}
this.parentPopup=_a73;
this.popupIndex=_a73?_a73.popupIndex+1:1;
if(this.isTopLevel){
var _a74=dojo.html.isNode(_a6d)?_a6d:null;
dojo.widget.PopupManager.opened(this,_a74);
}
if(this.isTopLevel&&!dojo.withGlobal(this.openedForWindow||dojo.global(),dojo.html.selection.isCollapsed)){
this._bookmark=dojo.withGlobal(this.openedForWindow||dojo.global(),dojo.html.selection.getBookmark);
}else{
this._bookmark=null;
}
if(_a6d instanceof Array){
_a6d={left:_a6d[0],top:_a6d[1],width:0,height:0};
}
with(this.domNode.style){
display="";
zIndex=this.beginZIndex+this.popupIndex;
}
if(_a70){
this.move(node,_a6f,_a72);
}else{
this.move(x,y,_a6f,_a6e);
}
this.domNode.style.display="none";
this.explodeSrc=_a6d;
this.show();
this.isShowingNow=true;
},move:function(x,y,_a77,_a78){
var _a79=(typeof x=="object");
if(_a79){
var _a7a=_a77;
var node=x;
_a77=y;
if(!_a7a){
_a7a={"BL":"TL","TL":"BL"};
}
dojo.html.placeOnScreenAroundElement(this.domNode,node,_a77,this.aroundBox,_a7a);
}else{
if(!_a78){
_a78="TL,TR,BL,BR";
}
dojo.html.placeOnScreen(this.domNode,x,y,_a77,true,_a78);
}
},close:function(_a7c){
if(_a7c){
this.domNode.style.display="none";
}
if(this.animationInProgress){
this.queueOnAnimationFinish.push(this.close,[]);
return;
}
this.closeSubpopup(_a7c);
this.hide();
if(this.bgIframe){
this.bgIframe.hide();
this.bgIframe.size({left:0,top:0,width:0,height:0});
}
if(this.isTopLevel){
dojo.widget.PopupManager.closed(this);
}
this.isShowingNow=false;
if(this.parent){
setTimeout(dojo.lang.hitch(this,function(){
try{
if(this.parent["focus"]){
this.parent.focus();
}else{
this.parent.domNode.focus();
}
}
catch(e){
dojo.debug("No idea how to focus to parent",e);
}
}),10);
}
if(this._bookmark&&dojo.withGlobal(this.openedForWindow||dojo.global(),dojo.html.selection.isCollapsed)){
if(this.openedForWindow){
this.openedForWindow.focus();
}
try{
dojo.withGlobal(this.openedForWindow||dojo.global(),"moveToBookmark",dojo.html.selection,[this._bookmark]);
}
catch(e){
}
}
this._bookmark=null;
},closeAll:function(_a7d){
if(this.parentPopup){
this.parentPopup.closeAll(_a7d);
}else{
this.close(_a7d);
}
},setOpenedSubpopup:function(_a7e){
this.currentSubpopup=_a7e;
},closeSubpopup:function(_a7f){
if(this.currentSubpopup==null){
return;
}
this.currentSubpopup.close(_a7f);
this.currentSubpopup=null;
},onShow:function(){
dojo.widget.PopupContainer.superclass.onShow.apply(this,arguments);
this.openedSize={w:this.domNode.style.width,h:this.domNode.style.height};
if(dojo.render.html.ie){
if(!this.bgIframe){
this.bgIframe=new dojo.html.BackgroundIframe();
this.bgIframe.setZIndex(this.domNode);
}
this.bgIframe.size(this.domNode);
this.bgIframe.show();
}
this.processQueue();
},processQueue:function(){
if(!this.queueOnAnimationFinish.length){
return;
}
var func=this.queueOnAnimationFinish.shift();
var args=this.queueOnAnimationFinish.shift();
func.apply(this,args);
},onHide:function(){
dojo.widget.HtmlWidget.prototype.onHide.call(this);
if(this.openedSize){
with(this.domNode.style){
width=this.openedSize.w;
height=this.openedSize.h;
}
}
this.processQueue();
}});
dojo.widget.defineWidget("dojo.widget.PopupContainer",[dojo.widget.HtmlWidget,dojo.widget.PopupContainerBase],{});
dojo.widget.PopupManager=new function(){
this.currentMenu=null;
this.currentButton=null;
this.currentFocusMenu=null;
this.focusNode=null;
this.registeredWindows=[];
this.registerWin=function(win){
if(!win.__PopupManagerRegistered){
dojo.event.connect(win.document,"onmousedown",this,"onClick");
dojo.event.connect(win,"onscroll",this,"onClick");
dojo.event.connect(win.document,"onkey",this,"onKey");
win.__PopupManagerRegistered=true;
this.registeredWindows.push(win);
}
};
this.registerAllWindows=function(_a83){
if(!_a83){
_a83=dojo.html.getDocumentWindow(window.top&&window.top.document||window.document);
}
this.registerWin(_a83);
for(var i=0;i<_a83.frames.length;i++){
try{
var win=dojo.html.getDocumentWindow(_a83.frames[i].document);
if(win){
this.registerAllWindows(win);
}
}
catch(e){
}
}
};
this.unRegisterWin=function(win){
if(win.__PopupManagerRegistered){
dojo.event.disconnect(win.document,"onmousedown",this,"onClick");
dojo.event.disconnect(win,"onscroll",this,"onClick");
dojo.event.disconnect(win.document,"onkey",this,"onKey");
win.__PopupManagerRegistered=false;
}
};
this.unRegisterAllWindows=function(){
for(var i=0;i<this.registeredWindows.length;++i){
this.unRegisterWin(this.registeredWindows[i]);
}
this.registeredWindows=[];
};
dojo.addOnLoad(this,"registerAllWindows");
dojo.addOnUnload(this,"unRegisterAllWindows");
this.closed=function(menu){
if(this.currentMenu==menu){
this.currentMenu=null;
this.currentButton=null;
this.currentFocusMenu=null;
}
};
this.opened=function(menu,_a8a){
if(menu==this.currentMenu){
return;
}
if(this.currentMenu){
this.currentMenu.close();
}
this.currentMenu=menu;
this.currentFocusMenu=menu;
this.currentButton=_a8a;
};
this.setFocusedMenu=function(menu){
this.currentFocusMenu=menu;
};
this.onKey=function(e){
if(!e.key){
return;
}
if(!this.currentMenu||!this.currentMenu.isShowingNow){
return;
}
var m=this.currentFocusMenu;
while(m){
if(m.processKey(e)){
e.preventDefault();
e.stopPropagation();
break;
}
m=m.parentPopup;
}
},this.onClick=function(e){
if(!this.currentMenu){
return;
}
var _a8f=dojo.html.getScroll().offset;
var m=this.currentMenu;
while(m){
if(dojo.html.overElement(m.domNode,e)||dojo.html.isDescendantOf(e.target,m.domNode)){
return;
}
m=m.currentSubpopup;
}
if(this.currentButton&&dojo.html.overElement(this.currentButton,e)){
return;
}
this.currentMenu.close();
};
};
dojo.provide("dojo.widget.ContentPane");
dojo.widget.defineWidget("dojo.widget.ContentPane",dojo.widget.HtmlWidget,function(){
this._styleNodes=[];
this._onLoadStack=[];
this._onUnloadStack=[];
this._callOnUnload=false;
this._ioBindObj;
this.scriptScope;
this.bindArgs={};
},{isContainer:true,adjustPaths:true,href:"",extractContent:true,parseContent:true,cacheContent:true,preload:false,refreshOnShow:false,handler:"",executeScripts:false,scriptSeparation:true,loadingMessage:"Loading...",isLoaded:false,postCreate:function(args,frag,_a93){
if(this.handler!==""){
this.setHandler(this.handler);
}
if(this.isShowing()||this.preload){
this.loadContents();
}
},show:function(){
if(this.refreshOnShow){
this.refresh();
}else{
this.loadContents();
}
dojo.widget.ContentPane.superclass.show.call(this);
},refresh:function(){
this.isLoaded=false;
this.loadContents();
},loadContents:function(){
if(this.isLoaded){
return;
}
if(dojo.lang.isFunction(this.handler)){
this._runHandler();
}else{
if(this.href!=""){
this._downloadExternalContent(this.href,this.cacheContent&&!this.refreshOnShow);
}
}
},setUrl:function(url){
this.href=url;
this.isLoaded=false;
if(this.preload||this.isShowing()){
this.loadContents();
}
},abort:function(){
var bind=this._ioBindObj;
if(!bind||!bind.abort){
return;
}
bind.abort();
delete this._ioBindObj;
},_downloadExternalContent:function(url,_a97){
this.abort();
this._handleDefaults(this.loadingMessage,"onDownloadStart");
var self=this;
this._ioBindObj=dojo.io.bind(this._cacheSetting({url:url,mimetype:"text/html",handler:function(type,data,xhr){
delete self._ioBindObj;
if(type=="load"){
self.onDownloadEnd.call(self,url,data);
}else{
var e={responseText:xhr.responseText,status:xhr.status,statusText:xhr.statusText,responseHeaders:xhr.getAllResponseHeaders(),text:"Error loading '"+url+"' ("+xhr.status+" "+xhr.statusText+")"};
self._handleDefaults.call(self,e,"onDownloadError");
self.onLoad();
}
}},_a97));
},_cacheSetting:function(_a9d,_a9e){
for(var x in this.bindArgs){
if(dojo.lang.isUndefined(_a9d[x])){
_a9d[x]=this.bindArgs[x];
}
}
if(dojo.lang.isUndefined(_a9d.useCache)){
_a9d.useCache=_a9e;
}
if(dojo.lang.isUndefined(_a9d.preventCache)){
_a9d.preventCache=!_a9e;
}
if(dojo.lang.isUndefined(_a9d.mimetype)){
_a9d.mimetype="text/html";
}
return _a9d;
},onLoad:function(e){
this._runStack("_onLoadStack");
this.isLoaded=true;
},onUnLoad:function(e){
dojo.deprecated(this.widgetType+".onUnLoad, use .onUnload (lowercased load)",0.5);
},onUnload:function(e){
this._runStack("_onUnloadStack");
delete this.scriptScope;
if(this.onUnLoad!==dojo.widget.ContentPane.prototype.onUnLoad){
this.onUnLoad.apply(this,arguments);
}
},_runStack:function(_aa3){
var st=this[_aa3];
var err="";
var _aa6=this.scriptScope||window;
for(var i=0;i<st.length;i++){
try{
st[i].call(_aa6);
}
catch(e){
err+="\n"+st[i]+" failed: "+e.description;
}
}
this[_aa3]=[];
if(err.length){
var name=(_aa3=="_onLoadStack")?"addOnLoad":"addOnUnLoad";
this._handleDefaults(name+" failure\n "+err,"onExecError","debug");
}
},addOnLoad:function(obj,func){
this._pushOnStack(this._onLoadStack,obj,func);
},addOnUnload:function(obj,func){
this._pushOnStack(this._onUnloadStack,obj,func);
},addOnUnLoad:function(){
dojo.deprecated(this.widgetType+".addOnUnLoad, use addOnUnload instead. (lowercased Load)",0.5);
this.addOnUnload.apply(this,arguments);
},_pushOnStack:function(_aad,obj,func){
if(typeof func=="undefined"){
_aad.push(obj);
}else{
_aad.push(function(){
obj[func]();
});
}
},destroy:function(){
this.onUnload();
dojo.widget.ContentPane.superclass.destroy.call(this);
},onExecError:function(e){
},onContentError:function(e){
},onDownloadError:function(e){
},onDownloadStart:function(e){
},onDownloadEnd:function(url,data){
data=this.splitAndFixPaths(data,url);
this.setContent(data);
},_handleDefaults:function(e,_ab7,_ab8){
if(!_ab7){
_ab7="onContentError";
}
if(dojo.lang.isString(e)){
e={text:e};
}
if(!e.text){
e.text=e.toString();
}
e.toString=function(){
return this.text;
};
if(typeof e.returnValue!="boolean"){
e.returnValue=true;
}
if(typeof e.preventDefault!="function"){
e.preventDefault=function(){
this.returnValue=false;
};
}
this[_ab7](e);
if(e.returnValue){
switch(_ab8){
case true:
case "alert":
alert(e.toString());
break;
case "debug":
dojo.debug(e.toString());
break;
default:
if(this._callOnUnload){
this.onUnload();
}
this._callOnUnload=false;
if(arguments.callee._loopStop){
dojo.debug(e.toString());
}else{
arguments.callee._loopStop=true;
this._setContent(e.toString());
}
}
}
arguments.callee._loopStop=false;
},splitAndFixPaths:function(s,url){
var _abb=[],_abc=[],tmp=[];
var _abe=[],_abf=[],attr=[],_ac1=[];
var str="",path="",fix="",_ac5="",tag="",_ac7="";
if(!url){
url="./";
}
if(s){
var _ac8=/<title[^>]*>([\s\S]*?)<\/title>/i;
while(_abe=_ac8.exec(s)){
_abb.push(_abe[1]);
s=s.substring(0,_abe.index)+s.substr(_abe.index+_abe[0].length);
}
if(this.adjustPaths){
var _ac9=/<[a-z][a-z0-9]*[^>]*\s(?:(?:src|href|style)=[^>])+[^>]*>/i;
var _aca=/\s(src|href|style)=(['"]?)([\w()\[\]\/.,\\'"-:;#=&?\s@]+?)\2/i;
var _acb=/^(?:[#]|(?:(?:https?|ftps?|file|javascript|mailto|news):))/;
while(tag=_ac9.exec(s)){
str+=s.substring(0,tag.index);
s=s.substring((tag.index+tag[0].length),s.length);
tag=tag[0];
_ac5="";
while(attr=_aca.exec(tag)){
path="";
_ac7=attr[3];
switch(attr[1].toLowerCase()){
case "src":
case "href":
if(_acb.exec(_ac7)){
path=_ac7;
}else{
path=(new dojo.uri.Uri(url,_ac7).toString());
}
break;
case "style":
path=dojo.html.fixPathsInCssText(_ac7,url);
break;
default:
path=_ac7;
}
fix=" "+attr[1]+"="+attr[2]+path+attr[2];
_ac5+=tag.substring(0,attr.index)+fix;
tag=tag.substring((attr.index+attr[0].length),tag.length);
}
str+=_ac5+tag;
}
s=str+s;
}
_ac8=/(?:<(style)[^>]*>([\s\S]*?)<\/style>|<link ([^>]*rel=['"]?stylesheet['"]?[^>]*)>)/i;
while(_abe=_ac8.exec(s)){
if(_abe[1]&&_abe[1].toLowerCase()=="style"){
_ac1.push(dojo.html.fixPathsInCssText(_abe[2],url));
}else{
if(attr=_abe[3].match(/href=(['"]?)([^'">]*)\1/i)){
_ac1.push({path:attr[2]});
}
}
s=s.substring(0,_abe.index)+s.substr(_abe.index+_abe[0].length);
}
var _ac8=/<script([^>]*)>([\s\S]*?)<\/script>/i;
var _acc=/src=(['"]?)([^"']*)\1/i;
var _acd=/.*(\bdojo\b\.js(?:\.uncompressed\.js)?)$/;
var _ace=/(?:var )?\bdjConfig\b(?:[\s]*=[\s]*\{[^}]+\}|\.[\w]*[\s]*=[\s]*[^;\n]*)?;?|dojo\.hostenv\.writeIncludes\(\s*\);?/g;
var _acf=/dojo\.(?:(?:require(?:After)?(?:If)?)|(?:widget\.(?:manager\.)?registerWidgetPackage)|(?:(?:hostenv\.)?setModulePrefix|registerModulePath)|defineNamespace)\((['"]).*?\1\)\s*;?/;
while(_abe=_ac8.exec(s)){
if(this.executeScripts&&_abe[1]){
if(attr=_acc.exec(_abe[1])){
if(_acd.exec(attr[2])){
dojo.debug("Security note! inhibit:"+attr[2]+" from  being loaded again.");
}else{
_abc.push({path:attr[2]});
}
}
}
if(_abe[2]){
var sc=_abe[2].replace(_ace,"");
if(!sc){
continue;
}
while(tmp=_acf.exec(sc)){
_abf.push(tmp[0]);
sc=sc.substring(0,tmp.index)+sc.substr(tmp.index+tmp[0].length);
}
if(this.executeScripts){
_abc.push(sc);
}
}
s=s.substr(0,_abe.index)+s.substr(_abe.index+_abe[0].length);
}
if(this.extractContent){
_abe=s.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);
if(_abe){
s=_abe[1];
}
}
if(this.executeScripts&&this.scriptSeparation){
var _ac8=/(<[a-zA-Z][a-zA-Z0-9]*\s[^>]*?\S=)((['"])[^>]*scriptScope[^>]*>)/;
var _ad1=/([\s'";:\(])scriptScope(.*)/;
str="";
while(tag=_ac8.exec(s)){
tmp=((tag[3]=="'")?"\"":"'");
fix="";
str+=s.substring(0,tag.index)+tag[1];
while(attr=_ad1.exec(tag[2])){
tag[2]=tag[2].substring(0,attr.index)+attr[1]+"dojo.widget.byId("+tmp+this.widgetId+tmp+").scriptScope"+attr[2];
}
str+=tag[2];
s=s.substr(tag.index+tag[0].length);
}
s=str+s;
}
}
return {"xml":s,"styles":_ac1,"titles":_abb,"requires":_abf,"scripts":_abc,"url":url};
},_setContent:function(cont){
this.destroyChildren();
for(var i=0;i<this._styleNodes.length;i++){
if(this._styleNodes[i]&&this._styleNodes[i].parentNode){
this._styleNodes[i].parentNode.removeChild(this._styleNodes[i]);
}
}
this._styleNodes=[];
try{
var node=this.containerNode||this.domNode;
while(node.firstChild){
dojo.html.destroyNode(node.firstChild);
}
if(typeof cont!="string"){
node.appendChild(cont);
}else{
node.innerHTML=cont;
}
}
catch(e){
e.text="Couldn't load content:"+e.description;
this._handleDefaults(e,"onContentError");
}
},setContent:function(data){
this.abort();
if(this._callOnUnload){
this.onUnload();
}
this._callOnUnload=true;
if(!data||dojo.html.isNode(data)){
this._setContent(data);
this.onResized();
this.onLoad();
}else{
if(typeof data.xml!="string"){
this.href="";
data=this.splitAndFixPaths(data);
}
this._setContent(data.xml);
for(var i=0;i<data.styles.length;i++){
if(data.styles[i].path){
this._styleNodes.push(dojo.html.insertCssFile(data.styles[i].path,dojo.doc(),false,true));
}else{
this._styleNodes.push(dojo.html.insertCssText(data.styles[i]));
}
}
if(this.parseContent){
for(var i=0;i<data.requires.length;i++){
try{
eval(data.requires[i]);
}
catch(e){
e.text="ContentPane: error in package loading calls, "+(e.description||e);
this._handleDefaults(e,"onContentError","debug");
}
}
}
var _ad7=this;
function asyncParse(){
if(_ad7.executeScripts){
_ad7._executeScripts(data.scripts);
}
if(_ad7.parseContent){
var node=_ad7.containerNode||_ad7.domNode;
var _ad9=new dojo.xml.Parse();
var frag=_ad9.parseElement(node,null,true);
dojo.widget.getParser().createSubComponents(frag,_ad7);
}
_ad7.onResized();
_ad7.onLoad();
}
if(dojo.hostenv.isXDomain&&data.requires.length){
dojo.addOnLoad(asyncParse);
}else{
asyncParse();
}
}
},setHandler:function(_adb){
var fcn=dojo.lang.isFunction(_adb)?_adb:window[_adb];
if(!dojo.lang.isFunction(fcn)){
this._handleDefaults("Unable to set handler, '"+_adb+"' not a function.","onExecError",true);
return;
}
this.handler=function(){
return fcn.apply(this,arguments);
};
},_runHandler:function(){
var ret=true;
if(dojo.lang.isFunction(this.handler)){
this.handler(this,this.domNode);
ret=false;
}
this.onLoad();
return ret;
},_executeScripts:function(_ade){
var self=this;
var tmp="",code="";
for(var i=0;i<_ade.length;i++){
if(_ade[i].path){
dojo.io.bind(this._cacheSetting({"url":_ade[i].path,"load":function(type,_ae4){
dojo.lang.hitch(self,tmp=";"+_ae4);
},"error":function(type,_ae6){
_ae6.text=type+" downloading remote script";
self._handleDefaults.call(self,_ae6,"onExecError","debug");
},"mimetype":"text/plain","sync":true},this.cacheContent));
code+=tmp;
}else{
code+=_ade[i];
}
}
try{
if(this.scriptSeparation){
delete this.scriptScope;
this.scriptScope=new (new Function("_container_",code+"; return this;"))(self);
}else{
var djg=dojo.global();
if(djg.execScript){
djg.execScript(code);
}else{
var djd=dojo.doc();
var sc=djd.createElement("script");
sc.appendChild(djd.createTextNode(code));
(this.containerNode||this.domNode).appendChild(sc);
}
}
}
catch(e){
e.text="Error running scripts from content:\n"+e.description;
this._handleDefaults(e,"onExecError","debug");
}
}});
dojo.provide("dojo.widget.Editor2Toolbar");
dojo.lang.declare("dojo.widget.HandlerManager",null,function(){
this._registeredHandlers=[];
},{registerHandler:function(obj,func){
if(arguments.length==2){
this._registeredHandlers.push(function(){
return obj[func].apply(obj,arguments);
});
}else{
this._registeredHandlers.push(obj);
}
},removeHandler:function(func){
for(var i=0;i<this._registeredHandlers.length;i++){
if(func===this._registeredHandlers[i]){
delete this._registeredHandlers[i];
return;
}
}
dojo.debug("HandlerManager handler "+func+" is not registered, can not remove.");
},destroy:function(){
for(var i=0;i<this._registeredHandlers.length;i++){
delete this._registeredHandlers[i];
}
}});
dojo.widget.Editor2ToolbarItemManager=new dojo.widget.HandlerManager;
dojo.lang.mixin(dojo.widget.Editor2ToolbarItemManager,{getToolbarItem:function(name){
var item;
name=name.toLowerCase();
for(var i=0;i<this._registeredHandlers.length;i++){
item=this._registeredHandlers[i](name);
if(item){
return item;
}
}
switch(name){
case "bold":
case "copy":
case "cut":
case "delete":
case "indent":
case "inserthorizontalrule":
case "insertorderedlist":
case "insertunorderedlist":
case "italic":
case "justifycenter":
case "justifyfull":
case "justifyleft":
case "justifyright":
case "outdent":
case "paste":
case "redo":
case "removeformat":
case "selectall":
case "strikethrough":
case "subscript":
case "superscript":
case "underline":
case "undo":
case "unlink":
case "createlink":
case "insertimage":
case "htmltoggle":
item=new dojo.widget.Editor2ToolbarButton(name);
break;
case "forecolor":
case "hilitecolor":
item=new dojo.widget.Editor2ToolbarColorPaletteButton(name);
break;
case "plainformatblock":
item=new dojo.widget.Editor2ToolbarFormatBlockPlainSelect("formatblock");
break;
case "formatblock":
item=new dojo.widget.Editor2ToolbarFormatBlockSelect("formatblock");
break;
case "fontsize":
item=new dojo.widget.Editor2ToolbarFontSizeSelect("fontsize");
break;
case "fontname":
item=new dojo.widget.Editor2ToolbarFontNameSelect("fontname");
break;
case "inserttable":
case "insertcell":
case "insertcol":
case "insertrow":
case "deletecells":
case "deletecols":
case "deleterows":
case "mergecells":
case "splitcell":
dojo.debug(name+" is implemented in dojo.widget.Editor2Plugin.TableOperation, please require it first.");
break;
case "inserthtml":
case "blockdirltr":
case "blockdirrtl":
case "dirltr":
case "dirrtl":
case "inlinedirltr":
case "inlinedirrtl":
dojo.debug("Not yet implemented toolbar item: "+name);
break;
default:
dojo.debug("dojo.widget.Editor2ToolbarItemManager.getToolbarItem: Unknown toolbar item: "+name);
}
return item;
}});
dojo.addOnUnload(dojo.widget.Editor2ToolbarItemManager,"destroy");
dojo.declare("dojo.widget.Editor2ToolbarButton",null,function(name){
this._name=name;
},{create:function(node,_af4,_af5){
this._domNode=node;
var cmd=_af4.parent.getCommand(this._name);
if(cmd){
this._domNode.title=cmd.getText();
}
this.disableSelection(this._domNode);
this._parentToolbar=_af4;
dojo.event.connect(this._domNode,"onclick",this,"onClick");
if(!_af5){
dojo.event.connect(this._domNode,"onmouseover",this,"onMouseOver");
dojo.event.connect(this._domNode,"onmouseout",this,"onMouseOut");
}
},disableSelection:function(_af7){
dojo.html.disableSelection(_af7);
var _af8=_af7.all||_af7.getElementsByTagName("*");
for(var x=0;x<_af8.length;x++){
dojo.html.disableSelection(_af8[x]);
}
},onMouseOver:function(){
var _afa=dojo.widget.Editor2Manager.getCurrentInstance();
if(_afa){
var _afb=_afa.getCommand(this._name);
if(_afb&&_afb.getState()!=dojo.widget.Editor2Manager.commandState.Disabled){
this.highlightToolbarItem();
}
}
},onMouseOut:function(){
this.unhighlightToolbarItem();
},destroy:function(){
this._domNode=null;
this._parentToolbar=null;
},onClick:function(e){
if(this._domNode&&!this._domNode.disabled&&this._parentToolbar.checkAvailability()){
e.preventDefault();
e.stopPropagation();
var _afd=dojo.widget.Editor2Manager.getCurrentInstance();
if(_afd){
var _afe=_afd.getCommand(this._name);
if(_afe){
_afe.execute();
}
}
}
},refreshState:function(){
var _aff=dojo.widget.Editor2Manager.getCurrentInstance();
var em=dojo.widget.Editor2Manager;
if(_aff){
var _b01=_aff.getCommand(this._name);
if(_b01){
var _b02=_b01.getState();
if(_b02!=this._lastState){
switch(_b02){
case em.commandState.Latched:
this.latchToolbarItem();
break;
case em.commandState.Enabled:
this.enableToolbarItem();
break;
case em.commandState.Disabled:
default:
this.disableToolbarItem();
}
this._lastState=_b02;
}
}
}
return em.commandState.Enabled;
},latchToolbarItem:function(){
this._domNode.disabled=false;
this.removeToolbarItemStyle(this._domNode);
dojo.html.addClass(this._domNode,this._parentToolbar.ToolbarLatchedItemStyle);
},enableToolbarItem:function(){
this._domNode.disabled=false;
this.removeToolbarItemStyle(this._domNode);
dojo.html.addClass(this._domNode,this._parentToolbar.ToolbarEnabledItemStyle);
},disableToolbarItem:function(){
this._domNode.disabled=true;
this.removeToolbarItemStyle(this._domNode);
dojo.html.addClass(this._domNode,this._parentToolbar.ToolbarDisabledItemStyle);
},highlightToolbarItem:function(){
dojo.html.addClass(this._domNode,this._parentToolbar.ToolbarHighlightedItemStyle);
},unhighlightToolbarItem:function(){
dojo.html.removeClass(this._domNode,this._parentToolbar.ToolbarHighlightedItemStyle);
},removeToolbarItemStyle:function(){
dojo.html.removeClass(this._domNode,this._parentToolbar.ToolbarEnabledItemStyle);
dojo.html.removeClass(this._domNode,this._parentToolbar.ToolbarLatchedItemStyle);
dojo.html.removeClass(this._domNode,this._parentToolbar.ToolbarDisabledItemStyle);
this.unhighlightToolbarItem();
}});
dojo.declare("dojo.widget.Editor2ToolbarDropDownButton",dojo.widget.Editor2ToolbarButton,{onClick:function(){
if(this._domNode&&!this._domNode.disabled&&this._parentToolbar.checkAvailability()){
if(!this._dropdown){
this._dropdown=dojo.widget.createWidget("PopupContainer",{});
this._domNode.appendChild(this._dropdown.domNode);
}
if(this._dropdown.isShowingNow){
this._dropdown.close();
}else{
this.onDropDownShown();
this._dropdown.open(this._domNode,null,this._domNode);
}
}
},destroy:function(){
this.onDropDownDestroy();
if(this._dropdown){
this._dropdown.destroy();
}
dojo.widget.Editor2ToolbarDropDownButton.superclass.destroy.call(this);
},onDropDownShown:function(){
},onDropDownDestroy:function(){
}});
dojo.declare("dojo.widget.Editor2ToolbarColorPaletteButton",dojo.widget.Editor2ToolbarDropDownButton,{onDropDownShown:function(){
if(!this._colorpalette){
this._colorpalette=dojo.widget.createWidget("ColorPalette",{});
this._dropdown.addChild(this._colorpalette);
this.disableSelection(this._dropdown.domNode);
this.disableSelection(this._colorpalette.domNode);
dojo.event.connect(this._colorpalette,"onColorSelect",this,"setColor");
dojo.event.connect(this._dropdown,"open",this,"latchToolbarItem");
dojo.event.connect(this._dropdown,"close",this,"enableToolbarItem");
}
},setColor:function(_b03){
this._dropdown.close();
var _b04=dojo.widget.Editor2Manager.getCurrentInstance();
if(_b04){
var _b05=_b04.getCommand(this._name);
if(_b05){
_b05.execute(_b03);
}
}
}});
dojo.declare("dojo.widget.Editor2ToolbarFormatBlockPlainSelect",dojo.widget.Editor2ToolbarButton,{create:function(node,_b07){
this._domNode=node;
this._parentToolbar=_b07;
this._domNode=node;
this.disableSelection(this._domNode);
dojo.event.connect(this._domNode,"onchange",this,"onChange");
},destroy:function(){
this._domNode=null;
},onChange:function(){
if(this._parentToolbar.checkAvailability()){
var sv=this._domNode.value.toLowerCase();
var _b09=dojo.widget.Editor2Manager.getCurrentInstance();
if(_b09){
var _b0a=_b09.getCommand(this._name);
if(_b0a){
_b0a.execute(sv);
}
}
}
},refreshState:function(){
if(this._domNode){
dojo.widget.Editor2ToolbarFormatBlockPlainSelect.superclass.refreshState.call(this);
var _b0b=dojo.widget.Editor2Manager.getCurrentInstance();
if(_b0b){
var _b0c=_b0b.getCommand(this._name);
if(_b0c){
var _b0d=_b0c.getValue();
if(!_b0d){
_b0d="";
}
dojo.lang.forEach(this._domNode.options,function(item){
if(item.value.toLowerCase()==_b0d.toLowerCase()){
item.selected=true;
}
});
}
}
}
}});
dojo.declare("dojo.widget.Editor2ToolbarComboItem",dojo.widget.Editor2ToolbarDropDownButton,{href:null,create:function(node,_b10){
dojo.widget.Editor2ToolbarComboItem.superclass.create.apply(this,arguments);
if(!this._contentPane){
dojo.require("dojo.widget.ContentPane");
this._contentPane=dojo.widget.createWidget("ContentPane",{preload:"true"});
this._contentPane.addOnLoad(this,"setup");
this._contentPane.setUrl(this.href);
}
},onMouseOver:function(e){
if(this._lastState!=dojo.widget.Editor2Manager.commandState.Disabled){
dojo.html.addClass(e.currentTarget,this._parentToolbar.ToolbarHighlightedSelectStyle);
}
},onMouseOut:function(e){
dojo.html.removeClass(e.currentTarget,this._parentToolbar.ToolbarHighlightedSelectStyle);
},onDropDownShown:function(){
if(!this._dropdown.__addedContentPage){
this._dropdown.addChild(this._contentPane);
this._dropdown.__addedContentPage=true;
}
},setup:function(){
},onChange:function(e){
if(this._parentToolbar.checkAvailability()){
var name=e.currentTarget.getAttribute("dropDownItemName");
var _b15=dojo.widget.Editor2Manager.getCurrentInstance();
if(_b15){
var _b16=_b15.getCommand(this._name);
if(_b16){
_b16.execute(name);
}
}
}
this._dropdown.close();
},onMouseOverItem:function(e){
dojo.html.addClass(e.currentTarget,this._parentToolbar.ToolbarHighlightedSelectItemStyle);
},onMouseOutItem:function(e){
dojo.html.removeClass(e.currentTarget,this._parentToolbar.ToolbarHighlightedSelectItemStyle);
}});
dojo.declare("dojo.widget.Editor2ToolbarFormatBlockSelect",dojo.widget.Editor2ToolbarComboItem,{href:dojo.uri.dojoUri("src/widget/templates/Editor2/EditorToolbar_FormatBlock.html"),setup:function(){
dojo.widget.Editor2ToolbarFormatBlockSelect.superclass.setup.call(this);
var _b19=this._contentPane.domNode.all||this._contentPane.domNode.getElementsByTagName("*");
this._blockNames={};
this._blockDisplayNames={};
for(var x=0;x<_b19.length;x++){
var node=_b19[x];
dojo.html.disableSelection(node);
var name=node.getAttribute("dropDownItemName");
if(name){
this._blockNames[name]=node;
var _b1d=node.getElementsByTagName(name);
this._blockDisplayNames[name]=_b1d[_b1d.length-1].innerHTML;
}
}
for(var name in this._blockNames){
dojo.event.connect(this._blockNames[name],"onclick",this,"onChange");
dojo.event.connect(this._blockNames[name],"onmouseover",this,"onMouseOverItem");
dojo.event.connect(this._blockNames[name],"onmouseout",this,"onMouseOutItem");
}
},onDropDownDestroy:function(){
if(this._blockNames){
for(var name in this._blockNames){
delete this._blockNames[name];
delete this._blockDisplayNames[name];
}
}
},refreshState:function(){
dojo.widget.Editor2ToolbarFormatBlockSelect.superclass.refreshState.call(this);
if(this._lastState!=dojo.widget.Editor2Manager.commandState.Disabled){
var _b1f=dojo.widget.Editor2Manager.getCurrentInstance();
if(_b1f){
var _b20=_b1f.getCommand(this._name);
if(_b20){
var _b21=_b20.getValue();
if(_b21==this._lastSelectedFormat&&this._blockDisplayNames){
return this._lastState;
}
this._lastSelectedFormat=_b21;
var _b22=this._domNode.getElementsByTagName("label")[0];
var _b23=false;
if(this._blockDisplayNames){
for(var name in this._blockDisplayNames){
if(name==_b21){
_b22.innerHTML=this._blockDisplayNames[name];
_b23=true;
break;
}
}
if(!_b23){
_b22.innerHTML="&nbsp;";
}
}
}
}
}
return this._lastState;
}});
dojo.declare("dojo.widget.Editor2ToolbarFontSizeSelect",dojo.widget.Editor2ToolbarComboItem,{href:dojo.uri.dojoUri("src/widget/templates/Editor2/EditorToolbar_FontSize.html"),setup:function(){
dojo.widget.Editor2ToolbarFormatBlockSelect.superclass.setup.call(this);
var _b25=this._contentPane.domNode.all||this._contentPane.domNode.getElementsByTagName("*");
this._fontsizes={};
this._fontSizeDisplayNames={};
for(var x=0;x<_b25.length;x++){
var node=_b25[x];
dojo.html.disableSelection(node);
var name=node.getAttribute("dropDownItemName");
if(name){
this._fontsizes[name]=node;
this._fontSizeDisplayNames[name]=node.getElementsByTagName("font")[0].innerHTML;
}
}
for(var name in this._fontsizes){
dojo.event.connect(this._fontsizes[name],"onclick",this,"onChange");
dojo.event.connect(this._fontsizes[name],"onmouseover",this,"onMouseOverItem");
dojo.event.connect(this._fontsizes[name],"onmouseout",this,"onMouseOutItem");
}
},onDropDownDestroy:function(){
if(this._fontsizes){
for(var name in this._fontsizes){
delete this._fontsizes[name];
delete this._fontSizeDisplayNames[name];
}
}
},refreshState:function(){
dojo.widget.Editor2ToolbarFormatBlockSelect.superclass.refreshState.call(this);
if(this._lastState!=dojo.widget.Editor2Manager.commandState.Disabled){
var _b2a=dojo.widget.Editor2Manager.getCurrentInstance();
if(_b2a){
var _b2b=_b2a.getCommand(this._name);
if(_b2b){
var size=_b2b.getValue();
if(size==this._lastSelectedSize&&this._fontSizeDisplayNames){
return this._lastState;
}
this._lastSelectedSize=size;
var _b2d=this._domNode.getElementsByTagName("label")[0];
var _b2e=false;
if(this._fontSizeDisplayNames){
for(var name in this._fontSizeDisplayNames){
if(name==size){
_b2d.innerHTML=this._fontSizeDisplayNames[name];
_b2e=true;
break;
}
}
if(!_b2e){
_b2d.innerHTML="&nbsp;";
}
}
}
}
}
return this._lastState;
}});
dojo.declare("dojo.widget.Editor2ToolbarFontNameSelect",dojo.widget.Editor2ToolbarFontSizeSelect,{href:dojo.uri.dojoUri("src/widget/templates/Editor2/EditorToolbar_FontName.html")});
dojo.widget.defineWidget("dojo.widget.Editor2Toolbar",dojo.widget.HtmlWidget,function(){
dojo.event.connect(this,"fillInTemplate",dojo.lang.hitch(this,function(){
if(dojo.render.html.ie){
this.domNode.style.zoom=1;
}
}));
},{templatePath:dojo.uri.dojoUri("src/widget/templates/EditorToolbar.html"),templateCssPath:dojo.uri.dojoUri("src/widget/templates/EditorToolbar.css"),ToolbarLatchedItemStyle:"ToolbarButtonLatched",ToolbarEnabledItemStyle:"ToolbarButtonEnabled",ToolbarDisabledItemStyle:"ToolbarButtonDisabled",ToolbarHighlightedItemStyle:"ToolbarButtonHighlighted",ToolbarHighlightedSelectStyle:"ToolbarSelectHighlighted",ToolbarHighlightedSelectItemStyle:"ToolbarSelectHighlightedItem",postCreate:function(){
var _b30=dojo.html.getElementsByClass("dojoEditorToolbarItem",this.domNode);
this.items={};
for(var x=0;x<_b30.length;x++){
var node=_b30[x];
var _b33=node.getAttribute("dojoETItemName");
if(_b33){
var item=dojo.widget.Editor2ToolbarItemManager.getToolbarItem(_b33);
if(item){
item.create(node,this);
this.items[_b33.toLowerCase()]=item;
}else{
node.style.display="none";
}
}
}
},update:function(){
for(var cmd in this.items){
this.items[cmd].refreshState();
}
},shareGroup:"",checkAvailability:function(){
if(!this.shareGroup){
this.parent.focus();
return true;
}
var _b36=dojo.widget.Editor2Manager.getCurrentInstance();
if(this.shareGroup==_b36.toolbarGroup){
return true;
}
return false;
},destroy:function(){
for(var it in this.items){
this.items[it].destroy();
delete this.items[it];
}
dojo.widget.Editor2Toolbar.superclass.destroy.call(this);
}});
dojo.provide("dojo.lfx.shadow");
dojo.lfx.shadow=function(node){
this.shadowPng=dojo.uri.dojoUri("src/html/images/shadow");
this.shadowThickness=8;
this.shadowOffset=15;
this.init(node);
};
dojo.extend(dojo.lfx.shadow,{init:function(node){
this.node=node;
this.pieces={};
var x1=-1*this.shadowThickness;
var y0=this.shadowOffset;
var y1=this.shadowOffset+this.shadowThickness;
this._makePiece("tl","top",y0,"left",x1);
this._makePiece("l","top",y1,"left",x1,"scale");
this._makePiece("tr","top",y0,"left",0);
this._makePiece("r","top",y1,"left",0,"scale");
this._makePiece("bl","top",0,"left",x1);
this._makePiece("b","top",0,"left",0,"crop");
this._makePiece("br","top",0,"left",0);
},_makePiece:function(name,_b3e,_b3f,_b40,_b41,_b42){
var img;
var url=this.shadowPng+name.toUpperCase()+".png";
if(dojo.render.html.ie55||dojo.render.html.ie60){
img=dojo.doc().createElement("div");
img.style.filter="progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"+url+"'"+(_b42?", sizingMethod='"+_b42+"'":"")+")";
}else{
img=dojo.doc().createElement("img");
img.src=url;
}
img.style.position="absolute";
img.style[_b3e]=_b3f+"px";
img.style[_b40]=_b41+"px";
img.style.width=this.shadowThickness+"px";
img.style.height=this.shadowThickness+"px";
this.pieces[name]=img;
this.node.appendChild(img);
},size:function(_b45,_b46){
var _b47=_b46-(this.shadowOffset+this.shadowThickness+1);
if(_b47<0){
_b47=0;
}
if(_b46<1){
_b46=1;
}
if(_b45<1){
_b45=1;
}
with(this.pieces){
l.style.height=_b47+"px";
r.style.height=_b47+"px";
b.style.width=(_b45-1)+"px";
bl.style.top=(_b46-1)+"px";
b.style.top=(_b46-1)+"px";
br.style.top=(_b46-1)+"px";
tr.style.left=(_b45-1)+"px";
r.style.left=(_b45-1)+"px";
br.style.left=(_b45-1)+"px";
}
}});
dojo.provide("dojo.widget.html.layout");
dojo.widget.html.layout=function(_b48,_b49,_b4a){
dojo.html.addClass(_b48,"dojoLayoutContainer");
_b49=dojo.lang.filter(_b49,function(_b4b,idx){
_b4b.idx=idx;
return dojo.lang.inArray(["top","bottom","left","right","client","flood"],_b4b.layoutAlign);
});
if(_b4a&&_b4a!="none"){
var rank=function(_b4e){
switch(_b4e.layoutAlign){
case "flood":
return 1;
case "left":
case "right":
return (_b4a=="left-right")?2:3;
case "top":
case "bottom":
return (_b4a=="left-right")?3:2;
default:
return 4;
}
};
_b49.sort(function(a,b){
return (rank(a)-rank(b))||(a.idx-b.idx);
});
}
var f={top:dojo.html.getPixelValue(_b48,"padding-top",true),left:dojo.html.getPixelValue(_b48,"padding-left",true)};
dojo.lang.mixin(f,dojo.html.getContentBox(_b48));
dojo.lang.forEach(_b49,function(_b52){
var elm=_b52.domNode;
var pos=_b52.layoutAlign;
with(elm.style){
left=f.left+"px";
top=f.top+"px";
bottom="auto";
right="auto";
}
dojo.html.addClass(elm,"dojoAlign"+dojo.string.capitalize(pos));
if((pos=="top")||(pos=="bottom")){
dojo.html.setMarginBox(elm,{width:f.width});
var h=dojo.html.getMarginBox(elm).height;
f.height-=h;
if(pos=="top"){
f.top+=h;
}else{
elm.style.top=f.top+f.height+"px";
}
if(_b52.onResized){
_b52.onResized();
}
}else{
if(pos=="left"||pos=="right"){
var w=dojo.html.getMarginBox(elm).width;
if(_b52.resizeTo){
_b52.resizeTo(w,f.height);
}else{
dojo.html.setMarginBox(elm,{width:w,height:f.height});
}
f.width-=w;
if(pos=="left"){
f.left+=w;
}else{
elm.style.left=f.left+f.width+"px";
}
}else{
if(pos=="flood"||pos=="client"){
if(_b52.resizeTo){
_b52.resizeTo(f.width,f.height);
}else{
dojo.html.setMarginBox(elm,{width:f.width,height:f.height});
}
}
}
}
});
};
dojo.html.insertCssText(".dojoLayoutContainer{ position: relative; display: block; overflow: hidden; }\n"+"body .dojoAlignTop, body .dojoAlignBottom, body .dojoAlignLeft, body .dojoAlignRight { position: absolute; overflow: hidden; }\n"+"body .dojoAlignClient { position: absolute }\n"+".dojoAlignClient { overflow: auto; }\n");
dojo.provide("dojo.dnd.DragAndDrop");
dojo.declare("dojo.dnd.DragSource",null,{type:"",onDragEnd:function(evt){
},onDragStart:function(evt){
},onSelected:function(evt){
},unregister:function(){
dojo.dnd.dragManager.unregisterDragSource(this);
},reregister:function(){
dojo.dnd.dragManager.registerDragSource(this);
}});
dojo.declare("dojo.dnd.DragObject",null,{type:"",register:function(){
var dm=dojo.dnd.dragManager;
if(dm["registerDragObject"]){
dm.registerDragObject(this);
}
},onDragStart:function(evt){
},onDragMove:function(evt){
},onDragOver:function(evt){
},onDragOut:function(evt){
},onDragEnd:function(evt){
},onDragLeave:dojo.lang.forward("onDragOut"),onDragEnter:dojo.lang.forward("onDragOver"),ondragout:dojo.lang.forward("onDragOut"),ondragover:dojo.lang.forward("onDragOver")});
dojo.declare("dojo.dnd.DropTarget",null,{acceptsType:function(type){
if(!dojo.lang.inArray(this.acceptedTypes,"*")){
if(!dojo.lang.inArray(this.acceptedTypes,type)){
return false;
}
}
return true;
},accepts:function(_b61){
if(!dojo.lang.inArray(this.acceptedTypes,"*")){
for(var i=0;i<_b61.length;i++){
if(!dojo.lang.inArray(this.acceptedTypes,_b61[i].type)){
return false;
}
}
}
return true;
},unregister:function(){
dojo.dnd.dragManager.unregisterDropTarget(this);
},onDragOver:function(evt){
},onDragOut:function(evt){
},onDragMove:function(evt){
},onDropStart:function(evt){
},onDrop:function(evt){
},onDropEnd:function(){
}},function(){
this.acceptedTypes=[];
});
dojo.dnd.DragEvent=function(){
this.dragSource=null;
this.dragObject=null;
this.target=null;
this.eventStatus="success";
};
dojo.declare("dojo.dnd.DragManager",null,{selectedSources:[],dragObjects:[],dragSources:[],registerDragSource:function(_b68){
},dropTargets:[],registerDropTarget:function(_b69){
},lastDragTarget:null,currentDragTarget:null,onKeyDown:function(){
},onMouseOut:function(){
},onMouseMove:function(){
},onMouseUp:function(){
}});
dojo.provide("dojo.dnd.HtmlDragManager");
dojo.declare("dojo.dnd.HtmlDragManager",dojo.dnd.DragManager,{disabled:false,nestedTargets:false,mouseDownTimer:null,dsCounter:0,dsPrefix:"dojoDragSource",dropTargetDimensions:[],currentDropTarget:null,previousDropTarget:null,_dragTriggered:false,selectedSources:[],dragObjects:[],dragSources:[],currentX:null,currentY:null,lastX:null,lastY:null,mouseDownX:null,mouseDownY:null,threshold:7,dropAcceptable:false,cancelEvent:function(e){
e.stopPropagation();
e.preventDefault();
},registerDragSource:function(ds){
if(ds["domNode"]){
var dp=this.dsPrefix;
var _b6d=dp+"Idx_"+(this.dsCounter++);
ds.dragSourceId=_b6d;
this.dragSources[_b6d]=ds;
ds.domNode.setAttribute(dp,_b6d);
if(dojo.render.html.ie){
dojo.event.browser.addListener(ds.domNode,"ondragstart",this.cancelEvent);
}
}
},unregisterDragSource:function(ds){
if(ds["domNode"]){
var dp=this.dsPrefix;
var _b70=ds.dragSourceId;
delete ds.dragSourceId;
delete this.dragSources[_b70];
ds.domNode.setAttribute(dp,null);
if(dojo.render.html.ie){
dojo.event.browser.removeListener(ds.domNode,"ondragstart",this.cancelEvent);
}
}
},registerDropTarget:function(dt){
this.dropTargets.push(dt);
},unregisterDropTarget:function(dt){
var _b73=dojo.lang.find(this.dropTargets,dt,true);
if(_b73>=0){
this.dropTargets.splice(_b73,1);
}
},getDragSource:function(e){
var tn=e.target;
if(tn===dojo.body()){
return;
}
var ta=dojo.html.getAttribute(tn,this.dsPrefix);
while((!ta)&&(tn)){
tn=tn.parentNode;
if((!tn)||(tn===dojo.body())){
return;
}
ta=dojo.html.getAttribute(tn,this.dsPrefix);
}
return this.dragSources[ta];
},onKeyDown:function(e){
},onMouseDown:function(e){
if(this.disabled){
return;
}
if(dojo.render.html.ie){
if(e.button!=1){
return;
}
}else{
if(e.which!=1){
return;
}
}
var _b79=e.target.nodeType==dojo.html.TEXT_NODE?e.target.parentNode:e.target;
if(dojo.html.isTag(_b79,"button","textarea","input","select","option")){
return;
}
var ds=this.getDragSource(e);
if(!ds){
return;
}
if(!dojo.lang.inArray(this.selectedSources,ds)){
this.selectedSources.push(ds);
ds.onSelected();
}
this.mouseDownX=e.pageX;
this.mouseDownY=e.pageY;
e.preventDefault();
dojo.event.connect(document,"onmousemove",this,"onMouseMove");
},onMouseUp:function(e,_b7c){
if(this.selectedSources.length==0){
return;
}
this.mouseDownX=null;
this.mouseDownY=null;
this._dragTriggered=false;
e.dragSource=this.dragSource;
if((!e.shiftKey)&&(!e.ctrlKey)){
if(this.currentDropTarget){
this.currentDropTarget.onDropStart();
}
dojo.lang.forEach(this.dragObjects,function(_b7d){
var ret=null;
if(!_b7d){
return;
}
if(this.currentDropTarget){
e.dragObject=_b7d;
var ce=this.currentDropTarget.domNode.childNodes;
if(ce.length>0){
e.dropTarget=ce[0];
while(e.dropTarget==_b7d.domNode){
e.dropTarget=e.dropTarget.nextSibling;
}
}else{
e.dropTarget=this.currentDropTarget.domNode;
}
if(this.dropAcceptable){
ret=this.currentDropTarget.onDrop(e);
}else{
this.currentDropTarget.onDragOut(e);
}
}
e.dragStatus=this.dropAcceptable&&ret?"dropSuccess":"dropFailure";
dojo.lang.delayThese([function(){
try{
_b7d.dragSource.onDragEnd(e);
}
catch(err){
var _b80={};
for(var i in e){
if(i=="type"){
_b80.type="mouseup";
continue;
}
_b80[i]=e[i];
}
_b7d.dragSource.onDragEnd(_b80);
}
},function(){
_b7d.onDragEnd(e);
}]);
},this);
this.selectedSources=[];
this.dragObjects=[];
this.dragSource=null;
if(this.currentDropTarget){
this.currentDropTarget.onDropEnd();
}
}else{
}
dojo.event.disconnect(document,"onmousemove",this,"onMouseMove");
this.currentDropTarget=null;
},onScroll:function(){
for(var i=0;i<this.dragObjects.length;i++){
if(this.dragObjects[i].updateDragOffset){
this.dragObjects[i].updateDragOffset();
}
}
if(this.dragObjects.length){
this.cacheTargetLocations();
}
},_dragStartDistance:function(x,y){
if((!this.mouseDownX)||(!this.mouseDownX)){
return;
}
var dx=Math.abs(x-this.mouseDownX);
var dx2=dx*dx;
var dy=Math.abs(y-this.mouseDownY);
var dy2=dy*dy;
return parseInt(Math.sqrt(dx2+dy2),10);
},cacheTargetLocations:function(){
dojo.profile.start("cacheTargetLocations");
this.dropTargetDimensions=[];
dojo.lang.forEach(this.dropTargets,function(_b89){
var tn=_b89.domNode;
if(!tn||!_b89.accepts([this.dragSource])){
return;
}
var abs=dojo.html.getAbsolutePosition(tn,true);
var bb=dojo.html.getBorderBox(tn);
this.dropTargetDimensions.push([[abs.x,abs.y],[abs.x+bb.width,abs.y+bb.height],_b89]);
},this);
dojo.profile.end("cacheTargetLocations");
},onMouseMove:function(e){
if((dojo.render.html.ie)&&(e.button!=1)){
this.currentDropTarget=null;
this.onMouseUp(e,true);
return;
}
if((this.selectedSources.length)&&(!this.dragObjects.length)){
var dx;
var dy;
if(!this._dragTriggered){
this._dragTriggered=(this._dragStartDistance(e.pageX,e.pageY)>this.threshold);
if(!this._dragTriggered){
return;
}
dx=e.pageX-this.mouseDownX;
dy=e.pageY-this.mouseDownY;
}
this.dragSource=this.selectedSources[0];
dojo.lang.forEach(this.selectedSources,function(_b90){
if(!_b90){
return;
}
var tdo=_b90.onDragStart(e);
if(tdo){
tdo.onDragStart(e);
tdo.dragOffset.y+=dy;
tdo.dragOffset.x+=dx;
tdo.dragSource=_b90;
this.dragObjects.push(tdo);
}
},this);
this.previousDropTarget=null;
this.cacheTargetLocations();
}
dojo.lang.forEach(this.dragObjects,function(_b92){
if(_b92){
_b92.onDragMove(e);
}
});
if(this.currentDropTarget){
var c=dojo.html.toCoordinateObject(this.currentDropTarget.domNode,true);
var dtp=[[c.x,c.y],[c.x+c.width,c.y+c.height]];
}
if((!this.nestedTargets)&&(dtp)&&(this.isInsideBox(e,dtp))){
if(this.dropAcceptable){
this.currentDropTarget.onDragMove(e,this.dragObjects);
}
}else{
var _b95=this.findBestTarget(e);
if(_b95.target===null){
if(this.currentDropTarget){
this.currentDropTarget.onDragOut(e);
this.previousDropTarget=this.currentDropTarget;
this.currentDropTarget=null;
}
this.dropAcceptable=false;
return;
}
if(this.currentDropTarget!==_b95.target){
if(this.currentDropTarget){
this.previousDropTarget=this.currentDropTarget;
this.currentDropTarget.onDragOut(e);
}
this.currentDropTarget=_b95.target;
e.dragObjects=this.dragObjects;
this.dropAcceptable=this.currentDropTarget.onDragOver(e);
}else{
if(this.dropAcceptable){
this.currentDropTarget.onDragMove(e,this.dragObjects);
}
}
}
},findBestTarget:function(e){
var _b97=this;
var _b98=new Object();
_b98.target=null;
_b98.points=null;
dojo.lang.every(this.dropTargetDimensions,function(_b99){
if(!_b97.isInsideBox(e,_b99)){
return true;
}
_b98.target=_b99[2];
_b98.points=_b99;
return Boolean(_b97.nestedTargets);
});
return _b98;
},isInsideBox:function(e,_b9b){
if((e.pageX>_b9b[0][0])&&(e.pageX<_b9b[1][0])&&(e.pageY>_b9b[0][1])&&(e.pageY<_b9b[1][1])){
return true;
}
return false;
},onMouseOver:function(e){
},onMouseOut:function(e){
}});
dojo.dnd.dragManager=new dojo.dnd.HtmlDragManager();
(function(){
var d=document;
var dm=dojo.dnd.dragManager;
dojo.event.connect(d,"onkeydown",dm,"onKeyDown");
dojo.event.connect(d,"onmouseover",dm,"onMouseOver");
dojo.event.connect(d,"onmouseout",dm,"onMouseOut");
dojo.event.connect(d,"onmousedown",dm,"onMouseDown");
dojo.event.connect(d,"onmouseup",dm,"onMouseUp");
dojo.event.connect(window,"onscroll",dm,"onScroll");
})();
dojo.provide("dojo.dnd.HtmlDragAndDrop");
dojo.declare("dojo.dnd.HtmlDragSource",dojo.dnd.DragSource,{dragClass:"",onDragStart:function(){
var _ba0=new dojo.dnd.HtmlDragObject(this.dragObject,this.type);
if(this.dragClass){
_ba0.dragClass=this.dragClass;
}
if(this.constrainToContainer){
_ba0.constrainTo(this.constrainingContainer||this.domNode.parentNode);
}
return _ba0;
},setDragHandle:function(node){
node=dojo.byId(node);
dojo.dnd.dragManager.unregisterDragSource(this);
this.domNode=node;
dojo.dnd.dragManager.registerDragSource(this);
},setDragTarget:function(node){
this.dragObject=node;
},constrainTo:function(_ba3){
this.constrainToContainer=true;
if(_ba3){
this.constrainingContainer=_ba3;
}
},onSelected:function(){
for(var i=0;i<this.dragObjects.length;i++){
dojo.dnd.dragManager.selectedSources.push(new dojo.dnd.HtmlDragSource(this.dragObjects[i]));
}
},addDragObjects:function(el){
for(var i=0;i<arguments.length;i++){
this.dragObjects.push(dojo.byId(arguments[i]));
}
}},function(node,type){
node=dojo.byId(node);
this.dragObjects=[];
this.constrainToContainer=false;
if(node){
this.domNode=node;
this.dragObject=node;
this.type=(type)||(this.domNode.nodeName.toLowerCase());
dojo.dnd.DragSource.prototype.reregister.call(this);
}
});
dojo.declare("dojo.dnd.HtmlDragObject",dojo.dnd.DragObject,{dragClass:"",opacity:0.5,createIframe:true,disableX:false,disableY:false,createDragNode:function(){
var node=this.domNode.cloneNode(true);
if(this.dragClass){
dojo.html.addClass(node,this.dragClass);
}
if(this.opacity<1){
dojo.html.setOpacity(node,this.opacity);
}
var ltn=node.tagName.toLowerCase();
var isTr=(ltn=="tr");
if((isTr)||(ltn=="tbody")){
var doc=this.domNode.ownerDocument;
var _bad=doc.createElement("table");
if(isTr){
var _bae=doc.createElement("tbody");
_bad.appendChild(_bae);
_bae.appendChild(node);
}else{
_bad.appendChild(node);
}
var _baf=((isTr)?this.domNode:this.domNode.firstChild);
var _bb0=((isTr)?node:node.firstChild);
var _bb1=_baf.childNodes;
var _bb2=_bb0.childNodes;
for(var i=0;i<_bb1.length;i++){
if((_bb2[i])&&(_bb2[i].style)){
_bb2[i].style.width=dojo.html.getContentBox(_bb1[i]).width+"px";
}
}
node=_bad;
}
if((dojo.render.html.ie55||dojo.render.html.ie60)&&this.createIframe){
with(node.style){
top="0px";
left="0px";
}
var _bb4=document.createElement("div");
_bb4.appendChild(node);
this.bgIframe=new dojo.html.BackgroundIframe(_bb4);
_bb4.appendChild(this.bgIframe.iframe);
node=_bb4;
}
node.style.zIndex=999;
return node;
},onDragStart:function(e){
dojo.html.clearSelection();
this.scrollOffset=dojo.html.getScroll().offset;
this.dragStartPosition=dojo.html.getAbsolutePosition(this.domNode,true);
this.dragOffset={y:this.dragStartPosition.y-e.pageY,x:this.dragStartPosition.x-e.pageX};
this.dragClone=this.createDragNode();
this.containingBlockPosition=this.domNode.offsetParent?dojo.html.getAbsolutePosition(this.domNode.offsetParent,true):{x:0,y:0};
if(this.constrainToContainer){
this.constraints=this.getConstraints();
}
with(this.dragClone.style){
position="absolute";
top=this.dragOffset.y+e.pageY+"px";
left=this.dragOffset.x+e.pageX+"px";
}
dojo.body().appendChild(this.dragClone);
dojo.event.topic.publish("dragStart",{source:this});
},getConstraints:function(){
if(this.constrainingContainer.nodeName.toLowerCase()=="body"){
var _bb6=dojo.html.getViewport();
var _bb7=_bb6.width;
var _bb8=_bb6.height;
var _bb9=dojo.html.getScroll().offset;
var x=_bb9.x;
var y=_bb9.y;
}else{
var _bbc=dojo.html.getContentBox(this.constrainingContainer);
_bb7=_bbc.width;
_bb8=_bbc.height;
x=this.containingBlockPosition.x+dojo.html.getPixelValue(this.constrainingContainer,"padding-left",true)+dojo.html.getBorderExtent(this.constrainingContainer,"left");
y=this.containingBlockPosition.y+dojo.html.getPixelValue(this.constrainingContainer,"padding-top",true)+dojo.html.getBorderExtent(this.constrainingContainer,"top");
}
var mb=dojo.html.getMarginBox(this.domNode);
return {minX:x,minY:y,maxX:x+_bb7-mb.width,maxY:y+_bb8-mb.height};
},updateDragOffset:function(){
var _bbe=dojo.html.getScroll().offset;
if(_bbe.y!=this.scrollOffset.y){
var diff=_bbe.y-this.scrollOffset.y;
this.dragOffset.y+=diff;
this.scrollOffset.y=_bbe.y;
}
if(_bbe.x!=this.scrollOffset.x){
var diff=_bbe.x-this.scrollOffset.x;
this.dragOffset.x+=diff;
this.scrollOffset.x=_bbe.x;
}
},onDragMove:function(e){
this.updateDragOffset();
var x=this.dragOffset.x+e.pageX;
var y=this.dragOffset.y+e.pageY;
if(this.constrainToContainer){
if(x<this.constraints.minX){
x=this.constraints.minX;
}
if(y<this.constraints.minY){
y=this.constraints.minY;
}
if(x>this.constraints.maxX){
x=this.constraints.maxX;
}
if(y>this.constraints.maxY){
y=this.constraints.maxY;
}
}
this.setAbsolutePosition(x,y);
dojo.event.topic.publish("dragMove",{source:this});
},setAbsolutePosition:function(x,y){
if(!this.disableY){
this.dragClone.style.top=y+"px";
}
if(!this.disableX){
this.dragClone.style.left=x+"px";
}
},onDragEnd:function(e){
switch(e.dragStatus){
case "dropSuccess":
dojo.html.removeNode(this.dragClone);
this.dragClone=null;
break;
case "dropFailure":
var _bc6=dojo.html.getAbsolutePosition(this.dragClone,true);
var _bc7={left:this.dragStartPosition.x+1,top:this.dragStartPosition.y+1};
var anim=dojo.lfx.slideTo(this.dragClone,_bc7,300);
var _bc9=this;
dojo.event.connect(anim,"onEnd",function(e){
dojo.html.removeNode(_bc9.dragClone);
_bc9.dragClone=null;
});
anim.play();
break;
}
dojo.event.topic.publish("dragEnd",{source:this});
},constrainTo:function(_bcb){
this.constrainToContainer=true;
if(_bcb){
this.constrainingContainer=_bcb;
}else{
this.constrainingContainer=this.domNode.parentNode;
}
}},function(node,type){
this.domNode=dojo.byId(node);
this.type=type;
this.constrainToContainer=false;
this.dragSource=null;
dojo.dnd.DragObject.prototype.register.call(this);
});
dojo.declare("dojo.dnd.HtmlDropTarget",dojo.dnd.DropTarget,{vertical:false,onDragOver:function(e){
if(!this.accepts(e.dragObjects)){
return false;
}
this.childBoxes=[];
for(var i=0,_bd0;i<this.domNode.childNodes.length;i++){
_bd0=this.domNode.childNodes[i];
if(_bd0.nodeType!=dojo.html.ELEMENT_NODE){
continue;
}
var pos=dojo.html.getAbsolutePosition(_bd0,true);
var _bd2=dojo.html.getBorderBox(_bd0);
this.childBoxes.push({top:pos.y,bottom:pos.y+_bd2.height,left:pos.x,right:pos.x+_bd2.width,height:_bd2.height,width:_bd2.width,node:_bd0});
}
return true;
},_getNodeUnderMouse:function(e){
for(var i=0,_bd5;i<this.childBoxes.length;i++){
with(this.childBoxes[i]){
if(e.pageX>=left&&e.pageX<=right&&e.pageY>=top&&e.pageY<=bottom){
return i;
}
}
}
return -1;
},createDropIndicator:function(){
this.dropIndicator=document.createElement("div");
with(this.dropIndicator.style){
position="absolute";
zIndex=999;
if(this.vertical){
borderLeftWidth="1px";
borderLeftColor="black";
borderLeftStyle="solid";
height=dojo.html.getBorderBox(this.domNode).height+"px";
top=dojo.html.getAbsolutePosition(this.domNode,true).y+"px";
}else{
borderTopWidth="1px";
borderTopColor="black";
borderTopStyle="solid";
width=dojo.html.getBorderBox(this.domNode).width+"px";
left=dojo.html.getAbsolutePosition(this.domNode,true).x+"px";
}
}
},onDragMove:function(e,_bd7){
var i=this._getNodeUnderMouse(e);
if(!this.dropIndicator){
this.createDropIndicator();
}
var _bd9=this.vertical?dojo.html.gravity.WEST:dojo.html.gravity.NORTH;
var hide=false;
if(i<0){
if(this.childBoxes.length){
var _bdb=(dojo.html.gravity(this.childBoxes[0].node,e)&_bd9);
if(_bdb){
hide=true;
}
}else{
var _bdb=true;
}
}else{
var _bdc=this.childBoxes[i];
var _bdb=(dojo.html.gravity(_bdc.node,e)&_bd9);
if(_bdc.node===_bd7[0].dragSource.domNode){
hide=true;
}else{
var _bdd=_bdb?(i>0?this.childBoxes[i-1]:_bdc):(i<this.childBoxes.length-1?this.childBoxes[i+1]:_bdc);
if(_bdd.node===_bd7[0].dragSource.domNode){
hide=true;
}
}
}
if(hide){
this.dropIndicator.style.display="none";
return;
}else{
this.dropIndicator.style.display="";
}
this.placeIndicator(e,_bd7,i,_bdb);
if(!dojo.html.hasParent(this.dropIndicator)){
dojo.body().appendChild(this.dropIndicator);
}
},placeIndicator:function(e,_bdf,_be0,_be1){
var _be2=this.vertical?"left":"top";
var _be3;
if(_be0<0){
if(this.childBoxes.length){
_be3=_be1?this.childBoxes[0]:this.childBoxes[this.childBoxes.length-1];
}else{
this.dropIndicator.style[_be2]=dojo.html.getAbsolutePosition(this.domNode,true)[this.vertical?"x":"y"]+"px";
}
}else{
_be3=this.childBoxes[_be0];
}
if(_be3){
this.dropIndicator.style[_be2]=(_be1?_be3[_be2]:_be3[this.vertical?"right":"bottom"])+"px";
if(this.vertical){
this.dropIndicator.style.height=_be3.height+"px";
this.dropIndicator.style.top=_be3.top+"px";
}else{
this.dropIndicator.style.width=_be3.width+"px";
this.dropIndicator.style.left=_be3.left+"px";
}
}
},onDragOut:function(e){
if(this.dropIndicator){
dojo.html.removeNode(this.dropIndicator);
delete this.dropIndicator;
}
},onDrop:function(e){
this.onDragOut(e);
var i=this._getNodeUnderMouse(e);
var _be7=this.vertical?dojo.html.gravity.WEST:dojo.html.gravity.NORTH;
if(i<0){
if(this.childBoxes.length){
if(dojo.html.gravity(this.childBoxes[0].node,e)&_be7){
return this.insert(e,this.childBoxes[0].node,"before");
}else{
return this.insert(e,this.childBoxes[this.childBoxes.length-1].node,"after");
}
}
return this.insert(e,this.domNode,"append");
}
var _be8=this.childBoxes[i];
if(dojo.html.gravity(_be8.node,e)&_be7){
return this.insert(e,_be8.node,"before");
}else{
return this.insert(e,_be8.node,"after");
}
},insert:function(e,_bea,_beb){
var node=e.dragObject.domNode;
if(_beb=="before"){
return dojo.html.insertBefore(node,_bea);
}else{
if(_beb=="after"){
return dojo.html.insertAfter(node,_bea);
}else{
if(_beb=="append"){
_bea.appendChild(node);
return true;
}
}
}
return false;
}},function(node,_bee){
if(arguments.length==0){
return;
}
this.domNode=dojo.byId(node);
dojo.dnd.DropTarget.call(this);
if(_bee&&dojo.lang.isString(_bee)){
_bee=[_bee];
}
this.acceptedTypes=_bee||[];
dojo.dnd.dragManager.registerDropTarget(this);
});
dojo.provide("dojo.dnd.*");
dojo.provide("dojo.dnd.HtmlDragMove");
dojo.declare("dojo.dnd.HtmlDragMoveSource",dojo.dnd.HtmlDragSource,{onDragStart:function(){
var _bef=new dojo.dnd.HtmlDragMoveObject(this.dragObject,this.type);
if(this.constrainToContainer){
_bef.constrainTo(this.constrainingContainer);
}
return _bef;
},onSelected:function(){
for(var i=0;i<this.dragObjects.length;i++){
dojo.dnd.dragManager.selectedSources.push(new dojo.dnd.HtmlDragMoveSource(this.dragObjects[i]));
}
}});
dojo.declare("dojo.dnd.HtmlDragMoveObject",dojo.dnd.HtmlDragObject,{onDragStart:function(e){
dojo.html.clearSelection();
this.dragClone=this.domNode;
if(dojo.html.getComputedStyle(this.domNode,"position")!="absolute"){
this.domNode.style.position="relative";
}
var left=parseInt(dojo.html.getComputedStyle(this.domNode,"left"));
var top=parseInt(dojo.html.getComputedStyle(this.domNode,"top"));
this.dragStartPosition={x:isNaN(left)?0:left,y:isNaN(top)?0:top};
this.scrollOffset=dojo.html.getScroll().offset;
this.dragOffset={y:this.dragStartPosition.y-e.pageY,x:this.dragStartPosition.x-e.pageX};
this.containingBlockPosition={x:0,y:0};
if(this.constrainToContainer){
this.constraints=this.getConstraints();
}
dojo.event.connect(this.domNode,"onclick",this,"_squelchOnClick");
},onDragEnd:function(e){
},setAbsolutePosition:function(x,y){
if(!this.disableY){
this.domNode.style.top=y+"px";
}
if(!this.disableX){
this.domNode.style.left=x+"px";
}
},_squelchOnClick:function(e){
dojo.event.browser.stopEvent(e);
dojo.event.disconnect(this.domNode,"onclick",this,"_squelchOnClick");
}});
dojo.provide("dojo.widget.Dialog");
dojo.declare("dojo.widget.ModalDialogBase",null,{isContainer:true,focusElement:"",bgColor:"black",bgOpacity:0.4,followScroll:true,closeOnBackgroundClick:false,trapTabs:function(e){
if(e.target==this.tabStartOuter){
if(this._fromTrap){
this.tabStart.focus();
this._fromTrap=false;
}else{
this._fromTrap=true;
this.tabEnd.focus();
}
}else{
if(e.target==this.tabStart){
if(this._fromTrap){
this._fromTrap=false;
}else{
this._fromTrap=true;
this.tabEnd.focus();
}
}else{
if(e.target==this.tabEndOuter){
if(this._fromTrap){
this.tabEnd.focus();
this._fromTrap=false;
}else{
this._fromTrap=true;
this.tabStart.focus();
}
}else{
if(e.target==this.tabEnd){
if(this._fromTrap){
this._fromTrap=false;
}else{
this._fromTrap=true;
this.tabStart.focus();
}
}
}
}
}
},clearTrap:function(e){
var _bfa=this;
setTimeout(function(){
_bfa._fromTrap=false;
},100);
},postCreate:function(){
with(this.domNode.style){
position="absolute";
zIndex=999;
display="none";
overflow="visible";
}
var b=dojo.body();
b.appendChild(this.domNode);
this.bg=document.createElement("div");
this.bg.className="dialogUnderlay";
with(this.bg.style){
position="absolute";
left=top="0px";
zIndex=998;
display="none";
}
b.appendChild(this.bg);
this.setBackgroundColor(this.bgColor);
this.bgIframe=new dojo.html.BackgroundIframe();
if(this.bgIframe.iframe){
with(this.bgIframe.iframe.style){
position="absolute";
left=top="0px";
zIndex=90;
display="none";
}
}
if(this.closeOnBackgroundClick){
dojo.event.kwConnect({srcObj:this.bg,srcFunc:"onclick",adviceObj:this,adviceFunc:"onBackgroundClick",once:true});
}
},uninitialize:function(){
this.bgIframe.remove();
dojo.html.removeNode(this.bg,true);
},setBackgroundColor:function(_bfc){
if(arguments.length>=3){
_bfc=new dojo.gfx.color.Color(arguments[0],arguments[1],arguments[2]);
}else{
_bfc=new dojo.gfx.color.Color(_bfc);
}
this.bg.style.backgroundColor=_bfc.toString();
return this.bgColor=_bfc;
},setBackgroundOpacity:function(op){
if(arguments.length==0){
op=this.bgOpacity;
}
dojo.html.setOpacity(this.bg,op);
try{
this.bgOpacity=dojo.html.getOpacity(this.bg);
}
catch(e){
this.bgOpacity=op;
}
return this.bgOpacity;
},_sizeBackground:function(){
if(this.bgOpacity>0){
var _bfe=dojo.html.getViewport();
var h=_bfe.height;
var w=_bfe.width;
with(this.bg.style){
width=w+"px";
height=h+"px";
}
var _c01=dojo.html.getScroll().offset;
this.bg.style.top=_c01.y+"px";
this.bg.style.left=_c01.x+"px";
var _bfe=dojo.html.getViewport();
if(_bfe.width!=w){
this.bg.style.width=_bfe.width+"px";
}
if(_bfe.height!=h){
this.bg.style.height=_bfe.height+"px";
}
}
this.bgIframe.size(this.bg);
},_showBackground:function(){
if(this.bgOpacity>0){
this.bg.style.display="block";
}
if(this.bgIframe.iframe){
this.bgIframe.iframe.style.display="block";
}
},placeModalDialog:function(){
var _c02=dojo.html.getScroll().offset;
var _c03=dojo.html.getViewport();
var mb;
if(this.isShowing()){
mb=dojo.html.getMarginBox(this.domNode);
}else{
dojo.html.setVisibility(this.domNode,false);
dojo.html.show(this.domNode);
mb=dojo.html.getMarginBox(this.domNode);
dojo.html.hide(this.domNode);
dojo.html.setVisibility(this.domNode,true);
}
var x=_c02.x+(_c03.width-mb.width)/2;
var y=_c02.y+(_c03.height-mb.height)/2;
with(this.domNode.style){
left=x+"px";
top=y+"px";
}
},_onKey:function(evt){
if(evt.key){
var node=evt.target;
while(node!=null){
if(node==this.domNode){
return;
}
node=node.parentNode;
}
if(evt.key!=evt.KEY_TAB){
dojo.event.browser.stopEvent(evt);
}else{
if(!dojo.render.html.opera){
try{
this.tabStart.focus();
}
catch(e){
}
}
}
}
},showModalDialog:function(){
if(this.followScroll&&!this._scrollConnected){
this._scrollConnected=true;
dojo.event.connect(window,"onscroll",this,"_onScroll");
}
dojo.event.connect(document.documentElement,"onkey",this,"_onKey");
this.placeModalDialog();
this.setBackgroundOpacity();
this._sizeBackground();
this._showBackground();
this._fromTrap=true;
setTimeout(dojo.lang.hitch(this,function(){
try{
this.tabStart.focus();
}
catch(e){
}
}),50);
},hideModalDialog:function(){
if(this.focusElement){
dojo.byId(this.focusElement).focus();
dojo.byId(this.focusElement).blur();
}
this.bg.style.display="none";
this.bg.style.width=this.bg.style.height="1px";
if(this.bgIframe.iframe){
this.bgIframe.iframe.style.display="none";
}
dojo.event.disconnect(document.documentElement,"onkey",this,"_onKey");
if(this._scrollConnected){
this._scrollConnected=false;
dojo.event.disconnect(window,"onscroll",this,"_onScroll");
}
},_onScroll:function(){
var _c09=dojo.html.getScroll().offset;
this.bg.style.top=_c09.y+"px";
this.bg.style.left=_c09.x+"px";
this.placeModalDialog();
},checkSize:function(){
if(this.isShowing()){
this._sizeBackground();
this.placeModalDialog();
this.onResized();
}
},onBackgroundClick:function(){
if(this.lifetime-this.timeRemaining>=this.blockDuration){
return;
}
this.hide();
}});
dojo.widget.defineWidget("dojo.widget.Dialog",[dojo.widget.ContentPane,dojo.widget.ModalDialogBase],{templatePath:dojo.uri.dojoUri("src/widget/templates/Dialog.html"),blockDuration:0,lifetime:0,closeNode:"",postMixInProperties:function(){
dojo.widget.Dialog.superclass.postMixInProperties.apply(this,arguments);
if(this.closeNode){
this.setCloseControl(this.closeNode);
}
},postCreate:function(){
dojo.widget.Dialog.superclass.postCreate.apply(this,arguments);
dojo.widget.ModalDialogBase.prototype.postCreate.apply(this,arguments);
},show:function(){
if(this.lifetime){
this.timeRemaining=this.lifetime;
if(this.timerNode){
this.timerNode.innerHTML=Math.ceil(this.timeRemaining/1000);
}
if(this.blockDuration&&this.closeNode){
if(this.lifetime>this.blockDuration){
this.closeNode.style.visibility="hidden";
}else{
this.closeNode.style.display="none";
}
}
if(this.timer){
clearInterval(this.timer);
}
this.timer=setInterval(dojo.lang.hitch(this,"_onTick"),100);
}
this.showModalDialog();
dojo.widget.Dialog.superclass.show.call(this);
},onLoad:function(){
this.placeModalDialog();
dojo.widget.Dialog.superclass.onLoad.call(this);
},fillInTemplate:function(){
},hide:function(){
this.hideModalDialog();
dojo.widget.Dialog.superclass.hide.call(this);
if(this.timer){
clearInterval(this.timer);
}
},setTimerNode:function(node){
this.timerNode=node;
},setCloseControl:function(node){
this.closeNode=dojo.byId(node);
dojo.event.connect(this.closeNode,"onclick",this,"hide");
},setShowControl:function(node){
node=dojo.byId(node);
dojo.event.connect(node,"onclick",this,"show");
},_onTick:function(){
if(this.timer){
this.timeRemaining-=100;
if(this.lifetime-this.timeRemaining>=this.blockDuration){
if(this.closeNode){
this.closeNode.style.visibility="visible";
}
}
if(!this.timeRemaining){
clearInterval(this.timer);
this.hide();
}else{
if(this.timerNode){
this.timerNode.innerHTML=Math.ceil(this.timeRemaining/1000);
}
}
}
}});
dojo.provide("dojo.widget.ResizeHandle");
dojo.widget.defineWidget("dojo.widget.ResizeHandle",dojo.widget.HtmlWidget,{targetElmId:"",templateCssPath:dojo.uri.dojoUri("src/widget/templates/ResizeHandle.css"),templateString:"<div class=\"dojoHtmlResizeHandle\"><div></div></div>",postCreate:function(){
dojo.event.connect(this.domNode,"onmousedown",this,"_beginSizing");
},_beginSizing:function(e){
if(this._isSizing){
return false;
}
this.targetWidget=dojo.widget.byId(this.targetElmId);
this.targetDomNode=this.targetWidget?this.targetWidget.domNode:dojo.byId(this.targetElmId);
if(!this.targetDomNode){
return;
}
this._isSizing=true;
this.startPoint={"x":e.clientX,"y":e.clientY};
var mb=dojo.html.getMarginBox(this.targetDomNode);
this.startSize={"w":mb.width,"h":mb.height};
dojo.event.kwConnect({srcObj:dojo.body(),srcFunc:"onmousemove",targetObj:this,targetFunc:"_changeSizing",rate:25});
dojo.event.connect(dojo.body(),"onmouseup",this,"_endSizing");
e.preventDefault();
},_changeSizing:function(e){
try{
if(!e.clientX||!e.clientY){
return;
}
}
catch(e){
return;
}
var dx=this.startPoint.x-e.clientX;
var dy=this.startPoint.y-e.clientY;
var newW=this.startSize.w-dx;
var newH=this.startSize.h-dy;
if(this.minSize){
var mb=dojo.html.getMarginBox(this.targetDomNode);
if(newW<this.minSize.w){
newW=mb.width;
}
if(newH<this.minSize.h){
newH=mb.height;
}
}
if(this.targetWidget){
this.targetWidget.resizeTo(newW,newH);
}else{
dojo.html.setMarginBox(this.targetDomNode,{width:newW,height:newH});
}
e.preventDefault();
},_endSizing:function(e){
dojo.event.disconnect(dojo.body(),"onmousemove",this,"_changeSizing");
dojo.event.disconnect(dojo.body(),"onmouseup",this,"_endSizing");
this._isSizing=false;
}});
dojo.provide("dojo.widget.FloatingPane");
dojo.declare("dojo.widget.FloatingPaneBase",null,{title:"",iconSrc:"",hasShadow:false,constrainToContainer:false,taskBarId:"",resizable:true,titleBarDisplay:true,windowState:"normal",displayCloseAction:false,displayMinimizeAction:false,displayMaximizeAction:false,_max_taskBarConnectAttempts:5,_taskBarConnectAttempts:0,templatePath:dojo.uri.dojoUri("src/widget/templates/FloatingPane.html"),templateCssPath:dojo.uri.dojoUri("src/widget/templates/FloatingPane.css"),fillInFloatingPaneTemplate:function(args,frag){
var _c18=this.getFragNodeRef(frag);
dojo.html.copyStyle(this.domNode,_c18);
dojo.body().appendChild(this.domNode);
if(!this.isShowing()){
this.windowState="minimized";
}
if(this.iconSrc==""){
dojo.html.removeNode(this.titleBarIcon);
}else{
this.titleBarIcon.src=this.iconSrc.toString();
}
if(this.titleBarDisplay){
this.titleBar.style.display="";
dojo.html.disableSelection(this.titleBar);
this.titleBarIcon.style.display=(this.iconSrc==""?"none":"");
this.minimizeAction.style.display=(this.displayMinimizeAction?"":"none");
this.maximizeAction.style.display=(this.displayMaximizeAction&&this.windowState!="maximized"?"":"none");
this.restoreAction.style.display=(this.displayMaximizeAction&&this.windowState=="maximized"?"":"none");
this.closeAction.style.display=(this.displayCloseAction?"":"none");
this.drag=new dojo.dnd.HtmlDragMoveSource(this.domNode);
if(this.constrainToContainer){
this.drag.constrainTo();
}
this.drag.setDragHandle(this.titleBar);
var self=this;
dojo.event.topic.subscribe("dragMove",function(info){
if(info.source.domNode==self.domNode){
dojo.event.topic.publish("floatingPaneMove",{source:self});
}
});
}
if(this.resizable){
this.resizeBar.style.display="";
this.resizeHandle=dojo.widget.createWidget("ResizeHandle",{targetElmId:this.widgetId,id:this.widgetId+"_resize"});
this.resizeBar.appendChild(this.resizeHandle.domNode);
}
if(this.hasShadow){
this.shadow=new dojo.lfx.shadow(this.domNode);
}
this.bgIframe=new dojo.html.BackgroundIframe(this.domNode);
if(this.taskBarId){
this._taskBarSetup();
}
dojo.body().removeChild(this.domNode);
},postCreate:function(){
if(dojo.hostenv.post_load_){
this._setInitialWindowState();
}else{
dojo.addOnLoad(this,"_setInitialWindowState");
}
},maximizeWindow:function(evt){
var mb=dojo.html.getMarginBox(this.domNode);
this.previous={width:mb.width||this.width,height:mb.height||this.height,left:this.domNode.style.left,top:this.domNode.style.top,bottom:this.domNode.style.bottom,right:this.domNode.style.right};
if(this.domNode.parentNode.style.overflow.toLowerCase()!="hidden"){
this.parentPrevious={overflow:this.domNode.parentNode.style.overflow};
dojo.debug(this.domNode.parentNode.style.overflow);
this.domNode.parentNode.style.overflow="hidden";
}
this.domNode.style.left=dojo.html.getPixelValue(this.domNode.parentNode,"padding-left",true)+"px";
this.domNode.style.top=dojo.html.getPixelValue(this.domNode.parentNode,"padding-top",true)+"px";
if((this.domNode.parentNode.nodeName.toLowerCase()=="body")){
var _c1d=dojo.html.getViewport();
var _c1e=dojo.html.getPadding(dojo.body());
this.resizeTo(_c1d.width-_c1e.width,_c1d.height-_c1e.height);
}else{
var _c1f=dojo.html.getContentBox(this.domNode.parentNode);
this.resizeTo(_c1f.width,_c1f.height);
}
this.maximizeAction.style.display="none";
this.restoreAction.style.display="";
if(this.resizeHandle){
this.resizeHandle.domNode.style.display="none";
}
this.drag.setDragHandle(null);
this.windowState="maximized";
},minimizeWindow:function(evt){
this.hide();
for(var attr in this.parentPrevious){
this.domNode.parentNode.style[attr]=this.parentPrevious[attr];
}
this.lastWindowState=this.windowState;
this.windowState="minimized";
},restoreWindow:function(evt){
if(this.windowState=="minimized"){
this.show();
if(this.lastWindowState=="maximized"){
this.domNode.parentNode.style.overflow="hidden";
this.windowState="maximized";
}else{
this.windowState="normal";
}
}else{
if(this.windowState=="maximized"){
for(var attr in this.previous){
this.domNode.style[attr]=this.previous[attr];
}
for(var attr in this.parentPrevious){
this.domNode.parentNode.style[attr]=this.parentPrevious[attr];
}
this.resizeTo(this.previous.width,this.previous.height);
this.previous=null;
this.parentPrevious=null;
this.restoreAction.style.display="none";
this.maximizeAction.style.display=this.displayMaximizeAction?"":"none";
if(this.resizeHandle){
this.resizeHandle.domNode.style.display="";
}
this.drag.setDragHandle(this.titleBar);
this.windowState="normal";
}else{
}
}
},toggleDisplay:function(){
if(this.windowState=="minimized"){
this.restoreWindow();
}else{
this.minimizeWindow();
}
},closeWindow:function(evt){
dojo.html.removeNode(this.domNode);
this.destroy();
},onMouseDown:function(evt){
this.bringToTop();
},bringToTop:function(){
var _c26=dojo.widget.manager.getWidgetsByType(this.widgetType);
var _c27=[];
for(var x=0;x<_c26.length;x++){
if(this.widgetId!=_c26[x].widgetId){
_c27.push(_c26[x]);
}
}
_c27.sort(function(a,b){
return a.domNode.style.zIndex-b.domNode.style.zIndex;
});
_c27.push(this);
var _c2b=100;
for(x=0;x<_c27.length;x++){
_c27[x].domNode.style.zIndex=_c2b+x*2;
}
},_setInitialWindowState:function(){
if(this.isShowing()){
this.width=-1;
var mb=dojo.html.getMarginBox(this.domNode);
this.resizeTo(mb.width,mb.height);
}
if(this.windowState=="maximized"){
this.maximizeWindow();
this.show();
return;
}
if(this.windowState=="normal"){
this.show();
return;
}
if(this.windowState=="minimized"){
this.hide();
return;
}
this.windowState="minimized";
},_taskBarSetup:function(){
var _c2d=dojo.widget.getWidgetById(this.taskBarId);
if(!_c2d){
if(this._taskBarConnectAttempts<this._max_taskBarConnectAttempts){
dojo.lang.setTimeout(this,this._taskBarSetup,50);
this._taskBarConnectAttempts++;
}else{
dojo.debug("Unable to connect to the taskBar");
}
return;
}
_c2d.addChild(this);
},showFloatingPane:function(){
this.bringToTop();
},onFloatingPaneShow:function(){
var mb=dojo.html.getMarginBox(this.domNode);
this.resizeTo(mb.width,mb.height);
},resizeTo:function(_c2f,_c30){
dojo.html.setMarginBox(this.domNode,{width:_c2f,height:_c30});
dojo.widget.html.layout(this.domNode,[{domNode:this.titleBar,layoutAlign:"top"},{domNode:this.resizeBar,layoutAlign:"bottom"},{domNode:this.containerNode,layoutAlign:"client"}]);
dojo.widget.html.layout(this.containerNode,this.children,"top-bottom");
this.bgIframe.onResized();
if(this.shadow){
this.shadow.size(_c2f,_c30);
}
this.onResized();
},checkSize:function(){
},destroyFloatingPane:function(){
if(this.resizeHandle){
this.resizeHandle.destroy();
this.resizeHandle=null;
}
}});
dojo.widget.defineWidget("dojo.widget.FloatingPane",[dojo.widget.ContentPane,dojo.widget.FloatingPaneBase],{fillInTemplate:function(args,frag){
this.fillInFloatingPaneTemplate(args,frag);
dojo.widget.FloatingPane.superclass.fillInTemplate.call(this,args,frag);
},postCreate:function(){
dojo.widget.FloatingPaneBase.prototype.postCreate.apply(this,arguments);
dojo.widget.FloatingPane.superclass.postCreate.apply(this,arguments);
},show:function(){
dojo.widget.FloatingPane.superclass.show.apply(this,arguments);
this.showFloatingPane();
},onShow:function(){
dojo.widget.FloatingPane.superclass.onShow.call(this);
this.onFloatingPaneShow();
},destroy:function(){
this.destroyFloatingPane();
dojo.widget.FloatingPane.superclass.destroy.apply(this,arguments);
}});
dojo.widget.defineWidget("dojo.widget.ModalFloatingPane",[dojo.widget.FloatingPane,dojo.widget.ModalDialogBase],{windowState:"minimized",displayCloseAction:true,postCreate:function(){
dojo.widget.ModalDialogBase.prototype.postCreate.call(this);
dojo.widget.ModalFloatingPane.superclass.postCreate.call(this);
},show:function(){
this.showModalDialog();
dojo.widget.ModalFloatingPane.superclass.show.apply(this,arguments);
this.bg.style.zIndex=this.domNode.style.zIndex-1;
},hide:function(){
this.hideModalDialog();
dojo.widget.ModalFloatingPane.superclass.hide.apply(this,arguments);
},closeWindow:function(){
this.hide();
dojo.widget.ModalFloatingPane.superclass.closeWindow.apply(this,arguments);
}});
dojo.provide("dojo.widget.Editor2Plugin.AlwaysShowToolbar");
dojo.event.topic.subscribe("dojo.widget.Editor2::onLoad",function(_c33){
if(_c33.toolbarAlwaysVisible){
var p=new dojo.widget.Editor2Plugin.AlwaysShowToolbar(_c33);
}
});
dojo.declare("dojo.widget.Editor2Plugin.AlwaysShowToolbar",null,function(_c35){
this.editor=_c35;
this.editor.registerLoadedPlugin(this);
this.setup();
},{_scrollSetUp:false,_fixEnabled:false,_scrollThreshold:false,_handleScroll:true,setup:function(){
var tdn=this.editor.toolbarWidget;
if(!tdn.tbBgIframe){
tdn.tbBgIframe=new dojo.html.BackgroundIframe(tdn.domNode);
tdn.tbBgIframe.onResized();
}
this.scrollInterval=setInterval(dojo.lang.hitch(this,"globalOnScrollHandler"),100);
dojo.event.connect("before",this.editor.toolbarWidget,"destroy",this,"destroy");
},globalOnScrollHandler:function(){
var isIE=dojo.render.html.ie;
if(!this._handleScroll){
return;
}
var dh=dojo.html;
var tdn=this.editor.toolbarWidget.domNode;
var db=dojo.body();
if(!this._scrollSetUp){
this._scrollSetUp=true;
var _c3b=dh.getMarginBox(this.editor.domNode).width;
this._scrollThreshold=dh.abs(tdn,true).y;
if((isIE)&&(db)&&(dh.getStyle(db,"background-image")=="none")){
with(db.style){
backgroundImage="url("+dojo.uri.dojoUri("src/widget/templates/images/blank.gif")+")";
backgroundAttachment="fixed";
}
}
}
var _c3c=(window["pageYOffset"])?window["pageYOffset"]:(document["documentElement"]||document["body"]).scrollTop;
if(_c3c>this._scrollThreshold){
if(!this._fixEnabled){
var _c3d=dojo.html.getMarginBox(tdn);
this.editor.editorObject.style.marginTop=_c3d.height+"px";
if(isIE){
tdn.style.left=dojo.html.abs(tdn,dojo.html.boxSizing.MARGIN_BOX).x;
if(tdn.previousSibling){
this._IEOriginalPos=["after",tdn.previousSibling];
}else{
if(tdn.nextSibling){
this._IEOriginalPos=["before",tdn.nextSibling];
}else{
this._IEOriginalPos=["",tdn.parentNode];
}
}
dojo.body().appendChild(tdn);
dojo.html.addClass(tdn,"IEFixedToolbar");
}else{
with(tdn.style){
position="fixed";
top="0px";
}
}
tdn.style.width=_c3d.width+"px";
tdn.style.zIndex=1000;
this._fixEnabled=true;
}
if(!dojo.render.html.safari){
var _c3e=(this.height)?parseInt(this.editor.height):this.editor._lastHeight;
if(_c3c>(this._scrollThreshold+_c3e)){
tdn.style.display="none";
}else{
tdn.style.display="";
}
}
}else{
if(this._fixEnabled){
(this.editor.object||this.editor.iframe).style.marginTop=null;
with(tdn.style){
position="";
top="";
zIndex="";
display="";
}
if(isIE){
tdn.style.left="";
dojo.html.removeClass(tdn,"IEFixedToolbar");
if(this._IEOriginalPos){
dojo.html.insertAtPosition(tdn,this._IEOriginalPos[1],this._IEOriginalPos[0]);
this._IEOriginalPos=null;
}else{
dojo.html.insertBefore(tdn,this.editor.object||this.editor.iframe);
}
}
tdn.style.width="";
this._fixEnabled=false;
}
}
},destroy:function(){
this._IEOriginalPos=null;
this._handleScroll=false;
clearInterval(this.scrollInterval);
this.editor.unregisterLoadedPlugin(this);
if(dojo.render.html.ie){
dojo.html.removeClass(this.editor.toolbarWidget.domNode,"IEFixedToolbar");
}
}});
dojo.provide("dojo.widget.Editor2");
dojo.widget.Editor2Manager=new dojo.widget.HandlerManager;
dojo.lang.mixin(dojo.widget.Editor2Manager,{_currentInstance:null,commandState:{Disabled:0,Latched:1,Enabled:2},getCurrentInstance:function(){
return this._currentInstance;
},setCurrentInstance:function(inst){
this._currentInstance=inst;
},getCommand:function(_c40,name){
var _c42;
name=name.toLowerCase();
for(var i=0;i<this._registeredHandlers.length;i++){
_c42=this._registeredHandlers[i](_c40,name);
if(_c42){
return _c42;
}
}
switch(name){
case "htmltoggle":
_c42=new dojo.widget.Editor2BrowserCommand(_c40,name);
break;
case "formatblock":
_c42=new dojo.widget.Editor2FormatBlockCommand(_c40,name);
break;
case "anchor":
_c42=new dojo.widget.Editor2Command(_c40,name);
break;
case "createlink":
_c42=new dojo.widget.Editor2DialogCommand(_c40,name,{contentFile:"dojo.widget.Editor2Plugin.CreateLinkDialog",contentClass:"Editor2CreateLinkDialog",title:"Insert/Edit Link",width:"300px",height:"200px"});
break;
case "insertimage":
_c42=new dojo.widget.Editor2DialogCommand(_c40,name,{contentFile:"dojo.widget.Editor2Plugin.InsertImageDialog",contentClass:"Editor2InsertImageDialog",title:"Insert/Edit Image",width:"400px",height:"270px"});
break;
default:
var _c44=this.getCurrentInstance();
if((_c44&&_c44.queryCommandAvailable(name))||(!_c44&&dojo.widget.Editor2.prototype.queryCommandAvailable(name))){
_c42=new dojo.widget.Editor2BrowserCommand(_c40,name);
}else{
dojo.debug("dojo.widget.Editor2Manager.getCommand: Unknown command "+name);
return;
}
}
return _c42;
},destroy:function(){
this._currentInstance=null;
dojo.widget.HandlerManager.prototype.destroy.call(this);
}});
dojo.addOnUnload(dojo.widget.Editor2Manager,"destroy");
dojo.lang.declare("dojo.widget.Editor2Command",null,function(_c45,name){
this._editor=_c45;
this._updateTime=0;
this._name=name;
},{_text:"Unknown",execute:function(para){
dojo.unimplemented("dojo.widget.Editor2Command.execute");
},getText:function(){
return this._text;
},getState:function(){
return dojo.widget.Editor2Manager.commandState.Enabled;
},destroy:function(){
}});
dojo.widget.Editor2BrowserCommandNames={"bold":"Bold","copy":"Copy","cut":"Cut","Delete":"Delete","indent":"Indent","inserthorizontalrule":"Horizental Rule","insertorderedlist":"Numbered List","insertunorderedlist":"Bullet List","italic":"Italic","justifycenter":"Align Center","justifyfull":"Justify","justifyleft":"Align Left","justifyright":"Align Right","outdent":"Outdent","paste":"Paste","redo":"Redo","removeformat":"Remove Format","selectall":"Select All","strikethrough":"Strikethrough","subscript":"Subscript","superscript":"Superscript","underline":"Underline","undo":"Undo","unlink":"Remove Link","createlink":"Create Link","insertimage":"Insert Image","htmltoggle":"HTML Source","forecolor":"Foreground Color","hilitecolor":"Background Color","plainformatblock":"Paragraph Style","formatblock":"Paragraph Style","fontsize":"Font Size","fontname":"Font Name"};
dojo.lang.declare("dojo.widget.Editor2BrowserCommand",dojo.widget.Editor2Command,function(_c48,name){
var text=dojo.widget.Editor2BrowserCommandNames[name.toLowerCase()];
if(text){
this._text=text;
}
},{execute:function(para){
this._editor.execCommand(this._name,para);
},getState:function(){
if(this._editor._lastStateTimestamp>this._updateTime||this._state==undefined){
this._updateTime=this._editor._lastStateTimestamp;
try{
if(this._editor.queryCommandEnabled(this._name)){
if(this._editor.queryCommandState(this._name)){
this._state=dojo.widget.Editor2Manager.commandState.Latched;
}else{
this._state=dojo.widget.Editor2Manager.commandState.Enabled;
}
}else{
this._state=dojo.widget.Editor2Manager.commandState.Disabled;
}
}
catch(e){
this._state=dojo.widget.Editor2Manager.commandState.Enabled;
}
}
return this._state;
},getValue:function(){
try{
return this._editor.queryCommandValue(this._name);
}
catch(e){
}
}});
dojo.lang.declare("dojo.widget.Editor2FormatBlockCommand",dojo.widget.Editor2BrowserCommand,{});
dojo.widget.defineWidget("dojo.widget.Editor2Dialog",[dojo.widget.HtmlWidget,dojo.widget.FloatingPaneBase,dojo.widget.ModalDialogBase],{templatePath:dojo.uri.dojoUri("src/widget/templates/Editor2/EditorDialog.html"),modal:true,width:"",height:"",windowState:"minimized",displayCloseAction:true,contentFile:"",contentClass:"",fillInTemplate:function(args,frag){
this.fillInFloatingPaneTemplate(args,frag);
dojo.widget.Editor2Dialog.superclass.fillInTemplate.call(this,args,frag);
},postCreate:function(){
if(this.contentFile){
dojo.require(this.contentFile);
}
if(this.modal){
dojo.widget.ModalDialogBase.prototype.postCreate.call(this);
}else{
with(this.domNode.style){
zIndex=999;
display="none";
}
}
dojo.widget.FloatingPaneBase.prototype.postCreate.apply(this,arguments);
dojo.widget.Editor2Dialog.superclass.postCreate.call(this);
if(this.width&&this.height){
with(this.domNode.style){
width=this.width;
height=this.height;
}
}
},createContent:function(){
if(!this.contentWidget&&this.contentClass){
this.contentWidget=dojo.widget.createWidget(this.contentClass);
this.addChild(this.contentWidget);
}
},show:function(){
if(!this.contentWidget){
dojo.widget.Editor2Dialog.superclass.show.apply(this,arguments);
this.createContent();
dojo.widget.Editor2Dialog.superclass.hide.call(this);
}
if(!this.contentWidget||!this.contentWidget.loadContent()){
return;
}
this.showFloatingPane();
dojo.widget.Editor2Dialog.superclass.show.apply(this,arguments);
if(this.modal){
this.showModalDialog();
}
if(this.modal){
this.bg.style.zIndex=this.domNode.style.zIndex-1;
}
},onShow:function(){
dojo.widget.Editor2Dialog.superclass.onShow.call(this);
this.onFloatingPaneShow();
},closeWindow:function(){
this.hide();
dojo.widget.Editor2Dialog.superclass.closeWindow.apply(this,arguments);
},hide:function(){
if(this.modal){
this.hideModalDialog();
}
dojo.widget.Editor2Dialog.superclass.hide.call(this);
},checkSize:function(){
if(this.isShowing()){
if(this.modal){
this._sizeBackground();
}
this.placeModalDialog();
this.onResized();
}
}});
dojo.widget.defineWidget("dojo.widget.Editor2DialogContent",dojo.widget.HtmlWidget,{widgetsInTemplate:true,loadContent:function(){
return true;
},cancel:function(){
this.parent.hide();
}});
dojo.lang.declare("dojo.widget.Editor2DialogCommand",dojo.widget.Editor2BrowserCommand,function(_c4e,name,_c50){
this.dialogParas=_c50;
},{execute:function(){
if(!this.dialog){
if(!this.dialogParas.contentFile||!this.dialogParas.contentClass){
alert("contentFile and contentClass should be set for dojo.widget.Editor2DialogCommand.dialogParas!");
return;
}
this.dialog=dojo.widget.createWidget("Editor2Dialog",this.dialogParas);
dojo.body().appendChild(this.dialog.domNode);
dojo.event.connect(this,"destroy",this.dialog,"destroy");
}
this.dialog.show();
},getText:function(){
return this.dialogParas.title||dojo.widget.Editor2DialogCommand.superclass.getText.call(this);
}});
dojo.widget.Editor2ToolbarGroups={};
dojo.widget.defineWidget("dojo.widget.Editor2",dojo.widget.RichText,function(){
this._loadedCommands={};
},{toolbarAlwaysVisible:false,toolbarWidget:null,scrollInterval:null,toolbarTemplatePath:dojo.uri.dojoUri("src/widget/templates/EditorToolbarOneline.html"),toolbarTemplateCssPath:null,toolbarPlaceHolder:"",_inSourceMode:false,_htmlEditNode:null,toolbarGroup:"",shareToolbar:false,contextMenuGroupSet:"",editorOnLoad:function(){
dojo.event.topic.publish("dojo.widget.Editor2::preLoadingToolbar",this);
if(this.toolbarAlwaysVisible){
dojo.require("dojo.widget.Editor2Plugin.AlwaysShowToolbar");
}
if(this.toolbarWidget){
this.toolbarWidget.show();
dojo.html.insertBefore(this.toolbarWidget.domNode,this.domNode.firstChild);
}else{
if(this.shareToolbar){
dojo.deprecated("Editor2:shareToolbar is deprecated in favor of toolbarGroup","0.5");
this.toolbarGroup="defaultDojoToolbarGroup";
}
if(this.toolbarGroup){
if(dojo.widget.Editor2ToolbarGroups[this.toolbarGroup]){
this.toolbarWidget=dojo.widget.Editor2ToolbarGroups[this.toolbarGroup];
}
}
if(!this.toolbarWidget){
var _c51={shareGroup:this.toolbarGroup,parent:this};
_c51.templatePath=this.toolbarTemplatePath;
if(this.toolbarTemplateCssPath){
_c51.templateCssPath=this.toolbarTemplateCssPath;
}
if(this.toolbarPlaceHolder){
this.toolbarWidget=dojo.widget.createWidget("Editor2Toolbar",_c51,dojo.byId(this.toolbarPlaceHolder),"after");
}else{
this.toolbarWidget=dojo.widget.createWidget("Editor2Toolbar",_c51,this.domNode.firstChild,"before");
}
if(this.toolbarGroup){
dojo.widget.Editor2ToolbarGroups[this.toolbarGroup]=this.toolbarWidget;
}
dojo.event.connect(this,"close",this.toolbarWidget,"hide");
this.toolbarLoaded();
}
}
dojo.event.topic.registerPublisher("Editor2.clobberFocus",this,"clobberFocus");
dojo.event.topic.subscribe("Editor2.clobberFocus",this,"setBlur");
dojo.event.topic.publish("dojo.widget.Editor2::onLoad",this);
},toolbarLoaded:function(){
},registerLoadedPlugin:function(obj){
if(!this.loadedPlugins){
this.loadedPlugins=[];
}
this.loadedPlugins.push(obj);
},unregisterLoadedPlugin:function(obj){
for(var i in this.loadedPlugins){
if(this.loadedPlugins[i]===obj){
delete this.loadedPlugins[i];
return;
}
}
dojo.debug("dojo.widget.Editor2.unregisterLoadedPlugin: unknow plugin object: "+obj);
},execCommand:function(_c55,_c56){
switch(_c55.toLowerCase()){
case "htmltoggle":
this.toggleHtmlEditing();
break;
default:
dojo.widget.Editor2.superclass.execCommand.apply(this,arguments);
}
},queryCommandEnabled:function(_c57,_c58){
switch(_c57.toLowerCase()){
case "htmltoggle":
return true;
default:
if(this._inSourceMode){
return false;
}
return dojo.widget.Editor2.superclass.queryCommandEnabled.apply(this,arguments);
}
},queryCommandState:function(_c59,_c5a){
switch(_c59.toLowerCase()){
case "htmltoggle":
return this._inSourceMode;
default:
return dojo.widget.Editor2.superclass.queryCommandState.apply(this,arguments);
}
},onClick:function(e){
dojo.widget.Editor2.superclass.onClick.call(this,e);
if(dojo.widget.PopupManager){
if(!e){
e=this.window.event;
}
dojo.widget.PopupManager.onClick(e);
}
},clobberFocus:function(){
},toggleHtmlEditing:function(){
if(this===dojo.widget.Editor2Manager.getCurrentInstance()){
if(!this._inSourceMode){
var html=this.getEditorContent();
this._inSourceMode=true;
if(!this._htmlEditNode){
this._htmlEditNode=dojo.doc().createElement("textarea");
dojo.html.insertAfter(this._htmlEditNode,this.editorObject);
}
this._htmlEditNode.style.display="";
this._htmlEditNode.style.width="100%";
this._htmlEditNode.style.height=dojo.html.getBorderBox(this.editNode).height+"px";
this._htmlEditNode.value=html;
with(this.editorObject.style){
position="absolute";
left="-2000px";
top="-2000px";
}
}else{
this._inSourceMode=false;
this._htmlEditNode.blur();
with(this.editorObject.style){
position="";
left="";
top="";
}
var html=this._htmlEditNode.value;
dojo.lang.setTimeout(this,"replaceEditorContent",1,html);
this._htmlEditNode.style.display="none";
this.focus();
}
this.onDisplayChanged(null,true);
}
},setFocus:function(){
if(dojo.widget.Editor2Manager.getCurrentInstance()===this){
return;
}
this.clobberFocus();
dojo.widget.Editor2Manager.setCurrentInstance(this);
},setBlur:function(){
},saveSelection:function(){
this._bookmark=null;
this._bookmark=dojo.withGlobal(this.window,dojo.html.selection.getBookmark);
},restoreSelection:function(){
if(this._bookmark){
this.focus();
dojo.withGlobal(this.window,"moveToBookmark",dojo.html.selection,[this._bookmark]);
this._bookmark=null;
}else{
dojo.debug("restoreSelection: no saved selection is found!");
}
},_updateToolbarLastRan:null,_updateToolbarTimer:null,_updateToolbarFrequency:500,updateToolbar:function(_c5d){
if((!this.isLoaded)||(!this.toolbarWidget)){
return;
}
var diff=new Date()-this._updateToolbarLastRan;
if((!_c5d)&&(this._updateToolbarLastRan)&&((diff<this._updateToolbarFrequency))){
clearTimeout(this._updateToolbarTimer);
var _c5f=this;
this._updateToolbarTimer=setTimeout(function(){
_c5f.updateToolbar();
},this._updateToolbarFrequency/2);
return;
}else{
this._updateToolbarLastRan=new Date();
}
if(dojo.widget.Editor2Manager.getCurrentInstance()!==this){
return;
}
this.toolbarWidget.update();
},destroy:function(_c60){
this._htmlEditNode=null;
dojo.event.disconnect(this,"close",this.toolbarWidget,"hide");
if(!_c60){
this.toolbarWidget.destroy();
}
dojo.widget.Editor2.superclass.destroy.call(this);
},_lastStateTimestamp:0,onDisplayChanged:function(e,_c62){
this._lastStateTimestamp=(new Date()).getTime();
dojo.widget.Editor2.superclass.onDisplayChanged.call(this,e);
this.updateToolbar(_c62);
},onLoad:function(){
try{
dojo.widget.Editor2.superclass.onLoad.call(this);
}
catch(e){
dojo.debug(e);
}
this.editorOnLoad();
},onFocus:function(){
dojo.widget.Editor2.superclass.onFocus.call(this);
this.setFocus();
},getEditorContent:function(){
if(this._inSourceMode){
return this._htmlEditNode.value;
}
return dojo.widget.Editor2.superclass.getEditorContent.call(this);
},replaceEditorContent:function(html){
if(this._inSourceMode){
this._htmlEditNode.value=html;
return;
}
dojo.widget.Editor2.superclass.replaceEditorContent.apply(this,arguments);
},getCommand:function(name){
if(this._loadedCommands[name]){
return this._loadedCommands[name];
}
var cmd=dojo.widget.Editor2Manager.getCommand(this,name);
this._loadedCommands[name]=cmd;
return cmd;
},shortcuts:[["bold"],["italic"],["underline"],["selectall","a"],["insertunorderedlist","\\"]],setupDefaultShortcuts:function(){
var exec=function(cmd){
return function(){
cmd.execute();
};
};
var self=this;
dojo.lang.forEach(this.shortcuts,function(item){
var cmd=self.getCommand(item[0]);
if(cmd){
self.addKeyHandler(item[1]?item[1]:item[0].charAt(0),item[2]==undefined?self.KEY_CTRL:item[2],exec(cmd));
}
});
}});

