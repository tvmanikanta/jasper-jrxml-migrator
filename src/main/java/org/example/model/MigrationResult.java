package org.example.model;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Holds the outcome of migrating a single .jrxml file.
 */
public class MigrationResult {

    public enum Status { SUCCESS, FAILURE, SKIPPED }

    private final File sourceFile;
    private final File targetFile;
    private final Status status;
    private final List<String> changes;
    private final String errorMessage;
    private final Throwable cause;

    private MigrationResult(File sourceFile, File targetFile, Status status,
                            List<String> changes, String errorMessage, Throwable cause) {
        this.sourceFile   = sourceFile;
        this.targetFile   = targetFile;
        this.status       = status;
        this.changes      = changes != null ? Collections.unmodifiableList(changes) : Collections.emptyList();
        this.errorMessage = errorMessage;
        this.cause        = cause;
    }

    public static MigrationResult success(File source, File target, List<String> changes) {
        return new MigrationResult(source, target, Status.SUCCESS, changes, null, null);
    }

    public static MigrationResult failure(File source, File target, String errorMessage, Throwable cause) {
        return new MigrationResult(source, target, Status.FAILURE,
                Collections.emptyList(), errorMessage, cause);
    }

    public static MigrationResult skipped(File source, File target) {
        return new MigrationResult(source, target, Status.SKIPPED,
                Collections.emptyList(), null, null);
    }

    public boolean isSuccess()  { return status == Status.SUCCESS; }
    public boolean isFailure()  { return status == Status.FAILURE; }
    public boolean isSkipped()  { return status == Status.SKIPPED; }

    public File         getSourceFile()    { return sourceFile; }
    public File         getTargetFile()    { return targetFile; }
    public Status       getStatus()        { return status; }
    public List<String> getChanges()       { return changes; }
    public String       getErrorMessage()  { return errorMessage; }
    public Throwable    getCause()         { return cause; }

    @Override
    public String toString() {
        return String.format("MigrationResult[%s, file=%s, changes=%d]",
                status, sourceFile.getName(), changes.size());
    }
}
