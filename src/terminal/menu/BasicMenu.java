package terminal.menu;

import terminal.core.QueryEvent;
import terminal.core.JTerminal;

import javax.swing.*;
import java.awt.*;

public class BasicMenu extends ListMenu<String> {
    private JTerminal terminal;
    private String[] strings;
    private JLabel[] labels;
    private int selection;


    BasicMenu(JTerminal term, String[] strings, int direction){
        this.terminal = term;
        this.labels = new JLabel[strings.length];
        this.strings = strings;
        initLayout();
        if(direction==VERTICAL){
            initVerticalMenu();
        } else {
            initHorizontalMenu();
        }
        selectItem(0);
    }

    private void initHorizontalMenu(){
        JTextArea textArea = new JTextArea();
        textArea.setBackground(background);
        textArea.setForeground(foreground);
        textArea.setText(terminal.getOutputComponent().getText());
        textArea.setFont(new Font("consolas", Font.PLAIN, terminal.getFontSize()));
        textArea.setEditable(false);
        textArea.setFocusable(false);

        JPanel labelPanel = new JPanel();
        labelPanel.setBackground(background);
        labelPanel.setForeground(foreground);
        labelPanel.setBorder(BorderFactory.createLineBorder(foreground,1));

        makeLabels();
        labelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        for (JLabel l:labels) {
            labelPanel.add(l);
        }
        this.add(textArea);
        this.add(labelPanel);
    }

    private void initVerticalMenu(){
        JTextArea textArea = new JTextArea();
        textArea.setBackground(background);
        textArea.setForeground(foreground);
        textArea.setText(terminal.getOutputComponent().getText());
        textArea.setFont(new Font("consolas", Font.PLAIN, terminal.getFontSize()));
        textArea.setEditable(false);
        textArea.setFocusable(false);
        JPanel menuPanel = new JPanel();
        menuPanel.setBackground(background);
        menuPanel.setForeground(foreground);
        menuPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel labelPanel = new JPanel();
        labelPanel.setBackground(background);
        labelPanel.setForeground(foreground);
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
        menuPanel.add(labelPanel);

        makeLabels();
        for(JLabel l:labels){
            labelPanel.add(l);
        }
        this.add(textArea);
        this.add(menuPanel);
    }

    private void makeLabels(){
        for(int i=0; i<strings.length; i++){
            JLabel label = new JLabel(strings[i]);
            label.setForeground(foreground);
            label.setBackground(background);
            label.setOpaque(true);
            label.setFont(new Font("consolas", Font.PLAIN, terminal.getFontSize()));
            labels[i] = label;
        }
    }
    @Override
    public void selectItem(int n){
        selection = n;
        labels[selection].setForeground(background);
        labels[selection].setBackground(highlight);
    }
    @Override
    public void deselectItem(){
        labels[selection].setForeground(foreground);
        labels[selection].setBackground(background);
    }

    @Override
    public int getNumLabels() {
        return strings.length;
    }

    @Override
    public void fireEvent (QueryEvent e){
        if(terminal !=null){
            terminal.queryActionPerformed(e);
        }
    }

    @Override
    public String getSelectedItem(){
        return this.strings[selection];
    }

    @Override
    public int getSelection() {
        return selection;
    }
}