package org.apache.dubbo.samples.quickstart.cache.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheLookupTest {

    @Test
    void createsValidHitAndMissResults() {
        CacheLookup hit = CacheLookup.hit("value");
        CacheLookup miss = CacheLookup.of(CacheLookup.Status.MISS);

        assertEquals(CacheLookup.Status.HIT, hit.status());
        assertEquals("value", hit.value());
        assertEquals(CacheLookup.Status.MISS, miss.status());
        assertNull(miss.value());
    }

    @Test
    void rejectsInvalidStateCombinations() {
        assertThrows(IllegalArgumentException.class,
                () -> CacheLookup.of(CacheLookup.Status.HIT));
        assertThrows(IllegalArgumentException.class,
                () -> new CacheLookup(CacheLookup.Status.HIT, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CacheLookup(CacheLookup.Status.ERROR, "unexpected"));
    }
}
