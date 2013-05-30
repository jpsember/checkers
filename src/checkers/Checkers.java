package checkers;
import treegame.*;
import java.awt.*;
import mytools.*;

public class Checkers extends java.applet.Applet implements Runnable {
	// =================================
	// Applet interface
	// =================================
	public void init() {
      prepareGameObjects();
      addComponents();
      // Make one of the panels trigger event thread code.
      brnPnl.attachEventThread(new EvtThread(this));
	}

	public void start() {
      // Create a thread that will monitor the game.  It will
      // feed instructions to the brain.
      thread = new Thread(this);
      thread.setDaemon(true);
      thread.start();
	}

	public void stop() {
      thread = null;
	}

	public void destroy() {
	}

	// =================================
	// Checkers thread
	// =================================
	public void run() {
      evalMove = new EvalMove();

      brain.startThread();

      while (thread != null) {
         evalMove.clear();
         readHumanMove();
         processBrain();
         processMove();
         processCtrlPnl();
         processConsole();
         processGUIEvents();
         ThreadCommand.sleep(100);
      }

      brain.stopThread();
      evalMove = null;
	}

   // Examine the output register from the Board to see if
   // human has selected a move.
   private void readHumanMove() {
      boolean fMoved;
      int iMove = 0;
      synchronized(tcBoardOut) {
         fMoved = tcBoardOut.testSignal();
         if (fMoved)
            iMove = tcBoardOut.getArg(BoardPnl.CMD_OUT_MOVE);
      }
      if (!fMoved) return;

      // If it is not the human's turn, ignore it.

      synchronized(rules) {
         if (rules.humanTurn()) {
            evalMove.setIndex(iMove);
            evalMove.setScore(0);
         }
      }
   }

   // Issue commands to, and read results from the brain thread.
   private void processBrain() {
      readBrnPnl();
      // If it is not the computer's turn, or the game is paused,
      // set the command to CMD_NONE.
      if (!rules.computerTurn() || rules.isGamePaused()) {
         setBrainTask(Brain.CMD_NONE, 0);
      } else {
         setBrainTask(Brain.CMD_CHOOSEMOVE,rules.getMoveNumber());
         synchronized(tcBrainIn) {
          synchronized(tcBrainOut) {
            // Has brain thought of a move yet?
            if (tcBrainOut.testSignal()) {
               // Yes, brain has thought of a move, so process it.
               evalMove.setIndex(tcBrainOut.getArg(Brain.OUT_CMD_MOVE));
               evalMove.setScore(tcBrainOut.getArg(Brain.OUT_CMD_SCORE));
               // Clear the input cmd to zero now that we have
               // processed the answer.
               setBrainTask(Brain.CMD_NONE, 0);
            }
         }}
      }

      // Find out what move the brain is considering.
      synchronized(tcBrainProgressOut) {
         if (tcBrainProgressOut.testSignal()) {
            ThreadEvent.postEvent(EVENT_THINKINGOFMOVE,
               new Integer(tcBrainProgressOut.getArg(
                  Brain.OUT_PROGRESS_MOVE)));
         }
      }
   }

   // Read the output registers from the brain panel.  If they
   // have changed, send them to the brain thread.
   private void readBrnPnl() {
      synchronized(tcBrnPnlOut) {
//         db.pr("readBrnPnl, tcBrnPnlOut = "+tcBrnPnlOut);
         if (tcBrnPnlOut.testSignal()) {
            synchronized(tcBrainAlgIn) {
               tcBrainAlgIn.setSignal();
               tcBrainAlgIn.setArg(Brain.IN_ALG_SEARCHDEPTH,
                  tcBrnPnlOut.getArg(BrnPnl.OUT_SEARCHDEPTH));
               tcBrainAlgIn.setArg(Brain.IN_ALG_ALGORITHM,
                  tcBrnPnlOut.getArg(BrnPnl.OUT_ALGORITHM));
               tcBrainAlgIn.setArg(Brain.IN_ALG_THINKAHEAD,
                  tcBrnPnlOut.getArg(BrnPnl.OUT_THINKAHEAD));
            }
         }
      }
   }

   // If we have a move to make, from either the human or computer,
   // process it.
   private void processMove() {
		if (evalMove.getIndex() >= 0) {
         EvalMove e = new EvalMove();
         evalMove.copyTo(e);
         ThreadEvent.postEvent(EVENT_PROCESSMOVE, e);
		}
   }

   private void processCtrlPnl() {
      boolean f = false;
      int iPlr = 0;
      int iCtrl = 0;
      synchronized(tcCtrlPnlOut) {
         f = tcCtrlPnlOut.testSignal();
         if (f) {
            iPlr = tcCtrlPnlOut.getArg(0);
            iCtrl = tcCtrlPnlOut.getArg(1);
         }
      }
      if (f) {
         ThreadEvent.postEvent(EVENT_CTRLCHANGE,
            new Point(iPlr, iCtrl) );
      }
   }

