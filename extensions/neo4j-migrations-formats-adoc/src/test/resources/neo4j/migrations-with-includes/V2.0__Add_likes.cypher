CREATE (m:User {
  name: 'Michael'
})
WITH m
MATCH (a:Author {
  name: 'Stephen King'
})-[:WROTE]->(b)
WITH m, a, collect(b) AS books
CREATE (m)-[:LIKES]->(a)
WITH m, books
UNWIND books AS b
CREATE (m)-[:LIKES]->(b);