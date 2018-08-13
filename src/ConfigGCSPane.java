import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.text.AbstractDocument;
import org.bzdev.lang.UnexpectedExceptionError;
import org.bzdev.math.RealValuedFunction;
import org.bzdev.graphs.RefPointName;
import org.bzdev.swing.*;
import org.bzdev.swing.text.*;
import org.bzdev.util.units.MKS;

public class ConfigGCSPane extends JPanel {

    // Need to be able to save the state just before
    // a dialog box appears so we can restore it if
    // the user hits the 'cancel' button.
    int savedUnitIndex;
    int savedRefPointIndex;
    String savedXString;
    String savedYString;
    String savedUsDistString;
    String savedGcsDistString;

    public void saveState() {
	savedUnitIndex = unitComboBox.getSelectedIndex();
	savedRefPointIndex = rpComboBox.getSelectedIndex();
	savedXString = xtf.getText().trim();
	savedYString = ytf.getText().trim();
	savedUsDistString = utf.getText().trim();
	savedGcsDistString = gcstf.getText().trim();
    }
    
    public void restoreState() {
	unitComboBox.setSelectedIndex(savedUnitIndex);
	rpComboBox.setSelectedIndex(savedRefPointIndex);
	xtf.setText(savedXString);
	ytf.setText(savedYString);
	utf.setText(savedUsDistString);
	gcstf.setText(savedGcsDistString);
    }

    static final Vector<String> units = new Vector<>(24);
    static final RealValuedFunction convert[] = {
	new RealValuedFunction() {public double valueAt(double x) {return x;}},
	
	new RealValuedFunction() {public double valueAt(double x) {
	    return MKS.nm(x);}},
	new RealValuedFunction() {public double valueAt(double x) {
	    return MKS.um(x);}},
	new RealValuedFunction() {public double valueAt(double x) {
	    return MKS.mm(x);}},
	new RealValuedFunction() {public double valueAt(double x) {
	    return MKS.cm(x);}},
	new RealValuedFunction() {public double valueAt(double x) {
	    return (x);}},
	new RealValuedFunction() {public double valueAt(double x) {
	    return MKS.km(x);}},
	new RealValuedFunction() {public double valueAt(double x) {
	    return MKS.inches(x);}},
	new RealValuedFunction() {public double valueAt(double x) {
	    return MKS.feet(x);}},
	new RealValuedFunction() {public double valueAt(double x) {
	    return MKS.yards(x);}},
	new RealValuedFunction() {public double valueAt(double x) {
	    return MKS.miles(x);}}
    };
    static RefPointName[] refpoints = RefPointName.values();
    static Vector<String> refpointStrings = new Vector<>();

    static {
	String values[] = {
	    "custom units",
	    "nm", "um", "mm", "cm", "m", "km",
	    "inches", "feet", "yards", "miles"
	};
	// explicity enumerate these inc ase the order in the bzdev
	// class library changes at some point.
	RefPointName[] names = {
	    RefPointName.UPPER_LEFT,
	    RefPointName.UPPER_CENTER,
	    RefPointName.UPPER_RIGHT,
	    RefPointName.CENTER_LEFT,
	    RefPointName.CENTER,
	    RefPointName.CENTER_RIGHT,
	    RefPointName.LOWER_LEFT,
	    RefPointName.LOWER_CENTER,
	    RefPointName.LOWER_RIGHT
	};
	for (String unit: values) {
	    units.add(unit);
	}
	for (RefPointName name: names) {
	    refpointStrings.add(name.toString());
	}
    }

    public double getXRefpoint() {return x;}
    
    public double getYRefpoint() {return y;}

    public RefPointName getRefPointName() {
	return refpoints[rpComboBox.getSelectedIndex()];
    }

    public static RefPointName getRefPointName(int index) {
	return refpoints[index];
    }

    public static double getGCSDist(String value, int unitIndex)
	throws NumberFormatException
    {
	return convert[unitIndex].valueAt(Double.valueOf(value));
    }

    public static double getGCSDist(double value, int unitIndex) {
	return convert[unitIndex].valueAt(value);
    }

    public double getScaleFactor() {
	return convert[unitComboBox.getSelectedIndex()].valueAt(gcsDist)
	    / usDist;
    }

    public void setDist(double distance) {
	usDist = distance;
	utf.setText(String.format("%10.3g", distance).trim());
    }

    double usDist = 1.0;
    double gcsDist = 1.0;
    double x;
    double y;
    VTextField utf;
    VTextField gcstf;
    VTextField xtf;
    VTextField ytf;

    CharDocFilter cdf = new CharDocFilter();
    InputVerifier vtfiv = new InputVerifier() {
	    public boolean verify(JComponent input) {
		JTextField tf = (VTextField)input;
		String string = tf.getText();
		try {
		    double value = new Double(string);
		    if (value > 0.0) {
			return true;
		    } else {
			return false;
		    }
		} catch (Exception e) {
		    return false;
		}
	    }
	};

