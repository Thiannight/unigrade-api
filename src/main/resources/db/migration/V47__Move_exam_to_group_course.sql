ALTER TABLE exam
    DROP CONSTRAINT exam_course_id_fkey,
    DROP COLUMN course_id,
    ADD COLUMN group_course_id UUID NOT NULL,
    ADD CONSTRAINT exam_group_course_id_fkey
        FOREIGN KEY (group_course_id) REFERENCES group_course (id) ON DELETE CASCADE;
