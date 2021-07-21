PREFIX ex:  <http://example.org/>

DELETE { }
INSERT { ?b0 ex:leaf false }
WHERE{ 
	{ ?b0 ex:type ex:join } UNION { ?b0 ex:type ex:union } .
	?b0 ex:arg ?q .
  } 