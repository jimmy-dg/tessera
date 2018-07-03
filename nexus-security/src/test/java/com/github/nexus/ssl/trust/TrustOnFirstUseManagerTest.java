package com.github.nexus.ssl.trust;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.*;

public class TrustOnFirstUseManagerTest {

    private TrustOnFirstUseManager trustManager;

    Path knownHosts;

    @Mock
    X509Certificate certificate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        knownHosts = Paths.get("knownHosts");
    }

    @After
    public void after() throws IOException {
        Files.deleteIfExists(knownHosts);
        verifyNoMoreInteractions(certificate);
        assertThat(Files.exists(knownHosts)).isFalse();
    }


    @Test
    public void testAddThumbPrintToKnownHostsList() throws CertificateException, IOException {
        trustManager = new TrustOnFirstUseManager(knownHosts);
        when(certificate.getEncoded()).thenReturn("certificate".getBytes());

        assertThat(Files.exists(knownHosts)).isFalse();

        trustManager.checkServerTrusted(new X509Certificate[]{certificate}, "s");

        assertThat(Files.exists(knownHosts)).isTrue();

        trustManager.checkClientTrusted(new X509Certificate[]{certificate}, "s");
        verify(certificate, times(2)).getEncoded();

    }

    @Test
    public void testFailedToGenerateWhiteListFile() throws IOException, CertificateEncodingException {
//        TemporaryFolder tmpDir = mock(TemporaryFolder.class);
//        Path anotherFile = mock(Path.class);
//        when(anotherFile.getParent()).thenReturn();
//        trustManager = new TrustOnFirstUseManager(anotherFile);
//        when(certificate.getEncoded()).thenReturn("certificate".getBytes());
//
//        try {
//            trustManager.checkServerTrusted(new X509Certificate[]{certificate}, "str");
//            failBecauseExceptionWasNotThrown(IOException.class);
//        } catch (Exception ex) {
//            assertThat(ex)
//                .isInstanceOf(CertificateException.class)
//                .hasMessage("Failed to save address and certificate fingerprint to whitelist");
//        }
//
//        verify(certificate).getEncoded();

    }

    @Test
    public void testAddFingerPrintFailedToWrite() throws CertificateException, IOException {
//        TemporaryFolder tmpDir = mock(TemporaryFolder.class);
//        Path notWritable = mock(Path.class);
//        when(notWritable.toFile().canWrite()).thenReturn(false);
//
//        trustManager = new TrustOnFirstUseManager(notWritable);
//
//        X509Certificate certificate = mock(X509Certificate.class);
//        when(certificate.getEncoded()).thenReturn("certificate".getBytes());
//
//        try {
//            trustManager.checkServerTrusted(new X509Certificate[]{certificate}, "s");
//            trustManager.checkClientTrusted(new X509Certificate[]{certificate}, "s");
//
//            failBecauseExceptionWasNotThrown(CertificateException.class);
//        } catch (Exception ex) {
//            assertThat(ex).isInstanceOf(CertificateException.class);
//        }
    }

    @Test
    public void testGetAcceptIssuers() throws IOException {
        trustManager = new TrustOnFirstUseManager(knownHosts);
        assertThat(trustManager.getAcceptedIssuers()).isEmpty();
    }
}
