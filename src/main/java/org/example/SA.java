package org.example;

import org.math.plot.Plot2DPanel;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SA {

    int rows = 0;
    int cols = 0;

    List<Integer>[] schedules;

    List<Double> bestHistory = new ArrayList<>();
    List<Double> allHistory = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Simulated annealing");

        String file1 = "src/main/java/org/example/sppnw42.txt";

        SA sa = new SA();

        sa.readFile(file1);


        List<Integer> initial = sa.greedyInit();

        int[][] matrix = sa.constructMatrix(initial);

        List<Integer> finalS = sa.runSA(initial);
        System.out.println(finalS);
        System.out.println(sa.feasible(sa.constructMatrix(finalS)));
        System.out.println(sa.cost(finalS, sa.constructMatrix(finalS)));



        double[] h = new double[sa.bestHistory.size()];
        double[] g = new double[sa.bestHistory.size()];
        for (int i = 0; i < sa.bestHistory.size(); i++){
            h[i] = sa.bestHistory.get(i);
            g[i] = sa.allHistory.get(i);
        }

        //Produce graph to show ALL history on same graph could be nice

        Plot2DPanel plot = new Plot2DPanel();
        plot.addLinePlot("Best cost history",  h);
        plot.addLinePlot("Cost history", g);
        plot.addLegend("NORTH");
        JFrame frame = new JFrame("Plot panel");
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
        float bestCost = 10000000;
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

        BufferedReader reader = new BufferedReader(new FileReader(fileName));

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
