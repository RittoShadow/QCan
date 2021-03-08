PREFIX ex:  <http://example.org/>
DELETE {
        ?x ex:type ex:var .
        ?x ex:id ?id .
        ?s ?p ?x .
    }
INSERT {
        ?b ex:type ex:var .
        ?s ?p ?b .
    }
WHERE
    {
        ?s ?p ?x .
        ?x ex:type ex:var .
        ?x ex:id ?id .
        BIND (BNODE() AS ?b)
    }