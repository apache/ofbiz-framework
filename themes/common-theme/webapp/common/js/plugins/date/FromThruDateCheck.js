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

var labelObject

//this code needs modifications yet its specific.

jQuery(document).ready( function() {
  jQuery("input[name*='fromDate']").on('focusout', checkDate);
  jQuery("input[name*='thruDate']").on('focusout', checkDate);
  if (jQuery("input[name*='fromDate']").length !== 0) {
    // retrieve label for date control
    labelObject = {
      "CommonUiLabels" : ["CommonFromDateThruDateCheck"],
    };
    getJSONuiLabels(labelObject, function(result){
      labelObject   = result.responseJSON;
    });
  }
});

function checkDate() {
  var a = jQuery("input[name*='fromDate']");
  var b = jQuery("input[name*='thruDate']");

  if(a.val() !="" && b.val() !="") {
    if (a.val() >= b.val()) {
      showjGrowlMessage(labelObject.CommonUiLabels[0], 'errorMessageJGrowl', true, null, null, null, "center");
    }
  }
}
