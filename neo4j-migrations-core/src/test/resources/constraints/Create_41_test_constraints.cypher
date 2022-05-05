CREATE CONSTRAINT constraint_name1 ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY;
CREATE CONSTRAINT constraint_name1 IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY;
CREATE CONSTRAINT constraint_name2 ON (n:Book) ASSERT exists(n.isbn);
CREATE CONSTRAINT constraint_name2 IF NOT EXISTS ON (n:Book) ASSERT exists(n.isbn);
CREATE CONSTRAINT constraint_name3 ON ()-[r:LIKED]-() ASSERT exists(r.day);
CREATE CONSTRAINT constraint_name3 IF NOT EXISTS ON ()-[r:LIKED]-() ASSERT exists(r.day);
CREATE CONSTRAINT constraint_name4 ON (n:Book) ASSERT n.isbn IS UNIQUE;
CREATE CONSTRAINT constraint_name4 IF NOT EXISTS ON (n:Book) ASSERT n.isbn IS UNIQUE;
