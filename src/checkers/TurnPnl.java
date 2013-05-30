package checkers;
import java.awt.*;
import treegame.*;
import mytools.*;

public class TurnPnl extends FancyPanel {

	TurnPnl (Rules r, BoardPnl b) {
		super("TURN",STYLE_PLAIN,new Color(200,200,180));
		boardPnl = b;
      rules = r;
		testRepaint();
	}

	public void paintInterior(Graphics g, boolean fBufferValid) {

		drawnTurn = desiredTurn;
      clearRect(g);
		if (drawnTurn >= 0)
   		boardPnl.plotPieceRect(g, 1 + (2*drawnTurn), g.getClipBounds(),0);
	}

	public void testRepaint() {
		synchronized(rules) {
			desiredTurn = (rules.state() == Rules.STATE_PLAYING) ?
            rules.turn() : -1;
		}
		if (drawnTurn != desiredTurn)
			repaint();
	}
	private Rules rules;
	private BoardPnl boardPnl;
	private int drawnTurn, desiredTurn;
}
