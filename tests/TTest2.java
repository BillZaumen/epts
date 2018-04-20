import org.bzdev.geom.*;
import javax.swing.*;

public class TTest2 {
    public static void main(String argv[]) throws Exception {

	TransitionTable table = new TransitionTable();
	System.out.println("state = " + table.getCurrentState());
	for (JMenuItem item: table.getMenuItems()) {
	    System.out.format("menu item \"%s\", enabled = %b\n",
			      item.getText(), item.isEnabled());
	}
	System.out.println(table.nextState
			   (SplinePathBuilder.CPointType.MOVE_TO));
	System.out.println("state = " + table.getCurrentState());
	for (JMenuItem item: table.getMenuItems()) {
	    System.out.format("menu item \"%s\", enabled = %b\n",
			      item.getText(), item.isEnabled());
	}
	
	System.out.println(table.nextState
			   (SplinePathBuilder.CPointType.SPLINE));
	System.out.println("state = " + table.getCurrentState());
	for (JMenuItem item: table.getMenuItems()) {
	    System.out.format("menu item \"%s\", enabled = %b\n",
			      item.getText(), item.isEnabled());
	}
	System.out.println(table.nextState
			   (SplinePathBuilder.CPointType.SPLINE));
	System.out.println("state = " + table.getCurrentState());
	for (JMenuItem item: table.getMenuItems()) {
	    System.out.format("menu item \"%s\", enabled = %b\n",
			      item.getText(), item.isEnabled());
	}
	
	System.out.println(table.nextState
			   (SplinePathBuilder.CPointType.CLOSE));
	System.out.println("state = " + table.getCurrentState());
	for (JMenuItem item: table.getMenuItems()) {
	    System.out.format("menu item \"%s\", enabled = %b\n",
			      item.getText(), item.isEnabled());
	}

	System.out.println(table.nextState(EPTS.Mode.PATH_END));
	System.out.println("state = " + table.getCurrentState());
	for (JMenuItem item: table.getMenuItems()) {
	    System.out.format("menu item \"%s\", enabled = %b\n",
			      item.getText(), item.isEnabled());
	}

    }
}
