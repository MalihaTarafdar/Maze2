import java.awt.Color;

public class Explorer {

	private Location loc;
	private final int size;
	private int dir; //0 EAST, 1 NORTH, 2 WEST, 3 SOUTH
	private int health;
	private Color color;

	public enum CardinalDirection {
		EAST, NORTH, WEST, SOUTH;
	}

	public enum RelativeDirection {
		LEFT, RIGHT;
	}

	public Explorer(int row, int col, int size, int dir) {
		loc = new Location(row, col);
		this.size = size;
		health = 100;
		color = Color.BLUE;
	}

	public void move(Structure[][] maze) {
		int moveRow = loc.getRow(), moveCol = loc.getCol();
		switch (dir) {
			case 0: moveCol++;
			break;
			case 1: moveRow--;
			break;
			case 2: moveCol--;
			break;
			case 3: moveRow++;
			break;
		}
		if (maze[moveRow][moveCol] == null) { //check for collision
			setLoc(moveRow, moveCol);
		}
	}

	public void turn(RelativeDirection turnDir) {
		if (turnDir == RelativeDirection.LEFT) {
			dir++;
			if (dir > 3)
				dir = 0;
		} else {
			dir--;
			if (dir < 0)
				dir = 3;
		}
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

	public CardinalDirection getDir() {
		switch (dir) {
			case 0: return CardinalDirection.EAST;
			case 1: return CardinalDirection.NORTH;
			case 2: return CardinalDirection.WEST;
			case 3: return CardinalDirection.SOUTH;
		}
		throw new IllegalArgumentException("Impossible direction");
	}

	public void heal(int healAmount) {
		health += healAmount;
		if (health > 100) {
			health = 100;
		}
	}
	public void takeDamage(int damageAmount) {
		health -= damageAmount;
		if (health < 0) {
			health = 0;
		}
	}
	public int getHealth() {
		return health;
	}

	public void setColor(Color color) {
		this.color = color;
	}
	public Color getColor() {
		return color;
	}
}