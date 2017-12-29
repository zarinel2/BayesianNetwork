import bn.core.*;
import bn.inference.*;
import bn.parser.BIFLexer;
import bn.parser.BIFParser;
import bn.parser.XMLBIFParser;
import bn.util.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class Sampler {
	private BayesianNetwork network;
	private HashMap<RandomVariable, String> evidence;
	private RandomVariable q;
	
	public Sampler(BayesianNetwork network, HashMap<RandomVariable, String> evidence, RandomVariable q){
		this.network = network;
		this.evidence = evidence;
		this.q = q;
	}
	/**
	 * Runs a rejection sample given the Sampler's Bayesian Network, evidence variables and query variable
	 * @param samples the number of samples to generate
	 * @return the distribution of the query variable in the assignments that are consistent with the evidence (will return NaN if the sample size is too small for the evidence)
	 */
	public Distribution rejectionSample(int samples){
		HashMap<String, Integer> queryDomainCounts = new HashMap<String, Integer>();
		for(Object o: q.getDomain()){
			queryDomainCounts.put(o.toString(), 0);
		}
		int total = 0;
		
		
		for(int i = 0; i < samples; i++){
			List<RandomVariable> variables = network.getVariableListTopologicallySorted();
			Assignment a = new Assignment();
			HashMap<String, String> a_2 = new HashMap<String, String>();
			//generate the assignment in topological order
			for(int j = 0; j < variables.size(); j++){
				RandomVariable curr = variables.get(j);
				List<Object> domain_vals= curr.getDomain();
				double target = Math.random(); // set a value between 0 and 1 to reach
				double cumulative_prob = 0.0;
				for(Object o: domain_vals){
					a.put(curr, o);
					a_2.put(curr.getName(), o.toString());
					cumulative_prob += network.getNodeForVariable(curr).cpt.get(a); //accumulate the probability of an outcome until it exceeds the random number
					if(cumulative_prob > target){
						break;
					}
				}
				
			}
			
			
			//evaluate the assignment
			boolean valid = true;
			//System.out.println(evidence);
			//System.out.println(a);
			for(RandomVariable r: evidence.keySet()){
				//System.out.println(evidence.get(r));
				//System.out.println(a_2.get(r.getName()));
				if(!evidence.get(r).toString().equals((a_2.get(r.getName())))){
					//System.out.println("bad assignment");
					valid = false;
				}
			}
			if(valid){
				total++;
				queryDomainCounts.put(a_2.get(q.getName()), queryDomainCounts.get(a_2.get(q.getName())) + 1);
			}
		}
		//System.out.println(queryDomainCounts);
		System.out.println(total + "/" + samples + " samples kept");
		
		Distribution out = new Distribution();
		for(Object o: queryDomainCounts.keySet()){
			out.put(o, queryDomainCounts.get(o)/(double)total);
		}
		System.out.println("Dist = " + out);
		return null;
	}
	
	/**
	 * Runs a likelihood-weighted sample for the Sampler's Bayesian Network, evidence and query
	 * @param samples the amount of assignments consistent with the evidence to generate
	 * @return The distribution of the query variable given the samples, weighted by likelihood.
	 */
	public Distribution weightedSample(int samples){
		HashMap<String, Double> queryDomainCounts = new HashMap<String, Double>();
		for(Object o: q.getDomain()){
			queryDomainCounts.put(o.toString(), 0.0);
		}
		
		HashMap<String, String> e_2 = new HashMap<String, String>();
		for(RandomVariable v: evidence.keySet()){
			e_2.put(v.getName(), evidence.get(v));
		}
		
		
		for(int i = 0; i < samples; i++){
			List<RandomVariable> variables = network.getVariableListTopologicallySorted();
			Assignment a = new Assignment();
			HashMap<String, String> a_2 = new HashMap<String, String>();
			double weight = 1.0;
			//generate the assignment
			for(int j = 0; j < variables.size(); j++){
				RandomVariable curr = variables.get(j);
				//check if evidence var
				if(e_2.containsKey(curr.getName())){
					//System.out.println(curr.getName() + " is evidence");
					
					a.put(curr, e_2.get(curr.getName()).toString());
					a_2.put(curr.getName(), e_2.get(curr.getName()));
					weight *= network.getNodeForVariable(curr).cpt.get(a); //reweight based on likelihood
				} else {
					
					List<Object> domain_vals= curr.getDomain();
					double target = Math.random();
					double cumulative_prob = 0.0;
					for(Object o: domain_vals){
						a.put(curr, o);
						a_2.put(curr.getName(), o.toString());
						cumulative_prob += network.getNodeForVariable(curr).cpt.get(a);
						if(cumulative_prob > target){
							break;
						}
					}
				}
			}
			//System.out.println(a);
			//System.out.println(a_2);
			//System.out.println(weight);
			queryDomainCounts.put(a_2.get(q.getName()), queryDomainCounts.get(a_2.get(q.getName())) + weight);
			
		}
		//System.out.println(queryDomainCounts);
		Distribution out = new Distribution();
		for(Object o: queryDomainCounts.keySet()){
			out.put(o, queryDomainCounts.get(o));
		}
		out.normalize();
		System.out.println("Dist = " + out);
		return null;
	}
	
	public void print(){
		System.out.println("Network:");
		network.print();
		System.out.println("\nEvidence:");
		System.out.println(evidence);
		System.out.println("\nQueries:");
		System.out.println(q);
	}
	
	public static void main(String[] args) {
		System.out.println("---PART 2---");

		// ---
		// XML Parser
		// ---
		XMLBIFParser xmlBif = new XMLBIFParser();
		BayesianNetwork bn = null;

		if (args[1].endsWith(".xml")) {
			try {
				bn = xmlBif.readNetworkFromFile(args[1]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (args[1].endsWith(".bif")) {
			try {
				InputStream input = new FileInputStream(args[1]);
				BIFLexer bl = new BIFLexer(input);
				BIFParser bp = new BIFParser(bl);
				bn = bp.parseNetwork();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// If filename does not match any of the given ones
			System.out.print("File extension not correct.");
			System.exit(5);
		}

		// Get the query Variable specified
		RandomVariable query = bn.getVariableByName(args[2]);

		int querySamples = Integer.parseInt(args[0]);

		// Hashmap linking the random variables given to the boolean values that
		// they are supposed to be
		HashMap<RandomVariable, String> observedValues = new HashMap<RandomVariable, String>();

		// Loop through the rest of the input and get the random variables and
		// set them accordingly
		for (int i = 3; i < args.length; i += 2) {
			// RandomVariable newRV = new RandomVariable(args[i]);
			observedValues.put(bn.getVariableByName(args[i]), args[i + 1]);
		}

		Sampler s = new Sampler(bn, observedValues, query);
		// s.print();
		System.out.println("Sampling:");
		System.out.println("Rejection Sampling:");
		
		long t_1 = System.currentTimeMillis();
		s.rejectionSample(querySamples);
		long d_t = System.currentTimeMillis() - t_1;
		
		System.out.println("Rejection Sampling run time: " + d_t + " ms");
		System.out.println("\nLikelihood Weighting Sampling:");
		
		t_1 = System.currentTimeMillis();
		s.weightedSample(querySamples);
		d_t = System.currentTimeMillis() - t_1;
		
		System.out.println("Likelihood Weighting Sampling run time: " + d_t + " ms");
	}
}
