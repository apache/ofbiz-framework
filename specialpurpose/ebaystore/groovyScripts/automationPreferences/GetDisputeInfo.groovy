/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.ebay.soap.eBLBaseComponents.DisputeReasonCodeType
import com.ebay.soap.eBLBaseComponents.DisputeExplanationCodeType

public static String makeSpace(String text){
    String result = ""
    for (int i=0; i<text.length(); i++) {
        if (i < text.length()-1) {
            if (String.valueOf(text.charAt(i+1)).equals(String.valueOf(text.charAt(i+1)).toUpperCase())) {
                result = result + String.valueOf(text.charAt(i)) + " " 
            } else {
                result = result + String.valueOf(text.charAt(i))
            } 
        }else{
            result = result + String.valueOf(text.charAt(i))
        }
    }
    return result
}
reasons = []
explanations = []

entry = [:]
entry.put("reasonCode", DisputeReasonCodeType.BUYER_HAS_NOT_PAID.toString())
entry.put("value",  makeSpace(DisputeReasonCodeType.BUYER_HAS_NOT_PAID.value()))
reasons.add(entry)
entry = [:]
entry.put("reasonCode", DisputeReasonCodeType.TRANSACTION_MUTUALLY_CANCELED.toString())
entry.put("value",  makeSpace(DisputeReasonCodeType.TRANSACTION_MUTUALLY_CANCELED.value()))
reasons.add(entry)

entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.BUYER_HAS_NOT_RESPONDED.toString())
entry.put("value", makeSpace(DisputeExplanationCodeType.BUYER_HAS_NOT_RESPONDED.value()))
explanations.add(entry)
entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.BUYER_REFUSED_TO_PAY.toString())
entry.put("value", makeSpace(DisputeExplanationCodeType.BUYER_REFUSED_TO_PAY.value()))
explanations.add(entry)
entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.BUYER_RETURNED_ITEM_FOR_REFUND.toString())
entry.put("value", makeSpace(DisputeExplanationCodeType.BUYER_RETURNED_ITEM_FOR_REFUND.value()))
explanations.add(entry)
entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.UNABLE_TO_RESOLVE_TERMS.toString())
entry.put("value", makeSpace(DisputeExplanationCodeType.UNABLE_TO_RESOLVE_TERMS.value()))
explanations.add(entry)
entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.BUYER_PURCHASING_MISTAKE.toString())
entry.put("value", makeSpace(DisputeExplanationCodeType.BUYER_PURCHASING_MISTAKE.value()))
explanations.add(entry)
entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.SHIP_COUNTRY_NOT_SUPPORTED.toString())
entry.put("value",  makeSpace(DisputeExplanationCodeType.SHIP_COUNTRY_NOT_SUPPORTED.value()))
explanations.add(entry)
entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.SHIPPING_ADDRESS_NOT_CONFIRMED.toString())
entry.put("value", makeSpace(DisputeExplanationCodeType.SHIPPING_ADDRESS_NOT_CONFIRMED.value()))
explanations.add(entry)
entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.PAYMENT_METHOD_NOT_SUPPORTED.toString())
entry.put("value", makeSpace(DisputeExplanationCodeType.PAYMENT_METHOD_NOT_SUPPORTED.value()))
explanations.add(entry)
entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.BUYER_NO_LONGER_REGISTERED.toString())
entry.put("value", makeSpace(DisputeExplanationCodeType.BUYER_NO_LONGER_REGISTERED.value()))
explanations.add(entry)
entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.BUYER_NO_LONGER_REGISTERED.toString())
entry.put("value", makeSpace(DisputeExplanationCodeType.BUYER_NO_LONGER_REGISTERED.value()))
explanations.add(entry)
entry = [:]
entry.put("explanationCode", DisputeExplanationCodeType.OTHER_EXPLANATION.toString())
entry.put("value", makeSpace(DisputeExplanationCodeType.OTHER_EXPLANATION.value()))
explanations.add(entry)

context.reasons = reasons
context.explanations = explanations
