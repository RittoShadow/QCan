PREFIX ex:  <http://example.org/>

DELETE { ?w ex:arg ?a .
		?w ex:pattern ?f .
		?w ex:type ex:union .
		?b0 ex:arg ?w . }
INSERT {
	?b0 ex:arg ?a .
	?b0 ex:pattern ?f .
	}
WHERE
  { 
	  {
	  SELECT ?b0
	  WHERE
		  {
		  	?b0 ex:type ex:union .
		  	?b0 ex:arg ?w .
		  	?w ex:type ex:union .
		  	FILTER NOT EXISTS{
		  		?b0 ex:arg ?o .
		  		?o ex:type ?t .
		  		FILTER(?t != ex:union)
		  	}
		  	OPTIONAL{ ?w ex:pattern ?f }
		  } LIMIT 1
	  }
	  ?b0 ex:arg ?w .
	  ?w ex:arg ?a .
  }