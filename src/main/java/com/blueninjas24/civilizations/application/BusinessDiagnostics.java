package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.BusinessRuntime;
import java.util.*;

/** Admin text from the immutable owner cache; never a SQL operation or a mutation command. */
public final class BusinessDiagnostics {
    private BusinessDiagnostics() {}
    public static List<String> describe(BusinessRuntime.Snapshot snapshot,UUID town) {
        var lines=new ArrayList<String>();
        var views=snapshot.businesses().values().stream().filter(v->v.settlement().equals(town))
                .sorted(Comparator.comparing(v->v.id().toString())).toList();
        lines.add("Business cache: "+snapshot.businesses().size()+" of "+snapshot.total()+" registered businesses; refreshed in bounded rotation.");
        if(views.isEmpty())lines.add("No business cached for this settlement. Automatic founding and production are not active yet.");
        for(var v:views.stream().limit(20).toList()) {
            lines.add(v.name()+" ["+v.industry()+"] "+v.id()+"; "+(v.paused()?"world absent: paused":"world available"));
            lines.add("Founder: "+v.founder()+"; owner: "+v.owner()+"; "+(v.ownerAvailable()?"owner available":"owner unavailable; succession not implemented"));
            lines.add("Workers: "+v.workers()+"/"+v.capacity()+"; vacancies: "+v.vacancies()+"; retained open assignments: "+v.openAssignments());
            lines.add("Site: "+v.x()+", "+v.y()+", "+v.z()+"; founded administrative month "+v.foundedStep()+". Registration grants no block rights.");
            if(v.lastSeparation()!=null)lines.add("Latest staffing separation: "+v.lastSeparation());
        }
        if(views.size()>20)lines.add("Showing 20 of "+views.size()+" cached businesses for this settlement.");
        return List.copyOf(lines);
    }
}
