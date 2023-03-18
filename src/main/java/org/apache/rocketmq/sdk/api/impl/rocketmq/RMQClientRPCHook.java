package org.apache.rocketmq.sdk.api.impl.rocketmq;

import org.apache.rocketmq.sdk.api.impl.MQClientInfo;
import org.apache.rocketmq.sdk.shade.common.acl.AclClientRPCHook;
import org.apache.rocketmq.sdk.shade.common.acl.SessionCredentials;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;

public class RMQClientRPCHook extends AclClientRPCHook {
    public RMQClientRPCHook(SessionCredentials sessionCredentials) {
        super(sessionCredentials);
    }

    @Override
    public void doBeforeRequest(String remoteAddr, RemotingCommand request) {
        super.doBeforeRequest(remoteAddr, request);
        request.setVersion(MQClientInfo.versionCode);
    }

    @Override
    public void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response) {
        super.doAfterResponse(remoteAddr, request, response);
    }
}
