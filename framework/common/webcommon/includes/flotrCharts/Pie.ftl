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
<!--[if IE]><script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/flotr-0.2.0-alpha/flotr/lib/excanvas.js</@ofbizContentUrl>"></script><![endif]-->
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/prototypejs/prototype.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/flotr/lib/canvas2image.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/flotr/lib/canvastext.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/flotr/flotr.js</@ofbizContentUrl>"></script>

<div id="${chartId}Div" style="width:800px;height:300px;"></div>

<script type="text/javascript">
document.observe('dom:loaded', function(){
    var dataAsText = '${dataText}';
    var chartData = [];
    chartData = dataAsText.split(',');
    var allData = [];
    var y = 0;
    for(var i=0; i<chartData.length-1 ; i=i+2) {
        var a = [[0, chartData[i+1]]];
        allData[y] = {data:a, label:chartData[i]};
        y++;
    }
    var f = Flotr.draw($('${chartId}Div'), allData, {
        HtmlText: false,
        grid: {verticalLines: false, horizontalLines: false},
        xaxis: {showLabels: false},
        yaxis: {showLabels: false},
        pie: {show: true, explode: 6},
        legend:{position: 'se', backgroundColor: '#D2E8FF'}
    });
});
</script>