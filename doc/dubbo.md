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

