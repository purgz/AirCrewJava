package org.example;

import java.io.IOException;
import java.util.*;

public class ImprovedBGA extends  BGA{


    public static void main(String[] args) throws IOException {



        ImprovedBGA iBGA = new ImprovedBGA();

        List<int[]> initialPop = iBGA.pseudoRandomInit(200);
        //System.out.println(initialPop);

       // iBGA.heuristicImprovement(initialPop.get(0));

        for (int i = 0; i < initialPop.get(2).length; i++){
            if (initialPop.get(5)[i] == 1){
                System.out.println(i);
                System.out.println(iBGA.schedules[i]);
            }
        }

        List<int[]> finalPop = iBGA.runBGA(initialPop, 0.9f, 1f, 2000);

        System.out.println("Number of violations " + iBGA.numViolations(iBGA.constructMatrixFromGeno(finalPop.get(0))));
        System.out.println("Cost of final " + iBGA.fitness(finalPop.get(0)));
        System.out.println("Final solution " + iBGA.genoToPheno(finalPop.get(0)));

    }

    public ImprovedBGA() throws IOException {
        String file1 = "/sppnw43.txt";

        super.readFile(file1);
        System.out.println(super.rows);
        System.out.println(super.cols);
    }



    // Heuristic improvement operator from paper 1
    public int[] heuristicImprovement(int[] child){

        List<Integer> colsInSolution = new ArrayList<>();
        for (int i = 0; i < child.length; i++){
            if (child[i] == 1){
                colsInSolution.add(i);
            }
        }
        // T in paper
        List<Integer> colsTemp = new ArrayList<>(colsInSolution);
        while(colsTemp.size() > 0){

            // Randomly select a column j, in T
            int randT = rand.nextInt(colsTemp.size());
            List<Integer> randomCol = schedules[colsTemp.get(randT)];
            colsTemp.remove(randT); //Remove from T

            //Find rows covered by j
            List<Integer> rowsCoveredByJ = new ArrayList<>();
            for (int i = 1; i < randomCol.size(); i++){
                rowsCoveredByJ.add(i);
            }

            System.out.println(rowsCoveredByJ);

        }

        // DROP procedure
        // Identify over covered rows, randomly remove until all rows covered by at most 1 column


        return null;
    }


    // Overriding run method
    public List<int[]> runBGA(List<int[]> population, float crossoverRate, float crossoverTypeRate, int maxIter){


        int popSize = population.size();
        int parentSize = popSize / 2;


        PopulationComparator comparator = new PopulationComparator();

        // EVALUATE FITNESS
        population.sort(comparator);

        float bestCost = 1000000f;

        //Keeping track of the best solution since it may be lost at each iteration without elitism
        int[] bestsolution = null;
        int timeSinceBestChanged = 0;

        int i = 0;
        while (i < maxIter || numViolations(constructMatrixFromGeno(bestsolution)) > 0){

            i++;
            timeSinceBestChanged++;

            if (fitness(population.get(0)) < bestCost){
                bestCost = fitness(population.get(0));
                bestsolution = population.get(0);
                timeSinceBestChanged = 0;
            }

            bestHistory.add((double) bestCost);
            allHistory.add((double) fitness(population.get(0)));

            if (timeSinceBestChanged > 1000){

                timeSinceBestChanged = 0;
            }

            List<int[]> newPopulation = new ArrayList<>();


            if (i % 100 == 0){
                System.out.println("Best at " + i + " " + fitness(population.get(0)));
                System.out.println("Best so far " + bestCost);
                //System.out.println("Current mutation rate " + mutationRate);

            }

            double p = (double) i / maxIter;

            if (Math.random() < p){
                newPopulation.add(population.get(0));
                population.remove(0);
            }

            if (i % 50 == 0){
                newPopulation.add(population.get(0));
                population.remove(0);
            }


            // SELECT PARENTS
            /*
            I'm using a combination of selecting the strongest parents to keep into the next generation and breed
            As well as using a tournament selection based on the rank of individuals with tournament size 2.
             */


            List<int[]> parents = new ArrayList<>();

            // Select parents using tournament selection based on the rank - decreases likelihood of elites dominating
            while (parents.size() < parentSize){
                // Kind of tournament on random pairs, winner is the one with higher rank
                int rand1 = rand.nextInt(population.size() - 1);
                int rand2 = rand.nextInt(population.size() - 1);
                int rand3 = rand.nextInt(population.size() - 1);
                int rand4 = rand.nextInt(population.size() - 1);

                //int best = Math.min(rand1, rand2);
                int best = Math.min(Math.min(Math.min(rand1,rand2),rand3), rand4);
                parents.add(population.get(best));

                List<int[]> options = new ArrayList<>();
                options.add(population.get(best));


                population.removeAll(options);

            }


            // VARIATION - Breed new individuals
            while (newPopulation.size() < popSize){
                int r = rand.nextInt(parents.size());
                int r2 = rand.nextInt(parents.size());
                int[] child = parents.get(r).clone();

                // Apply either multipoint crossover or uniform cross - prefer multipoint due to efficiency
                if (Math.random() < crossoverRate){
                    if (Math.random() < crossoverTypeRate){
                        //Apply crossover
                        child = crossOver1(parents.get(r), parents.get(r2));

                    } else {
                        child = uniformCrossOver(parents.get(r), parents.get(r2));
                    }
                }


                // Mutate the new child
                child = mutateGenoType(child);
                newPopulation.add(child);
            }

            // Sort based on fitness
            newPopulation.sort(comparator);

            // Update population
            population = newPopulation;
        }

        // Return the strongest solution found over the whole run
        population.set(0, bestsolution);
        return population;
    }


    public List<int[]> pseudoRandomInit(int populationSize){
        System.out.println("Generating pseudo-random initial population");

        List<int[]> population = new ArrayList<>();


        for (int i = 0; i < populationSize; i++){
            List<Integer> rowsCovered = new ArrayList<>();
            List<Integer> uncovered = new ArrayList<>();

            int[] solutionI = new int[super.cols];
            Arrays.fill(solutionI, 0);

            for (int j = 1; j <= super.rows; j++){
                uncovered.add(j);
            }

            while (uncovered.size() > 0){
                int rand = super.rand.nextInt(uncovered.size());
                int selectedRow = uncovered.get(rand);

                List<Integer> scheduleIndexes = new ArrayList<>();  // Just keeps track of the binary digit index in main string

                // Find all schedules that can cover this selected row
                for (int j = 0; j < schedules.length; j++){
                    if (schedules[j].subList(1,schedules[j].size()).contains(selectedRow)){
                        scheduleIndexes.add(j);
                    }
                }

                // Randomly select a column from schedulesCoveringThisRow (a_i) such that it doesnt cover an already covered row
                int randCol = super.rand.nextInt(scheduleIndexes.size());
                List<Integer> selectedCol = schedules[scheduleIndexes.get(randCol)];

                boolean overCovers = false;

                for (int j = 1; j < selectedCol.size(); j++){
                    if (rowsCovered.contains(selectedCol.get(j))){
                        overCovers = true;
                        break;
                    }
                }

                if (!overCovers){
                    solutionI[scheduleIndexes.get(randCol)] = 1;
                    for (int j = 1; j < schedules[scheduleIndexes.get(randCol)].size(); j++){
                        rowsCovered.addAll(schedules[scheduleIndexes.get(randCol)]);
                        uncovered.removeAll(schedules[scheduleIndexes.get(randCol)]);
                    }
                } else {
                    rowsCovered.add(uncovered.remove(rand));
                }

            }

            population.add(solutionI);
        }


        return population;
    }
}
