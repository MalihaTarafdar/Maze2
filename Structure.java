import java.awt.Color;

public class Structure {
	
	private Location loc;
	private final int size;
	private Color color;
	
	public Structure(int row, int col, int size, Color color) {
		loc = new Location(row, col);
		this.size = size;
		this.color = color;
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
	
	public void setColor(Color color) {
		this.color = color;
	}
	public Color getColor() {
		return color;
	}
}