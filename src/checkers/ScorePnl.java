package checkers;
import java.awt.*;
import treegame.*;
import mytools.*;

public class ScorePnl extends FancyPanel {
	private int drawnScores[];
	private TextField tf[];
	private Rules rules;

	ScorePnl (Rules rulesParm) {
		super("SCORE",STYLE_PLAIN,new Color( 200,200,180 ));

		rules = rulesParm;

		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();

		gc.fill = GridBagConstraints.NONE;
		gc.insets = new Insets(2,3,2,3);

		setLayout(gb);

		// Add some text fields and labels for the two players.

		tf = new TextField[3];
		drawnScores = new int[3];

		for (int i=0; i<3; i++) {
			{
				String name;
				if (i < 2)
					name = rules.sideName(i) + ":";
				else
					name = "Tied:";

				Label l = new Label(name, Label.RIGHT);
				FancyPanel.setGBC(gc, 0,i, 1,1, 4, 5);
				gc.anchor = GridBagConstraints.EAST;
				gb.setConstraints(l,gc);
				add(l);
			}

			{
				TextField l = new TextField(2);
				drawnScores[i] = -1;
				l.setEditable(false);

				FancyPanel.setGBC(gc, 1,i, 1,1, 6, 0);
				gc.anchor = GridBagConstraints.CENTER;
				gb.setConstraints(l,gc);
				add(l);
				tf[i] = l;
			}
		}
		testRepaint();
	}

	// Test if the scores have changed since we last drew them.
	// If so, update them.
	void testRepaint() {
		for (int i = 0; i<3; i++) {
			int nScore = rules.score(i);
			if (nScore != drawnScores[i]) {
				drawnScores[i] = nScore;
				tf[i].setText(Integer.toString(nScore));
			}
		}
	}

}
