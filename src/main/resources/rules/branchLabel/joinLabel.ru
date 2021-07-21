PREFIX ex:  <http://example.org/>

INSERT { ?a ex:cid ?x }
WHERE {
 ?u ex:type ex:union .
 ?u ex:arg ?a .
 { ?a ex:type ex:join . }
 UNION
 { ?a ex:type ex:TP . }
 BIND(STRUUID() AS ?x)
}