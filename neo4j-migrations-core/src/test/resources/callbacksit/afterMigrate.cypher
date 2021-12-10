OPTIONAL MATCH (n:MigrationNode) WITH count(n) AS cnt
CREATE (m:AfterMigrate {migratedNodes: cnt})
RETURN m;
