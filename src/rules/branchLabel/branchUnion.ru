PREFIX ex:  <http://example.org/> 

DELETE{
		?s ?p ?n .
		?n ex:value ?vname .
		?n ?r ?u .
	}
INSERT { ?x ex:type ex:var .
		?x ex:temp false .
		?x ex:value ?vname .
		?s ?p ?x .}
WHERE {
 {
 	{
	 	SELECT DISTINCT ?vname ?x
	 	WHERE
	 	{
		 {
		 SELECT DISTINCT ?vname
		 WHERE{
		 	?v ex:value ?vname .
		 	}
		 }
		 BIND(BNODE(?vname) AS ?x)
		}
	}
 }
 ?n ex:value ?vname .
 ?s ?p ?n .
 ?n ?r ?u .
}