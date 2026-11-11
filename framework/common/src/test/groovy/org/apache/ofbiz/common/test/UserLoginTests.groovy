/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.common.test

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.security.Security
import org.apache.ofbiz.security.login.MfaServices
import org.apache.ofbiz.service.LocalDispatcher
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.apache.ofbiz.webapp.control.LoginWorker
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import dev.samstevens.totp.code.DefaultCodeGenerator

@JunitJupiterTest
class UserLoginTests implements JupiterTestHelper {

    @Test
    void testCreateUserLogin() {
        String userLoginId = 'demo.person'

        Map serviceCtx = [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ]
        Map serviceResult = dispatcher.runSync('createUserLogin', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue createdUserLogin = from('UserLogin')
                .where('userLoginId', userLoginId)
                .queryOne()
        assert createdUserLogin
        assert createdUserLogin.enabled == 'Y'
    }

    /*
     * createUserLogin's duplicate-userLoginId check compares userLoginId ignoring case. A
     * userLoginId containing quotes and parentheses should be compared literally, just like any
     * other value, and match only an existing UserLogin with that exact id - so creating a new,
     * not-yet-registered userLoginId built from such characters should still succeed.
     */
    @Test
    void testCreateUserLoginIgnoreCaseDuplicateCheckComparesValueLiterally() {
        String userLoginId = "\\') OR (1=1) OR ('"

        Map serviceCtx = [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ]
        Map serviceResult = dispatcher.runSync('createUserLogin', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult): serviceResult.errorMessage

        GenericValue createdUserLogin = from('UserLogin')
                .where('userLoginId', userLoginId)
                .queryOne()
        assert createdUserLogin
    }

    @Test
    void testUserLoginMfaPendingDefersSuccessfulLoginCommit() {
        String userLoginId = "mfa.pending.${UUID.randomUUID()}"
        Map createResult = dispatcher.runSync('createUserLogin', [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ])
        assert ServiceUtil.isSuccess(createResult): createResult.errorMessage

        GenericValue userLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        userLogin.set('hasLoggedOut', 'Y')
        userLogin.set('successiveFailedLogins', 2L)
        userLogin.store()

        Map pendingResult = dispatcher.runSync('userLoginMfaPending', [
                username: userLoginId,
                password: 'ofbiz',
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(pendingResult): pendingResult.errorMessage
        assert pendingResult.userLogin.userLoginId == userLoginId
        assert !pendingResult.containsKey('userLoginSession')

        GenericValue pendingUserLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        assert pendingUserLogin.hasLoggedOut == 'Y'
        assert pendingUserLogin.successiveFailedLogins == 2L
        assert from('UserLoginHistory').where('userLoginId', userLoginId).queryCount() == 0

        Map normalResult = dispatcher.runSync('userLogin', [
                'login.username': userLoginId,
                'login.password': 'ofbiz',
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(normalResult): normalResult.errorMessage

        Map serviceAuthResult = dispatcher.runSync('userLogin', [
                'login.username': userLoginId,
                'login.password': 'ofbiz',
                isServiceAuth: true,
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(serviceAuthResult): serviceAuthResult.errorMessage

        GenericValue authenticatedUserLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()

        assert authenticatedUserLogin.hasLoggedOut == 'N'
        assert authenticatedUserLogin.successiveFailedLogins == 0L
    }

    @Test
    void testTotpChallengeFinalizesOnlyOnce() {
        String userLoginId = "mfa.flow.${UUID.randomUUID()}"
        Map createResult = dispatcher.runSync('createUserLogin', [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ])
        assert ServiceUtil.isSuccess(createResult): createResult.errorMessage
        GenericValue userLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()

        Map enrollment = dispatcher.runSync('createUserLoginTotpFactor', [
                userLogin: userLogin,
                label: 'Test authenticator',
                currentPassword: 'ofbiz',
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(enrollment): enrollment.errorMessage
        assert enrollment.provisioningUri.contains(enrollment.secret)

        String enrollmentCode = new DefaultCodeGenerator().generate(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
        Map confirmation = dispatcher.runSync('confirmUserLoginTotpFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                code: enrollmentCode
        ])
        assert ServiceUtil.isSuccess(confirmation): confirmation.errorMessage
        assert from('UserLoginAuthFactor').where('factorId', enrollment.factorId).queryOne().needsValidation == 'N'

        userLogin.set('hasLoggedOut', 'Y')
        userLogin.set('successiveFailedLogins', 2L)
        userLogin.store()
        Map primaryResult = dispatcher.runSync('userLoginMfaPending', [
                username: userLoginId,
                password: 'ofbiz',
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(primaryResult): primaryResult.errorMessage
        assert from('UserLogin').where('userLoginId', userLoginId).queryOne().hasLoggedOut == 'Y'
        assert from('UserLoginHistory').where('userLoginId', userLoginId).queryCount() == 0

        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setSession(new MockHttpSession())
        Map challenge = dispatcher.runSync('createUserLoginMfaChallenge', [userLogin: userLogin, request: request])
        assert ServiceUtil.isSuccess(challenge): challenge.errorMessage
        assert challenge.required
        assert request.getSession().getAttribute('userLogin') == null

        // Reusing the pending session markers in another browser session must not bind to this challenge.
        MockHttpServletRequest otherRequest = new MockHttpServletRequest()
        otherRequest.setSession(new MockHttpSession())
        otherRequest.getSession().setAttribute('_MFA_PENDING_CHALLENGE_', request.getSession().getAttribute('_MFA_PENDING_CHALLENGE_'))
        otherRequest.getSession().setAttribute('_MFA_PENDING_USERLOGIN_', userLoginId)
        // Enrollment confirmation consumes the current TOTP step; authenticate with the next
        // permitted step so this test exercises login rather than replay rejection.
        String totpCode = new DefaultCodeGenerator().generate(enrollment.secret,
                ((System.currentTimeMillis() / 30_000L) as long) + 1L)
        Map crossSession = dispatcher.runSync('verifyUserLoginMfaChallenge', [request: otherRequest, code: totpCode])
        assert ServiceUtil.isError(crossSession)

        Map verified = dispatcher.runSync('verifyUserLoginMfaChallenge', [request: request, code: totpCode])
        assert ServiceUtil.isSuccess(verified): verified.errorMessage
        assert from('UserLoginHistory').where('userLoginId', userLoginId).queryCount() == 0
        assert request.getSession().getAttribute('userLogin') == null

        Map completed = dispatcher.runSync('completeUserLoginMfa', [request: request])
        assert ServiceUtil.isSuccess(completed): completed.errorMessage
        assert request.getSession().getAttribute('userLogin') == null
        GenericValue completedLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        assert completedLogin.hasLoggedOut == 'N'
        assert completedLogin.successiveFailedLogins == 0L
        assert from('UserLoginHistory').where('userLoginId', userLoginId).queryCount() == 1

        // The exact accepted TOTP counter cannot be consumed again in a second challenge.
        Map replayChallenge = dispatcher.runSync('createUserLoginMfaChallenge', [userLogin: completed.userLogin, request: request])
        assert ServiceUtil.isSuccess(replayChallenge)
        Map replay = dispatcher.runSync('verifyUserLoginMfaChallenge', [request: request, code: totpCode])
        assert ServiceUtil.isSuccess(replay)
        assert replay.verified == false

        // An expired challenge cannot verify, even if the supplied code would otherwise be valid.
        Map expiredChallenge = dispatcher.runSync('createUserLoginMfaChallenge', [userLogin: completed.userLogin, request: request])
        assert ServiceUtil.isSuccess(expiredChallenge)
        GenericValue expiredRecord = from('UserLoginAuthChallenge').where('challengeId', expiredChallenge.challengeId).queryOne()
        expiredRecord.set('expiresDate', new java.sql.Timestamp(System.currentTimeMillis() - 1_000L))
        expiredRecord.store()
        Map expiredVerification = dispatcher.runSync('verifyUserLoginMfaChallenge', [request: request, code: totpCode])
        assert ServiceUtil.isError(expiredVerification)

        // Invalid codes are committed as verification outcomes so counters and lockout survive the response.
        Map limitedChallenge = dispatcher.runSync('createUserLoginMfaChallenge', [userLogin: completed.userLogin, request: request])
        assert ServiceUtil.isSuccess(limitedChallenge)
        for (int attempt = 1; attempt <= 5; attempt++) {
            Map invalidVerification = dispatcher.runSync('verifyUserLoginMfaChallenge', [request: request, code: 'wrong-code'])
            assert ServiceUtil.isSuccess(invalidVerification)
            assert invalidVerification.verified == false
            GenericValue limitedRecord = from('UserLoginAuthChallenge').where('challengeId', limitedChallenge.challengeId).queryOne()
            assert limitedRecord.attemptCount == attempt
        }
        GenericValue lockedChallenge = from('UserLoginAuthChallenge').where('challengeId', limitedChallenge.challengeId).queryOne()
        assert lockedChallenge.challengeStatus == 'LOCKED'
        Map overLimitVerification = dispatcher.runSync('verifyUserLoginMfaChallenge', [request: request, code: totpCode])
        assert ServiceUtil.isError(overLimitVerification)
    }

    @Test
    void testMfaFactorListExcludesSecretsAndSelfRevocationRequiresPassword() {
        String userLoginId = 'mfa.factor-management.' + UUID.randomUUID()
        Map createResult = dispatcher.runSync('createUserLogin', [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ])
        assert ServiceUtil.isSuccess(createResult): createResult.errorMessage
        GenericValue userLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()

        Map enrollment = dispatcher.runSync('createUserLoginTotpFactor', [
                userLogin: userLogin,
                label: 'Primary phone',
                currentPassword: 'ofbiz',
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(enrollment): enrollment.errorMessage
        String code = new DefaultCodeGenerator().generate(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
        Map confirmed = dispatcher.runSync('confirmUserLoginTotpFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                code: code
        ])
        assert ServiceUtil.isSuccess(confirmed): confirmed.errorMessage

        Map listed = dispatcher.runSync('getUserLoginAuthFactors', [userLogin: userLogin])
        assert ServiceUtil.isSuccess(listed): listed.errorMessage
        assert listed.factors.size() == 1
        Map safeFactor = listed.factors.first()
        assert safeFactor.factorId == enrollment.factorId
        assert !safeFactor.containsKey('secret')
        assert !safeFactor.containsKey('secretHash')

        String otherUserLoginId = 'mfa.factor-other-user.' + UUID.randomUUID()
        Map otherCreate = dispatcher.runSync('createUserLogin', [
                userLoginId: otherUserLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ])
        assert ServiceUtil.isSuccess(otherCreate): otherCreate.errorMessage
        GenericValue otherUserLogin = from('UserLogin').where('userLoginId', otherUserLoginId).queryOne()
        Map unauthorizedList = dispatcher.runSync('getUserLoginAuthFactors', [
                userLogin: otherUserLogin,
                targetUserLoginId: userLoginId
        ])
        assert ServiceUtil.isError(unauthorizedList)
        Map unauthorizedRevoke = dispatcher.runSync('revokeUserLoginAuthFactor', [
                userLogin: otherUserLogin,
                factorId: enrollment.factorId,
                reason: 'unauthorized cross-account attempt'
        ])
        assert ServiceUtil.isError(unauthorizedRevoke)
        assert from('UserLoginAuthFactor').where('factorId', enrollment.factorId).queryOne().thruDate == null

        Map rejectedRevoke = dispatcher.runSync('revokeUserLoginAuthFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                currentPassword: 'wrong-password',
                locale: Locale.US
        ])
        assert ServiceUtil.isError(rejectedRevoke)
        assert from('UserLoginAuthFactor').where('factorId', enrollment.factorId).queryOne().thruDate == null

        Map revoked = dispatcher.runSync('revokeUserLoginAuthFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                currentPassword: 'ofbiz',
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(revoked): revoked.errorMessage
        assert from('UserLoginAuthFactor').where('factorId', enrollment.factorId).queryOne().thruDate != null
    }

    @Test
    void testForcedTotpEnrollmentMustBeVerifiedBeforeLoginCompletion() {
        String userLoginId = "mfa.forced-enrollment.${UUID.randomUUID()}"
        String groupId = "MFA${UUID.randomUUID().toString().take(12)}"
        Map createResult = dispatcher.runSync('createUserLogin', [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ])
        assert ServiceUtil.isSuccess(createResult): createResult.errorMessage
        GenericValue userLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        getDelegator().makeValue('SecurityGroup', [
                groupId: groupId,
                groupName: 'MFA forced enrollment test',
                requireAuthFactor: 'Y'
        ]).create()
        getDelegator().makeValue('UserLoginSecurityGroup', [
                userLoginId: userLoginId,
                groupId: groupId,
                fromDate: new java.sql.Timestamp(System.currentTimeMillis())
        ]).create()

        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setSession(new MockHttpSession())
        Map challenge = dispatcher.runSync('createUserLoginMfaChallenge', [userLogin: userLogin, request: request])
        assert ServiceUtil.isSuccess(challenge): challenge.errorMessage
        assert challenge.required
        assert challenge.mustEnroll
        assert request.getSession().getAttribute('userLogin') == null

        Map enrollment = dispatcher.runSync('createUserLoginTotpFactor', [
                userLogin: userLogin,
                label: 'Forced enrollment test',
                currentPassword: 'ofbiz',
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(enrollment): enrollment.errorMessage
        request.getSession().setAttribute('_MFA_PENDING_ENROLLMENT_FACTOR_', enrollment.factorId)
        String enrollmentCode = new DefaultCodeGenerator().generate(enrollment.secret,
                (System.currentTimeMillis() / 30_000L) as long)
        Map confirmed = dispatcher.runSync('completeForcedTotpEnrollment', [request: request, code: enrollmentCode])
        assert ServiceUtil.isSuccess(confirmed): confirmed.errorMessage
        assert confirmed.activated
        assert from('UserLoginAuthFactor').where('factorId', enrollment.factorId).queryOne().needsValidation == 'N'
        assert request.getSession().getAttribute('userLogin') == null

        String loginCode = new DefaultCodeGenerator().generate(enrollment.secret,
                ((System.currentTimeMillis() / 30_000L) as long) + 1L)
        Map verified = dispatcher.runSync('verifyUserLoginMfaChallenge', [request: request, code: loginCode])
        assert ServiceUtil.isSuccess(verified): verified.errorMessage
        Map completed = dispatcher.runSync('completeUserLoginMfa', [request: request])
        assert ServiceUtil.isSuccess(completed): completed.errorMessage
        assert completed.userLogin.userLoginId == userLoginId
        assert from('UserLoginHistory').where('userLoginId', userLoginId).queryCount() == 1
    }

    @Test
    void testUserLoginMfaPendingRejectsServiceAuthentication() {
        Map result = dispatcher.runSync('userLoginMfaPending', [
                username: 'any-user',
                password: 'any-password',
                isServiceAuth: true
        ])
        assert ServiceUtil.isError(result)
    }

    @Test
    void testMfaPasswordChangeRequiresRecentFactorProofBeforeCallingUpdateService() {
        String userLoginId = 'mfa.password-gate.' + UUID.randomUUID()
        Map createResult = dispatcher.runSync('createUserLogin', [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ])
        assert ServiceUtil.isSuccess(createResult): createResult.errorMessage
        GenericValue userLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()

        AtomicInteger updateCalls = new AtomicInteger()
        LocalDispatcher mockedDispatcher = Proxy.newProxyInstance(LocalDispatcher.classLoader,
                [LocalDispatcher] as Class[], { Object proxy, java.lang.reflect.Method method, Object[] args ->
                    if (method.name == 'runSync') {
                        if (args[0] == 'isUserLoginMfaRequired') {
                            return [required: true]
                        }
                        if (args[0] == 'updatePassword') {
                            updateCalls.incrementAndGet()
                            return ServiceUtil.returnSuccess()
                        }
                    }
                    throw new UnsupportedOperationException(method.name)
                } as InvocationHandler) as LocalDispatcher
        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpSession session = new MockHttpSession()
        session.setAttribute('userLogin', userLogin)
        request.setSession(session)
        request.setAttribute('dispatcher', mockedDispatcher)
        request.setParameter('currentPassword', 'ofbiz')
        request.setParameter('newPassword', 'Mfa-Password-Change-1!')
        request.setParameter('newPasswordVerify', 'Mfa-Password-Change-1!')

        assert LoginWorker.updatePasswordWithMfa(request, new MockHttpServletResponse()) == 'error'
        assert updateCalls.get() == 0

        session.setAttribute(MfaServices.MFA_VERIFIED_AT, System.currentTimeMillis())
        assert LoginWorker.updatePasswordWithMfa(request, new MockHttpServletResponse()) == 'success'
        assert updateCalls.get() == 1
    }

    @Test
    void testMfaAdministrativePasswordRecoveryRequiresPermissionAndReasonAndAuditsSuccess() {
        String actorId = 'mfa.admin-recovery.actor.' + UUID.randomUUID()
        String targetId = 'mfa.admin-recovery.target.' + UUID.randomUUID()
        for (String userLoginId : [actorId, targetId]) {
            Map createResult = dispatcher.runSync('createUserLogin', [
                    userLoginId: userLoginId,
                    enabled: 'Y',
                    currentPassword: 'ofbiz',
                    currentPasswordVerify: 'ofbiz'
            ])
            assert ServiceUtil.isSuccess(createResult): createResult.errorMessage
        }
        GenericValue actor = from('UserLogin').where('userLoginId', actorId).queryOne()
        AtomicInteger updateCalls = new AtomicInteger()
        LocalDispatcher mockedDispatcher = Proxy.newProxyInstance(LocalDispatcher.classLoader,
                [LocalDispatcher] as Class[], { Object proxy, java.lang.reflect.Method method, Object[] args ->
                    if (method.name == 'runSync') {
                        if (args[0] == 'isUserLoginMfaRequired') {
                            return [required: true]
                        }
                        if (args[0] == 'updatePassword') {
                            updateCalls.incrementAndGet()
                            return ServiceUtil.returnSuccess()
                        }
                    }
                    throw new UnsupportedOperationException(method.name)
                } as InvocationHandler) as LocalDispatcher
        AtomicBoolean hasPermission = new AtomicBoolean(false)
        Security mockedSecurity = Proxy.newProxyInstance(Security.classLoader,
                [Security] as Class[], { Object proxy, java.lang.reflect.Method method, Object[] args ->
                    if (method.name == 'hasEntityPermission') {
                        return hasPermission.get()
                    }
                    return false
                } as InvocationHandler) as Security

        MockHttpSession session = new MockHttpSession()
        session.setAttribute('userLogin', actor)
        session.setAttribute(MfaServices.MFA_VERIFIED_AT, System.currentTimeMillis())
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setSession(session)
        request.setAttribute('dispatcher', mockedDispatcher)
        request.setAttribute('security', mockedSecurity)
        request.setAttribute('delegator', getDelegator())
        request.setParameter('userLoginId', targetId)
        request.setParameter('newPassword', 'Mfa-Admin-Reset-1!')
        request.setParameter('newPasswordVerify', 'Mfa-Admin-Reset-1!')
        request.setParameter('mfaAdminReason', 'Account recovery after verified support request')

        assert LoginWorker.updatePasswordWithMfa(request, new MockHttpServletResponse()) == 'error'
        assert updateCalls.get() == 0
        assert from('UserAuthenticationEvent').where([userLoginId: targetId, eventType: 'MFA_ADMIN_PASSWORD_RESET']).queryCount() == 0

        hasPermission.set(true)
        request.setParameter('mfaAdminReason', '')
        assert LoginWorker.updatePasswordWithMfa(request, new MockHttpServletResponse()) == 'error'
        assert updateCalls.get() == 0

        request.setParameter('mfaAdminReason', 'Account recovery after verified support request')
        assert LoginWorker.updatePasswordWithMfa(request, new MockHttpServletResponse()) == 'success'
        assert updateCalls.get() == 1
        GenericValue auditEvent = from('UserAuthenticationEvent').queryList()
                .find { it.userLoginId == targetId && it.eventType == 'MFA_ADMIN_PASSWORD_RESET' }
        assert auditEvent
        assert auditEvent.actorUserLoginId == actorId
        assert auditEvent.eventReason == 'Account recovery after verified support request'
    }

}
