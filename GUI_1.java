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
//         - In P VS Al mode, player will receive hints provide by the AI (hints will either be B or W)
//         - In Al VS P mode, player will receive guesses form the AI
//=====================================================================================================================
// Purpose of class:
//          UI class to construct UI and process related behavior.
// List of Variables:
//       - let textDisplayAtAIVsPlayer represent the the displaying text at AI VS Player mode in textArea (type String)
//          - let userFeedback represent the user feedback type in main class  (Data pass)
//          - let aiGuess represent the AI guesses from the AvP class (Data send)
//          - let executorService represent the executor service for launch other class (Manage AI thread)
//       - let rules represent the file name read by program (type String)
//          - let feedback represent the input getting from player/user (type String)
//          - let response represent the response of the AI of variables aiGuess (type String)
//          - let 'bwpeg' represent the number of black and white pegs
//          - let 'generated_code' represent the random code
//          - let 'guessed_code' represent the player's guess
//          - let 'text_pos_pixel' represent the x position of colour labels
//          - let 'line_pixel' represent the y position of colour labels and hint pegs
//          - let 'peg_pos_pixel' represent the x position of the hint pegs
//=====================================================================================================================
import javax.swing.*;    //For creating user interfaces
import java.awt.*;    //For creating user interfaces
import java.awt.event.*;    //For user/GUI interaction process
import java.io.*;
import java.nio.charset.StandardCharsets;    //Charset used at reading rule file content
import java.nio.file.Files;     //Read file in GUI
import java.nio.file.Paths;     //locate and get path of the file
import java.util.concurrent.*;  //Contains a set of classes (multithreaded)
import java.util.ArrayList;
import javax.swing.event.ChangeEvent; // For detecting tab switch
import javax.swing.event.ChangeListener; // For detecting tab switch

class GUI_1 extends JFrame implements ActionListener {   //Public class GUI_Test header

    // Initialize variables
    ArrayList<String> guessed_code = new ArrayList<String>(); //stores player's guesses
    int bwpeg[] = {0, 0}; //{number of black pegs,number of white pegs}
    String generated_code[] = new String[4];  //randomly generated code gets stored here
    int round = 0; //counts  of round
    int text_pos = 0;  //keep track of location of colour labels
    int peg_pos = 0;  //keeps track of location of pegs
    int game = 0;  // keep track of game number
    int previous_tab; // To check which panel tab is being used.

    //Create some Panels
    JTabbedPane tabbedPane = new JTabbedPane();         // lets the user switch between pages by clicking on the provided tab
    JPanel panelWelcome = new JPanel(new GridLayout(4, 1));         //welcome front panel, set to grid layout
    JPanel panelRules = new JPanel(new GridLayout(1, 1));       //rule display panel
    JPanel panelAIVsPlayer = new JPanel(new GridLayout(1, 2));   // AI vs Player (AI guess) panel
    JPanel panelPlayerVsAI = new JPanel();       // Player vs AI (Player guess) panel
    JPanel score = new JPanel();

