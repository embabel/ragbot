package com.embabel.examples.ragbot;

import com.embabel.agent.rag.ingestion.NeverRefreshExistingDocumentContentPolicy;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.agent.rag.model.ContentElement;
import com.embabel.agent.rag.model.Section;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.nio.file.Path;

@ShellComponent
record RagbotShell(LuceneSearchOperations luceneSearchOperations) {

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

    @ShellMethod("show sections")
    String sections() {
        var sections = luceneSearchOperations.findAll(Section.class);
        for (var section : sections) {
            System.out.println("Section ID: " + section.getId());
            System.out.println("Content: " + section.getTitle());
//            System.out.println("Metadata: " + section.getMetadata());
            System.out.println("-----");
        }
        return "\n\nTotal sections: " + sections.size();
    }

    @ShellMethod("show content elements")
    String contentElements() {
        var contentElements = luceneSearchOperations.findAll(ContentElement.class);
        for (var contentElement : contentElements) {
            System.out.println("Section ID: " + contentElement.getId());
            System.out.println(contentElement.getClass().getSimpleName());
//            System.out.println("Metadata: " + section.getMetadata());
            System.out.println("-----");
        }
        return "\n\nTotal content elements: " + contentElements.size();
    }

    @ShellMethod("show lucene statistics")
    String stats() {
        var count = luceneSearchOperations.getStatistics();
        return "Stats: " + count;
    }
}
