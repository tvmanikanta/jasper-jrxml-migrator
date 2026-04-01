package org.example.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates all MigrationResult objects for a migration run and
 * provides summary statistics and a text report.
 */
public class MigrationReport {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String        sourceFolder;
    private String        targetFolder;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private final List<MigrationResult> results = new ArrayList<>();

    public void addResult(MigrationResult result) {
        results.add(result);
    }

    public boolean hasErrors() {
        return results.stream().anyMatch(MigrationResult::isFailure);
    }

    public int getTotalCount()   { return results.size(); }
    public int getSuccessCount() { return (int) results.stream().filter(MigrationResult::isSuccess).count(); }
    public int getErrorCount()   { return (int) results.stream().filter(MigrationResult::isFailure).count(); }
    public int getSkippedCount() { return (int) results.stream().filter(MigrationResult::isSkipped).count(); }

    public int getTotalChanges() {
        return results.stream().mapToInt(r -> r.getChanges().size()).sum();
    }

    public long getDurationMs() {
        if (startTime == null || endTime == null) return 0;
        return ChronoUnit.MILLIS.between(startTime, endTime);
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================\n");
        sb.append("  JasperReports 6 → 7 Migration Report\n");
        sb.append("========================================================\n");
        sb.append("  Date          : ").append(startTime != null ? startTime.format(FMT) : "N/A").append("\n");
        sb.append("  Source Folder : ").append(sourceFolder).append("\n");
        sb.append("  Target Folder : ").append(targetFolder).append("\n");
        sb.append("  Duration      : ").append(getDurationMs()).append(" ms\n");
        sb.append("--------------------------------------------------------\n");
        sb.append("  Total files   : ").append(getTotalCount()).append("\n");
        sb.append("  Succeeded     : ").append(getSuccessCount()).append("\n");
        sb.append("  Skipped       : ").append(getSkippedCount()).append("\n");
        sb.append("  Errors        : ").append(getErrorCount()).append("\n");
        sb.append("  Total changes : ").append(getTotalChanges()).append("\n");
        sb.append("========================================================\n\n");

        // Detail section
        for (MigrationResult r : results) {
            sb.append("FILE: ").append(r.getSourceFile().getAbsolutePath()).append("\n");
            sb.append("  Status : ").append(r.getStatus()).append("\n");

            if (r.isFailure()) {
                sb.append("  Error  : ").append(r.getErrorMessage()).append("\n");
            } else if (r.isSkipped()) {
                sb.append("  Reason : already exists in target (overwrite=false)\n");
            } else {
                if (r.getChanges().isEmpty()) {
                    sb.append("  Changes: none (already compatible with JR7)\n");
                } else {
                    sb.append("  Changes (").append(r.getChanges().size()).append("):\n");
                    for (String change : r.getChanges()) {
                        sb.append("    • ").append(change).append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        if (hasErrors()) {
            sb.append("========================================================\n");
            sb.append("  ERRORS SUMMARY\n");
            sb.append("========================================================\n");
            for (MigrationResult r : results) {
                if (r.isFailure()) {
                    sb.append("  ✗ ").append(r.getSourceFile().getName())
                            .append(" — ").append(r.getErrorMessage()).append("\n");
                }
            }
        }

        return sb.toString();
    }

    // Getters & Setters
    public String        getSourceFolder() { return sourceFolder; }
    public void          setSourceFolder(String sourceFolder) { this.sourceFolder = sourceFolder; }
    public String        getTargetFolder() { return targetFolder; }
    public void          setTargetFolder(String targetFolder) { this.targetFolder = targetFolder; }
    public LocalDateTime getStartTime()    { return startTime; }
    public void          setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime()      { return endTime; }
    public void          setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public List<MigrationResult> getResults() { return results; }
}
