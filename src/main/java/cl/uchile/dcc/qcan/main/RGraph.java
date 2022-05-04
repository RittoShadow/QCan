package cl.uchile.dcc.qcan.main;

import cl.uchile.dcc.blabel.jena.JenaModelIterator;
import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.blabel.label.GraphLabelling;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingArgs;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingResult;
import cl.uchile.dcc.blabel.lean.DFSGraphLeaning;
import cl.uchile.dcc.blabel.lean.GraphLeaning.GraphLeaningResult;
import cl.uchile.dcc.qcan.paths.PGraph;
import cl.uchile.dcc.qcan.tools.CustomTripleBoundary;
import cl.uchile.dcc.qcan.visitors.FilterVisitor;
import com.google.common.hash.HashCode;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ext.com.google.common.collect.BiMap;
import org.apache.jena.ext.com.google.common.collect.HashBiMap;
import org.apache.jena.graph.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.walker.Walker;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.sse.writers.WriterPath;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.yars.nx.BNode;

import java.util.*;
import java.util.regex.Pattern;

import static cl.uchile.dcc.qcan.tools.CommonNodes.*;
import static cl.uchile.dcc.qcan.tools.Utils.*;
import static org.apache.jena.graph.GraphUtil.listObjects;
import static org.apache.jena.graph.GraphUtil.listSubjects;

/**
 * @author Jaime
 */
public class RGraph {

    //private static final UpdateRequest conjunctionRule = UpdateFactory.read(Objects.requireNonNull(RGraph.class.getClassLoader().getResourceAsStream("normalisation/conjunction.ru")));
    //private static final UpdateRequest disjunctionRule = UpdateFactory.read(Objects.requireNonNull(RGraph.class.getClassLoader().getResourceAsStream("normalisation/disjunction.ru")));
    //private static final UpdateRequest joinRule = UpdateFactory.read(Objects.requireNonNull(RGraph.class.getClassLoader().getResourceAsStream("join.ru")));
    //private final UpdateRequest branchCleanUpRule = UpdateFactory.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("branchLabel/cleanUp.ru")));
    //private static final UpdateRequest branchUnionRule = UpdateFactory.read(Objects.requireNonNull(RGraph.class.getClassLoader().getResourceAsStream("branchLabel/branchUnion.ru")));
    //private static final UpdateRequest filterVarsRule = UpdateFactory.read(Objects.requireNonNull(RGraph.class.getClassLoader().getResourceAsStream("branchLabel/filterVars.ru")));
    public Graph graph = GraphFactory.createDefaultGraph();
    public Set<Var> vars = new HashSet<>();
    public int nTriples;
    public int id = 0;
    public Node root;
    public boolean distinct = false;
    public boolean leaning = true;
    public boolean canonical = true;
    public boolean containsPaths = false;
    public BiMap<Node, Node> varMap = HashBiMap.create();
    public Map<Var, Node> exprMap = new HashMap<>();
    public Map<Node, Node> typeMap = new HashMap<>();
    private long labelTime = 0;
    private long minimisationTime = 0;
    private HashCode hashCode = null;


    /**
     * @param triples List of RDF triples.
     * @param vars    List of the variables in the triples.
     */
    public RGraph(List<Triple> triples, List<Var> vars) {
        if (vars != null) {
            this.vars.addAll(vars);
        }
        nTriples = triples.size();
        if (triples.size() > 1) {
            this.root = NodeFactory.createBlankNode();
            //Adding typing now.
            graph.add(Triple.create(this.root, typeNode, joinNode));
            typeMap.put(this.root, joinNode);
        }
        for (Triple t : triples) {
            Node n = NodeFactory.createBlankNode();
            if (triples.size() == 1) {
                this.root = n;
            } else {
                graph.add(Triple.create(root, argNode, n));
            }
            graph.add(Triple.create(n, typeNode, tpNode));
            typeMap.put(n, tpNode);
            // We create a blank node if we have a variable. If it's a literal, we add a literal node, and so on.
            Triple s = Triple.create(n, subjectNode, getValidNode(t.getSubject()));
            Triple p = Triple.create(n, preNode, getValidNode(t.getPredicate()));
            Triple o = Triple.create(n, objNode, getValidNode(t.getObject()));
            graph.add(s);
            graph.add(p);
            graph.add(o);
        }
    }

	/**
	 * @param triples List of RDF triples.
	 */
	public RGraph(List<Triple> triples) {
		this(triples, new ArrayList<>());
	}

    /**
     * @param root  This node will be the new root of the r-graph.
     * @param graph The graph on which the new r-graph is based.
     * @param vars  The list of variables in the r-graph.
     */
    public RGraph(Node root, Graph graph, Collection<Var> vars) {
        this.root = root;
        this.graph = graph;
        if (vars != null) {
            this.vars.addAll(vars);
        }
    }

