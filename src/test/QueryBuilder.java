package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Bound;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.E_GreaterThan;
import org.apache.jena.sparql.expr.E_GreaterThanOrEqual;
import org.apache.jena.sparql.expr.E_Lang;
import org.apache.jena.sparql.expr.E_LessThan;
import org.apache.jena.sparql.expr.E_LessThanOrEqual;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.E_LogicalNot;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_NotEquals;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.util.iterator.ExtendedIterator;

public class QueryBuilder {
	final String URI = "http://example.org/";
	private final Node typeNode = NodeFactory.createURI(this.URI+"type");
	private final Node tpNode = NodeFactory.createURI(this.URI+"TP");
	private final Node argNode = NodeFactory.createURI(this.URI+"arg");
	private final Node subNode = NodeFactory.createURI(this.URI+"subject");
	private final Node preNode = NodeFactory.createURI(this.URI+"predicate");
	private final Node objNode = NodeFactory.createURI(this.URI+"object");
	private final Node joinNode = NodeFactory.createURI(this.URI+"join");
	private final Node unionNode = NodeFactory.createURI(this.URI+"union");
	private final Node projectNode = NodeFactory.createURI(this.URI+"projection");
	private final Node opNode = NodeFactory.createURI(this.URI+"OP");
	private final Node limitNode = NodeFactory.createURI(this.URI+"limit");
	private final Node offsetNode = NodeFactory.createURI(this.URI+"offset");
	private final Node orderByNode = NodeFactory.createURI(this.URI+"orderBy");
	private final Node varNode = NodeFactory.createURI(this.URI+"var");
	private final Node orderNode = NodeFactory.createURI(this.URI+"order");
	private final Node valueNode = NodeFactory.createURI(this.URI+"value");
	private final Node dirNode = NodeFactory.createURI(this.URI+"direction");
	private final Node modNode = NodeFactory.createURI(this.URI+"modifier");
	private final Node functionNode = NodeFactory.createURI(this.URI+"function");
	private final Node andNode = NodeFactory.createURI(this.URI+"and");
	private final Node orNode = NodeFactory.createURI(this.URI+"or");
	private final Node notNode = NodeFactory.createURI(this.URI+"not");
	private final Node optionalNode = NodeFactory.createURI(this.URI+"optional");
	private final Node leftNode = NodeFactory.createURI(this.URI+"left");
	private final Node rightNode = NodeFactory.createURI(this.URI+"right");
	private final Node fromNode = NodeFactory.createURI(this.URI+"from");
	private final Node fromNamedNode = NodeFactory.createURI(this.URI+"fromNamed");
	private final Node graphNode = NodeFactory.createURI(this.URI+"graph");
	private final Node distinctNode = NodeFactory.createURI(this.URI+"distinct");
	private Graph graph;
	private Op op;
	
	public QueryBuilder(ExpandedGraph e){
		this.graph = e.graph;
	}
	
