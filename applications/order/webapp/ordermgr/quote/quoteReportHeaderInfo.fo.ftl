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

        <fo:table border-spacing="3pt">
            <fo:table-column column-width="1.5in"/>
            <fo:table-column column-width="3.75in"/>
            <fo:table-body>
                <fo:table-row>
                    <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OrderQuoteType}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block font-weight="bold">${(quoteType.get("description",locale))?default(quote.quoteTypeId?if_exists)}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block>${uiLabelMap.OrderOrderQuoteIssueDate}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${(quote.issueDate.toString())?if_exists}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block>${uiLabelMap.OrderOrderQuoteId}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${quote.quoteId}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block>${uiLabelMap.CommonStatus}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block font-weight="bold">${(statusItem.get("description", locale))?default(quote.statusId?if_exists)}</fo:block></fo:table-cell>
                </fo:table-row>
            </fo:table-body>
        </fo:table>
