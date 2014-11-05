/*
* Operation: Configure the server information and administrator login below. The program will connect to the
*            server and present the user with a list of catalogs. Select the origin catalog first and the destination
*            after. The program then pages through 1000 records at a time and creates a new record in the
*            destination catalog with the correct path to each asset from the first.
*
*            After the records are 'copied' you'll still need to regen thumbs/previews and pull in metadata.
*            NONE OF THE METADATA FROM THE ORIGIN CATALOG IS COPIED - Anything that is not embedded in the file
*            will not make the transition.
*
*            Compile: javac -classpath dam-client.jar:. Base64.java Duplicate.java
*            Run: java -classpath dam-client.jar:. Duplicate
*
* SCRIPTS:
*            Compile and run: ./go
*            Compile only: ./compile
*            Run only: ./run
*
* GOAL: To create a tool that can duplicate the contents of a catalog by copying all the records
*       to another catalog.
*
* CURRENT FEATURES:
*       [x] Present a list of catalogs for a user to select from.
*
* PLANNED FEATURES:
*       [ ] If the process fails for a record, tell us which one!
*       [ ] Copy all record metadata
*       [ ] Copy galleries
*
* NOTE: This program actually works! It is also very hacky and the code is very ugly.
*       Hopefully this will change over time as I work on it and learn more about Portfolio's API.
*/

// Java imports
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.xml.namespace.QName;

// Extensis imports
import extensis.portfolio.service.asset.Asset;
import extensis.portfolio.service.asset.AssetQuery;
import extensis.portfolio.service.asset.AssetQueryResultOptions;
import extensis.portfolio.service.asset.AssetQueryResults;
import extensis.portfolio.service.asset.AssetSEI;
import extensis.portfolio.service.asset.AssetSEIService;
import extensis.portfolio.service.asset.Attribute;
import extensis.portfolio.service.asset.Catalog;
import extensis.portfolio.service.asset.KeySpecification;
import extensis.portfolio.service.asset.MultiValuedAttribute;
import extensis.portfolio.service.asset.SortOptions;

public class Duplicate {

    public static final String SERVER_ADDRESS = "asteria.extnsis.com";
    public static final int SERVER_PORT = 8090;
    public static final String USERNAME = "administrator";
    public static final String PASSWORD = "password";

    // no need to instantiate this class
    private Duplicate() {}

