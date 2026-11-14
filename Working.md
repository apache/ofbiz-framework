# MFA Implementation: Simple File-by-File Guide

This guide explains how the native MFA implementation in Apache OFBiz® fits together.

## What is available now

- RFC 6238 TOTP authenticator enrollment, staged login challenges, factor listing and self-service revocation, step-up password-change protections, administrator recovery with mandatory auditing, and two-step REST API token handshakes are fully implemented.
- The global switch is disabled by default: `security.login.authFactor.enable=false` in `framework/security/config/security.properties`, ensuring 100% backward compatibility.
- The common login flow has full integration-test and browser-level verification across backoffice applications.
- The B2C eCommerce storefront (`plugins/ecommerce`) includes native customer-facing MFA challenge, enrollment, and self-service authenticator management templates matching the storefront Bootstrap 4 theme.
- REST API token minting (`/rest/auth/token`) supports an interactive two-step MFA challenge and verification handshake issuing RFC 8176 `amr` and `acr` assurance claims in JWT bearer tokens.

---

## 1. Data and service registration: what Apache OFBiz stores and can call

1. **`framework/security/entitydef/entitymodel.xml`** defines the entities used by MFA:
   - `UserLoginAuthFactor` stores configured authentication factors (e.g., `ULAF_TOTP`), encrypted secret, reserved cryptographic hash, validation status, and last-used step counter.
   - `UserLoginAuthChallenge` stores ephemeral server-side login challenges, expiry timestamp, attempt count, lockout threshold, and HMAC session binding.
   - `UserAuthenticationEvent` provides append-only security auditing for all lifecycle transactions (`MFA_ENROLL`, `MFA_FACTOR_REVOKED`, `MFA_ADMIN_PASSWORD_RESET`, `LOGIN_COMMITTED`, etc.).
   - `SecurityGroup.requireAuthFactor` adds an indicator (`Y`/`N`) to enforce mandatory MFA across active members of a security group.

2. **`framework/security/servicedef/services.xml`** registers the internal MFA operations: starting enrollment, confirming TOTP factors, listing and revoking factors, creating or verifying challenges, and step-up authentication.

3. **`framework/security/ofbiz-component.xml`** loads the security service definitions into the service engine.

4. **`framework/security/src/main/java/org/apache/ofbiz/security/login/MfaServices.java`** contains core factor and challenge logic:
   - Evaluates the global switch and active group-level policies via `isMfaRequired`.
   - Generates and verifies RFC 6238 TOTP codes with a configurable clock-skew window.
   - Prevents OTP replay attacks within the same 30-second time-step via `lastUsedStep`.
   - Generates memory-only SVG Data URIs for QR codes (zero external CDN or network dependencies).
   - Manages challenge expiration, atomic attempt increments, lockout enforcement, and immutable audit logs.
   - Enforces ownership/password verification for self-service revocation and permission/reason checks for administrative overrides.

---

## 2. Login: how the password check becomes an MFA challenge

1. **`framework/common/src/main/java/org/apache/ofbiz/common/login/LoginServices.java`**:
   - `userLoginMfaPending`: Validates primary credentials without committing successful login mutations or writing `UserLoginHistory`.
   - `finalizeUserLogin`: Commits database mutations, resets failed counters, and logs successful history only after secondary verification succeeds.

2. **`framework/common/servicedef/services.xml`** registers `userLoginMfaPending` and `finalizeUserLogin` for invocation by the web login layer.

3. **`framework/webapp/src/main/java/org/apache/ofbiz/webapp/control/LoginWorker.java`**:
   - Evaluates MFA policy upon primary authentication success.
   - Directs the user to a challenge or forced-enrollment view while keeping `session.getAttribute("userLogin")` strictly null.
   - Performs double session rotation (`request.changeSessionId()`) before and after MFA verification.
   - Maintains rolling step-up proof timestamps (`_MFA_VERIFIED_AT_`) in session for sensitive operations.

4. **`framework/webapp/src/main/java/org/apache/ofbiz/webapp/control/ExternalLoginKeysManager.java`**:
   - Propagates verified `_MFA_VERIFIED_AT_` timestamps across webapps via `externalLoginKey` SSO, preventing duplicate challenges during cross-component navigation.

5. **`framework/security/config/security.properties`** provides operational toggles:
   - `security.login.authFactor.enable=false` (Master switch)
   - `security.login.authFactor.pendingTimeout=300` (Challenge TTL in seconds)
   - `security.login.authFactor.maxVerifyAttempts=5` (Max attempts before challenge lockout)
   - `security.login.authFactor.reauthenticationWindow=300` (Step-up re-authentication window)
   - `security.login.authFactor.totp.issuer=Apache OFBiz` (TOTP authenticator label)
   - `security.login.authFactor.totp.window=1` (Clock skew tolerance)

---

## 3. Screens and browser routes: what the user sees

1. **`framework/common/webcommon/WEB-INF/common-controller.xml`** maps common web requests to handlers and views:
   - `/authFactorChallenge`: Renders the OTP verification screen for web login.
   - `/verifyAuthFactor`: Processes the submitted OTP code.
   - `/authFactorSetup`: Self-service MFA management portal under user profile.
   - `/authFactorForcedEnrollment`: Forced-enrollment interstitial screen for users mandated by security group policy.
   - `/beginTotpEnrollment` & `/confirmTotpEnrollment`: Multi-step enrollment handshake.
   - `/revokeMfaFactor`: Allows self-service factor revocation requiring password re-authentication.