    InputVerifier xytfiv = new InputVerifier() {
	    public boolean verify(JComponent input) {
		JTextField tf = (VTextField)input;
		String string = tf.getText();
		try {
		    double value = new Double(string);
		    return true;
		} catch (Exception e) {
		    return false;
		}
	    }
	};

    JComboBox<String> unitComboBox = new JComboBox<>(units);

    JComboBox<String> rpComboBox = new JComboBox<>(refpointStrings);

    public ConfigGCSPane() {
	super();
	cdf.setAllowedChars("09eeEE..,,++--");
	JLabel ul = new JLabel("User-Space Distance");
	utf = new VTextField(String.format("%10.3g", 1.0).trim(), 10) {
		@Override
		protected void onAccepted() {
		    try {
			String text = getText();
			if (text == null || text.length() == 0) {
			    usDist = 1.0;
			} else {
			    usDist = Double.valueOf(getText());
			}
		    } catch (Exception e) {
			throw new UnexpectedExceptionError(e);
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, "Must enter a positive number",
			 "Error", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)utf.getDocument()).setDocumentFilter(cdf);
	utf.setInputVerifier(vtfiv);
	JLabel points = new JLabel("pts (1/72 in.)");
	JLabel gcsl = new JLabel("GCS Distance");
	gcstf = new VTextField(String.format("%10.3g", 1.0).trim(), 10) {
		@Override
		protected void onAccepted() {
		    try {
			gcsDist = Double.valueOf(getText());
		    } catch (Exception e) {
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, "Must enter a positive number",
			 "Error", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)gcstf.getDocument()).setDocumentFilter(cdf);
	gcstf.setInputVerifier(vtfiv);
	xtf = new VTextField(String.format("%4.3g", 0.0), 10) {
		@Override
		protected void onAccepted() {
		    try {
			x = Double.valueOf(getText());
		    } catch (Exception e) {
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, "Must enter a real number",
			 "Error", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)xtf.getDocument()).setDocumentFilter(cdf);
	xtf.setInputVerifier(xytfiv);
	ytf = new VTextField(String.format("%4.3g", 0.0), 10) {
		@Override
		protected void onAccepted() {
		    try {
			y = Double.valueOf(getText());
		    } catch (Exception e) {
		    }
		}
		@Override
		protected boolean handleError() {
		    JOptionPane.showMessageDialog
			(this, "Must enter a real number",
			 "Error", JOptionPane.ERROR_MESSAGE);
		    return false;
		}
	    };
	((AbstractDocument)ytf.getDocument()).setDocumentFilter(cdf);
	ytf.setInputVerifier(xytfiv);
	JLabel xl = new JLabel("Reference Point X (GCS)");
	JLabel yl = new JLabel("Reference Point Y (GCS)");
	JLabel xyul = new JLabel("Reference Point");
	JLabel note =
	    new JLabel("Coordinates are in GCS units (custom units or meters)");

	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	setLayout(gridbag);
	c.insets = new Insets(5, 5, 5, 5);
	c.ipadx = 5;
	c.ipady = 5;
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = 1;
	gridbag.setConstraints(ul, c);
	add(ul);
	gridbag.setConstraints(utf, c);
	add(utf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(points, c);
	add(points);
	c.gridwidth = 1;
	gridbag.setConstraints(gcsl, c);
	add(gcsl);
	gridbag.setConstraints(gcstf, c);
	add(gcstf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(unitComboBox, c);
	add(unitComboBox);
	unitComboBox.setSelectedIndex(0);
	c.anchor = GridBagConstraints.CENTER;
	gridbag.setConstraints(note, c);
	add(note);
	c.anchor = GridBagConstraints.LINE_START;
	c.gridwidth = 1;
	gridbag.setConstraints(xl, c);
	add(xl);
	gridbag.setConstraints(yl, c);
	add(yl);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(xyul, c);
	gridbag.setConstraints(xyul, c);
	add(xyul);
	c.gridwidth = 1;
	gridbag.setConstraints(xtf, c);
	add(xtf);
	gridbag.setConstraints(ytf, c);
	add(ytf);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gridbag.setConstraints(rpComboBox, c);
	add(rpComboBox);
	rpComboBox.setSelectedIndex(RefPointName.LOWER_LEFT.ordinal());
    }

    public void setEditable(boolean editable) {
	unitComboBox.setEnabled(editable);
	rpComboBox.setEnabled(editable);
	xtf.setEditable(editable);
	ytf.setEditable(editable);
	utf.setEditable(editable);
	gcstf.setEditable(editable);
    }

}
