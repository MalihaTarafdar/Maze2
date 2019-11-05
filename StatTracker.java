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
		private int numAttempts;
		private int numWins;
		private int numLosses;
		
		public MazeStats() {
			bestTime = 0;
			numAttempts = 0;
			numWins = 0;
			numLosses = 0;
		}

		public void setBestTime(double bestTime) {
			this.bestTime = bestTime;
		}
		public double getBestTime() {
			return bestTime;
		}

		public void incrementNumAttempts() {
			numAttempts++;
		}
		public int getNumAttempts() {
			return numAttempts;
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