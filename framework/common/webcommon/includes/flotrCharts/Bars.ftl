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


<div id="${chartId}Div" style="width:600px;height:300px;"></div>

<script type="text/javascript">
  document.observe('dom:loaded', function(){
  function markerFomatter(obj) {
    var labelsAsText = "${labelsText}";
                        var labels = [];
                        labels = labelsAsText.split(",");
                        var index = "" + Number(obj.x)-1
                        var text = labels[index];
                        if(text) 
                        return text;
                        else
                        return "";
  }
    var dataAsText = "${dataText}";
    var chartData = [];
    chartData = dataAsText.split(',');
    var y = 1;
    var point1,
            d2 = [],
            markers1 = {data:[], markers:{show: true, position: 'ct', labelFormatter: markerFomatter}, bars:{show: false}};
 
    for(var i=0; i<chartData.length-1 ; i=i+2) {
        point1 = [y, Number(chartData[i+1])];
        y++;
        d2.push(point1);
        markers1.data.push(point1);
    }
    var labelsAsText = "${labelsText}";
    var labels = [];
    labels = labelsAsText.split(",");
     
    Flotr.draw(
      $('${chartId}Div'),
      [d2, markers1],
      {
        bars: {show:true, barWidth:0.5,lineWidth:labels.length},
        mouse: {track:true, relative:true},
        yaxis: {min: 0, autoscaleMargin: 1},
        xaxis: {labelsAngle: 90, min: 0, max : labels.length,
                        tickFormatter : function (val) {
                        return "";
                        }},
        spreadsheet: {show: false},
        legend:{show: true, backgroundColor: '#D2E8FF'}
      }
    );
  });
</script>
