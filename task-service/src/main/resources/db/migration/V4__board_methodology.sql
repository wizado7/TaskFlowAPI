ALTER TABLE boards ADD COLUMN board_methodology VARCHAR(20) NOT NULL DEFAULT 'KANBAN';

UPDATE boards SET board_methodology = 'SCRUM' WHERE board_type = 'SPRINT';
