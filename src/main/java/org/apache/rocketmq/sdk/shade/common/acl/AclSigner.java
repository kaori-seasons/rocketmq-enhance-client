package org.apache.rocketmq.sdk.shade.common.acl;

import org.apache.rocketmq.sdk.shade.common.acl.binary.Base64;
import org.apache.rocketmq.sdk.shade.common.constant.LoggerName;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;

import java.nio.charset.Charset;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AclSigner {
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final SigningAlgorithm DEFAULT_ALGORITHM = SigningAlgorithm.HmacSHA1;
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.ROCKETMQ_AUTHORIZE_LOGGER_NAME);
    private static final int CAL_SIGNATURE_FAILED = 10015;
    private static final String CAL_SIGNATURE_FAILED_MSG = "[%s:signature-failed] unable to calculate a request signature. error=%s";

    public static String calSignature(String data, String key) throws AclException {
        return calSignature(data, key, DEFAULT_ALGORITHM, DEFAULT_CHARSET);
    }

    public static String calSignature(String data, String key, SigningAlgorithm algorithm, Charset charset) throws AclException {
        return signAndBase64Encode(data, key, algorithm, charset);
    }

    private static String signAndBase64Encode(String data, String key, SigningAlgorithm algorithm, Charset charset) throws AclException {
        try {
            return new String(Base64.encodeBase64(sign(data.getBytes(charset), key.getBytes(charset), algorithm)), DEFAULT_CHARSET);
        } catch (Exception e) {
            String message = String.format(CAL_SIGNATURE_FAILED_MSG, Integer.valueOf((int) CAL_SIGNATURE_FAILED), e.getMessage());
            log.error(message, (Throwable) e);
            throw new AclException("CAL_SIGNATURE_FAILED", CAL_SIGNATURE_FAILED, message, e);
        }
    }

    private static byte[] sign(byte[] data, byte[] key, SigningAlgorithm algorithm) throws AclException {
        try {
            Mac mac = Mac.getInstance(algorithm.toString());
            mac.init(new SecretKeySpec(key, algorithm.toString()));
            return mac.doFinal(data);
        } catch (Exception e) {
            String message = String.format(CAL_SIGNATURE_FAILED_MSG, Integer.valueOf((int) CAL_SIGNATURE_FAILED), e.getMessage());
            log.error(message, (Throwable) e);
            throw new AclException("CAL_SIGNATURE_FAILED", CAL_SIGNATURE_FAILED, message, e);
        }
    }

    public static String calSignature(byte[] data, String key) throws AclException {
        return calSignature(data, key, DEFAULT_ALGORITHM, DEFAULT_CHARSET);
    }

    public static String calSignature(byte[] data, String key, SigningAlgorithm algorithm, Charset charset) throws AclException {
        return signAndBase64Encode(data, key, algorithm, charset);
    }

    private static String signAndBase64Encode(byte[] data, String key, SigningAlgorithm algorithm, Charset charset) throws AclException {
        try {
            return new String(Base64.encodeBase64(sign(data, key.getBytes(charset), algorithm)), DEFAULT_CHARSET);
        } catch (Exception e) {
            String message = String.format(CAL_SIGNATURE_FAILED_MSG, Integer.valueOf((int) CAL_SIGNATURE_FAILED), e.getMessage());
            log.error(message, (Throwable) e);
            throw new AclException("CAL_SIGNATURE_FAILED", CAL_SIGNATURE_FAILED, message, e);
        }
    }
}
