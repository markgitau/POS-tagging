import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Sudi {

    private Map<String, Map<String, Double>> observations, transitions;
    private String start = "#";

    public Sudi(){

        // transitions: maps tags to next tags, each next tag with its own probability.
        transitions = new HashMap<>();

        // observations: maps tags to words, each word with its own probability.
        observations = new HashMap<>();
    }

    /**
     * reads tag file and word file, builds the transitions and observations map
     * @param wordFile file name of word file
     * @param tagFile file name of tag file
     */
    public void train(String wordFile, String tagFile){
        BufferedReader wordReader = null, tagReader = null;  // tag file reader and word file reader

        try {
            wordReader = new BufferedReader(new FileReader("PS 5/" + wordFile));
            tagReader = new BufferedReader(new FileReader("PS 5/" + tagFile));
        }

        catch (Exception e){
            System.out.println("Training file not found.\n" + e.getMessage());
        }

        String tagLine, wordLine;  // each line of the tag file and word file

        try{
            // while we have not reached the end of the files
            assert tagReader != null;
            while((tagLine = tagReader.readLine()) != null && (wordLine = wordReader.readLine()) != null){

                // split both tag lines and word lines by space
                String[] tags = tagLine.split(" ");
                String[] words = wordLine.split(" ");

                // iterate through both tag and word lists
                for (int i = 0; i < words.length; i++){
                    String prevTag;  // previous tag, to be used in transitions

                    if(i == 0){prevTag = start;}  //start

                    else {prevTag = tags[i - 1];}

                    // add map entry to transitions. prevTag is the key of the outer map, and current tag is the key of the inner map.
                    addToMap(transitions, prevTag, tags[i]);

                    // add map entry to transitions. current tag is the key of the outer map, and current word is the key of the inner map.
                    addToMap(observations, tags[i], words[i]);
                }
            }
        }
        catch (Exception e){
            System.out.println("Error in reading training file.\n" + e.getMessage());
        }
        // convert scores in both maps to probabilities
        normalizeValues(observations);
        normalizeValues(transitions);
    }

    /**
     * adds entries to a nested map
     * @param map map with String = key and Map<String, Double> as value
     * @param key key of outer map
     * @param value key of inner map
     */
    public void addToMap(Map<String, Map<String, Double>> map, String key, String value){
        // if the map does not contain key, add an entry with key as  the key and its value as a new map.
        if (!map.containsKey(key)){
            map.put(key, new HashMap<>());
        }

        // if the map contains the key and the inner map contains value as its key, increment the value of the inner map
        else if(map.get(key).containsKey(value)){
            map.get(key).put(value, map.get(key).get(value) + 1.0);
        }

        // if the map contains key but the inner map does not contain value, add a new entry to the inner map
        // with value as its key and 1.0 as the value
        else {
            map.get(key).put(value, 1.0);
        }
    }

    /**
     * changes all the values in the inner map to probabilities
     * @param map nested map
     */
    public void normalizeValues(Map<String, Map<String, Double>> map){

        // iterate through the keys of the outer map
        for (String key: map.keySet()){
            Double total = 0.0; // total of the values of the inner map

            // iterate through all the keys of the inner map, summing up the values
            for (String inKey: map.get(key).keySet()){
                total += map.get(key).get(inKey);
            }

            // iterate through all the keys of the inner map, dividing the values by the total
            for (String inKey: map.get(key).keySet()){
                map.get(key).put(inKey, Math.log(map.get(key).get(inKey)/total));
            }
        }
    }


    /**
     * Viterbi algorithm
     * @param sentence list of words constituting a sentence
     * @return an array of tags matching the sentence's parts of speech
     */
    public List<String> viterbiDecoder(String[] sentence){

        List<HashMap<String, String>> backTrack = new ArrayList<>(); // allows us to track most likely previous state

        List<String> mostLikelyPath = new ArrayList<>();  // most likely tags

        Map<String, Double> currScores = new  HashMap<>(), nextScores; // mapping tags to their scores

        Set<String> currStates = new HashSet<>(), nextStates; // set of all tags


        // values at start
        currScores.put(start, 0.0);
        currStates.add(start);

        double notEncountered = -100.0;  // unseen penalty

        for (int i = 0; i < sentence.length; i++){

            nextStates = new HashSet<>();
            nextScores = new HashMap<>();

            // iterate through each state in currStates
            for (String state: currStates){

                // find the state in transitions and iterate through all possible next tags
                for (String nextTag: transitions.get(state).keySet()){

                    nextStates.add(nextTag); // add the next tag to the set possible next states

                    double nextScore;

                    // if the current word exists in the map whose key is nextTag
                    if (observations.get(nextTag).containsKey(sentence[i])){

                        // nextScore = current score + transition + observation
                        nextScore = currScores.get(state) + transitions.get(state).get(nextTag) + observations.get(nextTag).get(sentence[i]);
                    }
                    // if the current word does not exist in the map whose key is nextTag
                    else {
                        // nextScore = currentScore + transition - unseen penalty
                        nextScore = currScores.get(state) + transitions.get(state).get(nextTag) + notEncountered;
                    }

                    // if nextState isn't in nextScores or nextScore > nextScores[nextState]
                    if (!nextScores.containsKey(nextTag) || (nextScore > nextScores.get(nextTag))){

                        // set the tag's score to nextScore
                        nextScores.put(nextTag, nextScore);

                        // if we are at the end of the backtrace list, add a new map to the list
                        if(backTrack.size() <= i){
                            backTrack.add(i, new HashMap<>());
                        }
                        // put the map with nextTag as key and state as value
                        backTrack.get(i).put(nextTag, state);
                    }
                }
            }

            // update currStates and currScores before the next iteration
            currStates = nextStates;
            currScores = nextScores;
        }
        // tag with the best score
        String bestTag = null;

        // initialize maxValue to the lowest number an integer can take, because logarithms are negative
        double maxValue = Integer.MIN_VALUE;

        // iterate through the currScores map looking for the entry with the highest score
        for (String tag: currScores.keySet()){
            if (currScores.get(tag) > maxValue){
                bestTag = tag;
                maxValue = currScores.get(tag);
            }
        }
        // add best scoring tag to the best path
        mostLikelyPath.add(bestTag);

        int size = backTrack.size();

        // build the best path from the tags with the highest scores
        while (size > 0){
            mostLikelyPath.add(backTrack.get(size - 1).get(bestTag));
            bestTag = backTrack.get(size - 1).get(bestTag);
            size -= 1;
        }
        Collections.reverse(mostLikelyPath);
        mostLikelyPath.remove(0);  //remove #

        return mostLikelyPath;
    }

    /**
     * file based test that evaluates the performance on a pair of test files (corresponding lines with sentences and tags).
     * @param wordFile name of the test file that contains sentences
     * @param tagFile name of the test file that contains tags
     */
    public void fileTest(String wordFile, String tagFile){
        BufferedReader wordReader = null, tagReader = null;

        try {
            wordReader = new BufferedReader(new FileReader("PS 5/" + wordFile));
            tagReader = new BufferedReader(new FileReader("PS 5/" + tagFile));
        }

        catch (Exception e){
            System.out.println("Test file not found.\n" + e);
        }

        String tagLine, wordLine;  // each line of the tag file and word file

        // number of times a tag was predicted correctly, and total number of tags predicted.
        double numCorrect = 0.0, total = 0.0;

        try{
            while ((wordLine = wordReader.readLine()) != null && (tagLine = tagReader.readLine()) != null){

                // result after running viterbi algorithm and getting the most likely tags
                List<String> result = viterbiDecoder(wordLine.split(" "));

                // actual tags to compare to our most likely tags
                String[] tags = tagLine.split(" ");

                // checking whether predicted tags and actual tags match
                for (int i = 0; i < tags.length; i++){
                    if (result.get(i).equals(tags[i])) {
                        numCorrect += 1.0; // increment if the tags match
                    }
                    total += 1.0; //increment for each tag checked
                }
            }
            double percentage = (numCorrect/total)*100;
            System.out.println("for " + wordFile + ":");
            System.out.println("Tags correct: " + (int)numCorrect + "\nTags wrong: " + (int)(total-numCorrect) + "\nPercentage score: " + Math.round(percentage*100.0)/100.0 + "%\n");
        }
        catch (Exception e){
            System.out.println("Error reading test file.\n" + e);
        }
    }

    /**
     * console-based test method to give the tags from an input line.
     */
    public void consoleTest(){
        Scanner myScanner = new Scanner(System.in);
        System.out.println("Enter a sentence:");
        String input;
        do {
            input = myScanner.nextLine();
            if (input.equals("q")){
                System.out.println("Quitting test.");
            }
            else{
                String[] sentence = input.split(" ");
                System.out.println(viterbiDecoder(sentence));
            }
        }
        while (!input.equals("q"));
    }


    public static void main(String[] args) {
        Sudi sudi = new Sudi();
        sudi.train("brown-train-sentences.txt", "brown-train-tags.txt");
        sudi.fileTest("simple-test-sentences.txt", "simple-test-tags.txt");
        sudi.fileTest("brown-test-sentences.txt", "brown-test-tags.txt");
        sudi.consoleTest();
    }

}
