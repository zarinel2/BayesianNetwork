

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import bn.core.*;
import bn.parser.BIFLexer;
import bn.parser.BIFParser;
import bn.parser.XMLBIFParser;

public class Enumerator {
	//---
	// Ask the enumerator on a query variable, hashmap of givens, and a BayesianNetwork
	//---
	static Distribution enumeration_ask(RandomVariable queryVar, HashMap<RandomVariable, String> givens, BayesianNetwork bn) {
		//Empty distribution
		Distribution dist = new Distribution();
		
		//Put everything into a new assignment
		Assignment assign = new Assignment();
		Iterator<RandomVariable> iterRV = givens.keySet().iterator();
		while (iterRV.hasNext()) {
			RandomVariable rv = iterRV.next();
			assign.put(rv, givens.get(rv).toString());
		}
		
		//for each xi (domain element) of X
		Iterator<Object> iter = queryVar.getDomain().iterator();
		
		//While there is still domain elements to assign
		while (iter.hasNext()) {
			//String of value
			String toSet = iter.next().toString();
			
			//Assign that value
			assign.put(queryVar, toSet);
			
			//put the assignment of query variable in the givens
			givens.put(queryVar, toSet);
			
			//Put in the distrbution the object value of the query and the percentage chance of it's assignment.
			dist.put(toSet, enumerate_all(bn.getVariableListTopologicallySorted(), givens, bn, assign));
			
		}
		
		//Normalize
		dist.normalize();
		
		//Return distribution
		return dist;
	}
	
	//---
	// Recursive method for enumeration
	//---
	static double enumerate_all(List<RandomVariable> vars, HashMap<RandomVariable, String> givens, BayesianNetwork bn, Assignment assign) {
		//Terminal Case (if vars is empty return 1.0)
		if (vars.isEmpty()) {
			return 1.0;
		}
		
		//Create new list of vars (for recursion references)
		List<RandomVariable> newVars = new ArrayList<RandomVariable>();
		newVars.addAll(vars);
		
		//Get first random variable in topoligically sorted list
		RandomVariable first = newVars.remove(0);
		
		//If givens has a value for first RandomVariable
		if (givens.containsKey(first)) {
			//System.out.println("Givens does contain key: " + first.getName() + " with probability: " + bn.getProb(first, assign));
			//return the probability of the given assignment and the recursion of it with that given assignment.
			return bn.getProb(first, assign) * enumerate_all(newVars, givens, bn, assign);
		} else {
			//If the value is not in givens, calculate the sum over the hidden variable and return it
			//System.out.println("Givens doesn't contain key: " + first.getName() + ". Calculating sum over the hidden variable.\n");
			
			double sum = 0.0;
			
			//Iterate on domain of first Random Variable
			Iterator<Object> iter = first.getDomain().iterator();
			while (iter.hasNext()) {
				//Get the value (each element of the domain of current random variable) to put in the assignment
				String valueToPut = iter.next().toString();
				
				//Create new assignment that includes the newly assigned first variable (with this element of the domain) with value
				Assignment assign2 = assign.copy();
				assign2.put(first, valueToPut);
				
				//Create new givens HashMap with the first value in domain assigned
				@SuppressWarnings("unchecked") //suppress warning, since we know givens is of that type
				HashMap<RandomVariable, String> newGivens = (HashMap<RandomVariable, String>) givens.clone();
				newGivens.put(first, valueToPut);
								
				//Add to the sum (that returns) the probability of this assignment (of First) times the recursion of it with that probability assigned.
				sum += bn.getProb(first, assign2) * enumerate_all(newVars, newGivens, bn, assign2);
			}
			
			//System.out.println("The sum for, " + first.getName() + ", is: "+ sum);
			
			//Return the sum
			return sum;
		}
	}
	public static void main(String[] args) throws IOException{
		System.out.println("---PART 1---");
		
		//---
		// XML Parser
		//---
		XMLBIFParser xmlBif = new XMLBIFParser();
		BayesianNetwork bn = null;
		
		if (args[0].endsWith(".xml")) {
			try{
				bn = xmlBif.readNetworkFromFile(args[0]);
			}catch(Exception e){
				e.printStackTrace();
			}
		} else if (args[0].endsWith(".bif")) {
			InputStream input = new FileInputStream(args[0]);
			BIFLexer bl = new BIFLexer(input);
			BIFParser bp = new BIFParser(bl);
			bn = bp.parseNetwork();
		} else {
			//If filename does not match any of the given ones
			System.out.println("The file type does not match any supported.");
			System.exit(5);
		}
		
		//Get the query Variable specified from the bayesian network
		RandomVariable query_2 = bn.getVariableByName(args[1]);
		System.out.println("Query Variable: " + query_2.getName());
		
		//Hashmap linking the random variables given to the boolean values that they are supposed to be
		HashMap<RandomVariable, String> observedValues = new HashMap<RandomVariable, String>();
		
		//Loop through the rest of the input and get the random variables (the given ones) and set them accordingly (to given values)
		for (int i=2; i < args.length; i+=2) {
			observedValues.put(bn.getVariableByName(args[i]), args[i+1]);
		}
		
		//Print the Observed Values
		System.out.println("Givens (Observed Values):");
		Iterator<RandomVariable> iter = observedValues.keySet().iterator();
		while (iter.hasNext()) {
			RandomVariable currRV = iter.next();
			System.out.println(currRV.toString() + " : " + observedValues.get(currRV));
		}
		
		System.out.println(); //Print Blank Line for clean output
		System.out.println("Starting the Enumerator:");
		
		//Start the run time and print final distribution
		long t_1 = System.currentTimeMillis();
		System.out.println("Final Dist = " + enumeration_ask(query_2, observedValues, bn));
		long d_t = System.currentTimeMillis() - t_1;
		
		//Print the time
		System.out.println("Enumerator run time: " + d_t + " ms");
	
	}
}
