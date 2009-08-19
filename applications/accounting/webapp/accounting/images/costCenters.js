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

function changeAmountPercentage(elementId) {
    idArray = elementId.split('_');
    var data = null;
    new Ajax.Request('createGlAcctCatMemFromCostCenters', {
        asynchronous: false,
        onSuccess: function(transport) {
            data = transport.responseText.evalJSON(true);
        },
        parameters: {amountPercentage : $F(elementId), glAccountId : idArray[1], glAccountCategoryId : idArray[2]}
    });
    if (data._ERROR_MESSAGE_LIST_ != undefined) {
        Effect.Appear('notValidTotal_'+idArray[1], {duration: 0.0});
        Effect.Fade('notValidTotal_'+idArray[1], {duration: 5.0});
    } else {
        Effect.Appear('validTotal_'+idArray[1], {duration: 0.0});
        Effect.Fade('validTotal_'+idArray[1], {duration: 5.0});
    }
}


