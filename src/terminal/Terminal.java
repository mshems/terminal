package terminal;

import ui.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Terminal implements TerminalEventListener{
    private Dimension windowSize = new Dimension(850, 650);
    private TerminalIOComponent inputComponent;
    private TerminalIOComponent outputComponent;
    private JScrollPane scrollPane;
    private JPanel scrollPanel;
    private JFrame frame;
    private LinkedBlockingQueue<String> commandQueue;
    private LinkedList<String> commandTokens;
    private CommandMap commandMap;
    private boolean dualDisplay;

    public static final int LEFT_ALIGNED = 0;
    public static final int CENTERED = 1;
    public static final int RIGHT_ALIGNED = 2;

    public Terminal(String title, boolean dualDisplay){
        this.dualDisplay = dualDisplay;
        commandQueue = new LinkedBlockingQueue<>();
        commandTokens = new LinkedList<>();
        commandMap = new CommandMap();
        addDefaultCommands();
        initFrame(title);
    }

    private void addDefaultCommands(){
        commandMap.put("", ()->{});
        commandMap.put("clear", ()->this.clear());
    }

    /*
     * Initialize the Terminal window
     */
    private void initFrame(String title){
        frame = new JFrame(title);
        frame.setMinimumSize(windowSize);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        scrollPanel = new JPanel();
        scrollPanel.setLayout(new BoxLayout(scrollPanel, BoxLayout.Y_AXIS));
        if(dualDisplay){
            inputComponent = new TerminalIOComponent(false);
            inputComponent.setTerminalEventListener(this);
            outputComponent = new TerminalDisplayComponent();
            scrollPanel.add(outputComponent, BorderLayout.CENTER);
            outputComponent.setEditable(false);
            scrollPane = new JScrollPane(scrollPanel);
            frame.add(inputComponent, BorderLayout.SOUTH);
        } else {
            inputComponent = new TerminalIOComponent(true);
            inputComponent.setTerminalEventListener(this);
            outputComponent = inputComponent;
            scrollPanel.add(inputComponent);
            scrollPane = new JScrollPane(scrollPanel);
        }
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.pack();
    }

    /*
     * Enable and start the Terminal
     */
    public synchronized void start(){
        frame.setVisible(true);
        inputComponent.start();
        while(true) {
             try {
                 wait();
                 if (!commandQueue.isEmpty()) {
                     tokenize(commandQueue.take());
                     doCommand(commandTokens.peek());
                     //newLine();
                 }
                 inputComponent.advance();
             } catch (InterruptedException e) {
                //e.printStackTrace();
                 break;
             }
         }
    }

    private void tokenize(String command){
        String[] input = command
                .trim()
                .split("\\s+");
        Collections.addAll(commandTokens, input);
    }

    private void doCommand(String token){
        TerminalCommand command = commandMap.get(token);
        if(command != null) {
            command.executeCommand();
        } else {
            newLine();
            println("Command '"+token+"' not found");
        }
        commandTokens.clear();
    }

    /*
     * Wait for input, return the string entered
     */
    private synchronized String query(String queryPrompt){
        String input="";
        inputComponent.setCurrPrompt(queryPrompt);
        inputComponent.advance();
        inputComponent.setQuerying(true);
        synchronized (this) {
            try{
                this.wait();
                input = inputComponent.getInput();
            }catch (InterruptedException ex){
                //ex.printStackTrace();
            }
        }
        inputComponent.resetPrompt();
        newLine();
        return input.trim();
    }

    private synchronized <E> E queryMenu(ListMenu<E> menu){
        E obj = null;
        MenuKeyListener menuKeyListener = new MenuKeyListener(menu);
        inputComponent.removeTerminalKeyListener();
        inputComponent.addKeyListener(menuKeyListener);
        inputComponent.unmapArrows();
        scrollPane.setViewportView(menu);
        scrollPane.getViewport().setViewPosition(new Point(0,scrollPane.getViewport().getExtentSize().height));
        //scrollPane.
        if(!dualDisplay) {
            menu.addKeyListener(menuKeyListener);
            menu.requestFocusInWindow();
        }
        synchronized (this) {
            try{
                this.wait();
                obj = menu.getSelectedItem();
            }catch (InterruptedException ex){
                //ex.printStackTrace();
            }
        }
        inputComponent.removeKeyListener(menuKeyListener);
        inputComponent.addTerminalKeyListener();
        inputComponent.remapArrows();
        scrollPane.setViewportView(outputComponent);
        outputComponent.requestFocusInWindow();
        return obj;
    }

    public synchronized <E> E queryObjectListMenu(Map<String, E> map, int direction){
        ObjectListMenu<E> menu = new ObjectListMenu<E>(this, map, direction);
        return queryMenu(menu);
    }
    public synchronized String queryStringListMenu(String[] strings, int direction){
        StringListMenu menu = new StringListMenu(this, strings , direction);
        return (String) queryMenu(menu);
    }

    public String queryString(String queryPrompt, boolean allowEmptyString){
        while(true) {
            String input = query(queryPrompt);
            if (input.isEmpty() && allowEmptyString) {
                return input;
            } else if (!input.isEmpty()){
                return input;
            }
            println("Empty input not allowed");
        }
    }

    public boolean queryYN(String queryPrompt){
        switch(query(queryPrompt).toLowerCase()){
            case "y":
            case "yes":
                return true;
            default:
                return false;
        }
    }

    public Integer queryInteger(String queryPrompt, boolean allowNull){
        while(true) {
            try {
                return Integer.parseInt(query(queryPrompt));
            } catch (NumberFormatException e) {
                if(allowNull){
                    break;
                }
                println("Not an integer value");
            }
        }
        return null;
    }

    public Double queryDouble(String queryPrompt, boolean allowNull) {
        while (true) {
            try {
                return Double.parseDouble(query(queryPrompt));
            } catch (NumberFormatException e) {
                if (allowNull) {
                    break;
                }
                println("Not a double value");
            }
        }
        return null;
    }

    public Boolean queryBoolean(String queryPrompt, boolean allowNull){
        while(true) {
            switch (query(queryPrompt).toLowerCase()){
                case "t":
                case "true":
                    return true;
                case "f":
                case "false":
                    return false;
                default:
                    if(allowNull){
                        return null;
                    }
                    println("Not a boolean value");
            }
        }
    }

    public void putCommand(String key, TerminalCommand command){
        commandMap.put(key, command);
    }

    public void replaceCommand(String key, TerminalCommand command){
        commandMap.replace(key, command);
    }

    public void removeCommand(String key, TerminalCommand command) {
        commandMap.remove(key, command);
    }

    public void removeCommand(String key){
        commandMap.remove(key);
    }

    public LinkedList<String> getCommandTokens() {
        return commandTokens;
    }

    private void newLine(){
        inputComponent.newLine();
    }

    private void clear(){
        if(dualDisplay){
            outputComponent.clear();
        } else {
            inputComponent.clear();
        }
    }


    public void println(String str, int PRINT_FORMAT){
        switch (PRINT_FORMAT){
            case Terminal.LEFT_ALIGNED:
                outputComponent.print(str);
                break;
            case Terminal.CENTERED:
                outputComponent.printCentered(str);
                break;
            case Terminal.RIGHT_ALIGNED:
                outputComponent.printRightAligned(str);
                break;
            default:
                outputComponent.print(str);
                break;
        }
    }

    public void printf(String format, Object... args){
        outputComponent.print(String.format(format, args));
    }
    public void print(String s){
        outputComponent.print(s);
    }
    public void print(Integer n){
        outputComponent.print(n.toString());
    }
    public void print(Double d){
        outputComponent.print(d.toString());
    }
    public void print(Boolean b){
        outputComponent.print(b.toString());
    }
    public void println(String s){
        outputComponent.println(s);
    }
    public void println(Integer n){
        outputComponent.println(n.toString());
    }
    public void println(Double d){
        outputComponent.println(d.toString());
    }
    public void println(Boolean b){
        outputComponent.println(b.toString());
    }

    public synchronized void submitActionPerformed(SubmitEvent e) {
        this.notifyAll();
        try {
            commandQueue.put(e.inputString);
            inputComponent.updateHistory(e.inputString);
        }catch (InterruptedException ex){
            //ex.printStackTrace();
        }
    }

    public synchronized void queryActionPerformed(QueryEvent e) {
        this.notifyAll();
    }

    public synchronized void menuActionPerformed(MenuEvent e){
        this.notifyAll();
    }

    public TerminalIOComponent getInputComponent() {
        return inputComponent;
    }
    public TerminalIOComponent getOutputComponent() {
        return outputComponent;
    }
}
