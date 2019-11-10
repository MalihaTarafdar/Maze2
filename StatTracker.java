public class StatTracker {

	private MazeStats mazeStats1;
	private MazeStats mazeStats2;
	private MazeStats mazeStats3;

	public StatTracker() {
		mazeStats1 = new MazeStats();
		mazeStats2 = new MazeStats();
		mazeStats3 = new MazeStats();
	}

	public static class MazeStats {
		private double bestTime; //in seconds
		private int leastMoves;
		private int numWins;
		private int numLosses;
		
		public MazeStats() {
			bestTime = 0;
			leastMoves = 0;
			numWins = 0;
			numLosses = 0;
		}

		public void setBestTime(double bestTime) {
			this.bestTime = bestTime;
		}
		public double getBestTime() {
			return bestTime;
		}

		public void setLeastMoves(int leastMoves) {
			this.leastMoves = leastMoves;
		}
		public int getLeastMoves() {
			return leastMoves;
		}

		public void incrementNumWins() {
			numWins++;
		}
		public int getNumWins() {
			return numWins;
		}

		public void incrementNumLosses() {
			numLosses++;
		}
		public int getNumLosses() {
			return numLosses;
		}
	}

	public MazeStats getMazeStats(int maze) {
		switch (maze) {
			case 1: return mazeStats1;
			case 2: return mazeStats2;
			case 3: return mazeStats3;
		}
		throw new IllegalArgumentException("Not a valid maze");
	}
}