package org.apache.rocketmq.sdk.shade.common;

import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Configuration {
    private final InternalLogger log;
    private List<Object> configObjectList;
    private String storePath;
    private boolean storePathFromConfig;
    private Object storePathObject;
    private Field storePathField;
    private DataVersion dataVersion;
    private ReadWriteLock readWriteLock;
    private Properties allConfigs;

    public Configuration(InternalLogger log) {
        this.configObjectList = new ArrayList(4);
        this.storePathFromConfig = false;
        this.dataVersion = new DataVersion();
        this.readWriteLock = new ReentrantReadWriteLock();
        this.allConfigs = new Properties();
        this.log = log;
    }

    public Configuration(InternalLogger log, Object... configObjects) {
        this.configObjectList = new ArrayList(4);
        this.storePathFromConfig = false;
        this.dataVersion = new DataVersion();
        this.readWriteLock = new ReentrantReadWriteLock();
        this.allConfigs = new Properties();
        this.log = log;
        if (configObjects != null && configObjects.length != 0) {
            Object[] var3 = configObjects;
            int var4 = configObjects.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Object configObject = var3[var5];
                this.registerConfig(configObject);
            }

        }
    }

    public Configuration(InternalLogger log, String storePath, Object... configObjects) {
        this(log, configObjects);
        this.storePath = storePath;
    }

    public Configuration registerConfig(Object configObject) {
        try {
            this.readWriteLock.writeLock().lockInterruptibly();

            try {
                Properties registerProps = MixAll.object2Properties(configObject);
                this.merge(registerProps, this.allConfigs);
                this.configObjectList.add(configObject);
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        } catch (InterruptedException var7) {
            this.log.error("registerConfig lock error");
        }

        return this;
    }

    public Configuration registerConfig(Properties extProperties) {
        if (extProperties == null) {
            return this;
        } else {
            try {
                this.readWriteLock.writeLock().lockInterruptibly();

                try {
                    this.merge(extProperties, this.allConfigs);
                } finally {
                    this.readWriteLock.writeLock().unlock();
                }
            } catch (InterruptedException var6) {
                this.log.error("register lock error. {}" + extProperties);
            }

            return this;
        }
    }

    public void setStorePathFromConfig(Object object, String fieldName) {
        assert object != null;

        try {
            this.readWriteLock.writeLock().lockInterruptibly();

            try {
                this.storePathFromConfig = true;
                this.storePathObject = object;
                this.storePathField = object.getClass().getDeclaredField(fieldName);

                assert this.storePathField != null && !Modifier.isStatic(this.storePathField.getModifiers());

                this.storePathField.setAccessible(true);
            } catch (NoSuchFieldException var8) {
                throw new RuntimeException(var8);
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        } catch (InterruptedException var10) {
            this.log.error("setStorePathFromConfig lock error");
        }

    }

    private String getStorePath() {
        String realStorePath = null;

        try {
            this.readWriteLock.readLock().lockInterruptibly();

            try {
                realStorePath = this.storePath;
                if (this.storePathFromConfig) {
                    try {
                        realStorePath = (String)this.storePathField.get(this.storePathObject);
                    } catch (IllegalAccessException var7) {
                        this.log.error("getStorePath error, ", var7);
                    }
                }
            } finally {
                this.readWriteLock.readLock().unlock();
            }
        } catch (InterruptedException var9) {
            this.log.error("getStorePath lock error");
        }

        return realStorePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    public void update(Properties properties) {
        try {
            this.readWriteLock.writeLock().lockInterruptibly();

            try {
                this.mergeIfExist(properties, this.allConfigs);
                Iterator var2 = this.configObjectList.iterator();

                while(true) {
                    if (!var2.hasNext()) {
                        this.dataVersion.nextVersion();
                        break;
                    }

                    Object configObject = var2.next();
                    MixAll.properties2Object(properties, configObject);
                }
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        } catch (InterruptedException var8) {
            this.log.error("update lock error, {}", properties);
            return;
        }

        this.persist();
    }

    public void persist() {
        try {
            this.readWriteLock.readLock().lockInterruptibly();

            try {
                String allConfigs = this.getAllConfigsInternal();
                if (allConfigs != null) {
                    MixAll.string2File(allConfigs, this.getStorePath());
                }
            } catch (IOException var6) {
                this.log.error("persist string2File error, ", var6);
            } finally {
                this.readWriteLock.readLock().unlock();
            }
        } catch (InterruptedException var8) {
            this.log.error("persist lock error");
        }

    }

    public String getAllConfigsFormatString() {
        try {
            this.readWriteLock.readLock().lockInterruptibly();

            String var1;
            try {
                var1 = this.getAllConfigsInternal();
            } finally {
                this.readWriteLock.readLock().unlock();
            }

            return var1;
        } catch (InterruptedException var6) {
            this.log.error("getAllConfigsFormatString lock error");
            return null;
        }
    }

    public String getDataVersionJson() {
        return this.dataVersion.toJson();
    }

    public Properties getAllConfigs() {
        try {
            this.readWriteLock.readLock().lockInterruptibly();

            Properties var1;
            try {
                var1 = this.allConfigs;
            } finally {
                this.readWriteLock.readLock().unlock();
            }

            return var1;
        } catch (InterruptedException var6) {
            this.log.error("getAllConfigs lock error");
            return null;
        }
    }

    private String getAllConfigsInternal() {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator var2 = this.configObjectList.iterator();

        while(var2.hasNext()) {
            Object configObject = var2.next();
            Properties properties = MixAll.object2Properties(configObject);
            if (properties != null) {
                this.merge(properties, this.allConfigs);
            } else {
                this.log.warn("getAllConfigsInternal object2Properties is null, {}", configObject.getClass());
            }
        }

        stringBuilder.append(MixAll.properties2String(this.allConfigs));
        return stringBuilder.toString();
    }

    private void merge(Properties from, Properties to) {
        Object key;
        Object fromObj;
        for(Iterator var3 = from.keySet().iterator(); var3.hasNext(); to.put(key, fromObj)) {
            key = var3.next();
            fromObj = from.get(key);
            Object toObj = to.get(key);
            if (toObj != null && !toObj.equals(fromObj)) {
                this.log.info("Replace, key: {}, value: {} -> {}", new Object[]{key, toObj, fromObj});
            }
        }

    }

    private void mergeIfExist(Properties from, Properties to) {
        Iterator var3 = from.keySet().iterator();

        while(var3.hasNext()) {
            Object key = var3.next();
            if (to.containsKey(key)) {
                Object fromObj = from.get(key);
                Object toObj = to.get(key);
                if (toObj != null && !toObj.equals(fromObj)) {
                    this.log.info("Replace, key: {}, value: {} -> {}", new Object[]{key, toObj, fromObj});
                }

                to.put(key, fromObj);
            }
        }

    }
}
