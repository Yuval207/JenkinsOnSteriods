-- V5__add_source_type_to_projects.sql
-- Add source_type and original_file_name to projects, make repository_url nullable for ZIP uploads

ALTER TABLE projects ADD COLUMN source_type VARCHAR(50) DEFAULT 'GIT' NOT NULL;
ALTER TABLE projects ALTER COLUMN repository_url DROP NOT NULL;
ALTER TABLE projects ADD COLUMN original_file_name VARCHAR(512);

