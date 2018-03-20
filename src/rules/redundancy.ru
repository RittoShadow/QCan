PREFIX ex:  <http://example.org/>

DELETE { ?w ex:arg ?a .
		?w ex:mod ?f .
		?w ex:type ?type .
		?b0 ex:arg ?w . }
INSERT {
	?b0 ex:arg ?a .
	?b0 ex:mod ?f .
	}
WHERE
  { 
  	?b0 ex:type ?type .
  	FILTER(?type = ex:join || ?type = ex:union)
  	?b0 ex:arg ?w .
  	?w ex:type ?type .
  	FILTER NOT EXISTS{
  		?w ex:type ?t
  		FILTER(?type != ?t)
  	}
  	?w ex:arg ?a .
  	OPTIONAL{ ?w ex:mod ?f }
  } 