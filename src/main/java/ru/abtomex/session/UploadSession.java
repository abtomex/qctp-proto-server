package ru.abtomex.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class UploadSession {

    private final String sessionId;
    private final String fileName;
    private final long fileSize;
    private final int chunkSize;
    private final List<String> chunks;
    private final List<byte[]> qrPngCache;
    private final Instant createdAt = Instant.now();

    public int getTotal() {
        return qrPngCache.size();
    }

}
