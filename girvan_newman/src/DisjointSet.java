package src;

public class DisjointSet {
    int[] set;
    public DisjointSet(int n) {
        set = new int[n];
        for(int i = 0; i < n; i++) set[i] = i;
    }

    public int find(int x) {
        if (set[x] == x) return x;
        set[x] = find(set[x]);
        return set[x];
    }

    public void union(int a, int b) {
        a  = find(a);
        b = find(b);
        set [a] = b;
    }
}
