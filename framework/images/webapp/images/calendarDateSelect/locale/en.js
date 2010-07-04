Date.weekdays = $w("Sa M T W T F Su");
Date.months = $w("January February March April May June July August September October November December" );

Date.first_day_of_week = 0;


_translations = {
  "OK": "OK",
  "Now": "Now",
  "Today": "Today",
  "Clear": "Clear"
}

//load the data format
var dataFormatJs = "format_american.js" // For GB/CAN/AUS/NZ... maybe need more work...

var e = document.createElement("script");
e.src = "/images/calendarDateSelect/format/" + dataFormatJs;
e.type="text/javascript";
document.getElementsByTagName("head")[0].appendChild(e);
