# rocketmq-enhance-client
基于rocketmq-client与rocketmq-ons实现exactly-once语义

#### 背景信息
本文档参考阿里云Rocketmq的Exactly-once页面,覆盖Exactly-once功能
消息队列 RocketMQ 的 Exactly-Once 投递语义适用于接收消息 > 处理消息 > 结果持久化到数据库的流程，能够保证您的每一条消息消费的最终处理结果写入到您的数据库有且仅有一次，保证消息消费的幂等。
更多 Exactly-Once 投递语义的概念和典型使用场景，请参见 Exactly-Once 投递语义。

#### 操作步骤

若要使用该语义，请按照以下步骤进行操作：

在应用中添加 SDK 包依赖和 Spring 3.0 以上版本的依赖。详情请见步骤一：添加依赖。
在用于存储消息消费结果的数据库中创建 ```transaction_record``` 表。详情请见步骤二：创建消费事务表。
注意：存储消息消费结果的数据库系统必须支持本地事务。
在消息生产端使用 ```PropertyKeyConst.EXACTLYONCE_DELIVERY``` 属性设置打开 ```Exactly-Once``` 投递语义。详情请见步骤三：生产端开启 Exactly-Once 投递语义。
在消息消费端创建 ExactlyOnceConsumer，并开启 Exactly-Once 的消费模式。详情请见步骤四：消费端开启 Exactly-Once 投递语义。

>步骤一：添加依赖
>
消息队列 RocketMQ 的 ExactlyOnceConsumer 在客户端 SDK ons-client-ext-1.8.0.Final 中发布，若要使用 Exactly-Once 投递语义，需在应用中依赖该 SDK。
另外，ExactlyOnceConsumer 基于 Spring 实现了通过注解 @MQTransaction 开启 Exactly-Once 消费的方式，项目中已经内置Spring3.0的依赖

> 步骤一：创建消费事务表

若要使用消息队列 RocketMQ 的 Exactly-Once 投递语义，您需要在业务处理结果持久化的数据库中创建一张 transaction_record 表，保证此表与存储业务处理结果的表在同一个数据库内，且该数据库支持本地事务。
目前，消息队列 RocketMQ 的 Exactly-Once 投递语义支持您的业务访问 MySQL 和 SQLServer 两种类型的数据源。这两种类型的数据源下 transaction_record 表的建表语句如以下所示。
```
MySQL

CREATE TABLE `transaction_record` (
`consumer_group` varchar(128) NOT NULL DEFAULT '',
`message_id` varchar(255) NOT NULL DEFAULT '',
`topic_name` varchar(255) NOT NULL DEFAULT '',
`ctime` bigint(20) NOT NULL,
`queue_id` int(11) NOT NULL,
`offset` bigint(20) NOT NULL,
`broker_name` varchar(255) NOT NULL DEFAULT '',
`id` bigint(20) NOT NULL AUTO_INCREMENT,
PRIMARY KEY (`id`),
UNIQUE KEY `message_id_unique_key` (`message_id`),
KEY `ctime_key` (`ctime`),
KEY `load_key` (`queue_id`,`broker_name`,`topic_name`,`ctime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SQLServer

CREATE TABLE transaction_record
(
  [consumer_group] varchar(128) NOT NULL ,
  [message_id] varchar(255) NOT NULL ,
  [topic_name] varchar(255) NOT NULL ,
  [ctime] bigint NOT NULL ,
  [queue_id] int NOT NULL ,
  [offset] bigint NOT NULL ,
  [broker_name] varchar(50) NOT NULL ,
  [id] bigint IDENTITY(1,1)  PRIMARY KEY
)
CREATE NONCLUSTERED  INDEX load_key ON transaction_record 
(queue_id, broker_name, topic_name, ctime);
CREATE UNIQUE NONCLUSTERED INDEX message_id_uniq_key ON transaction_record 
(message_id);
CREATE NONCLUSTERED INDEX ctime_key ON transaction_record 
(ctime);
```
注意：

对于SQL Server 数据库，建议您通过ALTER DATABASE [USERDB] SET PARTNER SAFETY OFF;打开异步模式，提高数据库读写性能。
SQL Server 数据库同时还可以通过ALTER DATABASE [USERDB] SET DELAYED_DURABILITY=FORCED的方式开启 Delayed Durability 选项，通过开启该选项可以降低对数据库的 IOPS。

> 步骤二：生产端开启 Exactly-Once 投递语义

在生产端，您仅需要在创建 Producer 时，将PropertyKeyConst.EXACTLYONCE_DELIVERY属性设置为 true，即可打开 Exactly-Once 投递语义，示例代码如下所示。
请见test文件夹示例
```
/**
 * TestExactlyOnceProducer 启动
 * 通过 PropertyKeyConst.EXACTLYONCE_DELIVERY 开启 exactly-once 投递语义
 */
