ALTER TABLE group_course DROP CONSTRAINT group_course_course_id_group_id_key;
ALTER TABLE group_course ADD COLUMN school_year SMALLINT;
UPDATE group_course SET school_year = 2024 WHERE school_year IS NULL;
ALTER TABLE group_course ALTER COLUMN school_year SET NOT NULL;
ALTER TABLE group_course ADD CONSTRAINT group_course_course_id_group_id_school_year_key
    UNIQUE (course_id, group_id, school_year);