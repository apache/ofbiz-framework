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
<#if results.itemsAwaitingFeedback?has_content>
<script type="text/javascript">
    var active = "";
    function overStar(obj){
        if(obj.src.match("starActive")){
            active = obj.src;
        }else{
            active = "";
        }
        obj.src = "/images/rate/starHover.gif";
    }
    function outStar(obj){
        if(active != "" || obj.src == "/images/rate/starActive.gif"){
            obj.src = "/images/rate/starActive.gif";
        }else{
            obj.src = "/images/rate/starDefault.gif";
        }
    }
    function setRate(name,numstar,obj,feedbackCounter){
        for(i=1; i<=5; i++){
            id = "star"+name+feedbackCounter+"_"+i;
            if(numstar>=i){
                document.getElementById(id).src = "/images/rate/starActive.gif";
            }else{
                document.getElementById(id).src = "/images/rate/starDefault.gif";
            }
        }
        
        active = "/images/rate/starActive.gif";
        setTo = name+feedbackCounter;
        document.getElementById(setTo).value = numstar;
    }
</script>
<form name="leaveFeedback" action="<@ofbizUrl>sendLeaveFeedback</@ofbizUrl>" method="post">
    <input type="hidden" value="${parameters.productStoreId}" name="productStoreId"/>
<table cellspacing="0" class="basic-table">
    <tbody>
        <tr class="header-row">
            <td>Item</td>
            <td width="600">Rate this transaction</td>
            <td>Role</td>
        </tr>
        <#assign row = "alternate-row">
        <#assign FeedbackSize = 0>
        <#assign feedbackCounter = 0>
        <#assign role = "">
        <#if parameters.role?has_content>
            <#assign role = parameters.role>
        </#if>
        <#list results.itemsAwaitingFeedback as leaveFeedback>
        <#if !leaveFeedback.commentType?has_content>
        <#if parameters.role?has_content && parameters.transactionId?has_content && parameters.targetUser?has_content && parameters.commentingUser?has_content>
            <#if parameters.transactionId == leaveFeedback.transactionID && parameters.itemId == leaveFeedback.itemID>
                <#if role == "buyer">
                    <#assign feedbackCounter = feedbackCounter + 1>
                    <#if row == "">
                        <#assign row = "alternate-row">
                    <#else>
                        <#assign row = "">
                    </#if>
                    <input type="hidden" value="${leaveFeedback.itemID}" name="itemId${feedbackCounter}"/>
                    <input type="hidden" value="${leaveFeedback.transactionID}" name="transactionId${feedbackCounter}"/>
                    <input type="hidden" value="${leaveFeedback.userID}" name="targetUser${feedbackCounter}"/>
                    <input type="hidden" value="${leaveFeedback.commentingUser}" name="commentingUser${feedbackCounter}"/>
                    <input type="hidden" value="${leaveFeedback.role}" name="role${feedbackCounter}"/>
                    <tr class="${row}" style="border:#eeeeee solid thin">
                        <td valign="top">
                        ${leaveFeedback.itemID} - ${leaveFeedback.title}
                        <a target="_blank" href="http://payments.sandbox.ebay.com/ws/eBayISAPI.dll?ViewPaymentStatus&amp;transId=${leaveFeedback.transactionID}&amp;ssPageName=STRK:MESOX:VPS&amp;itemid=${leaveFeedback.itemID}">order details</a>
                        </td>
                        <td>
                            <input type="radio" name="commentType${feedbackCounter}" value="positive" 
                                onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Positive</span>
                            <input type="radio" name="commentType${feedbackCounter}" checked="checked" value="none" 
                                onclick="document.getElementById('rate${feedbackCounter}').style.display='none';"/><span>I'll leave Feedback later</span>
                            <div id="rate${feedbackCounter}" style="display:none">
                                <input type="text" value="" maxlength="80" size="80" name="commentText${feedbackCounter}" label="Tell us more"/>
                                <br />80 characters left<br /><br />
                            </div>
                        </td>
                        <td>
                            Buyer:<a target="_blank" href="http://myworld.sandbox.ebay.com/${leaveFeedback.userID}">${leaveFeedback.userID}</a>
                        </td>
                    </tr>
                <#else>
                    <#assign feedbackCounter = feedbackCounter + 1>
                    <#if row == "">
                        <#assign row = "alternate-row">
                    <#else>
                        <#assign row = "">
                    </#if>
                    <input type="hidden" value="${leaveFeedback.itemID}" name="itemId${feedbackCounter}"/>
                    <input type="hidden" value="${leaveFeedback.transactionID}" name="transactionId${feedbackCounter}"/>
                    <input type="hidden" value="${leaveFeedback.userID}" name="targetUser${feedbackCounter}"/>
                    <input type="hidden" value="${leaveFeedback.commentingUser}" name="commentingUser${feedbackCounter}"/>
                    <input type="hidden" value="${leaveFeedback.role}" name="role${feedbackCounter}"/>
                    <tr class="${row}"  style="border:#eeeeee solid thin">
                        <td valign="top">
                        ${leaveFeedback.itemID} - ${leaveFeedback.title}
                        <a target="_blank" href="http://payments.sandbox.ebay.com/ws/eBayISAPI.dll?ViewPaymentStatus&amp;transId=${leaveFeedback.transactionID}&amp;ssPageName=STRK:MESOX:VPS&amp;itemid=${leaveFeedback.itemID}">order details</a>
                        </td>
                        <td>
                        <input type="radio" name="commentType${feedbackCounter}" value="positive" 
                            onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Positive</span>
                        <input type="radio" name="commentType${feedbackCounter}" value="neutral" 
                            onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Neutral</span>
                        <input type="radio" name="commentType${feedbackCounter}" value="negative" 
                            onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Negative</span>
                        <input type="radio" name="commentType${feedbackCounter}" checked="checked" value="none" 
                            onclick="document.getElementById('rate${feedbackCounter}').style.display='none';"/><span>I'll leave Feedback later</span>
                        <div id="rate${feedbackCounter}" style="display:none">
                            <input type="text" value="" maxlength="80" size="80" name="commentText${feedbackCounter}" label="Tell us more"/>
                            <br />80 characters left<br /><br />
                            <b>Rate details about this purchase</b>
                            <table>
                                <tr>
                                    <td>
                                        How accurate was the item description?
                                    </td>
                                    <td>
                                        <input id="ItemAsDescribedId${feedbackCounter}" type="hidden" value="0" name="ratingItem${feedbackCounter}"/>
                                        <img id="starItemAsDescribedId${feedbackCounter}_1" src="/images/rate/starDefault.gif" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',1,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='block'"/>
                                        <img id="starItemAsDescribedId${feedbackCounter}_2" src="/images/rate/starDefault.gif" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',2,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='block'"/>
                                        <img id="starItemAsDescribedId${feedbackCounter}_3" src="/images/rate/starDefault.gif" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',3,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='none'"/>
                                        <img id="starItemAsDescribedId${feedbackCounter}_4" src="/images/rate/starDefault.gif" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',4,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='none'"/>
                                        <img id="starItemAsDescribedId${feedbackCounter}_5" src="/images/rate/starDefault.gif" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',5,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='none'"/>
                                    </td>
                                </tr>
                                <tr id="itemOption${feedbackCounter}" style="display:none;border:#eeeeee solid thin;text-indent:10px">
                                    <td colspan="2">
                                        Why weren't you satisfied with the item description?
                                        <table class="answers" width="100%" border="0" cellpadding="0" cellspacing="0">
                                            <tbody>
                                                <tr>
                                                    <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="5" type="radio" />
                                                    </td><td>Item was not received</td>
                                                </tr>
                                                <tr>
                                                    <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="6" type="radio" /></td>
                                                    <td>Quality did not match item description</td>
                                                </tr>
                                                <tr>
                                                    <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="2" type="radio" /></td>
                                                    <td>Item was damaged</td>
                                                </tr>
                                                <tr>
                                                    <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="1" type="radio" /></td>
                                                    <td>Item was a counterfeit, replica, or an unauthorized copy</td>
                                                </tr>
                                                <tr>
                                                    <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="3" type="radio" /></td>
                                                    <td>Wrong item</td>
                                                </tr>
                                                <tr>
                                                    <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="4" type="radio" checked="checked" /></td>
                                                    <td>Other</td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        How satisfied were you with the seller's communication?
                                    </td>
                                    <td>
                                        <input id="CommResponsivenessId${feedbackCounter}" type="hidden" value="0" name="ratingComm${feedbackCounter}"/>
                                        <img id="starCommResponsivenessId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',1,this,${feedbackCounter})"/>
                                        <img id="starCommResponsivenessId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',2,this,${feedbackCounter})"/>
                                        <img id="starCommResponsivenessId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',3,this,${feedbackCounter})"/>
                                        <img id="starCommResponsivenessId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',4,this,${feedbackCounter})"/>
                                        <img id="starCommResponsivenessId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',5,this,${feedbackCounter})"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        How quickly did the seller ship the item?
                                    </td>
                                    <td>
                                        <input id="ShippingTimeId${feedbackCounter}" type="hidden" value="0" name="ratingShip${feedbackCounter}"/>
                                        <img id="starShippingTimeId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',1,this,${feedbackCounter})"/>
                                        <img id="starShippingTimeId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',2,this,${feedbackCounter})"/>
                                        <img id="starShippingTimeId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',3,this,${feedbackCounter})"/>
                                        <img id="starShippingTimeId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',4,this,${feedbackCounter})"/>
                                        <img id="starShippingTimeId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',5,this,${feedbackCounter})"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        How reasonable were the shipping and handling charges?
                                    </td>
                                    <td>
                                        <input id="ShippingHandlingChargesId${feedbackCounter}" type="hidden" value="0" name="ratingShipHand${feedbackCounter}"/>
                                        <img id="starShippingHandlingChargesId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',1,this,${feedbackCounter})"/>
                                        <img id="starShippingHandlingChargesId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',2,this,${feedbackCounter})"/>
                                        <img id="starShippingHandlingChargesId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',3,this,${feedbackCounter})"/>
                                        <img id="starShippingHandlingChargesId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',4,this,${feedbackCounter})"/>
                                        <img id="starShippingHandlingChargesId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',5,this,${feedbackCounter})"/>
                                    </td>
                                </tr>
                            </table>
                        </div>
                        </td>
                        <td>
                            Seller:<a target="_blank" href="http://myworld.sandbox.ebay.com/${leaveFeedback.userID}">${leaveFeedback.userID}</a>
                        </td>
                    </tr>
                </#if>
            </#if>
        <#elseif role=="buyer">
            <#if leaveFeedback.role == "buyer">
                <#assign feedbackCounter = feedbackCounter + 1>
                <#if row == "">
                    <#assign row = "alternate-row">
                <#else>
                    <#assign row = "">
                </#if>
                <input type="hidden" value="${leaveFeedback.itemID}" name="itemId${feedbackCounter}"/>
                <input type="hidden" value="${leaveFeedback.transactionID}" name="transactionId${feedbackCounter}"/>
                <input type="hidden" value="${leaveFeedback.userID}" name="targetUser${feedbackCounter}"/>
                <input type="hidden" value="${leaveFeedback.commentingUser}" name="commentingUser${feedbackCounter}"/>
                <input type="hidden" value="${leaveFeedback.role}" name="role${feedbackCounter}"/>
                <tr class="${row}" style="border:#eeeeee solid thin">
                    <td valign="top">
                    ${leaveFeedback.itemID}]${leaveFeedback.title}
                    <a target="_blank" href="http://payments.sandbox.ebay.com/ws/eBayISAPI.dll?ViewPaymentStatus&amp;transId=${leaveFeedback.transactionID}&amp;ssPageName=STRK:MESOX:VPS&amp;itemid=${leaveFeedback.itemID}">order details</a>
                    </td>
                    <td>
                        <input type="radio" name="commentType${feedbackCounter}" value="positive" 
                            onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Positive</span>
                        <input type="radio" name="commentType${feedbackCounter}" checked="checked" value="none" 
                            onclick="document.getElementById('rate${feedbackCounter}').style.display='none';"/><span>I'll leave Feedback later</span>
                        <div id="rate${feedbackCounter}" style="display:none">
                            <input type="text" value="" maxlength="80" size="80" name="commentText${feedbackCounter}" label="Tell us more"/>
                            <br />80 characters left<br /><br />
                        </div>
                    </td>
                    <td>
                        Buyer:<a target="_blank" href="http://myworld.sandbox.ebay.com/${leaveFeedback.userID}">${leaveFeedback.userID}</a>
                    </td>
                </tr>
            </#if>
        <#elseif role == "seller">
            <#if leaveFeedback.role == "seller">
                <#assign feedbackCounter = feedbackCounter + 1>
                <#if row == "">
                    <#assign row = "alternate-row">
                <#else>
                    <#assign row = "">
                </#if>
                <input type="hidden" value="${leaveFeedback.itemID}" name="itemId${feedbackCounter}"/>
                <input type="hidden" value="${leaveFeedback.transactionID}" name="transactionId${feedbackCounter}"/>
                <input type="hidden" value="${leaveFeedback.userID}" name="targetUser${feedbackCounter}"/>
                <input type="hidden" value="${leaveFeedback.commentingUser}" name="commentingUser${feedbackCounter}"/>
                <input type="hidden" value="${leaveFeedback.role}" name="role${feedbackCounter}"/>
                <tr class="${row}"  style="border:#eeeeee solid thin">
                    <td valign="top">
                    ${leaveFeedback.itemID} - ${leaveFeedback.title}
                    <a target="_blank" href="http://payments.sandbox.ebay.com/ws/eBayISAPI.dll?ViewPaymentStatus&amp;transId=${leaveFeedback.transactionID}&amp;ssPageName=STRK:MESOX:VPS&amp;itemid=${leaveFeedback.itemID}">order details</a>
                    </td>
                    <td>
                    <input type="radio" name="commentType${feedbackCounter}" value="positive" 
                        onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Positive</span>
                    <input type="radio" name="commentType${feedbackCounter}" value="neutral" 
                        onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Neutral</span>
                    <input type="radio" name="commentType${feedbackCounter}" value="negative" 
                        onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Negative</span>
                    <input type="radio" name="commentType${feedbackCounter}" checked="checked" value="none" 
                        onclick="document.getElementById('rate${feedbackCounter}').style.display='none';"/><span>I'll leave Feedback later</span>
                    <div id="rate${feedbackCounter}" style="display:none">
                        <input type="text" value="" maxlength="80" size="80" name="commentText${feedbackCounter}" label="Tell us more"/>
                        <br />80 characters left<br /><br />
                        <b>Rate details about this purchase</b>
                        <table>
                            <tr>
                                <td>
                                    How accurate was the item description?
                                </td>
                                <td>
                                    <input id="ItemAsDescribedId${feedbackCounter}" type="hidden" value="0" name="ratingItem${feedbackCounter}"/>
                                    <img id="starItemAsDescribedId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',1,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='block'"/>
                                    <img id="starItemAsDescribedId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',2,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='block'"/>
                                    <img id="starItemAsDescribedId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',3,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='none'"/>
                                    <img id="starItemAsDescribedId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',4,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='none'"/>
                                    <img id="starItemAsDescribedId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',5,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='none'"/>
                                </td>
                            </tr>
                            <tr id="itemOption${feedbackCounter}" style="display:none;border:#eeeeee solid thin;text-indent:10px">
                                <td colspan="2">
                                    Why weren't you satisfied with the item description?
                                    <table class="answers" width="100%" border="0" cellpadding="0" cellspacing="0">
                                        <tbody>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="5" type="radio" />
                                                </td><td>Item was not received</td>
                                            </tr>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="6" type="radio" /></td>
                                                <td>Quality did not match item description</td>
                                            </tr>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="2" type="radio" /></td>
                                                <td>Item was damaged</td>
                                            </tr>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="1" type="radio" /></td>
                                                <td>Item was a counterfeit, replica, or an unauthorized copy</td>
                                            </tr>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="3" type="radio" /></td>
                                                <td>Wrong item</td>
                                            </tr>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="4" type="radio" checked="checked" /></td>
                                                <td>Other</td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    How satisfied were you with the seller's communication?
                                </td>
                                <td>
                                    <input id="CommResponsivenessId${feedbackCounter}" type="hidden" value="0" name="ratingComm${feedbackCounter}"/>
                                    <img id="starCommResponsivenessId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',1,this,${feedbackCounter})"/>
                                    <img id="starCommResponsivenessId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',2,this,${feedbackCounter})"/>
                                    <img id="starCommResponsivenessId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',3,this,${feedbackCounter})"/>
                                    <img id="starCommResponsivenessId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',4,this,${feedbackCounter})"/>
                                    <img id="starCommResponsivenessId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',5,this,${feedbackCounter})"/>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    How quickly did the seller ship the item?
                                </td>
                                <td>
                                    <input id="ShippingTimeId${feedbackCounter}" type="hidden" value="0" name="ratingShip${feedbackCounter}"/>
                                    <img id="starShippingTimeId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',1,this,${feedbackCounter})"/>
                                    <img id="starShippingTimeId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',2,this,${feedbackCounter})"/>
                                    <img id="starShippingTimeId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',3,this,${feedbackCounter})"/>
                                    <img id="starShippingTimeId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',4,this,${feedbackCounter})"/>
                                    <img id="starShippingTimeId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',5,this,${feedbackCounter})"/>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    How reasonable were the shipping and handling charges?
                                </td>
                                <td>
                                    <input id="ShippingHandlingChargesId${feedbackCounter}" type="hidden" value="0" name="ratingShipHand${feedbackCounter}"/>
                                    <img id="starShippingHandlingChargesId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',1,this,${feedbackCounter})"/>
                                    <img id="starShippingHandlingChargesId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',2,this,${feedbackCounter})"/>
                                    <img id="starShippingHandlingChargesId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',3,this,${feedbackCounter})"/>
                                    <img id="starShippingHandlingChargesId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',4,this,${feedbackCounter})"/>
                                    <img id="starShippingHandlingChargesId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',5,this,${feedbackCounter})"/>
                                </td>
                            </tr>
                        </table>
                    </div>
                    </td>
                    <td>
                        Seller:<a target="_blank" href="http://myworld.sandbox.ebay.com/${leaveFeedback.userID}">${leaveFeedback.userID}</a>
                    </td>
                </tr>
            </#if>
        <#else>
            <#assign feedbackCounter = feedbackCounter + 1>
            <#if row == "">
                <#assign row = "alternate-row">
            <#else>
                <#assign row = "">
            </#if>
            <input type="hidden" value="${leaveFeedback.itemID}" name="itemId${feedbackCounter}"/>
            <input type="hidden" value="${leaveFeedback.transactionID}" name="transactionId${feedbackCounter}"/>
            <input type="hidden" value="${leaveFeedback.userID}" name="targetUser${feedbackCounter}"/>
            <input type="hidden" value="${leaveFeedback.commentingUser}" name="commentingUser${feedbackCounter}"/>
            <input type="hidden" value="${leaveFeedback.role}" name="role${feedbackCounter}"/>
            <tr class="${row}">
                <td>
                ${leaveFeedback.itemID} - ${leaveFeedback.title}
                <a target="_blank" href="http://payments.sandbox.ebay.com/ws/eBayISAPI.dll?ViewPaymentStatus&amp;transId=${leaveFeedback.transactionID}&amp;ssPageName=STRK:MESOX:VPS&amp;itemid=${leaveFeedback.itemID}">order details</a>
                </td>
                <td>
                <#if leaveFeedback.role == "seller">
                    <input type="radio" name="commentType${feedbackCounter}" value="positive" 
                        onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Positive</span>
                    <input type="radio" name="commentType${feedbackCounter}" value="neutral" 
                        onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Neutral</span>
                    <input type="radio" name="commentType${feedbackCounter}" value="negative" 
                        onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Negative</span>
                    <input type="radio" name="commentType${feedbackCounter}" checked="checked" value="none" 
                        onclick="document.getElementById('rate${feedbackCounter}').style.display='none';"/><span>I'll leave Feedback later</span>
                <#else>
                    <input type="radio" name="commentType${feedbackCounter}" value="positive" 
                        onclick="document.getElementById('rate${feedbackCounter}').style.display='block';"/><span>Positive</span>
                    <input type="radio" name="commentType${feedbackCounter}" checked="checked" value="none" 
                        onclick="document.getElementById('rate${feedbackCounter}').style.display='none';"/><span>I'll leave Feedback later</span>
                </#if>
                <#if leaveFeedback.role == "seller">
                    <div id="rate${feedbackCounter}" style="display:none">
                        <input type="text" value="" maxlength="80" size="80" name="commentText${feedbackCounter}" label="Tell us more"/>
                        <br />80 characters left<br /><br />
                        <b>Rate details about this purchase</b>
                        <table>
                            <tr>
                                <td>
                                    How accurate was the item description?
                                </td>
                                <td>
                                    <input id="ItemAsDescribedId${feedbackCounter}" type="hidden" value="0" name="ratingItem${feedbackCounter}"/>
                                    <img id="starItemAsDescribedId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',1,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='block'"/>
                                    <img id="starItemAsDescribedId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',2,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='block'"/>
                                    <img id="starItemAsDescribedId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',3,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='none'"/>
                                    <img id="starItemAsDescribedId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',4,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='none'"/>
                                    <img id="starItemAsDescribedId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ItemAsDescribedId',5,this,${feedbackCounter});document.getElementById('itemOption${feedbackCounter}').style.display='none'"/>
                                </td>
                            </tr>
                            <tr id="itemOption${feedbackCounter}" style="display:none;border:#eeeeee solid thin;text-indent:10px">
                                <td colspan="2">
                                    Why weren't you satisfied with the item description?
                                    <table class="answers" width="100%" border="0" cellpadding="0" cellspacing="0">
                                        <tbody>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="5" type="radio" />
                                                </td><td>Item was not received</td>
                                            </tr>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="6" type="radio" /></td>
                                                <td>Quality did not match item description</td>
                                            </tr>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="2" type="radio" /></td>
                                                <td>Item was damaged</td>
                                            </tr>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="1" type="radio" /></td>
                                                <td>Item was a counterfeit, replica, or an unauthorized copy</td>
                                            </tr>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="3" type="radio" /></td>
                                                <td>Wrong item</td>
                                            </tr>
                                            <tr>
                                                <td width="25"><input name="AqItemAsDescribedId${feedbackCounter}" value="4" type="radio" checked="checked" /></td>
                                                <td>Other</td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    How satisfied were you with the seller's communication?
                                </td>
                                <td>
                                    <input id="CommResponsivenessId${feedbackCounter}" type="hidden" value="0" name="ratingComm${feedbackCounter}"/>
                                    <img id="starCommResponsivenessId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',1,this,${feedbackCounter})"/>
                                    <img id="starCommResponsivenessId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',2,this,${feedbackCounter})"/>
                                    <img id="starCommResponsivenessId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',3,this,${feedbackCounter})"/>
                                    <img id="starCommResponsivenessId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',4,this,${feedbackCounter})"/>
                                    <img id="starCommResponsivenessId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('CommResponsivenessId',5,this,${feedbackCounter})"/>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    How quickly did the seller ship the item?
                                </td>
                                <td>
                                    <input id="ShippingTimeId${feedbackCounter}" type="hidden" value="0" name="ratingShip${feedbackCounter}"/>
                                    <img id="starShippingTimeId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',1,this,${feedbackCounter})"/>
                                    <img id="starShippingTimeId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',2,this,${feedbackCounter})"/>
                                    <img id="starShippingTimeId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',3,this,${feedbackCounter})"/>
                                    <img id="starShippingTimeId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',4,this,${feedbackCounter})"/>
                                    <img id="starShippingTimeId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingTimeId',5,this,${feedbackCounter})"/>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    How reasonable were the shipping and handling charges?
                                </td>
                                <td>
                                    <input id="ShippingHandlingChargesId${feedbackCounter}" type="hidden" value="0" name="ratingShipHand${feedbackCounter}"/>
                                    <img id="starShippingHandlingChargesId${feedbackCounter}_1" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',1,this,${feedbackCounter})"/>
                                    <img id="starShippingHandlingChargesId${feedbackCounter}_2" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',2,this,${feedbackCounter})"/>
                                    <img id="starShippingHandlingChargesId${feedbackCounter}_3" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',3,this,${feedbackCounter})"/>
                                    <img id="starShippingHandlingChargesId${feedbackCounter}_4" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',4,this,${feedbackCounter})"/>
                                    <img id="starShippingHandlingChargesId${feedbackCounter}_5" src="/images/rate/starDefault.gif" alt="" onmouseover="overStar(this)" onmouseout="outStar(this)" onclick="setRate('ShippingHandlingChargesId',5,this,${feedbackCounter})"/>
                                </td>
                            </tr>
                        </table>
                    </div>
                <#else>
                    <div id="rate${feedbackCounter}" style="display:none">
                        <input type="text" value="" maxlength="80" size="80" name="commentText${feedbackCounter}" label="Tell us more"/>
                        <br />80 characters left<br /><br />
                    </div>
                </#if>
                </td>
                <td>
                <#if leaveFeedback.role == "buyer">
                    Buyer:<a target="_blank" href="http://myworld.sandbox.ebay.com/${leaveFeedback.userID}">${leaveFeedback.userID}</a>
                <#else>
                    Seller:<a target="_blank" href="http://myworld.sandbox.ebay.com/${leaveFeedback.userID}">${leaveFeedback.userID}</a>
                </#if>
                </td>
            </tr>
        </#if>
        </#if>    
        </#list>
    </tbody>
</table>
<br />
<input type="hidden" name="feedbackSize" value="${feedbackCounter}"/>
<input type="submit" value="Leave Feedback"/>
</form>
<#else>
No Leave Feedback.
</#if>