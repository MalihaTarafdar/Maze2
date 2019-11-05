import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Maze {

	private Structure[][] maze;
	private Explorer explorer;
	private Location end;

	public Maze(File map) {
		try {
			int row = 0, col = 0;
			final int SIZE = 12;
			BufferedReader temp = new BufferedReader(new FileReader(map));
			String line;
			int maxRow = 0, maxCol = 0;
			while ((line = temp.readLine()) != null) {
				if (line.length() > maxCol)
					maxCol = line.length();
				maxRow++;
			}
			temp.close();
			maze = new Structure[maxRow][maxCol];

			BufferedReader input = new BufferedReader(new FileReader(map));
			String text;
			while ((text = input.readLine()) != null) {
				for (int i = 0; i < text.length(); i++) {
					char c = text.charAt(i);
					if (c == '*') {
						maze[row][col] = new Wall(row, col, SIZE);
					} else if (c == 'S') {
						explorer = new Explorer(row, col, SIZE, 0);
					} else if (c == 'E') {
						end = new Location(row, col);
					}
					col++;
				}
				row++;
				col = 0;
			}
			input.close();
		} catch (IOException e) {
			System.err.println("File error");
			//TODO: handle error
		}
	}

	public Structure[][] getMaze() {
		return maze;
	}
	public Explorer getExplorer() {
		return explorer;
	}
	public Location getEnd() {
		return end;
	}
}