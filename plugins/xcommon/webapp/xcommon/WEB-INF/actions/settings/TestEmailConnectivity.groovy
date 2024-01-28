import com.fasterxml.jackson.databind.ObjectMapper
import com.simbaquartz.xcommon.util.AxUtilFormat
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime

String nowMoment = AxUtilFormat.formatDateLong(UtilDateTime.nowTimestamp())
mapper = new ObjectMapper()
try{
    String sendTo = parameters.sendTo
    String sendFrom = parameters.sendFrom
    def testEmailConnectivityContext = [
            userLogin: userLogin,
            sendFrom: sendFrom,
            sendTo: sendTo,
            subject: "Test email sent at " + nowMoment + "!",
            body: "This is a test email sent at " + nowMoment + "!",
    ]
    println("Sending email now...")
    Map serviceResult = run service: "sendMail", with: testEmailConnectivityContext
    println("Received service result as: " + serviceResult)
    String testResultMessage ="Email sent successfully To <" + sendTo + "> with subject: " + serviceResult.subject
    Map testResults = [
            emailConnected:true,
            message: testResultMessage
    ]
    mapper.writeValue(response.getWriter(), testResults)
}catch(Exception e){
    Debug.logError(e, "Sending email service failed with:" + e, "TestEmailConnectivity.groovy")
    String errorMessage = "Connectivity failed with error message: " + e.message
    mapper.writeValue(response.getWriter(), [emailConnected: false, errorResponse: errorMessage])
}
return "success"