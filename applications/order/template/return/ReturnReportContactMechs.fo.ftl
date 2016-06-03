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
<#escape x as x?xml>

        <#assign fromPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", returnHeader.fromPartyId, "compareDate", returnHeader.entryDate, "userLogin", userLogin))/>
        <#assign toPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", returnHeader.toPartyId, "compareDate", returnHeader.entryDate, "userLogin", userLogin))/>

        <fo:table>
          <fo:table-column column-width="3.50in"/>
          <fo:table-column column-width="1.00in"/>
          <fo:table-column column-width="2.75in"/>
          <fo:table-body>
          <fo:table-row>

            <fo:table-cell>
            <fo:table table-layout="fixed" border-style="solid" border-width="0.2pt" height="1in">
              <fo:table-column column-width="3.50in"/>
              <fo:table-body>
                <fo:table-row><fo:table-cell border-style="solid" border-width="0.2pt" padding="1mm"><fo:block font-weight="bold">${uiLabelMap.OrderReturnFromAddress}</fo:block></fo:table-cell></fo:table-row>
                <fo:table-row><fo:table-cell padding="1mm">
                  <fo:block white-space-collapse="false"><#if fromPartyNameResult.fullName?has_content>${fromPartyNameResult.fullName}<#else><#if postalAddressFrom??><#if (postalAddressFrom.toName)?has_content>${postalAddressFrom.toName}</#if><#if (postalAddressFrom.attnName)?has_content>
${postalAddressFrom.attnName}</#if></#if></#if><#if postalAddressFrom??>
${postalAddressFrom.address1}<#if (postalAddressFrom.address2)?has_content>
${postalAddressFrom.address2}</#if>
${postalAddressFrom.city}<#if (postalAddressFrom.stateProvinceGeoId)?has_content>, ${postalAddressFrom.stateProvinceGeoId}</#if><#if (postalAddressFrom.postalCode)?has_content>, ${postalAddressFrom.postalCode}</#if></#if>
                  </fo:block>

                </fo:table-cell></fo:table-row>
              </fo:table-body>
            </fo:table>
            </fo:table-cell>

            <fo:table-cell><fo:block/></fo:table-cell>

            <fo:table-cell>
            <fo:table table-layout="fixed" border-style="solid" border-width="0.2pt" height="1in">
              <fo:table-column column-width="2.75in"/>
              <fo:table-body>
                <fo:table-row><fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt"><fo:block font-weight="bold">${uiLabelMap.OrderReturnToAddress}</fo:block></fo:table-cell></fo:table-row>
                <fo:table-row><fo:table-cell padding="1mm">
                  <fo:block white-space-collapse="false"><#if toPartyNameResult.fullName?has_content>${toPartyNameResult.fullName}<fo:block /><#else><#if postalAddressTo??><#if (postalAddressTo.toName)?has_content>${postalAddressTo.toName}</#if><#if (postalAddressTo.attnName)?has_content>
${postalAddressTo.attnName}</#if></#if></#if><#if postalAddressTo??>
${postalAddressTo.address1}<#if (postalAddressTo.address2)?has_content>
${postalAddressTo.address2}</#if>
${postalAddressTo.city}<#if (postalAddressTo.stateProvinceGeoId)?has_content>, ${postalAddressTo.stateProvinceGeoId}</#if><#if (postalAddressTo.postalCode)?has_content>, ${postalAddressTo.postalCode}</#if></#if></fo:block>
                </fo:table-cell></fo:table-row>
              </fo:table-body>
            </fo:table>
            </fo:table-cell>

          </fo:table-row>
          </fo:table-body>
          </fo:table>

          <fo:block space-after="10pt"/>
</#escape>
