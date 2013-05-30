package checkers;
import treegame.*;
import java.awt.*;
import java.awt.event.*;
import mytools.*;

public class BrnPnl extends FancyPanel implements ItemListener {
   /** search depth */
   public static final int OUT_SEARCHDEPTH = 0;
   /** search algorithm to use (0...n) */
   public static final int OUT_ALGORITHM = 1;
   /** non-zero if thinking ahead should occur */
   public static final int OUT_THINKAHEAD = 2;
   /** evaluation options */
   public static final int OUT_EVALOPTIONS = 3;

   private static final int REG_OUT_LEN = 4;

	public static final int PE_MATERIAL = 0x0001;
	public static final int PE_POSITION = 0x0002;
	public static final int PE_MOBILITY = 0x0004;
	public static final int PE_ATTACK 	= 0x0008;
	public static final int PE_DISPLAY	= 0x4000;

	BrnPnl(Rules rulesParm) {

		super("BRAIN",STYLE_PLAIN,new Color(200,200,180));

		rules = rulesParm;

      initializeOutputRegister();

      initLayoutMgr();

		addHeader("SEARCH");
		depth = new Choice();
		{
			for (int i=1; i<=Search.MAX_PLY; i++)
				depth.add(Integer.toString(i));
			depth.addItemListener(this);
         depth.select(dataOut.getArg(OUT_SEARCHDEPTH));
		}
		addOurLComponent("Search Depth:",depth);

		alg = new Choice();
		{
			final String algs[] = {"MINIMAX","ALPHA/BETA"};
			for (int i=0; i<algs.length; i++)
				alg.add(algs[i]);
			alg.addItemListener(this);
         alg.select(dataOut.getArg(OUT_ALGORITHM));
		}
		addOurLComponent("Algorithm:",alg);

      searchBox = new Checkbox("",false);
      searchBox.addItemListener(this);
      searchBox.setState(dataOut.getArg(OUT_THINKAHEAD) != 0);
      addOurLComponent("Think Ahead:",searchBox);

		addHeader("EVALUATOR");
		{
			final String labels[] = {
				"Material:",
				"Position:",
				"Mobility:",
				"Attack:",
				"Display Values:",
			};
			evalBoxes = new Checkbox[EVAL_OPTIONS];
			for (int i = 0; i < EVAL_OPTIONS; i++) {
				Checkbox c = new Checkbox("", false);
				c.addItemListener(this);
            c.setState((dataOut.getArg(OUT_EVALOPTIONS) & (1 << i)) != 0);
				if (i == EVAL_OPTIONS-1)
					addOurLComponent(labels[i], c);
				else
					addSmallLComponent(labels[i], c);
            evalBoxes[i] = c;
			}
			if (nextX != 0) {
				nextX = 0;
				nextY++;
			}
		}

		addHeader("STATISTICS");

		{
			evaluation = new TextField(10);
			addOurLComponent("Board Value:",evaluation);
			evaluation.setEditable(false);

			nodeInfo1 = new TextField(7);
			addOurLComponent("Nodes traversed:",nodeInfo1);
			nodeInfo1.setEditable(false);

			if (false) {
				nodeInfo2 = new TextField(7);
				addOurLComponent("Evaluations:",nodeInfo2);
				nodeInfo2.setEditable(false);
			}
		}
		initFlag = false;

//		updateStatsDisplay();
	}

   private void initLayoutMgr() {
		gb = new GridBagLayout();
		setLayout(gb);

		gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.NONE;
		gc.insets = new Insets(1,2,1,2);
   }

	private void addOurLComponent(String text, Component c) {
		addLabel(text);
		addOurComponent(c);
	}

	private void addLabel(String text) {
		addOurComponent(new Label(text));
	}

	private void addHeader(String text) {
		Label n = new Label(text);
		if (nextX != 0) {
			nextX = 0;
			nextY++;
		}

		FancyPanel.setGBC(gc, nextX, nextY, 4,1, 0, 0);
		gc.anchor = GridBagConstraints.CENTER;
		gb.setConstraints(n, gc);
		add(n);
		nextY++;
	}

	private void addOurComponent(Component c) {
		FancyPanel.setGBC(gc, nextX, nextY, 2,1, 0, 0);
		gc.anchor = (nextX == 0) ? GridBagConstraints.EAST : GridBagConstraints.WEST;
		gb.setConstraints(c, gc);
		add(c);
		nextX+=2;
		if (nextX == 4) {
			nextX = 0;
			nextY++;
		}
	}

	private void addSmallLComponent(String text, Component c) {
		addSmallComponent(new Label(text));
		addSmallComponent(c);
	}

