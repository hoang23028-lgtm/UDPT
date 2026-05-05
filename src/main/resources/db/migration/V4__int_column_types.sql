-- No-op migration.
--
-- CockroachDB does not generally support ALTER COLUMN TYPE from INT8 to INT4.
-- We align the Java entities to INT8 instead (see entity field types).

SELECT 1;

