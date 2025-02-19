package org.example;

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


        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(36);
        list.add(40);
        list.add(156);
        list.add(167);
        int[] geno = bga.phenoToGeno(list);
        int[][] matrixFromBga = bga.constructMatrixFromGeno(bga.phenoToGeno(list));
        System.out.println(bga.numViolations(matrixFromBga));

        System.out.println(bga.mutationRate);

        System.out.println(Arrays.toString(geno));
        System.out.println(Arrays.toString(bga.mutateGenoType(geno)));
        System.out.println(bga.fitness(geno));




        List<int[]> initpopulation = new ArrayList<>();
        // random pop of length 200
        for (int i = 0; i < 200; i++){
            initpopulation.add(bga.randomPheno());
        }

        List<int[]> finalPop = bga.runBGA(initpopulation);

        System.out.println("Number of violations " + bga.numViolations(bga.constructMatrixFromGeno(finalPop.get(0))));
        System.out.println("Cost of final " + bga.fitness(finalPop.get(0)));
        System.out.println("Final solution " + bga.genoToPheno(finalPop.get(0)));


        /*
        // Just a little code to check that the bga is implementing the matrix the same as SA
        // Which is known to be correct
        SA sa = new SA();
        sa.readFile(file1);
        int[][] matrixFromSA = sa.constructMatrix(list);
        boolean same = true;
        for (int i = 0; i < matrixFromSA.length; i++){
            if (!Arrays.equals(matrixFromBga[i], matrixFromSA[i])){
                same = false;
            }
        }
        System.out.println("Are matrix from bga and sa the same " + same);
         */

    }

    // SORT SMALLEST TO LARGEST
    public class PopulationComparator implements Comparator<int[]> {

        @Override
        public int compare(int[] o1, int[] o2) {
            int cost1 = (int) fitness(o1);
            int cost2 = (int) fitness(o2);
            return cost1 - cost2;
        }
    }

    public List<int[]> runBGA(List<int[]> population){

        int maxIter = 10000;

        int popSize = population.size();
        int parentSize = 50;

        PopulationComparator comparator = new PopulationComparator();

        // EVALUATE FITNESS
        population.sort(comparator);

        // Attemping something similar to the simulated annealing reheating
        for (int i = 0; i < maxIter; i++){

            mutationRate = Math.max((float) 1 / population.get(0).length, mutationRate * 0.9f);
            if (i % 100 == 0){
                System.out.println("Best at " + i + " " + fitness(population.get(0)));
            }

            List<int[]> newPopulation = new ArrayList<>();

            // SELECT PARENTS
            List<int[]> parents = new ArrayList<>(population.subList(0,parentSize));
            List<int[]> tail = new ArrayList<>(population.subList(popSize - 50, popSize));

            parents.addAll(tail);

            newPopulation.addAll(parents);
            // VARIATION - Breed new individuals
            while (newPopulation.size() < popSize){
                int r = rand.nextInt(parents.size());
                int r2 = rand.nextInt(parents.size());
                int[] child = parents.get(r).clone();
                if (Math.random() < 0.85){
                    //Apply crossover
                    child = crossOver(parents.get(r), parents.get(r2));
                }
                child = mutateGenoType(child);
                newPopulation.add(child);
            }


            newPopulation.sort(comparator);

            population = newPopulation;
        }


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
        total = total + 10000 * (violations * violations);

        return total;
    }

    // Takes two genotypes and returns the simple crossover in array {child1,child2}
    public int[] crossOver(int[] parent1, int[] parent2){

        // Find random crossover point
        int crossover = rand.nextInt(parent1.length);

        int[] child1 = new int[parent1.length];

        for (int i = 0; i < crossover; i++){
            child1[i] = parent1[i];
        }

        for (int i = crossover; i < parent1.length; i++){
            child1[i] = parent2[i];
        }


        return child1;
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
            }
            if (rowSum != 1){
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
        mutationRate = 0.5f;
    }
}
