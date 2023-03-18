package org.apache.rocketmq.sdk.shade.remoting.netty;

import org.apache.rocketmq.sdk.shade.remoting.common.TlsMode;

public class TlsSystemConfig {
    public static final String TLS_ENABLE = "tls.enable";
    public static boolean tlsEnable = Boolean.parseBoolean(System.getProperty(TLS_ENABLE, "false"));
    public static final String TLS_TEST_MODE_ENABLE = "tls.test.mode.enable";
    public static boolean tlsTestModeEnable = Boolean.parseBoolean(System.getProperty(TLS_TEST_MODE_ENABLE, "true"));
    public static final String TLS_SERVER_NEED_CLIENT_AUTH = "tls.server.need.client.auth";
    public static String tlsServerNeedClientAuth = System.getProperty(TLS_SERVER_NEED_CLIENT_AUTH, "none");
    public static final String TLS_SERVER_KEYPATH = "tls.server.keyPath";
    public static String tlsServerKeyPath = System.getProperty(TLS_SERVER_KEYPATH, null);
    public static final String TLS_SERVER_KEYPASSWORD = "tls.server.keyPassword";
    public static String tlsServerKeyPassword = System.getProperty(TLS_SERVER_KEYPASSWORD, null);
    public static final String TLS_SERVER_CERTPATH = "tls.server.certPath";
    public static String tlsServerCertPath = System.getProperty(TLS_SERVER_CERTPATH, null);
    public static final String TLS_SERVER_AUTHCLIENT = "tls.server.authClient";
    public static boolean tlsServerAuthClient = Boolean.parseBoolean(System.getProperty(TLS_SERVER_AUTHCLIENT, "false"));
    public static final String TLS_SERVER_TRUSTCERTPATH = "tls.server.trustCertPath";
    public static String tlsServerTrustCertPath = System.getProperty(TLS_SERVER_TRUSTCERTPATH, null);
    public static final String TLS_CLIENT_KEYPATH = "tls.client.keyPath";
    public static String tlsClientKeyPath = System.getProperty(TLS_CLIENT_KEYPATH, null);
    public static final String TLS_CLIENT_KEYPASSWORD = "tls.client.keyPassword";
    public static String tlsClientKeyPassword = System.getProperty(TLS_CLIENT_KEYPASSWORD, null);
    public static final String TLS_CLIENT_CERTPATH = "tls.client.certPath";
    public static String tlsClientCertPath = System.getProperty(TLS_CLIENT_CERTPATH, null);
    public static final String TLS_CLIENT_AUTHSERVER = "tls.client.authServer";
    public static boolean tlsClientAuthServer = Boolean.parseBoolean(System.getProperty(TLS_CLIENT_AUTHSERVER, "false"));
    public static final String TLS_CLIENT_TRUSTCERTPATH = "tls.client.trustCertPath";
    public static String tlsClientTrustCertPath = System.getProperty(TLS_CLIENT_TRUSTCERTPATH, null);
    public static final String TLS_SERVER_MODE = "tls.server.mode";
    public static TlsMode tlsMode = TlsMode.parse(System.getProperty(TLS_SERVER_MODE, "permissive"));
    public static final String TLS_CONFIG_FILE = "tls.config.file";
    public static String tlsConfigFile = System.getProperty(TLS_CONFIG_FILE, "/etc/rocketmq/tls.properties");
}
