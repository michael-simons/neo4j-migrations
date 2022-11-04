// assert that edition is enterprise
// assume that version is 4.3
CREATE CONSTRAINT isbn_exists IF NOT EXISTS ON (book:Library) ASSERT exists(book.isbn);
