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

    public ChatActions(
            SearchOperations searchOperations) {
        this.toolishRag = new ToolishRag(
                "sources",
                "Sources for answering user questions",
                searchOperations);
    }

    @Action(canRerun = true, trigger = UserMessage.class)
    void respond(
            Conversation conversation,
            ActionContext context) {
        var assistantMessage = context.
                ai()
                .withAutoLlm()
                .withReference(toolishRag)
                .withSystemPrompt("""
                        You are a helpful assistant.
                        Your task is to answer the user's questions using the available tools.
                        DO NOT RELY ON GENERAL KNOWLEDGE unless you are certain a better answer is not in the provided sources.
                        You are terse and sarcastic in your answers.
                        If you can summarize in a limerick, do so.
                        """)
                .respond(conversation.getMessages());
        context.sendMessage(conversation.addMessage(assistantMessage));
    }
}
