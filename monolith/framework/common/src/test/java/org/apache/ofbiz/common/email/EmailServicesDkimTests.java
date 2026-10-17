/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.common.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.simplejavamail.utils.mail.dkim.Canonicalization;

public final class EmailServicesDkimTests {

    private static KeyPair testKeyPair;
    private static String testPrivateKeyPem;

    @BeforeAll
    public static void generateTestKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        testKeyPair = generator.generateKeyPair();
        String base64 = Base64.getEncoder().encodeToString(testKeyPair.getPrivate().getEncoded());
        StringBuilder pem = new StringBuilder("-----BEGIN PRIVATE KEY-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            pem.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        pem.append("-----END PRIVATE KEY-----\n");
        testPrivateKeyPem = pem.toString();
    }

    @Test
    public void parsePemRsaPrivateKeyParsesValidPem() throws Exception {
        RSAPrivateCrtKey parsed = EmailServices.parsePemRsaPrivateKey(testPrivateKeyPem);
        RSAPrivateCrtKey original = (RSAPrivateCrtKey) testKeyPair.getPrivate();
        assertEquals(original.getModulus(), parsed.getModulus());
    }

    @Test
    public void parsePemRsaPrivateKeyRejectsNonBase64Garbage() {
        String badPem = "-----BEGIN PRIVATE KEY-----\nnot valid base64!!!\n-----END PRIVATE KEY-----\n";
        assertThrows(GeneralSecurityException.class, () -> EmailServices.parsePemRsaPrivateKey(badPem));
    }

    @Test
    public void parsePemRsaPrivateKeyRejectsWellFormedNonKeyBytes() {
        // Valid base64, but not a key -- must fail as GeneralSecurityException, not unchecked.
        String fakePem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString("this is definitely not a key".getBytes(StandardCharsets.UTF_8))
                + "\n-----END PRIVATE KEY-----\n";
        assertThrows(GeneralSecurityException.class, () -> EmailServices.parsePemRsaPrivateKey(fakePem));
    }

    private static Delegator mockDelegatorReturning(GenericValue... rows) throws GenericEntityException {
        Delegator delegator = mock(Delegator.class);
        when(delegator.getDelegator()).thenReturn(delegator);
        when(delegator.findList(eq("MailDkimConfig"), any(), any(), any(), any(), any(), eq(true)))
                .thenReturn(Arrays.asList(rows));
        return delegator;
    }

