
        // This code inserts the value lookedup by a popup window back into the associated form element
        var re_id = new RegExp('id=(\\d+)');
        var num_id = (re_id.exec(String(window.location))
                ? new Number(RegExp.$1) : 0);
        var obj_caller = (window.opener ? window.opener.lookups[num_id] : null);
        
        
        // function passing selected value to calling window
        function set_value(value) {
                if (!obj_caller) return;
                window.close();
                obj_caller.target.value = value;
        }
        // function refreshes caller after posting new entry
        function refresh_caller(value) {
            var str = "/postSubContent";
            <#assign separator="?"/>
            <#if requestAttributes.contentId?exists>
                str += '${separator}';
                str += "contentId=" + "${requestAttributes.contentId}";
                <#assign separator="&"/>
            </#if>
            <#if requestAttributes.mapKey?exists>
                str += '${separator}';
                str += "mapKey=" + "${requestAttributes.mapKey}";
                <#assign separator="&"/>
            </#if>
                str += '${separator}';
                str += value;
            var requestStr = '"<@ofbizUrl>"' + escape(str)</@ofbizUrl> + '"';

            window.opener.replace(requestStr);
        }

        function lookupSubContent (viewName, contentId, mapKey, subDataResourceTypeId, subMimeTypeId) {
	    var viewStr = viewName;
            var my=20;
            var mx=20;
            var separator = "?";
            if (contentId != null && (contentId.length > 0)) {
                viewStr += separator + "contentIdTo=" + contentId;
                separator = "&";
            }
            if (mapKey != null && mapKey.length > 0) {
                viewStr += separator + "mapKey=" + mapKey;
                separator = "&";
            }
            if (subDataResourceTypeId != null && subDataResourceTypeId.length > 0) {
                viewStr += separator + "drDataResourceTypeId=" + subDataResourceTypeId;
                separator = "&";
            }
            if (subMimeTypeId != null && subMimeTypeId.length > 0) {
                viewStr += separator + "drMimeTypeId=" + subMimeTypeId;
            }
	    var obj_lookupwindow = window.open(viewStr, 'FieldLookup', 
                'width=700,height=550,scrollbars=yes,status=no,top='
                  +my+',left='+mx+',dependent=yes,alwaysRaised=yes');
	    obj_lookupwindow.opener = window;
            obj_lookupwindow.focus();
        }
