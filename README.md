# QCan
Software for canonicalising SPARQL queries.

# Description
QCan is a free software for the canonicalisation of SPARQL queries.
This software works with queries under SPARQL 1.0 syntax. 
Developed as part of a master's thesis.

# Benchmark

To run this software over a text file containing SPARQL queries (one per line) execute:

> java Benchmark filename numberOfQueries

Options:
* -l to enable the leaning of graphs.
* -o to enable the processing of queries containing OPTIONAL clauses.
* -f to enable the processing of queries containing FILTER expressions.

The MultipleGenerator class contains a main type that creates SPARQL queries from graphs (contained in the eval folder). We create queries where nodes are variables connected by a fixed predicate.

> java MultipleGenerator outputFile timeout

The UCQGeneratorTest class contains a main type that creates SPARQL queries that are "hard" to process. 
These queries contain n conjunctions of patterns containing m disjunctions.

> java UCQGeneratorTest conjunctions unions


# License

Copyright 2018 Jaime Salas <rittoshadow@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
