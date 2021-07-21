PREFIX ex:  <http://example.org/>

DELETE {
  ?a ex:cid ?xa .
  ?v ex:vid ?xv .
}
WHERE {
  { ?a ex:cid ?xa . }
  UNION
  { ?v ex:vid ?xv . }
}