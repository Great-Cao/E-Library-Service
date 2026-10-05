-- The "current loans" query filters on (user_id, returned_at IS NULL) and then orders
-- by loan id. Including id in the index lets SQLite read the rows already ordered
-- instead of sorting them afterwards.
--
-- This is a new migration rather than an edit to V1: V1 has already been applied to
-- existing databases, and changing it would break Flyway's checksum validation.

DROP INDEX IF EXISTS idx_loans_user_active;

CREATE INDEX idx_loans_user_active ON loans (user_id, id) WHERE returned_at IS NULL;
