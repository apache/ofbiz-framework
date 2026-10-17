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


var DropDownMenu = (
    function(menuElement) {
    var menuTitle = menuElement.find("h2:first");
    menuElement.children().each(function(node){
      // if there is a submenu
      var submenu = jQuery(this).find("ul:first");

      if(submenu != null){
        // make sub-menu invisible
        submenu.hide();
        // toggle the visibility of the submenu
        if (menuTitle != null) {
          menuTitle.mouseover (function(){ submenu.css({'display': 'block'});});
          menuTitle.mouseout (function(){submenu.hide();});
        }
        jQuery(this).mouseover (function(){submenu.css({'display': 'block'});});
        jQuery(this).mouseout (function(){submenu.hide();});
      }
    });
  }
);