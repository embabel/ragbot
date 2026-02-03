package com.embabel.examples.ragbot;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.rag.model.Chunk;
import com.embabel.agent.rag.model.Retrievable;
import com.embabel.agent.rag.service.SearchOperations;
import com.embabel.agent.rag.service.TextSearch;
import com.embabel.agent.rag.tools.ToolishRag;
import com.embabel.chat.Conversation;
import com.embabel.chat.UserMessage;
import com.embabel.common.core.types.SimilarityResult;
import com.embabel.common.core.types.SimpleSimilaritySearchResult;
import com.embabel.common.core.types.TextSimilaritySearchRequest;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;


class CarSearchOperations implements TextSearch {

    @Override
    public @NonNull String getLuceneSyntaxNotes() {
        return "only keywords";
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NonNull <T extends Retrievable> List<SimilarityResult<T>> textSearch(@NonNull TextSimilaritySearchRequest request, @NonNull Class<T> clazz) {
        return List.of(
                (SimilarityResult<T>) new SimpleSimilaritySearchResult<>(
                        Chunk.create("The Holden Commodore was initially based on an Opel design", "parent1"),
                        0.95
                ),
                (SimilarityResult<T>) new SimpleSimilaritySearchResult<>(
                        Chunk.create("The HQ Holden was a huge seller in Australia in the early 70s", "parent2"),
                        0.90
                )
        );
    }

    @Override
    public boolean supportsType(@NonNull String type) {
        return type.equals("Chunk");
    }
}

/**
 * The platform can use any action to respond to user messages.
 */
@EmbabelComponent
public class ChatActions {

    private final SearchOperations searchOperations;
    private final RagbotProperties properties;

    public ChatActions(
            SearchOperations searchOperations,
            RagbotProperties properties) {
        this.searchOperations = searchOperations;
        this.properties = properties;
    }

    @Action(
            canRerun = true,
            trigger = UserMessage.class
    )
    void respond(
            Conversation conversation,
            ActionContext context) {
        // We could use a simple prompt here but choose to use a template
        // as chatbots tend to require longer prompts
        var musicRag = new ToolishRag(
                "sources",
                "Classic music criticism",
                searchOperations);
        var carRag = new ToolishRag(
                "car_sources",
                "Car data",
                new CarSearchOperations());
        var assistantMessage = context.
                ai()
                .withLlm(properties.chatLlm())
                .withReferences(musicRag, carRag)
                .rendering("ragbot")
                .respondWithSystemPrompt(conversation, Map.of(
                        "properties", properties,
                        "voice", properties.voice(),
                        "objective", properties.objective()
                ));
        context.sendMessage(conversation.addMessage(assistantMessage));
    }
}
