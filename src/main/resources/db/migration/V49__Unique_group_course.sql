DROP INDEX IF EXISTS group_course_active_uk;

CREATE UNIQUE INDEX group_course_group_course_uk ON group_course (group_id, course_id);
