Date.weekdays = $w("L M X J V S D");
Date.months = $w("Enero Febrero Marzo Abril Mayo Junio Julio Agosto Septiembre Octubre Noviembre Diciembre" );

Date.first_day_of_week = 1;

_translations = {
  "OK": "Cancelar",
  "Now": "Ahora",
  "Clear": "Limpiar",
  "Today": "Hoy"
}

//load the data format
var dataFormatJs = "format_euro_24hr.js"

var e = document.createElement("script");
e.src = "/images/calendarDateSelect/format/" + dataFormatJs;
e.type="text/javascript";
document.getElementsByTagName("head")[0].appendChild(e);
