package org.apache.dubbo.samples.quickstart.cache.core;

import org.apache.dubbo.samples.quickstart.cache.config.SmartCacheProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheKeyGeneratorTest {

    @Test
    void createsVersionedNamespacedKey() {
        SmartCacheProperties properties = new SmartCacheProperties();
        properties.setKeyPrefix("app");
        CacheKeyGenerator generator = new CacheKeyGenerator(properties);

        assertEquals("app:user:v1:42", generator.generate("user", 42L));
        assertEquals("lock:app:user:v1:42", generator.lockKey("app:user:v1:42"));
    }

    @Test
    void rejectsEmptyKeys() {
        CacheKeyGenerator generator = new CacheKeyGenerator(new SmartCacheProperties());

        assertThrows(IllegalArgumentException.class, () -> generator.generate("user", null));
    }
}
