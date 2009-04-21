var DropDownMenu = Class.create();
DropDownMenu.prototype = {
 initialize: function(menuElement) {
    var menuTitle = $A(menuElement.getElementsByTagName("h2")).first();
    menuElement.childElements().each(function(node){
        // if there is a submenu
        var submenu = $A(node.getElementsByTagName("ul")).first();
        if(submenu != null){
            // make sub-menu invisible
            Element.extend(submenu).setStyle({display: 'none'});
            // toggle the visibility of the submenu
            if (menuTitle != null) {
                menuTitle.onmouseover = menuTitle.onmouseout = function(){Element.toggle(submenu);};
                menuTitle = null;
            }
            node.onmouseover = node.onmouseout = function(){Element.toggle(submenu);};
        }
    });
  }
};

Event.observe(window, "load", function(){

});

document.observe('dom:loaded', function(){
    var mainmenu = new DropDownMenu($('main-navigation'));
    var appmenu = new DropDownMenu($('app-navigation'));
});

