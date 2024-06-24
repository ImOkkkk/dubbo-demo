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
