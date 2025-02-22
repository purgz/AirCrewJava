package org.example;

import org.math.plot.Plot2DPanel;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

public class ImprovedBGA extends  BGA{


    public static void main(String[] args) throws IOException {

        String fileOption = ""; // either 1 2 or 3

        int numRunsForAverage = 5;

        boolean runFromCmd = false;
        if (args.length > 0){
            fileOption = args[0];
            runFromCmd = true;

            if (args.length > 1){
                // If an extra argument is supplied use it for the number of runs
                numRunsForAverage = Integer.parseInt(args[1]);
            }
        }

        if (!(fileOption.equals("1") || fileOption.equals("2") || fileOption.equals("3"))){
            if (runFromCmd){
                System.out.println("Invalid argument");
            }
        }


        // Run 30 times for each file and get average
        String file1 = "/sppnw41.txt";
        String file2 = "/sppnw42.txt";
        String file3 = "/sppnw43.txt";

        String fileToRun = file1;

        System.out.println("Running improved binary genetic algorithm (paper 1) on " + fileToRun);

        if (runFromCmd){
            if (fileOption.equals("1")){
                fileToRun = file1;
            } else if (fileOption.equals("2")){
                fileToRun = file2;
            } else if (fileOption.equals("3")){
                fileToRun = file3;
            }

        }

        List<List<Double>> allBestHistory  = new ArrayList<>();
        List<List<Double>> allAllHistory = new ArrayList<>();
        List<Float> bestForEach = new ArrayList<>();
        System.out.println("Running with file: " + fileToRun + " for " + numRunsForAverage + " iterations");
        for (int i = 0; i < numRunsForAverage; i++){
            System.out.println("Iteration " + (i + 1) + " of " + numRunsForAverage);
            ImprovedBGA ibgaFile1 = new ImprovedBGA(fileToRun);
            List<int[]> initialPop = ibgaFile1.pseudoRandomInit(50);
            List<int[]> finalPop = ibgaFile1.runBGA(initialPop, 0.9f, 1f, 30);
            bestForEach.add(ibgaFile1.fitness(finalPop.get(0)));
            allBestHistory.add(ibgaFile1.bestHistory);
            allAllHistory.add(ibgaFile1.allHistory);
        }

        // Calculate standard deviation and mean

        double mean = 0d;
        double standardDeviation = 0d;

        for (int i = 0; i < bestForEach.size(); i++){
            mean += bestForEach.get(i);
        }
        mean = mean / bestForEach.size();

        for (int i = 0; i < bestForEach.size(); i++){
            standardDeviation += Math.pow(bestForEach.get(i) - mean, 2);
        }
        standardDeviation =  Math.sqrt(standardDeviation/ bestForEach.size());

        System.out.println("Mean of best final results: " + mean);
        System.out.println("Standard deviation of final results " + standardDeviation);


        System.out.println("Producing plot of best and all history for " + fileToRun);
        Plot2DPanel plot = new Plot2DPanel();

        for (int i = 0; i < allBestHistory.size(); i++){
            double[] h = new double[allBestHistory.get(i).size()];
            double[] g = new double[allBestHistory.get(i).size()];
            for (int j = 0; j < allBestHistory.get(i).size(); j++){
                h[j] = allBestHistory.get(i).get(j);
                g[j] = allAllHistory.get(i).get(j);
            }
            plot.addLinePlot("Best cost history for "+i+"th run of "  + numRunsForAverage,  h);
            //plot.addLinePlot("Cost history", g);
        }
        // maybe plot only best history to reduce clutter
        plot.addLegend("NORTH");
        JFrame frame = new JFrame("Plot panel");
        frame.setSize(800,800);
        frame.setContentPane(plot);
        frame.setVisible(true);
    }

    public ImprovedBGA(String fileName) throws IOException {

        super.readFile(fileName);
        System.out.println(super.rows);
        System.out.println(super.cols);
    }


