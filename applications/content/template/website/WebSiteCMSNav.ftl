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
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/ui/js/jquery.cookie-1.4.0.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/plugins/jsTree/jquery.jstree.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/plugins/elrte-1.3/js/elrte.min.js</@ofbizContentUrl>"></script>
<#if language?has_content && language != "en">
<script language="javascript" src="<@ofbizContentUrl>/images/jquery/plugins/elrte-1.3/js/i18n/elrte.${language!"en"}.js</@ofbizContentUrl>" type="text/javascript"></script><#rt/>
</#if>
<link href="/images/jquery/plugins/elrte-1.3/css/elrte.min.css" rel="stylesheet" type="text/css">

<script type="text/javascript">
    function modifyJstreeCookieToSelectNewPage() {
        // core.initally_load and ui.initially_select don't work with the cookies plugin,
        // so we have to modify the cookie to achieve the same.
        // the newContentId is set in the global scope in WebSiteCmsContent.ftl because I could
        // not get it into the context of this view using the ofbiz xml vodoofoo.
        var contentIdFrom = '#${requestParameters.contentIdFrom!}';
        if (typeof newContentId !== 'undefined' && contentIdFrom !== '#') {
            $.cookie('jstree_open', $.cookie('jstree_open') + ',' + contentIdFrom);
            $.cookie('jstree_select', '#' + newContentId);
        }
    };

    jQuery(document).ready(loadTrees);

    var contentRoot = '${contentRoot!}';
    var menuRoot = '${menuRoot!}';
    var errorRoot = '${errorRoot!}';
    var webSiteId = '${webSiteId!}';
    var editorUrl = '<@ofbizUrl>/views/WebSiteCMSContent</@ofbizUrl>';
    var aliasUrl = '<@ofbizUrl>/views/WebSiteCMSPathAlias</@ofbizUrl>';
    var metaUrl = '<@ofbizUrl>/views/WebSiteCMSMetaInfo</@ofbizUrl>';

    // No drag'n'drop for nodes in subtrees with these contentIds.
    // Adding "false" prevents creating new "root" nodes.
    // we have no such subtrees OOTB so those are only as examples
    var unmovableSubtrees = [false, "homeContentsRoot", "merchantContentsRoot", "categoryChildren"];

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
           
            <#assign assocChilds  = content.getRelated("FromContentAssoc", null, null, false)!/>
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
            <#assign assocChilds  = content.getRelated("FromContentAssoc", null, null, false)!/>
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
            <#assign assocChilds  = content.getRelated("FromContentAssoc", null, null, false)!/>
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

var contextmenu = { 'items': {
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
                    },
                    'delete' : {
                        'label'  : "${uiLabelMap.CommonDelete}",
                        'action' : function (obj) {
                            <#assign message=StringUtil.wrapString(uiLabelMap.ConfirmDeleteContent) />
                            if (!window.confirm('${message}')) { return false; }
                            if(this.is_selected(obj)) { this.remove(); } else { this.remove(obj); }
                        }
                    }
                }
            }

