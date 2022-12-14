package src;

import java.util.ArrayList;
import java.util.HashSet;

import src.Graph.Edge;
import src.Graph.Pair;

public class GirvanNewman {
    int n;
    Graph g;
    int m;
    final int nThreads = 8;
    int k;

    public GirvanNewman(HashSet<int[]> edges, int n, int k, boolean serial) {
        g = new Graph(edges, n, serial);
        this.n = n;
        this.m = edges.size();
        this.k = k;

    }

    double bestModularity = -1;

    public double solve() {
        double mod;
        while (bestModularity - 0.001 < (mod = g.calculateModularity())) {
            bestModularity = Math.max(mod, bestModularity);
            EdgeBetweeness eb = new EdgeBetweeness(g, n);
            ArrayList<Graph.Edge> ebs = eb.calculateParallel();
            ebs.sort((o1, o2) -> Double.compare(o2.flow, o1.flow));
            int i = 0;
            for (Edge best : ebs) {
                if (i++ == k)
                    break;
                g.remove(best.a.id, best.b.id);
            }
            g.checkCommunity();
        }
        return bestModularity;

    }

    public double solveSerial() {
        double mod;
        while (bestModularity - 0.0001 < (mod = g.calculateModularity())) {
            System.out.println(mod);
            bestModularity = Math.max(mod, bestModularity);
            ArrayList<Graph.Edge> ebs = g.ebList();
            ebs.sort((o1, o2) -> Double.compare(o2.flow, o1.flow));
            int i = 0;
            for (Edge best : ebs) {
                if (i++ == k)
                    break;
                System.out.println(best.a.id + " " + best.b.id);
                g.remove(best.a.id, best.b.id);
            }
            g.checkCommunity();
        }
        return bestModularity;
    }

    // public double calculateModularity() {
    //     DisjointSet set = new DisjointSet(n);
    //     for (Edge edge : g.iter_edges.values()) {
    //         set.union(edge.a.id, edge.b.id);
    //     }
    //     double sum = 0;
    //     double m2 = m * 2;
    //     for (int i = 0; i < n; i++) {
    //         for (int j = 0; j < n; j++) {
    //             if (set.find(i) != set.find(j))
    //                 continue;
    //             double add = g.contains(i, j) ? 1 : 0;
    //             add -= (g.nodes[i].degree * g.nodes[j].degree) / m2;
    //             sum += add;
    //         }
    //     }
    //     return sum / m2;
    // }

    // public double calculateParallelModularity() {
    //     DisjointSet set = new DisjointSet(n);
    //     for (Edge edge : g.iter_edges.values()) {
    //         set.union(edge.a.id, edge.b.id);
    //     }

    //     double m2 = m * 2;

    //     List<Double> sums = Collections.synchronizedList(new ArrayList<Double>());

    //     ExecutorService pool = Executors.newFixedThreadPool(nThreads);
    //     for (int fi = 0; fi < n; fi++) {
    //         int i = fi;
    //         pool.execute(() -> {
    //             double sum = 0;
    //             for (int j = 0; j < n; j++) {
    //                 if (set.find(i) != set.find(j)) {
    //                     continue;
    //                 }
    //                 double add = g.contains(i, j) ? 1 : 0;
    //                 add -= (g.nodes[i].degree * g.nodes[j].degree) / m2;
    //                 sum += add;
    //             }
    //             sums.add(sum);
    //         });
    //     }

    //     pool.shutdown();

    //     try {
    //         pool.awaitTermination(500, TimeUnit.SECONDS);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }
    //     double sum = 0;
    //     for (double s : sums)
    //         sum += s;

    //     return sum / m2;
    // }

}
