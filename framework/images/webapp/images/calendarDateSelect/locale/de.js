Date.weekdays = $w('Mo Di Mi Do Fr Sa So');
Date.months = $w('Januar Februar März April Mai Juni Juli August September Oktober November Dezember');

Date.first_day_of_week = 1;

_translations = {
  "OK": "OK",
  "Now": "Jetzt",
  "Today": "Heute",
  "Clear": "Löschen"
}

//load the data format
var dataFormatJs = "format_euro_24hr.js"

var e = document.createElement("script");
e.src = "/images/calendarDateSelect/format/" + dataFormatJs;
e.type="text/javascript";
document.getElementsByTagName("head")[0].appendChild(e);