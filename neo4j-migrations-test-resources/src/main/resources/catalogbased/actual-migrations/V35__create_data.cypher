CREATE (b:Buch {title: 'Doctor Sleep', isbn: 'abcd', gelesen: 'ja'});
CREATE (p:Person {name: 'Michael'});

MATCH (b:Buch)
MATCH (p:Person)
CREATE (p) -[:HAT_GELESEN]->(b);

CREATE (b:Buch {title: 'Fairy Tale', isbn: '1234'});