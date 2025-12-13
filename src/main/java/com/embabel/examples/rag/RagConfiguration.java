package com.embabel.examples.rag;

import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.common.ai.model.DefaultModelSelectionCriteria;
import com.embabel.common.ai.model.ModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(RagbotProperties.class)
class RagConfiguration {

    private final Logger logger = LoggerFactory.getLogger(RagConfiguration.class);

    @Bean
    LuceneSearchOperations luceneSearchOperations(
            ModelProvider modelProvider,
            RagbotProperties properties) {
        var embeddingService = modelProvider.getEmbeddingService(DefaultModelSelectionCriteria.INSTANCE);
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
