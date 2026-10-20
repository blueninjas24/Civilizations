package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.util.*;

/** Verified authored bed pairs, not completed-world housing or permission to assign residents. */
public final class HomePlanCapacity {
    private HomePlanCapacity(){}
    public static int beds(ConstructionTemplate template) {
        if(template.purpose()!=Purpose.HOME)throw new IllegalArgumentException("A home plan is required");
        var blocks=new HashMap<Position,String>();template.stages().forEach(s->blocks.putAll(s.blocks()));
        var covered=new HashSet<Position>();int beds=0;
        for(var anchor:template.anchors())if(anchor.role()==Role.BED) {
            var foot=blocks.get(anchor.at());var properties=properties(foot);
            if(!"foot".equals(properties.get("part"))||!anchor.facing().name().toLowerCase(Locale.ROOT).equals(properties.get("facing")))throw new IllegalArgumentException("Home bed anchor must match an authored bed foot and facing");
            var offset=switch(anchor.facing()){case NORTH->new Position(0,0,-1);case EAST->new Position(1,0,0);case SOUTH->new Position(0,0,1);case WEST->new Position(-1,0,0);};
            var headAt=anchor.at().plus(offset);var head=blocks.get(headAt);var headProperties=properties(head);
            if(!material(foot).equals(material(head))||!"head".equals(headProperties.get("part"))||!properties.get("facing").equals(headProperties.get("facing"))
                    ||"true".equals(properties.get("occupied"))||"true".equals(headProperties.get("occupied"))
                    ||!covered.add(anchor.at())||!covered.add(headAt))throw new IllegalArgumentException("Home needs distinct complete unoccupied bed pairs");
            beds++;
        }
        long actual=blocks.values().stream().filter(s->material(s).endsWith("_bed")).count();
        if(beds==0||actual!=covered.size())throw new IllegalArgumentException("Every authored home bed must have one complete anchored pair");
        return beds;
    }
    private static String material(String state){return state==null?"":state.split("\\[",2)[0];}
    private static Map<String,String> properties(String state) {
        if(state==null||!material(state).endsWith("_bed")||!state.endsWith("]")||state.indexOf('[')<0)throw new IllegalArgumentException("Home bed state missing");
        var result=new HashMap<String,String>();
        for(var property:state.substring(state.indexOf('[')+1,state.length()-1).split(",")) {
            String[] pair=property.split("=",-1);if(pair.length!=2||result.putIfAbsent(pair[0],pair[1])!=null)throw new IllegalArgumentException("Ambiguous home bed properties");
        }return result;
    }
}
