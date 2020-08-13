PREFIX ex:  <http://example.org/>

DELETE { ?b0 ex:arg ?b1 .
		?b1 ?p ?o }
INSERT { ?b0 ?p ?o }
WHERE
  { 
	  {
	  	SELECT ?b0
	  	WHERE {
		  	?b0 ex:type ex:or .
		  	?b0 ex:arg ?b1 .
		  	?b1 ex:type ex:or .
	  	} LIMIT 1
		}
	?b0 ex:arg ?b1 .
	?b1 ex:type ex:or .
	?b1 ?p ?o .
  } 