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
<#if flashBackupCodes?has_content>
<div style="background-color: #fff3cd; border: 1px solid #ffeeba; color: #856404; padding: 15px; margin: 15px 0; border-radius: 4px;">
    <h4 style="margin-top: 0; color: #856404;"><strong>${uiLabelMap.SecurityMfaBackupCodesGeneratedTitle}</strong></h4>
    <p><strong>${uiLabelMap.SecurityMfaBackupCodesWarning}</strong></p>
    <div id="mfaBackupCodesList" style="font-family: monospace; font-size: 1.15em; display: flex; flex-wrap: wrap; gap: 10px; margin: 15px 0;">
        <#list flashBackupCodes as bCode>
            <div class="mfaBackupCode" style="background: #ffffff; color: #212529; padding: 8px 14px; border: 1px solid #ced4da; border-radius: 3px; font-weight: bold; min-width: 110px; text-align: center;">
                ${bCode}
            </div>
        </#list>
    </div>
    <button type="button" id="mfaCopyAllBackupCodesBtn" onclick="copyAllMfaBackupCodes(this)"
            style="padding: 8px 16px; border-radius: 3px; border: 1px solid #856404; background: #ffffff; color: #856404; font-weight: bold; cursor: pointer;">
        ${uiLabelMap.SecurityMfaCopyAllBackupCodes}
    </button>
    <#-- Rendered as normal (singly) HTML-escaped text, then read back via textContent at copy time: keeps
         the "Copied!" label correct regardless of the page's auto-escaping policy, instead of re-escaping
         it a second time for inlining into the script below. -->
    <span id="mfaBackupCodesCopiedLabel" style="display:none;">${uiLabelMap.SecurityMfaBackupCodesCopied}</span>
    <p style="margin-bottom: 0; margin-top: 10px;"><em>${uiLabelMap.SecurityMfaBackupCodesOneTimeNotice}</em></p>
</div>
<script>
    function copyAllMfaBackupCodes(buttonEl) {
        var codeEls = document.querySelectorAll('#mfaBackupCodesList .mfaBackupCode');
        var codes = [];
        for (var i = 0; i < codeEls.length; i++) {
            codes.push(codeEls[i].textContent.trim());
        }
        var text = codes.join('\n');
        var originalLabel = buttonEl.textContent;
        function showCopied() {
            var copiedLabelEl = document.getElementById('mfaBackupCodesCopiedLabel');
            buttonEl.textContent = copiedLabelEl ? copiedLabelEl.textContent : 'Copied';
            setTimeout(function () { buttonEl.textContent = originalLabel; }, 2000);
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(showCopied, function () {
                fallbackCopyMfaBackupCodes(text, showCopied);
            });
        } else {
            fallbackCopyMfaBackupCodes(text, showCopied);
        }
    }
    function fallbackCopyMfaBackupCodes(text, onDone) {
        var textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.focus();
        textarea.select();
        try {
            document.execCommand('copy');
        } catch (ignoredErr) {
            // No clipboard access available in this browser; the codes remain visible on screen to copy manually.
        }
        document.body.removeChild(textarea);
        onDone();
    }
</script>
</#if>
