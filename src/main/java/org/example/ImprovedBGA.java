package org.example;

import java.io.IOException;
import java.util.*;

public class ImprovedBGA extends  BGA{


    public static void main(String[] args) throws IOException {



        ImprovedBGA iBGA = new ImprovedBGA();

        List<int[]> initialPop = iBGA.pseudoRandomInit(200);
        //System.out.println(initialPop);

        List<int[]> finalPop = iBGA.runBGA(initialPop, 0.75f, 0.9f, 2000);

        System.out.println("Number of violations " + iBGA.numViolations(iBGA.constructMatrixFromGeno(finalPop.get(0))));
        System.out.println("Cost of final " + iBGA.fitness(finalPop.get(0)));
        System.out.println("Final solution " + iBGA.genoToPheno(finalPop.get(0)));

    }

    public ImprovedBGA() throws IOException {
        String file1 = "/sppnw41.txt";

        super.readFile(file1);
        System.out.println(super.rows);
        System.out.println(super.cols);
    }

    public List<int[]> pseudoRandomInit(int populationSize){
        System.out.println("Generating pseudo-random initial population");

        List<int[]> population = new ArrayList<>();


        for (int i = 0; i < populationSize; i++){
            List<Integer> rowsCovered = new ArrayList<>();
            List<Integer> uncovered = new ArrayList<>();

            int[] solutionI = new int[super.cols];
            Arrays.fill(solutionI, 0);

            for (int j = 1; j < super.rows + 1; j++){
                uncovered.add(j);
            }

            while (uncovered.size() > 0){
                int rand = super.rand.nextInt(uncovered.size());
                int selectedRow = uncovered.get(rand);

                List<List<Integer>> schedulesCoveringThisRow = new ArrayList<>();

                // Find all schedules that can cover this selected row
                for (int j = 0; j < schedules.length; j++){
                    for (int k = 1; k < schedules[j].size(); k++){
                        if (schedules[j].get(k) == selectedRow){
                            schedulesCoveringThisRow.add(schedules[j]);
                        }
                    }
                }

                // Randomly select a column from schedulesCoveringThisRow (a_i) such that it doesnt cover an already covered row
                int randCol = super.rand.nextInt(schedulesCoveringThisRow.size());
                List<Integer> selectedCol = schedulesCoveringThisRow.get(randCol);

                boolean overCovers = false;

                for (int j = 0; j < selectedCol.size(); j++){
                    if (rowsCovered.contains(selectedCol.get(j))){
                        overCovers = true;
                    }
                }

                if (!overCovers){
                    rowsCovered.add(selectedRow);
                    uncovered.remove(rand);
                    solutionI[randCol] = 1;
                } else {
                    uncovered.remove(rand);
                }
            }
            population.add(solutionI);
        }


        return population;
    }
}
