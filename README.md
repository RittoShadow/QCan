

![QCan](http://qcan.dcc.uchile.cl/QCan/assets/images/qcanLogoSmall.png)

# QCan
Software for canonicalising SPARQL queries.

# Description
QCan is a free software for the canonicalisation of SPARQL queries.
This software works with queries under SPARQL ~~1.0~~ 1.1 syntax. 
Developed as part of a master's thesis. Extended as part of a doctorate.

# Benchmark

To run this software over a text file containing SPARQL queries (one per line) execute:

> java Benchmark -x filename -n numberOfQueries -o offset

Options:
* -l to enable the minimisation of monotone parts of a query.
* -c to enable canonical labeling.
* -r to enable the rewriting of monotone parts of a query to UCQs (may involve an exponential blow-up).
* -d to output a file containing all distinct queries as well as how many duplicates are found.

The MultipleGenerator class contains a main type that creates SPARQL queries from graphs (contained in the eval folder). We create queries where nodes are variables connected by a fixed predicate.

> java MultipleGenerator outputFile timeout

The UCQGeneratorTest class contains a main type that creates SPARQL queries that are "hard" to process. 
These queries contain n conjunctions of patterns containing m disjunctions.

> java UCQGeneratorTest conjunctions unions

Includes an EasyCanonicalisation class 

> java EasyCanonicalisation -f fileContainingQueries -o outputFile

OR

> java EasyCanonicalisation -q query

Options:
* -m to enable minimisation/leaning

# Demo

Live demo [here](qcan.dcc.uchile.cl)

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
