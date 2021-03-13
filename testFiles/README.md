# Test files

We provide a description of the queries used to test this software.

## Aggregation tests

3
```
PREFIX : <http://example.com/data/#> 
SELECT ?g (AVG(?p) AS ?avg) ((MIN(?p) + MAX(?p)) / 2 AS ?c) 
WHERE {  ?g :p ?p . } GROUP BY ?g
```
4
```
PREFIX : <http://example.com/data/#> 
SELECT ?g (AVG(?p) AS ?avg) ((MIN(?p) + MAX(?p)) / 2 AS ?c) 
WHERE {  ?g :p ?p . } GROUP BY ?g HAVING(AVG(?p) > 5)
```

These queries must not be congruent because of the HAVING expression.

5
```
PREFIX : <http://data.example/> 
SELECT (AVG(?size) AS ?asize) 
WHERE {  ?x :size ?size } GROUP BY ?x HAVING(AVG(?size) > 10)
```

6
```
PREFIX : <http://data.example/> 
SELECT DISTINCT (AVG(?size) AS ?asize) 
WHERE {
    {  ?x :size ?size } 
    UNION 
    { ?x ?y ?size }
} GROUP BY ?x HAVING(AVG(?size) > 10)
```

7
```
PREFIX : <http://data.example/> 
SELECT DISTINCT (AVG(?size) AS ?asize) 
WHERE { ?x ?y ?size } GROUP BY ?x HAVING(AVG(?size) > 10)
```

Query 5 is not congruent to 6 and 7 because it lacks the DISTINCT keyword. Queries 6 and 7 are congruent after query 6 is minimised.

8
```
SELECT DISTINCT (COUNT(?s) AS ?x) 
WHERE { ?s ?p ?o }
```

9
```
SELECT 
(COUNT(DISTINCT ?s) AS ?x) WHERE { ?s ?p ?o }
```

DISTINCT COUNT is not the same as COUNT DISTINCT.

## Branch Relabel tests

1
```
PREFIX : <http://example.org/> 
SELECT DISTINCT ?z 
WHERE{ 
    {?w :mother ?x . } 
    UNION 
    {?w :father ?x . 
     ?x :sister ?y .} 
    UNION 
    {?c :mother ?d .
     ?d :sister ?y .}
    ?d ?p ?e .
    ?e :name ?f .
    ?x :sister ?y .
    ?y :name ?z .}
```
2
```
PREFIX : <http://example.org/> 
SELECT DISTINCT ?n 
WHERE{ 
    {?a :name ?n .
     ?c :mother ?m .
     ?m :sister ?a .}
    UNION 
    {?a :name ?n .
     ?c :father ?f .
     ?f :sister ?a .}
 }
```
3
```
PREFIX : <http://example.org/> 
SELECT DISTINCT ?n 
WHERE{ 
    {?a :name ?n .
     ?c :mother ?m .
     ?m :sister ?a .}
     UNION 
    {?a :name ?n .
     ?c :father ?m .
     ?m :sister ?a .}
 }
```

Queries 1, 2 and 3 are congruent. In particular, query 1 contains redundant triples and redundant CQs after rewriting. In query 3,
despite the fact that both CQs contain the same variable names, unlike query 2, because they are not projected, they are not related and
can be renamed without affecting the results.

4
```
SELECT DISTINCT ?a 
WHERE { 
    { ?a <http://example.org/p> ?z } 
    UNION 
    { ?a ?y ?z }
}
```
5
```
SELECT DISTINCT ?x 
WHERE { ?x ?y ?z .}
```

Queries 4 and 5 are congruent after minimisation. 

6
```
SELECT DISTINCT ?x 
WHERE { 
    { ?x <http://example.org/p> ?y } 
    UNION 
    { ?x <http://example.org/q> ?z }
}
```
7
```
SELECT DISTINCT ?x 
WHERE { 
    { ?x <http://example.org/p> ?y } 
    UNION 
    { ?x <http://example.org/q> ?y }
}
```

Queries 6 and 7 are congruent because variable ?y is renamed because it appears in both CQs but is not projected.

## Filter Pushing tests

These queries are used to test the rewriting of filter expressions (filter pushing)

1
```
SELECT ?s WHERE {  { ?s ?p ?o . } OPTIONAL { ?o ?p ?s }  FILTER(?s>?o) } # 1 == 2
```
2
```
SELECT ?s WHERE {  { ?s ?p ?o . FILTER(?s>?o) } OPTIONAL { ?o ?p ?s } }
```
Queries 1 and 2 are equivalent because the variables are always bound in the left and right side of the OPTIONAL expression.

3
```
PREFIX : <http://example.org/> SELECT DISTINCT ?x ?y ?z WHERE {  { ?x :sibling ?y }   OPTIONAL { ?x :twin ?z FILTER(?x != ?z)  } } # 3 != 4
```
4
```
PREFIX : <http://example.org/> SELECT DISTINCT ?x ?y ?z WHERE {  { ?x :sibling ?y }  OPTIONAL { ?x :twin ?z }  FILTER(?x != ?z) }
```
Queries 3 and 4 are not equivalent because the filter expression in query 3 is only applied to the right side of the OPTIONAL pattern (i.e. the optional part).

5
```
PREFIX : <http://example.org/> SELECT DISTINCT ?x ?y ?z WHERE {  { ?x :sibling ?y }   OPTIONAL { ?x :twin ?z FILTER(?x != ?z)  }} # 5 != 6
```
6
```
PREFIX : <http://example.org/> SELECT DISTINCT ?x ?y ?z WHERE {  { ?x :sibling ?y }   OPTIONAL { ?x :twin ?z }}
```

