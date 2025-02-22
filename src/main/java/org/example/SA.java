package org.example;

import org.math.plot.Plot2DPanel;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class SA {

    int rows = 0;
    int cols = 0;

    List<Integer>[] schedules;

    List<Double> bestHistory = new ArrayList<>();
    List<Double> allHistory = new ArrayList<>();

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

        System.out.println("Running Simulated annealing on " + fileToRun);




        List<List<Double>> allBestHistory  = new ArrayList<>();
        List<List<Double>> allAllHistory = new ArrayList<>();
        List<Float> bestForEach = new ArrayList<>();
        for (int i = 0; i < numRunsForAverage; i++){

            SA sa = new SA();
            List<Integer> initial = sa.greedyInit();
            sa.readFile(fileToRun);
            List<Integer> finalS = sa.runSA(initial);

            allBestHistory.add(sa.bestHistory);
            allAllHistory.add(sa.allHistory);
            bestForEach.add(sa.cost(finalS, sa.constructMatrix(finalS)));
            System.out.println("Solution found " + finalS + " Cost: " + sa.cost(finalS, sa.constructMatrix(finalS)));
        }



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

    public SA(){

    }


    public List<Integer> runSA(List<Integer> initial){
        Random rand = new Random();
        List<Integer> solution = new ArrayList<>(initial);

        int sinceBestChanged = 0;

        int[][] matrix = constructMatrix(solution);

        float initialTemp = 10000000;
        float temp = initialTemp;

        List<Integer> bestSolution = new ArrayList<>(solution);
        float bestCost = 70000;
        int[][] bestMatrix = constructMatrix(bestSolution);

        int softLimit = 100000;
        int iter = 0;


        while (iter < softLimit || !feasible(bestMatrix)){
//            // Some logging
//            System.out.println(iter);
//            System.out.println(bestSolution);
//            System.out.println(cost(bestSolution, bestMatrix));

            allHistory.add((double) cost(solution, matrix));
            bestHistory.add((double) bestCost);

            if (sinceBestChanged > 2000){
                sinceBestChanged = 0;
                temp = 1000000;
            }

            temp *= 0.999;

            List<Integer> neighbour = new ArrayList<>(solution);
            int[][] newMatrix;

            int addOrRemove = rand.nextInt(2);

            if (neighbour.size() < 1){
                addOrRemove = 1;
            }

            int r;
            if (addOrRemove == 1){

                r = rand.nextInt(cols - 1);
                if (!neighbour.contains(r))
                    neighbour.add(r);
            } else {
                r = rand.nextInt(neighbour.size());
                neighbour.remove(neighbour.get(r));
            }
            newMatrix = constructMatrix(neighbour);

            double diff = (double) cost(neighbour, newMatrix) - cost(solution, matrix);

            if (diff < 0){
                solution = neighbour;
                matrix = newMatrix;
            } else {
                double p = Math.exp((-diff / temp));
                if (Math.random() < p){
                    solution = neighbour;
                    matrix = newMatrix;
                }
            }

            if (cost(solution, matrix) < bestCost){

                sinceBestChanged = 0;
                bestCost = cost(solution, matrix);
                bestSolution = new ArrayList<>(solution);
                bestMatrix = constructMatrix(bestSolution);

            }


            sinceBestChanged++;
            iter++;
        }

        return bestSolution;
    }


    public int[][] constructMatrix(List<Integer> candidate){
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < matrix.length; i++){
            Arrays.fill(matrix[i], 0);
        }

        for (int i = 0; i < candidate.size(); i++){
            List<Integer> schedule = schedules[candidate.get(i)];
            for (int j = 1; j < schedule.size(); j++){
                matrix[schedule.get(j) - 1][candidate.get(i)] = 1;
            }
        }

        return matrix;
    }


    public boolean feasible(int[][] matrix){
        for (int i = 0; i < matrix.length; i++){
            int sum = 0;
            for (int j = 0; j < matrix[i].length; j++){
                sum = sum + matrix[i][j];
            }
            if (sum != 1){
                return false;
            }
        }
        return true;
    }

    public int numViolations(int[][] matrix){
        int total = 0;
        for (int i = 0; i < matrix.length; i++){
            int sum = 0;
            for (int j = 0; j < matrix[i].length; j++){
                sum = sum + matrix[i][j];
            }
            if (sum != 1){
                total++;
            }
        }
        return total;
    }

    public float cost(List<Integer> candidate, int[][] matrix){
        int total = 0;

        for (int i = 0; i < candidate.size(); i++){
            total = total + schedules[candidate.get(i)].get(0);
        }

        int v = numViolations(matrix);

        total = total + 100000 * (v * v);

        return total;
    }


    public List<Integer> greedyInit(){

        List<Integer> candidate = new ArrayList<>();

        Set<Integer> covered = new HashSet<>();
        Set<Integer> uncovered = new HashSet<>();
        for (int i = 1; i <= rows; i++){
            uncovered.add(i);
        }


        while (covered.size() != uncovered.size()){
            int bestCol = -1;
            float bestCost = 1000000000f;
            for (int i = 0; i < cols; i++){
                if (!candidate.contains(i)){
                    List<Integer> covers = schedules[i].subList(1, schedules[i].size());
                    Set<Integer> newCovered = new HashSet<>(covers);
                    newCovered.remove(covered);
                    int newCover = newCovered.size();
                    if (newCover == 0){
                        continue;
                    }

                    float costPerRow = (float) schedules[i].get(0) / newCover;

                    if (costPerRow < bestCost){
                        bestCol = i;
                        bestCost = costPerRow;
                    }
                }
            }
            candidate.add(bestCol);
            covered.addAll(schedules[bestCol].subList(1, schedules[bestCol].size()));
        }


        return candidate;
    }

    public void readFile(String fileName) throws IOException{

        InputStream is = SA.class.getResourceAsStream(fileName);
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
    }
}
