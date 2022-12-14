
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

public class Main {
    private static final String JAR_PATH = "jar";
    private static final String GRAPH_PATH = "graph";
    private static final HashMap<String, String> jarName = new HashMap<>();

    private static String[] getJarPaths() {
        File jarFolder = new File(JAR_PATH);
        String[] jars = new String[jarFolder.listFiles().length];
        int i = 0;
        for (File jar : jarFolder.listFiles()) {
            jars[i++] = jar.getPath();
            jarName.put(jar.getPath(), jar.getName());
        }
        return jars;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String[] jarPaths = getJarPaths();
        File graphFolder = new File(GRAPH_PATH);
        File[] graphes = graphFolder.listFiles();
        for (File graph : graphes) {
            System.err.println("Graph " + graph.getName());
            String path = graph.getAbsolutePath();
            for (String jar : jarPaths) {
                System.err.println("Jar " + jar);
                Process proc = Runtime.getRuntime().exec("java -jar " + jar + " " + path);
                proc.waitFor();
                byte[] input = new byte[512];
                proc.getInputStream().read(input);
                String name = graph.getName();

                int nullEnding = 0;
                while (input[nullEnding] != 0)
                    nullEnding++;

                String runtime = new String(input, 0, nullEnding);

                System.out.println(
                        jarName.get(jar) + " " + name + " " + runtime.split("\n")[0] + " " + runtime.split("\n")[1]);
            }
        }
    }
}