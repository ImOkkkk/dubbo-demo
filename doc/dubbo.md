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