Queries 5 and 6 are not equivalent because of the filter expression in query 5.

7
```
SELECT ?s WHERE {  { ?s ?p ?o . } UNION { ?o ?p ?s }  FILTER(?s>?o) }  # 7 == 8
```
8
```
SELECT ?s WHERE {  { ?s ?p ?o . FILTER(?s>?o) } UNION { ?o ?p ?s . FILTER(?s>?o) }  }
```

Queries 7 and 8 are equivalent because in query 8 both filter expressions are the same, all variables are projected, and contain "safe variables".

10
```
SELECT DISTINCT ?s WHERE { { ?s <http://ex.org/a> ?o } UNION { ?z <p> ?y FILTER(?z > 10) } . { ?s <http://ex.org/b> ?o } }
```
11
```
SELECT DISTINCT ?s WHERE { { ?s <http://ex.org/a> ?o } UNION { ?s <p> ?y FILTER(?s > 10) } . { ?s <http://ex.org/b> ?o } }
```

Queries 10 and 11 are not equivalent because query 10 contains variable ?z in the filter expression, which is not projected.

12
```
SELECT DISTINCT ?s WHERE { { { ?s <http://ex.org/a> ?o } UNION { ?z <p> ?y } FILTER(?z > 10) } . { ?s <http://ex.org/b> ?o } }
```
13
```
SELECT DISTINCT ?s WHERE { { { ?s <http://ex.org/a> ?o } UNION { ?s <p> ?y } FILTER(?s > 10) } . { ?s <http://ex.org/b> ?o } }
```

Same as 10 and 11, but the filter expression is pushed outside the CQ.

## Filter tests

1
````
SELECT * WHERE { ?s <http://ex.org/p> ?o1 . ?s <http://ex.org/q> ?o2 . FILTER((?o1 < 500 && ?o2 != 1000 && REGEX(?o1,"^a"))) }
````
2
````
SELECT * WHERE { ?s <http://ex.org/q> ?a . ?s <http://ex.org/p> ?b . FILTER(REGEX(?b,"^a") && ?a != 1000 && ?b < 500) }
````
3
````
SELECT * WHERE { ?s <http://ex.org/p> ?o1 . ?s <http://ex.org/q> ?o2 . FILTER(1000 != ?o2) FILTER(?o1 < 500 && REGEX(?o1,"^a")) }
````
4
````
SELECT * WHERE { ?s <http://ex.org/p> ?o1 . ?s <http://ex.org/p> ?o2 . FILTER((?o1 < 500 && ?o2 != 1000) || REGEX(?o1,"^a")) }
````
5
````
SELECT * WHERE { ?s <http://ex.org/p> ?a . ?s <http://ex.org/p> ?b . FILTER(REGEX(?b,"^a") || (?a != 1000 && ?b < 500)) }
````

Queries 1, 2 and 3 are congruent because logical and is commutative, and only differ in variable names. Query 4 is not congruent to the others because there is
a logical OR instead of AND. Queries 4 and 5 are congruent because logical OR is also commutative.

10
````
SELECT DISTINCT ?s 
WHERE { 
    { ?s <p> ?o } 
    UNION 
    { ?s <q> ?o }
    FILTER(?o < 15)}
````
11
````
SELECT DISTINCT ?r WHERE { { ?r <p> ?o } UNION { ?r <q> ?o } FILTER(?o < 15)}
````
12
````
SELECT DISTINCT ?r WHERE { { ?r <p> ?w } UNION { ?r <q> ?y } FILTER(?z < 15)}
````

Queries 10 and 11 are congruent whereas query 12 is not congruent to the others because variable ?z does not appear elsewhere.

15
````
PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>  
PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>  
SELECT DISTINCT ?l ?lat ?lng  
WHERE {   ?l geo:lat ?lat; geo:long ?lng; a <http://dbpedia.org/ontology/Place>.   
    FILTER(((?lat - xsd:float(44.849998))*(?lat - xsd:float(44.849998))+(?lng - xsd:float(7.716667))*(?lng - xsd:float(7.716667)))<xsd:float(1.0)) }
````
16
````
SELECT DISTINCT  ?v1 ?v0 ?v2 
WHERE  { ?v0 a <http://dbpedia.org/ontology/Place> ;
             <http://www.w3.org/2003/01/geo/wgs84_pos#lat>  ?v1 ;
             <http://www.w3.org/2003/01/geo/wgs84_pos#long>  ?v2    
        FILTER ( ( ( ( ?v2 - <http://www.w3.org/2001/XMLSchema#float>(7.716667) ) * ( ?v2 - <http://www.w3.org/2001/XMLSchema#float>(7.716667) ) ) + ( ( ?v1 - <http://www.w3.org/2001/XMLSchema#float>(44.849998) ) * ( ?v1 - <http://www.w3.org/2001/XMLSchema#float>(44.849998) ) ) ) < <http://www.w3.org/2001/XMLSchema#float>(1.0) )  }
````

Query 15 was a query in the linked Geo dataset, while query 16 is the result of the canonicalisation process.

17
````
PREFIX  rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
PREFIX  foaf:   <http://xmlns.com/foaf/0.1/> 
SELECT ?person 
WHERE {    ?person rdf:type  foaf:Person .
        FILTER NOT EXISTS { ?person foaf:name ?name } }
````
18
````
PREFIX  rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
PREFIX  foaf:   <http://xmlns.com/foaf/0.1/> 
SELECT ?person 
WHERE {    ?person rdf:type  foaf:Person .    
        FILTER EXISTS { ?person foaf:name ?name } }
````

Queries 17 and 18 contain FILTER NOT EXISTS and FILTER EXISTS, respectively. Evidently, they are not congruent or equivalent. 

