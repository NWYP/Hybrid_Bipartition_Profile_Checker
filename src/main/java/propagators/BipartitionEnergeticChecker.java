package propagators;

import cumulative.Task;
import profile.Profile;
import profile.Timepoint;
import java.util.*;

public class BipartitionEnergeticChecker {
    
    private final int capacity;
    private final int n;
    private final Task[] tasks;
    
    private final Task[] tasksByEst;
    private final Task[] tasksByEct;
    private final Task[] tasksByLst;
    private final Task[] tasksByLct;
    private final Task[] tasksByMiddle;
    private Profile profile;
    private ArrayList<Integer> delta;
    
    public BipartitionEnergeticChecker(Task[] tasks, int capacity) {
        if (tasks == null || tasks.length == 0 || capacity <= 0) {
            throw new IllegalArgumentException("Invalid input");
        }
        this.tasks = tasks;
        this.capacity = capacity;
        this.n = tasks.length;
        this.profile = new Profile();
        this.delta = new ArrayList<>();
        this.tasksByEst = copyAndSort(tasks, Comparator.comparingInt(Task::getEst));
        this.tasksByEct = copyAndSort(tasks, Comparator.comparingInt(Task::getEct));
        this.tasksByLst = copyAndSort(tasks, Comparator.comparingInt(Task::getLst));
        this.tasksByLct = copyAndSort(tasks, Comparator.comparingInt(Task::getLct));
        this.tasksByMiddle = copyAndSort(tasks, 
            Comparator.comparingInt(t -> t.getEst() + t.getLct()));
    }
    
    private Task[] copyAndSort(Task[] source, Comparator<Task> cmp) {
        Task[] copy = Arrays.copyOf(source, source.length);
        Arrays.sort(copy, cmp);
        return copy;
    }
    
    public boolean overloadCheck() {
        int maxLct = buildGlobalTimeline();
        int span = scheduleTasksGlobal(maxLct);
        if (span > maxLct) return false;
        if (delta.isEmpty()) return true;
        
        for (int b : delta) {
            int totalHeightAtB = 0;
            for (int i = 0; i < n; i++) {
                if (decomposedLct(i, b) == b) {
                    totalHeightAtB += tasks[i].getH();
                }
            }
            if (totalHeightAtB > capacity) return false;
            
            if (exceedsPeakHeight(b)) {
                if (!checkBipartitionAtBound(b)) return false;
            }
        }
        return true;
    }
    
    private int buildGlobalTimeline() {
        profile.reset();
        profile.Add(new Timepoint(tasksByEst[0].getEst(), capacity));
        Timepoint current = profile.first;
        int totalP = 0, i = 0, j = 0, k = 0;
        int maxLct = Integer.MIN_VALUE;
        
        while (i < n || j < n || k < n) {
            int timeEst = (i < n) ? tasksByEst[i].getEst() : Integer.MAX_VALUE;
            int timeEct = (j < n) ? tasksByEct[j].getEct() : Integer.MAX_VALUE;
            int timeLct = (k < n) ? tasksByLct[k].getLct() : Integer.MAX_VALUE;
            
            if (timeEst <= timeEct && timeEst <= timeLct) {
                current = advanceOrInsert(current, timeEst);
                current.increment += tasksByEst[i].getH();
                current.incrementMax += tasksByEst[i].getH();
                totalP += tasksByEst[i].getP();
                maxLct = Math.max(maxLct, tasksByEst[i].getLct());
                i++;
            } else if (timeEct <= timeLct) {
                current = advanceOrInsert(current, timeEct);
                current.increment -= tasksByEct[j].getH();
                current.isBound = true;
                j++;
            } else {
                current = advanceOrInsert(current, timeLct);
                current.incrementMax -= tasksByLct[k].getH();
                current.isBound = true;
                k++;
            }
        }
        current.InsertAfter(new Timepoint(maxLct + totalP, 0));
        return maxLct;
    }
    
