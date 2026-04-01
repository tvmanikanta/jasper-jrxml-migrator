package org.example.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.commons.io.FileUtils;
import org.example.migrator.JrxmlMigrator;
import org.example.model.MigrationReport;
import org.example.model.MigrationResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maven Mojo that migrates JasperReports .jrxml files from version 6.x to 7.x.
 *
 * Usage:
 * <pre>
 *   mvn jasper7:migrate-to-jasper7 \
 *     -DsourceFolder=/path/to/reports \
 *     -DtargetFolder=/path/to/migrated-reports
 * </pre>
 *
 * Or configured in pom.xml:
 * <pre>
 *   &lt;plugin&gt;
 *     &lt;groupId&gt;com.jasper.migration&lt;/groupId&gt;
 *     &lt;artifactId&gt;jasper7-migration-maven-plugin&lt;/artifactId&gt;
 *     &lt;version&gt;1.0.0&lt;/version&gt;
 *     &lt;executions&gt;
 *       &lt;execution&gt;
 *         &lt;phase&gt;migrate&lt;/phase&gt;
 *         &lt;goals&gt;
 *           &lt;goal&gt;migrate-to-jasper7&lt;/goal&gt;
 *         &lt;/goals&gt;
 *         &lt;configuration&gt;
 *           &lt;sourceFolder&gt;${project.basedir}/src/main/reports&lt;/sourceFolder&gt;
 *           &lt;targetFolder&gt;${project.build.directory}/migrated-reports&lt;/targetFolder&gt;
 *         &lt;/configuration&gt;
 *       &lt;/execution&gt;
 *     &lt;/executions&gt;
 *   &lt;/plugin&gt;
 * </pre>
 */
@Mojo(name = "migrate-to-jasper7", defaultPhase = LifecyclePhase.NONE, threadSafe = false)
public class MigrateToJasper7Mojo extends AbstractMojo {

    /**
     * The source folder containing .jrxml files to be migrated.
     * Subdirectories are scanned recursively.
     */
    @Parameter(property = "sourceFolder", required = true)
    private File sourceFolder;

    /**
     * The target folder where migrated .jrxml files will be written.
     * The original directory structure is preserved.
     */
    @Parameter(property = "targetFolder", required = true)
    private File targetFolder;

    /**
     * Whether to overwrite existing files in the target folder.
     * Default: true
     */
    @Parameter(property = "overwrite", defaultValue = "true")
    private boolean overwrite;

    /**
     * Whether to create a backup of original files alongside the migrated ones.
     * Backup files are written with a .jrxml.bak extension.
     * Default: false
     */
    @Parameter(property = "createBackup", defaultValue = "false")
    private boolean createBackup;

    /**
     * Whether to fail the build if any migration error occurs.
     * Default: true
     */
    @Parameter(property = "failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Whether to generate a migration report file in the target folder.
     * Default: true
     */
    @Parameter(property = "generateReport", defaultValue = "true")
    private boolean generateReport;

    /**
     * Whether to skip migration entirely.
     * Default: false
     */
    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    /**
     * Whether to copy non-.jrxml files from source to target preserving structure.
     * Default: true
     */
    @Parameter(property = "copyNonJrxml", defaultValue = "true")
    private boolean copyNonJrxml;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Jasper7 migration is skipped (skip=true).");
            return;
        }

        getLog().info("========================================================");
        getLog().info("  JasperReports 6 → 7 Migration Plugin");
        getLog().info("========================================================");

        validateParameters();
        ensureTargetDirectoryExists();

        List<File> jrxmlFiles = discoverJrxmlFiles();
        if (jrxmlFiles.isEmpty()) {
            getLog().warn("No .jrxml files found in source folder: " + sourceFolder.getAbsolutePath());
            return;
        }

        getLog().info("Found " + jrxmlFiles.size() + " .jrxml file(s) to migrate.");
        getLog().info("Source : " + sourceFolder.getAbsolutePath());
        getLog().info("Target : " + targetFolder.getAbsolutePath());
        getLog().info("--------------------------------------------------------");

        MigrationReport report = processMigrations(jrxmlFiles);
        logSummary(report);

        if (generateReport) {
            writeReportFile(report);
        }

