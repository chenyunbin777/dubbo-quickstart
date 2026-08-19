package org.apache.dubbo.samples.quickstart.cache.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.samples.quickstart.cache.annotation.SmartCachePut;
import org.apache.dubbo.samples.quickstart.cache.annotation.SmartCacheEvict;
import org.apache.dubbo.samples.quickstart.cache.annotation.SmartCacheable;
import org.apache.dubbo.samples.quickstart.cache.config.SmartCacheProperties;
import org.apache.dubbo.samples.quickstart.cache.core.CacheKeyGenerator;
import org.apache.dubbo.samples.quickstart.cache.core.CacheLookup;
import org.apache.dubbo.samples.quickstart.cache.core.RedisCacheExecutor;
import org.apache.dubbo.samples.quickstart.cache.support.AfterCommitExecutor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmartCacheAspectTest {

    private RedisCacheExecutor cacheExecutor;
    private CacheKeyGenerator keyGenerator;
    private SmartCacheAspect aspect;

    @BeforeEach
    void setUp() {
        cacheExecutor = mock(RedisCacheExecutor.class);
        keyGenerator = mock(CacheKeyGenerator.class);
        aspect = new SmartCacheAspect(
                cacheExecutor,
                keyGenerator,
                new SmartCacheProperties(),
                new ObjectMapper(),
                new AfterCommitExecutor());
    }

    @Test
    void returnsCachedValueWithoutCallingDatabase() throws Throwable {
        Method method = SampleService.class.getMethod("find", Long.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new Object[]{1L}, "database");
        when(keyGenerator.generate("sample", 1L)).thenReturn("sample:1");
        when(cacheExecutor.get("sample:1")).thenReturn(CacheLookup.hit("json"));
        when(cacheExecutor.deserialize(eq("json"), any())).thenReturn("cached");

        Object result = aspect.cacheable(joinPoint, method.getAnnotation(SmartCacheable.class));

        assertEquals("cached", result);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void storesShortLivedNullMarkerForMissingData() throws Throwable {
        Method method = SampleService.class.getMethod("find", Long.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new Object[]{2L}, null);
        when(keyGenerator.generate("sample", 2L)).thenReturn("sample:2");
        when(cacheExecutor.get("sample:2")).thenReturn(CacheLookup.of(CacheLookup.Status.MISS));

        Object result = aspect.cacheable(joinPoint, method.getAnnotation(SmartCacheable.class));

        assertNull(result);
        verify(cacheExecutor).putNull("sample:2", -1);
    }

    @Test
    void writesUpdateResultToCache() throws Throwable {
        Method method = SampleService.class.getMethod("update", String.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new Object[]{"new-value"}, "new-value");
        when(keyGenerator.generate("sample", "new-value")).thenReturn("sample:new-value");

        Object result = aspect.put(joinPoint, method.getAnnotation(SmartCachePut.class));

        assertEquals("new-value", result);
        verify(cacheExecutor).put("sample:new-value", "new-value", -1, -1);
    }

    @Test
    void evictsCacheAfterSuccessfulDelete() throws Throwable {
        Method method = SampleService.class.getMethod("delete", Long.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new Object[]{3L}, true);
        when(keyGenerator.generate("sample", 3L)).thenReturn("sample:3");

        Object result = aspect.evict(joinPoint, method.getAnnotation(SmartCacheEvict.class));

        assertEquals(true, result);
        verify(cacheExecutor).evict("sample:3");
    }

    @Test
    void keepsCacheWhenDeleteDidNotChangeDatabase() throws Throwable {
        Method method = SampleService.class.getMethod("delete", Long.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new Object[]{4L}, false);
        when(keyGenerator.generate("sample", 4L)).thenReturn("sample:4");

        Object result = aspect.evict(joinPoint, method.getAnnotation(SmartCacheEvict.class));

        assertEquals(false, result);
        verify(cacheExecutor, never()).evict("sample:4");
    }

    private ProceedingJoinPoint joinPoint(Method method, Object[] args, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new SampleService());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }

    static class SampleService {

        @SmartCacheable(namespace = "sample", key = "#id", mutex = false)
        public String find(Long id) {
            return "database";
        }

        @SmartCachePut(namespace = "sample", key = "#result")
        public String update(String value) {
            return value;
        }

        @SmartCacheEvict(namespace = "sample", key = "#id")
        public boolean delete(Long id) {
            return true;
        }
    }
}
