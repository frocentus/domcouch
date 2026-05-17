
	at java.base/java.util.concurrent.CompletableFuture$UniApply.tryFire(CompletableFuture.java:667)
	at java.base/java.util.concurrent.CompletableFuture.postComplete(CompletableFuture.java:531)
	at java.base/java.util.concurrent.CompletableFuture.postFire(CompletableFuture.java:635)
	at java.base/java.util.concurrent.CompletableFuture$UniCompose.tryFire(CompletableFuture.java:1184)
	at java.base/java.util.concurrent.CompletableFuture$Completion.exec(CompletableFuture.java:504)
		at reactor.core.publisher.BlockingOptionalMonoSubscriber.blockingGet(BlockingOptionalMonoSubscriber.java:129)
		at reactor.core.publisher.Mono.blockOptional(Mono.java:1831)
		at com.couchbase.client.core.util.DnsSrv.fromDnsSrv(DnsSrv.java:67)
		at com.couchbase.client.core.util.ConnectionStringUtil.fromDnsSrvOrThrowIfTlsRequired(ConnectionStringUtil.java:279)
		at com.couchbase.client.core.util.ConnectionStringUtil.seedNodesFromConnectionString(ConnectionStringUtil.java:86)
		at com.couchbase.client.core.config.DefaultConfigurationProvider.lambda$launchSeedNodeResolver$0(DefaultConfigurationProvider.java:226)
		at reactor.core.publisher.MonoRunnable.call(MonoRunnable.java:73)
		at reactor.core.publisher.MonoRunnable.call(MonoRunnable.java:32)
		at reactor.core.publisher.FluxSubscribeOnCallable$CallableSubscribeOnSubscription.run(FluxSubscribeOnCallable.java:228)
		at reactor.core.scheduler.SchedulerTask.call(SchedulerTask.java:68)
		at reactor.core.scheduler.SchedulerTask.call(SchedulerTask.java:28)
		at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:328)
		at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:309)
		at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1090)
		at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:614)
		at java.base/java.lang.Thread.run(Thread.java:1474)

# Kanban Integration Test Report

**Database**: `domcouch`.`kanban_test` | **Time**: 2026-05-17T13:41:06.343909400Z
## 1. Project CRUD

### Create Project
  - **Project** `FCA27DF2...` — `DomCouch Kanban Board` (Priority: High, Status: Active)

## 2. Kanban Lanes (Hierarchy)

### Create 5 lanes as response documents under the project
| Order | Lane | WIP Limit |
|-------|------|-----------|
| 1 | Backlog | 5 |
| 2 | Development | 5 |
| 3 | Testing | 5 |
| 4 | Deployment | 5 |
| 5 | Finished | 5 |
  - Created 5 lanes as children of project `FCA27DF2...`


## 3. Tasks (Documents + Folders)

### Create 12 tasks across 5 lanes
| Task | Lane | Priority | Assignee |
|------|------|----------|----------|
| Set up Couchbase cluster | Backlog | High | Alice |
| Design document schema | Backlog | Critical | Bob |
| Implement ViewNavigator | Development | High | Alice |
| Build lazy navigator | Development | Medium | Charlie |
| Write formula engine | Development | Critical | Bob |
| Unit test all items | Testing | High | Diana |
| Integration test views | Testing | Medium | Alice |
| Deploy to staging | Deployment | High | Charlie |
| Performance benchmark | Deployment | Medium | Bob |
| Release v0.2.0 | Finished | Critical | Alice |
| Write documentation | Finished | Low | Diana |
| Code review formula engine | Development | High | Eve |
  - Created 12 tasks | Project folder: `kanban_FCA27DF2`


## 4. Categorized View — Tasks by Lane

```sql
CREATE VIEW KanbanTasksByLane
  Key: Lane
  Columns: Title, Lane, Priority, Assignee
```

### Category breakdown (in-memory navigator):
    - **Backlog** (18 children)
    - **Deployment** (18 children)
    - **Development** (36 children)
    - **Finished** (18 children)
    - **Testing** (18 children)

  Total: 113 entries, 5 categories

  Lazy nav: walked 20 entries

## 5. Formula Column View

### Computed column: PriorityLabel = @If(Priority = ...)

  Formula column view: 108 entries
15:41:08.596 [main] WARN com.domcouch.impl.CouchbaseDatabase -- getAllDocuments failed: class com.couchbase.client.java.json.JsonArray cannot be cast to class com.couchbase.client.java.json.JsonObject (com.couchbase.client.java.json.JsonArray and com.couchbase.client.java.json.JsonObject are in unnamed module of loader 'app')
  Critical tasks: 0
15:41:08.613 [main] WARN com.domcouch.impl.CouchbaseDatabase -- getAllDocuments failed: class com.couchbase.client.java.json.JsonArray cannot be cast to class com.couchbase.client.java.json.JsonObject (com.couchbase.client.java.json.JsonArray and com.couchbase.client.java.json.JsonObject are in unnamed module of loader 'app')
  Collection: 0 total, found 0 kanban tasks
  Folder 'kanban_FCA27DF2' contains 0 tasks
  Two-level: lvl1=5 lvl2=11 total=124
  First category: Backlog position=1 children=18 siblings=2
  Development lane: 36 children, sub-nav count=113

---
**Test complete.** `kanban_test` scope contains all test data.
