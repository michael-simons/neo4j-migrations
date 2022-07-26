MATCH (n) RETURN count(n);
CREATE (f) RETURN f;
:use system
CALL apoc.trigger.add('test', "CALL apoc.log.info('OK') RETURN 1", { phase: 'after' });
:use neo4j
MATCH (n)
:use something
DETACH DELETE n;
:use system;
CALL apoc.trigger.remove('testTrigger');
