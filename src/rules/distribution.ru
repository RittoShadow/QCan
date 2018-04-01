PREFIX ex:  <http://example.org/>

DELETE{
	?b ?y ?b0 .
	?b0 ex:type ex:join .
  	?b0 ex:arg ?b1 .
  	?b1 ex:type ex:union .
  	?b1 ex:arg ?q1 .
  	?b0 ex:arg ?b2 .
}
INSERT{
	?x ex:type ex:join .
	?x ex:arg ?q1 .
	?x ex:arg ?b2 .
	?b1 ex:arg ?x .
	?b1 ex:type ex:union .
	?b ?y ?b1 .
}
WHERE
  { 
	{
	SELECT DISTINCT ?b0 ?b1 (BNODE() AS ?x) ?q1 ?b2
	WHERE{
		{
			SELECT ?b0 ?b1
		  	WHERE{
			  	?b0 ex:type ex:join .
			  	?b0 ex:arg ?b1 .
			  	?b1 ex:type ex:union .
			  	?b0 ex:arg ?b2 .
			  	FILTER(?b1 != ?b2)
			  	FILTER NOT EXISTS{
			  		?b2 ex:type ex:union .
			  		FILTER(?b2 != ?b1)
			  	}
			  	} LIMIT 1
			}
	  	?b1 ex:arg ?q1 .
	  	?b0 ex:arg ?b2 .
	  	FILTER( ?b1 != ?b2 )
	  	}
  	}
  	OPTIONAL {?b ?y ?b0 FILTER(?y = ex:arg || ?y = ex:OP)} .	
  } 