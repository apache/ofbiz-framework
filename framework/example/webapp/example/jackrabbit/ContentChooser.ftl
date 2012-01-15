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
${uiLabelMap.ExampleJackrabbitQuickContentSelect}
<form id="selectNode" action="EditRepositoryContent" type="post">
    <select name="path" id="nodePath">
        <option value="" selected></option>
        <#list parameters.contentList as content>
            <option value="${content}">${content}</option>
        </#list>
    </select>
    <select name="language" id="nodeLanguage">
        <option value="" selected></option>
    </select>
    <input type="submit" />
</form>

<script type="text/javascript">
    var languageList = ${parameters.languageList}

    jQuery("#nodePath").change(function() {
        var newOptions = languageList[jQuery(this).val()];

        var options = "";
        for (option in newOptions) {
            options = options + "<option value='" + newOptions[option] + "'>" + newOptions[option] + "</option>";

        }
        options = options + "<option value='' ></option>"

        jQuery("#nodeLanguage").children().remove();
        jQuery("#nodeLanguage").append(options);

    });
</script>

<br />