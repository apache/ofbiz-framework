// Time Format as "17:00" in DropDown Box
Date.padded2 = function(hour) { padded2 = hour.toString(); if ((parseInt(hour) < 10) || (parseInt(hour) == null)) padded2="0" + padded2; return padded2; }
Date.prototype.getAMPMHour = function() { hour=Date.padded2(this.getHours()); return (hour == null) ? 00 : (hour > 24 ? hour - 24 : hour ) }
Date.prototype.getAMPM = function() { return (this.getHours() < 12) ? "" : ""; }

// Formats date and time as "2000.01.20 17:00"
Date.prototype.toFormattedString = function(include_time)
{
   str = this.getFullYear() + "." + Date.padded2(this.getMonth()+1) + "." + Date.padded2(this.getDate());
   if (include_time) { str += " " + this.getHours() + ":" + this.getPaddedMinutes() }
   return str;
}