PREFIX ex:  <http://example.org/>

DELETE{
	?b0 ex:type ex:join .
  	?b0 ex:arg ?b1 .
  	?b1 ex:type ex:union .
  	?b1 ex:arg ?q1 .
  	?b0 ex:arg ?b2 .
  	?b2 ex:type ex:union .
  	?b2 ex:arg ?q2 .
}
INSERT{
	_:j ex:type ex:join .
	_:j ex:arg ?q1 .
	_:j ex:arg ?q2 .
	?b1 ex:arg _:j .
	?b1 ex:type ex:union .
	?b ex:arg _:u .
}
WHERE
  { 
  	?b0 ex:type ex:join .
  	?b0 ex:arg ?b1 .
  	{
  	SELECT ?b1
  		WHERE{ ?b1 ex:type ex:union }
  		LIMIT 1
  	}
  	?b1 ex:arg ?q1 .
  	?b0 ex:arg ?b2 .
  	?b2 ex:type ex:union .
  	?b2 ex:arg ?q2 .
  	OPTIONAL { ?b ex:arg ?b0 } .
  	FILTER( ?b1 != ?b2 ) .
  } 