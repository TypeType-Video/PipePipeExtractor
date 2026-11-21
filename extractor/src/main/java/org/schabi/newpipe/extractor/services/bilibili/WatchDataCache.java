package org.schabi.newpipe.extractor.services.bilibili;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WatchDataCache {
    private volatile long cid;
    private volatile long roomId;
    private volatile long startTime;
    private volatile String bvid;

    // Fuck you auto-enqueueing
    private volatile long lastCid;
    private volatile String currentUrl;
    private volatile String lastUrl;
    private final Map<String, Long> cidMap = new ConcurrentHashMap<>();
    private final Map<String, String> bvidMap = new ConcurrentHashMap<>();
    private final Map<String, Long> roomIdMap = new ConcurrentHashMap<>();
    private final Map<String, Long> startTimeMap = new ConcurrentHashMap<>();

    @Nullable
    public Long findCid(@Nullable final String id) {
        return id == null ? null : cidMap.get(id);
    }

    public long getCid(final String id) {
        final Long value = findCid(id);
        if (value == null) {
            throw new IllegalStateException("No cached cid for Bilibili stream id: " + id);
        }
        return value;
    }

    public void setCid(final String id, final long cid) {
        this.cid = cid;
        if (id != null) {
            cidMap.put(id, cid);
        }
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(final long roomId) {
        this.roomId = roomId;
    }

    @Nullable
    public Long findRoomId(@Nullable final String id) {
        return id == null ? null : roomIdMap.get(id);
    }

    public void setRoomId(final String id, final long roomId) {
        this.roomId = roomId;
        if (id != null) {
            roomIdMap.put(id, roomId);
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    @Nullable
    public Long findStartTime(@Nullable final String id) {
        return id == null ? null : startTimeMap.get(id);
    }

    public void setStartTime(final String id, final long startTime) {
        this.startTime = startTime;
        if (id != null) {
            startTimeMap.put(id, startTime);
        }
    }

    public void setBvid(final String id, final String bvid) {
        this.bvid = bvid;
        if (id != null && bvid != null) {
            bvidMap.put(id, bvid);
        }
    }

    @Nullable
    public String getBvid(final String id) {
        return id == null ? null : bvidMap.get(id);
    }

    public synchronized void init(final String url) {
        if (url != null && url.equals(currentUrl)) {
            return;
        }
        lastCid = cid;
        lastUrl = currentUrl;
        currentUrl = url;
    }

    public long getLastCid() {
        return lastCid;
    }

    public String getLastUrl() {
        return lastUrl;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }
}
