package com.embabel.examples.ragbot.javelit;

import com.embabel.agent.api.channel.MessageOutputChannelEvent;
import com.embabel.agent.api.channel.OutputChannel;
import com.embabel.agent.api.channel.OutputChannelEvent;
import com.embabel.agent.api.identity.SimpleUser;
import com.embabel.agent.api.identity.User;
import com.embabel.chat.*;
import com.embabel.examples.ragbot.RagbotProperties;
import io.javelit.core.Jt;
import io.javelit.core.JtContainer;
import io.javelit.core.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Javelit-based web UI for the RAG chatbot.
 * Provides a browser-based chat interface as an alternative to the shell.
 */
@Component
public record JavelitChatUI(
        Chatbot chatbot,
        RagbotProperties properties
) {
    private static final Logger logger = LoggerFactory.getLogger(JavelitChatUI.class);
    private static final AtomicReference<Server> serverRef = new AtomicReference<>();

    private static final User ANONYMOUS_USER = new SimpleUser(
            "anonymous",
            "Anonymous User",
            "anonymous",
            null
    );

    /**
     * Start the Javelit web server on the specified port.
     */
    public String start(int port, boolean openBrowser) {
        if (serverRef.get() != null) {
            return "Chat UI already running at http://localhost:" + port;
        }

        Server server = Server.builder(this::app, port).build();
        server.start();
        serverRef.set(server);

        String url = "http://localhost:" + port;
        logger.info("Javelit Chat UI started at {}", url);

        if (openBrowser) {
            openInBrowser(url);
        }

        return url;
    }

    /**
     * Start the Javelit web server on the specified port and open browser.
     */
    public String start(int port) {
        return start(port, true);
    }

    private void openInBrowser(String url) {
        try {
            // Try Desktop API first
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                logger.info("Opened browser at {}", url);
                return;
            }
        } catch (Exception e) {
            logger.debug("Desktop API failed: {}", e.getMessage());
        }

        // Fallback to platform-specific commands
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("mac")) {
                pb = new ProcessBuilder("open", url);
            } else if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", url);
            } else {
                pb = new ProcessBuilder("xdg-open", url);
            }
            pb.start();
            logger.info("Opened browser at {}", url);
        } catch (Exception e) {
            logger.warn("Failed to open browser: {}. Please open manually: {}", e.getMessage(), url);
        }
    }

    /**
     * Stop the Javelit web server.
     */
    public void stop() {
        Server server = serverRef.getAndSet(null);
        if (server != null) {
            server.stop();
            logger.info("Javelit Chat UI stopped");
        }
    }

    /**
     * Check if the server is running.
     */
    public boolean isRunning() {
        return serverRef.get() != null;
    }

    /**
     * The Javelit app definition - runs on each user interaction.
     */
    @SuppressWarnings("unchecked")
    private void app() {
        // Get or create session for this browser session
        var sessionState = Jt.sessionState();

        // Initialize message history for display
        List<Message> displayHistory = (List<Message>) sessionState
                .computeIfAbsent("displayHistory", key -> new ArrayList<>());

        // Get or create chat session
        ChatSession chatSession = (ChatSession) sessionState.computeIfAbsent("chatSession", key -> {
            var queue = new ArrayBlockingQueue<Message>(10);
            var outputChannel = new QueueingOutputChannel(queue);
            var session = chatbot.createSession(ANONYMOUS_USER, outputChannel, UUID.randomUUID().toString());
            sessionState.put("responseQueue", queue);
            return session;
        });

        BlockingQueue<Message> responseQueue = (BlockingQueue<Message>) sessionState.get("responseQueue");

        // Page title with persona name
        String persona = properties.voice() != null ? properties.voice().persona() : "Assistant";
        Jt.title(":speech_balloon: " + capitalize(persona) + " Chat").use();

        // Create container for messages
        JtContainer msgContainer = Jt.container().use();

        // Display all previous messages
        for (Message message : displayHistory) {
            if (message instanceof UserMessage) {
                Jt.markdown(":bust_in_silhouette: **You:** " + message.getContent()).use(msgContainer);
            } else if (message instanceof AssistantMessage) {
                Jt.markdown(":robot: **" + capitalize(persona) + ":** " + message.getContent()).use(msgContainer);
            }
        }

        // User input field
        String inputMessage = Jt.textInput("Your message:").use();

        // Process input when user submits
        if (inputMessage != null && !inputMessage.trim().isEmpty()) {
            // Add user message to history and display
            UserMessage userMessage = new UserMessage(inputMessage);
            displayHistory.add(userMessage);
            Jt.markdown(":bust_in_silhouette: **You:** " + inputMessage).use(msgContainer);

            // Send to chatbot
            try {
                chatSession.onUserMessage(userMessage);

                // Wait for response (with timeout)
                Message response = responseQueue.poll(60, TimeUnit.SECONDS);
                if (response != null) {
                    displayHistory.add(response);
                    Jt.markdown(":robot: **" + capitalize(persona) + ":** " + response.getContent()).use(msgContainer);
                } else {
                    Jt.warning("Response timed out").use(msgContainer);
                }
            } catch (Exception e) {
                logger.error("Error getting chatbot response", e);
                Jt.error("Error: " + e.getMessage()).use(msgContainer);
            }
        }

        // Add a divider and info section
        Jt.markdown("---").use();
        Jt.markdown("_Powered by Embabel Agent with RAG_").use();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * OutputChannel that queues assistant messages for retrieval.
     */
    private record QueueingOutputChannel(BlockingQueue<Message> queue) implements OutputChannel {
        @Override
        public void send(OutputChannelEvent event) {
            if (event instanceof MessageOutputChannelEvent msgEvent) {
                Message msg = msgEvent.getMessage();
                if (msg instanceof AssistantMessage) {
                    queue.offer(msg);
                }
            }
        }
    }
}
