import java.io.IOException;
import java.util.*;
import java.util.regex.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EmailScraper implements Runnable{
    private String url;

    public EmailScraper(String startingUrl){
        url = startingUrl;
    }

    @Override
    public void run() {
        String emailRegex = "(\\w+\\.?\\w*@[a-zA-Z]+(\\.[a-zA-Z]+)+)";
        String urlRegex = "((https:?/{1,2}(([^/\"]*\\.)?([^/\"]+\\.?[^/\"]{3})))(/[^\"]*)*\\??[^\"]*=?[^\"]*)";
        String relativeUrlRegex = "^(/{1}[^/.\"]+(/[^\"]*)*)$";
        int MAX_DEAD_ENDS = 5;

        if(Main.emailList.size() > Main.EMAIL_MAX/10){
            Main.uploadToDB();
        }
        else {
            try {
                HashSet<String> hrefs = new HashSet<>();
                Document doc = Jsoup.connect(url).userAgent("Web Crawler").timeout(0).ignoreHttpErrors(true).get();
                Elements links = doc.select("a");

                Matcher emailMatcher = Pattern.compile(emailRegex).matcher(doc.toString());
                Matcher urlMatcher = Pattern.compile(urlRegex).matcher(url);
                if (!emailMatcher.find()) {
                    while (urlMatcher.find()) {
                        int count = Main.deadEnds.getOrDefault(urlMatcher.group(5), 0);
                        Main.deadEnds.put(urlMatcher.group(5), count + 1);
                    }
                } else {
                    while (urlMatcher.find()) {
                        int count = Main.deadEnds.getOrDefault(urlMatcher.group(5), 0);
                        Main.deadEnds.put(urlMatcher.group(5), count - 1);
                    }
                }

                while (emailMatcher.find()) {
                    String email = emailMatcher.group(1).toLowerCase();
                    if (!Main.uploadedEmails.contains(email)) {
                        Main.emailList.add(email);
                    }
                }
                for (Element link : links) {
                    hrefs.add(link.attr("href"));
                }
                for (String link : hrefs) {
                    Matcher absUrlMatch = Pattern.compile(urlRegex).matcher(link);
                    Matcher relUrlMatch = Pattern.compile(relativeUrlRegex).matcher(link);

                    while (absUrlMatch.find()) {
                        String urlToAdd = absUrlMatch.group(1);
                        boolean isSupported = true;
                        ArrayList<String> unsupportedTypes = new ArrayList<String>(Arrays.asList(".pdf", ".jpg", ".png", ".zip", ".wmv", ".exe", ".mp3", ".mp4", ".pdf"));
                        if (!(urlToAdd.lastIndexOf(".") == -1) && urlToAdd.lastIndexOf(".") + 4 <= urlToAdd.toCharArray().length) {
                            String end = urlToAdd.substring(urlToAdd.lastIndexOf("."), urlToAdd.lastIndexOf(".") + 4);
                            isSupported = !unsupportedTypes.contains(end);
                        }
                        if (!Main.usedUrls.contains(urlToAdd) && isSupported && Main.deadEnds.getOrDefault(absUrlMatch.group(5), 0) < MAX_DEAD_ENDS) {
                            Main.urlList.add(urlToAdd);
                            Main.counter++;
                            Main.absoluteCounter++;
                        }
                    }

                    // I commented this out because it increased my run time by a factor of ~10

//                while (relUrlMatch.find()){
//                    Matcher domain = Pattern.compile(urlRegex).matcher(url);
//                    while (domain.find()){
//                        String completeUrl = domain.group(2) + relUrlMatch.group(1);
//                        System.out.println(completeUrl);
//                        boolean isSupported = true;
//                        ArrayList<String> unsupportedTypes = new ArrayList<String>(Arrays.asList(".pdf", ".jpg", ".png", ".zip", ".wmv", ".exe", ".mp3", ".mp4", ".pdf"));
//                        if(!(completeUrl.lastIndexOf(".") == -1) && completeUrl.lastIndexOf(".") + 4 <= completeUrl.toCharArray().length) {
//                            String end = completeUrl.substring(completeUrl.lastIndexOf("."), completeUrl.lastIndexOf(".") + 4);
//                            isSupported = !unsupportedTypes.contains(end);
//                        }
//                        if (!Main.usedUrls.contains(completeUrl) && isSupported && Main.deadEnds.getOrDefault(absUrlMatch.group(5), 0) < 10) {
//                            Main.urlList.add(completeUrl);
//                            Main.counter++;
//                            Main.relativeCounter++;
//                        }
//                    }
//                }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}
