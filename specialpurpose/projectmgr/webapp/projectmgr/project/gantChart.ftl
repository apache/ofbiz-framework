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

<script language="javascript">

            dojo.require("dojo.collections.Store");
            dojo.require("dojo.charting.Chart");
            dojo.require('dojo.json');
            dojo.require("dojo.date.common");
            dojo.require("dojo.event.*");
            dojo.require("dojo.io.*");

            var interv = dojo.date.dateParts.DAY;
            

            //  our sample data for our gantt chart.

        var json = [];

        // Convert the database data to json
        <#if tasks?has_content>
	        <#list tasks as taskNode>
                <#if taskNode.estimatedStartDate?exists>
                   var dtStart = new Date("${taskNode.estimatedStartDate?string("yyyy/MM/dd HH:mm")}");
                   <#else>
                   var dtStart = new Date("${chartStart?string("yyyy/MM/dd HH:mm")}");
                </#if>
                <#if taskNode.estimatedCompletionDate?exists>
                    var dtEnd = new Date("${taskNode.estimatedCompletionDate?string("yyyy/MM/dd HH:mm")}");
                   <#else>
                   var dtEnd = new Date("${chartEnd?string("yyyy/MM/dd HH:mm")}");
                </#if>
	            <#if taskNode.phaseName?exists>
	                json.push({high: dtEnd.getTime(), low: dtStart.getTime(), task: "${taskNode.phaseName}", type: "p"});
	            <#elseif taskNode.workEffortName?exists>
	                json.push({high: dtEnd.getTime(), low: dtStart.getTime(), task: "${taskNode.workEffortName}", type: "t"});
	            </#if>
	        </#list>
        </#if>

            //Parameters
            var chartStart = new Date("${chartStart?string("yyyy/MM/dd HH:mm")}");
            var dEnd = new Date("${chartEnd?string("yyyy/MM/dd HH:mm")}");
//          var duration = 14;      //Duration of the chart
            //Calculated parameters
            var nbDays = dojo.date.diff(chartStart, dtEnd, interv);

            var store = new dojo.collections.Store();
            store.setData(json);

            //  define the chart.
            var s1 = new dojo.charting.Series({
                dataSource:store,
                bindings:{ id:"id", high:"high", low:"low", label:"task", type:"type" },
                label:"Project tasks"
            });

            //  test the evaluate
/*
            var data = s1.evaluate();
            var a=[];
            for(var i=0; i<data.length; i++){
                a.push("{ high:"+data[i].high +", low:"+data[i].low + ", label:"+data[i].label + "}");
            }
            alert("Data evaluation:\n"+a.join("\n"));
*/          

            //////////////////////
            var data = s1.evaluate();
            
            
            //create the y-axis with task labels
            var yB2 = new dojo.charting.Axis();
            //Range is calculated to nbTasks * 10
            yB2.range={upper:parseInt(data.length * 30),lower:0};
            yB2.origin="min";
            yB2.showTicks = true;
            yB2.showLines = true;
            
            for(var i=data.length-1; i>=0; i--){
                yB2.labels.push({ label: data[i].label, value: parseInt((data.length - i)*30) });
            }

            //create the first x-axis (day-based)
            var xB = new dojo.charting.Axis();
            xB.range={upper:dtEnd.getTime(), lower:chartStart.getTime()};
            //setting the origin to more than y-axis.upper cause it to appear above the chart
            xB.origin = parseInt(yB2.range.upper + 30);
            xB.showTicks = true;
            xB.showLines = false;
            
            var dtStart = chartStart;
            for(var i = 0; i < nbDays; i++){
                xB.labels.push({ label: dateFormat(dtStart, '!ddd'), value: dtStart.getTime() });
                dtStart = dojo.date.add(dtStart, interv, 1);
            }
            
            //create the second x-axis (week-based)
            var xB2 = new dojo.charting.Axis();
            //use the same range as first axis
            xB2.range = xB.range;
            xB2.origin="min";
            xB2.showTicks = true;
            xB2.showLines = true;
            
            dtStart = chartStart;
            for(var i = 0; i < nbDays; i++){
                if(dateFormat(dtStart, '!ddd') == "Mon"){
                    xB2.labels.push({ label: dateFormat(dtStart, '!dd/!mm'), value: dtStart.getTime() });
                }
                dtStart = dojo.date.add(dtStart, interv, 1);
            }



