package checkers;
import treegame.*;
import mytools.*;

public class CkRules extends Rules {
	public final static int xMovesJ[] = {-2,2,2,-2};
	public final static int yMovesJ[] = {-2,-2,2,2};
	public final static int xMovesS[] = {-1,1,1,-1};
	public final static int yMovesS[] = {-1,-1,1,1};
	public final static int cMovesS[] = {-9,-7,9,7};
	public final static int cMovesJ[] = {-18,-14,18,14};

	private final static int maxBoardDimension = 8;
	private final static int columns = maxBoardDimension;
	private final static int rows = maxBoardDimension;
//	private final int maxJumps = 10;

	private final int MAX_MOVES_PER_GAME = 300;
	public final static int DRAW_REPEAT_COUNT = 4;
	public final static int DRAW_STAG_COUNT = 40;

	// This is just a guess:
	private final static int maxLegalMoves = 30;

	private byte boardCells[];
	private int piecesRemaining[];
	private int kingsCount[];

	public int maxMoves() {
		return maxLegalMoves;
	}

	public CkRules() {
		super();
		boardCells = new byte[columns * rows];
		piecesRemaining = new int[2];
		kingsCount = new int[2];
		maxMovesPerGame = MAX_MOVES_PER_GAME;
	}

	public Rules makeNew() {
		return new CkRules();
	}

	public void prepareHistory() {
		//db.a(history == null, "CkRules history already prepared!");
		//db.a(maxMovesPerGame != 0, "CkRules prepareHistory, maxMovesPerGame = 0");

		history = new History(maxMovesPerGame);
		prepareImageBuffer(4, DRAW_STAG_COUNT, DRAW_REPEAT_COUNT);
	}

	// Utility function:  converts x,y into index
	public static int cellIndex(int column, int row) {
		return column | (row << 3);
	}

	// Utility: converts index into x
	public static int cellX(int index) {
		return index & 7;
	}

	// Utility: converts index into y
	public static int cellY(int index) {
		return index >> 3;
	}

	public int totalPieces(int side) {
		return piecesRemaining[side];
	}

	public int totalKings(int side) {
		return kingsCount[side];
	}

	// copy rules image to another
	public void copyTo(Rules destParm, boolean includeImages) {
		CkRules dest = (CkRules)destParm;
		copyBase(dest, includeImages);
		for (int i=0; i<2; i++) {
			dest.piecesRemaining[i] = piecesRemaining[i];
			dest.kingsCount[i] = kingsCount[i];
		}
		for (int c = 0; c<columns; c++) {
			for (int r = 0; r<rows; r++)
				dest.setCell(c,r,readCell(c,r));
		}
	}

	public String sideName(int side) {
		return (side == 0 ? "Red" : "Black");
	}

	protected void resetBoard() {

		if (false) {
         Debug.print("CkRules: setting board for debug configuration");
			for (int i = 0; i<2; i++) {
				piecesRemaining[i] = 0;
				kingsCount[i] = 0;
			}

			for (int i=0; i<64; i++)
				boardCells[i] = 0;

			final int data[] = {
			4,0,3,
			2,2,3,

			1,4,3,
			4,7,2,

			0
			};

			for (int i = 0; data[i] > 0; i += 3) {
				int piece = data[i+0];

				setCell(data[i+1],data[i+2],piece);

				int side = pieceColor(piece);

            Debug.print("piece="+piece+" side="+side);
				piecesRemaining[side-1]++;
				if (kingFlag(piece))
					kingsCount[side-1]++;
			}
			return;
		}

		for (int y=0; y<rows; y++) {
			for (int x=0; x<columns; x++) {
				int color = 0;

				if (y < 3)
					color = ((x^y)&1) == 1 ? 3 : 0;

				if (y >= 8-3)
					color = ((x^y)&1) == 1 ? 1 : 0;

				setCell(x, y, color);

			}
		}
		for (int side = 0; side<2; side++) {
			piecesRemaining[side] = 12;
			kingsCount[side] = 0;
		}

	}

	private boolean promotionRow(int row) {
		return promotionRowSide(row, plrTurn);
	}

	public static boolean promotionRowSide(int row, int side) {
		final int promotionRows[] = {0,7};
		return (row == promotionRows[side]);
	}

	public static int pieceColor(int piece) {
		int color = 0;
		if (piece > 0)
			color = (piece + 1) >> 1;
		return color;
	}