	public Expr filterOperatorToString(Node n, Expr... exprs){
		if (n.equals(NodeFactory.createURI(this.URI+"eq"))){
			return new E_Equals(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"neq"))){
			return new E_NotEquals(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"lt"))){
			return new E_LessThan(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"gt"))){
			return new E_GreaterThan(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"lteq"))){
			return new E_LessThanOrEqual(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"gteq"))){
			return new E_GreaterThanOrEqual(exprs[0], exprs[1]);
		}
		else{
			if (n.isLiteral()){
				String f = (String) n.getLiteralValue();
				if (f.equals("bound")){
					return new E_Bound(exprs[0]);
				}
				else if (f.equals("lang")){
					return new E_Lang(exprs[0]);
				}
			}
			//TODO
			else{
				return null;
			}
		}
		return null;
	}
	
	public String getCleanLiteral(Node n){
		if (n.isLiteral()){
			String o = n.getLiteralValue().toString();
			if (o.contains("^^")){
				return o.substring(0, o.indexOf("^")).replace("\"", "");
			}
			else{
				return o;
			}
		}
		else{
			return "";
		}
	}
	
	public boolean isOrderedFunction(Node function){
		if (function.equals(NodeFactory.createURI(this.URI+"lt"))){
			return true;
		}
		else if (function.equals(NodeFactory.createURI(this.URI+"gt"))){
			return true;
		}
		else if (function.equals(NodeFactory.createURI(this.URI+"lteq"))){
			return true;
		}
		else if (function.equals(NodeFactory.createURI(this.URI+"gteq"))){
			return true;
		}
		return false;
	}
	
	public boolean isOperator(Node s){
		if (isOrderedFunction(s)){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"eq"))){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"neq"))){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"times"))){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"plus"))){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"substract"))){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"divide"))){
			return true;
		}
		else{
			return false;
		}
	}
	
	public Op joinToOp(Node n){
		Op ans = null;
		ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
		List<Triple> tripleList = new ArrayList<Triple>();
		BasicPattern bp = new BasicPattern();
		while (args.hasNext()){
			Node arg = args.next();
			Node type = GraphUtil.listObjects(graph, arg, typeNode).next();
			if (type.equals(unionNode)){
				ans = OpJoin.create(ans, unionToOp(arg));
			}
			else if (type.equals(joinNode)){
				ans = OpJoin.create(ans, joinToOp(arg));
			}
			else if (type.equals(optionalNode)){
				ans = OpJoin.create(ans, optionalToOp(arg));
			}
			else if (type.equals(tpNode)){
				tripleList.add(nodeToTriple(arg));
			}
			else if (type.equals(graphNode)){
				ans = OpJoin.create(ans, graphToOp(arg));
			}
		}
		Collections.sort(tripleList, new TripleComparator());
		for (Triple t : tripleList){
			bp.add(t);
		}
		ans = OpJoin.create(ans, new OpBGP(bp));
		if (GraphUtil.listObjects(graph, n, modNode).hasNext()){
			Node f = GraphUtil.listObjects(graph, n, modNode).next();
			ans = OpFilter.filter(filterToOp(GraphUtil.listObjects(graph, f, argNode).next()), ans);
		}
		return ans;
	}
	
	public Op tripleToOp(Node n){
		BasicPattern bp = new BasicPattern();
		Node subjects = GraphUtil.listObjects(graph, n, subNode).next();
		if (subjects.isBlank()){
			subjects = NodeFactory.createVariable(subjects.getBlankNodeLabel());
		}
		Node predicates = GraphUtil.listObjects(graph, n, preNode).next();
		if (predicates.isBlank()){
			predicates = NodeFactory.createVariable(predicates.getBlankNodeLabel());
		}
		Node objects = GraphUtil.listObjects(graph, n, objNode).next();
		if (objects.isBlank()){
			objects = NodeFactory.createVariable(objects.getBlankNodeLabel());
		}
		bp.add(Triple.create(subjects, predicates, objects));
		return new OpBGP(bp);
	}
	
	public Triple nodeToTriple(Node n){
		Node subjects = GraphUtil.listObjects(graph, n, subNode).next();
		if (subjects.isBlank()){
			subjects = NodeFactory.createVariable(subjects.getBlankNodeLabel());
		}
		Node predicates = GraphUtil.listObjects(graph, n, preNode).next();
		if (predicates.isBlank()){
			predicates = NodeFactory.createVariable(predicates.getBlankNodeLabel());
		}
		Node objects = GraphUtil.listObjects(graph, n, objNode).next();
		if (objects.isBlank()){
			objects = NodeFactory.createVariable(objects.getBlankNodeLabel());
		}
		return Triple.create(subjects, predicates, objects);
	}
	
	public String tripleToOp(Triple n){
		String ans = "";
		Node subjects = n.getSubject();
		Node predicates = n.getPredicate();
		Node objects = n.getObject();
		String sub = subjects.toString();
		String pre = predicates.toString();
		String obj = objects.toString();
		if (obj.contains("^^")){
			obj = obj.substring(0, obj.indexOf("^^"))+"^^<"+obj.substring(obj.lastIndexOf("^")+1)+">";
		}
		if (subjects.isBlank()){
			sub = "?"+subjects.getBlankNodeLabel();
			sub = sub.replace("-", "").replace(":", "");
		}
		else if (subjects.isURI()){
			sub = "<"+sub+">";
		}
		if (predicates.isBlank()){
			pre = "?"+predicates.getBlankNodeLabel();
			pre = pre.replace("-", "").replace(":", "");
		}
		else if (predicates.isURI()){
			pre = "<"+pre+">";
		}
		if (objects.isBlank()){
			obj = "?"+objects.getBlankNodeLabel();
			obj = obj.replace("-", "").replace(":", "");
		}
		else if (objects.isURI()){
			obj = "<"+obj+">";
		}
		ans = sub +" "+ pre +" "+ obj + " . ";
		return ans;
	}
	
	public Op unionToOp(Node n){
		Op ans = null;
		ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
		Node firstArg = args.next();
		Node firstType = GraphUtil.listObjects(graph, firstArg, typeNode).next();
		if (firstType.equals(unionNode)){
			ans = unionToOp(firstArg);
		}
		else if (firstType.equals(joinNode)){
			ans = joinToOp(firstArg);
		}
		else if (firstType.equals(optionalNode)){
			ans = optionalToOp(firstArg);
		}
		else if (firstType.equals(tpNode)){
			ans = tripleToOp(firstArg);
		}
		else if (firstType.equals(graphNode)){
			ans = graphToOp(firstArg);
		}
		while (args.hasNext()){
			Node arg = args.next();
			Node type = GraphUtil.listObjects(graph, arg, typeNode).next();
			if (type.equals(unionNode)){
				ans = new OpUnion(ans, unionToOp(arg));
			}
			else if (type.equals(joinNode)){
				ans = new OpUnion(ans, joinToOp(arg));
			}
			else if (type.equals(optionalNode)){
				ans = new OpUnion(ans, optionalToOp(arg));
			}
			else if (type.equals(tpNode)){
				ans = new OpUnion(ans, tripleToOp(arg));
			}
			else if (type.equals(graphNode)){
				ans = new OpUnion(ans, graphToOp(arg));
			}
		}
		if (GraphUtil.listObjects(graph, n, modNode).hasNext()){
			Node f = GraphUtil.listObjects(graph, n, modNode).next();
			ans = OpFilter.filter(filterToOp(GraphUtil.listObjects(graph, f, argNode).next()), ans);
		}
		return ans;
	}
	
	public Op optionalToOp(Node n){
		Op leftOp = null;
		Op rightOp = null;
		Node left = GraphUtil.listObjects(graph, n, leftNode).next();
		if (graph.contains(Triple.create(left, typeNode, unionNode))){
			leftOp = unionToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, joinNode))){
			leftOp = joinToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, tpNode))){
			leftOp = tripleToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, optionalNode))){
			leftOp = optionalToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, graphNode))){
			leftOp = graphToOp(left);
		}
		Node right = GraphUtil.listObjects(graph, n, rightNode).next();
		if (graph.contains(Triple.create(right, typeNode, unionNode))){
			rightOp = unionToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, joinNode))){
			rightOp = joinToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, tpNode))){
			rightOp = tripleToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, optionalNode))){
			rightOp = optionalToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, graphNode))){
			rightOp = graphToOp(right);
		}
		if (GraphUtil.listObjects(graph, n, modNode).hasNext()){
			Node f = GraphUtil.listObjects(graph, n, modNode).next();
			leftOp = OpFilter.filter(filterToOp(GraphUtil.listObjects(graph, f, argNode).next()), leftOp);
		}
		return OpLeftJoin.createLeftJoin(leftOp, rightOp, null);
	}
	
	public Op graphToOp(Node n){
		Op ans = null;
		Node val = GraphUtil.listObjects(graph, n, valueNode).next();
		Node next = GraphUtil.listObjects(graph, n, argNode).next();
		if (graph.contains(Triple.create(next, typeNode, unionNode))){
			ans = new OpGraph(val, unionToOp(next));
		}
		else if (graph.contains(Triple.create(next, typeNode, joinNode))){
			ans = new OpGraph(val, joinToOp(next));
		}
		else if (graph.contains(Triple.create(next, typeNode, tpNode))){
			ans = new OpGraph(val, tripleToOp(next));
		}
		else if (graph.contains(Triple.create(next, typeNode, optionalNode))){
			ans = new OpGraph(val, optionalToOp(next));
		}
		else if (graph.contains(Triple.create(next, typeNode, graphNode))){
			ans = new OpGraph(val, graphToOp(next));
		}
		if (GraphUtil.listObjects(graph, n, modNode).hasNext()){
			Node f = GraphUtil.listObjects(graph, n, modNode).next();
			ans = OpFilter.filter(filterToOp(GraphUtil.listObjects(graph, f, argNode).next()), ans);
		}
		return ans;
	}
	
	public Expr filterToOp(Node n){
		Expr e = null;
		if (graph.contains(n, typeNode, andNode)){
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
			while (args.hasNext()){
				e = new E_LogicalAnd(e, filterToOp(args.next()));
			}
		}
		else if (graph.contains(n, typeNode, orNode)){
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
			while (args.hasNext()){
				e = new E_LogicalOr(e, filterToOp(args.next()));
			}
		}
		else if (graph.contains(n, typeNode, notNode)){
			Node args = GraphUtil.listObjects(graph, n, argNode).next();
			e = new E_LogicalNot(filterToOp(args));
		}
		else{
			if (GraphUtil.listObjects(graph, n, functionNode).hasNext()){
				Node function = GraphUtil.listObjects(graph, n, functionNode).next();
				ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
				List<Node> argList = args.toList();
				int nParams = argList.size();
				int i = 0;
				if (nParams == 1){
					return filterOperatorToString(function, filterToOp(argList.get(0)));
				}
				List<Expr> params = new ArrayList<Expr>();
				for (int k = 0; k < nParams; k++){
					params.add(null);
				}
				for (Node arg: argList){
					if (GraphUtil.listObjects(graph, arg, valueNode).hasNext()){
						Node value = GraphUtil.listObjects(graph, arg, valueNode).next();
						Expr argString = NodeValue.makeNode(Var.alloc(value.getBlankNodeLabel()));
						if (isOrderedFunction(function)){
							int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
							params.set(order, argString);
						}
						else{
							params.set(i, argString);
						}
					}
					if (GraphUtil.listObjects(graph, arg, functionNode).hasNext()){
						Node fun = GraphUtil.listObjects(graph, arg, functionNode).next();
						if (isOrderedFunction(fun)){
							int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
							params.set(order, filterToOp(arg));
						}
						else{
							params.set(i, filterToOp(arg));
						}
					}
					i++;
				}
				return filterOperatorToString(function, params.get(0), params.get(1));
			}
			if (GraphUtil.listObjects(graph, n, valueNode).hasNext()){
				Node v = GraphUtil.listObjects(graph, n, valueNode).next();
				return NodeValue.makeNode(Var.alloc(v.getBlankNodeLabel()));
			}
		}
		return e;
	}
	
	public String getQuery(){
		Node project = GraphUtil.listSubjects(this.graph, typeNode, projectNode).next();
		ExtendedIterator<Node> projectVars = GraphUtil.listObjects(this.graph, project, argNode);
		ArrayList<Var> pVariables = new ArrayList<Var>();
		String s = "SELECT ";
		if (projectVars.hasNext()){
			while (projectVars.hasNext()){
				pVariables.add(Var.alloc(projectVars.next().getBlankNodeLabel()));
			}
		}
		else{
			s += "* ";
		}
		s += "\n";
		ExtendedIterator<Node> f = GraphUtil.listSubjects(graph, typeNode, fromNode);
		ExtendedIterator<Node> fn = GraphUtil.listSubjects(graph, typeNode, fromNamedNode);
		
		s = s + "\nWHERE {\n";
		ExtendedIterator<Node> m = GraphUtil.listObjects(graph, project, opNode);
		Node first = m.next();
		if (graph.contains(Triple.create(first, typeNode, fromNode))){
			first = m.next();
		}
		if (graph.contains(Triple.create(first, typeNode, unionNode))){
			op = unionToOp(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, joinNode))){
			op = joinToOp(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, optionalNode))){
			op = optionalToOp(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, tpNode))){
			op = tripleToOp(first);
		}
		if (GraphUtil.listSubjects(graph, typeNode, orderByNode).hasNext()){
			Node orderBy = GraphUtil.listSubjects(graph, typeNode, orderByNode).next();
			List<Node> args = GraphUtil.listObjects(graph, orderBy, argNode).toList();
			List<SortCondition> params = new ArrayList<SortCondition>();
			for (int i = 0; i < args.size(); i++){
				params.add(null);
			}
			for (Node a : args){
				int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, a, orderNode).next()));
				Node varName = GraphUtil.listObjects(graph, a, varNode).next();
				int dir = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, a, dirNode).next()));
				SortCondition sc = new SortCondition(Var.alloc(varName.getBlankNodeLabel()), dir);
				params.set(order, sc);
			}
			op = new OpOrder(op, params);
		}
		if (GraphUtil.listSubjects(graph, typeNode, limitNode).hasNext()){
			Node limit = GraphUtil.listSubjects(graph, typeNode, limitNode).next();
			int start = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, limit, offsetNode).next()));
			int finish = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, limit, valueNode).next()));
			op = new OpSlice(op, start, finish);
		}
		if (!pVariables.isEmpty()){
			op = new OpProject(op, pVariables);
		}
		if (this.graph.contains(project, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean))){
			op = new OpDistinct(op);
		}
		Query q = OpAsQuery.asQuery(op);
		if (f.hasNext()){
			ExtendedIterator<Node> URIs = GraphUtil.listObjects(graph, f.next(), argNode);
			if (URIs.hasNext()){
				while (URIs.hasNext()){
					q.addGraphURI(URIs.next().getURI());
				}
			}
		}
		if (fn.hasNext()){
			ExtendedIterator<Node> URIs = GraphUtil.listObjects(graph, fn.next(), argNode);
			if (URIs.hasNext()){
				while (URIs.hasNext()){
					q.addNamedGraphURI(URIs.next().getURI());
				}
			}
		}
		Query query = OpAsQuery.asQuery(op);
		String ans = query.toString();
		ans = newLabels(ans);
		return ans;
	}
	
	public String newLabels(String q){
		String ans = q;
		HashSet<String> vars = new HashSet<String>();
		Pattern pattern = Pattern.compile("\\?SK\\w+");
		Matcher matcher = pattern.matcher(q);
		while(matcher.find()){
			vars.add(matcher.group(0));
		}
		int i = 0;
		for (String var : vars){
			ans = ans.replace(var, "?v"+i);
			i++;
		}
		return ans;
	}
	
	public class TripleComparator implements Comparator<Triple>{

		@Override
		public int compare(Triple o1, Triple o2) {
			if (compareSubject(o1,o2) == 0){
				if (comparePredicate(o1,o2) == 0){
					return compareObject(o1,o2);
				}
				else{
					return comparePredicate(o1,o2);
				}
			}
			else{
				return compareSubject(o1,o2);
			}
		}
		
		public int compareSubject(Triple o1, Triple o2){
			if (o1.getSubject().isBlank()){
				if (o2.getSubject().isBlank()){
					return 0;
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getSubject().isBlank()){
					return -1;
				}
				else{
					return o1.getSubject().toString().compareTo(o2.getSubject().toString());
				}
			}
		}
		public int comparePredicate(Triple o1, Triple o2){
			if (o1.getPredicate().isBlank()){
				if (o2.getPredicate().isBlank()){
					return 0;
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getPredicate().isBlank()){
					return -1;
				}
				else{
					return o1.getPredicate().toString().compareTo(o2.getPredicate().toString());
				}
			}
		}
		public int compareObject(Triple o1, Triple o2){
			if (o1.getObject().isBlank()){
				if (o2.getObject().isBlank()){
					return 0;
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getObject().isBlank()){
					return -1;
				}
				else{
					return o1.getObject().toString().compareTo(o2.getObject().toString());
				}
			}
		}
	}
}
