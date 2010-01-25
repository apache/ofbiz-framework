import org.ofbiz.base.util.*;
import java.sql.*;

fromDateStr = parameters.fromDate;
thruDateStr = parameters.thruDate;

birtParameters = [:];
try {
	birtParameters.fromDate = Date.valueOf(fromDateStr);
	birtParameters.thruDate = Date.valueOf(thruDateStr);
} catch (e) {
	Debug.logError(e, "");
}

request.setAttribute("birtParameters", birtParameters);

return "success";
