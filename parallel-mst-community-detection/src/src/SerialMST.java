package src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import src.ParallelMST.Edge;

public class SerialMST {

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

    HashSet<Integer>[] adj_matrix;
    ArrayList<Edge>[] mst_mat;
    int n;
    Edge[] edges; //Weight is 0 until solve is called
    HashMap<Edge, Edge> map;

    public SerialMST(int n, ArrayList<int[]> edges) {
        this.n = n;
        map = new HashMap<>();
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
        for (final Edge edge : edges) {
            edge.weight = nover(edge);
        }
    }

    boolean betterModularity(double mod1, double mod2) {
        return mod1 > mod2;
    }

    public double solve() throws InterruptedException {
        calculateNover();
        ArrayList<Edge> mst = MST();

        EdgeBetweeness eb = new EdgeBetweeness(adj_matrix, map, n);
        eb.calculateSerial();

        mst.sort((o1, o2) -> Double.compare(o1.flow, o2.flow));

        double modularity = -1;
        int[] bset = new int[n];
        int[] set = new int[n];
        for (int j = n - 2; j >= 0; j--) {

            for (int k = 0; k < n; k++)
                set[k] = k;

            for (int k = 0; k < j; k++) {
                Edge e = mst.get(k);
                union(e.a, e.b, set);
            }

            double newModularity = modularity2(set);
            if (betterModularity(newModularity, modularity)) {
                modularity = newModularity;
                bset = set;
            }
        }

        HashMap<Integer, ArrayList<Integer>> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            map.putIfAbsent(find(i, bset), new ArrayList<>());
            map.get(find(i, bset)).add(i);
        }

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

    public ArrayList<Edge> MST() throws InterruptedException {
        int[] set = new int[n];
        for (int i = 0; i < n; i++)
            set[i] = i;
        ArrayList<Edge> answer = new ArrayList<>();
        int v = n;
        while (v > 1) {
            Edge[] closest = new Edge[n];
            for (int node = 0; node < n; node++) {

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
        // System.out.println(total);

        return total / m2;
    }
}