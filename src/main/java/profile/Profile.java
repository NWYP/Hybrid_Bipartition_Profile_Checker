package profile;

public class Profile {
    public Timepoint first;
    public Timepoint last;
    
    public Profile() {
        this.first = null;
        this.last = null;
    }
    
    public void reset() {
        this.first = null;
        this.last = null;
    }
    
    public void Add(Timepoint tp) {
        if (first == null) {
            first = tp;
            last = tp;
        } else {
            last.next = tp;
            last = tp;
        }
    }
}
