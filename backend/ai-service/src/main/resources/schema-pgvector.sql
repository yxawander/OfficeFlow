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
