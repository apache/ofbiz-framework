
    <div id="params_birtReport" style='display:none'>
        <INPUT type="HIDDEN" name="productId" value="${product.productId}"/>
    </div>
    <form id="form_birtReport" method="post"></form>
    <script type="text/javascript">
    function loadViewerbirtReport(){
    var formObj = document.getElementById( "form_birtReport" );
    var paramContainer = document.getElementById("params_birtReport");
    var oParams = paramContainer.getElementsByTagName('input');
    if( oParams )
    {
      for( var i=0;i<oParams.length;i++ )  
      {
        var param = document.createElement( "INPUT" );
        param.type = "HIDDEN";
        param.name= oParams[i].name;
        param.value= oParams[i].value;
        formObj.appendChild(param);
        
      }
    }
    formObj.action = "/birt/preview?__page=2&__report=component://scrum/webapp/scrum/reports/BacklogChart.rptdesign&__masterpage=true&__format=html";
    formObj.target = "birtReport";
    formObj.submit( );
    }
    
    </script>
    <iframe name="birtReport" frameborder="no"  scrolling = "auto"  style='height:350px;width:100%;' ></iframe>
    <script type="text/javascript">loadViewerbirtReport();</script> 