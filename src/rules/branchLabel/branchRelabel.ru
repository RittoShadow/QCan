PREFIX ex:  <http://example.org/>

DELETE {
  ?tp ?p ?v .
}
INSERT {
 ?tp ?p ?x .
 ?n ex:modifier ?filter .
}
WHERE {
	{
	SELECT DISTINCT ?v ?xa ?x ?p
	WHERE{
	{
		SELECT DISTINCT ?v ?xa (BNODE() AS ?x)
		WHERE
		{
			{
				SELECT DISTINCT ?v ?xa
				WHERE{
				  { 
					  ?a ex:type ex:join ; ex:arg ?tp ; ex:cid ?xa .
					  ?tp ex:type ex:TP; ?p ?v .
					  FILTER(isBlank(?v))
				  }
				  UNION
				  { 
				  	?tp ex:type ex:TP ; ex:cid ?xa ; ?p ?v . 
				  	FILTER(isBlank(?v))
				  }
				  FILTER NOT EXISTS { 
					  {
						  ?proj ex:type ex:projection .
						  ?proj ex:arg ?v.
					  }
					  UNION
					  {
						  ?v ex:temp true .
						  
					  }
					  UNION
					  {
						  ?order ex:type ex:orderBy .
						  ?order ex:arg ?o .
						  ?o ex:var ?v .
					  }
				  }
			  }
			}
		}	
	 }
	?tp ex:type ex:TP .
	?tp ?p ?v .
  }
	}
	?tp ?p ?v .
	FILTER( ?p != ex:modifier )
	OPTIONAL{ ?tp ex:modifier ?filter . ?n ex:arg ?tp . }
}