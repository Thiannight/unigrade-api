create table if not exists course
(
    id uuid default gen_random_uuid() primary key,
    reference varchar(20)  not null,
    title varchar(50)  not null,
    credits smallint not null,
    constraint course_reference_unique unique (reference),
    constraint course_title_unique unique (title)
);

create table if not exists exam
(
    id uuid default gen_random_uuid() primary key,
    exam_date timestamptz not null,
    coefficient numeric(5, 4) not null,
    course_id uuid not null,
    constraint exam_course_fk foreign key (course_id) references course (id)
);

create table if not exists users
(
    id char(8) primary key,
    first_name varchar(100) not null,
    last_name varchar(100),
    birth_date date not null,
    email varchar(100) not null,
    password varchar(255) not null,
    is_active boolean not null,
    role varchar(20) not null
);

create table if not exists promotion
(
    id uuid default gen_random_uuid() primary key,
    reference varchar(50) not null,
    start_year smallint not null,
    end_year smallint not null,
    constraint promotion_reference_unique unique (reference),
    constraint promotion_start_year_unique unique (start_year),
    constraint promotion_end_year_unique unique (end_year)
);

create table if not exists student_group
(
    id uuid default gen_random_uuid() primary key,
    reference char(2) not null,
    promotion_id uuid not null,
    constraint student_group_reference_unique unique (reference),
    constraint student_group_promotion_fk foreign key (promotion_id) references promotion (id)
);

create table if not exists grade
(
    id uuid default gen_random_uuid() primary key,
    score real not null,
    grade_date timestamptz not null,
    reason varchar(255) not null,
    student_id char(8) not null,
    exam_id uuid not null,
    constraint grade_student_fk foreign key (student_id) references users (id),
    constraint grade_exam_fk foreign key (exam_id) references exam (id)
);

create table if not exists membership
(
    id uuid default gen_random_uuid() primary key,
    group_id uuid not null,
    student_id char(8) not null,
    start_date date not null,
    end_date date,
    constraint membership_group_student_unique unique (group_id, student_id),
    constraint membership_group_fk foreign key (group_id) references student_group (id) on delete cascade,
    constraint membership_student_fk foreign key (student_id) references users (id) on delete cascade
);

create table if not exists group_course
(
    course_id uuid not null,
    group_id uuid not null,
    primary key (course_id, group_id),
    constraint group_course_course_fk foreign key (course_id) references course (id) on delete cascade,
    constraint group_course_group_fk foreign key (group_id) references student_group (id) on delete cascade
);

create table if not exists course_teacher
(
    course_id uuid not null,
    teacher_id char(8) not null,
    primary key (course_id, teacher_id),
    constraint course_teacher_course_fk foreign key (course_id) references course (id) on delete cascade,
    constraint course_teacher_teacher_fk foreign key (teacher_id) references users (id) on delete cascade
);
