package com.blueninjas24.civilizations.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

public class SavedAidProofTest {
    @TempDir Path directory;
    public static void write(Path directory,PendingFoodAid receipt,String value) throws IOException {
        Files.createDirectories(directory);
        try(var gzip=new GZIPOutputStream(Files.newOutputStream(directory.resolve(receipt.player()+".dat")));var out=new DataOutputStream(gzip)){
            out.writeByte(10);out.writeUTF("");out.writeByte(10);out.writeUTF("BukkitValues");
            out.writeByte(8);out.writeUTF(SavedAidProof.KEY);out.writeUTF(value);out.writeByte(0);out.writeByte(0);
        }
    }
    private PendingFoodAid receipt(){return new PendingFoodAid(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),12,"BREAD",4);}
    @Test void receiptEncodingRetainsOriginalInputsAndRejectsUnknownInvalidFormats(){
        var receipt=receipt();assertEquals(receipt,PendingFoodAid.decode(receipt.encode()));
        assertThrows(IllegalArgumentException.class,()->PendingFoodAid.decode("aid2|unknown"));
        assertThrows(IllegalArgumentException.class,()->new PendingFoodAid(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),0,"BREAD",4));
        assertThrows(IllegalArgumentException.class,()->new PendingFoodAid(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),12,"../BREAD",4));
        assertThrows(IllegalArgumentException.class,()->new PendingFoodAid(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),12,"BREAD",65));
    }
    @Test void onlyMatchingReceiptInSavedPlayerFileProvesADebit() throws Exception {
        var receipt=receipt();assertFalse(SavedAidProof.contains(directory,receipt));write(directory,receipt,receipt.encode());assertTrue(SavedAidProof.contains(directory,receipt));
        write(directory,receipt,receipt().encode());assertFalse(SavedAidProof.contains(directory,receipt));
    }
    @Test void truncatedOrInvalidCompressedPlayerDataIsNeverProof() throws Exception {
        var receipt=receipt();write(directory,receipt,receipt.encode());var file=directory.resolve(receipt.player()+".dat");var bytes=Files.readAllBytes(file);
        Files.write(file,Arrays.copyOf(bytes,bytes.length-4));assertThrows(IOException.class,()->SavedAidProof.contains(directory,receipt));
        Files.writeString(file,"not NBT");assertThrows(IOException.class,()->SavedAidProof.contains(directory,receipt));
    }
    @Test void nestedItemAndSlashNamedSpoofsCannotActAsRootReceipts() throws Exception {
        var receipt=receipt();var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeByte(10);out.writeUTF("");out.writeByte(8);out.writeUTF("BukkitValues/"+SavedAidProof.KEY);out.writeUTF(receipt.encode());
        out.writeByte(10);out.writeUTF("Inventory");out.writeByte(10);out.writeUTF("BukkitValues");out.writeByte(8);out.writeUTF(SavedAidProof.KEY);out.writeUTF(receipt.encode());
        out.writeByte(0);out.writeByte(0);out.writeByte(0);
        assertNull(SavedAidProof.readReceipt(new ByteArrayInputStream(bytes.toByteArray())));
    }
    @Test void invalidTagsArrayLengthsAndTrailingDataFailClosed() throws Exception {
        for(int type:new int[]{7,11,12,99}){
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeByte(10);out.writeUTF("");out.writeByte(type);out.writeUTF("bad");out.writeInt(-1);
            assertThrows(IOException.class,()->SavedAidProof.readReceipt(new ByteArrayInputStream(bytes.toByteArray())));
        }
        assertThrows(IOException.class,()->SavedAidProof.readReceipt(new ByteArrayInputStream(new byte[]{10,0,0,0,1})));
    }
    @Test void nestingAndInflationAreBounded() throws Exception {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeByte(10);out.writeUTF("");
        for(int i=0;i<66;i++){out.writeByte(10);out.writeUTF("x");}assertThrows(IOException.class,()->SavedAidProof.readReceipt(new ByteArrayInputStream(bytes.toByteArray())));
        bytes.reset();out.writeByte(10);out.writeUTF("");out.writeByte(7);out.writeUTF("large");out.writeInt(16*1024*1024);out.write(new byte[16*1024*1024]);out.writeByte(0);
        assertThrows(IOException.class,()->SavedAidProof.readReceipt(new ByteArrayInputStream(bytes.toByteArray())));
    }
    @Test void ordinaryScalarListAndArrayTagsAreSkippedWithoutChangingProof() throws Exception {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeByte(10);out.writeUTF("");
        for(int type=1;type<=6;type++){out.writeByte(type);out.writeUTF("value"+type);out.write(new byte[]{0});int size=switch(type){case 1->1;case 2->2;case 3,5->4;default->8;};out.write(new byte[size-1]);}
        out.writeByte(9);out.writeUTF("list");out.writeByte(3);out.writeInt(2);out.writeLong(0);
        for(int type:new int[]{7,11,12}){out.writeByte(type);out.writeUTF("array"+type);out.writeInt(0);}out.writeByte(0);
        assertNull(SavedAidProof.readReceipt(new ByteArrayInputStream(bytes.toByteArray())));
    }
}