public class TestExactlyOnceProducer {
    public static void main(String[] args) {
        Properties producerProperties = new Properties();
        producerProperties.setProperty(PropertyKeyConst.GROUP_ID, "{GROUP_ID}");
        producerProperties.setProperty(PropertyKeyConst.AccessKey, "{accessKey}");
        producerProperties.setProperty(PropertyKeyConst.SecretKey, "{secretKey}");
        producerProperties.setProperty(PropertyKeyConst.NAMESRV_ADDR, "{NAMESRV_ADDR}");
        producerProperties.setProperty(PropertyKeyConst.EXACTLYONCE_DELIVERY, "true");
        Producer producer = ExactlyOnceONSFactory.createProducer(producerProperties);
        producer.start();
        System.out.println("Producer Started");
        for (int i = 0; i < 10; i++) {
            Message message = new Message("{topic}", "{tag}", "mq send transaction message test".getBytes());
            try {
                SendResult sendResult = producer.send(message);
                assert sendResult != null;
                System.out.println(new Date() + " Send mq message success! msgId is: " + sendResult.getMessageId());
            } catch (ONSClientException e) {
                System.out.println("发送失败");
                //出现异常意味着发送失败，为避免消息丢失，建议缓存该消息然后进行重试。
            }
        }
        producer.shutdown();
    }
}
```
>步骤三：消费端开启 Exactly-Once 投递语义

使用消息队列 RocketMQ 的 Exactly-Once 投递语义进行消费时，消费端需要使用 ExactlyOnceONSFactory 调用createExactlyOnceConsumer 接口创建 ExactlyOnceConsumer，然后通过使用 ExactlyOnceConsumer 进行 Exactly-Once 模式的消费。
在使用 ExactlyOnceConsumer 时，需要注意以下两点：

创建 ExactlyOnceConsumer 时，可以通过设置 PropertyKeyConst.EXACTLYONCE_DELIVERY 属性打开或者关闭 Exactly-Once 投递语义。ExactlyOnceConsumer 默认打开 Exactly-Once 投递语义。
使用 ExactlyOnceConsumer 消费时，在消息监听器 MessageListener 的 consume 方法中，您的业务处理逻辑需要使用 MQDataSource 对数据库的进行读写操作。
您可以选择以下任一方式在消费端开启 Exactly-Once 投递语义：
以非 Spring 方式开启 Exactly-Once 投递语义
MessageListener 中以事务方式实现多项数据库操作和消息消费的事务性
MessageListener 中通过 Springboot 注解方式实现开启 Exactly-Once 投递语义
MessageListener 中通过 MyBatis 方式实现 Exactly-Once 投递语义
以非 Spring 方式开启 Exactly-Once 投递语义
示例如下所示。
```
/**
 * ExactlyOnceConsumer 启动
 * 通过 PropertyKeyConst.EXACTLYONCE_DELIVERY 开启 Exactly-Once 投递语义
 */
 public class TestExactlyOnceConsumer {
    private ExactlyOnceConsumer consumer;
    private TxMessageListener listener;
    public TestExactlyOnceConsumer() {
        Properties consumerProperties = new Properties();
        consumerProperties.setProperty(PropertyKeyConst.GROUP_ID, "{GID}");
        consumerProperties.setProperty(PropertyKeyConst.AccessKey, "{accessKey}");
        consumerProperties.setProperty(PropertyKeyConst.SecretKey, "{secretKey}");
        consumerProperties.setProperty(PropertyKeyConst.NAMESRV_ADDR, "{NAMESRV_ADDR}");
        this.consumer = ExactlyOnceONSFactory.createExactlyOnceConsumer(consumerProperties);
        this.consumer.subscribe("{topic}", "", new TestExactlyOnceListener());
        consumer.start();
        System.out.println("Consumer start success.");
    }
}
/**
 * SimpleListener 为使用 ExactlyOnceConsumer 消费的简单示例
 * 实现了一个简单的消息记录到数据库的过程，保证每条消息持久化到数据库有且仅有一次生效
 */