	public static boolean withinBoard(int x, int y) {
		return (((x | y) & 0x8) == 0);
	}

	// Construct a list of legal moves
	protected void buildMoveList() {

//		Debug.print("buildMoveList");

//		int plrTurn = plrTurn;
		int ourPawn = (plrTurn == 0) ? 1 : 3;
//		int enemyColor = (plrTurn ^ 1) + 1;
		int ourKing = ourPawn + 1;

		legalMoveCount = 0;

		// Perform two passes; the first time through, look for jumps.  If jumps
		// are possible, we can skip the second pass, since force jump is on.

		for (int pass = 0; pass < 2; pass++) {

			for (int s = 1; s < 64; s++) {
				int piece = boardCells[s];

				if (pieceColor(piece) != plrTurn + 1) continue;

				int x = s & 0x7;
				int y = s >> 3;
				CkMove m = new CkMove(0, x, y, kingFlag(piece), plrTurn);

				// Check for steps (not jumps).

				if (pass == 1) {
					int newStag = stagnateCount + 1;
					if (piece == ourPawn)
						newStag = 0;

					m.setStagnation(stagnateCount ^ newStag);

					// Process each possible direction.

					int d0 = 0;
					int d1 = 4;
					if (piece == ourPawn) {
						d0 = (plrTurn == 0) ? 0 : 2;
						d1 = d0 + 2;
					}
					for (int d = d0; d < d1; d++) {

						int nx = x + xMovesS[d];
						int ny = y + yMovesS[d];

						if (!withinBoard(nx,ny)) continue;

						if (pieceColor(readCell(nx,ny)) != 0) continue;

						m.setStagnation(stagnateCount ^ newStag);
						m.extend(d, 0);
						m.setIndex(legalMoveCount);

						legalMoves[legalMoveCount++] = new CkMove(m);
					}
					continue;
				}

				// Check for jumps, including multiple jumps.

				m.setStagnation(stagnateCount ^ 0);

				// Remove our piece from the board so we don't have to add/remove
				// it throughout the jump test.  All we need to know is whether it
				// has started as a king.

				boardCells[s] = 0;
				addJumpMoves(m, x, y, piece == ourKing);
				boardCells[s] = (byte)piece;
			}

			// If we have some legal moves, don't look for step moves.
			if (legalMoveCount > 0) break;
		}
	}

	// Make a move, update the game state if necessary, and
	// build a new legal move list.
	public void makeMove(int nMoveIndex, boolean flushBufferFlag) {

 		//db.a(nMoveIndex >= 0 && nMoveIndex < legalMoveCount(), " making move "+nMoveIndex+", not a legal move!  max is "+legalMoveCount() );

		CkMove m = (CkMove) legalMoves[nMoveIndex];

		int startCell = m.startPosition();
		int sx = startCell & 0x7;
		int sy = startCell >> 3;

		//db.a(withinBoard(sx,sy),"sx, sy not within board! "+db.p2String(sx,sy) );
		stagnateCount ^= m.getStagnation();

		int piece = readCell(sx,sy);
		setCell(sx,sy,0);

		int totalJumps = m.totalJumps();

		if (totalJumps == 0) {
			int jumpInfo = m.jumpFlags(0);

			int dx = sx + xMovesS[jumpInfo];
			int dy = sy + yMovesS[jumpInfo];
			//db.a(withinBoard(dx,dy),"dx, dy not within board! "+db.p2String(dx,dy) );
			if (
				!kingFlag(piece)
			 && promotionRow(dy)
			) {
				piece = (plrTurn == 0 ? 2 : 4);
				kingsCount[plrTurn]++;
			}
			setCell(dx,dy,piece);
		} else {
			int dx, dy;
			dx = dy = 0;
			for (int i=0; i<totalJumps; i++) {
				int jumpInfo = m.jumpFlags(i);

				dx = sx + xMovesJ[jumpInfo & 3];
				dy = sy + yMovesJ[jumpInfo & 3];

				//db.a(withinBoard(dx,dy),"dx, dy(2) not within board! "+db.p2String(dx,dy) );
				if (
					!kingFlag(piece)
				 && promotionRow(dy)
				) {
					piece = (plrTurn == 0 ? 2 : 4);
					kingsCount[plrTurn]++;
				}

				int cx = (sx + dx) >> 1;
				int cy = (sy + dy) >> 1;

				int capPiece = readCell(cx,cy);
//db.a(capPiece != 0, "makeMove: captured piece is zero");
				setCell(cx,cy,0);
				piecesRemaining[plrTurn ^ 1]--;
				if (kingFlag(capPiece))
					kingsCount[plrTurn ^ 1]--;

				sx = dx;
				sy = dy;

			}
			setCell(dx,dy,piece);
		}

		plrTurn ^= (0^1);

		if (flushBufferFlag)
			testFlushImageBuffer(m);

		// Make a copy of this move to add to the history.
//		CkMove mCopy = new CkMove(m);

		history.addMove(moveNumber, m);

		moveNumber++;
//		Debug.print("makeMove, stag chg is "+m.getStagnation()+", new stagnateCount is "+stagnateCount+", moveNumber "+moveNumber);

		// Add the board image to the history.

		addBoardImage();

		legalMoveCount = 0;

//		boardRepCount = 0;

		if (piecesRemaining[plrTurn] == 0) {
			gameState = STATE_WON + (plrTurn ^ 1);
			scores[gameState - STATE_WON]++;
			return;
		}

		gameState = checkForDraw();
		if (gameState != STATE_PLAYING) {
			scores[2]++;
			return;
		}

		// Construct legal moves.
		buildMoveList();

		// check if player is blocked.

		if (legalMoveCount == 0) {
			gameState = STATE_WON + (plrTurn ^ 1);
			scores[gameState - STATE_WON]++;
		}
	}

