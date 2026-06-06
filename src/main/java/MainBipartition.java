import cumulative.Task;
import propagators.BipartitionEnergeticChecker;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public class MainBipartition {
    
    private static final String DATA_PATH = "data/original";
    
    // Structure pour stocker les statistiques par groupe (n, C)
    private static class GroupStats {
        int totalInstances = 0;
        int infeasibleCount = 0;
        double totalTime = 0;
        List<Double> times = new ArrayList<>();
    }
    
    public static void main(String[] args) throws Exception {
        Path dataDir = Paths.get(DATA_PATH);
        
        if (!Files.exists(dataDir)) {
            System.out.println("Data directory not found: " + dataDir.toAbsolutePath());
            System.out.println("Please create the directory and add .dat files");
            return;
        }
        
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "*.dat")) {
            for (Path file : stream) {
                files.add(file);
            }
        }
        
        System.out.println("=== BIPARTITION ENERGETIC CHECKER BENCHMARK ===");
        System.out.println("Data path: " + DATA_PATH);
        System.out.println("Format: est p q h (avec C_max sur ligne 1)");
        System.out.println("Found " + files.size() + " instance files\n");
        
        // Grouper par (n, C)
        Map<String, GroupStats> groups = new TreeMap<>();
        int parseErrors = 0;
        int totalInstances = 0;
        int infeasibleCount = 0;
        double totalTime = 0;
        
        for (Path file : files) {
            try {
                InstanceData instance = loadInstance(file);
                if (instance == null) {
                    parseErrors++;
                    continue;
                }
                
                String groupKey = instance.tasks.length + "," + instance.capacity;
                GroupStats stats = groups.computeIfAbsent(groupKey, k -> new GroupStats());
                
                BipartitionEnergeticChecker checker = new BipartitionEnergeticChecker(instance.tasks, instance.capacity);
                
                long start = System.nanoTime();
                boolean feasible = checker.overloadCheck();
                long elapsed = System.nanoTime() - start;
                
                double elapsedMs = elapsed / 1_000_000.0;
                stats.totalTime += elapsedMs;
                stats.times.add(elapsedMs);
                stats.totalInstances++;
                totalInstances++;
                totalTime += elapsedMs;
                
                if (!feasible) {
                    stats.infeasibleCount++;
                    infeasibleCount++;
                }
                
                // Afficher progression
                if (stats.totalInstances % 50 == 0) {
                    System.out.print(".");
                }
                
            } catch (Exception e) {
                parseErrors++;
                System.err.println("Error loading " + file.getFileName() + ": " + e.getMessage());
            }
        }
        
        System.out.println("\n\n=== Consolidated CuSP Benchmark Results ===");
        System.out.println("Repetitions per instance: 1\n");
        
        // Afficher l'en-tête du tableau
        System.out.printf("%-15s | %-20s | %-15s | %-15s%n", 
            "Problem", "Avg CPU (ms)", "Fastest %", "False %");
        System.out.println("-----------------+----------------------+-----------------+-----------------");
        
        // Afficher les résultats par groupe (n, C)
        for (Map.Entry<String, GroupStats> entry : groups.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int n = Integer.parseInt(parts[0]);
            int capacity = Integer.parseInt(parts[1]);
            GroupStats stats = entry.getValue();
            
            double avgTime = stats.totalTime / stats.totalInstances;
            double falsePct = 100.0 * stats.infeasibleCount / stats.totalInstances;
            // Pour la bipartition, on calcule le fastest % (c'est le seul checker ici)
            double fastestPct = stats.totalInstances > 0 ? 100.0 : 0.0;
            
            System.out.printf("n=%-4d,c=%-4d | %-20s | %-15s | %-15s%n",
                n, capacity,
                String.format("%.3f", avgTime).replace('.', ','),
                String.format("%.1f%%", fastestPct).replace('.', ','),
                String.format("%.1f%%", falsePct).replace('.', ','));
        }
        
        // ================================================================
        // FORMAT DEMANDÉ : Summary Statistics
        // ================================================================
        System.out.println("\n=== Summary Statistics ===");
        System.out.printf("%-20s | %15s | %11s | %11s%n", 
            "Checker", "Avg CPU (ms)", "Fastest %", "False %");
        System.out.println("------------------------------------------------------------");
        
        double totalAvgTime = totalTime / totalInstances;
        double totalFalsePct = 100.0 * infeasibleCount / totalInstances;
        
        // Calcul du fastest % global (moyenne des fastest % par groupe)
        double totalFastestPct = 0;
        int groupCount = 0;
        for (GroupStats stats : groups.values()) {
            totalFastestPct += 100.0;
            groupCount++;
        }
        double avgFastestPct = groupCount > 0 ? totalFastestPct / groupCount : 0;
        
        System.out.printf("%-20s | %15.6f | %10.2f%% | %10.2f%%%n",
            "Bipartition",
            totalAvgTime,
            avgFastestPct,
            totalFalsePct);
        
        // ================================================================
        // DÉTAILS PAR GROUPE
        // ================================================================
        System.out.println("\n=== Detailed Results by (n, C) ===");
        System.out.println("Group (n,C) | Instances | Infeasible | Feasible | Avg Time (ms) | False %");
        System.out.println("------------+-----------+------------+----------+---------------+---------");
        
        for (Map.Entry<String, GroupStats> entry : groups.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int n = Integer.parseInt(parts[0]);
            int capacity = Integer.parseInt(parts[1]);
            GroupStats stats = entry.getValue();
            
            double avgTime = stats.totalTime / stats.totalInstances;
            double falsePct = 100.0 * stats.infeasibleCount / stats.totalInstances;
            
            System.out.printf("n=%-4d,c=%-4d | %-9d | %-10d | %-8d | %-13.3f | %-7.1f%%%n",
                n, capacity, 
                stats.totalInstances, 
                stats.infeasibleCount,
                stats.totalInstances - stats.infeasibleCount,
                avgTime, falsePct);
        }
        
        // ================================================================
        // RÉSUMÉ FINAL
        // ================================================================
        System.out.println("\n=== Final Summary ===");
        System.out.println("Total instances processed: " + totalInstances);
        System.out.println("Parse errors: " + parseErrors);
        System.out.println("Infeasible: " + infeasibleCount + " (" + 
            String.format("%.1f", 100.0 * infeasibleCount / totalInstances).replace('.', ',') + "%)");
        System.out.println("Feasible: " + (totalInstances - infeasibleCount));
        System.out.println("Average time per instance: " + 
            String.format("%.3f", totalTime / totalInstances).replace('.', ',') + " ms");
    }
    
    private static InstanceData loadInstance(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file);
        if (lines.isEmpty()) return null;
        
        int idx = 0;
        while (idx < lines.size() && lines.get(idx).trim().isEmpty()) idx++;
        if (idx >= lines.size()) return null;
        
        String[] first = lines.get(idx).trim().split("\\s+");
        if (first.length < 3) return null;
        
        int n = Integer.parseInt(first[0]);
        int capacity = Integer.parseInt(first[1]);
        int Cmax = Integer.parseInt(first[2]);
        
        Task[] tasks = new Task[n];
        int count = 0;
        
        for (int i = idx + 1; i < lines.size() && count < n; i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            if (parts.length < 4) continue;
            
            try {
                int est = Integer.parseInt(parts[0]);
                int p = Integer.parseInt(parts[1]);
                int q = Integer.parseInt(parts[2]);
                int h = Integer.parseInt(parts[3]);
                int lct = Cmax - q;
                tasks[count++] = new Task(count, est, lct, p, h, q);
            } catch (NumberFormatException e) {}
        }
        
        return count == n ? new InstanceData(tasks, capacity) : null;
    }
    
    private static class InstanceData {
        final Task[] tasks;
        final int capacity;
        InstanceData(Task[] tasks, int capacity) {
            this.tasks = tasks;
            this.capacity = capacity;
        }
    }
}
