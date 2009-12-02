Date.weekdays = $w('L Ma Me J V S D');
Date.months = $w('Janvier Février Mars Avril Mai Juin Juillet Août Septembre Octobre Novembre Décembre');

Date.first_day_of_week = 1;

_translations = {
  "OK": "OK",
  "Now": "Maintenant",
  "Today": "Aujourd'hui",
  "Clear": "Effacer"
}

//load the data format
var dataFormatJs = "format_euro_24hr.js"

var e = document.createElement("script");
e.src = "/images/calendarDateSelect/format/" + dataFormatJs;
e.type="text/javascript";
document.getElementsByTagName("head")[0].appendChild(e);
