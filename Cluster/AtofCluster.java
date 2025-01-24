package org.jlab.rec.atof.cluster;
import java.util.ArrayList;
import org.jlab.rec.atof.hit.AtofHit;
import org.jlab.rec.atof.hit.BarHit;

public class AtofCluster {
    
    ArrayList<BarHit> bar_hits;
    ArrayList<AtofHit> wedge_hits;
    double x,y,z,time,energy;
    double path_length;
    
    public ArrayList<BarHit> getBarHits() {
        return bar_hits;
    }

    public void setBarHits(ArrayList<BarHit> bar_hits) {
        this.bar_hits = bar_hits;
    }

    public ArrayList<AtofHit> getWedgeHits() {
        return wedge_hits;
    }

    public void setWedgeHits(ArrayList<AtofHit> wedge_hits) {
        this.wedge_hits = wedge_hits;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }
    
    public double getPath_length() {
        return path_length;
    }

    public void setPath_length(double path_length) {
        this.path_length = path_length;

    public final void computeClusterProperties() {
        this.energy=0;
        double max_energy = -1;
        AtofHit max_energy_hit = new AtofHit();
        BarHit max_energy_barhit = new BarHit();

        for(int i_wedge = 0; i_wedge<this.wedge_hits.size(); i_wedge++)
        {
            AtofHit this_wedge_hit = this.wedge_hits.get(i_wedge);
            double this_energy = this_wedge_hit.getEnergy();
            this.energy+=this_energy;
            if(this_energy>max_energy){max_energy_hit = this_wedge_hit; max_energy = this_energy;}
        }
        
        for(int i_bar = 0; i_bar<this.bar_hits.size(); i_bar++)
        {
            BarHit this_bar_hit = this.bar_hits.get(i_bar);
            double this_energy = this_bar_hit.getEnergy();
            this.energy+=this_energy;
            if(this_energy>max_energy){max_energy_barhit = this_bar_hit; max_energy = this_energy;}
        }
        
        if(max_energy_hit.getEnergy() > max_energy_barhit.getEnergy())
        {
            this.time = max_energy_hit.getTime();
            this.x = max_energy_hit.getX();
            this.y = max_energy_hit.getY();
            this.z = max_energy_hit.getZ();
            this.path_length = max_energy_hit.getPath_length();
        }
        else
        {
            this.time = max_energy_barhit.getTime();
            this.x = max_energy_barhit.getX();
            this.y = max_energy_barhit.getY();
            this.z = max_energy_barhit.getZ();
            this.path_length = max_energy_barhit.getPath_length();
        }
        
    }
    
    public double getPhi()
    {
        return Math.atan2(this.y, this.x);
    }
    
    public AtofCluster(ArrayList<BarHit> bar_hits, ArrayList<AtofHit> wedge_hits) 
	{
		this.bar_hits = bar_hits;  
		this.wedge_hits = wedge_hits;  
                this.computeClusterProperties();
        }
    public static void main(String[] args) {
    }
    
}
