PREFIX ex:  <http://example.org/>

INSERT { ?v ex:type ex:var . 
		?v ex:temp true . }
WHERE {
 ?n ex:value ?v .
 FILTER(ISBLANK(?v))
}