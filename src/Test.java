
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import org.phoenix.assetdatabase.AssetDatabase;
import org.phoenix.assetdatabase.AssetDatabaseImpl;
import org.phoenix.assetdatabase.IndexEntry;
import org.phoenix.assetdatabase.Subfile;
import org.phoenix.assetdatabase.TypeGroupInstance;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Vince
 */
public class Test {

    private static final TypeGroupInstance TEST_TGI = new TypeGroupInstance(0x5EB4B100, 0xDEADBEEF, 0x12345678_9ABCDEF0L);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
//        System.out.println("SAVE TEST BEGIN");
//        saveTest();
//        System.out.println("SAVE TEST END");
//        try {
//            Thread.sleep(1000);
//        } catch (Exception e) {
//        }
        System.out.println("LOAD TEST BEGIN");
        loadTest();
        System.out.println("LOAD TEST END");

    }

    private static void saveTest() throws IOException {
        AssetDatabase ad = new AssetDatabaseImpl(Paths.get("C:\\Users\\Vince\\My Documents\\NetBeansProjects_8\\PhoenixAssetDatabase\\test\\test.pad"));
        byte[] data = "RData test".getBytes();
        IndexEntry ie = new IndexEntry(TEST_TGI);
        Subfile sf = new Subfile();
        sf.getMetadata().put("LastModified", new Date().toString());
        sf.setData(data, true);
        ad.putSubfile(ie, sf);
        
        byte[] data2 = "Derp herp derp".getBytes();
        IndexEntry ie2 = new IndexEntry(new TypeGroupInstance(0xFFFFFFFF, 0xAAAAAAAA, 0xBBBBBBBDDDDDDDDL));
        Subfile sf2 = new Subfile();
        sf2.getMetadata().put("Author", "bluestorm96€");
        sf2.setData(data2, true);
        ad.putSubfile(ie2, sf2);
        
        ad.getMetadata().put("Title", "Test database!");
        
        ad.save();
    }

    private static void loadTest() throws IOException {
        AssetDatabase ad = new AssetDatabaseImpl(Paths.get("C:\\Users\\Vince\\My Documents\\NetBeansProjects_8\\PhoenixAssetDatabase\\test\\test.pad"));
        ad.load();
        System.out.println("Metadata: " + ad.getMetadata().toString());
        for (IndexEntry ie : ad.getIndex().getEntries()) {
            Subfile sf = ad.loadSubfile(ie.getTgi());
            System.out.println("Entry " + ie.getTgi() + " at pos " + ie.getFileOffset() + " size " + ie.getFileSize());
            System.out.println("Metadata: " + sf.getMetadata().toString());
            System.out.println("Data: " + new String(sf.getData()));
        }
    }
}
