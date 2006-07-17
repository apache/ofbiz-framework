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
<?xml version="1.0" encoding="UTF-8"?>

<#-- Generates PDF of return invoice -->
<#-- A great XSL:FO tutorial is at http://www.xml.com/pub/a/2001/01/17/xsl-fo/ -->

<#assign fromPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", returnHeader.fromPartyId, "compareDate", returnHeader.entryDate, "userLogin", userLogin))/>
<#assign toPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", returnHeader.toPartyId, "compareDate", returnHeader.entryDate, "userLogin", userLogin))/>

<#macro displayReturnAdjustment returnAdjustment>
    <#assign returnHeader = returnAdjustment.getRelatedOne("ReturnHeader")>
    <#assign adjReturnType = returnAdjustment.getRelatedOne("ReturnType")?if_exists>
    <fo:table-row>
    <fo:table-cell padding="1mm"/>
    <fo:table-cell padding="1mm"/>
    <fo:table-cell number-columns-spanned="3" padding="1mm">
      <fo:block wrap-option="wrap">
        <#if returnAdjustment.comments?has_content>${returnAdjustment.comments}<#else>${returnAdjustment.description?default("N/A")}</#if>
      </fo:block>
    </fo:table-cell>
    <fo:table-cell padding="1mm" text-align="right"><fo:block><@ofbizCurrency amount=returnAdjustment.amount isoCode=returnHeader.currencyUomId/></fo:block></fo:table-cell>
    </fo:table-row>
    <#assign total = total + returnAdjustment.get("amount")>
</#macro>

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <fo:layout-master-set>

    <fo:simple-page-master master-name="return-summary"
        margin-top="1in" margin-bottom="1in"
        margin-left="1in" margin-right="1in">
      <fo:region-body margin-top="3in" margin-bottom="0.5in"/>  <#-- main body with parties, list of returned items and total -->
      <fo:region-before extent="3in"/>  <#-- header with logo and date/returnId -->
      <fo:region-after extent="0.5in"/>  <#-- footer with page number and caption -->
    </fo:simple-page-master>

  </fo:layout-master-set>

  <fo:page-sequence master-reference="return-summary">


    <#-- header with logo and date/returnId -->


    <fo:static-content flow-name="xsl-region-before">
      <fo:block font-size="10pt">
      <fo:table>
        <fo:table-column column-width="2in"/>
        <fo:table-column column-width="1in"/>
        <fo:table-column column-width="3in"/>
        <fo:table-body>
        <fo:table-row>

        <fo:table-cell>
             ${screens.render("component://order/widget/ordermgr/OrderPrintForms.xml#CompanyLogo")}
        </fo:table-cell>

        <fo:table-cell/>

        <fo:table-cell>
          <fo:table><fo:table-column column-width="0.3in"/><fo:table-body><fo:table-row><fo:table-cell>
            <fo:table font-size="10pt">
            <fo:table-column column-width="1in"/>
            <fo:table-column column-width="1in"/>
            <fo:table-column column-width="1in"/>
            <fo:table-body>

            <fo:table-row>
              <fo:table-cell number-columns-spanned="3">
                <fo:block space-after="2mm" font-size="14pt" font-weight="bold" text-align="right">${uiLabelMap.OrderReturnSummary}</fo:block>
              </fo:table-cell>
            </fo:table-row>

            <fo:table-row>
              <fo:table-cell text-align="center" border-style="solid" border-width="0.2pt">
                <fo:block padding="1mm" font-weight="bold">${uiLabelMap.CommonDate}</fo:block>
              </fo:table-cell>
              <fo:table-cell text-align="center" border-style="solid" border-width="0.2pt">
                <fo:block padding="1mm" font-weight="bold">${uiLabelMap.OrderReturnId}</fo:block>
              </fo:table-cell>
              <fo:table-cell text-align="center" border-style="solid" border-width="0.2pt">
                <fo:block padding="1mm" font-weight="bold">${uiLabelMap.CommonStatus}</fo:block>
              </fo:table-cell>
            </fo:table-row>
                                  
            <fo:table-row>
              <fo:table-cell text-align="center" border-style="solid" border-width="0.2pt">
                <fo:block padding="1mm">${entryDate?string("yyyy-MM-dd")}</fo:block>
              </fo:table-cell>
              <fo:table-cell text-align="center" border-style="solid" border-width="0.2pt">
                <fo:block padding="1mm">${returnId}</fo:block>
              </fo:table-cell>
              <fo:table-cell text-align="center" border-style="solid" border-width="0.2pt">
                <fo:block padding="1mm">${currentStatus.get("description",locale)}</fo:block>
              </fo:table-cell>
            </fo:table-row>

          </fo:table-body>
          </fo:table>
        </fo:table-cell></fo:table-row></fo:table-body></fo:table>
      </fo:table-cell>

      </fo:table-row>
      </fo:table-body>
      </fo:table>
      </fo:block>

      <#-- return from and to -->

      <fo:block font-size="10pt" space-before="5mm">
        <fo:table>
          <fo:table-column column-width="2.75in"/>
          <fo:table-column column-width="0.5in"/>
          <fo:table-column column-width="2.75in"/>
          <fo:table-body>
          <fo:table-row>

            <fo:table-cell>
            <fo:table border-style="solid" border-width="0.2pt" height="1in">
              <fo:table-column column-width="2.75in"/>
              <fo:table-body>
                <fo:table-row><fo:table-cell border-style="solid" border-width="0.2pt" padding="1mm"><fo:block font-weight="bold">${uiLabelMap.OrderReturnFromAddress}</fo:block></fo:table-cell></fo:table-row>
                <fo:table-row><fo:table-cell padding="1mm">
                  <fo:block white-space-collapse="false"><#if fromPartyNameResult.fullName?has_content>${fromPartyNameResult.fullName}<#else/><#if postalAddressFrom?exists><#if (postalAddressFrom.toName)?has_content>${postalAddressFrom.toName}</#if><#if (postalAddressFrom.attnName)?has_content>
