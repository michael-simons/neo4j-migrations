LOAD CSV WITH HEADERS FROM 'https://data.neo4j.com/importing-cypher/movies.csv' AS row
CALL (row) {
   MERGE (m:Movie {movieId: row.movieId})
   MERGE (y:Year {year: row.year})
   MERGE (m)-[r:RELEASED_IN]->(y)
} IN 2 CONCURRENT TRANSACTIONS OF 10 ROWS ON ERROR CONTINUE REPORT STATUS as status
WITH status
WHERE status.errorMessage IS NOT NULL
RETURN status.transactionId AS transaction, status.committed AS commitStatus, status.errorMessage AS errorMessage
