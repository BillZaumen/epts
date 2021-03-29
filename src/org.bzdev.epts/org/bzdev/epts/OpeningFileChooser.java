package org.bzdev.epts;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import javax.imageio.ImageIO;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import java.beans.*;

import org.bzdev.scripting.Scripting;
import org.bzdev.swing.WholeNumbTextField;

/**
 * Custom file chooser for selecting files rather than opening them or
 * saving them.
 */
public class OpeningFileChooser  {
    int width = 0;
    int height = 0;
    File selectedFile = null;

    public int getWidth() {
	return width;
    }

    public int getHeight() {
	return height;
    }

    public File getSelectedFile() {
	return selectedFile;
    }

    public static final int APPROVE_OPTION = JFileChooser.APPROVE_OPTION;
    public static final int CANCEL_OPTION = JFileChooser.CANCEL_OPTION;
    public static final int DIMENSIONS_OPTION = 2;

    boolean allowNewFile = true;

    String eptsFilterName;
    String[] eptsExtensions;
	

    /**
     * Constructor.
     */
    public OpeningFileChooser(String eptsFilterName, String[] eptsExtensions ) {
	this.eptsFilterName = eptsFilterName;
	this.eptsExtensions = eptsExtensions;
    }

    /*
    private static void showTree(String prefix, JComponent c) {
	for (int i = 0; i < c.getComponentCount(); i++) {
	    System.out.format("%scomponent %d: class = %s\n", prefix, i,
			      c.getComponent(i).getClass());
	    if (c.getComponent(i) instanceof JTextField)
		System.out.println("........  found a text field");
	    else if (c.getComponent(i) instanceof JButton) {
		JButton b = (JButton)c.getComponent(i);
		    System.out.format("........  found a JButton "
				      + "(text = \"%s\")\n", b.getText());
	    }
	    if (c.getComponent(i) instanceof JComponent) {
		showTree(prefix + "    ", (JComponent)c.getComponent(i));
	    }
	}
    }
    */
    private static JTextField findTextField(JComponent c) {
	try {
	    for (int i = 0; i < c.getComponentCount(); i++) {
		Component ci = c.getComponent(i);
		JTextField tmp = null;
		if (ci instanceof JPanel) {
		    tmp = findTextField((JComponent)ci);
		}
		if (tmp != null) {
		    return (JTextField) tmp;
		} else if (ci instanceof JTextField) {
		    return (JTextField)ci;
		}
	    }
	} catch (Exception e){}
	return null;
    }
    
    JComponent bc = null;
    JButton b1 = null;
    JButton b2 = null;
    int i1 = -1; int i2 = -1;
    private  void removeStdButtons(JComponent c, String text)  {
	try {
	    JButton b1 = null;
	    JButton b2 = null;
	    int i1 = -1; int i2 = -1;
	    for (int i = 0; i < c.getComponentCount(); i++) {
		Component ci = c.getComponent(i);
		if (ci instanceof JPanel) {
		    removeStdButtons((JComponent)ci, text);
		} else if (ci instanceof JButton) {
		    JButton b = (JButton) ci;
		    String txt = b.getText();
		    if (txt != null && txt.length() > 0) {
			if (b1 == null) {
			    b1 = b;
			    i1 = i;
			} else if (b2 == null) {
			    b2 = b;
			    i2 = i;
			}
		    }
		}
	    }
	    if (b1 != null && b2 != null) {
		if (b1.getText().equals(text)
		    || b2.getText().equals(text)) {
		    bc = c;
		    this.b1 = b1;
		    this.b2 = b2;
		    this.i1 = i1;
		    this.i2 = i2;
		    c.remove(i2);
		    c.remove(i1);
		}
	    }
	} catch (Exception e){}
    }
    private void restoreStdButtons() {
	if (bc != null && b1 != null && b2 != null) {
	    bc.add(b1, i1);
	    bc.add(b2, i2);
	    bc = null;
	    b1 = null;
	    b2 = null;
	    i1 = -1;
	    i2 = -1;
	}
    }

    private static final String DIMENSIONS_BUTTON_TEXT = "Use Dimensions";
    private static final String APPROVE_BUTTON_TEXT = "Use File";
    private static final String RESET_BUTTON_TEXT = "Clear File";
    private static final String CANCEL  = "Cancel";

    private int status = -1;

