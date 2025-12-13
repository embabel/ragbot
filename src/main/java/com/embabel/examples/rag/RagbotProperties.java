package com.embabel.examples.rag;

import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.common.ai.model.LlmOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ragbot")
public record RagbotProperties(
        LlmOptions chatLlm,
        RoleGoalBackstory persona,
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
