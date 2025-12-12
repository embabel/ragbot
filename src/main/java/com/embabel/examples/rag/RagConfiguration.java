package com.embabel.examples.rag;

import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.common.ai.model.DefaultModelSelectionCriteria;
import com.embabel.common.ai.model.ModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@ConfigurationProperties(prefix = "rag")
record RagProperties(
        @DefaultValue("800") int maxChunkSize,
        @DefaultValue("50") int overlapSize,
        @DefaultValue("false") boolean includeSectionTitleInChunk
) implements ContentChunker.Config {
    @Override
    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    @Override
    public int getOverlapSize() {
        return overlapSize;
    }

    @Override
    public boolean getIncludeSectionTitleInChunk() {
        return includeSectionTitleInChunk;
    }

}

@Configuration
@EnableConfigurationProperties(RagProperties.class)
class RagConfiguration {

    private final Logger logger = LoggerFactory.getLogger(RagConfiguration.class);

    @Bean
    LuceneSearchOperations luceneSearchOperations(
            ModelProvider modelProvider,
            RagProperties properties) {
        var embeddingService = modelProvider.getEmbeddingService(DefaultModelSelectionCriteria.INSTANCE);
        logger.info(
                "Using embedding service {} with dimensions {}",
                embeddingService.getName(),
                embeddingService.getModel().dimensions()
        );
        var luceneSearchOperations = LuceneSearchOperations
                .withName("docs")
                .withEmbeddingService(embeddingService)
                .withChunkerConfig(properties)
                .withIndexPath(Paths.get("./.lucene-index"))
                .buildAndLoadChunks();
        logger.info("Loaded {} chunks into Lucene RAG store", luceneSearchOperations.count());
        return luceneSearchOperations;
    }

}
