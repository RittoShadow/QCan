# Summary

## Builder

Contains classes that build data structures.

### QueryBuilder

Builds a query from an r-graph. Used to serialise canonical graphs.

### RGraphBuilder

Builds an r-graph from a query. Implements Apache Jena's OpVisitor class.

## Data

Contains classes used to analyse data. Mostly for research purposes only.

## Generate

Contains classes used to generate queries for testing purposes.

### UCQGenerator

Generates queries that *enforce* an exponential rewriting to UCQs.

The UCQGeneratorTest class contains a main type that creates SPARQL queries that are "hard" to process. 
These queries contain n conjunctions of patterns containing m disjunctions.

> java -jar qcan-1.1-jar-with-dependencies ucq -c conjunctions -u unions

### Generator

Generates queries based on graphs represented as edge lists. Files in eval/ contain edge lists.

The MultipleGenerator class contains a main type that creates SPARQL queries from graphs (contained in the eval folder). We create queries where nodes are variables connected by a fixed predicate.

> java -jar qcan-1.1-jar-with-dependencies.jar multi -x folder -t timeout

For example, assuming we want to use the files in /eval/k and set a timeout of 10 seconds:

> java -jar qcan-1.1-jar-with-dependencies.jar multi -x /eval/k -t 10000"

## Main

### Benchmark

To run this software over a text file containing SPARQL queries (one per line) execute:

> java -jar qcan-1.1-jar-with-dependencies.jar benchmark -x filename -n numberOfQueries -o offset <options>

Options:
* -l to enable the minimisation of monotone parts of a query.
* -c to enable canonical labeling.
* -r to enable the rewriting of monotone parts of a query to UCQs (may involve an exponential blow-up).
* -d to output a file containing all distinct queries as well as how many duplicates are found.

### EasyCanonicalisation

The EasyCanonicalisation class encapsulates everything so you can pass
a file containing all the queries you need to canonicalise, and output a text file
containing the canonical form of each query.

> java -jar qcan-1.1-jar-with-dependencies.jar easy -f fileContainingQueries -o outputFile"

For example, assuming if we want to canonicalise and minimise a text file "queries.txt" and
output the results to "canonicalisedQueries.txt":

> java -jar qcan-1.1-jar-with-dependencies.jar easy -f queries.txt -o canonicalisedQueries.txt -m"

OR you can pass a single query as an argument:

> java -jar qcan-1.1-jar-with-dependencies.jar easy -q query

e.g.

> java -jar qcan-1.1-jar-with-dependencies.jar easy -q 'PREFIX : <http://example.org/> SELECT ?x WHERE { ?x :p ?y . ?x :q ?z . { SELECT ?y WHERE { ?b :p ?y . ?b :q ?c . } } } ' "


Options:
* -m to enable minimisation/leaning


### RGraph

Data structure used to represent queries. Contains the most important methods such as minimisation and canonical labeling.

### SingleQuery

Class constructor that encapsulates all the steps of the algorithm.

## Parsers

Includes classes that parse and normalise files that contain SPARQL queries according to different algorithms.

### JenaParser

This class normalises queries by transforming them into algebraic expressions provided by Apache Jena.

### NoParser

This class doesn't perform any normalisation techniques and simply outputs a partition of congruent queries. Used only to compare with other methods.

### QueryParser

This class normalises queries using our method of rewriting, minimisation, and canonical labeling. Flags can be set to turn on or off each step.

## Paths

Includes classes that deal with property paths. At this point in time, most of these
classes are experimental.

## Tools

Includes miscellaneous classes that are used elsewhere. CommonNodes, for instance,
contains static instances of nodes that are commonly used in r-graphs.

## Transformers

## Visitors