ALTER TABLE group_course DROP CONSTRAINT group_course_course_id_group_id_school_year_key;
ALTER TABLE group_course ADD COLUMN semester SMALLINT;
UPDATE group_course SET semester = 1 WHERE semester IS NULL;
ALTER TABLE group_course ALTER COLUMN semester SET NOT NULL;
ALTER TABLE group_course ADD CONSTRAINT group_course_semester_check CHECK (semester IN (1, 2));
ALTER TABLE group_course ADD CONSTRAINT group_course_course_id_group_id_school_year_semester_key
    UNIQUE (course_id, group_id, school_year, semester);