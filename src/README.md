#Summary

##Builder

Contains classes that build data structures.

###QueryBuilder

Builds a query from an r-graph. Used to serialise canonical graphs.

###RGraphBuilder

Builds an r-graph from a query. Implements Apache Jena's OpVisitor class.

##Data

Contains classes used to analyse data. Mostly for research purposes only.

##Generate

Contains classes used to generate queries for testing purposes.

###UCQGenerator

Generates queries that *enforce* an exponential rewriting to UCQs.

###Generator

Generates queries based on graphs represented as edge lists. Files in eval/ contain edge lists.

##Main

###Benchmark

To run this software over a text file containing SPARQL queries (one per line) execute:

> java Benchmark -x filename -n numberOfQueries -o offset

Options:
* -l to enable the minimisation of monotone parts of a query.
* -c to enable canonical labeling.
* -r to enable the rewriting of monotone parts of a query to UCQs (may involve an exponential blow-up).
* -d to output a file containing all distinct queries as well as how many duplicates are found.

###EasyCanonicalisation

Includes an EasyCanonicalisation class 

> java EasyCanonicalisation -f fileContainingQueries -o outputFile

OR

> java EasyCanonicalisation -q query

Options:
* -m to enable minimisation/leaning

###RGraph



###SingleQuery

##Parsers

##Paths

##Rules

##Tests

##Tools

##Transformers

##Visitors