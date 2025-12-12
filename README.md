<img align="left" src="https://github.com/embabel/embabel-agent/blob/main/embabel-agent-api/images/315px-Meister_der_Weltenchronik_001.jpg?raw=true" width="180">

![Build](https://github.com/embabel/rag-demo/actions/workflows/maven.yml/badge.svg)

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Apache Tomcat](https://img.shields.io/badge/apache%20tomcat-%23F8DC75.svg?style=for-the-badge&logo=apache-tomcat&logoColor=black)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)
![JSON](https://img.shields.io/badge/JSON-000?logo=json&logoColor=fff)
![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)

&nbsp;&nbsp;&nbsp;&nbsp;

&nbsp;&nbsp;&nbsp;&nbsp;

# RAG Demo

This project demonstrates Retrieval-Augmented Generation (RAG) using Embabel Agent with Apache Lucene for vector storage
and Spring Shell for interaction.

## Usage

Run the shell script to start Embabel under Spring Shell:

```bash
./scripts/shell.sh
```

You can also run the main class, `com.embabel.examples.rag.RagShellApplication`, directly from your IDE.

### Shell Commands

| Command        | Description                                                                                                                                                                                                                                                  |
|----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ingest [url]` | Ingest a URL into the RAG store. Uses Apache Tika to parse content hierarchically and chunks it for vector storage. Default URL is the text of the recent Australia Social Media ban for under 16s. Documents are only ingested if they don't already exist. |
| `zap`          | Clear all documents from the Lucene index. Returns the count of deleted documents.                                                                                                                                                                           |
| `chunks`       | Display all stored chunks with their IDs and content. Useful for debugging what content has been indexed.                                                                                                                                                    |
| `chat`         | Start an interactive chat session where you can ask questions about ingested content.                                                                                                                                                                        |

### Example Workflow

```bash
# Start the shell
./scripts/shell.sh

# Ingest a document
ingest https://example.com/document

# View what was indexed
chunks

# Chat with the RAG-powered assistant
chat
> What does this document say about X?

# Clear the index when done
zap
```

## Implementation

### RAG Configuration

RAG is configured in [`RagConfiguration.java`](./src/main/java/com/embabel/examples/rag/RagConfiguration.java):

```java

@Bean
LuceneSearchOperations luceneSearchOperations(
        ModelProvider modelProvider,
        RagProperties properties) {
    var embeddingService = modelProvider.getEmbeddingService(DefaultModelSelectionCriteria.INSTANCE);
    var luceneSearchOperations = LuceneSearchOperations
            .withName("docs")
            .withEmbeddingService(embeddingService)
            .withChunkerConfig(properties)
            .withIndexPath(Paths.get("./.lucene-index"))
            .buildAndLoadChunks();
    return luceneSearchOperations;
}
```

Key aspects:

- **Lucene with disk persistence**: The vector index is stored at `./.lucene-index`, surviving application restarts
- **Embedding service**: Uses the configured `ModelProvider` to get an embedding service for vectorizing content
- **Configurable chunking**: Content is split into chunks with configurable size (default 800 chars), overlap (default
  50 chars), and optional section title inclusion

Chunking properties can be configured via `application.properties`:

```properties
rag.max-chunk-size=800
rag.overlap-size=50
rag.include-section-title-in-chunk=false
```

### Chatbot Creation

The chatbot is created in [`ChatConfiguration.java`](./src/main/java/com/embabel/examples/rag/ChatConfiguration.java):

```java

@Bean
Chatbot chatbot(AgentPlatform agentPlatform) {
    return AgentProcessChatbot.utilityFromPlatform(agentPlatform);
}
```

The `AgentProcessChatbot.utilityFromPlatform()` method creates a chatbot that automatically discovers all `@Action`
methods in `@EmbabelComponent` classes. Any action with a matching trigger becomes eligible to be called when
appropriate messages arrive.

### Action Handling

Chat actions are defined in [`ChatActions.java`](./src/main/java/com/embabel/examples/rag/ChatActions.java):

```java

@EmbabelComponent
public class ChatActions {

    private final ToolishRag toolishRag;

    public ChatActions(SearchOperations searchOperations) {
        this.toolishRag = new ToolishRag(
                "sources",
                "Sources for answering user questions",
                searchOperations);
    }

    @Action(canRerun = true, trigger = UserMessage.class)
    void respond(Conversation conversation, ActionContext context) {
        var assistantMessage = context.ai()
                .withAutoLlm()
                .withReference(toolishRag)
                .withSystemPrompt("...")
                .respond(conversation.getMessages());
        context.sendMessage(conversation.addMessage(assistantMessage));
    }
}
```

Key concepts:

1. **`@EmbabelComponent`**: Marks the class as containing agent actions that can be discovered by the platform

2. **`@Action` annotation**:
    - `trigger = UserMessage.class`: This action is invoked whenever a `UserMessage` is received in the conversation
    - `canRerun = true`: The action can be executed multiple times (for each user message)

3. **`ToolishRag` as LLM reference**:
    - Wraps the `SearchOperations` (Lucene index) as a tool the LLM can use
    - When `.withReference(toolishRag)` is called, the LLM can search the RAG store to find relevant content
    - The LLM decides when to use this tool based on the user's question

4. **Response flow**:
    - User sends a message (triggering the action)
    - The action builds an AI request with the RAG reference
    - The LLM may call the RAG tool to retrieve relevant chunks
    - The LLM generates a response using retrieved context
    - The response is added to the conversation and sent back
