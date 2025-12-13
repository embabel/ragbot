package com.embabel.examples.rag;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.rag.service.SearchOperations;
import com.embabel.agent.rag.tools.ToolishRag;
import com.embabel.chat.Conversation;
import com.embabel.chat.UserMessage;

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
        var assistantMessage = context.
                ai()
                .withLlm(properties.chatLlm())
                .withSystemPrompt("""
                        You are a thorough, relentless guru on legislation and legal documents.
                        You research doggedly until you are absolutely sure.
                        You ground your answers in literal citations from the provided sources.
                        You value quality over speed.
                        Your task is to answer the user's questions using the available tools.
                        DO NOT RELY ON GENERAL KNOWLEDGE unless you are certain a better answer is not in the provided sources.
                        Be concise in your answers.
                        """)
                .withReference(toolishRag)
                .respond(conversation.getMessages());
        context.sendMessage(conversation.addMessage(assistantMessage));
    }
}
