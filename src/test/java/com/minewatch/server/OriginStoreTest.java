package com.minewatch.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OriginStoreTest {
    @Test void roundTripKeepsEveryField() {
        Map<String, OriginStore.Entry> m = new LinkedHashMap<>();
        m.put("uuid-1", new OriginStore.Entry("minecraft:overworld", 1.5, 64, -3.25, 90f, -10f));
        m.put("uuid-2", new OriginStore.Entry("minecraft:the_nether", 0, 70, 0, 0f, 0f));
        Map<String, OriginStore.Entry> back = OriginStore.fromJson(OriginStore.toJson(m));
        assertEquals(m, back);
    }

    @Test void corruptOrEmptyJsonGivesEmptyMap() {
        assertTrue(OriginStore.fromJson("{not json").isEmpty());
        assertTrue(OriginStore.fromJson("").isEmpty());
        assertTrue(OriginStore.fromJson("null").isEmpty());
    }

    @Test void saveLoadAndDeleteWhenEmpty(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("o.json");
        Map<String, OriginStore.Entry> m = new LinkedHashMap<>();
        m.put("a", new OriginStore.Entry("minecraft:overworld", 1, 2, 3, 4f, 5f));
        OriginStore.save(f, m);
        assertEquals(m, OriginStore.load(f));
        OriginStore.save(f, new LinkedHashMap<>());
        assertTrue(!Files.exists(f));
        assertTrue(OriginStore.load(f).isEmpty());
    }
}
