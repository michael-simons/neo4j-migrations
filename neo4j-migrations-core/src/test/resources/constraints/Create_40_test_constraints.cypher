CREATE CONSTRAINT constraint_name1 ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY;
CREATE CONSTRAINT constraint_name2 ON (n:Book) ASSERT exists(n.isbn);
CREATE CONSTRAINT constraint_name3 ON ()-[r:LIKED]-() ASSERT exists(r.day);
CREATE CONSTRAINT constraint_name4 ON (n:Book) ASSERT n.isbn IS UNIQUE;
CREATE CONSTRAINT reconsidering_my_life_choices1 ON (n:Book) ASSERT n.`fünny things are fünny and why not, add more fun. Wow  😱` IS UNIQUE;
CREATE CONSTRAINT reconsidering_my_life_choices2 ON (n:Book) ASSERT exists(n.`fünny things are fünny and why not, add more fun. Wow  😱`);
CREATE CONSTRAINT reconsidering_my_life_choices3 ON (n:Person) ASSERT (n.firstname, n.surname, n.`person.whatever`, n.`person.a,person.b`) IS NODE KEY;
