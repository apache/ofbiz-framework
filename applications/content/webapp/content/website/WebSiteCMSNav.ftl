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
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/plugins/jsTree/jquery.jstree.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/plugins/elrte-1.3/js/elrte.min.js</@ofbizContentUrl>"></script>
<#if language?has_content && language != "en">
<script language="javascript" src="<@ofbizContentUrl>/images/jquery/plugins/elrte-1.3/js/i18n/elrte.${language!"en"}.js</@ofbizContentUrl>" type="text/javascript"></script><#rt/>
</#if>

<link href="/images/jquery/plugins/elrte-1.3/css/elrte.min.css" rel="stylesheet" type="text/css">

<script type="text/javascript">
    jQuery(document).ready(loadTrees);
    jQuery(document).ready(createEditor);

    var contentRoot = '${contentRoot?if_exists}';
    var webSiteId = '${webSiteId?if_exists}';
    var editorUrl = '<@ofbizUrl>/views/WebSiteCMSContent</@ofbizUrl>';
    var aliasUrl = '<@ofbizUrl>/views/WebSiteCMSPathAlias</@ofbizUrl>';
    var metaUrl = '<@ofbizUrl>/views/WebSiteCMSMetaInfo</@ofbizUrl>';

    function loadTrees() {
        createSubsitesTree();
        createMenusTree();
        createErrorTree();
    }

    function cutNameLength(name) {
        var leng = 27;
        return name.substring(0, leng);
    }

<#-- creating the JSON Data -->
<#macro fillTreeSubsites assocList>
      <#if (assocList?has_content)>
        <#list assocList as assoc>
            <#assign content = assoc.getRelatedOne("ToContent", false)/>
            {
            "data": {"title" : cutNameLength("${content.contentName!assoc.contentIdTo}"), "attr": {"href": "javascript:void(0);", "onClick" : "callDocument('', '${assoc.contentIdTo}', jQuery('#${assoc.contentIdTo}'), '');"}},
           
            <#assign assocChilds  = content.getRelated("FromContentAssoc", null, null, false)?if_exists/>
                "attr": {"id" : "${assoc.contentIdTo}", "contentId" : "${assoc.contentId}", "fromDate" : "${assoc.fromDate}", "contentAssocTypeId" : "${assoc.contentAssocTypeId}"}
            <#if assocChilds?has_content>
                ,"children": [
                    <@fillTreeSubsites assocList = assocChilds/>
                ]
            </#if>
            <#if assoc_has_next>
            },
            <#else>
            }
            </#if>
        </#list>
      </#if>
</#macro>
<#macro fillTreeMenus assocList>
      <#if (assocList?has_content)>
        <#list assocList as assoc>
            <#assign content = assoc.getRelatedOne("ToContent", false)/>
            {
            "data": {"title" : cutNameLength("${content.contentName!assoc.contentIdTo}"), "attr": {"href": "javascript:void(0);", "onClick" : "callDocument('${assoc.contentIdTo}');"}},
            <#assign assocChilds  = content.getRelated("FromContentAssoc", null, null, false)?if_exists/>
                "attr": {"id" : "${assoc.contentIdTo}", "contentId" : "${assoc.contentId}", "fromDate" : "${assoc.fromDate}"}
            <#if assocChilds?has_content>
                ,"children": [
                    <@fillTreeMenus assocList = assocChilds/>
                ]
            </#if>
            <#if assoc_has_next>
            },
            <#else>
            }
            </#if>
        </#list>
      </#if>
</#macro>

<#macro fillTreeError assocList>
      <#if (assocList?has_content)>
        <#list assocList as assoc>
            <#assign content = assoc.getRelatedOne("ToContent", false)/>
            {
            "data": {"title" : cutNameLength("${content.contentName!assoc.contentIdTo}"), "attr": {"href": "javascript:void(0);", "onClick" : "callDocument('', '${assoc.contentIdTo}', '', '');"}},
            <#assign assocChilds  = content.getRelated("FromContentAssoc", null, null, false)?if_exists/>
                "attr": {"id" : "${assoc.contentIdTo}", "contentId" : "${assoc.contentId}", "fromDate" : "${assoc.fromDate}"}
            <#if assocChilds?has_content>
                ,"children": [
                    <@fillTreeError assocList = assocChilds/>
                ]
            </#if>
            <#if assoc_has_next>
            },
            <#else>
            }
            </#if>
        </#list>
      </#if>
</#macro>

var rawdata_subsites = [
    <#if (subsites?has_content)>
        <@fillTreeSubsites assocList = subsites/>
    </#if>
];

var rawdata_menus = [
    <#if (menus?has_content)>
        <@fillTreeMenus assocList = menus/>
    </#if>
];

var rawdata_errors = [
    <#if (errors?has_content)>
        <@fillTreeError assocList = errors/>
    </#if>
];

