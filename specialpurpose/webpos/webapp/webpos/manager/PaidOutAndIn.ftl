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
<div id="paidOutAndIn" style="display:none">
  <input type="hidden" id="paidType" name="type"/>
  <table border="0" width="100%">
    <tr>
      <td colspan="2"">&nbsp;</td>
    </tr>
    <tr>
      <td width="50%" align="right">
        <span id="amountPaidIn" style="display:none">${uiLabelMap.WebPosManagerPaidInAmount}</span>
        <span id="amountPaidOut" style="display:none">${uiLabelMap.WebPosManagerPaidOutAmount}</span>
      </td>
      <td width="50%" align="left"><input type="text" id="amountInOut" name="amountInOut" value=""/></td>
    </tr>
    <tr>
      <td width="50%" align="right">${uiLabelMap.WebPosManagerPaidOutAndIndReason}</td>
      <td width="50%" align="left">
        <div id="reasonIn" style="display:none">
          <select id="reasIn" name="reasonIn">
            <#list paidReasonIn as reason>
              <option value="${reason.enumId}">${reason.get("description", locale)?default(reason.enumId)}</option>
            </#list>
          </select>
        </div>
        <div id="reasonOut" style="display:none">
          <select id="reasOut" name="reasonOut">
            <#list paidReasonOut as reason>
              <option value="${reason.enumId}">${reason.get("description", locale)?default(reason.enumId)}</option>
            </#list>
          </select>
        </div>
      </td>
    </tr>
    <tr>
      <td width="50%" align="right">${uiLabelMap.WebPosManagerPaidOutAndIndReasonComment}</td>
      <td width="50%" align="left"><input type="text" id="reasonCommentInOut" name="reasonCommentInOut" value=""/></td>
    </tr>
    <tr>
      <td colspan="2"">&nbsp;</td>
    </tr>
    <tr>
      <td colspan="2" align="center">
        <input type="submit" value="${uiLabelMap.CommonConfirm}" id="paidOutAndInConfirm"/>
        <input type="submit" value="${uiLabelMap.CommonCancel}" id="paidOutAndInCancel"/>
      </td>
    </tr>
    <tr>
      <td colspan="2"><div class="errorPosMessage"><span id="paidOutAndInFormServerError"/></div></td>
    </tr>
  </table>
</div>