import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import software.amazon.awssdk.regions.Region

import javax.persistence.GeneratedValue

/**
 * Allows management of application settings, AWS/Email/CDN set up etc.
 * Great tutorial about groovy <a href="https://cwiki.apache.org/confluence/display/OFBIZ/OFBiz+Tutorial+-+From+Mini+Language+to+Groovy">here</a>
 */
String transactionMessage

// check if this is an update request, if so mark the updates
if(parameters.action && (parameters.action == "UPDATE")){
    awsSyncEnabled = parameters.awsSyncEnabled
    bucketName = parameters.bucketName
    accessKey = parameters.accessKey
    secretKey = parameters.secretKey
    region = parameters.region
    logInfo("Update request received...")
    List<GenericValue> valuesToUpdate = []
    if(awsSyncEnabled){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "appconfig", "systemPropertyId" : "app.storage.aws.s3.enabled"], false)
        if("syncEnabled" == awsSyncEnabled){
            existingValue.put("systemPropertyValue", "Y")
        }else{
            existingValue.put("systemPropertyValue", "N")
        }
        valuesToUpdate.add(existingValue)
        logInfo("awsSyncEnabled request received..." + awsSyncEnabled)
    }

    if(bucketName){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "appconfig", "systemPropertyId" : "app.storage.aws.s3.bucketName"], false)
        existingValue.put("systemPropertyValue", bucketName)
        valuesToUpdate.add(existingValue)
    }

    if(accessKey){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "appconfig", "systemPropertyId" : "app.storage.aws.s3.accessKey"], false)
        existingValue.put("systemPropertyValue", accessKey)
        valuesToUpdate.add(existingValue)
    }

    if(secretKey){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "appconfig", "systemPropertyId" : "app.storage.aws.s3.secretKey"], false)
        existingValue.put("systemPropertyValue", secretKey)
        valuesToUpdate.add(existingValue)
    }

    if(region){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "appconfig", "systemPropertyId" : "app.storage.aws.s3.config.region"], false)
        existingValue.put("systemPropertyValue", region)
        valuesToUpdate.add(existingValue)
    }

    if(UtilValidate.isNotEmpty(valuesToUpdate)){
        delegator.storeAll(valuesToUpdate)
    }

    // email settings update
    isEmailSendingEnabled = parameters.isEmailSendingEnabled
    redirectAllEmailsTo = parameters.redirectAllEmailsTo
    sendFromEmail = parameters.sendFromEmail
    emailServerHost = parameters.emailServerHost
    smtpUser = parameters.smtpUser
    smtpUserPassword = parameters.smtpUserPassword
    logInfo("Update request received...")
    if(isEmailSendingEnabled){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "general", "systemPropertyId" : "mail.notifications.enabled"], false)
        if("emailSendingEnabled" == isEmailSendingEnabled){
            existingValue.put("systemPropertyValue", "Y")
        }else{
            existingValue.put("systemPropertyValue", "N")
        }
        valuesToUpdate.add(existingValue)
        logInfo("isEmailSendingEnabled request received..." + isEmailSendingEnabled)
    }

    if(redirectAllEmailsTo){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "general", "systemPropertyId" : "mail.notifications.redirectTo"], false)
        existingValue.put("systemPropertyValue", redirectAllEmailsTo)
        valuesToUpdate.add(existingValue)
    }else{
        // set the value to null, this is needed to move the emails out of dev mode.
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "general", "systemPropertyId" : "mail.notifications.redirectTo"], false)
        existingValue.put("systemPropertyValue", "")
        valuesToUpdate.add(existingValue)
    }

    if(sendFromEmail){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "general", "systemPropertyId" : "defaultFromEmailAddress"], false)
        existingValue.put("systemPropertyValue", sendFromEmail)
        valuesToUpdate.add(existingValue)
    }

    if(emailServerHost){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "general", "systemPropertyId" : "mail.smtp.relay.host"], false)
        existingValue.put("systemPropertyValue", emailServerHost)
        valuesToUpdate.add(existingValue)
    }

    if(smtpUser){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "general", "systemPropertyId" : "mail.smtp.auth.user"], false)
        existingValue.put("systemPropertyValue", smtpUser)
        valuesToUpdate.add(existingValue)
    }
    if(smtpUserPassword){
        GenericValue existingValue = findOne("SystemProperty",["systemResourceId" : "general", "systemPropertyId" : "mail.smtp.auth.password"], false)
        existingValue.put("systemPropertyValue", smtpUserPassword)
        valuesToUpdate.add(existingValue)
    }

    if(UtilValidate.isNotEmpty(valuesToUpdate)){
        delegator.storeAll(valuesToUpdate)
    }

    transactionMessage = "Application settings have been updated successfully!"
}

