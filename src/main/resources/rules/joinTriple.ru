PREFIX ex:  <http://example.org/>

DELETE { ?w ex:arg ?b .
		?w ex:type ex:join .
		?y ?o ?w . }
INSERT{	?y ?o ?b }

WHERE
  {
        ?w ex:type ex:join .
        ?w ex:arg ?b .
        FILTER NOT EXISTS
  	{
	SELECT (COUNT(?x) AS ?t)
	WHERE{
		?w ex:arg ?x .
		?x ex:type ex:TP .
		}
	}
  	FILTER(?t = 1)
  } 