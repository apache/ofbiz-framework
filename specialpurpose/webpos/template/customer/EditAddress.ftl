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
<div id="editAddress" style="display:none">
  <table border="0" width="100%">
    <tr>
      <td width="100%" colspan="4">
        <br />
      </td>
    </tr>
    <tr>
      <td width="15%">
        <b>${uiLabelMap.PartyLastName}</b>
      </td>
      <td width="35%">
        <input type="hidden" id="partyId" name="partyId" value="" />
        <input type="hidden" id="contactMechId" name="contactMechId" value="" />
        <input type="hidden" id="contactMechPurposeTypeId" name="contactMechPurposeTypeId" value="" />
        <input type="text" id="personLastName" name="personLastName" value="" size="40" />
      </td>
      <td width="15%">
        <b>${uiLabelMap.PartyFirstName}</b>
      </td>
      <td width="35%">
        <input type="text" id="personFirstName" name="personFirstName" value="" size="40" />
      </td>
    </tr>
    <tr>
      <td width="15%">
        <b>${uiLabelMap.PartyAddressLine1}</b>
      </td>
      <td width="35%">
        <input type="text" id="personAddress1" name="personAddress1" value="" size="40" />
      </td>
      <td width="15%">
        <b>${uiLabelMap.PartyAddressLine2}</b>
      </td>
      <td width="35%">
        <input type="text" id="personAddress2" name="personAddress2" value="" size="40" />
      </td>
    </tr>
    <tr>
      <td width="15%">
        <b>${uiLabelMap.CommonCountry}</b>
      </td>
      <td width="35%">
        <input type="text" id="countryProvinceGeo" name="countryProvinceGeo" value="" />
      </td>
      <td width="15%">
        <b>${uiLabelMap.PartyState}</b>
      </td>
      <td width="35%">
        <input type="text" id="stateProvinceGeo" name="stateProvinceGeo" value="" />
      </td>
    </tr>
    <tr>
      <td width="15%">
        <b>${uiLabelMap.PartyCity}</b>
      </td>
      <td width="35%">
        <input type="text" id="personCity" name="personCity" value="" size="40" />
      </td>
      <td width="15%">
        <b>${uiLabelMap.PartyPostalCode}</b>
      </td>
      <td width="35%">
        <input type="text" id="personPostalCode" name="personPostalCode" value="" size="7" />
      </td>
    </tr>
    <tr>
      <td width="100%" colspan="4">
        <br />
      </td>
    </tr>
    <tr>
      <td width="100%" colspan="4" align="center">
        <input type="submit" id="editAddressCreateUpdate" name="editAddressCreateUpdate" value="${uiLabelMap.CommonCreate}"/>
        &nbsp;
        <input type="submit" id="editAddressCancel" name="editAddressCancel" value="${uiLabelMap.CommonCancel}"/>
      </td>
    </tr>
  </table>
</div>