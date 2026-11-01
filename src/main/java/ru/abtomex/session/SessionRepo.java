package ru.abtomex.session;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionRepo {
    private final Map<String, UploadSession> sessions = new ConcurrentHashMap<>();

    public UploadSession create(String fileName, long fileSize, int chunkSize, List<String> chunks, List<byte[]> qrPngCache) {
        String id = UUID.randomUUID().toString();
        UploadSession session = new UploadSession(
                id,
                fileName,
                fileSize,
                chunkSize,
                chunks,
                qrPngCache
        );
        sessions.put(id, session);
        return session;

    }

    public UploadSession get(String id) {
        return sessions.get(id);
    }

    public void remove(String id) {
        sessions.remove(id);
    }
}
