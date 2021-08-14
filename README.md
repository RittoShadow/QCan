![QCan](http://qcan.dcc.uchile.cl/QCan/assets/images/qcanLogoSmall.png)

# QCan
Software for canonicalising SPARQL queries.

# Description
QCan is a free software for the canonicalisation of SPARQL queries.
This software works with queries under SPARQL ~~1.0~~ 1.1 syntax. 
Developed as part of a master's thesis. Extended as part of a doctorate.

#Main classes

In order to run any of these classes, you must first compile the project.

> mvn compile

You may also build a executable JAR using:

> mvn package

# Benchmark

This class computes the canonical form of each query in the input file, and outputs
runtimes for each step of the canonicalisation algorithm, as well as the features it contains,
the number of variables in it, the size of the r-graph, and the number of triple patterns.
Optionally, it also outputs a file containing every unique query as well as the number of
times it appears in the input file.
To run this software over a text file containing SPARQL queries (one per line) with Maven:

> java -jar qcan-1.1-jar-with-dependencies.jar benchmark -x filename -n numberOfQueries -o offset <options>

Options:
* -l Enables the minimisation/leaning of the monotone parts of a query.
* -c Enables canonical labeling.
* -r Enables the rewriting of well-designed sub-patterns, property paths into BGPs and unions where possible, and monotone parts of a query to UCQs (may involve an exponential blow-up).
* -d Set to output a file containing all distinct queries as well as how many duplicates are found.

For example:

> java -jar qcan-1.1-jar-with-dependencies.jar benchmark -x projection.txt -n -1 -d -c -p -r -l"

Includes an EasyCanonicalisation class that encapsulates everything so you can pass
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

# Demo

Live demo [here](http://qcan.dcc.uchile.cl)

![Demo](http://qcan.dcc.uchile.cl/QCan/assets/images/qcanWeb.png)

# License

Copyright 2018 Jaime Salas <jaime.os.salas@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
