package org.apache.rocketmq.sdk.shade.remoting.netty;

import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.Properties;

public class TlsHelper {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);
    private static DecryptionStrategy decryptionStrategy = new DecryptionStrategy() {
        @Override
        public InputStream decryptPrivateKey(String privateKeyEncryptPath, boolean forClient) throws IOException {
            return new FileInputStream(privateKeyEncryptPath);
        }
    };

    public interface DecryptionStrategy {
        InputStream decryptPrivateKey(String str, boolean z) throws IOException;
    }

    public static void registerDecryptionStrategy(DecryptionStrategy decryptionStrategy2) {
        decryptionStrategy = decryptionStrategy2;
    }

    public static SslContext buildSslContext(boolean forClient) throws IOException, CertificateException {
        SslProvider provider;
        extractTlsConfigFromFile(new File(TlsSystemConfig.tlsConfigFile));
        logTheFinalUsedTlsConfig();
        if (OpenSsl.isAvailable()) {
            provider = SslProvider.OPENSSL;
            LOGGER.info("Using OpenSSL provider");
        } else {
            provider = SslProvider.JDK;
            LOGGER.info("Using JDK SSL provider");
        }
        if (forClient) {
            if (TlsSystemConfig.tlsTestModeEnable) {
                return SslContextBuilder.forClient().sslProvider(SslProvider.JDK).trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            }
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient().sslProvider(SslProvider.JDK);
            if (!TlsSystemConfig.tlsClientAuthServer) {
                sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            } else if (!isNullOrEmpty(TlsSystemConfig.tlsClientTrustCertPath)) {
                sslContextBuilder.trustManager(new File(TlsSystemConfig.tlsClientTrustCertPath));
            }
            return sslContextBuilder.keyManager(!isNullOrEmpty(TlsSystemConfig.tlsClientCertPath) ? new FileInputStream(TlsSystemConfig.tlsClientCertPath) : null, !isNullOrEmpty(TlsSystemConfig.tlsClientKeyPath) ? decryptionStrategy.decryptPrivateKey(TlsSystemConfig.tlsClientKeyPath, true) : null, !isNullOrEmpty(TlsSystemConfig.tlsClientKeyPassword) ? TlsSystemConfig.tlsClientKeyPassword : null).build();
        } else if (TlsSystemConfig.tlsTestModeEnable) {
            SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
            return SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey()).sslProvider(SslProvider.JDK).clientAuth(ClientAuth.OPTIONAL).build();
        } else {
            SslContextBuilder sslContextBuilder2 = SslContextBuilder.forServer(!isNullOrEmpty(TlsSystemConfig.tlsServerCertPath) ? new FileInputStream(TlsSystemConfig.tlsServerCertPath) : null, !isNullOrEmpty(TlsSystemConfig.tlsServerKeyPath) ? decryptionStrategy.decryptPrivateKey(TlsSystemConfig.tlsServerKeyPath, false) : null, !isNullOrEmpty(TlsSystemConfig.tlsServerKeyPassword) ? TlsSystemConfig.tlsServerKeyPassword : null).sslProvider(provider);
            if (!TlsSystemConfig.tlsServerAuthClient) {
                sslContextBuilder2.trustManager(InsecureTrustManagerFactory.INSTANCE);
            } else if (!isNullOrEmpty(TlsSystemConfig.tlsServerTrustCertPath)) {
                sslContextBuilder2.trustManager(new File(TlsSystemConfig.tlsServerTrustCertPath));
            }
            sslContextBuilder2.clientAuth(parseClientAuthMode(TlsSystemConfig.tlsServerNeedClientAuth));
            return sslContextBuilder2.build();
        }
    }

    private static void extractTlsConfigFromFile(File configFile) {
        if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
            LOGGER.info("Tls config file doesn't exist, skip it");
            return;
        }
        Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFile);
            properties.load(inputStream);
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        } catch (IOException e2) {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                }
            }
        } catch (Throwable th) {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
        TlsSystemConfig.tlsTestModeEnable = Boolean.parseBoolean(properties.getProperty(TlsSystemConfig.TLS_TEST_MODE_ENABLE, String.valueOf(TlsSystemConfig.tlsTestModeEnable)));
        TlsSystemConfig.tlsServerNeedClientAuth = properties.getProperty(TlsSystemConfig.TLS_SERVER_NEED_CLIENT_AUTH, TlsSystemConfig.tlsServerNeedClientAuth);
        TlsSystemConfig.tlsServerKeyPath = properties.getProperty(TlsSystemConfig.TLS_SERVER_KEYPATH, TlsSystemConfig.tlsServerKeyPath);
        TlsSystemConfig.tlsServerKeyPassword = properties.getProperty(TlsSystemConfig.TLS_SERVER_KEYPASSWORD, TlsSystemConfig.tlsServerKeyPassword);
        TlsSystemConfig.tlsServerCertPath = properties.getProperty(TlsSystemConfig.TLS_SERVER_CERTPATH, TlsSystemConfig.tlsServerCertPath);
        TlsSystemConfig.tlsServerAuthClient = Boolean.parseBoolean(properties.getProperty(TlsSystemConfig.TLS_SERVER_AUTHCLIENT, String.valueOf(TlsSystemConfig.tlsServerAuthClient)));
        TlsSystemConfig.tlsServerTrustCertPath = properties.getProperty(TlsSystemConfig.TLS_SERVER_TRUSTCERTPATH, TlsSystemConfig.tlsServerTrustCertPath);
        TlsSystemConfig.tlsClientKeyPath = properties.getProperty(TlsSystemConfig.TLS_CLIENT_KEYPATH, TlsSystemConfig.tlsClientKeyPath);
        TlsSystemConfig.tlsClientKeyPassword = properties.getProperty(TlsSystemConfig.TLS_CLIENT_KEYPASSWORD, TlsSystemConfig.tlsClientKeyPassword);
        TlsSystemConfig.tlsClientCertPath = properties.getProperty(TlsSystemConfig.TLS_CLIENT_CERTPATH, TlsSystemConfig.tlsClientCertPath);
        TlsSystemConfig.tlsClientAuthServer = Boolean.parseBoolean(properties.getProperty(TlsSystemConfig.TLS_CLIENT_AUTHSERVER, String.valueOf(TlsSystemConfig.tlsClientAuthServer)));
        TlsSystemConfig.tlsClientTrustCertPath = properties.getProperty(TlsSystemConfig.TLS_CLIENT_TRUSTCERTPATH, TlsSystemConfig.tlsClientTrustCertPath);
    }

    private static void logTheFinalUsedTlsConfig() {
        LOGGER.info("Log the final used tls related configuration");
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_TEST_MODE_ENABLE, Boolean.valueOf(TlsSystemConfig.tlsTestModeEnable));
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_SERVER_NEED_CLIENT_AUTH, TlsSystemConfig.tlsServerNeedClientAuth);
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_SERVER_KEYPATH, TlsSystemConfig.tlsServerKeyPath);
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_SERVER_KEYPASSWORD, TlsSystemConfig.tlsServerKeyPassword);
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_SERVER_CERTPATH, TlsSystemConfig.tlsServerCertPath);
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_SERVER_AUTHCLIENT, Boolean.valueOf(TlsSystemConfig.tlsServerAuthClient));
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_SERVER_TRUSTCERTPATH, TlsSystemConfig.tlsServerTrustCertPath);
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_CLIENT_KEYPATH, TlsSystemConfig.tlsClientKeyPath);
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_CLIENT_KEYPASSWORD, TlsSystemConfig.tlsClientKeyPassword);
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_CLIENT_CERTPATH, TlsSystemConfig.tlsClientCertPath);
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_CLIENT_AUTHSERVER, Boolean.valueOf(TlsSystemConfig.tlsClientAuthServer));
        LOGGER.info("{} = {}", TlsSystemConfig.TLS_CLIENT_TRUSTCERTPATH, TlsSystemConfig.tlsClientTrustCertPath);
    }

    private static ClientAuth parseClientAuthMode(String authMode) {
        if (null == authMode || authMode.trim().isEmpty()) {
            return ClientAuth.NONE;
        }
        ClientAuth[] values = ClientAuth.values();
        for (ClientAuth clientAuth : values) {
            if (clientAuth.name().equals(authMode.toUpperCase())) {
                return clientAuth;
            }
        }
        return ClientAuth.NONE;
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
