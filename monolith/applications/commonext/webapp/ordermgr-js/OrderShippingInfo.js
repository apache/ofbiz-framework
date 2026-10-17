/*
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
*/
var validateNewShippingAdd = null;
jQuery(document).ready( function() {

    var addShippingAddress = jQuery('#addShippingAddress');
    if (addShippingAddress.length) {
        // add the form validator
        addShippingAddress.validate();
       
       jQuery('#countryGeoId').change( function() {
            getAssociatedStateList('countryGeoId', 'stateProvinceGeoId', 'advice-required-stateProvinceGeoId', 'states');
        });

        // Populate state list based on default country
        getAssociatedStateList('countryGeoId', 'stateProvinceGeoId', 'advice-required-stateProvinceGeoId', 'states');
    }
});

function showEdit(edit, index) {
    var sufix = index;
    if (sufix == '-1') {
       sufix = "";
    }

    //display / hide edit element
    var element = document.getElementById("edit" + sufix);
    if (element != null) {
       var objBranch = element.style;
       if ("edit" == edit) {
         objBranch.display = "block";
       } else {
         objBranch.display = "none";
       }
    }
    var element = document.getElementById("display" + sufix);
    if (element != null) { 
       var objBranch = element.style;
       if (edit == "display") {
          objBranch.display = "block";
       } else {
          objBranch.display = "none";
       }
    }

    var next = true;
    for(var i = 0; next ; i++) {
      //hide / show display quantity
      var element = document.getElementById("displayQuantity" + sufix + i);
      if (element != null) { 
        var objBranch = element.style;
        if (edit == "display") {
          objBranch.display = "block";
        } else {
          objBranch.display = "none";
        }
      }

      //hide / show edit quantity
      var element = document.getElementById("editQuantity" + sufix + i);
      if (element != null) { 
        var objBranch = element.style;
        if (edit == "edit") {
          objBranch.display = "block";
        } else {
          objBranch.display = "none";
        }
      }
      if (element == null) {
         next = false;
      }
    }

    //Hide display OISG edit view
    var element = document.getElementById("OISGEdit" + sufix);
    if (element != null) {
      var objBranch = element.style;
      if (edit == "edit") {
        objBranch.display = "block";
      }
      else {
        objBranch.display = "none";
      }
    }
}

function restoreEditField(index) {
    var sufix = index;
    if (sufix == '-1')
      sufix = "";

    //display / hide edit element
    var next = true;
    for(var i = 0; next ; i++) {
      var editElement = document.getElementById("edit" + index + "_o_" + i);
      if (editElement == null) {
         next = false;
      } else {
         editElement.value = editElement.title;
      }
    }
}

function showShipByDate(e, id) {
    var element = document.getElementById(id);
    if (e.value == "new") {
       element.style.display = "block"; 
    } else {
       element.style.display = "none";
    }
}

function showView(view, index) {
    var sufix = index;
    if (sufix == '-1') {
       sufix = "";
    }
    //display / hide buttonDisplay element
    var element = document.getElementById("display" + sufix);
    if (element != null) {  
      var objBranch = element.style;
      if ("view" == view) {
        objBranch.display = "none";
      } else {
        objBranch.display = "block";
      }

      //display / hide buttonEdit element
      var element = document.getElementById("view" + sufix);
      if (element != null) {  
        var objBranch = element.style;
        if ("view" == view) {
          objBranch.display = "block";
        } else {
          objBranch.display = "none";
        }
      }

      //Hide display OISG show view
      var element = document.getElementById("OISGView" + sufix);
      if (element != null) {
        var objBranch = element.style;
        if (view == "view") {
          objBranch.display = "block";
        }
        else {
          objBranch.display = "none";
        }
      }
    }
}
