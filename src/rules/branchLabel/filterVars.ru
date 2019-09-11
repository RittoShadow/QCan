PREFIX ex:  <http://example.org/>

INSERT { ?v ex:type ex:var . 
		?v ex:temp true . }
WHERE {
	{
	 ?n ex:value ?v .
	 FILTER(ISBLANK(?v))
 	}
 	UNION
 	{
 	 ?n ex:type ex:bind .
 	 ?n ex:var ?v .
 	 FILTER(ISBLANK(?v))
 	}
 	FILTER NOT EXISTS { ?v ex:function ?f }
}