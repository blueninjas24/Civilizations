package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionTemplateSnapshotTest {
    @Test void semanticSnapshotRoundTripsEveryOriginalFieldAndRejectsTruncation()throws Exception {
        var template=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_storehouse.yml"));
        var bytes=ConstructionTemplateSnapshot.encode(template);assertEquals(template,ConstructionTemplateSnapshot.decode(bytes));
        assertEquals(template.fingerprint(),ConstructionTemplateSnapshot.decode(bytes).fingerprint());
        assertThrows(IOException.class,()->ConstructionTemplateSnapshot.decode(Arrays.copyOf(bytes,bytes.length-1)));
        assertThrows(IOException.class,()->ConstructionTemplateSnapshot.decode(Arrays.copyOf(bytes,bytes.length+1)));
        bytes[3]=2;assertThrows(IOException.class,()->ConstructionTemplateSnapshot.decode(bytes));
    }
    @Test void malformedCountCannotAllocateUnboundedCollections()throws Exception {
        var template=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_path.yml"));
        var bytes=ConstructionTemplateSnapshot.encode(template);
        // Locate the first stage count using the same primitive format, then exceed the hard limit.
        var input=new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));input.readInt();input.readUTF();input.readInt();
        input.readUTF();input.readUTF();input.readUTF();input.readInt();input.readInt();input.readInt();
        int offset=bytes.length-input.available();java.nio.ByteBuffer.wrap(bytes,offset,4).putInt(Integer.MAX_VALUE);
        assertThrows(IOException.class,()->ConstructionTemplateSnapshot.decode(bytes));
    }
}
