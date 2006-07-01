<#--
 *  Copyright (c) 2004-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@version    $Rev$
 *@since      3.2
-->

<div class="tabletext"><b>${uiLabelMap.ProductQuickCreateVirtualFromVariants}</b></div>
<form action="<@ofbizUrl>quickCreateVirtualWithVariants</@ofbizUrl>" method="post" style="margin: 0;" name="quickCreateVirtualWithVariants">
    <div>
        <span class="tabletext">${uiLabelMap.ProductVariantProductIds}:</span>
        <textarea name="variantProductIdsBag" rows="6" cols="20"></textarea>
    </div>
    <div>
        <span class="tabletext">Hazmat:</span>
        <select name="productFeatureIdOne" class="standardSelect">
            <option value="">- ${uiLabelMap.CommonNone} -</option>
            <#list hazmatFeatures as hazmatFeature>
                <option value="${hazmatFeature.productFeatureId}">${hazmatFeature.description}</option>
            </#list>
        </select>
        <input type="submit" class="smallSubmit" value="${uiLabelMap.ProductCreateVirtualProduct}"/>
    </div>
</form>
