MATCH (n) DETACH DELETE n;
CREATE (m:Person {name:'Michael'}) -[:LIKES]-> (n:Person {name:'Tina', klug:'ja'});
