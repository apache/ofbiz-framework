package org.apache.ofbiz.service

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.config.ServiceConfigUtil
import org.apache.ofbiz.testtools.GroovyScriptTestCase

class ServicePurgeTest extends GroovyScriptTestCase {

// ./gradlew 'ofbiz -t component=service -t suitename=servicetests -t case=service-purge-test' --debug-jvm

    void testRuntimeDataIsCleanedAfterServicePurge() {
        GenericValue sysUserLogin = delegator.findOne('UserLogin', true, 'userLoginId', 'system')
        String jobId = delegator.getNextSeqId('JobSandbox')

        def createRuntimeResult = dispatcher.runSync('createRuntimeData', [
                runtimeInfo: 'This is a runtimeInfo',
                userLogin  : sysUserLogin
        ])
        String runtimeDataId = createRuntimeResult.runtimeDataId

        dispatcher.runSync('createJobSandbox', [
                userLogin     : sysUserLogin,
                poolId        : ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool(),
                jobId         : jobId,
                runtimeDataId : runtimeDataId,
                statusId      : 'SERVICE_FINISHED',
                serviceName   : 'sendMail',
                finishDateTime: UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), -10)
        ])

        dispatcher.runSync('purgeOldJobs', [userLogin: sysUserLogin])

        assert EntityQuery.use(delegator).from('JobSandbox').where('jobId', jobId).queryCount() == 0
        assert EntityQuery.use(delegator).from('RuntimeData').where('runtimeDataId', runtimeDataId).queryCount() == 0
    }
}
