package main;

import classifier.Classifier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import jgit_utilities.GitSearcher;
import me.tongfei.progressbar.ProgressBar;
import metrics.DatasetBuilder;
import model.Issue;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.Utils.average;
import static utils.Utils.fillAffectedVersionList;

public class RetrieveTicketsID {

    public static final String PROJECT_NAME = "OPENJPA";
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

    /* Effettua la query in GET per il retrieve dei ticket da Jira e prende l'array delle versioni */
    public static void getVersionsWithReleaseDate() throws IOException, JSONException {

        String releaseName;
        Integer i;

        String url = "https://issues.apache.org/jira/rest/api/2/project/" + PROJECT_NAME;

        JSONObject json = readJsonFromUrl( url );

        JSONArray versions = json.getJSONArray( "versions" );

        // Per ogni versione controlla se questa abbia una release date e un nome e la aggiunge alla
        // mappa (LocalDate,String) @version_map
        for ( i = 0; i < versions.length(); i++ ) {
            if ( versions.getJSONObject(i).has( RELEASE_DATE ) && versions.getJSONObject(i).has("name") ) {
                releaseName = versions.getJSONObject(i).get("name").toString();
                version_map.put(LocalDate.parse(versions.getJSONObject(i).get( RELEASE_DATE ).toString()) , releaseName);
            }
        }

        // Assegna un indice intero ad ogni release nella lista in modo da poter essere utilizzato in seguito
        int releaseNumber = 1;
        for ( LocalDate k : version_map.keySet() ) {
            version_map.put(k, String.valueOf( releaseNumber ));
            releaseNumber++;
        }

        datasetBuilder = new DatasetBuilder( version_map , PROJECT_NAME);
    }


    public static List<String> getJsonAffectedVersionList(JSONArray avArray) throws JSONException {
        /*
            Questo metodo va a prendere tutte le affected version associate al ticket tale per cui sia associata
            una releaseDate, e ritorna la lista di AV relative al quel task
        */
        ArrayList<String> affectedVersions = new ArrayList<>();
        if ( avArray.length() > 0 ) {
            for (int k = 0; k < avArray.length(); k++) {
                JSONObject singleRelease = avArray.getJSONObject(k);
                if (singleRelease.has(RELEASE_DATE)) {
                    affectedVersions.add(singleRelease.getString("name"));
                }
            }
        }
        return affectedVersions;
    }


    public static void getTickets() throws IOException, JSONException {
        /*
            Questo metodo va ad effettuare la query verso Jira facendo il retrieve dei ticket
        */
        Integer j = 0;
        Integer i = 0;
        Integer total = 1;

        do {
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22" + PROJECT_NAME
                    + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,versions,resolutiondate,created,fixVersions&startAt="
                    + i.toString() + "&maxResults=1000";

            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");

            // Per ogni ticket chiuso o risolto di tipo bug vado a prendere la chiave
            for (; i < total && i < j; i++) {

                String key = ( issues.getJSONObject(i % 1000).get("key").toString() ).split("-")[1];

                JSONObject currentIssue = (JSONObject) issues.getJSONObject(i % 1000).get("fields");

                String resolutionDate = currentIssue.getString("resolutiondate").split("T")[0];

                String creationDate = currentIssue.getString("created").split("T")[0];

                // Prende il JSONArray associato alle affected version
                JSONArray avArray = currentIssue.getJSONArray("versions");
                List<String> avList = getJsonAffectedVersionList( avArray );

                Issue issue = new Issue( key, resolutionDate, creationDate, avList);

                list_of_issues.add(issue);

            }
        } while (i < total);

    }

    public static void getCommits() throws IOException, JSONException, GitAPIException {
        /*
            Questo metodo scorre la lista delle issue e va a fare il retrieve di tutte le grandezze necessarie a
            calcolare le metode e aggiunge la lista dei commit alle Issue relative
        */

        ProgressBar pb = new ProgressBar("SCANNING TICKET", list_of_issues.size());
        pb.start();
        for(Issue issue : list_of_issues){
            pb.step();
            issue.setCommits(GitSearcher.getAllCommitDetails(GitSearcher.openJGitRepository(), issue.getIssueKey(), issue));
            pb.setExtraMessage("<- Reading commits ->" + issue.getIssueKey());
        }
        pb.stop();

    }


