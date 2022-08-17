CREATE (b:Buch {title: 'Doctor Sleep', isbn: 'abcd'});
CREATE (p:Person {name: 'Michael'});
MATCH (b:Buch)
MATCH (p:Person)
CREATE (p) -[:HAT_GELESEN]->(b);
