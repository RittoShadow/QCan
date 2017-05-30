package test;

public class UnhandledSPARQLFeature extends Throwable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3051545533907643055L;
	
	public UnhandledSPARQLFeature(String s){
		System.err.println(s);
	}

}