   // If there have been any events posted to the ThreadEvent queue,
   // handle them in the event dispatch thread.
   private void processGUIEvents() {
      if (ThreadEvent.isEmpty()) return;
      if (true) { // Method #1: trigger an update(), within which we
                  // can run thread event code.
         FancyPanel.processEvents(); //triggerEventDispatchThread();
      }
      /*
      else {    // Method #2: use EventQueue.invokeLater(), which
                  // is not supported in earlier JDK's.
         EventQueue.invokeLater(new EvtThread());
      }
      */
   }

   private void processConsole() {
      synchronized(tcConsoleOut) {
         if (tcConsoleOut.testSignal()) {
            ThreadEvent.postEvent(EVENT_CONSOLEBUTTON,
               new Integer(tcConsoleOut.getArg(Console.OUT_BUTTON)));
         }
      }
   }

   // End of Checkers thread
	// =================================

   // =======================================
   // Event thread
   private void prepareGameObjects() {
		rules = new CkRules();
		rules.prepareHistory();
		rules.resetGame(0);
//      rules.setControl(1,true);

      eval = new CkEval();
      brain = new Brain(rules, eval, 4);
      tcBrainIn = brain.getReg(0);
      tcBrainOut = brain.getReg(4);
      tcBrainAlgIn = brain.getReg(1);
      tcBrainProgressOut = brain.getReg(2);
      tcBrainSearchOut = brain.getReg(3);

		boardPnl = new BoardPnl(rules);   //game.humanMoveCmd(), (CkRules) rules,brain.algorithmIn, eval);
      tcBoardOut = boardPnl.getReg(1);
		console = new Console(this, rules);
      tcConsoleOut = console.getReg();
      msgPnl = new MsgPnl();

		brnPnl = new BrnPnl(rules);
      tcBrnPnlIn = brnPnl.getReg(0);
      tcBrnPnlOut = brnPnl.getReg(1);

		scorePnl = new ScorePnl(rules);
		turnPnl = new TurnPnl(rules, boardPnl);
		ctrlPnl = new CtrlPnl(rules);
      tcCtrlPnlOut = ctrlPnl.getReg(1);

      displayWelcomeMsg();
   }

   private void addComponents() {
      // Add the components to the applet.
		GridBagLayout gb = new GridBagLayout();
		setLayout(gb);

		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.CENTER;

      // make board panel as large as possible; others' sizes will be
   	// only as large as the largest
		FancyPanel.setGBC(gc, 0,0, 2,2, 100, 100);
		gb.setConstraints(boardPnl,gc);
      add(boardPnl);

		FancyPanel.setGBC(gc, 2,3, 1,1, 0, 0);
		gb.setConstraints(console,gc);
		add(console);

		FancyPanel.setGBC(gc, 0,3, 2,1, 0, 0);
		gb.setConstraints(msgPnl, gc);
		add(msgPnl);

		FancyPanel.setGBC(gc, 2,0, 1,1, 0, 0);
		gb.setConstraints(scorePnl,gc);
		add(scorePnl);

		FancyPanel.setGBC(gc, 2,1, 1,2, 0, 0);
		gb.setConstraints(brnPnl,gc);
		add(brnPnl);

		FancyPanel.setGBC(gc, 0,2, 1,1, 40, 0);
		gb.setConstraints(turnPnl,gc);
      add(turnPnl);

		FancyPanel.setGBC(gc, 1,2, 1,1, 60, 0);
		gb.setConstraints(ctrlPnl,gc);
      add(ctrlPnl);
   }

   private void displayWelcomeMsg() {
            final String msgs[] = {
               "Welcome to Checkers!",
               "This game was written in Java.",
               "Checkers is traditionally played on a board with black and green squares."
            };
            if (miscMsgIndex >= msgs.length)
               miscMsgIndex = 0;
            displayMsg(msgs[miscMsgIndex++]);
   }

/*
	private void processNewEvalOptions() {
		if (rules.state() == rules.STATE_PLAYING) {
			displayCurrentEval();
			boardPnl.testRepaint(true);
		}
	}
*/
/*
	private void displayCurrentEval() {
		int value = eval.process(rules, brain.algorithmIn.get(Brain.P_EVALOPT), true);
		if (brnPnl != null) {
			brnPnl.dataIn.set(0,rules.turn() ^ (value < 0 ? 1 : 0));
			brnPnl.dataIn.set(1,Math.abs(value));
		}
	}
*/
/*
	private void processMainThread() {proc
		// Get graphics to update any new progress message from the brain.
		boardPnl.procBrainMsg(brain.progressOut);
		brnPnl.updateStatsDisplay();
		// If brain panel has signalled a change in the evaluator options, process it.
		if (brnPnl.evalOptionsChanged())
			processNewEvalOptions();
	}
*/

