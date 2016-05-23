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

<#assign returnPolicyEnabled = ReturnPolicyEnabled!>
<#assign eBayDetails = EBayDetails!>
<#if eBayDetails?has_content>
    <#assign returnPolicyDetails = eBayDetails.getReturnPolicyDetails()>
</#if>
<#if !returnPolicyEnabled??><#assign not = "not"></#if>
<#assign  title = "Return policy is "+(not!)+" enabled for this category.">

<form name="APIForm" id="APIForm" method="post" action="ReturnPolicyServlet" >
  <table align="center"  border="0">
    <tr><td><img src="ebay.gif" alt="" /></td></tr>
    <tr><td>${title!}</td></tr>
    <tr>
         <td>&nbsp;</td>
    </tr>
    <!-- specify return policy -->
    <#if returnPolicyEnabled! == true>
        <tr align="left">
            <td><b>Returns accepted:</b></td>
        </tr>
        <tr align="left">
                <td>
                        <select name="ReturnsAccepted">
                        <#if returnPolicyDetails?has_content>
                        <#assign retAccpTypeArray = returnPolicyDetails.getReturnsAccepted()!>
                        for(int j = 0; j < retAccpTypeArray.length; j++){
                            
                        <#list retAccpTypeArray as retAccpType>
                            <#assign returnAccepted = "">
                            <option value="${retAccpType.getReturnsAcceptedOption()!}"
                            <#if returnAccepted?? && returnAccepted.equalsIgnoreCase(retAccpType.getReturnsAcceptedOption()!)>selected="select"</#if>>
                            ${retAccpType.getDescription()!}</option>
                        </#list>
                        </#if>
                        </select>
                </td>
        </tr>
        <tr>
             <td></td>
        </tr>
        <tr align="left">
            <td><b>Item must be returned within:</b></td>
        </tr>
        <tr align="left">
            <td>
                <select  name="ReturnsWithin">
                 <#if returnPolicyDetails?has_content>
                 <#assign retWithinArray = returnPolicyDetails.getReturnsWithin()!>
                 <#list retWithinArray as retWithin>
                    <#assign returnWithin = "">
                    <option value="${retWithin.getReturnsWithinOption()!}"
                    <#if returnWithin?? && returnWithin.equalsIgnoreCase(retWithin.getReturnsWithinOption()!)>selected="select"</#if>>
                    ${retWithin.getDescription()!}</option>
                </#list>
                </#if>
                </select>
            </td>
        </tr>
        <tr>
             <td></td>
        </tr>
        <tr align="left">
            <td>
                <b>Refund will be given as:</b>
            </td>
        </tr>
        <tr align="left">
            <td>
                <select  name="Refund">
                 <#if returnPolicyDetails?has_content>
                 <#assign refundArray = returnPolicyDetails.getRefund()!>
                 <#list refundArray as refundAr>
                    <#assign refund = "">
                    <option value="${refundAr.getRefundOption()!}"
                    <#if refund?? && refund.equalsIgnoreCase(refundAr.getRefundOption())>selected="select"</#if>>
                    ${refundArray[j].getDescription()!}</option>
                 </#list>
                 </#if>
                </select>
            </td>
        </tr>
        <tr>
             <td></td>
        </tr>
        <tr align="left">
            <td><b>Return shipping will be paid by:</b>
            </td>
        </tr>
        <tr align="left">
            <td>
                <select name="ShippingCostPaidBy">
                <#if returnPolicyDetails?has_content>
                <#assign paidByArray = returnPolicyDetails.getShippingCostPaidBy()!>
                <#list paidByArray as paidBy>
                    <#assign shippingCostPaidBy = "">
                    <option value="${paidBy.getShippingCostPaidByOption()!}"
                    <#if shippingCostPaidBy?? && shippingCostPaidBy.equalsIgnoreCase(paidBy.getShippingCostPaidByOption()!)>selected="select"</#if>>
                    ${paidBy.getDescription()!}</option>
                </#list>
                </#if>
                </select>
            </td>
        </tr>
        <tr>
             <td></td>
        </tr>
        <tr align="left">
            <td>
                <b>Return policy details(500 character limit):</b>
            </td>
        </tr>
        <tr align="left">
            <td>
                <textarea name="ReturnPolicyDetailsDescription" cols="70" rows="6"<#if !returnPolicyDetails.isDescription().booleanValue()>disabled="true"</#if>></textarea>
            </td>
        </tr>
        <tr>
            <td></td>
        </tr>
        <tr>
            <td>
                <font size="2" face="Verdana,Geneva,Arial,Helvetica" color="#666666" style="font-size:11px">- Specify a return policy. <a target=_new HREF="http://pages.ebay.com/help/sell/contextual/return-policy.html" onclick="if(window.openContextualHelpWindow){return openContextualHelpWindow(this.href)}" target="helpwin">Learn More</a>.</font>
            </td>
        </tr>
    </#if>
    <tr>
         <td>&nbsp;</td>
    </tr>
    <tr>
      <td align="center">
        <input type="submit" name="btSubmit" id="btSubmit" value="Continue"/>
      </td>
    </tr>
  </table>
</form>
