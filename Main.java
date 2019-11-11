import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.util.ArrayList;

public class Main extends JPanel implements KeyListener, Runnable {
	private static final long serialVersionUID = 1L;

	private JFrame frame;
	private Thread thread;
	private Structure[][] maze;
	private GameState gameState;
	private Explorer explorer;
	private Location end;
	private Menu mainMenu = new Menu("2D Maze", "3D Maze", "Stats", "Quit");
	private Menu levelSelectMenu = new Menu("Level 1", "Level 2", "Level 3");
	private Menu pauseMenu = new Menu("Resume", "Quit");
	private StatTracker tracker2D = new StatTracker();
	private StatTracker tracker3D = new StatTracker();
	
	private final Font title = new Font("Positive System", Font.PLAIN, 100);
	private final Font main = new Font("Game Over", Font.PLAIN, 70);
	private final Font other = new Font("Game Over", Font.PLAIN, 100);
	private FontMetrics tm;
	private FontMetrics mm;
	private FontMetrics om;

	private Clip clip;

	private boolean onMaze2D;
	private boolean paused;
	private boolean win;
	private int mazeNum;
	private double duration; //in seconds
	private long startTime; //in nanoseconds
	private long endTime; //in nanoseconds
	
	private final int RENDER_DISTANCE = 4;
	private int colorIncrement = 25;
	private int startColor = 160;
	private boolean newBestTime = false;
	private int moveCount = 0;

	enum GameState {
		MAIN_MENU, LEVEL_SELECT, STATS, MAZE2D, MAZE3D, GAME_OVER;
	}

