

![QCan](http://qcan.dcc.uchile.cl/QCan/assets/images/qcanLogoSmall.png)

# QCan
Software for canonicalising SPARQL queries.

# Description
QCan is a free software for the canonicalisation of SPARQL queries.
This software works with queries under SPARQL ~~1.0~~ 1.1 syntax. 
Developed as part of a master's thesis. Extended as part of a doctorate.

# Main classes

(Optionally) In order to install the local jars into the local maven repository:

> mvn clean

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

> java -jar qcan-1.1-jar-with-dependencies.jar benchmark -x projection.txt -n -1 -d -c -p -r -l

In order to run the same experiments presented in our study [link comming soon], unzip
the test files contained in QCanTestSuite.zip and run:

> java -jar qcan-1.1-jar-with-dependencies.jar benchmark -x _filename_ -n -1 -d -c -p -r -l

over each of the files. Results will be created in either resultFiles/label, resultFiles/rewrite or
resultFiles/full depending on which flags are set. Times are measured in nanoseconds.

>-c -> label

>-c -r -> rewrite

>-c -r -l -> full

# Analysis

This class is used to summarise the results of the experiments.

> java -jar qcan-1.1-jar-with-dependencies.jar analysis -x _filename_ -d

This will display the minimum, mean, maximum, etc, of each column in _filename_. This should look as follows: 

```
DBpedia	Average	Median	Q25	Q75	Max	Min
Graph time	1.37E+07	1.01E+07	7845588	1.62E+07	4.91E+07	6925551
Rewrite time	123560.6744	29760	17998	98978	1.57E+06	13566
Label time	1.10E+07	9529150	7460818	1.30E+07	2.43E+07	6635002
Minimisation	9.32E+06	4835472	4240106	6330848	1.30E+08	3302497
Total time	3.20E+07	2.58E+07	1.97E+07	3.58E+07	1.61E+08	1.72E+07
Triples	1.744186047	0	0	0	9	0
Variables	2.302325581	0	0	0	12	0
Graph size	21.79069767	5	5	5	105	5
```

In addition, it should show how many times each SPARQL features appears in the query set (once per query).

# Easy Canonicalisation

Includes an EasyCanonicalisation class that encapsulates everything so you can pass
a file containing all the queries you need to canonicalise, and output a text file
containing the canonical form of each query.

> java -jar qcan-1.1-jar-with-dependencies.jar easy -f fileContainingQueries -o outputFile

For example, assuming if we want to canonicalise and minimise a text file "queries.txt" and
output the results to "canonicalisedQueries.txt":

> java -jar qcan-1.1-jar-with-dependencies.jar easy -f queries.txt -o canonicalisedQueries.txt -m
    
If we want to do the same but remove duplicate queries in the output:
    
> java -jar qcan-1.1-jar-with-dependencies.jar easy -f queries.txt -o canonicalisedQueries.txt -m -d

If want to remove duplicate queries from a file based on their canonicalised version, but keep the original query syntax in the output:
    
> java -jar qcan-1.1-jar-with-dependencies.jar easy -f queries.txt -o canonicalisedQueries.txt -m -d -k

OR you can pass a single query as an argument:

> java -jar qcan-1.1-jar-with-dependencies.jar easy -q query

e.g.

> java -jar qcan-1.1-jar-with-dependencies.jar easy -q 'PREFIX : <http://example.org/> SELECT ?x WHERE { ?x :p ?y . ?x :q ?z . { SELECT ?y WHERE { ?b :p ?y . ?b :q ?c . } } } '


Options:
* -m to enable minimisation/leaning
* -d to remove duplicates based on canonicalised query
* -k to keep original query syntax in the output rather than outputting canonicalised queries (only makes sense with -d to remove duplicates; keeps the first non-duplicated query encountered).

### UCQ Generator Test

Generates queries that *enforce* an exponential rewriting to UCQs.

The UCQGeneratorTest class contains a main type that creates SPARQL queries that are "hard" to process. 
These queries contain n conjunctions of patterns containing m disjunctions.

> java -jar qcan-1.1-jar-with-dependencies ucq -c conjunctions -u unions

Result files will be created in the resultFiles/ucq folder. Times are measured in nanoseconds.

# How to include in your projects

One option to include this in your own (Maven) project is building the jar with dependencies
using 

> mvn clean package

Then add the jar to src/main/resources, and you should be done!

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
