<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/imagemanagement/sizzle.min.js</@ofbizContentUrl>"></script>
<script type="text/javascript">
jQuery.noConflict();
jQuery(document).ready(function(){
    jQuery('img').attr('id',"previewImage");
    
    var size = getSizeVars();
    var maxWidth = size;
    var maxHeight = size;
    var ratio = 0;
    var width = jQuery('#previewImage').width();
    var height = jQuery('#previewImage').height();
    
    // Check if the current width is larger than the max
    if(width > maxWidth){
        ratio = maxWidth / width;
        jQuery('#previewImage').css("width", maxWidth); 
        jQuery('#previewImage').css("height", height * ratio); 
        height = height * ratio;
        width = width * ratio;
    }
    
    // Check if current height is larger than max
    if(height > maxHeight){
        ratio = maxHeight / height; 
        jQuery('#previewImage').css("height", maxHeight);
        jQuery('#previewImage').css("width", width * ratio);
        width = width * ratio;
    }
    
});

function getUrlVars(){
    var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++){
        hash = hashes[i].split('=');
        vars.push(hash[1]);
    }
    return vars;
}

function getSizeVars(){
    var vars = [], hash;
    var result = null;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('/');
    hash = hashes[6].split('-');
    var pathElement = hash[hash.length-1];
    result = pathElement.substring(0, pathElement.lastIndexOf(".")); 
    return result;
}
</script>
