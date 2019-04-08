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
<#assign contentURL = Static["org.apache.ofbiz.entity.util.EntityUtilProperties"].getPropertyValue("url", "content.url.prefix.standard", delegator)>
<script type="text/javascript">
$(document).ready(function(){
    var form = document.ImageShare;
    var protocol = document.location.protocol;
    var host = document.location.host;
    var contentURL = '${contentURL}';
    var imageUrl = form.imageURL.value;
    var imageThumbUrl = form.imageThumbnail.value;
    form.direct.setAttribute('readOnly','readonly');
    form.html.setAttribute('readOnly','readonly');
    form.forumCode.setAttribute('readOnly','readonly');
    form.altForumCode.setAttribute('readOnly','readonly');
    
    if (contentURL != "") {
        var pathImageUrlEcommerce = contentURL + imageUrl;
        var pathImageUrl = contentURL + imageUrl;
        var pathImageThumbnailUrl = contentURL + imageThumbUrl;
    }
    else {
        var pathImageUrlEcommerce = "http://localhost:8080" + imageUrl;
        var pathImageUrl = "http://" + host + imageUrl;
        var pathImageThumbnailUrl = "http://"+ host + imageThumbUrl;
    }
    
    if (form.contentId.value != "") {
        pathImageUrl = $('#ImageShare_direct').html(pathImageUrl).text();
        pathImageThumbnailUrl = $('#ImageShare_direct').html(pathImageThumbnailUrl).text();
        form.link.value = pathImageUrlEcommerce;
        form.direct.value = pathImageUrl;
        form.html.value = "<a target='_blank' title='Image' href='" + pathImageUrl + "'><img src='" + pathImageUrl + "' border='0'/></a>Uploaded with <a target='_blank' href='http://images.ofbiz.com'>ofbiz.com</a>";
        form.forumCode.value = "[URL=" + pathImageUrl + "/][IMG]" + pathImageUrl + "[/IMG][/URL]Uploaded with [URL=http://images.ofbiz.com]ofbiz.com[/URL]";
        form.altForumCode.value = "[URL=" + pathImageUrl + "/][IMG]" + pathImageUrl + "[/IMG][/URL]Uploaded with [URL=http://images.ofbiz.com]ofbiz.com[/URL]";
        form.forumThumbnail.value = "[URL=" + pathImageThumbnailUrl + "/][IMG]" + pathImageThumbnailUrl + "[/IMG][/URL]Uploaded with [URL=http://images.ofbiz.com]ofbiz.com[/URL]";
        form.altForumThumbnail.value = "[URL=" + pathImageThumbnailUrl + "/][IMG]" + pathImageThumbnailUrl + "[/IMG][/URL]Uploaded with [URL=http://images.ofbiz.com]ofbiz.com[/URL]";
        form.htmlThumbnail.value = "<a target='_blank' title='Image' href='" + pathImageThumbnailUrl + "'><img src='" + pathImageThumbnailUrl + "' border='0'/></a>Uploaded with <a target='_blank' href='http://images.ofbiz.com'>ofbiz.com</a>";
    }
});

</script>
