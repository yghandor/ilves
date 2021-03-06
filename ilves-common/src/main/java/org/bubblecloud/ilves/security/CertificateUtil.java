package org.bubblecloud.ilves.security;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bubblecloud.ilves.util.PropertiesUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Utility class for certificate management.
 *
 * @author Tommi S.E. Laukkanen
 */
public class CertificateUtil {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(CertificateUtil.class);
    /**
     * Ensure that BouncyCastle provider is loaded.
     */
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    /**
     * The security provider.
     */
    public static final String PROVIDER = "BC";
    /**
     * The keystore type.
     */
    public static final String KEY_STORE_TYPE = "BKS";
    /**
     * The certificate asymmetric encryption algorithm.
     */
    public static final String CERTIFICATE_ENCRYPTION_ALGORITHM = "RSA";
    /**
     * The certificate asymmetric encryption key size.
     */
    public static final int CERTIFICATE_KEY_SIZE = Integer.parseInt(
            PropertiesUtil.getProperty("site", "server-certificate-self-signed-key-size"));
    /**
     * The certificate signature algorithm.
     */
    public static final String CERTIFICATE_SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";

    /**
     * Checks server certificate exists and if it does not then generates self signed certificate it.
     *
     * @param certificateCommonName the certificate common name
     * @param ipAddress the certificate subject alternative name IP address or null
     * @param certificateAlias the certificate alias
     * @param certificatePrivateKeyPassword the certificate private key password
     * @param keyStorePath the key store path
     * @param keyStorePassword the key store password
     */
    public static void ensureServerCertificateExists(final String certificateCommonName,
                                                     final String ipAddress,
                                                     final String certificateAlias,
                                                     final String certificatePrivateKeyPassword,
                                                     final String keyStorePath,
                                                     final String keyStorePassword) {
        if (!hasCertificate(certificateAlias, keyStorePath, keyStorePassword)) {
            generateSelfSignedCertificate(certificateAlias, certificateCommonName, ipAddress,
                    keyStorePath, keyStorePassword, certificatePrivateKeyPassword);
        }
    }

