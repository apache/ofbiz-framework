<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
        <fo:table font-size="8pt">
           <fo:table-column column-width="4.5in"/>
           <fo:table-column column-width="2in"/>
            <fo:table-body>
              <fo:table-row>
                <fo:table-cell>
                   <fo:block text-align="left">
	            <#if logoImageUrl?has_content>
                    <fo:external-graphic src="${logoImageUrl}" overflow="hidden" height="40px"/>
                </#if>
                  </fo:block>
                </fo:table-cell>
              </fo:table-row>
              <fo:table-row>
                <fo:table-cell>
                  <fo:block>${companyName}</fo:block>
                  <#if postalAddress?exists>
                  <#if postalAddress?has_content>
                    <fo:block>${postalAddress.address1?if_exists}</fo:block>
                    <#if postalAddress.address2?has_content><fo:block>${postalAddress.address2?if_exists}</fo:block></#if>
                    <fo:block>${postalAddress.city?if_exists}, ${stateProvinceAbbrv?if_exists} ${postalAddress.postalCode?if_exists}, ${countryName?if_exists}</fo:block>
                  </#if>
                  
				  <#else>
                  <fo:block>${uiLabelMap.CommonNoPostalAddress}</fo:block>
                  <fo:block>${uiLabelMap.CommonFor}: ${companyName}</fo:block>
                  </#if>
                  <fo:table>
                    <fo:table-column column-width="15mm"/>
                    <fo:table-column column-width="25mm"/>
                    <fo:table-body>
                                  
                    <#if phone?exists>
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.CommonTelephoneAbbr}:</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${phone.countryCode?if_exists}-${phone.areaCode?if_exists}-${phone.contactNumber?if_exists}</fo:block></fo:table-cell>
                    </fo:table-row>
                    </#if>

                    <#if email?exists>
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.CommonEmail}:</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${email.infoString}</fo:block></fo:table-cell>
                    </fo:table-row>
                    </#if>

                    <#if website?exists>
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.CommonWebsite}:</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${website.infoString?if_exists}</fo:block></fo:table-cell>
                    </fo:table-row>
                    </#if>

                    <#if eftAccount?exists>
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.CommonFinBankName}:</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${eftAccount.bankName?if_exists}</fo:block></fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.CommonRouting}:</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${eftAccount.routingNumber?if_exists}</fo:block></fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.CommonBankAccntNrAbbr}:</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${eftAccount.accountNumber?if_exists}</fo:block></fo:table-cell>
                    </fo:table-row>
                    </#if>

                  </fo:table-body>
                </fo:table>             
                </fo:table-cell>
            </fo:table-row>
          </fo:table-body>
        </fo:table>
 
