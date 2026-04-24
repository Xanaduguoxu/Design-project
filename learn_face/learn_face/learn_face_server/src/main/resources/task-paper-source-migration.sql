-- Run on production database before deploying backend code.
-- Compatible with MySQL 5.7+ (uses information_schema + dynamic SQL).

SET @db = DATABASE();

-- 1) task.paper_source (teacher | student)
SET @col_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db
    AND table_name = 'task'
    AND column_name = 'paper_source'
);
SET @sql = IF(
  @col_exists = 0,
  'ALTER TABLE task ADD COLUMN paper_source VARCHAR(16) NOT NULL DEFAULT ''teacher'' COMMENT ''teacher|student''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) task.owner_user_id (student paper owner)
SET @col_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db
    AND table_name = 'task'
    AND column_name = 'owner_user_id'
);
SET @sql = IF(
  @col_exists = 0,
  'ALTER TABLE task ADD COLUMN owner_user_id BIGINT NULL COMMENT ''student paper owner user id''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) task.grading_mode (teacher | self)
SET @col_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db
    AND table_name = 'task'
    AND column_name = 'grading_mode'
);
SET @sql = IF(
  @col_exists = 0,
  'ALTER TABLE task ADD COLUMN grading_mode VARCHAR(16) NOT NULL DEFAULT ''teacher'' COMMENT ''teacher|self''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill old rows
UPDATE task
SET paper_source = 'teacher'
WHERE paper_source IS NULL OR paper_source = '';

UPDATE task
SET grading_mode = 'teacher'
WHERE grading_mode IS NULL OR grading_mode = '';

-- Add useful indexes
SET @idx_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db
    AND table_name = 'task'
    AND index_name = 'idx_task_source_owner_name'
);
SET @sql = IF(
  @idx_exists = 0,
  'CREATE INDEX idx_task_source_owner_name ON task (paper_source, owner_user_id, name)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db
    AND table_name = 'task'
    AND index_name = 'idx_task_name_source'
);
SET @sql = IF(
  @idx_exists = 0,
  'CREATE INDEX idx_task_name_source ON task (name, paper_source)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