    private int scheduleTasksGlobal(int maxLct) {
        int hReq = 0, hMaxInc = 0, overflow = 0, ect = Integer.MIN_VALUE;
        Timepoint t = profile.first;
        delta.clear();
        
        while (t.time < maxLct) {
            int slotLength = t.next.time - t.time;
            t.overflow = overflow;
            hMaxInc += t.incrementMax;
            int hMax = Math.min(hMaxInc, capacity);
            hReq += t.increment;
            int hCons = Math.min(hReq + overflow, hMax);
            t.cons = hCons;
            
            if (overflow > 0 && overflow < (hCons - hReq) * slotLength) {
                slotLength = Math.max(1, overflow / (hCons - hReq));
                t.InsertAfter(new Timepoint(t.time + slotLength, t.capacity));
            }
            overflow += (hReq - hCons) * slotLength;
            t.capacity = capacity - hCons;
            if (t.isBound && t.overflow > 0) delta.add(t.time);
            if (t.capacity < capacity) ect = t.next.time;
            t = t.next;
        }
        t.overflow = overflow;
        if (t.isBound && t.overflow > 0) delta.add(t.time);
        return overflow > 0 ? Integer.MAX_VALUE : ect;
    }
    
    private Timepoint advanceOrInsert(Timepoint current, int time) {
        if (time > current.time) {
            current.InsertAfter(new Timepoint(time, capacity));
            current = current.next;
        }
        return current;
    }
    
    private boolean exceedsPeakHeight(int b) {
        int totalHeight = 0;
        for (int i = 0; i < n; i++) {
            if (decomposedLct(i, b) == b) totalHeight += tasks[i].getH();
        }
        return totalHeight > capacity;
    }
    
    private int decomposedLct(int idx, int b) {
        Task t = tasks[idx];
        if (t.getLct() <= b) return t.getLct();
        if (t.getLst() <= b) return b;
        return -1;
    }
    
    private boolean checkBipartitionAtBound(int b) {
        long fixedBeforeEnergy = 0;
        long fixedAfterEnergy = 0;
        List<CrossingTask> crossingTasks = new ArrayList<>();
        
        for (Task t : tasks) {
            int est = t.getEst();
            int lct = t.getLct();
            int p = t.getP();
            int h = t.getH();
            
            if (lct <= b) {
                fixedBeforeEnergy += (long) p * h;
            } else if (est >= b) {
                fixedAfterEnergy += (long) p * h;
            } else {
                crossingTasks.add(new CrossingTask(p, h));
            }
        }
        
        int horizonStart = Integer.MAX_VALUE;
        int horizonEnd = Integer.MIN_VALUE;
        for (Task t : tasks) {
            horizonStart = Math.min(horizonStart, t.getEst());
            horizonEnd = Math.max(horizonEnd, t.getLct());
        }
        
        long capacityBefore = (long) capacity * Math.max(0, b - horizonStart) - fixedBeforeEnergy;
        long capacityAfter = (long) capacity * Math.max(0, horizonEnd - b) - fixedAfterEnergy;
        
        if (capacityBefore < 0 || capacityAfter < 0) return false;
        if (crossingTasks.isEmpty()) return true;
        
        return solveBipartitionDP(crossingTasks, capacityBefore, capacityAfter);
    }
    
    private boolean solveBipartitionDP(List<CrossingTask> tasks, long capBefore, long capAfter) {
        int m = tasks.size();
        if (capBefore > 10000 || m > 100) {
            return solveBipartitionGreedy(tasks, capBefore, capAfter);
        }
        
        boolean[][] dp = new boolean[m + 1][(int) capBefore + 1];
        dp[0][0] = true;
        
        for (int i = 0; i < m; i++) {
            CrossingTask t = tasks.get(i);
            for (int used = 0; used <= capBefore; used++) {
                if (dp[i][used]) {
                    long newUsed = used + t.totalEnergy;
                    if (newUsed <= capBefore) {
                        dp[i + 1][(int) newUsed] = true;
                    }
                    dp[i + 1][used] = true;
                }
            }
        }
        
        for (int used = 0; used <= capBefore; used++) {
            if (dp[m][used]) return true;
        }
        return false;
    }
    
    private boolean solveBipartitionGreedy(List<CrossingTask> tasks, long capBefore, long capAfter) {
        List<CrossingTask> sorted = new ArrayList<>(tasks);
        sorted.sort((a, b) -> Long.compare(b.totalEnergy, a.totalEnergy));
        
        long usedBefore = 0;
        long usedAfter = 0;
        
        for (CrossingTask t : sorted) {
            if (usedBefore + t.totalEnergy <= capBefore) {
                usedBefore += t.totalEnergy;
            } else if (usedAfter + t.totalEnergy <= capAfter) {
                usedAfter += t.totalEnergy;
            } else {
                return false;
            }
        }
        return true;
    }
    
    private static final class CrossingTask {
        final long totalEnergy;
        CrossingTask(int p, int h) {
            this.totalEnergy = (long) p * h;
        }
    }
}
