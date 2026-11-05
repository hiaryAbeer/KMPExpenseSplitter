# R8 rules for the release build.
#
# Most libraries in use (Koin, kotlinx.serialization, DataStore, Compose,
# Navigation) ship their own consumer rules, so this file stays small.
# Add a rule here only with a comment explaining which crash it prevents.

# Keep line numbers for readable crash reports, but hide original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip verbose/debug android.util.Log calls from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
