CREATE TABLE users
(
    id            SERIAL PRIMARY KEY,
    login         VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audio_files
(
    id           SERIAL PRIMARY KEY,
    user_id      INTEGER      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    system_path  VARCHAR(500) NOT NULL,
    format       VARCHAR(50)  NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    sber_request_file_id UUID,
    upload_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transcriptions
(
    id                    SERIAL PRIMARY KEY,
    audio_file_id         INTEGER     NOT NULL REFERENCES audio_files (id) ON DELETE CASCADE,
    sber_task_id          VARCHAR(100),
    sber_response_file_id UUID,
    status                VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'RUNNING', 'DONE', 'CANCELED', 'ERROR')),
    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_seconds      NUMERIC(10, 3),
    character_count       INTEGER,
    sentence_count        INTEGER
);

CREATE TABLE external_call_logs
(
    id               SERIAL PRIMARY KEY,
    transcription_id INTEGER     NOT NULL REFERENCES transcriptions (id) ON DELETE CASCADE,
    operation_type   VARCHAR(50) NOT NULL,
    http_method      VARCHAR(10) NOT NULL,
    http_status      INTEGER,
    message          TEXT,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE semantic_blocks
(
    id               SERIAL PRIMARY KEY,
    transcription_id INTEGER NOT NULL REFERENCES transcriptions (id) ON DELETE CASCADE,
    order_index      INTEGER NOT NULL,
    text_content     TEXT    NOT NULL,
    UNIQUE (transcription_id, order_index)
);

CREATE INDEX idx_user_login ON users (login);
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_audio_file_user_id ON audio_files (user_id);
CREATE INDEX idx_transcription_audio_file_id ON transcriptions (audio_file_id);
CREATE INDEX idx_transcription_status ON transcriptions (status);
CREATE INDEX idx_external_call_log_transcription_id ON external_call_logs (transcription_id);
CREATE INDEX idx_semantic_block_transcription_id ON semantic_blocks (transcription_id);