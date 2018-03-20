PREFIX ex:  <http://example.org/>

DELETE { ?b0 ex:arg ?b1 .
		?b1 ?p ?o }
INSERT { ?b0 ex:arg ?q }
WHERE
  { 
  	?b0 ex:type ex:or .
  	{
	  	?b1 ex:type ex:or .
	  	?b1 ex:arg ?q .
	  	?b1 ?p ?o
  	}
  	?b0 ex:arg ?b1 .
  } 