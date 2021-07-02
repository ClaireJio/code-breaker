//=====================================================================================================================
// The Code Breaker
// Casey Wang, Claire Sohn
// October 28, 2020
// Java 1.8
// ====================================================================================================================
// Project Definition – The Code Breaker game asks the player to find out a predetermined code within 10 guesses.
//                      The game will give feedback regarding the number of correct colours and positions.
//                      Players should be able to play both roles of a code generator and code breaker.
//                      Applies GUI interface to create functional program with required options (P VS AI, AI VS P, GUI)
//
// Input: Require player to click on color buttons to guess in P VS AI mode
//        Require player to input hints for Al to guess in Al VS P mode (hints will either be B or W)
//
// Output: Depend on the user choice
//		   - In P VS Al mode, player will receive hints provide by the AI (hints will either be B or W)
//         - In Al VS P mode, player will receive guesses form the AI
//=====================================================================================================================
// Purpose of class:
//          AI class to compete human player by guessing the correct code through algorithm.
// Class Variables:
//			- let remainingPossibleCodes represent all remaining possible codes
//          - let guessCount represent how many guesses have been conducted in current play
//          - let isLastGuessDisplayed represent whether the feedback sent to UI supposes to be the last one from algorithm
//          - let userFeedback represent the user feedback
//          - let aiGuess represent the AI guess
//          - let inputColorMap represent the mapping between color characters and their integer representation
//=====================================================================================================================

import java.util.*;   // Collections of built-in data structure types
import java.util.concurrent.BlockingQueue;   // BlockingQueue to manage data sharing between UI thread and AI thread

class GUI_AvP {   //Public class GUI_AvP header

    private List<String> remainingPossibleCodes = new ArrayList<>();  // remaining possibilities. Starting with 6^4 elements, then gradually reduced after each guess feedback.
    private int guessCount = 1;  // # of guesses have been conducted in current play. The first guess is always fixed to GGRR, so that the initial value is 1
    private boolean isLastGuessDisplayed = false;  // flag to indicate if the final guess has been conducted. If true and user still gives non-decisive feedback, user may cheat
    private final BlockingQueue<String> userFeedback;  // data sharing object to get user feedback from UI thread
    private final BlockingQueue<String> aiGuess;  // data sharing object to send AI guess to UI thread
    private static final Map<Integer, String> inputColorMap = new HashMap<>();  // mapping between color characters and their integer representation. Because of fixed mapping, use as static here

    // construct static color/integer mapping object
    static {
        // BGOPRY
        inputColorMap.put(1, "B");
        inputColorMap.put(2, "G");
        inputColorMap.put(3, "O");
        inputColorMap.put(4, "P");
        inputColorMap.put(5, "R");
        inputColorMap.put(6, "Y");
    }

    // look up mapping object to find the correspondent color
    public static String intToColor(String intString) {    //Start of intTocolor method
        StringBuilder output = new StringBuilder();
        // iterate each digit in an integer string, then attach mapped color to StringBuilder output
        for (Character c : intString.toCharArray()) {
            output.append(inputColorMap.get(Integer.parseInt(c.toString())));
        }
        // convert StringBuilder to String then output
        return output.toString();
    }   //End of intTocolor method

    // constructor
    public GUI_AvP(BlockingQueue<String> userFeedback, BlockingQueue<String> aiGuess) {
        // UI thread should pass in these two data sharing objects when constructing object and launching AI thread
        this.userFeedback = userFeedback;
        this.aiGuess = aiGuess;

        // build and store all possible codes
        for (int i = 1; i <= 6; i++) {  //Start of for loop
            for (int j = 1; j <= 6; j++) {
                for (int k = 1; k <= 6; k++) {
                    for (int l = 1; l <= 6; l++) {
                        remainingPossibleCodes.add(String.format("%d%d%d%d", i, j, k, l)); //this will store all the possibility (1296)
                    }
                }
            }
        }
    }       //End of for loop

