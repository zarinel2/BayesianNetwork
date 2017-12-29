Quick Description:
This allows you to create BayesianNetworks from files and run a Rejection Sampler (with however many samples you like) and an Enumerator on them.

(a) Contributors:

Name: William Tyler Wilson, Samuel Triest, Alex McKinley
Contact: wwils11@u.rochester.edu, striest@u.rochester.edu, amckinl2@u.rochester.edu

(b) How to Build the Project:

Commands to run (after unzipping and changing directory to inside zip folder where README.txt): 

1.	cd BayesianNetwork
2.	cd src
3.	javac *.java				      (this compiles)
4. 	java Enumerator aima-alarm.xml B J true M true        (example run command)

(c) How to run your project’s program(s) to demonstrate that it meets the requirements:

Run Commands: 
	Part 1: 
java Enumerator aima-alarm.xml B J true M true
java Enumerator aima-wet-grass.xml R S true
	Part 2: 
java Sampler 1000 aima-alarm.xml B J true M true

You can pretty much run the commands as they are in the project description.
