package org.example;

import org.math.plot.Plot2DPanel;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class BGA {

    int rows = 0;
    int cols = 0;

    List<Integer>[] schedules;

    List<Double> bestHistory = new ArrayList<>();
    List<Double> allHistory = new ArrayList<>();

    // Setting this to the standard 1 / numBits
    float mutationRate;

    Random rand = new Random();


    public static void main(String[] args) throws IOException {

        // Please excuse the duplicated code from SA file, it seemed easier this way

        String file1 = "/sppnw42.txt";

        BGA bga = new BGA();

        bga.readFile(file1);



        List<int[]> initpopulation = new ArrayList<>();
        // random pop of length 200
        for (int i = 0; i < 200; i++){
            initpopulation.add(bga.randomPheno());
        }

        List<int[]> finalPop = bga.runBGA(initpopulation, 0.9f, 0.8f, 2000);    // for file 2 and 1
       // List<int[]> finalPop = bga.runBGA(initpopulation, 0.75f, 0.6f);   // for file 3


        // PRODUCE DATA AND STANDARD DEVIATION FOR ALL 3 on 30 runs.



        System.out.println("Number of violations " + bga.numViolations(bga.constructMatrixFromGeno(finalPop.get(0))));
        System.out.println("Cost of final " + bga.fitness(finalPop.get(0)));
        System.out.println("Final solution " + bga.genoToPheno(finalPop.get(0)));

        double[] h = new double[bga.bestHistory.size()];
        double[] g = new double[bga.bestHistory.size()];
        for (int i = 0; i < bga.bestHistory.size(); i++){
            h[i] = bga.bestHistory.get(i);
            g[i] = bga.allHistory.get(i);
        }


        Plot2DPanel plot = new Plot2DPanel();
        plot.addLinePlot("Best cost history",  h);
        plot.addLinePlot("Cost history", g);
        plot.addLegend("NORTH");
        JFrame frame = new JFrame("Plot panel");
        frame.setSize(800,800);
        frame.setContentPane(plot);
        frame.setVisible(true);

    }

    // SORT SMALLEST TO LARGEST
    public class PopulationComparator implements Comparator<int[]> {

        @Override
        public int compare(int[] o1, int[] o2) {
            float cost1 =  fitness(o1);
            float cost2 =  fitness(o2);
            return Float.compare(cost1,cost2);
        }
    }

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

    // fitness function
    public float fitness(int[] genotype){
        int total = 0;
        int[][] constraints = constructMatrixFromGeno(genotype);

        for (int i = 0; i < genotype.length; i++){
            if (genotype[i] == 1){
                total += schedules[i].get(0);
            }
        }

        int violations = numViolations(constraints);

        // Simple static penalty coefficient
      //  total = total + 10000 * (violations * violations);
        // I found a linear penalty worked better
        total = total + 10000 * violations;

        return total;
    }

    public int[] crossOver1(int[] parent1, int[] parent2) {

        // Find random crossover point
        int crossover1 = rand.nextInt(parent1.length);
        int crossover2 = rand.nextInt(crossover1, parent1.length);
        int crossover3 = rand.nextInt(crossover2, parent1.length);
        int crossover4 = rand.nextInt(crossover3, parent1.length);

        int[] child1 = new int[parent1.length];

        System.arraycopy(parent1, 0, child1, 0, crossover1);

        if (crossover3 - crossover2 >= 0)
            System.arraycopy(parent2, crossover2, child1, crossover2, crossover3 - crossover2);

        if (crossover4 - crossover3 >= 0)
            System.arraycopy(parent1, crossover3, child1, crossover3, crossover4 - crossover3);

        if (parent1.length - crossover4 >= 0)
            System.arraycopy(parent2, crossover4, child1, crossover4, parent1.length - crossover4);

        return child1;
    }

    // Takes two genotypes and returns the simple crossover in array {child1,child2}
    // WOW - switching to multi-point crossover MASSIVELY improved performance
    public int[] crossOver(int[] parent1, int[] parent2){

        // Random n point crossover
        List<Integer> crossOverPoints = new ArrayList<>();
        int numCrossover = rand.nextInt(2,10);


        crossOverPoints.add(rand.nextInt(parent1.length));

        for (int i = 1; i < numCrossover; i++){
            crossOverPoints.add(rand.nextInt(crossOverPoints.get(i-1), parent1.length));
        }

        int[] child1 = new int[parent1.length];

        System.arraycopy(parent1, 0, child1, 0, crossOverPoints.get(1) - crossOverPoints.get(0));

        for (int i = 1; i < crossOverPoints.size(); i++){
            if (i % 2 == 0) {
                System.arraycopy(parent2, crossOverPoints.get(i-1), child1, crossOverPoints.get(i-1), crossOverPoints.get(i) - crossOverPoints.get(i-1));
            } else {
                System.arraycopy(parent1, crossOverPoints.get(i-1), child1, crossOverPoints.get(i-1), crossOverPoints.get(i) - crossOverPoints.get(i-1));
            }
            if (i == crossOverPoints.size() - 1){
                System.arraycopy(parent1, crossOverPoints.get(i), child1, crossOverPoints.get(i), parent1.length - crossOverPoints.get(i));
            }
        }

        return child1;
    }

    public int[] uniformCrossOver(int[] parent1, int parent2[]){

        int[] child = new int[parent1.length];

        for (int i = 0; i < parent1.length; i++){
            if (Math.random() < 0.5){
                child[i] = parent1[i];
            } else{
                child[i] = parent2[i];
            }
        }
        return child;
    }

    public int[] mutateGenoType(int[] geno){
        for (int i = 0; i < geno.length; i++){
            double r = Math.random();
            if (r < mutationRate){
                geno[i] = geno[i] == 1 ? 0 : 1;
            }
        }
        return geno;
    }


    public int numViolations(int[][] matrix){
        int v = 0;

        for (int i = 0; i < matrix.length; i++){
            int rowSum = 0;
            for (int j = 0; j < matrix[i].length; j++){
                rowSum += matrix[i][j];
                if (rowSum > 1){
                    v++;
                    break;
                }
            }
            if (rowSum == 0){
                v++;
            }
        }

        return v;
    }

    public int[][] constructMatrixFromGeno(int[] genotype){

        int[][] matrix = new int[rows][cols];

        for (int i = 0; i < matrix.length; i++){
            Arrays.fill(matrix[i], 0);
        }

        for (int i = 0; i < genotype.length; i++){
            if (genotype[i] == 1){
                List<Integer> schedule = schedules[i];
                for (int j = 1; j < schedule.size(); j++){
                    matrix[schedule.get(j) - 1][i] = 1;
                }
            }
        }

        return matrix;
    }

    public int[] randomPheno(){

        int[] pheno = new int[cols];
        for (int i = 0; i < pheno.length; i++){
            pheno[i] = Math.random() < 0.5 ? 1 : 0;
        }

        return pheno;
    }

    // Bit-string representation
    // Some nice printing functions
    public int[] phenoToGeno(List<Integer> phenotype){
        int[] geno = new int[cols];

        for (int i = 0; i < geno.length; i++){
            if (phenotype.contains(i)){
                geno[i] = 1;
            } else{
                geno[i] = 0;
            }
        }
        return geno;
    }

    public List<Integer> genoToPheno(int[] geno){
        List<Integer> pheno = new ArrayList<>();

        for (int i = 0; i < geno.length; i ++){
            if (geno[i] == 1){
                pheno.add(i);
            }
        }
        return pheno;
    }

    public BGA(){

    }

    public void readFile(String fileName) throws IOException {

        InputStream is = BGA.class.getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String currentLine = reader.readLine();

        // System.out.println(currentLine);

        rows = Integer.parseInt(currentLine.split(" ")[0]);
        cols = Integer.parseInt(currentLine.split(" ")[1]);

        schedules = new ArrayList[cols];

        int i = 0;
        while ((currentLine = reader.readLine()) != null){

            String[] line = currentLine.split(" ");

            //System.out.println(Arrays.toString(line));
            schedules[i] = new ArrayList<>();
            for (int j = 0; j < line.length; j++){
                if (j != 1){
                    schedules[i].add(Integer.parseInt(line[j]));
                }
            }

            i++;
        }

        reader.close();

        mutationRate =(float) 1 / schedules.length;
    }
}
