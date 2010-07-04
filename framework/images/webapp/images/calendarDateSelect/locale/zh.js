Date.weekdays = $w("? ? ? ? ? ? ?");
Date.months = $w("? ? ? ? ? ? ? ? ? ? ?? ??" );

Date.first_day_of_week = 1

_translations = {
  "OK": "??",
  "Now": "??",
  "Today": "??"
}
//load the data format
var dataFormatJs = "format_iso_date.js" // Not sure

var e = document.createElement("script");
e.src = "/images/calendarDateSelect/format/" + dataFormatJs;
e.type="text/javascript";
document.getElementsByTagName("head")[0].appendChild(e);
