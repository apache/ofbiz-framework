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

<div id="panel">
    <form method="post" action="<@ofbizUrl>CloseTerminal</@ofbizUrl>" name="CloseTerminalForm">
        <table border="0">
            <tr>
                <td colspan="2"">&nbsp;</td>
            </tr>
            <tr>
                <td><b>${uiLabelMap.WebPosManagerCloseTerminalCashAmount}</b></td>
                <td><input type="text" name="endingDrawerCashAmount" id="endingDrawerCashAmount" value="${parameters.endingDrawerCashAmount?default("")}"/></td>
            </tr>
            <tr>
                <td><b>${uiLabelMap.WebPosManagerCloseTerminalCheckAmount}</b></td>
                <td><input type="text" name="endingDrawerCheckAmount" id="endingDrawerCheckAmount" value="${parameters.endingDrawerCheckAmount?default("")}"/></td>
            </tr>
            <tr>
                <td><b>${uiLabelMap.WebPosManagerCloseTerminalCcAmount}</b></td>
                <td><input type="text" name="endingDrawerCcAmount" id="endingDrawerCcAmount" value="${parameters.endingDrawerCcAmount?default("")}"/></td>
            </tr>
            <tr>
                <td><b>${uiLabelMap.WebPosManagerCloseTerminalGcAmount}</b></td>
                <td><input type="text" name="endingDrawerGcAmount" id="endingDrawerGcAmount" value="${parameters.endingDrawerGcAmount?default("")}"/></td>
            </tr>
            <tr>
                <td><b>${uiLabelMap.WebPosManagerCloseTerminalOtherAmount}</b></td>
                <td><input type="text" name="endingDrawerOtherAmount" id="endingDrawerOtherAmount" value="${parameters.endingDrawerOtherAmount?default("")}"/></td>
            </tr>
            <tr>
                <td colspan="2"">&nbsp;</td>
            </tr>
            <tr>
                <td colspan="2" align="center">
                    <input type="submit" value="${uiLabelMap.CommonConfirm}" name="confirm"/>
                    <input type="submit" value="${uiLabelMap.CommonCancel}"/>
                </td>
            </tr>
        </table>
    </form>
</div>
<script language="javascript" type="text/javascript">
    document.CloseTerminalForm.endingDrawerCashAmount.focus();
</script>