    public int showOpeningDialog(Component parent, File cdir)
    {
	Component top = SwingUtilities.getRoot(parent);
	Frame fOwner = null;
	Dialog dOwner = null;
	Window wOwner = null;
	if (top instanceof Frame) {
	    fOwner = (Frame) top;
	} else if (top instanceof Dialog) {
	    dOwner = (Dialog) top;
	} else if (top instanceof Window) {
	    wOwner = (Window) top;
	}

	Boolean ro = UIManager.getBoolean("FileChooser.readOnly");
	UIManager.put("FileChooser.readOnly", Boolean.TRUE);
	if (cdir == null) cdir = new File (System.getProperty("user.dir"));
	JFileChooser fileChooser = new JFileChooser(cdir);
	UIManager.put("FileChooser.readOnly", ro);
	fileChooser.setMultiSelectionEnabled(false);
	fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
	JTextField tf = findTextField(fileChooser);
	tf.setEditable(false);
	fileChooser.setAcceptAllFileFilterUsed(false);
	String[] extensions =ImageIO.getReaderFileSuffixes();
	FileNameExtensionFilter filter =
	    new FileNameExtensionFilter(EPTS.localeString("Images"),
					extensions);
	Set<String> extensionSet = Scripting.getExtensionSet();
	String[] sextensions =
	    extensionSet.toArray(new String[extensionSet.size()]);
	ArrayList<String> allExtensionsList = new ArrayList<>();
	for (String ext: eptsExtensions) {
	    allExtensionsList.add(ext);
	}
	// allExtensionsList.add("epts");
	for (String ext: extensions) {
	    allExtensionsList.add(ext);
	}
	for (String ext: sextensions) {
	    allExtensionsList.add(ext);
	}
	String[] allExtensions = allExtensionsList
	    .toArray(new String[allExtensionsList.size()]);
	FileNameExtensionFilter afilter =
	    new FileNameExtensionFilter
	    (EPTS.localeString("StatesImagesScripts"), allExtensions);
	String eptsExtensions[] = {"epts"};
	FileNameExtensionFilter efilter =
	    new FileNameExtensionFilter(eptsFilterName, eptsExtensions);
	fileChooser.addChoosableFileFilter(afilter);
	fileChooser.addChoosableFileFilter(efilter);
	fileChooser.addChoosableFileFilter(filter);
	FileNameExtensionFilter sfilter =
	    new FileNameExtensionFilter(EPTS.localeString("Scripts"),
					sextensions);
	fileChooser.addChoosableFileFilter(sfilter);
	    
	JPanel panel = new JPanel(new BorderLayout());
	// panel.add(this, BorderLayout.CENTER);
	panel.add(fileChooser, BorderLayout.CENTER);
	// setControlButtonsAreShown(false);
	fileChooser.setApproveButtonText(APPROVE_BUTTON_TEXT);
	removeStdButtons(fileChooser, fileChooser.getApproveButtonText());
	JPanel ctlpane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
	InputVerifier iv = new InputVerifier() {
		@Override
		public boolean verify(JComponent input) {
		    WholeNumbTextField wntf = (WholeNumbTextField) input;
		    return (wntf.getValue() > 0);
		}
	    };
	WholeNumbTextField wtf = new WholeNumbTextField(8);
	WholeNumbTextField htf = new WholeNumbTextField(8);
	wtf.setDefaultValue(0);
	htf.setDefaultValue(0);
	wtf.setText("1024");
	htf.setText("1024");
	wtf.setInputVerifier(iv);
	htf.setInputVerifier(iv);
	JLabel wl = new JLabel(EPTS.localeString("widthLabel"));
	JLabel hl = new JLabel(EPTS.localeString("heightLabel"));
	JPanel wPanel = new JPanel(new BorderLayout());
	JPanel hPanel = new JPanel(new BorderLayout());
	wPanel.add(wl, BorderLayout.NORTH);
	wPanel.add(wtf, BorderLayout.SOUTH);
	hPanel.add(hl, BorderLayout.NORTH);
	hPanel.add(htf, BorderLayout.SOUTH);
	ctlpane.add(wl);
	ctlpane.add(wtf);
	ctlpane.add(hl);
	ctlpane.add(htf);
	JButton acceptDimensionsButton =
	    new JButton("Accept Dimensions");

	JButton approveButton = new JButton (APPROVE_BUTTON_TEXT);
	JButton resetButton = new JButton (RESET_BUTTON_TEXT);
	JButton cancelButton = new JButton("Cancel");
	ctlpane.add(acceptDimensionsButton);
	ctlpane.add(approveButton);
	ctlpane.add(resetButton);
	ctlpane.add(cancelButton);
	panel.add(ctlpane, BorderLayout.SOUTH);
	String title = "EPTS";
	JDialog dialog = (fOwner != null)? new JDialog(fOwner, title, true):
	    ((dOwner != null)? new JDialog(dOwner, title, true):
	     ((wOwner != null)? new
	      JDialog(wOwner, title, Dialog.ModalityType.APPLICATION_MODAL):
	      new JDialog((Frame)null, title,  true)));
	fileChooser.addPropertyChangeListener((pce) -> {
		String pname = pce.getPropertyName();
		if (pname.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
		    boolean hasSelection =
			(fileChooser.getSelectedFile() != null);
		    approveButton.setEnabled(hasSelection);
		    resetButton.setEnabled(hasSelection);
		    acceptDimensionsButton.setEnabled(!hasSelection);
		}
	    });
	status = -1;
	ActionListener actionListener = (ae) -> {
	    Object src = ae.getSource();
	    if (src == acceptDimensionsButton) {
		status = OpeningFileChooser.APPROVE_OPTION;
		fileChooser.setSelectedFile(null);
		tf.setText("");
		width = wtf.getValue();
		height = htf.getValue();
		status = OpeningFileChooser.DIMENSIONS_OPTION;
	    } else if (src == approveButton) {
		selectedFile = fileChooser.getSelectedFile();
		status =  OpeningFileChooser.APPROVE_OPTION;
	    } else if (src == resetButton) {
		fileChooser.setSelectedFile(null);
		tf.setText("");
		tf.requestFocus();
		fileChooser.repaint();
	    } else if (src == cancelButton) {
		status = OpeningFileChooser.CANCEL_OPTION;
	    }
	    if (status > -1) {
		dialog.setVisible(false);
	    }
	};
	acceptDimensionsButton.addActionListener(actionListener);
	approveButton.addActionListener(actionListener);
	resetButton.addActionListener(actionListener);
	cancelButton.addActionListener(actionListener);
	approveButton.setEnabled((getSelectedFile() != null));

	dialog.add(panel);
	dialog.pack();
	dialog.setVisible(true);
	restoreStdButtons();
	dialog.getContentPane().removeAll();
	dialog.dispose();
	if (status == -1) status = CANCEL_OPTION;
	return status;
    }
}

