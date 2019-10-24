public class Entity {

	private Location loc;
	private final int size;
	private int dir; //0 EAST, 1 NORTH, 2 WEST, 3 SOUTH
	private int health;

	public enum Direction {
		LEFT, RIGHT;
	}

	public Entity(int row, int col, int size, int dir) {
		loc = new Location(row, col);
		this.size = size;
		health = 100;
	}

	public void move(int dir, Structure[] maze) {
		
	}

	public void turn(Direction turnDir) {
		
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

	public int getDir() {
		return dir;
	}

	public void heal(int healthIncrement) {
		health += healthIncrement;
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
}