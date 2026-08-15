ALTER TABLE group_course DROP CONSTRAINT group_course_course_id_group_id_key;

ALTER TABLE group_course ADD COLUMN start_date DATE NOT NULL DEFAULT CURRENT_DATE;
ALTER TABLE group_course ADD COLUMN end_date DATE;
ALTER TABLE group_course ALTER COLUMN start_date DROP DEFAULT;

CREATE UNIQUE INDEX group_course_active_uk
    ON group_course (group_id, course_id)
    WHERE end_date IS NULL;
