PREFIX ex:  <http://example.org/>

INSERT { ?v ex:vid ?x }
WHERE {
 ?v ex:type ex:var .
 FILTER(isBlank(?v))
 BIND(STRUUID() AS ?x)
}