	// Undo a move.
	public void unMove() {
		plrTurn ^= (0^1);

		if (gameState != STATE_PLAYING) {
			if (gameState >= STATE_WON)
				scores[gameState - STATE_WON]--;
			gameState = STATE_PLAYING;
 		}

		CkMove m = (CkMove) history.getMove(moveNumber - 1);

		int start = m.startPosition();
		int endPiece = 0;

		int jumpCount = m.totalJumps();

		int endCell = start;
		if (jumpCount == 0) {
			int jumpFlags = m.jumpFlags(0);
			endCell += cMovesS[jumpFlags];
		} else {
			for (int i = 0; i<jumpCount; i++) {
				int jumpFlags = m.jumpFlags(i);
				int nextEnd = endCell + cMovesJ[jumpFlags & 3];

				boolean capKingFlag = (jumpFlags >> 2) != 0;

				boardCells[(endCell + nextEnd) >> 1] = (byte)(
					((plrTurn ^ 1) == 0 ? 1 : 3) + (capKingFlag ? 1 : 0));
				endCell = nextEnd;
				piecesRemaining[plrTurn ^ 1]++;
				if (capKingFlag)
					kingsCount[plrTurn ^ 1]++;
			}
		}
		endPiece = boardCells[endCell];
		boardCells[endCell] = 0;

		int startPiece = (m.startedAsKing() ? 2 : 1) + (plrTurn << 1);
		boardCells[start] = (byte)startPiece;

		// If we were promoted on the move, subtract from the kings count.
		if (startPiece != endPiece)
			kingsCount[plrTurn]--;
		//db.a(kingsCount[plrTurn] >= 0, "CkRules: unMove, kingsCount has gone negative");

		// Undo the stagnation count before generating the move list, since it
		// relies on the value.
		stagnateCount ^= m.getStagnation();
		buildMoveList();

		moveNumber--;
//		Debug.print("unMove, stag chg is "+m.getStagnation()+", new stagnateCount is "+stagnateCount+", moveNumber "+moveNumber);

		// If we are out of board images, and we aren't at the first move,
		// we have to reconstruct the board images by starting at the first
		// move and proceeding to this one again.

		removeBoard();
	}