    private String textDisplayAtAIVsPlayer;   // string to be displayed at messageDisplayAtAIVsPlayer text area
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);  //Manage AI thread, allow this main class to launch other class
    //Blocking Queue is a support operations, it will block the data from thread when reach it's limit
    private BlockingQueue<String> userFeedback = new ArrayBlockingQueue(1);  // Data pass to AI thread
    private BlockingQueue<String> aiGuess = new ArrayBlockingQueue(1);      // Data send from AI thread

    //Create some GUI components
    JLabel instructionsLabel = new JLabel("                         Please take a moment and think of an code ", JLabel.LEFT);
    JLabel titleLabel = new JLabel("The Code Breaker ", JLabel.CENTER);
    JButton RulesButton = new JButton(" Game Rules ");
    JButton PvAButton = new JButton(" Player VS AI ");
    JButton AvPButton = new JButton(" AI VS Player ");
    JButton submitButtonAtAIVsPlayer = new JButton("Submit");
    JButton resetButtonAtAIVsPlayer = new JButton("Reset");
    JTextArea messageDisplayAtAIVsPlayer = new JTextArea();        // Multi-line area that will displays plain text
    JTextArea messageDisplayRules = new JTextArea();
    JTextField playerInputAtAIVsPlayer = new JTextField();        //Allows the editing of lines of text

    // PvA buttons can be clicked to guess the code
    JButton bButtonPvA = new JButton("B");
    JButton gButtonPvA = new JButton("G");
    JButton oButtonPvA = new JButton("O");
    JButton pButtonPvA = new JButton("P");
    JButton rButtonPvA = new JButton("R");
    JButton yButtonPvA = new JButton("Y");
    JButton checkButtonPvA = new JButton("Check");  //check code for hints
    JButton deleteButtonPvA = new JButton("Delete");  //delete code
    JButton newgameButtonPvA = new JButton("New Game");  //play a new game
    JLabel nothing = new JLabel("");  //background (white box)
    JTextArea displayScore = new JTextArea();  //displays the scoreboard
    JScrollPane scrollPanel2 = new JScrollPane(displayScore);  //scroll pane for scoreboard

    // Messages to print in PvA
    JTextArea messageDisplayAtPvA = new JTextArea("Guess the code!");
    JTextArea messageDisplayAtPvA2 = new JTextArea("Changing the tab can reset the game");
    JTextArea messageDisplayAtPvA3 = new JTextArea("Your game history will appear in Score Board tab");


    // CONSTRUCTOR - Setup GUI
    public GUI_1() {        //GUI_Test method header

        executorService.execute(() -> {   //launch class GUI_AvPTest
            while (true) {
                new GUI_AvP(userFeedback, aiGuess).guess(); // Multi-threading
            }
        });

        setTitle("Code Breaker");     //Create a window with a title
        setSize(1100, 830);    // set the window size

        initializePanels();      //Call method initializePanels

        add(tabbedPane);

        // Initialize the PvA Score Board file
        if (game == 0){  // game==0 means a new game has started.
            try {
                new FileWriter("SCOREBOARD.txt", false).close();
            } //end try
            catch (Exception ignored){
            } //end catch
            game = 1; //will be used later for scoreboard
        } //end if

        // Detect which tab is being used
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int tab_idx = tabbedPane.getSelectedIndex();
                // if tab is switched to PvA
                if (tab_idx == 2){
                    // Reset and restart a new PvA game
                    start_new_game();
                    System.out.println("New game");
                }
                // if tab is switched to Score Board
                else if (tab_idx == 4) {
                    // Update the texts
                    try {
                        String history = new String(Files.readAllBytes(Paths.get("./SCOREBOARD.txt")), StandardCharsets.UTF_8);
                        displayScore.setText(history);
                        displayScore.setLineWrap(true);
                        displayScore.setWrapStyleWord(true);
                    } //end try
                    catch (Exception ignored) {       //catch and ignore the IO Exception
                    } //end catch
                }
            }
        });

        setVisible(true);        //Display GUI
        setDefaultCloseOperation(EXIT_ON_CLOSE);       //Close window when click on close button
        setButtonBehavior();           //This method will specify an action when the button is pressed

        generated_code = random_code();
    } // end of GUI_1 constructor

    // initialize panels with detailed components and layouts for PvA
    private void initializePanels() {   //Start of the initializePanels method

        // Add the panels to the frame and display the window
        tabbedPane.add("Welcome", panelWelcome);
        tabbedPane.add("Game Rules", panelRules);
        tabbedPane.add("Player vs AI", panelPlayerVsAI);
        tabbedPane.add("AI vs Player", panelAIVsPlayer);
        tabbedPane.add("Score Board", score);

        // Add all the components to the panels
        Font font = new Font("Serif", Font.PLAIN, 24);   //Set font to Serif
        titleLabel.setFont(font);       //set titleLabel to Serif font
        panelWelcome.add(titleLabel);
        panelWelcome.add(RulesButton);
        panelWelcome.add(PvAButton);
        panelWelcome.add(AvPButton);

        //Claire_PvA ***********************************************************************************
        // Initialize the PvA panel
        panelPlayerVsAI.setLayout(null);
        panelPlayerVsAI.setBackground(Color.LIGHT_GRAY);

        //Display Score Board tab
        score.setBackground(Color.LIGHT_GRAY);
        displayScore.setOpaque(true);
        displayScore.setEditable(false);
        scrollPanel2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); //the scroll panel will always be vertical
        score.add(scrollPanel2, BorderLayout.CENTER);
        scrollPanel2.setPreferredSize(new Dimension(450, 110));

        // Display PvA tab
        set_new_display();

        //add action listener for buttons for PvA
        bButtonPvA.addActionListener(this);
        gButtonPvA.addActionListener(this);
        oButtonPvA.addActionListener(this);
        pButtonPvA.addActionListener(this);
        rButtonPvA.addActionListener(this);
        yButtonPvA.addActionListener(this);
        checkButtonPvA.addActionListener(this);
        deleteButtonPvA.addActionListener(this);
        newgameButtonPvA.addActionListener(this);
        //Claire_PvA ***********************************************************************************

        //panel Rule
        messageDisplayRules.setEditable(false);  //User will not able to edit the messageDisplayAtAIVsPlayer text area
        try {
            String rules = new String(Files.readAllBytes(Paths.get("./Code Breaker rule file.txt")), StandardCharsets.UTF_8); //read file content in UTF-8 encoding. Computer requires a way to map between binary data and human readable characters, and such mapping set is called a charset or encoding. The initial charset is ASCII, which is able to handle alphabetic characters, numbers and common symbols. The successor is UTF-8. It includes ASCII, and is also capable of processing characters from other languages, such as Chinese, Japanese, Russian, etc.. Nowadays UTF-8 is the most popular charset in the world.
            messageDisplayRules.setText(rules);     //input rule file into textarea
            messageDisplayRules.setLineWrap(true);  //wrapped if they are too long to fit within the given width
            messageDisplayRules.setWrapStyleWord(true);  //Let one word for unit, make program unable to separate last word to next line
            JScrollPane scrollPanel = new JScrollPane(messageDisplayRules);         //provides scroll bar
            scrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); //the scroll panel will always be vertical
            panelRules.add(scrollPanel);    //add scroll panel to rule panel
            //scrollPanel.setOpaque(true);    //add scroll panel to rule panel

        } catch (Exception ignored) {      //try catch and ignore the IO Exception
        }   //End of try catch

        // panel AI vs Player
        messageDisplayAtAIVsPlayer.setEditable(false);      //User will not able to edit the messageDisplayAtAIVsPlayer text area
        messageDisplayAtAIVsPlayer.setText("My guess 1: GGBB" + System.lineSeparator());    // In different systems, e.g. Windows, MacOS, etc., the symbol to start a new line is different. System.lineSeparator() will add a proper new line symbol to the String object, based on which system it is in.
        panelAIVsPlayer.add(messageDisplayAtAIVsPlayer);           //Insert messageDisplayAtAIVsPlayer text area to AvP panel
        JPanel rightSideAtAIVsPlayer = new JPanel(new GridLayout(4, 1));    //Create right side layout for AvP panel
        rightSideAtAIVsPlayer.add(instructionsLabel);       //Insert text field and buttons
        rightSideAtAIVsPlayer.add(playerInputAtAIVsPlayer);
        rightSideAtAIVsPlayer.add(submitButtonAtAIVsPlayer);
        rightSideAtAIVsPlayer.add(resetButtonAtAIVsPlayer);
        panelAIVsPlayer.add(rightSideAtAIVsPlayer);      //Add layout to AvP panel
    }  //End of the initializePanels method

    // Add click action behavior to all buttons in the GUI
    private void setButtonBehavior() {
        //Start of setButtonBehavior method
        //Logics inside brackets will be executed once this button is clicked
        RulesButton.addActionListener(e -> tabbedPane.setSelectedIndex(1));     //Sets the selected index for tabbedPanes
        PvAButton.addActionListener(e -> tabbedPane.setSelectedIndex(2));
        AvPButton.addActionListener(e -> tabbedPane.setSelectedIndex(3));
        submitButtonAtAIVsPlayer.addActionListener(e -> {       //Execute when press submitButton
            String feedback = playerInputAtAIVsPlayer.getText().toUpperCase().trim();   //Converts player input as feedback. String.trim() function is to truncate leading and ending white spaces out of a String object, wherever possible. For example, after trim(), "  ABC " will be transformed to "ABC", and "hello" will still remain the same (no white space to be trimmed).
            //Converts all of the characters from player feedback to upper case
            textDisplayAtAIVsPlayer = messageDisplayAtAIVsPlayer.getText();
            if (feedback.length() <= 4 && feedback.replaceAll("B", "").replaceAll("W", "").isEmpty()) {   //Returns the length of this string
                //Returns true if length() is 0
                textDisplayAtAIVsPlayer += "Your response: " + feedback + System.lineSeparator() + System.lineSeparator();
                try {
                    if (feedback.equals("BBBB")) {
                        userFeedback.put("reset");
                        textDisplayAtAIVsPlayer += "I won!" + System.lineSeparator() + System.lineSeparator() + "Click reset button to play again ...";
                        submitButtonAtAIVsPlayer.setEnabled(false);     //User will not able to edit
                    } else {
                        userFeedback.put(feedback);  // It intends to send user feedback to AI logic. If AI logic isn't ready (no available space), the function will wait until an empty space comes.
                        String response = aiGuess.take();  // It intends to obtain AI guess result from AI logic. If a guess result isn't ready yet, the function will wait until one is available to be picked up.
                        if (response.isEmpty()) {   //Returns true if length() is 0
                            textDisplayAtAIVsPlayer += "Impossible!" + System.lineSeparator() + System.lineSeparator() + "Click reset button to play again ...";
                            submitButtonAtAIVsPlayer.setEnabled(false);  //User will not able to edit
                        } else {
                            textDisplayAtAIVsPlayer += response + System.lineSeparator();
                            playerInputAtAIVsPlayer.grabFocus();   // set focus to playerInputAtAIVsPlayer text field. Then user doesn't need to click text field, as it is already ready to receive user typed feedback.
                        }
                    }
                } catch (InterruptedException ignore) {    // BlockingQueue.put() and BlockingQueue.take() are blocking operations, which means they will keep waiting (blocked) until the function is executed successfully. However, such waiting could be terminated anytime by sending interruption signal to the thread. Once it happens, blocking operations will throw an InterruptedException object. At our scenario here, waiting operation isn't expected to be terminated intentionally, hence InterruptedException object, if be thrown, would be ignored.
                }       //end of try and catch
                messageDisplayAtAIVsPlayer.setText(textDisplayAtAIVsPlayer);
                playerInputAtAIVsPlayer.setText("");
            } else {
                JOptionPane.showMessageDialog(null, "Invalid input: " + feedback + ", please check");   //Pop up a standard dialog box that informs them of something
            }
        });     //End of submitButtonAtAIVsPlayer ActionListener
        resetButtonAtAIVsPlayer.addActionListener(e -> {    //Execute when press resetButton
            try {
                userFeedback.put("reset");
                playerInputAtAIVsPlayer.setText("");
                messageDisplayAtAIVsPlayer.setText("My guess 1: GGBB" + System.lineSeparator());
                submitButtonAtAIVsPlayer.setEnabled(true);
                while (!aiGuess.isEmpty()) {    //Start of while loop
                    aiGuess.clear();    //Clear aiGuess when returns true
                }    //End of while loop
                while (!userFeedback.isEmpty()) { //Start of while loop
                    userFeedback.clear(); //Clear userFeedback when returns true
                }   //End of while loop
            } catch (InterruptedException ignore) {
            }  //End of try and catch
        });   //End of resetButtonAtAIVsPlayer ActionListener
    }   //End of setButtonBehavior method

    // ACTION LISTENER - This method runs when an event occurs
    // Code in here only runs when a user interacts with a component
    // that has an action listener attached to it
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        int text_pos_pixel = text_pos*50 + 550;  //determines the x position of labels
        int line_pixel = round*50 + 50;  //determined the y position of labels

        //Create labels that show player's guesses
        JLabel bLabel = new JLabel("B", JLabel.CENTER);
        JLabel gLabel = new JLabel("G", JLabel.CENTER);
        JLabel oLabel = new JLabel("O", JLabel.CENTER);
        JLabel pLabel = new JLabel("P", JLabel.CENTER);
        JLabel rLabel = new JLabel("R", JLabel.CENTER);
        JLabel yLabel = new JLabel("Y", JLabel.CENTER);
        JLabel sLabel = new JLabel("S", JLabel.CENTER);
        JLabel nLabel = new JLabel("N", JLabel.CENTER);

        // Player is inputting a 4 digit code
        if((guessed_code.size()<4)&&(round < 10)){
            // Colpour buttons BGOPRY
            if (command.equals("B")) {
                //JLabel bLabel = new JLabel("B", JLabel.CENTER);
                panelPlayerVsAI.add(bLabel);
                bLabel.setBounds(text_pos_pixel,line_pixel,50,30);
                bLabel.setFont(new Font("Arial", Font.BOLD, 20));
                bLabel.setForeground(Color.WHITE);
                bLabel.setBackground(Color.BLUE);
                bLabel.setOpaque(true);
                text_pos++; //x-axis position
                guessed_code.add("B");
            } //end if
            if (command.equals("G")) {
                panelPlayerVsAI.add(gLabel);
                gLabel.setBounds(text_pos_pixel,line_pixel,50,30);
                gLabel.setFont(new Font("Arial", Font.BOLD, 20));
                gLabel.setForeground(Color.WHITE);
                gLabel.setBackground(Color.GREEN);
                gLabel.setOpaque(true);
                text_pos++; //x-axis position
                guessed_code.add("G");
            } //end if
            if (command.equals("O")) {
                panelPlayerVsAI.add(oLabel);
                oLabel.setBounds(text_pos_pixel,line_pixel,50,30);
                oLabel.setFont(new Font("Arial", Font.BOLD, 20));
                oLabel.setForeground(Color.WHITE);
                oLabel.setBackground(Color.ORANGE);
                oLabel.setOpaque(true);
                text_pos++; //x-axis position
                guessed_code.add("O");
            } //end if
            if (command.equals("P")) {
                panelPlayerVsAI.add(pLabel);
                pLabel.setBounds(text_pos_pixel,line_pixel,50,30);
                pLabel.setFont(new Font("Arial", Font.BOLD, 20));
                pLabel.setForeground(Color.WHITE);
                pLabel.setBackground(new Color(0x800080));
                pLabel.setOpaque(true);
                text_pos++; //x-axis position
                guessed_code.add("P");
            } //end if
            if (command.equals("R")) {
                panelPlayerVsAI.add(rLabel);
                rLabel.setBounds(text_pos_pixel,line_pixel,50,30);
                rLabel.setFont(new Font("Arial", Font.BOLD, 20));
                rLabel.setForeground(Color.WHITE);
                rLabel.setBackground(Color.RED);
                rLabel.setOpaque(true);
                text_pos++; //x-axis position
                guessed_code.add("R");
            } //end if
            if (command.equals("Y")) {
                panelPlayerVsAI.add(yLabel);
                yLabel.setBounds(text_pos_pixel,line_pixel,50,30);
                yLabel.setFont(new Font("Arial", Font.BOLD, 20));
                yLabel.setForeground(Color.BLACK);
                yLabel.setBackground(Color.YELLOW);
                yLabel.setOpaque(true);
                text_pos++; //x-axis position
                guessed_code.add("Y");
            } //end if
            // If Check is clicked before 4-digits complete
            if (command.equals("Check")){
                JTextField notField = new JTextField ("Please fill all 4 slots");
                panelPlayerVsAI.add(notField);
                notField.setBounds(675,600,200,30);
                notField.setFont(new Font("Arial", Font.BOLD, 20));
            } //end if
        } //end if
        // Player has inputted a 4-digit code
        else if ((guessed_code.size()>=4)&&(round < 10)){  //when game is not over yet
            // If Check is clicked after inputting a 4-digit code
            if (command.equals("Check")){
                bwpeg = imAI(guessed_code, generated_code); //call method imAI
                peg_pos = 0; //reset position

                // Delete a previous message
                panelPlayerVsAI.add(sLabel);
                sLabel.setBounds(500,600,500,30);
                sLabel.setFont(new Font("Arial", Font.BOLD, 20));
                sLabel.setForeground(Color.WHITE);
                sLabel.setBackground(Color.WHITE);
                sLabel.setOpaque(true);

                // Display "B" following the rules
                for (int i=0; i<bwpeg[0]; i++) {
                    int peg_pos_pixel = peg_pos*50 + 800;  //keep track of position of the pegs

                    JLabel bpLabel = new JLabel("B", JLabel.CENTER);
                    panelPlayerVsAI.add(bpLabel);
                    bpLabel.setBounds(peg_pos_pixel,line_pixel,50,30);
                    bpLabel.setFont(new Font("Arial", Font.BOLD, 20));
                    bpLabel.setForeground(Color.WHITE);
                    bpLabel.setBackground(Color.BLACK);
                    bpLabel.setOpaque(true);
                    peg_pos++;
                } //end for

                // Display "W" following the rules
                for (int i=0; i<bwpeg[1]; i++) {
                    int peg_pos_pixel = peg_pos*50 + 800;  //keep track of position of the pegs

                    JLabel wpLabel = new JLabel("W", JLabel.CENTER);
                    panelPlayerVsAI.add(wpLabel);
                    wpLabel.setBounds(peg_pos_pixel,line_pixel,50,30);
                    wpLabel.setFont(new Font("Arial", Font.BOLD, 20));
                    wpLabel.setForeground(Color.BLACK);
                    wpLabel.setBackground(Color.GRAY);
                    wpLabel.setOpaque(true);
                    peg_pos++;
                } //end for

                // Display "-" following the rules
                for (int i=0; i<4-(bwpeg[0]+bwpeg[1]); i++) {
                    int peg_pos_pixel = peg_pos*50 + 800;  //keep track of position of the pegs

                    //display "w" on GUI
                    JLabel wpLabel = new JLabel("-", JLabel.CENTER);
                    panelPlayerVsAI.add(wpLabel);
                    wpLabel.setBounds(peg_pos_pixel,line_pixel,50,30);
                    wpLabel.setFont(new Font("Arial", Font.BOLD, 20));
                    wpLabel.setForeground(Color.BLACK);
                    wpLabel.setBackground(Color.LIGHT_GRAY);
                    wpLabel.setOpaque(true);
                    peg_pos++;
                } //end for

                System.out.println(guessed_code);
                guessed_code.clear();  //remove previous guesses
                text_pos = 0; //reset position
                round++; // Goes to next round of 4 digit code guessing
            } //end if

            // If the number of the black pegs is 4 (Player wins)
            if(bwpeg[0]==4){  //4 black pegs
                JTextField wonField = new JTextField ("YOU WON");
                panelPlayerVsAI.add(wonField);
                wonField.setBounds(675,600,200,30);
                wonField.setHorizontalAlignment(JTextField.CENTER);
                wonField.setFont(new Font("Arial", Font.BOLD, 20));
                wonField.setForeground(Color.BLACK);
                wonField.setBackground(Color.WHITE);
                wonField.setOpaque(true);
                int line = round; //value of 'round' will change later, so store the value in 'line' for later use
                round = 10;  //set round to 10 so that game is now over

                // record-to_file
                record_to_sbfile("Game " +game+ ": You were able to guess the code in " +line+ " tries");
                game++;


            } //end if

            //Less than 4 black pegs found after all 10 rounds (Player lost)
            if((bwpeg[0]<4)&&(round == 10)){
                JTextField loseField = new JTextField ("YOU LOST");
                panelPlayerVsAI.add(loseField);
                loseField.setHorizontalAlignment(JTextField.CENTER);
                loseField.setBounds(675,600,200,30);
                loseField.setFont(new Font("Arial", Font.BOLD, 20));
                loseField.setForeground(Color.BLACK);
                loseField.setBackground(Color.WHITE);
                loseField.setOpaque(true);
                //record to file: Player lost
                record_to_sbfile("Game " +game+ ": You were not able to guess the code");

                game++;  //keep track of the number of games
            } //end if
        } //end else if

        // If Delete is clicked
        if (command.equals("Delete")){
            guessed_code.clear();  //remove previous guess
            //reset position
            text_pos = 0;
            peg_pos = 0;

            //Hide previous input
            JLabel qLabel = new JLabel("Q", JLabel.CENTER);
            panelPlayerVsAI.add(qLabel);
            qLabel.setBounds(500,line_pixel,550,30);
            qLabel.setFont(new Font("Arial", Font.BOLD, 20));
            qLabel.setForeground(Color.WHITE);
            qLabel.setBackground(Color.WHITE);
            qLabel.setOpaque(true);

            //Hide previous messages
            panelPlayerVsAI.add(sLabel);
            sLabel.setBounds(500,600,550,30);
            sLabel.setFont(new Font("Arial", Font.BOLD, 20));
            sLabel.setForeground(Color.WHITE);
            sLabel.setBackground(Color.WHITE);
            sLabel.setOpaque(true);

        } //end if

        // If new game is clicked
        if (command.equals("New Game")){
            start_new_game();
        } //end if

        //Claire_PvA **************************************************************
    } //end method actionPerformed


    public String[] random_code() {  //This method generates a random code for PvA
        String gc[] = new String[4];

        for (int i = 0; i<4; i++){
            double random_colour = Math.random()*6;  //6 colours
            if (random_colour <= 1){
                gc[i] = "B";
            }
            else if ((1< random_colour) && (random_colour <= 2)){
                gc[i] = "G";
            }
            else if ((2< random_colour) && (random_colour <= 3)){
                gc[i] = "O";
            }
            else if ((3< random_colour) && (random_colour <= 4)){
                gc[i] = "P";
            }
            else if ((4< random_colour) && (random_colour <= 5)){
                gc[i] = "R";
            }
            else if ((5< random_colour) && (random_colour <= 6)){
                gc[i] = "Y";
            }
        } //end for loop

        //Print answer key for PvA
        System.out.print(gc[0]);
        System.out.print(gc[1]);
        System.out.print(gc[2]);
        System.out.println(gc[3]);

        return gc; //return random code

    } //end method random_code


    //Claire_PvA **************************************************************
    public int[] imAI(ArrayList<String> guessc,String[] genc) {  //this method determines the number of black and white pegs

        //a flag to make sure that there are no duplicate results for a certain colour
        boolean found_in_guess[] = {false,false,false,false};
        boolean found_in_gen[] = {false,false,false,false};

        // Initialize bwpeg
        // {number of black pegs,number of white pegs}
        for (int i=0; i<bwpeg.length; i++)
            bwpeg[i] = 0;

        // Calculate #black pegs
        for (int i = 0; i < genc.length; i++) {
            if (genc[i] == guessc.get(i)){
                found_in_guess[i] = true; //flag
                found_in_gen[i] = true; //flag
                //bpeg[i] = "b";
                bwpeg[0] += 1; //add 1 to the number of black pegs
                System.out.println("b");

            } //end if
        } //end for

        // Calculate #white pegs
        for (int i = 0; i < guessc.size(); i++){
            if (found_in_guess[i] == true)
                continue;
            for (int j = 0; j < genc.length; j++){
                if (found_in_gen[j] == true)
                    continue;
                //search for same string value when i != j
                if (guessc.get(i) == genc[j]){
                    found_in_guess[i] = true;
                    found_in_gen[j] = true;
                    bwpeg[1] += 1; //add 1 to the number of white pegs
                    System.out.println("w");
                    break;
                } //end if
            } //end for
        } //end for
        return bwpeg;
    } //end method imAi

    public void start_new_game() {  //this method resets and start a new game

        round = 0; //reset round
        guessed_code.clear();  //remove previous guesses
        generated_code = random_code();  //generate new code
        //reset positions
        text_pos = 0;
        peg_pos = 0;
        //Initialize bwpeg
        // {number of black pegs,number of white pegs}
        for (int i=0; i<bwpeg.length; i++)
            bwpeg[i] = 0;

        // Redraw for a new game
        panelPlayerVsAI.removeAll();
        set_new_display();
        revalidate();
        repaint();

        //record to file: new game started
        record_to_sbfile("The game has been reset.");

    }

    public void record_to_sbfile(String mesg) {  //Write to scoreboard.txt
        try {
            FileWriter writer = new FileWriter("SCOREBOARD.txt",true);
            writer.write(mesg);
            writer.write(System.getProperty( "line.separator" ));
            writer.close();
            System.out.println("Successfully recorded score");
        } //end try
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } //end catch
    }

    public void set_new_display() {  //this method displays the state of new game

        // Add white background for the labels
        panelPlayerVsAI.add(nothing);
        nothing.setBounds(500,25,550,700);
        nothing.setBackground(Color.WHITE);
        nothing.setOpaque(true);

        // Add the messages
        panelPlayerVsAI.add(messageDisplayAtPvA);
        messageDisplayAtPvA.setBounds(100,80,300,30);
        messageDisplayAtPvA.setFont(new Font("Arial", Font.BOLD, 20));
        messageDisplayAtPvA.setForeground(Color.BLACK);
        messageDisplayAtPvA.setBackground(Color.LIGHT_GRAY);
        messageDisplayAtPvA.setEditable(false);

        panelPlayerVsAI.add(messageDisplayAtPvA2);
        messageDisplayAtPvA2.setBounds(40,40,450,30);
        messageDisplayAtPvA2.setFont(new Font("Arial", Font.BOLD, 17));
        messageDisplayAtPvA2.setForeground(Color.BLACK);
        messageDisplayAtPvA2.setBackground(Color.LIGHT_GRAY);
        messageDisplayAtPvA2.setEditable(false);

        panelPlayerVsAI.add(messageDisplayAtPvA3);
        messageDisplayAtPvA3.setBounds(50,720,450,30);
        messageDisplayAtPvA3.setFont(new Font("Arial", Font.BOLD, 15));
        messageDisplayAtPvA3.setForeground(Color.BLACK);
        messageDisplayAtPvA3.setBackground(Color.LIGHT_GRAY);
        messageDisplayAtPvA3.setEditable(false);

        // Add blue button & get positioned
        panelPlayerVsAI.add(bButtonPvA);
        bButtonPvA.setBounds(100,120,50,30);
        bButtonPvA.setForeground(Color.BLACK);
        bButtonPvA.setBackground(Color.BLUE);
        bButtonPvA.setOpaque(true);
        bButtonPvA.setFont(new Font("Arial", Font.BOLD, 20));

        // Add green button & get positioned
        panelPlayerVsAI.add(gButtonPvA);
        gButtonPvA.setBounds(150,120,50,30);
        gButtonPvA.setForeground(Color.BLACK);
        gButtonPvA.setBackground(Color.GREEN);
        gButtonPvA.setOpaque(true);
        gButtonPvA.setFont(new Font("Arial", Font.BOLD, 20));

        // Add orange button & get positioned
        panelPlayerVsAI.add(oButtonPvA);
        oButtonPvA.setBounds(200,120,50,30);
        oButtonPvA.setForeground(Color.BLACK);
        oButtonPvA.setBackground(Color.ORANGE);
        oButtonPvA.setOpaque(true);
        oButtonPvA.setFont(new Font("Arial", Font.BOLD, 20));

        // Add purple button & get positioned
        panelPlayerVsAI.add(pButtonPvA);
        pButtonPvA.setBounds(250,120,50,30);
        pButtonPvA.setForeground(Color.BLACK);
        pButtonPvA.setBackground(new Color(0x800080));
        pButtonPvA.setOpaque(true);
        pButtonPvA.setFont(new Font("Arial", Font.BOLD, 20));

        // Add red button & get positioned
        panelPlayerVsAI.add(rButtonPvA);
        rButtonPvA.setBounds(300,120,50,30);
        rButtonPvA.setForeground(Color.BLACK);
        rButtonPvA.setBackground(Color.RED);
        rButtonPvA.setOpaque(true);
        rButtonPvA.setFont(new Font("Arial", Font.BOLD, 20));

        // Add yellow button & get positioned
        panelPlayerVsAI.add(yButtonPvA);
        yButtonPvA.setBounds(350,120,50,30);
        yButtonPvA.setForeground(Color.BLACK);
        yButtonPvA.setBackground(Color.YELLOW);
        yButtonPvA.setOpaque(true);
        yButtonPvA.setFont(new Font("Arial", Font.BOLD, 20));

        // Add Check button & get positioned
        panelPlayerVsAI.add(checkButtonPvA);
        checkButtonPvA.setBounds(50,200,200,200);
        checkButtonPvA.setForeground(Color.BLACK);
        checkButtonPvA.setBackground(Color.WHITE);
        checkButtonPvA.setOpaque(true);
        checkButtonPvA.setFont(new Font("Arial", Font.BOLD, 20));

        // Add Delete button & get positioned
        panelPlayerVsAI.add(deleteButtonPvA);
        deleteButtonPvA.setBounds(250,200,200,200);
        deleteButtonPvA.setForeground(Color.BLACK);
        deleteButtonPvA.setBackground(Color.WHITE);
        deleteButtonPvA.setOpaque(true);
        deleteButtonPvA.setFont(new Font("Arial", Font.BOLD, 20));

        // Add Newgame button & get positioned
        panelPlayerVsAI.add(newgameButtonPvA);
        newgameButtonPvA.setBounds(50,400,400,300);
        newgameButtonPvA.setForeground(Color.BLACK);
        newgameButtonPvA.setBackground(Color.WHITE);
        newgameButtonPvA.setOpaque(true);
        newgameButtonPvA.setFont(new Font("Arial", Font.BOLD, 20));

    }


    //Main method
    public static void main(String[] args) {
        new GUI_1();  //Start the GUI
    }   //End of Main method
}   //End of public class GUI_Test
