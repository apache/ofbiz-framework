import org.ofbiz.base.util.*;
import java.sql.*;

fromDateStr = parameters.fromDate;
thruDateStr = parameters.thruDate;

Debug.logInfo("================ fromDateStr:" + fromDateStr, "");
Debug.logInfo("================ thruDateStr:" + thruDateStr, "");

birtParameters = [:];
try {
	birtParameters.fromDate = Date.valueOf(fromDateStr);
	birtParameters.thruDate = Date.valueOf(thruDateStr);
	//birtParameters.fromDate = Timestamp.valueOf(fromDateStr);
	//birtParameters.thruDate = Timestamp.valueOf(thruDateStr);
} catch (e) {
	Debug.logError(e, "");
}

Debug.logInfo("================ fromDate:" + birtParameters.fromDate, "");
Debug.logInfo("================ thruDate:" + birtParameters.thruDate, "");

request.setAttribute("birtParameters", birtParameters);

return "success";
