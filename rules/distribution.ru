PREFIX ex:  <http://example.org/>

DELETE{
	?b ex:arg ?b0 .
	?b0 ex:type ex:join .
  	?b0 ex:arg ?b1 .
  	?b1 ex:type ex:union .
  	?b1 ex:arg ?q1 .
  	?b0 ex:arg ?b2 .
}
INSERT{
	_:j ex:type ex:join .
	_:j ex:arg ?q1 .
	_:j ex:arg ?b2 .
	?b1 ex:arg _:j .
	?b1 ex:type ex:union .
	?b ex:arg ?b1 .
}
WHERE
  { 
  	OPTIONAL {?b ex:arg ?b0} .
  	?b0 ex:type ex:join .
  	?b0 ex:arg ?b1 .
  	?b1 ex:type ex:union .
  	?b1 ex:arg ?q1 .
  	?b0 ex:arg ?b2 .
  	FILTER( ?b1 != ?b2 ) .
  	FILTER NOT EXISTS{
  		?b0 ex:arg ?v .
  		?v ex:type ex:union .
  		FILTER(?v != ?b1)
  	}
  } 