    private static MimeMessage buildTestMessage() throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage mail = new MimeMessage(session);
        mail.setFrom(new InternetAddress("sender@example.com"));
        mail.setRecipients(Message.RecipientType.TO, "recipient@example.com");
        mail.setSubject("Test Subject");
        mail.setText("Test body");
        mail.saveChanges();
        return mail;
    }

    @Test
    public void dkimSignReturnsOriginalWhenNoConfigRow() throws Exception {
        Delegator delegator = mockDelegatorReturning();
        MimeMessage mail = buildTestMessage();
        assertEquals(mail, EmailServices.dkimSign(mail, delegator));
    }

    @Test
    public void dkimSignReturnsOriginalWhenNotEnabled() throws Exception {
        GenericValue config = mock(GenericValue.class);
        when(config.getString("enabled")).thenReturn("N");
        Delegator delegator = mockDelegatorReturning(config);
        MimeMessage mail = buildTestMessage();
        assertEquals(mail, EmailServices.dkimSign(mail, delegator));
    }

    @Test
    public void dkimSignReturnsOriginalWhenIncompleteConfig() throws Exception {
        GenericValue config = mock(GenericValue.class);
        when(config.getString("enabled")).thenReturn("Y");
        when(config.getString("signingDomain")).thenReturn("example.com");
        when(config.getString("selector")).thenReturn("");
        when(config.getString("privateKey")).thenReturn("");
        when(config.getString("mailDkimConfigId")).thenReturn("TEST_DKIM_1");
        Delegator delegator = mockDelegatorReturning(config);
        MimeMessage mail = buildTestMessage();
        assertEquals(mail, EmailServices.dkimSign(mail, delegator));
    }

    @Test
    public void dkimSignReturnsOriginalWhenPrivateKeyIsGarbage() throws Exception {
        GenericValue config = mock(GenericValue.class);
        when(config.getString("enabled")).thenReturn("Y");
        when(config.getString("signingDomain")).thenReturn("example.com");
        when(config.getString("selector")).thenReturn("ofbiz");
        when(config.getString("privateKey")).thenReturn("not a real key");
        when(config.getString("mailDkimConfigId")).thenReturn("TEST_DKIM_1");
        Delegator delegator = mockDelegatorReturning(config);
        MimeMessage mail = buildTestMessage();
        assertEquals(mail, EmailServices.dkimSign(mail, delegator));
    }

    @Test
    public void dkimSignWrapsAndSignsWhenFullyConfigured() throws Exception {
        GenericValue config = mock(GenericValue.class);
        when(config.getString("enabled")).thenReturn("Y");
        when(config.getString("signingDomain")).thenReturn("example.com");
        when(config.getString("selector")).thenReturn("ofbiz");
        when(config.getString("privateKey")).thenReturn(testPrivateKeyPem);
        when(config.getString("mailDkimConfigId")).thenReturn("TEST_DKIM_1");
        Delegator delegator = mockDelegatorReturning(config);
        MimeMessage mail = buildTestMessage();

        MimeMessage result = EmailServices.dkimSign(mail, delegator);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.writeTo(baos);
        String raw = baos.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("DKIM-Signature:"), "expected a DKIM-Signature header, got:\n" + raw);
        assertTrue(raw.contains("d=example.com"), "expected d=example.com in signature, got:\n" + raw);
        assertTrue(raw.contains("s=ofbiz"), "expected s=ofbiz in signature, got:\n" + raw);
        assertTrue(raw.contains("a=rsa-sha256"), "expected a=rsa-sha256 in signature, got:\n" + raw);
        assertTrue(raw.contains("c=relaxed/relaxed"), "expected c=relaxed/relaxed in signature, got:\n" + raw);
        // Anchored on the real tag separators (" " or "\r\n\t") rather than a bare "l=" substring,
        // which could spuriously match unrelated future body/subject/header content.
        assertTrue(!raw.contains(" l=") && !raw.contains("\r\n\tl="),
                "DKIM-Signature must not include an l= tag (setLengthParam must stay false), got:\n" + raw);
    }

    @Test
    public void dkimSignatureVerifiesAgainstDerivedPublicKey() throws Exception {
        GenericValue config = mock(GenericValue.class);
        when(config.getString("enabled")).thenReturn("Y");
        when(config.getString("signingDomain")).thenReturn("example.com");
        when(config.getString("selector")).thenReturn("ofbiz");
        when(config.getString("privateKey")).thenReturn(testPrivateKeyPem);
        when(config.getString("mailDkimConfigId")).thenReturn("TEST_DKIM_1");
        Delegator delegator = mockDelegatorReturning(config);
        MimeMessage mail = buildTestMessage();

        MimeMessage result = EmailServices.dkimSign(mail, delegator);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.writeTo(baos);
        String raw = baos.toString(StandardCharsets.UTF_8);

        int blankLineIndex = raw.indexOf("\r\n\r\n");
        String headerBlock = raw.substring(0, blankLineIndex);
        String bodyBlock = raw.substring(blankLineIndex + 4);

        // DkimMessage.writeTo() always writes DKIM-Signature first; capture its full (possibly
        // folded) value up to the first CRLF not followed by continuation whitespace.
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^DKIM-Signature: (.*?)\r\n(?=\\S)", java.util.regex.Pattern.DOTALL)
                .matcher(headerBlock + "\r\n");
        assertTrue(m.find(), "could not locate DKIM-Signature header in:\n" + headerBlock);
        String dkimSignatureRawValue = m.group(1);

        // Anchor on "\r\n\tb=" -- the exact literal DkimSigner.serializeSignature emits before the
        // signature (DkimSigner.java:623) -- rather than a bare lastIndexOf("b=").
        int bTagIndex = dkimSignatureRawValue.lastIndexOf("\r\n\tb=");
        assertTrue(bTagIndex >= 0, "could not find the b= tag boundary in:\n" + dkimSignatureRawValue);
        String preSignaturePortion = dkimSignatureRawValue.substring(0, bTagIndex + 5);
        String signatureBase64 = dkimSignatureRawValue.substring(bTagIndex + 5).replaceAll("\\s+", "");

        // DkimSigner's default signed-header set (DEFAULT_HEADERS_TO_SIGN), case-insensitive like the
        // library's own TreeSet<>(CASE_INSENSITIVE_ORDER) (DkimSigner.java:115) -- jakarta.mail's own
        // header casing varies ("Message-Id" vs "Message-ID").
        java.util.Set<String> headersToSign = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headersToSign.addAll(java.util.Arrays.asList(
                "From", "To", "Subject", "Content-Description", "Content-ID", "Content-Type",
                "Content-Transfer-Encoding", "Cc", "Date", "In-Reply-To", "List-Subscribe", "List-Post",
                "List-Owner", "List-Id", "List-Archive", "List-Help", "List-Unsubscribe", "MIME-Version",
                "Message-ID", "Resent-Sender", "Resent-Cc", "Resent-Date", "Resent-To", "Reply-To",
                "References", "Resent-Message-ID", "Resent-From", "Sender"));

        // Iterate result (the DkimMessage), not mail: DkimMessage.writeTo() signs itself
        // (DkimMessage.java:113), and compileHeadersToSign reverses the header order via add(0, ...).
        java.util.List<jakarta.mail.Header> matchedHeaders = new java.util.ArrayList<>();
        java.util.Enumeration<jakarta.mail.Header> allHeaders = result.getAllHeaders();
        while (allHeaders.hasMoreElements()) {
            jakarta.mail.Header h = allHeaders.nextElement();
            if (headersToSign.contains(h.getName())) {
                matchedHeaders.add(0, h);
            }
        }

        // Cross-check against the signature's own h= tag first, so a mismatch is a readable diff
        // rather than an opaque verify() failure.
        StringBuilder reconstructedHeaderNames = new StringBuilder();
        for (jakarta.mail.Header h : matchedHeaders) {
            reconstructedHeaderNames.append(h.getName()).append(":");
        }
        String hTag = reconstructedHeaderNames.substring(0, reconstructedHeaderNames.length() - 1);
        assertTrue(preSignaturePortion.contains("h=" + hTag + ";"),
                "reconstructed signed-header list did not match the signature's h= tag -- reconstructed [" + hTag
                        + "], signature says:\n" + preSignaturePortion);

        StringBuilder signedBytesBuilder = new StringBuilder();
        for (jakarta.mail.Header h : matchedHeaders) {
            signedBytesBuilder.append(Canonicalization.RELAXED.canonicalizeHeader(h.getName(), h.getValue())).append("\r\n");
        }
        signedBytesBuilder.append(Canonicalization.RELAXED.canonicalizeHeader("DKIM-Signature", preSignaturePortion));
        byte[] signedBytes = signedBytesBuilder.toString().getBytes(StandardCharsets.UTF_8);

        // Sanity check: bh= in the real signature should match our independently-canonicalized body.
        String canonicalBody = Canonicalization.RELAXED.canonicalizeBody(bodyBlock);
        String expectedBodyHash = Base64.getEncoder().encodeToString(
                java.security.MessageDigest.getInstance("SHA-256").digest(canonicalBody.getBytes(StandardCharsets.UTF_8)));
        assertTrue(preSignaturePortion.contains("bh=" + expectedBodyHash),
                "independently-canonicalized body hash did not match the signature's bh= tag -- got preSignaturePortion:\n"
                        + preSignaturePortion + "\nexpected bh=" + expectedBodyHash);

        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        // Verify against the SAME key used to sign (sanity check on our own reconstruction).
        java.security.Signature verifier = java.security.Signature.getInstance("SHA256withRSA");
        verifier.initVerify(testKeyPair.getPublic());
        verifier.update(signedBytes);
        assertTrue(verifier.verify(signatureBytes),
                "reconstructed signed bytes did not verify against the original test key -- our canonicalization "
                        + "reconstruction is wrong somewhere, not a real signing defect. Report this back rather than "
                        + "adjusting the assertion.");

        // The point of this test: verify against getDkimDnsRecord's published key too, proving the
        // two features agree.
        String recordValue = EmailServices.derivePublicKeyRecordValue((RSAPrivateCrtKey) testKeyPair.getPrivate());
        String base64PublicKeyFromRecord = recordValue.substring("v=DKIM1; k=rsa; p=".length());
        java.security.PublicKey publicKeyFromRecord = KeyFactory.getInstance("RSA")
                .generatePublic(new java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKeyFromRecord)));
        java.security.Signature verifier2 = java.security.Signature.getInstance("SHA256withRSA");
        verifier2.initVerify(publicKeyFromRecord);
        verifier2.update(signedBytes);
        assertTrue(verifier2.verify(signatureBytes),
                "DKIM signature did not verify against the public key getDkimDnsRecord would publish for the same "
                        + "MailDkimConfig -- this would mean the signing feature and the DNS-record helper feature "
                        + "are inconsistent with each other.");
    }

    @Test
    public void derivePublicKeyRecordValueMatchesOriginalPublicKey() throws Exception {
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) testKeyPair.getPrivate();
        String recordValue = EmailServices.derivePublicKeyRecordValue(privateKey);

        assertTrue(recordValue.startsWith("v=DKIM1; k=rsa; p="), "unexpected record value: " + recordValue);
        String base64PublicKey = recordValue.substring("v=DKIM1; k=rsa; p=".length());
        byte[] decoded = Base64.getDecoder().decode(base64PublicKey);
        PublicKey reconstructed = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));

        assertEquals(testKeyPair.getPublic().getEncoded().length, reconstructed.getEncoded().length);
        assertTrue(Arrays.equals(testKeyPair.getPublic().getEncoded(), reconstructed.getEncoded()),
                "reconstructed public key bytes did not match the original keypair's public key");
    }
}
