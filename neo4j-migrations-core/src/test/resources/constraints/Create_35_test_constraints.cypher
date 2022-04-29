CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY;
CREATE CONSTRAINT ON (n:Book) ASSERT exists(n.isbn);
CREATE CONSTRAINT ON ()-[r:LIKED]-() ASSERT exists(r.day);
CREATE CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE;
