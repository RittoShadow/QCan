PREFIX ex:  <http://example.org/>

DELETE {
  ?u ex:arg ?tp .
  ?tp ?p ?y .
}
INSERT {
 ?x ?p ?y .
 ?u ex:arg ?x .
}
WHERE{
	{
	SELECT DISTINCT ?tp (BNODE() AS ?x)
	WHERE{
		?tp ex:type ex:TP .
		}
	}
	?u ex:arg ?tp .
	?tp ?p ?y .
}