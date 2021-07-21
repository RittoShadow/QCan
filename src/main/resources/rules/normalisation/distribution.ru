PREFIX ex:  <http://example.org/>

DELETE{
	?b ?y ?b0 .
	?b0 ex:type ex:join .
  	?b0 ex:arg ?b1 .
  	?b0 ex:leaf true .
  	?b1 ex:type ex:union .
  	?b1 ex:arg ?q1 .
  	?b1 ex:leaf true .
  	?b0 ex:arg ?b2 .
  	?b2 ?p2 ?o2 .
  	?b0 ex:modifier ?f .
}
INSERT{
	?x ex:type ex:join .
	?x ex:arg ?z .
	?z ?p2 ?o2 .
	?x ex:arg ?q1 .
	?nu ex:arg ?x .
	?nu ex:type ex:union .
	?nu ex:leaf true .
	?b ?y ?nu .
	?nu ex:modifier ?f .
}
WHERE
  { 
	{
	SELECT DISTINCT ?b0 ?b1 ?nu (BNODE() AS ?x) (BNODE() AS ?z) ?q1 ?b2
	WHERE{
		{
			SELECT ?b0 ?b1 (BNODE() AS ?nu)
		  	WHERE{
			  	?b0 ex:type ex:join .
			  	?b0 ex:leaf true .
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
  	?b2 ?p2 ?o2 .
  	OPTIONAL {?b ?y ?b0 FILTER(?y = ex:arg || ?y = ex:OP || ?y = ex:right || ?y = ex:left)} .
  	OPTIONAL {?b0 ex:modifier ?f } .	
  } 