	public Main() {
		//register fonts
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("./fonts/Positive System.otf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("./fonts/game_over.ttf")));
		} catch (IOException | FontFormatException e) {}

		gameState = GameState.MAIN_MENU;

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

		//start music
		try {
			clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(new BufferedInputStream(getClass().getResourceAsStream("./sounds/menu_music.wav"))));
			clip.loop(Clip.LOOP_CONTINUOUSLY);
			clip.start();
		} catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
			System.err.println("An audio exception has occurred.");
		}
	}

	public void playMusic(String path) {
		try {
			clip.stop();
			clip.close();
			clip.open(AudioSystem.getAudioInputStream(new BufferedInputStream(getClass().getResourceAsStream(path))));
			clip.loop(Clip.LOOP_CONTINUOUSLY);
			clip.start();
		} catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
			System.err.println("An audio exception has occurred.");
		}
	}

	public void run() {
		while (true) {
			switch (gameState) {
				case MAZE2D:
					if (isEnd(explorer.getLoc().getRow(), explorer.getLoc().getCol())) {
						win = true;
						tracker2D.getMazeStats(mazeNum).incrementNumWins();
						endTime = System.nanoTime();
						duration += (endTime - startTime) / 1000000000.0;
						if (duration < tracker2D.getMazeStats(mazeNum).getBestTime() || tracker2D.getMazeStats(mazeNum).getBestTime() == 0) {
							tracker2D.getMazeStats(mazeNum).setBestTime(duration);
							newBestTime = true;
						}
						if (moveCount < tracker2D.getMazeStats(mazeNum).getLeastMoves() || tracker2D.getMazeStats(mazeNum).getLeastMoves() == 0) {
							tracker2D.getMazeStats(mazeNum).setLeastMoves(moveCount);
						}
						gameState = GameState.GAME_OVER;
					}
					break;
				case MAZE3D:
					if (explorer.getHealth() == 0) {
						win = false;
						tracker3D.getMazeStats(mazeNum).incrementNumLosses();
						gameState = GameState.GAME_OVER;
					}
					if (isEnd(explorer.getLoc().getRow(), explorer.getLoc().getCol())) {
						win = true;
						tracker3D.getMazeStats(mazeNum).incrementNumWins();
						endTime = System.nanoTime();
						duration += (endTime - startTime) / 1000000000.0;
						if (duration < tracker3D.getMazeStats(mazeNum).getBestTime() || tracker3D.getMazeStats(mazeNum).getBestTime() == 0) {
							tracker3D.getMazeStats(mazeNum).setBestTime(duration);
							newBestTime = true;
						}
						if (moveCount < tracker3D.getMazeStats(mazeNum).getLeastMoves() || tracker3D.getMazeStats(mazeNum).getLeastMoves() == 0) {
							tracker3D.getMazeStats(mazeNum).setLeastMoves(moveCount);
						}
						gameState = GameState.GAME_OVER;
					}
					break;
				case GAME_OVER:
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

	public boolean isEnd(int row, int col) {
		return row == end.getRow() && col == end.getCol();
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, frame.getWidth(), frame.getHeight());

		tm = g2.getFontMetrics(title);
		mm = g2.getFontMetrics(main);
		om = g2.getFontMetrics(other);

		switch (gameState) {
			case MAIN_MENU: paintMenu(g2);
				break;
			case LEVEL_SELECT: paintLevelSelect(g2);
				paintEsc(g2);
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
				paintEsc(g2);
				break;
			case GAME_OVER: paintGameOver(g2);
				break;
		}
	}

	public void paintCompass(Graphics2D g2, int height) {
		BufferedImage east, north, west, south;
		try {
			east = ImageIO.read(new File("./img/compass_east.png"));
			north = ImageIO.read(new File("./img/compass_north.png"));
			west = ImageIO.read(new File("./img/compass_west.png"));
			south = ImageIO.read(new File("./img/compass_south.png"));

			switch (explorer.getDir()) {
				case EAST: g2.drawImage(east, 780, height, this);
					break;
				case NORTH: g2.drawImage(north, 780, height, this);
					break;
				case WEST: g2.drawImage(west, 780, height, this);
					break;
				case SOUTH: g2.drawImage(south, 780, height, this);
					break;
			}
		} catch (IOException e) {
			System.err.println("Image not found");
		}
	}

	public void paintEsc(Graphics2D g2) {
		g2.setFont(main);
		g2.setPaint(new GradientPaint(30, 40, Color.BLUE, 102, 40, Color.CYAN));
		g2.drawRect(30, 30, 72, 50);
		g2.drawString("ESC", 40, 65);
	}

	public void paintMaze3D(Graphics2D g2) {
		ArrayList<Wall3D> walls = getWalls();
		for (int i = 0; i < walls.size(); i++) {
			g2.setColor(walls.get(i).getColor());
			g2.fill(walls.get(i).getPolygon());
			g2.setColor(Color.BLACK);
			g2.draw(walls.get(i).getPolygon());
		}

		paintCompass(g2, 20);

		//health
		g2.setColor(new Color(160, 0, 0));
		g2.fillRect(250, 15, explorer.getHealth() * 5, 50);
		g2.setStroke(new BasicStroke(5));
		g2.setFont(other);
		g2.setColor(Color.WHITE);
		g2.drawString(explorer.getHealth() + "", 240 - om.stringWidth(explorer.getHealth() + ""), 15 + om.getHeight() / 3 * 2);
		g2.drawRect(250, 15, 500, 50);

		//losing-light warning
		if (startColor < 140) {
			g2.setColor(new Color(255, 255, 255, 120));
			g2.setFont(main);
			String warning = "You are losing light! Quickly exit the maze!";
			g2.drawString(warning, frame.getWidth() / 2 - mm.stringWidth(warning) / 2, 760);
		}
	}

	public void darkenMaze() {
		double time = Math.round((System.nanoTime() - startTime) / 1000000000.0 * 100.0) / 100.0;
		if (time % 1.0 == 0) {
			startColor--; //darken color over time
			if (startColor < 4 * colorIncrement) { //check color range
				colorIncrement--; //less distance between colors
				if (colorIncrement < 0)
					colorIncrement = 0;
				startColor = 4 * colorIncrement;
			}
		}
		if (startColor < 20) {
			if (time % 0.5 == 0) {
				explorer.takeDamage(10); //explorer starts to take damage once maze is too dark
			}
		}
	}
	
	public ArrayList<Wall3D> getWalls() {
		int currentRow = explorer.getLoc().getRow(), currentCol = explorer.getLoc().getCol();
		ArrayList<Wall3D> walls = new ArrayList<Wall3D>();
		Wall3D frontWall = null;

		darkenMaze();

		for (int d = 0; d < RENDER_DISTANCE; d++) {
			final int WIDTH = 80, HEIGHT = 80;
			final int[] xpoints = {WIDTH * d, HEIGHT + WIDTH * d, HEIGHT + WIDTH * d, WIDTH * d};
			final int[] ypoints = {WIDTH * d, HEIGHT + WIDTH * d, frame.getHeight() - HEIGHT - WIDTH * d, frame.getHeight() - WIDTH * d};
			final Color color = new Color(startColor - colorIncrement * d, startColor - colorIncrement * d, startColor - colorIncrement * d);
			final Color darkerColor = new Color(color.getRed() - colorIncrement, color.getGreen() - colorIncrement, color.getBlue() - colorIncrement);
			Location leftWallLoc, rightWallLoc, centerWallLoc;

			switch(explorer.getDir()) {
				case EAST:
					leftWallLoc = new Location(currentRow - 1, currentCol + d);
					rightWallLoc = new Location(currentRow + 1, currentCol + d);
					centerWallLoc = new Location(currentRow, currentCol + d);
					break;
				case NORTH:
					leftWallLoc = new Location(currentRow - d, currentCol - 1);
					rightWallLoc = new Location(currentRow - d, currentCol + 1);
					centerWallLoc = new Location(currentRow - d, currentCol);
					break;
				case WEST:
					leftWallLoc = new Location(currentRow + 1, currentCol - d);
					rightWallLoc = new Location(currentRow - 1, currentCol - d);
					centerWallLoc = new Location(currentRow, currentCol - d);
					break;
				case SOUTH:
					leftWallLoc = new Location(currentRow + d, currentCol + 1);
					rightWallLoc = new Location(currentRow + d, currentCol - 1);
					centerWallLoc = new Location(currentRow + d, currentCol);
					break;
				default: 
					leftWallLoc = new Location(0, 0);
					rightWallLoc = new Location(0, 0);
					centerWallLoc = new Location(0, 0);
					break;
			}

			Color c;
			if (isOutOfBounds(leftWallLoc.getRow(), leftWallLoc.getCol()) || maze[leftWallLoc.getRow()][leftWallLoc.getCol()] instanceof Structure) {
				c = (isEnd(leftWallLoc.getRow(), leftWallLoc.getCol())) ? new Color(220, 220, 220) : color; //end is white
				walls.add(new Wall3D(new Polygon(xpoints, ypoints, 4), c));
			}
			if (isOutOfBounds(rightWallLoc.getRow(), rightWallLoc.getCol()) || maze[rightWallLoc.getRow()][rightWallLoc.getCol()] instanceof Structure) {
				c = (isEnd(rightWallLoc.getRow(), rightWallLoc.getCol())) ? new Color(220, 220, 220) : color; //end is white
				walls.add(new Wall3D(reflectPolygon(new Polygon(xpoints, ypoints, 4), false), c));
			}
			if ((frontWall == null) && (isOutOfBounds(centerWallLoc.getRow(), centerWallLoc.getCol()) || maze[centerWallLoc.getRow()][centerWallLoc.getCol()] instanceof Structure)) {
				c = (isEnd(centerWallLoc.getRow(), centerWallLoc.getCol())) ? new Color(220, 220, 220) : darkerColor; //end is white
				frontWall = new Wall3D(convertRectToPoly(new Rectangle(xpoints[0], ypoints[0], frame.getWidth() - 2 * xpoints[0], frame.getHeight() - 2 * ypoints[0])), c);
				walls.add(0, new Wall3D(convertRectToPoly(new Rectangle(xpoints[0] - (frame.getWidth() - 2 * xpoints[0]), ypoints[0], frame.getWidth() - 2 * xpoints[0], frame.getHeight() - 2 * ypoints[0])), darkerColor)); //left
				walls.add(0, new Wall3D(convertRectToPoly(new Rectangle(xpoints[0] + (frame.getWidth() - 2 * xpoints[0]), ypoints[0], frame.getWidth() - 2 * xpoints[0], frame.getHeight() - 2 * ypoints[0])), darkerColor)); //right
			}
			
			//rectangles to give effect of hallways
			Polygon hall = convertRectToPoly(new Rectangle(xpoints[0] - WIDTH, ypoints[0], WIDTH, ypoints[ypoints.length - 1] - ypoints[0]));
			walls.add(0, new Wall3D(hall, darkerColor)); //left
			walls.add(0, new Wall3D(reflectPolygon(hall, false), darkerColor)); //right

			//ceiligns and floors
			Polygon ceiling = convertRectToPoly(new Rectangle(0, ypoints[2], frame.getWidth(), ypoints[3] - ypoints[2]));
			walls.add(0, new Wall3D(ceiling, darkerColor)); //ceiling
			walls.add(0, new Wall3D(reflectPolygon(ceiling, true), darkerColor)); //floor
		}

		//front wall should always be on top
		if (frontWall != null)
			walls.add(frontWall);

		return walls;
	}

	public boolean isOutOfBounds(int row, int col) {
		return row < 0 || col < 0 || row >= maze.length || col >= maze[0].length;
	}

	public Polygon convertRectToPoly(Rectangle r) {
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
		for (int i = 0; i < maze.length; i++) {
			for (int j = 0; j < maze[0].length; j++) {
				Structure s = maze[i][j];
				if (s != null) {
					g2.setColor(s.getColor());
					g2.fillRect(s.getLoc().getCol() * s.getSize(), s.getLoc().getRow() * s.getSize(), s.getSize(), s.getSize());
				}
			}
		}
		g2.setColor(explorer.getColor());
		g2.fillOval(explorer.getLoc().getCol() * explorer.getSize(), explorer.getLoc().getRow() * explorer.getSize(), explorer.getSize(), explorer.getSize());
		paintCompass(g2, 500);
	}

	public void paintPauseMenu(Graphics2D g2) {
		int x = frame.getWidth() / 2 - 100;
		g2.setColor(Color.WHITE);
		g2.fillRect(x, 120, 200, 160);
		g2.setColor(Color.BLACK);
		g2.fillRect(x + 10, 130, 180, 140);

		g2.setFont(main);
		g2.setColor(Color.WHITE);
		int optionY = 180;
		Menu.Option[] options = pauseMenu.getOptions();
		for (int i = 0; i < options.length; i++) {
			g2.drawString(options[i].getName(), x + 70, optionY);
			optionY += 50;
		}
		for (int i = 0; i < options.length; i++) {
			if (options[i].isSelected()) {
				optionY = 165 + 50 * i;
				g2.setColor(Color.BLUE);
				g2.fillRect(x + 40, optionY, 10, 10);
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
		for (int i = 1; i <= 6; i++) {
			int x = (i <= 3) ? frame.getWidth() / 6 : frame.getWidth() / 6 + 400;
			int y = (i <= 3) ? frame.getHeight() / 6 + 220 * (i - 1) : frame.getHeight() / 6 + 220 * (i - 4);
			g2.setColor(Color.BLUE);
			g2.drawString((i <= 3) ? "2D Maze " + i : "3D Maze " + (i - 3), x, y);
			
			g2.setColor(Color.WHITE);
			y += mm.getHeight();
			g2.drawString("Best Time: " + ((i <= 3) ? Math.round(tracker2D.getMazeStats(i).getBestTime() * 1000) / 1000.0 : Math.round(tracker3D.getMazeStats(i - 3).getBestTime() * 1000) / 1000.0) + " seconds", x, y);
			y += mm.getHeight();
			g2.drawString("Least Moves: " + ((i <= 3) ? tracker2D.getMazeStats(i).getLeastMoves() : tracker3D.getMazeStats(i - 3).getLeastMoves()) + " moves", x, y);
			y += mm.getHeight();
			g2.drawString("Total Wins: " + ((i <= 3) ? tracker2D.getMazeStats(i).getNumWins() : tracker3D.getMazeStats(i - 3).getNumWins()) + " wins", x, y);
			y += mm.getHeight();
			g2.drawString("Total Losses: " + ((i <= 3) ? tracker2D.getMazeStats(i).getNumLosses() : tracker3D.getMazeStats(i - 3).getNumLosses()) + " losses", x, y);
		}
	}

	public void paintGameOver(Graphics2D g2) {
		g2.setColor(Color.WHITE);
		g2.setFont(other);
		if (win) {
			g2.drawString("YOU WIN", frame.getWidth() / 2 - om.stringWidth("YOU WIN") / 2, frame.getHeight() / 2);
			g2.setFont(main);
			g2.drawString("Number of moves: " + moveCount, frame.getWidth() / 2 - mm.stringWidth("Number of moves: " + moveCount) / 2, frame.getHeight() / 2 + om.getHeight());
		} else {
			g2.drawString("YOU LOSE", frame.getWidth() / 2 - om.stringWidth("YOU LOSE") / 2, frame.getHeight() / 2);
		}

		if (newBestTime) {
			g2.drawString("New best time!", frame.getWidth() / 2 - mm.stringWidth("New best time!") / 2, frame.getHeight() / 2 + om.getHeight() * 2);
		}
	}

	public void paintLevelSelect(Graphics2D g2) {
		int titleX = frame.getWidth() / 2 - om.stringWidth("Select Level") / 2;
		int titleY = frame.getHeight() / 4;
		g2.setFont(other);
		g2.setPaint(new GradientPaint(titleX, titleY, Color.BLUE, titleX + om.stringWidth("Select Level"), titleY, Color.CYAN));
		g2.drawString("Select Level", titleX, titleY);

		int optionX = frame.getWidth() / 2 - 250;
		Menu.Option[] options = levelSelectMenu.getOptions();
		for (int i = 0; i < options.length; i++) {
			g2.setFont(main);
			g2.setColor(Color.WHITE);
			g2.drawString(options[i].getName(), optionX, frame.getHeight() * 2 / 3);

			g2.setFont(title);
			int x = optionX + tm.stringWidth("1") / 2, y = frame.getHeight() / 2 + 50;
			Color c1, c2;
			switch (i) {
				case 0:
					c1 = Color.GREEN;
					c2 = new Color(0, 96, 0);
					break;
				case 2:
					c1 = Color.ORANGE;
					c2 = Color.RED;
					break;
				default:
					c1 = Color.CYAN;
					c2 = Color.BLUE;
					break;
			}
			g2.setPaint(new GradientPaint(x + tm.stringWidth("1") / 2, y - tm.getHeight(), c1, x + tm.stringWidth("1") / 2, y, c2));
			g2.drawString((i + 1) + "", x, y);
			optionX += 200;
		}
		optionX = frame.getWidth() / 2 - 250 - om.stringWidth("Level 3") / 2;
		for (int i = 0; i < options.length; i++) {
			if (options[i].isSelected()) {
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
		g2.setPaint(new GradientPaint(titleX, titleY - tm.getHeight(), Color.BLUE, titleX + tm.stringWidth("MAZE"), titleY, Color.CYAN));
		g2.drawString("MAZE", titleX, titleY);

		g2.setColor(Color.WHITE);
		g2.setFont(other);
		int optionY = frame.getHeight() / 2;
		Menu.Option[] options = mainMenu.getOptions();
		for (int i = 0; i < options.length; i++) {
			g2.drawString(options[i].getName(), frame.getWidth() / 2 - mm.stringWidth(options[i].getName()) / 2, optionY);
			optionY += 55;
		}
		for (int i = 0; i < options.length; i++) {
			if (options[i].isSelected()) {
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
					reset();
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
						moveCount++;
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

						playMusic("./sounds/maze_music.wav");

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
	}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public void reset() {
		playMusic("./sounds/menu_music.wav");
		startColor = 160;
		colorIncrement = 25;
		newBestTime = false;
		moveCount = 0;
		duration = 0;
		win = false;
	}

	public void delay(int ms) {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException e){}
	}

	public static void main(String[] args) {
		new Main();
	}
}