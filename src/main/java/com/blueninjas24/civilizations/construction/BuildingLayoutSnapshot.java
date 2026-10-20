package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.io.*;
import java.util.*;

/** Bounded canonical format for freezing a reviewed layout. Does not register or authorize a building. */
public final class BuildingLayoutSnapshot {
    public static final int MAX_BYTES = 131_072;
    private BuildingLayoutSnapshot() {}
    public static byte[] encode(BuildingLayout layout) {
        try {
            var bytes = new ByteArrayOutputStream(); var out = new DataOutputStream(bytes);
            out.writeInt(1); uuid(out, layout.id()); uuid(out, layout.settlement()); uuid(out, layout.world());
            out.writeUTF(layout.kind().name()); bounds(out, layout.plot()); bounds(out, layout.structure());
            position(out, layout.entrance()); out.writeInt(layout.shared().size());
            for (var space : layout.shared()) bounds(out, space);
            out.writeInt(layout.units().size());
            for (var unit : layout.units()) {
                uuid(out, unit.id()); position(out, unit.entrance()); out.writeInt(unit.spaces().size());
                for (var space : unit.spaces()) bounds(out, space);
                out.writeInt(unit.beds().size());
                for (var bed : unit.beds()) { position(out, bed.foot()); out.writeUTF(bed.facing().name()); }
            }
            out.flush(); if (bytes.size() > MAX_BYTES) throw new IllegalArgumentException("Oversized building layout");
            return bytes.toByteArray();
        } catch (IOException impossible) { throw new UncheckedIOException(impossible); }
    }
    public static BuildingLayout decode(byte[] bytes) throws IOException {
        Objects.requireNonNull(bytes);
        if (bytes.length > MAX_BYTES) throw new IOException("Oversized building layout");
        try {
            var in = new DataInputStream(new ByteArrayInputStream(bytes));
            if (in.readInt() != 1) throw new IOException("Unsupported building layout version");
            UUID id = uuid(in), settlement = uuid(in), world = uuid(in); var kind = Kind.valueOf(in.readUTF());
            Bounds plot = bounds(in), structure = bounds(in); var entrance = position(in);
            int sharedCount = count(in, BuildingLayout.MAX_SHARED); var shared = new ArrayList<Bounds>();
            for (int i = 0; i < sharedCount; i++) shared.add(bounds(in));
            int unitCount = count(in, BuildingLayout.MAX_UNITS); var units = new ArrayList<Unit>(); int totalBeds = 0;
            for (int i = 0; i < unitCount; i++) {
                UUID unit = uuid(in); var entry = position(in);
                int spaceCount = count(in, BuildingLayout.MAX_SPACES); var spaces = new ArrayList<Bounds>();
                for (int j = 0; j < spaceCount; j++) spaces.add(bounds(in));
                int bedCount = count(in, BuildingLayout.MAX_BEDS); totalBeds += bedCount;
                if (totalBeds > BuildingLayout.MAX_BEDS) throw new IOException("Building bed budget exceeded");
                var beds = new ArrayList<Bed>();
                for (int j = 0; j < bedCount; j++) beds.add(new Bed(position(in), Direction.valueOf(in.readUTF())));
                units.add(new Unit(unit, entry, spaces, beds));
            }
            if (in.available() != 0) throw new IOException("Trailing building layout data");
            var layout = new BuildingLayout(id, settlement, world, kind, plot, structure, entrance, shared, units);
            if (!Arrays.equals(bytes, encode(layout))) throw new IOException("Non-canonical building layout");
            return layout;
        } catch (IllegalArgumentException | ArithmeticException invalid) { throw new IOException("Invalid building layout", invalid); }
    }
    private static int count(DataInputStream in, int max) throws IOException {
        int n = in.readInt(); if (n < 0 || n > max) throw new IOException("Invalid building layout count"); return n;
    }
    private static void uuid(DataOutputStream out, UUID id) throws IOException { out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits()); }
    private static UUID uuid(DataInputStream in) throws IOException { return new UUID(in.readLong(), in.readLong()); }
    private static void position(DataOutputStream out, Position p) throws IOException { out.writeInt(p.x()); out.writeInt(p.y()); out.writeInt(p.z()); }
    private static Position position(DataInputStream in) throws IOException { return new Position(in.readInt(), in.readInt(), in.readInt()); }
    private static void bounds(DataOutputStream out, Bounds b) throws IOException { position(out, b.min()); position(out, b.max()); }
    private static Bounds bounds(DataInputStream in) throws IOException { return new Bounds(position(in), position(in)); }
}
