// English initialisation for the jQuery UI date picker plugin. 
// Only added by JLR in OFBiz to get rid of "Failed to load resource: the server responded with a status of 404 (Not Found)" in Chrome js console
// It's redundant with "Default regional settings" in function Datepicker() { 
jQuery(function($){
    $.datepicker.regional['en'] = {
        closeText: 'Done', // Display text for close link
        prevText: 'Prev', // Display text for previous month link
        nextText: 'Next', // Display text for next month link
        currentText: 'Today', // Display text for current month link
        monthNames: ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'], // Names of months for drop-down and formatting
        monthNamesShort: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'], // For formatting
        dayNames: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'], // For formatting
        dayNamesShort: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'], // For formatting
        dayNamesMin: ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'], // Column headings for days starting at Sunday
        weekHeader: 'Wk', // Column header for week of the year
        dateFormat: 'mm/dd/yy', // See format options on parseDate
        firstDay: 0, // The first day of the week, Sun = 0, Mon = 1, ...
        isRTL: false, // True if right-to-left language, false if left-to-right
        showMonthAfterYear: false, // True if the year select precedes month, false for month then year
        yearSuffix: ''}; // Additional text to append to the year in the month headers
    $.datepicker.setDefaults($.datepicker.regional['en']);
});
