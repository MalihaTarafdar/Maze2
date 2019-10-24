public class Structure {
	
	private Location loc;
	private final int size;
	
	public Structure(int row, int col, int size) {
		loc = new Location(row, col);
		this.size = size;
	}

	public void setLoc(int row, int col) {
		loc.setRow(row);
		loc.setCol(col);
	}
	public Location getLoc() {
		return loc;
	}

	public int getSize() {
		return size;
	}
}