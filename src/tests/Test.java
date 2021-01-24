package tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Before;

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
			QueryParserTest qp = new QueryParserTest(new File("testFiles/test20.txt"));
			assertTrue(qp.equalQueries(0, 1));
			assertTrue(qp.equalQueries(1, 2));
			qp = new QueryParserTest(new File("testFiles/test21.txt"));
			assertTrue(qp.equalQueries(0,1));
			assertTrue(qp.equalQueries(1,2));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void namedGraphTest(){
		try{
			QueryParserTest qp = new QueryParserTest(new File("testFiles/test13.txt"));
			assertTrue(qp.equalQueries(0, 1));
			assertTrue(qp.equalQueries(2, 3));
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void filterTest(){
		try{
			QueryParserTest qp = new QueryParserTest(new File("testFiles/test15.txt"));
			assertTrue(qp.equalQueries(0, 1));
			assertFalse(qp.equalQueries(1, 2));
			QueryParserTest qp1 = new QueryParserTest(new File("testFiles/filterTest1"));
			assertTrue(qp1.equalQueries(0, 1));
			qp = new QueryParserTest(new File("testFiles/filterTest8.txt"));
			assertTrue(qp.equalQueries(0,1));
			qp = new QueryParserTest(new File("testFiles/filterTest7.txt")); // EXISTS and NOT EXISTS
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	@org.junit.Test
	public void filterPushTest() {
		try {
			QueryParserTest qp = new QueryParserTest(new File("testFiles/filterPushTest1.txt"));
			assertFalse(qp.equalQueries(1,2));
			assertFalse(qp.equalQueries(3,4));
			qp = new QueryParserTest(new File("testFiles/filterPushTest.txt"));
			assertTrue(qp.equalQueries(0,1));
			assertFalse(qp.equalQueries(2,3));
			assertFalse(qp.equalQueries(4,5));
			assertTrue(qp.equalQueries(6,7));
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
			assertTrue(qp.equalQueries(4, 5));
			assertTrue(qp.equalQueries(6, 7));
			qp = new QueryParserTest(new File("testFiles/test23.txt"));

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
			assertFalse(qp.equalQueries(3,4));
			assertFalse(qp.equalQueries(4,5));
			assertTrue(qp.equalQueries(5,6));
			assertFalse(qp.equalQueries(7,8));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void generalTest() {
		try {
			//QueryParserTest qp = new QueryParserTest(new File("testFiles/test17.txt"));
			//qp = new QueryParserTest(new File("testFiles/test24.txt"));
			QueryParserTest qp = new QueryParserTest(new File("testFiles/test18.txt"));
			qp = new QueryParserTest(new File("testFiles/test19.txt"));
			qp = new QueryParserTest(new File("testFiles/test24.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
