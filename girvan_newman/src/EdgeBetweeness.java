package src;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.ArrayList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import src.Graph.Node;
import src.Graph.Edge;

public class EdgeBetweeness {
    Graph g;
    int n;
    int nThreads = 8;

    EdgeBetweeness(Collection<int[]> edges, int n, boolean serial) {
        this.n = n;
        g = new Graph(edges, n, false);
    }

    EdgeBetweeness(Graph g, int n) {
        this.n = n;
        this.g = g;
    }

    public ArrayList<Edge> calculateSerial() {
        for (int i = 0; i < n; i++) {
            calculateFlow(i);
        }
        for (Edge e : g.edges.values())
            e.flow /= 2;
        return new ArrayList<>(g.edges.values());

    }

    public ArrayList<Edge> calculateParallel() {
        for (Edge e : g.iter_edges.values())
            e.flow = 0;
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
        for (Edge e : g.iter_edges.values())
            e.flow /= 2;
        return new ArrayList<>(g.iter_edges.values());
    }

    private void buildLevelGraph(Node root, ArrayList<Integer> distance, ArrayList<ArrayList<Integer>> parents,
            int[] paths, Stack<Integer> order) {
        Queue<Node> q = new LinkedList<>();
        q.add(root);

        while (!q.isEmpty()) {
            Node src = q.poll();
            order.push(src.id);

            for (Edge edge : src) {
                Node child = edge.getDestination(src);
                int parentDistance = distance.get(src.id);

                if (distance.get(child.id) == -1) {
                    distance.set(child.id, parentDistance + 1);
                    q.add(child);
                }

                if (distance.get(child.id) == parentDistance + 1) {
                    paths[child.id] += paths[src.id];
                    parents.get(child.id).add(src.id);
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
                Edge e = g.get(pop, par);
                double flowCalculation = flow * ((double) paths[par] / paths[pop]);
                e.flow += flowCalculation;
                flows[par] += flowCalculation;
            }
        }
    }

    private void calculateFlow(int root) {
        //Initialize the first level with the root
        ArrayList<Integer> distances = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
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

        buildLevelGraph(g.nodes[root], distances, parents, paths, order);

        addFlows(distances, parents, paths, order);
    }

}