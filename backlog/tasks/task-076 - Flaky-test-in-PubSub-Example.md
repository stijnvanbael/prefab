---
id: task-076
title: Flaky test in PubSub Example
status: Done
assignee: []
created_date: '2026-01-08 18:09'
updated_date: '2026-01-19 17:32'
labels:
  - "\U0001F41Ebug"
dependencies: []
ordinal: 9000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MessageIntegrationTest

```
org.awaitility.core.ConditionTimeoutException: Assertion condition defined as a Lambda expression in be.appify.prefab.example.pubsub.message.MessageIntegrationTest 
Expecting ArrayList:
  [[], [], []]
to contain:
  [[UnreadMessage[message=SimpleReference[id=c7fc0948-130c-4249-b3d2-02a8db7e8107], channel=SimpleReference[id=b4b7d79a-be8a-4d70-9c50-b9fa1c513a78]]],
    [UnreadMessage[message=SimpleReference[id=c7fc0948-130c-4249-b3d2-02a8db7e8107], channel=SimpleReference[id=b4b7d79a-be8a-4d70-9c50-b9fa1c513a78]]]]
but could not find the following element(s):
  [[UnreadMessage[message=SimpleReference[id=c7fc0948-130c-4249-b3d2-02a8db7e8107], channel=SimpleReference[id=b4b7d79a-be8a-4d70-9c50-b9fa1c513a78]]]]
 within 5 seconds.
```

Note: when the test fails, the GCloud emulator has exceptions in the logs:
```
[pubsub] Exception in thread "grpc-default-executor-0" Exception in thread "grpc-default-executor-3" java.lang.NoSuchMethodError: 'io.grpc.StatusRuntimeException io.grpc.InternalStatus.asRuntimeException(io.grpc.Status, io.grpc.Metadata, boolean)'
2026-01-15T09:49:01.271209591Z [pubsub] 	at io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl.closedInternal(ServerCallImpl.java:378)
2026-01-15T09:49:01.271324341Z [pubsub] 	at io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl.closed(ServerCallImpl.java:364)
2026-01-15T09:49:01.271472924Z [pubsub] 	at io.grpc.internal.ServerImpl$JumpToApplicationThreadServerStreamListener$1Closed.runInContext(ServerImpl.java:910)
2026-01-15T09:49:01.271480591Z [pubsub] 	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37)
2026-01-15T09:49:01.271569758Z [pubsub] 	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133)
2026-01-15T09:49:01.271574341Z [pubsub] 	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
2026-01-15T09:49:01.271832133Z [pubsub] 	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
2026-01-15T09:49:01.271912924Z [pubsub] 	at java.base/java.lang.Thread.run(Thread.java:1583)
2026-01-15T09:49:01.271998341Z [pubsub] java.lang.NoSuchMethodError: 'io.grpc.StatusRuntimeException io.grpc.InternalStatus.asRuntimeException(io.grpc.Status, io.grpc.Metadata, boolean)'
2026-01-15T09:49:01.272059633Z [pubsub] 	at io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl.closedInternal(ServerCallImpl.java:378)
2026-01-15T09:49:01.272127758Z [pubsub] 	at io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl.closed(ServerCallImpl.java:364)
2026-01-15T09:49:01.272202466Z [pubsub] 	at io.grpc.internal.ServerImpl$JumpToApplicationThreadServerStreamListener$1Closed.runInContext(ServerImpl.java:910)
2026-01-15T09:49:01.272423341Z [pubsub] 	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37)
2026-01-15T09:49:01.272510841Z [pubsub] 	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133)
2026-01-15T09:49:01.272623674Z [pubsub] 	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
2026-01-15T09:49:01.272690216Z [pubsub] 	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
2026-01-15T09:49:01.272842216Z [pubsub] 	at java.base/java.lang.Thread.run(Thread.java:1583)
2026-01-15T09:49:01.275787966Z [pubsub] Exception in thread "grpc-default-executor-5" java.lang.NoSuchMethodError: 'io.grpc.StatusRuntimeException io.grpc.InternalStatus.asRuntimeException(io.grpc.Status, io.grpc.Metadata, boolean)'
2026-01-15T09:49:01.275814591Z [pubsub] 	at io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl.closedInternal(ServerCallImpl.java:378)
2026-01-15T09:49:01.275870133Z [pubsub] 	at io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl.closed(ServerCallImpl.java:364)
2026-01-15T09:49:01.275893758Z [pubsub] 	at io.grpc.internal.ServerImpl$JumpToApplicationThreadServerStreamListener$1Closed.runInContext(ServerImpl.java:910)
2026-01-15T09:49:01.275987383Z [pubsub] 	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37)
2026-01-15T09:49:01.276029299Z [pubsub] 	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133)
2026-01-15T09:49:01.276030716Z [pubsub] 	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
2026-01-15T09:49:01.276031466Z [pubsub] 	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
2026-01-15T09:49:01.276032133Z [pubsub] 	at java.base/java.lang.Thread.run(Thread.java:1583)
```
<!-- SECTION:DESCRIPTION:END -->
