package com.embabel.examples.ragbot;

import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.common.ai.model.LlmOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ragbot")
public record RagbotProperties(
        @NestedConfigurationProperty LlmOptions chatLlm,
        @NestedConfigurationProperty RoleGoalBackstory persona,
        @NestedConfigurationProperty ContentChunker.DefaultConfig chunkerConfig,
        @DefaultValue("false") boolean includeSectionTitleInChunk
) {
}