    // calcuate matching level result from two codes
    // for example, GGGG/GGGG should return BBBB, and YYYY/OYYY should return BBB, and YYYY/OOOO should return empty string
    public String calculateMatchingLevel(String code, String guess) {
        // calculate B
        int bCount = 0;
        int[] codeDigitCount = new int[6];
        int[] guessDigitCount = new int[6];
        for (int i = 0; i < 4; i++) {
            // iterate code/guess to find out exact matches (position/color both match)
            if (code.charAt(i) == guess.charAt(i)) {
                bCount++;
            }
            codeDigitCount[Integer.parseInt(Character.toString(code.charAt(i))) - 1]++;
            guessDigitCount[Integer.parseInt(Character.toString(guess.charAt(i))) - 1]++;
        }
        // calculate W
        int wCount = 0;
        // iterate all color possibilities array to count color match
        for (int i = 0; i < 6; i++) {
            wCount += Math.min(codeDigitCount[i], guessDigitCount[i]);
        }
        // position/color both match count should be excluded from color match count, the remaining is count of color match but position mismatch
        wCount -= bCount;
        // output to human readable B/W string
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < bCount; i++) {      //start of for loop
            result.append("B");
        }
        for (int i = 0; i < wCount; i++) {
            result.append("W");
        }
        return result.toString();
    }   // end of for loop


    // based on user feedback, eliminate impossible codes
    // eg. if remain 16 possible code in matchcode then transform that 16 possibility in to remainingPossibleCodes (1296 to 16)
    public void calculateRemainingPossibleCodes(String guess, String feedback) throws InterruptedException {
        List<String> matchedCodes = new ArrayList<>();

        // iterate all possible codes
        for (int i = 0; i < remainingPossibleCodes.size(); i++) { // for (String code : remainingPossibleCodes) {
            String code = remainingPossibleCodes.get(i);
            // if the possibility match with user feedback store in matchedCodes arrayList, keep in the list
            if (calculateMatchingLevel(code, guess).equals(feedback)) {
                matchedCodes.add(code);
            }
        }

        //Transform the possibility after matching into remainingPossibleCodes arrayList
        remainingPossibleCodes = matchedCodes;

        // check if the remaining possible code is only 1
        // if so, directly send UI this code and expect BBBB response from user
        if (remainingPossibleCodes.size() == 1) {
            guessCount++;
            aiGuess.put(String.format("My guess %d: %s", guessCount, intToColor(remainingPossibleCodes.get(0))));
            userFeedback.take();
            if (feedback.equals("reset")) {
                throw new InterruptedException();
            }
            aiGuess.put("");
            throw new InterruptedException();
        }
        // check if the remaining possible code is only 1
        // if so, user may cheat, terminate the process
        if (remainingPossibleCodes.size() == 0) {
            aiGuess.put("");
            throw new InterruptedException();
        }
    }


    // main function to perform guess algorithm
    // as the first guess is always 1122 (GGRR), AI can start from the feedback of the initial guess, then distribute to different logic branches
    public void guess() {
        try {
            // get user feedback from UI
            String feedback = userFeedback.take();
            // if the feedback is "reset", which indicates UI is asking to terminate, an InterruptedException is then thrown in order to gracefully finish the thread process
            if (feedback.equals("reset")) {
                throw new InterruptedException();
            }
            // calculate the space of all remaining possibility code
            // Present as filter, filter out the non possible guess
            calculateRemainingPossibleCodes("1122", feedback);
            // distribute to different logic branches as per user feedback
            switch (feedback) {
                case "WWWW":
                    // response WWWW already implies the actual code
                    isLastGuessDisplayed = true;
                    getGuessResult(remainingPossibleCodes.get(0));
                    break;
                case "WWW":
                    secondGuessAfter3W(getGuessResult("1213"));
                    break;
                case "WW":
                    secondGuessAfter2W(getGuessResult("2344"));
                    break;
                case "W":
                    secondGuessAfter1W(getGuessResult("2344"));
                    break;
                case "":
                    secondGuessAfter0W(getGuessResult("3345"));
                    break;
                case "BWW":
                    secondGuessAfter1B2W(getGuessResult("1213"));
                    break;
                case "BW":
                    secondGuessAfter1B1W(getGuessResult("1134"));
                    break;
                case "B":
                    secondGuessAfter1B(getGuessResult("1344"));
                    break;
                case "BBWW":
                    getUniqueCodeFromLastGuess("1213");
                    break;
                case "BBW":
                    secondGuessAfter2B1W(getGuessResult("1223"));
                    break;
                case "BB":
                    secondGuessAfter2B(getGuessResult("1234"));
                    break;
                case "BBB":
                    secondGuessAfter3B(getGuessResult("1223"));
                    break;
            }
        } catch (Exception ignore) { // gracefully finish the thread running once any exception (mostly InterruptedException) is thrown
        }
    }


    // function to facilitate processing scenarios when only 2 possible codes remain
    // guesses first one, if not hit, guesses second one and expects a BBBB response from user
    private void determineCodeFromTwoCandidates() throws InterruptedException {
        // guess first one
        getGuessResult(remainingPossibleCodes.get(0));
        // not hit, implies the second one is the correct code
        // expect BBBB feedback from user, otherwise user cheats
        isLastGuessDisplayed = true;
        getGuessResult(remainingPossibleCodes.get(1));
    }


    // function to facilitate processing scenarios when only 1 possible code remains
    // guesses this one and expects a BBBB response from user
    private void getUniqueCodeFromLastGuess(String guess) throws InterruptedException {
        calculateRemainingPossibleCodes(guess, getGuessResult(guess));
        // expect BBBB feedback from user, otherwise user cheats
        isLastGuessDisplayed = true;
        getGuessResult(remainingPossibleCodes.get(0));
    }

    // guess logic once initial user feedback is WWW
    private void secondGuessAfter3W(String feedback) throws InterruptedException {
        // second guess would be 1213
        calculateRemainingPossibleCodes("1213", feedback);
        // second guess feedback processing logic
        switch (feedback) {
            case "BWW":
                calculateRemainingPossibleCodes("1415", getGuessResult("1415"));
                break;
            case "BW":
            case "BB":
                calculateRemainingPossibleCodes("1145", getGuessResult("1145"));
                break;
            case "BBW":
                calculateRemainingPossibleCodes("4115", getGuessResult("4115"));
                break;
        }
        // third guess would be good enough to reach the conclusion, otherwise user may cheat
        isLastGuessDisplayed = true;
        getGuessResult(remainingPossibleCodes.get(0));
    }

    // guess logic once initial user feedback is WW
    private void secondGuessAfter2W(String feedback) throws InterruptedException {
        // second guess would be 2344
        calculateRemainingPossibleCodes("2344", feedback);
        String nextFeedback;
        // second guess feedback processing logic
        switch (feedback) {
            case "WWW":
                determineCodeFromTwoCandidates();
                break;
            case "WW":
                // third guess is needed
                nextFeedback = getGuessResult("3215");
                calculateRemainingPossibleCodes("3215", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "BWWW":
                    case "BW":
                    case "B":
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWW":
                    case "BBWW":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3231");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("3213");
                        break;
                }
                break;
            case "W":
                // third guess is needed
                nextFeedback = getGuessResult("5215");
                calculateRemainingPossibleCodes("5215", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "BWW":
                    case "BBWW":
                    case "BBW":
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BB":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("3511");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("3611");
                        break;
                }
                break;
            case "":
                getUniqueCodeFromLastGuess("1515");
                break;
            case "BWW":
                getUniqueCodeFromLastGuess("2413");
                break;
            case "BW":
                // third guess is needed
                nextFeedback = getGuessResult("2415");
                calculateRemainingPossibleCodes("2415", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "WWW":
                    case "BWWW":
                    case "BBWW":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWW":
                    case "BBW":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("2253");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("2236");
                        break;
                }
                break;
            case "B":
                // third guess is needed
                nextFeedback = getGuessResult("2256");
                calculateRemainingPossibleCodes("2256", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "BBB":
                    case "BWW":
                    case "BBW":
                    case "BB":
                    case "W":
                    case "BW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BBWW":
                    case "":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                }
                break;
            case "BBW":
                getUniqueCodeFromLastGuess("2234");
                break;
            case "BB":
                // third guess is needed
                nextFeedback = getGuessResult("3315");
                calculateRemainingPossibleCodes("3315", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "B":
                    case "":
                    case "W":
                    case "BWW":
                    case "BW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BBW":
                    case "BBB":
                    case "BB":
                    case "WW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                }
                break;
            case "BBB":
                getUniqueCodeFromLastGuess("2314");
                break;
        }
    }


    // guess logic once initial user feedback is W
    private void secondGuessAfter1W(String feedback) throws InterruptedException {
        // second guess would be 2344
        calculateRemainingPossibleCodes("2344", feedback);
        String nextFeedback;
        // second guess feedback processing logic
        switch (feedback) {
            case "WWW":
                getUniqueCodeFromLastGuess("2335");
                break;
            case "WW":
                // third guess is needed
                nextFeedback = getGuessResult("3235");
                calculateRemainingPossibleCodes("3235", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WW":
                    case "":
                    case "BBWW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "W":
                        getUniqueCodeFromLastGuess("4613");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("5263");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("3413");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("3416");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3256");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("1336");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("1536");
                        break;
                }
                break;
            case "W":
                // third guess is needed
                nextFeedback = getGuessResult("3516");
                calculateRemainingPossibleCodes("3516", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "W":
                    case "B":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                        getUniqueCodeFromLastGuess("4651");
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("6255");
                        break;
                    case "BWWW":
                        getUniqueCodeFromLastGuess("5613");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("1461");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("4551");
                        break;
                    case "BBWW":
                        getUniqueCodeFromLastGuess("1113");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3551");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("4515");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("1145");
                        break;
                }
                break;
            case "":
                // third guess is needed
                nextFeedback = getGuessResult("5515");
                calculateRemainingPossibleCodes("5515", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WW":
                    case "W":
                    case "B":
                    case "BBWW":
                    case "BBW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWW":
                    case "BW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BB":
                    case "BBB":
                        getUniqueCodeFromLastGuess("1516");
                        break;
                }
                break;
            case "BWWW":
                determineCodeFromTwoCandidates();
                break;
            case "BWW":
                // third guess is needed
                nextFeedback = getGuessResult("3245");
                calculateRemainingPossibleCodes("3245", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                        getUniqueCodeFromLastGuess("2436");
                        break;
                    case "BWWW":
                    case "BWW":
                    case "BW":
                    case "BBWW":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3234");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("3243");
                        break;
                }
                break;
            case "BW":
                // third guess is needed
                nextFeedback = getGuessResult("4514");
                calculateRemainingPossibleCodes("4514", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "WWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("2456");
                        break;
                    case "W":
                        getUniqueCodeFromLastGuess("2635");
                        break;
                    case "":
                        getUniqueCodeFromLastGuess("2636");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("1356");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("4361");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("1635");
                        break;
                    case "BBWW":
                    case "BBW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("3614");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("4414");
                        break;
                }
                break;
            case "B":
                // third guess is needed
                nextFeedback = getGuessResult("3315");
                calculateRemainingPossibleCodes("3315", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WW":
                        getUniqueCodeFromLastGuess("5641");
                        break;
                    case "W":
                        getUniqueCodeFromLastGuess("2566");
                        break;
                    case "":
                    case "BWWW":
                    case "BB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("5361");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("5614");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("6614");
                        break;
                    case "BBWW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3331");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("3316");
                        break;
                }
                break;
            case "BBWW":
                getUniqueCodeFromLastGuess("2434");
                break;
            case "BBW":
                // third guess is needed
                nextFeedback = getGuessResult("2425");
                calculateRemainingPossibleCodes("2425", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWW":
                    case "BBW":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "WW":
                    case "W":
                    case "BWW":
                    case "BW":
                    case "BB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                }
                break;
            case "BB":
                // third guess is needed
                nextFeedback = getGuessResult("1545");
                calculateRemainingPossibleCodes("1545", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWW":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("2654");
                        break;
                    case "W":
                        getUniqueCodeFromLastGuess("2353");
                        break;
                    case "":
                        getUniqueCodeFromLastGuess("1136");
                        break;
                    case "BWW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("2564");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("2335");
                        break;
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BBB":
                // third guess is needed
                nextFeedback = getGuessResult("1335");
                calculateRemainingPossibleCodes("1335", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "W":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "":
                    case "BW":
                    case "B":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
        }
    }


    // guess logic once initial user feedback is empty
    private void secondGuessAfter0W(String feedback) throws InterruptedException {
        // second guess would be 3345
        calculateRemainingPossibleCodes("3345", feedback);
        String nextFeedback;
        // second guess feedback processing logic
        switch (feedback) {
            case "WWWW":
                determineCodeFromTwoCandidates();
                break;
            case "WWW":
                // third guess is needed
                nextFeedback = getGuessResult("4653");
                calculateRemainingPossibleCodes("4653", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "WWW":
                    case "BBWW":
                    case "BBW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BWWW":
                        getUniqueCodeFromLastGuess("4536");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("4534");
                        break;
                    case "BW":
                    case "BB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("4453");
                        break;
                }
                break;
            case "WW":
                // third guess is needed
                nextFeedback = getGuessResult("6634");
                calculateRemainingPossibleCodes("6634", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWW":
                        getUniqueCodeFromLastGuess("4566");
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("4556");
                        break;
                    case "W":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWWW":
                    case "BBWW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("4656");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("5653");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("1444");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("5636");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("4654");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("1413");
                        break;
                }
                break;
            case "W":
                // third guess is needed
                nextFeedback = getGuessResult("6646");
                calculateRemainingPossibleCodes("6646", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WW":
                    case "BW":
                    case "B":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("1416");
                        break;
                    case "BBWW":
                        getUniqueCodeFromLastGuess("1416");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("5666");
                        break;
                    case "BB":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "":
                // expects BBBB feedback
                isLastGuessDisplayed = true;
                getGuessResult(remainingPossibleCodes.get(0));
                break;
            case "BWWW":
                getUniqueCodeFromLastGuess("3453");
                break;
            case "BWW":
                // third guess is needed
                nextFeedback = getGuessResult("3454");
                calculateRemainingPossibleCodes("3454", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "BBWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                        getUniqueCodeFromLastGuess("4535");
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("1436");
                        break;
                    case "BWWW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("4356");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("3536");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3564");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("3463");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("3456");
                        break;
                }
                break;
            case "BW":
                // third guess is needed
                nextFeedback = getGuessResult("3636");
                calculateRemainingPossibleCodes("3636", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "WWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("4364");
                        break;
                    case "W":
                        getUniqueCodeFromLastGuess("4565");
                        break;
                    case "":
                        getUniqueCodeFromLastGuess("4544");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("4366");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("1565");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("4546");
                        break;
                    case "BBWW":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3466");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("3556");
                        break;
                }
                break;
            case "B":
                // third guess is needed
                nextFeedback = getGuessResult("3656");
                calculateRemainingPossibleCodes("3656", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWW":
                    case "WW":
                    case "W":
                    case "":
                    case "BBW":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("5665");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("6446");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("4446");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("4646");
                        break;
                }
                break;
            case "BBWW":
                // third guess is needed
                nextFeedback = getGuessResult("3435");
                calculateRemainingPossibleCodes("3435", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "BBWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWWW":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BBW":
                // third guess is needed
                nextFeedback = getGuessResult("3443");
                calculateRemainingPossibleCodes("3443", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WW":
                        getUniqueCodeFromLastGuess("4355");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("3334");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("3356");
                        break;
                    case "BBWW":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BBW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("3455");
                        break;
                }
                break;
            case "BB":
                // third guess is needed
                nextFeedback = getGuessResult("3636");
                calculateRemainingPossibleCodes("3636", nextFeedback);
                switch (nextFeedback) {
                    case "WWW":
                    case "BBWW":
                    case "BBW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("5365");
                        break;
                    case "W":
                        getUniqueCodeFromLastGuess("6445");
                        break;
                    case "":
                        getUniqueCodeFromLastGuess("1444");
                        break;
                    case "BWW":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("3565");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("4645");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("3446");
                        break;
                }
                break;
            case "BBB":
                // third guess is needed
                nextFeedback = getGuessResult("3446");
                calculateRemainingPossibleCodes("3446", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "BWW":
                    case "BW":
                    case "B":
                    case "BBW":
                    case "BB":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
        }
    }


    // guess logic once initial user feedback is BWW
    private void secondGuessAfter1B2W(String feedback) throws InterruptedException {
        // second guess would be 1213
        calculateRemainingPossibleCodes("1213", feedback);
        String nextFeedback;
        // second guess feedback processing logic
        switch (feedback) {
            case "WWWW":
                // expects BBBB feedback
                isLastGuessDisplayed = true;
                getGuessResult(remainingPossibleCodes.get(0));
                break;
            case "WWW":
                getUniqueCodeFromLastGuess("1145");
                break;
            case "WW":
                getUniqueCodeFromLastGuess("1415");
                break;
            case "BWW":
                // third guess is needed
                nextFeedback = getGuessResult("1114");
                calculateRemainingPossibleCodes("1114", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "W":
                    case "B":
                    case "BBW":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BW":
                // third guess is needed
                nextFeedback = getGuessResult("2412");
                calculateRemainingPossibleCodes("2412", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "BWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BBWW":
                determineCodeFromTwoCandidates();
                break;
            case "BBW":
                getUniqueCodeFromLastGuess("1145");
                break;
            case "BB":
                // third guess is needed
                nextFeedback = getGuessResult("1145");
                calculateRemainingPossibleCodes("1145", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WW":
                    case "W":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BBB":
                // third guess is needed
                nextFeedback = getGuessResult("1114");
                calculateRemainingPossibleCodes("1114", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "BBW":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
        }
    }


    // guess logic once initial user feedback is BW
    private void secondGuessAfter1B1W(String feedback) throws InterruptedException {
        // second guess would be 1134
        calculateRemainingPossibleCodes("1134", feedback);
        String nextFeedback;
        // second guess feedback processing logic
        switch (feedback) {
            case "WWW":
                getUniqueCodeFromLastGuess("1312");
                break;
            case "WW":
                // third guess is needed
                nextFeedback = getGuessResult("3521");
                calculateRemainingPossibleCodes("3521", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                    case "BBWW":
                    case "BBW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("4612");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("3312");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("2423");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("4621");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("3321");
                        break;
                }
                break;
            case "W":
                // third guess is needed
                nextFeedback = getGuessResult("2352");
                calculateRemainingPossibleCodes("2352", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "BBWW":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "WWW":
                        getUniqueCodeFromLastGuess("3226");
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("5621");
                        break;
                    case "W":
                    case "BWWW":
                    case "B":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("2223");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("6242");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("2323");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("2462");
                        break;
                }
                break;
            case "":
                // third guess is needed
                nextFeedback = getGuessResult("2525");
                calculateRemainingPossibleCodes("2525", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "WW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                    case "BBWW":
                    case "BBW":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("2252");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("2262");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("2225");
                        break;
                }
                break;
            case "BWWW":
                getUniqueCodeFromLastGuess("1341");
                break;
            case "BWW":
                // third guess is needed
                nextFeedback = getGuessResult("1315");
                calculateRemainingPossibleCodes("1315", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "BWWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                        getUniqueCodeFromLastGuess("4151");
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("4161");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("6451");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("1461");
                        break;
                    case "BBWW":
                        getUniqueCodeFromLastGuess("1351");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("1361");
                        break;
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("1113");
                        break;
                }
                break;
            case "BW":
                // third guess is needed
                nextFeedback = getGuessResult("1516");
                calculateRemainingPossibleCodes("1516", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "WWW":
                    case "BWWW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("2145");
                        break;
                    case "":
                        getUniqueCodeFromLastGuess("2324");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("1661");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("1245");
                        break;
                    case "BBWW":
                        getUniqueCodeFromLastGuess("1561");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("1551");
                        break;
                    case "BB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("1511");
                        break;
                }
                break;
            case "B":
                // third guess is needed
                nextFeedback = getGuessResult("1256");
                calculateRemainingPossibleCodes("1256", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("2524");
                        break;
                    case "W":
                    case "BWW":
                    case "BBWW":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("5224");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("2224");
                        break;
                }
                break;
            case "BBWW":
                getUniqueCodeFromLastGuess("1314");
                break;
            case "BBW":
                // third guess is needed
                nextFeedback = getGuessResult("1315");
                calculateRemainingPossibleCodes("1315", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "BWWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                    case "BWW":
                    case "BW":
                    case "BBW":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BB":
                // third guess is needed
                nextFeedback = getGuessResult("1235");
                calculateRemainingPossibleCodes("1235", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWW":
                    case "BBWW":
                    case "BBW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WW":
                    case "BWW":
                    case "BB":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BBB":
                determineCodeFromTwoCandidates();
                break;
        }
    }


    // guess logic once initial user feedback is B
    private void secondGuessAfter1B(String feedback) throws InterruptedException {
        // second guess would be 1344
        calculateRemainingPossibleCodes("1344", feedback);
        String nextFeedback;
        // second guess feedback processing logic
        switch (feedback) {
            case "WWW":
                getUniqueCodeFromLastGuess("1335");
                break;
            case "WW":
                // third guess is needed
                nextFeedback = getGuessResult("3135");
                calculateRemainingPossibleCodes("3135", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WW":
                    case "":
                    case "BBWW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "W":
                        getUniqueCodeFromLastGuess("4623");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("5163");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("3423");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("3426");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3156");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("1436");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("1536");
                        break;
                }
                break;
            case "W":
                // third guess is needed
                nextFeedback = getGuessResult("3526");
                calculateRemainingPossibleCodes("3526", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "W":
                    case "B":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                        getUniqueCodeFromLastGuess("4652");
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("6155");
                        break;
                    case "BWWW":
                        getUniqueCodeFromLastGuess("5623");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("1462");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("4552");
                        break;
                    case "BBWW":
                        getUniqueCodeFromLastGuess("1123");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3552");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("4525");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("1145");
                        break;
                }
                break;
            case "":
                // third guess is needed
                nextFeedback = getGuessResult("5525");
                calculateRemainingPossibleCodes("5525", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WW":
                    case "W":
                    case "B":
                    case "BBWW":
                    case "BBW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWW":
                    case "BW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("1516");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("1516");
                        break;
                }
                break;
            case "BWWW":
                determineCodeFromTwoCandidates();
                break;
            case "BWW":
                // third guess is needed
                nextFeedback = getGuessResult("3145");
                calculateRemainingPossibleCodes("3145", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                        getUniqueCodeFromLastGuess("1436");
                        break;
                    case "BWWW":
                    case "BWW":
                    case "BW":
                    case "BBWW":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3134");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("3143");
                        break;
                }
                break;
            case "BW":
                // third guess is needed
                nextFeedback = getGuessResult("4524");
                calculateRemainingPossibleCodes("4524", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "WWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("1456");
                        break;
                    case "W":
                        getUniqueCodeFromLastGuess("1635");
                        break;
                    case "":
                        getUniqueCodeFromLastGuess("1636");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("1356");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("4362");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("1336");
                        break;
                    case "BBWW":
                    case "BBW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("3624");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("4424");
                        break;
                }
                break;
            case "B":
                // third guess is needed
                nextFeedback = getGuessResult("3325");
                calculateRemainingPossibleCodes("3325", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WW":
                        getUniqueCodeFromLastGuess("5642");
                        break;
                    case "W":
                        getUniqueCodeFromLastGuess("1566");
                        break;
                    case "":
                    case "BWWW":
                    case "BB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("5362");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("5624");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("6624");
                        break;
                    case "BBWW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("3332");
                        break;
                    case "BBB":
                        getUniqueCodeFromLastGuess("3326");
                        break;
                }
                break;
            case "BBWW":
                getUniqueCodeFromLastGuess("1434");
                break;
            case "BBW":
                // third guess is needed
                nextFeedback = getGuessResult("1415");
                calculateRemainingPossibleCodes("1415", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWW":
                    case "BBW":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WW":
                    case "W":
                    case "BWW":
                    case "BW":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BB":
                // third guess is needed
                nextFeedback = getGuessResult("1415");
                calculateRemainingPossibleCodes("1415", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WW":
                    case "BBW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "W":
                        getUniqueCodeFromLastGuess("3324");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("1546");
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("1356");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("1136");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("1136");
                        break;
                }
                break;
            case "BBB":
                // third guess is needed
                nextFeedback = getGuessResult("1335");
                calculateRemainingPossibleCodes("1335", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "BW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "B":
                    case "BBW":
                    case "BB":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
        }
    }


    // guess logic once initial user feedback is BBW
    private void secondGuessAfter2B1W(String feedback) throws InterruptedException {
        // second guess would be 1223
        calculateRemainingPossibleCodes("1223", feedback);
        switch (feedback) {
            case "WWWW":
                // expects BBBB feedback
                isLastGuessDisplayed = true;
                getGuessResult(remainingPossibleCodes.get(0));
                break;
            case "WWW":
                getUniqueCodeFromLastGuess("2145");
                break;
            case "WW":
                getUniqueCodeFromLastGuess("4115");
                break;
            case "BWW":
                getUniqueCodeFromLastGuess("2145");
                break;
            case "BW":
                getUniqueCodeFromLastGuess("4512");
                break;
            case "BBWW":
                determineCodeFromTwoCandidates();
                break;
            case "BBW":
                getUniqueCodeFromLastGuess("1245");
                break;
            case "BB":
                getUniqueCodeFromLastGuess("1415");
                break;
            case "BBB":
                getUniqueCodeFromLastGuess("1145");
                break;
        }
    }


    // guess logic once initial user feedback is BB
    private void secondGuessAfter2B(String feedback) throws InterruptedException {
        // second guess would be 1234
        calculateRemainingPossibleCodes("1234", feedback);
        String nextFeedback;
        // second guess feedback processing logic
        switch (feedback) {
            case "WWWW":
                determineCodeFromTwoCandidates();
                break;
            case "WWW":
                // third guess is needed
                nextFeedback = getGuessResult("1325");
                calculateRemainingPossibleCodes("1325", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "BWWW":
                    case "BBWW":
                    case "BBW":
                    case "BB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WWW":
                        getUniqueCodeFromLastGuess("4152");
                        break;
                    case "WW":
                        getUniqueCodeFromLastGuess("4162");
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("3126");
                        break;
                    case "BW":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "WW":
                // third guess is needed
                nextFeedback = getGuessResult("1325");
                calculateRemainingPossibleCodes("1325", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWW":
                        getUniqueCodeFromLastGuess("5162");
                        break;
                    case "WW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BWW":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BW":
                        getUniqueCodeFromLastGuess("4522");
                        break;
                    case "B":
                        getUniqueCodeFromLastGuess("4622");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("5125");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("2116");
                        break;
                }
                break;
            case "W":
                getUniqueCodeFromLastGuess("2515");
                break;
            case "BWWW":
                getUniqueCodeFromLastGuess("1323");
                break;
            case "BWW":
                // third guess is needed
                nextFeedback = getGuessResult("1352");
                calculateRemainingPossibleCodes("1352", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWW":
                    case "BBWW":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "WW":
                    case "BWWW":
                    case "BW":
                    case "BBB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BWW":
                        getUniqueCodeFromLastGuess("1623");
                        break;
                    case "BBW":
                        getUniqueCodeFromLastGuess("1323");
                        break;
                    case "BB":
                        getUniqueCodeFromLastGuess("1462");
                        break;
                }
                break;
            case "BW":
                // third guess is needed
                nextFeedback = getGuessResult("2156");
                calculateRemainingPossibleCodes("2156", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "WWWW":
                    case "WWW":
                    case "W":
                    case "BWWW":
                    case "BWW":
                    case "BW":
                    case "B":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "B":
                // third guess is needed
                nextFeedback = getGuessResult("1315");
                calculateRemainingPossibleCodes("1315", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "W":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "":
                    case "BWW":
                    case "BW":
                    case "BBW":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BBWW":
                determineCodeFromTwoCandidates();
                break;
            case "BBW":
                getUniqueCodeFromLastGuess("3526");
                break;
            case "BB":
                // third guess is needed
                nextFeedback = getGuessResult("1536");
                calculateRemainingPossibleCodes("1536", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "BW":
                    case "B":
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BBW":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                }
                break;
            case "BBB":
                // expects BBBB feedback
                isLastGuessDisplayed = true;
                getGuessResult(remainingPossibleCodes.get(0));
                break;
        }
    }


    // guess logic once initial user feedback is BBB
    private void secondGuessAfter3B(String feedback) throws InterruptedException {
        // second guess would be 1223
        calculateRemainingPossibleCodes("1223", feedback);
        String nextFeedback;
        // second guess feedback processing logic
        switch (feedback) {
            case "BWWW":
            case "BBWW":
                // expects BBBB feedback
                isLastGuessDisplayed = true;
                getGuessResult(remainingPossibleCodes.get(0));
                break;
            case "BWW":
                // third guess is needed
                nextFeedback = getGuessResult("1145");
                calculateRemainingPossibleCodes("1145", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "BW":
                    case "B":
                        determineCodeFromTwoCandidates();
                        break;
                    case "BB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                }
                break;
            case "BW":
                // third guess is needed
                nextFeedback = getGuessResult("1114");
                calculateRemainingPossibleCodes("1114", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "BBW":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BBW":
                getUniqueCodeFromLastGuess("1415");
                break;
            case "BB":
                // third guess is needed
                nextFeedback = getGuessResult("1114");
                calculateRemainingPossibleCodes("1114", nextFeedback);
                // third guess feedback processing logic
                switch (nextFeedback) {
                    case "BBW":
                    case "BBB":
                        // expects BBBB feedback
                        isLastGuessDisplayed = true;
                        getGuessResult(remainingPossibleCodes.get(0));
                        break;
                    case "BB":
                        determineCodeFromTwoCandidates();
                        break;
                }
                break;
            case "BBB":
                determineCodeFromTwoCandidates();
                break;
        }
    }

    // send AI guess to UI and then retrieves user feedback
    private String getGuessResult(String guess) throws InterruptedException {   //start of method getGuessResult
        // increment guess count
        guessCount++;
        // send AI guess to UI
        aiGuess.put(String.format("My guess %d: %s", guessCount, intToColor(guess)));
        // wait to get user feedback
        String feedback = userFeedback.take();
        // if UI is asking to terminate AI thread, throw InterruptedException
        if (feedback.equals("reset")) {
            throw new InterruptedException();
        }
        // if BBBB response is expected but gets alternative user feedback, user may cheat
        if (isLastGuessDisplayed) {
            // tell UI the user feedback is impossible
            aiGuess.put("");
        }
        return feedback;
    }   //end of method getGuessResult
}      //end of public class GUI_AvP
