// assert that edition is enterprise
// assume that version is 4.4
// assert that q' RETURN true
CREATE CONSTRAINT isbn_exists IF NOT EXISTS  FOR (book:Library) REQUIRE book.isbn IS NOT NULL;
