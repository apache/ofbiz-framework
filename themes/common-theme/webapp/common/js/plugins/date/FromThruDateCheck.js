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


//this code needs modifications yet its specific.

jQuery(document).ready( function() {
  jQuery("input[name*='fromDate']").bind('focusout', checkDate);
  jQuery("input[name*='thruDate']").bind('focusout', checkDate);
});

function checkDate() {
  var a = jQuery("input[name*='fromDate']");
  var b = jQuery("input[name*='thruDate']");

  if(a.val() !="" && b.val() !="") {
    if (a.val() >= b.val()) {
      showErrorAlertLoadUiLabel("", "", "CommonUiLabels", "CommonFromDateThruDateCheck")
    }
  }
}