    public static void setOpeningAndFixedVersions(){
        /*
            Va ad impostare la fix version e la opening version sottoforma di indici interi e salvata all'interno
            dell'oggetto issue. Per la fixversion andiamo ad utilizzare la resolution date, mentre per quanto riguarda
            la opening version andiamo ad utilizzare la creation date.
        */
        for (Issue issue : list_of_issues){
            // Fix Version
            for(LocalDate date : version_map.keySet()){
                issue.setFixVersion(Integer.parseInt(Iterables.getLast(version_map.get(date))));
                if (date.isEqual(LocalDate.parse(issue.getResolutionDate())) || date.isAfter(LocalDate.parse(issue.getResolutionDate())) ){
                    break;
                }
            }

            // Opening Version
            for(LocalDate date : version_map.keySet()){
                issue.setOpeningVersion(Integer.parseInt(Iterables.getLast(version_map.get(date))));
                if (date.isEqual(LocalDate.parse(issue.getCreationDate())) || date.isAfter(LocalDate.parse(issue.getCreationDate()))){
                    break;
                }
            }
        }
    }


    public static void setAffectedVersionsAV(){
        /*
            Va ad impostare le affected versions come indice per ogni issue che abbia l'informazione indicata in Jira.
            Vengono inoltre popolate due liste separate di issue che hanno e non hanno delle AV indicate.
        */
        for (Issue issue : list_of_issues){
            if (issue.getAffectedVersion().isEmpty()){
                list_of_issues_without_AV.add( issue );
            }
            else{
                // AV in questo caso è indicata
                ArrayList<Integer> avs = new ArrayList<>();
                List<String> affectedVersions = issue.getAffectedVersion();
                avs = (ArrayList<Integer>) fillAffectedVersionList(affectedVersions, version_map);

                // Controlliamo che la affected version sia coerente, ovvero che non sia >= della fix version
                avs.removeIf( av -> av >= issue.getFixVersion());
                if (avs.isEmpty()) {
                    list_of_issues_without_AV.add( issue );
                }
                else{
                    // Se quindi sopravvive al controllo di cui sopra andiamo ad aggiungere l'issue alla lista di quelle
                    // che hanno una AV oltre ad assegnarla all'oggetto issue
                    int minValue = Collections.min(avs);
                    int maxValue = (issue.getFixVersion() - 1);
                    avs = new ArrayList<>(IntStream.rangeClosed(minValue, maxValue).boxed().collect(Collectors.toList()));
                    issue.setAffectedVersionIndex(avs);
                    list_of_issues_with_AV.add( issue );
                }
            }
        }
    }

    public static void setInjectedVersionAV(){
        /*
            La injected version viene impostata come il minimo tra tutte le affected version dichiarate nella issue
        */
        int iv;
        for (Issue issue : list_of_issues_with_AV){
            iv = Collections.min(issue.getAffectedVersionIndex());
            issue.setInjectedVersion(iv);
        }
    }

    public static void computeProportionIncremental(List<Issue> issues){
        /*
            Effettua il calcolo della proportion, nello specifico andando ad indicare P come la media delle
            versioni precedenti considerando le issue che hanno le affected e le injection version.
        */
        ArrayList<Double> proportions = new ArrayList<>();
        for (Issue issue : issues){
            if (issue.getOpeningVersion() != issue.getFixVersion()) {
                double fv = issue.getFixVersion();
                double ov = issue.getOpeningVersion();
                double iv = issue.getInjectedVersion();
                double p = (fv - iv)/(fv - ov);
                proportions.add( p );
            }
        }
        p = average( proportions );
    }

    public static void setAffectedAndInjectedVersionsP(List<Issue> issues){
        /*
            Andiamo ad assegnare la affected e la injected version stimata sfruttando il valore di P precedentemente
            calcolato su tutte le issue di cui avevamo le affected version.
        */
        for ( Issue issue : issues ){
            int fv = issue.getFixVersion();
            int ov = issue.getOpeningVersion();
            int pInt = (int) p;
            issue.setInjectedVersion(fv - ((fv - ov) * pInt));

            int minAVValue = issue.getInjectedVersion();
            int maxAVValue = issue.getFixVersion() - 1;
            // Come affected version imposto la lista di versioni che sono incluse tra l'injected e la fix version-1
            issue.setAffectedVersionIndex(new ArrayList<>( IntStream.rangeClosed(minAVValue, maxAVValue).boxed().collect(Collectors.toList()) ));
        }
    }

    public static void removeIssuesWithoutCommits(){
        /*
            Rimuove le issue senza commit
        */
        list_of_issues.removeIf( issue -> issue.getCommits().isEmpty() );
    }


    public static int getVersionFromLocalDate( LocalDate localDate ){
        /*
            Questo metodo è utilizzato dai Commit per ricavare la loro versione attraverso la commitDate
        */
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
    /*
        Questo metodo permette il caricamento e il computo delle metriche attraverso i dati conservati negli oggetti
        CommitFileDetails e la conseguente scrittura sul CSV
    */
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
        }

        Classifier.train();

    }


}
