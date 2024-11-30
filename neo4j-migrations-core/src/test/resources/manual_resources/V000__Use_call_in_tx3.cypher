MATCH (type:AssetType) MATCH (a:Asset)-[:ASSET_TYPE]->(type)
CALL (a, type) {
CALL apoc.create.setLabels(a, [ type.naam ] ) yield node
} IN 2 CONCURRENT TRANSACTIONS OF 1000 ROWS;