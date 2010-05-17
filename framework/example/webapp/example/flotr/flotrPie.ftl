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

<div id="container" style="width:600px;height:300px;"></div>

<script type="text/javascript">
  document.observe('dom:loaded', function(){
    // Fill series.
    var d1 = [[0, Math.ceil(Math.random()*40)]];
    var d2 = [[0, Math.ceil(Math.random()*30)]];
    var d3 = [[0, Math.ceil(Math.random()*20)]];
    var d4 = [[0, Math.ceil(Math.random()*10)]];
    var d5 = [[0, Math.ceil(Math.random()*10)]];

    //Draw the graph.
    var f = Flotr.draw($('container'), [
        {data:d1, label: 'Comedy', pie:{explode: Math.ceil(Math.random()*10)}},
        {data:d2, label: 'Action'},
        {data:d3, label: 'Romance', pie:{explode: Math.ceil(Math.random()*10)} },
        {data:d4, label: 'Drama'},
        {data:d5, label: 'Other', pie:{explode: Math.ceil(Math.random()*10)} }
    ], {
        HtmlText: false,
        grid: {
          verticalLines: false,
          horizontalLines: false
        },
        xaxis: {showLabels: false},
        yaxis: {showLabels: false},
        pie: {
          show: true,
          explode: 6
        },
        legend:{
          position: 'se',
          backgroundColor: '#D2E8FF'
        }
    });
  });
</script>

<div id="containerb" style="width:600px;height:300px;"></div>

<script type="text/javascript">
  /**
   * Wait till dom's finished loading.
   */
  document.observe('dom:loaded', function(){
    function markerFomatter(obj) {
      return obj.y+'%';
    }

    /**
     * Fill series d1 and d2 width random values.
     */
    var point,
            d1 = [],
            d2 = [],
            markers = {data:[], markers:{show: true, position: 'ct', labelFormatter: markerFomatter}, bars:{show: false}};

    for(var i = 0; i < 4; i++ ){
      point = [i, Math.ceil(Math.random()*10)];
      d1.push(point);
      markers.data.push(point);

      point = [i+0.5, Math.ceil(Math.random()*10)];
      d2.push(point);
      markers.data.push(point);
    }

    /**
     * Draw the graph in the first container.
     */
    Flotr.draw(
      $('containerb'),
      [d1, d2, markers],
      {
        bars: {show:true, barWidth:0.5},
        mouse: {track:true, relative:true},
        yaxis: {min: 0, autoscaleMargin: 1},
        spreadsheet: {show: false}
      }
    );
  });
</script>