public class SimpleListener implements MessageListener {
    private MQDataSource dataSource;
    public SimpleListener() {
        this.dataSource = new MQDataSource("{url}", "{user}", "{passwd}", "{driver}");
    }
    @Override
    public Action consume(Message message, ConsumeContext context) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            /**
             * 在此处对消费到的消息 message 做业务计算处理，使用 MQDataSource 将处理结果持久化到数据库系统。
             * 此示例演示了消费并记录消息到数据库系统的场景，实际的业务处理按照：接收消息->业务处理->结果持久化的过程,
             * Exactly-Once 投递语义保证消息处理的持久化过程有且仅有一次。
             */
            connection = dataSource.getConnection();
            statement = connection.prepareStatement("INSERT INTO app(msg, ctime) VALUES(?, ?");
            statement.setString(1, new String(message.getBody()));
            statement.setLong(2, System.currentTimeMillis());
            statement.execute();
            System.out.println("consume message success");
            return Action.CommitMessage;
        } catch (Throwable e) {
            System.out.println("consume message fail:" + e.getMessage());
            return Action.ReconsumeLater;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
```
MessageListener 中以事务方式实现多项数据库操作和消息消费的事务性
示例如下所示。
```
/**
 * TestExactlyOnceListener 实现
 * 实现了一个事务中对多个业务表进行更新的场景，保证事务内的操作有且仅有一次生效
 */
public class SimpleTxListener implements MessageListener {
    private MQDataSource dataSource;
    public SimpleTxListener() {
        this.dataSource = new MQDataSource("{url}", "{user}", "{passwd}", "{driver}");
    }
        @Override
    public Action consume(Message message, ConsumeContext context) {
        Connection connection = null;
        Statement statement = null;
        try {
            /**
             *  在此处对消费到的消息 message 做业务计算处理，使用 MQDataSource 将处理结果持久化到数据库系统。
             *  此范例演示了在一个事务内对多个表进行更新的业务场景，Exactly-Once 投递语义保证事务内的操作有且仅有一次。
             *  实际的业务处理按照:接收消息->业务处理->结果持久化的流程来设计
             */
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            String insertSql = String.format("INSERT INTO app(msg, message_id, ctime) VALUES(\"%s\", \"%s\", %d)",
                new String(message.getBody()), message.getMsgID(), System.currentTimeMillis());
            String updateSql = String.format("UPDATE consume_count SET cnt = count + 1 WHERE consumer_group = \"%s\"", "GID_TEST");
            statement = connection.createStatement();
            statement.execute(insertSql);
            statement.execute(updateSql);
            connection.commit();
            System.out.println("consume message :" + message.getMsgID());
            return Action.CommitMessage;
        } catch (Throwable e) {
            try {
                connection.rollback();
            } catch (Exception e1) {
            }
            System.out.println("consume message fail");
            return Action.ReconsumeLater;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) { }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) { }
            }
        }
    }                                                      
}
```
MessageListener 中通过 Springboot 注解方式实现开启 Exactly-Once 投递语义
1.创建 MessageListener，如下所示。

```
/**
 * MessageListener 通过注解方式开启 Exactly-Once
 * 只需在 MessageListener 的 consume 方法加上 @MQTransaction 即可开启
 * 适用于 springboot 微服务中使用
 */
public class TestMessageListener implements MessageListener {
    private final static String INSERTSQLFORMAT = "INSERT INTO app(message_id, ctime) VALUES(\"%s\", %d)";
    private MQDataSource dataSource;
    @Override
    @MQTransaction
    public Action consume(Message message, ConsumeContext context) {
        Connection connection = null;
        Statement statement = null;
        try {
            String insertSql = String.format(INSERTSQLFORMAT, message.getMsgID(), System.currentTimeMillis());
            connection = this.dataSource.getConnection();
            statement = connection.createStatement();
            statement.execute(insertSql);
            return Action.CommitMessage;
        } catch (Throwable e) {
            return Action.ReconsumeLater;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch(Exception e) { }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) { }
            }
        }
    }
    public void setDataSource(MQDataSource dataSource) {
        this.dataSource = dataSource;
    }
}
```
2.在 consumer.xml 中定义消费者 Bean 等信息。

```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean id="mqDataSource" class="com.aliyun.openservices.ons.api.impl.rocketmq.exactlyonce.datasource.MQDataSource" init-method="init" destroy-method="close">
        <property name="url" value="{url}" />
        <property name="username" value="{user}" />
        <property name="password" value="{passwd}" />
        <property name="driverClass" value="{driver}" />
    </bean>
    <bean id="msgListener" class="com.aliyun.openservices.ons.api.impl.rocketmq.exactlyonce.spring.TestMessageListener">
        <property name="dataSource" ref="mqDataSource"> <!--消费者配置信息-->
        </property>
    </bean> <!--Listener 配置-->
    <!-- 多 GID 订阅同一个 Topic，可以创建多个 ConsumerBean-->
    <bean id="consumer" class="com.aliyun.openservices.ons.api.bean.ExactlyOnceConsumerBean" init-method="start" destroy-method="shutdown">
        <property name="properties" > <!--消费者配置信息-->
            <props>
                <prop key="GROUP_ID">{gid}</prop> 
                <prop key="AccessKey">{accessKey}</prop>
                <prop key="SecretKey">{secretKey}</prop>
                <!--将消费者线程数固定为 50 个
                <prop key="ConsumeThreadNums">50</prop>
                -->
            </props>
        </property>
        <property name="subscriptionTable">
            <map>
                <entry value-ref="msgListener">
                    <key>
                        <bean class="com.aliyun.openservices.ons.api.bean.Subscription">
                            <property name="topic" value="{topic}"/>
                            <property name="expression" value="{subExpression}"/><!--expression 即 Tag，可以设置成具体的 Tag，如 taga||tagb||tagc，也可设置成*。 *仅代表订阅所有 Tag，不支持通配-->
                        </bean>
                    </key>
                </entry>
            </map>
        </property>
    </bean>
