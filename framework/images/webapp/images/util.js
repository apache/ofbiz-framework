jQuery.ajaxSetup({
  dataFilter: function(data, type) {
    var prefixes = ['//', 'while(true);', 'for(;;);'],
    i,
    l,
    pos;

    if (type != 'json' && type != 'jsonp') {
      return data;
    }

    for (i = 0, l = prefixes.length; i < l; i++) {
      pos = data.indexOf(prefixes[i]);
      if (pos === 0) {
        return data.substring(prefixes[i].length);
      }
    }

    return data;
  }
});
