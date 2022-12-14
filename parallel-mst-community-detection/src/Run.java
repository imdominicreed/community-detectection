
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import src.ParallelMST;
import src.SerialMST;

public class Run {
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {

        Scanner scanner = new Scanner(new File(args[0]));
        ArrayList<int[]> edges = new ArrayList<>();
        int n = 0;
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            String[] split = line.split(" ");
            int a = Integer.parseInt(split[0]);
            int b = Integer.parseInt(split[1]);
            edges.add(new int[] { a, b });
            n = Math.max(n, Math.max(a, b));
        }
        n++;
        scanner.close();
        long start = System.nanoTime();

        ParallelMST mst = new ParallelMST(n, edges, 8);
        double mod = mst.solve();

        long end = System.nanoTime();
        System.out.println(end - start);
        System.out.println(mod);

    }
}
