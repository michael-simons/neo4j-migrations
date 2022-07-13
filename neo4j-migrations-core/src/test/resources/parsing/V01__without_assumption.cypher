// I was there.
CREATE CONSTRAINT isbn_exists IF NOT EXISTS ON (book:Library) ASSERT exists(book.isbn);
MATCH (n) RETURN n;