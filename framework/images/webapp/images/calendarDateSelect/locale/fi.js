Date.weekdays = $w("Ma Ti Ke To Pe La Su");
Date.months = $w("Tammikuu Helmikuu Maaliskuu Huhtikuu Toukokuu Kes‰kuu Hein‰kuu Elokuu Syyskuu Lokakuu Marraskuu Joulukuu" );

Date.first_day_of_week = 1

_translations = {
  "OK": "OK",
  "Now": "Nyt",
  "Today": "T‰n‰‰n"
}

//load the data format
var dataFormatJs = "format_euro_24hr.js"

var e = document.createElement("script");
e.src = "/images/calendarDateSelect/format/" + dataFormatJs;
e.type="text/javascript";
document.getElementsByTagName("head")[0].appendChild(e);
