CREATE (n:`__Neo4jMigration` {version:"BASELINE"})
    -[:MIGRATED_TO {connectedAs:"neo4j",at:datetime(),in:duration("P5M1.5D"),by:"msimons"}]->
    (m:`__Neo4jMigration` {checksum:"1783004164",description:"A migration",source:"V0001__A_migration.cypher",type:"CYPHER",version:"0001"});
CREATE (m:BeforeInfo);