    /**
     * @param data A collection of triples of nodes.
     */
    public RGraph(Collection<org.semanticweb.yars.nx.Node[]> data) {
        Set<Node> rootCandidates = new HashSet<>();
        Set<Node> predicates = new HashSet<>();
        Set<Node> objects = new HashSet<>();
        for (org.semanticweb.yars.nx.Node[] node : data) {
            Node subject;
            Node predicate;
            Node object;
            subject = nxToJenaNode(node[0]);
            predicate = nxToJenaNode(node[1]);
            object = nxToJenaNode(node[2]);
            if (subject != null) {
                graph.add(Triple.create(subject, predicate, object));
                rootCandidates.add(subject);
                predicates.add(predicate);
                objects.add(object);
                if (predicate.equals(typeNode)) {
                    typeMap.put(subject, object);
                }
            } else {
                System.err.println("Invalid blank node label.");
            }
        }
        for (Node p : predicates) {
            rootCandidates.remove(p);
        }
        for (Node o : objects) {
            rootCandidates.remove(o);
        }
        ExtendedIterator<Node> tempNodes = GraphUtil.listSubjects(graph, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
        while (tempNodes.hasNext()) {
            Node tempNode = tempNodes.next();
            rootCandidates.remove(tempNode);
        }
        if (rootCandidates.size() == 1) {
            this.root = (Node) rootCandidates.toArray()[0];
        } else {
            for (Node n : rootCandidates) {
                if (typeMap.containsKey(n)) {
                    if (typeMap.get(n).equals(projectNode)) {
                        this.root = n;
                    }
                }
            }
        }
        if (this.root == null) {
            System.err.println("Something went wrong.");
            System.err.println(this);
            System.err.println("Root candidates: ");
            for (Node n : rootCandidates) {
                System.err.println(n);
            }
        }
    }

    /**
     * @param s The subject of this path pattern.
     * @param o The object of this path pattern.
     * @param p The graph representation of this property path.
     */
    public RGraph(Node s, Node o, PGraph p) {
        Set<Var> vars = new HashSet<>();
        Graph graph = GraphFactory.createPlainGraph();
        Node tp = NodeFactory.createBlankNode();
        graph.add(Triple.create(tp, typeNode, triplePathNode));
        typeMap.put(tp, triplePathNode);
        if (s.isVariable()) {
            vars.add(Var.alloc(s));
            graph.add(Triple.create(tp, subjectNode, NodeFactory.createBlankNode(s.getName())));
        } else if (s.isBlank()) {
            graph.add(Triple.create(tp, subjectNode, NodeFactory.createBlankNode(s.getBlankNodeLabel())));
        } else if (s.isURI()) {
            graph.add(Triple.create(tp, subjectNode, NodeFactory.createURI(s.getURI())));
        } else if (s.isLiteral()) {
            graph.add(Triple.create(tp, subjectNode, NodeFactory.createLiteralByValue(s.getLiteralValue(), s.getLiteralDatatype())));
        }
        if (o.isVariable()) {
            vars.add(Var.alloc(o));
            graph.add(Triple.create(tp, objNode, NodeFactory.createBlankNode(o.getName())));
        } else if (o.isBlank()) {
            graph.add(Triple.create(tp, objNode, NodeFactory.createBlankNode(o.getBlankNodeLabel())));
        } else if (o.isURI()) {
            graph.add(Triple.create(tp, objNode, NodeFactory.createURI(o.getURI())));
        } else if (o.isLiteral()) {
            graph.add(Triple.create(tp, objNode, NodeFactory.createLiteralByValue(o.getLiteralValue(), o.getLiteralDatatype())));
        }
        GraphUtil.addInto(graph, p.getMinimalDFA());
        Node n = NodeFactory.createBlankNode();
        graph.add(Triple.create(n, typeNode, pathNode));
        typeMap.put(n, pathNode);
        graph.add(Triple.create(n, argNode, p.getStartState()));
        graph.add(Triple.create(tp, preNode, n));
        this.graph = graph;
        this.vars = vars;
        this.root = tp;
    }

    /**
     * @param s The subject of this path pattern.
     * @param o The object of this path pattern.
     * @param p The property path of this path pattern.
     */
    public RGraph(Node s, Node o, Path p) {
        Set<Var> vars = new HashSet<>();
        Graph graph = GraphFactory.createPlainGraph();
        Node tp = NodeFactory.createBlankNode();
        graph.add(Triple.create(tp, typeNode, triplePathNode));
        typeMap.put(tp, triplePathNode);
        if (s.isVariable()) {
            vars.add(Var.alloc(s));
        }
        graph.add(Triple.create(tp, subjectNode, getValidNode(s)));
        if (o.isVariable()) {
            vars.add(Var.alloc(o));
        }
        graph.add(Triple.create(tp, objNode, getValidNode(o)));
        Node predicate = NodeFactory.createLiteral(WriterPath.asString(p));
        graph.add(Triple.create(tp, preNode, predicate));
        this.graph = graph;
        this.vars = vars;
        this.root = tp;
    }

    /**
     * @param table A table containing explicit values.
     */
    public RGraph (Table table) {
        this(NodeFactory.createBlankNode(),GraphFactory.createDefaultGraph(),new HashSet<>());
        graph.add(Triple.create(root, typeNode, tableNode));
        typeMap.put(root, tableNode);
        Iterator<Binding> iter = table.rows();
        while (iter.hasNext()) {
            Node rowNode = NodeFactory.createBlankNode();
            graph.add(Triple.create(root, argNode, rowNode));
            Binding b = iter.next();
            Iterator<Var> vars = b.vars();
            while (vars.hasNext()) {
                Var var = vars.next();
                Node bindingNode = NodeFactory.createBlankNode();
                graph.add(Triple.create(rowNode, argNode, bindingNode));
                graph.add(Triple.create(bindingNode, typeNode, bindNode));
                typeMap.put(bindingNode, bindNode);
                graph.add(Triple.create(bindingNode, varNode, NodeFactory.createBlankNode(var.getVarName())));
                Node value = b.get(var);
                graph.add(Triple.create(bindingNode, valueNode, value));
            }
        }
    }

    public RGraph (OpGroup arg) {
        this(NodeFactory.createBlankNode(),GraphFactory.createDefaultGraph(),new HashSet<>());
        graph.add(Triple.create(root, typeNode, groupByNode));
        typeMap.put(root, groupByNode);
        Node varNode = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, argNode, varNode));
        VarExprList vExpr = arg.getGroupVars();
        for (Var v : vExpr.getVars()) {
            graph.add(Triple.create(varNode, valueNode, NodeFactory.createBlankNode(v.getVarName())));
        }
        Map<Var, Expr> varsExpr = vExpr.getExprs();
        for (Map.Entry<Var, Expr> m : varsExpr.entrySet()) {
            Var v = m.getKey();
            FilterVisitor fv = new FilterVisitor();
            Node vNode = NodeFactory.createBlankNode();
            exprMap.put(v, vNode);
            Walker.walk(m.getValue(), fv);
            Node r = fv.getGraph().root;
            GraphUtil.addInto(graph, fv.getGraph().graph);
            graph.add(Triple.create(vNode, valueNode, r));
        }
    }

    public RGraph() {
        this(NodeFactory.createBlankNode(),GraphFactory.createDefaultGraph(),new HashSet<>());
        graph.add(Triple.create(root,typeNode,nullNode));
        typeMap.put(root,nullNode);
    }

    /**
     * Creates an r-graph that represents the projection of variables (SELECT vars) and adds it to this r-graph.
     *
     * @param vars A collection of projected variables.
     */
    public void project(Collection<Var> vars) {
        // Used to check if there's a projection node
        if (!isProjection()) {
            Node root = NodeFactory.createBlankNode();
            graph.add(Triple.create(root, typeNode, projectNode));
            typeMap.put(root, projectNode);
            graph.add(Triple.create(root, opNode, this.root));
            this.root = root;
        } else {
            List<Var> varList = new ArrayList<>();
            ExtendedIterator<Node> varNodes = GraphUtil.listObjects(graph, root, argNode);
            while (varNodes.hasNext()) {
                varList.add(Var.alloc(varNodes.next().getBlankNodeLabel()));
            }
            if (!varList.isEmpty()) {
                if (!(varList.containsAll(vars) && vars.containsAll(varList))) {
                    Node root = NodeFactory.createBlankNode();
                    graph.add(Triple.create(root, typeNode, projectNode));
                    typeMap.put(root, projectNode);
                    graph.add(Triple.create(root, opNode, this.root));
                    this.root = root;
                }
            }
        }
        if (this.vars != null) {
            this.vars.addAll(vars);
        }
        for (Var v : vars) {
            graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));
        }
    }

    public void project(Collection<Var> vars, int n) {
        this.project(vars);
        graph.add(Triple.create(root, idNode, NodeFactory.createLiteralByValue(n, XSDDatatype.XSDinteger)));
    }

    public void ask() {
        Node root = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, typeNode, askNode));
        typeMap.put(root, askNode);
        graph.add(Triple.create(root, opNode, this.root));
        if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)) {
            graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
        }
        for (Var v : vars) {
            graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));
        }
        this.root = root;
    }

    public void construct(Template template) {
        Node root = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, typeNode, constructNode));
        typeMap.put(root, constructNode);
        graph.add(Triple.create(root, opNode, this.root));
        if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)) {
            graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
        }
        if (template != null) {
            BasicPattern bp = template.getBGP();
            RGraph rg = new RGraph(bp.getList());
            graph.add(Triple.create(root, argNode, rg.root));
            GraphUtil.addInto(graph, rg.graph);
        }
        this.root = root;
    }

    public void describe() {
        Node root = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, typeNode, describeNode));
        typeMap.put(root, describeNode);
        graph.add(Triple.create(root, opNode, this.root));
        if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)) {
            graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
        }
        for (Var v : vars) {
            graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));
        }
        this.root = root;
    }

    /**
     * Creates a node that indicates if the DISTINCT keyword has been used, and adds it to this r-graph.
     *
     * @param isDistinct A boolean that is true if the DISTINCT keyword has been used, and false otherwise.
     */
    public void setDistinctNode(boolean isDistinct) {
        if (isProjection()) {
            graph.add(Triple.create(root, distinctNode, NodeFactory.createLiteralByValue(isDistinct, XSDDatatype.XSDboolean)));
        } else if (isConstruction()) {
            graph.add(Triple.create(root, distinctNode, NodeFactory.createLiteralByValue(isDistinct, XSDDatatype.XSDboolean)));
        } else {
            Node p = NodeFactory.createBlankNode();
            graph.add(Triple.create(p, typeNode, projectNode));
            typeMap.put(p, projectNode);
            graph.add(Triple.create(p, opNode, root));
            graph.add(Triple.create(p, distinctNode, NodeFactory.createLiteralByValue(isDistinct, XSDDatatype.XSDboolean)));
            this.root = p;
        }
    }

    public void setReducedNode(boolean isReduced) {
        if (isProjection()) {
            graph.add(Triple.create(root, reducedNode, NodeFactory.createLiteralByValue(isReduced, XSDDatatype.XSDboolean)));
        } else if (isConstruction()) {
            graph.add(Triple.create(root, reducedNode, NodeFactory.createLiteralByValue(isReduced, XSDDatatype.XSDboolean)));
        } else {
            Node p = NodeFactory.createBlankNode();
            graph.add(Triple.create(p, typeNode, projectNode));
            typeMap.put(p, projectNode);
            graph.add(Triple.create(p, opNode, root));
            graph.add(Triple.create(p, reducedNode, NodeFactory.createLiteralByValue(isReduced, XSDDatatype.XSDboolean)));
            this.root = p;
        }
    }

    /**
     * @return Returns true if the DISTINCT keyword has been used, and false otherwise.
     */
    public boolean isDistinct() {
        return this.graph.contains(root, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
    }

	/**
     * Joins this r-graph with another r-graph. The result is a conjunction of both r-graphs. (Q_1 AND Q_2)
     *
     * @param arg1 An r-graph to join with this one.
     */
    public void join(RGraph arg1) {
        Node root = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, typeNode, joinNode));
        typeMap.put(root, joinNode);
        merge(arg1);
        Node type0 = typeMap.get(this.root);
        Node type1 = typeMap.get(arg1.root);
        if (!this.root.equals(root)) {
            graph.add(Triple.create(root, argNode, this.root));
        }
        if (!arg1.root.equals(root)) {
            graph.add(Triple.create(root, argNode, arg1.root));
        }
        if (type0.equals(joinNode)) {
            ExtendedIterator<Node> args = GraphUtil.listObjects(graph, this.root, argNode);
            graph.delete(Triple.create(this.root, typeNode, joinNode));
            typeMap.remove(this.root);
            graph.delete(Triple.create(root, argNode, this.root));
            if (type1.equals(joinNode)) {
                ExtendedIterator<Node> args1 = GraphUtil.listObjects(arg1.graph, arg1.root, argNode);
                graph.delete(Triple.create(arg1.root, typeNode, joinNode));
                typeMap.remove(arg1.root);
                graph.delete(Triple.create(root, argNode, arg1.root));
                while (args1.hasNext()) {
                    Node n = args1.next();
                    graph.add(Triple.create(root, argNode, n));
                    graph.delete(Triple.create(arg1.root, argNode, n));
                }
            }
            while (args.hasNext()) {
                Node n = args.next();
                graph.add(Triple.create(root, argNode, n));
                graph.delete(Triple.create(this.root, argNode, n));
            }
        } else if (type1.equals(joinNode)) {
            ExtendedIterator<Node> args = GraphUtil.listObjects(arg1.graph, arg1.root, argNode);
            graph.delete(Triple.create(arg1.root, typeNode, joinNode));
            typeMap.remove(arg1.root);
            graph.delete(Triple.create(root, argNode, arg1.root));
            while (args.hasNext()) {
                Node n = args.next();
                graph.add(Triple.create(root, argNode, n));
                graph.delete(Triple.create(arg1.root, argNode, n));
            }
            arg1.root = root;
        }
        this.root = root;
    }

    /**
     * Joins this r-graph with another r-graph. The result is the union of both r-graphs. (Q_1 UNION Q_2)
     *
     * @param arg1 An r-graph to join with this one.
     */
    public void union(RGraph arg1) {
        Node root = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, typeNode, unionNode));
        typeMap.put(root, unionNode);
        merge(arg1);
        Node type0 = typeMap.get(this.root);
        Node type1 = typeMap.get(arg1.root);
        if (!this.root.equals(root)) {
            graph.add(Triple.create(root, argNode, this.root));
        }
        if (!arg1.root.equals(root)) {
            graph.add(Triple.create(root, argNode, arg1.root));
        }
        if (type0.equals(unionNode)) {
            ExtendedIterator<Node> args = GraphUtil.listObjects(graph, this.root, argNode);
            graph.delete(Triple.create(this.root, typeNode, unionNode));
            typeMap.remove(this.root);
            graph.delete(Triple.create(root, argNode, this.root));
            if (type1.equals(unionNode)) {
                ExtendedIterator<Node> args1 = GraphUtil.listObjects(arg1.graph, arg1.root, argNode);
                graph.delete(Triple.create(arg1.root, typeNode, unionNode));
                typeMap.remove(arg1.root);
                graph.delete(Triple.create(root, argNode, arg1.root));
                while (args1.hasNext()) {
                    Node n = args1.next();
                    graph.add(Triple.create(root, argNode, n));
                    graph.delete(Triple.create(arg1.root, argNode, n));
                }
            }
            while (args.hasNext()) {
                Node n = args.next();
                graph.add(Triple.create(root, argNode, n));
                graph.delete(Triple.create(this.root, argNode, n));
            }

        } else if (type1.equals(unionNode)) {
            ExtendedIterator<Node> args = GraphUtil.listObjects(arg1.graph, arg1.root, argNode);
            graph.delete(Triple.create(arg1.root, typeNode, unionNode));
            typeMap.remove(arg1.root);
            graph.delete(Triple.create(root, argNode, arg1.root));
            while (args.hasNext()) {
                Node n = args.next();
                graph.add(Triple.create(root, argNode, n));
                graph.delete(Triple.create(arg1.root, argNode, n));
            }
            arg1.root = root;
        }
        this.root = root;
    }

    /**
     * Joins this r-graph with another r-graph. This includes the second r-graph as an optional query pattern. (Q_1 OPT Q_2)
     *
     * @param arg1 An r-graph that represents an optional query pattern.
     */
    public void optional(RGraph arg1) {
        Node root = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, typeNode, optionalNode));
        typeMap.put(root, optionalNode);
        graph.add(Triple.create(root, leftNode, this.root));
        graph.add(Triple.create(root, rightNode, arg1.root));
        this.root = root;
        merge(arg1);
    }

    public void minus(RGraph arg1) {
        Node root = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, typeNode, minusNode));
        typeMap.put(root, minusNode);
        graph.add(Triple.create(root, leftNode, this.root));
        graph.add(Triple.create(root, rightNode, arg1.root));
        this.root = root;
        merge(arg1);
    }

    /**
     * @param arg1 An r-graph that represents a BIND expression (BIND var AS expr) and adds it to this r-graph.
     */
    public void bind(RGraph arg1) {
        checkForAnonVars(arg1);
        Node n = getPatternNode();
        graph.add(Triple.create(n, extendNode, arg1.root));
        merge(arg1);
    }

    public void checkForAnonVars(RGraph arg1) {
        for (Var v : exprMap.keySet()) {
            Node vNode = NodeFactory.createBlankNode(v.getVarName());
            ExtendedIterator<Node> ops = GraphUtil.listSubjects(arg1.graph, argNode, vNode);
            while (ops.hasNext()) {
                Node op = ops.next();
                arg1.graph.delete(Triple.create(op, argNode, vNode));
                arg1.graph.add(Triple.create(op, argNode, exprMap.get(v)));
            }
            ExtendedIterator<Node> blanks = GraphUtil.listSubjects(arg1.graph, valueNode, vNode);
            while (blanks.hasNext()) {
                Node blank = blanks.next();
                arg1.graph.delete(Triple.create(blank, valueNode, vNode));
                arg1.graph.add(Triple.create(blank, valueNode, exprMap.get(v)));
            }
        }
    }

    /**
     * Creates an r-graph that represents each of the assignments in a BIND expression (BIND var AS expr) and adds it to this r-graph.
     *
     * @param expr An expression in an assignment.
     * @param var  The variable the expression is assigned to.
     */
    public void bindNode(Node expr, Var var) {
        graph.add(Triple.create(this.root, varNode, NodeFactory.createBlankNode(var.getVarName())));
        graph.add(Triple.create(this.root, typeNode, bindNode));
        typeMap.put(this.root, bindNode);
        graph.add(Triple.create(this.root, argNode, expr));
    }

    /**
     * Creates an r-graph that represents a FILTER expression (FILTER expr) and adds it to this r-graph.
     *
     * @param n A node that represents an expression.
     */
    public void filter(Node n) {
        Node filter = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, modNode, filter));
        graph.add(Triple.create(filter, typeNode, filterNode));
        typeMap.put(filter, filterNode);
        graph.add(Triple.create(filter, argNode, n));
    }

    /**
     * Creates an r-graph that represents a FILTER expression (FILTER expr) and adds it to this r-graph.
     *
     * @param arg1 An r-graph that represents an expression.
     */
    public void filter(RGraph arg1) {
        checkForAnonVars(arg1);
        Node n = getPatternNode();
        Node filter = NodeFactory.createBlankNode();
        graph.add(Triple.create(n, filterNode, filter));
        graph.add(Triple.create(filter, argNode, arg1.root));
        merge(arg1);
    }

    /**
     * Creates an r-graph that represents the conjunction of two expressions (FILTER expr1 && expr2) and adds it to this r-graph .
     *
     * @param arg1 An r-graph that represents an expression.
     * @param arg2 An r-graph that represents an expression.
     * @return Returns the node that represents the AND operator.
     */
    public Node filterAnd(Node arg1, Node arg2) {
        Node o = NodeFactory.createBlankNode();
        graph.add(Triple.create(o, typeNode, andNode));
        typeMap.put(o, andNode);
        List<Node> args = new ArrayList<>();
        if (typeMap.containsKey(arg1) && typeMap.get(arg1).equals(andNode)) {
            ExtendedIterator<Node> argNodes = GraphUtil.listObjects(graph,arg1,argNode);
            while (argNodes.hasNext()) {
                Node arg = argNodes.next();
                graph.delete(Triple.create(arg1,argNode,arg));
                args.add(arg);
            }
            graph.delete(Triple.create(arg1,typeNode,andNode));
            typeMap.remove(arg1);
        }
        else {
            args.add(arg1);
        }
        if (typeMap.containsKey(arg2) && typeMap.get(arg2).equals(andNode)) {
            ExtendedIterator<Node> argNodes = GraphUtil.listObjects(graph,arg2,argNode);
            while (argNodes.hasNext()) {
                Node arg = argNodes.next();
                graph.delete(Triple.create(arg2,argNode,arg));
                args.add(arg);
            }
            graph.delete(Triple.create(arg2,typeNode,andNode));
            typeMap.remove(arg2);
        }
        else {
            args.add(arg2);
        }
        for (Node arg : args) {
            graph.add(Triple.create(o, argNode, arg));
        }
        return o;
    }

    /**
     * Creates an r-graph that represents the disjunction of two expressions (FILTER expr1 || expr2) and adds it to this r-graph.
     *
     * @param arg1 An r-graph that represents an expression.
     * @param arg2 An r-graph that represents an expression.
     * @return Returns the node that represents the OR operator.
     */
    public Node filterOr(Node arg1, Node arg2) {
        Node o = NodeFactory.createBlankNode();
        graph.add(Triple.create(o, typeNode, orNode));
        typeMap.put(o, orNode);
        List<Node> args = new ArrayList<>();
        if (typeMap.containsKey(arg1) && typeMap.get(arg1).equals(orNode)) {
            ExtendedIterator<Node> argNodes = GraphUtil.listObjects(graph,arg1,argNode);
            while (argNodes.hasNext()) {
                Node arg = argNodes.next();
                graph.delete(Triple.create(arg1,argNode,arg));
                args.add(arg);
            }
            graph.delete(Triple.create(arg1,typeNode,orNode));
            typeMap.remove(arg1);
        }
        else {
            args.add(arg1);
        }
        if (typeMap.containsKey(arg2) && typeMap.get(arg2).equals(orNode)) {
            ExtendedIterator<Node> argNodes = GraphUtil.listObjects(graph,arg2,argNode);
            while (argNodes.hasNext()) {
                Node arg = argNodes.next();
                graph.delete(Triple.create(arg2,argNode,arg));
                args.add(arg);
            }
            graph.delete(Triple.create(arg2,typeNode,orNode));
            typeMap.remove(arg2);
        }
        else {
            args.add(arg2);
        }
        for (Node arg : args) {
            graph.add(Triple.create(o, argNode, arg));
        }
        return o;
    }

    /**
     * Creates an r-graph that represents the negation of an expression (FILTER !expr) and adds it to this r-graph.
     *
     * @param arg1 An r-graph that represents an expression.
     * @return Returns the node that represents the NOT operator.
     */
    public Node filterNot(Node arg1) {
        Node o = NodeFactory.createBlankNode();
        graph.add(Triple.create(o, typeNode, notNode));
        typeMap.put(o, notNode);
        graph.add(Triple.create(o, argNode, arg1));
        return o;
    }

    /**
     * Creates an r-graph that represents a function over an expression (FILTER (f expr)) and adds it to this r-graph.
     *
     * @param op   A node that represents a SPARQL function.
     * @param arg1 A node that represents an expression.
     * @return Returns the node that represents the function.
     */
    public Node filterFunction(Node op, Node arg1) {
        Node n = NodeFactory.createBlankNode();
        Node a = NodeFactory.createBlankNode();
        if (!GraphUtil.listObjects(graph, arg1, functionNode).hasNext()) {
            graph.add(Triple.create(a, valueNode, arg1));
        } else {
            a = arg1;
        }
        graph.add(Triple.create(n, functionNode, op));
        graph.add(Triple.create(n, argNode, a));
        return n;
    }

    public Node filterFunction(String op, Node arg1) {
        return filterFunction(NodeFactory.createLiteral(op), arg1);
    }

    /**
     * Creates an r-graph that represents a binary SPARQL function (FILTER (f expr1 expr2)) and adds it to this r-graph.
     *
     * @param op   A string that represents a binary SPARQL function.
     * @param arg1 A node that represents an expression.
     * @param arg2 A node that represents an expression.
     * @return Returns a node that represents the function.
     */
    public Node filterFunction(String op, Node arg1, Node arg2) {
        Node n = NodeFactory.createBlankNode();
        Node o = NodeFactory.createLiteral(op);
        Node a = NodeFactory.createBlankNode();
        Node b = NodeFactory.createBlankNode();
        graph.add(Triple.create(n, functionNode, o));
        if (!GraphUtil.listObjects(graph, arg1, functionNode).hasNext()) {
            graph.add(Triple.create(a, valueNode, arg1));
        } else {
            a = arg1;
        }
        if (!GraphUtil.listObjects(graph, arg2, functionNode).hasNext()) {
            graph.add(Triple.create(b, valueNode, arg2));
        } else {
            b = arg2;
        }
        graph.add(Triple.create(n, argNode, a));
        graph.add(Triple.create(n, argNode, b));
        if (isOrderedFunction(op)) {
            graph.add(Triple.create(a, orderNode, NodeFactory.createLiteralByValue(0, XSDDatatype.XSDint)));
            graph.add(Triple.create(b, orderNode, NodeFactory.createLiteralByValue(1, XSDDatatype.XSDint)));
        }
        return n;
    }

    public Node filterFunction(Node o, List<Node> nodes) {
        int i = 0;
        Node n = NodeFactory.createBlankNode();
        String op = o.isURI() ? o.getURI() : o.getLiteralLexicalForm();
        graph.add(Triple.create(n, functionNode, o));
        for (Node node : nodes) {
            Node newNode = NodeFactory.createBlankNode();
            if (!GraphUtil.listObjects(graph, node, functionNode).hasNext()) {
                graph.add(Triple.create(n, argNode, newNode));
                graph.add(Triple.create(newNode, valueNode, node));
                if (isOrderedFunction(op) || o.isURI()) {
                    graph.add(Triple.create(newNode, orderNode, NodeFactory.createLiteralByValue(i++, XSDDatatype.XSDint)));
                }
            } else {
                graph.add(Triple.create(n, argNode, node));
                if (isOrderedFunction(op) || o.isURI()) {
                    graph.add(Triple.create(node, orderNode, NodeFactory.createLiteralByValue(i++, XSDDatatype.XSDint)));
                }
            }
        }
        return n;
    }

