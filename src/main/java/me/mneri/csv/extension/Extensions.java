package me.mneri.csv.extension;

public class Extensions {
    public static final boolean SIMD_SUPPORTED = isSimdSupported();

    private static boolean isSimdSupported() {
        try {
            Class.forName("jdk.incubator.vector.Vector"); // The SIMD extension is currently in the incubator
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
        }
        try {
            Class.forName("java.lang.vector.Vector"); // The package name is speculation
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
        }
        return false;
    }
}
