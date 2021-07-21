package cl.uchile.dcc.paths;

import cl.uchile.dcc.main.SingleQuery;
import cl.uchile.dcc.blabel.label.GraphColouring;
import cl.uchile.dcc.op.OpEpsilon;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.lib.RandomLib;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.algebra.optimize.TransformMergeBGPs;
import org.apache.jena.sparql.algebra.optimize.TransformPathFlattern;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.graph.NodeTransformLib;
import org.apache.jena.sparql.path.*;
import cl.uchile.dcc.tools.CommonNodes;
import cl.uchile.dcc.tools.Tools;
import cl.uchile.dcc.transformers.UCQTransformer;
import cl.uchile.dcc.visitors.OpRenamer;
import cl.uchile.dcc.visitors.TopDownVisitor;

import java.util.*;

public class RPQMinimiser extends TopDownVisitor {
    Set<Var> varsInScope = new HashSet<>();

    public Op visit(OpProject project) {
        varsInScope.addAll(project.getVars());
        return new OpProject(visit(project.getSubOp()), project.getVars());
    }

    public Op visit(OpOrder order) {
        for (SortCondition sortCondition : order.getConditions()) {
            varsInScope.addAll(sortCondition.getExpression().getVarsMentioned());
        }
        return new OpOrder(visit(order.getSubOp()), order.getConditions());
    }

    public Op visit(OpFilter filter) {
        for (Expr expr : filter.getExprs()) {
            varsInScope.addAll(expr.getVarsMentioned());
        }
        return OpFilter.filter(filter.getExprs(),visit(filter.getSubOp()));
    }

    public Op visit(OpExtend extend) {
        varsInScope.addAll(extend.getVarExprList().getVars());
        return OpExtend.create(visit(extend.getSubOp()), extend.getVarExprList());
    }

    public Op visit(OpGroup group) {
        for (ExprAggregator expr : group.getAggregators()) {
            Set<Var> vars = expr.getAggregator().getExprList().getVarsMentioned();
            varsInScope.addAll(vars);
        }
        varsInScope.addAll(group.getGroupVars().getVars());
        return new OpGroup(visit(group.getSubOp()),group.getGroupVars(), group.getAggregators());
    }

    public Op visit(OpGraph graph) {
        if (graph.getNode().isVariable()) {
            varsInScope.add(Var.alloc(graph.getNode()));
        }
        return new OpGraph(graph.getNode(),visit(graph.getSubOp()));
    }

    public Op visit(OpService service) {
        if (service.getServiceElement() != null) {
            if (service.getService().isVariable()) {
                varsInScope.add(Var.alloc(service.getService()));
            }
            if (service.getServiceElement().getServiceNode().isVariable()) {
                varsInScope.add(Var.alloc(service.getServiceElement().getServiceNode()));
            }
            return new OpService(service.getService(),visit(service.getSubOp()),service.getServiceElement(),service.getSilent());
        }
        else {
            if (service.getService().isVariable()) {
                varsInScope.add(Var.alloc(service.getService()));
            }
            return new OpService(service.getService(),visit(service.getSubOp()),service.getSilent());
        }
    }

