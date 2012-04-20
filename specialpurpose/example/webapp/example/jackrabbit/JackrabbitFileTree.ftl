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

<div id="jackrabbitFileTree">${parameters.fileTree!""}</div>

<script type="text/javascript">
    var rawdata = ${parameters.fileTree!};

    jQuery(function () {
        jQuery("#jackrabbitFileTree").jstree({
            "plugins" : [ "themes", "json_data", "ui", "contextmenu"],
            "json_data" : {
                "data" : rawdata
            },
            'contextmenu': {
                'items': {
                    'ccp' : false,
                    'create' : false,
                    'rename' : false,
                    'open' : {
                        'label' : "${uiLabelMap.ExampelsJackrabbitOpenFile}",
                        'action' : function(obj) {
                            openFileFromRepository(obj.attr('nodepath'), obj.attr('nodetype'));
                         }
                    },
                    'remove' : {
                        'label' : "${uiLabelMap.ExampelsJackrabbitRemoveFile}",
                        'action' : function(obj) {
                            removeFileFromRepository(obj.attr('nodepath'), obj.attr('nodetype'));
                         }
                        },
                    'download' : {
                        'label' : "${uiLabelMap.ExampelsJackrabbitDownloadFile}",
                        'action' : function(obj) {
                            downloadFileFromRepository(obj.attr('nodepath'), obj.attr('nodetype'));
                        }
                   }
                 }
             }
        });
    });

    function openFileFromRepository(nodepath, nodetype) {
        var parameters = {"path" : nodepath};
        var url = "OpenFileInformation";

        runPostRequest(url, parameters)
    }

    function removeFileFromRepository(nodepath, nodetype) {
        var parameters = {"path" : nodepath};
        var url = "RemoveRepositoryFile";

        runPostRequest(url, parameters)
    }

    function downloadFileFromRepository(nodepath, nodetype) {
        if ("nt:folder" == nodetype) { // the open function for foldes is not supported yet.
            return;
        }

        var parameters = {"path" : nodepath};
        var url = "GetFileFromRepository";

        runPostRequest(url, parameters)
    }

    function runPostRequest(url, parameters) {
        // create a hidden form
        var form = jQuery('<form></form>');

        form.attr("method", "POST");
        form.attr("action", url);

        jQuery.each(parameters, function(key, value) {
            var field = jQuery('<input></input>');

            field.attr("type", "hidden");
            field.attr("name", key);
            field.attr("value", value);

            form.append(field);
        });

        // The form needs to be apart of the document in
        // order for us to be able to submit it.
        jQuery(document.body).append(form);
        form.submit();
        form.remove();
    }


</script>