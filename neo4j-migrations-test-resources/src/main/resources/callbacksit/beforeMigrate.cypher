OPTIONAL MATCH (n:MigrationNode) WITH count(n) AS cnt
CREATE (m:BeforeMigrate {migratedNodes: cnt})
RETURN m;
