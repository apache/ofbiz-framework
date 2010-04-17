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

<script type="text/javascript">

    dojo.require("dojo.widget.*");
    dojo.require("dojo.event.*");
    dojo.require("dojo.io.*");

    var treeSelected = false;

    dojo.addOnLoad(function() {
            dojo.event.topic.subscribe("showDataResources",
             function(message) {
                treeSelected = true;
                var ctx = new Array();
                ctx['dataCategoryId'] = message.node.widgetId;
                callOfbiz('<@ofbizUrl>listDataResources</@ofbizUrl>', ctx);
             }
        );
        var cmsdata = dojo.byId("cmsdata");
    });


    function callOfbiz(url, ctx) {
        var bindArgs = {
            url: url,
            method: 'POST',
            mimetype: 'text/html',
            content: ctx,
            error: function(type, data, evt) {
                alert("An error occured loading content! : " + data);
            },
            load: function(type, data, evt) {
                var innerPage = dojo.byId('cmscontent');
                innerPage.innerHTML = data;
            }
        };
        dojo.io.bind(bindArgs);
    }
</script>

<style type="text/css">
.dojoContextMenu {
    background-color: #ccc;
    font-size: 10px;
}
</style>

<#-- looping macro -->
<#macro fillTree assocList>
  <#if (assocList?has_content)>
    <#list assocList as assoc>
        <div dojoType="TreeNode" title="${assoc.categoryName?default(assoc.dataCategoryId)}" widgetId="${assoc.dataCategoryId}"
                object="${assoc.dataCategoryId}">
            <#assign assocs = assoc.getRelated("ChildDataCategory")?if_exists/>
            <#if (assocs?has_content)>
                <@fillTree assocList = assocs/>
            </#if>
        </div>
    </#list>
  </#if>
</#macro>

<!--dl dojoType="TreeContextMenu" id="contextMenu" style="font-size: 1em; color: #ccc;">
    <dt dojoType="TreeMenuItem" id="newCat" caption="New Category"/>
    <dt dojoType="TreeMenuItem" id="delCat" caption="Remove Category"/>
    <dt dojoType="TreeMenuItem" id="editCat" caption="Edit Category"/>
    <dt dojoType="TreeMenuItem" id="upLoad" caption="Upload file"/>
</dl-->

<dojo:TreeSelector widgetId="webCmsTreeSelector" eventNames="select:showDataResources"></dojo:TreeSelector>
<div dojoType="Tree" widgetId="webCmsTree" selector="webCmsTreeSelector" toggler="fade" toggleDuration="500">
    <#if (subCategories?has_content)>
        <@fillTree assocList = subCategories/>
    </#if>
</div>


