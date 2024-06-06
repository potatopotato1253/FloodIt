import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import tester.Tester;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

//Represents a single square of the game area
class Cell {
  // In logical coordinates, with the origin at the top-left corner of the screen
  int x;
  int y;
  Color color;
  boolean flooded;
  // the four adjacent cells to this one
  Cell left;
  Cell top;
  Cell right;
  Cell bottom;

  Cell(int x, int y, Color color) {
    this.x = x;
    this.y = y;
    this.color = color;
    this.flooded = false;
    this.left = null;
    this.top = null;
    this.right = null;
    this.bottom = null;
  }
}

class FloodItWorld extends World {
  // All the cells of the game
  ArrayList<ArrayList<Cell>> board;
  int sizeOfBoard;
  int numColor;
  Random ran;
  Color occupiedAreaColor;
  int maxClicks;
  int currClicks;
  boolean flooding;
  int maxSteps;
  int floodCounter;
  ArrayList<Cell> cellsToProcess;

  FloodItWorld(int sizeOfBoard, int numColor, Color occupiedAreaColor) {
    this.board = new ArrayList<ArrayList<Cell>>();
    this.sizeOfBoard = sizeOfBoard;
    this.numColor = numColor;
    this.ran = new Random();
    this.occupiedAreaColor = null;
    this.maxSteps = calculateMaxSteps();
    this.currClicks = 0;
    this.flooding = false;
    this.cellsToProcess = new ArrayList<Cell>();
    this.floodCounter = 0;
    this.makeBoard();

  }

  FloodItWorld(int sizeOfBoard, int numColor, Random ran) {
    this.board = new ArrayList<ArrayList<Cell>>();
    this.sizeOfBoard = sizeOfBoard;
    this.numColor = numColor;
    this.ran = ran;
    this.occupiedAreaColor = null;
    this.maxSteps = calculateMaxSteps();
    this.currClicks = 0;
    this.flooding = false;
    this.cellsToProcess = new ArrayList<Cell>();
    this.floodCounter = 0;
    this.makeBoard();

  }

  // Produce a board with a certain amount of rows and number of random color
  // in it
  void makeBoard() {
    board = new ArrayList<ArrayList<Cell>>();
    for (int y = 0; y < sizeOfBoard; y++) {
      ArrayList<Cell> row = new ArrayList<Cell>();
      for (int x = 0; x < sizeOfBoard; x++) {
        Color randomCol = makeRandomColor();
        Cell cell1 = new Cell(x, y, randomCol);
        row.add(cell1);
      }
      board.add(row);
    }

    for (int y = 0; y < sizeOfBoard; y++) {
      for (int x = 0; x < sizeOfBoard; x++) {
        Cell currentCell = getCell(x, y);

        if (x > 0) {
          currentCell.left = getCell(x - 1, y);
        }
        if (y > 0) {
          currentCell.top = getCell(x, y - 1);
        }
        if (x < sizeOfBoard - 1) {
          currentCell.right = getCell(x + 1, y);
        }
        if (y < sizeOfBoard - 1) {
          currentCell.bottom = getCell(x, y + 1);
        }
      }
    }
  }

  // Help to randomize the color pattern when running the game
  Color makeRandomColor() {
    int random1 = ran.nextInt(numColor);
    Color[] colors = {
        Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.PINK,
    };
    return colors[random1];
  }

  // Get the position of the cell in order to color code them
  Cell getCell(int posx, int posy) {
    return board.get(posy).get(posx);
  }

  @Override
  // Make a specific scene for the game base on the player choice of
  // size and number of color they want to play with
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(sizeOfBoard * 20, sizeOfBoard * 20);
    for (int y = 0; y < sizeOfBoard; y++) {
      for (int x = 0; x < sizeOfBoard; x++) {
        Cell cell = getCell(x, y);
        WorldImage cellImage = new RectangleImage(20, 20, OutlineMode.SOLID, cell.color);
        scene.placeImageXY(cellImage, cell.x * 20 + 10, cell.y * 20 + 10);
      }
    }
    WorldImage clicksRemaining = new TextImage("Clicks remaining: " + (maxClicks - currClicks), 16,
        Color.BLACK);
    scene.placeImageXY(clicksRemaining, sizeOfBoard * 10, sizeOfBoard * 20 - 30);

