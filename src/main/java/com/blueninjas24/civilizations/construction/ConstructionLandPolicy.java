package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.Bounds;
import java.util.*;

/** Approved bounded clear-land heuristic, not a claim of historical natural-block provenance. */
public record ConstructionLandPolicy(int buildingBuffer) {
    public enum Kind { AIR,NATURAL_LOOKING_GROUND,ROAD,OTHER }
    public enum Finding { INCOMPLETE,PROTECTED,FOOTPRINT_OCCUPIED,UNSUITABLE_GROUND,NEARBY_OBSTACLE }
    public record Cell(Kind kind,boolean protectedArea){public Cell{Objects.requireNonNull(kind);}}
    public record Report(Set<Finding> findings){public Report{findings=Set.copyOf(findings);}public boolean suitable(){return findings.isEmpty();}}
    public ConstructionLandPolicy{if(buildingBuffer<0||buildingBuffer>16)throw new IllegalArgumentException("Construction building buffer must be 0..16");}
    public Set<Position> requiredCells(ConstructionTemplate template,Position origin) {
        long width=template.size().x()+2L*buildingBuffer,depth=template.size().z()+2L*buildingBuffer;
        if(width*depth*(template.size().y()+1L)>ConstructionPreview.MAX_OBSERVATION_CELLS)throw new IllegalArgumentException("Template and buffer exceed the bounded site preview budget");
        var cells=new TreeSet<Position>();
        for(int y=-1;y<template.size().y();y++)for(int z=-buildingBuffer;z<template.size().z()+buildingBuffer;z++)for(int x=-buildingBuffer;x<template.size().x()+buildingBuffer;x++)cells.add(origin.plus(new Position(x,y,z)));
        cells.addAll(ConstructionPreview.requiredCells(template,Transform.identity(),origin));
        if(cells.size()>ConstructionPreview.MAX_OBSERVATION_CELLS)throw new IllegalArgumentException("Buffered connector coverage exceeds bounded budget");return Collections.unmodifiableSet(cells);
    }
    public Report review(ConstructionTemplate template,Position origin,Map<Position,Cell> observations,boolean complete) {
        var required=requiredCells(template,origin);var findings=EnumSet.noneOf(Finding.class);
        if(!complete||!observations.keySet().containsAll(required))return new Report(Set.of(Finding.INCOMPLETE));
        Bounds footprint=ConstructionPreview.footprint(template,Transform.identity(),origin);
        for(var p:required) {
            var cell=observations.get(p);if(cell.protectedArea())findings.add(Finding.PROTECTED);
            if(footprint.contains(p)) {if(cell.kind()!=Kind.AIR)findings.add(Finding.FOOTPRINT_OCCUPIED);}
            else if(p.y()==origin.y()-1) {
                boolean underBuilding=p.x()>=footprint.minimum().x()&&p.x()<=footprint.maximum().x()&&p.z()>=footprint.minimum().z()&&p.z()<=footprint.maximum().z();
                if(underBuilding&&cell.kind()!=Kind.NATURAL_LOOKING_GROUND)findings.add(Finding.UNSUITABLE_GROUND);
                else if(!underBuilding&&cell.kind()!=Kind.NATURAL_LOOKING_GROUND&&cell.kind()!=Kind.ROAD)findings.add(Finding.NEARBY_OBSTACLE);
            }else if(cell.kind()!=Kind.AIR)findings.add(Finding.NEARBY_OBSTACLE);
        }
        return new Report(findings);
    }
}