// Fetch and update system settings here.
//Fetch regions
List<Region> awsAvailableRegions = Region.regions()

EntityCondition condition = EntityCondition.makeCondition([
        EntityCondition.makeCondition("systemResourceId", EntityOperator.EQUALS, "appconfig"),
        EntityCondition.makeCondition("systemPropertyId", EntityOperator.LIKE, "app.storage.aws%")
], EntityOperator.AND)
List<GeneratedValue> awsConfigItems = from("SystemProperty").where(condition).queryList()

Map awsConfiguration = [:]
if (UtilValidate.isNotEmpty(awsConfigItems)) {
    awsConfigItems.each { awsConfigItem ->
        String propertyId = awsConfigItem.getString("systemPropertyId")
        String propertyValue = awsConfigItem.getString("systemPropertyValue")

        switch (propertyId) {
            case "app.storage.aws.s3.enabled":
                if ("Y".equalsIgnoreCase(propertyValue)) {
                    awsConfiguration.put("isSyncEnabled", true)
                } else {
                    awsConfiguration.put("isSyncEnabled", false)
                }
                break
            case "app.storage.aws.s3.bucketName":
                awsConfiguration.put("bucketName", propertyValue)
                break
            case "app.storage.aws.s3.folder.uploads":
                awsConfiguration.put("uploadDirectoryName", propertyValue)
                break
            case "app.storage.aws.s3.config.region":
                awsConfiguration.put("region", propertyValue)
                break
            case "app.storage.aws.s3.accessKey":
                awsConfiguration.put("accessKey", propertyValue)
                break
            case "app.storage.aws.s3.secretKey":
                awsConfiguration.put("secretKey", propertyValue)
                break
        }
    }
}

context.awsConfiguration = awsConfiguration
context.awsAvailableRegions = awsAvailableRegions
context.transactionMessage = transactionMessage

// Email configuration management
EntityCondition emailSettingsCriteria = EntityCondition.makeCondition([
        EntityCondition.makeCondition("systemResourceId", EntityOperator.EQUALS, "general"),
        EntityCondition.makeCondition("systemPropertyId", EntityOperator.LIKE, "mail.%")
], EntityOperator.AND)
List<GeneratedValue> emailConfigItems = from("SystemProperty").where(emailSettingsCriteria).queryList()

// special handling for property not starting with mail.
GenericValue defaultFromEmailAddressConfigItem = findOne("SystemProperty",["systemResourceId" : "general", "systemPropertyId" : "defaultFromEmailAddress"], false)

if(UtilValidate.isNotEmpty(defaultFromEmailAddressConfigItem))
    emailConfigItems.add(defaultFromEmailAddressConfigItem)

Map emailConfiguration = [:]
if (UtilValidate.isNotEmpty(emailConfigItems)) {
    emailConfigItems.each { emailConfigItem ->
        String propertyId = emailConfigItem.getString("systemPropertyId")
        String propertyValue = emailConfigItem.getString("systemPropertyValue")

        switch (propertyId) {
            case "mail.notifications.enabled":
                if ("Y".equalsIgnoreCase(propertyValue)) {
                    emailConfiguration.put("isEmailSendingEnabled", true)
                } else {
                    emailConfiguration.put("isEmailSendingEnabled", false)
                }
                break
            case "mail.notifications.redirectTo":
                emailConfiguration.put("redirectAllEmailsTo", propertyValue)
                emailConfiguration.put("sendTestEmailTo", propertyValue)
                break
            case "defaultFromEmailAddress":
                emailConfiguration.put("sendFromEmail", propertyValue)
                break
            case "mail.smtp.relay.host":
                emailConfiguration.put("emailServerHost", propertyValue)
                break
            case "mail.smtp.auth.user":
                emailConfiguration.put("smtpUser", propertyValue)
                break
            case "mail.smtp.auth.password":
                emailConfiguration.put("smtpUserPassword", propertyValue)
                break
        }
    }
}

context.emailConfiguration = emailConfiguration

