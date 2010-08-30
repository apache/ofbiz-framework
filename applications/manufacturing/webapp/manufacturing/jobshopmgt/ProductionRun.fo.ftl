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
<#if productionRunId?has_content>
            <fo:list-block provisional-distance-between-starts="40mm">
                <fo:list-item>
                    <fo:list-item-label><fo:block font-size="12">${uiLabelMap.ManufacturingProductionRunId}</fo:block></fo:list-item-label>
                    <fo:list-item-body start-indent="body-start()"><fo:block><fo:inline font-size="14" font-weight="bold" space-end="5mm">${productionRunData.workEffortId?if_exists}</fo:inline><#if productionRunData.productionRunName?exists> ${productionRunData.productionRunName}</#if></fo:block></fo:list-item-body>
                </fo:list-item>
                <fo:list-item>
                    <fo:list-item-label><fo:block font-size="12">${uiLabelMap.ProductProductId}</fo:block></fo:list-item-label>
                    <fo:list-item-body start-indent="body-start()"><fo:block><fo:inline font-size="14" font-weight="bold" space-end="5mm">${productionRunData.productId?if_exists}</fo:inline><#if productionRunData.product.productName?exists> ${productionRunData.product.productName}</#if></fo:block></fo:list-item-body>
                </fo:list-item>
            </fo:list-block>
            <fo:block><fo:leader leader-length="100%" leader-pattern="rule" rule-style="solid" rule-thickness="0.1mm" color="black"/></fo:block>
            <fo:table>
                <fo:table-column column-width="9cm"/>
                <fo:table-column column-width="9cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell padding="2pt">
                            <fo:list-block provisional-distance-between-starts="50mm">
                                <fo:list-item>
                                    <fo:list-item-label><fo:block>${uiLabelMap.ManufacturingQuantityToProduce}</fo:block></fo:list-item-label>
                                    <fo:list-item-body start-indent="body-start()"><fo:block>${productionRunData.quantityToProduce?if_exists}</fo:block></fo:list-item-body>
                                </fo:list-item>
                                <fo:list-item space-after="5mm">
                                    <fo:list-item-label><fo:block>${uiLabelMap.ManufacturingQuantityRemaining}</fo:block></fo:list-item-label>
                                    <fo:list-item-body start-indent="body-start()"><fo:block>${quantityRemaining?if_exists}</fo:block></fo:list-item-body>
                                </fo:list-item>
                                <fo:list-item>
                                    <fo:list-item-label><fo:block>${uiLabelMap.ManufacturingEstimatedStartDate}</fo:block></fo:list-item-label>
                                    <fo:list-item-body start-indent="body-start()"><fo:block><#if productionRunData.estimatedStartDate?exists>${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(productionRunData.estimatedStartDate, "dd/MM/yyyy")}</#if></fo:block></fo:list-item-body>
                                </fo:list-item>
                                <fo:list-item>
                                    <fo:list-item-label><fo:block>${uiLabelMap.ManufacturingEstimatedCompletionDate}</fo:block></fo:list-item-label>
                                    <fo:list-item-body start-indent="body-start()"><fo:block><#if productionRunData.estimatedCompletionDate?exists>${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(productionRunData.estimatedCompletionDate, "dd/MM/yyyy")}</#if></fo:block></fo:list-item-body>
                                </fo:list-item>
                            </fo:list-block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:list-block provisional-distance-between-starts="50mm">
                                <fo:list-item>
                                    <fo:list-item-label><fo:block>${uiLabelMap.ManufacturingQuantityProduced}</fo:block></fo:list-item-label>
                                    <fo:list-item-body start-indent="body-start()"><fo:block>${productionRunData.quantityProduced?if_exists}</fo:block></fo:list-item-body>
                                </fo:list-item>
                                <fo:list-item space-after="5mm">
                                    <fo:list-item-label><fo:block>${uiLabelMap.ManufacturingQuantityRejected}</fo:block></fo:list-item-label>
                                    <fo:list-item-body start-indent="body-start()"><fo:block>${quantityRejected?if_exists}</fo:block></fo:list-item-body>
                                </fo:list-item>
                                <fo:list-item>
                                    <fo:list-item-label><fo:block>${uiLabelMap.CommonStartDate}</fo:block></fo:list-item-label>
                                    <fo:list-item-body start-indent="body-start()"><fo:block><#if productionRunData.actualStartDate?exists>${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(productionRunData.actualStartDate, "dd/MM/yyyy")}</#if></fo:block></fo:list-item-body>
                                </fo:list-item>
                                <fo:list-item>
                                    <fo:list-item-label><fo:block>${uiLabelMap.ManufacturingActualCompletionDate}</fo:block></fo:list-item-label>
                                    <fo:list-item-body start-indent="body-start()"><fo:block><#if productionRunData.actualCompletionDate?exists>${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(productionRunData.actualCompletionDate, "dd/MM/yyyy")}</#if></fo:block></fo:list-item-body>
                                </fo:list-item>
                            </fo:list-block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
            <fo:block space-after.optimum="0.3cm"><fo:leader leader-length="100%" leader-pattern="rule" rule-style="solid" rule-thickness="0.1mm" color="black"/></fo:block>

            <#-- Components   -->
            <#if productionRunComponentsData?has_content>
            <fo:table width="22cm">
              <fo:table-column column-width="20%"/>
              <fo:table-column column-width="40%"/>
              <fo:table-column column-width="10%"/>
              <fo:table-column column-width="10%"/>
              <fo:table-column column-width="10%"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell><fo:block>${uiLabelMap.ProductProductId}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.CommonDescription}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingQuantity}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingIssuedQuantity}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingQuantityRemaining}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#list productionRunComponentsData as productionRunComponentData>

                    <#assign resQuantityComp = productionRunComponentData.estimatedQuantity - productionRunComponentData.issuedQuantity>

                       <fo:table-row>
                            <fo:table-cell>
                                <fo:block>${productionRunComponentData.productId?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block>${productionRunComponentData.internalName?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block>${productionRunComponentData.estimatedQuantity?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block>${productionRunComponentData.issuedQuantity?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block>${resQuantityComp?if_exists}</fo:block>
                             </fo:table-cell>
                        </fo:table-row>
                    </#list>
                </fo:table-body>
            </fo:table>
            </#if>
            <fo:block space-after.optimum="0.3cm"><fo:leader leader-length="100%" leader-pattern="rule" rule-style="solid" rule-thickness="0.1mm" color="black"/></fo:block>

            <#-- Tasks   -->
            <#if productionRunRoutingTasks?has_content>
            <fo:table width="100%">
              <fo:table-column column-width="20%"/>
              <fo:table-column column-width="30%"/>
              <fo:table-column column-width="30%"/>
              <fo:table-column column-width="10%"/>
              <fo:table-column column-width="10%"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell><fo:block>${uiLabelMap.FixedAsset}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingRoutingTask}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.CommonDescription}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingTaskEstimatedSetupMillis}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingTaskEstimatedMilliSeconds}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#list productionRunRoutingTasks as productionRunRoutingTask>
                        <fo:table-row>
                            <fo:table-cell>
                                <fo:block>${productionRunRoutingTask.fixedAssetId?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block>${productionRunRoutingTask.workEffortName?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block>${productionRunRoutingTask.description?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block>${productionRunRoutingTask.estimatedSetupMillis?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block>${productionRunRoutingTask.estimatedMilliSeconds?if_exists}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#list>
                </fo:table-body>
            </fo:table>
            </#if>
            <fo:block space-after.optimum="0.3cm"><fo:leader leader-length="100%" leader-pattern="rule" rule-style="solid" rule-thickness="0.1mm" color="black"/></fo:block>

            <#if productionRunContents?has_content>
            <fo:block space-after.optimum="0.3cm"></fo:block>
            <fo:table>
              <fo:table-column column-width="9cm"/>
              <fo:table-column column-width="9cm"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell><fo:block>Documents</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>Link</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#list productionRunContents as productionRunContent>
                        <fo:table-row>
                            <fo:table-cell padding="2pt">
                                <fo:block>${productionRunContent.contentName?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block><fo:basic-link background-color="lightblue" external-destination="<@ofbizContentUrl>/content/control/ViewBinaryDataResource?dataResourceId=${productionRunContent.drDataResourceId}</@ofbizContentUrl>">${uiLabelMap.CommonView}</fo:basic-link></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#list>
                </fo:table-body>
            </fo:table>
            </#if>
</#if>
</#escape>
