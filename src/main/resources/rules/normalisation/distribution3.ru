PREFIX ex:  <http://example.org/>

SELECT DISTINCT *
WHERE
  { 
  OPTIONAL {?b ?y ?b0 FILTER(?y = ex:arg || ?y = ex:OP)} .
  	?b0 ex:type ex:join .
  	{
  	SELECT ?b1
  		WHERE{ 
  		?b0 ex:arg ?b1 .
  		?b1 ex:type ex:union }
  		LIMIT 1
  	}
  	?b1 ex:arg ?q1 .
  	?b0 ex:arg ?b2 .
  	?b2 ex:type ex:union .
  	?b2 ex:arg ?q2 .
  	FILTER( ?b1 != ?b2 ) .
  } 