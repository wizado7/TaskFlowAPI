ALTER TABLE tasks ADD COLUMN story_points INT;

ALTER TABLE sprints ADD COLUMN goal TEXT;
ALTER TABLE sprints ADD COLUMN capacity_points INT;
ALTER TABLE sprints ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PLANNED';
ALTER TABLE sprints ADD COLUMN completed_at TIMESTAMP;

UPDATE sprints SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'PLANNED' END;

CREATE INDEX idx_sprints_active_board ON sprints(board_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_tasks_board_sprint ON tasks(board_id, sprint_id);
