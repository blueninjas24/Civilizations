package com.blueninjas24.civilizations.application;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/** Read-only pinned Minecraft NBT adapter. A void saveData return is never proof of a saved debit. */
public final class SavedAidProof {
    public static final String KEY = "civilizations:pending_food_aid";
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private SavedAidProof() {}
    public static boolean contains(Path playerDirectory, PendingFoodAid receipt) throws IOException {
        return contains(playerDirectory, receipt.player(), KEY, receipt.encode());
    }
    /** Full-file proof for a supported root BukkitValues receipt; no inventory mutation. */
    public static boolean contains(Path playerDirectory, UUID player, String key, String expected) throws IOException {
        if (!key.equals(KEY) && !key.equals(PendingFoodPurchase.KEY) && !key.equals(PendingFoodBarter.KEY)
                && !key.equals(PendingLivestockSale.KEY)) throw new IllegalArgumentException("Unsupported player receipt key");
        var directory=playerDirectory.toAbsolutePath().normalize();
        var file=directory.resolve(player+".dat").normalize();
        if (!file.getParent().equals(directory)) throw new IOException("Invalid player proof path");
        if (!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)) return false;
        if (Files.size(file)>MAX_BYTES) throw new IOException("Oversized saved player file");
        try(var stream=Files.newInputStream(file);var gzip=new GZIPInputStream(stream)) {
            return expected.equals(readReceipt(gzip, key));
        }
    }
    /** Parse only the root BukkitValues string; never accept an item-container or nested spoof. */
    static String readReceipt(InputStream raw) throws IOException {
        return readReceipt(raw, KEY);
    }
    private static String readReceipt(InputStream raw, String key) throws IOException {
        var limited=new FilterInputStream(raw) {
            int remaining=MAX_BYTES;
            @Override public int read() throws IOException { if(remaining==0)throw new IOException("Player NBT limit exceeded");int n=super.read();if(n>=0)remaining--;return n; }
            @Override public int read(byte[] bytes,int offset,int length) throws IOException {
                if(remaining==0)throw new IOException("Player NBT limit exceeded");int n=super.read(bytes,offset,Math.min(length,remaining));if(n>0)remaining-=n;return n;
            }
        };
        var input=new DataInputStream(limited);
        if(input.readUnsignedByte()!=10)throw new IOException("Unsupported player NBT root");input.readUTF();
        var reader=new Reader(input, key);reader.payload(10,0,"");if(input.read()!=-1)throw new IOException("Trailing player NBT data");return reader.receipt;
    }
    private static final class Reader {
        private final DataInputStream input;
        private String receipt;
        private int tags;
        private final String key;
        Reader(DataInputStream input, String key){this.input=input;this.key=key;}
        private int length() throws IOException {int n=input.readInt();if(n<0||n>MAX_BYTES)throw new IOException("Invalid NBT length");return n;}
        private void discard(long bytes) throws IOException {
            if(bytes<0||bytes>MAX_BYTES)throw new IOException("NBT payload too large");
            var buffer=new byte[4096];while(bytes>0){int n=input.read(buffer,0,(int)Math.min(bytes,buffer.length));if(n<0)throw new EOFException();bytes-=n;}
        }
        void payload(int type,int depth,String path) throws IOException {
            if(depth>64||path.length()>1024||++tags>100000)throw new IOException("Player NBT nesting/tag limit exceeded");
            switch(type) {
                case 1 -> input.readByte(); case 2 -> input.readShort(); case 3,5 -> input.readInt(); case 4,6 -> input.readLong();
                case 7 -> discard(length());
                case 8 -> {var value=input.readUTF();if(depth==2&&path.equals("BukkitValues/"+key)){if(receipt!=null)throw new IOException("Duplicate player proof");receipt=value;}}
                case 9 -> {int element=input.readUnsignedByte(),size=length();if(size>100000||element==0&&size>0)throw new IOException("Invalid NBT list");for(int i=0;i<size;i++)payload(element,depth+1,path+"/[]");}
                case 10 -> {int next;while((next=input.readUnsignedByte())!=0){var name=input.readUTF();payload(next,depth+1,path.isEmpty()?name:path+"/"+name);}}
                case 11 -> discard(Math.multiplyExact((long)length(),4)); case 12 -> discard(Math.multiplyExact((long)length(),8));
                default -> throw new IOException("Unknown player NBT tag: "+type);
            }
        }
    }
}
