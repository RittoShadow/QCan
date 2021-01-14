package transformers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.Op0;
import org.apache.jena.sparql.algebra.op.Op1;
import org.apache.jena.sparql.algebra.op.Op2;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpN;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_NegPropSet;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.sparql.path.PathWriter;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.sse.writers.WriterPath;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.yars.nx.NodeComparator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cl.uchile.dcc.blabel.jena.JenaModelIterator;
import cl.uchile.dcc.blabel.lean.DFSGraphLeaning;
import cl.uchile.dcc.blabel.lean.GraphLeaning.GraphLeaningResult;
import paths.PGraph;
import visitors.TopDownVisitor;

@SuppressWarnings("unused")
public class BGPCollapser extends TransformCopy {
	List<Var> projectedVars;
	Set<Node> namedVars = new HashSet<>();
	Op op;
	List<Pair<Node,Node>> edges = new ArrayList<>();
	List<Pair<Node,Node>> multiEdges = new ArrayList<>();
	List<Pair<Pair<Node,Node>,Path>> pathEdges = new ArrayList<>();
	BiMap<Var,Node> varMap = HashBiMap.create();
	boolean sequenceMode = true;
	boolean setSemantics = false;
	boolean enableUnion = true;
	
	public BGPCollapser(Query q) {
		projectedVars = q.getProjectVars();
		op = Algebra.compile(q);
	}
	
	public BGPCollapser(Op op, List<Var> projectedVars, boolean sequence) {
		this.projectedVars = projectedVars;
		this.op = op;
		this.sequenceMode = sequence;
	}
	
	public BGPCollapser(Op op, List<Var> projectedVars, boolean sequence, boolean union) {
		this(op, projectedVars, sequence);
		this.enableUnion = union;
	}
	
	public void computeEdges(Graph g) {
		ExtendedIterator<Triple> triples = GraphUtil.findAll(g);
		while (triples.hasNext()) {
			Triple triple = triples.next();
			if (triple.getPredicate().isBlank() || triple.getPredicate().isVariable()) {
				namedVars.add(triple.getSubject());
				namedVars.add(triple.getPredicate());
			}
			edges.add(new Pair<Node,Node>(triple.getSubject(),triple.getObject()));
		}
		Map<Pair<Node,Node>,Integer> edgeInstances = new HashMap<Pair<Node,Node>,Integer>();
		for (Pair<Node,Node> pair : edges) {
			Node nLeft = pair.getLeft();
			Node nRight = pair.getRight();
			if (nLeft.getBlankNodeLabel().compareTo(nRight.getBlankNodeLabel()) <= 0) {
				if (!edgeInstances.containsKey(pair)) {
					edgeInstances.put(pair, 1);
				}
				else {
					edgeInstances.put(pair, edgeInstances.get(pair) + 1);
				}
			}
			else {
				Pair<Node,Node> inversePair = new Pair<Node,Node>(nRight,nLeft);
				if (!edgeInstances.containsKey(inversePair)) {
					edgeInstances.put(inversePair, 1);
				}
				else {
					edgeInstances.put(inversePair, edgeInstances.get(inversePair) + 1);
				}
			}
		}
		for (Map.Entry<Pair<Node,Node>, Integer> entry : edgeInstances.entrySet()) {
			if (entry.getValue() > 1) {
				multiEdges.add(new Pair<Node,Node>(entry.getKey().getLeft(), entry.getKey().getRight()));
			}
		}
	}
	
	public void computeEdges(OpBGP op) {
		BasicPattern bp = op.getPattern();
		List<Triple> triples = bp.getList();
		for (Triple t : triples) {
			if (t.getPredicate().isURI()) {
				edges.add(new Pair<Node, Node>(t.getSubject(),t.getObject()));
			}
		}
	}
	
	public void computeEdges(OpSequence op) {
		for (Op o : op.getElements()) {
			if (o instanceof OpBGP) {
				computeEdges((OpBGP) o);
			}
			else if (o instanceof OpPath) {
				TriplePath tp = ((OpPath) o).getTriplePath();
				edges.add(new Pair<Node,Node>(tp.getSubject(),tp.getObject()));
			}
			else if (o instanceof OpJoin) {
				List<Triple> triples = triplesInJoin(o);
				for (Triple t : triples) {
					edges.add(new Pair<Node,Node>(t.getSubject(),t.getObject()));
				}
			}
			else {
				System.exit(-1);
			}
		}
	}
	
	public void determineNamedVariables() {
		Map<Node,Integer> varInstances = new HashMap<Node,Integer>();
		for (Pair<Node,Node> edge : edges) {
			if (!varInstances.containsKey(edge.getLeft())) {
				varInstances.put(edge.getLeft(),1);
			}
			else {
				varInstances.put(edge.getLeft(),varInstances.get(edge.getLeft()) + 1);
			}
			if (!varInstances.containsKey(edge.getRight())) {
				varInstances.put(edge.getRight(),1);
			}
			else {
				varInstances.put(edge.getRight(),varInstances.get(edge.getRight()) + 1);
			}
		}
		for (Map.Entry<Node, Integer> v : varInstances.entrySet()) {
			if (v.getKey().isBlank()) {
				if (projectedVars.contains(varMap.inverse().get(v.getKey()))) {
					namedVars.add(v.getKey());
				}
				else if (v.getValue() != 2) {
					namedVars.add(v.getKey());
				}
			}
		}
	}
	
