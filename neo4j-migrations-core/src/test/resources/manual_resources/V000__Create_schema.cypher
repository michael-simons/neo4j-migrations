CREATE CONSTRAINT unique_people FOR (n:Person) REQUIRE n.name IS UNIQUE;
