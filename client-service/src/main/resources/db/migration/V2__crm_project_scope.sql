ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS project_id UUID,
    ADD COLUMN IF NOT EXISTS stage VARCHAR(40) NOT NULL DEFAULT 'LEAD',
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_clients_project_id ON clients(project_id);
CREATE INDEX IF NOT EXISTS idx_clients_project_stage ON clients(project_id, stage);
