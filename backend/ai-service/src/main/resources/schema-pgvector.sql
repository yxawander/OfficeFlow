-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create vector_store table for RAG document storage
CREATE TABLE IF NOT EXISTS vector_store (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content     TEXT NOT NULL,
    metadata    JSONB DEFAULT '{}',
    embedding   vector(1024) NOT NULL
);

-- Create HNSW index for fast cosine similarity search
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops);

-- Track loaded documents to prevent duplicate ingestion on restart
CREATE TABLE IF NOT EXISTS loaded_documents (
    id          SERIAL PRIMARY KEY,
    source      VARCHAR(500) UNIQUE NOT NULL,
    file_hash   VARCHAR(64),
    chunk_count INTEGER NOT NULL DEFAULT 0,
    loaded_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Multi-turn conversation support
CREATE TABLE IF NOT EXISTS ai_conversation (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL DEFAULT '新对话',
    user_id     BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ai_conversation_message (
    id              SERIAL PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES ai_conversation(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conv_msg_conv_id ON ai_conversation_message(conversation_id);
