package com.embabel.examples.ragbot;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.rag.service.SearchOperations;
import com.embabel.agent.rag.tools.ToolishRag;
import com.embabel.chat.Conversation;
import com.embabel.chat.UserMessage;

import java.util.Map;

/**
 * The platform can use any action to respond to user messages.
 */
@EmbabelComponent
public class ChatActions {

    private final ToolishRag toolishRag;
    private final RagbotProperties properties;

    public ChatActions(
            SearchOperations searchOperations,
            RagbotProperties properties) {
        this.toolishRag = new ToolishRag(
                "sources",
                "Sources for answering user questions",
                searchOperations
        );
        this.properties = properties;
    }

    @Action(canRerun = true, trigger = UserMessage.class)
    void respond(
            Conversation conversation,
            ActionContext context) {
        // We could use a simple prompt here but choose to use a template
        // as chatbots tend to require longer prompts
        var assistantMessage = context.
                ai()
                .withLlm(properties.chatLlm())
                .withReference(toolishRag)
                .withTemplate("ragbot")
                .respondWithSystemPrompt(conversation, Map.of(
                        "properties", properties,
                        "persona", properties.persona(),
                        "objective", properties.objective()
                ));
        context.sendMessage(conversation.addMessage(assistantMessage));
    }
}
