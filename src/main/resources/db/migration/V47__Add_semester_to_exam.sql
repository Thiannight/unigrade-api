ALTER TABLE exam ADD COLUMN semester SMALLINT;
UPDATE exam SET semester = 1 WHERE semester IS NULL;
ALTER TABLE exam ALTER COLUMN semester SET NOT NULL;
ALTER TABLE exam ADD CONSTRAINT exam_semester_check CHECK (semester IN (1, 2));