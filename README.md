# raft-kv

本项目是一个基于 raft 协议的分布式 Key-Value 服务, 具有基于日志的节点选举, 心跳维护, 持久化功能.

## 如何运行 🏃

假设一个三节点的系统:


|节点名|地址|server 端口|日志核心端口|
|:--:|:--:|:--:|:--:|
|A|localhost|10001|20001|
|B|localhost|10002|20002|
|C|localhost|10003|20003|

那么需要分别运行:

节点 A
> -gc A,localhost,10001,20001 B,localhost,23334,33334 C,localhost,23335,33335 -m group-member -i A -d 你的项目根目录/raft/raft-core/logs/a/

节点 B
> -gc A,localhost,10001,20001 B,localhost,23334,33334 C,localhost,23335,33335 -m group-member -i B -d 你的项目根目录/raft/raft-core/logs/a/

节点 C
> -gc A,localhost,10001,20001 B,localhost,23334,33334 C,localhost,23335,33335 -m group-member -i C -d 你的项目根目录/raft/raft-core/logs/a/

客户端启动(初始链接到 server A为例)

> -gc A,localhost,10001,20001 B,localhost,23334,33334 C,localhost,23335,33335 -m group-member -i A -d 你的项目根目录/raft/raft-core/logs/a/ -c

启动后即可在客户端 console 中执行 set/get 操作.

> set a hello-world
>
> get a

## 原理部分

### 架构和组件

项目一共由三个主要部分组成, 分别是 ==核心组件==, ==kv服务组件==, ==客户端组件==

1. 核心组件

   核心组件是 raft 算法的直接实现, 主要包含了 ==节点选举==, ==日志复制==.

   其核心是 raft 算法([论文](./document.pdf), [参考博客](https://www.cnblogs.com/xybaby/p/10124083.html)),  raft 算法中节点可能有三种状态: `leader`, `follower`, `candidate`.
   节点初始为 follower, 超时后发起选举变成 candidate, 选举成功变为 leader, 在 candidate 和 leader 状态收到了比自己 term 更大的消息则会变回 follower. 节点选举和任期通过 term 标示, term 单调递增.

   系统运行的过程大致是这样: 节点用 `term` 标志当前任期, `voteFor` 标志本任期投给了谁. 初始时节点 `term` 为 0, `voteFor` 为 `null`, 一段时间后, 节点可能收到了其他节点的投票信息, 节点选择投或不投.
   也有可能在收到其他节点投票消息之前, 就已经因为超时发起选举, 此时将自己的 term 以及最后一条日志的 index 和 term 广播,
   收到过半的节点的投票后将当选为 leader, 此时开始复制日志到其他 follower 节点, 当有过半的节点都复制到了 index , 那么 leader commit 这个 index, 并通知follower commit 到这个 index.
   此时 commit 了的操作便可以通知上层服务进行应用了.
   
   核心组件包含有 ==日志组件==, ==定时器组件==, ==通信组件==, ==成员信息组件==.

   1. 日志组件

      用来获取和添加日志条目, 基于文件的日志组件有三个部分, 持久化日志文件, 日志缓冲, 日志索引. 
      当核心组件收到一条命令后封装为日志条目放在日志缓冲中, 当这个日志条目在被过半的 follower 节点收到后, leader 节点将这个日志条目 commit 并持久化到文件中. 并为这个条目建立索引, 便于通过偏移从日志文件中找到日志条目.

   2. 定时器组件

      定时器会在定时结束之后发起选举操作, 当接受到 leader 节点的日志复制的请求之后, 定时器会被重置.

   3. 通信组件

      通信组件主要用于核心组件和其他节点的核心组件进行通信, 通信组件使用 Netty 发送消息, 当收到消息后 Netty 会通知 EventBus 的订阅者处理逻辑.
      Netty 使用 ProtoBuf 进行序列化和反序列化. 

      通信组件维护了一个主动建立/被动建立的 "节点-Channel" 映射, 发送消息时从主动 Map 这里查找对应通道发送消息. 在当选为 leader 后节点还会关闭被动建立的 Map 中的映射, 因为在短时间内都不会用到这些连接.

   4. 成员信息组件

      成员信息组件用来维护成员的日志复制进度, 当 leader 收到 follower 的复制成功消息之后会将这个节点的复制进度前推.
      通过成员组件的复制进度也能算出 CommitIndex.

2. kv服务组件

   kv 服务组件是在核心组件上提供 key-value 服务的组件, kv 组件在收到 get/set 请求之后会调用核心组件持久化操作. 

3. 客户端组件

   客户端组件用来向用户提供命令行操作界面, 支持 set/get/exit 等命令, 命令回车之后会通过 socket 被发送到 server, server 完成日志复制会应用日志, 之后返回结果给 client, client 回显结果, 等待下一次输入.

组件之间的耦合关系大致如下图, Server 对 Client, 核心组件和其他核心组件, 使用 Netty 通信. Client 对 Server 使用 `try-resource` 方式建立阻塞的 Socket 通信方式. Server 直接持有核心组件, 添加日志时直接调用, 核心组件完成日志复制之后通过回调的方式应用日志到 Server.


### 线程模型

1. Node 

   组件中包含两个线程: Scheduler 调度器线程和 TaskExecutor 任务执行线程.
   
   Scheduler 用来调度任务给 TaskExecutor 执行. Scheduler 一共包含两种任务, 一个是定时器任务, delay 时间之后就执行发起选举的逻辑, 在这之前节点若收到了 leader 消息那么任务将会被终止. 
   二是日志复制任务, 这个任务在节点成为 leader 之后开始每隔一段时间就被调度一次, 用来同步 follower 节点的日志.

2. Server

   Server 的任务由 workerGroup 线程处理. 收到 Client 的消息之后调用核心组件同步日志, 完成同步日志后将由核心组件回调 service 的接口, 完成日志的应用.

3. Client

   Client 是单线程的阻塞的方式发送命令, 阻塞等待返回, 之后回显结果到命令行.

## 性能测试 📈

## 🐞 🔨