<#-------------------------------------------------------------------------------------create Tree-->
    function createSubsitesTree() {
        <#if contentRoot?has_content>
        jQuery("#${contentRoot}").jstree({
            "plugins" : [ "themes", "json_data", "ui", "cookies", "contextmenu", "crrm", "dnd"],
            "core" : {
                "html_titles" : true
            },
            "json_data" : {
                        "ajax" : {
                            "url" : "<@ofbizUrl>/getContentAssocsJson</@ofbizUrl>",
                            "type" : "GET",
                            "data" : function (n) {
                                return {
                                    "contentId" : n.attr ? n.attr("id") : contentRoot
                                };
                            }
                        }
            },
            "ui" : {
                select_limit: 1
            },
            "crrm": {
                "move" : {
                    "default_position" : "first",
                    "check_move" : checkMove
                }
            },
            'contextmenu': contextmenu
        }).bind("move_node.jstree", moveContent).bind("remove.jstree", deleteContent).bind("select_node.jstree", selectNode);
        </#if>
    }

    function loadTrees() {
        modifyJstreeCookieToSelectNewPage();
        createSubsitesTree();
        createMenusTree();
        createErrorTree();
    }

    function createMenusTree() {
        <#if menuRoot?has_content>
            jQuery(function () {
                jQuery("#${menuRoot}").jstree({
                    "plugins" : [ "themes", "json_data", "ui", "cookies", "contextmenu", "crrm", "dnd"],
                    "core" : {
                        "html_titles" : true
                    },
                    "json_data" : {
                        "ajax" : {
                            "url" : "<@ofbizUrl>/getContentAssocsJson</@ofbizUrl>",
                            "type" : "GET",
                            "data" : function (n) {
                                return {
                                    "contentId" : n.attr ? n.attr("id") : menuRoot
                                };
                        }
                        }
                    },
                    "ui" : {
                        select_limit: 1
                    },
                    'contextmenu': contextmenu
                }).bind("move_node.jstree", moveContent).bind("remove.jstree", deleteContent).bind("select_node.jstree", selectNode);
        });
        </#if>
  }

  function createErrorTree() {
        <#if errorRoot?has_content>
    jQuery(function () {
                jQuery("#${errorRoot}").jstree({
                    "plugins" : [ "themes", "json_data", "ui", "cookies", "contextmenu", "crrm", "dnd"],
            "core" : {
                "html_titles" : true
            },
            "json_data" : {
                        "ajax" : {
                            "url" : "<@ofbizUrl>/getContentAssocsJson</@ofbizUrl>",
                            "type" : "GET",
                            "data" : function (n) {
                                return {
                                    "contentId" : n.attr ? n.attr("id") : errorRoot
                                };
                            }
                        }
                    },
                    "ui" : {
                        select_limit: 1
            },
                    'contextmenu': contextmenu
                }).bind("move_node.jstree", moveContent).bind("remove.jstree", deleteContent).bind("select_node.jstree", selectNode);
            });
        </#if>
    }

<#-------------------------------------------------------------------------------------selectNode function-->
    function selectNode(event, data) {
        var node = data.rslt.obj;

        if (typeof node.attr === 'undefined') {
            return;
        }

        callDocument('', node.attr('id'), node, '');
    }

<#-------------------------------------------------------------------------------------checkMove function-->
    function checkMove(move) {
        // do not allow copies
        if (move.cy == true) {
            return false;
        }

        // determine all nodes in the paths to the root from the original and new position of the node in the tree
        var nodesToRoot = [];
        // move.o gives us the original position
        nodesToRoot = jQuery.merge(nodesToRoot, move.ot.get_path(move.o, true));
        // move.r gives us the new position
        nodesToRoot = jQuery.merge(nodesToRoot, move.ot.get_path(move.r, true));

        if (move.cr === -1 && jQuery.inArray(false, unmovableSubtrees) >= 0) {
            return false;
                        }

        for (var i = 0; i < unmovableSubtrees.length; i++) {
            if (jQuery.inArray(unmovableSubtrees[i], nodesToRoot) >= 0) {
               return false;
                        }
                        }
        return true;
                        }
