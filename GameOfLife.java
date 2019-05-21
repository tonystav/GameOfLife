import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GameOfLife extends JPanel implements ItemListener {
    // Serial Version UID needed to suppress warning:
    // "The serializable class BWGameOfLife does not declare a static final serialVersionUID field of type long"
    private static final long serialVersionUID = 1L;

    /* Inner class that defines drawing modes:
     * 1. Free hand drawing on main grid,
     * 2. Stamping from pattern grid to main grid
     * 3. Screen copy from main grid to pattern grid
     */
    private static class drawingMode {
	private enum drawingModes { freeHand, patternStamp, screenCopy };
	private static drawingModes currentDrawingMode;

	public drawingMode () {
	    drawingMode.currentDrawingMode = drawingModes.freeHand;
	}

	public boolean isFreeHand() {
	    if (drawingMode.currentDrawingMode == drawingModes.freeHand) { return true; }
	    else { return false; }
	    }

	public void setFreeHand() {
	    drawingMode.currentDrawingMode = drawingModes.freeHand;
	}

	public boolean isScreenCopy() {
	    if (drawingMode.currentDrawingMode == drawingModes.screenCopy) { return true; }
	    else { return false; }
	}

	public void setScreenCopy() {
	    drawingMode.currentDrawingMode = drawingModes.screenCopy;
	}

	public boolean isPatternStamp() {
	    if (drawingMode.currentDrawingMode == drawingModes.patternStamp) { return true; }
	    else { return false; }
	}

	public void setPatternStamp() {
	    drawingMode.currentDrawingMode = drawingModes.patternStamp;
	}
    }

    /* Inner class that defines display modes:
     * 1. Black & white
     * 2. Grey scale (9 levels, to match the number of neighboring cells)
     * 3. Color scale (black, brown, red, orange, yellow, green, blue purple, white)
     */
    private static class displayMode {
	private enum displayModes { blackWhite, greyScale, colorScale };
	private static displayModes currentDisplayMode;

	public displayMode () {
	    displayMode.currentDisplayMode = displayModes.blackWhite;
	}

	public boolean isBlackWhite() {
	    if (displayMode.currentDisplayMode == displayModes.blackWhite) { return true; }
	    else { return false; }
	}

	public void setBlackWhite() {
	    displayMode.currentDisplayMode = displayModes.blackWhite;
	}

	public boolean isGreyScale() {
	    if (displayMode.currentDisplayMode == displayModes.greyScale) { return true; }
	    else { return false; }
	}

	public void setGreyScale() {
	    displayMode.currentDisplayMode = displayModes.greyScale;
	}

	public boolean isColorScale() {
	    if (displayMode.currentDisplayMode == displayModes.colorScale) { return true; }
	    else { return false; }
	}

	public void setColorScale() {
	    displayMode.currentDisplayMode = displayModes.colorScale;
	}
    }

    // Window sections
    private static JFrame gridFrame;
    private static JLayeredPane layeredPanel;
    private static JPanel gridPanel, patternPanel, gridToolPanel, rulePanel;
    //private static JToolBar gridToolBar;

    // Window components
    private static JCheckBoxMenuItem[] cbmi;
    private static JTextArea rowValue, colValue, counterValue;
    private static Action startStop, step, clear;
    private static JLabel patternGridLabel;
    private static JLabel testLabel;

    // Grid parameters
    private static int[][] gridLeft, gridRight, patternGrid;
    private static Integer gridX, gridY, startX, startY, endX, endY, pGridX, pGridY, pStartX, pStartY, pEndX, pEndY;
    private static Integer screenSize = 720, gridSize = 360;
    private static Integer patternSize = (screenSize / 3) + 1, patternGridSize = (gridSize / 3);
    private static Integer rectangleSize = 2;
    private static Integer minimumOnMaximumOff = 2, maximumOnMinimumOff = 3;
    private static boolean[] wildcardsOn = new boolean[9];	// Use 9 values in order to access elements by actual value
    private static boolean[] wildcardsOff = new boolean[9];	// 9 elements = 0 through 8; ignore 0, use only 1 through 8

    // Timing parameters
    private static Timer timer;
    private static Integer delayTime = 100;
    private static Integer generation = 0;

    // Settings
    private static Boolean running = false, leftFrame = true, dragging = false, wrapAround = false, wildcardsOnly = false, showGrid = false;
    private static drawingMode drawingModeCurrently;
    private static displayMode displayModeCurrently;

	public GameOfLife() {
	    super(new BorderLayout());
	    resetGrids();

	    // Display grid setup
            gridPanel = new JPanel();
            gridPanel.setPreferredSize(new Dimension(screenSize, screenSize));
            gridPanel.setSize(screenSize, screenSize);
            gridPanel.setBounds(0, 0, gridSize * rectangleSize, gridSize * rectangleSize);
            gridPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

            gridPanel.addMouseListener(new MouseAdapter() {
        	public void mouseExited(MouseEvent msvnt) {
        	    gridFrame.repaint();
        	}

        	public void mouseClicked(MouseEvent msvnt) {
        	    Graphics g = getGraphics();
        	    Graphics2D g2d = (Graphics2D) g;
        	    int xMod = 0, yMod = 0;

        	    try {
	                startX = msvnt.getX(); startY = msvnt.getY();

	                xMod = startX % rectangleSize; yMod = startY % rectangleSize;
	                if ((xMod) != 0) { startX = startX - xMod; }
	                if ((yMod) != 0) { startY = startY - yMod; }

			// Allow for mapping of 1 grid cell to given # of screen pixels
			gridX = startX / rectangleSize; gridY = startY / rectangleSize;

	                if (drawingModeCurrently.isFreeHand()) {
				// Process only those clicks that occur inside the grid limits
		                if (gridX <= gridSize && gridY <= gridSize) {
					int cellValue = 0;
					if (leftFrame) { cellValue = reverseCell(gridLeft, gridX, gridY); }
			        	else { cellValue = reverseCell(gridRight, gridX, gridY); }

					if (cellValue == 0) {
					    g2d.setColor(Color.BLACK);
					}
					else {
					    if (displayModeCurrently.isBlackWhite()) {
						g2d.setColor(Color.WHITE);
					    }
					    else if (displayModeCurrently.isGreyScale()) {
						if (leftFrame) {
						    cellValue = checkCell(gridLeft, gridX, gridY) + 1;
						    gridLeft[gridX][gridY] = cellValue;
						}
						else {
						    cellValue = checkCell(gridRight, gridX, gridY) + 1;
						    gridRight[gridX][gridY] = cellValue;
						}

						int greyShade = 255 / cellValue;
						Color clr = new Color(greyShade, greyShade, greyShade);
						g2d.setColor(clr);
					    }
					    else if (displayModeCurrently.isColorScale()) {
						if (leftFrame) {
						    cellValue = checkCell(gridLeft, gridX, gridY) + 1;
						    gridLeft[gridX][gridY] = cellValue;
						}
						else {
						    cellValue = checkCell(gridRight, gridX, gridY) + 1;
						    gridRight[gridX][gridY] = cellValue;
						}

						switch (cellValue) {
						    case 1:	g2d.setColor(Color.WHITE);
						    		break;
						    case 2:	g2d.setColor(Color.RED);
					    			break;
						    case 3:	g2d.setColor(Color.ORANGE);
					    			break;
						    case 4:	g2d.setColor(Color.YELLOW);
					    			break;
						    case 5:	g2d.setColor(Color.GREEN);
					    			break;
						    case 6:	g2d.setColor(Color.BLUE);
					    			break;
						    case 7:	g2d.setColor(new Color(102, 0, 153));	// Purple
					    			break;
						    case 8:	g2d.setColor(new Color(102, 51, 0));	// Brown
					    			break;
						}
					    }
					}

					g2d.fillRect(startX, startY, rectangleSize, rectangleSize);
					gridFrame.repaint();
		                }
	                }
	                // Only need to handle alternate drawing modes here
	                else if (drawingModeCurrently.isPatternStamp()) {
	                	stampGrid(gridX, gridY);
	                	g2d.setColor(Color.WHITE);

	                	if (leftFrame) { paintFromThisGrid(gridLeft, g2d); }
	                	else { paintFromThisGrid(gridRight, g2d); }
	                }
	                else if (drawingModeCurrently.isScreenCopy()) {
	                	copyGrid(gridX, gridY);
	                	g2d.setColor(Color.WHITE);
	                	paintPatternGrid(g2d);
	                }
        	    }
        	    catch (ArrayIndexOutOfBoundsException aioobe) {
        		// Ignore any clicks that occur outside the panel boundaries
        	    }
        	}
            });

            gridPanel.addMouseMotionListener(new MouseMotionAdapter() {
        	public void mouseMoved(MouseEvent msvnt) {
        	    int startX = msvnt.getX(); int startY = msvnt.getY();
        	    int focusArea = (screenSize - patternSize);
        	    rowValue.setText(Integer.toString(startY));
        	    colValue.setText(Integer.toString(startX));

        	    // Update select rectangle when game is not active
        	    if ((drawingModeCurrently.isPatternStamp()) || (drawingModeCurrently.isScreenCopy())) {
                	if ((startX < focusArea) && (startY < focusArea)) {
                	    gridFrame.repaint();
                	}
        	    }
        	}

        	public void mouseDragged(MouseEvent msvnt) {
        	    Graphics g = getGraphics();
        	    Graphics2D g2d = (Graphics2D) g;
        	    int xMod = 0, yMod = 0;

        	    try {
                	rowValue.setText(Integer.toString(msvnt.getY()));
            		colValue.setText(Integer.toString(msvnt.getX()));

	                startX = msvnt.getX(); startY = msvnt.getY();

	                // Allow drawing only when style is free hand:
	                // No need to handle alternate drawing modes when dragging mouse
	                if (drawingModeCurrently.isFreeHand()) {
		                xMod = startX % rectangleSize; yMod = startY % rectangleSize;
		                if ((xMod) != 0) { startX = startX - xMod; }
		                if ((yMod) != 0) { startY = startY - yMod; }

				// Allow for mapping of 1 grid cell to given # of screen pixels
	                	gridX = startX / rectangleSize; gridY = startY / rectangleSize;

				// Process only those clicks that occur inside the grid limits
		                if (gridX <= gridSize && gridY <= gridSize) {
					int cellValue = 0;
					if (leftFrame) { cellValue = reverseCell(gridLeft, gridX, gridY); }
			        	else { cellValue = reverseCell(gridRight, gridX, gridY); }

					if (cellValue == 0) {
					    g2d.setColor(Color.BLACK);
					}
					else {
					    if (displayModeCurrently.isBlackWhite()) {
						g2d.setColor(Color.WHITE);
					    }
					    else if (displayModeCurrently.isGreyScale()) {
						if (leftFrame) {
						    cellValue = checkCell(gridLeft, gridX, gridY) + 1;
						    gridLeft[gridX][gridY] = cellValue;
						}
						else {
						    cellValue = checkCell(gridRight, gridX, gridY) + 1;
						    gridRight[gridX][gridY] = cellValue;
						}

						int greyShade = 255 / cellValue;
						Color clr = new Color(greyShade, greyShade, greyShade);
						g2d.setColor(clr);
					    }
					    else if (displayModeCurrently.isColorScale()) {
						if (leftFrame) {
						    cellValue = checkCell(gridLeft, gridX, gridY) + 1;
						    gridLeft[gridX][gridY] = cellValue;
						}
						else {
						    cellValue = checkCell(gridRight, gridX, gridY) + 1;
						    gridRight[gridX][gridY] = cellValue;
						}

						switch (cellValue) {
						    case 1:	g2d.setColor(Color.WHITE);
						    		break;
						    case 2:	g2d.setColor(Color.RED);
					    			break;
						    case 3:	g2d.setColor(Color.ORANGE);
					    			break;
						    case 4:	g2d.setColor(Color.YELLOW);
					    			break;
						    case 5:	g2d.setColor(Color.GREEN);
					    			break;
						    case 6:	g2d.setColor(Color.BLUE);
					    			break;
						    case 7:	g2d.setColor(new Color(102, 0, 153));	// Purple
					    			break;
						    case 8:	g2d.setColor(new Color(102, 51, 0));	// Brown
					    			break;
						}
					    }
					}

					g2d.fillRect(startX, startY, rectangleSize, rectangleSize);

					if (dragging) { gridFrame.repaint(); }
	                	}
		            }
        	    }
	            catch (ArrayIndexOutOfBoundsException aioobe) {
	            	// Ignore any clicks that occur outside the panel boundaries
	            }
	    	}
            });

            add(gridPanel, BorderLayout.CENTER);

            // Pattern grid & controls: Need inner & outer panels in order to properly position everything
            JPanel outerSidePanel = new JPanel();
            outerSidePanel.setLayout(new BorderLayout());
            outerSidePanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

            JPanel innerSidePanel = new JPanel();
            innerSidePanel.setLayout(new BorderLayout());

            patternGridLabel = new JLabel("Pattern Grid", SwingConstants.CENTER);
            innerSidePanel.add(patternGridLabel, BorderLayout.NORTH);

            patternPanel = new JPanel();
            patternPanel.setPreferredSize(new Dimension(patternSize, patternSize));
            patternPanel.setSize(patternSize, patternSize);
            // Compensate for height of label above grid
            patternPanel.setBounds(0, patternGridLabel.getHeight(), patternGridSize * rectangleSize, patternGridSize * rectangleSize);
            patternPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
            patternPanel.setBackground(Color.BLACK);
            patternPanel.setOpaque(true);
            innerSidePanel.add(patternPanel, BorderLayout.CENTER);

            patternPanel.addMouseListener(new MouseAdapter() {
        	public void mouseClicked(MouseEvent msvnt) {
        	    Graphics g = getGraphics();
        	    Graphics2D g2d = (Graphics2D) g;
        	    int pxMod = 0, pyMod = 0;

        	    try {
        		pStartX = msvnt.getX(); pStartY = msvnt.getY();

        		pxMod = pStartX % rectangleSize; pyMod = pStartY % rectangleSize;
        		if ((pxMod) != 0) { pStartX = pStartX - pxMod; }
        		if ((pyMod) != 0) { pStartY = pStartY - pyMod; }

        		// Allow for mapping of 1 grid cell to given # of screen pixels
        		pGridX = pStartX / rectangleSize; pGridY = pStartY / rectangleSize;

        		// Process only those clicks that occur inside the grid limits
        		if (pGridX <= patternGridSize && pGridY <= patternGridSize) {
        		    int cellValue = 0;
        		    cellValue = reverseCell(patternGrid, pGridX, pGridY);

				if (cellValue == 0) {
				    g2d.setColor(Color.BLACK);
				}
				else {
				    if (displayModeCurrently.isBlackWhite()) {
					g2d.setColor(Color.WHITE);
				    }
				    else if (displayModeCurrently.isGreyScale()) {
					cellValue = checkCell(patternGrid, pGridX, pGridY) + 1;
					patternGrid[pGridX][pGridY] = cellValue;

					int greyShade = 255 / cellValue;
					Color clr = new Color(greyShade, greyShade, greyShade);
					g2d.setColor(clr);
				    }
				    else if (displayModeCurrently.isColorScale()) {
					cellValue = checkCell(patternGrid, pGridX, pGridY) + 1;
					patternGrid[pGridX][pGridY] = cellValue;

					switch (cellValue) {
					    case 1:	g2d.setColor(Color.WHITE);
					    		break;
					    case 2:	g2d.setColor(Color.RED);
				    			break;
					    case 3:	g2d.setColor(Color.ORANGE);
				    			break;
					    case 4:	g2d.setColor(Color.YELLOW);
				    			break;
					    case 5:	g2d.setColor(Color.GREEN);
				    			break;
					    case 6:	g2d.setColor(Color.BLUE);
				    			break;
					    case 7:	g2d.setColor(new Color(102, 0, 153));	// Purple
				    			break;
					    case 8:	g2d.setColor(new Color(102, 51, 0));	// Brown
				    			break;
					}
				    }
				}

        		    // Limit drawing height to area between labels & controls and width to area between main grid and window edge
        		    g2d.fillRect(pStartX + gridPanel.getWidth() + 2, pStartY + patternGridLabel.getHeight() + 2, rectangleSize, rectangleSize);
                	}
        	    }
        	    catch (ArrayIndexOutOfBoundsException aioobe) {
        		// Ignore any clicks that occur outside the panel boundaries
        	    }
        	}
            });

            patternPanel.addMouseMotionListener(new MouseMotionAdapter() {
        	public void mouseDragged(MouseEvent msvnt) {
        	    Graphics g = getGraphics();
        	    Graphics2D g2d = (Graphics2D) g;
        	    int pxMod = 0, pyMod = 0;

        	    try {
            		pStartX = msvnt.getX(); pStartY = msvnt.getY();

            		pxMod = pStartX % rectangleSize; pyMod = pStartY % rectangleSize;
	                if ((pxMod) != 0) { pStartX = pStartX - pxMod; }
	                if ((pyMod) != 0) { pStartY = pStartY - pyMod; }

			// Allow for mapping of 1 grid cell to given # of screen pixels
                	pGridX = pStartX / rectangleSize; pGridY = pStartY / rectangleSize;

			// Process only those clicks that occur inside the grid limits
	                if (pGridX <= patternGridSize && pGridY <= patternGridSize) {
	                    int cellValue = 0;
	                    cellValue = reverseCell(patternGrid, pGridX, pGridY);

				if (cellValue == 0) {
				    g2d.setColor(Color.BLACK);
				}
				else {
				    if (displayModeCurrently.isBlackWhite()) {
					g2d.setColor(Color.WHITE);
				    }
				    else if (displayModeCurrently.isGreyScale()) {
					cellValue = checkCell(patternGrid, pGridX, pGridY) + 1;
					patternGrid[pGridX][pGridY] = cellValue;

					int greyShade = 255 / cellValue;
					Color clr = new Color(greyShade, greyShade, greyShade);
					g2d.setColor(clr);
				    }
				    else if (displayModeCurrently.isColorScale()) {
					cellValue = checkCell(patternGrid, pGridX, pGridY) + 1;
					patternGrid[pGridX][pGridY] = cellValue;

					switch (cellValue) {
					    case 1:	g2d.setColor(Color.WHITE);
					    		break;
					    case 2:	g2d.setColor(Color.RED);
				    			break;
					    case 3:	g2d.setColor(Color.ORANGE);
				    			break;
					    case 4:	g2d.setColor(Color.YELLOW);
				    			break;
					    case 5:	g2d.setColor(Color.GREEN);
				    			break;
					    case 6:	g2d.setColor(Color.BLUE);
				    			break;
					    case 7:	g2d.setColor(new Color(102, 0, 153));	// Purple
				    			break;
					    case 8:	g2d.setColor(new Color(102, 51, 0));	// Brown
				    			break;
					}
				    }
				}

	                    // Limit drawing height to area between labels & controls and width to area between main grid and window edge
	                    g2d.fillRect(pStartX + gridPanel.getWidth() + 2, pStartY + patternGridLabel.getHeight() + 2, rectangleSize, rectangleSize);

	                    if (dragging) { patternPanel.repaint(); }
	                }
	            }
	            catch (ArrayIndexOutOfBoundsException aioobe) {
	            	// Ignore any clicks that occur outside the panel boundaries
	            }
	    	}
            });

            innerSidePanel.add(patternPanel, BorderLayout.CENTER);

            JPanel clearButtonPanel = new JPanel();
            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
        	    patternPanel.removeAll();
        	    initializeGrid(patternGrid);
        	    gridFrame.repaint();
        	}
            });
            clearButtonPanel.add(clearButton, BorderLayout.CENTER);
            innerSidePanel.add(clearButtonPanel, BorderLayout.SOUTH);
            outerSidePanel.add(innerSidePanel, BorderLayout.NORTH);

            JPanel outerButtonPanel = new JPanel();
            JPanel innerButtonPanel = new JPanel();
            innerButtonPanel.setLayout(new GridLayout(0, 1));

            JLabel drawingStyleLabel = new JLabel("Drawing Mode", SwingConstants.CENTER);
            drawingStyleLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
            innerButtonPanel.add(drawingStyleLabel);

            // Drawing Mode Buttons
            ButtonGroup drawingStyleButtons = new ButtonGroup();
            JRadioButton freeHand = new JRadioButton("Freehand in Main Grid");
            freeHand.setSelected(true);
            freeHand.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
        	    drawingModeCurrently.setFreeHand();
        	}
            });
            drawingStyleButtons.add(freeHand);
            innerButtonPanel.add(freeHand);

            JRadioButton stamp = new JRadioButton("Stamp: Pattern > Main Grid");
            stamp.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
            	drawingModeCurrently.setPatternStamp();
                }
            });
            drawingStyleButtons.add(stamp);
            innerButtonPanel.add(stamp);

            JRadioButton copy = new JRadioButton("Copy: Main Grid > Pattern");
            copy.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
            	drawingModeCurrently.setScreenCopy();
                }
            });
            drawingStyleButtons.add(copy);
            innerButtonPanel.add(copy);

            JLabel displayStyleLabel = new JLabel("Display Mode", SwingConstants.CENTER);
            displayStyleLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
            innerButtonPanel.add(displayStyleLabel);

            // Display Mode Buttons
            ButtonGroup displayStyleButtons = new ButtonGroup();
            JRadioButton blackAndWhite = new JRadioButton("Black and White");
            blackAndWhite.setSelected(true);
            blackAndWhite.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
        	    displayModeCurrently.setBlackWhite();
        	    gridFrame.repaint();
        	}
            });
            displayStyleButtons.add(blackAndWhite);
            innerButtonPanel.add(blackAndWhite);

            JRadioButton greyScaling = new JRadioButton("Grey Scale");
            greyScaling.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    displayModeCurrently.setGreyScale();
        	    gridFrame.repaint();
                }
            });
            displayStyleButtons.add(greyScaling);
            innerButtonPanel.add(greyScaling);

            JRadioButton colorScaling = new JRadioButton("Color Scale");
            colorScaling.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    displayModeCurrently.setColorScale();
        	    gridFrame.repaint();
                }
            });
            displayStyleButtons.add(colorScaling);
            innerButtonPanel.add(colorScaling);

            // Need filler panel to ensure that pattern panel stays at top of window
            JPanel fillerPanel = new JPanel();
            innerButtonPanel.add(fillerPanel);
            outerButtonPanel.add(innerButtonPanel);
            outerSidePanel.add(outerButtonPanel, BorderLayout.CENTER);

            JPanel counterPanel = new JPanel();
            counterPanel.setLayout(new BorderLayout());
            counterPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
            JLabel counterLabel = new JLabel("Generations", JLabel.CENTER);
            counterPanel.add(counterLabel, BorderLayout.NORTH);
            counterValue = new JTextArea(1, 6);
            counterValue.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, false));
            counterValue.setEditable(false);
            counterPanel.add(counterValue, BorderLayout.SOUTH);
            counterValue.setText(Integer.toString(generation));
            outerSidePanel.add(counterPanel, BorderLayout.SOUTH);

            add(outerSidePanel, BorderLayout.EAST);
	}

	public static JPanel createToolPanel() {
	    gridToolPanel = new JPanel();

	    JPanel buttonPanel = new JPanel();

            JLabel rg = new JLabel("Cell Size", JLabel.CENTER);
            Hashtable<Integer, JLabel> cllSz = new Hashtable<Integer, JLabel>();
            cllSz.put(2, new JLabel("2"));
            cllSz.put(3, new JLabel("3"));
            cllSz.put(4, new JLabel("4"));
            cllSz.put(5, new JLabel("5"));

            JSlider dotSize = new JSlider(JSlider.HORIZONTAL, 2, 5, 2);
            dotSize.setMajorTickSpacing(10);
            dotSize.setMinorTickSpacing(10);
            dotSize.setPaintTicks(true);
            dotSize.setPaintLabels(true);
            dotSize.setLabelTable(cllSz);

            dotSize.addChangeListener(new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			rectangleSize = (Integer) ((JSlider)e.getSource()).getValue();
			gridSize = (screenSize / rectangleSize);
		        patternSize = (screenSize / 3) + 1; patternGridSize = (gridSize / 3);
		        resetGrids();
	    		gridFrame.repaint();
		}
            });
            buttonPanel.add(rg);
            buttonPanel.add(dotSize);

            JButton startStopButton = new JButton("Start");
            startStopButton.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (running) {
				running = false;
				timer.stop();
				((JButton) e.getSource()).setText("Start");
			}
			else {
				running = true;
				timer.start();
				((JButton) e.getSource()).setText("Stop");
			}
		}
            });
            buttonPanel.add(startStopButton);

            JButton stepButton = new JButton("Step");
            stepButton.addActionListener(new ActionListener() {
		@Override
	        public void actionPerformed(ActionEvent e) {
	    		if (leftFrame) { nextFrame(gridLeft, gridRight); }
	    		else { nextFrame(gridRight, gridLeft); }

	    		gridFrame.repaint();
	        }
            });
            buttonPanel.add(stepButton);

            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(new ActionListener() {
		@Override
	        public void actionPerformed(ActionEvent e) {
	        	gridPanel.removeAll();

	    		initializeGrid(gridLeft);
	    		initializeGrid(gridRight);

	    		gridFrame.repaint();

	    		generation = 0;
	    		counterValue.setText(Integer.toString(generation));
		}
            });
            buttonPanel.add(clearButton);

            JPanel rowPanel = new JPanel();
            rowPanel.setLayout(new BorderLayout());
            JLabel whichRow = new JLabel("Row", JLabel.CENTER);
            rowPanel.add(whichRow, BorderLayout.NORTH);
            rowValue = new JTextArea(1, 3);
            rowValue.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, false));
            rowValue.setEditable(false);
            rowPanel.add(rowValue, BorderLayout.SOUTH);
            buttonPanel.add(rowPanel);

            JPanel colPanel = new JPanel();
            colPanel.setLayout(new BorderLayout());
            JLabel whichCol = new JLabel("Col", JLabel.CENTER);
            colPanel.add(whichCol, BorderLayout.NORTH);
            colValue = new JTextArea(1, 3);
            colValue.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, false));
            colValue.setEditable(false);
            colPanel.add(colValue, BorderLayout.SOUTH);
            buttonPanel.add(colPanel);

            JCheckBox wrap = new JCheckBox("Wrap Grid");
            wrap.setSelected(false);
            wrap.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBox wrpCB = (JCheckBox) e.getSource();

			if (wrpCB.isSelected()) { wrapAround = true; }
			else { wrapAround = false; }
		}
            });
            buttonPanel.add(wrap);

            JCheckBox seeGrid = new JCheckBox("Show Grid Lines");
            seeGrid.setSelected(false);
            seeGrid.addChangeListener(new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			JCheckBox cGrdCB = (JCheckBox) e.getSource();

			if (cGrdCB.isSelected()) {showGrid = true; }
			else { showGrid = false; }

			gridFrame.repaint();
		}
            });
            buttonPanel.add(seeGrid);
            gridToolPanel.add(buttonPanel, BorderLayout.CENTER);

            JLabel fps = new JLabel("Speed", JLabel.CENTER);
            Hashtable<Integer, JLabel> frmsPrScnd = new Hashtable<Integer, JLabel>();
            //frmsPrScnd.put(1, new JLabel("1"));	// Showing all slider settings causes crowding
            frmsPrScnd.put(2, new JLabel("2"));
            //frmsPrScnd.put(3, new JLabel("3"));
            frmsPrScnd.put(4, new JLabel("4"));
            //frmsPrScnd.put(5, new JLabel("5"));
            frmsPrScnd.put(6, new JLabel("6"));
            //frmsPrScnd.put(7, new JLabel("7"));
            frmsPrScnd.put(8, new JLabel("8"));
            //frmsPrScnd.put(9, new JLabel("9"));
            frmsPrScnd.put(10, new JLabel("10"));
            //frmsPrScnd.put(11, new JLabel("11"));
            frmsPrScnd.put(12, new JLabel("12"));
            //frmsPrScnd.put(13, new JLabel("13"));
            frmsPrScnd.put(14, new JLabel("14"));
            //frmsPrScnd.put(15, new JLabel("15"));
            frmsPrScnd.put(16, new JLabel("16"));
            //frmsPrScnd.put(17, new JLabel("17"));
            frmsPrScnd.put(18, new JLabel("18"));
            //frmsPrScnd.put(19, new JLabel("19"));
            frmsPrScnd.put(20, new JLabel("20"));

            JSlider framesPerSecond = new JSlider(JSlider.HORIZONTAL, 1, 20, 10);
            framesPerSecond.setMajorTickSpacing(20);
            framesPerSecond.setMinorTickSpacing(1);
            framesPerSecond.setPaintTicks(true);
            framesPerSecond.setPaintLabels(true);
            framesPerSecond.setLabelTable(frmsPrScnd);

            framesPerSecond.addChangeListener(new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (running) { timer.stop(); }

			delayTime = 500 / ((JSlider)e.getSource()).getValue();
			timer.setDelay(delayTime);

			if (running) { timer.start(); }
		}
            });

            JPanel sliderPanel = new JPanel();
            sliderPanel.add(fps);
            sliderPanel.add(framesPerSecond);
            gridToolPanel.add(sliderPanel);

            return gridToolPanel;
	}

    public static JPanel createRulePanel() {
        rulePanel = new JPanel();
        rulePanel.setLayout(new BorderLayout());

        JPanel groupRulePanel = new JPanel();
        groupRulePanel.setLayout(new GridLayout(0, 1));

        JLabel groupTitle = new JLabel("Neighbor Cell Settings", JLabel.CENTER);
        groupTitle.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        groupRulePanel.add(groupTitle);

        JLabel mm1 = new JLabel("Basic Min. On / Max. Off", JLabel.CENTER);
        Hashtable<Integer, JLabel> moMo1 = new Hashtable<Integer, JLabel>();
        moMo1.put(1, new JLabel("1"));
        moMo1.put(2, new JLabel("2"));
        moMo1.put(3, new JLabel("3"));
        moMo1.put(4, new JLabel("4"));
        moMo1.put(5, new JLabel("5"));
        moMo1.put(6, new JLabel("6"));
        moMo1.put(7, new JLabel("7"));
        moMo1.put(8, new JLabel("8"));
        groupRulePanel.add(mm1);

        JSlider minOnMaxOff = new JSlider(JSlider.HORIZONTAL, 1, 8, 2);
        minOnMaxOff.setMajorTickSpacing(10);
        minOnMaxOff.setMinorTickSpacing(10);
        minOnMaxOff.setPaintTicks(true);
        minOnMaxOff.setPaintLabels(true);
        minOnMaxOff.setLabelTable(moMo1);

        minOnMaxOff.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
        	minimumOnMaximumOff = ((JSlider)e.getSource()).getValue();
            }
        });
        groupRulePanel.add(minOnMaxOff);

        JLabel mm2 = new JLabel("Basic Max. On / Min. Off", JLabel.CENTER);
        Hashtable<Integer, JLabel> moMo2 = new Hashtable<Integer, JLabel>();
        moMo2.put(1, new JLabel("1"));
        moMo2.put(2, new JLabel("2"));
        moMo2.put(3, new JLabel("3"));
        moMo2.put(4, new JLabel("4"));
        moMo2.put(5, new JLabel("5"));
        moMo2.put(6, new JLabel("6"));
        moMo2.put(7, new JLabel("7"));
        moMo2.put(8, new JLabel("8"));
        groupRulePanel.add(mm2);

        JSlider maxOnMinOff = new JSlider(JSlider.HORIZONTAL, 1, 8, 3);
        maxOnMinOff.setMajorTickSpacing(10);
        maxOnMinOff.setMinorTickSpacing(10);
        maxOnMinOff.setPaintTicks(true);
        maxOnMinOff.setPaintLabels(true);
        maxOnMinOff.setLabelTable(moMo2);

        maxOnMinOff.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
		maximumOnMinimumOff = ((JSlider)e.getSource()).getValue();
            }
	});
        groupRulePanel.add(maxOnMinOff);

        JPanel onOffPanel = new JPanel();
        JButton minMaxOnOff = new JButton("Disable Min/Max");
        minMaxOnOff.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
        	if (wildcardsOnly) {
        	    ((JButton) e.getSource()).setText("Disable Min/Max");
        	    wildcardsOnly = false;
        	    minOnMaxOff.setEnabled(true);
        	    maxOnMinOff.setEnabled(true);
        	}
        	else {
        	    ((JButton) e.getSource()).setText("Enable Min/Max");
        	    wildcardsOnly = true;
        	    minOnMaxOff.setEnabled(false);
        	    maxOnMinOff.setEnabled(false);
        	}
            }
        });
        onOffPanel.add(minMaxOnOff);
        groupRulePanel.add(onOffPanel);

        JLabel wildCardOnTitle = new JLabel("Wildcard On Settings", JLabel.CENTER);
        wildCardOnTitle.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        groupRulePanel.add(wildCardOnTitle);

        JPanel wildcardOnPanel = new JPanel();
        wildcardOnPanel.setLayout(new GridLayout(2, 8));

        
        JCheckBox wildcardCBOn1 = new JCheckBox("1");
        wildcardCBOn1.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (wildcardCBOn1.isSelected()) { wildcardsOn[1] = true; }
                else { wildcardsOn[1] = false; };
              }
            });
        wildcardOnPanel.add(wildcardCBOn1);

        JCheckBox wildcardCBOn2 = new JCheckBox("2");
        wildcardCBOn2.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            if (wildcardCBOn2.isSelected()) { wildcardsOn[2] = true; }
            else { wildcardsOn[2] = false; };
          }
        });
        wildcardOnPanel.add(wildcardCBOn2);

        JCheckBox wildcardCBOn3 = new JCheckBox("3");
        wildcardCBOn3.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            if (wildcardCBOn3.isSelected()) { wildcardsOn[3] = true; }
            else { wildcardsOn[3] = false; };
          }
        });
        wildcardOnPanel.add(wildcardCBOn3);

        JCheckBox wildcardCBOn4 = new JCheckBox("4");
        wildcardCBOn4.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            if (wildcardCBOn4.isSelected()) { wildcardsOn[4] = true; }
            else { wildcardsOn[4] = false; };
          }
        });
        wildcardOnPanel.add(wildcardCBOn4);

        JCheckBox wildcardCBOn5 = new JCheckBox("5");
        wildcardCBOn5.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            if (wildcardCBOn5.isSelected()) { wildcardsOn[5] = true; }
            else { wildcardsOn[5] = false; };
          }
        });
        wildcardOnPanel.add(wildcardCBOn5);

        JCheckBox wildcardCBOn6 = new JCheckBox("6");
        wildcardCBOn6.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            if (wildcardCBOn6.isSelected()) { wildcardsOn[6] = true; }
            else { wildcardsOn[6] = false; };
          }
        });
        wildcardOnPanel.add(wildcardCBOn6);

        JCheckBox wildcardCBOn7 = new JCheckBox("7");
        wildcardCBOn7.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            if (wildcardCBOn7.isSelected()) { wildcardsOn[7] = true; }
            else { wildcardsOn[7] = false; };
          }
        });
        wildcardOnPanel.add(wildcardCBOn7);

        JCheckBox wildcardCBOn8 = new JCheckBox("8");
        wildcardCBOn8.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (wildcardCBOn8.isSelected()) { wildcardsOn[8] = true; }
                else { wildcardsOn[8] = false; };
              }
            });
        wildcardOnPanel.add(wildcardCBOn8);
        groupRulePanel.add(wildcardOnPanel);

        JLabel wildCardOffTitle = new JLabel("Wildcard Off Settings", JLabel.CENTER);
        wildCardOffTitle.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        groupRulePanel.add(wildCardOffTitle);

        JPanel wildcardOffPanel = new JPanel();
        wildcardOffPanel.setLayout(new GridLayout(2, 8));

        JCheckBox wildcardCBOff1 = new JCheckBox("1");
        wildcardCBOff1.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (wildcardCBOff1.isSelected()) { wildcardsOff[1] = true; }
                else { wildcardsOff[1] = false; };
              }
            });
        wildcardOffPanel.add(wildcardCBOff1);

        JCheckBox wildcardCBOff2 = new JCheckBox("2");
        wildcardCBOff2.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (wildcardCBOff2.isSelected()) { wildcardsOff[2] = true; }
                else { wildcardsOff[2] = false; };
              }
            });
        wildcardOffPanel.add(wildcardCBOff2);

        JCheckBox wildcardCBOff3 = new JCheckBox("3");
        wildcardCBOff3.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (wildcardCBOff3.isSelected()) { wildcardsOff[3] = true; }
                else { wildcardsOff[3] = false; };
              }
            });
        wildcardOffPanel.add(wildcardCBOff3);

        JCheckBox wildcardCBOff4 = new JCheckBox("4");
        wildcardCBOff4.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (wildcardCBOff4.isSelected()) { wildcardsOff[4] = true; }
                else { wildcardsOff[4] = false; };
              }
            });
        wildcardOffPanel.add(wildcardCBOff4);

        JCheckBox wildcardCBOff5 = new JCheckBox("5");
        wildcardCBOff5.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (wildcardCBOff5.isSelected()) { wildcardsOff[5] = true; }
                else { wildcardsOff[5] = false; };
              }
            });
        wildcardOffPanel.add(wildcardCBOff5);

        JCheckBox wildcardCBOff6 = new JCheckBox("6");
        wildcardCBOff6.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (wildcardCBOff6.isSelected()) { wildcardsOff[6] = true; }
                else { wildcardsOff[6] = false; };
              }
            });
        wildcardOffPanel.add(wildcardCBOff6);

        JCheckBox wildcardCBOff7 = new JCheckBox("7");
        wildcardCBOff7.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (wildcardCBOff7.isSelected()) { wildcardsOff[7] = true; }
                else { wildcardsOff[7] = false; };
              }
            });
        wildcardOffPanel.add(wildcardCBOff7);

        JCheckBox wildcardCBOff8 = new JCheckBox("8");
        wildcardCBOff8.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (wildcardCBOff8.isSelected()) { wildcardsOff[8] = true; }
                else { wildcardsOff[8] = false; };
              }
            });
        wildcardOffPanel.add(wildcardCBOff8);
        groupRulePanel.add(wildcardOffPanel);

        JPanel resetPanel = new JPanel();
        JButton reset = new JButton("Reset");
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
        	minOnMaxOff.setValue(2);
        	maxOnMinOff.setValue(3);

        	minMaxOnOff.setText("Disable Min/Max");
        	wildcardsOnly = false;
        	minOnMaxOff.setEnabled(true);
        	maxOnMinOff.setEnabled(true);

        	wildcardCBOn1.setSelected(false);
        	wildcardCBOn2.setSelected(false);
        	wildcardCBOn3.setSelected(false);
        	wildcardCBOn4.setSelected(false);
        	wildcardCBOn5.setSelected(false);
        	wildcardCBOn6.setSelected(false);
        	wildcardCBOn7.setSelected(false);
        	wildcardCBOn8.setSelected(false);

        	wildcardCBOff1.setSelected(false);
        	wildcardCBOff2.setSelected(false);
        	wildcardCBOff3.setSelected(false);
        	wildcardCBOff4.setSelected(false);
        	wildcardCBOff5.setSelected(false);
        	wildcardCBOff6.setSelected(false);
        	wildcardCBOff7.setSelected(false);
        	wildcardCBOff8.setSelected(false);
            }
        });
        resetPanel.add(reset);
        groupRulePanel.add(resetPanel);

        rulePanel.add(groupRulePanel, BorderLayout.NORTH);

        JPanel fillerPanel = new JPanel();
        rulePanel.add(fillerPanel, BorderLayout.SOUTH);

        return rulePanel;
    }

    private static void initializeGrid(int[][] whichGrid) {
	for (int row = 0; row < whichGrid.length; row++) {
	    for (int col = 0; col < whichGrid.length; col++) {
		whichGrid[row][col] = 0;
	    }
	}
    }

    private static void resetGrids() {
	// Set the grids to the designated grid size limit
	gridLeft = new int[gridSize][gridSize];
	gridRight = new int[gridSize][gridSize];
	patternGrid = new int[patternGridSize][patternGridSize];

	initializeGrid(gridLeft);
	initializeGrid(gridRight);
	initializeGrid(patternGrid);
    }

	private static int checkCell(int[][] currentGrid, int row, int col) {
		int howManyActiveNeighbors = 0;
		int gridLimit = currentGrid.length - 1, insideGridLimit = currentGrid.length - 2;

		// Direct method is to test & set each case individually: more instructions but easier to debug
		if (wrapAround) {	// Wrap around to opposite grid edges, to test all 8 cells
			// Top left corner
			if ((row == 0) && (col == 0)) {
				if (currentGrid[gridLimit][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][1] != 0) { howManyActiveNeighbors++; }
			}
			// Top row, middle columns
			else if ((row == 0) && (col > 0 && col < gridLimit)) {
				if (currentGrid[gridLimit][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][col] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][col+1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][col+1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][col] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][col+1] != 0) { howManyActiveNeighbors++; }
			}
			// Top right corner
			else if ((row == 0) && (col == gridLimit)) {
				if (currentGrid[gridLimit][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][0] != 0) { howManyActiveNeighbors++; }
			}
			// Left side, middle rows
			else if ((row > 0 && row < gridLimit) && (col == 0)) {
				if (currentGrid[row-1][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row-1][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row-1][1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row][1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][1] != 0) { howManyActiveNeighbors++; }
			}
			// Middle area
			else if ((row > 0 && row < gridLimit) && (col > 0 && col < gridLimit)) {
				if (currentGrid[row-1][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row-1][col] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row-1][col+1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row][col+1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][col] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][col+1] != 0) { howManyActiveNeighbors++; }
			}
			// Right side, middle rows
			else if ((row > 0 && row < gridLimit) && (col == gridLimit)) {
				if (currentGrid[row-1][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row-1][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row-1][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][0] != 0) { howManyActiveNeighbors++; }
			}
			// Bottom left corner
			else if ((row == gridLimit) && (col == 0)) {
				if (currentGrid[insideGridLimit][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][1] != 0) { howManyActiveNeighbors++; }
			}
			// Bottom row, middle columns
			else if ((row == gridLimit) && (col > 0 && col < gridLimit)) {
				if (currentGrid[insideGridLimit][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][col] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][col+1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][col+1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][col] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][col+1] != 0) { howManyActiveNeighbors++; }
			}
			// Bottom right corner
			else if ((row == gridLimit) && (col == gridLimit)) {
				if (currentGrid[insideGridLimit][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][0] != 0) { howManyActiveNeighbors++; }
			}
		}
		else {	// NOT wrap around
			// Top left corner
			if ((row == 0) && (col == 0)) {
				if (currentGrid[1][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][1] != 0) { howManyActiveNeighbors++; }
			}
			// Top row, middle columns
			else if ((row == 0) && (col > 0 && col < gridLimit)) {
				if (currentGrid[0][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[0][col+1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][col] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][col+1] != 0) { howManyActiveNeighbors++; }
			}
			// Top right corner
			else if ((row == 0) && (col == gridLimit)) {
				if (currentGrid[0][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[1][gridLimit] != 0) { howManyActiveNeighbors++; }
			}
			// Left side, middle rows
			else if ((row > 0 && row < gridLimit) && (col == 0)) {
				if (currentGrid[row-1][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row-1][1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row][1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][1] != 0) { howManyActiveNeighbors++; }
			}
			// Middle area: test all 8 cells around current cells
			else if ((row > 0 && row < gridLimit) && (col > 0 && col < gridLimit)) {
				if (currentGrid[row-1][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row-1][col] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row-1][col+1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row][col+1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][col] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][col+1] != 0) { howManyActiveNeighbors++; }
			}
			// Right side, middle rows
			else if ((row > 0 && row < gridLimit) && (col == gridLimit)) {
				if (currentGrid[row-1][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row-1][gridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[row+1][gridLimit] != 0) { howManyActiveNeighbors++; }
			}
			// Bottom left corner
			else if ((row == gridLimit) && (col == 0)) {
				if (currentGrid[insideGridLimit][0] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][1] != 0) { howManyActiveNeighbors++; }
			}
			// Bottom row, middle columns
			else if ((row == gridLimit) && (col > 0 && col < gridLimit)) {
				if (currentGrid[insideGridLimit][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][col] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][col+1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][col-1] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[gridLimit][col+1] != 0) { howManyActiveNeighbors++; }
			}
			// Bottom right corner
			else if ((row == gridLimit) && (col == gridLimit)) {
				if (currentGrid[gridLimit][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][insideGridLimit] != 0) { howManyActiveNeighbors++; }
				if (currentGrid[insideGridLimit][gridLimit] != 0) { howManyActiveNeighbors++; }
			}
		}

		return howManyActiveNeighbors;
	}

	private static void checkAndUpdateCell(int[][] currentGrid, int row, int col, int[][] nextGrid) {
		int howManyActiveNeighbors = checkCell(currentGrid, row, col);

		/* Below are basic Game of Life rules, which are incorporated into enhanced, more flexible rules
		Any live cell with fewer than 2 live neighbors dies, as if by underpopulation.
		Any live cell with 2 or 3 live neighbors lives on to the next generation.
		Any live cell with more than 3 live neighbors dies, as if by overpopulation.
		Any dead cell with exactly 3 live neighbors becomes a live cell, as if by reproduction. */
		if (!wildcardsOnly) {	// Allows more flexibility than always having range of values active
		    if ((currentGrid[row][col] == 0) && (howManyActiveNeighbors == maximumOnMinimumOff)) {
			if (displayModeCurrently.isBlackWhite()) { nextGrid[row][col] = 1; }
			else { nextGrid[row][col] = howManyActiveNeighbors; }
		    }
		    else if (currentGrid[row][col] != 0) {
			if ((howManyActiveNeighbors < minimumOnMaximumOff) || (howManyActiveNeighbors > maximumOnMinimumOff)) {
			    if (displayModeCurrently.isBlackWhite()) { nextGrid[row][col] = 0; }
			    else { nextGrid[row][col] = howManyActiveNeighbors; }
			}
			else if ((howManyActiveNeighbors == minimumOnMaximumOff) || (howManyActiveNeighbors == maximumOnMinimumOff)) {
			    if (displayModeCurrently.isBlackWhite()) { nextGrid[row][col] = 1; }
			    else { nextGrid[row][col] = howManyActiveNeighbors; }
			}
		    }
		}

		// Additional rules, implemented by wildcard on / off checkboxes
		if ((currentGrid[row][col] == 0) && (howManyActiveNeighbors > 0) && (wildcardsOn[howManyActiveNeighbors] == true)) {
		    if (displayModeCurrently.isBlackWhite()) { nextGrid[row][col] = 1; }
		    else { nextGrid[row][col] = howManyActiveNeighbors; }
		}
		else if ((currentGrid[row][col] != 0) && (howManyActiveNeighbors > 0) && (wildcardsOff[howManyActiveNeighbors] == true)) {
		    nextGrid[row][col] = 0;
		}
	}

    private static void nextFrame(int[][] currentGrid, int[][] nextGrid) {
	for (int row = 0; row < currentGrid.length; row++) {
	    for (int col = 0; col < currentGrid.length; col++) {
		checkAndUpdateCell(currentGrid, row, col, nextGrid);
	    }
	}

	initializeGrid(currentGrid);

	if (leftFrame) { leftFrame = false; }
	else { leftFrame = true; }

	generation++;
	counterValue.setText(Integer.toString(generation));
    }

    private static int reverseCell(int[][] whichGrid, int row, int col) {
	if (whichGrid[row][col] == 0) {
	    whichGrid[row][col] = 1;
	    return 1;
	}
	else {
	    whichGrid[row][col] = 0;
	    return 0;
	}
    }

    // Separate copy and stamp methods for clarity
    private void stampGrid(int mainGridRow, int mainGridCol) {
	// First check which frame to use: less overhead than performing test inside loops
	if (leftFrame) {
	    for (int currentRow = 0; currentRow < patternGrid.length; currentRow++) {
		for (int currentCol = 0; currentCol < patternGrid.length; currentCol++) {
		    gridLeft[mainGridRow + currentRow][mainGridCol + currentCol] = patternGrid[currentRow][currentCol];
		}
	    }
	}
	else {
	    for (int currentRow = 0; currentRow < patternGrid.length; currentRow++) {
		for (int currentCol = 0; currentCol < patternGrid.length; currentCol++) {
		    gridRight[mainGridRow + currentRow][mainGridCol + currentCol] = patternGrid[currentRow][currentCol];
		}
	    }
	}
    }

    private void copyGrid(int mainGridRow, int mainGridCol) {
	// First check which frame to use: less overhead than performing test inside loops
	if (leftFrame) {
	    for (int currentRow = 0; currentRow < patternGrid.length; currentRow++) {
		for (int currentCol = 0; currentCol < patternGrid.length; currentCol++) {
		    patternGrid[currentRow][currentCol] = gridLeft[mainGridRow + currentRow][mainGridCol + currentCol];
		}
	    }
	}
	else {
	    for (int currentRow = 0; currentRow < patternGrid.length; currentRow++) {
		for (int currentCol = 0; currentCol < patternGrid.length; currentCol++) {
		    patternGrid[currentRow][currentCol] = gridRight[mainGridRow + currentRow][mainGridCol + currentCol];
		}
	    }
	}
    }

    private void paintGridLines(Graphics2D g2d) {
	int stepSize = screenSize / gridSize;
	g2d.setColor(Color.GRAY);

	for (int row = 0; row < screenSize - 2; row = row + stepSize) {
	    g2d.drawLine(0, row, screenSize, row);
	}

	for (int col = 0; col < screenSize - 2; col = col + stepSize) {
	    g2d.drawLine(col, 0, col, screenSize);
	}
    }

    public void paintFromThisGrid(int[][] whichGrid, Graphics2D g2d) {
    	int whichGridSize = whichGrid.length - 1;

    	for (int row = 0; row <= whichGridSize; row++) {
    	    for (int col = 0; col <= whichGridSize; col++) {
    		if (whichGrid[row][col] == 0) {
    		    g2d.setColor(Color.BLACK);
    		}
    		else {
    		    if (displayModeCurrently.isBlackWhite()) {
    			g2d.setColor(Color.WHITE);
    		    }
    		    else if (displayModeCurrently.isGreyScale()) {
    			int greyShade = 255 / whichGrid[row][col];
    			if (greyShade < 170) { greyShade *= 1.5; }
    			Color clr = new Color(greyShade, greyShade, greyShade);
    			g2d.setColor(clr);
    		    }
    		    else if (displayModeCurrently.isColorScale()) {
    			switch (whichGrid[row][col]) {
    			    case 1:	g2d.setColor(Color.WHITE);
    			    		break;
    			    case 2:	g2d.setColor(Color.RED);
    			    		break;
    			    case 3:	g2d.setColor(Color.ORANGE);
		    			break;
    			    case 4:	g2d.setColor(Color.YELLOW);
		    			break;
    			    case 5:	g2d.setColor(Color.GREEN);
		    			break;
    			    case 6:	g2d.setColor(Color.BLUE);
		    			break;
    			    case 7:	g2d.setColor(new Color(102, 0, 153));	// Purple
		    			break;
    			    case 8:	g2d.setColor(new Color(102, 51, 0));	// Brown
		    			break;
    			}
    		    }
    		}

    		// Allow for mapping of 1 grid cell to designated # of screen pixels
    		g2d.fillRect(row * rectangleSize, col * rectangleSize, rectangleSize, rectangleSize);
    	    }
    	}
    }

    public void paintPatternGrid(Graphics2D g2d) {
    	int patternGridSize = patternGrid.length - 1;

    	for (int row = 0; row <= patternGridSize; row++) {
    	    for (int col = 0; col <= patternGridSize; col++) {
    		if (patternGrid[row][col] == 0) {
    		    g2d.setColor(Color.BLACK);
    		}
    		else {
    		    if (displayModeCurrently.isBlackWhite()) {
    			g2d.setColor(Color.WHITE);
    		    }
    		    else if (displayModeCurrently.isGreyScale()) {
    			int greyShade = 255 / patternGrid[row][col];
    			if (greyShade < 170) { greyShade *= 1.5; }
    			Color clr = new Color(greyShade, greyShade, greyShade);
    			g2d.setColor(clr);
    		    }
    		    else if (displayModeCurrently.isColorScale()) {
    			switch (patternGrid[row][col]) {
    			    case 1:	g2d.setColor(Color.WHITE);
    			    		break;
    			    case 2:	g2d.setColor(Color.RED);
    			    		break;
    			    case 3:	g2d.setColor(Color.ORANGE);
		    			break;
    			    case 4:	g2d.setColor(Color.YELLOW);
		    			break;
    			    case 5:	g2d.setColor(Color.GREEN);
		    			break;
    			    case 6:	g2d.setColor(Color.BLUE);
		    			break;
    			    case 7:	g2d.setColor(new Color(102, 0, 153));	// Purple
		    			break;
    			    case 8:	g2d.setColor(new Color(102, 51, 0));	// Brown
		    			break;
    			}
    		    }
    		}

    		// Allow for mapping of 1 grid cell to designated amount of screen pixels
    		g2d.fillRect((row * rectangleSize) + gridPanel.getWidth() + 2,
    				(col * rectangleSize) + patternGridLabel.getHeight() + 2,
    				rectangleSize, rectangleSize);
    	    }
    	}
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.WHITE);
        Graphics2D g2d = (Graphics2D) g;

        // Refresh grid pattern
        if (leftFrame) { paintFromThisGrid(gridLeft, g2d); }
        else { paintFromThisGrid(gridRight, g2d); }

        // Update select rectangle
        if ((drawingModeCurrently.isPatternStamp()) || (drawingModeCurrently.isScreenCopy())) {
            // Get display grid size
            int focusArea = (screenSize - patternSize);
            // Get task bar size
            int taskbarheight = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight()
        	    - GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getHeight());

            // Get mouse location, then compensate for sizes of surrounding panels and task bar
	    int x = MouseInfo.getPointerInfo().getLocation().x - rulePanel.getWidth() - 4;
	    int y = MouseInfo.getPointerInfo().getLocation().y - gridToolPanel.getHeight() - taskbarheight + 12;

	    if ((x < focusArea) && (y < focusArea) && (x > 0) && (y > 0)) {
		if (leftFrame) { paintFromThisGrid(gridLeft, g2d); }
		else { paintFromThisGrid(gridRight, g2d); }

		g2d.setColor(Color.WHITE);
		g2d.drawRect(x, y, patternSize, patternSize);
	    }
	}

        // Update grid lines
        if (showGrid) { paintGridLines(g2d); }

        paintPatternGrid(g2d);
    }

    @Override
    public void paintComponent(Graphics g) {
    	super.paintComponent(g);
        g.setColor(Color.WHITE);
        Graphics2D g2d = (Graphics2D) g;

        paintFromThisGrid(patternGrid, g2d);
        if (showGrid) { paintGridLines(g2d); }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        JCheckBoxMenuItem mi = (JCheckBoxMenuItem)(e.getSource());
        boolean selected = (e.getStateChange() == ItemEvent.SELECTED);

        //Set the enabled state of the appropriate Action.
        if (mi == cbmi[0]) {
            startStop.setEnabled(selected);
        }
        else if (mi == cbmi[1]) {
            step.setEnabled(selected);
        }
        else if (mi == cbmi[2]) {
            clear.setEnabled(selected);
        }
    }

    // Create & display the GUI.
    public static void createAndShowGUI() {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window
        gridFrame = new JFrame("Game Of Life");
        gridFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Set up the content & pattern panes
        GameOfLife bwGOL = new GameOfLife();
        gridFrame.add(bwGOL, BorderLayout.CENTER);

        // Set up the controls bar
        gridToolPanel = createToolPanel();
        gridFrame.add(gridToolPanel, BorderLayout.NORTH);

        // Set up the rules panel
        rulePanel = createRulePanel();
        gridFrame.add(rulePanel, BorderLayout.WEST);

        //Display the window.
        gridFrame.pack();
        gridFrame.setVisible(true);
    }

    public static void main(String[] args) {
        createAndShowGUI();

        drawingModeCurrently = new drawingMode();
        displayModeCurrently = new displayMode();

        timer = new Timer(delayTime, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
        	if (leftFrame) { nextFrame(gridLeft, gridRight); }
        	else { nextFrame(gridRight, gridLeft);         	}

            	gridFrame.repaint();
            }
        });
    }
}