package org.apache.rocketmq.sdk.shade.common.acl;

import org.apache.rocketmq.sdk.shade.common.MixAll;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
import org.springframework.beans.PropertyAccessor;

public class SessionCredentials {
    public static final String ACCESS_KEY = "AccessKey";
    public static final String SECRET_KEY = "SecretKey";
    public static final String SIGNATURE = "Signature";
    public static final String SECURITY_TOKEN = "SecurityToken";
    private String accessKey;
    private String secretKey;
    private String securityToken;
    private String signature;
    private String namespaceId;
    public static final Charset CHARSET = Charset.forName("UTF-8");
    public static final String KEY_FILE = System.getProperty("rocketmq.client.keyFile", System.getProperty("user.home") + File.separator + "key");

    public SessionCredentials() {
        Properties prop;
        String keyContent = null;
        try {
            keyContent = MixAll.file2String(KEY_FILE);
        } catch (IOException e) {
        }
        if (keyContent != null && (prop = MixAll.string2Properties(keyContent)) != null) {
            updateContent(prop);
        }
    }

    public SessionCredentials(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public SessionCredentials(String accessKey, String secretKey, String securityToken) {
        this(accessKey, secretKey);
        this.securityToken = securityToken;
    }

    public void updateContent(Properties prop) {
        String value = prop.getProperty("AccessKey");
        if (value != null) {
            this.accessKey = value.trim();
        }
        String value2 = prop.getProperty("SecretKey");
        if (value2 != null) {
            this.secretKey = value2.trim();
        }
        String value3 = prop.getProperty("SecurityToken");
        if (value3 != null) {
            this.securityToken = value3.trim();
        }
    }

    public String getAccessKey() {
        return this.accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSignature() {
        return this.signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSecurityToken() {
        return this.securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    public String getNamespaceId() {
        return this.namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * 1) + (this.accessKey == null ? 0 : this.accessKey.hashCode()))) + (this.secretKey == null ? 0 : this.secretKey.hashCode()))) + (this.signature == null ? 0 : this.signature.hashCode()))) + (this.namespaceId == null ? 0 : this.namespaceId.hashCode());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SessionCredentials other = (SessionCredentials) obj;
        if (this.accessKey == null) {
            if (other.accessKey != null) {
                return false;
            }
        } else if (!this.accessKey.equals(other.accessKey)) {
            return false;
        }
        if (this.secretKey == null) {
            if (other.secretKey != null) {
                return false;
            }
        } else if (!this.secretKey.equals(other.secretKey)) {
            return false;
        }
        if (this.signature == null) {
            if (other.signature != null) {
                return false;
            }
        } else if (!this.signature.equals(other.signature)) {
            return false;
        }
        if (this.namespaceId == null) {
            if (other.namespaceId != null) {
                return false;
            }
            return true;
        } else if (!this.namespaceId.equals(other.namespaceId)) {
            return false;
        } else {
            return true;
        }
    }

    public String toString() {
        return "SessionCredentials [accessKey=" + this.accessKey + ", secretKey=" + this.secretKey + ", signature=" + this.signature + ", SecurityToken=" + this.securityToken + ", namespaceId=" + this.namespaceId + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
