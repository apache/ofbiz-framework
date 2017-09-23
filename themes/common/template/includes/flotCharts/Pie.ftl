<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/common/js/jquery/plugins/flot/excanvas.min.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/common/js/jquery/plugins/flot/jquery.flot.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/common/js/jquery/plugins/flot/jquery.flot.pie.js</@ofbizContentUrl>"></script>


<div id="${chartId}Div" style="width:800px;height:300px;"></div>

<script type="text/javascript">
jQuery(document).ready( function(){
    /* Code Example: How should a chart Data Object look like */
    /*var d1 = [[0, Math.ceil(Math.random()*40)]];
    var d2 = [[0, Math.ceil(Math.random()*30)]];
    var d3 = [[0, Math.ceil(Math.random()*20)]];
    var d4 = [[0, Math.ceil(Math.random()*10)]];
    var d5 = [[0, Math.ceil(Math.random()*10)]];
    var data = [
        {data:d1, label: 'Comedy'},
        {data:d2, label: 'Action'},
        {data:d3, label: 'Romance'},
        {data:d4, label: 'Drama'},
        {data:d5, label: 'Other'}
    ];*/
    /* End Example */

    var dataAsText = '${StringUtil.wrapString(dataText)}';
    var chartData = [];
    chartData = dataAsText.split(',');
    var allData = [];
    var y = 0;

    for(var i=0; i<chartData.length-1 ; i=i+2) {
        var a = [[0, chartData[i+1]]];
        allData[y] = {label:chartData[i], data:a};
        y++;
    }

    jQuery.plot(jQuery("#${chartId}Div"), allData,
    {
            series: {
                    pie: {
                            show: true,
                            label: {
                                show: true,
                                radius: 3/4,

                                formatter: function(label, series){
                                    return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'+label+'<br/>'+Math.round(series.percent)+'%</div>';
                                },
                                background: {
                                    opacity: 0.5 ,
                                    color: '#000000'
                                },
                            }

                    }
            },
            grid: {
                autoHighlight: true,
                hoverable: true
            },
             legend: {
            show: false
            }

    });

});
</script>
