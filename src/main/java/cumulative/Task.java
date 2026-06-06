package cumulative;

public class Task {
    private final int id;
    private final int est;
    private final int lct;
    private final int p;
    private final int h;
    private final int q;
    
    public Task(int id, int est, int lct, int p, int h, int q) {
        this.id = id;
        this.est = est;
        this.lct = lct;
        this.p = p;
        this.h = h;
        this.q = q;
    }
    
    public int getId() { return id; }
    public int getEst() { return est; }
    public double getEstD() { return est; }
    public int getEct() { return est + p; }
    public double getEctD() { return est + p; }
    public int getLst() { return lct - p; }
    public double getLstD(int dummy) { return lct - p; }
    public int getLct() { return lct; }
    public double getLctD(int dummy) { return lct; }
    public int getP() { return p; }
    public int getH() { return h; }
    public int getQ() { return q; }
    
    @Override
    public String toString() {
        return String.format("Task[%d: est=%d, lct=%d, p=%d, h=%d, q=%d]", 
                             id, est, lct, p, h, q);
    }
}
