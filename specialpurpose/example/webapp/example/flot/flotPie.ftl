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
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/plugins/flot/excanvas.min.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/jquery-1.7.2.min.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/plugins/flot/jquery.flot.js</@ofbizContentUrl>"></script>
<script language="javascript" type="text/javascript" src="<@ofbizContentUrl>/images/jquery/plugins/flot/jquery.flot.pie.js</@ofbizContentUrl>"></script>

<div id="container" style="width:600px;height:300px;"></div>

<script type="text/javascript">
  jQuery(document).ready( function(){
    // Fill series.
    var d1 = [[0, Math.ceil(Math.random()*40)]];
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
    ];
  });
</script>

<div id="containerb" style="width:600px;height:300px;"></div>

<script type="text/javascript">
  /**
   * Wait till dom's finished loading.
   */
  jQuery(document).ready( function(){
    /**
     * Draw the graph in the first container.
     */
    jQuery.plot(jQuery("#containerb"), allData,
    {
            series: {
                    pie: {
                            show: true
                    }
            }
            grid: {
                autoHighlight: true,
                hoverable: true
            }
    });
  });
</script>
