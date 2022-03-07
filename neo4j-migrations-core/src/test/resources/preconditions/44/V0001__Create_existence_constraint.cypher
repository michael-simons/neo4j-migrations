// assert that edition is enterprise
// assume that version is 4.4
// assume q' RETURN false
CREATE CONSTRAINT isbn_exists IF NOT EXISTS FOR (book:Library) REQUIRE book.isbn IS NOT NULL;
