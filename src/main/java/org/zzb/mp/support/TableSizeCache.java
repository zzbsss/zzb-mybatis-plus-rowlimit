package org.zzb.mp.support;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class TableSizeCache {

    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();

    public void put(String tableName, long rowCount) {
        cache.put(tableName, rowCount);
    }

    public Long get(String tableName) {
        return cache.get(tableName);
    }
}
