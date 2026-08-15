ALTER TABLE exam DROP COLUMN course_id;
ALTER TABLE exam ADD COLUMN group_course_id UUID NOT NULL;
ALTER TABLE exam ADD CONSTRAINT exam_group_course_id_fkey
    FOREIGN KEY (group_course_id) REFERENCES group_course (id) ON DELETE CASCADE;