2. **`framework/common/widget/CommonScreens.xml` & `CommonForms.xml`**:
   - Define the screens and forms for challenge input, forced enrollment, QR code rendering, and factor management.

3. **Theme Integration**:
   - Profile avatar menus in `flatgrey`, `helveticus`, and `rainbowstone` include links to `/webtools/control/authFactorSetup`.

---

## 4. Security administration: group policy, factor review, and recovery

1. **`framework/common/widget/SecurityForms.xml`**:
   - Adds `requireAuthFactor` to the Security Group edit form.
   - Adds mandatory `mfaAdminReason` field to the administrative password recovery form.

2. **`framework/common/widget/SecurityScreens.xml`**:
   - Security group policy: `/webtools/control/EditSecurityGroup?groupId=GROUP_ID`.
   - User factor management & administrative recovery: `/webtools/control/editlogin?userLoginId=USER_LOGIN_ID`.

3. **`framework/webapp/src/main/java/org/apache/ofbiz/webapp/control/LoginWorker.java`**:
   - Enforces `SECURITY_PWD_UPDATE` permission and mandatory reason logging when administrators revoke factors or reset passwords for MFA-enforced accounts.

---

## 5. Password and recovery entry points

- **Common security password update:** `framework/common/webcommon/WEB-INF/security-controller.xml` maps `updatePassword` to `LoginWorker.updatePasswordWithMfa`.
- **Party profile password update:** `applications/party/webapp/partymgr/WEB-INF/controller.xml` maps `ProfileUpdatePassword` to the MFA-aware handler.
- **Ecommerce customer password update:** `plugins/ecommerce/minilang/customer/CustomerEvents.xml` enforces recent MFA proof before customer password modifications.
- **Password hints and email-token recovery:** `applications/securityext/src/main/java/org/apache/ofbiz/securityext/login/LoginEvents.java` blocks unauthenticated password recovery options for MFA-enforced accounts and records audit events.

---

## 6. B2C eCommerce storefront integration (`plugins/ecommerce`)

- **`ecommerce/webapp/ecommerce/WEB-INF/controller.xml`**:
  - Maps `storeLogin` and `storeCheckLogin` responses to `authFactorRequired` and `authFactorEnrollmentRequired`.
  - Defines storefront endpoints: `authFactorSetup`, `beginTotpEnrollment`, `confirmTotpEnrollment`, and `revokeMfaFactor`.
- **Native Storefront FreeMarker Templates**:
  - `AuthFactorSetup.ftl`: Customer portal under profile for managing and revoking registered TOTP authenticators.
  - `AuthFactorEnrollment.ftl`: In-line customer enrollment view with memory-generated SVG QR code and manual secret key.
  - `AuthFactorChallenge.ftl`: Responsive login challenge form ensuring seamless brand continuity during customer second-factor verification.
- **`ecommerce/template/customer/ViewProfile.ftl`**:
  - Links directly to `authFactorSetup` from the customer profile interface.

---

## 7. REST API & modern token assurance

- **`framework/rest-api/src/main/java/org/apache/ofbiz/ws/rs/resources/AuthenticationResource.java`**:
  - **Step 1 (`POST /rest/auth/token`):** When primary credentials pass for an MFA-enforced user, the server responds with HTTP 202 `MFA_REQUIRED` and an ephemeral `challengeId`.
  - **Step 2 (`POST /rest/auth/mfa/{challengeId}/verify`):** Submits the 6-digit TOTP code. Upon verification, the server returns HTTP 200 with JWT access and refresh tokens.
  - **JWT Claims:** Issued access tokens embed RFC 8176 `amr: ["pwd", "totp"]` and `acr: "loa-mfa"` claims to verify secondary assurance downstream.

---

## 8. Tests: what has been verified

1. **`framework/common/src/test/groovy/org/apache/ofbiz/common/test/UserLoginTests.groovy` (9/9 Integration Tests Passing)**:
   - `testUserLoginMfaPendingDefersSuccessfulLoginCommit`
   - `testTotpChallengeFinalizesOnlyOnce`
   - `testMfaFactorListExcludesSecretsAndSelfRevocationRequiresPassword`
   - `testUserLoginMfaPendingRejectsServiceAuthentication`
   - `testForcedTotpEnrollmentMustBeVerifiedBeforeLoginCompletion`
   - `testMfaPasswordChangeRequiresRecentFactorProofBeforeCallingUpdateService`
   - `testMfaAdministrativePasswordRecoveryRequiresPermissionAndReasonAndAuditsSuccess`
2. **`applications/securityext/src/test/java/org/apache/ofbiz/securityext/login/LoginEventsTests.java` (Passing)**:
   - Verifies storefront login wrapper preservation of MFA challenge and enrollment states.
3. **`framework/webapp/src/test/java/org/apache/ofbiz/webapp/control/JWTManagerAssuranceTests.java` & `AuthenticationContextTests.java` (Passing)**:
   - Verifies JWT AMR/ACR claims and cross-channel trust propagation.
4. **Interactive Browser Verification**:
   - Verified end-to-end enrollment, QR code rendering, challenge completion, and self-service revocation across both backoffice and eCommerce storefront.

---

## 9. Boundaries and deployment notes

- **Global toggle:** MFA remains globally disabled by default (`security.login.authFactor.enable=false`), guaranteeing zero impact on existing test suites, internal service calls, or legacy password-only deployments.
- **Rollout recommendation:** Test in staging with selected security groups (`requireAuthFactor="Y"`) before enabling the global master toggle in production.
