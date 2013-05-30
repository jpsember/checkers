package checkers;
import java.awt.*;
import java.awt.event.*;
import treegame.*;
import mytools.*;

public class BoardPnl extends FancyPanel
 implements MouseListener {

   /** 0..n-1 if human has selected a move */
   public static final int CMD_OUT_MOVE = 0;
   /** Length of OUTPUT ThreadCommand */
   public static final int CMD_OUT_LEN = 1;

	// Constructor
	BoardPnl(CkRules rulesParm) {
		super("Jeff's CHECKERS Game", STYLE_SUNKEN, new Color(200,200,180));
      makeDoubleBuffered();
		rules = rulesParm;
      initColors();
		highlightPreviousMove();		// In case we restarted applet in the middle of a game
		addMouseListener(this);
	}

	// Override component function to return desired size of board panel:
	public Dimension getPreferredSize() {
		Dimension out = super.getPreferredSize();
		out.height += SQUARE_WIDTH * CkRules.columns();
		out.width += SQUARE_WIDTH * CkRules.rows();
		return out;
	}

	// Plot a piece centered within a bounding rectangle
	public void plotPieceRect(Graphics g, int nPiece, Rectangle bounds, int hlType) {
		plotSinglePiece(g, nPiece, bounds, PIECE_HEIGHT/2, hlType, CkRules.kingFlag(nPiece));
		if (CkRules.kingFlag(nPiece))
			plotSinglePiece(g, nPiece, bounds, -PIECE_HEIGHT/2, hlType, false);
	}

   private void initColors() {
		// construct background and board colors.

      final int rgb[] = {
         57,200,80,
         180,180,180,
         230,230,230,
         80,210,120,
         232,232,232,

         40,120,40,	// frame 1
         80,160,80,	// frame 2

         100,100,255,		// eval text
      };

      colors = createColorTable(rgb);

		// construct colors for the pieces.

      final int rgbPieces[] = {
         // red pieces:
         180,80,80,
         200,120,120,
         // black pieces:
         60,60,60,
         120,120,120,
         // common 'destination' highlight color:
         200,200,100,
         // old move highlight colors, red & black:
         200+15,120+55,120+55,
         120+55,120+55,120+55,
      };
      pieceColors = createColorTable(rgbPieces);

   }

	// Plot a single checker
	// Precondition:
	//	g = graphics context
	//	nPiece = piece to plot
	//	bounds = rectangle to plot checker within
	//	yOffset = added to centered y-coordinate before plotting
	//	highlight = 0: none 1:highlight only 2:destination highlighting
	//	bottomHalf = true if plotting only the bottom half of a king
	private void plotSinglePiece(Graphics g, int nPiece, Rectangle bounds,
	 int yOffset, int highlight, boolean bottomHalf) {

		int colorIndex = (CkRules.pieceColor(nPiece) - 1) * 2;

		int startX = bounds.x + (bounds.width - PIECE_WIDTH) / 2;
		int startY = yOffset + bounds.y +
			(bounds.height - (PIECE_HEIGHT + PIECE_OVALHEIGHT)) / 2;

		if (highlight != 1) {
			g.setColor(pieceColors[colorIndex]);

			if (!bottomHalf)
				g.fillOval(startX, startY, PIECE_WIDTH, PIECE_OVALHEIGHT);
			g.fillOval(startX, startY + PIECE_HEIGHT, PIECE_WIDTH, PIECE_OVALHEIGHT);
			g.fillRect(startX, startY + PIECE_OVALHEIGHT/2, PIECE_WIDTH, PIECE_HEIGHT);
		}

		if (highlight == 2)
			g.setColor(pieceColors[4]);
		else if (highlight == 1)
			g.setColor(pieceColors[CkRules.pieceColor(nPiece)+4]);
		else
			g.setColor(pieceColors[colorIndex + 1]);

		if (!bottomHalf)
			g.drawArc(startX, startY, PIECE_WIDTH, PIECE_OVALHEIGHT, 0, 360);
		g.drawArc(startX, startY + PIECE_HEIGHT, PIECE_WIDTH, PIECE_OVALHEIGHT,
			180,180);

		g.drawLine(startX, startY + PIECE_OVALHEIGHT/2,
			startX, startY + PIECE_OVALHEIGHT/2 + PIECE_HEIGHT-1);
		g.drawLine(startX + PIECE_WIDTH-1, startY + PIECE_OVALHEIGHT/2,
			startX + PIECE_WIDTH-1, startY + PIECE_OVALHEIGHT/2 + PIECE_HEIGHT-1);
	}

	// Paint the board if necessary.
	public void paintInterior(Graphics g, boolean fBufferValid) {

		if (
			!sizeDefined
		 || !windowSize.equals(this.getInteriorSize())
		)
			defineSize();

		// Determine the clipping rectangle.  We don't need to update anything that
		// is completely outside this boundary.
		Rectangle clip = g.getClipBounds();

		// If the clip bounds is totally contained within the board, we don't need
		// to update the frame or background graphics.

		Rectangle boardRect = new Rectangle(cellsRect.x, cellsRect.y,
			cellSize.width * CkRules.columns(), cellSize.height * CkRules.rows());

//      if (!fBufferValid)
//         dirtyFlags = ~0;

		if (!rectContainsRect(boardRect,clip)) {
//			dirtyFlags = ~0;
			// Plot a frame around the board.
			drawFrame(g);
		}

		if (clip.intersects(boardRect))
			updateCells(g, clip);

//		dirtyFlags = 0;
	}

	// Test if current board state disagrees with the last drawn state,
	// and call repaint() if so.
	// Set repaintEvalValues true to force a redraw of evaluation values for
	// any pieces.
	public void testRepaint(boolean repaintEvalValues) {

		if (!sizeDefined) return;

		// Compare rules with last drawn state, and refresh cells that have changed.

		{
			// copy current rules to thread-safe location for comparisons.
			CkRules curr = new CkRules();
			synchronized(rules) {
				rules.copyTo(curr, false);
			}

			for (int r=0; r<CkRules.rows(); r++) {
				for (int c=0; c<CkRules.columns(); c++) {
					int v = curr.readCell(c,r);
					if (
						v != drawn.readCell(c,r)
					 || (repaintEvalValues && v != 0)
					) {
						repaintCell(c,r);
					}
				}
			}
			curr.copyTo(drawn, false);
		}

		{
			// Sort the desired array into decreasing order of cell index
			// for easy comparing.
			int i,j;
			for (i=0; i<HL_FLAG_MAX; i++) {
				for (j=i+1; j<HL_FLAG_MAX; j++) {
					if ((cellHFlags[i] & 0xff) < (cellHFlags[j] & 0xff)) {
						int temp = cellHFlags[i];
						cellHFlags[i] = cellHFlags[j];
						cellHFlags[j] = temp;
					}
				}
			}
		}

		// Compare the desired with the current.

		{
			int i = 0;
			int j = 0;

			while (true) {
				// if old is higher than new, mark old, increment old index.
				// if new is higher than old, mark new, increment new index.
				// if new == old, mark new if highlighting has changed,
				//		increment old & new indices.

				int iVal = (i < HL_FLAG_MAX) ? cellHFlags[i] : -1;
				int jVal = (j < HL_FLAG_MAX) ? drawnHFlags[j] : -1;

				if (iVal < 0 && jVal < 0) break;

				int iCell = (iVal >= 0) ? iVal & 0xff : 0;
				int jCell = (jVal >= 0) ? jVal & 0xff : 0;

				if ((iVal | jVal) == 0) break;

				int changedCell = -1;

				if (jCell > iCell) {
					changedCell = jCell;
					j++;
				} else if (iCell > jCell) {
					changedCell = iCell;
					i++;
				} else {
					if (iVal != jVal)
						changedCell = iCell;
					i++;
					j++;
				}

				if (changedCell > 0)
					repaintCell(CkRules.cellX(changedCell-1), CkRules.cellY(changedCell - 1));

			}

			// Update the list of what cells we've highlighted and drawn.
			for (i=0; i<HL_FLAG_MAX; i++)
				drawnHFlags[i] = cellHFlags[i];
		}

	}

	public void processNewGame() {
		removeAllHighlights();
		testRepaint(true);
	}

	private void removeAllHighlights() {
		cellHRemove(~0);
		brainMsgDrawn = 0;
	}

	private void highlightPreviousMove() {

		//	Remove any old highlighting.
		removeAllHighlights();

		if (rules.undoPossible()) {
			CkMove m = (CkMove) rules.getUndoMove();

			// Add highlighting for this last move.

			int squares = m.totalPositions(true);

//			db.pr("highlighting the undo move, sqr="+squares+", mv="+m);

			for (int i=0; i<squares; i++) {
				CkMove mh = m.getStepInMove(i, true);
				int piece = mh.startPiece();
//				db.pr(" step "+i+" is "+mh);
				if (i == squares-1) {
					cellHRemove(mh.startPosition(), HL_PIECE);
					cellHAdd(mh.startPosition(), HL_MISC);
				} else {
					cellHAdd(mh.startPosition(), 1 << (piece - 1));
				}
			}
		}

	}

	// Update board display after a move.
	public void processMove() {
		highlightPreviousMove();
		testRepaint(true);

	}

	private Rectangle cellRect(int x, int y) {
		return new Rectangle(
			x * cellSize.width + cellsRect.x,
			y * cellSize.height + cellsRect.y,
			cellSize.width, cellSize.height);
	}

	// Draw the frame around the board
	private void drawFrame(Graphics g) {
		for (int i = 1; i <= 6; i++) {
			int j = (i == 1 || i == 6) ? C_FRAME2 : C_FRAME1;
			Rectangle r = new Rectangle(
				cellsRect.x - i, cellsRect.y - i,
				cellsRect.width + i*2-1, cellsRect.height + i*2 - 1);
			g.setColor(colors[j]);
			g.drawRect(r.x, r.y, r.width, r.height);
		}
	}

	// Update the cells on the board that overlap the clip region.
	private void updateCells(Graphics g, Rectangle clip) {
   /*
		int evalFlags;
		synchronized(algorithmCmd) {
			evalFlags = algorithmCmd.get(Brain.P_EVALOPT);
		}
   */
		g.setFont(evalFont);
//		FontMetrics evalMetrics = g.getFontMetrics();
//		int fontAscent = evalMetrics.getAscent() - 2;

		int x,y;
		for (y=0; y<CkRules.rows(); y++) {
			for (x=0; x<CkRules.columns(); x++) {
				// Determine if this square is in the update region.
				Rectangle r = cellRect(x,y);
				if (!r.intersects(clip)) continue;

				int piece = rules.readCell(x,y);
//				int origPiece = piece;
				int cellNum = CkRules.cellIndex(x,y);
				int hlFlags = cellHRead(cellNum);

				// Plot background of cell.

				g.setColor(colors[C_GRID + ((x + y) & 1)]);
				g.fillRect(r.x, r.y, r.width, r.height);

				// If highlighting is occurring, just plot a highlight; no piece.

				int hlType = 0;

				if ((hlFlags & HL_PIECE) != 0) {
					hlType = 1;
					for (int i=0; i<4; i++) {
						if ((hlFlags & (1 << i)) != 0) {
							piece = i+1;
							break;
						}
					}
				}

				if ((hlFlags & HL_MISC) != 0)
					hlType = 2;

				if (piece != 0) {
					plotPieceRect(g, piece, r, hlType);

               /*
					// If we are to display the evaluation values, do so.

					if (
						origPiece != 0
					 && (evalFlags & BrnPnl.PE_DISPLAY) != 0
					) {
						g.setColor(colors[C_EVALTEXT]);
						for (int v=0; v<4; v++) {
							if ((evalFlags & (1 << v)) == 0) continue;
							String s = Integer.toString(eval.getSavedValue(x,y,v));

							int fx = r.x + 1 + ((v & 1) != 0 ? r.width/2 : 0);
							int fy = r.y + ((v & 2) != 0 ? r.height - 2 : fontAscent);

							g.drawString(s, fx, fy);
						}
					}
               */
				}
			}
		}

	}

	// Determine which square, if any, contains a point
	// Precondition:
	//	x,y contain point on screen
	// Postcondition:
	//	returns square 0...n, or -1 of not in a square
	private int ptToSquare(Point pt) {

		if (!cellsRect.contains(pt))
			return -1;

		int column = (pt.x - cellsRect.x) / cellSize.width;
		int row = (pt.y - cellsRect.y) / cellSize.height;

		return CkRules.cellIndex(column,row);
	}

	// Determine highlighting for a cell
	private int cellHRead(int cell) {
		for (int i=0; i<HL_FLAG_MAX; i++) {
			if ((cellHFlags[i] & 0xff) != cell + 1) continue;
			return (cellHFlags[i] >> 8);
		}
		return 0;
	}

	// Set highlighting flags for a cell
	// Precondition:
	//	cell = index to add highlighting for
	//	flags = flags to set for cell
	private void cellHAdd(int cell, int flags) {

		// Find this cell in the highlight list, or allocate new one.

		int i, j;
		j = -1;
		for (i=0; i < HL_FLAG_MAX; i++) {
			if (cellHFlags[i] == 0) {
				j = i;
				continue;		// we found an empty one
			}

			if ((cellHFlags[i] & 0xff) == cell + 1) {
				j = i;
				break;
			}
		}

		cellHFlags[j] |= (cell+1) | (flags << 8);
	}

	// Remove specified highlighting flags from a cell
	private void cellHRemove(int cell, int flags) {
		for (int i=0; i<HL_FLAG_MAX; i++) {
			int cCell = cellHFlags[i] & 0xff;
			if (cCell != cell+1) continue;

			int cFlags = cellHFlags[i] >> 8;

			if ((cFlags & flags) != 0) {
				cFlags &= ~flags;
				if (cFlags == 0)
					cCell = 0;
				cellHFlags[i] = cCell | (cFlags << 8);
			}
			break;
		}
	}

	// Remove all occurrences of specified highlighting flags
	private void cellHRemove(int flags) {
		for (int i=0; i<HL_FLAG_MAX; i++) {
			int cCell = cellHFlags[i] & 0xff;
			if (cCell == 0) continue;

			int cFlags = cellHFlags[i] >> 8;

			if ((cFlags & flags) != 0) {
				cFlags &= ~flags;
				if (cFlags == 0)
					cCell = 0;
				cellHFlags[i] = cCell | (cFlags << 8);
			}
		}
	}

	private void repaintCell(int x, int y) {
		Rectangle cRect = cellRect(x, y);
		repaint(REPAINT_DELAY, cRect.x, cRect.y, cRect.width, cRect.height);
	}

   public ThreadCommand getReg(int n) {
      ThreadCommand tc = null;
      switch (n) {
      case 1:
         tc = cmdOut;
         break;
      }
      return tc;
   }

   public void setReg(int n, ThreadCommand tc) {
      switch (n) {
      case 1:
         cmdOut = tc;
         break;
      }
   }

   // =======================================
	// MouseListener interface
   // =======================================
	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		// Pressed within bounds of board?
		int cell = ptToSquare(translatePoint(e.getPoint()));
		if (cell < 0) return;	// no.
		// Is it a human's turn?
		if (!rules.humanTurn()) return;		// returns false if state != PLAYING
		updateMoveEnter(cell, false);
	}
	public void mouseReleased(MouseEvent e) {
		if (!rules.humanTurn()) return;
		int cell = ptToSquare(translatePoint(e.getPoint()));
		updateMoveEnter(cell, true);
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
   // =======================================

	// Process a mouse press/release in a particular cell.
	private void updateMoveEnter(int cell, boolean releasedFlag) {
//		db.pr("updateMoveEnter cell="+cell+" released="+releasedFlag+" moveStepsEnt="+moveStepsEntered);
		if (!releasedFlag) {
			mousePressCell = cell + 1;
		} else {
			if (cell + 1 != mousePressCell) {
				moveStepsEntered = 0;
			} else {
				// He has released the mouse.  Add the cell he has released it in
				// to the edit list.  The piece type will get filled in below.
				if (moveStepsEntered == 0)
					removeAllHighlights();
				editCells[moveStepsEntered++] = cell;
			}

			// See if the cells he has entered match any legal moves so far.

			boolean matchFlag = false;
			boolean foundMove = false;
			int moveIndex = 0;

			if (moveStepsEntered > 0) {
//				db.pr("MoveSteps so far entered:");for (int j=0; j<moveStepsEntered; j++) db.pr(" "+editCells[j]);
				synchronized(rules) {
					for (int i=0; i<rules.legalMoveCount(); i++) {
						CkMove m = (CkMove) rules.legalMove(i);
						int steps = m.totalPositions(false);
//						db.pr(" seeing if it matches move "+i+", "+steps+" steps, "+m);
						int j;
						for (j=0; j<steps; j++) {
							if (j >= moveStepsEntered) break;

							CkMove mStep = m.getStepInMove(j, false);
//							db.pr("  step "+j+" startPos="+mStep.startPosition());
							if (mStep.startPosition() != (editCells[j] & 0xff)) break;

							// We have a match up to this point, so store the
							// piece type information in the upper byte of our array
							// so it knows how to highlight it.

							editCells[j] |= mStep.startPiece() << 8;
						}

//						db.pr(" j="+j+", moveStepsEntered="+moveStepsEntered );

						// If we ran out of steps in this move, skip it.
						if (j < moveStepsEntered) continue;

						// We have a match.  If we got to compare all the steps in
						// the move, it's a complete match.

						matchFlag = true;
						if (j == steps) {
							foundMove = true;
							moveIndex = i;
							moveStepsEntered = 0;
							break;
						}
					}
					if (!matchFlag) {
//						db.pr(" didn't match any moves, clearing MoveStepsEntered");
						moveStepsEntered = 0;
					}
				}
			}

			// Unhighlight everything so far if we have reset the move, or have
			// finished specifying one.

			if (moveStepsEntered == 0 && !matchFlag) {
				removeAllHighlights();

				// Highlight those pieces that have legal moves.

				highlightLegalMovePieces();
			} else {
				if (!foundMove) {
					// Highlight the latest cell we added.
					int hlCell = editCells[moveStepsEntered-1];
					int pieceType = (hlCell >> 8);
					cellHAdd(hlCell & 0xff, 1 << (pieceType - 1));
				} else {
					// Set this move as the one to make.
					synchronized(cmdOut) {
                  // If previous move has not been processed, ignore
                  // this one.
                  if (cmdOut.setSignal())
							cmdOut.setArg(CMD_OUT_MOVE, moveIndex);
					}
				}
			}
		}
		testRepaint(false);
	}

	// Highlight those pieces that have legal moves defined.
	private void highlightLegalMovePieces() {
		synchronized(rules) {
			for (int i = 0; i<rules.legalMoveCount(); i++) {
				CkMove m = (CkMove) rules.legalMove(i);
				cellHAdd(m.startPosition(), HL_MISC);
			}
		}
	}

   // (Event thread)
	public void updateBrainThinkingMove(int hlMove) {
		if (brainMsgDrawn == hlMove)
			return;

		// Erase old if necessary
		if (brainMsgDrawn != 0)
			cellHRemove(brainThinkCell, HL_MISC);
		brainMsgDrawn = hlMove;

		if (brainMsgDrawn != 0) {
			if (drawn.legalMoveCount() > brainMsgDrawn-1) {
				CkMove m = (CkMove)drawn.legalMove(brainMsgDrawn-1);
				brainThinkCell = m.startPosition();
				cellHAdd(brainThinkCell,HL_MISC);
			}
		}
		testRepaint(false);
   }

/*
	// Process a new message from the brain.
	// 0: not thinking
	// 1...x: thinking of move n+1
	public void procBrainMsg(ThrCmd msg) {
		int hlMove;
		synchronized(msg) {
			hlMove = msg.get(0);
		}

		if (hlMove == -1)
			hlMove = 0;

		if (brainMsgDrawn == hlMove)
			return;

		// Erase old if necessary
		if (brainMsgDrawn != 0)
			cellHRemove(brainThinkCell, HL_MISC);
		brainMsgDrawn = hlMove;

		if (brainMsgDrawn != 0) {
			if (drawn.legalMoveCount() > brainMsgDrawn-1) {
				CkMove m = (CkMove)drawn.legalMove(brainMsgDrawn-1);
				brainThinkCell = m.startPosition();
				cellHAdd(brainThinkCell,HL_MISC);
			}
		}
		testRepaint(false);
	}
*/
	// Set up dimensions and position of board graphics
	// Postcondition:
	//	windowSize	= size of content region
	//	cellSize = size of each square
	//	boardInset = position of first square
	//	cellsRect = position & size of all squares
	//	sizeDefined = true
	private void defineSize() {

		// Determine the size of the interior of the panel.
		windowSize = getInteriorSize();

		cellSize = new Dimension(SQUARE_WIDTH, SQUARE_WIDTH);
		Dimension size = new Dimension(
			cellSize.width * CkRules.columns(),
			cellSize.height * CkRules.rows());

		boardInset = new Point(
         (windowSize.width - size.width) / 2, // + insets.left,
			(windowSize.height - size.height) / 2 // + insets.top
      );

		cellsRect = new Rectangle(boardInset.x, boardInset.y, size.width, size.height);

		sizeDefined = true;
	}

	private static final int SQUARE_WIDTH = 40;
	private static final int PIECE_HEIGHT = 9;
	private static final int PIECE_WIDTH = 30;
	private static final int PIECE_OVALHEIGHT = 7;

	private CkRules rules;

	// 'drawn' will contain an image of the last board we drew, so we
	// can avoid redrawing pieces that have not changed.
	private CkRules drawn = new CkRules();
//		sizeDefined = false;
//		drawn = new CkRules();

	private static Font evalFont = new Font("Courier", Font.BOLD, 10);

	private boolean sizeDefined = false;
	private Dimension windowSize;		// size of content region
	private Dimension cellSize;
	private Rectangle cellsRect;
//	private int dirtyFlags;
	private static final int REPAINT_DELAY = 100;	// .1 seconds
//	private static final int DIRTY_PIECES	= 0x0002;
	private Point boardInset;
//	private static final int C_BGND = 0;
	private static final int C_GRID = 1;
//	private static final int C_GRID2 = 2;
//	private static final int C_LASTMOVE = 3;
//	private static final int C_TRACKMOUSE = 4;
	private static final int C_FRAME1 = 5;
	private static final int C_FRAME2 = 6;
//	private static final int C_EVALTEXT = 7;
	private Color colors[];
	private Color pieceColors[];
	private int mousePressCell = 0;		// 0: none, else 1+cell index
	private int moveStepsEntered = 0;	// # of locations user has specified
//	private static final int maxEditCells = 16;
	private int[] editCells = new int[HL_FLAG_MAX];
   		// low byte = cell, high byte = piece type
	private int brainMsgDrawn = 0;		// 0, or 1+index of move
	private int brainThinkCell;		// index of cell brain is thinking of moving

	// Cell highlighting:
	private int[] cellHFlags = new int[HL_FLAG_MAX];	// cell index in lower 8 bits, flags in upper
	private int[] drawnHFlags = new int[HL_FLAG_MAX];	// the flags that were last drawn
	private static final int HL_FLAG_MAX = 30;			// maximum number of highlight flags

	private static final int HL_RPAWN = 1;
	private static final int HL_RKING = 2;
	private static final int HL_BPAWN = 4;
	private static final int HL_BKING = 8;
	private static final int HL_PIECE = (HL_RPAWN|HL_RKING|HL_BPAWN|HL_BKING);
	private static final int HL_MISC = 16;				// highlight for destination position
 	private ThreadCommand cmdOut = new ThreadCommand(CMD_OUT_LEN);
}
