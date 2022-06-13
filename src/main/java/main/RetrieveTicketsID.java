package main;

import classifier.Classifier;
import com.google.common.collect.Iterables;
import com.google.common.collect.MultimapBuilder;
import jgit_utilities.GitSearcher;
import me.tongfei.progressbar.ProgressBar;
import metrics.DatasetBuilder;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.Utils.fillAffectedVersionList;

public class RetrieveTicketsID {

    public static final String PROJECT_NAME ="STORM";
    private static final Multimap<LocalDate, String> version_map =  MultimapBuilder.treeKeys().linkedListValues().build();
    private static final List<Issue> list_of_issues = new ArrayList<>();
    private static final List<Issue> list_of_issues_with_AV = new ArrayList<>();
    private static final List<Issue> list_of_issues_without_AV = new ArrayList<>();

    private static double p;
    public static final String RELEASE_DATE = "releaseDate";

    private static DatasetBuilder datasetBuilder;

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
    public static List<String> getJsonAffectedVersionList(JSONArray avArray) throws JSONException {
        ArrayList<String> affectedVersions = new ArrayList<>();
        if ( avArray.length() > 0 ) {
            // For each release in the AV version
            for (int k = 0; k < avArray.length(); k++) {
                JSONObject singleRelease = avArray.getJSONObject(k);
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

            // For each closed ticket get the key of the ticket
            for (; i < total && i < j; i++) {

                String key = ( issues.getJSONObject(i % 1000).get("key").toString() ).split("-")[1];

                JSONObject currentIssue = (JSONObject) issues.getJSONObject(i % 1000).get("fields");

                String resolutionDate = currentIssue.getString("resolutiondate").split("T")[0];

                String creationDate = currentIssue.getString("created").split("T")[0];

                // get JSONArray associated to the affected versions.
                JSONArray avArray = currentIssue.getJSONArray("versions");

                // Get a List from the JSONArray with only dated affected versions.
                List<String> avList = getJsonAffectedVersionList( avArray );

                Issue issue = new Issue( key, resolutionDate, creationDate, avList);

                list_of_issues.add(issue);

            }
        } while (i < total);

    }

    public static void getCommits() throws IOException, JSONException, GitAPIException {
        ProgressBar pb = new ProgressBar("SCANNING TICKET", list_of_issues.size()); // name, initial max
        pb.start();
        for(Issue issue : list_of_issues){
            pb.step();
            issue.setCommits(GitSearcher.getAllCommitDetails(GitSearcher.openJGitRepository(), issue.getIssueKey(), issue));
            pb.setExtraMessage("<- Reading commits ->" + issue.getIssueKey());
        }
        pb.stop();

    }

    public static double average( List<Double> array ){
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
                issue.setFixVersion(Integer.parseInt( Iterables.getLast( version_map.get(date) )));
                if ( date.isEqual( LocalDate.parse( issue.getResolutionDate() ) ) || date.isAfter( LocalDate.parse(issue.getResolutionDate()) ) ){
                    break;
                }
            }
            for( LocalDate date : version_map.keySet()){
                issue.setOpeningVersion(Integer.parseInt( Iterables.getLast( version_map.get(date) )));
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
            if ( issue.getAffectedVersion().isEmpty() ){
                list_of_issues_without_AV.add( issue );
            }
            else{
                ArrayList<Integer> avs = new ArrayList<>();
                List<String> affectedVersions = issue.getAffectedVersion();
                avs = (ArrayList<Integer>) fillAffectedVersionList(affectedVersions, version_map);
                // Check if the reported affected versions for the current issue are not coherent
                // with the reported fixed version ( i.e. av > fv ).
                avs.removeIf( av -> av >= issue.getFixVersion());
                if ( avs.isEmpty() ) {
                    list_of_issues_without_AV.add( issue );
                }
                else{
                    int minValue = Collections.min( avs );
                    int maxValue = ( issue.getFixVersion() - 1 );
                    avs = new ArrayList<>( IntStream.rangeClosed(minValue, maxValue).boxed().collect(Collectors.toList()) );
                    issue.setAffectedVersionIndex(avs);
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
            iv = Collections.min( issue.getAffectedVersionIndex());
            issue.setInjectedVersion(iv);
        }
    }

    public static void computeProportionIncremental(List<Issue> issues){
        ArrayList<Double> proportions = new ArrayList<>();
        for ( Issue issue : issues ){
            if ( issue.getOpeningVersion() != issue.getFixVersion()) {
                double fv = issue.getFixVersion();
                double ov = issue.getOpeningVersion();
                double iv = issue.getInjectedVersion();
                double p = ( fv - iv )/( fv - ov );
                proportions.add( p );
            }
        }
        p = average( proportions );
    }

    public static void setAffectedAndInjectedVersionsP(List<Issue> issues){
        for ( Issue issue : issues ){
            int fv = issue.getFixVersion();
            int ov = issue.getOpeningVersion();
            int pInt = (int) p;
            if ( fv == ov ) {
                issue.setInjectedVersion(fv - pInt);
            } else{
                issue.setInjectedVersion(fv - ((fv - ov) * pInt));
            }
            int minAVValue = issue.getInjectedVersion();
            int maxAVValue = issue.getFixVersion() - 1;
            issue.setAffectedVersionIndex(new ArrayList<>( IntStream.rangeClosed(minAVValue, maxAVValue).boxed().collect(Collectors.toList()) ));
        }
    }

    /* This Method is used to clean the issues array from all issue tickets that have no related commits. */
    public static void removeIssuesWithoutCommits(){
        list_of_issues.removeIf( issue -> issue.getCommits().isEmpty() );
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
        boolean train = true;

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

            Utils.printFullInfoFromTicket(list_of_issues);
        }

        Classifier.train();

    }


}
