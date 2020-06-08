import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    static int counter = 0;
    static int relativeCounter = 0;
    static int absoluteCounter = 0;
    static final int EMAIL_MAX = 10_000;
    private static final int THREAD_MAX = 750;
    static final Set<String> emailList = Collections.synchronizedSet(new HashSet<String>(1_500));
    static final Set<String> uploadedEmails = Collections.synchronizedSet(new HashSet<>(13_500));
    static final Set<String> urlList = Collections.synchronizedSet(new HashSet<>(1_000));
    static final Map<String, Integer> deadEnds = Collections.synchronizedMap(new HashMap<>(1_000));
    static Set<String> usedUrls = Collections.synchronizedSet(new HashSet<>(300_000));
    private static long startTime = System.nanoTime();
    private static int totalEmails = 0;


    public Main() {
    }

    public static void main(String[] args) throws ConcurrentModificationException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_MAX);
        urlList.add("https://touro.edu/");


        while (totalEmails <= EMAIL_MAX){
            synchronized (urlList) {
                Iterator<String> itr = urlList.iterator();
                while (itr.hasNext()) {
                    String url = itr.next();
                    executor.execute(new EmailScraper(url));
                    itr.remove();
                    usedUrls.add(url);
                }
            }
            totalEmails = emailList.size() + uploadedEmails.size();
            System.out.println("Emails: " + totalEmails);
            System.out.println("URLs: " + usedUrls.size());
        }
        System.out.println("Closed ");
        if(emailList.size() > 0) {
            uploadToDB();
        }
        System.out.println("URL Count: " + Main.counter);
        System.out.println("Absolute: " + Main.absoluteCounter);
        System.out.println("Relative: " + Main.relativeCounter);
        long endTime = System.nanoTime();
        long timeTook = (endTime - Main.startTime)/1_000_000_000;
        if(timeTook < 60){
            System.out.println("Time took: " + timeTook + " seconds");
        }
        else {
            System.out.println("Time took: " + timeTook/60 + " minutes");
        }
        executor.shutdownNow();

        System.exit(0);
    }

    public static void uploadToDB(){

        String connectionURL = "jdbc:sqlserver://database-1.cbjmpwcdjfmq.us-east-1.rds.amazonaws.com:1433;databaseName=vanderhoof;user=admin;password=mco368Touro";
        try (Connection con = DriverManager.getConnection(connectionURL);
             PreparedStatement ps = con.prepareStatement("INSERT INTO Emails VALUES (?)")) {
            con.setAutoCommit(false);
            synchronized (Main.emailList) {
                Iterator<String> itr = Main.emailList.iterator();
                while (itr.hasNext()) {
                    String email = itr.next();
                    itr.remove();
                    if(!Main.uploadedEmails.contains(email)) {
                        ps.setString(1, email);
                        ps.addBatch();
                        Main.uploadedEmails.add(email);
                    }
                }
                ps.executeBatch();
                con.commit();
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }

    }
}
