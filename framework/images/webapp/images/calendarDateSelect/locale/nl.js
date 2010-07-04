Date.weekdays = $w('Ma Di Wo Do Vr Za Zo');
Date.months = $w('Januari Februari Maart April Mei Juni Juli Augustus September Oktober November December');

Date.first_day_of_week = 1;

_translations = {
  "OK": "OK",
  "Now": "Nu",
  "Today": "Vandaag",
  "Clear": "Wissen"
}

//load the data format
var dataFormatJs = "format_euro_24hr.js"

var e = document.createElement("script");
e.src = "/images/calendarDateSelect/format/" + dataFormatJs;
e.type="text/javascript";
document.getElementsByTagName("head")[0].appendChild(e);
