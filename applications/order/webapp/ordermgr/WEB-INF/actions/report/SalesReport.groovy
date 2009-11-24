import org.ofbiz.base.util.*;

fromDateStr = parameters.fromDate;
thruDateStr = parameters.thruDate;

birtParameters = [:];
birtParameters.fromDate = UtilDateTime.toTimestamp(fromDateStr);
birtParameters.thruDate = UtilDateTime.toTimestamp(thruDateStr);

request.setAttribute("birtParameters", birtParameters);

return "success";