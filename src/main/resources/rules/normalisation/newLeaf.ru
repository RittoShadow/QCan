PREFIX ex:  <http://example.org/>

DELETE { ?b0 ex:leaf false }
INSERT { ?b0 ex:leaf true }
WHERE{ 
	{ ?b0 ex:type ex:join } UNION { ?b0 ex:type ex:union } .
	?b0 ex:arg ?q .
	?q ex:leaf true .
	FILTER NOT EXISTS{
		?b0 ex:arg ?a .
		?a ex:leaf false .
		FILTER(?a != ?q)
	}
  } 