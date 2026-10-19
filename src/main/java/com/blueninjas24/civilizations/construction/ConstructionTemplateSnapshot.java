package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.economy.Commodity;
import java.io.*;
import java.util.*;

/** Versioned, bounded semantic snapshot. Accepted plans never follow subsequent edits to template files. */
public final class ConstructionTemplateSnapshot {
    public static final int MAX_BYTES=8_388_608;
    private ConstructionTemplateSnapshot(){}
    public static byte[] encode(ConstructionTemplate template) {
        try {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            out.writeInt(1);out.writeUTF(template.id());out.writeInt(template.revision());out.writeUTF(template.name());out.writeUTF(template.attribution());out.writeUTF(template.purpose().name());
            out.writeInt(template.size().x());out.writeInt(template.size().y());out.writeInt(template.size().z());out.writeInt(template.stages().size());
            for(var stage:template.stages()) {
                out.writeUTF(stage.id());out.writeLong(stage.work());out.writeInt(stage.materials().size());
                for(var item:stage.materials().entrySet()){out.writeUTF(item.getKey().id());out.writeLong(item.getValue());}
                out.writeInt(stage.blocks().size());for(var item:stage.blocks().entrySet()){position(out,item.getKey());out.writeUTF(item.getValue());}
            }
            out.writeInt(template.anchors().size());for(var anchor:template.anchors()){out.writeUTF(anchor.role().name());position(out,anchor.at());out.writeUTF(anchor.facing().name());}
            out.flush();if(bytes.size()>MAX_BYTES)throw new IllegalArgumentException("Oversized construction snapshot");return bytes.toByteArray();
        }catch(IOException impossible){throw new UncheckedIOException(impossible);}
    }
    public static ConstructionTemplate decode(byte[] bytes)throws IOException {
        Objects.requireNonNull(bytes);if(bytes.length>MAX_BYTES)throw new IOException("Oversized construction snapshot");
        try {
            var in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readInt()!=1)throw new IOException("Unsupported construction snapshot version");
            String id=in.readUTF();int revision=in.readInt();String name=in.readUTF(),attribution=in.readUTF();var purpose=Purpose.valueOf(in.readUTF());
            var size=new Size(in.readInt(),in.readInt(),in.readInt());int stageCount=count(in,ConstructionTemplate.MAX_STAGES);
            var stages=new ArrayList<Stage>();int total=0;
            for(int i=0;i<stageCount;i++) {
                String stageId=in.readUTF();long work=in.readLong();int materialCount=count(in,32);var materials=new TreeMap<Commodity,Long>();
                for(int j=0;j<materialCount;j++)if(materials.put(new Commodity(in.readUTF()),in.readLong())!=null)throw new IOException("Duplicate snapshot material");
                int blockCount=count(in,ConstructionTemplate.MAX_BLOCKS);total=Math.addExact(total,blockCount);
                if(total>ConstructionTemplate.MAX_BLOCKS)throw new IOException("Snapshot block budget exceeded");
                var blocks=new TreeMap<Position,String>();for(int j=0;j<blockCount;j++)if(blocks.put(position(in),in.readUTF())!=null)throw new IOException("Duplicate snapshot coordinate");
                stages.add(new Stage(stageId,work,materials,blocks));
            }
            int anchorCount=count(in,ConstructionTemplate.MAX_ANCHORS);var anchors=new ArrayList<Anchor>();
            for(int i=0;i<anchorCount;i++)anchors.add(new Anchor(Role.valueOf(in.readUTF()),position(in),Direction.valueOf(in.readUTF())));
            if(in.available()!=0)throw new IOException("Trailing snapshot data");
            var template=new ConstructionTemplate(id,revision,name,attribution,purpose,size,stages,anchors);
            if(!Arrays.equals(bytes,encode(template)))throw new IOException("Non-canonical construction snapshot");
            return template;
        }catch(IllegalArgumentException|ArithmeticException invalid){throw new IOException("Invalid construction snapshot",invalid);}
    }
    private static int count(DataInputStream in,int maximum)throws IOException{int n=in.readInt();if(n<0||n>maximum)throw new IOException("Snapshot count exceeds budget");return n;}
    private static void position(DataOutputStream out,Position p)throws IOException{out.writeInt(p.x());out.writeInt(p.y());out.writeInt(p.z());}
    private static Position position(DataInputStream in)throws IOException{return new Position(in.readInt(),in.readInt(),in.readInt());}
}
