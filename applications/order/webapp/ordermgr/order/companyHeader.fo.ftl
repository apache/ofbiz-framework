<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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
-->
        <fo:table font-size="8pt"  font-family="sans-serif">
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
                      <fo:table-cell><fo:block>${uiLabelMap.CommonTelephoneAbbr}:Tel:</fo:block></fo:table-cell>
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
 
