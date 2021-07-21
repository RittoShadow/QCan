PREFIX ex:  <http://example.org/>

DELETE{
	?b0 ex:type ex:join .
  	?b0 ex:arg ?b1 .
  	?b0 ex:leaf true .
  	?b1 ex:arg ?q1 .
  	?b1 ex:type ex:union .
  	?b1 ex:leaf true .
  	?b0 ex:arg ?b2 .
  	?b2 ex:type ex:union .
  	?b2 ex:arg ?q2 .
  	?b2 ex:leaf true .
  	?b ?y ?b0 .
  	?b0 ex:modifier ?f .
  	?q1 ?p1 ?o1 .
  	?q2 ?p2 ?o2 .
}
INSERT{
	?x ex:type ex:join .
	?x ex:arg ?z1 .
	?z1 ?p1 ?o1 .
	?x ex:arg ?z2 .
	?z2 ?p2 ?o2 .
	?nu ex:arg ?x .
	?nu ex:type ex:union .
	?nu ex:leaf true .
	?b ?y ?nu .
	?nu ex:modifier ?f .
}
WHERE
  { 
  	{
  	SELECT DISTINCT ?b ?y ?b0 ?b1 ?b2 ?nu (BNODE() AS ?x) ?q1 (BNODE() AS ?z1) ?q2 (BNODE() AS ?z2)
  	WHERE
  	{
		{
		SELECT DISTINCT ?b ?y ?b0 ?b1 ?b2 ?nu
		WHERE{
			{
			SELECT DISTINCT ?b0 ?b1 (BNODE() AS ?nu)
			WHERE{
				?b0 ex:type ex:join .
				?b0 ex:leaf true .
				?b0 ex:arg ?b1 .
				?b1 ex:type ex:union .
				?b0 ex:arg ?b2 .
				?b2 ex:type ex:union .
				FILTER(?b1 != ?b2)
				} LIMIT 1
			}
			OPTIONAL {?b ?y ?b0 FILTER(?y = ex:arg || ?y = ex:OP || ?y = ex:right || ?y = ex:left)} .
			?b0 ex:arg ?b2 .
			?b2 ex:type ex:union .
			FILTER(?b1 != ?b2)
			}
		}
	  	?b1 ex:arg ?q1 .
	  	?b2 ex:arg ?q2 .
	  	}
  	}
  	?q1 ?p1 ?o1 .
  	?q2 ?p2 ?o2 .
  	OPTIONAL {?b0 ex:modifier ?f } .
  } 