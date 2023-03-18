package org.apache.rocketmq.sdk.shade.common.utils;

import com.alibaba.druid.wall.violation.ErrorCode;
import org.apache.rocketmq.sdk.shade.common.MQVersion;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import org.springframework.beans.factory.BeanFactory;

public class HttpTinyClient {
    public static HttpResult httpGet(String url, List<String> headers, List<String> paramValues, String encoding, long readTimeoutMs) throws IOException {
        String resp;
        String encodedContent = encodingParams(paramValues, encoding);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url + (null == encodedContent ? "" : "?" + encodedContent)).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout((int) readTimeoutMs);
            conn.setReadTimeout((int) readTimeoutMs);
            setHeaders(conn, headers, encoding);
            conn.connect();
            int respCode = conn.getResponseCode();
            if (200 == respCode) {
                resp = IOTinyUtils.toString(conn.getInputStream(), encoding);
            } else {
                resp = IOTinyUtils.toString(conn.getErrorStream(), encoding);
            }
            HttpResult httpResult = new HttpResult(respCode, resp);
            if (conn != null) {
                conn.disconnect();
            }
            return httpResult;
        } catch (Throwable th) {
            if (conn != null) {
                conn.disconnect();
            }
            throw th;
        }
    }

    private static String encodingParams(List<String> paramValues, String encoding) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if (null == paramValues) {
            return null;
        }
        Iterator<String> iter = paramValues.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next()).append("=");
            sb.append(URLEncoder.encode(iter.next(), encoding));
            if (iter.hasNext()) {
                sb.append(BeanFactory.FACTORY_BEAN_PREFIX);
            }
        }
        return sb.toString();
    }

    private static void setHeaders(HttpURLConnection conn, List<String> headers, String encoding) {
        if (null != headers) {
            Iterator<String> iter = headers.iterator();
            while (iter.hasNext()) {
                conn.addRequestProperty(iter.next(), iter.next());
            }
        }
        conn.addRequestProperty("Client-Version", MQVersion.getVersionDesc(MQVersion.CURRENT_VERSION));
        conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + encoding);
        conn.addRequestProperty("Metaq-Client-RequestTS", String.valueOf(System.currentTimeMillis()));
    }

    public static HttpResult httpPost(String url, List<String> headers, List<String> paramValues, String encoding, long readTimeoutMs) throws IOException {
        String resp;
        String encodedContent = encodingParams(paramValues, encoding);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(ErrorCode.INTO_OUTFILE);
            conn.setReadTimeout((int) readTimeoutMs);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            setHeaders(conn, headers, encoding);
            conn.getOutputStream().write(encodedContent.getBytes("UTF-8"));
            int respCode = conn.getResponseCode();
            if (200 == respCode) {
                resp = IOTinyUtils.toString(conn.getInputStream(), encoding);
            } else {
                resp = IOTinyUtils.toString(conn.getErrorStream(), encoding);
            }
            HttpResult httpResult = new HttpResult(respCode, resp);
            if (null != conn) {
                conn.disconnect();
            }
            return httpResult;
        } catch (Throwable th) {
            if (null != conn) {
                conn.disconnect();
            }
            throw th;
        }
    }

    public static class HttpResult {
        public final int code;
        public final String content;

        public HttpResult(int code, String content) {
            this.code = code;
            this.content = content;
        }
    }
}