    if (wonYet()) {
      WorldImage winMessage = new TextImage("You won! Press 'R' to restart.", 16, Color.BLACK);
      scene.placeImageXY(winMessage, sizeOfBoard * 10, 20);
    }
    else if (currClicks >= maxClicks) {
      WorldImage loseMessage = new TextImage("You lost! Press 'R' to restart.", 16, Color.BLACK);
      scene.placeImageXY(loseMessage, sizeOfBoard * 10, 20);
    }

    return scene;
  }

  //  //Determines whether the game is over or not by checking if 
  //  //the player has either won or lost ye
  //  boolean isGameOver() {
  //    if (board == null) {
  //      // game hasn't started yet
  //      return false;
  //    }
  //    boolean gameWon = wonYet();
  //    boolean gameLost = currClicks > maxClicks;
  //
  //    return gameWon || gameLost;
  //  }
  //
  //  //Checks if the game is over, and if it is, returns a WorldEnd 
  //  //object indicating the game is over and the final scene to display
  //  public WorldEnd worldEnds() {
  //    if (isGameOver()) {
  //      return new WorldEnd(true, this.makeScene());
  //    }
  //    return new WorldEnd(false, this.makeScene());
  //

  // Handles the click on the board
  public void onMouseClicked(Posn pos) {
    int x = pos.x / 20;
    int y = pos.y / 20;

    if (x >= 0 && x < sizeOfBoard && y >= 0 && y < sizeOfBoard) {
      handleClick(x, y);
    }
  }

  //To handle a mouse click event on a cell in the game 
  //board and update the game board accordingly.
  void handleClick(int x, int y) {
    Cell clickedCell = getCell(x, y);
    Color clickedColor = clickedCell.color;
    Color initialColor = getCell(0, 0).color;

    if (!clickedColor.equals(initialColor)) {
      cellsToProcess.clear();
      cellsToProcess.add(getCell(0, 0));
      occupiedAreaColor = initialColor;
      flooding = true;
      floodFill(0, 0, initialColor, clickedColor);
      currClicks++;
    }
  }

  // To flood fill the game board starting from the given coordinates 
  //and changing all cells with the oldColor to the newColo
  void floodFill(int x, int y, Color oldColor, Color newColor) {
    if (x >= 0 && x < this.sizeOfBoard && y >= 0 && y < this.sizeOfBoard) {
      Cell currentCell = getCell(x, y);

      if (currentCell.color.equals(oldColor)) {
        currentCell.color = newColor;

        floodFill(x - 1, y, oldColor, newColor); // left
        floodFill(x + 1, y, oldColor, newColor); // right
        floodFill(x, y - 1, oldColor, newColor); // up
        floodFill(x, y + 1, oldColor, newColor); // down
      }
    }
  }

  //To flood fill the adjacent cells of a given cell 
  //with the same color as the given cell.
  void floodAdjacentCells(Cell cell) {
    ArrayList<Cell> nextCells = new ArrayList<>(
        Arrays.asList(cell.left, cell.top, cell.right, cell.bottom));

    for (Cell adjacentCell : nextCells) {
      if (adjacentCell != null && !adjacentCell.flooded
          && adjacentCell.color.equals(occupiedAreaColor)) {
        adjacentCell.color = cell.color;
        adjacentCell.flooded = true;
        cellsToProcess.add(adjacentCell);
      }
    }
  }

  // Updates the color of all flooded cells
  void updateClickedCellsColor(Color newColor) {
    for (ArrayList<Cell> row : board) {
      for (Cell cell : row) {
        if (cell.flooded) {
          cell.color = newColor;
        }
      }
    }
    // check for new cells to flood
    for (ArrayList<Cell> row : board) {
      for (Cell cell : row) {
        if (cell.flooded) {
          floodAdjacentCells(cell);
        }
      }
    }
  }

  // Takes in the coordinates of a cell, marks it as flooded,
  // and then floods the adjacent cells with the same color
  void flood(int x, int y) {
    Cell clickedCell = getCell(x, y);
    clickedCell.flooded = true;

    floodAdjacentCells(clickedCell);
  }

  // Returns true if the cell at (x, y) has adjacent cells with the same color
  boolean hasAdjacentCellsWithSameColor(int x, int y) {
    Cell cell = getCell(x, y);

    ArrayList<Cell> adjacentCells = new ArrayList<>(
        Arrays.asList(cell.left, cell.top, cell.right, cell.bottom));

    for (Cell adjacentCell : adjacentCells) {
      if (adjacentCell != null && adjacentCell.color.equals(cell.color)) {
        return true;
      }
    }
    return false;
  }

  // Call this method at the end of flood() or whenever you want to reset the
  // flooded state of cells
  void resetFlooded() {
    for (ArrayList<Cell> row : board) {
      for (Cell cell : row) {
        cell.flooded = false;
      }
    }
  }

  //Add this method to check if the player has won
  boolean wonYet() {
    Color firstCellColor = getCell(0, 0).color;
    for (ArrayList<Cell> row : board) {
      for (Cell cell : row) {
        if (!cell.color.equals(firstCellColor)) {
          return false;
        }
      }
    }
    return true;
  }

  //Updates the state of the game on every tick of the timer.
  @Override
  public void onTick() {
    if (flooding) {
      floodCounter++;
      if (floodCounter >= 5) {
        if (cellsToProcess.isEmpty()) {
          flooding = false;
        }
        else {
          Cell cellToProcess = cellsToProcess.remove(0);
          floodAdjacentCells(cellToProcess);
        }
        floodCounter = 0;
      }
    }
    else { 
      if (wonYet()) {
        // handle win condition
      }
      else if (currClicks >= maxClicks) {
       // handle loss condition
      }
    }
  }
  
  //Calculates the maximum number of steps a player can take in the game.
  int calculateMaxSteps() {
    return (sizeOfBoard * numColor) / 2;
  }

  //Handles the key events in the game.
  @Override
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      this.resetGame();
    }
  }

  //Resets the game to its initial state.
  void resetGame() {
    this.board = new ArrayList<ArrayList<Cell>>();
    this.maxSteps = calculateMaxSteps();
    this.currClicks = 0;
    this.flooding = false;
    this.makeBoard();
  }
}

