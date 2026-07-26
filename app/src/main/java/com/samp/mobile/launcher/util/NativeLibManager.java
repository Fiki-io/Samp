package com.samp.mobile.launcher.util;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.joom.paranoid.Obfuscate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Obfuscate
public class NativeLibManager {

    private static final String TAG = "NativeLibManager";
    
    private static final String BASE_URL_ARM64 = "https://github.com/garden-byte/samp/releases/download/JniLibs/";
    private static final String BASE_URL_ARMV7 = "https://github.com/garden-byte/samp/releases/download/JniLibsv7/";

    private static final String[] LIBS_ARM64 = {
        "libGTASA.so",
        "libOpenAL64.so",
        "libSCAnd.so",
        "libluajit-5.1.so",
        "libmonetloader.so"
    };

    private static final String[] LIBS_ARMV7 = {
        "libGTASA.so",
        "libImmEmulatorJ.so",
        "libOpenAL32.so",
        "libSCAnd.so",
        "libluajit-5.1.so",
        "libmonetloader.so"
    };

    public static class DownloadItem {
        public final String fileName;
        public final String downloadUrl;

        public DownloadItem(String fileName, String downloadUrl) {
            this.fileName = fileName;
            this.downloadUrl = downloadUrl;
        }
    }

    public static File getLibDir(Context context) {
        return context.getDir("libs", Context.MODE_PRIVATE);
    }

    public static boolean isCpu64Bit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Process.is64Bit();
        }
        for (String abi : Build.SUPPORTED_ABIS) {
            if (abi.contains("arm64") || abi.contains("x86_64")) {
                return true;
            }
        }
        return false;
    }

    public static String[] getRequiredLibraries() {
        return isCpu64Bit() ? LIBS_ARM64 : LIBS_ARMV7;
    }

    public static String getBaseDownloadUrl() {
        return isCpu64Bit() ? BASE_URL_ARM64 : BASE_URL_ARMV7;
    }

    public static List<DownloadItem> getMissingLibraries(Context context) {
        List<DownloadItem> missing = new ArrayList<>();
        File libDir = getLibDir(context);
        String baseUrl = getBaseDownloadUrl();
        String[] requiredLibs = getRequiredLibraries();

        for (String libName : requiredLibs) {
            File file = new File(libDir, libName);
            if (!file.exists() || file.length() == 0) {
                missing.add(new DownloadItem(libName, baseUrl + libName));
            }
        }
        return missing;
    }

    public static boolean areAllLibrariesPresent(Context context) {
        File libDir = getLibDir(context);
        String[] requiredLibs = getRequiredLibraries();

        for (String libName : requiredLibs) {
            if (libName.equals("libmonetloader.so") || libName.equals("libImmEmulatorJ.so")) {
                continue;
            }
            File file = new File(libDir, libName);
            if (!file.exists() || file.length() == 0) {
                return false;
            }
        }
        return true;
    }

    public static void loadLibraries(Context context) {
        File libDir = getLibDir(context);
        boolean is64 = isCpu64Bit();

        if (is64) {
            loadIfExist(libDir, "libOpenAL64.so");
        } else {
            loadIfExist(libDir, "libImmEmulatorJ.so");
            loadIfExist(libDir, "libOpenAL32.so");
        }

        loadIfExist(libDir, "libSCAnd.so");
        loadIfExist(libDir, "libluajit-5.1.so");
        loadIfExist(libDir, "libGTASA.so");

        if (new SharedPreferenceCore().getBoolean(context, "MLOADER")) {
            loadIfExist(libDir, "libmonetloader.so");
        }

        try {
            System.loadLibrary("samp");
        } catch (UnsatisfiedLinkError e) {
            File sampFile = new File(libDir, "libsamp.so");
            if (sampFile.exists()) {
                System.load(sampFile.getAbsolutePath());
            } else {
                Log.e(TAG, "Failed to load libsamp: " + e.getMessage());
            }
        }
    }

    private static void loadIfExist(File dir, String libName) {
        File file = new File(dir, libName);
        if (file.exists() && file.length() > 0) {
            try {
                System.load(file.getAbsolutePath());
                Log.i(TAG, "Successfully loaded native library from private dir: " + libName);
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "Error loading " + libName + " from private dir: " + e.getMessage());
            }
        } else {
            try {
                String libShortName = libName.replace("lib", "").replace(".so", "");
                System.loadLibrary(libShortName);
            } catch (UnsatisfiedLinkError ignored) {
            }
        }
    }
}
