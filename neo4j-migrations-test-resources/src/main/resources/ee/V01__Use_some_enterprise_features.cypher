CREATE CONSTRAINT constraint_name FOR ()-[like:LIKED]-() REQUIRE like.day IS NOT NULL;
