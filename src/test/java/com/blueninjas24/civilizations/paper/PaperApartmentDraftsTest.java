package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.ApartmentDeclaration.Draft;
import com.blueninjas24.civilizations.construction.BuildingLayout.Bounds;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperApartmentDraftsTest {
    private UUID worldId=new UUID(0,1);private long now=1000;private boolean allowed=true;
    private Bounds selected=new Bounds(new Position(0,0,0),new Position(8,3,4));
    private Position here=new Position(4,1,0);private final List<Draft> submitted=new ArrayList<>();
    private final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{
        if(m.getName().equals("getUID"))return worldId;throw new AssertionError(m.getName());});
    private Player player(UUID id){return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
        case "getUniqueId"->id;case "getWorld"->world;case "hasPermission"->allowed;
        case "getLocation"->new Location(world,here.x(),here.y(),here.z());case "sendMessage"->null;
        default->throw new AssertionError(m.getName());});}
    private PaperApartmentDrafts editor(){return new PaperApartmentDrafts(p->selected,(p,d)->submitted.add(d),()->now);}
    private void command(PaperApartmentDrafts e,Player p,String...args){var full=new ArrayList<>(List.of("building"));full.addAll(List.of(args));e.command(p,full.toArray(String[]::new));}
    @Test void selectionsExplicitlyDefinePrivateUnitsSharedAccessAndSeparateYardBeforeFrozenReview(){
        var e=editor();var p=player(new UUID(0,2));command(e,p,"apartments",new UUID(0,3).toString());
        selected=new Bounds(new Position(0,0,1),new Position(3,3,4));here=new Position(1,1,1);command(e,p,"unit","west");
        selected=new Bounds(new Position(5,0,1),new Position(8,3,4));here=new Position(6,1,1);command(e,p,"unit","east");
        selected=new Bounds(new Position(0,0,0),new Position(8,3,0));command(e,p,"shared");
        selected=new Bounds(new Position(-2,0,-2),new Position(10,4,6));command(e,p,"apartmentplot");
        assertTrue(submitted.isEmpty());command(e,p,"reviewapartments");var d=submitted.getFirst();
        assertEquals(2,d.units().size());assertEquals(1,d.shared().size());assertNotEquals(d.plot(),d.structure());
        assertThrows(IllegalArgumentException.class,()->command(e,p,"reviewapartments"));
        assertEquals(2,d.units().size());
    }
    @Test void rejectedEditsKeepPriorDraftAndExpiryOrWorldChangeCannotSubmit(){
        var e=editor();var p=player(new UUID(0,2));command(e,p,"apartments",new UUID(0,3).toString());
        selected=new Bounds(new Position(0,0,1),new Position(3,3,4));here=new Position(1,1,1);command(e,p,"unit","west");
        assertThrows(IllegalArgumentException.class,()->command(e,p,"shared"));
        command(e,p,"reviewapartments");assertEquals(1,submitted.getFirst().units().size());
        here=new Position(1,1,1);command(e,p,"apartments",new UUID(0,3).toString());now+=PaperApartmentDrafts.IDLE_MILLIS;e.prune(id->p);
        assertThrows(IllegalArgumentException.class,()->command(e,p,"reviewapartments"));
        command(e,p,"apartments",new UUID(0,3).toString());worldId=UUID.randomUUID();e.prune(id->p);
        assertThrows(IllegalArgumentException.class,()->command(e,p,"reviewapartments"));
    }
    @Test void editorsAreBoundedAndPermissionLossOrDisconnectDropsUnsavedWork(){
        var e=editor();for(int n=0;n<4;n++)command(e,player(new UUID(0,10+n)),"apartments",new UUID(0,3).toString());
        assertThrows(IllegalArgumentException.class,()->command(e,player(new UUID(0,99)),"apartments",new UUID(0,3).toString()));
        allowed=false;e.prune(this::player);allowed=true;var p=player(new UUID(0,99));command(e,p,"apartments",new UUID(0,3).toString());
        e.prune(id->null);assertThrows(IllegalArgumentException.class,()->command(e,p,"reviewapartments"));
    }
}
