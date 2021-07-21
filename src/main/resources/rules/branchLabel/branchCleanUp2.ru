PREFIX ex:  <http://example.org/> 

DELETE {
		?a ex:value ?v .
		?a ex:type ex:var .
		?b ex:temp false .
		?b ex:value ?v .
		?b ex:type ex:var .
		?a ex:temp true . }
WHERE {
 {
 ?b ex:type ex:var .
 ?b ex:temp false .
 ?b ex:value ?v .
 }
 UNION
 {
 ?a ex:type ex:var .
 ?a ex:temp true .
 ?a ex:value ?v .
 }
}