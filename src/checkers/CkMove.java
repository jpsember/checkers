package checkers;
import treegame.*;
public class CkMove extends Move {
	// Constructor.
	public CkMove(CkMove src) {
		src.copyTo(this);
	}

	public Move makeCopy() {
		return new CkMove(this);
	}

	// Abstract functions:

	// Determine a value for this move to presort it by during the search algorithm.
	public int preSortValue() {
		return totalJumps() * 2 + (startedAsKing() ? 1 : 0);
	}

	public void copyTo(CkMove dest) {
		super.copyTo(dest);
		dest.flags = flags;
		dest.vectors = vectors;
	}

	public boolean equals(CkMove m) {
		return (super.equals(m) && flags == m.flags && vectors == m.vectors);
	}

	public String toString() {
		int totalPos = totalPositions(false);

		boolean wasKing = false;
		String out = new String();

		for (int i=0; i<totalPos; i++) {
			CkMove m = getStepInMove(i, false);
			int cell = m.startPosition();
			if (i == 0) {
				wasKing = m.startedAsKing();
				out += coordString(cell);
				continue;
			}

			out += captureMove() ? 'x' : '-';
			out += coordString(cell);
			if (m.startedAsKing() && !wasKing) {
				wasKing = true;
				out += "(k)";
			}
		}
//		out += " ("+Integer.toString(flags)+","+Integer.toString(vectors) + ")";
		return out;
	}

	// Checkers-specific:

	// Constructor.
	// Sets starting location and king flag.  No jumps or direction defined yet.
	public CkMove(int index, int startCol, int startRow, boolean kingFlag, int turn) {
		super(index);
		flags = startCol | (startRow << 3) | (kingFlag ? (1 << 6) : 0)
			| (turn << 7);
		vectors = 0;
	}

	public boolean startedAsKing() {
		return ((flags >> 6) & 1) == 0 ? false : true;
	}

	// Determine the piece type and color
	public int startPiece() {
		return (side() << 1) + (startedAsKing() ? 2 : 1);
	}

	public String coordString(int cell) {
		String out = new String();
		out += (char)('A'+CkRules.cellX(cell));
		out += (char)('1'+(7-CkRules.cellY(cell)));
		return out;
	}

	// Determine the number of locations associated with a move.
	//	For step moves (no jumping), there are 2 positions, the start and end
	//	positions of the piece.
	//	For jumps, there are 2n+1 locations.  0 is the starting position;
	//	for jump #x, 2x+1 is the piece captured in that jump, and 2x+2 is
	//	the ending location for that jump.
	public int totalPositions(boolean includeCaptures) {
		if (!captureMove()) return 2;
		int n = totalJumps();
		return (includeCaptures ? (n * 2 + 1) : (n + 1));
	}

	public boolean captureMove() {
		return (totalJumps() > 0);
	}

	// Determine location and piece type at a particular step in the move.
	// Precondition:
	//	locNumber is the location within the move.
	// Postcondition:
	//	Returns a Move object containing the location, color, and piece type.
	//	The direction and capture flags are not defined!
	public CkMove getStepInMove(int locNumber, boolean includeCaptures) {

		boolean isKing = startedAsKing();

		int sx = flags & 7;
		int sy = (flags >> 3) & 7;

		int theSide = side();

		int jumpIndex = 0;

		int currLoc = 0;
		while (true) {
			if (currLoc == locNumber) break;
			int stepFlags = jumpFlags(jumpIndex);
			int dir = stepFlags & 3;
			if (!captureMove()) {
				sx += CkRules.xMovesS[dir];
				sy += CkRules.yMovesS[dir];
			} else {
				if (includeCaptures) {
					// Test if we want to return the captured piece.
					currLoc++;
					if (currLoc == locNumber) {
						sx += CkRules.xMovesS[dir];
						sy += CkRules.yMovesS[dir];
						theSide ^= 1;
						isKing = (stepFlags >> 2) != 0;
						break;
					}
				}
				sx += CkRules.xMovesJ[dir];
				sy += CkRules.yMovesJ[dir];
				jumpIndex++;
			}
			if (!isKing && CkRules.promotionRowSide(sy, side()) ) {
				isKing = true;
			}
			currLoc++;
		}
		return new CkMove(0,sx,sy,isKing,theSide);
	}

	public int startPosition() {
		return (flags & 0x3f);
	}

	// Determine the direction of jump #index
	public int jumpFlags(int index) {
		return (vectors >> (index * 3)) & 0x7;
	}

	public static int maxJumps() {
		return maxJumps;
	}

	public int totalJumps() {
		return (flags >> 8);
	}

	// Extend a move in a particular direction
	// Preconditions:
	//	number of jumps < maxJumps()
	//	direction is 0..3
	//	pieceCaptured is the piece being captured, or 0 if it's not a capture move
	public void extend(int direction, int pieceCaptured) {
		if (pieceCaptured == 0) {
			vectors = direction;
		} else {
			int jumpsDefined = flags >> 8;
			vectors |= (direction | ((CkRules.kingFlag(pieceCaptured) ? 1 : 0) << 2))
				<< (3 * jumpsDefined);
			flags += (1 << 8);	// Add one to the jumps defined.
		}
	}

	public void shrink() {
		int jumps = flags >> 8;
		flags -= (1 << 8);
		vectors &= ~(7 << ((jumps-1) * 3));
	}

	// Determine which side the move is associated with
	private int side() {
		return (flags >> 7) & 1;
	}

	// flags contains:
	//	Bits		Contents
	//	----		--------
	//	0..2		X coordinate
	//	3..5		Y coordinate
	//	6			set if piece was king initially
	//	7			which side this move is (0:red 1:black)
	//	8..11		number of captures (0:simple move, or 1...10)
	private int flags;

	// vectors contains up to 10 sets of 3 bits:
	//	Bits		Contents
	//	----		--------
	//	0..1		Direction (NW, NE, SE, SW)
	//	2			Captured king flag (unused if no jumps in move)
	private int vectors;

	private final static int maxJumps = 10;


}
