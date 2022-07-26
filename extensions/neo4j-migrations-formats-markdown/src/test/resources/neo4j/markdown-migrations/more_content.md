# Something else

More work has been done in the next version

```id=V1.3__something_different
CREATE (fm:Musician  {
  name: 'Freddie Mercury'
})-[:MEMBER_OF  {
  role: 'Singer'
}]->(b:Band  {
  name: 'Queen'
})
RETURN *
```
