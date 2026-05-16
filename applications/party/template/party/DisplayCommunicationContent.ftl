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


<#-- iframe isolates the email's <style> block from the parent page. Stored content is
     HTML-entity-encoded, so decode it via a textarea before assigning to srcdoc. -->
<#assign iframeId = "commContent_" + childCommEvent.communicationEventId>
<iframe id="${iframeId}"
        sandbox="allow-same-origin"
        style="width: 100%; min-height: 600px; border: 1px solid #ccc; margin-left: 14%;"></iframe>
<script type="text/javascript">
    (function() {
        var iframe = document.getElementById('${iframeId}');
        iframe.onload = function() {
            try {
                this.style.height = (this.contentWindow.document.documentElement.scrollHeight + 20) + 'px';
            } catch (e) {}
        };
        var decoder = document.createElement('textarea');
        decoder.innerHTML = '${StringUtil.wrapString((childCommEvent.content!'')?js_string)}';
        iframe.srcdoc = decoder.value;
    })();
</script>