PREFIX ex:  <http://example.org/>

DELETE{
	?b0 ex:type ex:join .
  	?b0 ex:arg ?b1 .
  	?b1 ex:arg ?q1 .
  	?b0 ex:arg ?b2 .
  	?b2 ex:type ex:union .
  	?b2 ex:arg ?q2 .
  	?b ?y ?b0 .
}
INSERT{
	?x ex:type ex:join .
	?x ex:arg ?q1 .
	?x ex:arg ?q2 .
	?b1 ex:arg ?x .
	?b ?y ?b1 .
}
WHERE
  { 
  	{
  	SELECT DISTINCT ?b ?y ?b0 ?b1 ?b2 (BNODE() AS ?x) ?q1 ?q2
  	WHERE
  	{
		{
		SELECT DISTINCT ?b ?y ?b0 ?b1 ?b2
		WHERE{
			{
			SELECT DISTINCT ?b0 ?b1
			WHERE{
				?b0 ex:type ex:join .
				?b0 ex:arg ?b1 .
				?b1 ex:type ex:union .
				?b0 ex:arg ?b2 .
				?b2 ex:type ex:union .
				FILTER(?b1 != ?b2)
				} LIMIT 1
			}
			OPTIONAL {?b ?y ?b0 FILTER(?y = ex:arg || ?y = ex:OP)} .
			?b0 ex:arg ?b2 .
			?b2 ex:type ex:union .
			FILTER(?b1 != ?b2)
			}
		}
	  	?b1 ex:arg ?q1 .
	  	?b2 ex:arg ?q2 .
	  	}
  	}
  } 