<#-------------------------------------------------------------------------------------moveContent function-->
    function moveContent(event, data) {
        var tree = data.inst;
        var node = data.rslt.o;
        var newParent = data.rslt.np;
        var oldParent = data.rslt.op

        if (typeof node.attr === 'undefined') {
            return;
        }

        var ctx = {};
        ctx['contentIdTo'] = node.attr('id');
        ctx['contentIdFrom'] = node.attr('contentId');
        ctx['contentIdFromNew'] = newParent.attr('id');
        ctx['fromDate'] = node.attr('fromDate');
        ctx['contentAssocTypeId'] = node.attr('contentAssocTypeId');

        //jQuerry Ajax Request
        jQuery.ajax({
            url:  '<@ofbizUrl>/moveContentJson</@ofbizUrl>',
            type: 'POST',
            data: ctx,
            error: function(msg) {
                jQuery.jstree.rollback(data.rlbk);
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}", "${uiLabelMap.ErrorMovingContent} : " + JSON.stringify(msg));
            },
            success: function(msg) {
                if (msg._ERROR_MESSAGE_) {
                    jQuery.jstree.rollback(data.rlbk);
                    showErrorAlert("${uiLabelMap.CommonErrorMessage2}", "${uiLabelMap.ErrorMovingContent} : " + msg._ERROR_MESSAGE_);
                } else {
                    var result = JSON.parse(msg);
                    node.attr('fromDate', result.attr.fromDate);
                    node.attr('contentId', result.attr.contentId);
                    tree.select_node(node, true, null);
                }
            }
        });
    }

<#-------------------------------------------------------------------------------------deleteDocument function-->
    function deleteContent(event, data) {
        var tree = data.inst;
        var node = data.rslt.obj;
        var treeNode = jQuery(node).closest('div.jstree');

        if (typeof node.attr === 'undefined') {
            return;
        }

        var ctx = {};
        ctx['contentRoot'] = treeNode.attr('contentId');
        ctx['webSiteId'] = webSiteId;
        ctx['contentId'] = node.attr('id');

        //jQuerry Ajax Request
        jQuery.ajax({
            url: '<@ofbizUrl>/deleteContentJson</@ofbizUrl>',
            type: 'POST',
            data: ctx,
            error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorDeletingContent} : " + JSON.stringify(msg));
                jQuery.jstree.rollback(data.rlbk);
            },
            success: function(msg) {
                callDocument(false, node.attr('contentId'), '', '')
            }
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
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + JSON.stringify(msg));
            },
            success: function(msg) {
                jQuery('#cmscontent').html(msg);
                createEditor();
            }
        });
     }

<#-------------------------------------------------------------------------------------createEditor function-->
    function createEditor() {
        if($('#cmseditor').length) {
            var opts = {
                cssClass : 'el-rte',
                lang     : '${language!"en"}',
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
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + JSON.stringify(msg));
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
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.ErrorLoadingContent} : " + JSON.stringify(msg));
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
    function pathRemove(websiteId, pathAlias, fromDate, contentId) {
        var remAliasUrl = '<@ofbizUrl>/removeWebSitePathAliasJson</@ofbizUrl>';

        jQuery.ajax({
                url: remAliasUrl,
                type: 'POST',
            data: { "webSiteId" : webSiteId, "pathAlias" : pathAlias, "fromDate": fromDate},
                error: function(msg) {
                showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonUnspecifiedErrorOccurred} : " + JSON.stringify(msg));
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
                    // if the content id is set reload the contentScreen and tree
                    if (contentId && contentId.length) {
                        callDocument('', contentId, '', '');
                        jQuery("#${contentRoot}").jstree('refresh', '#'+form.contentIdFrom.value);
                    }
                },
                error: function(msg) {
                    showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.CommonErrorSubmittingForm} : " + JSON.stringify(msg));
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

<#if (content?has_content)>
    <div id="${contentRoot}"></div>
<#else>
    <a href="javascript:void(0);" class="buttontext">${uiLabelMap.ContentWebSiteAddTree}</a>
</#if>
<div class="label">
    ${uiLabelMap.ContentWebSiteMenus}
</div>
<div>
    ${uiLabelMap.ContentWebSiteAddNewMenus}
</div>
<div>&nbsp;</div>

<#if (menus?has_content)>
    <div id="${menuRoot}"></div>
<#else>
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
<#if (errorRoot?has_content)>
    <div id="${errorRoot}"></div>
<#else>
    <a href="javascript:void(0);" class="buttontext">${uiLabelMap.ContentWebSiteAddError}</a>
</#if>
