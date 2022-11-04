// Cannot mix these statements in one tx
DROP CONSTRAINT foo IF EXISTS;
MATCH (n) detach delete n;
