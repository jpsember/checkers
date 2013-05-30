package checkers;
import treegame.*;
public class CkEval extends Eval {

	private final int vA = 8;
	private final int vB = 6;
	private final int vC = 5;
	private final int vD = 0;
	private final int vE = -3;

	private short resultsArray[];

	// Constructor
	public CkEval() {
	}

	public int getSavedValue(int x, int y, int v) {
		return resultsArray[((x >> 1) | (y << 2)) * 4 + v];
	}

	// Evaluate the board position relative to the player whose turn it is.
	// Returns a value for the board position; positive is good for the current player.
	public int process(Rules absRules, int evalOptions, boolean saveResults) {

		CkRules rules = (CkRules) absRules;

		if (saveResults) {
			if (resultsArray == null)
				resultsArray = new short[32*4];
			for (int i = 0; i<(32*4); i++)
				resultsArray[i] = 0;
		}

		int nTotalScore = 0;

		int plr;
		for (plr = 0; plr < 2; plr++) {
			int nScore = 0;
			int side = plr ^ rules.turn();
			int ourPawn = (side == 0) ? 1 : 3;
			int ourKing = ourPawn + 1;

			int ourPieceCount = rules.totalPieces(side);
			int oppPieceCount = rules.totalPieces(side ^ 1);
			int oppKingCount = rules.totalKings(side ^ 1);
//if (saveResults) db.pr("Eval side "+side+", ourPieceCount="+ourPieceCount+", opp cnt="+oppPieceCount+", oppKings="+oppKingCount);

			// Award bonus points if side is ahead in material; award more points
			// if opponent has fewer absolute pieces.
			if (ourPieceCount > oppPieceCount) {
				nScore += ((ourPieceCount - oppPieceCount) * 50) / (oppPieceCount+1);
			}

			for (int y = 0; y<CkRules.rows(); y++) {
				int advY = (side == 0) ? y : CkRules.rows() - 1 - y;
//		if (saveResults) db.pr(" y="+y+", setting advY to "+advY);
				for (int x = 0; x < CkRules.columns(); x++) {
					int piece = rules.readCell(x,y);

					if (piece != ourKing && piece != ourPawn) continue;

					int v0, v1, v2, v3;
					v0 = 0;
					v1 = 0;
					v2 = 0;
					v3 = 0;
					if ((evalOptions & BrnPnl.PE_MATERIAL) != 0) {
						// -------------------
						// Material component
						// -------------------

						v0 = (piece == ourKing) ? 50 : 20;
					}

					if ((evalOptions & BrnPnl.PE_POSITION) != 0) {
						// --------------------------
						// Pawn advancement component
						// --------------------------
						if (piece == ourPawn) {
							// Try to keep pawns on the back row to protect against
							// enemy king moves.
							final int rowPoints[] = {
								12,9,6,4,2,1,0,0
							};
							v1 = rowPoints[advY];

							// Award bonus for pawn staying at back row if
							// opponent doesn't have many kings.

//if (saveResults) db.pr(" our pawn is at "+x+","+y+", advY="+advY);
							if (
								advY == 7
							 &&	(oppKingCount << 2) < oppPieceCount
							) {
								v1 = 10;
							}
						}

						// --------------------------
						// Position component
						// --------------------------

						if (piece == ourKing) {
							final int locationValues[] = {
								0 ,vC,0 ,vD,0 ,vE,0 ,vE,
								vC,0 ,vC,0 ,vD,0 ,vE,0 ,
								0 ,vC,0 ,vB,0 ,vD,0 ,vE,
								vD,0 ,vB,0 ,vA,0 ,vD,0 ,
								0 ,vD,0 ,vA,0 ,vB,0 ,vD,
								vE,0 ,vD,0 ,vB,0 ,vC,0 ,
								0 ,vE,0 ,vD,0 ,vC,0 ,vC,
								vE,0 ,vE,0 ,vD,0 ,vC,0
							};
							v1 += locationValues[(y << 3) | x];
						}
					}

					if ((evalOptions & BrnPnl.PE_MOBILITY) != 0) {
						// --------------------------
						// Mobility component
						// --------------------------
						int d0,d1;
						if (piece == ourPawn) {
							d0 = (side == 0) ? 0 : 2;
							d1 = d0 + 2;
						} else {
							d0 = 0;
							d1 = 4;
						}
						for (; d0 < d1; d0++) {
							int dx = x + CkRules.xMovesS[d0];
							int dy = y + CkRules.yMovesS[d0];
							if (!CkRules.withinBoard(dx,dy)) continue;
							int p1 = rules.readCell(dx,dy);
							if (p1 == 0) {
								v2 += 1;
								continue;
							}
							if (CkRules.pieceColor(p1) == side+1) continue;

							dx += CkRules.xMovesS[d0];
							dy += CkRules.yMovesS[d0];
							if (!CkRules.withinBoard(dx,dy)) continue;

							p1 = rules.readCell(dx,dy);
							if (p1 == 0)
								v2 += 1;
						}
					}

					if ((evalOptions & BrnPnl.PE_ATTACK) != 0) {
						// --------------------------
						// Attack component
						// --------------------------

						// If we are winning, award points for getting close to
						// an opposing piece.

						if (piece == ourKing
						 && ourPieceCount > oppPieceCount
						) {
							final int offsets[] = {
								-1,1,10,
								0,2,4,
								1,1,10,
								2,0,4,
								1,-1,10,
								0,-2,4,
								-1,-1,10,
								-2,0,4
							};
							for (int i=0; i<3*8; i+=3) {
								int dx = x + offsets[i];
								int dy = y + offsets[i+1];
								// Don't award any points if it's at the edge of the board.
								if (dx <= 0 || dx >= 7 || dy <= 0 || dy >= 7) continue;
//								if (!rules.withinBoard(dx,dy)) continue;
								int attPiece = rules.readCell(dx,dy);

								if (CkRules.pieceColor(attPiece) != 2-side)
									continue;

								int value = offsets[i+2];
								if (CkRules.kingFlag(attPiece))
									value = value << 1;

								v3 += value;
							}
						}
					}


					nScore += v0+v1+v2+v3;
					if (saveResults) {
						int arrInd = ((x >> 1) | (y << 2)) * 4;
						resultsArray[arrInd+0] = (short)v0;
						resultsArray[arrInd+1] = (short)v1;
						resultsArray[arrInd+2] = (short)v2;
						resultsArray[arrInd+3] = (short)v3;
					}

				}
			}

			if (plr == 0)
				nTotalScore = nScore;
			else
				nTotalScore -= nScore;

		}
		return nTotalScore;
	}
}