	private void addSmallComponent(Component c) {
		FancyPanel.setGBC(gc, nextX, nextY, 1,1, 0, 0);
		gc.anchor = ((nextX & 1) == 0) ? GridBagConstraints.EAST : GridBagConstraints.WEST;
		gb.setConstraints(c, gc);
		add(c);
		nextX++;
		if (nextX == 4) {
			nextX = 0;
			nextY++;
		}
	}

//	private final int SEARCH_OPTIONS = 1;
   /*
	private final int searchFlags[] = {
		Brain.PS_AHEAD,
	};
*/
	private final int EVAL_OPTIONS = 5;
	private final int evalFlags[] = {
		PE_MATERIAL,
		PE_POSITION,
		PE_MOBILITY,
		PE_ATTACK,
		PE_DISPLAY
	};

/*
	public boolean evalOptionsChanged() {
		boolean out = false;
		synchronized(signalChangeCmd) {
			out = (signalChangeCmd.get(0) != 0);
			signalChangeCmd.set(0,0);
		}
		return out;
	}
*/
	public void itemStateChanged(ItemEvent e) {
		// Don't process any changes if we are setting up the controls.
		if (initFlag) return;

      synchronized(dataOut) {

         dataOut.setSignal();
         dataOut.setArg(OUT_SEARCHDEPTH, depth.getSelectedIndex() + 1);
			dataOut.setArg(OUT_ALGORITHM, alg.getSelectedIndex());
         dataOut.setArg(OUT_THINKAHEAD, searchBox.getState() ? 1 : 0);

			int n = 0;
			for (int i = 0; i<EVAL_OPTIONS; i++)
				if (evalBoxes[i].getState())
					n |= evalFlags[i];
         dataOut.setArg(OUT_EVALOPTIONS, n);
		}
	}

/*
	// Change the displayed values to agree with a register object.
	private void updateDisplayedValues(ThrCmd v) {
		alg.select(v.get(Brain.P_ALGORITHM));
		depth.select(v.get(Brain.P_DEPTH) - 1);

//		depthValue.setText(Integer.toString(v.get(Brain.P_DEPTH)));
		for (int i = 0; i<SEARCH_OPTIONS; i++)
			searchBoxes[i].setState((v.get(Brain.P_SEARCHOPT) & searchFlags[i]) != 0);
		for (int i = 0; i<EVAL_OPTIONS; i++)
			evalBoxes[i].setState((v.get(Brain.P_EVALOPT) & evalFlags[i]) != 0);
	}
*/

/*
	// Update the statistics values if they have changed since we drew them.
	public void updateStatsDisplay() {
		boolean modified = false;

		synchronized(dataIn) {
			modified = !dataIn.equals(dataInDisp);
			if (modified)
				dataIn.copyTo(dataInDisp);
		}

		if (!modified) return;

		int side = dataInDisp.get(0);
		if (side < 0)
			evaluation.setText("");
		else {
			int n = dataInDisp.get(1);
			if (n == 0)
				evaluation.setText("EVEN");
			else
				evaluation.setText(rules.sideName(side) + " +" + n);
		}

		int trav = dataInDisp.get(2);
		int eval = dataInDisp.get(3);
		if ((trav | eval) == 0) {
			nodeInfo1.setText("");
//			nodeInfo2.setText("");
		} else {
			nodeInfo1.setText(Integer.toString(trav));
//			nodeInfo2.setText(Integer.toString(eval));
		}
	}
*/
//	public ThrCmd dataIn = new ThrCmd();

	//	For current board evaluation:
	//		[0] side with advantage (-1 if undefined)
	//		[1] amount of advantage (0: even)
	//	For brain search statistics:
	//		[2] number of nodes traversed
	//		[3] number of position evaluations performed
	//

//	private final int ALG_MINIMAX = 0;
//	private final int ALG_ALPHABETA = 1;
//	private final int ALG_TOTAL = 2;

	// dataOut
	//	Controls brain search parameters:
	//		[0] search depth
	//		[1] algorithm to use
	//		[2] search options
	//		[3] evaluator options
//	private ThrCmd dataOut;
//	private ThrCmd dataOutDisp = new ThrCmd();
//	private ThrCmd dataInDisp = new ThrCmd();
//	private ThrCmd signalChangeCmd = new ThrCmd();

	private Rules rules;
	private Choice alg;
	private Checkbox searchBox;
	private Choice depth;

	private TextField evaluation;
	private TextField nodeInfo1, nodeInfo2;

	private boolean initFlag = true;

	private Checkbox evalBoxes[];

	private GridBagLayout gb;
	private	GridBagConstraints gc;
	private int nextX = 0;
   private int nextY = 0;

   private void initializeOutputRegister() {
      dataOut.setSignal();
      dataOut.setArg(OUT_SEARCHDEPTH, 2);
      dataOut.setArg(OUT_ALGORITHM, 1);
      dataOut.setArg(OUT_THINKAHEAD, 1);
      dataOut.setArg(OUT_EVALOPTIONS, PE_MATERIAL | PE_POSITION);
   }

	private ThreadCommand dataIn = new ThreadCommand(REG_IN_LEN);
	private ThreadCommand dataOut = new ThreadCommand(REG_OUT_LEN);
//	private ThreadCommand dataOutDisp = new ThreadCommand(REG_OUT_LEN);

   public ThreadCommand getReg(int n) {
      ThreadCommand tc = null;
      switch (n) {
      case 0:
         tc = dataIn;
         break;
      case 1:
         tc = dataOut;
         break;
      }
      return tc;
   }

	/**
    * Input register for evaluation and search.
    * Signal : set true if registers have been modified
    *  and display needs to be updated to reflect this
    * IN_SIDEAHEAD side with advantage (-1 if undefined)
    * IN_ADVANTAGE amount of advantage (0: even)
    * IN_NODECOUNT number of nodes traversed
    * IN_EVALCOUNT number of position evaluations performed
    */
   public final static int IN_SIDEAHEAD = 0;
   public final static int IN_ADVANTAGE = 1;
   public final static int IN_NODECOUNT = 2;
   public final static int IN_EVALCOUNT = 3;
   private static final int REG_IN_LEN = 4;

   public void testRepaint() {
      synchronized(dataIn) {
         if (dataIn.testSignal()) {
            int side = dataIn.getArg(IN_SIDEAHEAD);
            if (side < 0)
               evaluation.setText("");
            else {
               int n = dataIn.getArg(IN_ADVANTAGE);
               if (n == 0)
                  evaluation.setText("EVEN");
               else
                  evaluation.setText(rules.sideName(side) + " +" + n);
            }

            int trav = dataIn.getArg(IN_NODECOUNT);
            int eval = dataIn.getArg(IN_EVALCOUNT);
            if ((trav | eval) == 0)
               nodeInfo1.setText("");
            else
               nodeInfo1.setText(Integer.toString(trav));
         }
      }
	}

}
