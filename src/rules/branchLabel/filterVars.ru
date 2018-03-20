PREFIX ex:  <http://example.org/>

INSERT { ?v ex:type ex:var }
WHERE {
 ?n ex:value ?v .
 FILTER(ISBLANK(?v))
}