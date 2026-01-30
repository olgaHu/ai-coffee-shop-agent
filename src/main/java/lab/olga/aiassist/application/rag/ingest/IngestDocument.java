package lab.olga.aiassist.application.rag.ingest;

import java.util.Map;

public record IngestDocument(
        String id,
        String text,
        Map<String, Object> metadata
) {}