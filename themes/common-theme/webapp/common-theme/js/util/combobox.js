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

function other_choice(dropDown) {
    var optValue = jQuery(dropDown).children("option:selected").val();
    return optValue == "_OTHER_";
}

function activate(field) {
  field.prop("disabled",false)
      .css('visibility', 'visible')
      .focus();
}

function process_choice(selection,textfield) {
  if(other_choice(selection)) {
    activate(textfield);
  } else {
    textfield.prop("disabled", true).val('').css('visibility', 'hidden');
  }
}

function check_choice(dropDown) {
  if(!other_choice(dropDown)) {
    dropDown.blur();
    alert('Please check your menu selection first');
    dropDown.focus();
  }
}
