package org.jlab.rec.atof.cluster;

import cnuphys.magfield.MagneticFields;
import java.util.ArrayList;
import org.jlab.clas.swimtools.Swim;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.geom.base.Detector;
import org.jlab.geom.detector.alert.ATOF.AlertTOFFactory;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.rec.atof.hit.AtofHit;
import org.jlab.rec.atof.hit.BarHit;
import org.jlab.rec.atof.hit.HitFinder;
import org.jlab.rec.atof.trackMatch.TrackProjector;
import org.jlab.utils.CLASResources;

public class ClusterFinder {

    private ArrayList<AtofCluster> clusters;

    public ClusterFinder() {
        clusters = new ArrayList<>();
    }

    public void setClusters(ArrayList<AtofCluster> clusters) {
        this.clusters = clusters;
    }

    public ArrayList<AtofCluster> getClusters() {
        return clusters;
    }

    public void MakeClusters(HitFinder hitfinder) {
        clusters.clear();
        ArrayList<AtofHit> wedge_hits = hitfinder.getWedgeHits();
        ArrayList<BarHit> bar_hits = hitfinder.getBarHits();

        double sigma_Z = 6000.0;
        double sigma_T = 1000.0;
        int sigma_module = 1;
        int sigma_component = 1;

        // Processing wedge hits
        for (int i_wedge = 0; i_wedge < wedge_hits.size(); i_wedge++) {
            AtofHit this_wedge_hit = wedge_hits.get(i_wedge);
            if (this_wedge_hit.getIs_in_a_cluster()) continue;

            ArrayList<AtofHit> this_cluster_wedge_hits = new ArrayList<>();
            ArrayList<BarHit> this_cluster_bar_hits = new ArrayList<>();

            this_wedge_hit.setIs_in_a_cluster(true);
            this_cluster_wedge_hits.add(this_wedge_hit);

            // Grouping wedge hits
            for (int j_wedge = i_wedge + 1; j_wedge < wedge_hits.size(); j_wedge++) {
                AtofHit other_wedge_hit = wedge_hits.get(j_wedge);
                if (other_wedge_hit.getIs_in_a_cluster()) continue;

                int delta_module = Math.abs(this_wedge_hit.computeModule_index() - other_wedge_hit.computeModule_index());
                if (delta_module > 30) delta_module = 60 - delta_module;

                int delta_component = Math.abs(this_wedge_hit.getComponent() - other_wedge_hit.getComponent());
                double delta_T = Math.abs(this_wedge_hit.getTime() - other_wedge_hit.getTime());

                if (delta_module <= sigma_module && delta_component <= sigma_component && delta_T < sigma_T) {
                    other_wedge_hit.setIs_in_a_cluster(true);
                    this_cluster_wedge_hits.add(other_wedge_hit);
                }
            }

            // Matching bar hits to wedge hits
            for (int j_bar = 0; j_bar < bar_hits.size(); j_bar++) {
                BarHit other_bar_hit = bar_hits.get(j_bar);
                if (other_bar_hit.getIs_in_a_cluster()) continue;

                int delta_module = Math.abs(this_wedge_hit.computeModule_index() - other_bar_hit.computeModule_index());
                if (delta_module > 30) delta_module = 60 - delta_module;

                double delta_Z = Math.abs(this_wedge_hit.getZ() - other_bar_hit.getZ());
                double delta_T = Math.abs(this_wedge_hit.getTime() - other_bar_hit.getTime());

                if (delta_module <= sigma_module && delta_Z < sigma_Z && delta_T < sigma_T) {
                    other_bar_hit.setIs_in_a_cluster(true);
                    this_cluster_bar_hits.add(other_bar_hit);
                }
            }

            AtofCluster cluster = new AtofCluster(this_cluster_bar_hits, this_cluster_wedge_hits);
            clusters.add(cluster);
        }

        // Processing bar hits
        for (int i_bar = 0; i_bar < bar_hits.size(); i_bar++) {
            BarHit this_bar_hit = bar_hits.get(i_bar);
            if (this_bar_hit.getIs_in_a_cluster()) continue;

            ArrayList<AtofHit> this_cluster_wedge_hits = new ArrayList<>();
            ArrayList<BarHit> this_cluster_bar_hits = new ArrayList<>();
            this_bar_hit.setIs_in_a_cluster(true);
            this_cluster_bar_hits.add(this_bar_hit);

            for (int j_bar = i_bar + 1; j_bar < bar_hits.size(); j_bar++) {
                BarHit other_bar_hit = bar_hits.get(j_bar);
                if (other_bar_hit.getIs_in_a_cluster()) continue;

                int delta_module = Math.abs(this_bar_hit.computeModule_index() - other_bar_hit.computeModule_index());
                if (delta_module > 30) delta_module = 60 - delta_module;

                double delta_Z = Math.abs(this_bar_hit.getZ() - other_bar_hit.getZ());
                double delta_T = Math.abs(this_bar_hit.getTime() - other_bar_hit.getTime());

                if (delta_module <= sigma_module && delta_Z < sigma_Z && delta_T < sigma_T) {
                    other_bar_hit.setIs_in_a_cluster(true);
                    this_cluster_bar_hits.add(other_bar_hit);
                }
            }

            AtofCluster cluster = new AtofCluster(this_cluster_bar_hits, this_cluster_wedge_hits);
            clusters.add(cluster);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar <jar-file> org.jlab.rec.atof.cluster.ClusterFinder <input-file>");
            return;
        }

        String inputFile = args[0];

        try {
            System.setProperty("CLAS12DIR", "../../");
            String mapDir = CLASResources.getResourcePath("etc") + "/data/magfield";

            MagneticFields.getInstance().initializeMagneticFields(
                mapDir,
                "Symm_torus_r2501_phi16_z251_24Apr2018.dat",
                "Symm_solenoid_r601_phi1_z1201_13June2018.dat"
            );

            HipoDataSource reader = new HipoDataSource();
            reader.open(inputFile);

            HitFinder hitFinder = new HitFinder();
            TrackProjector projector = new TrackProjector();
            ClusterFinder clusterFinder = new ClusterFinder();

            int eventCount = 0;

            while (reader.hasEvent()) {
                DataEvent event = reader.getNextEvent();
                eventCount++;

                projector.ProjectTracks(event);
                hitFinder.FindHits(event, null, projector); // Update with detector object
                clusterFinder.MakeClusters(hitFinder);

                System.out.println("Event " + eventCount + ": Found " + clusterFinder.getClusters().size() + " clusters.");
            }

            reader.close();
            System.out.println("Processing completed.");

        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}



//old code below 


/*

package org.jlab.rec.atof.cluster;

import cnuphys.magfield.MagneticFields;
import java.util.ArrayList;
import org.jlab.clas.swimtools.Swim;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.base.Detector;
import org.jlab.geom.detector.alert.ATOF.AlertTOFFactory;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.rec.atof.hit.AtofHit;
import org.jlab.rec.atof.hit.BarHit;
import org.jlab.rec.atof.hit.HitFinder;
import org.jlab.rec.atof.trackMatch.TrackProjector;
import org.jlab.utils.CLASResources;

public class ClusterFinder {

    private ArrayList<AtofCluster> clusters;
    
    public void setClusters(ArrayList<AtofCluster> clusters) {
        this.clusters = clusters;
    }
    
    public ArrayList<AtofCluster> getClusters() {
        return clusters;
    }

    public void MakeClusters(HitFinder hitfinder) {

        //A list of clusters is built for each event
        clusters.clear();

        //Getting the list of hits, they must have been ordered by energy already
        ArrayList<AtofHit> wedge_hits = hitfinder.getWedgeHits();
        ArrayList<BarHit> bar_hits = hitfinder.getBarHits();

        double sigma_Phi = 6.0; //angle opening of a layer. to be read from DB in the future
        double sigma_Z = 6000;//to be read from DB in the future
        double sigma_T = 1000;//timing resolution to be read from DB in the future
	int sigma_module = 1; //hits are always within +-1 phi module of the most energetic
        int sigma_component = 1;//hits are always within +-1 z component of the most energetic

	
        //Looping through wedge hits first
        for (int i_wedge = 0; i_wedge < wedge_hits.size(); i_wedge++) {
            AtofHit this_wedge_hit = wedge_hits.get(i_wedge);
            //Make a cluster for each wedge hit that has not been previously clustered
            if (this_wedge_hit.getIs_in_a_cluster()) {
                continue;
            }
            //Holding onto the hits composing the cluster
            ArrayList<AtofHit> this_cluster_wedge_hits = new ArrayList<>();
            ArrayList<BarHit> this_cluster_bar_hits = new ArrayList<>();

            //Indicate that this hit now is in a cluster
            this_wedge_hit.setIs_in_a_cluster(true);
            //And store it
            this_cluster_wedge_hits.add(this_wedge_hit);

            //Check if other wedge hits should be clustered with the current one
            //Start from the index of the current one and look at less energetic hits
            for (int j_wedge = i_wedge + 1; j_wedge < wedge_hits.size(); j_wedge++) {
                AtofHit other_wedge_hit = wedge_hits.get(j_wedge);
                //If that other hit is already involved in a cluster, skip it
                if (other_wedge_hit.getIs_in_a_cluster()) {
                    continue;
                }
                //Check the distance between the hits
                //For now we use phi module and z component differences from what is observed in simu
                int delta_module = Math.abs(this_wedge_hit.computeModule_index() - other_wedge_hit.computeModule_index());
                if (delta_module > 30) {
                    delta_module = 60 - delta_module;
                }
                int delta_component = Math.abs(this_wedge_hit.getComponent() - other_wedge_hit.getComponent());
                //Later we could use z and phi threshold
                //double delta_Phi = Math.abs(this_wedge_hit.getPhi() - other_wedge_hit.getPhi());
                //double delta_Z = Math.abs(this_wedge_hit.getZ() - other_wedge_hit.getZ());
                //Time matching
                double delta_T = Math.abs(this_wedge_hit.getTime() - other_wedge_hit.getTime());

                if (delta_module <= sigma_module)//delta_Phi <= sigma_Phi)
                {
                    if (delta_component <= sigma_component)//delta_Z <= sigma_Z)
                    {
                        if (delta_T < sigma_T) {
                            other_wedge_hit.setIs_in_a_cluster(true);
                            this_cluster_wedge_hits.add(other_wedge_hit);
                        }
                    }
                }
            }

            //After clustering wedge hits, check if bar hits should be clustered with them
            for (int j_bar = 0; j_bar < bar_hits.size(); j_bar++) {
                BarHit other_bar_hit = bar_hits.get(j_bar);
                //Skip already clustered hits
                if (other_bar_hit.getIs_in_a_cluster()) {
                    continue;
                }
                //Check the distance between the hits
                //For now we use phi module difference from what is observed in simu
                int delta_module = Math.abs(this_wedge_hit.computeModule_index() - other_bar_hit.computeModule_index());
                if (delta_module > 30) {
                    delta_module = 60 - delta_module;
                }
                //Later we could use phi threshold
                //double delta_Phi = Math.abs(this_wedge_hit.getPhi() - other_wedge_hit.getPhi());
                double delta_Z = Math.abs(this_wedge_hit.getZ() - other_bar_hit.getZ());
                //Time matching
                double delta_T = Math.abs(this_wedge_hit.getTime() - other_bar_hit.getTime());
                if (delta_module <= sigma_module)//delta_Phi < sigma_Phi)
                {
                    if (delta_Z < sigma_Z) {
                        if (delta_T < sigma_T) {
                            other_bar_hit.setIs_in_a_cluster(true);
                            this_cluster_bar_hits.add(other_bar_hit);
                        }
                    }
                }
            }//End loop bar hits
            //After all wedge and bar hits have been grouped, build the cluster
            AtofCluster cluster = new AtofCluster(this_cluster_bar_hits, this_cluster_wedge_hits);
            //And add it to the list of clusters
            clusters.add(cluster);
        }//End loop on all wedge hits

        //Now make clusters from bar hits that are not associated with wedge hits
        //Loop through all bar hits
        for (int i_bar = 0; i_bar < bar_hits.size(); i_bar++) {
            BarHit this_bar_hit = bar_hits.get(i_bar);
            //Skip hits that have already been clustered
            if (this_bar_hit.getIs_in_a_cluster()) {
                continue;
            }

            ArrayList<AtofHit> this_cluster_wedge_hits = new ArrayList<AtofHit>();
            ArrayList<BarHit> this_cluster_bar_hits = new ArrayList<BarHit>();
            this_bar_hit.setIs_in_a_cluster(true);
            this_cluster_bar_hits.add(this_bar_hit);
            
            //Loop through less energetic clusters
            for (int j_bar = i_bar + 1; j_bar < bar_hits.size(); j_bar++) {
                BarHit other_bar_hit = bar_hits.get(j_bar);
                //Skip already clustered hits
                if (other_bar_hit.getIs_in_a_cluster()) {
                    continue;
                }
                
                //Check the distance between the hits
                //For now we use phi module difference from what is observed in simu
                int delta_module = Math.abs(this_bar_hit.computeModule_index() - other_bar_hit.computeModule_index());
                if (delta_module > 30) {
                    delta_module = 60 - delta_module;
                }
                //Later we could use phi threshold
                //double delta_Phi = Math.abs(this_wedge_hit.getPhi() - other_wedge_hit.getPhi());
                double delta_Z = Math.abs(this_bar_hit.getZ() - other_bar_hit.getZ());
                //Time matching
                double delta_T = Math.abs(this_bar_hit.getTime() - other_bar_hit.getTime());

                if (delta_module <= sigma_module)//delta_Phi < sigma_Phi)
                {
                    if (delta_Z < sigma_Z) {
                        if (delta_T < sigma_T) {
                            other_bar_hit.setIs_in_a_cluster(true);
                            this_cluster_bar_hits.add(other_bar_hit);
                        }
                    }
                }
            }
            AtofCluster cluster = new AtofCluster(this_cluster_bar_hits, this_cluster_wedge_hits);
            clusters.add(cluster);
        }
    }

    public ClusterFinder() {
        clusters = new ArrayList<AtofCluster>();
    }

    public static void main(String[] args) {
        // TODO code application logic here
        AlertTOFFactory factory = new AlertTOFFactory();
        DatabaseConstantProvider cp = new DatabaseConstantProvider(11, "default");
        Detector atof = factory.createDetectorCLAS(cp);

        //READING MAG FIELD MAP
        System.setProperty("CLAS12DIR", "../../");
        String mapDir = CLASResources.getResourcePath("etc") + "/data/magfield";
        try {
            MagneticFields.getInstance().initializeMagneticFields(mapDir,
                    "Symm_torus_r2501_phi16_z251_24Apr2018.dat", "Symm_solenoid_r601_phi1_z1201_13June2018.dat");
        } catch (Exception e) {
            e.printStackTrace();
        }
        float[] b = new float[3];
        Swim swimmer = new Swim();
        swimmer.BfieldLab(0, 0, 0, b);
        double B = Math.abs(b[2]);

        //Track Projector Initialisation with B field
        TrackProjector projector = new TrackProjector();
        projector.setB(B);

        //Input to be read
        String input = "/Users/npilleux/Desktop/alert/atof-reconstruction/coatjava/reconstruction/alert/src/main/java/org/jlab/rec/atof/Hit/update_protons_to_test_with_tracks.hipo";
        HipoDataSource reader = new HipoDataSource();
        reader.open(input);

        HitFinder hitfinder = new HitFinder();

        int event_number = 0;
        while (reader.hasEvent()) {
            DataEvent event = (DataEvent) reader.getNextEvent();
            event_number++;
            projector.ProjectTracks(event);
            hitfinder.FindHits(event, atof, projector);
            ClusterFinder clusterfinder = new ClusterFinder();
            clusterfinder.MakeClusters(hitfinder);
        }
    }

}
*/
    
