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
    function cmsSave() {
        var editor = dojo.widget.byId("w_editor");
        if (editor) {
            var cmsdata = dojo.byId("cmsdata");
            cmsdata.value = editor.getEditorContent();
        } else {
            alert("Cannot locate editor widget!");
        }

        // submit the form
        var form = document.cmsform;
        if (form != null) {
        /*
            var url = form.action;
            var bindArgs = {
                url: url,
                method: "POST",
                mimetype: "text/json",
                formNode: form,
                error: function(type, data, evt) {
                    alert("An error occurred.");
                },
                load: function(type, data, evt) {
                    alert("Form Submitted");
                    window.location = window.location;
                }
            };
         */

            //alert("Calling -> " + url);
            //dojo.io.bind(bindArgs);

            form.submit();
            return false;
        } else {
            alert("Cannot find the cmsform!");
        }
    }
</script>

<div class="left">&nbsp;</div>
<div id="editorcontainer" class="nocolumns">
    <table>
      <tr>
        <td align="right" colspan="2">
            <div id="cmseditor" style="margin: 0; border: 1px solid black;">
                <#--
                <div id="cmseditor" dojoType="Editor2" minHeight="300px" style="border: 1px solid black;">
                    ${(dataText.textData)?if_exists}
                </div>
                -->
            </div>
        </td>
      </tr>
      <tr><td colspan="2">&nbsp;</td></tr>
      <tr>
        <td align="center" colspan="2">
            <a href="javascript:void(0);" onclick="javascript:cmsSave();" class="buttontext">Save</a>
        </td>
      </tr>
    </table>
</div>

<#--<textarea id="raw" cols="80" rows="40">${(dataText.textData)?if_exists}</textarea>-->