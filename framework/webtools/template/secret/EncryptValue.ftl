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

<p>${uiLabelMap.WebtoolsEncryptValueInfo}</p>
<ul>
    <li>${uiLabelMap.WebtoolsSecretSystemPropertyRequiredInfo}</li>
    <li>${uiLabelMap.WebtoolsSecretPasswordsFileRequiredInfo}</li>
</ul>

<#assign inlineEventList = eventMessageList![]/>
<#assign inlineErrorList = errorMessageList![]/>
<#if inlineEventList?has_content>
  <div class="successMessage" style="padding:8px;margin-bottom:8px;border:1px solid #5a5;background:#efffef;color:#040;">
    <#list inlineEventList as msg><p>${msg}</p></#list>
  </div>
</#if>
<#if inlineErrorList?has_content>
  <div class="errorMessage" style="padding:8px;margin-bottom:8px;">
    <#list inlineErrorList as err><p>${err}</p></#list>
  </div>
</#if>

<hr/>

<#assign prevTarget = parameters.secretTarget!"SYSTEM_PROPERTY"/>
<form class="basic-form" method="post" action="<@ofbizUrl>createEncryptedSecret</@ofbizUrl>">
    <table class="basic-table" cellspacing="0">
        <tbody>
            <tr>
                <td class="label">
                    <label>${uiLabelMap.WebtoolsSecretTarget}</label>
                </td>
                <td>
                    <label><input type="radio" name="secretTarget" value="SYSTEM_PROPERTY"<#if prevTarget == "SYSTEM_PROPERTY"> checked="checked"</#if>/>${uiLabelMap.WebtoolsSecretTargetSystemProperty}</label>
                    <label><input type="radio" name="secretTarget" value="PASSWORDS_FILE"<#if prevTarget == "PASSWORDS_FILE"> checked="checked"</#if>/>${uiLabelMap.WebtoolsSecretTargetPasswordsFile}</label>
                </td>
            </tr>
            <tr>
                <td class="label">
                    <label id="lblSecretResourceId">${uiLabelMap.WebtoolsSecretSystemResourceId}</label>
                </td>
                <td>
                    <input type="text" size="40" name="systemResourceId" value="${parameters.systemResourceId!}"/>
                </td>
            </tr>
            <tr>
                <td class="label">
                    <label id="lblSecretPropertyId">${uiLabelMap.WebtoolsSecretSystemPropertyId}</label>
                </td>
                <td>
                    <input type="text" size="40" name="systemPropertyId" value="${parameters.systemPropertyId!}"/>
                </td>
            </tr>
            <tr>
                <td class="label">
                    <label>${uiLabelMap.WebtoolsSecretLookupKey}</label>
                </td>
                <td>
                    <input type="text" size="40" name="lookupKey" value="${parameters.lookupKey!}"/>
                    <div>${uiLabelMap.WebtoolsSecretLookupKeyInfo}</div>
                </td>
            </tr>
            <tr>
                <td class="label">
                    <label>${uiLabelMap.WebtoolsSecretValue}</label>
                </td>
                <td>
                    <input type="password" size="40" name="secretValue" autocomplete="new-password"/>
                </td>
            </tr>
            <tr>
                <td class="label"></td>
                <td>
                    <input type="submit" value="${uiLabelMap.WebtoolsSecretEncryptAndSave}"/>
                </td>
            </tr>
        </tbody>
    </table>
</form>
<script>
(function () {
    var lblResource = document.getElementById('lblSecretResourceId');
    var lblProperty = document.getElementById('lblSecretPropertyId');
    var labels = {
        SYSTEM_PROPERTY: {
            resource: '${uiLabelMap.WebtoolsSecretSystemResourceId}',
            property: '${uiLabelMap.WebtoolsSecretSystemPropertyId}'
        },
        PASSWORDS_FILE: {
            resource: '${uiLabelMap.WebtoolsSecretFileResourceId}',
            property: '${uiLabelMap.WebtoolsSecretFilePropertyId}'
        }
    };
    function updateLabels() {
        var checked = document.querySelector('input[name="secretTarget"]:checked');
        var set = labels[checked ? checked.value : 'SYSTEM_PROPERTY'];
        lblResource.innerHTML = set.resource;
        lblProperty.innerHTML = set.property;
    }
    document.querySelectorAll('input[name="secretTarget"]').forEach(function (r) {
        r.addEventListener('change', updateLabels);
    });
    updateLabels();

    // Client-side validation: reject marker wrappers and pre-encrypted values before submit
    var SAFE_ID = /^[\w.\-]+$/;
    var form = document.querySelector('form[action$="createEncryptedSecret"]');
    var errDiv = document.createElement('div');
    errDiv.style.cssText = 'display:none;padding:8px;margin-bottom:8px;border:1px solid #a00;background:#fff0f0;color:#700;';
    form.parentNode.insertBefore(errDiv, form);

    form.addEventListener('submit', function (e) {
        var errors = [];
        var lookupKeyInput = form.querySelector('input[name="lookupKey"]');
        var secretValueInput = form.querySelector('input[name="secretValue"]');
        var lookupKey = lookupKeyInput.value.trim();
        var secretValue = secretValueInput.value;

        if (lookupKey && !SAFE_ID.test(lookupKey)) {
            errors.push('Lookup Key must contain only letters, digits, dots, hyphens, and underscores. Do not enter a LOOKUP(...) or similar marker — type the key name only.');
            lookupKeyInput.style.borderColor = '#a00';
        } else {
            lookupKeyInput.style.borderColor = '';
        }
        if (secretValue.trim().startsWith('ENC(')) {
            errors.push('Secret Value must be the plain secret. Do not paste an ENC(...) encrypted value — type or paste the original password.');
            secretValueInput.style.borderColor = '#a00';
        } else {
            secretValueInput.style.borderColor = '';
        }

        if (errors.length > 0) {
            errDiv.innerHTML = errors.map(function (m) { return '<p style="margin:2px 0">' + m + '</p>'; }).join('');
            errDiv.style.display = 'block';
            e.preventDefault();
        } else {
            errDiv.style.display = 'none';
        }
    });
}());
</script>

<hr/>

<p>${uiLabelMap.WebtoolsSecretCsvUploadInfo}</p>
<ul>
    <li>${uiLabelMap.WebtoolsSecretCsvColumns}</li>
    <li>${uiLabelMap.WebtoolsSecretCsvTargetInfo}</li>
</ul>

<form class="basic-form" method="post" action="<@ofbizUrl>uploadEncryptedSecrets</@ofbizUrl>" enctype="multipart/form-data">
    <table class="basic-table" cellspacing="0">
        <tbody>
            <tr>
                <td class="label">
                    <label>${uiLabelMap.WebtoolsSecretCsvFile}</label>
                </td>
                <td>
                    <input type="file" name="uploadedFile" accept=".csv,text/csv"/>
                </td>
            </tr>
            <tr>
                <td class="label"></td>
                <td>
                    <input type="submit" value="${uiLabelMap.WebtoolsSecretUploadAndEncrypt}"/>
                </td>
            </tr>
        </tbody>
    </table>
</form>
