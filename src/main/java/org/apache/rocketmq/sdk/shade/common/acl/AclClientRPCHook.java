package org.apache.rocketmq.sdk.shade.common.acl;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.RPCHook;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;

import java.lang.reflect.Field;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class AclClientRPCHook implements RPCHook {
    private final SessionCredentials sessionCredentials;
    protected ConcurrentHashMap<Class<? extends CommandCustomHeader>, Field[]> fieldCache = new ConcurrentHashMap<>();

    public AclClientRPCHook(SessionCredentials sessionCredentials) {
        this.sessionCredentials = sessionCredentials;
    }

    @Override
    public void doBeforeRequest(String remoteAddr, RemotingCommand request) {
        request.addExtField(SessionCredentials.SIGNATURE, AclUtils.calSignature(AclUtils.combineRequestContent(request, parseRequestContent(request, this.sessionCredentials.getAccessKey(), this.sessionCredentials.getSecurityToken())), this.sessionCredentials.getSecretKey()));
        request.addExtField("AccessKey", this.sessionCredentials.getAccessKey());
        if (this.sessionCredentials.getSecurityToken() != null) {
            request.addExtField("SecurityToken", this.sessionCredentials.getSecurityToken());
        }
        request.setNamespaceId(this.sessionCredentials.getNamespaceId());
    }

    @Override
    public void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response) {
    }

    protected SortedMap<String, String> parseRequestContent(RemotingCommand request, String ak, String securityToken) {
        CommandCustomHeader header = request.readCustomHeader();
        SortedMap<String, String> map = new TreeMap<>();
        map.put("AccessKey", ak);
        if (securityToken != null) {
            map.put("SecurityToken", securityToken);
        }
        if (null != header) {
            try {
                Field[] fields = this.fieldCache.get(header.getClass());
                if (null == fields) {
                    fields = header.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                    }
                    Field[] tmp = (Field[]) this.fieldCache.putIfAbsent(header.getClass(), fields);
                    if (null != tmp) {
                        fields = tmp;
                    }
                }
                for (Field field2 : fields) {
                    Object value = field2.get(header);
                    if (null != value && !field2.isSynthetic()) {
                        map.put(field2.getName(), value.toString());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("incompatible exception.", e);
            }
        }
        if (null != request.getExtFields()) {
            map.putAll(request.getExtFields());
        }
        return map;
    }

    public SessionCredentials getSessionCredentials() {
        return this.sessionCredentials;
    }
}
