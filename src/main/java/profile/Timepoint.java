package profile;

public class Timepoint {
    public int time;
    public int capacity;
    public int increment;
    public int incrementMax;
    public int cons;
    public int overflow;
    public boolean isBound;
    public Timepoint next;
    
    public Timepoint(int time, int capacity) {
        this.time = time;
        this.capacity = capacity;
        this.increment = 0;
        this.incrementMax = 0;
        this.cons = 0;
        this.overflow = 0;
        this.isBound = false;
        this.next = null;
    }
    
    public void InsertAfter(Timepoint tp) {
        tp.next = this.next;
        this.next = tp;
    }
}
