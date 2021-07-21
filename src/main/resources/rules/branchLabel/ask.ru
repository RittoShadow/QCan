PREFIX ex:  <http://example.org/> 

DELETE {
		?p ex:OP ?u .
		?u ex:type ex:union .
		?u ex:arg ?t .
		?u ex:arg ?j .
		?t ex:cid ?tid .
		?j ex:cid ?cid .
		?j ex:type ex:join .
		?j ex:arg ?tp . }
WHERE {
	?p ex:type ex:projection .
	?p ex:OP ?u .
	?u ex:type ex:union .	
	OPTIONAL
	{
		?u ex:arg ?t .
		?t ex:type ex:TP .
		?t ex:cid ?tid .
	}
	OPTIONAL
	{
		?u ex:arg ?j .
		?j ex:type ex:join .
		?j ex:arg ?tp .
		?j ex:cid ?cid .
	}
}