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

import org.apache.ofbiz.base.crypto.HashCrypt
import org.apache.ofbiz.common.login.LoginServices
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.security.login.MfaServices
import org.apache.ofbiz.security.login.TotpUtil
import org.apache.ofbiz.service.LocalDispatcher
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.apache.ofbiz.webapp.control.LoginWorker
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession

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

    /*
     * Verifies that a service accepting login.username/login.password (e.g. SetTimeZoneFromBrowser),
     * which runs through ServiceDispatcher.checkAuth()'s plain userLogin path rather than
     * userLoginMfaPending, is denied for an MFA-mandated account and leaves successiveFailedLogins
     * unchanged instead of resetting it.
     */
    @Test
    void testServiceLevelLoginDeniedForMfaMandatedAccountAndDoesNotResetFailedLogins() {
        String userLoginId = "mfa.service.auth.${UUID.randomUUID()}"
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
        String enrollmentCode = TotpUtil.generateTotp(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
        Map confirmation = dispatcher.runSync('confirmUserLoginTotpFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                code: enrollmentCode
        ])
        assert ServiceUtil.isSuccess(confirmation): confirmation.errorMessage

        // Simulate an account that already has failed attempts on record, as a wrong MFA code during a
        // pending challenge would leave it (handleFailedMfaAttempt).
        userLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        userLogin.set('successiveFailedLogins', 2L)
        userLogin.store()

        Map serviceAuthResult = dispatcher.runSync('userLogin', [
                'login.username': userLoginId,
                'login.password': 'ofbiz',
                isServiceAuth: true,
                locale: Locale.US
        ])
        assert !ServiceUtil.isSuccess(serviceAuthResult)

        GenericValue afterServiceAuth = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        // The correct password must not be rewarded with a reset of the failed-login count, and it must
        // not be punished with an increment either -- it is simply not a completed login.
        assert afterServiceAuth.successiveFailedLogins == 2L
        assert from('UserLoginHistory').where('userLoginId', userLoginId).queryCount() == 0

        // An audit trail is still left for an administrator reviewing this account, even though no
        // challenge could be created for a non-interactive caller.
        assert from('UserAuthenticationEvent')
                .where('userLoginId', userLoginId, 'eventType', 'MFA_REQUIRED')
                .queryCount() == 1

        // A plain account with no factor and no MFA-mandated group membership is unaffected.
        String plainUserLoginId = "mfa.service.auth.plain.${UUID.randomUUID()}"
        dispatcher.runSync('createUserLogin', [
                userLoginId: plainUserLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ])
        Map plainServiceAuthResult = dispatcher.runSync('userLogin', [
                'login.username': plainUserLoginId,
                'login.password': 'ofbiz',
                isServiceAuth: true,
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(plainServiceAuthResult): plainServiceAuthResult.errorMessage
    }

    @Test
    void testForcedPasswordChangeDuringMfaLoginChallengeIsActuallyApplied() {
        String userLoginId = "mfa.pwdchange.${UUID.randomUUID()}"
        Map createResult = dispatcher.runSync('createUserLogin', [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz',
                requirePasswordChange: 'Y'
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
        String enrollmentCode = TotpUtil.generateTotp(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
        Map confirmation = dispatcher.runSync('confirmUserLoginTotpFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                code: enrollmentCode
        ])
        assert ServiceUtil.isSuccess(confirmation): confirmation.errorMessage

        // Part 1: the requirePasswordChange form reposts USERNAME/PASSWORD/newPassword/newPasswordVerify
        // to the login event (the only endpoint common to every webapp), which, since a factor is
        // active, must still issue a fresh MFA challenge. login() must stash those fields for replay
        // instead of silently dropping them -- doMainLogin needs a full webapp request context this
        // test cannot provide, so the challenge-required branch (reached before doMainLogin is ever
        // called) is exercised directly, the same way the production code path reaches it.
        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpSession session = new MockHttpSession()
        request.setSession(session)
        request.setAttribute('delegator', delegator)
        request.setAttribute('dispatcher', dispatcher)
        request.setParameter('USERNAME', userLoginId)
        request.setParameter('PASSWORD', 'ofbiz')
        request.setParameter('newPassword', 'brandNewPassword1')
        request.setParameter('newPasswordVerify', 'brandNewPassword1')
        MockHttpServletResponse response = new MockHttpServletResponse()

        String challengeView = LoginWorker.login(request, response)
        assert challengeView == 'authFactorRequired'

        MfaServices.PendingMfaReplay replay = MfaServices.takePendingMfaReplay(session)
        assert replay != null
        assert replay.kind == 'updatePasswordDuringLogin'
        assert replay.params.USERNAME == userLoginId
        assert replay.params.PASSWORD == 'ofbiz'
        assert replay.params.newPassword == 'brandNewPassword1'
        assert replay.params.newPasswordVerify == 'brandNewPassword1'

        // Part 2: once the challenge above is verified, completeVerifiedMfaLogin hands this same stashed
        // replay to the private applyRequestedPasswordChange helper -- the same one login()'s own
        // synchronous success path calls -- which must actually invoke updatePassword and clear
        // requirePasswordChange, rather than leaving the form stuck in a resubmission loop.
        java.lang.reflect.Method applyRequestedPasswordChange = LoginWorker.getDeclaredMethod(
                'applyRequestedPasswordChange', HttpServletRequest, HttpSession, LocalDispatcher, GenericValue,
                boolean, String, String, String, String, String)
        applyRequestedPasswordChange.setAccessible(true)
        String applyError = applyRequestedPasswordChange.invoke(null, request, session, dispatcher, userLogin, false,
                replay.params.USERNAME, replay.params.PASSWORD, replay.params.TOKEN,
                replay.params.newPassword, replay.params.newPasswordVerify)

        assert applyError == null: applyError
        GenericValue updatedUserLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        assert updatedUserLogin.requirePasswordChange == 'N'
        assert HashCrypt.comparePassword(updatedUserLogin.currentPassword, LoginServices.getHashType(), 'brandNewPassword1')
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

        String enrollmentCode = TotpUtil.generateTotp(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
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
        String totpCode = TotpUtil.generateTotp(enrollment.secret,
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
        // Failed attempts also count towards the account-level lockout (max.failed.logins, default 3),
        // which fires before the per-challenge limit (maxVerifyAttempts, default 5) ever would. The replay
        // attempt above already contributed one failure, so compute how many more are needed from the
        // account's actual current count rather than assuming it starts at zero.
        Map limitedChallenge = dispatcher.runSync('createUserLoginMfaChallenge', [userLogin: completed.userLogin, request: request])
        assert ServiceUtil.isSuccess(limitedChallenge)
        int maxFailedLogins = 3
        long failedLoginsBeforeLockoutTest = from('UserLogin').where('userLoginId', userLoginId).queryOne().successiveFailedLogins ?: 0L
        int attemptsUntilLockout = (maxFailedLogins - failedLoginsBeforeLockoutTest) as int
        for (int attempt = 1; attempt <= attemptsUntilLockout; attempt++) {
            Map invalidVerification = dispatcher.runSync('verifyUserLoginMfaChallenge', [request: request, code: 'wrong-code'])
            assert ServiceUtil.isSuccess(invalidVerification)
            assert invalidVerification.verified == false
            GenericValue limitedRecord = from('UserLoginAuthChallenge').where('challengeId', limitedChallenge.challengeId).queryOne()
            assert limitedRecord.attemptCount == attempt
            if (attempt == attemptsUntilLockout) {
                assert invalidVerification.accountLocked == true
            }
        }
        GenericValue lockedUserLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        assert lockedUserLogin.enabled == 'N'
        assert lockedUserLogin.successiveFailedLogins == maxFailedLogins as Long
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
        String code = TotpUtil.generateTotp(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
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
        String enrollmentCode = TotpUtil.generateTotp(enrollment.secret,
                (System.currentTimeMillis() / 30_000L) as long)
        Map confirmed = dispatcher.runSync('completeForcedTotpEnrollment', [request: request, code: enrollmentCode])
        assert ServiceUtil.isSuccess(confirmed): confirmed.errorMessage
        assert confirmed.activated
        assert from('UserLoginAuthFactor').where('factorId', enrollment.factorId).queryOne().needsValidation == 'N'
        assert request.getSession().getAttribute('userLogin') == null

        // The code just verified above already satisfies the pending login challenge (completeForcedTotpEnrollment
        // marks it VERIFIED), so completing the login needs no separate code re-entry here -- a second
        // verifyUserLoginMfaChallenge call would only find the challenge already past PENDING.
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

        LocalDispatcher mockedDispatcher = Proxy.newProxyInstance(LocalDispatcher.classLoader,
                [LocalDispatcher] as Class[], { Object proxy, java.lang.reflect.Method method, Object[] args ->
                    if (method.name == 'runSync' && args[0] == 'isUserLoginMfaRequired') {
                        return [required: true]
                    }
                    throw new UnsupportedOperationException(method.name)
                } as InvocationHandler) as LocalDispatcher
        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpSession session = new MockHttpSession()
        session.setAttribute('userLogin', userLogin)
        request.setSession(session)

        // Same gate the updatePassword simple-method now calls via call-class-method (LoginSimpleEvents.xml).
        assert !MfaServices.hasRecentMfaProof(request, userLogin, mockedDispatcher)

        session.setAttribute(MfaServices.MFA_VERIFIED_AT, System.currentTimeMillis())
        assert MfaServices.hasRecentMfaProof(request, userLogin, mockedDispatcher)
    }

    /*
     * Verifies that a wrong code bundled with beginTotpEnrollment, confirmTotpEnrollment,
     * revokeMfaFactor or generateBackupCodes to satisfy an expired re-authentication window counts
     * towards the account lockout, the same way a wrong code at the dedicated reverifyAuthFactor
     * endpoint does -- hasRecentMfaProof's own inline code-check is the single code path all four
     * of those callers share.
     */
    @Test
    void testHasRecentMfaProofCountsWrongCodeTowardsLockout() {
        String userLoginId = 'mfa.reauth-code.' + UUID.randomUUID()
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
        String enrollmentCode = TotpUtil.generateTotp(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
        Map confirmation = dispatcher.runSync('confirmUserLoginTotpFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                code: enrollmentCode
        ])
        assert ServiceUtil.isSuccess(confirmation): confirmation.errorMessage

        LocalDispatcher mockedDispatcher = Proxy.newProxyInstance(LocalDispatcher.classLoader,
                [LocalDispatcher] as Class[], { Object proxy, java.lang.reflect.Method method, Object[] args ->
                    if (method.name == 'runSync' && args[0] == 'isUserLoginMfaRequired') {
                        return [required: true]
                    }
                    throw new UnsupportedOperationException(method.name)
                } as InvocationHandler) as LocalDispatcher
        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpSession session = new MockHttpSession()
        session.setAttribute('userLogin', userLogin)
        request.setSession(session)
        request.setAttribute('delegator', delegator)
        // No MFA_VERIFIED_AT in session: the re-authentication window has not been satisfied, so
        // hasRecentMfaProof must fall through to its own inline code-check below.
        request.setParameter('mfaCode', 'wrong-code')

        assert !MfaServices.hasRecentMfaProof(request, userLogin, mockedDispatcher)

        GenericValue afterWrongCode = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        assert afterWrongCode.successiveFailedLogins == 1L
        assert from('UserAuthenticationEvent')
                .where('userLoginId', userLoginId, 'eventType', 'MFA_REVERIFY_FAILURE')
                .queryCount() == 1
    }

    @Test
    void testHasRecentMfaProofClearsFailedLoginsOnSuccessfulStepUp() {
        String userLoginId = 'mfa.reauth-clear.' + UUID.randomUUID()
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
        String enrollmentCode = TotpUtil.generateTotp(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
        Map confirmation = dispatcher.runSync('confirmUserLoginTotpFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                code: enrollmentCode
        ])
        assert ServiceUtil.isSuccess(confirmation): confirmation.errorMessage

        // An earlier, unrelated mistake already left two strikes on this account.
        userLogin.set('successiveFailedLogins', 2L)
        userLogin.store()

        LocalDispatcher mockedDispatcher = Proxy.newProxyInstance(LocalDispatcher.classLoader,
                [LocalDispatcher] as Class[], { Object proxy, java.lang.reflect.Method method, Object[] args ->
                    if (method.name == 'runSync' && args[0] == 'isUserLoginMfaRequired') {
                        return [required: true]
                    }
                    throw new UnsupportedOperationException(method.name)
                } as InvocationHandler) as LocalDispatcher
        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpSession session = new MockHttpSession()
        session.setAttribute('userLogin', userLogin)
        request.setSession(session)
        request.setAttribute('delegator', delegator)
        String totpCode = TotpUtil.generateTotp(enrollment.secret,
                ((System.currentTimeMillis() / 30_000L) as long) + 1L)
        request.setParameter('mfaCode', totpCode)

        assert MfaServices.hasRecentMfaProof(request, userLogin, mockedDispatcher)

        // A valid step-up code is itself proof of possession, just like a correct primary password --
        // it must clear strikes left over from earlier wrong attempts, not leave them standing until
        // the next full login.
        GenericValue afterSuccess = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        assert afterSuccess.successiveFailedLogins == 0L
    }

    @Test
    void testReverifyAuthFactorClearsFailedLoginsOnSuccessfulStepUp() {
        String userLoginId = 'mfa.reverify-clear.' + UUID.randomUUID()
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
        String enrollmentCode = TotpUtil.generateTotp(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
        Map confirmation = dispatcher.runSync('confirmUserLoginTotpFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                code: enrollmentCode
        ])
        assert ServiceUtil.isSuccess(confirmation): confirmation.errorMessage

        // Two earlier, unrelated strikes, exactly like the previous test but exercised through the
        // dedicated reverifyAuthFactor endpoint instead of the bundled-code shortcut in
        // hasRecentMfaProof -- these are two independent code paths that must behave the same way.
        userLogin.set('successiveFailedLogins', 2L)
        userLogin.store()

        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpSession session = new MockHttpSession()
        session.setAttribute('userLogin', userLogin)
        request.setSession(session)
        request.setAttribute('delegator', delegator)
        String totpCode = TotpUtil.generateTotp(enrollment.secret,
                ((System.currentTimeMillis() / 30_000L) as long) + 1L)
        request.setParameter('code', totpCode)
        MockHttpServletResponse response = new MockHttpServletResponse()

        String view = LoginWorker.reverifyAuthFactor(request, response)

        assert view == 'success'
        GenericValue afterSuccess = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        assert afterSuccess.successiveFailedLogins == 0L
    }

    @Test
    void testConfirmTotpEnrollmentCodeIsNeverMisreadAsAWrongStepUpCode() {
        String userLoginId = 'mfa.confirm-second.' + UUID.randomUUID()
        Map createResult = dispatcher.runSync('createUserLogin', [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ])
        assert ServiceUtil.isSuccess(createResult): createResult.errorMessage
        GenericValue userLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()

        // An already-confirmed first factor, so the account has an active factor and the step-up gate
        // applies to everything below, including enrolling a second one.
        Map firstFactor = dispatcher.runSync('createUserLoginTotpFactor', [
                userLogin: userLogin,
                label: 'Primary phone',
                currentPassword: 'ofbiz',
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(firstFactor): firstFactor.errorMessage
        String firstFactorCode = TotpUtil.generateTotp(firstFactor.secret, (System.currentTimeMillis() / 30_000L) as long)
        Map firstConfirmation = dispatcher.runSync('confirmUserLoginTotpFactor', [
                userLogin: userLogin,
                factorId: firstFactor.factorId,
                code: firstFactorCode
        ])
        assert ServiceUtil.isSuccess(firstConfirmation): firstConfirmation.errorMessage

        // Begin enrolling a second factor. Its own confirmation code is unrelated to the first factor's
        // TOTP secret, so it could never pass as a step-up code for that first factor.
        Map secondFactor = dispatcher.runSync('createUserLoginTotpFactor', [
                userLogin: userLogin,
                label: 'Backup phone',
                currentPassword: 'ofbiz',
                locale: Locale.US
        ])
        assert ServiceUtil.isSuccess(secondFactor): secondFactor.errorMessage
        String secondFactorCode = TotpUtil.generateTotp(secondFactor.secret, (System.currentTimeMillis() / 30_000L) as long)

        LocalDispatcher mockedDispatcher = Proxy.newProxyInstance(LocalDispatcher.classLoader,
                [LocalDispatcher] as Class[], { Object proxy, java.lang.reflect.Method method, Object[] args ->
                    if (method.name == 'runSync' && args[0] == 'isUserLoginMfaRequired') {
                        return [required: true]
                    }
                    return method.invoke(dispatcher, args)
                } as InvocationHandler) as LocalDispatcher

        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpSession session = new MockHttpSession()
        session.setAttribute('userLogin', userLogin)
        request.setSession(session)
        request.setAttribute('delegator', delegator)
        request.setAttribute('dispatcher', mockedDispatcher)
        // No MFA_VERIFIED_AT in session: the re-authentication window has expired, same as the reported
        // scenario (a slow user taking longer than the window to scan the QR code and type a code).
        request.setParameter('factorId', secondFactor.factorId as String)
        request.setParameter('code', secondFactorCode)
        MockHttpServletResponse response = new MockHttpServletResponse()

        String view = LoginWorker.confirmTotpEnrollment(request, response)

        assert view == 'authFactorRequired'
        GenericValue afterAttempt = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        assert (afterAttempt.successiveFailedLogins ?: 0L) == 0L
        assert from('UserAuthenticationEvent')
                .where('userLoginId', userLoginId, 'eventType', 'MFA_REVERIFY_FAILURE')
                .queryCount() == 0
        assert from('UserLoginAuthFactor').where('factorId', secondFactor.factorId).queryOne().needsValidation == 'Y'
    }

    /*
     * Verifies that revokeMfaFactor requires recent MFA proof even after the re-authentication
     * window expires, the same way beginTotpEnrollment and generateBackupCodes require it
     * unconditionally -- a standing password alone is not step-up proof.
     */
    @Test
    void testRevokeMfaFactorRequiresRecentProofEvenWithCorrectPassword() {
        String userLoginId = 'mfa.revoke-reauth.' + UUID.randomUUID()
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
        String enrollmentCode = TotpUtil.generateTotp(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
        Map confirmation = dispatcher.runSync('confirmUserLoginTotpFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                code: enrollmentCode
        ])
        assert ServiceUtil.isSuccess(confirmation): confirmation.errorMessage

        // Delegates every other service call through to the real dispatcher, so if the bug were still
        // present this would actually revoke the factor for real, instead of just failing to invoke
        // an unstubbed method.
        LocalDispatcher mockedDispatcher = Proxy.newProxyInstance(LocalDispatcher.classLoader,
                [LocalDispatcher] as Class[], { Object proxy, java.lang.reflect.Method method, Object[] args ->
                    if (method.name == 'runSync' && args[0] == 'isUserLoginMfaRequired') {
                        return [required: true]
                    }
                    return method.invoke(dispatcher, args)
                } as InvocationHandler) as LocalDispatcher

        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpServletResponse response = new MockHttpServletResponse()
        MockHttpSession session = new MockHttpSession()
        session.setAttribute('userLogin', userLogin)
        request.setSession(session)
        request.setAttribute('delegator', delegator)
        request.setAttribute('dispatcher', mockedDispatcher)
        // No MFA_VERIFIED_AT in session and no fresh code supplied: the re-authentication window has
        // expired with no step-up proof at all -- only the (correct) password is offered.
        request.setParameter('currentPassword', 'ofbiz')
        request.setParameter('factorId', enrollment.factorId as String)

        String view = LoginWorker.revokeMfaFactor(request, response)

        assert view == 'authFactorRequired'
        assert from('UserLoginAuthFactor').where('factorId', enrollment.factorId).queryOne().thruDate == null
    }

    @Test
    void testRevokeMfaFactorRequiresRecentProofForOwnFactorEvenWithReasonSupplied() {
        String userLoginId = 'mfa.revoke-reauth-reason.' + UUID.randomUUID()
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
        String enrollmentCode = TotpUtil.generateTotp(enrollment.secret, (System.currentTimeMillis() / 30_000L) as long)
        Map confirmation = dispatcher.runSync('confirmUserLoginTotpFactor', [
                userLogin: userLogin,
                factorId: enrollment.factorId,
                code: enrollmentCode
        ])
        assert ServiceUtil.isSuccess(confirmation): confirmation.errorMessage

        LocalDispatcher mockedDispatcher = Proxy.newProxyInstance(LocalDispatcher.classLoader,
                [LocalDispatcher] as Class[], { Object proxy, java.lang.reflect.Method method, Object[] args ->
                    if (method.name == 'runSync' && args[0] == 'isUserLoginMfaRequired') {
                        return [required: true]
                    }
                    return method.invoke(dispatcher, args)
                } as InvocationHandler) as LocalDispatcher

        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpServletResponse response = new MockHttpServletResponse()
        MockHttpSession session = new MockHttpSession()
        session.setAttribute('userLogin', userLogin)
        request.setSession(session)
        request.setAttribute('delegator', delegator)
        request.setAttribute('dispatcher', mockedDispatcher)
        // Same expired-window, password-only scenario as above, but this time a reason is also supplied,
        // the way an administrator revoking someone else's factor would. A caller revoking their OWN
        // factor must still be blocked -- a reason is not a substitute for recent MFA proof just because
        // the admin path happens to require one instead.
        request.setParameter('currentPassword', 'ofbiz')
        request.setParameter('factorId', enrollment.factorId as String)
        request.setParameter('reason', 'self-service cleanup')

        String view = LoginWorker.revokeMfaFactor(request, response)

        assert view == 'authFactorRequired'
        assert from('UserLoginAuthFactor').where('factorId', enrollment.factorId).queryOne().thruDate == null
    }

    @Test
    void testMfaBackupRecoveryCodes() {
        String userLoginId = 'backup-code-test-user'
        delegator.create('UserLogin', [
                userLoginId: userLoginId,
                currentPassword: HashCrypt.cryptUTF8(LoginServices.getHashType(), null, 'ofbiz'),
                enabled: 'Y',
                isSystem: 'N'
        ])
        GenericValue userLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()

        // 1. Generate backup codes via service
        Map genResult = dispatcher.runSync('generateUserLoginBackupCodes', [
                userLogin: userLogin,
                currentPassword: 'ofbiz'
        ])
        assert ServiceUtil.isSuccess(genResult): genResult.errorMessage
        List<String> codes = (List<String>) genResult.backupCodes
        assert codes != null
        assert codes.size() == 10

        // Check remaining count
        Map countResult = dispatcher.runSync('getRemainingBackupCodesCount', [userLogin: userLogin])
        assert ServiceUtil.isSuccess(countResult)
        assert countResult.remainingCount == 10

        // Database check: verify hashes are stored, not plain codes
        List stored = from('UserLoginAuthFactor')
                .where('userLoginId', userLoginId, 'factorType', MfaServices.BACKUP_CODE)
                .queryList()
        assert stored.size() == 10
        stored.each { factor ->
            assert factor.thruDate == null
            assert factor.secretHash != null
            assert !codes.contains(factor.secretHash)
        }
        // 2. Authenticate challenge using a backup code (with hyphens & uppercase)
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setSession(new MockHttpSession())
        Map challenge = dispatcher.runSync('createUserLoginMfaChallenge', [userLogin: userLogin, request: request])
        assert ServiceUtil.isSuccess(challenge)
        assert challenge.required

        String firstCode = codes[0]
        String codeToSubmit = firstCode.toUpperCase()
        Map verifyResult = dispatcher.runSync('verifyUserLoginMfaChallenge', [request: request, code: codeToSubmit])
        assert ServiceUtil.isSuccess(verifyResult): verifyResult.errorMessage
        assert verifyResult.verified == true

        // Complete login
        Map completed = dispatcher.runSync('completeUserLoginMfa', [request: request])
        assert ServiceUtil.isSuccess(completed): completed.errorMessage

        // Verify that the used code is now consumed (single-use)
        Map countAfterUse = dispatcher.runSync('getRemainingBackupCodesCount', [userLogin: userLogin])
        assert countAfterUse.remainingCount == 9

        // Verify audit event logged
        assert from('UserAuthenticationEvent').where([
                userLoginId: userLoginId,
                eventType: 'MFA_BACKUP_CODE_USED',
                factorType: MfaServices.BACKUP_CODE
        ]).queryCount() == 1

        // 3. Replay attack: submitting the SAME backup code again must FAIL
        MockHttpServletRequest replayRequest = new MockHttpServletRequest()
        replayRequest.setSession(new MockHttpSession())
        Map replayChallenge = dispatcher.runSync('createUserLoginMfaChallenge', [userLogin: userLogin, request: replayRequest])
        assert ServiceUtil.isSuccess(replayChallenge)

        Map replayVerify = dispatcher.runSync('verifyUserLoginMfaChallenge', [request: replayRequest, code: firstCode])
        assert ServiceUtil.isSuccess(replayVerify)
        assert replayVerify.verified == false
    }

}
