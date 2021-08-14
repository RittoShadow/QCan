package tests;

import org.junit.Before;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Test {

	@Before
	public void setUp() throws Exception {
	}
	
	@org.junit.Test
	public void labelTest(){
		try {
			QueryParserTest qp = new QueryParserTest(new File("testFiles/labelTest.txt"));
			assertTrue(qp.equalQueries(0,1));
			assertTrue(qp.equalQueries(2,3));
			assertTrue(qp.equalQueries(3,4));
			assertTrue(qp.equalQueries(5,6));
			assertTrue(qp.equalQueries(7,8));
			assertTrue(qp.equalQueries(9,10));
			assertTrue(qp.equalQueries(10,11));
			assertTrue(qp.equalQueries(12,13));
			assertTrue(qp.equalQueries(14,15));
			assertTrue(qp.equalQueries(15,16));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void unionTest(){
		try {
			QueryParserTest qp = new QueryParserTest(new File("testFiles/unionTest.txt"));
			assertTrue(qp.equalQueries(0, 1));
			assertTrue(qp.equalQueries(1, 2));
			assertTrue(qp.equalQueries(3, 4));
			assertTrue(qp.equalQueries(4, 5));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void branchRelabelTest(){
		try{
			QueryParserTest qp = new QueryParserTest(new File("testFiles/branchRelabelTest.txt"));
			assertTrue(qp.equalQueries(0,1));
			assertTrue(qp.equalQueries(1,2));
			assertTrue(qp.equalQueries(3,4));
			assertTrue(qp.equalQueries(5,6));
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void UCQMinimisationTest() {
		try {
			QueryParserTest qp = new QueryParserTest(new File("testFiles/ucqMinimisationTest.txt"));
			assertTrue(qp.equalQueries(0, 1));
			assertTrue(qp.equalQueries(1, 2));
			assertTrue(qp.equalQueries(3,4));
			assertTrue(qp.equalQueries(4,5));
			assertTrue(qp.equalQueries(6,7));
			assertTrue(qp.equalQueries(7,8));
			assertTrue(qp.equalQueries(9,10));
			assertTrue(qp.equalQueries(10,11));
			assertTrue(qp.equalQueries(12,13));
			assertTrue(qp.equalQueries(13,14));
			assertTrue(qp.equalQueries(15,16));
			assertTrue(qp.equalQueries(17,18));
			assertFalse(qp.equalQueries(16,19));
			assertTrue(qp.equalQueries(19,20));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void namedGraphTest(){
		try{
			QueryParserTest qp = new QueryParserTest(new File("testFiles/namedGraph.txt"));
			assertTrue(qp.equalQueries(0, 1));
			assertTrue(qp.equalQueries(2, 3));
			assertFalse(qp.equalQueries(3,4));

		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void filterTest(){
		try{
			QueryParserTest qp1 = new QueryParserTest(new File("testFiles/filterTest.txt"));
			assertTrue(qp1.equalQueries(0, 1));
			assertTrue(qp1.equalQueries(1,2));
			assertFalse(qp1.equalQueries(2,3));
			assertTrue(qp1.equalQueries(3,4));
			assertTrue(qp1.equalQueries(9, 10));
			assertFalse(qp1.equalQueries(10, 11));
			assertFalse(qp1.equalQueries(12, 13));
			assertTrue(qp1.equalQueries(14,15));
			assertFalse(qp1.equalQueries(16, 17));
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	@org.junit.Test
	public void filterPushTest() {
		try {
			QueryParserTest qp = new QueryParserTest(new File("testFiles/filterPushTest.txt"));
			assertTrue(qp.equalQueries(0,1));
			assertFalse(qp.equalQueries(2,3));
			assertFalse(qp.equalQueries(4,5));
			assertTrue(qp.equalQueries(6,7));
			assertFalse(qp.equalQueries(9,10));
			assertFalse(qp.equalQueries(11,12));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void optionalTest(){
		try {
			QueryParserTest qp = new QueryParserTest(new File("testFiles/optionalTest.txt"));
			assertTrue(qp.equalQueries(0, 1));
			//assertTrue(qp.equalQueries(2, 3));
			assertTrue(qp.equalQueries(2, 3));
			assertFalse(qp.equalQueries(4, 5));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void propertyPathTest() {
		QueryParserTest qp;
		try {
			qp = new QueryParserTest(new File("testFiles/propertyPathsTest1.txt"));
			assertTrue(qp.equalQueries(0, 1));
			assertTrue(qp.equalQueries(1, 2));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@org.junit.Test
	public void aggregationTest() {
		try {
			QueryParserTest qp = new QueryParserTest(new File("testFiles/aggregationTest.txt"));
			assertFalse(qp.equalQueries(2,3));
			assertFalse(qp.equalQueries(4,5));
			assertTrue(qp.equalQueries(5,6));
			assertFalse(qp.equalQueries(7,8));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@org.junit.Test
	public void minusTest() {
		try {
			QueryParserTest qp = new QueryParserTest(new File("testFiles/minusDistTest.txt"));
			assertTrue(qp.equalQueries(0,1));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