    /**
     * Checks whether key store contains given certificate.
     * @param certificateAlias the certificate alias
     * @param keyStorePath the key store path
     * @param keyStorePassword the key store password
     * @return TRUE if certificate exists.
     */
    public static boolean hasCertificate(final String certificateAlias,
                                         final String keyStorePath,
                                         final String keyStorePassword) {
        try {
            final KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);
            return keyStore.containsAlias(certificateAlias);
        } catch (final Exception e) {
            throw new SecurityException("Error checking if certificate exists '" + certificateAlias + "' in key store: "
                    + keyStorePath, e);
        }
    }

    /**
     * Gets certificate from key store.
     * @param certificateAlias the certificate alias
     * @param keyStorePath the key store path
     * @param keyStorePassword the key store password
     * @return the certificate or null if certificate does not exist.
     */
    public static X509Certificate getCertificate(final String certificateAlias,
                                                 final String keyStorePath,
                                                 final String keyStorePassword) {
        try {
            final KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);
            return (X509Certificate) keyStore.getCertificate(certificateAlias);
        } catch (final Exception e) {
            throw new SecurityException("Error loading certificate '" + certificateAlias + "' from key store: "
                    + keyStorePath, e);
        }
    }

    /**
     * Saves certificate to key store.
     * @param certificateAlias the certificate alias
     * @param keyStorePath the key store path
     * @param keyStorePassword the key store password
     * @param certificate the certificate
     */
    public static void saveCertificate(final String certificateAlias,
                                                 final String keyStorePath,
                                                 final String keyStorePassword, final X509Certificate certificate) {
        try {
            final KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);
            keyStore.setCertificateEntry(certificateAlias, certificate);
            saveKeyStore(keyStore, keyStorePath, keyStorePassword);
        } catch (final Exception e) {
            throw new SecurityException("Error saving certificate '" + certificateAlias + "' to key store: "
                    + keyStorePath, e);
        }
    }

    /**
     * Gets certificate private key from key store.
     * @param certificateAlias the certificate alias
     * @param keyStorePath the key store path
     * @param keyStorePassword the key store password
     * @param keyEntryPassword the key entry password
     * @return the certificate or null if certificate does not exist.
     */
    public static PrivateKey getPrivateKey(final String certificateAlias,
                                                 final String keyStorePath,
                                                 final String keyStorePassword,
                                                 final String keyEntryPassword) {
        try {
            final KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);
            return (PrivateKey) keyStore.getKey(certificateAlias, keyEntryPassword.toCharArray());
        } catch (final Exception e) {
            throw new SecurityException("Error loading private key '" + certificateAlias + "' from key store: "
                    + keyStorePath, e);
        }
    }

    /**
     * Removes certificate from key store.
     * @param certificateAlias the certificate alias
     * @param keyStorePath the key store path
     * @param keyStorePassword the key store password
     */
    public static void removeCertificate(final String certificateAlias,
                                                 final String keyStorePath,
                                                 final String keyStorePassword) {
        try {
            final KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);
            keyStore.deleteEntry(certificateAlias);
            saveKeyStore(keyStore, keyStorePath, keyStorePassword);
        } catch (final Exception e) {
            throw new SecurityException("Error removing certificate '" + certificateAlias + "' from key store: "
                    + keyStorePath, e);
        }
    }

    /**
     * Generates and self signed certificate and saves it to key store.
     * @param alias the certificate alias
     * @param commonName the certificate common name
     * @param ipAddress the subject alternative name IP address or null
     * @param keyStorePath the key store path
     * @param keyStorePassword the key store password
     * @param keyEntryPassword the key entry password
     */
    private static void generateSelfSignedCertificate(final String alias,
                                                     final String commonName,
                                                     final String ipAddress,
                                                     final String keyStorePath,
                                                     final String keyStorePassword,
                                                     final String keyEntryPassword) {
        try {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(CERTIFICATE_ENCRYPTION_ALGORITHM, PROVIDER);
            keyGen.initialize(CERTIFICATE_KEY_SIZE);
            final KeyPair keyPair = keyGen.generateKeyPair();
            final X509Certificate certificate = buildCertificate(commonName,ipAddress, keyPair);
            LOGGER.info("Generated self signed certificate: " + certificate);
            final KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);
            keyStore.setKeyEntry(alias, (Key) keyPair.getPrivate(), keyEntryPassword.toCharArray(),
                    new X509Certificate[]{certificate});
            saveKeyStore(keyStore, keyStorePath, keyStorePassword);
        } catch (final Exception e) {
            throw new RuntimeException("Unable to generate self signed certificate.", e);
        }
    }

    /**
     * Generates and self signed certificate and saves it to key store with fingerprint as alias.
     *
     * @param commonName the certificate common name
     * @param ipAddress the subject alternative name IP address or null
     * @param keyStorePath the key store path
     * @param keyStorePassword the key store password
     * @param keyEntryPassword the key entry password
     * @return fingerprint e.q. alias of the generated certificate.
     */
    public static String generateSelfSignedCertificate(final String commonName,
                                                      final String ipAddress,
                                                      final String keyStorePath,
                                                      final String keyStorePassword,
                                                      final String keyEntryPassword) {
        try {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(CERTIFICATE_ENCRYPTION_ALGORITHM, PROVIDER);
            keyGen.initialize(CERTIFICATE_KEY_SIZE);
            final KeyPair keyPair = keyGen.generateKeyPair();
            final X509Certificate certificate = buildCertificate(commonName,ipAddress, keyPair);
            final String alias = DigestUtils.sha256Hex(certificate.getEncoded());
            LOGGER.info("Generated self signed certificate: " + certificate);
            final KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);
            keyStore.setKeyEntry(alias, (Key) keyPair.getPrivate(), keyEntryPassword.toCharArray(),
                    new X509Certificate[]{certificate});
            saveKeyStore(keyStore, keyStorePath, keyStorePassword);
            return alias;
        } catch (final Exception e) {
            throw new RuntimeException("Unable to generate self signed certificate.", e);
        }
    }

    /**
     * Loads key store.
     * @param keyStorePath the key store path
     * @param keyStorePassword the key store password
     * @return the key store
     */
    public static KeyStore loadKeyStore(final String keyStorePath, final String keyStorePassword) {
        try {
            final File keyStoreFile = new File(keyStorePath);
            if (keyStoreFile.exists()) {
                final FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFile);
                final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE, PROVIDER);
                keyStore.load(keyStoreInputStream, keyStorePassword.toCharArray());
                keyStoreInputStream.close();
                return keyStore;
            } else {
                final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE, PROVIDER);
                keyStore.load(null, keyStorePassword.toCharArray());
                return keyStore;
            }
        } catch (final Exception e) {
            throw new SecurityException("Unable to load key store: " + keyStorePath, e);
        }
    }

    /**
     * Saves key store.
     * @param keyStore the key store
     * @param keyStorePath the key store path
     * @param keyStorePassword the key store password
     */
    public static void saveKeyStore(final KeyStore keyStore, final String keyStorePath, final String keyStorePassword) {
        try {
            final FileOutputStream keyStoreOutputStream = new FileOutputStream(keyStorePath, false);
            keyStore.store(keyStoreOutputStream, keyStorePassword.toCharArray());
            keyStoreOutputStream.flush();
            keyStoreOutputStream.close();
        } catch (final Exception e) {
            throw new SecurityException("Unable to save key store: " + keyStorePath, e);
        }
    }

    /**
     * Build self signed certificate from key pair.
     * @param commonName the certificate common name
     * @param ipAddress the subject alternative name IP address or null
     * @param keyPair the key pair.
     * @return the certificate
     * @throws Exception if error occurs in certificate generation process.
     */
    private static X509Certificate buildCertificate(final String commonName, final String ipAddress,
                                                    KeyPair keyPair) throws Exception {

        final Date notBefore = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
        final Date notAfter = DateUtils.addYears(notBefore, 100);
        final BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        final X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.CN, commonName);

        final SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(
                ASN1Sequence.getInstance(keyPair.getPublic().getEncoded()));

        final X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(nameBuilder.build(),
                serial, notBefore, notAfter, nameBuilder.build(), subjectPublicKeyInfo);

        if (ipAddress != null) {
            certGen.addExtension(Extension.subjectAlternativeName,
                    false, new GeneralNames(
                            new GeneralName(GeneralName.iPAddress, ipAddress)));
        }


        final ContentSigner sigGen = new JcaContentSignerBuilder(CERTIFICATE_SIGNATURE_ALGORITHM)
                .setProvider(PROVIDER).build(keyPair.getPrivate());
        final X509Certificate cert = new JcaX509CertificateConverter().setProvider(PROVIDER)
                .getCertificate(certGen.build(sigGen));

        return cert;
    }

}
