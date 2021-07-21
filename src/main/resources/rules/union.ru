PREFIX ex:  <http://example.org/>

DELETE { ?b0 ex:type ex:union }
WHERE
  { 
  	?b0 ex:type ex:union .
  	?b0 ex:arg ?w .
  	FILTER (!BOUND(?w))
  } 