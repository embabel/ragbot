package com.embabel.examples.ragbot;

import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.common.ai.model.LlmOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties for chatbot
 *
 * @param chatLlm       LLM model and hyperparameters to use
 * @param objective     the goal of the chatbot's responses: For example, to answer legal questions
 * @param persona       the persona and output style of the chatbot while achieving its objective
 * @param chunkerConfig configuration for ingestion
 */
@ConfigurationProperties(prefix = "ragbot")
public record RagbotProperties(
        @NestedConfigurationProperty LlmOptions chatLlm,
        String objective,
        String persona,
        @NestedConfigurationProperty ContentChunker.DefaultConfig chunkerConfig
) {
}
