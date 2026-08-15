ALTER TABLE teacher_course
    DROP CONSTRAINT teacher_course_course_id_teacher_id_school_year_key,
    DROP COLUMN school_year,
    ADD COLUMN priority SMALLINT NOT NULL;

ALTER TABLE teacher_course
    ADD CONSTRAINT teacher_course_course_id_teacher_id_key UNIQUE (course_id, teacher_id);
