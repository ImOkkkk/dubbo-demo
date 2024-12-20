## 基础

### 总体架构

Dubbo的主要节点角色

- **Container**：服务运行容器，为服务的稳定运行提供运行环境。Tomcat容器、Jetty容器。

- **Provider**：提供方，暴露接口提供服务。

  **Dubbo3.x推崇应用级注册新特性**，不改变任何Dubbo配置的情况下，兼容一个应用从2.x平滑升级到3.x版本，这个新特性为了将来能**支持十万甚至百万的集群实例地址发现**，并且可以**与不同的微服务体系实现地址发现互联互通**。

  ```yaml
  dubbo:
    application:
      register-mode: all		//interface：只接口级注册 instance：只应用级注册 all：接口级注册、应用级注册同时存在(默认)
  ```

- **Consumer**：消费方，调用已暴露的接口。

  **超时时间优先级**：消费者Method > 提供者Method > 消费者Reference > 提供者Service > 消费者全局配置

  ```java
  @DubboReference(timeout = 3000, retries = 3, cluster = ClusterRules.FAIL_BACK, loadbalance = LoadbalanceRules.ROUND_ROBIN)
      private DemoService demoService;
  ```

  **容错策略**

  ![image-20240624142325857](https://pic-go-image.oss-cn-beijing.aliyuncs.com/pic/image-20240624142325857.png)

  | 容错策略   | 说明                                                         |
  | ---------- | ------------------------------------------------------------ |
  | failover   | 故障转移策略，A调用B1出现异常失败时，A会自动尝试调用B2或其他机器。 |
  | failfast   | 快速失败策略，A调用发生异常后，直接抛出异常中断调用流程。    |
  | failsafe   | 失败安全策略，A调用即使发生了异常，对于代码而言返回的是一个null的结果。 |
  | failback   | 失败自动恢复策略，A调用即使发生了异常，对于代码而言返回的是一个null的结果，但是Dubbo会记录失败请求，然后定时重发。 |
  | forking    | 并行策略，A同时向B1、B2发起调用(这里需要结合forks参数来配合同时向多少个提供方节点发起调用)，任何一个调用成功的话，就返回结果。 |
  | broadcast  | 广播策略，A同时向B集群的所有提供者发起调用，全部调用完成后若有一个异常，则抛出异常；都无异常的话则将最后一次调用的结果返回。 |
  | available  | 可用策略，不做负载均衡，遍历所有提供方节点，遇到不可用的提供方节点不发起调用，找到第一个可用的节点发起调用。 |
  | mergeable  | 合并策略，自动将多个节点请求的返回结果进行合并。             |
  | mock       | 伪造策略，调用失败发生非业务异常，可以伪造RPC调用的结果      |
  | zone-aware | 区域策略，即优先调用同区域的服务，同区域没有则根据一定的权重算法选择其他区域的服务。 |

  ```yaml
  dubbo:
    consumer:
      check: false		//为整个消费方添加启动不检查提供方服务是否正常
    application:
      service-discovery:
        migration: APPLICATION_FIRST		//FORCE_INTERFACE：只订阅接口级信息 FORCE_APPLICATION：只订阅应用信息 APPLICATION_FIRST：注册中心有应用级注册信息则订阅应用级信息，否则订阅接口级信息，起到了智能决策来兼容过渡方案。
  ```

- **Registry**：注册中心，管理注册的服务与接口。

  消费者自动感知新的提供方节点。

- **Monitor**：监控中心，统计服务调用次数和调用时间。

  调用成功数、失败数、耗时时间上送给监控中心，在监控中心设置不同的告警策略。

![image-20240509103927448](https://pic-go-image.oss-cn-beijing.aliyuncs.com/pic/image-20240509103927448.png)

## 异步化实践

```java
@DubboService
public class AsyncOrderFacadeImpl implements AsyncOrderFacade {
    private static final Executor threadPool = Executors.newFixedThreadPool(8);

    @Override
    public OrderInfo queryOrderById(String id) {
        // 模拟执行一段耗时的业务逻辑
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        OrderInfo resultInfo =
                new OrderInfo(
                        "GeekDubbo",
                        Thread.currentThread().getName() + "#服务方异步方式之RpcContext.startAsync#" + id,
                        new BigDecimal(129));
        return resultInfo;
    }

    //使用CompletableFuture实现异步
    @Override
    public CompletableFuture<OrderInfo> queryOrderByIdFuture(String id) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Thread.sleep(5000);
                        OrderInfo resultInfo =
                                new OrderInfo(
                                        "GeekDubbo",
                                        Thread.currentThread().getName()
                                                + "#服务方异步方式之RpcContext.startAsync#"
                                                + id,
                                        new BigDecimal(129));
                        return resultInfo;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
    }

    //使用AsyncContext实现异步
    @Override
    public OrderInfo queryOrderByIdAsyncContext(String id) {
        AsyncContext asyncContext = RpcContext.startAsync();
        threadPool.execute(
                () -> {
                    asyncContext.signalContextSwitch();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    asyncContext.write(
                            new OrderInfo(
                                    "GeekDubbo",
                                    Thread.currentThread().getName()
                                            + "#服务方异步方式之RpcContext.startAsync#"
                                            + id,
                                    new BigDecimal(129)));
                });
        return null;
    }
}
```

**如果 queryOrderById 这个方法的调用量上来了，很容易导致 Dubbo 线程池耗尽。**(Dubbo线程池默认线程数200)

- **Provider异步**

  - 使用CompletableFuture实现异步
  - 使用AsyncContext实现异步

- **Consumer异步**

  ```java
  @RestController
  @RequestMapping("/demo")
  public class DemoController {
      @DubboReference(timeout = 10000)
      private AsyncOrderFacade asyncOrderFacade;
  
          // Consumer异步调用
          CompletableFuture<OrderInfo> future =
                  CompletableFuture.supplyAsync(
                          () -> {
                              return asyncOrderFacade.queryOrderById(id);
                          });
          future.whenComplete(
                  (v, t) -> {
                      if (t != null) {
                          t.printStackTrace();
                      } else {
                          System.out.println("Response: " + JSON.toJSONString(v));
                      }
                  });
          return JSON.toJSONString(future.get());
      }
  }
  ```

### Dubbo异步实现原理

1. 开启异步模式：

   org.apache.dubbo.rpc.RpcContext#startAsync，通过CAS的方式创建了一个CompletableFuture对象，存储在当前的上下文RpcContextAttachment对象中

2. 在异步线程中同步父线程的上下文信息：

   org.apache.dubbo.rpc.AsyncContextImpl#signalContextSwitch，把asyncContext对象传入到子线程，然后将asyncContext中的上下文信息充分拷贝到子线程中。

3. 将异步结果写入到异步线程的上下文信息中：

   org.apache.dubbo.rpc.AsyncContextImpl#write，异步化结果存入了CompletableFuture对象中，拦截处只需要调用CompletableFuture#get(long timeout, TimeUnit unit) 就能拿到异步化结果了。

### 异步运用场景

1. 异步化耗时的操作并没有在queryOrderById方法所在线程中继续占用资源，而是在新开辟的线程池中占用资源。**对于一些IO耗时的操作，比较影响客户体验和使用性能的一些地方**。
2. queryOrderById开启异步操作后就立即返回了，异步线程的完成与否，不太影响 queryOrderById 的返回操作。**若某段业务逻辑开启异步执行后不太影响主线程的原有业务逻辑**。
3. 在 queryOrderById中，只开启了一个异步化的操作，站在时序的角度上看，queryOrderById 方法返回了，但是异步化的逻辑还在慢慢执行着，对时序的先后顺序没有严格要求。**时序上没有严格要求的业务逻辑**。

### CompletableFuture

- 任务执行完成后再并发执行其他任务：thenXxxAsync
- 合并两个线程任务的结果，并做进一步累加和处理：thenCombine
- 两个线程任务并发执行，谁先执行完成就以谁为准：applyToEither

## 隐式传递

RpcContext

- ServiceContext：在Dubbo内部使用，用于传递调用链路上的参数信息，如invoker对象等
- **ClientAttachment**：在Client端使用，往ClientAttachment中写入的参数将被传递到Server端
- **ServerAttachment**：在Server端使用，从ServerAttachment中读取的参数是从Client中传递过来的
- ServerContext：在Client端和Server端使用，用于从Server端回传Client端使用，Server端写入到ServerContext的参数在调用结束后可以在Client端ServerContext获取到

1. Dubbo系统调用时，想传递一些通用参数，可通过Dubbo提供的扩展如Filter等实现统一的参数传递
2. Dubbo系统调用时，想传递接口定义之外的参数，可在调用接口前使用setAttachment传递参数

### 定义过滤器的步骤

1. 实现org.apache.dubbo.rpc.Filter接口
2. @Activate 注解标识是提供方维度的过滤器，还是消费方维度的 过滤器
3. 重写invoke方法，提供方过滤器从invocation取出traceId并设置到ClientAttachment、MDC中，消费方过滤器从ClientAttachment取出traceId并设置到invoation中；
4. 最后将自定义的类路径添加到META-INF/dubbo/org.apache.dubbo.rpc.Filter 文件中，并取个别名。

### 应用场景

1. 传递请求流水号，分布式应用中通过流水号全局检索日志。
2. 传递用户信息，不同系统在处理业务逻辑时获取用户层面的信息。
3. 传递凭证信息，如Cookie、Token等。

### RpcContext生命周期

- **SERVER_LOCAL**：作用于Provider侧，在org.apache.dubbo.rpc.RpcContext.RestoreContext#restore 中被设置进去，即在线程切换将父线程的信息拷贝至子线程时被调用，然而却又在 Provider 转为 Consumer 角色时被清除数据。
- **CLIENT_ATTACHMENT**：用于将附属信息作为 Consumer 传递到下一跳 Provider，在 Provider 和 Consumer 的 Filter 中的 org.apache.dubbo.rpc.BaseFilter.Listener#onResponse、 org.apache.dubbo.rpc.BaseFilter.Listener#onError 方法都会被清除数据。
- **SERVER_ATTACHMENT**：SERVER_ATTACHMENT 作为 Provider 侧用于接收上一跳 Consumer 的发来附属信息，在 Provider 和 Consumer 的 Filter 中的 org.apache.dubbo.rpc.BaseFilter.Listener#onResponse、 org.apache.dubbo.rpc.BaseFilter.Listener#onError 方法都会被清除数据。
- **SERVICE_CONTEXT**：SERVICE_CONTEXT 用于将附属信息作为 Provider 返回给 Consumer，在 Provider 侧的 org.apache.dubbo.rpc.filter.ContextFilter#onResponse、 org.apache.dubbo.rpc.filter.ContextFilter#onError 方法都会被清除数据。

## 泛化调用

采用一种统一的方式来发起对任何服务方法的调用。

### 应用场景

- **透式调用**：发起方只是想调用提供者拿到结果，没有过多的业务逻辑诉求，即使有，
  也是拿到结果后再继续做分发处理。
- **代理服务**：所有的请求都会经过代理服务器，而代理服务器不会感知任何业务逻辑，只是一个通道，接收数据 -> 发起调用 -> 返回结果，调用流程非常简单纯粹。
- **前端网关**：前端网关，有些内网环境的运营页面，对 URL 的格式没有那么严格的讲究，页面的功能都是和后端服务一对一的操作，非常简单直接。

**如何获取下游接口对象**

@DubboReference

![image-20240720094202498](https://pic-go-image.oss-cn-beijing.aliyuncs.com/pic/image-20240720094202498.png)

### 泛化调用三部曲

- 接口类名、接口方法名、接口方法参数类名、业务请求参数。
- 根据接口类名创建 ReferenceConfig 对象，设置 **generic = true 、url = 协议 +IP+PORT** 两个重要属性，调用 referenceConfig.get 拿到 genericService 泛化对象。
- 传入接口方法名、接口方法参数类名、业务请求参数，调用 genericService.$invoke 方法拿到响应对象，并通过 Ognl 表达式语言判断响应成功或失败，然后完成数据最终返回。

## 点点直连：万能修复接口

**ReferenceConfigBase**

```java
//org.apache.dubbo.config.ReferenceConfig#parseUrl
	private void parseUrl(Map<String, String> referenceParameters) {
    		//将配置的url地址按照分号进行切割，得到一个字符串数组
        String[] us = SEMICOLON_SPLIT_PATTERN.split(url);
        if (ArrayUtils.isNotEmpty(us)) {
            for (String u : us) {
              	//主要逻辑
                URL url = URL.valueOf(u);
                if (StringUtils.isEmpty(url.getPath())) {
                    url = url.setPath(interfaceName);
                }
                url = url.setScopeModel(getScopeModel());
                url = url.setServiceModel(consumerModel);
                if (UrlUtils.isRegistry(url)) {
                    urls.add(url.putAttribute(REFER_KEY, referenceParameters));
                } else {
                    URL peerUrl = getScopeModel().getApplicationModel().getBeanFactory().getBean(ClusterUtils.class).mergeUrl(url, referenceParameters);
                    peerUrl = peerUrl.putAttribute(PEER_KEY, true);
                    urls.add(peerUrl);
                }
            }
        }
    }

		//org.apache.dubbo.common.URLStrParser#parseEncodedStr
    public static URL parseEncodedStr(String encodedURLStr) {
        Map<String, String> parameters = null;
        int pathEndIdx = encodedURLStr.toUpperCase().indexOf("%3F");// '?'
        if (pathEndIdx >= 0) {
            parameters = parseEncodedParams(encodedURLStr, pathEndIdx + 3);
        } else {
            pathEndIdx = encodedURLStr.length();
        }

        //decodedBody format: [protocol://][username:password@][host:port]/[path]
        String decodedBody = decodeComponent(encodedURLStr, 0, pathEndIdx, false, DECODE_TEMP_BUF.get());
        return parseURLBody(encodedURLStr, decodedBody, parameters);
    }
```

赋值 url 为**dubbo://[机器IP结点]:[机器IP提供Dubbo服务的端口]**

![CleanShot 2024-07-24 at 22.17.46@2x](https://pic-go-image.oss-cn-beijing.aliyuncs.com/pic/CleanShot%202024-07-24%20at%2022.17.46%402x.png)

### 应用场景

1. 修复产线事件，通过直连 + 泛化 + 动态代码编译执行，可以轻松临时解决产线棘手的问题。
2. 绕过注册中心直接联调测试，有些公司由于测试环境的复杂性，有时候不得不采用简单的直连方式，来快速联调测试验证功能。
3. 检查服务存活状态，如果需要针对多台机器进行存活检查，那就需要循环调用所有服务的存活检查接口。

### 动态编译调用三部曲

- 将 Java 代码利用 Groovy 插件的 groovyClassLoader 加载器编译为 Class 对象。
- 将 Class 信息创建 Bean 定义对象后，移交给 Spring 容器去创建单例 Bean 对象。
- 调用单例 Bean 对象的 run 方法，完成动态代码调用。

## 事件通知

![CleanShot 2024-07-28 at 17.05.43@2x](https://pic-go-image.oss-cn-beijing.aliyuncs.com/pic/CleanShot%202024-07-28%20at%2017.05.43%402x.png)

```java
@DubboService
@Component
public class PayFacadeImpl implements PayFacade {
	// 商品支付功能:一个大方法
	@Override
	public PayResp recvPay(PayReq req){
		// 支付核心业务逻辑处理 
		method1();
		// 埋点已支付的商品信息
		method2();
		// 发送支付成功短信给用户
		method3();
		// 通知物流派件
 		method4();
		// 返回支付结果
		return buildSuccResp();
	}
}
```

**解耦技巧**

1. 功能相关性：将一些功能非常相近的汇聚成一块。
2. 密切相关性：按照与主流程的密切相关性，将一个个小功能分为密切与非密切。
3. 状态变更性：按照是否有明显业务状态的先后变更，将一个个小功能再归类。

![CleanShot 2024-07-28 at 17.06.37@2x](https://pic-go-image.oss-cn-beijing.aliyuncs.com/pic/CleanShot%202024-07-28%20at%2017.06.37%402x.png)

事件驱动，简单理解就是**一个事件会触发另一个或多个事件做出对应的响应行为**。

**反射调用，在接口级别配置回调方法**

![CleanShot 2024-07-28 at 17.46.22@2x](https://pic-go-image.oss-cn-beijing.aliyuncs.com/pic/CleanShot%202024-07-28%20at%2017.46.22%402x.png)

```java
@Activate(group = CommonConstants.CONSUMER)
public class FutureFilter implements ClusterFilter, ClusterFilter.Listener {

    protected static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(FutureFilter.class);

    @Override
    public Result invoke(final Invoker<?> invoker, final Invocation invocation) throws RpcException {
        fireInvokeCallback(invoker, invocation);
        // need to configure if there's return value before the invocation in order to help invoker to judge if it's
        // necessary to return future.
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        if (result.hasException()) {
            fireThrowCallback(invoker, invocation, result.getException());
        } else {
            fireReturnCallback(invoker, invocation, result.getValue());
        }
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        fireThrowCallback(invoker, invocation, t);
    }
}
```

**FutureFilter 过滤器**

- 在 invoker.invoke(invocation) 方法之前，利用 fireInvokeCallback 方法反射调用了接口配置中指定服务中的 onInvoke 方法。
- 然后在 onResponse 响应时，处理了正常返回和异常返回的逻辑，分别调用了接口配置中指定服务中的 onReturn、onThrow 方法。
- 最后在 onError 框架异常后，调用了接口配置中指定服务中的  onThrow 方法

### 常用的事件回调机制

- Spring 框架，使用 publishEvent 方法发布 ApplicationEvent 事件。
- Tomcat 框架，在 Javax 规范的 ServletContainerInitializer.onStartup 方法中继续循环回调ServletContextInitializer 接口的所有实现类。
-  JVM 的关闭钩子事件，当程序正常退出或调用 System.exit 或虚拟机被关闭时，会调用Runtime.addShutdownHook 注册的线程。

### 事件通知的应用

- **职责分离**，可以按照功能相关性剥离开，让各自的逻辑是内聚的、职责分明的。
- **解耦**，把复杂的面向过程风格的一坨代码分离，可以按照功能是技术属性还是业务属性剥离。
- **事件溯源**，针对一些事件的实现逻辑，如果遇到未知异常后还想再继续尝试重新执行的话，可以考虑事件持久化并支持在一定时间内重新回放执行。

### Dubbo事件通知三部曲

1. 创建一个服务类，在该类中添加 onInvoke、onReturn、onThrow 三个方法。
2. 其次，在三个方法中按照源码 FutureFilter 的规则定义好方法入参。
3. 最后，@DubboReference 注解中给需要关注事件的 Dubbo 接口添加配置即可。

**Dubbo 框架的事件通知机制，即使有重试机制的存在，但也只会触发一次。**

## 参数验证

**ValidationFilter**

```java
    //org.apache.dubbo.validation.filter.ValidationFilter#invoke
		@Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
      //Validation 接口的代理类被注入成功，且该调用的方法有 validation 属性
        if (validation != null && !invocation.getMethodName().startsWith("$")
            && ConfigUtils.isNotEmpty(invoker.getUrl().getMethodParameter(invocation.getMethodName(), VALIDATION_KEY))) {
            try {
              // 接着通过 url 中 validation 属性值，并且为该方法创建对应的校验实现类
                Validator validator = validation.getValidator(invoker.getUrl());
              // 若找到校验实现类的话，则真正开启对方法的参数进行校验
                if (validator != null) {
                    validator.validate(invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments());
                }
            } catch (RpcException e) {
                throw e;
            } catch (Throwable t) {
                return AsyncRpcResult.newDefaultAsyncResult(t, invocation);
            }
        }
      // 能来到这里，说明要么没有配置校验过滤器，要么就是校验了但参数都合法 // 既然没有抛异常的话，那么就直接调用下一个过滤器的逻辑
        return invoker.invoke(invocation);
    }

		//org.apache.dubbo.validation.support.AbstractValidation#getValidator
    @Override
    public Validator getValidator(URL url) {
        String key = url.toFullString();
      	//validators 是一个 Map 结构，即底层可以说明每个方法都可以有不同的校验器
        Validator validator = validators.get(key);
        if (validator == null) {
            validators.put(key, createValidator(url));
            validator = validators.get(key);
        }
        return validator;
    }

  //org.apache.dubbo.validation.support.jvalidation.JValidator
	public class JValidator implements Validator {
    // 进入到 Dubbo 框架默认的校验器中，发现真实采用的是 javax 第三方的 validation 插件 
    private final javax.validation.Validator validator;
  }
```

1. 先找到ValidationFilter过滤器的invoke入口
2. 紧接着找到根据validation属性值创建校验器的createValidator方法
3. 然后发现创建了一个JValidator对象
4. 在该对象中发现了关于javax包名的第三方validation插件

### 参数验证的应用

- 单值简单规则验证，各个字段的校验逻辑毫无关联、相互独立
- 提前拦截脏请求，尽可能把一些参数值不合法的情况提前过滤掉
- 通用网关领域，对于不合法的请求尽量拦截掉，避免不必要的请求打到下游，浪费资源

### 两种方式

- 一般方式，设置validation为jvalidation、jvalidationNew两种框架提供的值
- 特殊方式，设置validation为自定义校验器的类路径，并将自定义的类路径添加到META-INF文件夹下面的org.apache.dubbo.validation.Validation 文件中

### 参数校验三部曲

1. 寻找具有拦截机制的接口，且该接口具有读取请求对象数据的能力
2. 寻找一套注解来定义校验的标准规则，并将该注解修饰到请求对象的字段上
3. 寻找一套校验逻辑来根据注解的标准规则来校验字段值，提供通用的校验能力

## 为接口提供缓存

### CacheFilter

```java
//过滤器被触发的调用入口
org.apache.dubbo.cache.filter.CacheFilter#invoke
↓
//根据invoker.getUrl()获取缓存容器
org.apache.dubbo.cache.support.AbstractCacheFactory#getCache
↓
//若缓存容器没有的话，则自动创建一个缓存容器
org.apache.dubbo.cache.support.AbstractCacheFactory#createCache
↓
//最终创建的是一个 LruCache 对象，该对象的内部使用的 LRU2Cache 存储数据
org.apache.dubbo.cache.support.lru.LruCache#LruCache
//存储调用结果的对象
    private final Map<Object, Object> store;
    public LruCache(URL url) {
        final int max = url.getParameter("cache.size", 1000);
        this.store = new LRU2Cache<>(max);
    }
//LRU2Cache 的带参构造方法，在 LruCache 构造方法中，默认传入的大小是 1000
↓
//若继续放数据时，若发现现有数据个数大于 maxCapacity 最大容量的话
//则会考虑抛弃掉最古老的一个，也就是会抛弃最早进入缓存的那个对象
    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > maxCapacity;
    }
↓
//JDK 中的 LinkedHashMap 源码在发生节点插入后
//给了子类一个扩展删除最旧数据的机制
java.util.HashMap#afterNodeInsertion
    void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }
```

### 缓存策略

- lru
- threadlocal：使用的是 ThreadLocalCacheFactory 工厂类，类名中 ThreadLocal 是本地线 程的意思，而 ThreadLocal 最终还是使用的是 JVM 内存。
- jcache：使用的是 JCacheFactory 工厂类，是提供 javax-spi 缓存实例的工厂类，既然是一 种 **spi 机制**，可以接入很多自制的开源框架。
- expiring：使用的是 ExpiringCacheFactory 工厂类，内部的 ExpiringCache 中还是使用的 Map 数据结构来存储数据，仍然使用的是 JVM 内存。

### jcache

```java
javax.cache.Caching.CachingProviderRegistry#getCachingProvider(java.lang.ClassLoader)
    public CachingProvider getCachingProvider(ClassLoader classLoader) {
  		//获取所有 CachingProvider 接口的所有实现类
  		//如果配置了javax.cache.spi.CachingProvider系统属性，那就用loadClass方法加载实现类
  		//否则通过 ServiceLoader.load JDK 的 SPI 机制进行加载所有的 CachingProvider 实现类
      Iterator<CachingProvider> iterator = getCachingProviders(classLoader).iterator();
			//迭代开始循环所有的实现类
      if (iterator.hasNext()) {
        //取出第一个实现类
        CachingProvider provider = iterator.next();
				//然后再尝试看看是否还有第二个实现类
        if (iterator.hasNext()) {
          //如果有第二个实现类，则直接抛出多个缓存提供者的异常
          throw new CacheException("Multiple CachingProviders have been configured when only a single CachingProvider is expected");
        } else {
          //如果没有第二个实现类，那么就直接使用第一个实现类
					//也就意味着，当前系统在运行时只允许有一个实现类去实现 CachingProvider 接口
          return provider;
        }
      } else {
        throw new CacheException("No CachingProviders have been configured");
      }
    }
```

**Redisson**

```java
org.redisson.jcache.JCachingProvider#loadConfig
    private Config loadConfig(URI uri) {
        Config config = null;
        try {
            URL yamlUrl = null;
            if (DEFAULT_URI_PATH.equals(uri.getPath())) {
                //尝试加载 /redisson-jcache.yaml 配置文件
                yamlUrl = JCachingProvider.class.getResource("/redisson-jcache.yaml");
            } else {
                yamlUrl = uri.toURL();
            }
            if (yamlUrl != null) {
                //最终转成 org.redisson.config.Config 对象
                config = Config.fromYAML(yamlUrl);
            } else {
                //若没有 /redisson-jcache.yaml 配置文件则抛出文件不存在的异常
                throw new FileNotFoundException("/redisson-jcache.yaml");
            }
        } catch (JsonProcessingException e) {
            throw new CacheException(e);
        } catch (IOException e) {
            try {
                URL jsonUrl = null;
                if (DEFAULT_URI_PATH.equals(uri.getPath())) {
                    //尝试加载 /redisson-jcache.json 配置文件
                    jsonUrl = JCachingProvider.class.getResource("/redisson-jcache.json");
                } else {
                    jsonUrl = uri.toURL();
                }
                if (jsonUrl != null) {
                    config = Config.fromJSON(jsonUrl);
                }
            } catch (IOException ex) {
                // skip
            }
        }
        return config;
    }
```

### 应用场景

- 数据库缓存，对于从数据库查询出来的数据，如果多次查询变化差异较小的话，可以按照一定的维度缓存起来，减少访问数据库的次数，为数据库减压。
- 业务层缓存，对于一些聚合的业务逻辑，执行时间过长或调用次数太多，而又可以容忍一段时间内数据的差异性，也可以考虑采取一定的维度缓存起来。

## 流量控制

### 单机限流

![CleanShot 2024-09-24 at 21.54.01@2x](https://pic-go-image.oss-cn-beijing.aliyuncs.com/pic/CleanShot%202024-09-24%20at%2021.54.01%402x.png)

### 分布式限流

### 应用场景

- 合法性限流，比如验证码攻击、恶意 IP 爬虫、恶意请求参数，利用限流可以有效拦截这些恶意请求，保证正常业务的运转。
- 业务限流，参考接口的业务调用频率，我们要合理地评估功能的并发支撑能力。
- 网关限流，当首页加载流量异常爆炸时，也可以进行有效的限流控制。
- 连接数限流，比如利用线程池的数量来控制流量。

