ALTER TABLE tasks RENAME COLUMN deadline TO end_date;
ALTER TABLE tasks ADD COLUMN start_date TIMESTAMP;

CREATE TABLE task_attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    uploader_email VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(500) NOT NULL,
    stored_file_name VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(255),
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_task_attachments_task ON task_attachments(task_id);
CREATE INDEX idx_task_attachments_created_at ON task_attachments(created_at);
