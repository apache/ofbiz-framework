Date.weekdays = $w('P W Ś C P S N');
Date.months = $w('Styczeń Luty Marzec Kwiecień Maj Czerwiec Lipiec Sierpień Wrzesień Październik Listopad Grudzień');

Date.first_day_of_week = 1

_translations = {
  "OK": "OK",
  "Now": "Teraz",
  "Today": "Dziś"
}

//load the data format
var dataFormatJs = "format_euro_24hr.js"

var e = document.createElement("script");
e.src = "/images/calendarDateSelect/format/" + dataFormatJs;
e.type="text/javascript";
document.getElementsByTagName("head")[0].appendChild(e);
