# 分布式缓存框架mini版本

### 快速接入：

一，导入依赖：
```xml
    <!-- 分布式缓存框架 -->
    <dependency>
        <groupId>com.github.chenhaiyangs</groupId>
        <artifactId>ruubypay-framework-cachex</artifactId>
        <version>1.0.0</version>
    </dependency>
```    

二，导入一个xml配置，修改各个值为自己环境的配置值
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--suppress ALL -->
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd">

    <!--redis配置 -->
    <!-- Jedis 连接池配置 -->
    <bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">
        <property name="maxTotal" value="2000" />
        <property name="maxIdle" value="100" />
        <property name="minIdle" value="50" />
        <property name="maxWaitMillis" value="2000" />
        <property name="testOnBorrow" value="true" />
        <property name="testOnReturn" value="false" />
        <property name="testWhileIdle" value="true" />
    </bean>
    <!-- shardedJedisPool 连接池 -->
    <bean id="shardedJedisPool" class="redis.clients.jedis.ShardedJedisPool">
        <constructor-arg ref="jedisPoolConfig" />
        <constructor-arg>
            <list>
                <bean class="redis.clients.jedis.JedisShardInfo">
                    <constructor-arg value="127.0.0.1" />
                    <constructor-arg type="int" value="6379" />
                    <constructor-arg value="01" />
                </bean>
            </list>
        </constructor-arg>
    </bean>

    <!--
        序列化方式 内置支持jdk的序列化方式和protobuf的序列化方式
        如果两种序列化都不满足业务，请自行实现com.ruubypay.cachex.ISerializer 接口进行扩展。
    -->
    <bean id="valueSearlizer" class="com.ruubypay.cachex.seriallizer.JdkSerializer"/>

    <!--
        缓存实现类
        需要将连接池 注入
        需要将序列化方式 注入
     -->
    <bean id="cacheManager" class="com.ruubypay.cachex.cache.ShardedJedisCacheManager">
        <constructor-arg ref="shardedJedisPool"/>
        <constructor-arg ref="valueSearlizer"/>
    </bean>

    <!--
        缓存配置类
        namespace 命名空间，每个key都会有一个前缀 ${namespace}-
        batchLockTime:分布式锁的锁时间。
        本框架是数据一致性优先于性能的框架。如果有改动配置：某个key类型的缓存功能由关闭->开启。某个key类型的缓存超时时间发生变更
        框架会在多个服务节点选取一个leader 去批量删除该类型的key。在此期间，该key类型的数据查询全部都走数据库（会影响性能，大并发下最好不要修改线上缓存的配置。）
        configs 缓存key的配置，具体是一个map<String,String> 可以将对应的热配置中心配置group对象注入。
        如果使用了配置中心configx
        <config:group id="cachekeyConfig" node="cache-limit" />
        假设注解配置为：
        @CacheEnabled(key ="'getPushList:'+#args[0].userID",configFromOuter = true)
        在需要在配置中心配置key 为 'getPushList:',value为 true,1000  ......true代表使用缓存，false禁用 。1000表示超时时间，单位为秒，0代表永远不超时
        
        throwExceptionWhenDeleteCacheFail:如果是<aop:before...... 下执行的删除缓存逻辑。也就是先删除缓存，后更新数据库。该配置设置是否在删除缓存失败时抛出异常。
        及时抛出异常，可以避免脏数据的产生。
    -->
    <bean id="cacheConfig" class="com.ruubypay.cachex.CacheConfig">
        <property name="namespace" value="pushcenter"/>
        <property name="batchLockTime" value="120000"/>
        <property name="configs" ref="cachekeyConfig"/>
        <property name="throwExceptionWhenDeleteCacheFail" value="true"/>
    </bean>

    <!--
        缓存管理器，用于实现缓存的所有逻辑
        注入缓存配置和缓存功能的具体实现
     -->
    <bean id="cacheHandler" class="com.ruubypay.cachex.CacheHandler">
        <constructor-arg ref="cacheConfig"/>
        <constructor-arg ref="cacheManager"/>
    </bean>

    <!-- aop拦截切面 -->
    <bean id="cacheInterceptor" class="com.ruubypay.cachex.aop.AspectjAopInterceptor">
        <constructor-arg ref="cacheHandler"/>
    </bean>

    <!-- aspectj织入 配置-->
    <aop:aspectj-autoproxy proxy-target-class="true"/>
    <aop:config proxy-target-class="true">
        <!-- 处理 @RateLimit AOP-->
        <aop:aspect ref="cacheInterceptor" order="99">
            <aop:pointcut id="cachePointCut" expression="execution(* com.ruubypay.business.impl.*.*(..)) &amp;&amp;@annotation(cacheEnabled)"/>
            <aop:around pointcut-ref="cachePointCut" method="proceed"/>
        </aop:aspect>
        <aop:aspect ref="cacheInterceptor">
            <aop:pointcut id="cachedeletePointCut" expression="execution(* com.ruubypay.business.impl.*.*(..)) &amp;&amp;@annotation(cacheDelete)"/>
            <!-- 缓存删除功能包含在增删改之前 和 在增删改之后两种，使用者可以在性能和一致性两个方向选择权衡配置其一或者两个都配置（更能保证一致性） -->
            <!-- 在增删改操作之后删除缓存 -->
            <aop:after-returning pointcut-ref="cachedeletePointCut" method="deleteCacheAfter" returning="returnValue"/>
            <!-- 在增删改操作之前删除缓存-->
            <aop:before pointcut-ref="cachedeletePointCut" method="deleteCacheBefore"/>
        </aop:aspect>
    </aop:config>