${postalAddressFrom.attnName}</#if></#if></#if><#if postalAddressFrom?exists>
${postalAddressFrom.address1}<#if (postalAddressFrom.address2)?has_content>
${postalAddressFrom.address2}</#if>
${postalAddressFrom.city}<#if (postalAddressFrom.stateProvinceGeoId)?has_content>, ${postalAddressFrom.stateProvinceGeoId}</#if><#if (postalAddressFrom.postalCode)?has_content>, ${postalAddressFrom.postalCode}</#if></#if>
                  </fo:block>

                </fo:table-cell></fo:table-row>
              </fo:table-body>
            </fo:table>
            </fo:table-cell>

            <fo:table-cell/>

            <fo:table-cell>
            <fo:table border-style="solid" border-width="0.2pt" height="1in">
              <fo:table-column column-width="2.75in"/>
              <fo:table-body>
                <fo:table-row><fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt"><fo:block font-weight="bold">${uiLabelMap.OrderReturnToAddress}</fo:block></fo:table-cell></fo:table-row>
                <fo:table-row><fo:table-cell padding="1mm">
                  <fo:block white-space-collapse="false"><#if toPartyNameResult.fullName?has_content>${toPartyNameResult.fullName}<#else/><#if postalAddressTo?exists><#if (postalAddressTo.toName)?has_content>${postalAddressTo.toName}</#if><#if (postalAddressTo.attnName)?has_content>
