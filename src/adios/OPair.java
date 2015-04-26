package adios;

public class OPair<L, R> {
    public L l;
    public R r;
    public OPair(L l, R r){
        this.l = l;
        this.r = r;
    }
    public L getL(){ return l; }
    public R getR(){ return r; }
    public void setL(L l){ this.l = l; }
    public void setR(R r){ this.r = r; }
    
    public boolean equals(Object o) {
    	if (o instanceof OPair<?, ?>)
    		return false;
    	else {
    		OPair<?, ?> temp = (OPair<?, ?>) o;
    		return temp.l.equals(l) && temp.r.equals(r);
    	}
    }
}
