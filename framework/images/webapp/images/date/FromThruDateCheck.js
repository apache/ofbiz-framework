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
