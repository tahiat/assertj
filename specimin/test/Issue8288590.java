import static java.nio.file.StandardOpenOption.*;
import static java.util.stream.Collectors.joining;

import java.io.*;
import java.nio.file.Files;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class JavacBug {
    public static void main(String[] args) throws IOException, InterruptedException {
        String code = null;

        for (int i = 1; i < 5; i++) {
            code = """
            import static java.util.Arrays.asList;

            import java.util.List;

            public class Test {

            {interfaces}

            {records}

            {usage}

            }
            """
                .replace("{interfaces}", IntStream
                    .rangeClosed(1, i)
                    .mapToObj(j -> "interface I" + j + "<" + typevars(j) + ">" + " {}")
                    .collect(joining("\n")))
                .replace("{records}", IntStream
                    .rangeClosed(1, i)
                    .mapToObj(j -> "record R" + j + "<" + typevars(j) + ">(" + typevars(j, k -> " T" + k, k -> "List<T" + k + ">") + ")" + " implements I" + j + "<" + typevars(j) + "> {}")
                    .collect(joining("\n")))
                .replace("{usage}", IntStream
                    .rangeClosed(1, i)
                    .mapToObj(j ->
                          "<" + typevars(j) + "> I" + j + "<" + typevars(j) + "> m" + j + "(" + typevars(j, k -> " T" + k) + ") {\n"
                        + " return new R" + j + "<" + ">(" + typevars(j, k -> "", k -> "asList(T" + k + ")") + ");\n"
                        + "}")
                    .collect(joining("\n"))
                    );

            Files.writeString(new File("Test.java").toPath(), code, CREATE, WRITE, TRUNCATE_EXISTING);
            // Process p = Runtime.getRuntime().exec("javac -J-XX:+UnlockExperimentalVMOptions -J-XX:hashCode=2 Test.java");
            Process p = Runtime.getRuntime().exec("javac Test.java");
            p.waitFor();
            if (p.exitValue() != 0) {
                BufferedReader e = p.errorReader();

                System.err.println("Error:");
                String s;
                while ((s = e.readLine()) != null)
                    System.err.println(s);

                System.err.println(code);

                return;
            }
        }

        System.out.println("No error found. Last code example tried:");
        System.out.println(code);
    }

    private static String typevars(int j) {
        return typevars(j, k -> "");
    }

    private static String typevars(int j, IntFunction<String> suffix) {
        return typevars(j, suffix, k -> "T" + k);
    }

    private static String typevars(int j, IntFunction<String> suffix, IntFunction<String> type) {
        return IntStream.rangeClosed(1, j).mapToObj(k -> type.apply(k) + suffix.apply(k)).collect(joining(", "));
    }
}