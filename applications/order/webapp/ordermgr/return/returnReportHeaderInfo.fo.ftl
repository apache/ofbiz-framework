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

          </fo:table-body></fo:table>
        </fo:table-cell></fo:table-row></fo:table-body></fo:table>
</#escape>