//    public void filterNormalisation() {
//        Graph before = GraphFactory.createPlainGraph();
//        while (!before.isIsomorphicWith(graph)) {
//            before = GraphFactory.createPlainGraph();
//            GraphUtil.addInto(before, graph);
//            UpdateAction.execute(conjunctionRule, graph);
//            UpdateAction.execute(disjunctionRule, graph);
//        }
//    }

    public void service(Node service, boolean silent) {
        Node n = NodeFactory.createBlankNode();
        graph.add(Triple.create(n, typeNode, serviceNode));
        graph.add(Triple.create(n, valueNode, service));
        graph.add(Triple.create(n, silentNode, NodeFactory.createLiteralByValue(silent, XSDDatatype.XSDboolean)));
        graph.add(Triple.create(n, argNode, root));
        typeMap.put(n, serviceNode);
        this.root = n;
    }

    /**
     * @param group An r-graph representing a GROUP BY expression.
     */
    public void groupBy(RGraph group) {
        Node n = getPatternNode();
        graph.add(Triple.create(n, argNode, group.root));
        GraphUtil.addInto(graph, group.graph);
        ExtendedIterator<Node> vars = listSubjects(group.graph, typeNode, varNode);
        while (vars.hasNext()) {
            Node v = vars.next();
            if (GraphUtil.listObjects(group.graph, v, functionNode).hasNext()) {
                graph.delete(Triple.create(v, typeNode, varNode));
            }
        }
    }

	/**
	 *
	 * @return If the root of the graph is connected to a filter, aggregation, etc, returns the root. Otherwise, it creates a new root and returns it.
	 */

	public Node getPatternNode() {
        if (!typeMap.get(root).equals(extraNode)) {
            Node n = NodeFactory.createBlankNode();
            graph.add(Triple.create(n, subNode, root));
            graph.add(Triple.create(n, typeNode, extraNode));
            typeMap.put(n, extraNode);
            root = n;
        }
        return root;
    }

    /**
     * @param args A list of r-graphs that represent aggregation expressions.
     */
    public void aggregation(RGraph r, List<RGraph> args) {
        Node n = getPatternNode();
        Node extend = NodeFactory.createBlankNode();
        graph.add(Triple.create(r.root, patternNode, extend));
        graph.add(Triple.create(extend, typeNode, aggregateNode));
        typeMap.put(extend, aggregateNode);
        graph.add(Triple.create(n, argNode, r.root));
        GraphUtil.addInto(this.graph, r.graph);
        for (RGraph arg : args) {
            graph.add(Triple.create(extend, argNode, arg.root));
            exprMap.putAll(arg.exprMap);
            GraphUtil.addInto(this.graph, arg.graph);
        }
    }

    /**
     * @param op       The name of the aggregate function.
     * @param distinct True if distinct.
     * @param var      The variable to which the function is assigned.
     * @param arg      A node pointing to a variable or expression.
     * @return A node pointing to this aggregate function.
     */
    public Node aggregationFunction(String op, boolean distinct, Var var, Node arg) {
        Node n = getAggregateNode(op, distinct, var);
        if (!GraphUtil.listObjects(graph, arg, functionNode).hasNext()) {
            Node a = NodeFactory.createBlankNode();
            graph.add(Triple.create(n, argNode, a));
            graph.add(Triple.create(a, valueNode, arg));
            graph.add(Triple.create(arg, typeNode, varNode));
            typeMap.put(arg, varNode);
        } else {
            graph.add(Triple.create(n, argNode, arg));
        }
        return n;
    }

    /**
     * @param op The name of the aggregate function. (COUNT in this case)
     * @param var The variable to which the function is assigned.
     * @param arg A blank node.
     * @return
     */
    public Node aggregationCount(String op, boolean distinct, Var var, Node arg) {
        Node n = getAggregateNode(op, distinct, var);
        graph.add(Triple.create(n, argNode, arg));
        graph.add(Triple.create(arg, valueNode, NodeFactory.createLiteral("*")));
        return n;
    }

    public Node getAggregateNode(String op, boolean distinct, Var var) {
        Node n = NodeFactory.createBlankNode();
        if (var != null) {
            exprMap.put(var, n);
        }
        Node o = NodeFactory.createLiteral(op);
        graph.add(Triple.create(n, functionNode, o));
        if (distinct) {
            graph.add(Triple.create(n, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
        }
        return n;
    }

    /**
     * Creates an r-graph that represents the RDF datasets that form the default graph, and adds it to this r-graph.
     *
     * @param g A collection of strings that identify RDF datasets.
     */
    public void fromGraph(Collection<String> g) {
        Node n = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, modNode, n));
        graph.add(Triple.create(n, typeNode, fromNode));
        typeMap.put(n, fromNode);
        for (String s : g) {
            graph.add(Triple.create(n, argNode, NodeFactory.createURI(s)));
        }
    }

    /**
     * Creates an r-graph that represents the RDF datasets that define the named graphs, and adds it to this r-graph.
     *
     * @param g A collection of strings that identify RDF datasets.
     */
    public void fromNamedGraph(Collection<String> g) {
        Node aux;
        ExtendedIterator<Node> e = listSubjects(graph, typeNode, fromNode);
        if (e.hasNext()) {
            aux = e.next();
        } else {
            aux = NodeFactory.createBlankNode();
            graph.add(Triple.create(aux, typeNode, fromNode));
            typeMap.put(aux, fromNode);
        }
        Node n = NodeFactory.createBlankNode();
        graph.add(Triple.create(aux, modNode, n));
        graph.add(Triple.create(n, typeNode, fromNamedNode));
        typeMap.put(n, fromNamedNode);
        for (String s : g) {
            graph.add(Triple.create(n, argNode, NodeFactory.createURI(s)));
        }
    }

    /**
     * Creates an r-graph that represents an ORDER BY clause, and adds it to this r-graph.
     *
     * @param exprs The list of expressions according to which the results are ordered.
     * @param dir   A list of integers that indicates if results are ordered ascending or descending.
     */

    public void orderBy(List<Expr> exprs, List<Integer> dir) {
        if (!isProjection()) {
            Node project = NodeFactory.createBlankNode();
            graph.add(Triple.create(project, typeNode, projectNode));
            typeMap.put(project, projectNode);
            graph.add(Triple.create(project, opNode, root));
            this.root = project;
        }
        Node order = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, modNode, order));
        graph.add(Triple.create(order, typeNode, orderByNode));
        typeMap.put(order, orderByNode);
        for (int i = 0; i < exprs.size(); i++) {
            Node auxNode = NodeFactory.createBlankNode();
            graph.add(Triple.create(order, argNode, auxNode));
            if (exprs.get(i).isVariable()) {
                graph.add(Triple.create(auxNode, valueNode, NodeFactory.createBlankNode(exprs.get(i).getVarName())));
            } else {
                FilterVisitor fv = new FilterVisitor();
                Walker.walk(exprs.get(i), fv);
                RGraph rg = fv.getGraph();
                graph.add(Triple.create(auxNode, valueNode, rg.root));
                GraphUtil.addInto(this.graph, rg.graph);
            }
            graph.add(Triple.create(auxNode, orderNode, NodeFactory.createLiteralByValue(i, XSDDatatype.XSDint)));
            graph.add(Triple.create(auxNode, dirNode, NodeFactory.createLiteralByValue(dir.get(i), XSDDatatype.XSDint)));
        }
    }

    /**
     * Creates an r-graph that represents a combination of LIMIT and OFFSET, and adds it to this r-graph.
     *
     * @param offset A number that causes the solutions to start at the specified number. An offset of 0 has no effect.
     * @param limit  A number that specifies the number of solutions to return.
     */
    public void slice(int offset, int limit) {
        //Node order = GraphUtil.listSubjects(graph, typeNode, projectNode).next();
        if (!isProjection()) {
            Node root = NodeFactory.createBlankNode();
            graph.add(Triple.create(root, typeNode, projectNode));
            typeMap.put(root, projectNode);
            graph.add(Triple.create(root, opNode, this.root));
            this.root = root;
        }
        Node lNode = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, modNode, lNode));
        graph.add(Triple.create(lNode, typeNode, limitNode));
        typeMap.put(lNode, limitNode);
        graph.add(Triple.create(lNode, offsetNode, NodeFactory.createLiteralByValue(offset, XSDDatatype.XSDint)));
        graph.add(Triple.create(lNode, valueNode, NodeFactory.createLiteralByValue(limit, XSDDatatype.XSDint)));
    }

    public void graphOp(Node n) {
        Node r = NodeFactory.createBlankNode();
        graph.add(Triple.create(r, typeNode, graphNode));
        typeMap.put(r, graphNode);
        if (n.isVariable()) {
            n = NodeFactory.createBlankNode(n.getName());
        }
        graph.add(Triple.create(r, valueNode, n));
        graph.add(Triple.create(r, argNode, this.root));
        this.root = r;
    }

    public void turnDistinctOn() {
        this.distinct = true;
    }

    public void setLeaning(boolean b) {
        this.leaning = b;
    }

    public boolean isProjection() {
        if (root != null) {
            return typeMap.get(root).equals(projectNode);
        } else {
            return false;
        }
    }

    public boolean isConstruction() {
        if (root != null) {
            return typeMap.get(root).equals(constructNode);
        } else {
            return false;
        }
    }

	public boolean containsUnion() {
        return listSubjects(graph, typeNode, unionNode).hasNext();
    }

    public boolean containsJoin() {
        return listSubjects(graph, typeNode, joinNode).hasNext();
    }

    public Model asModel() {
        return ModelFactory.createModelForGraph(graph);
    }

    public String toString() {
        StringBuilder ans = new StringBuilder();
        ExtendedIterator<Triple> e = GraphUtil.findAll(this.graph);
        while (e.hasNext()) {
            Triple t = e.next();
            ans.append(t.toString());
            ans.append("\n");
        }
        return ans.toString();
    }

    public void print() {
        System.out.println(this);
        System.out.println();
    }

    /**
     * @return Returns a set containing all triples in this r-graph.
     */
    public TreeSet<org.semanticweb.yars.nx.Node[]> getTriples() {
        Model model = this.asModel();
        JenaModelIterator jmi = new JenaModelIterator(model);
        TreeSet<org.semanticweb.yars.nx.Node[]> triples = new TreeSet<>(org.semanticweb.yars.nx.NodeComparator.NC);
        while (jmi.hasNext()) {
            org.semanticweb.yars.nx.Node[] triple = jmi.next();
            triples.add(new org.semanticweb.yars.nx.Node[]{triple[0], triple[1], triple[2]});
        }
        return triples;
    }

    public GraphLeaningResult DFSLeaning(Collection<org.semanticweb.yars.nx.Node[]> triples) throws InterruptedException {
        return this.DFSLeaning(triples, false, false);
    }

    public GraphLeaningResult DFSLeaning(Collection<org.semanticweb.yars.nx.Node[]> triples, boolean randomiseBindings, boolean prune) throws InterruptedException {
        DFSGraphLeaning dfsgl = new DFSGraphLeaning(triples, randomiseBindings, prune);
        return dfsgl.call();
    }

    /**
     * @param triples A collection of RDF triples.
     * @return A canonical labeling of all blank nodes.
     * @throws InterruptedException
     * @throws HashCollisionException
     */
    public GraphLabellingResult label(Collection<org.semanticweb.yars.nx.Node[]> triples) throws InterruptedException, HashCollisionException {
        GraphLabellingArgs gla = new GraphLabellingArgs();
        gla.setDistinguishIsoPartitions(false);
        GraphLabelling gl = new GraphLabelling(triples, gla);
        return gl.call();
    }

