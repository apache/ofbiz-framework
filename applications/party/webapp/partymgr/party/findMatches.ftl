<#--
 *  Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
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
 * @author     Andy Zeneski
 * @created    July 12, 2002
 * @version    1.0
 */
-->

<br/>
<TABLE border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <TR>
    <TD width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">${uiLabelMap.PageTitleAddressMatches}</div>
          </td>
          <td valign="middle" align="right">
            <a href="<@ofbizUrl>addressMatchMap</@ofbizUrl>" class="submenutextright">${uiLabelMap.PageTitleAddressMatchMap}</a>
          </td>
        </tr>
      </table>
    </TD>
  </TR>
  <TR>
    <TD width="100%" >
      <center>
      <table border="0" cellspacing="0" cellpadding="2" class="boxbottom">
        <form name="matchform" method="post" action="<@ofbizUrl>findAddressMatch?match=true</@ofbizUrl>">
          <tr>
            <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyLastName} :</div></td>
            <td width="5%">&nbsp;</td>
            <td><input type="text" class="inputBox" name="lastName" value="${parameters.lastName?if_exists}"/>*</td>
          </tr>
          <tr>
            <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyFirstName} :</div></td>
            <td width="5%">&nbsp;</td>
            <td><input type="text" class="inputBox" name="firstName" value="${parameters.firstName?if_exists}"/>*</td>
          </tr>
          <tr>
            <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.CommonAddress1} :</div></td>
            <td width="5%">&nbsp;</td>
            <td><input type="text" class="inputBox" name="address1" value="${parameters.address1?if_exists}"/>*</td>
          </tr>
          <tr>
            <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.CommonAddress2} :</div></td>
            <td width="5%">&nbsp;</td>
            <td><input type="text" class="inputBox" name="address2" value="${parameters.address2?if_exists}"/></td>
          </tr>
          <tr>
            <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.CommonCity} :</div></td>
            <td width="5%">&nbsp;</td>
            <td><input type="text" class="inputBox" name="city" value="${parameters.city?if_exists}"/>*</td>
          </tr>
          <tr>
            <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.CommonStateProvince} :</div></td>
            <td width="5%">&nbsp;</td>
            <td>
              <select name="stateProvinceGeoId" class="selectBox">
                <#if currentStateGeo?has_content>
                  <option value="${currentStateGeo.geoId}">${currentStateGeo.geoName?default(currentStateGeo.geoId)}</option>
                  <option value="${currentStateGeo.geoId}">---</option>
                </#if>
                <option value="ANY">${uiLabelMap.CommonAny} ${uiLabelMap.CommonStateProvince}</option>
                ${screens.render("component://common/widget/CommonScreens.xml#states")}
              </select>
            </td>
          </tr>
          <tr>
            <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyZipCode} :</div></td>
            <td width="5%">&nbsp;</td>
            <td><input type="text" class="inputBox" name="postalCode" value="${parameters.postalCode?if_exists}"/>*</td>
          </tr>
          <tr>
            <td colspan="2">&nbsp;</td>
            <td><input type="submit" value="${uiLabelMap.PageTitleFindMatches}" class="smallSubmit"></td>
          </tr>
        </form>
        <tr><td colspan="5">&nbsp;</td></tr>

        <tr><td colspan="5">&nbsp;</td></tr>
        <#if match?has_content>
          <tr>
            <td colspan="5">
              <table border="0" cellspacing="5" cellpadding="5" width="100%">
                  <tr>
                    <td align="center" colspan="7">
                      <div class="tabletext"><font color="blue"><b>${uiLabelMap.PartyAddressMatching}:</b> ${lastName} / ${firstName} @ ${addressString}</font></div>
                    </td>
                  </tr>
                  <tr>
                    <td><div class="tableheadtext">${uiLabelMap.PartyLastName}</td>
                    <td><div class="tableheadtext">${uiLabelMap.PartyFirstName}</td>
                    <td><div class="tableheadtext">${uiLabelMap.CommonAddress1}</td>
                    <td><div class="tableheadtext">${uiLabelMap.CommonAddress2}</td>
                    <td><div class="tableheadtext">${uiLabelMap.CommonCity}</td>
                    <td><div class="tableheadtext">${uiLabelMap.PartyZipCode}</td>
                    <td><div class="tableheadtext">${uiLabelMap.PartyPartyId}</td>
                  </tr>
                <#if matches?has_content>
                  <#list matches as match>
                    <#assign person = match.getRelatedOne("Party").getRelatedOne("Person")?if_exists>
                    <#assign group = match.getRelatedOne("Party").getRelatedOne("PartyGroup")?if_exists>
                    <tr>
                      <#if person?has_content>
                        <td><div class="tabletext">${person.lastName}</td>
                        <td><div class="tabletext">${person.firstName}</td>
                      <#elseif group?has_content>
                        <td colspan="2"><div class="tabletext">${group.groupName}</td>
                      <#else>
                        <td colspan="2"><div class="tabletext">[unknown partyType]</td>
                      </#if>
                      <td><div class="tabletext">${Static["org.ofbiz.party.party.PartyWorker"].makeMatchingString(delegator, match.address1)}</td>
                      <td><div class="tabletext">${Static["org.ofbiz.party.party.PartyWorker"].makeMatchingString(delegator, match.address2?default("N/A"))}</td>
                      <td><div class="tabletext">${match.city}</td>
                      <td><div class="tabletext">${match.postalCode}</td>
                      <td><a href="<@ofbizUrl>viewprofile?partyId=${match.partyId}</@ofbizUrl>" class="buttontext">${match.partyId}</a></td>
                    </tr>
                  </#list>
                <#else>
                  <tr><td align="center" colspan="7"><div class="tabletext">${uiLabelMap.PartyNoMatch}</div></td></tr>
                </#if>
              </table>
            </td>
          </tr>
        </#if>
      </table>
      </center>
    </TD>
  </TR>

</TABLE>