	public void printBoard() {
		System.out.println("Status="+gameState+" Turn="+plrTurn+" #moves="+legalMoveCount);

		for (int r = 0; r < rows(); r++) {
			for (int c=0; c< columns(); c++) {
				final String strs[] = {" ","x","X","o","O"};
				if (((r + c) & 1) != 0)
					System.out.print(strs[readCell(c,r)]);
				else
					System.out.print(".");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}

	public void dumpMoves() {
		for (int i=0; i< legalMoveCount(); i++)
			Debug.print(" move "+i+": "+legalMoves[i]);
	}

	public static int columns() {
		return columns;
	}

	public static int rows() {
		return rows;
	}

	public static boolean kingFlag(int piece) {
		return (piece == 2 || piece == 4);
	}

	public int readCell(int c, int r) {
		return boardCells[cellIndex(c,r)];
	}

	public void setCell(int c, int r, int nContents) {
		boardCells[cellIndex(c,r)] = (byte)nContents;
	}

	// Try extending the current move in each possible direction.
	// Add moves to list if found.
	private void addJumpMoves(CkMove m, int sx, int sy, boolean kingFlag) {

		// Determine the range of directions to examine.  If not a king,
		// they can only move forward.

		int startD = 0;
		int endD = 4;

		if (!kingFlag) {
			startD = (plrTurn == 0 ? 0 : 2);
			endD = startD + 2;
		}

		// Save the current move total, to detect if we found any further jump moves.

		int oldMoveTotal = legalMoveCount;

		// Don't add any more jumps if we've exceeded the limit.

		if (m.totalJumps() < CkMove.maxJumps())
		 for (int d = startD; d < endD; d++) {

		 	// Determine where a jump in this direction would take us.

			int nx = sx + xMovesJ[d];
			int ny = sy + yMovesJ[d];

			// We can't jump off the board...

			if (!withinBoard(nx,ny)) continue;

			// ... or onto another piece.

			if (readCell(nx,ny) != 0) continue;

			// Interpolate to find the coordinates of the piece we are capturing

			int cx = (sx+nx)>>1;
			int cy = (sy+ny)>>1;

			// There has to be an enemy piece there to capture.

			int captured = readCell(cx,cy);
			if (pieceColor(captured) != 2 - plrTurn) continue;

			// We found a jump in this direction, so simulate the jump
			// and call the function again.

		/*	According to the rules of checkers, you can't become a king and
			keep jumping.

			// Determine the new king flag.  If jumping will take us to the
			// promotion rank, set it true.

			boolean newKingFlag = kingFlag || promotionRow(ny);
		*/

			// Remove the captured piece from the board.

			setCell(cx,cy,0);

			// Add this jump to the move.

			m.extend(d, captured);

			// Call the function recursively, with the destination of this jump
			// as the starting point for the next jump.

			addJumpMoves(m, nx, ny, kingFlag /*newKingFlag*/);

			// Undo the effect of the simulated jump.

			// Subtract the last jump from the move

			m.shrink();

			// Put the captured piece back on the board.

			setCell(cx,cy,captured);
		}

		// We only add the move to the list if we have reached the end of any possible
		// jumps.

		if (
			oldMoveTotal == legalMoveCount
		 && m.totalJumps() > 0
		) {
			m.setIndex(legalMoveCount);
			legalMoves[legalMoveCount++] = new CkMove(m);
		}
	}

	// Add compressed board image to history buffer; detect a draw by
	// repetition or stagnation.
	private void addBoardImage() {
		//db.a(includeImages,"CkRules addBoardImage, no images included");
		// We store the following information:
		// [3] x 32	The contents of each of the active squares on the board
		// [8] some sort of checksum for quicker comparing

		int work = 0;
		int workRem = 31;
		int checkSum = 0;

		for (int y = 0; y < rows; y++) {
			for (int x = 1 - (y & 1); x < rows; x+= 2) {
				int piece = readCell(x,y);

				checkSum ^= (piece + x) << y;

				if (workRem < 3) {
					storeInBoardImage(work);
					work = 0;
					workRem = 31;
				}

				work = (work << 3) | piece;
				workRem -= 3;
			}
		}

		// We ought to be able to store in the remaining work int.
		//db.a(workRem >= 16, "workRem not valid");

		work = (work << 16) | (checkSum & 0xffff);
		storeInBoardImage(work);
	}

	// Test if this move was a pawn move or capture.
	// If so, clear the board image buffer.  It will be reconstructed from
	// scratch if required (if moves are undone, for instance).
	private void testFlushImageBuffer(CkMove m) {
		//db.a(includeImages,"CkRules: testFlushImageBuffer called with no images included");
//		Debug.print("TestFlush move "+m);
		if ( (!m.startedAsKing()) || m.captureMove()) {
//			Debug.print(" is capture/pawn move, clearing images");
			clearImages();
		}
	}

}