    public static void main(String[] args) throws Exception {

        // Create Service for accessing the Portfolio Server.
        AssetSEI service = getAssetService(SERVER_ADDRESS, SERVER_PORT);

        // Use getRSAPublicEncryptionKey to get the necessary Key for encrypting the password.
        KeySpecification ks = service.getRSAPublicEncryptionKey();
				System.out.println("Public key: " + ks + "\n");

        // Encrypt the password.
        String encryptedPassword = encryptPasswordForKeySpec(ks, PASSWORD);
				System.out.println("Encrypted password: " + encryptedPassword + "\n");

        // Pass the username and the encrypted password through the login method.
        // This will return a Session-ID, which can be used to access other API services.
        String sessionId = service.login(USERNAME, encryptedPassword);
        		System.out.println("SessionID: " + sessionId + "\n");

        // List the catalogs that are available to this user and ask which catalogs to use
        // for duplication.
        List<Catalog> catalogs = service.getCatalogs(sessionId);
        if (catalogs.size() == 0) {
            System.out.println("No catalogs available.  Go to http://" + SERVER_ADDRESS + ":8091/ to create a catalog.");
            return;
        }

        System.out.println(catalogs.size() + " catalog(s) available, please select source.");
        int i = 0;
        for (Catalog catalog : catalogs) {
            System.out.println(i + ")  " + catalog.getName());
            i++;
        }
        System.out.println();

        // get source
        int sourceCatalogIndex = StdIn.readInt();
        // System.out.println(sourceCatalogIndex);

        // get destination
        int destinationCatalogIndex = StdIn.readInt();

        // Make a new AssetQuery that will return the contents of the entire catalog
        AssetQuery assetQuery = new AssetQuery();
        AssetQueryResultOptions resultOptions = new AssetQueryResultOptions();

        int pageSize = 1000;
        int pageIndex = 0;
        String queryId = "";
        boolean paging = true;
        AssetQueryResults results = new AssetQueryResults();
        List<String> newItems = new ArrayList<String>();

        // TODO: Implement Paging here!

        // Determine what will be returned from the query with an AssetQueryResultOptions object
        resultOptions.setPageSize(pageSize);
        resultOptions.setStartingIndex(pageIndex);
        resultOptions.getFieldNames().add("Path");
        // resultOptions.getFieldNames().add("Filename");
        // resultOptions.getFieldNames().add("Directory Path");
        // resultOptions.getFieldNames().add("Cataloged");

        // Determine how this information will be sorted using a SortOptions object
        // NOTE: I'm not using sortOption during this proof-of-concept
        /* SortOptions sortOptions = new SortOptions();
        assetQuery.setSortOptions(sortOptions);
        sortOptions.setSortFieldName("Cataloged");
        sortOptions.setSortOrderAscending(true); */

        do {
           // get some results!
           results = service.getAssets(sessionId, catalogs.get(sourceCatalogIndex).getCatalogId(), assetQuery, resultOptions);
           queryId = results.getQueryCacheId(); // could be used to cache the query id and reuse it but I'm not actually doing that

           // What did we get?
           System.out.println("Found " + results.getAssets().size() + " out of " + results.getTotalNumberOfAssets() + " assets.");
           // print the results
           /* for (Asset asset : results.getAssets()) {
               System.out.println();
               System.out.println(" Path: " + getAttributeValue("Path", asset));
               // System.out.println("  Filename: " + getAttributeValue("Filename", asset));
               // System.out.println("  Directory Path: " + getAttributeValue("Directory Path", asset));
               // System.out.println("  Cataloged: " + getAttributeValue("Cataloged", asset));
           } */

           // Create a list of items to add to the destination catalog
           newItems.clear();
           for (Asset asset : results.getAssets()) {
            // newItems.add(getAttributeValue("Directory Path", asset));
               newItems.add(getAttributeValue("Path", asset));
               
           }

           // Add the Assets to our destination catalog
           List<String> assetIds = service.addAssetsByPath(sessionId, catalogs.get(destinationCatalogIndex).getCatalogId(), newItems); 

           // check to see if we're done
           if ( results.getAssets().size() < pageSize ) {
                paging = false; // Yes, we're done!
           } else {
                // update the index for the next set of results
                resultOptions.setStartingIndex(resultOptions.getStartingIndex() + pageSize);
           }
        } while (paging);
        // End of Paging block

        // When you are finished with the Portfolio server, close the session
        // by passing the Session ID to the logout method.
        service.logout(sessionId);
    }

    // Utility methods
    public static AssetSEI getAssetService(String serverAddress, int serverPort) throws Exception {
        // Create Service for accessing the Portfolio Server.
        URL serviceWsdlURL = new URL("http://" + SERVER_ADDRESS + ":" + SERVER_PORT + "/ws/1.0/AssetService?wsdl");
        QName serviceQName = new QName("http://portfolio.extensis/service/asset", "AssetSEIService");
        return new AssetSEIService(serviceWsdlURL, serviceQName).getAssetSEIPort();
    }

    public static String encryptPasswordForKeySpec(KeySpecification ksd, String password) throws Exception {
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(new BigInteger(ksd.getModulusBase16(), 16), new BigInteger(ksd.getExponent()));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey pk = keyFactory.generatePublic(keySpec);
        return Base64.encodeBytes(encrypt(pk, password.getBytes()));
    }

    private static byte[] encrypt(PublicKey pk, byte[] src) {
        try {
          Cipher cipher = Cipher.getInstance("RSA");
          cipher.init(Cipher.ENCRYPT_MODE, pk);
          return cipher.doFinal(src);
        } catch (Exception e) {
          throw new RuntimeException("error encrypting cipher data: ", e);
        }
    }

    private static String getAttributeValue(String name, Asset asset) {
        MultiValuedAttribute a = getAttribute(name, asset);
        if (a != null && a.getValues().size() > 0)
            return a.getValues().get(0);
        return null;
    }

    private static MultiValuedAttribute getAttribute(String name, Asset asset)
    {
        for (MultiValuedAttribute a : asset.getAttributes())
        {
            if (name.equals(a.getName()))
                return a;
        }
        return null;
    }
}