    public Op visit(OpSequence op) {
        Op ans = transformBNodesToVariables(op);
        List<Op> ops = opsInSequence(ans);
        List<Op> newOps = new ArrayList<>();
        List<Op> paths = new ArrayList<>();
        int n = 0;
        for (Op e : ops) {
            if (e instanceof OpTriple) {
                BasicPattern bp = new BasicPattern();
                bp.add(((OpTriple) e).getTriple());
                newOps.add(new OpBGP(bp));
                n++;
            }
            else if (e instanceof OpBGP) {
                n += ((OpBGP) e).getPattern().size();
                newOps.add(e);
            }
            else if (e instanceof OpPath) {
                TriplePath tp = ((OpPath) e).getTriplePath();
                if (tp.getPath() instanceof P_ZeroOrMore1) { //Case p*
                    Path subPath = ((P_ZeroOrMore1) tp.getPath()).getSubPath();
                    if (subPath instanceof P_Inverse) { //Case ^(p*) == (^p)*
                        Path subSubPath = ((P_Inverse) subPath).getSubPath();
                        e = new OpPath(new TriplePath(tp.getObject(),new P_ZeroOrMore1(subSubPath),tp.getSubject()));
                    }
                }
                else if (tp.getPath() instanceof P_Inverse) { //Case (x,^p,y) == (y,p,x)
                    Path subPath = ((P_Inverse) tp.getPath()).getSubPath();
                    if (subPath instanceof P_Link) {
                        e = new OpPath(new TriplePath(tp.getObject(),subPath,tp.getSubject()));
                    }
                }
                else if (tp.getPath() instanceof P_ZeroOrOne) { //Case (x,p?,y)
                    Path subPath = ((P_ZeroOrOne) tp.getPath()).getSubPath();
                    if (subPath instanceof P_Link) {
                        e = new OpPath(new TriplePath(tp.getSubject(),subPath,tp.getObject()));
                    }
                }
                paths.add(e);
                newOps.add(e);
            }
            else {
                newOps.add(e);
            }
        }
        if (!paths.isEmpty()) {
            Graph graph = GraphFactory.createPlainGraph();
            List<Triple> groundTriples = new ArrayList<>();
            for (Var var : varsInScope) { //Grounding variables that appear elsewhere.
                Triple t = Triple.create(var, CommonNodes.valueNode, NodeFactory.createLiteral(var.getVarName()));
                graph.add(Triple.create(NodeFactory.createURI(var.getVarName()), CommonNodes.valueNode, NodeFactory.createLiteral(var.getVarName())));
                groundTriples.add(t);
            }
            OpSequence sequence = OpSequence.create();
            for (Op o : newOps) {
                if (o instanceof OpPath) {
                    TriplePath tp = ((OpPath) o).getTriplePath();
                    if (tp.getPath() instanceof P_ZeroOrMore1) {
                        Node subject = tp.getSubject();
                        Path subPath = ((P_ZeroOrMore1) tp.getPath()).getSubPath();
                        Node object = tp.getObject();
                        Op emptyTransition = epsilonBetween(subject, object);
                        if (subPath instanceof P_Link) {
                            emptyTransition = unionOfN(subject, object, subPath, n);
                            sequence.add(emptyTransition);
                        }

                    }
                } else {
                    sequence.add(o);
                }
            }
            for (Triple t : groundTriples) {
                //sequence.add(new OpTriple(t));
            }
            UCQTransformer ucqTransformer = new UCQTransformer();
            Op newOp = Transformer.transform(ucqTransformer,sequence);
            newOp = Transformer.transform(ucqTransformer,newOp);
            List<Op> opsInUnion = ucqTransformer.opsInUnion(newOp);
            int CQs = opsInUnion.size();
            newOp = modifiedUnion(opsInUnion);
            //newOp = new OpProject(newOp,new ArrayList<>(varsInScope));
            //newOp = new OpDistinct(newOp);
            newOp = transformBNodesToVariables(newOp);
            newOp = Transformer.transform(new TransformMergeBGPs(),newOp);
            try {
                SingleQuery sq = new SingleQuery(newOp);
                newOp = sq.getCanonOp();
                if (newOp instanceof OpDistinct) {
                    if (((OpDistinct) newOp).getSubOp() instanceof OpProject) {
                        newOp = ((OpProject) ((OpDistinct) newOp).getSubOp()).getSubOp();
                    }
                }
                if (ucqTransformer.opsInUnion(newOp).size() < CQs) {
                    Map<String,String> varMap = sq.getVarMap();
                    Map<Var,Var> reverseVarMap = new HashMap<>();
                    for (Var var : varsInScope) {
                        if (varMap.containsKey(var.getVarName())) {
                            reverseVarMap.put(Var.alloc(varMap.get(var.getVarName())),var);
                        }
                    }
                    newOp = NodeTransformLib.transform(new OpRenamer(reverseVarMap),newOp);
                    return newOp;
                }
                else {
                    return ans;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (GraphColouring.HashCollisionException e) {
                e.printStackTrace();
            }
            return newOp;
        }
        //return ans;
        return op;
    }

//    public Op visit(OpSequence op) {
//        Op ans = transformBNodesToVariables(op);
//        List<Op> ops = opsInSequence(ans);
//        List<Op> newOps = new ArrayList<>();
//        List<Op> paths = new ArrayList<>();
//        Set<Set<Triple>> setSet = computeCanonicalGraphs(ans);
//        Set<Triple> tripleSet = computeCanonicalGraph(ans);
//        for (Op e : ops) {
//            if (e instanceof OpTriple) {
//                BasicPattern bp = new BasicPattern();
//                bp.add(((OpTriple) e).getTriple());
//                newOps.add(new OpBGP(bp));
//            }
//            else if (e instanceof OpBGP) {
//                newOps.add(e);
//            }
//            else if (e instanceof OpPath) {
//                TriplePath tp = ((OpPath) e).getTriplePath();
//                if (tp.getPath() instanceof P_ZeroOrMore1) { //Case p*
//                    Path subPath = ((P_ZeroOrMore1) tp.getPath()).getSubPath();
//                    if (subPath instanceof P_Inverse) { //Case ^(p*) == (^p)*
//                        Path subSubPath = ((P_Inverse) subPath).getSubPath();
//                        e = new OpPath(new TriplePath(tp.getObject(),new P_ZeroOrMore1(subSubPath),tp.getSubject()));
//                    }
//                }
//                else if (tp.getPath() instanceof P_Inverse) { //Case (x,^p,y) == (y,p,x)
//                    Path subPath = ((P_Inverse) tp.getPath()).getSubPath();
//                    if (subPath instanceof P_Link) {
//                        e = new OpPath(new TriplePath(tp.getObject(),subPath,tp.getSubject()));
//                    }
//                }
//                else if (tp.getPath() instanceof P_ZeroOrOne) { //Case (x,p?,y)
//                    Path subPath = ((P_ZeroOrOne) tp.getPath()).getSubPath();
//                    if (subPath instanceof P_Link) {
//                        e = new OpPath(new TriplePath(tp.getSubject(),subPath,tp.getObject()));
//                    }
//                }
//                paths.add(e);
//                newOps.add(e);
//            }
//            else {
//                newOps.add(e);
//            }
//        }
//        List<Set<Binding>> minimalBindings = new ArrayList<>();
//        if (!paths.isEmpty()) {
//            Set<Binding> previousMinBinding = null;
//            if (!tripleSet.isEmpty()) {
//                Graph graph = GraphFactory.createPlainGraph();
//                List<Triple> groundTriples = new ArrayList<>();
//                if (!tripleSet.isEmpty()) {
//                    for (Triple t : tripleSet) {
//                        Node s = t.getSubject();
//                        Node p = t.getPredicate();
//                        Node o = t.getObject();
//                        if (s.isVariable()) {
//                            s = NodeFactory.createURI(s.getName());
//                        }
//                        if (p.isVariable()) {
//                            p = NodeFactory.createURI(p.getName());
//                        }
//                        if (o.isVariable()) {
//                            o = NodeFactory.createURI(o.getName());
//                        }
//                        graph.add(Triple.create(s,p,o));
//                    }
//                    for (Var var : varsInScope) { //Grounding variables that appear elsewhere.
//                        Triple t = Triple.create(var,CommonNodes.valueNode,NodeFactory.createLiteral(var.getVarName()));
//                        graph.add(Triple.create(NodeFactory.createURI(var.getVarName()),CommonNodes.valueNode,NodeFactory.createLiteral(var.getVarName())));
//                        groundTriples.add(t);
//                    }
//                }
//                OpSequence sequence = OpSequence.create();
//                for (Op o : newOps) {
//                    if (o instanceof OpPath) {
//                        TriplePath tp = ((OpPath) o).getTriplePath();
//                        if (tp.getPath() instanceof P_ZeroOrMore1) {
//                            Node subject = tp.getSubject();
//                            Path subPath = ((P_ZeroOrMore1) tp.getPath()).getSubPath();
//                            Node object = tp.getObject();
//                            Op emptyTransition = epsilonBetween(subject,object);
//                            if (subPath instanceof P_Link) {
//                                emptyTransition = unionOfN(subject,object,subPath,2);
//                                sequence.add(emptyTransition);
//                            }
//
//                        }
//                    }
//                    else {
//                        sequence.add(o);
//                    }
//                }
//                for (Triple t : groundTriples) {
//                    sequence.add(new OpTriple(t));
//                }
//                Query query = OpAsQuery.asQuery(sequence);
//                query.setDistinct(true);
//                Model model = ModelFactory.createModelForGraph(graph);
//                QueryExecution queryExecution = QueryExecutionFactory.create(query,model);
//                ResultSet resultSet = queryExecution.execSelect();
//                Binding minBinding = null;
//                int minNodes = 0;
//                Set<Binding> minList = new HashSet<>();
//                while (resultSet.hasNext()) {
//                    Binding binding = resultSet.nextBinding();
//                    Set<Node> nodes = new HashSet<>();
//                    if (minBinding == null) {
//                        minBinding = binding;
//                        minList.add(binding);
//                    }
//                    for (Var var : query.getProjectVars()) {
//                        nodes.add(binding.get(var));
//                    }
//                    if (minNodes == 0) {
//                        minNodes = nodes.size();
//                    }
//                    else {
//                        if (nodes.size() < minNodes) {
//                            minList = new HashSet<>();
//                            minNodes = nodes.size();
//                            minList.add(binding);
//                            minBinding = binding;
//                        }
//                        else if (nodes.size() == minNodes) {
//                            minList.add(binding);
//                        }
//                    }
//                }
//                if (!minList.isEmpty()) {
//                    if (previousMinBinding == null) {
//                        previousMinBinding = minList;
//                        minimalBindings.add(minList);
//                    }
//                    if (SetUtils.disjoint(previousMinBinding,minList)) {
//                        System.out.println("Cannot lean.");
//                        //return op;
//                    }
//                    else {
//                        previousMinBinding = minList;
//                        minimalBindings.add(minList);
//                    }
//                }
//            }
//            if (previousMinBinding == null) {
//                return op;
//            }
//            List<Binding> finalList = new ArrayList<>(previousMinBinding);
//            Binding minBinding = finalList.get(0);
//            for (Binding binding : finalList) {
//                boolean presentInAll = true;
//                for (Set<Binding> bindingSet : minimalBindings) {
//                    if (!bindingSet.contains(binding)) {
//                        presentInAll = false;
//                        break;
//                    }
//                }
//                if (presentInAll) {
//                    minBinding = binding;
//                }
//
//            }
//            Binding finalPreviousMinBinding = minBinding;
//            Op finalAns = OpSequence.create();
//            for (Op o : newOps) {
//                ((OpSequence)finalAns).add(o);
//            }
//            finalAns = NodeTransformLib.transform(new NodeTransform() {
//                @Override
//                public Node apply(Node node) {
//                    if (node.isVariable()) {
//                        Var var = Var.alloc(node);
//                        if (finalPreviousMinBinding.contains(var)) {
//                            Node uri = finalPreviousMinBinding.get(var);
//                            if (uri.isURI()) {
//                                return Var.alloc(finalPreviousMinBinding.get(var).getURI());
//                            }
//                        }
//                    }
//                    return node;
//                }
//            },finalAns);
//            ans = OpSequence.create();
//            List<Op> elts = new ArrayList<>();
//            for (Op o : ((OpSequence)finalAns).getElements()) {
//                if (!elts.contains(o)) {
//                    elts.add(o);
//                }
//                if (o instanceof OpPath) {
//                    TriplePath triplePath = ((OpPath) o).getTriplePath();
//                    if (triplePath.getSubject().equals(triplePath.getObject()) && triplePath.getPath() instanceof P_ZeroOrMore1) {
//                        if (elts.contains(o)) {
//                            elts.remove(o);
//                        }
//                    }
//                }
//            }
//            for (Op o : elts) {
//                ((OpSequence)ans).add(o);
//            }
//        }
//        return ans;
//    }

    public Set<Set<TriplePath>> powerset(Set<TriplePath> triples) {
        return powerset(new HashSet<>(),triples);
    }

    public Set<Set<Triple>> triplePowerset(Set<Triple> triples) {
        return triplePowerset(new HashSet<>(),triples);
    }

    public Set<Set<TriplePath>> powerset(Set<TriplePath> current,Set<TriplePath> triples) {
        Set<Set<TriplePath>> ans = new HashSet<>();
        if (current.size() == triples.size()) {
            return ans;
        }
        else {
            for (TriplePath triple : triples) {
                if (!current.contains(triple)) {
                    Set<TriplePath> newSet = new HashSet<>(current);
                    newSet.add(triple);
                    ans.add(newSet);
                    ans.addAll(powerset(newSet,triples));
                }
            }
        }
        return ans;
    }

    public Set<Set<Triple>> triplePowerset(Set<Triple> current,Set<Triple> triples) {
        Set<Set<Triple>> ans = new HashSet<>();
        if (current.size() == triples.size()) {
            ans.add(new HashSet<>());
            return ans;
        }
        else {
            for (Triple triple : triples) {
                if (!current.contains(triple)) {
                    Set<Triple> newSet = new HashSet<>(current);
                    newSet.add(triple);
                    ans.add(newSet);
                    ans.addAll(triplePowerset(newSet,triples));
                }
            }
        }
        return ans;
    }

    public Op transformBNodesToVariables(Op op) {
        Op ans = op;
        Set<Var> vars = Tools.allVarsContainedIn(op);
        Set<Var> blankVars = new HashSet<>();
        Map<Var,Var> varVarMap = new HashMap<>();
        for (Var var : vars) {
            if (var.isBlankNodeVar()) {
                blankVars.add(var);
            }
        }
        for (Var var : blankVars) {
            Var newVar = Var.alloc(var.getVarName().replace("?",""));
            if (!vars.contains(newVar)) {
                vars.add(newVar);
                varVarMap.put(var,newVar);
            }
            else {
                String newVarName = newVar.getVarName();
                newVarName = newVarName.substring(1);
                if (StringUtils.isNumeric(newVarName)) {
                    int id = Integer.parseInt(newVarName);
                    do {
                        newVar = Var.alloc("P" + id++);
                    }
                    while (vars.contains(newVar));
                    vars.add(newVar);
                    varVarMap.put(var,newVar);
                }
            }
        }
        ans = NodeTransformLib.transform(new OpRenamer(varVarMap),op);
        return ans;
    }

    public boolean isSequence(Path path) {
        if (path instanceof P_Seq) {
            return isSequence(((P_Seq) path).getLeft()) && isSequence(((P_Seq) path).getRight());
        }
        else if (path instanceof P_Link) {
            return true;
        }
        else if (path instanceof P_Inverse) {
            return isSequence(((P_Inverse) path).getSubPath());
        }
        else {
            return false;
        }
    }

    public List<Path> pathsInSequence(Path path) {
        if (isSequence(path)) {
            List<Path> ans = new ArrayList<>();
            if (path instanceof P_Seq) {
                ans.addAll(pathsInSequence(((P_Seq) path).getLeft()));
                ans.addAll(pathsInSequence(((P_Seq) path).getRight()));
            }
            else if (path instanceof P_Link) {
                ans.add(path);
            }
            else if (path instanceof P_Inverse) {
                ans.add(path);
            }
            else {

            }
            return ans;
        }
        else {
            return null;
        }
    }

    public List<Op> opsInSequence(Op sequence) {
        List<Op> ans = new ArrayList<>();
        if (sequence instanceof OpSequence) {
            List<Op> elts = ((OpSequence) sequence).getElements();
            for (Op op : elts) {
                ans.addAll(opsInSequence(op));
            }
        }
        else {
            ans.add(sequence);
        }
        return ans;
    }

    public Op epsilonBetween(Node n1, Node n2) {
        Op ans = null;
        Expr expr = new E_Equals(new ExprVar(n1),new ExprVar(n2));
        BasicPattern bp = new BasicPattern();
        int random = RandomLib.qrandom.nextInt();
        bp.add(Triple.create(n1,Var.alloc("p"),Var.alloc("o")));
        bp.add(Triple.create(n2,Var.alloc("p"),Var.alloc("o")));
        ans = new OpBGP(bp);
        bp = new BasicPattern();
        bp.add(Triple.create(Var.alloc("s"),Var.alloc("p1"),n1));
        bp.add(Triple.create(Var.alloc("s"),Var.alloc("p1"),n2));
        ans = OpUnion.create(ans,new OpBGP(bp));
        ans = OpFilter.filter(expr,ans);
        return ans;
    }

    public Op unionOfN(Node n1, Node n2, Path predicate, int n) {
        Op ans = new OpEpsilon(n1,((P_Link) predicate).getNode(),n2);
        for (int i = 0; i < n; i++){
            Op path = new OpPath(new TriplePath(n1,new P_FixedLength(predicate,i+1),n2));
            path = Transformer.transform(new TransformPathFlattern(),path);
            if (ans == null) {
                ans = path;
            }
            else {
                ans = OpUnion.create(ans,path);
            }
        }
        return ans;
    }

    public Op modifiedUnion(List<Op> opsInUnion) {
        Op ans = null;
        for (Op op : opsInUnion) {
            List<OpEpsilon> epsilonOps = new ArrayList<>();
            List<Op> otherOps = new ArrayList<>();
            List<Op> opsInJoin = opsInJoin(op);
            BasicPattern bp = new BasicPattern();
            for (Op op1 : opsInJoin) {
                if (op1 instanceof OpEpsilon) {
                    epsilonOps.add((OpEpsilon) op1);
                }
                else {
                    otherOps.add(op1);
                }
            }
            Set<Set<OpEpsilon>> epsilonSet = Tools.powerSet(new HashSet<>(epsilonOps));
            for (Op op1 : otherOps) {
                if (op1 instanceof OpTriple) {
                    Triple triple = ((OpTriple) op1).getTriple();
                    bp.add(triple);
                }
            }
            for (Set<OpEpsilon> opEpsilon : epsilonSet) {
                BasicPattern bp1 = new BasicPattern(bp);
                for (OpEpsilon epsilon : opEpsilon) {
                    bp1.add(epsilon.asTriple());
                }
                if (ans == null) {
                    ans = new OpBGP(bp1);
                }
                else {
                    ans = OpUnion.create(ans,new OpBGP(bp1));
                }
            }
        }
        return ans;
    }

    public List<Op> opsInJoin(Op join) {
        List<Op> ans = new ArrayList<>();
        if (join instanceof OpJoin) {
            ans.addAll(opsInJoin(((OpJoin) join).getLeft()));
            ans.addAll(opsInJoin(((OpJoin) join).getRight()));
        }
        else if (join instanceof OpBGP && !(join instanceof OpEpsilon)) {
            List<Triple> tripleList = ((OpBGP) join).getPattern().getList();
            for (Triple triple : tripleList) {
                ans.add(new OpTriple(triple));
            }
        }
        else {
            ans.add(join);
        }
        return ans;
    }

    public Set<Op> computeCanonicalQuery(Op op) {
        Set<Op> ans = new HashSet<>();
        if (op instanceof OpSequence) {
            List<Op> ops = ((OpSequence) op).getElements();
            for (Op o : ops) {
                if (o instanceof OpPath) {

                }
            }
        }
        return ans;
    }


    public Set<Triple> computeCanonicalGraph(Op op) {
        Set<Triple> ans = new HashSet<>();
        if (op instanceof OpSequence) {
            List<Op> ops = ((OpSequence) op).getElements();
            for (Op o : ops) {
                ans.addAll(computeCanonicalGraph(o));
            }
        }
        else if (op instanceof OpPath) {
            TriplePath triplePath = ((OpPath) op).getTriplePath();
            Path path = triplePath.getPath();
            if (path instanceof P_Seq) {
                Op newOp = Transformer.transform(new TransformPathFlattern(),op);
                newOp = transformBNodesToVariables(newOp);
                Set<Triple> results = computeCanonicalGraph(newOp);
                ans.addAll(results);
            }
            else if (path instanceof P_ZeroOrMore1) {
                Set<Triple> subSets = computeCanonicalGraph(new OpPath(new TriplePath(triplePath.getSubject(),((P_ZeroOrMore1) path).getSubPath(),triplePath.getObject())));
                ans.addAll(subSets);
            }
            else if (path instanceof P_Inverse) {
                Set<Triple> subSets = computeCanonicalGraph(new OpPath(new TriplePath(triplePath.getObject(),((P_Inverse) path).getSubPath(),triplePath.getSubject())));
                ans.addAll(subSets);
            }
            else if (path instanceof P_Link) {
                ans.add(Triple.create(triplePath.getSubject(),((P_Link) path).getNode(),triplePath.getObject()));
            }
        }
        else if (op instanceof OpBGP) {
            BasicPattern bp = ((OpBGP) op).getPattern();
            ans.addAll(bp.getList());
        }
        else if (op instanceof OpTriple) {
            ans.add(((OpTriple) op).getTriple());
        }
        return ans;
    }

    public Set<Set<Triple>> computeCanonicalGraphs(Op op) {
        Set<Set<Triple>> ans = new HashSet<>();
        if (op instanceof OpSequence) {
            List<Op> ops = ((OpSequence) op).getElements();
            List<Set<Set<Triple>>> results = new ArrayList<>();
            for (Op o : ops) {
                results.add(computeCanonicalGraphs(o));
            }
            Set<List<Set<Triple>>> cartesianProduct = Sets.cartesianProduct(results);
            for (List<Set<Triple>> product : cartesianProduct) {
                Set<Triple> newSet = new HashSet<>();
                for (Set<Triple> tripleSet : product) {
                    newSet.addAll(tripleSet);
                }
                ans.add(newSet);
            }
        }
        else if (op instanceof OpPath) {
            TriplePath triplePath = ((OpPath) op).getTriplePath();
            Path path = triplePath.getPath();
            if (path instanceof P_Seq) {
                Op newOp = Transformer.transform(new TransformPathFlattern(),op);
                newOp = transformBNodesToVariables(newOp);
                Set<Set<Triple>> results = computeCanonicalGraphs(newOp);
                for (Set<Triple> set1 : results) {
                    for (Set<Triple> set2 : results) {
                        Set<Triple> newSet = new HashSet<>();
                        newSet.addAll(set1);
                        newSet.addAll(set2);
                        ans.add(newSet);
                    }
                }
            }
            else if (path instanceof P_ZeroOrMore1) {
                Set<Set<Triple>> subSets = computeCanonicalGraphs(new OpPath(new TriplePath(triplePath.getSubject(),((P_ZeroOrMore1) path).getSubPath(),triplePath.getObject())));
                for (Set<Triple> subSet : subSets) {
                    Set<Set<Triple>> results = triplePowerset(subSet);
                    ans.addAll(results);
                }
            }
            else if (path instanceof P_Inverse) {
                Set<Set<Triple>> subSets = computeCanonicalGraphs(new OpPath(new TriplePath(triplePath.getObject(),((P_Inverse) path).getSubPath(),triplePath.getSubject())));
                ans.addAll(subSets);
            }
            else if (path instanceof P_Link) {
                ans.add(Collections.singleton(Triple.create(triplePath.getSubject(),((P_Link) path).getNode(),triplePath.getObject())));
            }
        }
        else if (op instanceof OpBGP) {
            Set<Triple> set = new HashSet<>();
            BasicPattern bp = ((OpBGP) op).getPattern();
            for (Triple t : bp.getList()) {
                set.add(t);
            }
            ans.add(set);
        }
        else if (op instanceof OpTriple) {
            ans.add(Collections.singleton(((OpTriple) op).getTriple()));
        }
        return ans;
    }

    public static void main(String[] args) {
        Query query = QueryFactory.create("SELECT DISTINCT ?a ?e WHERE { ?a <p> ?b . ?b <p>* ?c . ?c <p> ?d . ?d <p>* ?e . }");
        Op op = Algebra.compile(query);
        op = Transformer.transform(new TransformPathFlattern(), op);
        System.out.println(new RPQMinimiser().visit(op));
    }
}
