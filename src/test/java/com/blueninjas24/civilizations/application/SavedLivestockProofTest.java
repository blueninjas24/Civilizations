package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SavedLivestockProofTest {
    @TempDir Path directory;
    PendingLivestockSale receipt(Product product, PendingLivestockSale.Stage stage) {
        return new PendingLivestockSale(new Terms(new UUID(1, 1), new UUID(2, 2), new UUID(3, 3),
                new UUID(4, 4), new UUID(5, 5), product, 1, 3, 800), stage);
    }
    Path file(PendingLivestockSale receipt) { return directory.resolve(receipt.terms().player() + ".dat"); }
    void write(PendingLivestockSale receipt, String key, boolean nested, boolean duplicate) throws IOException {
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(file(receipt))); var out = new DataOutputStream(gzip)) {
            out.writeByte(10); out.writeUTF("");
            if (nested) { out.writeByte(10); out.writeUTF("Inventory"); }
            out.writeByte(10); out.writeUTF("BukkitValues");
            out.writeByte(8); out.writeUTF(key); out.writeUTF(receipt.encode());
            if (duplicate) { out.writeByte(8); out.writeUTF(key); out.writeUTF(receipt.encode()); }
            out.writeByte(0); if (nested) out.writeByte(0); out.writeByte(0);
        }
    }
    boolean proof(PendingLivestockSale receipt) throws IOException {
        return SavedAidProof.contains(directory, receipt.terms().player(), PendingLivestockSale.KEY, receipt.encode());
    }
    @Test void onlyExactSavedPlayerProductAndStageProvesThisReceipt() throws Exception {
        for (var product : Product.values()) for (var stage : PendingLivestockSale.Stage.values()) {
            var receipt = receipt(product, stage); Files.deleteIfExists(file(receipt)); assertFalse(proof(receipt));
            write(receipt, PendingLivestockSale.KEY, false, false); assertTrue(proof(receipt));
            var otherStage = stage == PendingLivestockSale.Stage.PREPARED ? PendingLivestockSale.Stage.EXCHANGED : PendingLivestockSale.Stage.PREPARED;
            assertFalse(proof(receipt.withStage(otherStage)));
            assertFalse(proof(receipt(product == Product.MILK ? Product.EGGS : Product.MILK, stage)));
            assertFalse(SavedAidProof.contains(directory, new UUID(9, 9), PendingLivestockSale.KEY, receipt.encode()));
        }
    }
    @Test void nestedOrOtherReceiptKeysDoNotProveLivestockAndArbitraryKeysRemainForbidden() throws Exception {
        var receipt = receipt(Product.MILK, PendingLivestockSale.Stage.EXCHANGED);
        write(receipt, PendingLivestockSale.KEY, true, false); assertFalse(proof(receipt));
        for (var key : List.of(SavedAidProof.KEY, PendingFoodPurchase.KEY, PendingFoodBarter.KEY, "BukkitValues/" + PendingLivestockSale.KEY)) {
            write(receipt, key, false, false); assertFalse(proof(receipt));
        }
        write(receipt, "civilizations:invented_payment", false, false);
        assertThrows(IllegalArgumentException.class, () -> SavedAidProof.contains(directory, receipt.terms().player(), "civilizations:invented_payment", receipt.encode()));
    }
    @Test void duplicateCorruptedAndTruncatedSavesCannotConfirmDelivery() throws Exception {
        var receipt = receipt(Product.EGGS, PendingLivestockSale.Stage.EXCHANGED);
        write(receipt, PendingLivestockSale.KEY, false, true); assertThrows(IOException.class, () -> proof(receipt));
        write(receipt, PendingLivestockSale.KEY, false, false); var bytes = Files.readAllBytes(file(receipt));
        Files.write(file(receipt), Arrays.copyOf(bytes, bytes.length - 4)); assertThrows(IOException.class, () -> proof(receipt));
        Files.writeString(file(receipt), "not compressed player NBT"); assertThrows(IOException.class, () -> proof(receipt));
    }
}
