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

The UCQGeneratorTest class contains a main type that creates SPARQL queries that are "hard" to process. 
These queries contain n conjunctions of patterns containing m disjunctions.

> java UCQGeneratorTest conjunctions unions

###Generator

Generates queries based on graphs represented as edge lists. Files in eval/ contain edge lists.

The MultipleGenerator class contains a main type that creates SPARQL queries from graphs (contained in the eval folder). We create queries where nodes are variables connected by a fixed predicate.

> java MultipleGenerator outputFile timeout

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

Data structure used to represent queries. Contains the most important methods such as minimisation and canonical labeling.

###SingleQuery

Class constructor that encapsulates all the steps of the algorithm.

##Parsers

Includes classes that parse and normalise files that contain SPARQL queries according to different algorithms.

###JenaParser

This class normalises queries by transforming them into algebraic expressions provided by Apache Jena.

###NoParser

This class doesn't perform any normalisation techniques and simply outputs a partition of congruent queries. Used only to compare with other methods.

###QueryParser

This class normalises queries using our method of rewriting, minimisation, and canonical labeling. Flags can be set to turn on or off each step.

##Paths

##Rules

##Tests

##Tools

##Transformers

##Visitors