	public Op transform(OpBGP op) {
		if (sequenceMode) {
			return op;
		}
		Graph g = GraphFactory.createPlainGraph();
//		List<Node> namedVariables = new ArrayList<Node>();
		BasicPattern bp = op.getPattern();
		BasicPattern ansBp = new BasicPattern();
		List<Triple> triples = bp.getList();
		List<Triple> otherTriples = new ArrayList<Triple>();
		for (Triple t : triples) {
			Node s = getValidNode(t.getSubject());
			Node p = getValidNode(t.getPredicate());
			Node o = getValidNode(t.getObject());
//			if (s.isURI() || s.isLiteral() || o.isURI() || o.isLiteral()) {
//				otherTriples.add(Triple.create(s, p, o));
//			}
			if (t.getSubject().isVariable()) {
				varMap.put((Var) t.getSubject(), s);
			}
			if (t.getPredicate().isVariable()) {
				varMap.put((Var) t.getPredicate(), p); 
			}
			if (t.getObject().isVariable()) {
				varMap.put((Var) t.getObject(), o);
			}
			g.add(Triple.create(s, p, o));
		}
		try {
			g = getLeanForm(g);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ExtendedIterator<Triple> gTriples = GraphUtil.findAll(g);
		while (gTriples.hasNext()) {
			Triple t = gTriples.next();
			Node s = t.getSubject();
			Node p = t.getPredicate();
			Node o = t.getObject();
			if (s.isBlank()) {
				s = Var.alloc(s.getBlankNodeLabel());
			}
			if (p.isBlank()) {
				p = Var.alloc(p.getBlankNodeLabel());
			}
			if (o.isBlank()) {
				o = Var.alloc(o.getBlankNodeLabel());
			}
			ansBp.add(Triple.create(s, p, o));
		}
//		computeEdges(g);
//		determineNamedVariables();
//		namedVariables.addAll(namedVars);
//		for (int j = 0; j < namedVariables.size(); j++) {
//			Node n = namedVariables.get(j);
//			for (int k = 0; k <= j; k++) {
//				Node m = namedVariables.get(k);
//				List<List<Node>> paths.paths = pathsBetween(n,m);
//				for (List<Node> path : paths.paths) {
//					if (path.isEmpty()) {
//						ExtendedIterator<Node> triplePaths = GraphUtil.listPredicates(g, n, m);
//						ExtendedIterator<Node> reversePaths = GraphUtil.listPredicates(g, m, n);
//						while (triplePaths.hasNext()) {
//							pathEdges.add(new Pair<Pair<Node, Node>, Path>(new Pair<Node, Node>(n,m), new P_Link(triplePaths.next())));
//						}
//						while (reversePaths.hasNext()) {
//							pathEdges.add(new Pair<Pair<Node, Node>, Path>(new Pair<Node, Node>(m,n), new P_Link(reversePaths.next())));
//						}
//						
//					}
//					else if (n.equals(m) && path.size() == 1) { //Obscure case when there's a two node loop.
//						List<Node> triplePaths = GraphUtil.listPredicates(g, n, path.get(0)).toList();
//						List<Node> reversePaths = GraphUtil.listPredicates(g, path.get(0), n).toList();
//						Path p = null;
//						List<Path> twoPaths = new ArrayList<Path>();
//						for (Node t : triplePaths) {
//							twoPaths.add(PathFactory.pathLink(t));
//						}
//						for (Node t : reversePaths) {
//							twoPaths.add(PathFactory.pathInverse(PathFactory.pathLink(t)));
//						}
//						p = PathFactory.pathSeq(twoPaths.get(0), PathFactory.pathInverse(twoPaths.get(1)));
//						pathEdges.add(new Pair<Pair<Node,Node>,Path>(new Pair<Node,Node>(n,m),p));
//					}
//					else {
//						Node current = n;
//						Path p = null;
//						for (int i = 0; i < path.size() + 1; i++) {
//							Node next;
//							if (i == path.size()) {
//								next = m;
//							}
//							else {
//								next = path.get(i);
//							}
//							ExtendedIterator<Node> triplePaths = GraphUtil.listPredicates(g, current, next);
//							ExtendedIterator<Node> reversePaths = GraphUtil.listPredicates(g, next, current);
//							current = next;
//							while (triplePaths.hasNext()) {
//								if (i == 0) {
//									p = new P_Link(triplePaths.next());
//								}
//								else {
//									p = new P_Seq(p, new P_Link(triplePaths.next()));
//								}
//							}
//							while (reversePaths.hasNext()) {
//								if (i == 0) {
//									p = new P_Inverse(new P_Link(reversePaths.next()));
//								}
//								else {
//									p = new P_Seq(p, new P_Inverse(new P_Link(reversePaths.next())));
//								}
//							}
//						}
//						pathEdges.add(new Pair<Pair<Node, Node>, Path>(new Pair<Node, Node>(n,m), p));
//					}
//				}
//			}
//		}
//		for (Pair<Pair<Node,Node>,Path> pathEdge : pathEdges) {
//			Pair<Node,Node> subjectObject = pathEdge.getLeft();
//			Path p = pathEdge.getRight();
//			Node s = varMap.containsValue(subjectObject.getLeft()) ? varMap.inverse().get(subjectObject.getLeft()) : NodeFactory.createBlankNode();
//			Node ob = varMap.containsValue(subjectObject.getRight()) ? varMap.inverse().get(subjectObject.getRight()) : NodeFactory.createBlankNode();
//			TriplePath tp = new TriplePath(s, p, ob);
//			PathTransform pt = new PathTransform();
//			Op o = pt.getResult(tp);
//			((OpSequence) ans).add(o);
//		}
//		if (!otherTriples.isEmpty()) {
//			BasicPattern bp1 = new BasicPattern();
//			for (Triple t : otherTriples) {
//				bp1.add(t);
//			}
//			((OpSequence) ans).add(new OpBGP(bp1));
//		}
//		if (((OpSequence) ans).getElements().size() == 1){
//			ans = ((OpSequence) ans).get(0);
//		}
		clean();
		Op ans = new OpBGP(ansBp);
		return ans;	
	}
	
	public Op transform(OpSequence op, List<Op> listOp) {
		if (!sequenceMode) {
			return op;
		}
		Graph g = GraphFactory.createPlainGraph();
//		List<Node> namedVariables = new ArrayList<Node>();
		List<Triple> triples = new ArrayList<Triple>();
		for (Op o : listOp) {
			if (o instanceof OpBGP) { // Add all triples in BGPs to graph
				BasicPattern bp = ((OpBGP) o).getPattern();
				triples.addAll(bp.getList());
			}
			else if (o instanceof OpTriple) {
				triples.add(((OpTriple) o).getTriple());
			}
			else if (o instanceof OpPath) { // Paths
				TriplePath tp = ((OpPath) o).getTriplePath();
				if (tp.getPath() instanceof P_NegPropSet) {
					triples.add(Triple.create(tp.getSubject(), NodeFactory.createLiteral(WriterPath.asString(tp.getPath())), tp.getObject()));
				}
				else {
					PGraph pg = new PGraph(tp);
					Path p = pg.getNormalisedPath();
					if (tp.getSubject().equals(tp.getObject())) {
						if (tp.getPath() instanceof P_ZeroOrMore1) {
							
						}
						else {
							triples.add(Triple.create(tp.getSubject(), NodeFactory.createLiteral(WriterPath.asString(p)), tp.getObject()));
						}
					}
					else {
						triples.add(Triple.create(tp.getSubject(), NodeFactory.createLiteral(WriterPath.asString(p)), tp.getObject()));
					}
				}
			}
			else if (o instanceof OpJoin) {
				triples.addAll(triplesInJoin(o));
			}
		}
		Op ans = OpSequence.create();
		List<Triple> otherTriples = new ArrayList<Triple>();
		for (Triple t : triples) {
			Node s = getValidNode(t.getSubject());
			Node p = getValidNode(t.getPredicate());
			Node o = getValidNode(t.getObject());
//			if (s.isURI() || s.isLiteral() || o.isURI() || o.isLiteral()) {
//				otherTriples.add(Triple.create(s, p, o));
//			}
			if (t.getSubject().isVariable()) {
				varMap.put((Var) t.getSubject(), s);
			}
			if (t.getPredicate().isVariable()) {
				varMap.put((Var) t.getPredicate(), p); 
			}
			if (t.getObject().isVariable()) {
				varMap.put((Var) t.getObject(), o);
			}
			g.add(Triple.create(s, p, o));
		}
		try {
			g = getLeanForm(g);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ExtendedIterator<Triple> gTriples = GraphUtil.findAll(g);
		List<TriplePath> finalTriples = new ArrayList<TriplePath>();
		while (gTriples.hasNext()) {
			Triple t = gTriples.next();
			Node s = t.getSubject();
			Node p = t.getPredicate();
			Node o = t.getObject();
			Path pre = null;
			if (s.isVariable()) {
				s = NodeFactory.createBlankNode(t.getSubject().getName());
			}
			if (p.isVariable()) {
				p = NodeFactory.createBlankNode(t.getPredicate().getName());
			}
			else if (p.isLiteral()) {
				pre = SSE.parsePath(p.getLiteralLexicalForm());
			}
			if (o.isVariable()) {
				o = NodeFactory.createBlankNode(t.getObject().getName());
			}
			if (pre == null) {
				otherTriples.add(t);
			}
			else {
				finalTriples.add(new TriplePath(s, pre, o));
			}
		}
//		computeEdges(g);
//		determineNamedVariables();
//		namedVariables.addAll(namedVars);
//		for (int i = 0 ; i < namedVariables.size(); i++) {
//			Node n = namedVariables.get(i);
//			for (int j = 0; j <= i; j++) {
//				Node m = namedVariables.get(j);
//				List<List<Node>> paths.paths = pathsBetween(n,m);
//				for (List<Node> path : paths.paths) {
//					if (path.isEmpty()) {
//						ExtendedIterator<Node> triplePaths = GraphUtil.listPredicates(g, n, m);
//						ExtendedIterator<Node> reversePaths = GraphUtil.listPredicates(g, m, n);
//						while (triplePaths.hasNext()) {
//							Node triplePath = triplePaths.next();
//							if (triplePath.isLiteral()) {
//								pathEdges.add(new Pair<Pair<Node, Node>, Path>(new Pair<Node, Node>(n,m), SSE.parsePath(triplePath.getLiteralLexicalForm())));
//							}
//							else {
//								pathEdges.add(new Pair<Pair<Node, Node>, Path>(new Pair<Node, Node>(n,m), new P_Link(triplePath)));
//							}
//						}
//						if (!n.equals(m)) {
//							while (reversePaths.hasNext()) {
//								Node reversePath = reversePaths.next();
//								if (reversePath.isLiteral()) {
//									pathEdges.add(new Pair<Pair<Node, Node>, Path>(new Pair<Node, Node>(m,n), SSE.parsePath(reversePath.getLiteralLexicalForm())));
//								}
//								else {
//									pathEdges.add(new Pair<Pair<Node, Node>, Path>(new Pair<Node, Node>(m,n), new P_Link(reversePath)));
//								}
//							}
//						}		
//					}
//					else if (n.equals(m) && path.size() == 1) { //Obscure case when there's a two node loop.
//						List<Node> triplePaths = GraphUtil.listPredicates(g, n, path.get(0)).toList();
//						List<Node> reversePaths = GraphUtil.listPredicates(g, path.get(0), n).toList();
//						Path p = null;
//						List<Path> twoPaths = new ArrayList<Path>();
//						for (Node t : triplePaths) {
//							if (t.isLiteral()) {
//								twoPaths.add(SSE.parsePath(t.getLiteralLexicalForm()));
//							}
//							else {
//								twoPaths.add(PathFactory.pathLink(t));
//							}
//						}
//						for (Node t : reversePaths) {
//							if (t.isLiteral()) {
//								twoPaths.add(SSE.parsePath(t.getLiteralLexicalForm()));
//							}
//							else {
//								twoPaths.add(PathFactory.pathInverse(PathFactory.pathLink(t)));
//							}
//						}
//						p = PathFactory.pathSeq(twoPaths.get(0), PathFactory.pathInverse(twoPaths.get(1)));
//						if (twoPaths.get(0) instanceof P_ZeroOrMore1 && twoPaths.get(1) instanceof P_ZeroOrMore1) {
//							//do nothing
//						}
//						else {
//							pathEdges.add(new Pair<Pair<Node,Node>,Path>(new Pair<Node,Node>(n,m),p));
//						}
//						
//					}
//					else {
//						Node current = n;
//						Path p = null;
//						for (int k = 0; k < path.size() + 1; k++) {
//							Node next;
//							if (k == path.size()) {
//								next = m;
//							}
//							else {
//								next = path.get(k);
//							}
//							ExtendedIterator<Node> triplePaths = GraphUtil.listPredicates(g, current, next);
//							ExtendedIterator<Node> reversePaths = GraphUtil.listPredicates(g, next, current);
//							current = next;
//							while (triplePaths.hasNext()) {
//								Node triplePath = triplePaths.next();
//								if (k == 0) {
//									if (triplePath.isLiteral()) {
//										p = SSE.parsePath(triplePath.getLiteralLexicalForm());
//									}
//									else {
//										p = new P_Link(triplePath);
//									}
//								}
//								else {
//									if (triplePath.isLiteral()) {
//										p = new P_Seq(p, SSE.parsePath(triplePath.getLiteralLexicalForm()));
//									}
//									else {
//										p = new P_Seq(p, new P_Link(triplePath));
//									}
//								}
//							}
//							while (reversePaths.hasNext()) {
//								Node reversePath = reversePaths.next();
//								if (k == 0) {
//									if (reversePath.isLiteral()) {
//										p = new P_Inverse(SSE.parsePath(reversePath.getLiteralLexicalForm()));
//									}
//									else {
//										p = new P_Inverse(new P_Link(reversePath));
//									}
//								}
//								else {
//									if (reversePath.isLiteral()) {
//										p = new P_Seq(p, new P_Inverse(SSE.parsePath(reversePath.getLiteralLexicalForm())));
//									}
//									else {
//										p = new P_Seq(p, new P_Inverse(new P_Link(reversePath)));
//									}
//								}
//							}
//							
//						}
//						pathEdges.add(new Pair<Pair<Node, Node>, Path>(new Pair<Node, Node>(n,m), p));
//					}
//				}
//			}
//		}
//		for (Pair<Pair<Node,Node>,Path> pathEdge : pathEdges) {
//			Pair<Node,Node> subjectObject = pathEdge.getLeft();
//			Path p = pathEdge.getRight();
//			Node s = varMap.containsValue(subjectObject.getLeft()) ? varMap.inverse().get(subjectObject.getLeft()) : NodeFactory.createBlankNode();
//			Node ob = varMap.containsValue(subjectObject.getRight()) ? varMap.inverse().get(subjectObject.getRight()) : NodeFactory.createBlankNode();
//			TriplePath tp = new TriplePath(s, p, ob);
//			PathTransform pt = new PathTransform();
//			Op o = pt.getResult(tp);
//			((OpSequence) ans).add(o);
//		}
		if (!otherTriples.isEmpty()) {
			BasicPattern bp1 = new BasicPattern();
			for (Triple t : otherTriples) {
				bp1.add(t);
			}
			((OpSequence) ans).add(new OpBGP(bp1));
		}
		if (!finalTriples.isEmpty()) {
			for (TriplePath tp : finalTriples) {
				((OpSequence) ans).add(new OpPath(tp));
			}
		}
		if (((OpSequence) ans).getElements().size() == 1){
			ans = ((OpSequence) ans).get(0);
		}
		ans = containment(ans);
		clean();
		return ans;
	}
	
	public Op transform(OpJoin op, Op left, Op right) {
		List<Op> opsInJoin = opsInJoin(op);
		OpSequence opSeq = OpSequence.create();
		for (Op o : opsInJoin) {
			opSeq.add(o);
		}
		return transform(opSeq, opSeq.getElements());
	}
	
	public Op transform(OpUnion op, Op left, Op right) {
		if (enableUnion) {
			List<Op> ops = allOpsInUnion(op);
			List<Op> newOps = new ArrayList<Op>();
			for (Op o : ops) {
				Op newAns = null;
				if (o instanceof OpBGP) {
					sequenceMode = false;
					newAns = transform((OpBGP) o);
					sequenceMode = true;
				}
				else if (o instanceof OpTriple) {
					Triple t = ((OpTriple) o).getTriple();
//					if (t.getPredicate().isURI()) {
//						newAns = new OpPath(new TriplePath(t.getSubject(), PathFactory.pathLink(t.getPredicate()), t.getObject()));
//					}
//					else {
//						newAns = o;
//					}
					newAns = o;
				}
				else if (o instanceof OpJoin) {
					TopDownVisitor tdv = new TopDownVisitor(o, this.projectedVars);
					newAns = tdv.getOp();
				}
				else if (o instanceof OpSequence) {
					TopDownVisitor tdv = new TopDownVisitor(o, this.projectedVars);
					newAns = tdv.getOp();
				}
				else {
					newAns = o;
				}
				if (newAns instanceof OpPath) {
					newOps.add(newAns);
				}
				else if (newAns instanceof OpTriple) {
					Triple t = ((OpTriple) newAns).getTriple();
//					if (t.getPredicate().isURI()) {
//						newAns = new OpPath(new TriplePath(t.getSubject(), PathFactory.pathLink(t.getPredicate()), t.getObject()));
//						newOps.add(newAns);
//					}
					newOps.add(newAns);
				}
				else {
					newOps.add(newAns);
				}
			}
//			List<Op> copyOps = new ArrayList<Op>();
//			Map<Pair<Node,Node>,Path> partitions = new HashMap<Pair<Node,Node>,Path>();
//			Node auxSubject = Var.alloc(NodeFactory.createBlankNode().getBlankNodeLabel());
//			Node auxObject = Var.alloc(NodeFactory.createBlankNode().getBlankNodeLabel());
//			copyOps.addAll(newOps);
//			for (int i = 0; i < copyOps.size(); i++) {
//				if (copyOps.get(i) instanceof OpPath) {
//					OpPath opA = (OpPath) copyOps.get(i);
//					newOps.remove(opA);
//					TriplePath tpA = opA.getTriplePath();
//					Node subjectA = tpA.getSubject();
//					Node objectA = tpA.getObject();
//					if (!projectedVars.contains(subjectA)) {
//						subjectA = auxSubject;
//					}
//					if (!projectedVars.contains(objectA)) {
//						objectA = auxObject;
//					}
//					Pair<Node,Node> pair = new Pair<Node,Node>(subjectA, objectA);
//					if (partitions.containsKey(pair)) {
//						partitions.put(pair, PathFactory.pathAlt(partitions.get(pair), tpA.getPath()));
//					}
//					else {
//						partitions.put(pair, tpA.getPath());
//					}
//				}
//			}
			Op ans = null;
			for (Op o : newOps) {
				if (ans == null) {
					ans = o;
				}
				else {
					ans = OpUnion.create(ans, o);
				}
			}
//			for (Map.Entry<Pair<Node,Node>, Path> entry : partitions.entrySet()) {
//				if (ans == null) {
//					ans = new OpPath(new TriplePath(entry.getKey().getLeft(), entry.getValue(), entry.getKey().getRight()));
//				}
//				else {
//					ans = OpUnion.create(ans, new OpPath(new TriplePath(entry.getKey().getLeft(), entry.getValue(), entry.getKey().getRight())));
//				}
//			}
			if (setSemantics) {
				 
			}
			return ans;
		}
		else {
			return op;
		}
	}
	
	public boolean isConnected(Graph g) {
		return true;
	}
	
	public List<List<Node>> pathsBetween(Node n, Node m) {
		Set<Node> visited = new HashSet<Node>();
		List<List<Node>> paths = new ArrayList<List<Node>>();
		List<Node> path = new ArrayList<Node>();
		if (n.equals(m)) {
			simpleCycle(n,m,null,visited,path,paths);
		}
		else {
			dfs(n,m,visited,path,paths);
		}
		return paths;
	}
	
	public void dfs(Node n, Node m, Set<Node> visited, List<Node> path, List<List<Node>> paths) {
		visited.add(n);
		if (n.equals(m)) {
			visited.remove(n);
			path.remove(n);
			List<Node> ans = new ArrayList<Node>();
			ans.addAll(path);
			paths.add(ans);
			return;
		}
		else {
			if (visited.size() > 1 && namedVars.contains(n)) {
				return;
			}
		}
		Set<Node> adjacentNodes = new HashSet<Node>();
		for (Pair<Node,Node> edge : edges) {
			if (edge.getLeft().equals(n)) {
				adjacentNodes.add(edge.getRight());
			}
			else if (edge.getRight().equals(n)) {
				adjacentNodes.add(edge.getLeft());
			}
		}
		for (Node v : adjacentNodes) {
			if (!visited.contains(v)) {
				path.add(v);
				dfs(v,m,visited,path,paths);
				path.remove(v);
			}
		}
		visited.remove(n);
		return;
	}
	
	public void trivialLoop(Node n) {
		
	}
	
	public void simpleCycle(Node n, Node m, Node previous, Set<Node> visited, List<Node> path, List<List<Node>> paths) {
		visited.add(n);
		if (n.equals(m)) {
			visited.remove(n);
			List<Node> ans = new ArrayList<Node>();
			ans.addAll(path);	
			if (ans.size() > 0) {
				ans.remove(n);
				List<Node> reverseAns = new ArrayList<Node>();
				for (int i = ans.size() - 1; i >= 0; i--) {
					reverseAns.add(ans.get(i));
				}
				if (!paths.contains(reverseAns)) { // Make sure we don't add the same path but reversed.
					paths.add(ans);
				}
				return;
			}	
		}
		else {
			if (namedVars.contains(n)) {
				return;
			}
		}
		Set<Node> adjacentNodes = new HashSet<Node>();
		for (Pair<Node,Node> edge : edges) {
			if (edge.getLeft().equals(n)) {
				adjacentNodes.add(edge.getRight());
			}
			else if (edge.getRight().equals(n)) {
				adjacentNodes.add(edge.getLeft());
			}
		}
		for (Node v : adjacentNodes) {
			if (!visited.contains(v)) {
				if (v.equals(previous)) {
					if (multiEdges.contains(new Pair<Node,Node>(n,previous)) || multiEdges.contains(new Pair<Node,Node>(previous,n))) {
						path.add(v);
						simpleCycle(v,m,n,visited,path,paths);
						return;
					}
					else {
						continue;
					}
				}
				path.add(v);
				simpleCycle(v,m,n,visited,path,paths);
				path.remove(v);
			}
		}
		visited.remove(n);
		return;
	}
	
	public void dfsForLoop(Node n, Node m, Node previous, Set<Node> visited, List<Node> path, List<List<Node>> paths) {
		visited.add(n);
		if (n.equals(m)) {
			visited.remove(n);
			List<Node> ans = new ArrayList<Node>();
			ans.addAll(path);	
			if (ans.size() > 0) {
				ans.remove(n);
				List<Node> reverseAns = new ArrayList<Node>();
				for (int i = ans.size() - 1; i >= 0; i--) {
					reverseAns.add(ans.get(i));
				}
				if (!paths.contains(reverseAns)) { // Make sure we don't add the same path but reversed.
					paths.add(ans);
				}
				return;
			}	
		}
		else {
			if (namedVars.contains(n)) {
				return;
			}
		}
		Set<Node> adjacentNodes = new HashSet<Node>();
		for (Pair<Node,Node> edge : edges) {
			if (edge.getLeft().equals(n)) {
				adjacentNodes.add(edge.getRight());
			}
			else if (edge.getRight().equals(n)) {
				adjacentNodes.add(edge.getLeft());
			}
		}
		for (Node v : adjacentNodes) {
			if (!visited.contains(v)) {
				if (v.equals(previous)) {
					continue;
				}
				path.add(v);
				dfsForLoop(v,m,n,visited,path,paths);
				path.remove(v);
			}
		}
		visited.remove(n);
		return;
	}
	
	public List<Triple> triplesInJoin(Op op) {
		List<Triple> triples = new ArrayList<Triple>();
		if (op instanceof OpJoin) {
			Op left = ((OpJoin) op).getLeft();
			Op right = ((OpJoin) op).getRight();
			if (left instanceof OpTriple) {
				triples.add(((OpTriple) left).getTriple());
			}
			else if (left instanceof OpPath) {
				TriplePath tp = ((OpPath) left).getTriplePath();
				if (tp.getPath() instanceof P_Link) {
					triples.add(Triple.create(tp.getSubject(), tp.getPredicate(), tp.getObject()));
				}
				else {
					Node predicate = NodeFactory.createLiteral(PathWriter.asString(tp.getPath()));
					triples.add(Triple.create(tp.getSubject(), predicate, tp.getObject()));
				}
				
			}
			else if (left instanceof OpJoin) {
				triples.addAll(triplesInJoin(left));
			}
			else {
				return null;
			}
			if (right instanceof OpTriple) {
				triples.add(((OpTriple) right).getTriple());
			}
			else if (right instanceof OpPath) {
				TriplePath tp = ((OpPath) right).getTriplePath();
				if (tp.getPath() instanceof P_Link) {
					triples.add(Triple.create(tp.getSubject(), tp.getPredicate(), tp.getObject()));
				}
				else {
					Node predicate = NodeFactory.createLiteral(PathWriter.asString(tp.getPath()));
					triples.add(Triple.create(tp.getSubject(), predicate, tp.getObject()));
				}
			}
			else if (right instanceof OpJoin) {
				triples.addAll(triplesInJoin(right));
			}
			else {
				return null;
			}
		}
		else {
			return triples;
		}
		return triples;
	}
	
	public List<Op> opsInJoin(Op op) {
		List<Op> ans = new ArrayList<Op>();
		if (op instanceof OpJoin) {
			Op left = ((OpJoin) op).getLeft();
			Op right = ((OpJoin) op).getRight();
			if (left instanceof OpSequence) {
				for (Op o : ((OpSequence) left).getElements()) {
					ans.add(o);
				}
			}
			else if (left instanceof OpJoin) {
				ans.addAll(opsInJoin(left));
			}
			else {
				ans.add(left);
			}
			if (right instanceof OpSequence) {
				for (Op o : ((OpSequence) right).getElements()) {
					ans.add(o);
				}
			}
			else if (right instanceof OpJoin) {
				ans.addAll(opsInJoin(right));
			}
			else {
				ans.add(right);
			}
		}
		else {
			return ans;
		}
		return ans;
	}
	
	public List<Op> opsInUnion(Op op){
		List<Op> ans = new ArrayList<Op>();
		if (op instanceof OpUnion) {
			Op leftOp = ((OpUnion) op).getLeft();
			Op rightOp = ((OpUnion) op).getRight();
			if (leftOp instanceof OpTriple) {
				ans.add(leftOp);
			}
			else if (leftOp instanceof OpBGP) {
				ans.add(leftOp);
			}
			else if (leftOp instanceof OpJoin) {
				ans.add(leftOp);
			}
			else if (leftOp instanceof OpPath) {
				ans.add(leftOp);
			}
			else if (leftOp instanceof OpUnion) {
				ans.addAll(opsInUnion(leftOp));
			}
			else if (leftOp instanceof OpSequence) {
				ans.add(leftOp);
			}
			else {
				return null;
			}
			if (rightOp instanceof OpTriple) {
				ans.add(rightOp);
			}
			else if (rightOp instanceof OpBGP) {
				ans.add(rightOp);
			}
			else if (rightOp instanceof OpJoin) {
				ans.add(rightOp);
			}
			else if (rightOp instanceof OpPath) {
				ans.add(rightOp);
			}
			else if (rightOp instanceof OpUnion) {
				ans.addAll(opsInUnion(rightOp));
			}
			else if (rightOp instanceof OpSequence) {
				ans.add(rightOp);
			}
			else {
				return null;
			}
		}
		else {
			return ans;
		}
		return ans;
	}
	
	public List<Op> allOpsInUnion(Op op){
		List<Op> ans = new ArrayList<Op>();
		if (op instanceof OpUnion) {
			Op leftOp = ((OpUnion) op).getLeft();
			Op rightOp = ((OpUnion) op).getRight();
			if (leftOp instanceof OpUnion) {
				ans.addAll(allOpsInUnion(leftOp));
			}
			else {
				ans.add(leftOp);
			}
			if (rightOp instanceof OpUnion) {
				ans.addAll(allOpsInUnion(rightOp));
			}
			else {
				ans.add(rightOp);
			}
		}
		else {
			return ans;
		}
		return ans;
	}
	
	public List<Op> findOpSequence(Op op) {
		return findOpSequence(op, new ArrayList<Op>());
	}
	
	public void clean() {
		this.namedVars = new HashSet<Node>();
		this.edges = new ArrayList<Pair<Node,Node>>();
		this.pathEdges = new ArrayList<Pair<Pair<Node,Node>,Path>>();
		this.varMap = HashBiMap.create();
	}
	
	public List<Op> findOpSequence(Op op, List<Op> ans) {
		if (op instanceof Op1) {
			findOpSequence(((Op1) op).getSubOp(), ans);
		}
		else if (op instanceof Op2) {
			findOpSequence(((Op2) op).getLeft(), ans);
			findOpSequence(((Op2) op).getRight(), ans);
		}
		else if (op instanceof OpN) {
			if (op instanceof OpSequence) {
				ans.add(op);
			}
			List<Op> opList = ((OpN) op).getElements();
			for (Op o : opList) {
				findOpSequence(o, ans);
			}
		}
		else if (op instanceof Op0) {
			return ans;
		}
		else {
			return ans;
		}
		return ans;
	}
	
	public TreeSet<org.semanticweb.yars.nx.Node[]> getTriples(Graph g){
		Model model = ModelFactory.createModelForGraph(g);
		JenaModelIterator jmi = new JenaModelIterator(model);
		TreeSet<org.semanticweb.yars.nx.Node[]> triples = new TreeSet<>(NodeComparator.NC);
		while(jmi.hasNext()){
			org.semanticweb.yars.nx.Node[] triple = jmi.next();
			triples.add(new org.semanticweb.yars.nx.Node[]{triple[0],triple[1],triple[2]});
		}
		return triples;
	}
	
	public GraphLeaningResult DFSLeaning(Collection<org.semanticweb.yars.nx.Node[]> triples) throws InterruptedException{
		DFSGraphLeaning dfsgl = new DFSGraphLeaning(triples, false, false);
		return dfsgl.call();
	}
	
	public Graph getLeanForm(Graph g) throws InterruptedException{
		for (Var v : projectedVars) {
			if (varMap.containsKey(v)) {
				g.add(Triple.create(varMap.get(v), NodeFactory.createURI("value"), NodeFactory.createLiteral(v.getName())));
			}
		}
		GraphLeaningResult glResult = this.DFSLeaning(getTriples(g));
		Graph graph = GraphFactory.createPlainGraph();
		for (org.semanticweb.yars.nx.Node[] node : glResult.getLeanData()){
			Node subject = null, predicate = null, object = null;
			if (Pattern.matches("_:.+", node[0].toN3())){
				String str = node[0].toN3().substring(2);
				str = str.replace("x78", "x");
				subject = NodeFactory.createBlankNode(str);
			}
			else if (Pattern.matches("<.+>", node[0].toN3())){
				subject = NodeFactory.createURI(node[0].toN3().replaceAll("<|>", ""));
			}
			else{
				subject = createLiteralWithType(node[0].toN3());
			}
			if (Pattern.matches("<.+>", node[1].toN3())){
				predicate = NodeFactory.createURI(node[1].toN3().replaceAll("<|>", ""));
			}
			else if (Pattern.matches("_:.+", node[1].toN3())){
				String str = node[1].toN3().substring(2);
				str = str.replace("x78", "x");
				predicate = NodeFactory.createBlankNode(str);
				if (varMap.inverse().containsKey(predicate)) {
					predicate = varMap.inverse().get(predicate);
				}
			}
			else{
				predicate = createLiteralWithType(node[1].toN3());
			}
			if (Pattern.matches("_:.+", node[2].toN3())){
				String str = node[2].toN3().substring(2);
				str = str.replace("x78", "x");
				object = NodeFactory.createBlankNode(str);
			}
			else if (Pattern.matches("<.+>", node[2].toN3())){
				object = NodeFactory.createURI(node[2].toN3().replaceAll("<|>", ""));
			}
			else{
				object = createLiteralWithType(node[2].toN3());
			}
			if (subject != null){
				graph.add(Triple.create(subject, predicate, object));
			}
			else{
				System.err.println("Invalid blank node label.");
			}
		}
		for (Var v : projectedVars) {
			if (varMap.containsKey(v)) {
				graph.delete(Triple.create(varMap.get(v), NodeFactory.createURI("value"), NodeFactory.createLiteral(v.getName())));
			}
		}
		return graph;
	}
	
	public Op containment(Op op) {
		Op ans = op;
		if (op instanceof OpSequence) {
			BiMap<Path,Node> nodePathMap = HashBiMap.create();
			Map<String,PGraph> pathGraphMap = new HashMap<String,PGraph>();
			BasicPattern bp = new BasicPattern();
			List<Op> listOp = ((OpSequence) op).getElements();
			List<Triple> triples = new ArrayList<Triple>();
			Graph g = GraphFactory.createPlainGraph();
			for (Path p : nodePathMap.keySet()) {
				PGraph pg = new PGraph(PathWriter.asString(p));
				pathGraphMap.put(PathWriter.asString(p), pg);
			}
			for (Op o : listOp) {
				if (o instanceof OpBGP) { // Add all triples in BGPs to graph
					BasicPattern bp1 = ((OpBGP) o).getPattern();
					triples.addAll(bp1.getList());
					bp.addAll(bp1);
				}
				else if (o instanceof OpTriple) {
					triples.add(((OpTriple) o).getTriple());
					bp.add(((OpTriple) o).getTriple());
				}
				else if (o instanceof OpPath) { // Paths
					TriplePath tp = ((OpPath) o).getTriplePath();
					Path p = tp.getPath();
					if (!nodePathMap.containsKey(p)) {
						nodePathMap.put(p, Var.alloc("p"+nodePathMap.size()));
					}
					triples.add(Triple.create(tp.getSubject(), NodeFactory.createLiteral(WriterPath.asString(p)), tp.getObject()));
					bp.add(Triple.create(tp.getSubject(), nodePathMap.get(p), tp.getObject()));
				}
				else if (o instanceof OpJoin) {
					List<Triple> triplesInJoin = triplesInJoin(o);
					if (triplesInJoin != null) {
						triples.addAll(triplesInJoin(o));
					}
				}
			}
			for (Triple t : triples) {
				Node s = t.getSubject();
				Node p = t.getPredicate();
				Node o = t.getObject();
				if (s.isVariable()) {
					s = NodeFactory.createBlankNode(s.getName());
				}
				if (p.isVariable()) {
					p = NodeFactory.createBlankNode(p.getName());
				}
				if (o.isVariable()) {
					o = NodeFactory.createBlankNode(o.getName());
				}
				g.add(Triple.create(s, p, o));
			}
			List<Var> vars = new ArrayList<Var>();
			for (Node var : nodePathMap.values()) {
				vars.add(Var.alloc(var));
			}
			Op opBGP = new OpBGP(bp);
			//opBGP = new OpProject(opBGP, vars);
			Query q = OpAsQuery.asQuery(opBGP);
			QueryExecution qe = QueryExecutionFactory.create(q, ModelFactory.createModelForGraph(g));
			ResultSet rs = qe.execSelect();
			List<Var> pVars = new ArrayList<Var>();
			for (String s : rs.getResultVars()) {
				pVars.add(Var.alloc(s));
			}
			pVars.removeAll(vars);
			QuerySolution minSolution = null;
			Map<Pair<Path,Path>,Boolean> containmentCache = new HashMap<Pair<Path,Path>,Boolean>();
			int minNodes = 0;
			while (rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
//				System.out.println(qs);
				Set<Node> nodes = new HashSet<Node>();
				Set<Node> paths = new HashSet<Node>();
				boolean isValid = true;
				if (minSolution == null) {
					minSolution = qs;
				}
				for (Var var : pVars) {
					nodes.add(qs.get(var.getName()).asNode());
				}
				for (Var var : vars) {
					paths.add(qs.get(var.getName()).asNode());
					Path p1 = nodePathMap.inverse().get(var);
					Node n = qs.get(var.getName()).asNode();
					if (n.isLiteral()) {
						Path p2 = SSE.parsePath((String) n.getLiteralValue());
						if (!containmentCache.containsKey(new Pair<Path,Path>(p1, p2))) {
							if (p1.equals(p2)) {
								containmentCache.put(new Pair<Path,Path>(p1, p2), true);
							}
							else {
								PGraph pg1 = new PGraph(p1);
								PGraph pg2 = new PGraph(p2);
								containmentCache.put(new Pair<Path,Path>(p1, p2), pg1.containedIn(pg2));
								containmentCache.put(new Pair<Path,Path>(p2, p1), pg2.containedIn(pg1));
							}
						}
						if (!containmentCache.get(new Pair<Path,Path>(p2, p1))) {
							isValid = false;
						}
					}
					else if (n.isURI()) {
						Path p2 = PathFactory.pathLink(n);
						if (!containmentCache.containsKey(new Pair<Path,Path>(p1, p2))) {
							if (p1.equals(p2)) {
								containmentCache.put(new Pair<Path,Path>(p1, p2), true);
							}
							else {
								PGraph pg1 = new PGraph(p1);
								PGraph pg2 = new PGraph(p2);
								containmentCache.put(new Pair<Path,Path>(p1, p2), false);
								containmentCache.put(new Pair<Path,Path>(p2, p1), pg2.containedIn(pg1));
							}
						}
						if (!containmentCache.get(new Pair<Path,Path>(p2, p1))) {
							isValid = false;
						}
					}
					else if (n.isBlank()) {
						isValid = false;
					}
					
				}
				if (minNodes == 0) {
					minNodes = nodes.size();
				}
				else {
					if (isValid) {
						if (nodes.size() < minNodes) {
							minNodes = nodes.size();
							minSolution = qs;
						}
					}
					
				}
			}
			List<Op> newOps = new ArrayList<Op>();
			for (Triple t : bp.getList()) {
				Op nextOp = null;
				Node s = t.getSubject();
				Node p = t.getPredicate();
				Node o = t.getObject();
				if (s.isVariable()) {
					if (minSolution.get(s.getName()).asNode().isBlank()) {
						s = Var.alloc(minSolution.get(s.getName()).asNode().getBlankNodeLabel());
					}
				}
				if (o.isVariable()) {
					if (minSolution.get(o.getName()).asNode().isBlank()) {
						o = Var.alloc(minSolution.get(o.getName()).asNode().getBlankNodeLabel());
					}
				}
				if (p.isVariable()) {
					if (vars.contains(p)) {
						p = minSolution.get(p.getName()).asNode();
						if (p.isLiteral()) {
							nextOp = new OpPath(new TriplePath(s, SSE.parsePath((String) p.getLiteralValue()), o));
						}
						else {
							nextOp = new OpTriple(new Triple(s, p, o));
						}
					}
					else {
						nextOp = new OpTriple(new Triple(s, p, o));
					}
				}
				else {
					nextOp = new OpTriple(new Triple(s, p, o));
				}
				if (!newOps.contains(nextOp)) {
					newOps.add(nextOp);
				}
			}
//			System.out.println(minSolution);
			ans = OpSequence.create();
			for (Op o : newOps) {
				((OpSequence) ans).add(o);
			}
		}
		else if (op instanceof OpUnion) {
			List<Op> ops = allOpsInUnion(op);
			List<Op> newOps = new ArrayList<Op>();
			for (Op o : ops) {
				newOps.add(containment(o));
			} 
			for (int i = 1; i < newOps.size(); i++) {
				
			}
		}
		else if (op instanceof OpJoin) {
			OpSequence opSeq = OpSequence.create();
			List<Op> ops = opsInJoin(op);
			for (Op o : ops) {
				opSeq.add(o);
			}
			return containment(opSeq);
		}
		else {
			return op;
		}
		return ans;
	}
	
	public Node getValidNode(Node n) {
		if (n.isVariable()) {
			return NodeFactory.createBlankNode(n.getName());
		}
		if (n.isLiteral()) {
			if (n.getLiteralLanguage().equals("")) {
				return NodeFactory.createLiteralByValue(n.getLiteralValue().toString(), n.getLiteralDatatype());
			}
			else {
				return NodeFactory.createLiteral(n.getLiteralValue().toString()+"@"+n.getLiteralLanguage());
			}
		}
		else {
			return n;
		}
	}
	
	public Node createLiteralWithType(String s){
		Node ans;
		s = s.replaceAll("\"", "");
		String[] split = s.split("@");
		if (split.length > 1) {
			String lang = split[split.length-1];
			if (s.contains("^^")) {
				lang = lang.substring(0,lang.indexOf("^^"));
			}
			return NodeFactory.createLiteral(s.substring(0,s.lastIndexOf("@")), lang);
		}
		if (s.contains("^^")){
				ans = NodeFactory.createLiteralByValue(s.substring(0, s.indexOf("^^")), NodeFactory.getType(s.substring(1+s.lastIndexOf("^")).replaceAll("<|>", "")));
		}
		else{
			ans = NodeFactory.createLiteralByValue(s, XSDDatatype.XSDstring);
		}
		return ans;
	}
	
	public Op getResult() {
		return this.op;
	}
}
