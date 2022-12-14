package src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ParallelMST {

    static class Graph {
        HashSet<Integer>[] adj_matrix;
        Edge[] edges;
        int n;

        Graph(HashSet<Integer>[] mat, Edge[] e, int nodes) {
            n = nodes;
            e = edges;
            adj_matrix = mat;
        }
    }

    static class Edge {
        int a;
        int b;
        double weight;
        double flow;

        synchronized public void addFlow(double value) {
            flow += value;
        }

        Edge(int a, int b) {
            this.a = Math.min(a, b);
            this.b = Math.max(a, b);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + a;
            result = prime * result + b;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Edge other = (Edge) obj;
            if (a != other.a)
                return false;
            if (b != other.b)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Edge [flow=" + flow + "]";
        }
    }

    final int nThreads;
    HashSet<Integer>[] adj_matrix;
    ArrayList<Edge>[] mst_mat;
    int n;
    Edge[] edges; //Weight is 0 until solve is called
    HashMap<Edge, Edge> map;

    public ParallelMST(int n, ArrayList<int[]> edges, int nThreads) {
        map = new HashMap<>();
        this.nThreads = nThreads;
        this.n = n;
        adj_matrix = new HashSet[n];
        mst_mat = new ArrayList[n];
        this.edges = new Edge[edges.size()];

        for (int i = 0; i < n; i++) {
            adj_matrix[i] = new HashSet<>();
            mst_mat[i] = new ArrayList<>();
        }

        int i = 0;
        for (int[] edge : edges) {
            int a = edge[0];
            int b = edge[1];
            this.edges[i] = new Edge(a, b);
            adj_matrix[a].add(b);
            adj_matrix[b].add(a);
            mst_mat[a].add(this.edges[i]);
            mst_mat[b].add(this.edges[i]);
            map.put(this.edges[i], this.edges[i]);
            i++;
        }
    }

    void calculateNover() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        for (final Edge edge : edges) {
            executor.execute(() -> {
                edge.weight = nover(edge);
            });
        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.DAYS);

    }

    boolean betterModularity(double mod1, double mod2) {
        if (mod1 > 0.7)
            return false;
        return mod1 > mod2;
    }

    double modularity;

    public double solve() throws InterruptedException {
        calculateNover();

        ArrayList<Edge> mst = parallelMST();
        EdgeBetweeness eb = new EdgeBetweeness(adj_matrix, map, n);
        eb.calculateParallel();

        mst.sort((o1, o2) -> Double.compare(o1.flow, o2.flow));
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        modularity = -1;

        for (int j = 0; j < n - 1; j++) {
            int Process = j;
            executor.execute(() -> {
                int[] set = new int[n];
                for (int k = 0; k < n; k++)
                    set[k] = k;

                for (int k = 0; k < Process; k++) {
                    Edge e = mst.get(k);
                    union(e.a, e.b, set);
                }
                double newModularity = modularity2(set);
                synchronized (this) {
                    if (betterModularity(newModularity, modularity)) {
                        modularity = newModularity;
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.DAYS);

        return modularity;

    }

    int find(int x, int[] set) {
        if (set[x] == x)
            return x;
        set[x] = find(set[x], set);
        return set[x];
    }

    void union(int a, int b, int[] set) {
        a = find(a, set);
        b = find(b, set);
        set[a] = b;
    }

    public ArrayList<Edge> parallelMST() throws InterruptedException {
        int[] set = new int[n];
        for (int i = 0; i < n; i++)
            set[i] = i;
        ArrayList<Edge> answer = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        int v = n - 1;

        while (v > 1) {
            int excess = n % nThreads;
            int offSet = 0;
            int workAmount = n / nThreads;
            Edge[] closest = new Edge[n];
            Semaphore latch = new Semaphore(-nThreads + 1);
            HashSet<Integer> nodes = new HashSet<>();
            for (int i = 0; i < n; i++)
                nodes.add(i);
            for (int i = 0; i < nThreads; i++) {
                final int start = i * workAmount + offSet;
                final int end = (excess > 0 ? 1 : 0) + start + workAmount;
                if (excess > 0) {
                    excess--;
                    offSet++;
                }
                executor.execute(() -> {
                    for (int node = start; node < end; node++) {

                        Edge best = null;
                        double weight = Double.MAX_VALUE;
                        for (Edge e : mst_mat[node]) {
                            if (find(e.a, set) == find(e.b, set))
                                continue;
                            if (e.weight < weight) {
                                weight = e.weight;
                                best = e;
                            }
                        }
                        closest[node] = best;
                    }
                    latch.release();

                });

            }

            latch.acquire();
            Arrays.sort(edges, (o1, o2) -> Double.compare(o1.weight, o2.weight));

            for (int i = 0; i < n; i++) {
                Edge e = closest[i];
                if (e == null || find(e.a, set) == find(e.b, set))
                    continue;
                union(e.a, e.b, set);
                v--;
                answer.add(e);
            }

        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.DAYS);
        return answer;
    }

    public double nover(Edge edge) {
        int intersection = calculateIntersection(adj_matrix[edge.a], adj_matrix[edge.b]);
        int union = adj_matrix[edge.a].size() + adj_matrix[edge.b].size() - intersection;
        return intersection / (double) union;
    }

    public int calculateIntersection(HashSet<Integer> a, HashSet<Integer> b) {
        int count = 0;
        if (a.size() > b.size()) {
            HashSet<Integer> tmp = a;
            a = b;
            b = tmp;
        }
        for (int num : a) {
            if (b.contains(num))
                count++;
        }
        return count;
    }

    public double modularity(int[] set) {

        double m2 = edges.length * 2;

        double total = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (find(i, set) != find(j, set))
                    continue;
                double sum = adj_matrix[i].contains(j) ? 1 : 0;
                sum -= (adj_matrix[i].size() * adj_matrix[j].size()) / m2;
                total += sum;
            }
        }

        return total / m2;
    }

    public double modularity2(int[] set) {
        ArrayList<ArrayList<Integer>> communties = new ArrayList<>();
        HashMap<Integer, Integer> values = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (!values.containsKey(find(set[i], set))) {
                values.put(find(set[i], set), communties.size());
                communties.add(new ArrayList<>());
            }
            communties.get(values.get(find(set[i], set))).add(i);
        }

        double m2 = edges.length * 2;
        double total = 0;

        for (ArrayList<Integer> community : communties) {
            for (int i : community) {
                for (int j : community) {
                    double sum = adj_matrix[i].contains(j) ? 1 : 0;
                    sum -= (adj_matrix[i].size() * adj_matrix[j].size()) / m2;
                    total += sum;
                }
            }
        }
        return total / m2;
    }
}