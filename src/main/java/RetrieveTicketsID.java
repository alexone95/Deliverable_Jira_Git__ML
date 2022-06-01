import classifier.Classifier;
import com.google.common.collect.Iterables;
import com.google.common.collect.MultimapBuilder;
import me.tongfei.progressbar.ProgressBar;
import model.Issue;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.common.collect.Multimap;
import utils.Utils;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RetrieveTicketsID {

    public static String PROJECT_NAME ="STORM";
    private static final Multimap<LocalDate, String> version_map =  MultimapBuilder.treeKeys().linkedListValues().build();
    private static final List<Issue> list_of_issues = new ArrayList<>();
    private static final ArrayList<Issue> list_of_issues_with_AV = new ArrayList<>();
    private static final ArrayList<Issue> list_of_issues_without_AV = new ArrayList<>();

    public static double p;
    public static final String RELEASE_DATE = "releaseDate";

    public static DatasetBuilder datasetBuilder;

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        } finally {
            is.close();
        }
    }

    /* This Method performs a rest API to Jira querying for all versions with related dates for the
    specified software project. */
    public static void getVersionsWithReleaseDate() throws IOException, JSONException {

        String releaseName;
        Integer i;

        // Url for the GET request to get information associated to Jira project
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + PROJECT_NAME;

        JSONObject json = readJsonFromUrl( url );

        // Get the JSONArray associated to project version
        JSONArray versions = json.getJSONArray( "versions" );

        // For each version check if version has release date and name, and add it to relative list
        for ( i = 0; i < versions.length(); i++ ) {
            if ( versions.getJSONObject(i).has( RELEASE_DATE ) && versions.getJSONObject(i).has("name") ) {
                releaseName = versions.getJSONObject(i).get("name").toString();
                version_map.put(LocalDate.parse(versions.getJSONObject(i).get( RELEASE_DATE ).toString()) , releaseName);
            }
        }

        // Give an index to each release in the list
        int releaseNumber = 1;
        for ( LocalDate k : version_map.keySet() ) {
            version_map.put(k, String.valueOf( releaseNumber ));
            releaseNumber++;
        }

        datasetBuilder = new DatasetBuilder( version_map , PROJECT_NAME);
    }

    /* This Method takes the JSON Array containing all affected versions specified for the ticket
    and returns a String ArrayList containing only those having an associated release date. */
    public static ArrayList<String> getJsonAffectedVersionList(JSONArray AVarray) throws JSONException {
        ArrayList<String> affectedVersions = new ArrayList<>();
        if ( AVarray.length() > 0 ) {
            // For each release in the AV version
            for (int k = 0; k < AVarray.length(); k++) {
                JSONObject singleRelease = AVarray.getJSONObject(k);
                // Check if the single release has been released
                if ( singleRelease.has( RELEASE_DATE ) ) {
                    affectedVersions.add(singleRelease.getString("name"));
                }
            }
        }
        return affectedVersions;
    }


    public static void getTickets() throws IOException, JSONException {

        Integer j = 0;
        Integer i = 0;
        Integer total = 1;

        // Get JSON API for closed bugs w/ AV in the project

        do {
            // Only gets a max of 1000 at a time, so must do this multiple times if bugs > 1000

            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22" + PROJECT_NAME
                    + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,versions,resolutiondate,created,fixVersions&startAt="
                    + i.toString() + "&maxResults=1000";

            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            //total = 300;

            // For each closed ticket...
            for (; i < total && i < j; i++) {
                // ... get the key of the ticket,

                String key = ( issues.getJSONObject(i % 1000).get("key").toString() ).split("-")[1];

                JSONObject currentIssue = (JSONObject) issues.getJSONObject(i % 1000).get("fields");

                String resolutionDate = currentIssue.getString("resolutiondate").split("T")[0];

                String creationDate = currentIssue.getString("created").split("T")[0];

                // , get JSONArray associated to the affected versions.
                JSONArray AVarray = currentIssue.getJSONArray("versions");

                // Get a Java List from the JSONArray with only dated affected versions.
                ArrayList<String> AVlist = getJsonAffectedVersionList( AVarray );

                Issue issue = new Issue( key, resolutionDate, creationDate, AVlist );

                list_of_issues.add(issue);

            }
        } while (i < total);

    }

    public static void getCommits() throws IOException, JSONException, GitAPIException, ParseException {
        ProgressBar pb = new ProgressBar("SCANNING TICKET", list_of_issues.size()); // name, initial max
        pb.start();
        for(Issue issue : list_of_issues){
            pb.step();
            issue.commits = GitSearcher.getAllCommitDetails(GitSearcher.openJGitRepository(), issue.issue_key, issue);
            pb.setExtraMessage("<- Reading commits ->" + issue.issue_key);
        }
        pb.stop();

    }

    public static double average( ArrayList<Double> array ){
        double avg = 0.0;
        double sum = 0.0;
        for ( double p : array ){
            sum += p;
        }
        avg = ( sum/( array.size() ) );
        return avg;
    }

    /* This Method is used in order to set Opening and Fixed Versions for every issue
    that has been retrieved. Actually, those informations are stored into the issue object's
    state as Integer values (in the shape of version's indexes). */
    public static void setOpeningAndFixedVersions(){
        for ( Issue issue : list_of_issues ){
            for( LocalDate date : version_map.keySet()){
                issue.fix_version = Integer.parseInt( Iterables.getLast( version_map.get(date) ));
                if ( date.isEqual( LocalDate.parse( issue.getResolutionDate() ) ) || date.isAfter( LocalDate.parse(issue.getResolutionDate()) ) ){
                    break;
                }
            }
            for( LocalDate date : version_map.keySet()){
                issue.opening_version =  Integer.parseInt( Iterables.getLast( version_map.get(date) ));
                if ( date.isEqual( LocalDate.parse( issue.getCreationDate() ) ) || date.isAfter( LocalDate.parse(issue.getCreationDate()) ) ){
                    break;
                }
            }
        }
    }

    /* This Method is used in order to set the Affected Versions indexes for every issue
    that has been retrieved and that maintains the AVs information.
    Furthermore, issues with declared AVs are separated from those without them, so that
    it will be possible to handle the two groups of issues separately. */
    public static void setAffectedVersionsAV(){
        for ( Issue issue : list_of_issues ){
            // If the current issue has not specified AVs it'll be appended to the array
            // that will be used to perform the Proportion Method.
            if ( issue.getAffected_version().size() == 0 ){
                list_of_issues_without_AV.add( issue );
            }
            else{
                ArrayList<Integer> avs = new ArrayList<>();
                ArrayList<String> affectedVersions = issue.getAffected_version();
                for ( String version : affectedVersions ){
                    for( LocalDate date : version_map.keySet() ){
                        if ( Iterables.get(version_map.get(date),0).equals( version )){
                            avs.add( Integer.valueOf( Iterables.getLast( version_map.get(date) )) );
                            break;
                        }
                    }
                }
                // Check if the reported affected versions for the current issue are not coherent
                // with the reported fixed version ( i.e. av > fv ).
                avs.removeIf( av -> av >= issue.fix_version );
                if ( avs.isEmpty() ) {
                    list_of_issues_without_AV.add( issue );
                }
                else{
                    int minValue = Collections.min( avs );
                    int maxValue = ( issue.fix_version - 1 );
                    avs = new ArrayList<>( IntStream.rangeClosed(minValue, maxValue).boxed().collect(Collectors.toList()) );
                    issue.affected_version_index = avs;
                    list_of_issues_with_AV.add( issue );
                }
            }
        }
    }


    /* This Method is used to set the Injected version of every issue having declared affected
    versions. The injected version is set as the minimum between all declared affected versions. */
    public static void setInjectedVersionAV(){
        int iv;
        for ( Issue issue : list_of_issues_with_AV ){
            iv = Collections.min( issue.affected_version_index );
            issue.injected_version = iv;
        }
    }

    public static void computeProportionIncremental(ArrayList<Issue> issues){
        ArrayList<Double> proportions = new ArrayList<>();
        for ( Issue issue : issues ){
            if ( !( issue.opening_version == issue.fix_version ) ) {
                double fv = issue.fix_version;
                double ov = issue.opening_version;
                double iv = issue.injected_version;
                double p = ( fv - iv )/( fv - ov );
                proportions.add( p );
            }
        }
        p = average( proportions );
    }

    public static void setAffectedAndInjectedVersionsP(ArrayList<Issue> issues){
        for ( Issue issue : issues ){
            int fv = issue.fix_version;
            int ov = issue.opening_version;
            int P = (int) p;
            if ( fv == ov ) {
                issue.injected_version = (fv - (P));
            } else{
                issue.injected_version = (fv - (( fv - ov ) * P )) ;
            }
            int minAVValue = issue.injected_version;
            int maxAVValue = issue.fix_version - 1;
            issue.affected_version_index = new ArrayList<>( IntStream.rangeClosed(minAVValue, maxAVValue).boxed().collect(Collectors.toList()) );
        }
    }

    /* This Method is used to clean the issues array from all issue tickets that have no related commits. */
    public static void removeIssuesWithoutCommits(){
        System.out.println(list_of_issues.size());
        list_of_issues.removeIf( issue -> issue.getCommits().isEmpty() );
        System.out.println(list_of_issues.size());
    }


    /* This Method is called by Commit Objects in order to retrieve their own version
    using the value of the commit local date. */
    public static int getVersionFromLocalDate( LocalDate localDate ){
        int version = -1;
        for( LocalDate date : version_map.keySet()){
            version = Integer.parseInt( Iterables.getLast( version_map.get(date) ));
            if ( date.isEqual( localDate ) || date.isAfter( localDate ) ){
                break;
            }
        }
        return version;
    }


    public static void populateDatasetMapAndWriteToCSV() throws IOException{
        datasetBuilder.populateFileDataset(list_of_issues_with_AV);
        datasetBuilder.populateFileDataset(list_of_issues_without_AV);
        datasetBuilder.writeToCSV(PROJECT_NAME);
    }


    public static void main(String[] args) throws Exception {
        boolean train = false;

        if (train) {
            getVersionsWithReleaseDate();

            getTickets();

            setOpeningAndFixedVersions();

            setAffectedVersionsAV();

            setInjectedVersionAV();

            computeProportionIncremental(list_of_issues_with_AV);

            setAffectedAndInjectedVersionsP(list_of_issues_without_AV);

            getCommits();

            removeIssuesWithoutCommits();

            populateDatasetMapAndWriteToCSV();

            Utils.print_full_info_from_ticket(list_of_issues);
        }

        Classifier.train();

    }


}