</beans>
```
三，在代码上添加如下注解
```java
    @CacheEnabled(key ="'getPushList:'+#args[0].userId",configFromOuter = true)
    public Result getUserById(User user){
        //业务代码
        ......
    }
    
    @CacheDelete({@CacheDeleteKey(key ="'getPushList:'+#args[0].userId" )})
    public Result delete(User user){
        //业务代码
        ......
    }
```
四，配置中心添加缓存组
    
    配置key:'getPushList:',value则是每一个key的配置。true,1000 布尔值代表是否使用缓存。1000代表缓存过期时间，单位秒，0表示永不过期
    
### CacheEnabled注解属性详解

key

    一个缓存表达式。支持spel表达式
    例如，'getPushList:'+#args[0].userId 会自动解析成你想要的key,假如userId为11，则转换成的key为${namespace}-getPushList:11。
    #args[0].userId代表解析第一个参数的userId字段。
expire

    超时时间，单位秒。0则为不超时。默认0
useCache

    是否启用缓存。默认是启用的。当设置为false时，表示不使用缓存。
configFromOuter
    
    配置是否来源于外部，默认是false。如果设置为true，则需要注入每个key的配置。
    假如设置为true。需要在框架cacheConfig注入一个map赋值给属性configx
    假设注解上配置的key为'getPushList:'+#args[0].userId。map中的配置需要为 key:'getPushList:',value为true,1000 这样的配置，第一个配置是是否启用缓存，第二个配置是缓存超时时间。
    
### CacheDelete注解属性详解

value 
    
    是一个参数类型为@CacheDeleteKey的数组。
    CacheDeleteKey只有一个属性为key,是要删除的key的表达式。
    为什么cacheDelete注解支持删除多个key呢？
    用户缓存，有根据id查询的缓存，有根据name查询的缓存......当发生用户数据更新时，这些key都是需要被清空的。
    
    建议缓存只缓存一份。根据id映射。如果有根据name查询的情况，先缓存name和id的关系，再根据id去查询。
    这样能节省存储资源也能避免存储数据太多导致的数据一致性问题。
    根据id查询
    id->cahche
    根据name查询：
    name->cache->id->cache
    
### 实现机制
    
在查询函数上添加注解。如果缓存存在，直接使用缓存返回。如果缓存不存在，放行，并将查询结果放入缓存。<br/>
当检测到配置中心的配置发生改变。超时时间更新，缓存禁用->缓存启用。则会选举一个leader去删除以前的老缓存。<br/>
在删除缓存期间，所有的读请求都走数据库。（维持了数据一致性，但对数据库造成了压力）<br/>


在写函数上添加注解。会在写操作的前和后（根据你的切面配置）删除缓存。<br/>

### 缓存一致性实现

在写接口上，先删缓存后操作数据库，还是先操作数据库后删缓存。引发了很多的人讨论。<br/>
假如方案是先删除缓存，后操作数据库：<br/>
删除缓存失败，结束请求。保护了数据一致性。本框架提供了throwExceptionWhenDeleteCacheFail配置，如果是先删除缓存，是否允许删除失败抛出异常！从而及早结束请求。<br/>
删除缓存成功，操作数据库失败，也最多会导致一次cache miss。问题不大。<br/>

假如先操作数据库，后删除缓存：<br/>
操作数据库成功，删除缓存失败，则导致脏读。<br/>

但是，先删除缓存，再操作数据库，也不能完全保证数据一致性。假如缓存删除期间。另外的线程正好也读取了脏数据放入了缓存。则又造成了脏读。<br/>

最保底的做法是，数据库操作前先删除缓存，数据库操作后休眠一秒后再删除缓存。但是休眠一秒后再删除缓存，和业务代码同步，影响接口性能。这部分可以异步化。<br/>

网上存在的删除缓存方案有使用消息队列的。把删除缓存失败的任务放入消息队列重复消费，直到删除成功。<br/>

本框架没有采用以上方案，一来过于复杂。二来，不易维护。<br/>

本框架的删缓存的时候会生成一个1秒的缓存锁。假设缓存key为getPushList:11，在1秒内，所有的针对key为getPushList:11的读请求都走数据库，不写缓存。一秒后，写请求已经完成。所有的getPushList:11的读请求就可以继续使用缓存了。<br/>


### 扩展性

分别有脚本解析器扩展，序列化方式扩展和缓存实现扩展。<br>

序列化扩展请自行实现com.ruubypay.cachex.ISerializer接口<br/>
项目内置了<br/>
com.ruubypay.cachex.seriallizer.ProtobufSerializer 基于protobuf的序列化方式。<br/>
com.ruubypay.cachex.seriallizer.JdkSerializer 基于jdk的序列化方式。<br/>

脚本解析器项目内置了Spring的spel解析器。<br>
如果想扩展，请自行实现<br>
com.ruubypay.cachex.AbstractScriptParser <br>

缓存实现扩展请自行实现 com.ruubypay.cachex.ICacheManager。<br>
项目内置的实现类为一个基于redis的缓存实现 com.ruubypay.cachex.cache.ShardedJedisCacheManager。<br/>








   