        if (failOnError && report.hasErrors()) {
            throw new MojoFailureException(
                "Migration completed with " + report.getErrorCount() + " error(s). " +
                "Check the migration report for details. Set failOnError=false to ignore errors."
            );
        }
    }

    private void validateParameters() throws MojoExecutionException {
        if (!sourceFolder.exists()) {
            throw new MojoExecutionException(
                "Source folder does not exist: " + sourceFolder.getAbsolutePath());
        }
        if (!sourceFolder.isDirectory()) {
            throw new MojoExecutionException(
                "Source folder is not a directory: " + sourceFolder.getAbsolutePath());
        }
    }

    private void ensureTargetDirectoryExists() throws MojoExecutionException {
        if (!targetFolder.exists()) {
            if (!targetFolder.mkdirs()) {
                throw new MojoExecutionException(
                    "Cannot create target folder: " + targetFolder.getAbsolutePath());
            }
            getLog().info("Created target folder: " + targetFolder.getAbsolutePath());
        }
    }

    private List<File> discoverJrxmlFiles() throws MojoExecutionException {
        List<File> files = new ArrayList<>();
        try {
            Files.walkFileTree(sourceFolder.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    if (name.toLowerCase().endsWith(".jrxml")) {
                        files.add(file.toFile());
                    } else if (copyNonJrxml) {
                        copyNonJrxmlFile(file.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    getLog().warn("Cannot access file: " + file + " — " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to scan source folder", e);
        }
        return files;
    }

    private void copyNonJrxmlFile(File file) {
        try {
            Path relativePath = sourceFolder.toPath().relativize(file.toPath());
            File targetFile = targetFolder.toPath().resolve(relativePath).toFile();
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            if (overwrite || !targetFile.exists()) {
                FileUtils.copyFile(file, targetFile);
            }
        } catch (IOException e) {
            getLog().warn("Failed to copy non-jrxml file: " + file.getName() + " — " + e.getMessage());
        }
    }

    private MigrationReport processMigrations(List<File> jrxmlFiles) {
        MigrationReport report = new MigrationReport();
        report.setStartTime(LocalDateTime.now());
        report.setSourceFolder(sourceFolder.getAbsolutePath());
        report.setTargetFolder(targetFolder.getAbsolutePath());

        JrxmlMigrator migrator = new JrxmlMigrator(getLog());

        for (File sourceFile : jrxmlFiles) {
            Path relativePath = sourceFolder.toPath().relativize(sourceFile.toPath());
            File targetFile = targetFolder.toPath().resolve(relativePath).toFile();

            if (!overwrite && targetFile.exists()) {
                getLog().info("  SKIP  (already exists) : " + relativePath);
                MigrationResult skipped = MigrationResult.skipped(sourceFile, targetFile);
                report.addResult(skipped);
                continue;
            }

            getLog().info("  Migrating : " + relativePath);
            MigrationResult result = migrator.migrate(sourceFile, targetFile);
            report.addResult(result);

            if (result.isSuccess()) {
                int changeCount = result.getChanges().size();
                if (changeCount == 0) {
                    getLog().info("    → No changes needed (already compatible)");
                } else {
                    getLog().info("    → Applied " + changeCount + " transformation(s)");
                    result.getChanges().forEach(change ->
                        getLog().debug("       • " + change));
                }
                if (createBackup) {
                    createBackupFile(sourceFile);
                }
            } else {
                getLog().error("    → FAILED: " + result.getErrorMessage());
            }
        }

        report.setEndTime(LocalDateTime.now());
        return report;
    }

    private void createBackupFile(File sourceFile) {
        try {
            File backupFile = new File(sourceFile.getAbsolutePath() + ".bak");
            FileUtils.copyFile(sourceFile, backupFile);
        } catch (IOException e) {
            getLog().warn("Failed to create backup for: " + sourceFile.getName());
        }
    }

    private void logSummary(MigrationReport report) {
        getLog().info("--------------------------------------------------------");
        getLog().info("  Migration Summary");
        getLog().info("--------------------------------------------------------");
        getLog().info("  Total files   : " + report.getTotalCount());
        getLog().info("  Succeeded     : " + report.getSuccessCount());
        getLog().info("  Skipped       : " + report.getSkippedCount());
        getLog().info("  Errors        : " + report.getErrorCount());
        getLog().info("  Total changes : " + report.getTotalChanges());
        getLog().info("  Duration      : " + report.getDurationMs() + " ms");
        getLog().info("========================================================");
    }

    private void writeReportFile(MigrationReport report) {
        File reportFile = new File(targetFolder, "migration-report.txt");
        try {
            FileUtils.writeStringToFile(reportFile, report.toText(), "UTF-8");
            getLog().info("Migration report written to: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            getLog().warn("Could not write migration report: " + e.getMessage());
        }
    }
}
