// CREATE CONSTRAINT constraint_name ON (book:Book) ASSERT book.isbn IS NOT NULL;
// CREATE CONSTRAINT constraint_name ON (book:Book) ASSERT exists(book.isbn)
// CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY;
CREATE CONSTRAINT constraint_name ON ()-[like:LIKED]-() ASSERT exists(like.day);