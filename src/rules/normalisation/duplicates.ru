PREFIX ex:  <http://example.org/>

DELETE { ?w ex:arg ?b .
		?b ex:type ex:join .
		?b ex:arg ?q }
INSERT{	?w ex:arg ?q }

WHERE
  { 
  	?w ex:type ex:union .
  	?w ex:arg ?b .
  	?b ex:type ex:join .
  	?b ex:arg ?q .
  	{
	SELECT (COUNT(?x) AS ?t)
	WHERE{
		?w ex:arg ?x .
		?x ex:type ex:join .
		}
	}
  	FILTER(?t = 1)
  } 