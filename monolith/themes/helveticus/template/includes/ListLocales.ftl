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
<div id="localeContainer">
    <div id="header">
        <input class="filter" id="search" type="text" placeholder="${uiLabelMap.CommonTypeToFilter}" autofocus/>
        <a href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.CommonCancel}</a>
    </div>
    <div id="locales">
        <#assign availableLocales = Static["org.apache.ofbiz.base.util.UtilMisc"].availableLocales()/>
        <#list availableLocales as availableLocale>
            <#assign langAttr = availableLocale.toString()?replace("_", "-")>
            <#assign langDir = "ltr">
            <#if !Static["java.awt.ComponentOrientation"].getOrientation(availableLocale).isLeftToRight()>
                <#assign langDir = "rtl">
            </#if>
            <div class="locale" lang="${langAttr}" dir="${langDir}">
                <a href="<@ofbizUrl>setSessionLocale?newLocale=${availableLocale.toString()}</@ofbizUrl>">
                    ${availableLocale.getDisplayName(availableLocale)}&nbsp;[${langAttr}]
                </a>
            </div>
        </#list>
    </div>
</div>

<script>
    $(document).ready( function() {
        $('#search').on('keyup', function() {
            const value = $(this).val().toLowerCase();
            $('#locales .locale').filter(function() {
                const t = $(this);
                t.toggle(t.text().toLowerCase().indexOf(value) > -1)
            });
        });
    });
</script>

<style>
    /* always reserve scrollbar width, to prevent flickering when filtering locales and scrollbar appears/disappears */
    html {
        scrollbar-gutter: stable;
    }

    body {
        display: flex;
        flex-direction: column;
        padding: 2rem;
        background-color: #181c32 !important;

        #localeContainer {
            display: flex;
            flex-direction: column;
            align-items: center;

            #header {
                display: flex;
                flex-direction: column;
                align-items: center;
                width: 100%;
                max-width: 52rem;
                margin: 2rem 0;

                input {
                    height: 4rem;
                    font-size: 3rem;
                    padding: 2rem;
                    background-color: #3F4254;
                    color: white;
                }

                a {
                    display: inline-block;
                    font-size: 2rem;
                    margin-top: 2rem;
                    color: white;

                    &:hover {
                        text-decoration: underline;
                    }
                }
            }

            #locales {
                display: flex;
                justify-content: space-between;
                flex-wrap: wrap;
                gap: 1rem;

                /* avoid a last line with big gaps between locales */
                &::after {
                    content: "";
                    flex: auto;
                }

                .locale {
                    width: fit-content;
                    border: 1px solid #dfe0e4;
                    border-radius: 0.5rem;
                    transition: all 0.1s ease-in-out;
                    background-color: #3F4254;
                    font-size: 1.6rem;

                    &:hover {
                        background-color: #e1f0ff;
                        border: 1px solid #e1f0ff;
                    }

                    a {
                        display: inline-block;
                        padding: 1rem;
                        color: white;
                        font-weight: bold;
                        cursor: pointer;

                        &:hover {
                            color:  #181c32
                        }
                    }
                }
            }
        }
    }
</style>
