package lab.olga.aiassist.ingest;

import java.util.Map;

public record IngestDocument(
        String pointId,          // UUID string   (UUID.nameUUIDFromBytes(...))
        String vectorText,       // only for embedding input
        Map<String, Object> payload
) {}
