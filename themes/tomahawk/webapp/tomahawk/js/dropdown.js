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

var DropDownMenu = Class.create();

DropDownMenu.prototype = {
  initialize: function(menuElement) {
    var menuTitle = $A(menuElement.getElementsByTagName("h2")).first();

    menuElement.childElements().each(function(node){
      // if there is a submenu
      var submenu = $A(node.getElementsByTagName("ul")).first();
      if(submenu != null){
        // make sub-menu invisible
        Element.hide(submenu);
        // toggle the visibility of the submenu
        if (menuTitle != null) {
          menuTitle.onmouseover = function(){Element.extend(submenu).setStyle({display: 'block'});};
          menuTitle.onmouseout = function(){Element.hide(submenu);};
        }
        node.onmouseover = function(){Element.extend(submenu).setStyle({display: 'block'});};
        node.onmouseout = function(){Element.hide(submenu);};
      }
    });
  }
};