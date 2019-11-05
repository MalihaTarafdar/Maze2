import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Main extends JPanel implements KeyListener, Runnable {
	private static final long serialVersionUID = 1L;

	private JFrame frame;
	private Thread thread;
	
	private Structure[][] maze;
	private GameState gameState = GameState.MAIN_MENU;
	private Explorer explorer;
	private Location end;
	private Menu mainMenu = new Menu("2D Maze", "3D Maze", "Stats", "Quit");
	private Menu levelSelectMenu = new Menu("Level 1", "Level 2", "Level 3");
	private Menu pauseMenu = new Menu("Resume", "Quit");
	private StatTracker tracker = new StatTracker();
	
	private boolean onMaze2D;
	private boolean paused;
	private boolean win;
	private boolean newHighScore;
	private int mazeNum;
	private double duration; //in seconds
	private long startTime; //in nanoseconds
	private long endTime; //in nanoseconds

	private Font title = new Font("Positive System", Font.PLAIN, 100);
	private Font main = new Font("Game Over", Font.PLAIN, 70);
	private Font other = new Font("Game Over", Font.PLAIN, 100);
	private FontMetrics tm;
	private FontMetrics mm;
	private FontMetrics om;

	private final int RENDER_DISTANCE = 4; //for 3D maze

	enum GameState {
		MAIN_MENU, LEVEL_SELECT, STATS, MAZE2D, MAZE3D, GAME_OVER;
	}

	public Main() {		
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("./fonts/Positive System.otf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("./fonts/game_over.ttf")));
		} catch (IOException | FontFormatException e) {}

		//TODO: Music, graphics

		frame = new JFrame("Maze");
		frame.add(this);
		frame.addKeyListener(this);
		frame.setSize(1000, 800);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);

		thread = new Thread(this);
		thread.start();
	}

	public void run() {
		while (true) {
			switch (gameState) {
				case MAZE2D:
				case MAZE3D:
					if (explorer.getHealth() == 0) {
						win = false;
						tracker.getMazeStats(mazeNum).incrementNumLosses();
						gameState = GameState.GAME_OVER;
					}
					if (explorer.getLoc().getRow() == end.getRow() && explorer.getLoc().getCol() == end.getCol()) {
						win = true;
						tracker.getMazeStats(mazeNum).incrementNumWins();
						gameState = GameState.GAME_OVER;
					}
					break;
				case GAME_OVER:
					endTime = System.nanoTime();
					duration += (endTime - startTime) / 1000000000.0;
					if (duration < tracker.getMazeStats(mazeNum).getBestTime() || tracker.getMazeStats(mazeNum).getBestTime() == 0) {
						tracker.getMazeStats(mazeNum).setBestTime(duration);
						newHighScore = true;
					} else {
						newHighScore = false;
					}
					delay(1500);
					gameState = GameState.MAIN_MENU;
					reset();
					repaint();
					break;
				case MAIN_MENU:
					break;
				case LEVEL_SELECT:
					break;
				case STATS:
					break;
			}
			repaint();
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, frame.getWidth(), frame.getHeight());

		tm = g2.getFontMetrics(title);
		mm = g2.getFontMetrics(main);
		om = g2.getFontMetrics(other);

		//TODO: Compass and health
		//TODO: Paint escape to go back
		switch (gameState) {
			case MAIN_MENU: paintMenu(g2);
				break;
			case LEVEL_SELECT: paintLevelSelect(g2);
				break;
			case MAZE2D: paintMaze2D(g2);
				if (paused) {
					paintPauseMenu(g2);
				}
				break;
			case MAZE3D: paintMaze3D(g2);
				if (paused) {
					paintPauseMenu(g2);
				}
				break;
			case STATS: paintStats(g2);
				break;
			case GAME_OVER: paintGameOver(g2);
				break;
		}
	}

	public void paintMaze3D(Graphics2D g2) {
		ArrayList<Wall3D> walls = getWalls();
		for (int i = 0; i < walls.size(); i++) {
			g2.setColor(walls.get(i).getColor());
			g2.fill(walls.get(i).getPolygon());
			g2.setColor(Color.BLACK);
			g2.draw(walls.get(i).getPolygon());
		}
	}

	public ArrayList<Wall3D> getWalls() {
		int currentRow = explorer.getLoc().getRow(), currentCol = explorer.getLoc().getCol();
		ArrayList<Wall3D> walls = new ArrayList<Wall3D>();
		Wall3D frontWall = null;

		for (int d = 0; d < RENDER_DISTANCE; d++) {
			final int WIDTH = 80;
			final int HEIGHT = 80;
			int[] xpoints = {WIDTH * d, HEIGHT + WIDTH * d, HEIGHT + WIDTH * d, WIDTH * d};
			int[] ypoints = {WIDTH * d, HEIGHT + WIDTH * d, frame.getHeight() - HEIGHT - WIDTH * d, frame.getHeight() - WIDTH * d};
			Color color = new Color(128 - 25 * d, 128 - 25 * d, 128 - 25 * d);
			Color darkerColor = new Color(color.getRed() - 25, color.getGreen() - 25, color.getBlue() - 25);
			boolean hasFrontWall = false;

			switch(explorer.getDir()) {
				case EAST:
					if (isOutOfBounds(currentRow - 1, currentCol + d) || maze[currentRow - 1][currentCol + d] instanceof Wall) //left walls
						walls.add(new Wall3D(new Polygon(xpoints, ypoints, 4), color));
					if (isOutOfBounds(currentRow + 1, currentCol + d) || maze[currentRow + 1][currentCol + d] instanceof Wall) //right walls
						walls.add(new Wall3D(reflectPolygon(new Polygon(xpoints, ypoints, 4), false), color));
					if (currentCol + d >= maze[0].length || maze[currentRow][currentCol + d] instanceof Wall)
						hasFrontWall = true;
					break;
				case NORTH:
					if (isOutOfBounds(currentRow - d, currentCol - 1) || maze[currentRow - d][currentCol - 1] instanceof Wall) //left walls
						walls.add(new Wall3D(new Polygon(xpoints, ypoints, 4), color));
					if (isOutOfBounds(currentRow - d, currentCol + 1) || maze[currentRow - d][currentCol + 1] instanceof Wall) //right walls
						walls.add(new Wall3D(reflectPolygon(new Polygon(xpoints, ypoints, 4), false), color));
					if (currentRow - d < 0 || maze[currentRow - d][currentCol] instanceof Wall)
						hasFrontWall = true;
					break;
				case WEST:
					if (isOutOfBounds(currentRow + 1, currentCol - d) || maze[currentRow + 1][currentCol - d] instanceof Wall) //left walls
						walls.add(new Wall3D(new Polygon(xpoints, ypoints, 4), color));
					if (isOutOfBounds(currentRow - 1, currentCol - d) || maze[currentRow - 1][currentCol - d] instanceof Wall) //right walls
						walls.add(new Wall3D(reflectPolygon(new Polygon(xpoints, ypoints, 4), false), color));
					if (currentCol - d < 0 || maze[currentRow][currentCol - d] instanceof Wall)
						hasFrontWall = true;
					break;
				case SOUTH:
					if (isOutOfBounds(currentRow + d, currentCol + 1) || maze[currentRow + d][currentCol + 1] instanceof Wall) //left walls
						walls.add(new Wall3D(new Polygon(xpoints, ypoints, 4), color));
					if (isOutOfBounds(currentRow + d, currentCol - 1) || maze[currentRow + d][currentCol - 1] instanceof Wall) //right walls
						walls.add(new Wall3D(reflectPolygon(new Polygon(xpoints, ypoints, 4), false), color));
					if (currentRow + d >= maze.length || maze[currentRow + d][currentCol] instanceof Wall)
						hasFrontWall = true;
					break;
			}
			
			//rectangles to give effect of hallways
			Polygon hall = convertRect(new Rectangle(xpoints[0] - WIDTH, ypoints[0], WIDTH, ypoints[ypoints.length - 1] - ypoints[0]));
			walls.add(0, new Wall3D(hall, new Color(color.getRed() - 25, color.getGreen() - 25, color.getBlue() - 25))); //left
			walls.add(0, new Wall3D(reflectPolygon(hall, false), darkerColor)); //right
			
			//front wall
			if (hasFrontWall && frontWall == null) {
				frontWall = new Wall3D(convertRect(new Rectangle(xpoints[0], ypoints[0], frame.getWidth() - 2 * xpoints[0], frame.getHeight() - 2 * ypoints[0])), darkerColor);
				walls.add(0, new Wall3D(convertRect(new Rectangle(xpoints[0] - (frame.getWidth() - 2 * xpoints[0]), ypoints[0], frame.getWidth() - 2 * xpoints[0], frame.getHeight() - 2 * ypoints[0])), darkerColor)); //left
				walls.add(0, new Wall3D(convertRect(new Rectangle(xpoints[0] + (frame.getWidth() - 2 * xpoints[0]), ypoints[0], frame.getWidth() - 2 * xpoints[0], frame.getHeight() - 2 * ypoints[0])), darkerColor)); //right
			}

			//ceiling
			Polygon ceiling = new Polygon();
			ceiling.addPoint(0, ypoints[3]);
			ceiling.addPoint(0, ypoints[2]);
			ceiling.addPoint(frame.getWidth(), ypoints[2]);
			ceiling.addPoint(frame.getWidth(), ypoints[3]);
			walls.add(0, new Wall3D(ceiling, darkerColor));

			//floor
			walls.add(0, new Wall3D(reflectPolygon(ceiling, true), darkerColor));
		}

		//front wall should always be on top
		if (frontWall != null)
			walls.add(frontWall);

		return walls;
	}

	public boolean isOutOfBounds(int row, int col) {
		return row < 0 || col < 0 || row >= maze.length || col >= maze[0].length;
	}

	public Polygon convertRect(Rectangle r) {
		int[] xpoints = {(int)r.getX(), (int)(r.getX() + r.getWidth()), (int)(r.getX() + r.getWidth()), (int)(r.getX())};
		int[] ypoints = {(int)r.getY(), (int)(r.getY()), (int)(r.getY() + r.getHeight()), (int)(r.getY() + r.getHeight())};
		return new Polygon(xpoints, ypoints, 4);
	}

	public Polygon reflectPolygon(Polygon p, boolean horizontal) {
		Polygon reflectedP = new Polygon();
		for (int i = 0; i < p.npoints; i++) {
			if (horizontal) {
				reflectedP.addPoint(p.xpoints[i], frame.getHeight() - p.ypoints[i]);
			} else {
				reflectedP.addPoint(frame.getWidth() - p.xpoints[i], p.ypoints[i]);
			}
		}
		return reflectedP;
	}

	public static class Wall3D {
		private final Polygon polygon;
		private final Color color;
		public Wall3D(Polygon polygon, Color color) {
			this.polygon = polygon;
			this.color = color;
		}
		public Polygon getPolygon() {
			return polygon;
		}
		public Color getColor() {
			return color;
		}
	}

	public void paintMaze2D(Graphics2D g2) {
		for (Structure[] row : maze) {
			for (Structure s : row) {
				if (s != null) {
					g2.setColor(s.getColor());
					g2.fillRect(s.getLoc().getCol() * s.getSize(), s.getLoc().getRow() * s.getSize(), s.getSize(), s.getSize());
				}
			}
		}
		g2.setColor(explorer.getColor());
		g2.fillOval(explorer.getLoc().getCol() * explorer.getSize(), explorer.getLoc().getRow() * explorer.getSize(), explorer.getSize(), explorer.getSize());
		g2.drawString(explorer.getDir() + "", 800, 50);
	}

	public void paintPauseMenu(Graphics2D g2) {
		g2.setColor(Color.WHITE);
		g2.fillRect(350, 100, 200, 200);
		g2.setColor(Color.BLACK);
		g2.fillRect(360, 110, 180, 180);

		g2.setFont(main);
		g2.setColor(Color.WHITE);
		int optionY = 180;
		for (Menu.Option option : pauseMenu.getOptions()) {
			g2.drawString(option.getName(), 420, optionY);
			optionY += 50;
		}
		for (int i = 0; i < pauseMenu.getOptions().length; i++) {
			if (pauseMenu.getOptions()[i].isSelected()) {
				optionY = 165 + 50 * i;
				g2.setColor(Color.BLUE);
				g2.fillRect(390, optionY, 10, 10);
			}
		}
	}

	public void paintStats(Graphics2D g2) {
		int titleX = frame.getWidth() / 2 - om.stringWidth("Statistics") / 2;
		int titleY = frame.getHeight() / 10;
		g2.setFont(other);
		g2.setPaint(new GradientPaint(titleX, titleY, Color.BLUE, titleX + om.stringWidth("Statistics"), titleY + om.getHeight(), Color.CYAN));
		g2.drawString("Statistics", titleX, titleY);

		g2.setFont(main);
		for (int i = 1; i <= 3; i++) {
			int x = frame.getWidth() / 6;
			int y = frame.getHeight() / 6 + 220 * (i - 1);
			g2.setColor(Color.BLUE);
			g2.drawString("Maze " + i, x, y);
			
			g2.setColor(Color.WHITE);
			y += mm.getHeight();
			g2.drawString("Best Time: " + Math.round(tracker.getMazeStats(i).getBestTime() * 1000) / 1000.0 + " seconds", x, y);
			y += mm.getHeight();
			g2.drawString("Total Attempts: " + tracker.getMazeStats(i).getNumAttempts() + " attempts", x, y);
			y += mm.getHeight();
			g2.drawString("Total Wins: " + tracker.getMazeStats(i).getNumWins() + " wins", x, y);
			y += mm.getHeight();
			g2.drawString("Total Losses: " + tracker.getMazeStats(i).getNumLosses() + " losses", x, y);
		}
	}

	public void paintGameOver(Graphics2D g2) {
		g2.setColor(Color.WHITE);
		g2.setFont(other);
		if (win) {
			g2.drawString("YOU WIN", frame.getWidth() / 2 - om.stringWidth("YOU WIN") / 2, frame.getHeight() / 2);
		} else {
			g2.drawString("YOU LOSE", frame.getWidth() / 2 - om.stringWidth("YOU LOSE") / 2, frame.getHeight() / 2);
		}
		if (newHighScore) {
			g2.setFont(main);
			g2.drawString("New high score!", frame.getWidth() / 2 - mm.stringWidth("New high score!") / 2, frame.getHeight() / 2 + om.getHeight());
		}
	}

	public void paintLevelSelect(Graphics2D g2) {
		int titleX = frame.getWidth() / 2 - om.stringWidth("Select Level") / 2;
		int titleY = frame.getHeight() / 4;
		g2.setFont(other);
		g2.setPaint(new GradientPaint(titleX, titleY, Color.BLUE, titleX + om.stringWidth("Select Level"), titleY + om.getHeight(), Color.CYAN));
		g2.drawString("Select Level", titleX, titleY);

		g2.setColor(Color.WHITE);
		g2.setFont(main);
		int optionX = frame.getWidth() / 2 - 250;
		for (Menu.Option option : levelSelectMenu.getOptions()) {
			g2.drawString(option.getName(), optionX, frame.getHeight() * 2 / 3);
			optionX += 200;
		}
		optionX = frame.getWidth() / 2 - 250 - om.stringWidth("Level 3") / 2;
		for (int i = 0; i < levelSelectMenu.getOptions().length; i++) {
			if (levelSelectMenu.getOptions()[i].isSelected()) {
				optionX += 200 * i;
				g2.setColor(Color.BLUE);
				g2.drawRect(optionX, frame.getHeight() / 2 - 50, 200, 200);
			}
		}
	}

	public void paintMenu(Graphics2D g2) {
		int titleX = frame.getWidth() / 2 - tm.stringWidth("MAZE") / 2;
		int titleY = frame.getHeight() / 4;
		g2.setFont(title);
		g2.setPaint(new GradientPaint(titleX, titleY, Color.BLUE, titleX + tm.stringWidth("MAZE"), titleY + tm.getHeight(), Color.CYAN));
		g2.drawString("MAZE", titleX, titleY);

		g2.setColor(Color.WHITE);
		g2.setFont(other);
		int optionY = frame.getHeight() / 2;

		for (Menu.Option option : mainMenu.getOptions()) {
			g2.drawString(option.getName(), frame.getWidth() / 2 - mm.stringWidth(option.getName()) / 2, optionY);
			optionY += 55;
		}

		for (int i = 0; i < mainMenu.getOptions().length; i++) {
			if (mainMenu.getOptions()[i].isSelected()) {
				optionY = frame.getHeight() / 2 + 57 * i - 20;
				g2.setColor(Color.BLUE);
				g2.fillRect(frame.getWidth() / 2 - 140, optionY, 10, 10);
			}
		}
	}

	public void keyPressed(KeyEvent e) {
		if (paused) {
			if (e.getKeyCode() == KeyEvent.VK_UP) {
				pauseMenu.moveBackward();
			} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				pauseMenu.moveForward();
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				if (pauseMenu.getOptions()[1].isSelected()) {
					gameState = GameState.MAIN_MENU;
				}
				paused = false;
				startTime = System.nanoTime();
			}
		} else {
			switch (gameState) {
				case MAZE3D:
				case MAZE2D:
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						explorer.move(maze);
					} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						explorer.turn(Explorer.RelativeDirection.LEFT);
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						explorer.turn(Explorer.RelativeDirection.RIGHT);
					} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						paused = true;
						endTime = System.nanoTime();
						duration += (endTime - startTime) / 1000000000.0;
					}
					break;
				case MAIN_MENU:
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						mainMenu.moveBackward();
					} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
						mainMenu.moveForward();
					} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						if (mainMenu.getOptions()[0].isSelected()) {
							onMaze2D = true;
							gameState = GameState.LEVEL_SELECT;
						} else if (mainMenu.getOptions()[1].isSelected()) {
							onMaze2D = false;
							gameState = GameState.LEVEL_SELECT;
						} else if (mainMenu.getOptions()[2].isSelected()) {
							gameState = GameState.STATS;
						} else {
							System.exit(0);
						}
					}
					break;
				case LEVEL_SELECT:
					Maze map;
					if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						levelSelectMenu.moveBackward();
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						levelSelectMenu.moveForward();
					} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						if (levelSelectMenu.getOptions()[0].isSelected()) {
							map = new Maze(new File("./mazes/maze1.txt"));
							mazeNum = 1;
						} else if (levelSelectMenu.getOptions()[1].isSelected()) {
							map = new Maze(new File("./mazes/maze2.txt"));
							mazeNum = 2;
						} else {
							map = new Maze(new File("./mazes/maze3.txt"));
							mazeNum = 3;
						}
						maze = map.getMaze();
						explorer = map.getExplorer();
						end = map.getEnd();
						tracker.getMazeStats(mazeNum).incrementNumAttempts();
						duration = 0;
						startTime = System.nanoTime();

						if (onMaze2D) {
							gameState = GameState.MAZE2D;
						} else {
							gameState = GameState.MAZE3D;
						}
					} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						gameState = GameState.MAIN_MENU;
					}
					break;
				case STATS:
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						gameState = GameState.MAIN_MENU;
					}
					break;
				case GAME_OVER:
					break;
			}
		}

		if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			mainMenu.resetOptions();
			levelSelectMenu.resetOptions();
			pauseMenu.resetOptions();
		}
		// repaint();
	}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public void reset() {
		duration = 0;
		win = false;
		for (int i = 0; i < maze.length; i++) {
			for (int j = 0; j < maze[0].length; j++) {
				maze[i][j] = null;
			}
		}
	}

	public void delay(int ms) {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException e){}
	}

	public static void main(String[] args) {
		Main game = new Main();
	}
}