    // Heuristic improvement operator from paper 1
    // This thing is a miracle worker
    public int[] heuristicImprovement(int[] child){

        // DROP procedure
        // Identify over covered rows, randomly remove until all rows covered by at most 1 column
        List<Integer> colsInSolutionIndex = new ArrayList<>();
        for (int i = 0; i < child.length; i++){
            if (child[i] == 1){
                colsInSolutionIndex.add(i);
            }
        }
        // T in paper

        List<Integer> colsTemp = new ArrayList<>(colsInSolutionIndex);
        while(colsTemp.size() > 0){

            // Randomly select a column j, in T
            int randT = rand.nextInt(colsTemp.size());
            List<Integer> randomCol = schedules[colsTemp.get(randT)];
            int selectedCol = colsTemp.get(randT);

            colsTemp.remove(randT); //Remove from T


            //Find rows covered by j
            List<Integer> rowsCoveredByJ = new ArrayList<>();
            for (int i = 1; i < randomCol.size(); i++){
                rowsCoveredByJ.add(randomCol.get(i));
            }


            for (int row : rowsCoveredByJ){
                // If number of cols in S covering this row is >= 2 then remove J from the solution
                int w_i = 0;
                for (int col : colsInSolutionIndex){
                    if (schedules[col].subList(1, schedules[col].size()).contains(row)){
                        w_i++;
                    }
                }
                if (w_i >= 2){
                    // Remove column J from s
                    colsInSolutionIndex.remove(Integer.valueOf(selectedCol));
                }
            }


        }


        // ADD procedure
        // Find uncovered rows - U
        List<Integer> uncoveredRows = new ArrayList<>();
        for (int i = 1; i <= rows; i++){
            uncoveredRows.add(i);
        }
        for (Integer i : colsInSolutionIndex){
            uncoveredRows.removeAll(schedules[i].subList(1, schedules[i].size()));
        }

        List<Integer> dummy = new ArrayList<>(uncoveredRows);

        while (dummy.size() > 0){
            // Randomly select a row in V
            int r = rand.nextInt(dummy.size());
            int selectedRow = dummy.get(r);
            dummy.remove(r);


            // Search for new column with rows as subset of Uncovered rows U,  and minimises c / {num covered}
            List<Integer> columnsToCheck = new ArrayList<>();
            for (int i = 0; i < schedules.length; i++){
                if (colsInSolutionIndex.contains(i)){
                    continue;
                }
                // Check if rows covered is a subset - can use containsAll method
                if (schedules[i].subList(1,schedules[i].size()).contains(selectedRow) && new HashSet<>(uncoveredRows).containsAll(schedules[i].subList(1, schedules[i].size()))){
                    columnsToCheck.add(i);
                }

                if (columnsToCheck.size() != 0){

                    // sort by cost / rows covered
                    int greedyChoice = 0;
                    float minSoFar = Float.MAX_VALUE;
                    for (int j = 0; j < columnsToCheck.size(); j++){
                        float bj = (float) schedules[columnsToCheck.get(j)].size() - 1;
                        float cj = schedules[columnsToCheck.get(j)].get(0);
                        if ((bj / cj) < minSoFar){
                            minSoFar = bj / cj;
                            greedyChoice = columnsToCheck.get(j);
                        }
                    }

                    // Add to solution
                    colsInSolutionIndex.add(greedyChoice);
                    // Remove all the new covered rows from dummy and uncovered rows
                    uncoveredRows.removeAll(schedules[greedyChoice].subList(1,schedules[greedyChoice].size()));
                    dummy.removeAll(schedules[greedyChoice].subList(1,schedules[greedyChoice].size()));
                }
            }
        }

        return phenoToGeno(colsInSolutionIndex);
    }

    public float fitnessWithoutViolations(int[] genotype){
        int total = 0;
        int[][] constraints = constructMatrixFromGeno(genotype);

        for (int i = 0; i < genotype.length; i++){
            if (genotype[i] == 1){
                total += schedules[i].get(0);
            }
        }
        return total;
    }


    public List<int[]> stochasticRanking(List<int[]> population){

        List<int[]> sorted = new ArrayList<>(population);

        int numSwaps = 1;
        // While no more change in ranking
        double pf = 0.25;
        while (numSwaps > 0){
           // System.out.println(numSwaps);
            numSwaps = 0;

            for (int i = 0; i < sorted.size() - 1; i++){
                int v1 = numViolations(constructMatrixFromGeno(sorted.get(i)));
                int v2 = numViolations(constructMatrixFromGeno(sorted.get(i+1)));
                double u = Math.random();
                if (v1 == 0 && v2 == 0 || u < pf){
                    if (fitnessWithoutViolations(sorted.get(i)) > fitnessWithoutViolations(sorted.get(i+1))){
                        // Swap i and i + 1
                        int[] tmp = sorted.get(i);
                        sorted.set(i, sorted.get(i+1));
                        sorted.set(i + 1, tmp);
                        numSwaps++;
                    }
                } else {
                    // Swap if G is less
                    if (v1 > v2){
                        int[] tmp = sorted.get(i);
                        sorted.set(i, sorted.get(i+1));
                        sorted.set(i + 1, tmp);
                        numSwaps++;
                    }
                }

            }
        }
        return sorted;
    }


    // Overriding run method
    // This now includes the stochastic ranking method instead of sorting by cost
    // Also includes heuristic operator applied on each offspring.
    public List<int[]> runBGA(List<int[]> population, float crossoverRate, float crossoverTypeRate, int maxIter){


        int popSize = population.size();
        int parentSize = popSize / 2;


        PopulationComparator comparator = new PopulationComparator();

        // EVALUATE FITNESS
        // Implement stochastic ranking
        //population.sort(comparator);
        population = stochasticRanking(population);

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


            if (i % 10 == 0){
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

                // RUN THE heuristic improvement operator
                child = heuristicImprovement(child);


                newPopulation.add(child);
            }

            // Sort based on fitness
            //newPopulation.sort(comparator);
            // Using the stochastic sorting instead
            newPopulation = stochasticRanking(newPopulation);

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
