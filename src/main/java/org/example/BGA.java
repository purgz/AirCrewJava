package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class BGA {

    int rows = 0;
    int cols = 0;

    List<Integer>[] schedules;

    List<Double> bestHistory = new ArrayList<>();
    List<Double> allHistory = new ArrayList<>();


    public static void main(String[] args) throws IOException {

        // Please excuse the duplicated code from SA file, it seemed easier this way

        String file1 = "/sppnw41.txt";

        BGA bga = new BGA();

        bga.readFile(file1);

        System.out.println(bga.schedules[0]);
        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(36);
        list.add(40);
        list.add(156);
        list.add(167);
        bga.phenoToGeno(list);
        int[][] matrixFromBga = bga.constructMatrixFromGeno(bga.phenoToGeno(list));
        System.out.println(bga.numViolations(matrixFromBga));
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
    }
}
