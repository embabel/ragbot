package com.embabel.examples.rag;

import com.embabel.agent.rag.ingestion.NeverRefreshExistingDocumentContentPolicy;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.nio.file.Path;

@ShellComponent
record RagShell(LuceneSearchOperations luceneSearchOperations) {

    @ShellMethod("Ingest URL or file path")
    String ingest(@ShellOption(
            help = "URL or file path to ingest",
            defaultValue = "./data/osammab2024419.md") String location) {
        var uri = location.startsWith("http://") || location.startsWith("https://")
                ? location
                : Path.of(location).toAbsolutePath().toUri().toString();
        var ingested = NeverRefreshExistingDocumentContentPolicy.INSTANCE
                .ingestUriIfNeeded(
                        luceneSearchOperations,
                        new TikaHierarchicalContentReader(),
                        uri
                );
        return ingested != null ?
                "Ingested document with ID: " + ingested :
                "Document already exists, no ingestion performed.";
    }

    @ShellMethod("clear all documents")
    String zap() {
        var count = luceneSearchOperations.clear();
        return "All %d documents deleted".formatted(count);
    }

    @ShellMethod("show chunks")
    String chunks() {
        var chunks = luceneSearchOperations.findAll();
        for (var chunk : chunks) {
            System.out.println("Chunk ID: " + chunk.getId());
            System.out.println("Content: " + chunk.getText());
            System.out.println("Metadata: " + chunk.getMetadata());
            System.out.println("-----");
        }
        return "\n\nTotal chunks: " + chunks.size();
    }
}
