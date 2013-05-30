package checkers;

import java.awt.*;
import java.awt.event.*;
import mytools.*;
import treegame.*;

public class Console extends FancyPanel
   implements ActionListener {

   // Output register:

   public static final int OUT_BUTTON = 0;
   private static final int OUT_LEN = 1;

   public static final int BUTTON_START = 0;
   public static final int BUTTON_UNDO = 1;
   public static final int BUTTON_REDO = 2;
   public static final int BUTTON_RESTART = 3;
   public static final int BUTTON_PAUSE = 4;
   public static final int BUTTON_RESUME = 5;

   /**
    * Constructor
    */
	public Console(Checkers ck, Rules r) {
      super(null,STYLE_PLAIN,new Color(200,200,180));
      rules = r;
//      checkers = ck;

		GridLayout gl = new GridLayout(0,2);
		gl.setHgap(3);
		gl.setVgap(3);
		setLayout(gl);

      addButtons();
   }

 	// Test if we should update the enabling or label of the button.
	public void updateButtonEnabling(boolean fInit) {
      synchronized(rules) {
         for (int iButton = 0; iButton < BUTTONS; iButton++) {
            int text = iButton;
            boolean enabled = true;
            switch (iButton) {
            case BUTTON_RESTART:
               if (rules.state() != Rules.STATE_PLAYING
                || rules.getMoveNumber() == 0
               )
                  enabled = false;
               break;
            case BUTTON_START:
               // If game is over, set MENU_START.
               // else, if current player is computer, set PAUSE or RESUME, depending if game is paused.
               // else, set PAUSE and disable
               if (rules.state() == Rules.STATE_PLAYING) {
                  if (!rules.humanController(rules.turn())) {
                     text = (rules.isGamePaused() ?
                        BUTTON_RESUME : BUTTON_PAUSE);
                  } else {
                     text = BUTTON_PAUSE;
                     enabled = false;
                  }
               }
               break;
            case BUTTON_UNDO:
               enabled = rules.undoPossible();
               break;
            case BUTTON_REDO:
               enabled = rules.redoPossible();
               break;
            }
            if (fInit || enabled != fButtonsEnabled[iButton]) {
               buttons[iButton].setEnabled(enabled);
               fButtonsEnabled[iButton] = enabled;
            }
            if (fInit || text != iButtonTextDrawn[iButton]) {
               iButtonTextDrawn[iButton] = text;
               buttons[iButton].setLabel(buttonStrs[text]);
            }
         }
      }
	}

   private void addButtons() {
      for (int i = 0; i < BUTTONS; i++) {
         Button b = new Button();
         b.addActionListener(this);
         buttons[i] = b;
         add(b);
      }
		updateButtonEnabling(true);
   }

	private final String buttonStrs[] = {
		"Start",
      "Undo Move",
      "Redo Move",
		"New Game",

      "Pause",
      "Resume",
	};
   private static final int BUTTONS = 4;
   private Button[] buttons = new Button[BUTTONS];
   private boolean[] fButtonsEnabled = new boolean[BUTTONS];
   private int[] iButtonTextDrawn = new int[BUTTONS];

   // =================================================
   // ActionListener interface
	public void actionPerformed(ActionEvent e) {
      int i;
      for (i = 0; e.getSource() != buttons[i]; i++) ;

      synchronized(tcOut) {
         tcOut.setSignal();
         tcOut.setArg(OUT_BUTTON, iButtonTextDrawn[i]);
      }
	}
   // =================================================

   public ThreadCommand getReg() {
      return tcOut;
   }

   private Rules rules;
//   private Checkers checkers;
   private ThreadCommand tcOut = new ThreadCommand(OUT_LEN);
}
