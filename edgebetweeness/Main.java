
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import src.GirvanNewman;

public class Main {

    static int n = 0;

    static int add(int num, HashMap<Integer, Integer> m) {
        if (!m.containsKey(num))
            m.put(num, n++);
        return m.get(num);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            return;
        }

        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        HashSet<int[]> edges = new HashSet<>();
        String line;
        HashMap<Integer, Integer> m = new HashMap<>();
        while ((line = br.readLine()) != null) {
            String[] split = line.split(" ");
            String a = split[0];
            String b = split[1];
            int ia = Integer.parseInt(a);
            int ib = Integer.parseInt(b);

            edges.add(new int[] { add(ia, m), add(ib, m) });
        }
        br.close();
        long start = System.nanoTime();
        GirvanNewman gn = new GirvanNewman(edges, n, 10, true);
        double mod = gn.solveSerial();
        long end = System.nanoTime();
        System.out.println(end - start);
        System.out.println(mod);

    }
}
