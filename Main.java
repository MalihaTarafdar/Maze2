import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class Main extends JPanel implements KeyListener, Runnable {
	private static final long serialVersionUID = 1L;

	private JFrame frame;
	private Thread thread;
	
	private Structure[][] maze;
	private GameState gameState = GameState.MAIN_MENU;
	private Explorer explorer;
	private Location end;
	private Maze map;
	
	private boolean onMaze2D;
	private boolean paused;
	private boolean win;
	
	private Menu mainMenu = new Menu("2D Maze", "3D Maze", "Settings", "Quit");
	private Menu levelSelectMenu = new Menu("Level 1", "Level 2", "Level 3");
	private Menu pauseMenu = new Menu("Resume", "Quit");

	private Font title = new Font("Positive System", Font.PLAIN, 100);
	private Font main = new Font("Game Over", Font.PLAIN, 70);
	private Font other = new Font("Game Over", Font.PLAIN, 100);
	private FontMetrics tm;
	private FontMetrics mm;
	private FontMetrics om;

	enum GameState {
		MAIN_MENU, LEVEL_SELECT, SETTINGS, MAZE2D, MAZE3D, GAME_OVER;
	}

	public Main() {		
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("./fonts/Positive System.otf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("./fonts/game_over.ttf")));
		} catch (IOException | FontFormatException e) {}

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
						gameState = GameState.GAME_OVER;
					}
					if (explorer.getLoc().getRow() == end.getRow() && explorer.getLoc().getCol() == end.getCol()) {
						win = true;
						gameState = GameState.GAME_OVER;
					}
				break;
				case GAME_OVER:
					delay(1000);
					gameState = GameState.MAIN_MENU;
					reset();
					repaint();
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
			case SETTINGS: paintSettings(g2);
				break;
			case GAME_OVER: paintGameOver(g2);
				break;
		}
	}

	public void paintMaze3D(Graphics2D g2) {

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

	public void paintSettings(Graphics2D g2) {

	}

	public void paintGameOver(Graphics2D g2) {
		g2.setColor(Color.WHITE);
		g2.setFont(other);
		if (win) {
			g2.drawString("YOU WIN", frame.getWidth() / 2 - om.stringWidth("YOU WIN") / 2, frame.getHeight() / 2);
		} else {
			g2.drawString("YOU LOSE", frame.getWidth() / 2 - om.stringWidth("YOU LOSE") / 2, frame.getHeight() / 2);
		}
	}

	public void paintMaze2D(Graphics2D g2) {
		for (Structure[] row : maze) {
			for (Structure s : row) {
				if (s != null) {
					g2.setColor(Color.GRAY);
					g2.fillRect(s.getLoc().getCol() * s.getSize(), s.getLoc().getRow() * s.getSize(), s.getSize(), s.getSize());
				}
			}
		}
		g2.setColor(Color.WHITE);
		g2.fillRect(explorer.getLoc().getCol() * explorer.getSize(), explorer.getLoc().getRow() * explorer.getSize(), explorer.getSize(), explorer.getSize());
		g2.drawString(explorer.getDir() + "", 700, 100);
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
			}
		} else {
			switch (gameState) {
				case MAZE3D:
				case MAZE2D:
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						explorer.move(explorer.getDir(), maze);
					} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						explorer.turn(Explorer.Direction.LEFT);
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						explorer.turn(Explorer.Direction.RIGHT);
					} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						paused = true;
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
							gameState = GameState.SETTINGS;
						} else {
							System.exit(0);
						}
					}
					break;
				case LEVEL_SELECT:
					if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						levelSelectMenu.moveBackward();
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						levelSelectMenu.moveForward();
					} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						if (levelSelectMenu.getOptions()[0].isSelected()) {
							map = new Maze(new File("./mazes/maze1.txt"));
						} else if (levelSelectMenu.getOptions()[1].isSelected()) {
							map = new Maze(new File("./mazes/maze2.txt"));
						} else {
							map = new Maze(new File("./mazes/maze3.txt"));
						}
						maze = map.getMaze();
						explorer = map.getExplorer();
						end = map.getEnd();
						if (onMaze2D) {
							gameState = GameState.MAZE2D;
						} else {
							gameState = GameState.MAZE3D;
						}
					} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						gameState = GameState.MAIN_MENU;
					}
					break;
				case SETTINGS:
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						gameState = GameState.MAIN_MENU;
					}
					break;
			}
		}

		if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			mainMenu.resetOptions();
			levelSelectMenu.resetOptions();
			pauseMenu.resetOptions();
		}
		repaint();
	}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public void reset() {
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