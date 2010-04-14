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

<#assign returnPolicyEnabled = ReturnPolicyEnabled?if_exists>
<#assign eBayDetails = EBayDetails?if_exists>
<#if eBayDetails?has_content>
    <#assign returnPolicyDetails = eBayDetails.getReturnPolicyDetails()>
</#if>
<#if !returnPolicyEnabled?exists><#assign not = "not"></#if>
<#assign  title = "Return policy is "+not?if_exists+" enabled for this category.">

<form name="APIForm" id="APIForm" method="post" action="ReturnPolicyServlet" >
  <table align="center"  border="0">
    <tr><td><img src="ebay.gif" alt="" /></td></tr>
    <tr><td>${title?if_exists}</td></tr>
    <tr>
         <td>&nbsp;</td>
    </tr>
    <!-- specify return policy -->
    <#if returnPolicyEnabled?if_exists == true>
        <tr align="left">
            <td><b>Returns accepted:</b></td>
        </tr>
        <tr align="left">
                <td>
                        <select name="ReturnsAccepted">
                        <#if returnPolicyDetails?has_content>
                        <#assign retAccpTypeArray = returnPolicyDetails.getReturnsAccepted()?if_exists>
                        for(int j = 0; j < retAccpTypeArray.length; j++){
                            
                        <#list retAccpTypeArray as retAccpType>
                            <#assign returnAccepted = "">
                            <option value="${retAccpType.getReturnsAcceptedOption()?if_exists}"
                            <#if returnAccepted != null && returnAccepted.equalsIgnoreCase(retAccpType.getReturnsAcceptedOption()?if_exists)>selected="select"</#if>>
                            ${retAccpType.getDescription()?if_exists}</option>
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
                 <#assign retWithinArray = returnPolicyDetails.getReturnsWithin()?if_exists>
                 <#list retWithinArray as retWithin>
                    <#assign returnWithin = "">
                    <option value="${retWithin.getReturnsWithinOption()?if_exists}"
                    <#if returnWithin!=null && returnWithin.equalsIgnoreCase(retWithin.getReturnsWithinOption()?if_exists)>selected="select"</#if>>
                    ${retWithin.getDescription()?if_exists}</option>
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
                 <#assign refundArray = returnPolicyDetails.getRefund()?if_exists>
                 <#list refundArray as refundAr>
                    <#assign refund = "">
                    <option value="${refundAr.getRefundOption()?if_exists}"
                    <#if refund!=null && refund.equalsIgnoreCase(refundAr.getRefundOption())>selected="select"</#if>>
                    ${refundArray[j].getDescription()?if_exists}</option>
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
                <#assign paidByArray = returnPolicyDetails.getShippingCostPaidBy()?if_exists>
                <#list paidByArray as paidBy>
                    <#assign shippingCostPaidBy = "">
                    <option value="${paidBy.getShippingCostPaidByOption()?if_exists}"
                    <#if shippingCostPaidBy!=null && shippingCostPaidBy.equalsIgnoreCase(paidBy.getShippingCostPaidByOption()?if_exists)>selected="select"</#if>>
                    ${paidBy.getDescription()?if_exists}</option>
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
                <textarea name="ReturnPolicyDetailsDescription" cols="70" rows="6"<#if !returnPolicyDetails.isDescription()?if_exists.booleanValue()>disabled="true"</#if>></textarea>
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