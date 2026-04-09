---
id: TASK-109
title: Response caching (@Cached) for GET endpoints
status: To Do
assignee: []
created_date: '2026-04-09 15:29'
labels:
  - "\U0001F4E6feature"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Read-heavy business applications – product catalogues, reference data APIs, reporting dashboards – perform the same expensive queries repeatedly. Spring Boot provides a well-established caching abstraction (`@Cacheable`, `@CacheEvict`) but wiring it up for every aggregate endpoint requires significant boilerplate: declaring the cache name, annotating each method, and adding eviction on every write operation.

Prefab should generate this wiring automatically when the developer places a `@Cached` annotation on an aggregate.

Example usage:

```java
@Aggregate
@GetById
@GetList
@Cached(ttl = 300)   // cache GetById and GetList results for 5 minutes
public record ProductCategory(
    @Id Reference<ProductCategory> id,
    @Version long version,
    String code,
    String label
) {
    @Update
    public void rename(String label) { ... }

    @Delete
    public void delete() { ... }
}
```

The annotation processor generates `@Cacheable` and `@CacheEvict` calls in the service layer:
- `@Cacheable(cacheNames = "productCategory", key = "#id")" on `getById`
- `@Cacheable(cacheNames = "productCategory:list", ...)" on `getList` (with a composite key from filter/sort/page params)
- `@CacheEvict(cacheNames = {"productCategory", "productCategory:list"}, ...)" on every `@Update` and `@Delete` operation (and after `@Create`)

The `ttl` attribute on `@Cached` is used to register a `CaffeineSpec` or `RedisCacheConfiguration` in an auto-generated `CacheConfiguration` class when Caffeine or Spring Cache Redis is on the classpath.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add @Cached(ttl = -1, cacheNames = "") annotation to prefab-core; ttl = -1 means no expiry; cacheNames defaults to the aggregate class name in camelCase
- [ ] #2 The annotation processor detects @Cached on aggregates and annotates the generated service getById and getList methods with Spring's @Cacheable using the configured cache name and a suitable cache key derived from method parameters
- [ ] #3 Every generated write operation (create, update, delete) in the service gets @CacheEvict annotations targeting both the by-id cache and the list cache so stale data is never served
- [ ] #4 When ttl > 0 and Caffeine is on the classpath, generate a CacheConfiguration @Configuration class that registers a CaffeineSpec-based CacheManager with the configured TTL for each @Cached aggregate
- [ ] #5 When ttl > 0 and spring-boot-starter-data-redis is on the classpath, generate a RedisCacheConfiguration-based configuration instead, consistent with Spring Boot's Redis cache auto-configuration
- [ ] #6 Add a @Cached(condition = "...") attribute (SpEL expression) passed through to @Cacheable(condition=...) for cases where caching should be conditional
- [ ] #7 The annotation processor emits a warning (not an error) if @Cached is placed on an aggregate that has no @GetById and no @GetList annotation, since there would be nothing to cache
- [ ] #8 Add annotation-processor unit tests for the CachingPlugin following the pattern of existing plugin tests
- [ ] #9 README updated with a 'Caching' section documenting the annotation, TTL configuration, and cache backend selection
<!-- AC:END -->