	private CkRules rules;

//	private BoardGame game;
//	private Brain brain;
//	private CkEval eval;

	private TurnPnl turnPnl;
	private CtrlPnl ctrlPnl;
	private ScorePnl scorePnl;
	private BoardPnl boardPnl;
   private Thread thread;
   private Console console;
	private MsgPnl msgPnl;
	private BrnPnl brnPnl;

	private int miscMsgIndex;

//   private boolean fInit;
   private EvalMove evalMove;

   private void displayMsg(String s) {
//      db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");
      msgPnl.set(s);
      iMsgIndex = 0;
   }
   private int iMsgIndex;

   private int startPlayer = 0;
   private ThreadCommand tcConsoleOut;
   private ThreadCommand tcBoardOut;
   private ThreadCommand tcBrainProgressOut;
   private ThreadCommand tcBrainIn, tcBrainOut;
   private ThreadCommand tcBrainAlgIn;
   private ThreadCommand tcBrainSearchOut;
   private static final int EVENT_CONSOLEBUTTON = 1;
   private static final int EVENT_PROCESSMOVE = 2;
   private static final int EVENT_THINKINGOFMOVE = 3;
   private static final int EVENT_CTRLCHANGE = 4;
   private Brain brain;
   private CkEval eval;

   // Assign a new brain task if new is different than old.
   // If old task is CMD_CLEAR or CMD_PROCMOVE, test if they are
   // done; if not, block until they are done.
   private void setBrainTask(int iTask, int n) {
      int iOldTask = tcBrainIn.getArgLock(Brain.IN_CMD_TASK);
      if (iOldTask == Brain.CMD_CLEAR || iOldTask == Brain.CMD_PROCMOVE) {
         synchronized(tcBrainOut) {
            while (!tcBrainOut.testSignalLock()) {
               try {tcBrainOut.wait();} catch (InterruptedException e) {
                Debug.print("Interrupted exception in Checkers: "+e);
               }
               // What do we do with this interruptedException?
            }
         }
      }

      synchronized(tcBrainIn) {
         synchronized(tcBrainOut) {
            if (
               tcBrainIn.getArg(Brain.IN_CMD_TASK) != iTask
             || tcBrainIn.getArg(Brain.IN_CMD_MOVE) != n
            ) {
               tcBrainIn.setArg(Brain.IN_CMD_TASK, iTask);
               tcBrainIn.setArg(Brain.IN_CMD_MOVE, n);
               // Clear any old completed signal.
               tcBrainOut.testSignal();
            }
         }
      }
   }
   private ThreadCommand tcBrnPnlIn;
   private ThreadCommand tcBrnPnlOut;
   private ThreadCommand tcCtrlPnlOut;
   public void processEvents() {
//         db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");
         while (!ThreadEvent.isEmpty()) {
            ThreadEvent e = ThreadEvent.getEvent();
            switch (e.getId()) {
            case EVENT_CONSOLEBUTTON:
               procCONSOLEBUTTON(((Integer)e.getObj()).intValue());
               break;
            case EVENT_PROCESSMOVE:
               procPROCESSMOVE((EvalMove)e.getObj());
               break;
            case EVENT_THINKINGOFMOVE:
               boardPnl.updateBrainThinkingMove(
                  ((Integer)e.getObj()).intValue());
               break;
            case EVENT_CTRLCHANGE:
               procCTRLCHANGE((Point)e.getObj());
               break;
            }
         }
         console.updateButtonEnabling(false);
         brnPnl.testRepaint();
         scorePnl.testRepaint();
         turnPnl.testRepaint();
      }

      private void procCONSOLEBUTTON(int iButton) {
//         db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");
         switch (iButton) {
         case Console.BUTTON_START:
            if (rules.score(0) + rules.score(1) != 0)
               startPlayer ^= 1;
         case Console.BUTTON_RESTART:
            rules.resetGame(startPlayer);
            setPaused(false);
            setBrainTask(Brain.CMD_CLEAR,0);
            boardPnl.processNewGame();
            displayWelcomeMsg();
            break;

         case Console.BUTTON_PAUSE:
            setPaused(true);
            break;

         case Console.BUTTON_RESUME:
            setPaused(false);
            break;

         case Console.BUTTON_UNDO:
            if (rules.undoPossible()) {
               setPaused(true);
               procMoveNumber(-1);
               setBrainTask(Brain.CMD_CLEAR,0);
            }
            break;

         case Console.BUTTON_REDO:
            if (rules.redoPossible()) {
               setPaused(true);
               procMoveNumber(rules.getRedoMove());
               setBrainTask(Brain.CMD_CLEAR,0);
            }
            break;
         }
      }