<#-------------------------------------------------------------------------------------create Tree-->
  function createSubsitesTree() {
    jQuery(function () {
        jQuery("#subsites").jstree({
            "plugins" : [ "themes", "json_data", "ui", "contextmenu", "crrm"],
            "core" : {
                "html_titles" : true
            },
            "ui" : {
                "initially_select" : ["${parameters.contentId!}"]
            },
            "json_data" : {
                "data" : rawdata_subsites,
                "progressive_render" : false
            },
            'contextmenu': {
                'items': {
                    'ccp' : false,
                    'create' : false,
                    'rename' : false,
                    'remove' : false,
                    'create1' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceLongText}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'ELECTRONIC_TEXT');
                        }
                    },
                    'create2' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceShortText}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'SHORT_TEXT');
                        }
                    },
                    'create3' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceUrlResource}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'URL_RESOURCE');
                        }
                    },
                    'create4' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentDataResourceImage}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'IMAGE_OBJECT');
                        }
                    },
                    'create5' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceVideo}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'VIDEO_OBJECT');
                        }
                    },
                    'create6' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceAudio}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'AUDIO_OBJECT');
                        }
                    },
                    'create7' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceOther}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'OTHER_OBJECT');
                        }
                    }
                }
            }
        });
    });
  }

  function createMenusTree() {
    jQuery(function () {
        jQuery("#menus").jstree({
            "plugins" : [ "themes", "json_data", "ui", "contextmenu", "crrm"],
            "core" : {
                "html_titles" : true
            },
            "json_data" : {
                "data" : rawdata_menus,
                "progressive_render" : false
            },
            'contextmenu': {
                'items': {
                    'ccp' : false,
                    'create' : false,
                    'rename' : false,
                    'remove' : false,
                    'create1' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceLongText}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'ELECTRONIC_TEXT');
                        }
                    },
                    'create2' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceShortText}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'SHORT_TEXT');
                        }
                    },
                    'create3' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceUrlResource}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'URL_RESOURCE');
                        }
                    },
                    'create4' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentDataResourceImage}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'IMAGE_OBJECT');
                        }
                    },
                    'create5' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceVideo}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'VIDEO_OBJECT');
                        }
                    },
                    'create6' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceAudio}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'AUDIO_OBJECT');
                        }
                    },
                    'create7' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceOther}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'OTHER_OBJECT');
                        }
                    }
                }
            }
        });
    });
  }

  function createErrorTree() {
    jQuery(function () {
        jQuery("#errors").jstree({
            "plugins" : [ "themes", "json_data", "ui", "contextmenu", "crrm"],
            "core" : {
                "html_titles" : true
            },
            "json_data" : {
                "data" : rawdata_errors,
                "progressive_render" : false
            },
            'contextmenu': {
                'items': {
                    'ccp' : false,
                    'create' : false,
                    'rename' : false,
                    'remove' : false,
                    'create1' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceLongText}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'ELECTRONIC_TEXT');
                        }
                    },
                    'create2' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceShortText}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'SHORT_TEXT');
                        }
                    },
                    'create3' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceUrlResource}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'URL_RESOURCE');
                        }
                    },
                    'create4' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentDataResourceImage}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'IMAGE_OBJECT');
                        }
                    },
                    'create5' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceVideo}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'VIDEO_OBJECT');
                        }
                    },
                    'create6' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceAudio}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'AUDIO_OBJECT');
                        }
                    },
                    'create7' : {
                        'label' : "${uiLabelMap.CommonNew} ${uiLabelMap.ContentResourceOther}",
                        'action' : function(obj) {
                            callDocument(true, obj.attr('id'), '', 'OTHER_OBJECT');
                        }
                    }
                }
            }
        });
    });
  }
<#-------------------------------------------------------------------------------------callDocument function-->
    function callDocument(sub, contentId, objstr, dataResourceTypeId) {
        var ctx = {};
        ctx['contentRoot'] = contentRoot;
        ctx['webSiteId'] = webSiteId;

        if (sub && contentId) {
            if (dataResourceTypeId) {
                ctx['dataResourceTypeId'] = dataResourceTypeId;
            }

            ctx['contentIdFrom'] = contentId;
            ctx['contentAssocTypeId'] = 'SUB_CONTENT';

        } else {
            if (contentId != null && contentId.length) {
                ctx['contentId'] = contentId;
            }
            if (objstr) {
                ctx['contentIdFrom'] = objstr.attr('contentid');
                ctx['fromDate'] = objstr.attr('fromdate');
                ctx['contentAssocTypeId'] = objstr.attr('contentassoctypeid');
            }
        }

        //jQuerry Ajax Request
        jQuery.ajax({
            url: editorUrl,
            type: 'POST',
            data: ctx,
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                jQuery('#cmscontent').html(msg);

                // CREATE / LOAD Editor
                createEditor();
            }
        });
     }

