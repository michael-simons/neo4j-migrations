CREATE database foo IF NOT EXISTS WAIT;
:use foo;
CREATE (n:InFoo {foo: 'bar'});
:use neo4j;
CREATE (n:InNeo4j);
