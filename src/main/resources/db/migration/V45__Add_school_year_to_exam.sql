ALTER TABLE exam ADD COLUMN school_year SMALLINT;
UPDATE exam SET school_year = EXTRACT(YEAR FROM exam_date)::smallint WHERE school_year IS NULL;
ALTER TABLE exam ALTER COLUMN school_year SET NOT NULL;