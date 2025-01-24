package org.jlab.rec.atof.trackMatch;

import org.jlab.geom.prim.Point3D;

public class TrackProjection {
    private Point3D _BarIntersect = new Point3D(); 
    private Point3D _WedgeIntersect = new Point3D(); 
    
    Float _BarPathLength; 
    
    Float _WedgePathLength; 
    
    Float _BarInPathLength;
    Float _WedgeInPathLength;
    
    public TrackProjection() {
        _BarIntersect = new Point3D(Double.NaN, Double.NaN, Double.NaN);
        _WedgeIntersect = new Point3D(Double.NaN, Double.NaN, Double.NaN);
        _BarPathLength = Float.NaN;
        _WedgePathLength = Float.NaN;
        _BarInPathLength = Float.NaN;
        _WedgeInPathLength = Float.NaN;
    }
    public Point3D get_BarIntersect() {
        return _BarIntersect;
    }
    public Point3D get_WedgeIntersect() {
        return _WedgeIntersect;
    }
    public Float get_BarPathLength() {
        return _BarPathLength;
    }
    public Float get_BarInPathLength() {
        return _BarInPathLength;
    }

    public Float get_WedgePathLength() {
        return _WedgePathLength;
    }
    
    public Float get_WedgeInPathLength() {
        return _WedgeInPathLength;
    }

    public void set_BarIntersect(Point3D BarIntersect) {
        this._BarIntersect = BarIntersect;
    }
    public void set_WedgeIntersect(Point3D WedgeIntersect) {
        this._WedgeIntersect = WedgeIntersect;
    }
    public void set_BarPathLength(Float BarPathLength) {
        this._BarPathLength = BarPathLength;
    }
    public void set_WedgePathLength(Float WedgePathLength) {
        this._WedgePathLength = WedgePathLength;
    }
    
    public void set_BarInPathLength(Float BarInPathLength) {
        this._BarInPathLength = BarInPathLength;
    }
    public void set_WedgeInPathLength(Float WedgeInPathLength) {
        this._WedgeInPathLength = WedgeInPathLength;
    }

    public static void main(String arg[]) {
    }
}
