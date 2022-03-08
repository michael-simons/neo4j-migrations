// Hello
// assume that version is 3.4
CREATE (agent:`007`) RETURN agent;
UNWIND RANGE(1,6) AS i
WITH i CREATE (n:OtherAgents {idx: '00' + i})
RETURN n
;
// this is not a precondition
// assert that edition is enterprise
MATCH (n) RETURN n;
// assert in target q' match (n:`007`) return count(n) = 0;
CREATE (f:F) RETURN f;