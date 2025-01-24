package org.jlab.rec.atof.hit;

import java.util.List;
import org.jlab.geom.base.*;
import org.jlab.geom.detector.alert.ATOF.*;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.geom.prim.Point3D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.rec.atof.constants.Parameters;
import org.jlab.rec.atof.trackMatch.TrackProjection;
import org.jlab.rec.atof.trackMatch.TrackProjector;

public class AtofHit {

    private int sector, layer, component, order;
    private int TDC, ToT;
    private double time, energy, x, y, z;
    private String type;
    private boolean is_in_a_cluster;
    private double path_length, inpath_length;

    public int getSector() {
        return sector;
    }

    public void setSector(int sector) {
        this.sector = sector;
    }

    public int getLayer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getComponent() {
        return component;
    }

    public void setComponent(int component) {
        this.component = component;
    }

    public int getTDC() {
        return TDC;
    }

    public void setTDC(int tdc) {
        this.TDC = tdc;
    }

    public int getToT() {
        return ToT;
    }

    public void setToT(int tot) {
        this.ToT = tot;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getIs_in_a_cluster() {
        return is_in_a_cluster;
    }

    public void setIs_in_a_cluster(boolean is_in_a_cluster) {
        this.is_in_a_cluster = is_in_a_cluster;
    }

    public double getPath_length() {
        return path_length;
    }

    public void setPath_length(double path_length) {
        this.path_length = path_length;
    }

    public double getInpath_length() {
        return inpath_length;
    }

    public void setInpath_length(double inpath_length) {
        this.inpath_length = inpath_length;
    }

    public int computeModule_index() {
        //Index ranging 0 to 60 for each wedge+bar module
        return 4 * this.sector + this.layer;
    }

    public final String makeType() {
        //Type of hit can be wedge, bar up, bar down or bar.
        //Avoids testing components and order every time.
        String itype = "undefined";
        if (this.component == 10 && this.order == 0) {
            itype = "bar down";
        } else if (this.component == 10 && this.order == 1) {
            itype = "bar up";
        } else if (this.component < 10) {
            itype = "wedge";
        }
        this.type = itype;
        return itype;
    }

    public final int TDC_to_time() {
        double tdc2time, veff, distance_to_sipm;
        if (null == this.type) {
            System.out.print("Null hit type, cannot convert tdc to time.");
            return 1;
        } else {
            switch (this.type) {
                case "wedge" -> {
                    tdc2time = Parameters.TDC2TIME;
                    veff = Parameters.VEFF;
                    //Wedge hits are placed at the center of wedges and sipm at their top
                    distance_to_sipm = Parameters.WEDGE_THICKNESS / 2.;
                }
                case "bar up" -> {
                    tdc2time = Parameters.TDC2TIME;
                    veff = Parameters.VEFF;
                    //The distance will be computed at barhit level when z information is available
                    distance_to_sipm = 0;
                }
                case "bar down" -> {
                    tdc2time = Parameters.TDC2TIME;
                    veff = Parameters.VEFF;
                    //The distance will be computed at barhit level when z information is available
                    distance_to_sipm = 0;
                }
                case "bar" -> {
                    System.out.print("Bar hit type, cannot convert tdc to time.");
                    return 1;
                }
                default -> {
                    System.out.print("Undefined hit type, cannot convert tdc to time.");
                    return 1;
                }
            }
        }
        //Hit time. Will need implementation of offsets.
        this.time = tdc2time * this.TDC - distance_to_sipm / veff;
        return 0;
    }

    public final int ToT_to_energy() {
        double tot2energy;
        if (null == this.type) {
            System.out.print("Null hit type, cannot convert tot to energy.");
            return 1;
        } else {
            switch (this.type) {
                case "wedge" -> {
                    tot2energy = Parameters.TOT2ENERGY_WEDGE;
                    //For now hits are considered in the middle of the wedge
                    //And the SiPM on top 
                    double distance_hit_to_sipm = Parameters.WEDGE_THICKNESS / 2.;
                    this.energy = tot2energy * this.ToT * Math.exp(distance_hit_to_sipm / Parameters.ATT_L);
                }
                case "bar up" -> {
                    tot2energy = Parameters.TOT2ENERGY_BAR;
                    //only half the information in the bar, 
                    //the attenuation will be computed when the full hit is formed
                    this.energy = tot2energy * this.ToT;
                }
                case "bar down" -> {
                    tot2energy = Parameters.TOT2ENERGY_BAR;
                    //only half the information in the bar, 
                    //the attenuation will be computed when the full hit is formed
                    this.energy = tot2energy * this.ToT;
                }
                case "bar" -> {
                    System.out.print("Bar hit type, cannot convert tot to energy.");
                    return 1;
                }
                default -> {
                    System.out.print("Undefined hit type, cannot convert tot to energy.");
                    return 1;
                }
            }
        }
        return 0;
    }

    public final int slc_to_xyz(Detector atof) {
        int sl;
        if (null == this.type) {
            return 1;
        } else {
            switch (this.type) {
                case "wedge" ->
                    sl = 1;
                case "bar up", "bar down", "bar" ->
                    sl = 0;
                default -> {
                    return 1;
                }
            }
        }
        Component comp = atof.getSector(this.sector).getSuperlayer(sl).getLayer(this.layer).getComponent(this.component);
        Point3D midpoint = comp.getMidpoint();
        //Midpoints defined in the system were z=0 is the upstream end of the atof
        //Translation to the system were z=0 is the center of the atof
        //Units are mm
        this.x = midpoint.x();
        this.y = midpoint.y();
        this.z = midpoint.z() - Parameters.LENGTH_ATOF / 2.;
        return 0;
    }

    public boolean matchBar(AtofHit hit2match) {
        if (this.getSector() != hit2match.getSector()) {
            //Two hits in different sectors
            return false; 
        } else if (this.getLayer() != hit2match.getLayer()) {
            //Two hits in different layers
            return false; 
        } else if (this.getComponent() != 10 || hit2match.getComponent() != 10) {
            //At least one hit not in the bar
            return false; 
        } else if (this.getOrder() > 1 || hit2match.getOrder() > 1) {
            //At least one hit has incorrect order
            return false; 
        } else {
            //Match if one is order 0 and the other is order 1
            return this.getOrder() != hit2match.getOrder();
        }
    }

    public double getPhi() {
        return Math.atan2(this.y, this.x);
    }

    public AtofHit(int sector, int layer, int component, int order, int tdc, int tot, Detector atof) {
        this.sector = sector;
        this.layer = layer;
        this.component = component;
        this.order = order;
        this.TDC = tdc;
        this.ToT = tot;
        this.is_in_a_cluster = false;

        this.makeType();
        int is_ok = this.TDC_to_time();
        if (is_ok != 1) {
            is_ok = this.ToT_to_energy();
        }
        if (is_ok != 1) {
            is_ok = this.slc_to_xyz(atof);
        }
    }

    public AtofHit(int sector, int layer, int component, int order, int tdc, int tot, Detector atof, TrackProjector track_projector) {
        this.sector = sector;
        this.layer = layer;
        this.component = component;
        this.order = order;
        this.TDC = tdc;
        this.ToT = tot;
        this.is_in_a_cluster = false;

        //First the type needs to be set
        this.makeType();
        //From it the coordinates can be computed
        this.slc_to_xyz(atof);
        //From them tracks can be matched
        this.matchTrack(track_projector);
        //And energy and time can then be computed
        this.ToT_to_energy();
        this.TDC_to_time();
    }

    public final void matchTrack(TrackProjector track_projector) {
        double sigma_phi = 0;
        double sigma_z = 0;

        List<TrackProjection> Projections = track_projector.getProjections();
        for (int i_track = 0; i_track < Projections.size(); i_track++) {
            Point3D projection_point = new Point3D();
            if (null == this.getType()) {
                System.out.print("Impossible to match track and hit; hit type is null \n");
            } else {
                switch (this.getType()) {
                    case "wedge" -> {
                        sigma_phi = Parameters.SIGMA_PHI_TRACK_MATCHING_WEDGE;
                        sigma_z = Parameters.SIGMA_Z_TRACK_MATCHING_WEDGE;
                        projection_point = Projections.get(i_track).get_WedgeIntersect();
                    }
                    case "bar up", "bar down" -> {
                        System.out.print("WARNING : YOU ARE MATCHING A TRACK TO A SINGLE HIT IN THE BAR. \n");
                        sigma_phi = Parameters.SIGMA_PHI_TRACK_MATCHING_BAR;
                        sigma_z = Parameters.SIGMA_Z_TRACK_MATCHING_BAR;
                        projection_point = Projections.get(i_track).get_BarIntersect();
                    }
                    default ->
                        System.out.print("Impossible to match track and hit; hit type is undefined \n");
                }
            }
            if (Math.abs(this.getPhi() - projection_point.toVector3D().phi()) < sigma_phi) {
                if (Math.abs(this.getZ() - projection_point.z()) < sigma_z) {
                    if ("wedge".equals(this.getType())) {
                        this.setPath_length(Projections.get(i_track).get_WedgePathLength());
                    } else {
                        this.setPath_length(Projections.get(i_track).get_BarPathLength());
                    }
                }
            }
        }
    }

    public int matchTrack(DataEvent event) {

        String track_bank_name = "AHDC::Projections";
        if (event == null) { // check if there is an event
            //System.out.print(" no event \n");
            return 1;
        } else if (event.hasBank(track_bank_name) == false) {
            return 1;
            // check if there are ahdc tracks in the event
            //System.out.print("no tracks \n");
        } else {
            DataBank track_bank = event.getBank(track_bank_name);
            int nt = track_bank.rows(); // number of tracks
            double sigma_phi = 0;
            double sigma_z = 0;

            //Looping through all tracks
            for (int i = 0; i < nt; i++) {

                Float xt = null, yt = null, zt = null, path = null, inpath = null;
                if (null == this.getType()) {
                    System.out.print("Impossible to match track and hit; hit type is null \n");
                } else {
                    switch (this.getType()) {
                        case "wedge" -> {
                            sigma_phi = Parameters.SIGMA_PHI_TRACK_MATCHING_WEDGE;
                            sigma_z = Parameters.SIGMA_Z_TRACK_MATCHING_WEDGE;
                            xt = track_bank.getFloat("x_at_wedge", i);
                            yt = track_bank.getFloat("y_at_wedge", i);
                            zt = track_bank.getFloat("z_at_wedge", i);
                            path = track_bank.getFloat("L_at_wedge", i);
                            inpath = track_bank.getFloat("L_in_wedge", i);
                        }
                        case "bar" -> {
                            sigma_phi = Parameters.SIGMA_PHI_TRACK_MATCHING_BAR;
                            sigma_z = Parameters.SIGMA_Z_TRACK_MATCHING_BAR;
                            xt = track_bank.getFloat("x_at_bar", i);
                            yt = track_bank.getFloat("y_at_bar", i);
                            zt = track_bank.getFloat("z_at_bar", i);
                            path = track_bank.getFloat("L_at_bar", i);
                            inpath = track_bank.getFloat("L_in_bar", i);
                        }
                        case "bar up", "bar down" -> {
                            System.out.print("WARNING : YOU ARE MATCHING A TRACK TO A SINGLE HIT IN THE BAR. \n");
                        }
                        default ->
                            System.out.print("Impossible to match track and hit; hit type is undefined \n");
                    }
                }
                Point3D projection_point = new Point3D(xt, yt, zt);
                if (Math.abs(this.getPhi() - projection_point.toVector3D().phi()) < sigma_phi) {
                    if (Math.abs(this.getZ() - projection_point.z()) < sigma_z) {
                        this.setPath_length(path);
                        this.setInpath_length(inpath);
                    }
                }
            }
        }
        return 0;
    }

    public AtofHit() {
    }

    public static void main(String[] args) {
    }
}
