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
<div class="page-title"><span>${uiLabelMap.WebtoolsModelInduceFromDb}</span></div>
<form class="basic-form" method="post" action="<@ofbizUrl>CreateModelInduceFromDb</@ofbizUrl>">
    <table class="basic-table" cellspacing="0">
        <tbody>
            </tr>
                <td class="label">
                    <label>${uiLabelMap.ModelInduceDatasourceName}</label>
                </td>
                <td>
                    <input type="text" name="datasourceName"/>
                </td>
            </tr>
            <tr>
                <td class="label">
                    <label>${uiLabelMap.ModelInducePackageName}</label>
                </td>
                <td>
                    <input type="text" name="packageName"/>
                </td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <select name="induceType">
                        <option value="entitymodel">${uiLabelMap.ModelInduceEntityModel}</option>
                        <option value="entitygroup">${uiLabelMap.ModelInduceEntityGroup}</option>
                    </select>
                </td>
            </tr>
            <tr>
                <td>
                    <input type="submit" name="submitButton"/>
                </td><td></td>
            </tr>
        </tbody>
    </table>
</form>
</hr>
<div>${uiLabelMap.typeAndTitleDisclaimer}</div>
<div>${uiLabelMap.pleaseAddManually}</div>
</hr>
<div>
    <textarea cols="60" rows="50" name="${uiLabelMap.ModelInduceInducedText}">${inducedText!}</textarea>
</div>
