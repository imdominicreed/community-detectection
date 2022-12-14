package src;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.ArrayList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import src.ParallelMST.Edge;

public class EdgeBetweeness {
    int n;
    int nThreads = 8;
    HashSet<Integer>[] adj_matrix;
    HashMap<Edge, Edge> edges;

    EdgeBetweeness(HashSet<Integer>[] adj_matrix, HashMap<Edge, Edge> edge, int n) {
        this.n = n;
        this.adj_matrix = adj_matrix;
        this.edges = edge;
    }

    public void calculateSerial() {
        for (int i = 0; i < n; i++) {
            calculateFlow(i);
        }
    }

    public void calculateParallel() {
        ExecutorService pool = Executors.newFixedThreadPool(8);

        for (int i = 0; i < n; i++) {
            int finalI = i;
            pool.execute(new Runnable() {
                public void run() {
                    calculateFlow(finalI);

                }
            });
        }
        try {
            pool.shutdown();
            pool.awaitTermination(100, TimeUnit.DAYS);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        pool.shutdownNow();
    }

    private void buildLevelGraph(int root, ArrayList<Integer> distance, ArrayList<ArrayList<Integer>> parents,
            int[] paths, Stack<Integer> order) {
        Queue<Integer> q = new LinkedList<>();
        q.add(root);

        while (!q.isEmpty()) {
            int src = q.poll();
            order.push(src);

            for (int child : adj_matrix[src]) {
                int parentDistance = distance.get(src);

                if (distance.get(child) == -1) {
                    distance.set(child, parentDistance + 1);
                    q.add(child);
                }

                if (distance.get(child) == parentDistance + 1) {
                    paths[child] += paths[src];
                    parents.get(child).add(src);
                }
            }
        }
    }

    private void addFlows(ArrayList<Integer> distances, ArrayList<ArrayList<Integer>> parents, int[] paths,
            Stack<Integer> order) {
        double[] flows = new double[n];

        while (!order.isEmpty()) {
            int pop = order.pop();

            for (int par : parents.get(pop)) {
                double flow = 1 + flows[pop];
                Edge e = edges.get(new Edge(par, pop));
                double flowCalculation = flow * ((double) paths[par] / paths[pop]);
                e.addFlow(flowCalculation);
                flows[par] += flowCalculation;
            }
        }
    }

    private void calculateFlow(int root) {
        //Initialize the first level with the root
        ArrayList<Integer> distances = new ArrayList<>(n);
        for (int i = 0;  < n; i++)
            distances.add(-1);
        distances.set(root, 0);

        //Prepares parent adj matrix
        ArrayList<ArrayList<Integer>> parents = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            parents.add(new ArrayList<>());

        //Number of paths to node from root
        int[] paths = new int[n];
        paths[root] = 1; //root node starts at 1

        Stack<Integer> order = new Stack<>();

        buildLevelGraph(root, distances, parents, paths, order);

        addFlows(distances, parents, paths, order);
    }

}