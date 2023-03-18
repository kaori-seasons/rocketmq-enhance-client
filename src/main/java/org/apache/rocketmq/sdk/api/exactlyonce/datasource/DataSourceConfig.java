package org.apache.rocketmq.sdk.api.exactlyonce.datasource;

import org.apache.commons.lang3.StringUtils;

public class DataSourceConfig implements Comparable {
    private String url;
    private String user;
    private String passwd;
    private String driver;
    private String productName;

    public DataSourceConfig() {
        this.url = "";
        this.user = "";
        this.passwd = "";
        this.driver = "com.mysql.jdbc.Driver";
        this.productName = "";
    }

    public DataSourceConfig(String url, String user, String passwd, String driver) {
        this(url, user, passwd, driver, null);
    }

    public DataSourceConfig(String url, String user, String passwd, String driver, String productName) {
        this.url = "";
        this.user = "";
        this.passwd = "";
        this.driver = "com.mysql.jdbc.Driver";
        this.productName = "";
        this.url = url;
        this.user = user;
        this.passwd = passwd;
        this.driver = driver;
        this.productName = productName;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPasswd() {
        return this.passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getDriver() {
        return this.driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getProductName() {
        return this.productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @Override
    public String toString() {
        return "DataSourceConfig{url='" + this.url + "', user='" + this.user + "', driver='" + this.driver + "'}";
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return -1;
        }
        DataSourceConfig configTmp = (DataSourceConfig) o;
        if (!configTmp.user.equals(this.user) || !configTmp.passwd.equals(this.passwd) || !configTmp.url.equals(this.url) || !configTmp.driver.equals(this.driver)) {
            return StringUtils.compare(this.url, configTmp.url);
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataSourceConfig)) {
            return false;
        }
        DataSourceConfig config = (DataSourceConfig) o;
        if (this.url != null) {
            if (!this.url.equals(config.url)) {
                return false;
            }
        } else if (config.url != null) {
            return false;
        }
        if (this.user != null) {
            if (!this.user.equals(config.user)) {
                return false;
            }
        } else if (config.user != null) {
            return false;
        }
        if (this.passwd != null) {
            if (!this.passwd.equals(config.passwd)) {
                return false;
            }
        } else if (config.passwd != null) {
            return false;
        }
        return this.driver != null ? this.driver.equals(config.driver) : config.driver == null;
    }

    @Override
    public int hashCode() {
        return (31 * ((31 * ((31 * (this.url != null ? this.url.hashCode() : 0)) + (this.user != null ? this.user.hashCode() : 0))) + (this.passwd != null ? this.passwd.hashCode() : 0))) + (this.driver != null ? this.driver.hashCode() : 0);
    }
}
