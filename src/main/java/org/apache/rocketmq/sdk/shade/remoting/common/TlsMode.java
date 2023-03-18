package org.apache.rocketmq.sdk.shade.remoting.common;

public enum TlsMode {
    DISABLED("disabled"),
    PERMISSIVE("permissive"),
    ENFORCING("enforcing");
    
    private String name;

    TlsMode(String name) {
        this.name = name;
    }

    public static TlsMode parse(String mode) {
        TlsMode[] values = values();
        for (TlsMode tlsMode : values) {
            if (tlsMode.name.equals(mode)) {
                return tlsMode;
            }
        }
        return PERMISSIVE;
    }

    public String getName() {
        return this.name;
    }
}