class ExamplesFloodIt {

  FloodItWorld game1;
  Color randomColor1;
  ArrayList<Color> possibleColors1;

  FloodItWorld game2;
  Color randomColor2;
  ArrayList<Color> possibleColors2;

  FloodItWorld game3;
  Color randomColor3;
  ArrayList<Color> possibleColors3;

  FloodItWorld game4;
  Color randomColor4;
  ArrayList<Color> possibleColors4;

  FloodItWorld game5;
  Color randomColor5;
  ArrayList<Color> possibleColors5;

  FloodItWorld game6;
  Color randomColor6;
  ArrayList<Color> possibleColors6;

  FloodItWorld game7;
  Color randomColor7;
  ArrayList<Color> possibleColors7;

  FloodItWorld game0;
  Color randomColor0;
  ArrayList<Color> possibleColors0;

  // board of 2x2
  ArrayList<ArrayList<Cell>> game1cells;
  Cell left1;
  Cell right1;
  Cell bottomleft;
  Cell bottomright;

  // board of 3x3
  ArrayList<ArrayList<Cell>> game0cells;
  Cell lefttop;
  Cell leftmiddle;
  Cell leftbottom;
  Cell middletop;
  Cell middle;
  Cell middlebottom;
  Cell righttop;
  Cell rightmiddle;
  Cell rightbottom;

  // Initialize the game with the given board size and number of colors
  void testInitializeGame(Tester t) {
    FloodItWorld game = new FloodItWorld(2, 3, new Random(3));
    game.bigBang(40, 40, 1);

  }

  void initFlood() {
    this.game0 = new FloodItWorld(3, 3, new Random(3));
    this.randomColor1 = game0.makeRandomColor();
    this.possibleColors1 = new ArrayList<Color>(Arrays.asList(Color.RED, Color.BLUE, Color.GREEN));

    this.game1 = new FloodItWorld(2, 3, new Random(3));
    this.randomColor1 = game1.makeRandomColor();
    this.possibleColors1 = new ArrayList<Color>(Arrays.asList(Color.RED, Color.BLUE, Color.GREEN));

    this.game2 = new FloodItWorld(6, 4, new Random(4));
    this.randomColor2 = game2.makeRandomColor();
    this.possibleColors2 = new ArrayList<Color>(
        Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW));

