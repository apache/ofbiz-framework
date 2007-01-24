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

<div class="tabletext"><b>${uiLabelMap.ProductQuickCreateVirtualFromVariants}</b></div>
<form action="<@ofbizUrl>quickCreateVirtualWithVariants</@ofbizUrl>" method="post" style="margin: 0;" name="quickCreateVirtualWithVariants">
    <div>
        <span class="tabletext">${uiLabelMap.ProductVariantProductIds}:</span>
        <textarea name="variantProductIdsBag" rows="6" cols="20"></textarea>
    </div>
    <div>
        <span class="tabletext">Hazmat:</span>
        <select name="productFeatureIdOne">
            <option value="">- ${uiLabelMap.CommonNone} -</option>
            <#list hazmatFeatures as hazmatFeature>
                <option value="${hazmatFeature.productFeatureId}">${hazmatFeature.description}</option>
            </#list>
        </select>
        <input type="submit" value="${uiLabelMap.ProductCreateVirtualProduct}"/>
    </div>
</form>