</beans>
```
3.运行已经与 Spring 集成好的消费者，如下所示。

```
public class ConsumeWithSpring {
    public static void main(String[] args) {
        /**
         * 消费者 Bean 配置在 consumer.xml 中，可通过 ApplicationContext 获取或者直接注入到其他类（比如具体的 Controller）中
         */
        ApplicationContext context = new ClassPathXmlApplicationContext("consumer.xml");
        System.out.println("Consumer Started");
    }
```
MessageListener 中通过 MyBatis 方式实现 Exactly-Once 投递语义
1.设计业务数据库写入操作 DAO。

```
public interface AppDAO {
    Integer insertMessage(String msgId);
}
```
2.在 mapper.xml 文件中编写插入操作 sql 语句。
```
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
    <mapper namespace="com.aliyun.openservices.tcp.example.mybatis.AppDAO">
        <insert id="insertMessage">
            INSERT INTO app (message_id, ctime) VALUES (#{msgId}, now())
        </insert>
    </mapper>
```
3.利用 MQDataSource 实现自定义的 DataSourceFactory。

```
public class MQDataSourceFactoty extends DruidDataSourceFactory implements DataSourceFactory {
    protected Properties properties;
    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
    @Override
    public DataSource getDataSource() {
        try {
            DruidDataSource druidDataSource = (DruidDataSource) createDataSource(this.properties);
            return new MQDataSource(druidDataSource);
        } catch (Exception e) {
            System.out.println("err:" + e.getMessage());
        }
        return null;
    }
}
```

4.在 mybatis-config.xml 中注册 datasource 为 MQDataSourceFactoty 类型。
```
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <!-- 配置数据库连接信息 -->
            <dataSource type="com.aliyun.openservices.tcp.example.mybatis.MQDataSourceFactoty">
                <property name="driverClass" value="com.mysql.jdbc.Driver"/>
                <property name="url" value="{url}"/>
                <property name="username" value="{username}"/>
                <property name="password" value="{password}"/>
                <property name="initialSize" value="10" />
                <property name="maxActive" value="20"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <mapper resource="mapper.xml"/>
    </mappers>
</configuration>
```
5.在监听器中使用 Mytatis 的方式访问数据库，实现 Exactly-Once 的消费方式。

```
public class TestMybatisListener implements MessageListener {
    private static SqlSessionFactory sqlSessionFactory;
    static {
        String resource = "mybatis-config.xml";
        Reader reader = null;
        try {
            reader= Resources.getResourceAsReader(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }
    @Override
    public Action consume(Message message, ConsumeContext context) {
        long begion = System.currentTimeMillis();
          SqlSession sqlSession = null;
        try {
            sqlSession = sqlSessionFactory.openSession();
            AppDAO appDAO = sqlSession.getMapper(AppDAO.class);
            appDAO.insertMessage(message.getMsgID());
            System.out.println("consume : " + message.getMsgID());
            sqlSession.commit();
            return Action.CommitMessage;
        } catch (Exception e) {
            e.printStackTrace();
              sqlSession.rollback();
            return Action.ReconsumeLater;
        } finally {
            sqlSession.close();
        }
    }
}
```
