<%@ page import="org.eclipse.birt.report.utility.ParameterAccessor,
				 org.eclipse.birt.report.IBirtConstants,
				 org.eclipse.birt.report.session.*" %>

<%-- Map Java attributes to Javascript constants --%>
<script type="text/javascript">
// <![CDATA[
            
    Constants.nullValue = '<%= IBirtConstants.NULL_VALUE %>';
    
	// Request attributes
	if ( !Constants.request )
	{
		Constants.request = {};
	}
	Constants.request.format = '<%= ParameterAccessor.getFormat(request) %>';
	Constants.request.rtl = <%= ParameterAccessor.isRtl( request ) %>;
	Constants.request.isDesigner = <%= ParameterAccessor.isDesigner() %>;
	Constants.request.servletPath = "<%= request.getAttribute( "ServletPath" ) %>".substr(1);
	<%  IViewingSession viewingSession = ViewingSessionUtil.getSession(request);
		String subSessionId = null;
		if ( viewingSession != null )
		{
			subSessionId = viewingSession.getId();
		}%>
	Constants.viewingSessionId = <%= subSessionId!=null?"\"" + subSessionId + "\"":"null" %>;	
// ]]>
</script>
