/* Copyright (c) 2008, Shlomy Gantz/BlueBrick Inc.
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of Shlomy Gantz or BlueBrick Inc. nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY SHLOMY GANTZ/BLUEBRICK INC. ''AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL SHLOMY GANTZ/BLUEBRICK INC. BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
   // function that loads each Task Item and fills TaskList array with each task attributes.

   // You should be able to add items to the chart in realtime via javascript and issuing "g.Draw()" command.

   // Parameters:

   // pID: (required) is a unique ID used to identify each row for parent functions and for setting dom id for hiding/showing

   // pName: (required) is the task Label

   // pStart: (required) the task start date, can enter empty date ('') for groups

   // pEnd: (required) the task end date, can enter empty date ('') for groups

   // pColor: (required) the html color for this task; e.g. '00ff00'

   // pLink: (optional) any http link navigated to when task bar is clicked.

   // pMile: UNUSED - in future will represent a milestone

   // pRes: (optional) resource name

   // pComp: (required) completion percent

   // pGroup: (optional) indicates whether this is a group(parent) - 0=NOT Parent; 1=IS Parent

   // pParent: (required) identifies a parent pID, this causes this task to be a child of identified task

   // pOpen: UNUSED - in future can be initially set to close folder when chart is first drawn


   // ******* 1024,768 replaced by 1024,768 by JLR *******


var JSGantt; if (!JSGantt) JSGantt = {};

var vTimeout = 0;

//function JSGantt() {}

JSGantt.isIE = function () {

	if(typeof document.all != 'undefined')
	return true;
	else
	return false;
	}

function Graphics(canvas)

{

	this.canvas = canvas;

	this.cache = new Array;

	this.shapes = new Object;

	this.nObject = 0;



	// defaults

	this.penColor = "black";

	this.zIndex = 0;

}


Graphics.prototype.createPlotElement = function(x,y,w,h) {
	// detect canvas
	if ( (this.canvas == undefined) || (this.canvas == "") )
		this.oCanvas = document.body;
	else
		this.oCanvas = document.getElementById(this.canvas);

	// retrieve DIV
	var oDiv;
	oDiv = document.createElement('div');
	this.oCanvas.appendChild(oDiv);

	oDiv.style.position = "absolute";
	oDiv.style.margin = "0px";
	oDiv.style.padding = "0px";
	oDiv.style.overflow = "hidden";
	oDiv.style.border = "0px";

	// set attributes
	oDiv.style.zIndex = this.zIndex;
	oDiv.style.backgroundColor = this.penColor;

	oDiv.style.left = x + "px";
	oDiv.style.top = y + "px";
	oDiv.style.width = w + "px";
	oDiv.style.height = h + "px";

	oDiv.style.visibility = "visible";

	return oDiv;
}


Graphics.prototype.releasePlotElement = function(oDiv)

{

	oDiv.style.visibility = "hidden";

	this.cache.push(oDiv);

}



Graphics.prototype.addShape = function(shape)

{

	shape.oGraphics = this;

	shape.graphicsID = this.nObject;

	this.shapes[this.nObject] = shape;

	this.nObject++;

	shape.draw();

	return shape;

}



Graphics.prototype.removeShape = function(shape)

{

	if ( (shape instanceof Object) &&

		(shape.oGraphics == this) &&

		(this.shapes[shape.graphicsID] == shape) )

	{

		shape.undraw();

		this.shapes[shape.graphicsID] = undefined;

		shape.oGraphics = undefined;

	}

}

Graphics.prototype.clear = function()

{

	for ( var i in this.shapes )

		this.removeShape(this.shapes[i]);

}





//=============================================================================

// Point

Graphics.prototype.drawPoint = function(x,y)

{

	return this.addShape(new Point(x,y))

}



function Point(x,y)

{

	this.x = x;

	this.y = y;

}

Point.prototype.draw = function()

{

	this.oDiv = this.oGraphics.createPlotElement(this.x,this.y,1,1);

}

Point.prototype.undraw = function()

{

	this.oGraphics.releasePlotElement(this.oDiv);

	this.oDiv = undefined;

}



//=============================================================================

// Line

Graphics.prototype.drawLine = function(x1,y1,x2,y2)

{

	return this.addShape(new Line(x1,y1,x2,y2))

}



function Line(x1,y1,x2,y2)

{

	this.x1 = x1;

	this.y1 = y1;

	this.x2 = x2;

	this.y2 = y2;

}



Line.prototype.draw = function()

{

	this.plots = new Array;



	var dx = this.x2 - this.x1;

	var dy = this.y2 - this.y1;

	var x = this.x1;

	var y = this.y1;



	var n = Math.max(Math.abs(dx),Math.abs(dy));

	dx = dx / n;

	dy = dy / n;

	for ( i = 0; i <= n; i++ )

	{

		this.plots.push(this.oGraphics.createPlotElement(Math.round(x),Math.round(y),1,1));



		x += dx;

		y += dy;

	}

}

Line.prototype.undraw = function()

{

	while ( this.plots.length )

		this.oGraphics.releasePlotElement(this.plots.pop());

	this.plots = undefined;

}





JSGantt.TaskItem = function(pID, pName, pStart, pEnd, pColor, pLink, pMile, pRes, pComp, pGroup, pParent, pOpen, pDepend)

   {



      var vID    = pID;

      var vName  = pName;

      var vStart = new Date();

      var vEnd   = new Date();

      var vColor = pColor;

      var vLink  = pLink;

      var vMile  = pMile;

      var vRes   = pRes;

      var vComp  = pComp;

      var vGroup = pGroup;

      var vParent = pParent;

      var vOpen   = pOpen;

      var vDepend = pDepend;

      var vLevel = 0;

      var vNumKid = 0;

      var vVisible  = 1;

      var x1, y1, x2, y2;





      if (vGroup != 1)

      {

         var vDateParts = pStart.split('/');

         vStart.setFullYear(parseInt(vDateParts[2], 10), parseInt(vDateParts[0], 10) - 1, parseInt(vDateParts[1], 10));

         vDateParts = pEnd.split('/');

         vEnd.setFullYear(parseInt(vDateParts[2], 10), parseInt(vDateParts[0], 10) - 1, parseInt(vDateParts[1], 10));

      }



      this.getID       = function(){ return vID };

      this.getName     = function(){ return vName };

      this.getStart    = function(){ return vStart};

      this.getEnd      = function(){ return vEnd  };

      this.getColor    = function(){ return vColor};

      this.getLink     = function(){ return vLink };

      this.getMile     = function(){ return vMile };

	  this.getDepend     = function(){ return vDepend };

      this.getResource = function(){ if(vRes) return vRes; else return '&nbsp';  };

      this.getCompVal  = function(){ if(vComp) return vComp; else return 0; };

      this.getCompStr  = function(){ if(vComp) return vComp+'%'; else return ''; };

      this.getDuration = function(vFormat){

        if (vMile) return '-';



		if(vFormat == 'day') {

          tmpDays =  Math.ceil((this.getEnd() - this.getStart()) /  (24 * 60 * 60 * 1000) + 1);

               if(tmpDays == 1) return (tmpDays + ' Day'); else return(tmpDays + ' Days');

        }

        if(vFormat == 'week') {

          tmpWeeks =  ((this.getEnd() - this.getStart()) /  (24 * 60 * 60 * 1000) + 1)/7;

               if(tmpWeeks == 1) return ('1 Week'); else return(tmpWeeks.toFixed(1) + ' Weeks');

        }

        if(vFormat == 'month') {

          tmpMonths =  ((this.getEnd() - this.getStart()) /  (24 * 60 * 60 * 1000) + 1)/30;

               if(tmpMonths == 1) return ('1 Month'); else return(tmpMonths.toFixed(1) + ' Months');

        }

      };

      this.getParent   = function(){ return vParent };

      this.getGroup    = function(){ return vGroup };

      this.getOpen     = function(){ return vOpen };

      this.getLevel    = function(){ return vLevel };

      this.getNumKids  = function(){ return vNumKid };

      this.getStartX   = function(){ return x1 };

      this.getStartY   = function(){ return y1 };

      this.getEndX     = function(){ return x2 };

      this.getEndY     = function(){ return y2 };

      this.getVisible  = function(){ return vVisible };



	  this.setDepend   = function(pDepend){ vDepend = pDepend;};

      this.setStart    = function(pStart){ vStart = pStart;};

      this.setEnd      = function(pEnd)  { vEnd   = pEnd;  };

      this.setLevel    = function(pLevel){ vLevel = pLevel;};

      this.setNumKid   = function(pNumKid){ vNumKid = pNumKid;};

      this.setCompVal  = function(pCompVal){ vComp = pCompVal;};

      this.setStartX   = function(pX) {x1 = pX; };

      this.setStartY   = function(pY) {y1 = pY; };

      this.setEndX     = function(pX) {x2 = pX; };

      this.setEndY     = function(pY) {y2 = pY; };

      this.setOpen     = function(pOpen) {vOpen = pOpen; };

      this.setVisible  = function(pVisible) {vVisible = pVisible; };

  }





  // function that loads the main gantt chart properties and functions

  // pDiv: (required) this is a DIV object created in HTML

  // pStart: UNUSED - future use to force minimum chart date

  // pEnd: UNUSED - future use to force maximum chart date

  // pWidth: UNUSED - future use to force chart width and cause objects to scale to fit within that width

  // pShowRes: UNUSED - future use to turn on/off display of resource names

  // pShowDur: UNUSED - future use to turn on/off display of task durations

  // pFormat: (required) - used to indicate whether chart should be drawn in "day", "week", or "month" format

JSGantt.GanttChart =  function(pGanttVar, pDiv, pFormat)

  {

      var vGanttVar = pGanttVar;

      var vDiv      = pDiv;

      var vFormat   = pFormat;

      var vShowRes  = 1;

      var vShowDur  = 1;

      var vShowComp = 1;

      var vNumUnits  = 0;

      var pWidth  = 1024;    // added by JLR

      var pHeight  = 768;    // added by JLR



      var gr = new Graphics('rightside');

      var vTaskList = new Array();


      var month=new Array(12);

          month[0]="January";

          month[1]="February";

          month[2]="March";

          month[3]="April";

          month[4]="May";

          month[5]="June";

          month[6]="July";

          month[7]="August";

          month[8]="September";

          month[9]="October";

          month[10]="November";

          month[11]="December";


      this.setShowRes  = function(pShow) { vShowRes  = pShow; };

      this.setShowDur  = function(pShow) { vShowDur  = pShow; };

      this.setShowComp = function(pShow) { vShowComp = pShow; };

      this.setFormat = function(pFormat){
         vFormat = pFormat;

         this.Draw();
      };



      this.getShowRes  = function(){ return vShowRes };

      this.getShowDur  = function(){ return vShowDur };

      this.getShowComp = function(){ return vShowComp };


      this.CalcTaskXY = function ()

      {

        var vList = this.getList();

        var vTaskDiv;

        var vParDiv;

        var vLeft, vTop, vHeight, vWidth;



        for(i = 0; i < vList.length; i++)

        {

          vID = vList[i].getID();



          vTaskDiv = document.getElementById("taskbar_"+vID);

          vBarDiv  = document.getElementById("bardiv_"+vID);

          vParDiv  = document.getElementById("childgrid_"+vID);



          if(vBarDiv) {

            vList[i].setStartX( vBarDiv.offsetLeft );

            vList[i].setStartY( vParDiv.offsetTop+vBarDiv.offsetTop+6 );

            vList[i].setEndX( vBarDiv.offsetLeft + vBarDiv.offsetWidth );

            vList[i].setEndY( vParDiv.offsetTop+vBarDiv.offsetTop+6 );

          }

       }

    }



    this.AddTaskItem = function(value)

    {

         vTaskList.push(value);

    }



    this.getList   = function() { return vTaskList };

    this.getGraphics = function() {return gr;};



    this.drawDependency =function(x1,y1,x2,y2)

    {

	var gr = this.getGraphics();

	gr.penColor = "red";

	if(x1 < x2)

	{

	 gr.drawLine(x1,y1,x1+5,y1);

	 gr.drawLine(x1+5,y1,x1+5,y2);

	 gr.drawLine(x1+5,y2,x2,y2);

	}

	else

	{

	var Xpoints = new Array(x1,x1+5,   x1+5,   x2-5,    x2-5,x2);

	var Ypoints = new Array(y1,y1,   y2-5,    y2-5,    y2,y2);

	 gr.drawLine(x1,y1,x1+5,y1);

	 gr.drawLine(x1+5,y1,x1+5,y2-10);

	 gr.drawLine(x1+5,y2-10,x2-5,y2-10);

	 gr.drawLine(x2-5,y2-10,x2-5,y2);

	 gr.drawLine(x2-5,y2,x2,y2);

	}

	}





	this.DrawDependencies = function ()
        {

		//First recalculate the x,y

		this.CalcTaskXY();



		var gr = this.getGraphics();

		gr.clear();



	  	var vList = this.getList();



           for(var i = 0; i < vList.length; i++)

           {

  		     //if(!isNaN(vList[i].getDepend()) && document.getElementById("childgrid_"+vList[i].getID()).style.display=='')
  		     if(!isNaN(vList[i].getDepend()) && vList[i].getVisible()==1)

    		 {

		       var ii = this.getArrayLocationByID(vList[i].getDepend());

			   //if(document.getElementById("childgrid_"+vList[ii].getID()).style.display=='')
			   if(vList[ii].getVisible()==1)

			   {

			     this.drawDependency(vList[ii].getEndX(),vList[ii].getEndY(),vList[i].getStartX(),vList[i].getStartY())

			   }

	        }

	     }

	}



	this.getArrayLocationByID = function(pId)  {



	 var vList = this.getList();

	 for(var i = 0; i < vList.length; i++)

	 {

	   if(vList[i].getID()==pId)

	   return i;

	 }



	}



    this.Draw = function()

    {

         var vCurrDate = new Date();

         var vMaxDate = new Date();

         var vMinDate = new Date();

         var vTmpDate = new Date();

         var vNxtDate = new Date();

         var vTaskLeft = 0;

         var vTaskRight = 0;

         var vNumCols = 0;

         var vID = 0;

         var vMainTable = "";

         var vLeftTable = "";

         var vRightTable = "";

         var vDateRowStr = "";

         var vItemRowStr = "";

         var vSpanSet = 0;

         var vColWidth = 0;

         var vColUnit = 0;

         var vChartWidth = 0;

         var vNumDays = 0;

         var vDayWidth = 0;

         var vStr = "";

         var vNameWidth = 220;

         var vStatusWidth = 70;

         var vLeftWidth = 15 + 220 + 70 + 70 + 70;

      if(vTaskList.length > 0)

      {


        vCurrDate.setFullYear(vCurrDate.getFullYear(), vCurrDate.getMonth(), vCurrDate.getDate());



        // Process all tasks preset parent date and completion %

        JSGantt.processRows(vTaskList, 0, -1, 1);



        // get overall min/max dates plus padding

        vMinDate = JSGantt.getMinDate(vTaskList, vFormat);

        vMaxDate = JSGantt.getMaxDate(vTaskList, vFormat);



        // Calculate chart width variables.  vColWidth can be altered manually to change each column width

        // May be smart to make this a parameter of GanttChart or set it based on existing pWidth parameter

        if(vFormat == 'day') {

          vColWidth = 18;

          vColUnit = 1;

        }

        if(vFormat == 'week') {

          vColWidth = 37;

          vColUnit = 7;

        }

        if(vFormat == 'month') {

          vColWidth = 37;

          vColUnit = 30;

        }





        vNumDays = Math.ceil((Date.parse(vMaxDate) - Date.parse(vMinDate)) / ( 24 * 60 * 60 * 1000));

        vNumUnits = vNumDays / vColUnit;

        vChartWidth = vNumUnits * vColWidth + 1;

        vDayWidth = (vColWidth / vColUnit) + (1/vColUnit);



        vMainTable =

           "<TABLE id=theTable cellSpacing=0 cellPadding=0 border=0><TBODY><TR>" +

           "<TD vAlign=top bgColor=#ffffff>";



         if(vShowRes !=1) vNameWidth+=vStatusWidth;
         if(vShowDur !=1) vNameWidth+=vStatusWidth;
         if(vShowComp!=1) vNameWidth+=vStatusWidth;

         // DRAW the Left-side of the chart (names, resources, comp%)

         vLeftTable =

           '<DIV class=scroll id=leftside style="width:' + vLeftWidth + 'px"><TABLE cellSpacing=0 cellPadding=0 border=0><TBODY>' +

           '<TR style="HEIGHT: 17px">' +

           '  <TD style="WIDTH: 15px; HEIGHT: 17px"></TD>' +

           '  <TD style="WIDTH: ' + vNameWidth + 'px; HEIGHT: 17px"><NOBR></NOBR></TD>';

           if(vShowRes ==1) vLeftTable += '  <TD style="WIDTH: ' + vStatusWidth + 'px; HEIGHT: 17px"></TD>' ;
           if(vShowDur ==1) vLeftTable += '  <TD style="WIDTH: ' + vStatusWidth + 'px; HEIGHT: 17px"></TD>' ;
           if(vShowComp==1) vLeftTable += '  <TD style="WIDTH: ' + vStatusWidth + 'px; HEIGHT: 17px"></TD>' ;

           vLeftTable +=
           '<TR style="HEIGHT: 20px">' +

           '  <TD style="BORDER-TOP: #efefef 1px solid; WIDTH: 15px; HEIGHT: 20px"></TD>' +

           '  <TD style="BORDER-TOP: #efefef 1px solid; WIDTH: ' + vNameWidth + 'px; HEIGHT: 20px"><NOBR></NOBR></TD>' ;

           if(vShowRes ==1) vLeftTable += '  <TD style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid; WIDTH: 60px; HEIGHT: 20px" align=center nowrap>Resource</TD>' ;
           if(vShowDur ==1) vLeftTable += '  <TD style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid; WIDTH: 60px; HEIGHT: 20px" align=center nowrap>Duration</TD>' ;
           if(vShowComp==1) vLeftTable += '  <TD style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid; WIDTH: 60px; HEIGHT: 20px" align=center nowrap>% Comp.</TD>' ;

           vLeftTable += '</TR>';


            for(i = 0; i < vTaskList.length; i++)

            {

               vID = vTaskList[i].getID();

  		       if(vTaskList[i].getVisible() == 0)

                 vLeftTable += '<TR id=child_' + vID + ' style="display:none">' ;

			   else

                 vLeftTable += '<TR id=child_' + vID + '>' ;


			   vLeftTable +=

                  '  <TD class=gdatehead style="WIDTH: 15px; HEIGHT: 20px; BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid;">&nbsp;</TD>' +

                  '  <TD class=gname style="WIDTH: ' + vNameWidth + 'px; HEIGHT: 20px; BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px;" nowrap><NOBR><span style="color: #aaaaaa">';


               for(j=1; j<vTaskList[i].getLevel(); j++) {

                  vLeftTable += '&nbsp&nbsp&nbsp&nbsp';

               }


               vLeftTable += '</span>';


               if( vTaskList[i].getGroup()) {

                 if( vTaskList[i].getOpen() == 1)
                   vLeftTable += '<SPAN id="group_' + vID + '" style="color:#000000; cursor:pointer; font:bold; FONT-SIZE: 12px;" onclick="JSGantt.folder(' + vID + ','+vGanttVar+');'+vGanttVar+'.DrawDependencies();">&ndash;</span><span style="color:#000000">&nbsp</SPAN>' ;
                 else
                   vLeftTable += '<SPAN id="group_' + vID + '" style="color:#000000; cursor:pointer; font:bold; FONT-SIZE: 12px;" onclick="JSGantt.folder(' + vID + ','+vGanttVar+');'+vGanttVar+'.DrawDependencies();">+</span><span style="color:#000000">&nbsp</SPAN>' ;

              } else {

                 vLeftTable += '<span style="color: #000000; font:bold; FONT-SIZE: 12px;">&nbsp&nbsp&nbsp</span>';
              }



              vLeftTable +=

                '<span onclick=JSGantt.taskLink("' + vTaskList[i].getLink() + '",1024,768); style="cursor:pointer"> ' + vTaskList[i].getName() + '</span></NOBR></TD>' ;

              if(vShowRes ==1) vLeftTable += '  <TD class=gname style="WIDTH: 60px; HEIGHT: 20px; TEXT-ALIGN: center; BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid;" align=center><NOBR>' + vTaskList[i].getResource() + '</NOBR></TD>' ;

              if(vShowDur ==1) vLeftTable += '  <TD class=gname style="WIDTH: 60px; HEIGHT: 20px; TEXT-ALIGN: center; BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid;" align=center><NOBR>' + vTaskList[i].getDuration(vFormat) + '</NOBR></TD>' ;

              if(vShowComp==1) vLeftTable += '  <TD class=gname style="WIDTH: 60px; HEIGHT: 20px; TEXT-ALIGN: center; BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid;" align=center><NOBR>' + vTaskList[i].getCompStr()  + '</NOBR></TD>' ;

              vLeftTable += '</TR>';



            }





            // DRAW the date format selector at bottom left.  Another potential GanttChart parameter to hide/show this selector

            vLeftTable += '</TD></TR>' +

              '<TR><TD border=1 colspan=5 align=left style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 11px; BORDER-LEFT: #efefef 1px solid; height=18px">&nbsp;&nbsp;Powered by <a href=http://www.jsgantt.com>jsGantt</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Format:';



            if (vFormat=='day') vLeftTable += '<INPUT TYPE=RADIO NAME="radFormat" VALUE="day" checked>Day';

            else                vLeftTable += '<INPUT TYPE=RADIO NAME="radFormat" onclick=JSGantt.changeFormat("day",'+vGanttVar+'); VALUE="day">Day';



            if (vFormat=='week') vLeftTable += '<INPUT TYPE=RADIO NAME="radFormat" VALUE="week" checked>Week';

            else                vLeftTable += '<INPUT TYPE=RADIO NAME="radFormat" onclick=JSGantt.changeFormat("week",'+vGanttVar+') VALUE="week">Week';



            if (vFormat=='month') vLeftTable += '<INPUT TYPE=RADIO NAME="radFormat" checked>Month';

            else                vLeftTable += '<INPUT TYPE=RADIO NAME="radFormat" onclick=JSGantt.changeFormat("month",'+vGanttVar+') VALUE="month">Month';



            vLeftTable += '</TD></TR></TBODY></TABLE></TD>';



            vMainTable += vLeftTable;





      // Draw the Chart Rows

      vRightTable =

      '<TD style="width: ' + vChartWidth + 'px;" vAlign=top bgColor=#ffffff>' +

      '<DIV class=scroll2 id=rightside>' +

      '<TABLE style="width: ' + vChartWidth + 'px;" cellSpacing=0 cellPadding=0 border=0>' +

      '<TBODY><TR style="HEIGHT: 18px">';



         vTmpDate.setFullYear(vMinDate.getFullYear(), vMinDate.getMonth(), vMinDate.getDate());



         // Major Date Header

         while(Date.parse(vTmpDate) <= Date.parse(vMaxDate))

         {



            vStr = vTmpDate.getFullYear() + '';

            vStr = vStr.substring(2,4);



	    if(vFormat == 'day')

            {

		vRightTable += '<td class=gdatehead style="FONT-SIZE: 12px; HEIGHT: 19px;" align=center colspan=7>' + (vTmpDate.getMonth()+1) + '/' + vTmpDate.getDate() + ' - ';

                vTmpDate.setDate(vTmpDate.getDate()+6);

		vRightTable += (vTmpDate.getMonth()+1) + '/' + vTmpDate.getDate() + '/'  + vStr + '</td>';

                vTmpDate.setDate(vTmpDate.getDate()+1);



           }

           if(vFormat == 'week')

           {

		vRightTable += '<td class=gdatehead align=center style="FONT-SIZE: 12px; HEIGHT: 19px;" width='+vColWidth+'px>`'+ vStr + '</td>';

                vTmpDate.setDate(vTmpDate.getDate()+7);

           }

           if(vFormat == 'month')

           {

	     vRightTable += '<td class=gdatehead align=center style="FONT-SIZE: 12px; HEIGHT: 19px;" width='+vColWidth+'px>`'+ vStr + '</td>';

             vTmpDate.setDate(vTmpDate.getDate() + 1);

             while(vTmpDate.getDate() > 1)

             {

               vTmpDate.setDate(vTmpDate.getDate() + 1);

             }

           }



        }





        vRightTable += '</TR><TR>';



         // Minor Date header and Cell Rows

         vTmpDate.setFullYear(vMinDate.getFullYear(), vMinDate.getMonth(), vMinDate.getDate());

         vNxtDate.setFullYear(vMinDate.getFullYear(), vMinDate.getMonth(), vMinDate.getDate());

         vNumCols = 0;



         while(Date.parse(vTmpDate) <= Date.parse(vMaxDate))

         {



	    if(vFormat == 'day')

            {



              if(vTmpDate.getDay() % 6 == 0) {

                vDateRowStr += '<td class="gheadwkend" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; HEIGHT: 19px; BORDER-LEFT: #efefef 1px solid;" bgcolor=#cfcfcf align=center><div style="width: '+vColWidth+'px">' + vTmpDate.getDate() + '</div></td>';

                vItemRowStr += '<td class="gheadwkend" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid; cursor: default;" bgcolor=#cfcfcf align=center><div style="width: '+vColWidth+'px">&nbsp</div></td>';

              }

              else {

                vDateRowStr += '<td class="ghead" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; HEIGHT: 19px; BORDER-LEFT: #efefef 1px solid;" align=center><div style="width: '+vColWidth+'px">' + vTmpDate.getDate() + '</div></td>';

                vItemRowStr += '<td class="ghead" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid; cursor: default;" align=center><div style="width: '+vColWidth+'px">&nbsp&nbsp</div></td>';

              }



              vTmpDate.setDate(vTmpDate.getDate() + 1);



            }



	    if(vFormat == 'week')

            {



              vNxtDate.setDate(vNxtDate.getDate() + 7);

              if(vNxtDate <= vMaxDate) {

                vDateRowStr += '<td class="ghead" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; HEIGHT: 19px; BORDER-LEFT: #efefef 1px solid;" align=center width:'+vColWidth+'px><div style="width: '+vColWidth+'px">' + (vTmpDate.getMonth()+1) + '/' + vTmpDate.getDate() + '</div></td>';

                vItemRowStr += '<td class="ghead" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid;" align=center><div style="width: '+vColWidth+'px">&nbsp&nbsp</div></td>';

              } else {

                vDateRowStr += '<td class="ghead" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; HEIGHT: 19px; BORDER-LEFT: #efefef 1px solid; BORDER-RIGHT: #efefef 1px solid;" align=center width:'+vColWidth+'px><div style="width: '+vColWidth+'px">' + (vTmpDate.getMonth()+1) + '/' + vTmpDate.getDate() + '</div></td>';

                vItemRowStr += '<td class="ghead" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid; BORDER-RIGHT: #efefef 1px solid;" align=center><div style="width: '+vColWidth+'px">&nbsp&nbsp</div></td>';

              }



              vTmpDate.setDate(vTmpDate.getDate() + 7);



            }



	    if(vFormat == 'month')

            {



              vNxtDate.setDate(vNxtDate.getDate() + 31);

              if(vNxtDate <= vMaxDate) {

                vDateRowStr += '<td class="ghead" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; HEIGHT: 19px; BORDER-LEFT: #efefef 1px solid;" align=center width:'+vColWidth+'px><div style="width: '+vColWidth+'px">' + month[vTmpDate.getMonth()].substr(0,3) + '</div></td>';

                vItemRowStr += '<td class="ghead" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid;" align=center><div style="width: '+vColWidth+'px">&nbsp&nbsp</div></td>';

              } else {

                vDateRowStr += '<td class="ghead" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; HEIGHT: 19px; BORDER-LEFT: #efefef 1px solid; BORDER-RIGHT: #efefef 1px solid;" align=center width:'+vColWidth+'px><div style="width: '+vColWidth+'px">' + month[vTmpDate.getMonth()].substr(0,3) + '</div></td>';

                vItemRowStr += '<td class="ghead" style="BORDER-TOP: #efefef 1px solid; FONT-SIZE: 12px; BORDER-LEFT: #efefef 1px solid; BORDER-RIGHT: #efefef 1px solid;" align=center><div style="width: '+vColWidth+'px">&nbsp&nbsp</div></td>';

              }



              vTmpDate.setDate(vTmpDate.getDate() + 1);

              while(vTmpDate.getDate() > 1)

              {

                vTmpDate.setDate(vTmpDate.getDate() + 1);

              }



            }



        }







        vRightTable += vDateRowStr + '</TR>';

        vRightTable += '</TBODY></TABLE>';




        // Draw each row

        for(i = 0; i < vTaskList.length; i++)

        {

           vTmpDate.setFullYear(vMinDate.getFullYear(), vMinDate.getMonth(), vMinDate.getDate());

           vTaskStart = vTaskList[i].getStart();

           vTaskEnd   = vTaskList[i].getEnd();



           vNumCols = 0;

           vID = vTaskList[i].getID();



           vNumUnits = Math.ceil((vTaskList[i].getEnd() - vTaskList[i].getStart()) / (24 * 60 * 60 * 1000)) + 1;



           vSpanSet = 0;



	       if(vTaskList[i].getVisible() == 0)

             vRightTable += '<DIV id=childgrid_' + vID + ' style="position:relative; display:none;">';

           else

		     vRightTable += '<DIV id=childgrid_' + vID + ' style="position:relative">';



           vRightTable += '<DIV><TABLE style="position:relative; top:0px; width: ' + vChartWidth + 'px;" cellSpacing=0 cellPadding=0 border=0><TR class=yesdisplay style="HEIGHT: 20px">' + vItemRowStr + '</TR></TABLE></DIV>';



             if( vTaskList[i].getMile()) {



               // Build date string for Title

               vStr = vTaskStart.getFullYear() + '';

               vStr = vStr.substring(2,4);

               vDateRowStr = vTaskStart.getMonth() + '/' + vTaskStart.getDate() + '/' + vStr;



               vTaskLeft = (Date.parse(vTaskList[i].getStart()) - Date.parse(vMinDate)) / (24 * 60 * 60 * 1000);

               vTaskRight = 1



		vRightTable +=

                   '<div id=bardiv_' + vID + ' style="position:absolute; top:0px; left:' + Math.ceil((vTaskLeft * (vDayWidth) + 1)) + 'px; height: 16px; width:12px; overflow:hidden;">' +

                   '<div id=taskbar_' + vID + ' title="' + vTaskList[i].getName() + ': ' + vDateRowStr + '" style="height: 16px; width:12px; overflow:hidden; cursor: pointer;" onclick=JSGantt.taskLink("' + vTaskList[i].getLink() + '",1024,768);>';



                if(vTaskList[i].getCompVal() < 100)

 		  vRightTable += '&loz;</div></div>' ;

                else

 		  vRightTable += '&diams;</div></div>' ;



             } else {



               // Build date string for Title

               vStr = vTaskStart.getFullYear() + '';

               vStr = vStr.substring(2,4);

               vDateRowStr = vTaskStart.getMonth() + '/' + vTaskStart.getDate() + '/' + vStr;

               vStr = vTaskEnd.getFullYear() + '';

               vStr = vStr.substring(2,4);


               vDateRowStr += ' - ' + vTaskEnd.getMonth() + '/' + vTaskEnd.getDate() + '/' + vStr;



               vTaskLeft = (Date.parse(vTaskList[i].getStart()) - Date.parse(vMinDate)) / (24 * 60 * 60 * 1000);

               vTaskRight = (Date.parse(vTaskList[i].getEnd()) - Date.parse(vTaskList[i].getStart())) / (24 * 60 * 60 * 1000) + 1/vColUnit;



               // Draw Group Bar  which has outer div with inner group div and several small divs to left and right to create angled-end indicators

               if( vTaskList[i].getGroup()) {

		vRightTable +=

                   '<div id=bardiv_' + vID + ' style="position:absolute; top:5px; left:' + Math.ceil(vTaskLeft * (vDayWidth) + 1) + 'px; height: 7px; width:' + Math.ceil((vTaskRight) * (vDayWidth) - 1) + 'px">' +

                   '<div id=taskbar_' + vID + ' title="' + vTaskList[i].getName() + ': ' + vDateRowStr + '" class=gtask style="background-color:#000000; height: 7px; width:' + Math.ceil((vTaskRight) * (vDayWidth) -1) + 'px;  cursor: pointer;">' +

                   '<div style="Z-INDEX: -4; float:left; background-color:#666666; height:3px; overflow: hidden; margin-top:1px; ' +

                      'margin-left:1px; margin-right:1px; filter: alpha(opacity=80); opacity:0.8; width:' + vTaskList[i].getCompStr() + '; ' +

                      'cursor: pointer;" onclick=JSGantt.taskLink("' + vTaskList[i].getLink() + '",1024,768);></div></div>' +

                   '<div style="Z-INDEX: -4; float:left; background-color:#000000; height:4px; overflow: hidden; width:1px;"></div>' +

                   '<div style="Z-INDEX: -4; float:right; background-color:#000000; height:4px; overflow: hidden; width:1px;"></div>' +

                   '<div style="Z-INDEX: -4; float:left; background-color:#000000; height:3px; overflow: hidden; width:1px;"></div>' +

                   '<div style="Z-INDEX: -4; float:right; background-color:#000000; height:3px; overflow: hidden; width:1px;"></div>' +

                   '<div style="Z-INDEX: -4; float:left; background-color:#000000; height:2px; overflow: hidden; width:1px;"></div>' +

                   '<div style="Z-INDEX: -4; float:right; background-color:#000000; height:2px; overflow: hidden; width:1px;"></div>' +

                   '<div style="Z-INDEX: -4; float:left; background-color:#000000; height:1px; overflow: hidden; width:1px;"></div>' +

                   '<div style="Z-INDEX: -4; float:right; background-color:#000000; height:1px; overflow: hidden; width:1px;"></div>' +

                   '</div>' ;



               } else {

                // Draw Task Bar  which has outer DIV with enclosed colored bar div, and opaque completion div

		vRightTable +=

                   '<div id=bardiv_' + vID + ' style="position:absolute; top:4px; left:' + Math.ceil(vTaskLeft * (vDayWidth) + 1) + 'px; width:' + Math.ceil((vTaskRight) * (vDayWidth) - 1) + 'px">' +

                   '<div id=taskbar_' + vID + ' title="' + vTaskList[i].getName() + ': ' + vDateRowStr + '" class=gtask style="background-color:#' + vTaskList[i].getColor() +'; height: 13px; width:' + Math.ceil((vTaskRight) * (vDayWidth) - 1) + 'px; cursor: pointer;" ' +

                     'onclick=JSGantt.taskLink("' + vTaskList[i].getLink() + '",1024,768);>' +

                   '<div class=gcomplete style="Z-INDEX: -4; float:left; background-color:black; height:5px; overflow: auto; margin-top:4px; filter: alpha(opacity=40); opacity:0.4; width:' + vTaskList[i].getCompStr() + '; overflow:hidden"></div></div></div>' ;



               }



             }



          vRightTable += '</DIV>';

        }

          vMainTable += vRightTable + '</DIV></TD></TR></TBODY></TABLE></BODY></HTML>';


            vDiv.innerHTML = vMainTable;

         }

      }


   }







      // Recursively process task tree ... set min, max dates of parent tasks and identfy task level.

JSGantt.processRows = function(pList, pID, pRow, pLevel)

      {



         var vMinDate = new Date();

         var vMaxDate = new Date();

         var vMinSet  = 0;

         var vMaxSet  = 0;

         var vList    = pList;

         var vLevel   = pLevel;

         var i        = 0;

         var vNumKid  = 0;

         var vCompSum = 0;



         for(i = 0; i < pList.length; i++)

         {



            if(pList[i].getParent() == pID) {



               pList[i].setLevel(vLevel);

               vNumKid++;



               if(pList[i].getGroup() == 1) {

                  JSGantt.processRows(vList, pList[i].getID(), i, vLevel+1);

               }



               if( vMinSet==0 || pList[i].getStart() < vMinDate) {

                  vMinDate = pList[i].getStart();

                  vMinSet = 1;

               }



               if( vMaxSet==0 || pList[i].getEnd() > vMaxDate) {

                  vMaxDate = pList[i].getEnd();

                  vMaxSet = 1;

               }



               vCompSum += pList[i].getCompVal();



            }



         }



         if(pRow >= 0) {

            pList[pRow].setStart(vMinDate);

            pList[pRow].setEnd(vMaxDate);

            pList[pRow].setNumKid(vNumKid);

            pList[pRow].setCompVal(Math.ceil(vCompSum/vNumKid));

         }



      }







      // Used to determine the minimum date of all tasks and set lower bound based on format

JSGantt.getMinDate = function getMinDate(pList, pFormat)

      {

         var vDate = new Date();

         vDate.setFullYear(pList[0].getStart().getFullYear(), pList[0].getStart().getMonth(), pList[0].getStart().getDate());



         // Parse all Task End dates to find min

         for(i = 0; i < pList.length; i++)

         {

            if(Date.parse(pList[i].getStart()) < Date.parse(vDate))

               vDate.setFullYear(pList[i].getStart().getFullYear(), pList[i].getStart().getMonth(), pList[i].getStart().getDate());

         }





         // Adjust min date to specific format boundaries (first of week or first of month)

         if (pFormat=='day')

         {

            vDate.setDate(vDate.getDate() - 1);

            while(vDate.getDay() % 7 > 0)

            {

                vDate.setDate(vDate.getDate() - 1);

            }

         }



         if (pFormat=='week')

         {

            vDate.setDate(vDate.getDate() - 7);

            while(vDate.getDay() % 7 > 0)

            {

                vDate.setDate(vDate.getDate() - 1);

            }

         }



         if (pFormat=='month')

         {

            while(vDate.getDate() > 1)

            {

                vDate.setDate(vDate.getDate() - 1);

            }

         }



         return(vDate);

      }







      // Used to determine the minimum date of all tasks and set lower bound based on format

JSGantt.getMaxDate= function (pList, pFormat)

      {

         var vDate = new Date();

         vDate.setFullYear(pList[0].getEnd().getFullYear(), pList[0].getEnd().getMonth(), pList[0].getEnd().getDate());



         // Parse all Task End dates to find max

         for(i = 0; i < pList.length; i++)

         {

            if(Date.parse(pList[i].getEnd()) > Date.parse(vDate))

            vDate.setFullYear(pList[i].getEnd().getFullYear(), pList[i].getEnd().getMonth(), pList[i].getEnd().getDate());

         }



         // Adjust max date to specific format boundaries (end of week or end of month)

         if (pFormat=='day')

         {

            vDate.setDate(vDate.getDate() + 1);

            while(vDate.getDay() % 6 > 0)

            {

                vDate.setDate(vDate.getDate() + 1);

            }

         }



         if (pFormat=='week')

         {

            //For weeks, what is the last logical boundary?

            vDate.setDate(vDate.getDate() + 11);

            while(vDate.getDay() % 6 > 0)

            {

                vDate.setDate(vDate.getDate() + 1);

            }

         }



         // Set to last day of current Month

         if (pFormat=='month')

         {

            while(vDate.getDay() > 1)

            {

                vDate.setDate(vDate.getDate() + 1);

            }

            vDate.setDate(vDate.getDate() - 1);

         }



         return(vDate);

      }







      // This function finds the document id of the specified object

JSGantt.findObj = function (theObj, theDoc)

      {

         var p, i, foundObj;

         if(!theDoc) theDoc = document;

         if( (p = theObj.indexOf("?")) > 0 && parent.frames.length){

            theDoc = parent.frames[theObj.substring(p+1)].document;

            theObj = theObj.substring(0,p);

         }

         if(!(foundObj = theDoc[theObj]) && theDoc.all)

            foundObj = theDoc.all[theObj];



         for (i=0; !foundObj && i < theDoc.forms.length; i++)

            foundObj = theDoc.forms[i][theObj];



         for(i=0; !foundObj && theDoc.layers && i < theDoc.layers.length; i++)

            foundObj = JSGantt.findObj(theObj,theDoc.layers[i].document);



         if(!foundObj && document.getElementById)

            foundObj = document.getElementById(theObj);



         return foundObj;

      }





JSGantt.changeFormat =      function(pFormat,ganttObj) {



        if(ganttObj)

		{

		ganttObj.setFormat(pFormat);

		ganttObj.DrawDependencies();

		}

        else

           alert('Chart undefined');



      }





      // Function to open/close and hide/show children of specified task

 JSGantt.folder= function (pID,ganttObj) {


        var vList = ganttObj.getList();

        for(i = 0; i < vList.length; i++)

        {

          if(vList[i].getID() == pID) {

              if( vList[i].getOpen() == 1 ) {

  			    vList[i].setOpen(0);

                JSGantt.hide(pID,ganttObj);

                if (JSGantt.isIE())
                  JSGantt.findObj('group_'+pID).innerText = '+';
                else
                  JSGantt.findObj('group_'+pID).textContent = '+';

              } else {

  			    vList[i].setOpen(1);

                JSGantt.show(pID, 1, ganttObj);

                if (JSGantt.isIE())
                  JSGantt.findObj('group_'+pID).innerText = '–';
                else
                  JSGantt.findObj('group_'+pID).textContent = '–';

              }

          }

        }

      }





  JSGantt.hide=     function (pID,ganttObj) {

        var vList = ganttObj.getList();

        var vID   = 0;

        for(var i = 0; i < vList.length; i++)

        {

          if(vList[i].getParent() == pID) {

            vID = vList[i].getID();

            JSGantt.findObj('child_' + vID).style.display = "none";

            JSGantt.findObj('childgrid_' + vID).style.display = "none";

            vList[i].setVisible(0);

            if(vList[i].getGroup() == 1)

              JSGantt.hide(vID,ganttObj);

          }

        }

      }





      // Function to show children of specified task

     JSGantt.show =  function (pID, pTop, ganttObj) {

        var vList = ganttObj.getList();

        var vID   = 0;

        for(var i = 0; i < vList.length; i++)

        {

          if(vList[i].getParent() == pID) {

            vID = vList[i].getID();

            if(pTop == 1) {

              if (JSGantt.isIE()) { // IE;



                if( JSGantt.findObj('group_'+pID).innerText == '+') {

                  JSGantt.findObj('child_'+vID).style.display = "";

                  JSGantt.findObj('childgrid_'+vID).style.display = "";

                  vList[i].setVisible(1);

                }



              } else {



                if( JSGantt.findObj('group_'+pID).textContent == '+') {

                  JSGantt.findObj('child_'+vID).style.display = "";

                  JSGantt.findObj('childgrid_'+vID).style.display = "";

                  vList[i].setVisible(1);

                }

              }

            } else {

              if (JSGantt.isIE()) { // IE;



                if( JSGantt.findObj('group_'+pID).innerText == '–') {

                  JSGantt.findObj('child_'+vID).style.display = "";

                  JSGantt.findObj('childgrid_'+vID).style.display = "";

                  vList[i].setVisible(1);

                }



              } else {



                if( JSGantt.findObj('group_'+pID).textContent == '–') {

                  JSGantt.findObj('child_'+vID).style.display = "";

                  JSGantt.findObj('childgrid_'+vID).style.display = "";

                  vList[i].setVisible(1);

                }

              }

            }


            if(vList[i].getGroup() == 1)

              JSGantt.show(vID, 0,ganttObj);



          }

        }

      }





  // function to open window to display task link

JSGantt.taskLink = function(pRef,pWidth,pHeight)

  {

    if(pWidth)  vWidth =pWidth;  else vWidth =400;
    if(pHeight) vHeight=pHeight; else vHeight=400;

    var OpenWindow=window.open(pRef, "newwin", "height="+vHeight+",width="+vWidth);

  }


JSGantt.parseXML = function(ThisFile,pGanttVar){
	var is_chrome = navigator.userAgent.toLowerCase().indexOf('chrome') > -1;   // Is this Chrome

	try { //Internet Explorer
		xmlDoc=new ActiveXObject("Microsoft.XMLDOM");
		}
	catch(e) {
		try { //Firefox, Mozilla, Opera, Chrome etc.
			if (is_chrome==false) {  xmlDoc=document.implementation.createDocument("","",null); }
		}
		catch(e) {
			alert(e.message);
			return;
		}
	}

	if (is_chrome==false) { 	// can't use xmlDoc.load in chrome at the moment
		xmlDoc.async=false;
		xmlDoc.load(ThisFile);		// we can use  loadxml
		JSGantt.AddXMLTask(pGanttVar)
		xmlDoc=null;			// a little tidying
		Task = null;
	}
	else {
		JSGantt.ChromeLoadXML(ThisFile,pGanttVar);
		ta=null;	// a little tidying
	}
}

JSGantt.AddXMLTask = function(pGanttVar){

	Task=xmlDoc.getElementsByTagName("task");

	var n = xmlDoc.documentElement.childNodes.length;	// the number of tasks. IE gets this right, but mozilla add extra ones (Whitespace)

	for(var i=0;i<n;i++) {

		// optional parameters may not have an entry (Whitespace from mozilla also returns an error )
		// Task ID must NOT be zero other wise it will be skipped
		try { pID = Task[i].getElementsByTagName("pID")[0].childNodes[0].nodeValue;
		} catch (error) {pID =0;}
		pID *= 1;	// make sure that these are numbers rather than strings in order to make jsgantt.js behave as expected.

		if(pID!=0){
	 		try { pName = Task[i].getElementsByTagName("pName")[0].childNodes[0].nodeValue;
			} catch (error) {pName ="No Task Name";}			// If there is no corresponding entry in the XML file the set a default.

			try { pColor = Task[i].getElementsByTagName("pColor")[0].childNodes[0].nodeValue;
			} catch (error) {pColor ="0000ff";}

			try { pParent = Task[i].getElementsByTagName("pParent")[0].childNodes[0].nodeValue;
			} catch (error) {pParent =0;}
			pParent *= 1;

			try { pStart = Task[i].getElementsByTagName("pStart")[0].childNodes[0].nodeValue;
			} catch (error) {pStart ="";}

			try { pEnd = Task[i].getElementsByTagName("pEnd")[0].childNodes[0].nodeValue;
			} catch (error) { pEnd ="";}

			try { pLink = Task[i].getElementsByTagName("pLink")[0].childNodes[0].nodeValue;
			} catch (error) { pLink ="";}

			try { pMile = Task[i].getElementsByTagName("pMile")[0].childNodes[0].nodeValue;
			} catch (error) { pMile=0;}
			pMile *= 1;

			try { pRes = Task[i].getElementsByTagName("pRes")[0].childNodes[0].nodeValue;
			} catch (error) { pRes ="";}

			try { pComp = Task[i].getElementsByTagName("pComp")[0].childNodes[0].nodeValue;
			} catch (error) {pComp =0;}
			pComp *= 1;

			try { pGroup = Task[i].getElementsByTagName("pGroup")[0].childNodes[0].nodeValue;
			} catch (error) {pGroup =0;}
			pGroup *= 1;

			try { pOpen = Task[i].getElementsByTagName("pOpen")[0].childNodes[0].nodeValue;
			} catch (error) { pOpen =1;}
			pOpen *= 1;

			try { pDepend = Task[i].getElementsByTagName("pDepend")[0].childNodes[0].nodeValue;
			} catch (error) { pDepend =0;}
			pDepend *= 1;
			if (pDepend==0){pDepend='x'} // need this to draw the dependency lines

			// Finally add the task
			pGanttVar.AddTaskItem(new JSGantt.TaskItem(pID , pName, pStart, pEnd, pColor,  pLink, pMile, pRes,  pComp, pGroup, pParent, pOpen, pDepend));
		}
	}
}

JSGantt.ChromeLoadXML = function(ThisFile,pGanttVar){
// Thanks to vodobas at mindlence,com for the initial pointers here.
	XMLLoader = new XMLHttpRequest();
	XMLLoader.onreadystatechange= function(){
    JSGantt.ChromeXMLParse(pGanttVar);
	};
	XMLLoader.open("GET", ThisFile, false);
	XMLLoader.send(null);
}

JSGantt.ChromeXMLParse = function (pGanttVar){
// Manually parse the file as it is loads quicker
	if (XMLLoader.readyState == 4) {
		var ta=XMLLoader.responseText.split(/<task>/gi);

		var n = ta.length;	// the number of tasks.
		for(var i=1;i<n;i++) {
			Task = ta[i].replace(/<[/]p/g, '<p');
			var te = Task.split(/<pid>/i)

			if(te.length> 2){var pID=te[1];} else {var pID = 0;}
			pID *= 1;

			var te = Task.split(/<pName>/i)
			if(te.length> 2){var pName=te[1];} else {var pName = "No Task Name";}

			var te = Task.split(/<pstart>/i)
			if(te.length> 2){var pStart=te[1];} else {var pStart = "";}

			var te = Task.split(/<pEnd>/i)
			if(te.length> 2){var pEnd=te[1];} else {var pEnd = "";}

			var te = Task.split(/<pColor>/i)
			if(te.length> 2){var pColor=te[1];} else {var pColor = '0000ff';}

			var te = Task.split(/<pLink>/i)
			if(te.length> 2){var pLink=te[1];} else {var pLink = "";}

			var te = Task.split(/<pMile>/i)
			if(te.length> 2){var pMile=te[1];} else {var pMile = 0;}
			pMile  *= 1;

			var te = Task.split(/<pRes>/i)
			if(te.length> 2){var pRes=te[1];} else {var pRes = "";}

			var te = Task.split(/<pComp>/i)
			if(te.length> 2){var pComp=te[1];} else {var pComp = 0;}
			pComp  *= 1;

			var te = Task.split(/<pGroup>/i)
			if(te.length> 2){var pGroup=te[1];} else {var pGroup = 0;}
			pGroup *= 1;

			var te = Task.split(/<pParent>/i)
			if(te.length> 2){var pParent=te[1];} else {var pParent = 0;}
			pParent *= 1;

			var te = Task.split(/<pOpen>/i)
			if(te.length> 2){var pOpen=te[1];} else {var pOpen = 1;}
			pOpen *= 1;

			var te = Task.split(/<pDepend>/i)
			if(te.length> 2){var pDepend=te[1];} else {var pDepend = "x";}
			pDepend *= 1;
			if (pDepend==0){pDepend='x'} // need this to draw the dependency lines


			// Finally add the task
			pGanttVar.AddTaskItem(new JSGantt.TaskItem(pID , pName, pStart, pEnd, pColor,  pLink, pMile, pRes,  pComp, pGroup, pParent, pOpen, pDepend));
		}
	}
}
