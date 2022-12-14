package src;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Graph {
    Node[] nodes;
    HashMap<Pair, Edge> edges;
    HashMap<Pair, Edge> iter_edges;
    ArrayList<Community> communities;
    HashSet<Integer> checkCommunity;
    int[] communityPointer;
    int n;
    int m;
    boolean serial;

    Graph(Collection<int[]> edges, int n, boolean serial) {
        this.serial = serial;
        this.n = n;
        this.m = edges.size();
        nodes = new Node[n];
        this.edges = new HashMap<>();
        iter_edges = new HashMap<>();
        for (int i = 0; i < n; i++)
            nodes[i] = new Node(i);
        for (int[] e : edges) {
            Node a = nodes[e[0]];
            Node b = nodes[e[1]];
            a.degree++;
            b.degree++;
            Edge edge = new Edge(a, b);
            this.edges.put(new Pair(e[0], e[1]), edge);
            iter_edges.put(new Pair(e[0], e[1]), edge);

            a.add(edge);
            b.add(edge);
        }
        communities = new ArrayList<>();
        Community community = new Community(0);
        communities.add(community);
        checkCommunity = new HashSet<>();
        communityPointer = new int[n];
        for (Node node : nodes)
            community.add(node);
    }

    public Edge get(int a, int b) {
        return edges.get(new Pair(a, b));
    }

    public void remove(int a, int b) {
        Edge r = iter_edges.remove(new Pair(a, b));
        nodes[a].remove(r);
        nodes[b].remove(r);
        communities.get(communityPointer[a]).remove(a, b);
        checkCommunity.add(communityPointer[a]);
    }

    public void checkCommunity() {
        for (int community : checkCommunity) {
            communities.get(community).checkCommunity();
        }
        checkCommunity.clear();
    }

    public double calculateModularity() {
        double mod = 0;
        for (Community com : communities) {
            mod += com.calculateModularity();
        }
        return mod / (edges.size() * 2);
    }

    public ArrayList<Edge> ebList() {
        ArrayList<Edge> list = new ArrayList<>();
        for (Community com : communities) {
            if (serial)
                list.addAll(com.calculateSerial());
            else
                list.addAll(com.calculateParallel());
        }
        return list;
    }

    public boolean contains(int a, int b) {
        return edges.containsKey(new Pair(a, b));
    }

    public class Pair {
        int a;
        int b;

        Pair(int a, int b) {
            this.a = Math.min(a, b);
            this.b = Math.max(a, b);
        }

        @Override
        public boolean equals(Object o) {
            Pair po = (Pair) o;
            return a == po.a && b == po.b;
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }

    }

    public class Edge {
        volatile double flow;
        Node a, b;

        Edge(Node a, Node b) {
            this.a = a;
            this.b = b;
        }

        public Node getDestination(Node src) {
            return src.id == a.id ? b : a;
        }

        @Override
        public String toString() {
            return a.toString() + " " + b.toString() + ": " + flow;
        }

    }

    public class Node extends ArrayList<Edge> {
        int id;
        int degree;

        Node(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return Integer.toString(id);
        }
    }

    class Community {
        ArrayList<Node> communityNodes;
        HashMap<Pair, Edge> edges;
        double modularity;
        boolean calculated;
        boolean ebCalculated;
        int id;

        Community(int id) {
            communityNodes = new ArrayList<>();
            edges = new HashMap<>();
            calculated = false;
            this.id = id;
        }

        Community(ArrayList<Node> nodes, int id) {
            communityNodes = new ArrayList<>();
            edges = new HashMap<>();
            for (Node node : nodes)
                add(node);
            calculated = false;
            this.id = id;
        }

        void add(Node n) {
            communityPointer[n.id] = id;
            communityNodes.add(n);
            for (Edge e : n) {
                edges.put(new Pair(e.a.id, e.b.id), e);
            }

        }

        void remove(int a, int b) {
            edges.remove(new Pair(a, b));
            ebCalculated = false;
        }

        void checkCommunity() {
            ArrayList<ArrayList<Node>> newCommunities = new ArrayList<>();
            boolean[] visited = new boolean[n];
            int pointer = id;
            for (Node n : communityNodes) {
                if (visited[n.id])
                    continue;
                ArrayList<Node> newCom = new ArrayList<>();
                dfs(n, newCom, visited, pointer);
                if (pointer == id)
                    pointer = communities.size() - 1;
                pointer++;
                newCommunities.add(newCom);
            }
            if (newCommunities.size() == 1)
                return;
            communityNodes = newCommunities.get(0);
            calculated = false;
            for (int i = 1; i < newCommunities.size(); i++) {
                communities.add(new Community(newCommunities.get(i), communities.size()));
            }
        }

        void dfs(Node n, ArrayList<Node> nodes, boolean[] visited, int pointer) {
            Stack<Node> stack = new Stack<>();
            stack.add(n);
            visited[n.id] = true;
            while (!stack.isEmpty()) {
                n = stack.pop();
                communityPointer[n.id] = pointer;
                nodes.add(n);
                for (Edge e : n) {
                    if (!visited[e.getDestination(n).id]) {
                        visited[e.getDestination(n).id] = true;
                        stack.add(e.getDestination(n));
                    }
                }
            }
        }

        double calculateModularity() {
            if (calculated)
                return modularity;
            calculated = true;
            if (serial || 3000 > communityNodes.size())
                return calculateSerialModularity();
            return calculateParallelModularity();
        }

        double calculateSerialModularity() {

            modularity = 0;
            double m2 = m * 2;
            for (Node a : communityNodes) {
                for (Node b : communityNodes) {
                    double add = contains(a.id, b.id) ? 1 : 0;
                    add -= (nodes[a.id].degree * nodes[b.id].degree) / m2;
                    modularity += add;
                }
            }
            calculated = true;
            return modularity;
        }

        public double calculateParallelModularity() {
            double m2 = m * 2;

            List<Double> sums = Collections.synchronizedList(new ArrayList<Double>());

            ExecutorService pool = Executors.newFixedThreadPool(8);
            for (int fi = 0; fi < communityNodes.size(); fi++) {
                int i = communityNodes.get(fi).id;
                pool.execute(() -> {
                    double sum = 0;
                    for (int nj = 0; nj < communityNodes.size(); nj++) {
                        int j = communityNodes.get(nj).id;
                        double add = contains(i, j) ? 1 : 0;
                        add -= (nodes[i].degree * nodes[j].degree) / m2;
                        sum += add;
                    }
                    sums.add(sum);
                });
            }

            pool.shutdown();

            try {
                pool.awaitTermination(500, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            modularity = 0;
            for (double s : sums)
                modularity += s;

            return modularity;
        }

        public Collection<Edge> calculateSerial() {
            if (ebCalculated)
                return edges.values();
            for (Edge e : edges.values())
                e.flow = 0;
            for (int i = 0; i < n; i++) {
                calculateFlow(i);
            }
            for (Edge e : edges.values())
                e.flow /= 2;
            ebCalculated = true;
            return edges.values();

        }

        public Collection<Edge> calculateParallel() {
            if (ebCalculated)
                return edges.values();
            ExecutorService pool = Executors.newFixedThreadPool(8);
            for (Edge e : edges.values())
                e.flow = 0;
            for (Node node : communityNodes) {
                int finalI = node.id;
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
            for (Edge e : edges.values())
                e.flow /= 2;
            pool.shutdownNow();
            ebCalculated = true;
            return edges.values();
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
                    Edge e = get(pop, par);
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

            buildLevelGraph(nodes[root], distances, parents, paths, order);

            addFlows(distances, parents, paths, order);
        }
    }
}