    this.game3 = new FloodItWorld(10, 5, new Random(5));
    this.randomColor3 = game3.makeRandomColor();
    this.possibleColors3 = new ArrayList<Color>(
        Arrays.asList(Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.PINK));

    this.game4 = new FloodItWorld(14, 6, new Random(6));
    this.randomColor4 = game4.makeRandomColor();
    this.possibleColors4 = new ArrayList<Color>(
        Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.PINK));

    this.game5 = new FloodItWorld(18, 6, new Random(2));
    this.randomColor5 = game5.makeRandomColor();
    this.possibleColors5 = new ArrayList<Color>(
        Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.PINK));

    this.game6 = new FloodItWorld(22, 3, new Random(6));
    this.randomColor6 = game6.makeRandomColor();
    this.possibleColors6 = new ArrayList<Color>(
        Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.PINK));

    this.game7 = new FloodItWorld(26, 2, new Random(4));
    this.randomColor7 = game7.makeRandomColor();
    this.possibleColors7 = new ArrayList<Color>(
        Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE));

    // Example to test a 2x2 board for MakeBoard

    this.left1 = new Cell(0, 0, Color.GREEN);
    this.right1 = new Cell(1, 0, Color.GREEN);
    this.bottomleft = new Cell(0, 1, Color.RED);
    this.bottomright = new Cell(1, 1, Color.BLUE);

    this.game1cells = new ArrayList<ArrayList<Cell>>(
        Arrays.asList(new ArrayList<Cell>(Arrays.asList(this.left1, this.right1)),
            new ArrayList<Cell>(Arrays.asList(this.bottomleft, this.bottomright))));

    this.lefttop = new Cell(0, 0, Color.GREEN);
    this.leftmiddle = new Cell(0, 1, Color.BLUE);
    this.leftbottom = new Cell(0, 2, Color.RED);
    this.middletop = new Cell(1, 0, Color.GREEN);
    this.middle = new Cell(1, 1, Color.RED);
    this.middlebottom = new Cell(1, 2, Color.BLUE);
    this.righttop = new Cell(2, 0, Color.RED);
    this.rightmiddle = new Cell(2, 1, Color.RED);
    this.rightbottom = new Cell(2, 2, Color.BLUE);

    this.game0cells = new ArrayList<ArrayList<Cell>>(Arrays.asList(
        new ArrayList<Cell>(Arrays.asList(this.lefttop, this.middletop, this.righttop)),
        new ArrayList<Cell>(Arrays.asList(this.leftmiddle, this.middle, this.rightmiddle)),
        new ArrayList<Cell>(Arrays.asList(this.leftbottom, this.middlebottom, this.rightbottom))));
  }

  void testMakeScene(Tester t) {
    initFlood();

    WorldScene expectedScene1 = new WorldScene(40, 40);
    RectangleImage cellImage1 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game1.getCell(0, 0).color);
    RectangleImage cellImage2 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game1.getCell(1, 0).color);
    RectangleImage cellImage3 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game1.getCell(0, 1).color);
    RectangleImage cellImage4 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game1.getCell(1, 1).color);
    expectedScene1.placeImageXY(cellImage1, 10, 10);
    expectedScene1.placeImageXY(cellImage2, 30, 10);
    expectedScene1.placeImageXY(cellImage3, 10, 30);
    expectedScene1.placeImageXY(cellImage4, 30, 30);

    WorldImage clicksRemaining = new TextImage(
        "Clicks remaining: " + (game1.maxClicks - game1.currClicks), 16, Color.BLACK);
    expectedScene1.placeImageXY(clicksRemaining, 20, 10);

    if (game1.wonYet()) {
      WorldImage winMessage = new TextImage("You won! Press 'R' to restart.", 16, Color.BLACK);
      expectedScene1.placeImageXY(winMessage, 20, 20);
    }
    else if (game1.currClicks >= game1.maxClicks) {
      WorldImage loseMessage = new TextImage("You lost! Press 'R' to restart.", 16, Color.BLACK);
      expectedScene1.placeImageXY(loseMessage, 20, 20);
    }

    t.checkExpect(game1.makeScene(), expectedScene1);

    WorldScene expectedScene0 = new WorldScene(60, 60);
    RectangleImage cellImage5 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game0.getCell(0, 0).color);
    RectangleImage cellImage6 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game0.getCell(1, 0).color);
    RectangleImage cellImage7 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game0.getCell(2, 0).color);
    RectangleImage cellImage8 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game0.getCell(0, 1).color);
    RectangleImage cellImage9 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game0.getCell(1, 1).color);
    RectangleImage cellImage10 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game0.getCell(2, 1).color);
    RectangleImage cellImage11 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game0.getCell(0, 2).color);
    RectangleImage cellImage12 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game0.getCell(1, 2).color);
    RectangleImage cellImage13 = new RectangleImage(20, 20, OutlineMode.SOLID,
        game0.getCell(2, 2).color);
    expectedScene0.placeImageXY(cellImage5, 10, 10);
    expectedScene0.placeImageXY(cellImage6, 30, 10);
    expectedScene0.placeImageXY(cellImage7, 50, 10);
    expectedScene0.placeImageXY(cellImage8, 10, 30);
    expectedScene0.placeImageXY(cellImage9, 30, 30);
    expectedScene0.placeImageXY(cellImage10, 50, 30);
    expectedScene0.placeImageXY(cellImage11, 10, 50);
    expectedScene0.placeImageXY(cellImage12, 30, 50);
    expectedScene0.placeImageXY(cellImage13, 50, 50);
    WorldImage clicksRemaining2 = new TextImage(
        "Clicks remaining: " + (game0.maxClicks - game0.currClicks), 16, Color.BLACK);
    expectedScene0.placeImageXY(clicksRemaining2, 30, 30);
    if (game1.wonYet()) {
      WorldImage winMessage = new TextImage("You won! Press 'R' to restart.", 16, Color.BLACK);
      expectedScene0.placeImageXY(winMessage, 30, 20);
    }
    else if (game1.currClicks >= game1.maxClicks) {
      WorldImage loseMessage = new TextImage("You lost! Press 'R' to restart.", 16, Color.BLACK);
      expectedScene0.placeImageXY(loseMessage, 30, 20);
    }

    t.checkExpect(game0.makeScene(), expectedScene0);

  }

  void testMakeRandomColor(Tester t) {
    initFlood();
    t.checkExpect(randomColor1, Color.RED);
    t.checkExpect(randomColor2, Color.BLUE);
    t.checkExpect(randomColor3, Color.RED);
    t.checkExpect(randomColor4, Color.GREEN);
    t.checkExpect(randomColor5, Color.BLUE);
    t.checkExpect(randomColor6, Color.RED);
    t.checkExpect(randomColor7, Color.BLUE);
  }

  void testMakeBoard(Tester t) {
    initFlood();
    // connecting cells for 2x2 board
    this.left1.right = this.right1;
    this.right1.left = this.left1;
    this.bottomleft.right = this.bottomright;
    this.bottomright.left = this.bottomleft;
    this.left1.bottom = this.bottomleft;
    this.bottomleft.top = this.left1;
    this.right1.bottom = this.bottomright;
    this.bottomright.top = this.right1;
    t.checkExpect(game1.board, game1cells);
    t.checkExpect(game1.board.get(0).size(), 2);
    t.checkExpect(game1.board.get(1).size(), 2);

    // connecting cell for 3x3 board
    this.lefttop.right = this.middletop;
    this.lefttop.bottom = this.leftmiddle;

    this.leftmiddle.right = this.middle;
    this.leftmiddle.top = this.lefttop;
    this.leftmiddle.bottom = this.leftbottom;

    this.leftbottom.right = this.middlebottom;
    this.leftbottom.top = this.leftmiddle;

    this.middletop.left = this.lefttop;
    this.middletop.right = this.righttop;
    this.middletop.bottom = this.middle;

    this.middle.top = this.middletop;
    this.middle.bottom = this.middlebottom;
    this.middle.left = this.leftmiddle;
    this.middle.right = this.rightmiddle;

    this.middlebottom.top = this.middle;
    this.middlebottom.left = this.leftbottom;
    this.middlebottom.right = this.rightbottom;

    this.righttop.left = this.middletop;
    this.righttop.bottom = this.rightmiddle;

    this.rightmiddle.top = this.righttop;
    this.rightmiddle.bottom = this.rightbottom;
    this.rightmiddle.left = this.middle;

    this.rightbottom.top = this.rightmiddle;
    this.rightbottom.left = this.middlebottom;
    t.checkExpect(game0.board, game0cells);

    t.checkExpect(game2.board.get(0).size(), 6);
    t.checkExpect(game2.board.get(1).size(), 6);

    // some test just to get the size
    t.checkExpect(game3.board.get(0).size(), 10);
    t.checkExpect(game3.board.get(1).size(), 10);
  }

  void testGetCell(Tester t) {
    initFlood();
    t.checkExpect(game1.getCell(0, 0), game1.board.get(0).get(0));
    t.checkExpect(game2.getCell(2, 4), game2.board.get(4).get(2));
    t.checkExpect(game3.getCell(9, 5), game3.board.get(5).get(9));
    t.checkExpect(game4.getCell(5, 7), game4.board.get(7).get(5));
    t.checkExpect(game5.getCell(8, 3), game5.board.get(3).get(8));
    t.checkExpect(game6.getCell(21, 10), game6.board.get(10).get(21));
    t.checkExpect(game7.getCell(12, 25), game7.board.get(25).get(12));
  }

  void testOnMouseClicked(Tester t) {
    initFlood();

    t.checkExpect(game1.getCell(0, 0).color, Color.GREEN);
    game1.onMouseClicked(new Posn(30, 30));
    Color initialColor = game1.getCell(0, 0).color;
    t.checkExpect(initialColor, game1.getCell(1, 0).color);
    t.checkExpect(game1.getCell(1, 1).color, Color.BLUE);
  }

  void testHandleClick(Tester t) {
    initFlood();

    t.checkExpect(game1.getCell(0, 0).color, Color.GREEN);
    t.checkExpect(game1.currClicks, 0);
    // Click on the bottom-right cell with the blue color
    game1.handleClick(1, 1);
    t.checkExpect(game1.getCell(0, 0).color, Color.BLUE);
    t.checkExpect(game1.getCell(1, 0).color, Color.BLUE);
    t.checkExpect(game1.currClicks, 1);

    initFlood();
    t.checkExpect(game1.getCell(0, 0).color, Color.GREEN);
    t.checkExpect(game1.currClicks, 0);
    // Click on the top-right cell with the green color
    game1.handleClick(1, 0);
    t.checkExpect(game1.getCell(0, 0).color, Color.GREEN);
    t.checkExpect(game1.getCell(1, 0).color, Color.GREEN);
    t.checkExpect(game1.currClicks, 0);
  }

  void testFloodFill(Tester t) {
    initFlood();

    // Test on a 3x3 board
    this.game0.floodFill(0, 0, this.lefttop.color, Color.PINK);
    t.checkExpect(this.game0cells.get(0).get(0).color, Color.GREEN);
    t.checkExpect(this.game0cells.get(1).get(0).color, Color.BLUE);
    t.checkExpect(this.game0cells.get(2).get(0).color, Color.RED);
    t.checkExpect(this.game0cells.get(0).get(1).color, Color.GREEN);
    t.checkExpect(this.game0cells.get(1).get(1).color, Color.RED);
    t.checkExpect(this.game0cells.get(2).get(1).color, Color.BLUE);
    t.checkExpect(this.game0cells.get(0).get(2).color, Color.RED);
    t.checkExpect(this.game0cells.get(1).get(2).color, Color.RED);
    t.checkExpect(this.game0cells.get(2).get(2).color, Color.BLUE);

    // Test on a 2x3 board
    this.game1.floodFill(0, 0, this.left1.color, Color.YELLOW);
    t.checkExpect(this.game1cells.get(0).get(0).color, Color.GREEN);
    t.checkExpect(this.game1cells.get(1).get(0).color, Color.RED);
    t.checkExpect(this.game1cells.get(0).get(1).color, Color.GREEN);
    t.checkExpect(this.game1cells.get(1).get(1).color, Color.BLUE);
  }

  void testUpdateClickedCellsColor(Tester t) {
    // Initialize the game with a 3x3 board and 3 colors
    FloodItWorld game = new FloodItWorld(3, 3, new Random(1));
    game.makeBoard();
    game.board.get(0).get(0).flooded = true;
    game.board.get(0).get(1).flooded = true;
    game.board.get(1).get(0).flooded = true;
    game.updateClickedCellsColor(Color.BLUE);

    // Check if all flooded cells have been updated with the new color
    for (ArrayList<Cell> row : game.board) {
      for (Cell cell : row) {
        if (cell.flooded) {
          t.checkExpect(cell.color, Color.BLUE);
        }
      }
    }
  }

  // test if won yet
  void testWonYet(Tester t) {
    initFlood();

    t.checkExpect(game1.wonYet(), false);

    FloodItWorld gameWon = new FloodItWorld(3, 3, new Random(3));
    gameWon.board.get(0).get(0).color = Color.BLUE;
    gameWon.board.get(0).get(1).color = Color.BLUE;
    gameWon.board.get(0).get(2).color = Color.BLUE;
    gameWon.board.get(1).get(0).color = Color.BLUE;
    gameWon.board.get(1).get(1).color = Color.BLUE;
    gameWon.board.get(1).get(2).color = Color.BLUE;
    gameWon.board.get(2).get(0).color = Color.BLUE;
    gameWon.board.get(2).get(1).color = Color.BLUE;
    gameWon.board.get(2).get(2).color = Color.BLUE;

    t.checkExpect(gameWon.wonYet(), true);
  }

  void testonKeyEvent(Tester t) {
    initFlood();

    // Test pressing 'R' to restart the game
    game1.onKeyEvent("R");
    t.checkExpect(game1.currClicks, 0);
    t.checkExpect(game1.board.get(0).get(0).color, Color.GREEN);
    t.checkExpect(game1.board.get(1).get(1).color, Color.BLUE);

    // Test pressing other keys does not restart the game
  }

  //  void testGameOver(Tester t) {
  //    // Test when the game has not ended
  //    t.checkExpect(game1.isGameOver(), false);
  //
  //    // Test when the game has ended due to the player winning
  //    game1.currClicks = 0;
  //    game1.board.get(0).get(0).color = Color.BLUE;
  //    game1.board.get(0).get(1).color = Color.BLUE;
  //    game1.board.get(1).get(0).color = Color.BLUE;
  //    game1.board.get(1).get(1).color = Color.BLUE;
  //    t.checkExpect(game1.isGameOver(), true);
  //
  //    // Test when the game has ended due to the player losing (running out of clicks)
  //    game1.currClicks = game1.maxClicks;
  //    t.checkExpect(game1.isGameOver(), true);
  //  }
  //
  //  void testWorldEnds(Tester t) {
  //    initFlood();
  //
  //    t.checkExpect(game1.worldEnds().worldEnds, false);
  //
  //    // this is to simulate the game until it ends in a win
  //    game1.board.get(0).get(0).color = Color.BLUE;
  //    game1.board.get(0).get(1).color = Color.BLUE;
  //    game1.board.get(1).get(0).color = Color.BLUE;
  //    game1.board.get(1).get(1).color = Color.BLUE;
  //    game1.onMouseClicked(new Posn(0, 0));
  //    t.checkExpect(game1.worldEnds().worldEnds, true);
  //
  //    game1 = new FloodItWorld(2, 3, new Random(3));
  //    game1.currClicks = game1.maxClicks;
  //    game1.onMouseClicked(new Posn(0, 0));
  //  t.checkExpect(game1.worldEnds().worldEnds, false);


  void testFlood(Tester t) {
    initFlood();
    FloodItWorld game = game1;

    // Set up the board with a known configuration
    ArrayList<ArrayList<Cell>> board = game.board;
    board.get(0).get(0).color = Color.RED;
    board.get(0).get(1).color = Color.BLUE;
    board.get(1).get(0).color = Color.YELLOW;
    board.get(1).get(1).color = Color.GREEN;

    game.flood(0, 0);

    t.checkExpect(game.getCell(0, 0).color, Color.RED);
    t.checkExpect(game.getCell(0, 1).color, Color.YELLOW);
    t.checkExpect(game.getCell(1, 0).color, Color.BLUE);
    t.checkExpect(game.getCell(1, 1).color, Color.GREEN);
  }

  void testResetFlooded(Tester t) {
    // Initialize game1
    this.initFlood();
    FloodItWorld game1 = this.game1;
    ArrayList<ArrayList<Cell>> board1 = game1.board;

    board1.get(0).get(0).color = Color.RED;
    board1.get(0).get(1).color = Color.GREEN;
    board1.get(1).get(0).color = Color.GREEN;
    board1.get(1).get(1).color = Color.YELLOW;

    game1.flood(0, 0);
    t.checkExpect(board1.get(0).get(0).flooded, true);
    t.checkExpect(board1.get(0).get(1).flooded, true);
    t.checkExpect(board1.get(1).get(0).flooded, true);
    t.checkExpect(board1.get(1).get(1).flooded, true);
    // the last three test here is failing because it is not being flooded

    game1.resetFlooded();
    t.checkExpect(board1.get(0).get(0).flooded, false);
    t.checkExpect(board1.get(0).get(1).flooded, false);
    t.checkExpect(board1.get(1).get(0).flooded, false);
    t.checkExpect(board1.get(1).get(1).flooded, false);
  }

  void testOnTick(Tester t) {
    FloodItWorld game = new FloodItWorld(2, 2, new Random(1));

    t.checkExpect(game.getCell(0, 0).color, Color.BLUE);
    t.checkExpect(game.getCell(0, 1).color, Color.RED);
    t.checkExpect(game.getCell(1, 0).color, Color.RED);
    t.checkExpect(game.getCell(1, 1).color, Color.RED);

    game.onMousePressed(new Posn(5, 5));
    t.checkExpect(game.getCell(0, 0).color, Color.BLUE);

    game.onTick();
    t.checkExpect(game.getCell(0, 0).color, Color.BLUE);
    t.checkExpect(game.getCell(0, 1).color, Color.RED);
    t.checkExpect(game.getCell(1, 0).color, Color.RED);
    t.checkExpect(game.getCell(1, 1).color, Color.RED);
  }

  void testCalculateMaxSteps(Tester t) {
    initFlood();

    t.checkExpect(game0.calculateMaxSteps(), 4);
    t.checkExpect(game1.calculateMaxSteps(), 3);
    t.checkExpect(game2.calculateMaxSteps(), 12);
    t.checkExpect(game3.calculateMaxSteps(), 25);
    t.checkExpect(game4.calculateMaxSteps(), 42);
    t.checkExpect(game5.calculateMaxSteps(), 54);
    t.checkExpect(game6.calculateMaxSteps(), 33);
    t.checkExpect(game7.calculateMaxSteps(), 26);
  }

  void testOnKeyEvent(Tester t) {
    initFlood();

    FloodItWorld newgame = new FloodItWorld(2, 3, new Random(3));
    game1.occupiedAreaColor = Color.RED;
    game1.floodFill(0, 0, game1.getCell(0, 0).color, Color.RED);

    t.checkExpect(game1.getCell(0, 0).color, Color.RED);
    t.checkExpect(game1.getCell(1, 0).color, Color.RED);
    t.checkExpect(game1.getCell(0, 1).color, Color.RED);
    t.checkExpect(game1.getCell(1, 1).color, Color.RED); 

    // Reset the game
    game1.onKeyEvent("r");

    // So for this test right here I was trying to make so that the first 
    //game1 get flood and then when
    // you click r it should make a new game where all the cell is not the same color
    // and then I compare if the first game1 color in this cell is equal to 
    // the new game cell color that this position 
    t.checkExpect(game1.getCell(0, 1).color.equals(newgame.getCell(0, 1).color), false);
    t.checkExpect(game1.getCell(0, 0).color.equals(newgame.getCell(0, 0).color), false);
    t.checkExpect(game1.getCell(1, 0).color.equals(newgame.getCell(1, 0).color), false);
    t.checkExpect(game1.getCell(1, 1).color.equals(newgame.getCell(1, 1).color), true);
  }

  void testResetGame(Tester t) {
    initFlood();
    game1.resetGame();
    
    t.checkExpect(game1.sizeOfBoard, 2);
    t.checkExpect(game1.numColor, 3);
    t.checkExpect(game1.maxSteps, 3);
    t.checkExpect(game1.currClicks, 0);
    t.checkExpect(game1.flooding, false);
  }
  
}
