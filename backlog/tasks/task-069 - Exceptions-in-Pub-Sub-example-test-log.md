---
id: task-069
title: Exceptions in Pub/Sub example test log
status: Done
assignee: []
created_date: '2025-12-30 12:53'
updated_date: '2026-01-08 18:09'
labels:
  - "\U0001F41Ebug"
dependencies: []
ordinal: 26000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
```
2025-12-30T13:50:14.252+01:00 ERROR 79011 --- [prefab.pubsub-example] [ault-executor-1] c.g.c.p.v.StreamingSubscriberConnection  : terminated streaming with exception

com.google.api.gax.rpc.NotFoundException: com.google.api.gax.rpc.NotFoundException: io.grpc.StatusRuntimeException: NOT_FOUND: Subscription does not exist (resource=user-integration-test)
        at com.google.api.gax.rpc.ApiExceptionFactory.createException(ApiExceptionFactory.java:90) ~[gax-2.71.0.jar:2.71.0]
        at com.google.api.gax.rpc.ApiExceptionFactory.createException(ApiExceptionFactory.java:41) ~[gax-2.71.0.jar:2.71.0]
        at com.google.cloud.pubsub.v1.StreamingSubscriberConnection$1.onFailure(StreamingSubscriberConnection.java:376) ~[google-cloud-pubsub-1.143.0.jar:1.143.0]
        at com.google.api.core.ApiFutures$1.onFailure(ApiFutures.java:84) ~[api-common-2.54.0.jar:2.54.0]
        at com.google.common.util.concurrent.Futures$CallbackListener.run(Futures.java:1125) ~[guava-33.4.8-jre.jar:na]
        at com.google.common.util.concurrent.DirectExecutor.execute(DirectExecutor.java:30) ~[guava-33.4.8-jre.jar:na]
        at com.google.common.util.concurrent.AbstractFuture.executeListener(AbstractFuture.java:1004) ~[guava-33.4.8-jre.jar:na]
        at com.google.common.util.concurrent.AbstractFuture.complete(AbstractFuture.java:767) ~[guava-33.4.8-jre.jar:na]
        at com.google.common.util.concurrent.AbstractFuture.setException(AbstractFuture.java:516) ~[guava-33.4.8-jre.jar:na]
        at com.google.api.core.AbstractApiFuture$InternalSettableFuture.setException(AbstractApiFuture.java:92) ~[api-common-2.54.0.jar:2.54.0]
        at com.google.api.core.AbstractApiFuture.setException(AbstractApiFuture.java:74) ~[api-common-2.54.0.jar:2.54.0]
        at com.google.api.core.SettableApiFuture.setException(SettableApiFuture.java:51) ~[api-common-2.54.0.jar:2.54.0]
        at com.google.cloud.pubsub.v1.StreamingSubscriberConnection$StreamingPullResponseObserver.onError(StreamingSubscriberConnection.java:298) ~[google-cloud-pubsub-1.143.0.jar:1.143.0]
        at com.google.api.gax.tracing.TracedResponseObserver.onError(TracedResponseObserver.java:104) ~[gax-2.71.0.jar:2.71.0]
        at com.google.api.gax.grpc.ExceptionResponseObserver.onErrorImpl(ExceptionResponseObserver.java:84) ~[gax-grpc-2.71.0.jar:2.71.0]
        at com.google.api.gax.rpc.StateCheckingResponseObserver.onError(StateCheckingResponseObserver.java:84) ~[gax-2.71.0.jar:2.71.0]
        at com.google.api.gax.grpc.GrpcDirectStreamController$ResponseObserverAdapter.onClose(GrpcDirectStreamController.java:148) ~[gax-grpc-2.71.0.jar:2.71.0]
        at io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:565) ~[grpc-core-1.71.0.jar:1.71.0]
        at io.grpc.internal.ClientCallImpl.access$100(ClientCallImpl.java:72) ~[grpc-core-1.71.0.jar:1.71.0]
        at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:733) ~[grpc-core-1.71.0.jar:1.71.0]
        at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:714) ~[grpc-core-1.71.0.jar:1.71.0]
        at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37) ~[grpc-core-1.71.0.jar:1.71.0]
        at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133) ~[grpc-core-1.71.0.jar:1.71.0]
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144) ~[na:na]
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642) ~[na:na]
        at java.base/java.lang.Thread.run(Thread.java:1582) ~[na:na]
Caused by: com.google.api.gax.rpc.NotFoundException: io.grpc.StatusRuntimeException: NOT_FOUND: Subscription does not exist (resource=user-integration-test)
        at com.google.api.gax.rpc.ApiExceptionFactory.createException(ApiExceptionFactory.java:90) ~[gax-2.71.0.jar:2.71.0]
        at com.google.api.gax.rpc.ApiExceptionFactory.createException(ApiExceptionFactory.java:41) ~[gax-2.71.0.jar:2.71.0]
        at com.google.api.gax.grpc.GrpcApiExceptionFactory.create(GrpcApiExceptionFactory.java:86) ~[gax-grpc-2.71.0.jar:2.71.0]
        at com.google.api.gax.grpc.GrpcApiExceptionFactory.create(GrpcApiExceptionFactory.java:66) ~[gax-grpc-2.71.0.jar:2.71.0]
        at com.google.api.gax.grpc.ExceptionResponseObserver.onErrorImpl(ExceptionResponseObserver.java:82) ~[gax-grpc-2.71.0.jar:2.71.0]
        ... 11 common frames omitted
Caused by: io.grpc.StatusRuntimeException: NOT_FOUND: Subscription does not exist (resource=user-integration-test)
        at io.grpc.Status.asRuntimeException(Status.java:532) ~[grpc-api-1.71.0.jar:1.71.0]
        ... 10 common frames omitted
```
<!-- SECTION:DESCRIPTION:END -->