      // Call with -1 to undo a move.
      private void procMoveNumber(int mvNumber) {
//            db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");
         if (mvNumber < 0) {
            synchronized(rules) {
               rules.unMove();
            }
         } else {
            synchronized(rules) {
               rules.makeMove(mvNumber, true);
            }
            // Send the message to the brain to process this move.
            setBrainTask(Brain.CMD_PROCMOVE, mvNumber);
         }

         // Post an event to process this move in the event dispatch thread.

         if (mvNumber < 0)
            procMOVEUNDONE();
         else
            procMOVEMADE();
      }

      private void procPROCESSMOVE(EvalMove e) {
//         db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");
         setPaused(false);
         processEvalMove(e);
         procMoveNumber(e.getIndex());
         rules.clearRedo();
      }

      private void procCTRLCHANGE(Point pt) {
//            db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");
         int plr = pt.x;
         boolean humanFlag = (pt.y != 0);

         // If we are switching the current player's turn from human
         // to computer, pause the game, and update the main menu panel.

         synchronized(rules) {
            if (rules.turn() == plr && !humanFlag)
               setPaused(true);
            rules.setControl(plr, humanFlag);
         }
      }

      private void procMOVEUNDONE() {
//            db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");
         boardPnl.processMove();
         msgPnl.clear();
         displayCurrentEval();
      }

      private void procMOVEMADE() {
//         db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");
         boardPnl.processMove();

         switch (rules.state()) {
         case Rules.STATE_DRAWNREP:
            displayMsg("Tie game!  Draw by repetition.  This position has appeared "+(CkRules.DRAW_REPEAT_COUNT+1)+" times.");
            break;

         case Rules.STATE_DRAWNSTAG:
            displayMsg("Tie game!  There have been no captures or progress towards crowning for "+CkRules.DRAW_STAG_COUNT+" moves.");
            break;

         case Rules.STATE_DRAWNMAXMOVES:
            displayMsg("Tie game!  The program's move capacity has been reached.");
            break;

         case Rules.STATE_PLAYING:
            displayCurrentEval();
            break;

         case Rules.STATE_WON:
         case Rules.STATE_WON+1:
            {
            int lastPlr = rules.turn() ^ 1;
               int nHumans = (rules.humanController(0) ? 1 : 0) + (rules.humanController(1) ? 1 : 0);
               if (nHumans == 1) {
                  displayMsg(rules.humanController(lastPlr) ? "You beat me!" : "I win!");
               } else {
                  displayMsg(rules.sideName(lastPlr) + " wins!");
               }
            }
            break;
         }

         if (rules.state() != Rules.STATE_PLAYING)
            tcBrnPnlIn.setArgLock(0, -1);	// set evaluation to undefined.
      }


      private void displayCurrentEval() {
//         db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");
         int value = eval.process(rules, 0, true);
         synchronized(tcBrnPnlIn) {
            tcBrnPnlIn.setSignal();
            tcBrnPnlIn.setArg(BrnPnl.IN_SIDEAHEAD,
               rules.turn() ^ (value < 0 ? 1 : 0));
            tcBrnPnlIn.setArg(BrnPnl.IN_ADVANTAGE,Math.abs(value));
         }
      }

      private void processEvalMove(EvalMove e) {
//            db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");

         // We calculated the moves to victory when thinking about this move.
         // Since we are making the move immediately, subtract one from the
         // count and only report it if it's still greater than zero.

         int movesToMate = e.movesToVictory();

         if (movesToMate > 1) {
            synchronized(rules) {
               int side = (rules.turn() + movesToMate - 1) & 1;
      			// if (!rules.humanController(side))
                  displayMsg(rules.sideName(side) +
                     " wins in " + (movesToMate-1));
                  iMsgIndex = 1;

            }
         } else if (iMsgIndex != 0)
            displayMsg("");

         // If a computer came up with this move, have the brain panel display
         // statistics about the search.
         synchronized(rules) {
            if (rules.computerTurn()) {
               tcBrnPnlIn.setArgLock(2,tcBrainSearchOut.getArgLock(0));
               tcBrnPnlIn.setArgLock(3,tcBrainSearchOut.getArgLock(1));
            }
         }

      }
      public void setPaused(boolean paused) {
//         db.a(fInit || EventQueue.isDispatchThread(),"not dispatch thread!");
         rules.setGamePaused(paused);
         if (paused && brain != null) {
            // Send brain message of 0 to clear any highlights from brain thinking.
            brain.setThinkingMove(0);
         }
      }
   }

class EvtThread implements Runnable {
   Checkers checkers;
   public EvtThread(Checkers c) {
      checkers = c;
   }
   public void run() {
      checkers.processEvents();
   }
}


