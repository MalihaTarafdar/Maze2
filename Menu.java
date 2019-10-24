public class Menu {

	private Option[] options;
	private int selectedIndex;

	public Menu(String... optionNames) {
		options = new Option[optionNames.length];
		for (int i = 0; i < options.length; i++) {
			options[i] = new Option(optionNames[i]);
		}
		selectedIndex = 0;
		options[selectedIndex].setSelected(true);
	}

	public static class Option {
		private String name;
		private boolean selected;
		public Option(String name) {
			this.name = name;
			selected = false;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public void setSelected(boolean selected) {
			this.selected = selected;
		}
		public boolean isSelected() {
			return selected;
		}
	}

	public void resetOptions() {
		selectedIndex = 0;
    options[0].setSelected(true);
    for (int x = 1; x < options.length; x++) {
			options[x].setSelected(false);
		}
	}

	public void moveForward() {
		options[selectedIndex].setSelected(false);
		selectedIndex++;
		if (selectedIndex >= options.length)
			selectedIndex = 0;
		options[selectedIndex].setSelected(true);
	}

	public void moveBackward() {
		options[selectedIndex].setSelected(false);
		selectedIndex--;
		if (selectedIndex < 0)
			selectedIndex = options.length - 1;
		options[selectedIndex].setSelected(true);
	}

	public Option[] getOptions() {
		return options;
	}
}