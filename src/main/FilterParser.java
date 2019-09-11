package main;

import java.util.regex.Pattern;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
public class FilterParser {
	
	private RGraph graph;
	private int id;
	final String URI = "http://example.org/";
	
	public FilterParser(RGraph e, int filterId){
		this.graph = e;
		this.id = filterId;
	}
	
	public void parse(String s){
		this.graph.filter(parseRestOfString(s,0));
	}
	
	public Node parseRestOfString(String s, int openPar){
		s = s.trim();
		if (s.startsWith("(&&")){
			openPar++;
			s = s.substring(1);
			String left = findFirstArg(s, openPar);
			String right = findSecondArg(s, openPar);
			return graph.filterAnd(parseRestOfString(left, openPar+1), parseRestOfString(right, openPar+1));
		}
		else if (s.startsWith("(||")){
			openPar++;
			s = s.substring(1);
			String left = findFirstArg(s, openPar);
			String right = findSecondArg(s, openPar);
			return graph.filterOr(parseRestOfString(left, openPar+1), parseRestOfString(right, openPar+1));
		}
		else if (s.startsWith("(! ")){
			openPar++;
			s = s.substring(1);
			String arg = findFirstArg(s, openPar);
			return graph.filterNot(parseRestOfString(arg, openPar+1));
		}
		else if (s.startsWith("(")){
			openPar++;
			String op = s.substring(1, s.indexOf(" "));
			s = s.substring(s.indexOf(" "), s.lastIndexOf(")")).trim();
			String arg1, arg2;
			if (!s.contains(" ") || s.startsWith("\"")){
				return graph.filterFunction(op, parseRestOfString(s, openPar+1));
			}
			if (s.startsWith("(")){
				arg1 = findFirstArg(s,openPar);
				arg2 = s.substring(indexOfNextParenthesis(s,openPar)+1).trim();
			}
			else{
				arg1 = s.substring(0,s.indexOf(" "));
				arg2 = s.substring(1+s.indexOf(" ")).trim();
			}
			if (arg2.equals("")){
				return graph.filterFunction(op, parseRestOfString(arg1, openPar+1));
			}
			else{
				return graph.filterFunction(op, parseRestOfString(arg1,openPar+1), parseRestOfString(arg2,openPar+1));
			}	
		}
		else if (s.startsWith("?")){
			return NodeFactory.createBlankNode(s.substring(1));
		}
		else if (Pattern.matches("<.+://.+>", s)){
			return NodeFactory.createURI(s.replaceAll("<|>", ""));
		}
		else{
			return graph.createLiteralWithType(s);
		}
	}
	
	public int indexOfNextParenthesis(String s, int openPar){
		int balance = openPar;
		for (int i = 0; i < s.length(); i++){
			if (s.charAt(i) == ')'){
				balance--;
				if (balance == openPar){
					return i;
				}
			}
			else if (s.charAt(i) == '('){
				balance++;
			}
		}
		return s.length() - 1;
	}
	
	public String findFirstArg(String s, int openPar){
		return s.substring(s.indexOf("("), 1+indexOfNextParenthesis(s, openPar));
	}
	
	public String findSecondArg(String s, int openPar){
		int i = indexOfNextParenthesis(s, openPar);
		String r = s.substring(i+1);
		return r.substring(r.indexOf("("), 1+indexOfNextParenthesis(r, openPar));
	}
	
	public Node conjunctionNode(Node arg1, Node arg2, int id){
		Node o = NodeFactory.createBlankNode("and"+id);
		return o;
	}
	
	public Node disjunctionNode(Node arg1, Node arg2, int id){
		Node o = NodeFactory.createBlankNode("or"+id);
		return o;
	}
}