<#-------------------------------------------------------------------------------------createEditor function-->
    function createEditor() {
        if($('#cmseditor').length) {
            var opts = {
                cssClass : 'el-rte',
                lang     : '${language}',
                height   : 350,
                toolbar  : 'maxi',
                doctype  : '<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">', //'<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN">',
                cssfiles : ['/images/jquery/plugins/elrte-1.3/css/elrte-inner.css']
            }
            jQuery('#cmseditor').elrte(opts);
        }
    }
<#-------------------------------------------------------------------------------------callMetaInfo function-->
function callMetaInfo(contentId) {
        var ctx = {"contentId" : contentId, "webSiteId" : webSiteId};

        jQuery.ajax({
            url: metaUrl,
            type: 'POST',
            data: ctx,
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                jQuery('#cmscontent').html(msg);
            }
        });
    }

<#-------------------------------------------------------------------------------------callPathAlias function-->
    function callPathAlias(contentId) {
        var ctx = {"contentId" : contentId, "webSiteId" : webSiteId};

        // get the alias screen
        jQuery.ajax({
            url: aliasUrl,
            type: 'POST',
            data: ctx,
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + msg);
            },
            success: function(msg) {
                jQuery('#cmscontent').html(msg);
            }
        });
    }

<#-------------------------------------------------------------------------------------saveMetaInfo function-->
    function saveMetaInfo(form) {
        // save title
        document.cmsmeta_title.objectInfo.value = form.title.value;
        ajaxSubmitForm(document.cmsmeta_title);

        // save title property
        document.cmsmeta_titleProperty.objectInfo.value = form.titleProperty.value;
        ajaxSubmitForm(document.cmsmeta_titleProperty);

        // save meta-description
        document.cmsmeta_metaDescription.objectInfo.value = form.metaDescription.value;
        ajaxSubmitForm(document.cmsmeta_metaDescription);

        // save meta-keywords
        document.cmsmeta_metaKeywords.objectInfo.value = form.metaKeywords.value;
        ajaxSubmitForm(document.cmsmeta_metaKeywords);
    }

<#-------------------------------------------------------------------------------------pathSave function-->
    function pathSave(contentId) {
        var form = document.cmspathform;
        if (form != null) {
            var url = form.action;
            jQuery.ajax({
                url: url,
                type: 'POST',
                data: jQuery(form).serialize(),
                error: function(msg) {
                    showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonUnspecifiedErrorOccurred}");
                },
                success: function(msg) {
                    callPathAlias(contentId);
                }
            });
        }
    }

<#-------------------------------------------------------------------------------------pathRemove function-->
    function pathRemove(websiteId, pathAlias, contentId) {
        var remAliasUrl = '<@ofbizUrl>/removeWebSitePathAliasJson</@ofbizUrl>';

        jQuery.ajax({
                url: remAliasUrl,
                type: 'POST',
                data: {"pathAlias" : pathAlias, "webSiteId" : webSiteId},
                error: function(msg) {
                    showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonUnspecifiedErrorOccurred} : " + msg);
                },
                success: function(msg) {
                    callPathAlias(contentId);
                }
        });
    }

<#-------------------------------------------------------------------------------------ajaxSubmitForm function-->
    function ajaxSubmitForm(form, contentId) {
        if (form != null) {
            var url = form.action;
            jQuery.ajax({
                url: url,
                type: 'POST',
                async: false,
                data: jQuery(form).serialize(),
                success: function(data) {
                    // if the content id is set reload the contentScreen
                    if (contentId && contentId.length) {
                        callDocument('', contentId, '', '');
                    }
                },
                error: function(msg) {
                    showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonErrorSubmittingForm} : " + msg);
                }
            });
        }
    }

</script>


<div class="label">
    ${uiLabelMap.ContentWebSiteContent}
</div>
<div>
    ${uiLabelMap.ContentWebSiteAddSubdContent}
</div>
<div>&nbsp;</div>

<div id="subsites"></div>
<#if (!subsites?has_content)>
    <a href="javascript:void(0);" class="buttontext">${uiLabelMap.ContentWebSiteAddTree}</a>
</#if>
<div class="label">
    ${uiLabelMap.ContentWebSiteMenus}
</div>
<div>
    ${uiLabelMap.ContentWebSiteAddNewMenus}
</div>
<div>&nbsp;</div>

<div id="menus"></div>
<#if (!menus?has_content)>
    <a href="javascript:void(0);" class="buttontext">${uiLabelMap.ContentWebSiteAddMenu}</a>
</#if>

<div>&nbsp;</div>
<div>&nbsp;</div>

<div class="label">
    ${uiLabelMap.ContentWebSiteErrors}
</div>
<div>
    ${uiLabelMap.ContentWebSiteAddNewErrors}
</div>
<div>&nbsp;</div>
<div id="errors"></div>
<#if (!errors?has_content)>
    <a href="javascript:void(0);" class="buttontext">${uiLabelMap.ContentWebSiteAddError}</a>
</#if>
