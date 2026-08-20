ALTER TABLE grade DROP CONSTRAINT grade_student_id_fkey,
    ADD CONSTRAINT grade_student_id_fkey FOREIGN KEY (student_id) REFERENCES users (id);
ALTER TABLE grade DROP CONSTRAINT grade_exam_id_fkey,
    ADD CONSTRAINT grade_exam_id_fkey FOREIGN KEY (exam_id) REFERENCES exam (id);

ALTER TABLE membership DROP CONSTRAINT membership_group_id_fkey,
    ADD CONSTRAINT membership_group_id_fkey FOREIGN KEY (group_id) REFERENCES student_group (id);
ALTER TABLE membership DROP CONSTRAINT membership_student_id_fkey,
    ADD CONSTRAINT membership_student_id_fkey FOREIGN KEY (student_id) REFERENCES users (id);

ALTER TABLE group_course DROP CONSTRAINT group_course_course_id_fkey,
    ADD CONSTRAINT group_course_course_id_fkey FOREIGN KEY (course_id) REFERENCES course (id);
ALTER TABLE group_course DROP CONSTRAINT group_course_group_id_fkey,
    ADD CONSTRAINT group_course_group_id_fkey FOREIGN KEY (group_id) REFERENCES student_group (id);

ALTER TABLE teacher_course DROP CONSTRAINT teacher_course_course_id_fkey,
    ADD CONSTRAINT teacher_course_course_id_fkey FOREIGN KEY (course_id) REFERENCES course (id);
ALTER TABLE teacher_course DROP CONSTRAINT teacher_course_teacher_id_fkey,
    ADD CONSTRAINT teacher_course_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES users (id);

ALTER TABLE exam DROP CONSTRAINT exam_group_course_id_fkey,
    ADD CONSTRAINT exam_group_course_id_fkey FOREIGN KEY (group_course_id) REFERENCES group_course (id);