//	public void subQueryRelabelling() {
//        ExtendedIterator<Node> projections = GraphUtil.listSubjects(graph, typeNode, projectNode);
//        if (projections.hasNext()) {
//            List<Node> projectionList = projections.toList();
//            int n = 0;
//            if (projectionList.size() > 1) {
//                Map<Node, Graph> map = new HashMap<>();
//                for (Node p : projectionList) {
//                    graph.add(Triple.create(p, idNode, NodeFactory.createLiteralByValue(n++, XSDDatatype.XSDinteger)));
//                    GraphExtract ge = new GraphExtract(new CustomTripleBoundary(projectionList, p));
//                    Graph g = ge.extract(p, graph);
//                    map.put(p, g);
//                }
//                List<Graph> subGraphs = new ArrayList<>();
//                for (Map.Entry<Node, Graph> entry : map.entrySet()) {
//                    Graph currentGraph = entry.getValue();
//                    Node currentNode = entry.getKey();
//                    List<Node> currentProjectionNodes = GraphUtil.listObjects(currentGraph, currentNode, argNode).toList();
//                    Node id = GraphUtil.listObjects(currentGraph, currentNode, idNode).hasNext() ? GraphUtil.listObjects(currentGraph, currentNode, idNode).next() : NodeFactory.createLiteralByValue(0, XSDDatatype.XSDinteger);
//                    ExtendedIterator<Node> nodes = GraphUtil.listSubjects(currentGraph, typeNode, varNode);
//                    while (nodes.hasNext()) {
//                        Node node = nodes.next();
//                        if (node.isBlank()) {
//                            if (!currentProjectionNodes.contains(node)) {
//                                currentGraph.add(Triple.create(node, idNode, id));
//                            }
//                        }
//                    }
//                    UpdateAction.execute(subgraphRule, currentGraph);
//                    subGraphs.add(currentGraph);
//                    currentGraph.delete(Triple.create(currentNode, idNode, id));
//                }
//                Graph ans = GraphFactory.createDefaultGraph();
//                for (Graph g : subGraphs) {
//                    GraphUtil.addInto(ans, g);
//                }
//                this.graph = ans;
//            } else {
//                Node p = projectionList.get(0);
//                ExtendedIterator<Node> ids = GraphUtil.listObjects(graph, p, idNode);
//                if (ids.hasNext()) {
//                    Node id = ids.next();
//                    graph.delete(Triple.create(p, idNode, id));
//                }
//            }
//        }
//    }

    /**
     * Performs a leaning of the current r-graph.
     *
     * @return Returns the lean form of this graph.
     * @throws InterruptedException
     */
    public RGraph getLeanForm() throws InterruptedException {
        GraphLeaningResult glResult = this.DFSLeaning(getTriples());
        Collection<org.semanticweb.yars.nx.Node[]> nodes = glResult.getLeanData();
        return new RGraph(nodes);
    }

    public RGraph canonicalLabel(boolean verbose) throws HashCollisionException, InterruptedException {
        GraphLabellingResult glr = this.label(this.getTriples());
        if (verbose) {
            System.out.println("Labelling results: \n");
            System.out.println("Number of blank nodes: " + glr.getBnodeCount());
            System.out.println("Number of colouring iterations: " + glr.getColourIterationCount());
            System.out.println("Number of partitions found: " + glr.getPartitionCount());
        }
        RGraph ans = new RGraph(glr.getGraph());
        ans.varMap = this.varMap;
        HashMap<org.semanticweb.yars.nx.Node, HashCode> map = glr.getHashGraph().getBlankNodeHashes();
        for (Map.Entry<org.semanticweb.yars.nx.Node, HashCode> entry : map.entrySet()) {
            Node n = nxToJenaNode(entry.getKey());
            if (varMap.inverse().containsKey(n)) {
                Node in = varMap.inverse().get(n);
                varMap.put(in, NodeFactory.createBlankNode("SK00" + entry.getValue().toString()));
            }
        }
        ans.hashCode = glr.getUniqueGraphHash();
        return ans;
    }

    /**
     * @param verbose A boolean that allows messages to appear during canonicalisation.
     * @return Returns the canonical form of the r-graph.
     * @throws InterruptedException
     * @throws HashCollisionException
     */
    public final RGraph getCanonicalForm(boolean verbose) throws InterruptedException, HashCollisionException {
        RGraph result;
        if (verbose) {
            System.out.println("CQ Normalisation");
        }
        boolean distinct = this.graph.contains(this.root, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
        if (leaning && distinct) {
            long t = System.nanoTime();
            if (verbose) {
                System.out.println("Branch relabelling");
            }
            if (verbose) {
                System.out.println("UCQ minimisation");
            }
            RGraph ans;
            if (containsPaths) {
                ans = this;
//				ans = uc2rpqMinimisation();
            } else {
                ans = ucqMinimisation();
            }
            if (verbose) {
                System.out.println("Beginning leaning.");
            }
            ExtendedIterator<Node> vars = GraphUtil.listSubjects(ans.graph, typeNode, varNode);
            while (vars.hasNext()) {
                Node var = vars.next();
                ans.graph.delete(Triple.create(var, typeNode, varNode));
            }
            long time1 = System.nanoTime() - t;
            if (verbose) {
                System.out.println("Beginning labelling");
            }
            t = System.nanoTime();
            if (verbose) {
                ans.print();
            }
            if (canonical) {
                long time = System.nanoTime();
                ans = ans.canonicalLabel(verbose);
                time = System.nanoTime() - time;
                ans.setLabelTime(time);
            }
            ans.setMinimisationTime(time1);
            result = ans;
        } else {
            ExtendedIterator<Node> vars = GraphUtil.listSubjects(graph, typeNode, varNode);
            while (vars.hasNext()) {
                Node var = vars.next();
                graph.delete(Triple.create(var, typeNode, varNode));
            }
            if (canonical) {
                long time = System.nanoTime();
                RGraph ans = canonicalLabel(verbose);
                time = System.nanoTime() - time;
                ans.setLabelTime(time);
                result = ans;
            } else {
                result = this;
            }
        }
        return result;
    }

    public Set<Node> listMonotoneParts() {
        Set<Node> ans = new HashSet<>();
        ExtendedIterator<Node> cqs = listSubjects(graph, typeNode, joinNode);
        ExtendedIterator<Node> ucqs = listSubjects(graph, typeNode, unionNode);
        while (cqs.hasNext()) {
            Node cq = cqs.next();
            if (isUCQ(cq)) {
                ans.add(cq);
            }
        }
        while (ucqs.hasNext()) {
            Node ucq = ucqs.next();
            if (isUCQ(ucq)) {
                ans.add(ucq);
                ExtendedIterator<Node> cqsInUnion = GraphUtil.listObjects(graph, ucq, argNode);
                while (cqsInUnion.hasNext()) {
                    Node cq = cqsInUnion.next();
                    ans.remove(cq);
                }
            }
        }
        return ans;
    }

    public final RGraph ucqMinimisation() throws InterruptedException {
        List<RGraph> result = new ArrayList<>();
        GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
        Set<Node> ucqs = listMonotoneParts();
        for (Node ucq : ucqs) {
            if (graph.contains(Triple.create(ucq, typeNode, unionNode))) { //Make sure it's a union of conjunctive queries.
                Node subjectUnion, predicateUnion;
                if (listSubjects(graph, opNode, ucq).hasNext()) {
                    subjectUnion = listSubjects(graph, opNode, ucq).next();
                    predicateUnion = opNode;
                } else if (listSubjects(graph, leftNode, ucq).hasNext()) {
                    subjectUnion = listSubjects(graph, leftNode, ucq).next();
                    predicateUnion = leftNode;
                } else if (listSubjects(graph, rightNode, ucq).hasNext()) {
                    subjectUnion = listSubjects(graph, rightNode, ucq).next();
                    predicateUnion = rightNode;
                } else if (listSubjects(graph, subNode, ucq).hasNext()) {
                    subjectUnion = listSubjects(graph, subNode, ucq).next();
                    predicateUnion = subNode;
                } else if (listSubjects(graph, valueNode, ucq).hasNext()) {
                    subjectUnion = listSubjects(graph, valueNode, ucq).next();
                    predicateUnion = valueNode;
                } else {
                    subjectUnion = listSubjects(graph, argNode, ucq).next();
                    predicateUnion = argNode;
                }
                ExtendedIterator<Node> cQueries = listObjects(graph, ucq, argNode);
                if (GraphUtil.listObjects(graph, ucq, argNode).toList().size() > 1) { //More than one BGP in union
                    Graph inner = ge.extract(ucq, graph);
                    Graph outer = GraphFactory.createPlainGraph();
                    CustomTripleBoundary ctb = new CustomTripleBoundary(Collections.singletonList(ucq), null);
                    GraphExtract extract = new GraphExtract(ctb);
                    outer = extract.extract(root, graph);
                    List<Node> innerVars = GraphUtil.listSubjects(inner, typeNode, varNode).toList();
                    List<Node> outerVars = GraphUtil.listSubjects(outer, typeNode, varNode).toList();
                    Map<List<Node>, List<RGraph>> partitionsByVars = new HashMap<>();
                    outer.remove(subjectUnion, predicateUnion, ucq);
                    for (Node p : innerVars) {
                        inner.delete(Triple.create(p, typeNode, varNode));
                    }
                    for (Node p : outerVars) {
                        outer.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
                        outer.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
                        outer.add(Triple.create(p, typeNode, varNode));
                        inner.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
                        inner.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
                        inner.add(Triple.create(p, typeNode, varNode));
                    }
                    //Iterate through all conjunctive queries.
                    while (cQueries.hasNext()) {
                        Node cRoot = cQueries.next();
                        Graph cGraph = ge.extract(cRoot, inner);
                        List<Node> cqVars = GraphUtil.listSubjects(cGraph, typeNode, varNode).toList();
                        List<Node> projectedVarsInCQ = new ArrayList<>();
                        //Ground all variables that appear outside the CQ.
                        for (Node f : outerVars) {
                            if (cqVars.contains(f)) {
                                cGraph.add(Triple.create(f, typeNode, varNode));
                                cGraph.add(Triple.create(f, valueNode, NodeFactory.createLiteral(f.getBlankNodeLabel())));
                                cGraph.add(Triple.create(f, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
                                projectedVarsInCQ.add(f);
                            }
                        }
                        if (!partitionsByVars.containsKey(projectedVarsInCQ)) {
                            partitionsByVars.put(projectedVarsInCQ, new ArrayList<>());
                        }
                        List<RGraph> currentPartition = partitionsByVars.get(projectedVarsInCQ);
                        currentPartition.add(cqMinimisation(cRoot, cGraph));
                        partitionsByVars.put(projectedVarsInCQ, currentPartition);
                        result.add(cqMinimisation(cRoot, cGraph));
                    }
                    result = containmentChecksPerPartition(partitionsByVars);
                    RGraph eg = new RGraph(root, outer, vars);
                    Node uNode = NodeFactory.createBlankNode();
                    //If there's only a single non-redundant CQ.
                    if (result.size() == 1) {
                        RGraph e = result.get(0);
                        Node eRoot = e.root;
                        eg.merge(e);
                        eg.graph.add(Triple.create(subjectUnion, predicateUnion, eRoot));
                        eg.graph.delete(Triple.create(subjectUnion, predicateUnion, ucq));
                        eg.eliminateRedundantJoins();
                    } else {
                        eg.graph.add(Triple.create(subjectUnion, predicateUnion, uNode));
                        eg.graph.add(Triple.create(uNode, typeNode, unionNode));
                        for (RGraph e : result) {
                            Node eRoot = e.root;
                            eg.merge(e);
                            eg.graph.add(Triple.create(uNode, argNode, eRoot));
                            eg.eliminateRedundantJoins();
                        }
                        eg.graph.delete(Triple.create(subjectUnion, predicateUnion, ucq));
                    }
                    //eg.setDistinctNode(true);
                    //UpdateAction.execute(branchCleanUpRule, eg.graph);
                    eg.branchUnion(varMap);
                    this.graph = eg.graph;
                }
            } else { // BGP instead
                if (GraphUtil.listObjects(graph, ucq, typeNode).next().equals(joinNode)) {
                    if (isUCQ(ucq)) {
                        Node subjectBGP, predicateBGP;
                        if (listSubjects(graph, opNode, ucq).hasNext()) {
                            subjectBGP = listSubjects(graph, opNode, ucq).next();
                            predicateBGP = opNode;
                        } else if (listSubjects(graph, leftNode, ucq).hasNext()) {
                            subjectBGP = listSubjects(graph, leftNode, ucq).next();
                            predicateBGP = leftNode;
                        } else if (listSubjects(graph, rightNode, ucq).hasNext()) {
                            subjectBGP = listSubjects(graph, rightNode, ucq).next();
                            predicateBGP = rightNode;
                        } else if (listSubjects(graph, subNode, ucq).hasNext()) {
                            subjectBGP = listSubjects(graph, subNode, ucq).next();
                            predicateBGP = subNode;
                        } else if (listSubjects(graph, valueNode, ucq).hasNext()) {
                            subjectBGP = listSubjects(graph, valueNode, ucq).next();
                            predicateBGP = valueNode;
                        } else {
                            subjectBGP = listSubjects(graph, argNode, ucq).next();
                            predicateBGP = argNode;
                        }
                        Graph inner = ge.extract(ucq, graph);
                        Graph outer = GraphFactory.createPlainGraph();
                        CustomTripleBoundary ctb = new CustomTripleBoundary(Collections.singletonList(ucq), null);
                        GraphExtract extract = new GraphExtract(ctb);
                        outer = extract.extract(root, graph);
                        List<Node> innerVars = GraphUtil.listSubjects(inner, typeNode, varNode).toList();
                        List<Node> outerVars = GraphUtil.listSubjects(outer, typeNode, varNode).toList();
                        outer.remove(subjectBGP, predicateBGP, ucq);
                        for (Node p : innerVars) {
                            inner.delete(Triple.create(p, typeNode, varNode));
                        }
                        for (Node p : outerVars) {
                            outer.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
                            outer.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
                            outer.add(Triple.create(p, typeNode, varNode));
                            inner.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
                            inner.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
                            inner.add(Triple.create(p, typeNode, varNode));
                        }
                        //Create a new r-graph based on this conjunctive query.
                        RGraph e = cqMinimisation(ucq, inner);
                        RGraph eg = new RGraph(root, outer, this.vars);
                        Node eRoot = e.root;
                        if (e.graph.contains(Triple.create(e.root, typeNode, joinNode))) {
                            List<Node> bgps = GraphUtil.listObjects(e.graph, e.root, argNode).toList();
                            if (bgps.size() == 1) {
                                eRoot = bgps.get(0);
                            }
                        }
                        eg.merge(e);
                        eg.graph.delete(Triple.create(subjectBGP, predicateBGP, ucq));
                        eg.graph.add(Triple.create(subjectBGP, predicateBGP, eRoot));
                        eg.root = root;
                        eg.eliminateRedundantJoins();
                        eg.branchUnion(varMap);
                        this.graph = eg.graph;
                    }
                }
            }
            result = new ArrayList<>();
        }
        //UpdateAction.execute(branchCleanUpRule, graph);
        //UpdateAction.execute(branchUnionRule, graph);
        removeTemporaryVars();
        //UpdateAction.execute(joinRule, graph);
        return this;
    }

    public boolean isUCQ(Node n) {
        Node type = GraphUtil.listObjects(graph, n, typeNode).next();
        boolean ans = true;
        if (type.equals(unionNode) || type.equals(joinNode)) {
            ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
            while (args.hasNext()) {
                Node arg = args.next();
                ans = ans & isUCQ(arg);
            }
            return ans;
        } else return type.equals(tpNode);
    }

    public void removeTemporaryVars() {
        ExtendedIterator<Node> tempNodes = GraphUtil.listSubjects(graph, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
        while (tempNodes.hasNext()) {
            Node temp = tempNodes.next();
            Node value = GraphUtil.listObjects(graph, temp, valueNode).hasNext() ? GraphUtil.listObjects(graph, temp, valueNode).next() : null;
            graph.delete(Triple.create(temp, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
            if (value != null) {
                graph.delete(Triple.create(temp, valueNode, value));
            }
            if (graph.contains(Triple.create(temp, typeNode, varNode))) {
                graph.delete(Triple.create(temp, typeNode, varNode));
            }
        }
    }

    public void branchUnion(Map<Node, Node> varMap) {
        //UpdateAction.execute(branchUnionRule, graph);
        ExtendedIterator<Node> vars = GraphUtil.listSubjects(graph, typeNode, varNode);
        while (vars.hasNext()) {
            Node var = vars.next();
            Node value = GraphUtil.listObjects(graph, var, valueNode).hasNext() ? GraphUtil.listObjects(graph, var, valueNode).next() : null;
            if (value != null) {
                String string = value.toString().replace("\"", "");
                Var var1 = Var.alloc(string);
                if (!varMap.containsKey(var1)) {
                    varMap.put(var1, var);
                }
                graph.delete(Triple.create(var, valueNode, value));
            }
        }
    }

	/**
	 * Deletes all JOINs with only a single triple pattern.
	 */
	public void eliminateRedundantJoins() {
        ExtendedIterator<Node> joins = GraphUtil.listSubjects(graph, typeNode, joinNode);
        while (joins.hasNext()) {
            Node join = joins.next();
            List<Node> args = GraphUtil.listObjects(graph, join, argNode).toList();
            if (args.size() == 1) {
                Node arg = args.get(0);
                if (graph.contains(Triple.create(arg, typeNode, tpNode))) {
                    Node subject = GraphUtil.listObjects(graph, arg, subjectNode).next();
                    Node predicate = GraphUtil.listObjects(graph, arg, preNode).next();
                    Node object = GraphUtil.listObjects(graph, arg, objNode).next();
                    graph.delete(Triple.create(arg, subjectNode, subject));
                    graph.delete(Triple.create(arg, preNode, predicate));
                    graph.delete(Triple.create(arg, objNode, object));
                    graph.delete(Triple.create(arg, typeNode, tpNode));
                    graph.delete(Triple.create(join, typeNode, joinNode));
                    graph.add(Triple.create(join, typeNode, tpNode));
                    graph.add(Triple.create(join, subjectNode, subject));
                    graph.add(Triple.create(join, preNode, predicate));
                    graph.add(Triple.create(join, objNode, object));
                }
            }
        }
    }

	/**
	 *
	 * @param cRoot The root of the r-graph to minimise
	 * @param cGraph The graph to minimise.
	 * @return Returns the lean form of this conjunctive query's r-graph.
	 * @throws InterruptedException
	 */
    public RGraph cqMinimisation(Node cRoot, Graph cGraph) throws InterruptedException {
        RGraph e = new RGraph(cRoot, cGraph, this.vars);
        //UpdateAction.execute(filterVarsRule, e.graph);
        ExtendedIterator<Node> cqFilterVars = listSubjects(e.graph, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
        while (cqFilterVars.hasNext()) {
            Node p = cqFilterVars.next();
            e.graph.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
        }
        RGraph a = null;
        a = e.getLeanForm();
        if (a.graph.contains(a.root, typeNode, joinNode)) {
            List<Node> bgps = GraphUtil.listObjects(a.graph, a.root, argNode).toList();
            if (bgps.size() == 1) {
                Node bgp = bgps.get(0);
                a.graph.delete(Triple.create(a.root, argNode, bgp));
                a.graph.delete(Triple.create(a.root, typeNode, joinNode));
                a.root = bgp;
            }
        }
        return a;
    }


    public List<RGraph> containmentChecksPerPartition(Map<List<Node>, List<RGraph>> partitions) {
        List<RGraph> result = new ArrayList<>();
        for (List<RGraph> partition : partitions.values()) {
            List<RGraph> minPartition = containmentChecks(partition);
            result.addAll(minPartition);
        }
        return result;
    }

    public void branchCleanUp(Graph graph) {

    }

    public List<RGraph> containmentChecks(List<RGraph> result) {
        List<RGraph> redundant = new ArrayList<>();
        List<Integer> redundantIds = new ArrayList<>();
        long time = System.nanoTime();
        int numberOfChecks = 0;
        for (int i = 0; i < result.size(); i++) {
            for (int j = i + 1; j < result.size(); j++) {
                boolean a = false;
                boolean b = false;
                RGraph e = result.get(i);
                RGraph e1 = result.get(j);
                ExtendedIterator<Triple> t0 = GraphUtil.findAll(e1.graph);
                ExtendedIterator<Triple> t1 = GraphUtil.findAll(e.graph);
                BasicPattern b0 = new BasicPattern();
                BasicPattern b1 = new BasicPattern();
                while (t0.hasNext()) {
                    Triple t = t0.next();
                    b0.add(getTripleWithVars(t));
                }
                while (t1.hasNext()) {
                    Triple t = t1.next();
                    b1.add(getTripleWithVars(t));
                }
                Query q = OpAsQuery.asQuery(new OpBGP(b0));
                Query q1 = OpAsQuery.asQuery(new OpBGP(b1));
                q.setQueryAskType();
                q1.setQueryAskType();
                Model m0 = ModelFactory.createModelForGraph(e.graph);
                Model m1 = ModelFactory.createModelForGraph(e1.graph);
                if (!redundantIds.contains(i)) {
                    QueryExecution qexec = QueryExecutionFactory.create(q, m0); // Q is contained in Q1
                    a = qexec.execAsk();
                    numberOfChecks++;
                }
                if (!redundantIds.contains(j)) {
                    QueryExecution qexec1 = QueryExecutionFactory.create(q1, m1); // Q1 is contained in Q
                    b = qexec1.execAsk();
                    numberOfChecks++;
                }
                if (a && b) {
                    //redundant.add(e);
                    redundantIds.add(i);
                } else if (a) {
                    redundantIds.add(i);
                } else if (b) {
                    redundantIds.add(j);
                }
            }
        }
        for (int i : redundantIds) {
            redundant.add(result.get(i));
        }
        for (RGraph e : redundant) {
            result.remove(e);
        }
        time = System.nanoTime() - time;
        System.out.println("Containment checks: " + time + " ns");
        System.out.println("Number of containment checks: " + numberOfChecks);
        return result;
    }

    public int getNumberOfNodes() {
        return this.graph.size();
    }

    public int getNumberOfTriples() {
        ExtendedIterator<Node> triples = listSubjects(graph, typeNode, tpNode);
        return triples.toList().size();
    }

    public int getNumberOfVars() {
        return listSubjects(graph, typeNode, varNode).toList().size();
    }

    public Node nxToJenaNode(org.semanticweb.yars.nx.Node node) {
        Node ans;
        if (Pattern.matches("_:.+", node.toN3())) {
            String s = BNode.unescapeForBNode(node.toN3());
            s = s.substring(s.indexOf(":") + 1);
            if (s.startsWith("j")) {
                s = s.substring(1);
            }
            ans = NodeFactory.createBlankNode(s);
        } else if (Pattern.matches("<.+>", node.toN3())) {
            ans = NodeFactory.createURI(node.toN3().replaceAll("[<>]", ""));
        } else {
            ans = createLiteralWithType(node.toN3());
        }
        return ans;
    }

    public void merge(RGraph graph) {
        this.varMap.putAll(graph.varMap);
        this.exprMap.putAll(graph.exprMap);
        this.typeMap.putAll(graph.typeMap);
        GraphUtil.addInto(this.graph, graph.graph);
    }

    /**
     * @param n A node that may be a literal or a variable.
     * @return A blank node if the input is a variable or a literal with the correct language or datatype otherwise.
     */
    public Node getValidNode(Node n) {
        if (n.isVariable()) {
            Node ans = NodeFactory.createBlankNode(n.getName());
            graph.add(Triple.create(ans, typeNode, varNode));
            typeMap.put(ans, varNode);            varMap.put(Var.alloc(n), ans);
            return ans;
        }
        if (n.isLiteral()) {
            if (n.getLiteralLanguage().equals("")) {
                return NodeFactory.createLiteralByValue(n.getLiteralLexicalForm(), n.getLiteralDatatype());
            } else {
                return NodeFactory.createLiteral(n.getLiteralLexicalForm() + "@" + n.getLiteralLanguage());
            }
        } else {
            return n;
        }
    }

    @Override
    public int hashCode() {
        if (hashCode != null) {
            return this.hashCode.asInt();
        }
        else {
            return this.graph.hashCode();
        }
    }

    public long getLabelTime() {
        return labelTime;
    }


    public void setLabelTime(long labelTime) {
        this.labelTime = labelTime;
    }


    public long getMinimisationTime() {
        return minimisationTime;
    }


    public void setMinimisationTime(long minimisationTime) {
        this.minimisationTime = minimisationTime;
    }
}
