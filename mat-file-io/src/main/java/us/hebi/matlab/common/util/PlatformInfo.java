package us.hebi.matlab.common.util;

import java.util.Locale;

/**
 * Utility methods that provide information about the underlying platform.
 *
 * @author Florian Enner
 * @since 19 Oct 2018
 */
public final class PlatformInfo {

    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    public static boolean isUnix() {
        return IS_UNIX;
    }

    public static boolean isMac() {
        return IS_MAC;
    }

    public static boolean isAndroid() {
        return IS_ANDROID;
    }

    /**
     * @return {@code true} if the current execution is within MATLAB
     */
    public static boolean isMatlab() {
        return IS_MATLAB;
    }

    public static boolean hasSecurityManager() {
        return System.getSecurityManager() != null;
    }

    /**
     * @return 6, 7, 8, 9, 10, 11, ...
     */
    public static int getJavaVersion() {
        return JAVA_VERSION;
    }

    // Underlying OS (http://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/)
    private static final String OS = System.getProperty("os.name").toLowerCase(Locale.US);
    private static final boolean IS_WINDOWS = OS.contains("win");
    private static final boolean IS_MAC = OS.contains("mac");
    private static final boolean IS_UNIX = OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0;
    private static final boolean IS_SOLARIS = OS.contains("sunos");

    private static final boolean IS_ANDROID = "Dalvik".equals(System.getProperty("java.vm.name"));
    private static final boolean IS_MATLAB = isMatlab0();
    private static final int JAVA_VERSION = getMajorJavaVersion0();

    private static boolean isMatlab0() {
        try {
            // Check whether the JMI (Java Matlab Interface - jmi.jar) is on the class path.
            Class.forName("com.mathworks.jmi.Matlab", true, PlatformInfo.class.getClassLoader());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static int getMajorJavaVersion0() {
        // Default Android to Java6
        if (isAndroid()) return 6;

        // 1.6, 1.7, 1.8, 9, 10, ...
        String version = System.getProperty("java.specification.version", "6");
        String majorVersion = version.startsWith("1.") ? version.substring(2) : version;
        return Integer.parseInt(majorVersion);
    }

    private PlatformInfo() {
    }

}
