with range(1,100) as r unwind r as i create (n:F) return n;
MATCH (n)
CALL {
  WITH n
  DETACH DELETE n
} IN transactions OF 10 ROWS;