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
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/common/js/plugins/imagemanagement/sizzle.min.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/common/js/plugins/imagemanagement/jquery.Jcrop.min.js</@ofbizContentUrl>"></script>
<link rel="stylesheet" href="<@ofbizContentUrl>/common/js/plugins/imagemanagement/jquery.Jcrop.css</@ofbizContentUrl>" type="text/css" />
<script type="text/javascript">
jQuery.noConflict();
jQuery(document).ready(function(){
    jQuery('td.cropbox img').Jcrop({
        onChange: showPreview,
        onSelect: showPreview
    });
});

var imgWidth = jQuery('td.cropbox img').width();
var imgHeight = jQuery('td.cropbox img').height();
var imageUrl = jQuery('#ImageCropping_imageURL').val();
var imageName = jQuery('#ImageCropping_imageName').val();
var productId = jQuery('#ImageCropping_productId').val();

if (imageName != "") {
    jQuery('#ImageCropping tr').append("<td class='label'><span>${uiLabelMap.CommonPreview}</span></td><td><div style='width:100px;height:100px;overflow:hidden;'><img src='"+imageUrl+"' id='preview' /></div></td>");
    jQuery('#ImageCropping tbody').append("<tr><td><input type='submit' value='${uiLabelMap.CommonSubmit}' name='submitButton' class='smallSubmit'/></td></tr>");
    jQuery('#ImageCropping tbody').append("<tr><td><a class='buttontext' title=' ' href='/catalog/control/ListImageManage?productId="+productId+"'>${uiLabelMap.CommonCancel}</a></td></tr>");
}

function showPreview(coords){
    jQuery('#ImageCropping_imageX').val(coords.x);
    jQuery('#ImageCropping_imageY').val(coords.y);
    jQuery('#ImageCropping_imageW').val(coords.w);
    jQuery('#ImageCropping_imageH').val(coords.h);
                
    if (parseInt(coords.w) > 0){
        var rx = 100 / coords.w;
        var ry = 100 / coords.h;
        
        jQuery('#preview').css({
            width: Math.round(rx * imgWidth) + 'px',
            height: Math.round(ry * imgHeight) + 'px',
            marginLeft: '-' + Math.round(rx * coords.x) + 'px',
            marginTop: '-' + Math.round(ry * coords.y) + 'px'
        });
    }
}
</script>
