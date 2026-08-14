ALTER TABLE membership DROP CONSTRAINT membership_group_id_student_id_key;

CREATE UNIQUE INDEX membership_active_group_student_uk
    ON membership (group_id, student_id)
    WHERE end_date IS NULL;

CREATE UNIQUE INDEX membership_active_student_uk
    ON membership (student_id)
    WHERE end_date IS NULL;