//gantt series
            //to display second axis and labels on same chart, we need a first Plot object
            var p2 = new dojo.charting.Plot(xB2, yB2);

            //The second Plot object hold the data Series
            var p3 = new dojo.charting.Plot(xB, yB2);
            p3.renderType = dojo.charting.RenderPlotSeries.Grouped;
            p3.addSeries({ data:s1, plotter: dojo.charting.Plotters.Gantt });
            
            var pa2 = new dojo.charting.PlotArea();
            //Add the 2 Plot to the PlotArea
            pa2.plots.push(p2);
            pa2.plots.push(p3);
            
            //Calculate chart height & width
            var chartH = data.length * 30;  // height of the bars
            var chartW = nbDays * 50;  // width of one day

            pa2.size={width:chartW, height:chartH};
            pa2.padding={top:30, right:30, bottom:30, left:60 };
            
            //  auto assign colors, and increase the step 
            s1.color = pa2.nextColor();
            
            // Create the Chart and add the PlotArea
            var chart = new dojo.charting.Chart(null, "Test chart", "This is a potential description");
            chart.addPlotArea({ x:5,y:100, plotArea:pa2 }); // position of the chart on screen
            

            dojo.addOnLoad(function(){
                chart.node = dojo.byId("chartTest1");
                chart.render();
                document.getElementById("chartTest1").setAttribute("style", "width:" + chartW + "; height:" + chartH + ";");
                document.getElementById("plotLabels1").setAttribute("style", "width:" + chartW + "; height:" + chartH + ";");
                //Call PlotArea.render with the custom function
                pa2.render(s1, customPlot);

            });
            dojo.debug("--end callBack");
               
          dojo.event.connect(dojo, "loaded", "init")

    function customPlot(node, srcObject){
            //First solution for custom labels
            //Display labels in a div elt overlapping the graph

            var x = parseInt(node.getAttribute("x"));
            var y = parseInt(node.getAttribute("y"));
            var width = parseInt(node.getAttribute("width")) - 10;


            if(srcObject.type == "p"){
                node.setAttribute("y", y + 15);
                node.setAttribute("rx", "10");
                node.setAttribute("ry", "10");
                node.setAttribute("height", "5");
                node.setAttribute("type", "arc");
                node.setAttribute("style", "fill:#000000;fill-opacity:0.75000000;fill-rule:evenodd;stroke:#000000;stroke-width:1.0000000px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1.0000000");
//              dojo.debug(srcObject.type);
            }else if(srcObject.type == "j"){
                node.setAttribute("x", x + 15);
                node.setAttribute("y", y + 10);
                node.setAttribute("rx", "10");
                node.setAttribute("ry", "10");
                node.setAttribute("width", "10");
                node.setAttribute("height", "10");
                node.setAttribute("type", "arc");
                node.setAttribute("style", "fill:#00FF00;fill-opacity:0.75000000;fill-rule:evenodd;stroke:#000000;stroke-width:1.0000000px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1.0000000");
//              dojo.debug(srcObject.type);
            }else{
                node.setAttribute("height", "20");
            }

            var div = document.createElement("div");
            if(srcObject.type != "p"){
                if(srcObject.type == "j"){
                    div.setAttribute("class", "milestone-label");
                    x += 15;
                }else{
                    div.setAttribute("class", "bar-label");
                }
                div.setAttribute("style", "top:" + parseInt(y + 4) + "px; left:" + parseInt(x + 5) + "px; width:" + width + "px");
                div.appendChild(document.createTextNode(srcObject.task));
                document.getElementById("plotLabels1").appendChild(div);
            }
            //connect the mouseover event to the task label
            dojo.event.connect(div, "onmouseover", dj_global, "onMouseOver");

    }

    function onMouseOver(evt){
        logMe(evt);
    }   

    function logMe(evt){
        // FIXME: it appears that we're not actually getting this passed from IE!?!
        //if(!evt){ evt = window.event; }
        dojo.debug(evt.type + ' was fired');
/*
        lastEvt = dump(evt);
        for(var x in evt){
            dojo.debug(x+": "+evt[x]);
        }
*/
        dojo.debug("some event was fired");
    }

function dateFormat(aDate, displayPat){
    /********************************************************
    *   Valid Masks:
    *   !mmmm = Long month (eg. January)
    *   !mmm = Short month (eg. Jan)
    *   !mm = Numeric date (eg. 07)
    *   !m = Numeric date (eg. 7)
    *   !dddd = Long day (eg. Monday)
    *   !ddd = Short day (eg. Mon)
    *   !dd = Numeric day (eg. 07)
    *   !d = Numeric day (eg. 7)
    *   !yyyy = Year (eg. 1999)
    *   !yy = Year (eg. 99)
   ********************************************************/

    intMonth = aDate.getMonth();
    intDate = aDate.getDate();
    intDay = aDate.getDay();
    intYear = aDate.getFullYear();

    var months_long =  new Array ('January','February','March','April',
       'May','June','July','August','September','October','November','December')
    var months_short = new Array('Jan','Feb','Mar','Apr','May','Jun',
       'Jul','Aug','Sep','Oct','Nov','Dec')
    var days_long = new Array('Sunday','Monday','Tuesday','Wednesday',
       'Thursday','Friday','Saturday')
    var days_short = new Array('Sun','Mon','Tue','Wed','Thu','Fri','Sat')

    var mmmm = months_long[intMonth]
    var mmm = months_short[intMonth]
    var mm = intMonth < 9?'0'+ (1 + intMonth) + '':(1+intMonth)+'';
    var m = 1+intMonth+'';
    var dddd = days_long[intDay];
    var ddd = days_short[intDay];
    var dd = intDate<10?'0'+intDate+'':intDate+'';
    var d = intDate+'';
    var yyyy = intYear;

    century = 0;
    while((intYear-century)>=100)
        century = century + 100;

    var yy = intYear - century
    if(yy<10)
        yy = '0' + yy + '';

    displayDate = new String(displayPat);

    displayDate = displayDate.replace(/!mmmm/i,mmmm);
    displayDate = displayDate.replace(/!mmm/i,mmm);
    displayDate = displayDate.replace(/!mm/i,mm);
    displayDate = displayDate.replace(/!m/i,m);
    displayDate = displayDate.replace(/!dddd/i,dddd);
    displayDate = displayDate.replace(/!ddd/i,ddd);
    displayDate = displayDate.replace(/!dd/i,dd);
    displayDate = displayDate.replace(/!d/i,d);
    displayDate = displayDate.replace(/!yyyy/i,yyyy);
    displayDate = displayDate.replace(/!yy/i,yy);

    return displayDate;
}
</script>

<div id="chartTest1">
<div id="plotLabels1"/>
</div>
<br/><br/>
<#if tasks?has_content>
<br/><br/><br/><br/><br/><br/>
	<#list tasks as taskNode>
	<br/>
	</#list>
</#if>
