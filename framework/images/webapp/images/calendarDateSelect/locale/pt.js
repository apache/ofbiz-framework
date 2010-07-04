Date.weekdays = $w('D S T Q Q S S');
Date.months = $w('Janeiro Fevereiro Março Abril Maio Junho Julho Agosto Setembro Outubro Novembro Dezembro');

Date.first_day_of_week = 0

_translations = {
  "OK": "OK",
  "Now": "Agora",
  "Today": "Hoje",
  "Clear": "Limpar"
}

//load the data format
var dataFormatJs = "format_euro_24hr.js"

var e = document.createElement("script");
e.src = "/images/calendarDateSelect/format/" + dataFormatJs;
e.type="text/javascript";
document.getElementsByTagName("head")[0].appendChild(e);
