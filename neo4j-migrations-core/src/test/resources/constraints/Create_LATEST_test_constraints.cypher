CREATE CONSTRAINT constraint_name1 FOR (n:Person) REQUIRE (n.firstname, n.surname) IS NODE KEY;
CREATE CONSTRAINT constraint_name3 FOR (n:Book) REQUIRE n.isbn IS NOT NULL;
CREATE CONSTRAINT constraint_name4 FOR ()-[r:LIKED]-() REQUIRE r.day IS NOT NULL;
CREATE CONSTRAINT constraint_name5 FOR (n:Book) REQUIRE n.isbn IS UNIQUE;
CREATE CONSTRAINT constraint_name6 FOR ()-[l:LIKED]-() REQUIRE l.`x,liked.y` IS NOT NULL;
CREATE CONSTRAINT reconsidering_my_life_choices1 FOR (n:Person) REQUIRE (n.firstname, n.surname, n.`person.whatever`, n.`person.a,person.b 😱`) IS NODE KEY;
