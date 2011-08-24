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
<#-- some labels are not unescaped in the JSON object so we have to do this manualy -->
function unescapeHtmlText(text) {
    return jQuery('<div />').html(text).text()
}
 
jQuery(window).load(createTree());

<#-- creating the JSON Data -->
var rawdata = [
        <#if (completedTree?has_content)>
            <@fillTree rootCat = completedTree/>
        </#if>
        
        <#macro fillTree rootCat>
            <#if (rootCat?has_content)>
                <#list rootCat as root>
                    {
                    "data": {"title" : unescapeHtmlText("<#if root.groupName?exists>${root.groupName?js_string} [${root.partyId}]<#else>${root.partyId?js_string}</#if>"), "attr": {"href" : "<@ofbizUrl>/viewprofile?partyId=${root.partyId}</@ofbizUrl>","onClick" : "callDocument('${root.partyId}');"}},
                    "attr": {"id" : "${root.partyId}", "rel" : "root"}
                    <#if root.child?exists>
                    ,"state" : "closed"
                    </#if>
                    <#if root_has_next>
                        },
                    <#else>
                        }
                    </#if>
                </#list>
            </#if>
        </#macro>
     ];

 <#-- create Tree-->
  function createTree() {
    jQuery(function () {
        $.cookie('jstree_select', null);
        $.cookie('jstree_open', null);
        
        jQuery("#tree").jstree({
        "core" : { "initially_open" : [ "${partyId}" ] },
        "plugins" : [ "themes", "json_data","ui" ,"cookies", "types", "crrm"],
            "json_data" : {
                "data" : rawdata,
                          "ajax" : { "url" : "<@ofbizUrl>getHRChild</@ofbizUrl>", "type" : "POST",
                          "data" : function (n) {
                            return { 
                                "partyId" : n.attr ? n.attr("id").replace("node_","") : 1 ,
                                "additionParam" : "','category" ,
                                "hrefString" : "viewprofile?partyId=" ,
                                "onclickFunction" : "callDocument"
                        }; 
                    }
                }
            },
            "types" : {
             "valid_children" : [ "root" ],
             "types" : {
                 "CATEGORY" : {
                     "icon" : { 
                         "image" : "/images/jquery/plugins/jsTree/themes/apple/d.png",
                         "position" : "10px40px"
                     }
                 }
             }
            }
        });
    });
  }
  
  function callDocument(id,type) {
    //jQuerry Ajax Request
    var dataSet = {};
        URL = 'viewprofile';
        dataSet = {"partyId" : id, "ajaxUpdateEvent" : "Y"};
        
    jQuery.ajax({
        url: URL,
        type: 'POST',
        data: dataSet,
        error: function(msg) {
            alert("An error occured loading content! : " + msg);
        },
        success: function(msg) {
            jQuery('div.contentarea').html(msg);
        }
    });
  }
  
</script>

<div id="tree"></div>
