PREFIX ex:  <http://example.org/>

INSERT { ?v ex:temp true . }
WHERE {
	{
	 ?sub ?predicate ?n .
	 ?n ex:value ?v .
	 ?v ex:type ex:var .
	 FILTER(ISBLANK(?v))
 	}
 	UNION
 	{
 	 ?sub ?predicate ?n .
 	 ?n ex:type ex:binding .
 	 ?n ex:var ?v .
 	 FILTER(ISBLANK(?v))
 	}
 	FILTER NOT EXISTS { ?v ex:function ?f }
}