${postalAddressTo.attnName}</#if></#if></#if><#if postalAddressTo?exists>
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
      </fo:block>

        <fo:table height="0.25in" space-before="5mm">
          <fo:table-column column-width="0.85in"/>
          <fo:table-column column-width="0.85in"/>
          <fo:table-column column-width="2in"/>
          <fo:table-column column-width="0.5in"/>
          <fo:table-column column-width="0.85in"/>
          <fo:table-column column-width="0.85in"/>
          <fo:table-body>
            <fo:table-row text-align="center" font-weight="bold">
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.OrderOrderId}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.ProductProductId}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.CommonDescription}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.OrderQty}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.OrderUnitPrice}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.OrderAmount}</fo:block></fo:table-cell>
            </fo:table-row>
          </fo:table-body>
        </fo:table>

    </fo:static-content>


    <#-- footer.  Use it for standard boilerplate text. -->


    <fo:static-content flow-name="xsl-region-after">
      <#-- displays page number.  "theEnd" is an id of a fo:block at the very end -->    
      <fo:block space-before="5mm" font-size="10pt" text-align="center">${uiLabelMap.CommonPage} <fo:page-number/> ${uiLabelMap.CommonOf} <fo:page-number-citation ref-id="theEnd"/></fo:block>
    </fo:static-content>

  
    <#-- main body -->

    
    <fo:flow flow-name="xsl-region-body">

      <#-- Items returned -->
      
      <fo:block font-size="10pt">
        <fo:table border-style="solid" border-width="0.2pt" height="5in">
          <fo:table-column column-width="0.85in"/>
          <fo:table-column column-width="0.85in"/>
          <fo:table-column column-width="2in"/>
          <fo:table-column column-width="0.5in"/>
          <fo:table-column column-width="0.85in"/>
          <fo:table-column column-width="0.85in"/>
          <fo:table-body>

            <#-- each item -->
            <#assign total = 0.0/>
            <#list returnItems as returnItem>
              <fo:table-row>
                <fo:table-cell padding="1mm" font-size="8pt">
                  <fo:block>${returnItem.orderId}</fo:block>
                </fo:table-cell>
                <fo:table-cell padding="1mm" font-size="8pt">
                  <fo:block>
                    <#if returnItem.orderItemSeqId?exists>${returnItem.getRelatedOne("OrderItem").getString("productId")}</#if>
                  </fo:block>
                </fo:table-cell>
                <fo:table-cell padding="1mm"><fo:block wrap-option="wrap">${returnItem.description}</fo:block></fo:table-cell>
                <fo:table-cell padding="1mm" text-align="right"><fo:block>${returnItem.returnQuantity}</fo:block></fo:table-cell>
                <fo:table-cell padding="1mm" text-align="right"><fo:block><@ofbizCurrency amount=returnItem.returnPrice isoCode=returnHeader.currencyUomId/></fo:block></fo:table-cell>
                <fo:table-cell padding="1mm" text-align="right"><fo:block><@ofbizCurrency amount=(returnItem.returnPrice * returnItem.returnQuantity) isoCode=returnHeader.currencyUomId/></fo:block></fo:table-cell>
              </fo:table-row>
              <#assign total = total + returnItem.returnQuantity.doubleValue() * returnItem.returnPrice.doubleValue()/>
              
              <#assign returnItemAdjustments = returnItem.getRelated("ReturnAdjustment")>
              <#if (returnItemAdjustments?has_content)>
                  <#list returnItemAdjustments as returnItemAdjustment>
                     <@displayReturnAdjustment returnAdjustment=returnItemAdjustment/>
                  </#list>
              </#if>
            </#list>
            
            <#-- order level adjustments -->
            <#if (returnAdjustments?has_content)>                  
                <#list returnAdjustments as returnAdjustment>
                    <@displayReturnAdjustment returnAdjustment=returnAdjustment/>
                </#list>
            </#if>

        </fo:table-body>
        </fo:table>
      </fo:block>

      <#-- total -->
        <fo:table space-before="5mm" font-size="10pt">
          <fo:table-column column-width="0.85in"/>
          <fo:table-column column-width="0.85in"/>
          <fo:table-column column-width="2in"/>
          <fo:table-column column-width="0.5in"/>
          <fo:table-column column-width="0.85in"/>
          <fo:table-column column-width="0.85in"/>
          <fo:table-body>
            <fo:table-row>
              <fo:table-cell/>
              <fo:table-cell/>
              <fo:table-cell/>
              <fo:table-cell/>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt">
                <fo:block font-weight="bold" text-align="center">${uiLabelMap.CommonTotal}</fo:block>
              </fo:table-cell>
              <fo:table-cell text-align="right" padding="1mm" border-style="solid" border-width="0.2pt">
                <fo:block><@ofbizCurrency amount=total isoCode=returnHeader.currencyUomId/></fo:block>
              </fo:table-cell>
            </fo:table-row>
          </fo:table-body>
        </fo:table>
      <fo:block id="theEnd"/>  <#-- marks the end of the pages and used to identify page-number at the end -->
    </fo:flow>
  </fo:page-sequence>
</fo:root>
